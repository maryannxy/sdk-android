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

    fun primaryBuzzer(tone: Int) : Deferred<Boolean>{
        return async {
            logInfo("primaryBuzzer")
            val characteristic = asyncFindCharacteristic(XYDeviceService.XY4Primary, XYDeviceCharacteristic.XY4PrimaryBuzzer).await()
            if (characteristic != null) {
                characteristic.setValue(tone, BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                return@async asyncWriteCharacteristic(characteristic).await()
            } else {
                return@async true
            }
        }
    }
}