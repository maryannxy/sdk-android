package com.xyfindables.sdk.scanner

import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.SystemClock
import android.util.LongSparseArray
import com.xyfindables.core.XYBase
import com.xyfindables.sdk.XYBluetoothDevice
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import java.util.HashMap

abstract class XYFilteredSmartScan(context: Context): XYBase() {

    //we want to use the application context for everything
    protected val context = context.applicationContext

    var startTime = 0L
    var scanResultCount = 0

    val resultsPerSecond: Float
        get() {
            return scanResultCount / (startTime/1000F)
        }

    val devices = HashMap<Long, XYBluetoothDevice>()

    private val listeners = HashMap<String, Listener>()

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
        startTime = SystemClock.currentThreadTimeMillis()
    }

    open fun stop() {
        startTime = 0
        scanResultCount = 0
    }

    fun addListener(key: String, listener: Listener) {
        launch(CommonPool){
            synchronized(listeners) {
                listeners.put(key, listener)
            }
        }
    }

    fun removeListener(key: String) {
        launch(CommonPool){
            synchronized(listeners) {
                listeners.remove(key)
            }
        }
    }

    protected fun onScanResult(scanResults: List<XYScanResult>): List<XYScanResult> {
        scanResultCount++
        for (scanResult in scanResults) {
            devices[scanResult.deviceId]!!.rssi = scanResult.rssi
        }
        return scanResults
    }

    protected fun reportEntered(device: XYBluetoothDevice) {
        synchronized(listeners) {
            for ((_, listener) in listeners) {
                launch (CommonPool) {
                    listener.entered(device)
                }
            }
        }
    }

    protected fun reportExited(device: XYBluetoothDevice) {
        synchronized(listeners) {
            for ((_, listener) in listeners) {
                launch (CommonPool) {
                    listener.exited(device)
                }
            }
        }
    }

    protected fun reportDetected(device: XYBluetoothDevice) {
        synchronized(listeners) {
            for ((_, listener) in listeners) {
                launch (CommonPool) {
                    listener.detected(device)
                }
            }
        }
    }

    protected fun getBluetoothManager(context: Context): BluetoothManager {
        return context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

}