package com.xyfindables.sdk.actionHelper

import android.content.Context

import com.xyfindables.sdk.action.XYDeviceAction

/**
 * Created by alex.mcelroy on 9/6/2017.
 */

abstract class XYActionHelper {

    var action: XYDeviceAction? = null

    interface Callback {
        fun completed(success: Boolean)
    }

    interface Notification {
        fun updated(status: Boolean)
    }

    fun start(context: Context) {
        action!!.start(context)
    }
}
