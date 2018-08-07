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
import kotlinx.android.synthetic.main.fragment_current_time.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch


class CurrentTimeFragment : XYAppBaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_current_time, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button_refresh.setOnClickListener {
            setTimeValues()
        }
    }

    private fun setTimeValues() {
        ui {
            button_refresh.isEnabled = false
            activity?.showProgressSpinner()

            localTimeInformation.text = ""
            currentTime.text = ""
            referenceTimeInformation.text = ""
        }

        when (activity?.device) {
            is XY4BluetoothDevice -> {
                val x4 = (activity?.device as? XY4BluetoothDevice)
                x4?.let { getX4Values(it) }
            }
            is XY3BluetoothDevice -> {
                val x3 = (activity?.device as? XY3BluetoothDevice)
                x3?.let { getX3Values(it) }
            }
            is XY2BluetoothDevice -> {
                unsupported("Not supported by XY2BluetoothDevice")
            }
            else -> {
                unsupported("unknown device")
            }
        }
    }

    //TODO - 2 below methods are redundant - how to combine ?
    private fun getX3Values(device: XY3BluetoothDevice) {
        launch(CommonPool) {
            val time = device.currentTimeService.currentTime.get().await()
            when {
                time.value == null -> ui { currentTime.text = time.error.toString() }
                else -> ui { currentTime.text = time.value.toString() }
            }
        }

        launch(CommonPool) {
            val localTime = device.currentTimeService.localTimeInformation.get().await()
            when {
                localTime.value == null -> ui { localTimeInformation.text = localTime.error.toString() }
                else -> ui { localTimeInformation.text = localTime.value.toString() }
            }
        }

        launch(CommonPool) {
            val refTime = device.currentTimeService.referenceTimeInformation.get().await()
            when {
                refTime.value == null -> ui { referenceTimeInformation.text = refTime.error.toString() }
                else -> ui { referenceTimeInformation.text = refTime.value.toString() }
            }
            ui {
                button_refresh.isEnabled = true
                activity?.hideProgressSpinner()
            }
        }
    }

    private fun getX4Values(x4: XY4BluetoothDevice) {
        launch(CommonPool) {
            val time = x4.currentTimeService.currentTime.get().await()
            when {
                time.value == null -> ui { currentTime.text = time.error.toString() }
                else -> ui { currentTime.text = time.value.toString() }
            }
        }

        launch(CommonPool) {
            val localTime = x4.currentTimeService.localTimeInformation.get().await()
            when {
                localTime.value == null -> ui { localTimeInformation.text = localTime.error.toString() }
                else -> ui { localTimeInformation.text = localTime.value.toString() }
            }
        }

        launch(CommonPool) {
            val refTime = x4.currentTimeService.referenceTimeInformation.get().await()
            when {
                refTime.value == null -> ui { referenceTimeInformation.text = refTime.error.toString() }
                else -> ui { referenceTimeInformation.text = refTime.value.toString() }
            }
            ui {
                button_refresh.isEnabled = true
                activity?.hideProgressSpinner()
            }
        }
    }

    private fun unsupported(text : String) {
        activity?.showToast(text)
        ui {
            button_refresh.isEnabled = true
            activity?.hideProgressSpinner()
        }
    }

    companion object {

        fun newInstance() =
                CurrentTimeFragment()
    }

}
