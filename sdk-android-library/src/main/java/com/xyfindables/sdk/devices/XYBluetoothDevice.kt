package com.xyfindables.sdk.devices

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.ParcelUuid
import com.xyfindables.sdk.ads.XYBleAd
import com.xyfindables.sdk.gatt.XYBluetoothGatt
import com.xyfindables.sdk.scanner.XYScanRecord
import com.xyfindables.sdk.scanner.XYScanResult
import kotlinx.coroutines.experimental.*
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.ArrayList

open class XYBluetoothDevice (context: Context, device:BluetoothDevice, private val hash:Int) : XYBluetoothGatt(context, device, false, null, null, null, null) {

    //hash - the reason for the hash system is that some devices rotate MAC addresses or polymorph in other ways
    //the user generally wants to treat a single physical device as a single logical device so the
    //hash that is passed in to create the class is used to make sure that the reuse of existing instances
    //is done based on device specific logic on "sameness"

    private var references = 0

    private var _stayConnected = false
    var stayConnected : Boolean
        get() {
            return _stayConnected
        }
        set(value) {
            _stayConnected = value
            if (!_stayConnected) {
                cleanUpIfNeeded()
            }
        }

    protected val listeners = HashMap<String, WeakReference<Listener>>()
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

    open val address : String
        get() {
            return device.address
        }

    open val name: String?
        get() {
            return device.name
        }

    init {
        logInfo("XYBluetoothDevice Created: ${device.address}")
    }

    open var outOfRangeDelay = OUTOFRANGE_DELAY

    var notifyExit : ((device: XYBluetoothDevice)->(Unit))? = null

    override fun hashCode(): Int {
        return hash
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

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
        logInfo("onEnter: $address")
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
        logInfo("onExit: $address")
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
        logInfo("onConnectionStateChange: $hash : $newState")
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
    fun <T> connection(closure: suspend ()->T?) : Deferred<T?> {
        return async(CommonPool) {
            logInfo("connection")
            var result: T? = null
            references++
            if (connectGatt().await()) {
                val discovered = asyncDiscover().await()
                if (discovered == null) {
                    logError("connection: Discover Failed!", false)
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

            //if the global and local last connection times do not match
            //after the delay, that means a newer connection is now responsible for closing it
            val localAccessTime = now
            lastAccessTime = localAccessTime

            delay(CLEANUP_DELAY)

            //the goal is to close the connection if the ref count is
            //down to zero.  We have to check the lastAccess to make sure the delay is after
            //the last guy, not an earlier one

            if (!stayConnected && !closed && references == 0 && lastAccessTime == localAccessTime) {
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

    companion object : XYCreator() {

        //gap after last connection that we wait to close the connection
        private const val CLEANUP_DELAY = 5000

        //the value we set the rssi to when we go out of range
        const val OUTOFRANGE_RSSI = -999

        //the period of time to wait for marking something as out of range
        //if we have not gotten any ads or been connected to it
        const val OUTOFRANGE_DELAY = 15000

        var canCreate = false
        val manufacturerToCreator = HashMap<Int, XYCreator>()
        val serviceToCreator = HashMap<UUID, XYCreator>()

        private fun getDevicesFromManufacturers(context:Context, scanResult:XYScanResult, globalDevices: HashMap<Int, XYBluetoothDevice>, newDevices: HashMap<Int, XYBluetoothDevice>) {
            for ((manufacturerId, creator) in manufacturerToCreator) {
                val bytes = scanResult.scanRecord?.getManufacturerSpecificData(manufacturerId)
                if (bytes != null) {
                    creator.getDevicesFromScanResult(context, scanResult, globalDevices, newDevices)
                }
            }
        }

        private fun getDevicesFromServices(context:Context, scanResult:XYScanResult, globalDevices: HashMap<Int, XYBluetoothDevice>, newDevices: HashMap<Int, XYBluetoothDevice>) {
            for ((uuid, creator) in serviceToCreator) {
                val bytes = scanResult.scanRecord?.getServiceData(ParcelUuid(uuid))
                if (bytes != null) {
                    creator.getDevicesFromScanResult(context, scanResult, globalDevices, newDevices)
                }
            }
        }

        override fun getDevicesFromScanResult(context:Context, scanResult:XYScanResult, globalDevices: HashMap<Int, XYBluetoothDevice>, newDevices: HashMap<Int, XYBluetoothDevice>) {

            getDevicesFromServices(context, scanResult, globalDevices, newDevices)
            getDevicesFromManufacturers(context, scanResult, globalDevices, newDevices)

            if (newDevices.size == 0) {
                val hash = hashFromScanResult(scanResult)

                if (canCreate && hash != null) {
                    val createdDevice = XYBluetoothDevice(context, scanResult.device, hash)
                    newDevices[hash] = createdDevice
                    globalDevices[hash] = createdDevice
                }
            }
        }

        fun hashFromScanResult(scanResult: XYScanResult): Int? {
            return scanResult.address.hashCode()
        }

    }
}