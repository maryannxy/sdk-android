package com.xyfindables.sdk.devices

import android.bluetooth.BluetoothDevice
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

    private val CLEANUP_DELAY = 5000
    val OUTOFRANGE_RSSI = -999

    private var references = 0

    private val listeners = HashMap<String, WeakReference<Listener>>()
    val ads = HashMap<Int, XYBleAd>()

    var rssi = -999

    var detectCount = 0
    var enterCount = 0
    var exitCount = 0

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

    fun onEnter() {
        enterCount++
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
    }

    fun onExit() {
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
    }

    fun onDetect() {
        detectCount++
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

    fun getConnectedGatt(context: Context, timeout:Int?) : Deferred<XYBluetoothGatt?> {
        return async(CommonPool) {
            logInfo("getConnectedGatt")
            val gatt = XYBluetoothGatt.from(context, device, null).await()
            delay(100) //this is to prevent a 133 on some devices
            if (gatt.asyncConnect(timeout).await()) {
                return@async gatt
            } else {
                return@async null
            }
        }
    }

    //make a safe session to interact with the device
    //if null is passed back, the sdk was unable to create the safe session
    fun <T> access(closure: suspend ()->Deferred<T?>) : Deferred<T?> {
        return async<T?>(CommonPool) {
            logInfo("access")
            var result: T? = null
            references++
            if (connectGatt().await()) {
                if (asyncDiscover().await()) {
                    result = closure().await()
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
            delay(CLEANUP_DELAY)
            if (!closed && references == 0) {
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