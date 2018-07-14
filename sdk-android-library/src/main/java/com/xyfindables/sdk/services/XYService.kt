package com.xyfindables.sdk.services

import android.bluetooth.BluetoothGattCharacteristic
import com.xyfindables.core.XYBase
import com.xyfindables.sdk.devices.XYBluetoothDevice
import kotlinx.coroutines.experimental.Deferred
import java.util.*

abstract class XYService(val device: XYBluetoothDevice) : XYBase() {

    abstract val uuid : UUID

    fun readInt(characteristic: UUID): Deferred<Int?> {
        return device.access {
            return@access device.asyncFindAndReadCharacteristicInt(
                    uuid,
                    characteristic,
                    BluetoothGattCharacteristic.FORMAT_UINT8,
                    0
            ).await()
        }
    }

    fun writeInt(characteristic: UUID, value: Int): Deferred<Boolean?> {
        return device.access {
            return@access device.asyncFindAndWriteCharacteristic(
                    uuid,
                    characteristic,
                    value,
                    BluetoothGattCharacteristic.FORMAT_UINT8,
                    0
            ).await()
        }
    }

    fun readBytes(characteristic: UUID): Deferred<ByteArray?> {
        return device.access {
            return@access device.asyncFindAndReadCharacteristicBytes(
                    uuid,
                    characteristic
            ).await()
        }
    }

    fun writeBytes(characteristic: UUID, bytes: ByteArray): Deferred<Boolean?> {
        return device.access {
            return@access device.asyncFindAndWriteCharacteristic(
                    uuid,
                    characteristic,
                    bytes
            ).await()
        }
    }
}