package com.xyfindables.sdk

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.content.Context
import com.xyfindables.core.XYBase
import com.xyfindables.sdk.scanner.XYScanResult
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.produce
import java.util.*
import java.util.concurrent.TimeoutException

open class XYBluetoothDevice (val context: Context, val device:BluetoothDevice) : XYBase() {

    private val CLEANUP_DELAY = 1000
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
            synchronized(listeners) {
                listeners.put(key, listener)
            }
        }
    }

    fun removeListener(key: String) {
        launch(CommonPool){
            synchronized(listeners) {
                listeners.remove(key)
            }
        }
    }

    private fun getConnectedGatt(context: Context, timeout:Int = 100000) : Deferred<XYBluetoothGatt?> {
        logInfo("getConnectedGatt")
        return async(CommonPool) {
            logInfo("getConnectedGatt: async")
            val gatt = XYBluetoothGatt.from(context, device, null).await()
            if (gatt.asyncConnect(timeout).await()) {
                return@async gatt
            } else {
                return@async null
            }
        }
    }

    //make a safe session to interact with the device
    //if null is passed back, the sdk was unable to create the safe session
    fun access(context: Context, closure:suspend (gatt: XYBluetoothGatt?)->Unit) {
        logInfo("access")
        launch(CommonPool) {
            logInfo("access:async")
            references++
            val connectedGatt = getConnectedGatt(context).await()
            if (connectedGatt != null) {
                if (!connectedGatt.asyncDiscover().await()) {
                    closure(null)
                } else {
                    closure(connectedGatt)
                }
                cleanUpIfNeeded(connectedGatt)
            } else {
                closure(null)
            }
            references--
        }
    }

    //the goal is to leave connections hanging for a little bit in the case
    //that they need to be reestablished in short notice
    private fun cleanUpIfNeeded(gatt: XYBluetoothGatt) {
        launch(CommonPool) {
            delay(CLEANUP_DELAY)
            if (!gatt.closed && references == 0) {
                gatt.asyncClose().await()
            }
        }
    }

    companion object {

        val manufacturerToCreator = HashMap<Int, (context:Context, scanResult: XYScanResult) -> XYBluetoothDevice?>()
        fun fromScanResult(context:Context, scanResult: XYScanResult) : XYBluetoothDevice {
            for ((manufacturerId, creator) in manufacturerToCreator) {
                val bytes = scanResult.scanRecord?.getManufacturerSpecificData(manufacturerId)
                if (bytes != null) {
                    val device = creator(context, scanResult)
                    if (device !=null) {
                        return device
                    }
                }
            }
            return XYBluetoothDevice(context, scanResult.device)
        }
    }

}