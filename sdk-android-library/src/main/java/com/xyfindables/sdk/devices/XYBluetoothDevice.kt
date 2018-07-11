package com.xyfindables.sdk.devices

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.xyfindables.core.XYBase
import com.xyfindables.sdk.gatt.clients.XYBluetoothGatt
import com.xyfindables.sdk.scanner.XYScanResult
import kotlinx.coroutines.experimental.*
import java.util.*

open class XYBluetoothDevice (context: Context, device:BluetoothDevice) : XYBluetoothGatt(context, device, false, null, null, null, null) {

    private val CLEANUP_DELAY = 5000
    val OUTOFRANGE_RSSI = -999

    private var references = 0

    private val listeners = HashMap<String, Listener>()

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
                launch (CommonPool) {
                    listener.entered(this@XYBluetoothDevice)
                }
            }
        }
    }

    fun onExit() {
        exitCount++
        synchronized(listeners) {
            for ((_, listener) in listeners) {
                launch (CommonPool) {
                    listener.exited(this@XYBluetoothDevice)
                }
            }
        }
    }

    fun onDetect() {
        detectCount++
        synchronized(listeners) {
            for ((_, listener) in listeners) {
                launch (CommonPool) {
                    listener.detected(this@XYBluetoothDevice)
                }
            }
        }
    }

    fun onConnectionStateChange(newState: Int) {
        synchronized(listeners) {
            for ((_, listener) in listeners) {
                launch (CommonPool) {
                    listener.connectionStateChanged(this@XYBluetoothDevice, newState)
                }
            }
        }
    }

    interface Listener {
        fun entered(device: XYBluetoothDevice)

        fun exited(device: XYBluetoothDevice)

        fun detected(device: XYBluetoothDevice)

        fun connectionStateChanged(device: XYBluetoothDevice, newState: Int)
    }

    fun addListener(key: String, listener: Listener) {
        launch(CommonPool){
            logInfo("addListener")
            synchronized(listeners) {
                listeners.put(key, listener)
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
    fun access(closure: suspend ()->Deferred<Boolean>) : Deferred<Boolean> {
        return async(CommonPool) {
            logInfo("access")
            var result = false
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

    companion object {

        var canCreate = false

        val manufacturerToCreator = HashMap<Int, (context:Context, scanResult: XYScanResult) -> XYBluetoothDevice?>()
        fun fromScanResult(context:Context, scanResult: XYScanResult) : XYBluetoothDevice? {
            for ((manufacturerId, creator) in manufacturerToCreator) {
                val bytes = scanResult.scanRecord?.getManufacturerSpecificData(manufacturerId)
                if (bytes != null) {
                    val device = creator(context, scanResult)
                    if (device !=null) {
                        return device
                    }
                }
            }
            if (canCreate)
                return XYBluetoothDevice(context, scanResult.device)
            else
                return null
        }
    }

}