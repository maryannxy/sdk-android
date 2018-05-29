package com.xyfindables.sdk.action.dialog

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic

import com.xyfindables.sdk.XYDevice
import com.xyfindables.sdk.XYDeviceCharacteristic
import com.xyfindables.sdk.XYDeviceService
import com.xyfindables.sdk.action.XYDeviceAction

import java.util.UUID

/**
 * Created by alex.mcelroy on 11/15/2017.
 */

abstract class SetSpotaPatchData(private val _device: XYDevice, private val value: Array<ByteArray>) : XYDeviceAction(_device) {
    private var counter = 0

    override val serviceId: UUID
        get() = XYDeviceService.SPOTA_SERVICE_UUID

    override val characteristicId: UUID
        get() = XYDeviceCharacteristic.SPOTA_PATCH_DATA_UUID

    init {
        logAction(TAG, TAG)
    }

    override fun statusChanged(status: Int, gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, success: Boolean): Boolean {
        var result = super.statusChanged(status, gatt, characteristic, success)
        when (status) {
            XYDeviceAction.STATUS_CHARACTERISTIC_FOUND -> {
                //                if (Build.VERSION.SDK_INT >= 21) {
                //                    gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
                //                }
                characteristic.value = value[counter]
                //                Log.v(TAG, "testOta-write-hexValue = : " + counter + " : " + bytesToHex(value[counter]));
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                if (!gatt.writeCharacteristic(characteristic)) {
                    logError(TAG, "testOta-SetSpotaPatchData writeCharacteristic failed", false)
                    result = true
                } else {
                    result = false
                }
            }
            XYDeviceAction.STATUS_CHARACTERISTIC_WRITE -> {
                counter++
                if (counter < value.size) {
                    characteristic.value = value[counter]
                    //                    Log.v(TAG, "testOta-write-hexValue = : " + counter + " : " + bytesToHex(value[counter]));
                    characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    if (!gatt.writeCharacteristic(characteristic)) {
                        logError(TAG, "testOta-SetSpotaPatchData writeCharacteristic failed", false)
                        result = true
                    } else {
                        result = false
                    }
                }
            }
        }
        return result
    }

    companion object {
        private val TAG = SetSpotaPatchData::class.java.simpleName

        private val hexArray = "0123456789ABCDEF".toCharArray()

        private fun bytesToHex(bytes: ByteArray): String {
            val hexChars = CharArray(bytes.size * 2)
            for (j in bytes.indices) {
                val v = bytes[j] and 0xFF
                hexChars[j * 2] = hexArray[v.ushr(4)]
                hexChars[j * 2 + 1] = hexArray[v and 0x0F]
            }
            return String(hexChars)
        }
    }
}
