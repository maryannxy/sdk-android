package com.xyfindables.sdk.sample.fragments

import android.content.Context
import android.widget.TextView
import com.xyfindables.sdk.sample.activities.XYFinderDeviceActivity
import com.xyfindables.sdk.services.Service
import com.xyfindables.ui.XYBaseFragment
import com.xyfindables.ui.ui
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch

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

    open fun initServiceSetTextView(service: Service.IntegerCharacteristic, textView: TextView) {
        ui {
            activity?.showProgressSpinner()
        }
        launch(CommonPool) {
            val result = service.get().await()
            when {
                result.value == null -> ui { textView.text = result.error.toString() }
                else -> ui { textView.text = result.value.toString() }
            }
            ui {
                activity?.hideProgressSpinner()
            }
        }
    }

    open fun unsupported(text: String) {
        activity?.showToast(text)
        ui {
            activity?.hideProgressSpinner()
        }
    }

    companion object {
        private val TAG = XYAppBaseFragment::class.java.simpleName
    }
}