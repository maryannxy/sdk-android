package com.xyfindables.sdk.sample

import com.xyfindables.sdk.scanner.XYFilteredSmartScan
import com.xyfindables.ui.XYBaseActivity

abstract class XYAppBaseActivity : XYBaseActivity() {
    val scanner: XYFilteredSmartScan
        get() {
            return (this.applicationContext as XYApplication).scanner
        }
}