package com.xyfindables.sdk.action

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic

import com.xyfindables.core.XYBase
import com.xyfindables.sdk.XYDevice
import com.xyfindables.sdk.XYDeviceCharacteristic
import com.xyfindables.sdk.XYDeviceService

import java.util.UUID

/**
 * Created by alex.mcelroy on 10/16/2017.
 */

abstract class XYDeviceActionSetColor(device: XYDevice, var value: ByteArray) : XYDeviceAction(device) {

    override val serviceId: UUID
        get() = XYDeviceService.XY4Primary

    override val characteristicId: UUID
        get() = XYDeviceCharacteristic.XY4PrimaryColor

    init {
        logAction(TAG, TAG)
    }

    override fun statusChanged(status: Int, gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, success: Boolean): Boolean {
        logExtreme(TAG, "statusChanged:$status:$success")
        var result = super.statusChanged(status, gatt, characteristic, success)
        when (status) {
            XYDeviceAction.STATUS_CHARACTERISTIC_FOUND -> {
                characteristic?.value = value
                if (gatt !== null) {
                    if (!gatt!!.writeCharacteristic(characteristic)) {
                        result = true
                    }
                }
            }
        }
        return result
    }

    companion object {
        private val TAG = XYDeviceActionSetColor::class.java.simpleName
    }
}