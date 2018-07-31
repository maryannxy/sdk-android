package com.xyfindables.sdk.sample

import android.os.Bundle
import com.xyfindables.sdk.devices.*
import com.xyfindables.sdk.gatt.XYBluetoothResult
import com.xyfindables.ui.ui
import com.xyfindables.ui.views.XYTextView
import kotlinx.android.synthetic.main.device_activity.*
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

    private fun updateAdList() {
        ui {
            val adList: XYTextView? = findViewById(R.id.adList)
            var txt = ""
            for ((_, ad) in device!!.ads) {
                txt = txt + ad.toString() + "\r\n"
            }
            adList?.text = txt
        }
    }

    private val xy3DeviceListener = object : XY3BluetoothDevice.Listener() {
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

    private val xy4DeviceListener = object : XY4BluetoothDevice.Listener() {
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

        override fun buttonSinglePressed(device: XYFinderBluetoothDevice) {
            showToast("Button Pressed: Single")
        }

        override fun buttonDoublePressed(device: XYFinderBluetoothDevice) {
            showToast("Button Pressed: Double")
        }

        override fun buttonLongPressed(device: XYFinderBluetoothDevice) {
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

        find.setOnClickListener {
            logInfo("beepButton: onClick")
            ui {
                find.isEnabled = false
            }
            logInfo("beepButton: got xyDevice")
            launch(CommonPool) {
                (device as? XYFinderBluetoothDevice)?.find()?.await()
                ui {
                    find.isEnabled = true
                }
            }
        }

        startTest.setOnClickListener {
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

        stay_awake.setOnClickListener {
            launch(CommonPool) {
                logInfo("stayAwakeButton: onClick")
                ui {
                    stay_awake.isEnabled = false
                }
                val stayAwake = (device as? XYFinderBluetoothDevice)?.stayAwake()
                if (stayAwake == null) {
                    showToast("Stay Awake Failed to Complete Call")
                } else {
                    showToast("Stay Awake Set")
                }
                ui {
                    stay_awake.isEnabled = true
                }
            }
        }

        fall_asleep.setOnClickListener {
            launch(CommonPool) {
                logInfo("fallAsleepButton: onClick")
                ui {
                    fall_asleep.isEnabled = false
                }
                val fallAsleep = (device as? XYFinderBluetoothDevice)?.fallAsleep()
                if (fallAsleep == null) {
                    showToast("Fall Asleep Failed to Complete Call")
                } else {
                    showToast("Fall Asleep Set")
                }
                ui {
                    fall_asleep.isEnabled = true
                }
            }
        }

        lock.setOnClickListener {
            logInfo("lockButton: onClick")

            ui {
                lock.isEnabled = false
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
                    lock.isEnabled = true
                }
            }
        }

        unlock.setOnClickListener{
            logInfo("unlockButton: onClick")

            ui {
                unlock.isEnabled = false
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
                    unlock.isEnabled = true
                }
            }
        }
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
                    if (stayAwake.value != 0) {
                        fall_asleep.isEnabled = true
                        stay_awake.isEnabled = false
                    } else {
                        fall_asleep.isEnabled = false
                        stay_awake.isEnabled = true
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
                    if (lock.error != null || lock.error != null) {
                        lock_value.setText("----")
                    } else {
                        lock_value.setText(bytesToHex(lock.value!!))
                    }
                }
            }
        }
    }

    fun update() {
        ui {
            logInfo("update")
            if (device != null) {
                family.text = device!!.name
                rssi.text = device!!.rssi.toString()

                val iBeaconDevice = device as XYIBeaconBluetoothDevice?
                if (iBeaconDevice != null) {
                    major.text = iBeaconDevice.major.toString()
                    minor.text = iBeaconDevice.minor.toString()
                }

                pulseCount.text = device!!.detectCount.toString()
                enterCount.text = device!!.enterCount.toString()
                exitCount.text = device!!.exitCount.toString()
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
