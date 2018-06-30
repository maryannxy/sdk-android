package com.xyfindables.sdk.action

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic

import com.xyfindables.sdk.XYDevice
import com.xyfindables.sdk.XYDeviceCharacteristic
import com.xyfindables.sdk.XYDeviceService

import java.util.UUID

/**
 * Created by alex.mcelroy on 9/6/2017.
 */

abstract class XYDeviceActionSetRegistrationModern(device: XYDevice, var value: Boolean) : XYDeviceAction(device) {

    override val serviceId: UUID
        get() = XYDeviceService.XY4Primary

    override val characteristicId: UUID
        get() = XYDeviceCharacteristic.XY4PrimaryStayAwake

    init {
        logAction(TAG, TAG)
    }

    override fun statusChanged(status: Int, gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, success: Boolean): Boolean {
        logExtreme(TAG, "statusChanged:$status:$success")
        var result = super.statusChanged(status, gatt, characteristic, success)
        when (status) {
            XYDeviceAction.STATUS_CHARACTERISTIC_FOUND -> {
                if (value) {
                    characteristic?.setValue(0x01, BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                } else {
                    characteristic?.setValue(0x00, BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                }
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
        private val TAG = XYDeviceActionSetRegistrationModern::class.java.simpleName
    }
}