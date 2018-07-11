package com.xyfindables.sdk

import android.os.Build
import com.xyfindables.core.XYBase

class XYCallByVersion : XYBase() {

    class Call(
            val version: Int,
            val closure: () -> Unit
    ) {
        fun call() {
            closure()
        }
    }

    private val calls = ArrayList<Call>()

    fun add(version: Int, closure: () -> Unit) : XYCallByVersion {
        calls.add(Call(version, closure))
        return this
    }

    fun call() {
        for (i in 0..calls.lastIndex) {
            if (Build.VERSION.SDK_INT >= calls[i].version) {
                calls[i].call()
                return
            }
        }
        logError("No Call for OS Version Found", true)
    }
}