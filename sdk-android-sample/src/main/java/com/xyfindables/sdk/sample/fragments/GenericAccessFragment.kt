package com.xyfindables.sdk.sample.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.xyfindables.sdk.devices.XYFinderBluetoothDevice
import com.xyfindables.sdk.sample.R
import com.xyfindables.ui.ui
import kotlinx.android.synthetic.main.fragment_generic_access.*
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

        button_generic_refresh.setOnClickListener {
            logInfo("refresh clicked")
            update()
        }
    }

    override fun update() {

    }

    companion object {
        private const val TAG = "GenericAccessFragment"

        fun newInstance() =
                GenericAccessFragment()
    }
}
