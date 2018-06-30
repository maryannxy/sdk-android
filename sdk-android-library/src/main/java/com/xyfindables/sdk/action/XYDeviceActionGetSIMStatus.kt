package com.xyfindables.sdk.action

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.xyfindables.sdk.XYDevice
import com.xyfindables.sdk.XYDeviceCharacteristic
import com.xyfindables.sdk.XYDeviceService

import java.util.UUID

/**
 * Created by arietrouw on 1/2/17.
 */

abstract class XYDeviceActionGetSIMStatus(device: XYDevice) : XYDeviceAction(device) {

    var value: ByteArray? = null

    override val serviceId: UUID
        get() = XYDeviceService.ExtendedControl

    override val characteristicId: UUID
        get() = XYDeviceCharacteristic.ExtendedControlSIMStatus

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
                    if (!gatt!!.readCharacteristic(characteristic)) {
                        logError(TAG, "connTest-Characteristic Read Failed", false)
                        result = true
                    }
                }
            }
        }
        return result
    }

    companion object {

        private val TAG = XYDeviceActionGetSIMStatus::class.java.simpleName
    }
}