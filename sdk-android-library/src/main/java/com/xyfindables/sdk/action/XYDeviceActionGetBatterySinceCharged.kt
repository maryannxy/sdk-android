package com.xyfindables.sdk.action

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.xyfindables.sdk.XYDevice

import com.xyfindables.sdk.gatt.XYDeviceCharacteristic
import com.xyfindables.sdk.gatt.XYDeviceService

import java.util.UUID

/**
 * Created by alex.mcelroy on 5/16/2017.
 */

abstract class XYDeviceActionGetBatterySinceCharged(device: XYDevice) : XYDeviceAction(device) {

    var value: ByteArray? = null

    override val serviceId: UUID
        get() = XYDeviceService.BatteryStandard

    override val characteristicId: UUID
        get() = XYDeviceCharacteristic.BatterySinceCharged

    init {
        logAction(TAG, TAG)
    }

    override fun statusChanged(status: Int, gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, success: Boolean): Boolean {
        logExtreme(TAG, "statusChanged:$status:$success")
        var result = super.statusChanged(status, gatt, characteristic, success)
        when (status) {
            XYDeviceAction.STATUS_CHARACTERISTIC_READ -> {
                value = characteristic?.value
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

        private val TAG = XYDeviceActionGetBatterySinceCharged::class.java.simpleName
    }
}
