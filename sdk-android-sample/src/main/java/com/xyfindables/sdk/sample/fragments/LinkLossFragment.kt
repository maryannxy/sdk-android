package com.xyfindables.sdk.sample.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.xyfindables.sdk.devices.XY2BluetoothDevice
import com.xyfindables.sdk.devices.XY3BluetoothDevice
import com.xyfindables.sdk.devices.XY4BluetoothDevice
import com.xyfindables.sdk.sample.R
import com.xyfindables.ui.ui
import kotlinx.android.synthetic.main.fragment_link_loss.*


class LinkLossFragment : XYAppBaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_link_loss, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button_link_loss_refresh.setOnClickListener {
            initLinkLossValues()
        }
    }

    private fun initLinkLossValues() {
        ui {
            text_alert_level.text = ""
        }

        when (activity?.device) {
            is XY4BluetoothDevice -> {
                val x4 = (activity?.device as? XY4BluetoothDevice)
                x4?.let {
                    initServiceSetTextView(x4.linkLossService.alertLevel, text_alert_level)
                }
            }
            is XY3BluetoothDevice -> {
                val x3 = (activity?.device as? XY3BluetoothDevice)
                x3?.let {
                    initServiceSetTextView(x3.linkLossService.alertLevel, text_alert_level)
                }
            }
            is XY2BluetoothDevice -> {
                val x2 = (activity?.device as? XY3BluetoothDevice)
                x2?.let {
                    initServiceSetTextView(x2.linkLossService.alertLevel, text_alert_level)
                }
            }
            else -> {
                unsupported("unknown device")
            }

        }
    }

    companion object {
        private const val TAG = "LinkLossFragment"

        fun newInstance() =
                LinkLossFragment()
    }

}
