package com.xyfindables.sdk.sample

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.xyfindables.core.guard
import com.xyfindables.sdk.*
import com.xyfindables.sdk.devices.XY4BluetoothDevice
import com.xyfindables.sdk.devices.XYBluetoothDevice
import com.xyfindables.sdk.devices.XYFinderBluetoothDevice
import com.xyfindables.sdk.devices.XYIBeaconBluetoothDevice

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
    }

    fun updateAdList() {
        launch(UIThread) {
            val adList: XYTextView? = findViewById(R.id.adList)
            var txt = ""
            for ((_, ad) in device!!.ads) {
                txt = txt + ad.toString() + "\r\n"
            }
            adList?.text = txt
        }
    }

    val deviceListener = object : XYBluetoothDevice.Listener {
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
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (device != null) {
            device!!.addListener(TAG, deviceListener)
            update()
        }

        updateAdList()

        val primaryBuzzerButton : XYButton = findViewById(R.id.primaryBuzzer)
        primaryBuzzerButton.setOnClickListener(View.OnClickListener {
            logInfo("primaryBuzzerButton: onClick")
            launch(UIThread) {
                primaryBuzzerButton.setEnabled(false)
            }
            logInfo("primaryBuzzerButton: got xyDevice")
            launch(CommonPool) {
                (device as? XYFinderBluetoothDevice)?.find()?.await()
                launch(UIThread) {
                    primaryBuzzerButton.setEnabled(true)
                }
            }
        })

        val stayAwakeButton : XYButton = findViewById(R.id.stay_awake)
        stayAwakeButton.setOnClickListener {
            launch(CommonPool) {
                logInfo("stayAwakeButton: onClick")
                launch(UIThread) {
                    stayAwakeButton.setEnabled(false)
                }
                val xy4 = device as? XY4BluetoothDevice
                if (xy4 != null) {
                    if (!xy4.writePrimaryStayAwake(1).await()!!) {
                        showToast("stayAwakeButton:writePrimaryStayAwake failed")
                    }
                    updateStayAwakeEnabledStates()
                }
            }
        }

        val fallAsleepButton : XYButton = findViewById(R.id.fall_asleep)
        fallAsleepButton.setOnClickListener {
            launch(CommonPool) {
                logInfo("fallAsleepButton: onClick")
                launch(UIThread) {
                    fallAsleepButton.setEnabled(false)
                }
                val xy4 = device as? XY4BluetoothDevice
                if (xy4 != null) {
                    if (!xy4.writePrimaryStayAwake(0).await()!!) {
                        showToast("fallAsleepButton:writePrimaryStayAwake failed")
                    }
                    updateStayAwakeEnabledStates()
                }
            }
        }

        val lockButton : XYButton = findViewById(R.id.lock)
        lockButton.setOnClickListener(View.OnClickListener {
            logInfo("lockButton: onClick")

            launch(UIThread) {
                lockButton.setEnabled(false)
            }
            logInfo("primaryBuzzerButton: got xyDevice")
            launch(CommonPool) {
                val locked = (device as? XYFinderBluetoothDevice)?.lock(XY4BluetoothDevice.DEFAULT_LOCK_CODE)?.await()
                if (locked == null) {
                    showToast("Lock Failed to Complete Call")
                } else if (locked){
                    showToast("Locked")
                } else if (locked){
                    showToast("Lock Failed")
                }
                launch(UIThread) {
                    lockButton.setEnabled(true)
                }
            }
        })

        val unlockButton : XYButton = findViewById(R.id.unlock)
        unlockButton.setOnClickListener(View.OnClickListener {
            logInfo("unlockButton: onClick")

            launch(UIThread) {
                unlockButton.setEnabled(false)
            }
            logInfo("primaryBuzzerButton: got xyDevice")
            launch(CommonPool) {
                val unlocked = (device as? XYFinderBluetoothDevice)?.unlock(XY4BluetoothDevice.DEFAULT_LOCK_CODE)?.await()
                if (unlocked == null) {
                    showToast("Unlock Failed to Complete Call")
                } else if (unlocked){
                    showToast("Unlocked")
                } else if (unlocked){
                    showToast("Unlock Failed")
                }
                launch(UIThread) {
                    unlockButton.setEnabled(true)
                }
            }
        })
        update()
    }

    fun updateStayAwakeEnabledStates() : Deferred<Unit> {
        return async (CommonPool) {
            logInfo("updateStayAwakeEnabledStates")
            val xy4 = device as? XY4BluetoothDevice
            if (xy4 != null) {
                val stayAwake = xy4.readPrimaryStayAwake().await()
                if (stayAwake == null) {
                    logError("updateStayAwakeEnabledStates: readPrimaryStayAwake failed", false)
                } else {
                    logInfo("updateStayAwakeEnabledStates: $stayAwake")
                    launch(UIThread) {
                        val fallAsleepButton: XYButton = findViewById(R.id.fall_asleep)
                        val stayAwakeButton: XYButton = findViewById(R.id.stay_awake)
                        if (stayAwake != 0) {
                            fallAsleepButton.setEnabled(true)
                            stayAwakeButton.setEnabled(false)
                        } else {
                            fallAsleepButton.setEnabled(false)
                            stayAwakeButton.setEnabled(true)
                        }
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
    fun updateLockValue() : Deferred<Unit>{
        return async (CommonPool) {
            logInfo("updateLockValue")
            val xy4 = device as? XY4BluetoothDevice
            if (xy4 != null) {
                var lock = xy4.readPrimaryLock().await()
                if (lock == null) {
                    logInfo("updateLock: read failed to complete, trying to unlock")
                    val result = xy4.writePrimaryUnlock(XY4BluetoothDevice.DEFAULT_LOCK_CODE).await()
                    if (result == null) {
                        logError("updateLock: failed complete call", false)
                        return@async
                    }
                    else if (!result) {
                        logError("updateLock: failed to unlock", false)
                        return@async
                    } else {
                        logInfo("updateLock: trying to read again")
                        lock = xy4.readPrimaryLock().await()
                    }
                }

                if (lock == null) {
                    logInfo("updateLock: lock is null even after unlock")
                    return@async
                }

                logInfo("updateLock: ${lock.size}")
                launch(UIThread) {
                    val lockEdit: EditText = findViewById(R.id.lock_value)
                    lockEdit.setText(bytesToHex(lock))
                }
            }
        }
    }

    fun update() {
        launch(UIThread) {
            logInfo("update")
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
        launch(CommonPool) {
            updateStayAwakeEnabledStates().await()
            //updateLockValue().await()
        }
    }

    companion object {
        var EXTRA_DEVICEID = "DeviceId"
        private val TAG = XYFinderDeviceActivity::class.java.simpleName
    }
}
