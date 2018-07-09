package com.xyfindables.sdk.sample

import android.app.Application
import android.os.Build
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
}