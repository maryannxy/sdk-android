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

    class IntegerCharacteristic(service: Service, uuid:UUID, val formatType: Int = BluetoothGattCharacteristic.FORMAT_UINT8, val offset:Int = 0) : Characteristic(service, uuid) {

        fun get() : Deferred<Int?> {
            return service.readInt(uuid, formatType, offset)
        }

        fun set(value: Int) : Deferred<Boolean?> {
            return service.writeInt(uuid, value, formatType, offset)
        }
    }

    class FloatCharacteristic(service: Service, uuid:UUID, val formatType: Int = BluetoothGattCharacteristic.FORMAT_FLOAT, val offset:Int = 0) : Characteristic(service, uuid) {

        fun get() : Deferred<Float?> {
            return service.readFloat(uuid, formatType, offset)
        }

        fun set(mantissa: Int, exponent: Int) : Deferred<Boolean?> {
            return service.writeFloat(uuid, mantissa, exponent, formatType, offset)
        }
    }

    class StringCharacteristic(service: Service, uuid:UUID) : Characteristic(service, uuid) {

        fun get() : Deferred<String?> {
            return service.readString(uuid)
        }

        fun set(value: String) : Deferred<Boolean?> {
            return service.writeString(uuid, value)
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

    private fun readInt(characteristic: UUID, formatType: Int = BluetoothGattCharacteristic.FORMAT_UINT8, offset:Int = 0): Deferred<Int?> {
        return device.connection {
            val result =  device.asyncFindAndReadCharacteristicInt(
                    serviceUuid,
                    characteristic,
                    formatType,
                    offset
            ).await()
            if (result == null) {
                logError("readInt: Returned Null", false)
            }
            return@connection result
        }
    }

    private fun writeInt(characteristic: UUID, value: Int, formatType: Int = BluetoothGattCharacteristic.FORMAT_UINT8, offset:Int = 0): Deferred<Boolean?> {
        return device.connection {
            val result = device.asyncFindAndWriteCharacteristic(
                    serviceUuid,
                    characteristic,
                    value,
                    formatType,
                    offset
            ).await()
            if (result == null) {
                logError("writeInt: Returned Null", false)
            } else if (result == false) {
                logError("writeInt failed", false)
            }
            return@connection result
        }
    }

    private fun readFloat(characteristic: UUID, formatType: Int = BluetoothGattCharacteristic.FORMAT_FLOAT, offset:Int = 0): Deferred<Float?> {
        return device.connection {
            return@connection device.asyncFindAndReadCharacteristicFloat(
                    serviceUuid,
                    characteristic,
                    formatType,
                    offset
            ).await()
        }
    }

    private fun writeFloat(characteristic: UUID, mantissa: Int, exponent: Int, formatType: Int = BluetoothGattCharacteristic.FORMAT_FLOAT, offset:Int = 0): Deferred<Boolean?> {
        return device.connection {
            return@connection device.asyncFindAndWriteCharacteristicFloat(
                    serviceUuid,
                    characteristic,
                    mantissa,
                    exponent,
                    formatType,
                    offset
            ).await()
        }
    }

    private fun readString(characteristic: UUID, offset:Int = 0): Deferred<String?> {
        return device.connection {
            return@connection device.asyncFindAndReadCharacteristicString(
                    serviceUuid,
                    characteristic,
                    offset
            ).await()
        }
    }

    private fun writeString(characteristic: UUID, value: String): Deferred<Boolean?> {
        return device.connection {
            return@connection device.asyncFindAndWriteCharacteristic(
                    serviceUuid,
                    characteristic,
                    value
            ).await()
        }
    }

    private fun enableNotify(characteristic: UUID, enabled: Boolean): Deferred<Boolean?> {
        return device.connection {
            return@connection device.asyncFindAndWriteCharacteristicNotify(
                    serviceUuid,
                    characteristic,
                    enabled
            ).await()
        }
    }

    private fun readBytes(characteristic: UUID): Deferred<ByteArray?> {
        return device.connection {
            return@connection device.asyncFindAndReadCharacteristicBytes(
                    serviceUuid,
                    characteristic
            ).await()
        }
    }

    private fun writeBytes(characteristic: UUID, bytes: ByteArray): Deferred<Boolean?> {
        return device.connection {
            return@connection device.asyncFindAndWriteCharacteristic(
                    serviceUuid,
                    characteristic,
                    bytes
            ).await()
        }
    }
}