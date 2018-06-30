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

abstract class XYDeviceActionGetHardwareCreateDate(device: XYDevice) : XYDeviceAction(device) {

    var value: ByteArray? = null

    override val serviceId: UUID
        get() = XYDeviceService.XY4Primary

    override val characteristicId: UUID
        get() = XYDeviceCharacteristic.XY4PrimaryHardwareCreateDate

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
                if (!gatt!!.readCharacteristic(characteristic)) {
                    logError(TAG, "connTest-Characteristic Read Failed", false)
                    result = true
                }
            }
        }
        return result
    }

    companion object {
        private val TAG = XYDeviceActionGetHardwareCreateDate::class.java.simpleName
    }
}
