package com.xyfindables.sdk.action

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic

import com.xyfindables.sdk.XYDevice
import com.xyfindables.sdk.XYDeviceCharacteristic
import com.xyfindables.sdk.XYDeviceService

import java.util.UUID

/**
 * Created by alex.mcelroy on 7/21/2017.
 */

open class XYDeviceActionGetSelfTest(device: XYDevice) : XYDeviceAction(device) {

    var value: ByteArray

    override val serviceId: UUID
        get() = XYDeviceService.ExtendedControl

    override val characteristicId: UUID
        get() = XYDeviceCharacteristic.ExtendedControlSelfTest

    init {
        logAction(TAG, TAG)
    }

    override fun statusChanged(status: Int, gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, success: Boolean): Boolean {
        logExtreme(TAG, "statusChanged:$status:$success")
        var result = super.statusChanged(status, gatt, characteristic, success)
        when (status) {
            XYDeviceAction.STATUS_CHARACTERISTIC_READ -> value = characteristic.value
            XYDeviceAction.STATUS_CHARACTERISTIC_FOUND -> if (!gatt.readCharacteristic(characteristic)) {
                result = true
            }
        }
        return result
    }

    companion object {

        private val TAG = com.xyfindables.sdk.action.XYDeviceActionGetSelfTest::class.java.simpleName
    }
}