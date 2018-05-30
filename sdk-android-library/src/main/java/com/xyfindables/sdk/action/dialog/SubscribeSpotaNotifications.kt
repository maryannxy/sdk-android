package com.xyfindables.sdk.action.dialog

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import com.xyfindables.sdk.XYDevice
import com.xyfindables.sdk.XYDeviceCharacteristic
import com.xyfindables.sdk.XYDeviceService
import com.xyfindables.sdk.action.XYDeviceAction

import java.util.UUID

/**
 * Created by alex.mcelroy on 11/22/2017.
 */

abstract class SubscribeSpotaNotifications(device: XYDevice) : XYDeviceAction(device) {

    private var _gatt: BluetoothGatt? = null
    private var _characteristic: BluetoothGattCharacteristic? = null

    override val serviceId: UUID
        get() = XYDeviceService.SPOTA_SERVICE_UUID

    override val characteristicId: UUID
        get() = XYDeviceCharacteristic.SPOTA_SERV_STATUS_UUID

    init {
        logAction(TAG, TAG)
    }

    fun stop() {
        if (_gatt != null && _characteristic != null) {
            _gatt!!.setCharacteristicNotification(_characteristic, false)
            _gatt = null
            _characteristic = null
        } else {
            logError(TAG, "connTest-Stopping non-started notifications", false)
        }
    }

    override fun statusChanged(status: Int, gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, success: Boolean): Boolean {
        logExtreme(TAG, "statusChanged:$status:$success")
        val result = super.statusChanged(status, gatt, characteristic, success)
        when (status) {
            XYDeviceAction.STATUS_CHARACTERISTIC_UPDATED -> {
                //                Log.i(TAG, "testOta-statusChanged:Updated:" + characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
                return true
            }
            XYDeviceAction.STATUS_CHARACTERISTIC_FOUND -> {
                logExtreme(TAG, "testOta-subscribeSpotaNotifications:statusChanged:Characteristic Found")
                if (gatt !== null) {
                    if (!gatt.setCharacteristicNotification(characteristic, true)) {
                        logError(TAG, "testOta-notifications-Characteristic Notification Failed", false)
                        return true
                    } else {
                        logExtreme(TAG, "testOta-notifications-Characteristic Notification Succeeded")
                        _gatt = gatt
                        _characteristic = characteristic
                    }
                    val descriptor = characteristic?.getDescriptor(XYDeviceCharacteristic.SPOTA_DESCRIPTOR_UUID)
                    descriptor?.value = byteArrayOf(0x01, 0x00)
                    if (!gatt.writeDescriptor(descriptor)) {
                        logError(TAG, "testOta-notifications-Write Descriptor failed", false)
                        return true
                    } else {
                        logExtreme(TAG, "testOta-notifications-Write Descriptor succeeded")
                    }
                }
                return false
            }
        }
        return result
    }

    companion object {
        private val TAG = SubscribeSpotaNotifications::class.java.simpleName
    }
}
