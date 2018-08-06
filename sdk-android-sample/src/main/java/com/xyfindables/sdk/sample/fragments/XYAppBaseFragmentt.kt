package com.xyfindables.sdk.sample.fragments

import android.content.Context
import com.xyfindables.sdk.sample.activities.XYFinderDeviceActivity
import com.xyfindables.ui.XYBaseFragment

abstract class XYAppBaseFragment : XYBaseFragment() {

    var activity: XYFinderDeviceActivity? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is XYFinderDeviceActivity) {
            activity = context
        } else {
            logError(TAG, "context is not instance of XYFinderDeviceActivity!", true)
        }
    }

    open fun update() {}


    companion object {
        private val TAG = XYAppBaseFragment::class.java.simpleName
    }
}