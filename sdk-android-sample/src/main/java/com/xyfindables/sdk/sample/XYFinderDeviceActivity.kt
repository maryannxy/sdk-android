package com.xyfindables.sdk.sample

import android.os.Bundle
import android.view.View
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import com.xyfindables.sdk.*
import com.xyfindables.sdk.devices.XYBluetoothDevice
import com.xyfindables.sdk.devices.XYFinderBluetoothDevice
import com.xyfindables.sdk.devices.XYIBeaconBluetoothDevice
import com.xyfindables.sdk.gatt.clients.XYBluetoothGatt

import com.xyfindables.ui.views.XYButton
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import kotlin.experimental.and

/**
 * Created by arietrouw on 12/28/17.
 */

class XYFinderDeviceActivity : XYAppBaseActivity() {

    private var device: XYBluetoothDevice? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = getIntent()
        val deviceId = intent.getStringExtra(XYFinderDeviceActivity.EXTRA_DEVICEID)
        logInfo("onCreate: $deviceId")
        device = scanner.devices[deviceId]
        if (device == null) {
            showToast("Failed to Find Device")
            finish()
        }
        setContentView(R.layout.device_activity)
        scanner.start()

        val listView: ListView? = findViewById(R.id.adList)
        val adapter: BaseAdapter? = XYDeviceAdapter(this)
        listView!!.adapter = adapter
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (device != null) {
            device!!.addListener(TAG, object : XYBluetoothDevice.Listener {
                override fun entered(device: XYBluetoothDevice) {
                    update()
                    showToast("Entered")
                }

                override fun exited(device: XYBluetoothDevice) {
                    update()
                    showToast("Exited")
                }

                override fun detected(device: XYBluetoothDevice) {
                    update()
                }

                override fun connectionStateChanged(device: XYBluetoothDevice, newState: Int) {
                    update()
                    if (newState == 0) {
                        showToast("Connected")
                    } else {
                        showToast("Disconnected")
                    }
                }
            })
            update()
        }

        val primaryBuzzerButton : XYButton = findViewById(R.id.primaryBuzzer)
        primaryBuzzerButton.setOnClickListener(View.OnClickListener {
            logInfo("primaryBuzzerButton: onClick")
            primaryBuzzerButton.setEnabled(false)
            logInfo("primaryBuzzerButton: got xyDevice")
            launch(CommonPool) {
                (device as? XYFinderBluetoothDevice)?.find()?.await()
                launch(UIThread) {
                    primaryBuzzerButton.setEnabled(true)
                }
            }
        })

        val stayAwakeButton : XYButton = findViewById(R.id.stay_awake)
        stayAwakeButton.setOnClickListener(View.OnClickListener {
            logInfo("stayAwakeButton: onClick")
            stayAwakeButton.setEnabled(false)
            //device!!.access {
                /*if (!writePrimaryStayAwake(1).await()) {
                    showToast("stayAwakeButton:writePrimaryStayAwake failed")
                }*/
                //updateStayAwakeEnabledStates()
            //}
        })

        val fallAsleepButton : XYButton = findViewById(R.id.fall_asleep)
        fallAsleepButton.setOnClickListener(View.OnClickListener {
            logInfo("fallAsleepButton: onClick")
            fallAsleepButton.setEnabled(false)
            //device!!.access {
                /*val xy4 = gatt as XY4Gatt
                if(!xy4.writePrimaryStayAwake(0).await()) {
                    showToast("fallAsleepButton:writePrimaryStayAwake failed")
                }*/
                //updateStayAwakeEnabledStates()
            //}
        })

        val lock : XYButton = findViewById(R.id.lock)
        lock.setOnClickListener(View.OnClickListener {
            logInfo("lock: onClick")

            //device!!.access {
                /*val xy4 = gatt as XY4Gatt
                if (!xy4.writePrimaryLock(xy4.defaultUnlockCode).await()) {
                    showToast("lockButton:writePrimaryLock failed")
                }*/
                //updateLockValue()
            //}
        })

        val unlock : XYButton = findViewById(R.id.unlock)
        unlock.setOnClickListener(View.OnClickListener {
            logInfo("unlock: onClick")
            //device!!.access {
                /*val xy4 = gatt as XY4Gatt
                if(!xy4.writePrimaryUnlock(xy4.defaultUnlockCode).await()) {
                    showToast("unlockButton:writePrimaryLock failed")
                }*/
                //updateLockValue()
            //}
        })
    }

    fun updateStayAwakeEnabledStates() {
        /*device!!.access {
            val xy4 = gatt as XY4Gatt
            val stayAwake = xy4.readPrimaryStayAwake().await()
            if (stayAwake == null) {
                showToast("updateStayAwakeEnabledStates:readPrimaryStayAwake failed")
            } else {
                launch(UIThread) {
                    val fallAsleepButton : XYButton = findViewById(R.id.fall_asleep)
                    val stayAwakeButton : XYButton = findViewById(R.id.stay_awake)
                    if (stayAwake != 0) {
                        fallAsleepButton.setEnabled(true)
                        stayAwakeButton.setEnabled(false)
                    } else {
                        fallAsleepButton.setEnabled(false)
                        stayAwakeButton.setEnabled(true)
                    }
                }
            }
        }*/
    }

    private val hexArray = "0123456789ABCDEF".toCharArray()

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = (bytes[j] and 0xFF.toByte()).toInt()

            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }

    fun updateLockValue() {
        /*device!!.access {
            val xy4 = gatt as XY4Gatt
            var lock = xy4.readPrimaryLock().await()
            if (lock == null) {
                if (!xy4.writePrimaryUnlock(xy4.defaultUnlockCode).await()) {
                    logInfo("updateLock: failed to unlock")
                    return@access
                }
            }

            lock = xy4.readPrimaryLock().await()
            if (lock == null) {
                logInfo("updateLock: lock is null even after unlock")
                return@access
            }

            logInfo("updateLock: ${lock.size}")
            launch(UIThread) {
                val lockEdit : EditText = findViewById(R.id.lock_value)
                launch(UIThread) {
                    lockEdit.setText(bytesToHex(lock))
                }
            }
        }*/
    }

    fun update() {
        launch(UIThread) {
            if (device != null) {
                val nameView : TextView = findViewById(R.id.family)
                nameView.setText(device!!.name)

                val rssiView : TextView = findViewById(R.id.rssi)
                rssiView.setText(device!!.rssi.toString())

                val iBeaconDevice = device as XYIBeaconBluetoothDevice?
                if (iBeaconDevice != null) {
                    val majorView: TextView = findViewById(R.id.major)
                    majorView.setText(iBeaconDevice.major.toString())

                    val minorView: TextView = findViewById(R.id.minor)
                    minorView.setText(iBeaconDevice.minor.toString())
                }

                val pulsesView : TextView = findViewById(R.id.pulseCount)
                pulsesView.setText(device!!.detectCount.toString())

                val enterView : TextView = findViewById(R.id.enterCount)
                enterView.setText(device!!.enterCount.toString())

                val exitView : TextView = findViewById(R.id.exitCount)
                exitView.setText(device!!.exitCount.toString())
            }
        }
        updateStayAwakeEnabledStates()
        updateLockValue()
    }

    companion object {
        var EXTRA_DEVICEID = "DeviceId"
        private val TAG = XYFinderDeviceActivity::class.java.simpleName
    }
}
