package com.xyfindables.sdk.action

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.xyfindables.sdk.XYDevice
import com.xyfindables.sdk.gatt.XYDeviceCharacteristic
import com.xyfindables.sdk.gatt.XYDeviceService

import java.util.UUID

/**
 * Created by alex.mcelroy on 6/12/2017.
 */

abstract class XYDeviceActionOtaWrite(private val _device: XYDevice, var value: Array<ByteArray>) : XYDeviceAction(_device) {
    private var counter = 0

    override val serviceId: UUID
        get() = XYDeviceService.BasicConfig

    override val characteristicId: UUID
        get() = XYDeviceCharacteristic.BasicConfigOtaWrite

    init {
        logAction(TAG, TAG)
    }

    override fun statusChanged(status: Int, gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, success: Boolean): Boolean {

        logExtreme(TAG, "testOta-statusChanged:$status:$success")
        var result = super.statusChanged(status, gatt, characteristic, success)
        when (status) {
            XYDeviceAction.STATUS_CHARACTERISTIC_FOUND -> {
                _device.otaMode(true)
                logExtreme(TAG, "testOta-found: " + counter + " : " + success + ": length: " + value.size)
                characteristic?.value = value[counter]
                gatt!!.writeCharacteristic(characteristic)
                logExtreme(TAG, "testOta-value = " + bytesToHex(value[counter]))
            }
            XYDeviceAction.STATUS_CHARACTERISTIC_WRITE -> {
                counter++
                if (counter < value.size) {
                    logExtreme(TAG, "testOta-write: $counter : $success")
                    characteristic?.value = value[counter]
                    gatt!!.writeCharacteristic(characteristic)
                    logExtreme(TAG, "testOta-value = " + bytesToHex(value[counter]))
                    result = false
                } else {
                    logExtreme(TAG, "testOta-write-FINISHED: $success: otaMode set to false")
                    _device.otaMode(false)
                    result = true
                }
            }
        }
        return result
    }

    companion object {

        private val TAG = XYDeviceActionOtaWrite::class.java.simpleName

        private val hexArray = "0123456789ABCDEF".toCharArray()

        private fun bytesToHex(bytes: ByteArray): String {
            val hexChars = CharArray(bytes.size * 2)
            for (j in bytes.indices) {
                val v = bytes[j].toInt() and 0xFF
                hexChars[j * 2] = hexArray[v.ushr(4)]
                hexChars[j * 2 + 1] = hexArray[v and 0x0F]
            }
            return String(hexChars)
        }
    }
}
