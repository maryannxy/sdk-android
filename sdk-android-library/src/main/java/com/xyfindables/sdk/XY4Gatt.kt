package com.xyfindables.sdk

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.os.Handler
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async

class XY4Gatt(context: Context,
              device: BluetoothDevice,
              autoConnect: Boolean,
              callback: BluetoothGattCallback?,
              transport: Int?,
              phy: Int?,
              handler: Handler?) : XYBluetoothGatt(context, device, autoConnect, callback, transport, phy, handler) {

    val defaultUnlockCode = byteArrayOf(0x00.toByte(), 0x01.toByte(), 0x02.toByte(), 0x03.toByte(), 0x04.toByte(), 0x05.toByte(), 0x06.toByte(), 0x07.toByte(), 0x08.toByte(), 0x09.toByte(), 0x0a.toByte(), 0x0b.toByte(), 0x0c.toByte(), 0x0d.toByte(), 0x0e.toByte(), 0x0f.toByte())

    fun writePrimaryBuzzer(tone: Int) : Deferred<Boolean>{
        return asyncFindAndWriteCharacteristic(
                XYDeviceService.XY4Primary,
                XYDeviceCharacteristic.XY4PrimaryBuzzer,
                tone,
                BluetoothGattCharacteristic.FORMAT_UINT8,
                0
        )
    }

    fun readPrimaryStayAwake() : Deferred<Int?>{
        return asyncFindAndReadCharacteristicInt(
                XYDeviceService.XY4Primary,
                XYDeviceCharacteristic.XY4PrimaryStayAwake,
                BluetoothGattCharacteristic.FORMAT_UINT8,
                0
        )
    }

    fun writePrimaryStayAwake(flag: Int) : Deferred<Boolean>{
        return asyncFindAndWriteCharacteristic(
                XYDeviceService.XY4Primary,
                XYDeviceCharacteristic.XY4PrimaryStayAwake,
                flag,
                BluetoothGattCharacteristic.FORMAT_UINT8,
                0
        )
    }

    fun readPrimaryLock() : Deferred<ByteArray?>{
        return asyncFindAndReadCharacteristicBytes(
                XYDeviceService.XY4Primary,
                XYDeviceCharacteristic.XY4PrimaryLock
        )
    }

    fun writePrimaryLock(bytes: ByteArray) : Deferred<Boolean>{
        return asyncFindAndWriteCharacteristic(
                XYDeviceService.XY4Primary,
                XYDeviceCharacteristic.XY4PrimaryLock,
                bytes
        )
    }

    fun writePrimaryUnlock(bytes: ByteArray) : Deferred<Boolean>{
        return asyncFindAndWriteCharacteristic(
                XYDeviceService.XY4Primary,
                XYDeviceCharacteristic.XY4PrimaryUnlock,
                bytes
        )
    }
}