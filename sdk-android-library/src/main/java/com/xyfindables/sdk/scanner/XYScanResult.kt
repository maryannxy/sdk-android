package com.xyfindables.sdk.scanner

import android.bluetooth.BluetoothDevice
import android.os.Parcel
import com.xyfindables.core.XYBase

//kotlin wrapper and way for us to treat pre 5.0 Android the same

abstract class XYScanResult:XYBase() {

    abstract val timestampNanos : Long

    abstract val rssi : Int

    abstract val scanRecord : XYScanRecord?

    abstract val device : BluetoothDevice?

    abstract fun writeToParcel(dest: Parcel, flags: Int)

    abstract fun describeContents() : Int

    val address : String
        get() {
            return device?.address ?: "00:00:00:00:00:00"
        }
}