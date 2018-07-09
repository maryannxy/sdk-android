package com.xyfindables.sdk.sample

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.widget.RelativeLayout
import android.widget.TextView
import com.xyfindables.core.XYBase

import com.xyfindables.sdk.XYDevice

/**
 * Created by arietrouw on 12/27/17.
 */

class XYDeviceItemView(context: Context, attrs: AttributeSet) : RelativeLayout(context, attrs) {

    private var device: XYDevice? = null

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
            val nameView = findViewById<TextView>(R.id.name)
            nameView.text = device!!.family.name

            val rssiView = findViewById<TextView>(R.id.rssi)
            rssiView.text = device!!.rssi.toString()

            val majorView = findViewById<TextView>(R.id.major)
            majorView.text = device!!.major.toString()

            val minorView = findViewById<TextView>(R.id.minor)
            minorView.text = device!!.minor.toString()

            val pulsesView = findViewById<TextView>(R.id.pulses)
            pulsesView.text = device!!.detectCount.toString()
        }
    }

    fun setDevice(device: XYDevice?) {
        if (device != null) {
            device.removeListener(TAG)
        } else {
            XYBase.logError(TAG, "Setting NULL device")
        }

        this.device = device

        device!!.addListener(TAG, object : XYDevice.Listener {
            override fun entered(device: XYDevice) {

            }

            override fun exited(device: XYDevice) {

            }

            override fun detected(device: XYDevice) {
                update()
            }

            override fun buttonPressed(device: XYDevice, buttonType: XYDevice.ButtonType) {

            }

            override fun buttonRecentlyPressed(device: XYDevice, buttonType: XYDevice.ButtonType) {

            }

            override fun connectionStateChanged(device: XYDevice, newState: Int) {

            }

            override fun readRemoteRssi(device: XYDevice, rssi: Int) {
                update()
            }

            override fun updated(device: XYDevice) {

            }

            override fun statusChanged(status: XYDevice.BluetoothStatus) {

            }
        })
        update()
    }

    companion object {

        private val TAG = XYDeviceItemView::class.java.simpleName
    }
}
