package com.xyfindables.sdk.sample

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.xyfindables.sdk.devices.*
import com.xyfindables.sdk.gatt.XYBluetoothResult
import com.xyfindables.ui.ui
import com.xyfindables.ui.views.XYButton
import com.xyfindables.ui.views.XYTextView
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import kotlin.experimental.and

/**
 * Created by arietrouw on 12/28/17.
 */

class XYFinderDeviceActivity : XYAppBaseActivity() {

    private var device: XYBluetoothDevice? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        val deviceHash = intent.getIntExtra(XYFinderDeviceActivity.EXTRA_DEVICEHASH, 0)
        logInfo("onCreate: $deviceHash")
        device = scanner.devices[deviceHash]
        if (device == null) {
            showToast("Failed to Find Device")
            finish()
        }
        setContentView(R.layout.device_activity)
    }

    override fun onStop() {
        super.onStop()
        device!!.removeListener(TAG)
    }

    fun updateAdList() {
        ui {
            val adList: XYTextView? = findViewById(R.id.adList)
            var txt = ""
            for ((_, ad) in device!!.ads) {
                txt = txt + ad.toString() + "\r\n"
            }
            adList?.text = txt
        }
    }

    val xy3DeviceListener = object : XY3BluetoothDevice.Listener() {
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
            if (newState == 2) {
                showToast("Connected")
                update()
            } else {
                showToast("Disconnected")
            }
        }

        override fun buttonSinglePressed() {
            showToast("Button Pressed: Single")
        }

        override fun buttonDoublePressed() {
            showToast("Button Pressed: Double")
        }

        override fun buttonLongPressed() {
            showToast("Button Pressed: Long")
        }
    }

    val xy4DeviceListener = object : XY4BluetoothDevice.Listener() {
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
            if (newState == 2) {
                showToast("Connected")
                update()
            } else {
                showToast("Disconnected")
            }
        }

        override fun buttonSinglePressed() {
            showToast("Button Pressed: Single")
        }

        override fun buttonDoublePressed() {
            showToast("Button Pressed: Double")
        }

        override fun buttonLongPressed() {
            showToast("Button Pressed: Long")
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (device != null) {
            if ((device as? XY4BluetoothDevice) != null) {
                device!!.addListener(TAG, xy4DeviceListener)
            } else {
                if ((device as? XY3BluetoothDevice) != null) {
                    device!!.addListener(TAG, xy3DeviceListener)
                }
            }
            update()
        }

        updateAdList()

        val beepButton : XYButton = findViewById(R.id.find)
        beepButton.setOnClickListener {
            logInfo("beepButton: onClick")
            ui {
                beepButton.isEnabled = false
            }
            logInfo("beepButton: got xyDevice")
            launch(CommonPool) {
                (device as? XYFinderBluetoothDevice)?.find()?.await()
                ui {
                    beepButton.isEnabled = true
                }
            }
        }

        val startTestButton : XYButton = findViewById(R.id.startTest)
        startTestButton.setOnClickListener {
            val xy4 = device as? XY4BluetoothDevice
            if (xy4 != null) {
                testXy4()
            } else {
                val xy3 = device as? XY3BluetoothDevice
                if (xy3 != null) {
                    testXy3()
                } else {

                }
            }
        }

        val stayAwakeButton : XYButton = findViewById(R.id.stay_awake)
        stayAwakeButton.setOnClickListener {
            launch(CommonPool) {
                logInfo("stayAwakeButton: onClick")
                ui {
                    stayAwakeButton.isEnabled = false
                }
                val stayAwake = (device as? XYFinderBluetoothDevice)?.stayAwake()
                if (stayAwake == null) {
                    showToast("Stay Awake Failed to Complete Call")
                } else {
                    showToast("Stay Awake Set")
                }
                ui {
                    stayAwakeButton.isEnabled = true
                }
            }
        }

        val fallAsleepButton : XYButton = findViewById(R.id.fall_asleep)
        fallAsleepButton.setOnClickListener {
            launch(CommonPool) {
                logInfo("fallAsleepButton: onClick")
                ui {
                    fallAsleepButton.isEnabled = false
                }
                val fallAsleep = (device as? XYFinderBluetoothDevice)?.fallAsleep()
                if (fallAsleep == null) {
                    showToast("Fall Asleep Failed to Complete Call")
                } else {
                    showToast("Fall Asleep Set")
                }
                ui {
                    fallAsleepButton.isEnabled = true
                }
            }
        }

        val lockButton : XYButton = findViewById(R.id.lock)
        lockButton.setOnClickListener {
            logInfo("lockButton: onClick")

            ui {
                lockButton.isEnabled = false
            }
            logInfo("primaryBuzzerButton: got xyDevice")
            launch(CommonPool) {
                val locked = (device as? XYFinderBluetoothDevice)?.lock()?.await()
                when {
                    locked == null -> showToast("Device does not support Lock")
                    locked.error == null -> {
                        showToast(locked.toString())
                        updateStayAwakeEnabledStates()
                    }
                    else -> showToast("Lock Error: ${locked.error}")
                }
                ui {
                    lockButton.isEnabled = true
                }
            }
        }

        val unlockButton : XYButton = findViewById(R.id.unlock)
        unlockButton.setOnClickListener(View.OnClickListener {
            logInfo("unlockButton: onClick")

            ui {
                unlockButton.isEnabled = false
            }
            launch(CommonPool) {
                val unlocked = (device as? XYFinderBluetoothDevice)?.unlock()?.await()
                when {
                    unlocked == null -> showToast("Device does not support Unlock")
                    unlocked.error == null -> {
                        showToast("Unlocked: ${unlocked}")
                        updateStayAwakeEnabledStates()
                    }
                    else -> showToast("Unlock Error: ${unlocked.error}")
                }
                ui {
                    unlockButton.isEnabled = true
                }
            }
        })
        //readUpdates()
        //enableButtonNotify()
    }

    fun enableButtonNotify() : Deferred<Unit> {
        return async (CommonPool) {
            logInfo("enableButtonNotify")
            val xy4 = device as? XY4BluetoothDevice
            if (xy4 != null) {
                val notify = xy4.primary.buttonState.enableNotify(true).await()
                showToast(notify.toString())
            } else {
                val xy3 = device as? XY3BluetoothDevice
                if (xy3 != null) {
                    val notify = xy3.control.button.enableNotify(true).await()
                    showToast(notify.toString())
                }
            }
            return@async
        }
    }

    private fun updateStayAwakeEnabledStates() : Deferred<Unit> {
        return async (CommonPool) {
            logInfo("updateStayAwakeEnabledStates")
            val xy4 = device as? XY4BluetoothDevice
            if (xy4 != null) {
                val stayAwake = xy4.primary.stayAwake.get().await()
                logInfo("updateStayAwakeEnabledStates: $stayAwake")
                ui {
                    val fallAsleepButton: XYButton = findViewById(R.id.fall_asleep)
                    val stayAwakeButton: XYButton = findViewById(R.id.stay_awake)
                    if (stayAwake.value != 0) {
                        fallAsleepButton.isEnabled = true
                        stayAwakeButton.isEnabled = false
                    } else {
                        fallAsleepButton.isEnabled = false
                        stayAwakeButton.isEnabled = true
                    }
                }
            } else {
                logError("updateStayAwakeEnabledStates: Not an XY4!", false)
            }
            return@async
        }
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

    //it is possible that reading the lock value is not implemented in the firmware
    private fun updateLockValue() : Deferred<Unit>{
        return async (CommonPool) {
            logInfo("updateLockValue")
            val xy4 = device as? XY4BluetoothDevice
            if (xy4 != null) {
                val lock = xy4.primary.lock.get().await()

                logInfo("updateLock: ${lock}")
                ui {
                    val lockEdit: EditText = findViewById(R.id.lock_value)
                    if (lock.error != null || lock.error != null) {
                        lockEdit.setText("----")
                    } else {
                        lockEdit.setText(bytesToHex(lock.value!!))
                    }
                }
            }
        }
    }

    fun update() {
        ui {
            logInfo("update")
            if (device != null) {
                val nameView : TextView = findViewById(R.id.family)
                nameView.text = device!!.name

                val rssiView : TextView = findViewById(R.id.rssi)
                rssiView.text = device!!.rssi.toString()

                val iBeaconDevice = device as XYIBeaconBluetoothDevice?
                if (iBeaconDevice != null) {
                    val majorView: TextView = findViewById(R.id.major)
                    majorView.text = iBeaconDevice.major.toString()

                    val minorView: TextView = findViewById(R.id.minor)
                    minorView.text = iBeaconDevice.minor.toString()
                }

                val pulsesView : TextView = findViewById(R.id.pulseCount)
                pulsesView.text = device!!.detectCount.toString()

                val enterView : TextView = findViewById(R.id.enterCount)
                enterView.text = device!!.enterCount.toString()

                val exitView : TextView = findViewById(R.id.exitCount)
                exitView.text = device!!.exitCount.toString()
            }
        }
    }

    private fun readUpdates() {
        launch(CommonPool) {
            updateStayAwakeEnabledStates().await()
            updateLockValue().await()
            update()
        }
    }

    private fun testXy4() {
        //TODO - disable btn, show progress
        logInfo("textXy4")
        launch{
            val xy4 = device as? XY4BluetoothDevice
            xy4?.connection {
                for (i in 0..10000) {
                    val text = "Hello+$i"
                    val write = xy4.primary.lock.set(XY4BluetoothDevice.DEFAULT_LOCK_CODE).await()
                    if (write.error == null) {
                        logInfo("testXy4: Success: $text")
                    } else {
                        logInfo("testXy4: Fail: $text : ${write.error}")
                    }
                }
                return@connection XYBluetoothResult(true)
            }
        }
    }

    private fun testXy3() {

    }

    private fun testXy2() {

    }

    private fun testXyGps() {

    }

    companion object {
        var EXTRA_DEVICEHASH = "DeviceHash"
        private val TAG = XYFinderDeviceActivity::class.java.simpleName
    }
}
