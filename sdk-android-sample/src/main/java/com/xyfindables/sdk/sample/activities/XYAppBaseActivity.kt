package com.xyfindables.sdk.sample.activities

import com.xyfindables.sdk.sample.XYApplication
import com.xyfindables.sdk.scanner.XYFilteredSmartScan
import com.xyfindables.ui.XYBaseActivity

abstract class XYAppBaseActivity : XYBaseActivity() {
    val scanner: XYFilteredSmartScan
        get() {
            return (this.applicationContext as XYApplication).scanner
        }
}