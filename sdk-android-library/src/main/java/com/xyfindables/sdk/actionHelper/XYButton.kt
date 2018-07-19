package com.xyfindables.sdk.actionHelper

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.util.Log
import com.xyfindables.sdk.XYDevice

import com.xyfindables.sdk.action.*
import com.xyfindables.sdk.devices.XYFinderBluetoothDevice

//import static com.xyfindables.core.XYBase.logError;

/**
 * Created by alex.mcelroy on 9/6/2017.
 */

class XYButton : XYActionHelper {

    private var _gatt: BluetoothGatt? = null
    private var _characteristic: BluetoothGattCharacteristic? = null

    interface Callback : XYActionHelper.Callback {
        fun started(success: Boolean, value: Int)
    }

    protected constructor(device: XYDevice, callback: Callback) {
        if (device.family === XYFinderBluetoothDevice.Family.XY4) {
            action = object : XYDeviceActionGetButtonStateModern(device) {
                override fun statusChanged(status: Int, gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, success: Boolean): Boolean {
                    //logExtreme(TAG, "statusChanged:" + status + ":" + success);
                    val result = super.statusChanged(status, gatt, characteristic, success)
                    when (status) {
                        XYDeviceAction.STATUS_CHARACTERISTIC_FOUND -> callback.started(success, value)
                        XYDeviceAction.STATUS_COMPLETED -> callback.completed(success)
                    }
                    return result
                }
            }
        } else {
            action = object : XYDeviceActionGetButtonState(device) {
                override fun statusChanged(status: Int, gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, success: Boolean): Boolean {
                    //logExtreme(TAG, "statusChanged:" + status + ":" + success);
                    val result = super.statusChanged(status, gatt, characteristic, success)
                    when (status) {
                        XYDeviceAction.STATUS_CHARACTERISTIC_FOUND -> callback.started(success, value)
                        XYDeviceAction.STATUS_COMPLETED -> callback.completed(success)
                    }
                    return result
                }
            }
        }
    }

    protected constructor(device: XYDevice, notification: XYActionHelper.Notification) {
        if (device.family === XYFinderBluetoothDevice.Family.XY4) {
            action = object : XYDeviceActionSubscribeButtonModern(device) {
                override fun statusChanged(status: Int, gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, success: Boolean): Boolean {
                    //logExtreme(TAG, "statusChanged:" + status + ":" + success);
                    val result = super.statusChanged(status, gatt, characteristic, success)
                    when (status) {
                        XYDeviceAction.STATUS_CHARACTERISTIC_UPDATED -> {
                            notification.updated(success)
                        }
                    }
                    return result
                }
            }
        } else {
            action = object : XYDeviceActionSubscribeButton(device) {
                override fun statusChanged(status: Int, gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, success: Boolean): Boolean {
                    //logExtreme(TAG, "statusChanged:" + status + ":" + success);
                    val result = super.statusChanged(status, gatt, characteristic, success)
                    when (status) {
                        XYDeviceAction.STATUS_CHARACTERISTIC_UPDATED -> {
                            notification.updated(success)
                        }
                    }
                    return result
                }
            }
        }
    }

    protected fun stop() {
        if (_gatt != null && _characteristic != null) {
            _gatt!!.setCharacteristicNotification(_characteristic, false)
            _gatt = null
            _characteristic = null
        } else {
            Log.e(TAG, "connTest-Stopping non-started button notifications", IllegalArgumentException())
        }
    }

    companion object {

        private val TAG = XYBeep::class.java.simpleName
    }
}
