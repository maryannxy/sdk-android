package com.xyfindables.sdk.actionHelper

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic

import com.xyfindables.sdk.XYDevice
import com.xyfindables.sdk.action.XYDeviceAction
import com.xyfindables.sdk.action.XYDeviceActionBuzzModernConfig

import java.util.Arrays

/**
 * Created by alex.mcelroy on 10/20/2017.
 */

class XYBeepConfig(device: XYDevice, slot: Int, song: ByteArray, callback: Callback) : XYActionHelper() {

    interface Callback : XYActionHelper.Callback {
        fun started(success: Boolean)
    }

    init {
        if (device.family === XYDevice.Family.XY4) {
            val slotArray = byteArrayOf(slot.toByte())
            val config = ByteArray(257)
            System.arraycopy(slotArray, 0, config, 0, 1)
            System.arraycopy(song, 0, config, 1, song.size)
            action = object : XYDeviceActionBuzzModernConfig(device, config) {
                override fun statusChanged(status: Int, gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, success: Boolean): Boolean {
                    logExtreme(TAG, "statusChanged:$status:$success")
                    val result = super.statusChanged(status, gatt, characteristic, success)
                    when (status) {
                        XYDeviceAction.STATUS_STARTED -> callback.started(success)
                        XYDeviceAction.STATUS_COMPLETED -> callback.completed(success)
                    }
                    return result
                }
            }
        }
    }

    companion object {
        private val TAG = XYBeep::class.java.simpleName
    }
}