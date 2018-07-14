package com.xyfindables.sdk.devices

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.content.Context
import com.xyfindables.sdk.ads.XYBleAd
import com.xyfindables.sdk.gatt.XYBluetoothGatt
import com.xyfindables.sdk.scanner.XYScanRecord
import com.xyfindables.sdk.devices.router.Router
import com.xyfindables.sdk.devices.router.RouterInterface
import com.xyfindables.sdk.scanner.XYScanResult
import kotlinx.coroutines.experimental.*
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.util.*

open class XYBluetoothDevice (context: Context, device:BluetoothDevice) : XYBluetoothGatt(context, device, false, null, null, null, null) {

    private var references = 0

    private val listeners = HashMap<String, WeakReference<Listener>>()
    val ads = HashMap<Int, XYBleAd>()

    var rssi = OUTOFRANGE_RSSI

    var detectCount = 0
    var enterCount = 0
    var exitCount = 0

    //set this to true if the device should report that it is out of
    //range right after discconnect.  Generally used for devices
    //with rotating MAC addresses
    var exitAfterDisconnect = false

    //last time this device was accessed (connected to)
    var lastAccessTime = 0L

    //last time we heard a ad from this device
    var lastAdTime = 0L

    val address : String
        get() {
            return device.address
        }

    val id : String
        get() {
            return device.address
        }

    val name: String?
        get() {
            return device.name
        }

    var outOfRangeDelay = OUTOFRANGE_DELAY

    var notifyExit : ((device: XYBluetoothDevice)->(Unit))? = null

    //this should only be called from the onEnter function so that
    //there is one onExit for every onEnter
    private fun checkForExit() {
        launch(CommonPool) {
            //logInfo("checkForExit: $id")
            delay(outOfRangeDelay)

            //check if something else has already marked it as exited
            //this should only happen if another system (exit on connection drop for example)
            //marks this as out of range
            if (rssi == OUTOFRANGE_RSSI) {
                return@launch
            }

            if ((now - lastAdTime) > outOfRangeDelay && (now - lastAccessTime) > outOfRangeDelay) {
                rssi = OUTOFRANGE_RSSI
                onExit()

                //make it thread safe
                val localNotifyExit = notifyExit
                if (localNotifyExit != null) {
                    launch(CommonPool) {
                        localNotifyExit(this@XYBluetoothDevice)
                    }
                }
            } else {
                checkForExit()
            }
        }
    }

    fun onEnter() {
        logInfo("onEnter: $id")
        enterCount++
        lastAdTime = now
        synchronized(listeners) {
            for ((_, listener) in listeners) {
                val innerListener = listener.get()
                if (innerListener != null) {
                    launch (CommonPool) {
                        innerListener.entered(this@XYBluetoothDevice)
                    }
                }
            }
        }
        checkForExit()
    }

    fun onExit() {
        logInfo("onExit: $id")
        exitCount++
        synchronized(listeners) {
            for ((_, listener) in listeners) {
                val innerListener = listener.get()
                if (innerListener != null) {
                    launch(CommonPool) {
                        innerListener.exited(this@XYBluetoothDevice)
                    }
                }
            }
        }
        asyncClose()
    }

    fun onDetect() {
        detectCount++
        lastAdTime = now
        synchronized(listeners) {
            for ((_, listener) in listeners) {
                val innerListener = listener.get()
                if (innerListener != null) {
                    launch(CommonPool) {
                        innerListener.detected(this@XYBluetoothDevice)
                    }
                }
            }
        }
    }

    fun onConnectionStateChange(newState: Int) {
        logInfo("onConnectionStateChange: $id : $newState")
        synchronized(listeners) {
            for ((_, listener) in listeners) {
                val innerListener = listener.get()
                if (innerListener != null) {
                    launch(CommonPool) {
                        innerListener.connectionStateChanged(this@XYBluetoothDevice, newState)
                    }
                }
            }
        }
        //if a connection drop means we should mark it as out of range, then lets do it!
        if (exitAfterDisconnect) {
            launch(CommonPool) {
                rssi = OUTOFRANGE_RSSI
                onExit()

                //make it thread safe
                val localNotifyExit = notifyExit
                if (localNotifyExit != null) {
                    launch(CommonPool) {
                        localNotifyExit(this@XYBluetoothDevice)
                    }
                }
            }
        }
    }

    fun updateAds(record: XYScanRecord) {
        val buffer = ByteBuffer.wrap(record.bytes)
        while (buffer.hasRemaining()) {
            val ad = XYBleAd(buffer)
            ads[ad.hashCode()] = ad
        }
    }

    fun addListener(key: String, listener: Listener) {
        launch(CommonPool){
            logInfo("addListener")
            synchronized(listeners) {
                listeners.put(key, WeakReference(listener))
            }
        }
    }

    fun removeListener(key: String) {
        launch(CommonPool){
            logInfo("removeListener")
            synchronized(listeners) {
                listeners.remove(key)
            }
        }
    }

    //make a safe session to interact with the device
    //if null is passed back, the sdk was unable to create the safe session
    fun <T> access(closure: suspend ()->T?) : Deferred<T?> {
        return async(CommonPool) {
            logInfo("access")
            var result: T? = null
            references++
            if (connectGatt().await()) {
                val discovered = asyncDiscover().await()
                if (discovered == null) {
                    logError("access: Discover Failed!", false)
                } else {
                    result = closure()
                }
                cleanUpIfNeeded()
            }
            references--
            return@async result
        }
    }

    //the goal is to leave connections hanging for a little bit in the case
    //that they need to be reestablished in short notice
    private fun cleanUpIfNeeded() {
        launch(CommonPool) {
            logInfo("cleanUpIfNeeded")

            //if the global and local last access times do not match
            //after the delay, that means a newer access is now responsible for closing it
            val localAccessTime = now
            lastAccessTime = localAccessTime

            delay(CLEANUP_DELAY)

            //the goal is to close the connection if the ref count is
            //down to zero.  We have to check the lastAccess to make sure the delay is after
            //the last guy, not an earlier one

            if (!closed && references == 0 && lastAccessTime == localAccessTime) {
                asyncClose().await()
            }
        }
    }

    interface Listener {
        fun entered(device: XYBluetoothDevice)

        fun exited(device: XYBluetoothDevice)

        fun detected(device: XYBluetoothDevice)

        fun connectionStateChanged(device: XYBluetoothDevice, newState: Int)
    }

    companion object {
        //gap after last access that we wait to close the connection
        private const val CLEANUP_DELAY = 5000

        //the value we set the rssi to when we go out of range
        const val OUTOFRANGE_RSSI = -999

        //the period of time to wait for marking something as out of range
        //if we have not gotten any ads or been connected to it
        const val OUTOFRANGE_DELAY = 15000

        val router = Router()
        var canCreate = false
        val manufacturerToCreator = HashMap<Int, (context:Context, scanResult: XYScanResult) -> XYBluetoothDevice?>()

        private val manufacturerRouter : RouterInterface = object : RouterInterface {
            override fun run(context: Context, scanResult: XYScanResult): XYBluetoothDevice? {
                for ((manufacturerId, creator) in manufacturerToCreator) {
                    val bytes = scanResult.scanRecord?.getManufacturerSpecificData(manufacturerId)
                    if (bytes != null) {
                        val device = creator(context, scanResult)
                        if (device !=null) {
                            return device
                        }
                    }
                }
                return null
            }
        }

        fun fromScanResult(context:Context, scanResult: XYScanResult) : XYBluetoothDevice? {
            val device = router.run(context, scanResult)

            if (device != null) {
                return device
            }

            if (canCreate)
                return XYBluetoothDevice(context, scanResult.device)
            else
                return null
        }

        init {
            router.routers.add(manufacturerRouter)
        }
    }
}