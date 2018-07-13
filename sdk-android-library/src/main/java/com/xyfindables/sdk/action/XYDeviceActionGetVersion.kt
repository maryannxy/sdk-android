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

abstract class XYDeviceActionGetVersion(device: XYDevice) : XYDeviceAction(device) {

    var value: String? = null

    override val serviceId: UUID
        get() = XYDeviceService.Control

    override val characteristicId: UUID
        get() = XYDeviceCharacteristic.ControlVersion

    init {
        logAction(TAG, TAG)
    }

    override fun statusChanged(status: Int, gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, success: Boolean): Boolean {
        logExtreme(TAG, "statusChanged:$status:$success")
        var result = super.statusChanged(status, gatt, characteristic, success)
        when (status) {
            XYDeviceAction.STATUS_CHARACTERISTIC_READ -> {
                val versionBytes = characteristic!!.value
                if (versionBytes.size > 0) {
                    value = ""
                    for (b in versionBytes) {
                        value += String.format("%x", b)
                    }
                    val intValue = java.lang.Long.parseLong(value, 16)
                    value = intValue.toString()
                }
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

        private val TAG = XYDeviceActionGetVersion::class.java.simpleName
    }
}