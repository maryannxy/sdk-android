package com.xyfindables.sdk.sample.views

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import com.xyfindables.sdk.devices.XYBluetoothDevice
import com.xyfindables.sdk.sample.R
import com.xyfindables.sdk.sample.XYApplication

import com.xyfindables.sdk.scanner.XYFilteredSmartScan

/**
 * Created by arietrouw on 12/28/17.
 */

class XYBLEStatsView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    private val scanner : XYFilteredSmartScan

    private val smartScanListener = object : XYFilteredSmartScan.Listener() {
        override fun entered(device: XYBluetoothDevice) {
            update()
        }

        override fun exited(device: XYBluetoothDevice) {
            update()
        }

        override fun detected(device: XYBluetoothDevice) {
            update()
        }

        override fun connectionStateChanged(device: XYBluetoothDevice, newState: Int) {

        }

        override fun statusChanged(status: XYFilteredSmartScan.BluetoothStatus) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    init {
        scanner = (context.applicationContext as XYApplication).scanner
        scanner.addListener(TAG, smartScanListener)
    }

    fun update() {
        post {
            val uptimeView = findViewById<TextView>(R.id.text_uptime)!!
            uptimeView.text = ("%.2f").format(scanner.uptimeSeconds)

            val pulseCountView = findViewById<TextView>(R.id.text_pulses)!!
            pulseCountView.text = scanner.scanResultCount.toString()

            val pulsePerSecView = findViewById<TextView>(R.id.text_pulses_per_second)!!
            pulsePerSecView.text = ("%.2f").format(scanner.resultsPerSecond)

            val devices = findViewById<TextView>(R.id.text_devices)
            devices.text = scanner.devices.size.toString()
        }
    }

    companion object {
        private val TAG = XYBLEStatsView::class.java.simpleName
    }
}