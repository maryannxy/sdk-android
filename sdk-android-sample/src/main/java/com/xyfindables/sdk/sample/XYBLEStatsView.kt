package com.xyfindables.sdk.sample

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

import com.xyfindables.sdk.XYDevice
import com.xyfindables.sdk.XYSmartScan

/**
 * Created by arietrouw on 12/28/17.
 */

class XYBLEStatsView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    init {
        XYSmartScan.instance.addListener(TAG, object : XYDevice.Listener {
            override fun entered(device: XYDevice) {
                update()
            }

            override fun exited(device: XYDevice) {
                update()
            }

            override fun detected(device: XYDevice) {
                update()
            }

            override fun buttonPressed(device: XYDevice, buttonType: XYDevice.ButtonType) {

            }

            override fun buttonRecentlyPressed(device: XYDevice, buttonType: XYDevice.ButtonType) {

            }

            override fun statusChanged(status: XYSmartScan.Status) {

            }

            override fun updated(device: XYDevice) {
                update()
            }

            override fun connectionStateChanged(device: XYDevice, newState: Int) {

            }

            override fun readRemoteRssi(device: XYDevice, rssi: Int) {

            }

        })
    }

    fun update() {
        post {
            val pulseCountView = findViewById<TextView>(R.id.pulses)!!
            pulseCountView.text = XYSmartScan.instance.pulseCount.toString()

            val pulsePerSecView = findViewById<TextView>(R.id.pulsesPerSecond)!!
            pulsePerSecView.text = XYSmartScan.instance.pulsesPerSecond.toString()

            val devices = findViewById<TextView>(R.id.devices)
            devices.text = XYSmartScan.instance.deviceCount.toString()
        }
    }

    companion object {
        private val TAG = XYBLEStatsView::class.java.simpleName
    }
}
