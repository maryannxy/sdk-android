package com.xyfindables.sdk.action

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic

import com.xyfindables.sdk.XYDevice
import com.xyfindables.sdk.XYDeviceCharacteristic
import com.xyfindables.sdk.XYDeviceService

import java.util.Arrays
import java.util.UUID

/**
 * Created by alex.mcelroy on 10/20/2017.
 */

open class XYDeviceActionBuzzModernConfig protected constructor(device: XYDevice, private val value: ByteArray) : XYDeviceAction(device) {
    private var counter = 0

    override val serviceId: UUID
        get() = XYDeviceService.XY4Primary

    override val characteristicId: UUID
        get() = XYDeviceCharacteristic.XY4PrimaryBuzzerConfig

    init {
        logAction(TAG, TAG)
    }

    override fun statusChanged(status: Int, gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, success: Boolean): Boolean {
        logExtreme(TAG, "statusChanged:$status:$success")
        var result = super.statusChanged(status, gatt, characteristic, success)
        when (status) {
            XYDeviceAction.STATUS_CHARACTERISTIC_FOUND -> {
                logExtreme(TAG, "testSoundConfig: found: $success")
                var slotPlusOffset = byteArrayOf(value[0], 0.toByte())
                var slice = Arrays.copyOfRange(value, 1, 19)
                var packet = ByteArray(slotPlusOffset.size + slice.size)
                System.arraycopy(slotPlusOffset, 0, packet, 0, slotPlusOffset.size)
                System.arraycopy(slice, 0, packet, slotPlusOffset.size, slice.size)
                characteristic.value = packet
                if (!gatt.writeCharacteristic(characteristic)) {
                    result = true
                }
            }
            XYDeviceAction.STATUS_CHARACTERISTIC_WRITE -> {
                counter++
                logExtreme(TAG, "testSoundConfig: write: " + success + "counter: " + counter)
                slotPlusOffset = byteArrayOf(value[0], (counter * 9).toByte())
                slice = Arrays.copyOfRange(value, counter * 18 + 1, counter * 18 + 19)
                if (counter == 14) {
                    slice = Arrays.copyOfRange(value, 256, 257)
                }
                packet = ByteArray(slotPlusOffset.size + slice.size)
                System.arraycopy(slotPlusOffset, 0, packet, 0, slotPlusOffset.size)
                System.arraycopy(slice, 0, packet, slotPlusOffset.size, slice.size)
                characteristic.setValue(packet)

                result = counter == 14

                if (!gatt.writeCharacteristic(characteristic)) {
                    logError(TAG, "testSoundConfig-writeCharacteristic failed", false)
                    result = true
                }
            }
        }
        return result
    }

    companion object {
        private val TAG = XYDeviceActionBuzzModernConfig::class.java.simpleName
    }
}
