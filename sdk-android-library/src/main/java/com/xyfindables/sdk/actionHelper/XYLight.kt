package com.xyfindables.sdk.actionHelper

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.xyfindables.sdk.XYDevice
import com.xyfindables.sdk.action.XYDeviceAction
import com.xyfindables.sdk.action.XYDeviceActionGetLED
import com.xyfindables.sdk.action.XYDeviceActionSetLED

/**
 * Created by alex.mcelroy on 9/29/2017.
 */

class XYLight : XYActionHelper {

    interface Callback : XYActionHelper.Callback {
        fun started(success: Boolean, value: ByteArray)
    }

    constructor(device: XYDevice, callback: Callback) {
        action = object : XYDeviceActionGetLED(device) {
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

    constructor(device: XYDevice, value: Int, callback: Callback) {
        action = object : XYDeviceActionSetLED(device, value) {
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

        private val TAG = XYLight::class.java.simpleName
    }
}
