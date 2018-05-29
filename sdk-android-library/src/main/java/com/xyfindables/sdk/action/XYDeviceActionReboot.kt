package com.xyfindables.sdk.action

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic

import com.xyfindables.sdk.XYDevice
import com.xyfindables.sdk.XYDeviceCharacteristic
import com.xyfindables.sdk.XYDeviceService

import java.util.UUID

/**
 * Created by alex.mcelroy on 6/12/2017.
 */

abstract class XYDeviceActionReboot(device: XYDevice, var value: Int) : XYDeviceAction(device) {

    override val serviceId: UUID
        get() = XYDeviceService.BasicConfig

    override val characteristicId: UUID
        get() = XYDeviceCharacteristic.BasicConfigReboot

    init {
        logAction(TAG, TAG)
    }

    override fun statusChanged(status: Int, gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, success: Boolean): Boolean {
        logExtreme(TAG, "statusChanged:$status:$success")
        var result = super.statusChanged(status, gatt, characteristic, success)
        when (status) {
            XYDeviceAction.STATUS_CHARACTERISTIC_FOUND -> {
                characteristic.setValue(value, BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                if (!gatt.writeCharacteristic(characteristic)) {
                    result = true
                }
                logExtreme(TAG, "testOta-rebootFound: $success")
            }
        }
        return result
    }

    companion object {

        private val TAG = XYDeviceActionReboot::class.java.simpleName
    }
}
