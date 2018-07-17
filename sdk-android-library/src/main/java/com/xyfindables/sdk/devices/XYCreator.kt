package com.xyfindables.sdk.devices

import android.content.Context
import android.util.SparseArray
import com.xyfindables.core.XYBase
import com.xyfindables.sdk.scanner.XYScanResult

abstract class XYCreator : XYBase() {
    //create a device object of best fit
    //we pass in the devices list to prevent garbage collection hell
    abstract fun addDevicesFromScanResult(context: Context, scanResult: XYScanResult, devices: HashMap<Int, XYBluetoothDevice>)

    //return an id that uniquely identifies the device for device bundling
    abstract fun hashFromScanResult(scanResult: XYScanResult) : Int?
}