package com.xyfindables.sdk.actionHelper

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.xyfindables.sdk.XYDevice
import com.xyfindables.sdk.action.XYDeviceActionGetSelfTest
import com.xyfindables.sdk.action.XYDeviceActionGetSelfTestModern
import com.xyfindables.sdk.action.XYDeviceActionSetSelfTest
import com.xyfindables.sdk.action.XYDeviceActionSetSelfTestModern

/**
 * Created by alex.mcelroy on 9/6/2017.
 */

class XYSelfTest : XYActionHelper {

    constructor(device: XYDevice, callback: XYActionHelper.Callback) {
        if (device.family === XYDevice.Family.XY4) {
            action = object : XYDeviceActionGetSelfTestModern(device) {
                override fun statusChanged(status: Int, gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, success: Boolean): Boolean {
                    logExtreme(TAG, "statusChanged:$status:$success")
                    val result = super.statusChanged(status, gatt, characteristic, success)
                    when (status) {
                        XYDeviceAction.STATUS_COMPLETED -> callback.completed(success)
                    }
                    return result
                }
            }
        } else {
            action = object : XYDeviceActionGetSelfTest(device) {
                override fun statusChanged(status: Int, gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, success: Boolean): Boolean {
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
        if (device.family === XYDevice.Family.XY4) {
            action = object : XYDeviceActionSetSelfTestModern(device, value) {
                override fun statusChanged(status: Int, gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, success: Boolean): Boolean {
                    logExtreme(TAG, "statusChanged:$status:$success")
                    val result = super.statusChanged(status, gatt, characteristic, success)
                    when (status) {
                        XYDeviceAction.STATUS_COMPLETED -> callback.completed(success)
                    }
                    return result
                }
            }
        } else {
            action = object : XYDeviceActionSetSelfTest(device, value) {
                override fun statusChanged(status: Int, gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, success: Boolean): Boolean {
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

        private val TAG = XYSelfTest::class.java.simpleName
    }
}
