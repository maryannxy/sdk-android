package com.xyfindables.sdk

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.content.Context
import com.xyfindables.core.XYBase
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.produce
import java.util.concurrent.TimeoutException

class XYBluetoothDevice (val context: Context, val device:BluetoothDevice) : XYBase() {

    private val CLEANUP_DELAY = 10000
    private var references = 0

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

}