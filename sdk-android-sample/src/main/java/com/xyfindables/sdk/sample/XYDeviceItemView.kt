package com.xyfindables.sdk.sample

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView

import com.xyfindables.sdk.XYDevice
import com.xyfindables.sdk.XYSmartScan

/**
 * Created by arietrouw on 12/27/17.
 */

class XYDeviceItemView(context: Context, attrs: AttributeSet) : RelativeLayout(context, attrs) {

    private var _device: XYDevice? = null

    init {
        setOnClickListener {
            if (_device != null) {
                val intent = Intent(context, XYDeviceActivity::class.java)
                intent.putExtra(XYDeviceActivity.EXTRA_DEVICEID, _device!!.id)
                context.startActivity(intent)
            }
        }
    }

    fun update() {
        post {
            val nameView = findViewById<TextView>(R.id.name)
            nameView.text = _device!!.family.name

            val rssiView = findViewById<TextView>(R.id.rssi)
            rssiView.text = _device!!.rssi.toString()

            val majorView = findViewById<TextView>(R.id.major)
            majorView.text = _device!!.major.toString()

            val minorView = findViewById<TextView>(R.id.minor)
            minorView.text = _device!!.minor.toString()

            val pulsesView = findViewById<TextView>(R.id.pulses)
            pulsesView.text = _device!!.detectCount.toString()
        }
    }

    fun setDevice(device: XYDevice?) {
        if (_device != null) {
            _device!!.removeListener(TAG)
            _device = null
        }
        if (device != null) {
            _device = device
            _device!!.addListener(TAG, object : XYDevice.Listener {
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

                override fun statusChanged(status: XYSmartScan.Status) {

                }
            })
            update()
        }
    }

    companion object {

        private val TAG = XYDeviceItemView::class.java.simpleName
    }
}
