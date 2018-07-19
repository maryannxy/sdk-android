package com.xyfindables.sdk.actionHelper

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.xyfindables.sdk.XYDevice

import com.xyfindables.sdk.action.*
import com.xyfindables.sdk.devices.XYFinderBluetoothDevice

/**
 * Created by alex.mcelroy on 9/5/2017.
 */

class XYBeep : XYActionHelper {

    interface Callback : XYActionHelper.Callback {
        fun started(success: Boolean)
    }

    constructor(device: XYDevice, callback: Callback) {
        if (device.family === XYFinderBluetoothDevice.Family.XY4) {
            action = object : XYDeviceActionBuzzModern(device, value) {
                override fun statusChanged(status: Int, gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, success: Boolean): Boolean {
                    logExtreme(TAG, "statusChanged:$status:$success")
                    val result = super.statusChanged(status, gatt, characteristic, success)
                    when (status) {
                        XYDeviceAction.STATUS_STARTED -> callback.started(success)
                        XYDeviceAction.STATUS_COMPLETED -> callback.completed(success)
                    }
                    return result
                }
            }
        } else {
            action = object : XYDeviceActionBuzz(device) {
                override fun statusChanged(status: Int, gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, success: Boolean): Boolean {
                    logExtreme(TAG, "statusChanged:$status:$success")
                    val result = super.statusChanged(status, gatt, characteristic, success)
                    when (status) {
                        XYDeviceAction.STATUS_STARTED -> callback.started(success)
                        XYDeviceAction.STATUS_COMPLETED -> callback.completed(success)
                    }
                    return result
                }
            }
        }
    }

    constructor(device: XYDevice, index: Int, callback: Callback) {
        if (device.family === XYFinderBluetoothDevice.Family.XY4) {
            val value = byteArrayOf(index.toByte(), 3.toByte())
            action = object : XYDeviceActionBuzzSelectModern(device, value) {
                override fun statusChanged(status: Int, gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, success: Boolean): Boolean {
                    logExtreme(TAG, "statusChanged:$status:$success")
                    val result = super.statusChanged(status, gatt, characteristic, success)
                    when (status) {
                        XYDeviceAction.STATUS_STARTED -> callback.started(success)
                        XYDeviceAction.STATUS_COMPLETED -> callback.completed(success)
                    }
                    return result
                }
            }
        } else {
            action = object : XYDeviceActionBuzzSelect(device, index) {
                override fun statusChanged(status: Int, gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, success: Boolean): Boolean {
                    logExtreme(TAG, "statusChanged:$status:$success")
                    val result = super.statusChanged(status, gatt, characteristic, success)
                    when (status) {
                        XYDeviceAction.STATUS_STARTED -> callback.started(success)
                        XYDeviceAction.STATUS_COMPLETED -> callback.completed(success)
                    }
                    return result
                }
            }
        }
    }

    companion object {

        private val TAG = XYBeep::class.java.simpleName

        // verify values we should use for standard beep of xy4, also create custom variables containing different configurations
        protected val value = byteArrayOf(0x0b.toByte(), 0x03.toByte())
    }
}
