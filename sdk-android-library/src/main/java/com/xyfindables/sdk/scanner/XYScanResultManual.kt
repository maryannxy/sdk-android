package com.xyfindables.sdk.scanner

import android.bluetooth.BluetoothDevice
import android.os.Parcel

class XYScanResultManual(
        val _device: BluetoothDevice,
        val _rssi: Int,
        val _scanRecord: XYScanRecord?,
        val _timestampNanos: Long
) : XYScanResult() {
    override val device: BluetoothDevice
        get() = _device

    override val rssi: Int
        get() = _rssi

    override val scanRecord: XYScanRecord?
        get() = _scanRecord

    override val timestampNanos: Long
        get() = _timestampNanos

    override fun describeContents(): Int {
        logError("describeContents: Not Implemented", true)
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        logError("writeToParcel: Not Implemented", true)
    }
}