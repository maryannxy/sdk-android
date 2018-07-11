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

/**
 * Created by arietrouw on 12/27/17.
 */

class XYDeviceItemView(context: Context, attrs: AttributeSet) : RelativeLayout(context, attrs) {

    private var device: XYBluetoothDevice? = null

    init {
        setOnClickListener {
            if (device != null) {
                val intent = Intent(context, XYFinderDeviceActivity::class.java)
                intent.putExtra(XYFinderDeviceActivity.EXTRA_DEVICEID, device!!.id)
                context.startActivity(intent)
            }
        }
    }

    fun update() {
        post {
            val familyView = findViewById<TextView>(R.id.family)
            familyView.text = device!!.javaClass.simpleName

            val nameView = findViewById<TextView>(R.id.name)
            nameView.text = device!!.name

            val addressView = findViewById<TextView>(R.id.address)
            addressView.text = device!!.address

            val rssiView = findViewById<TextView>(R.id.rssi)
            rssiView.text = device!!.rssi.toString()

            val ibeacon = device as? XYIBeaconBluetoothDevice

            val majorView = findViewById<TextView>(R.id.major)
            val minorView = findViewById<TextView>(R.id.minor)
            val uuidView = findViewById<TextView>(R.id.uuid)

            val majorLabelView = findViewById<TextView>(R.id.majorLabel)
            val minorLabelView = findViewById<TextView>(R.id.minorLabel)

            if (ibeacon != null) {
                majorView.text = ibeacon.major.toString()
                minorView.text = ibeacon.minor.toString()
                uuidView.text = ibeacon.uuid.toString()
                majorView.visibility = View.VISIBLE
                minorView.visibility = View.VISIBLE
                majorLabelView.visibility = View.VISIBLE
                minorLabelView.visibility = View.VISIBLE
            } else {
                uuidView.text = "N/A"
                majorView.visibility = View.GONE
                minorView.visibility = View.GONE
                majorLabelView.visibility = View.GONE
                minorLabelView.visibility = View.GONE
            }

            val pulsesView = findViewById<TextView>(R.id.pulses)
            pulsesView.text = device!!.detectCount.toString()
        }
    }

    fun setDevice(device: XYBluetoothDevice?) {
        if (device != null) {
            device.removeListener(TAG)
        } else {
            XYBase.logError(TAG, "Setting NULL device")
        }

        this.device = device

        device!!.addListener(TAG, object : XYBluetoothDevice.Listener {
            override fun entered(device: XYBluetoothDevice) {

            }

            override fun exited(device: XYBluetoothDevice) {

            }

            override fun detected(device: XYBluetoothDevice) {
                update()
            }

            override fun connectionStateChanged(device: XYBluetoothDevice, newState: Int) {

            }
        })
        update()
    }

    companion object {

        private val TAG = XYDeviceItemView::class.java.simpleName
    }
}
