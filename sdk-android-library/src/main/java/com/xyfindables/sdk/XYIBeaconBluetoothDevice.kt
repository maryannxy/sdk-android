package com.xyfindables.sdk

import android.bluetooth.BluetoothDevice
import android.content.Context
import java.util.*

open class XYIBeaconBluetoothDevice(context: Context, device: BluetoothDevice) : XYBluetoothDevice(context, device) {
    val uuid : UUID
        get() {
            return UUID.randomUUID()
        }

    val major : Int
        get() {
            return 0
        }

    val minor : Int
        get() {
            return 0
        }

    val power : Short
        get() {
            return 0
        }
}