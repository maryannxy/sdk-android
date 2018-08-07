package com.xyfindables.sdk.devices

import android.content.Context
import com.xyfindables.core.XYBase
import com.xyfindables.sdk.scanner.XYScanResult

abstract class XYCreator : XYBase() {
    //create a device object of best fit
    //we pass in the devices list to prevent garbage collection hell
    abstract fun getDevicesFromScanResult(context: Context, scanResult: XYScanResult, globalDevices: HashMap<Int, XYBluetoothDevice>, foundDevices: HashMap<Int, XYBluetoothDevice>)
}