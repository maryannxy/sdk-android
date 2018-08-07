package com.xyfindables.sdk.sample.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.xyfindables.sdk.devices.XYFinderBluetoothDevice
import com.xyfindables.sdk.sample.R
import com.xyfindables.ui.ui
import kotlinx.android.synthetic.main.fragment_alert.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch


class GenericAccessFragment : XYAppBaseFragment() {


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_generic_access, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button_refresh.setOnClickListener {
            logInfo("refresh clicked")
            update()
        }
    }

    override fun update() {

    }

    private fun name() {
        ui {
            button_refresh.isEnabled = false
        }
        logInfo("beepButton: got xyDevice")
        launch(CommonPool) {
            (activity?.device as? XYFinderBluetoothDevice)?.find()?.await()
            ui {
                button_refresh.isEnabled = true
            }
        }
    }

    companion object {
        private const val TAG = "GenericAccessFragment"

        fun newInstance() =
                GenericAccessFragment()
    }
}
