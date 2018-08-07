package com.xyfindables.sdk.sample

import android.content.Context
import com.xyfindables.sdk.devices.XY4BluetoothDevice
import com.xyfindables.sdk.devices.XYFinderBluetoothDevice
import com.xyfindables.sdk.scanner.XYScanResult

class TestObj(context: Context, scanResult: XYScanResult, hash: Int) : XY4BluetoothDevice(context, scanResult, hash) {

    var dev : XY4BluetoothDevice? = null
    private var _device: XYFinderBluetoothDevice? = null

    init {
        logInfo("test: init")
        dev = this
    }

    fun getTestDevice() : XY4BluetoothDevice? {
        logInfo("test: foo")
        return dev
    }

}