package com.xyfindables.sdk.action

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.xyfindables.sdk.XYDevice

import com.xyfindables.sdk.gatt.XYDeviceCharacteristic
import com.xyfindables.sdk.gatt.XYDeviceService

import java.util.UUID

/**
 * Created by arietrouw on 1/2/17.
 */

abstract class XYDeviceActionSetRegistration(device: XYDevice, var value: Boolean) : XYDeviceAction(device) {

    override val serviceId: UUID
        get() = XYDeviceService.ExtendedConfig

    override val characteristicId: UUID
        get() = XYDeviceCharacteristic.ExtendedConfigRegistration

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

        private val TAG = XYDeviceActionSetRegistration::class.java.simpleName
    }
}