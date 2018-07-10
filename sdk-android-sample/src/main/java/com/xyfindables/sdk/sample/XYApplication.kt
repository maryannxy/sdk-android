package com.xyfindables.sdk.sample

import android.app.Application
import android.os.Build
import com.xyfindables.sdk.XYAppleBluetoothDevice
import com.xyfindables.sdk.XYFinderBluetoothDevice
import com.xyfindables.sdk.XYIBeaconBluetoothDevice
import com.xyfindables.sdk.scanner.XYFilteredSmartScan
import com.xyfindables.sdk.scanner.XYFilteredSmartScanLegacy
import com.xyfindables.sdk.scanner.XYFilteredSmartScanModern

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
        super.onCreate()
        XYAppleBluetoothDevice.enable(true)
        XYIBeaconBluetoothDevice.enable(true)
        XYFinderBluetoothDevice.enable(true)
        scanner.start()
    }
}