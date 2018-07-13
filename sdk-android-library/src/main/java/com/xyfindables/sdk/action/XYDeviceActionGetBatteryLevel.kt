package com.xyfindables.sdk.action

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic

import com.xyfindables.sdk.gatt.XYDeviceCharacteristic
import com.xyfindables.sdk.gatt.XYDeviceService
import com.xyfindables.sdk.XYDevice

import java.util.UUID

/**
 * Created by arietrouw on 1/2/17.
 */

abstract class XYDeviceActionGetBatteryLevel(device: XYDevice) : XYDeviceAction(device) {

    var value: Int = 0

    override val serviceId: UUID
        get() = XYDeviceService.BatteryStandard

    override val characteristicId: UUID
        get() = XYDeviceCharacteristic.BatteryLevel

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
                if (gatt != null) {
                    if (!gatt.readCharacteristic(characteristic)) {
                        result = true
                    }
                }
            }
        }
        return result
    }

    companion object {

        private val TAG = XYDeviceActionGetBatteryLevel::class.java.simpleName
    }

}