package com.xyfindables.sdk.sample

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView

import com.xyfindables.sdk.XYDevice
import com.xyfindables.sdk.XYSmartScan
import com.xyfindables.ui.XYBaseActivity
import com.xyfindables.ui.views.XYButton

/**
 * Created by arietrouw on 12/28/17.
 */

class XYDeviceActivity : XYBaseActivity() {

    private var _device: XYDevice? = null

    protected fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        val intent = getIntent()
        val deviceId = intent.getStringExtra(XYDeviceActivity.EXTRA_DEVICEID)
        _device = XYSmartScan.instance.deviceFromId(deviceId)
        if (_device == null) {
            showToast("Failed to Find Device")
            finish()

        }
        setContentView(R.layout.device_activity)
    }

    protected fun onPostCreate(savedInstanceState: Bundle) {
        super.onPostCreate(savedInstanceState)
        if (_device != null) {
            _device!!.addListener(TAG, object : XYDevice.Listener {
                override fun entered(device: XYDevice) {
                    update()
                    showToast("Entered")
                }

                override fun exited(device: XYDevice) {
                    update()
                    showToast("Exited")
                }

                override fun detected(device: XYDevice) {
                    update()
                }

                override fun buttonPressed(device: XYDevice, buttonType: XYDevice.ButtonType) {
                    update()
                    showToast("Button Pressed")
                }

                override fun buttonRecentlyPressed(device: XYDevice, buttonType: XYDevice.ButtonType) {

                }

                override fun connectionStateChanged(device: XYDevice, newState: Int) {
                    update()
                    if (_device!!.isConnected) {
                        showToast("Connected")
                    } else {
                        showToast("Disconnected")
                    }
                }

                override fun readRemoteRssi(device: XYDevice, rssi: Int) {
                    update()
                }

                override fun updated(device: XYDevice) {
                    update()
                }

                override fun statusChanged(status: XYSmartScan.Status) {

                }
            })
            update()
        }

        val connectButton = findViewById(R.id.connect)
        connectButton.setOnClickListener(View.OnClickListener {
            connectButton.setEnabled(false)
            _device!!.stayConnected(this@XYDeviceActivity, true)
        })

        val disconnectButton = findViewById(R.id.disconnect)
        disconnectButton.setOnClickListener(View.OnClickListener {
            connectButton.setEnabled(true)
            _device!!.stayConnected(this@XYDeviceActivity, false)
        })
    }

    protected fun onResume() {
        super.onResume()
    }

    fun update() {
        runOnUiThread(Runnable {
            if (_device != null) {
                val nameView = findViewById(R.id.name)
                nameView.setText(_device!!.family.name)

                val rssiView = findViewById(R.id.rssi)
                rssiView.setText(_device!!.rssi.toString())

                val majorView = findViewById(R.id.major)
                majorView.setText(_device!!.major.toString())

                val minorView = findViewById(R.id.minor)
                minorView.setText(_device!!.minor.toString())

                val pulsesView = findViewById(R.id.pulseCount)
                pulsesView.setText(_device!!.detectCount.toString())

                val enterView = findViewById(R.id.enterCount)
                enterView.setText(_device!!.enterCount.toString())

                val exitView = findViewById(R.id.exitCount)
                exitView.setText(_device!!.exitCount.toString())

                val actionSuccessView = findViewById(R.id.actionSuccessCount)
                actionSuccessView.setText(_device!!.actionSuccessCount.toString())

                val actionFailCount = findViewById(R.id.actionFailCount)
                actionFailCount.setText(_device!!.actionFailCount.toString())

                val actionQueueCount = findViewById(R.id.actionQueueCount)
                actionQueueCount.setText(_device!!.actionQueueCount.toString())

                val connectButton = findViewById(R.id.connect)
                connectButton.setVisibility(if (_device!!.isConnected) View.INVISIBLE else View.VISIBLE)

                val disconnectButton = findViewById(R.id.disconnect)
                disconnectButton.setVisibility(if (_device!!.isConnected) View.VISIBLE else View.INVISIBLE)
            }
        })
    }

    companion object {
        var EXTRA_DEVICEID = "DeviceId"
        private val TAG = XYDeviceActivity::class.java.simpleName
    }
}
