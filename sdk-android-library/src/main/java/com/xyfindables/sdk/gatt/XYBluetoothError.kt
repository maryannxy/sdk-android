package com.xyfindables.sdk.gatt

import com.xyfindables.core.XYBase

open class XYBluetoothError(message: String) : Error(message) {

    init {
        XYBase.logError("XYBluetoothError", message, false)
    }

    override fun toString(): String {
        return "XYBluetoothError: ${super.toString()}"
    }
}