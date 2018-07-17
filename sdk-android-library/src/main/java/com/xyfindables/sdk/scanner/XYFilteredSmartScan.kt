package com.xyfindables.sdk.scanner

import android.bluetooth.BluetoothManager
import android.content.Context
import com.xyfindables.core.XYBase
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

    val devices = HashMap<Int, XYBluetoothDevice>()

    fun addDevicesFromScanResult(scanResult: XYScanResult, devices: HashMap<Int, XYBluetoothDevice>) {
        val hash = XYBluetoothDevice.hashFromScanResult(scanResult)
        synchronized(devices) {
            if (hash != null) {
                //only add them if they do not already exist
                val device = this.devices[hash]
                if (device == null) {
                    XYBluetoothDevice.addDevicesFromScanResult(context, scanResult, devices)
                }
            }
        }
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

    private var handleDeviceNotifyExit = fun(device: XYBluetoothDevice) {
        devices.remove(device.hashCode())
        reportExited(device)
    }

    protected fun onScanResult(scanResults: List<XYScanResult>): List<XYScanResult> {
        scanResultCount += scanResults.size
        val devices = HashMap<Int, XYBluetoothDevice>()
        for (scanResult in scanResults) {
            addDevicesFromScanResult(scanResult, devices)
            logInfo("onScanResult: ${devices.size}")
            if (devices.size > 0) {
                for ((_, device) in devices) {
                    var currentDevice = this.devices[device.hashCode()]
                    if (currentDevice == null) {
                        currentDevice = device
                        this.devices[device.hashCode()] = device
                    } else {
                        currentDevice.updateBluetoothDevice(scanResult.device)
                    }
                    if (scanResult.scanRecord != null) {
                        currentDevice.updateAds(scanResult.scanRecord!!)
                    }
                    if (currentDevice.rssi == -999) {
                        reportEntered(device)
                        currentDevice.onEnter()
                        currentDevice.notifyExit = handleDeviceNotifyExit
                    }
                    currentDevice.rssi = scanResult.rssi
                    reportDetected(currentDevice)
                    currentDevice.onDetect()
                }
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
        //this is the thread that all calls should happen on for gatt calls.
        val BluetoothThread = newFixedThreadPoolContext(1, "BluetoothThread")
    }

}