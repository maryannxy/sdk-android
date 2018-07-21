package com.xyfindables.sdk.sample

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.xyfindables.sdk.devices.XYBluetoothDevice

import com.xyfindables.sdk.scanner.XYFilteredSmartScan
import com.xyfindables.ui.ui

import java.util.ArrayList

class XYDeviceAdapter(private val activity: Activity) : BaseAdapter() {
    private var devices: List<Pair<Int, XYBluetoothDevice>>

    val scanner : XYFilteredSmartScan
        get() {
            return (activity.applicationContext as XYApplication).scanner
        }

    val smartScannerListener = object : XYFilteredSmartScan.Listener() {
        override fun entered(device: XYBluetoothDevice) {
            ui {
                devices = scanner.devices.toList()
                notifyDataSetChanged()
            }
        }

        override fun exited(device: XYBluetoothDevice) {
            ui {
                devices = scanner.devices.toList()
                notifyDataSetChanged()
            }
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
        return devices[position].second
    }

    override fun getItemId(position: Int): Long {
        return devices[position].second.address.hashCode().toLong()
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
