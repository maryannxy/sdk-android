package com.xyfindables.sdk.sample

import android.app.Application
import android.os.Build
import com.xyfindables.core.XYBase
import com.xyfindables.sdk.devices.*
import com.xyfindables.sdk.scanner.*

class XYApplication : Application() {
    private var _scanner: XYFilteredSmartScan? = null
    val scanner: XYFilteredSmartScan
        get() {
            if (_scanner == null) {
                if (Build.VERSION.SDK_INT >= 21) {
                    _scanner = XYFilteredSmartScanModern(this.applicationContext)
                } else {
                    _scanner = XYFilteredSmartScanLegacy(this.applicationContext)
                }
            }
            return _scanner!!
        }

    override fun onCreate() {
        XYBase.logInfo("XYApplication", "onCreate")
        super.onCreate()
        XYAppleBluetoothDevice.enable(true)
        XYIBeaconBluetoothDevice.enable(true)
        XYFinderBluetoothDevice.enable(true)
        XY4BluetoothDevice.enable(true)
        XY3BluetoothDevice.enable(true)
        scanner.start()
    }

    override fun onTerminate() {
        XYBase.logInfo("XYApplication", "onTerminate")
        scanner.stop()
        super.onTerminate()
    }
}