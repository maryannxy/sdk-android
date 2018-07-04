package com.xyfindables.sdk.sample

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.xyfindables.core.XYBase

import com.xyfindables.sdk.XYDevice
import com.xyfindables.sdk.XYSmartScan

import java.util.ArrayList

class XYDeviceAdapter(private val _activity: Activity) : BaseAdapter() {
    private var _devices: List<XYDevice>

    val scanner : XYSmartScan
        get() {
            return XYSmartScan.instance
        }

    init {
        _devices = ArrayList()
        XYSmartScan.instance.addListener(TAG, object : XYDevice.Listener {
            override fun entered(device: XYDevice) {
                _activity.runOnUiThread(Thread(Runnable {
                    _devices = scanner.devices!!
                    notifyDataSetChanged()
                }))
            }

            override fun exited(device: XYDevice) {
                _activity.runOnUiThread(Thread(Runnable {
                    _devices = scanner.devices!!
                    notifyDataSetChanged()
                }))
            }

            override fun detected(device: XYDevice) {
            }

            override fun buttonPressed(device: XYDevice, buttonType: XYDevice.ButtonType) {}

            override fun buttonRecentlyPressed(device: XYDevice, buttonType: XYDevice.ButtonType) {}

            override fun statusChanged(status: XYSmartScan.Status) {}

            override fun updated(device: XYDevice) {

            }

            override fun connectionStateChanged(device: XYDevice, newState: Int) {

            }

            override fun readRemoteRssi(device: XYDevice, rssi: Int) {

            }

        })
    }

    override fun getCount(): Int {
        return _devices.size
    }

    override fun getItem(position: Int): Any {
        return _devices[position]
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).hashCode().toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        if (view == null) {
            view = _activity.layoutInflater.inflate(R.layout.device_item, parent, false)
        }
        (view as XYDeviceItemView).setDevice(getItem(position) as XYDevice)

        return view
    }

    companion object {

        private val TAG = XYDeviceAdapter::class.java.simpleName
    }
}
