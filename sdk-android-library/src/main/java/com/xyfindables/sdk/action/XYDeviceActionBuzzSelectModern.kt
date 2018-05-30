package com.xyfindables.sdk.action

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic

import com.xyfindables.sdk.XYDevice
import com.xyfindables.sdk.XYDeviceCharacteristic
import com.xyfindables.sdk.XYDeviceService

import java.util.UUID

/**
 * Created by alex.mcelroy on 9/28/2017.
 */

abstract class XYDeviceActionBuzzSelectModern protected constructor(device: XYDevice, private val _value: ByteArray) : XYDeviceAction(device) {

    override val serviceId: UUID
        get() = XYDeviceService.XY4Primary

    override val characteristicId: UUID
        get() = XYDeviceCharacteristic.XY4PrimaryBuzzer

    init {
        logAction(TAG, TAG)
    }

    override fun statusChanged(status: Int, gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, succes: Boolean): Boolean {
        logExtreme(TAG, "statusChanged:$status:$succes")
        var result = super.statusChanged(status, gatt, characteristic, succes)
        when (status) {
            XYDeviceAction.STATUS_CHARACTERISTIC_FOUND -> {
                characteristic?.value = _value
                if (gatt !== null) {
                    if (!gatt!!.writeCharacteristic(characteristic)) {
                        logError(TAG, "testSoundConfig-writeCharacteristic failed", false)
                        result = true
                    }
                }
            }
        }
        return result
    }

    companion object {
        private val TAG = XYDeviceActionBuzzSelectModern::class.java.simpleName
    }
}
