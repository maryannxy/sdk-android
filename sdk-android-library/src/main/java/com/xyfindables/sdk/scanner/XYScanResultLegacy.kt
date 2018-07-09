package com.xyfindables.sdk.scanner

import android.annotation.TargetApi
import android.bluetooth.BluetoothDevice
import android.os.Parcel
import com.xyfindables.sdk.bluetooth.ScanResultLegacy

@TargetApi(18)
class XYScanResultLegacy(private val scanResult:ScanResultLegacy): XYScanResult() {

    override val timestampNanos : Long
        get() {
            return scanResult.timestampNanos
        }

    override val rssi : Int
        get() {
            return scanResult.rssi
        }

    override val scanRecord : XYScanRecord?
        get() {
            val scanRecord = scanResult.scanRecord
            if (scanRecord != null) {
                return XYScanRecordLegacy(scanRecord)
            }
            return null
        }

    override val device : BluetoothDevice
        get() {
            return scanResult.device
        }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        scanResult.writeToParcel(dest, flags)
    }

    override fun describeContents() : Int {
        return scanResult.describeContents()
    }

    override fun hashCode() : Int {
        return scanResult.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return scanResult.equals(other)
    }

    override fun toString(): String {
        return scanResult.toString()
    }
}