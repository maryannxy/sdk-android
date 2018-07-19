package com.xyfindables.sdk.actionHelper

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic

import com.xyfindables.sdk.XYDevice
import com.xyfindables.sdk.action.XYDeviceAction
import com.xyfindables.sdk.action.XYDeviceActionUnlock
import com.xyfindables.sdk.action.XYDeviceActionUnlockModern
import com.xyfindables.sdk.devices.XYFinderBluetoothDevice

/**
 * Created by alex.mcelroy on 9/6/2017.
 */

class XYUnlock(device: XYDevice, value: ByteArray, callback: Callback) : XYActionHelper() {

    interface Callback : XYActionHelper.Callback

    init {
        if (device.family === XYFinderBluetoothDevice.Family.XY4) {
            action = object : XYDeviceActionUnlockModern(device, _defaultXy4UnlockCode) {
                override fun statusChanged(status: Int, gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, success: Boolean): Boolean {
                    logExtreme(TAG, "statusChanged:$status:$success")
                    val result = super.statusChanged(status, gatt, characteristic, success)
                    when (status) {
                        XYDeviceAction.STATUS_COMPLETED -> callback.completed(success)
                    }
                    return result
                }
            }
        } else {
            action = object : XYDeviceActionUnlock(device, value) {
                override fun statusChanged(status: Int, gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, success: Boolean): Boolean {
                    logExtreme(TAG, "statusChanged:$status:$success")
                    val result = super.statusChanged(status, gatt, characteristic, success)
                    when (status) {
                        XYDeviceAction.STATUS_COMPLETED -> callback.completed(success)
                    }
                    return result
                }
            }
        }
    }

    companion object {

        private val TAG = XYUnlock::class.java.simpleName

        val _defaultUnlockCode = byteArrayOf(0x2f.toByte(), 0xbe.toByte(), 0xa2.toByte(), 0x07.toByte(), 0x52.toByte(), 0xfe.toByte(), 0xbf.toByte(), 0x31.toByte(), 0x1d.toByte(), 0xac.toByte(), 0x5d.toByte(), 0xfa.toByte(), 0x7d.toByte(), 0x77.toByte(), 0x76.toByte(), 0x80.toByte())
        val _defaultXy4UnlockCode = byteArrayOf(0x00.toByte(), 0x01.toByte(), 0x02.toByte(), 0x03.toByte(), 0x04.toByte(), 0x05.toByte(), 0x06.toByte(), 0x07.toByte(), 0x08.toByte(), 0x09.toByte(), 0x0a.toByte(), 0x0b.toByte(), 0x0c.toByte(), 0x0d.toByte(), 0x0e.toByte(), 0x0f.toByte())
    }
}
