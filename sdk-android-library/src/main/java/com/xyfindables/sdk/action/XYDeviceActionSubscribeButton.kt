package com.xyfindables.sdk.action

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import com.xyfindables.sdk.XYDeviceCharacteristic
import com.xyfindables.sdk.XYDeviceService
import com.xyfindables.sdk.XYDevice

import java.util.UUID

/**
 * Created by arietrouw on 1/1/17.
 */

abstract class XYDeviceActionSubscribeButton(device: XYDevice) : XYDeviceAction(device) {

    private var _gatt: BluetoothGatt? = null
    private var _characteristic: BluetoothGattCharacteristic? = null

    override val serviceId: UUID
        get() = XYDeviceService.Control

    override val characteristicId: UUID
        get() = XYDeviceCharacteristic.ControlButton

    init {
        logAction(TAG, TAG)
    }

    fun stop() {
        if (_gatt != null && _characteristic != null) {
            _gatt!!.setCharacteristicNotification(_characteristic, false)
            _gatt = null
            _characteristic = null
        } else {
            logError(TAG, "connTest-Stopping non-started button notifications", false)
        }
    }

    override fun statusChanged(status: Int, gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, success: Boolean): Boolean {
        logExtreme(TAG, "statusChanged:$status:$success")
        var result = super.statusChanged(status, gatt, characteristic, success)
        when (status) {
            XYDeviceAction.STATUS_CHARACTERISTIC_UPDATED -> {
                logInfo(TAG, "statusChanged:Updated:" + characteristic?.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)!!)
                result = false
            }
            XYDeviceAction.STATUS_CHARACTERISTIC_FOUND -> {
                logInfo(TAG, "statusChanged:Characteristic Found")
                if (gatt !== null) {
                    if (!gatt.setCharacteristicNotification(characteristic, true)) {
                        logError(TAG, "connTest-Characteristic Notification Failed", false)
                    } else {
                        _gatt = gatt
                        _characteristic = characteristic
                    }

                    val descriptor = characteristic?.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
                    descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    if (!gatt.writeDescriptor(descriptor)) {
                        result = true
                    }
                }
            }
        }
        return result
    }

    companion object {

        private val TAG = XYDeviceActionSubscribeButton::class.java.simpleName

        val CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        val BUTTONPRESS_SINGLE = 1
        val BUTTONPRESS_DOUBLE = 2
        val BUTTONPRESS_LONG = 3
    }
}