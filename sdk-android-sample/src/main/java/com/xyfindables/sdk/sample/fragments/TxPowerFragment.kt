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
import kotlinx.android.synthetic.main.fragment_tx_power.*


class TxPowerFragment : XYAppBaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tx_power, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button_tx_refresh.setOnClickListener {
            setTxValues()
        }
    }

    private fun setTxValues() {
        ui {
            text_tx_power.text = ""
        }

        when (activity?.device) {
            is XY4BluetoothDevice -> {
                val x4 = (activity?.device as? XY4BluetoothDevice)
                x4?.let {
                    initServiceSetTextView(x4.txPowerService.txPowerLevel, text_tx_power)
                }
            }
            is XY3BluetoothDevice -> {
                val x3 = (activity?.device as? XY3BluetoothDevice)
                x3?.let {
                    initServiceSetTextView(x3.txPowerService.txPowerLevel, text_tx_power)
                }
            }
            is XY2BluetoothDevice -> {
                unsupported("Not supported by XY2BluetoothDevice")
            }
            else -> {
                unsupported("unknown device")
            }

        }
    }

    companion object {
        private const val TAG = "TxPowerFragment"

        fun newInstance() =
                TxPowerFragment()
    }

}
