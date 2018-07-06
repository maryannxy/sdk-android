package com.xyfindables.sdk

import android.os.Build

class XYCallByVersion {

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
            if (Build.VERSION.SDK_INT <= calls[i].version) {
                calls[i].call()
                return
            }
        }
        throw UnsupportedOperationException()
    }
}