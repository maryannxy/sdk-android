package com.xyfindables.sdk.sample.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.xyfindables.sdk.devices.XYFinderBluetoothDevice
import com.xyfindables.sdk.sample.R
import com.xyfindables.ui.ui
import kotlinx.android.synthetic.main.fragment_battery.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch


class BatteryFragment : XYAppBaseFragment() {

    private var job: Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_battery, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button_battery.setOnClickListener {
            getBatteryLevel()
        }
    }

    override fun onPause() {
        super.onPause()
        job?.cancel()
    }

    override fun update() {
        logInfo("update")
    }

    private fun getBatteryLevel() {
        logInfo("batteryButton: onClick")
        ui {
            button_battery.isEnabled = false
            activity?.showProgressSpinner()
        }
        job = launch(CommonPool) {
            val level = (activity?.device as? XYFinderBluetoothDevice)?.batteryLevel()?.await()
            when {
                level == null -> activity?.showToast("Unable to get battery level")
                level.value == null -> activity?.showToast("Unable to get battery level.value")
                else -> ui {
                    battery_level?.text = level.value.toString()
                }
            }

            ui {
                button_battery.isEnabled = true
                activity?.hideProgressSpinner()
            }
        }
    }


    companion object {
        private const val TAG = "BatteryFragment"

        fun newInstance() =
                BatteryFragment()
    }

}
