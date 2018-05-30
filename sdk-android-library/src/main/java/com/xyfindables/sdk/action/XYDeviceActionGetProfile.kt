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

class XYDeviceActionGetProfile(device: XYDevice) : XYDeviceAction(device) {

    var value: Int = 0

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
            XYDeviceAction.STATUS_CHARACTERISTIC_READ -> {
                value = characteristic?.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)!!
            }
            XYDeviceAction.STATUS_CHARACTERISTIC_FOUND -> {
                if (gatt !== null) {
                    if (!gatt.readCharacteristic(characteristic)) {
                        result = true
                    }
                }
            }
        }
        return result
    }

    companion object {
        private val TAG = XYDeviceActionGetProfile::class.java.simpleName
    }
}
