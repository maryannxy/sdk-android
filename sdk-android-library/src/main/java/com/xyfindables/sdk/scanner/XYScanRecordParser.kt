package com.xyfindables.sdk.scanner

import com.xyfindables.core.XYBase

abstract class XYScanRecordParser(val scanRecord: XYScanRecord) : XYBase() {
    abstract fun isValid() : Boolean
}