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

    fun deviceFromId(id:String) : XYBluetoothDevice? {
        return null
    }

    fun getDevicesFromScanResult(scanResult: XYScanResult, globalDevices: HashMap<Int, XYBluetoothDevice>, foundDevices: HashMap<Int, XYBluetoothDevice>) {
        //only add them if they do not already exist
        XYBluetoothDevice.getDevicesFromScanResult(context, scanResult, globalDevices, foundDevices)

        //add (or replace) all the found devices
        for ((_, foundDevice) in foundDevices) {
            globalDevices[foundDevice.hashCode()] = foundDevice
        }
    }

    private val listeners = HashMap<String, WeakReference<Listener>>()

    open class Listener : XYBluetoothDevice.Listener() {
        open fun statusChanged(status: BluetoothStatus) {

        }

        open fun buttonPressed(device: XYBluetoothDevice) {

        }
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
        for (scanResult in scanResults) {
            val foundDevices = HashMap<Int, XYBluetoothDevice>()
            getDevicesFromScanResult(scanResult, this.devices, foundDevices)
            this.devices.putAll(foundDevices)
            if (foundDevices.size > 0) {
                for ((_, device) in foundDevices) {
                    this.devices[device.hashCode()] = device
                    device.updateBluetoothDevice(scanResult.device)

                    if (scanResult.scanRecord != null) {
                        device.updateAds(scanResult.scanRecord!!)
                    }
                    if (device.rssi == XYBluetoothDevice.OUTOFRANGE_RSSI) {
                        reportEntered(device)
                        device.onEnter()
                        device.notifyExit = handleDeviceNotifyExit
                    }
                    device.rssi = scanResult.rssi
                    reportDetected(device)
                    device.onDetect()
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