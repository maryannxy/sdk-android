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
import kotlinx.android.synthetic.main.fragment_generic_access.*


class GenericAccessFragment : XYAppBaseFragment() {


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_generic_access, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button_generic_refresh.setOnClickListener {
            setGenericAccessValues()
        }
    }

    private fun setGenericAccessValues() {
        ui {
            text_device_name.text = ""
            text_appearance.text = ""
            text_privacy_flag.text = ""
            text_reconnection_address.text = ""
            text_peripheral_params.text = ""
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
                val x2 = (activity?.device as? XY2BluetoothDevice)
                x2?.let { getX2Values(it) }
            }
            else -> {
                unsupported("unknown device")
            }

        }
    }

    private fun getX4Values(device: XY4BluetoothDevice) {
        initServiceSetTextView(device.genericAccessService.deviceName, text_device_name)
        initServiceSetTextView(device.genericAccessService.appearance, text_appearance)
        initServiceSetTextView(device.genericAccessService.privacyFlag, text_privacy_flag)
        initServiceSetTextView(device.genericAccessService.reconnectionAddress, text_reconnection_address)
        initServiceSetTextView(device.genericAccessService.peripheralPreferredConnectionParameters, text_peripheral_params)
    }

    private fun getX3Values(device: XY3BluetoothDevice) {
        initServiceSetTextView(device.genericAccessService.deviceName, text_device_name)
        initServiceSetTextView(device.genericAccessService.appearance, text_appearance)
        initServiceSetTextView(device.genericAccessService.privacyFlag, text_privacy_flag)
        initServiceSetTextView(device.genericAccessService.reconnectionAddress, text_reconnection_address)
        initServiceSetTextView(device.genericAccessService.peripheralPreferredConnectionParameters, text_peripheral_params)
    }

    private fun getX2Values(device: XY2BluetoothDevice) {
        initServiceSetTextView(device.genericAccessService.deviceName, text_device_name)
        initServiceSetTextView(device.genericAccessService.appearance, text_appearance)
        initServiceSetTextView(device.genericAccessService.privacyFlag, text_privacy_flag)
        initServiceSetTextView(device.genericAccessService.reconnectionAddress, text_reconnection_address)
        initServiceSetTextView(device.genericAccessService.peripheralPreferredConnectionParameters, text_peripheral_params)
    }

    companion object {
        private const val TAG = "GenericAccessFragment"

        fun newInstance() =
                GenericAccessFragment()
    }
}
