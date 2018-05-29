package com.xyfindables.sdk.sample

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter

import com.xyfindables.sdk.XYDevice
import com.xyfindables.sdk.XYSmartScan

import java.util.ArrayList

class XYDeviceAdapter(private val _activity: Activity) : BaseAdapter() {
    private val _devices: ArrayList<XYDevice>

    init {
        _devices = ArrayList()
        XYSmartScan.instance.addListener(TAG, object : XYDevice.Listener {
            override fun entered(device: XYDevice) {
                _activity.runOnUiThread(Thread(Runnable {
                    _devices.add(device)
                    notifyDataSetChanged()
                }))
            }

            override fun exited(device: XYDevice) {
                _activity.runOnUiThread(Thread(Runnable {
                    _devices.remove(device)
                    notifyDataSetChanged()
                }))
            }

            override fun detected(device: XYDevice) {}

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
        var convertView = convertView
        if (convertView == null) {
            convertView = _activity.layoutInflater.inflate(R.layout.device_item, parent, false)
        }
        (convertView as XYDeviceItemView).setDevice(getItem(position) as XYDevice)

        return convertView
    }

    companion object {

        private val TAG = XYDeviceAdapter::class.java.simpleName
    }
}
