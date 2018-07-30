package com.xyfindables.sdk.sample

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import com.xyfindables.core.XYBase
import com.xyfindables.sdk.devices.XYBluetoothDevice

import com.xyfindables.sdk.devices.XYIBeaconBluetoothDevice
import kotlinx.android.synthetic.main.device_item.view.*

/**
 * Created by arietrouw on 12/27/17.
 */

class XYDeviceItemView(context: Context, attrs: AttributeSet) : RelativeLayout(context, attrs) {

    private var device: XYBluetoothDevice? = null

    init {
        setOnClickListener {
            if (device != null) {
                val intent = Intent(context, XYFinderDeviceActivity::class.java)
                intent.putExtra(XYFinderDeviceActivity.EXTRA_DEVICEHASH, device!!.hashCode())
                context.startActivity(intent)
            }
        }
    }

    fun update() {
        post {
            family.text = device!!.javaClass.simpleName
            name.text = device!!.name
            address.text = device!!.address
            rssi.text = device!!.rssi.toString()
            val majorLabelView = findViewById<TextView>(R.id.majorLabel)
            val minorLabelView = findViewById<TextView>(R.id.minorLabel)

            val ibeacon = device as? XYIBeaconBluetoothDevice
            if (ibeacon != null) {
                major.text = ibeacon.major.toString()
                minor.text = ibeacon.minor.toString()
                uuid.text = ibeacon.uuid.toString()
                major.visibility = View.VISIBLE
                minor.visibility = View.VISIBLE
                majorLabelView.visibility = View.VISIBLE
                minorLabelView.visibility = View.VISIBLE
            } else {
                uuid.text = "N/A"
                major.visibility = View.GONE
                minor.visibility = View.GONE
                majorLabelView.visibility = View.GONE
                minorLabelView.visibility = View.GONE
            }

            pulses.text = device!!.detectCount.toString()
        }
    }

    private val deviceListener = object : XYBluetoothDevice.Listener() {
        override fun entered(device: XYBluetoothDevice) {

        }

        override fun exited(device: XYBluetoothDevice) {

        }

        override fun detected(device: XYBluetoothDevice) {
            update()
        }

        override fun connectionStateChanged(device: XYBluetoothDevice, newState: Int) {

        }
    }

    fun setDevice(device: XYBluetoothDevice?) {
        if (device != null) {
            device.removeListener(TAG)
        } else {
            XYBase.logError(TAG, "Setting NULL device")
        }

        this.device = device

        device!!.addListener(TAG, deviceListener)
        update()
    }

    companion object {

        private val TAG = XYDeviceItemView::class.java.simpleName
    }
}
