package com.xyfindables.sdk.sample

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import com.xyfindables.sdk.*

import com.xyfindables.ui.XYBaseActivity
import com.xyfindables.ui.views.XYButton
import com.xyfindables.ui.views.XYEditText
import com.xyfindables.ui.views.XYTextView
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch

/**
 * Created by arietrouw on 12/28/17.
 */

class XYDeviceActivity : XYBaseActivity() {

    private var device: XYDevice? = null
    private var bluetoothDevice : XYBluetoothDevice? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = getIntent()
        val deviceId = intent.getStringExtra(XYDeviceActivity.EXTRA_DEVICEID)
        device = XYSmartScan.instance.deviceFromId(deviceId)
        if (device == null) {
            showToast("Failed to Find Device")
            finish()

        }
        setContentView(R.layout.device_activity)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (device != null) {
            device!!.addListener(TAG, object : XYDevice.Listener {
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
                    if (device.isConnected) {
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

        val primaryBuzzerButton : XYButton = findViewById(R.id.primaryBuzzer)
        primaryBuzzerButton.setOnClickListener(View.OnClickListener {
            logInfo("connectButton: onClick")
            primaryBuzzerButton.setEnabled(false)
            bluetoothDevice = XYBluetoothDevice(this, device!!.bluetoothDevice!!)
            logInfo("connectButton: got xyDevice")
            bluetoothDevice!!.access(this) {
                gatt: XYBluetoothGatt? ->
                if (gatt != null) {
                    val xy4 = gatt as XY4Gatt
                    for (i in 0..8) {
                        xy4.primaryBuzzer(i).await()
                    }
                    launch(UIThread) {
                        primaryBuzzerButton.setEnabled(true)
                    }
                }
            }
        })

        val disconnectButton : XYButton = findViewById(R.id.disconnect)
        disconnectButton.setOnClickListener(View.OnClickListener {
            primaryBuzzerButton.setEnabled(true)
        })
    }

    override fun onResume() {
        super.onResume()
    }

    fun update() {
        runOnUiThread(Runnable {
            if (device != null) {
                val nameView : TextView = findViewById(R.id.family)
                nameView.setText(device!!.family.name)

                val rssiView : TextView = findViewById(R.id.rssi)
                rssiView.setText(device!!.rssi.toString())

                val majorView : TextView = findViewById(R.id.major)
                majorView.setText(device!!.major.toString())

                val minorView : TextView = findViewById(R.id.minor)
                minorView.setText(device!!.minor.toString())

                val pulsesView : TextView = findViewById(R.id.pulseCount)
                pulsesView.setText(device!!.detectCount.toString())

                val enterView : TextView = findViewById(R.id.enterCount)
                enterView.setText(device!!.enterCount.toString())

                val exitView : TextView = findViewById(R.id.exitCount)
                exitView.setText(device!!.exitCount.toString())

                val actionSuccessView : TextView = findViewById(R.id.actionSuccessCount)
                actionSuccessView.setText(device!!.actionSuccessCount.toString())

                val actionFailCount : TextView = findViewById(R.id.actionFailCount)
                actionFailCount.setText(device!!.actionFailCount.toString())

                val actionQueueCount : TextView = findViewById(R.id.actionQueueCount)
                actionQueueCount.setText(device!!.actionQueueCount.toString())
            }
        })
    }

    companion object {
        var EXTRA_DEVICEID = "DeviceId"
        private val TAG = XYDeviceActivity::class.java.simpleName
    }
}
