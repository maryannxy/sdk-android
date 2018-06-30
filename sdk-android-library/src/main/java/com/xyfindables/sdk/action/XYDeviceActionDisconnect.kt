package com.xyfindables.sdk.action

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic

import com.xyfindables.sdk.XYDeviceCharacteristic
import com.xyfindables.sdk.XYDeviceService
import com.xyfindables.sdk.XYDevice

import java.util.UUID

/**
 * Created by arietrouw on 1/2/17.
 */

abstract class XYDeviceActionDisconnect(device: XYDevice) : XYDeviceAction(device) {

    override val serviceId: UUID
        get() = XYDeviceService.Control

    override val characteristicId: UUID
        get() = XYDeviceCharacteristic.ControlDisconnect

    init {
        logAction(TAG, TAG)
    }

    override fun statusChanged(status: Int, gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, success: Boolean): Boolean {
        logExtreme(TAG, "statusChanged:$status:$success")
        var result = super.statusChanged(status, gatt, characteristic, success)
        when (status) {
            XYDeviceAction.STATUS_CHARACTERISTIC_FOUND -> {
                characteristic?.setValue(1, BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                if (!gatt!!.writeCharacteristic(characteristic)) {
                    result = true
                }
            }
        }
        return result
    }

    companion object {

        private val TAG = XYDeviceActionDisconnect::class.java.simpleName
    }
}