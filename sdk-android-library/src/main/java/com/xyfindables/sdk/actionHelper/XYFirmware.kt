package com.xyfindables.sdk.actionHelper

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.xyfindables.sdk.XYDevice
import com.xyfindables.sdk.action.XYDeviceActionGetVersion
import com.xyfindables.sdk.action.XYDeviceActionGetVersionModern

/**
 * Created by alex.mcelroy on 9/6/2017.
 */

class XYFirmware(device: XYDevice, callback: Callback) : XYActionHelper() {

    interface Callback : XYActionHelper.Callback {
        fun started(success: Boolean, value: String)
    }

    init {
        if (device.family === XYDevice.Family.XY4) {
            action = object : XYDeviceActionGetVersionModern(device) {
                override fun statusChanged(status: Int, gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, success: Boolean): Boolean {
                    logExtreme(TAG, "statusChanged:$status:$success")
                    val result = super.statusChanged(status, gatt, characteristic, success)
                    when (status) {
                        XYDeviceAction.STATUS_CHARACTERISTIC_READ -> callback.started(success, value)
                        XYDeviceAction.STATUS_COMPLETED -> callback.completed(success)
                    }
                    return result
                }
            }
        } else {
            action = object : XYDeviceActionGetVersion(device) {
                override fun statusChanged(status: Int, gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, success: Boolean): Boolean {
                    logExtreme(TAG, "statusChanged:$status:$success")
                    val result = super.statusChanged(status, gatt, characteristic, success)
                    when (status) {
                        XYDeviceAction.STATUS_CHARACTERISTIC_READ -> callback.started(success, value)
                        XYDeviceAction.STATUS_COMPLETED -> callback.completed(success)
                    }
                    return result
                }
            }
        }
    }

    companion object {

        private val TAG = XYFirmware::class.java.simpleName
    }
}
