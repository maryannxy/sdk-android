package com.xyfindables.sdk.scanner

import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.SystemClock
import com.xyfindables.core.XYBase
import com.xyfindables.sdk.UIThread
import com.xyfindables.sdk.devices.XYBluetoothDevice
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import java.lang.ref.WeakReference
import java.util.HashMap

abstract class XYFilteredSmartScan(context: Context): XYBase() {

    //we want to use the application context for everything
    protected val context = context.applicationContext

    var startTime = 0L
    var scanResultCount = 0

    val resultsPerSecond: Float
        get() {
            if (startTime == 0L) {
                return 0F
            }
            return scanResultCount / (uptimeSeconds)
        }

    val now: Long
        get() {
            return SystemClock.uptimeMillis()
        }

    val uptime: Long
        get() {
            if (startTime == 0L) {
                return 0
            } else {
                return now - startTime
            }
        }

    val uptimeSeconds: Float
        get() {
            return uptime/1000F
        }

    val devices = HashMap<String, XYBluetoothDevice>()

    fun deviceFromScanResult(scanResult: XYScanResult) : XYBluetoothDevice? {
        var device : XYBluetoothDevice? = null
        synchronized(devices) {
            device = devices[scanResult.deviceId]
            if (device == null) {
                device = XYBluetoothDevice.fromScanResult(context, scanResult)
                //the device will come back null if the device parser for that type of device
                //is disabled
                if (device != null) {
                    devices.set(device!!.id, device!!)
                }
            }
        }
        return device
    }

    private val listeners = HashMap<String, WeakReference<Listener>>()

    interface Listener : XYBluetoothDevice.Listener {
        fun statusChanged(status: BluetoothStatus)
    }

    enum class BluetoothStatus {
        None,
        Enabled,
        BluetoothUnavailable,
        BluetoothUnstable,
        BluetoothDisabled,
        LocationDisabled
    }

    private var _background = false
    var background: Boolean
        get() {
            return _background
        }
        set(background) {
            _background = background
        }

    open fun start() {
        logInfo("start")
        startTime = now
    }

    open fun stop() {
        logInfo("stop")
        startTime = 0
        scanResultCount = 0
    }

    fun addListener(key: String, listener: Listener) {
        logInfo("addListener")
        launch(CommonPool){
            synchronized(listeners) {
                listeners.put(key, WeakReference(listener))
            }
        }
    }

    fun removeListener(key: String) {
        logInfo("removeListener")
        launch(CommonPool){
            synchronized(listeners) {
                listeners.remove(key)
            }
        }
    }

    protected fun onScanResult(scanResults: List<XYScanResult>): List<XYScanResult> {
        scanResultCount++
        for (scanResult in scanResults) {
            val device = deviceFromScanResult(scanResult)
            if (device != null) {
                if (scanResult.scanRecord != null) {
                    device.updateAds(scanResult.scanRecord!!)
                }
                if (device.rssi == -999) {
                    reportEntered(device)
                    device.onEnter()
                }
                device.rssi = scanResult.rssi
                reportDetected(device)
                device.onDetect()
            }
        }
        return scanResults
    }

    protected fun reportEntered(device: XYBluetoothDevice) {
        logInfo("reportEntered")
        synchronized(listeners) {
            for ((_, listener) in listeners) {
                val innerListener = listener.get()
                if (innerListener != null) {
                    launch(CommonPool) {
                        innerListener.entered(device)
                    }
                }
            }
        }
    }

    protected fun reportExited(device: XYBluetoothDevice) {
        logInfo("reportExited")
        synchronized(listeners) {
            for ((_, listener) in listeners) {
                val innerListener = listener.get()
                if (innerListener != null) {
                    launch(CommonPool) {
                        innerListener.exited(device)
                    }
                }
            }
        }
    }

    protected fun reportDetected(device: XYBluetoothDevice) {
        //logInfo("reportDetected")
        synchronized(listeners) {
            for ((_, listener) in listeners) {
                val innerListener = listener.get()
                if (innerListener != null) {
                    launch(CommonPool) {
                        innerListener.detected(device)
                    }
                }
            }
        }
    }

    protected fun getBluetoothManager(context: Context): BluetoothManager {
        return context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    companion object {
        //this is the thread that all calls should happen on for gatt calls.  Using UIThread
        //for now since that is needed for 4.4, but should allow non-ui thread for later
        //versions
        val BluetoothThread = newFixedThreadPoolContext(1, "BluetoothThread") //UIThread
    }

}