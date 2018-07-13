package com.xyfindables.sdk.action.dialog

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic

import com.xyfindables.sdk.XYDevice
import com.xyfindables.sdk.gatt.XYDeviceCharacteristic
import com.xyfindables.sdk.gatt.XYDeviceService
import com.xyfindables.sdk.action.XYDeviceAction

import java.util.UUID

/**
 * Created by alex.mcelroy on 11/15/2017.
 */

abstract class SetSpotaMemDev protected constructor(device: XYDevice, private val value: Int) : XYDeviceAction(device) {

    override val serviceId: UUID
        get() = XYDeviceService.SPOTA_SERVICE_UUID

    override val characteristicId: UUID
        get() = XYDeviceCharacteristic.SPOTA_MEM_DEV_UUID

    init {
        logAction(TAG, TAG)
    }

    override fun statusChanged(status: Int, gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, success: Boolean): Boolean {
        logExtreme(TAG, "statusChanged:$status:$success")
        var result = super.statusChanged(status, gatt, characteristic, success)
        when (status) {
            XYDeviceAction.STATUS_CHARACTERISTIC_FOUND -> {
                characteristic?.setValue(value, BluetoothGattCharacteristic.FORMAT_UINT32, 0)
                if (gatt !== null) {
                    if (!gatt.writeCharacteristic(characteristic)) {
                        logError(TAG, "testOta-SetSpotaMemDev write failed", false)
                        result = true
                    } else {
                        logExtreme(TAG, "testOta-SetSpotaMemDev write succeed")
                        result = false
                    }
                }
            }
        }
        return result
    }

    companion object {
        private val TAG = SetSpotaMemDev::class.java.simpleName
    }
}
