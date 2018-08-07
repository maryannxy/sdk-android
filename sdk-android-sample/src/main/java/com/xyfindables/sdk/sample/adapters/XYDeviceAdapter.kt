package com.xyfindables.sdk.sample.adapters

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.xyfindables.sdk.devices.XYBluetoothDevice
import com.xyfindables.sdk.devices.XYFinderBluetoothDevice
import com.xyfindables.sdk.sample.R
import com.xyfindables.sdk.sample.XYApplication
import com.xyfindables.sdk.sample.views.XYDeviceItemView
import com.xyfindables.sdk.scanner.XYFilteredSmartScan
import com.xyfindables.ui.ui
import java.util.*

class XYDeviceAdapter(private val activity: Activity) : BaseAdapter() {
    private var devices: List<XYFinderBluetoothDevice>
    private var lastSort = System.currentTimeMillis()

    private val scanner : XYFilteredSmartScan
        get() {
            return (activity.applicationContext as XYApplication).scanner
        }

    private val smartScannerListener = object : XYFilteredSmartScan.Listener() {
        override fun entered(device: XYBluetoothDevice) {
            refreshDevices()
        }

        override fun exited(device: XYBluetoothDevice) {
            refreshDevices()
        }

        override fun detected(device: XYBluetoothDevice) {
            refreshDevices()
        }
    }

    fun refreshDevices() {
        if ((System.currentTimeMillis() -  lastSort) > 5000) {
            devices = XYFinderBluetoothDevice.sortedList(scanner.devices)
            ui { notifyDataSetChanged() }
            lastSort = System.currentTimeMillis()
        }
    }

    init {
        devices = ArrayList()
        scanner.addListener(TAG, smartScannerListener)
    }

    override fun getCount(): Int {
        return devices.size
    }

    override fun getItem(position: Int): Any {
        return devices[position]
    }

    override fun getItemId(position: Int): Long {
        return devices[position].address.hashCode().toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        if (view == null) {
            view = activity.layoutInflater.inflate(R.layout.device_item, parent, false)
        }
        (view as XYDeviceItemView).setDevice(getItem(position) as XYBluetoothDevice)

        return view
    }

    companion object {

        private val TAG = XYDeviceAdapter::class.java.simpleName
    }
}
