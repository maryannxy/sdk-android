package com.xyfindables.sdk.devices.router

import android.content.Context
import com.xyfindables.sdk.devices.XYBluetoothDevice
import com.xyfindables.sdk.scanner.XYScanResult

interface RouterInterface {
    fun run (context: Context, scanResult: XYScanResult) : XYBluetoothDevice?
}