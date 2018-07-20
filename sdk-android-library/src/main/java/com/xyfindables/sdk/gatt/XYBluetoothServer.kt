package com.xyfindables.sdk.gatt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.content.Context
import android.os.Handler

open class XYBluetoothServer(context: Context,
                             device: BluetoothDevice?,
                             autoConnect: Boolean,
                             callback: BluetoothGattCallback?,
                             transport: Int?,
                             phy: Int?,
                             handler: Handler?)
    : XYBluetoothGatt(
        context,
        device,
        autoConnect,
        callback,
        transport,
        phy,
        handler
    ) {

    var gattServer: BluetoothGattServer? = null

    private var callback = object : BluetoothGattServerCallback() {

    }

    fun startServer() : Boolean {
        var result = false
        synchronized(this) {
            if (gattServer == null) {
                gattServer = bluetoothManager?.openGattServer(context, callback)
                if (gattServer != null) {
                    result = true
                }
            }
        }
        return result
    }

    fun stopServer() {
        synchronized(this) {
            gattServer?.close()
            gattServer = null
        }
    }

}