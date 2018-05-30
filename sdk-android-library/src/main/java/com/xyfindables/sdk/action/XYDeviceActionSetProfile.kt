package com.xyfindables.sdk.action

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic

import com.xyfindables.sdk.XYDevice
import com.xyfindables.sdk.XYDeviceCharacteristic
import com.xyfindables.sdk.XYDeviceService

import java.util.UUID

/**
 * Created by alex.mcelroy on 6/13/2017.
 */

class XYDeviceActionSetProfile(device: XYDevice, var value: Int) : XYDeviceAction(device) {

    override val serviceId: UUID
        get() = XYDeviceService.ExtendedControl

    override val characteristicId: UUID
        get() = XYDeviceCharacteristic.GpsProfile

    init {
        logAction(TAG, TAG)
    }

    override fun statusChanged(status: Int, gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, success: Boolean): Boolean {
        logExtreme(TAG, "statusChanged:$status:$success")
        var result = super.statusChanged(status, gatt, characteristic, success)
        when (status) {
            XYDeviceAction.STATUS_CHARACTERISTIC_FOUND -> {
                characteristic?.setValue(value, BluetoothGattCharacteristic.FORMAT_UINT16, 0)
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
        private val TAG = XYDeviceActionSetProfile::class.java.simpleName
    }
}
