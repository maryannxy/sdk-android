package com.xyfindables.sdk.services

import android.bluetooth.BluetoothGattCharacteristic
import com.xyfindables.core.XYBase
import com.xyfindables.sdk.devices.XYBluetoothDevice
import com.xyfindables.sdk.gatt.XYBluetoothResult
import kotlinx.coroutines.experimental.Deferred
import java.util.*

abstract class Service(val device: XYBluetoothDevice) : XYBase() {

    abstract val serviceUuid : UUID

    open class Characteristic(val service: Service, val uuid:UUID) : XYBase() {
        fun enableNotify(enable: Boolean) : Deferred<XYBluetoothResult<Boolean>> {
            return service.enableNotify(uuid, enable)
        }
    }

    class IntegerCharacteristic(service: Service, uuid:UUID, val formatType: Int = BluetoothGattCharacteristic.FORMAT_UINT8, val offset:Int = 0) : Characteristic(service, uuid) {

        fun get() : Deferred<XYBluetoothResult<Int>> {
            return service.readInt(uuid, formatType, offset)
        }

        fun set(value: Int) : Deferred<XYBluetoothResult<Int>> {
            logInfo("IntegerCharacteristic: Set")
            return service.writeInt(uuid, value, formatType, offset)
        }
    }

    class FloatCharacteristic(service: Service, uuid:UUID, val formatType: Int = BluetoothGattCharacteristic.FORMAT_FLOAT, val offset:Int = 0) : Characteristic(service, uuid) {

        fun get() : Deferred<XYBluetoothResult<Float>> {
            return service.readFloat(uuid, formatType, offset)
        }

        fun set(mantissa: Int, exponent: Int) : Deferred<XYBluetoothResult<ByteArray>> {
            return service.writeFloat(uuid, mantissa, exponent, formatType, offset)
        }
    }

    class StringCharacteristic(service: Service, uuid:UUID) : Characteristic(service, uuid) {

        fun get() : Deferred<XYBluetoothResult<String>> {
            return service.readString(uuid)
        }

        fun set(value: String) : Deferred<XYBluetoothResult<String>> {
            return service.writeString(uuid, value)
        }
    }

    class BytesCharacteristic(service: Service, uuid:UUID) : Characteristic(service, uuid) {
        fun get() : Deferred<XYBluetoothResult<ByteArray>> {
            return service.readBytes(uuid)
        }

        fun set(value: ByteArray) : Deferred<XYBluetoothResult<ByteArray>> {
            return service.writeBytes(uuid, value)
        }
    }

    private fun readInt(characteristic: UUID, formatType: Int = BluetoothGattCharacteristic.FORMAT_UINT8, offset:Int = 0): Deferred<XYBluetoothResult<Int>> {
        return device.connection {
            return@connection device.findAndReadCharacteristicInt(
                    serviceUuid,
                    characteristic,
                    formatType,
                    offset
            ).await()
        }
    }

    private fun writeInt(characteristic: UUID, value: Int, formatType: Int = BluetoothGattCharacteristic.FORMAT_UINT8, offset:Int = 0): Deferred<XYBluetoothResult<Int>> {
        return device.connection {
            logInfo("writeInt: connection")
            return@connection device.findAndWriteCharacteristic(
                    serviceUuid,
                    characteristic,
                    value,
                    formatType,
                    offset
            ).await()
        }
    }

    private fun readFloat(characteristic: UUID, formatType: Int = BluetoothGattCharacteristic.FORMAT_FLOAT, offset:Int = 0): Deferred<XYBluetoothResult<Float>> {
        return device.connection {
            return@connection device.findAndReadCharacteristicFloat(
                    serviceUuid,
                    characteristic,
                    formatType,
                    offset
            ).await()
        }
    }

    private fun writeFloat(characteristic: UUID, mantissa: Int, exponent: Int, formatType: Int = BluetoothGattCharacteristic.FORMAT_FLOAT, offset:Int = 0): Deferred<XYBluetoothResult<ByteArray>> {
        return device.connection {
            return@connection device.findAndWriteCharacteristicFloat(
                    serviceUuid,
                    characteristic,
                    mantissa,
                    exponent,
                    formatType,
                    offset
            ).await()
        }
    }

    private fun readString(characteristic: UUID, offset:Int = 0): Deferred<XYBluetoothResult<String>> {
        return device.connection {
            return@connection device.findAndReadCharacteristicString(
                    serviceUuid,
                    characteristic,
                    offset
            ).await()
        }
    }

    private fun writeString(characteristic: UUID, value: String): Deferred<XYBluetoothResult<String>> {
        return device.connection {
            return@connection  device.findAndWriteCharacteristic(
                    serviceUuid,
                    characteristic,
                    value
            ).await()
        }
    }

    private fun enableNotify(characteristic: UUID, enabled: Boolean): Deferred<XYBluetoothResult<Boolean>> {
        return device.connection {
            return@connection device.findAndWriteCharacteristicNotify(
                    serviceUuid,
                    characteristic,
                    enabled
            ).await()
        }
    }

    private fun readBytes(characteristic: UUID): Deferred<XYBluetoothResult<ByteArray>> {
        return device.connection {
            return@connection  device.findAndReadCharacteristicBytes(
                    serviceUuid,
                    characteristic
            ).await()
        }
    }

    private fun writeBytes(characteristic: UUID, bytes: ByteArray): Deferred<XYBluetoothResult<ByteArray>> {
        return device.connection {
            return@connection device.findAndWriteCharacteristic(
                    serviceUuid,
                    characteristic,
                    bytes
            ).await()
        }
    }
}