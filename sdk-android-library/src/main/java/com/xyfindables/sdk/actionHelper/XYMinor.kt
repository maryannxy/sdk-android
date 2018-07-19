package com.xyfindables.sdk.actionHelper

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.xyfindables.sdk.XYDevice

import com.xyfindables.sdk.action.*
import com.xyfindables.sdk.devices.XYFinderBluetoothDevice

/**
 * Created by alex.mcelroy on 9/6/2017.
 */

class XYMinor : XYActionHelper {

    constructor(device: XYDevice, callback: XYActionHelper.Callback) {
        if (device.family === XYFinderBluetoothDevice.Family.XY4) {
            action = object : XYDeviceActionGetMinorModern(device) {
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
            action = object : XYDeviceActionGetMinor(device) {
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

    constructor(device: XYDevice, value: Int, callback: XYActionHelper.Callback) {
        if (device.family === XYFinderBluetoothDevice.Family.XY4) {
            action = object : XYDeviceActionSetMinorModern(device, value) {
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
            action = object : XYDeviceActionSetMinor(device, value) {
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
        private val TAG = XYMinor::class.java.simpleName
    }
}
