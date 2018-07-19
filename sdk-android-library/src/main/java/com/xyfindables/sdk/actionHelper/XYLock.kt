package com.xyfindables.sdk.actionHelper

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.xyfindables.sdk.XYDevice

import com.xyfindables.sdk.action.XYDeviceAction
import com.xyfindables.sdk.action.XYDeviceActionSetLock
import com.xyfindables.sdk.action.XYDeviceActionSetLockModern
import com.xyfindables.sdk.devices.XYFinderBluetoothDevice

/**
 * Created by alex.mcelroy on 9/6/2017.
 */

class XYLock(device: XYDevice, value: ByteArray, callback: XYActionHelper.Callback) : XYActionHelper() {

    init {
        if (device.family === XYFinderBluetoothDevice.Family.XY4) {
            action = object : XYDeviceActionSetLockModern(device, value) {
                override fun statusChanged(status: Int, gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, success: Boolean): Boolean {
                    logExtreme(TAG, "statusChanged:$status:$success")
                    val result = super.statusChanged(status, gatt, characteristic, success)
                    when (status) {
                        XYDeviceAction.STATUS_COMPLETED -> callback.completed(success)
                    }
                    return result
                }
            }
        } else {
            action = object : XYDeviceActionSetLock(device, value) {
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
    }

    companion object {
        private val TAG = XYLock::class.java.simpleName
    }
}
