package com.xyfindables.sdk.devices

import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import com.xyfindables.sdk.XYDeviceCharacteristic
import com.xyfindables.sdk.XYDeviceService
import com.xyfindables.sdk.gatt.clients.XYBluetoothGatt
import com.xyfindables.sdk.scanner.XYScanResult
import kotlinx.coroutines.experimental.Deferred
import java.nio.ByteBuffer
import java.util.*

open class XY4BluetoothDevice(context: Context, scanResult: XYScanResult) : XYFinderBluetoothDevice(context, scanResult) {

    val defaultUnlockCode = byteArrayOf(0x00.toByte(), 0x01.toByte(), 0x02.toByte(), 0x03.toByte(), 0x04.toByte(), 0x05.toByte(), 0x06.toByte(), 0x07.toByte(), 0x08.toByte(), 0x09.toByte(), 0x0a.toByte(), 0x0b.toByte(), 0x0c.toByte(), 0x0d.toByte(), 0x0e.toByte(), 0x0f.toByte())

    override fun find() : Deferred<Boolean> {
        return writePrimaryBuzzer(1)
    }

    fun writePrimaryBuzzer(tone: Int) : Deferred<Boolean> {
        return access {
            return@access asyncFindAndWriteCharacteristic(
                    XYDeviceService.XY4Primary,
                    XYDeviceCharacteristic.XY4PrimaryBuzzer,
                    tone,
                    BluetoothGattCharacteristic.FORMAT_UINT8,
                    0
            )
        }
    }

    fun readPrimaryStayAwake() : Deferred<Int?> {
        return asyncFindAndReadCharacteristicInt(
                XYDeviceService.XY4Primary,
                XYDeviceCharacteristic.XY4PrimaryStayAwake,
                BluetoothGattCharacteristic.FORMAT_UINT8,
                0
        )
    }

    fun writePrimaryStayAwake(flag: Int) : Deferred<Boolean> {
        return asyncFindAndWriteCharacteristic(
                XYDeviceService.XY4Primary,
                XYDeviceCharacteristic.XY4PrimaryStayAwake,
                flag,
                BluetoothGattCharacteristic.FORMAT_UINT8,
                0
        )
    }

    fun readPrimaryLock() : Deferred<ByteArray?> {
        return asyncFindAndReadCharacteristicBytes(
                XYDeviceService.XY4Primary,
                XYDeviceCharacteristic.XY4PrimaryLock
        )
    }

    fun writePrimaryLock(bytes: ByteArray) : Deferred<Boolean> {
        return asyncFindAndWriteCharacteristic(
                XYDeviceService.XY4Primary,
                XYDeviceCharacteristic.XY4PrimaryLock,
                bytes
        )
    }

    fun writePrimaryUnlock(bytes: ByteArray) : Deferred<Boolean> {
        return asyncFindAndWriteCharacteristic(
                XYDeviceService.XY4Primary,
                XYDeviceCharacteristic.XY4PrimaryUnlock,
                bytes
        )
    }

    companion object {

        val uuid = UUID.fromString("a44eacf4-0104-0000-0000-5f784c9977b5")

        fun enable(enable: Boolean) {
            if (enable) {
                XYFinderBluetoothDevice.enable(true)
                XYFinderBluetoothDevice.uuidToCreator[uuid] = {
                    context: Context,
                    scanResult: XYScanResult
                    ->
                    XY4BluetoothDevice(context, scanResult)
                }
            } else {
                XYFinderBluetoothDevice.uuidToCreator.remove(uuid)
            }
        }
    }
}