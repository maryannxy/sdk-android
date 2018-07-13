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

abstract class SetSpotaPatchLen(device: XYDevice, internal var value: Int) : XYDeviceAction(device) {

    override val serviceId: UUID
        get() = XYDeviceService.SPOTA_SERVICE_UUID

    override val characteristicId: UUID
        get() = XYDeviceCharacteristic.SPOTA_PATCH_LEN_UUID

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
                        logError(TAG, "testOta-SetSpotaPatchLen write failed", false)
                        result = true
                    }
                }
            }
        }
        return result
    }

    companion object {
        private val TAG = SetSpotaPatchLen::class.java.simpleName
    }
}
