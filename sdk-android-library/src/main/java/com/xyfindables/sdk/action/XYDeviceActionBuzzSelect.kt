package com.xyfindables.sdk.action

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic

import com.xyfindables.sdk.gatt.XYDeviceCharacteristic
import com.xyfindables.sdk.gatt.XYDeviceService
import com.xyfindables.sdk.XYDevice

import java.util.UUID

/**
 * Created by arietrouw on 1/1/17.
 */

abstract class XYDeviceActionBuzzSelect protected constructor(device: XYDevice, private val _index: Int) : XYDeviceAction(device) {

    override val serviceId: UUID
        get() = XYDeviceService.Control

    override val characteristicId: UUID
        get() = XYDeviceCharacteristic.ControlBuzzerSelect

    init {
        logExtreme(TAG, TAG)
    }

    override fun statusChanged(status: Int, gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, success: Boolean): Boolean {
        logExtreme(TAG, "statusChanged:$status:$success")
        var result = super.statusChanged(status, gatt, characteristic, success)
        when (status) {
            XYDeviceAction.STATUS_CHARACTERISTIC_FOUND -> {
                characteristic?.setValue(_index, BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                if (gatt !== null) {
                    if (!gatt.writeCharacteristic(characteristic)) {
                        result = true
                    }
                }
            }
        }
        return result
    }

    companion object {

        private val TAG = XYDeviceActionBuzzSelect::class.java.simpleName
    }
}
