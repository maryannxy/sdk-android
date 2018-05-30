package com.xyfindables.sdk.actionHelper

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.xyfindables.sdk.XYDevice
import com.xyfindables.sdk.action.XYDeviceAction
import com.xyfindables.sdk.action.XYDeviceActionGetGPSInterval
import com.xyfindables.sdk.action.XYDeviceActionSetGPSInterval

/**
 * Created by alex.mcelroy on 9/29/2017.
 */

class XYGPSInterval : XYActionHelper {

    interface Callback : XYActionHelper.Callback {
        fun started(success: Boolean, value: ByteArray)
    }

    constructor(device: XYDevice, callback: Callback) {
        action = object : XYDeviceActionGetGPSInterval(device) {
            override fun statusChanged(status: Int, gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, success: Boolean): Boolean {
                logExtreme(TAG, "statusChanged:$status:$success")
                val result = super.statusChanged(status, gatt, characteristic, success)
                when (status) {
                    XYDeviceAction.STATUS_CHARACTERISTIC_READ -> callback.started(success, value!!)
                    XYDeviceAction.STATUS_COMPLETED -> callback.completed(success)
                }
                return result
            }
        }
    }

    constructor(device: XYDevice, value: ByteArray, callback: Callback) {
        action = object : XYDeviceActionSetGPSInterval(device, value) {
            override fun statusChanged(status: Int, gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, success: Boolean): Boolean {
                logExtreme(TAG, "statusChanged:$status:$success")
                val result = super.statusChanged(status, gatt, characteristic, success)
                when (status) {
                    XYDeviceAction.STATUS_COMPLETED -> callback.completed(success)
                }
                return result
            }
        }
    }

    companion object {

        private val TAG = XYGPSInterval::class.java.simpleName
    }
}
