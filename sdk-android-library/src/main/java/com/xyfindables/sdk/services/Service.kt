package com.xyfindables.sdk.services

import android.bluetooth.BluetoothGattCharacteristic
import com.xyfindables.core.XYBase
import com.xyfindables.sdk.devices.XYBluetoothDevice
import kotlinx.coroutines.experimental.Deferred
import java.util.*

abstract class Service(val device: XYBluetoothDevice) : XYBase() {

    abstract val serviceUuid : UUID

    open class Characteristic(val service: Service, val uuid:UUID) {
        fun enableNotify(enable: Boolean) : Deferred<Boolean?> {
            return service.enableNotify(uuid, enable)
        }
    }

    class IntegerCharacteristic(service: Service, uuid:UUID) : Characteristic(service, uuid) {

        fun get() : Deferred<Int?> {
            return service.readInt(uuid)
        }

        fun set(value: Int) : Deferred<Boolean?> {
            return service.writeInt(uuid, value)
        }
    }

    class BytesCharacteristic(service: Service, uuid:UUID) : Characteristic(service, uuid) {
        fun get() : Deferred<ByteArray?> {
            return service.readBytes(uuid)
        }

        fun set(value: ByteArray) : Deferred<Boolean?> {
            return service.writeBytes(uuid, value)
        }
    }

    private fun readInt(characteristic: UUID): Deferred<Int?> {
        return device.access {
            return@access device.asyncFindAndReadCharacteristicInt(
                    serviceUuid,
                    characteristic,
                    BluetoothGattCharacteristic.FORMAT_UINT8,
                    0
            ).await()
        }
    }

    private fun writeInt(characteristic: UUID, value: Int): Deferred<Boolean?> {
        return device.access {
            return@access device.asyncFindAndWriteCharacteristic(
                    serviceUuid,
                    characteristic,
                    value,
                    BluetoothGattCharacteristic.FORMAT_UINT8,
                    0
            ).await()
        }
    }

    private fun enableNotify(characteristic: UUID, enabled: Boolean): Deferred<Boolean?> {
        return device.access {
            return@access device.asyncFindAndWriteCharacteristicNotify(
                    serviceUuid,
                    characteristic,
                    enabled
            ).await()
        }
    }

    private fun readBytes(characteristic: UUID): Deferred<ByteArray?> {
        return device.access {
            return@access device.asyncFindAndReadCharacteristicBytes(
                    serviceUuid,
                    characteristic
            ).await()
        }
    }

    private fun writeBytes(characteristic: UUID, bytes: ByteArray): Deferred<Boolean?> {
        return device.access {
            return@access device.asyncFindAndWriteCharacteristic(
                    serviceUuid,
                    characteristic,
                    bytes
            ).await()
        }
    }
}