package com.xyfindables.sdk.devices

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import com.xyfindables.sdk.scanner.XYScanResult
import com.xyfindables.sdk.scanner.XYScanResultManual
import unsigned.Uint
import unsigned.Ushort
import java.util.*

//this is a "fake" device that represents the device that is doing the scanning
open class XYMobileBluetoothDevice(context: Context, scanResult: XYScanResult, hash:Int) : XYFinderBluetoothDevice(context, scanResult, hash) {

    init {
        //we use this since the user would prefer for it to survice a resinstall.  DO NOT USE for advertising!
        val uniqueId = Uint(Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID).hashCode())
        _major = Ushort(uniqueId.and(Uint(0xffff0000)).shr(16).toInt())
        _minor = Ushort(uniqueId.and(0xffff).toInt())

        var address = device?.address
        if (address == null) {
            val bluetoothManager = context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            if (bluetoothManager != null) {
                address = bluetoothManager?.adapter?.address
            }
        }
        _address = address ?: "00:00:00:00:00:00"
    }

    companion object {

        val FAMILY_UUID = UUID.fromString("735344c9-e820-42ec-9da7-f43a2b6802b9")

        fun create(context: Context, device: BluetoothDevice? = null) : XYMobileBluetoothDevice {
            val fakeScanResult = XYScanResultManual(device, -20, null, SystemClock.elapsedRealtimeNanos())
            return XYMobileBluetoothDevice(context, fakeScanResult, FAMILY_UUID.hashCode())
        }
    }
}