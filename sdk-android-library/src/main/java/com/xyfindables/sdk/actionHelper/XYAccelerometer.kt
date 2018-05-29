package com.xyfindables.sdk.actionHelper

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.xyfindables.sdk.XYDevice
import com.xyfindables.sdk.action.XYDeviceActionGetAccelerometerInactive
import com.xyfindables.sdk.action.XYDeviceActionGetAccelerometerMovementCount
import com.xyfindables.sdk.action.XYDeviceActionGetAccelerometerRaw
import com.xyfindables.sdk.action.XYDeviceActionGetAccelerometerThreshold
import com.xyfindables.sdk.action.XYDeviceActionGetAccelerometerTimeout

/**
 * Created by alex.mcelroy on 9/29/2017.
 */

class XYAccelerometer(var _device: XYDevice) : XYActionHelper() {

    interface Raw : XYActionHelper.Callback {
        fun read(success: Boolean, value: ByteArray)
    }

    interface Inactive : XYActionHelper.Callback {
        fun read(success: Boolean, value: Int)
    }

    interface MovementCount : XYActionHelper.Callback {
        fun read(success: Boolean, value: Int)
    }

    interface Threshold : XYActionHelper.Callback {
        fun read(success: Boolean, value: ByteArray)
    }

    interface Timeout : XYActionHelper.Callback {
        fun read(success: Boolean, value: ByteArray)
    }

    fun getRaw(callback: Raw) {
        action = object : XYDeviceActionGetAccelerometerRaw(_device) {
            override fun statusChanged(status: Int, gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, success: Boolean): Boolean {
                logExtreme(TAG, "statusChanged:$status:$success")
                val result = super.statusChanged(status, gatt, characteristic, success)
                when (status) {
                    XYDeviceAction.STATUS_CHARACTERISTIC_READ -> callback.read(success, value)
                    XYDeviceAction.STATUS_COMPLETED -> callback.completed(success)
                }
                return result
            }
        }
    }

    fun getInactive(callback: Inactive) {
        action = object : XYDeviceActionGetAccelerometerInactive(_device) {
            override fun statusChanged(status: Int, gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, success: Boolean): Boolean {
                logExtreme(TAG, "statusChanged:$status:$success")
                val result = super.statusChanged(status, gatt, characteristic, success)
                when (status) {
                    XYDeviceAction.STATUS_CHARACTERISTIC_READ -> callback.read(success, value)
                    XYDeviceAction.STATUS_COMPLETED -> callback.completed(success)
                }
                return result
            }
        }
    }

    fun getMovementCount(callback: MovementCount) {
        action = object : XYDeviceActionGetAccelerometerMovementCount(_device) {
            override fun statusChanged(status: Int, gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, success: Boolean): Boolean {
                logExtreme(TAG, "statusChanged:$status:$success")
                val result = super.statusChanged(status, gatt, characteristic, success)
                when (status) {
                    XYDeviceAction.STATUS_CHARACTERISTIC_READ -> callback.read(success, value)
                    XYDeviceAction.STATUS_COMPLETED -> callback.completed(success)
                }
                return result
            }
        }
    }

    fun getThreshold(callback: Threshold) {
        action = object : XYDeviceActionGetAccelerometerThreshold(_device) {
            override fun statusChanged(status: Int, gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, success: Boolean): Boolean {
                logExtreme(TAG, "statusChanged:$status:$success")
                val result = super.statusChanged(status, gatt, characteristic, success)
                when (status) {
                    XYDeviceAction.STATUS_CHARACTERISTIC_READ -> callback.read(success, value)
                    XYDeviceAction.STATUS_COMPLETED -> callback.completed(success)
                }
                return result
            }
        }
    }

    fun getTimeout(callback: Timeout) {
        action = object : XYDeviceActionGetAccelerometerTimeout(_device) {
            override fun statusChanged(status: Int, gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, success: Boolean): Boolean {
                logExtreme(TAG, "statusChanged:$status:$success")
                val result = super.statusChanged(status, gatt, characteristic, success)
                when (status) {
                    XYDeviceAction.STATUS_CHARACTERISTIC_READ -> callback.read(success, value)
                    XYDeviceAction.STATUS_COMPLETED -> callback.completed(success)
                }
                return result
            }
        }
    }

    companion object {

        private val TAG = XYAccelerometer::class.java.simpleName
    }
}
