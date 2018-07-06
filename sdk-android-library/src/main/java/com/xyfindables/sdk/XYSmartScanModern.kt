package com.xyfindables.sdk

import android.annotation.TargetApi
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

import com.xyfindables.core.XYBase
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.ArrayList
import java.util.ConcurrentModificationException
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Created by arietrouw on 3/1/17.
 */

@TargetApi(21)
open class XYSmartScanModern : XYSmartScan() {

    private var _pulseCountForScan = 0

    var reportUnknownDevices = false

    private var pendingBleRestart21 = false
    private var pendingBleRestart21Plus = false

    init {
        logExtreme(TAG, "XYSmartScanModern")

        legacy = false

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action

                if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR)
                    when (state) {
                        BluetoothAdapter.STATE_OFF -> {
                            logInfo(TAG, "BluetoothAdapter.ACTION_STATE_CHANGED:STATE_OFF")
                            if (pendingBleRestart21) {
                                val bluetoothAdapter = getBluetoothManager(context).adapter

                                if (bluetoothAdapter == null) {
                                    logInfo(TAG, "Bluetooth Disabled")
                                    return
                                }
                                bluetoothAdapter.enable()
                                pendingBleRestart21 = false
                            } else if (pendingBleRestart21Plus) {
                                val bluetoothAdapter = getBluetoothManager(context).adapter

                                if (bluetoothAdapter == null) {
                                    logInfo(TAG, "Bluetooth Disabled")
                                    return
                                }
                                bluetoothAdapter.enable()
                                pendingBleRestart21Plus = false
                            } else {
                                setStatus(XYSmartScan.Status.BluetoothDisabled)
                            }
                        }
                        else -> {
                        }
                    }
                }
            }
        }
    }

    @TargetApi(21)
    private fun getSettings21(): Any {
        var builder = android.bluetooth.le.ScanSettings.Builder()
        builder = builder.setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY)
        return builder.build()
    }

    @TargetApi(23)
    private fun getSettings23(): Any {
        var builder = android.bluetooth.le.ScanSettings.Builder()
        builder = builder.setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY)
        return builder.build()
    }

    val scanCallback = object : ScanCallback() {

        override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult) {
            async (CommonPool) {
                processScanResult21(result)
                pulseCount++
                _pulseCountForScan++
            }
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            for (result in results) {
                async (CommonPool) {
                    processScanResult21(result)
                    pulseCount++
                    _pulseCountForScan++
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            when (errorCode) {
                ScanCallback.SCAN_FAILED_ALREADY_STARTED -> XYBase.logError(TAG, "scan21:onScanFailed:SCAN_FAILED_ALREADY_STARTED", true)
                ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> {
                    XYBase.logError(TAG, "scan21:onScanFailed:SCAN_FAILED_APPLICATION_REGISTRATION_FAILED", false)
                    reset(context!!)
                }
                ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> XYBase.logError(TAG, "scan21:onScanFailed:SCAN_FAILED_FEATURE_UNSUPPORTED", true)
                ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> {
                    XYBase.logError(TAG, "scan21:onScanFailed:SCAN_FAILED_INTERNAL_ERROR", false)
                    reset(context!!)
                }
                else -> XYBase.logError(TAG, "scan21:onScanFailed:Unknown:$errorCode", true)
            }
        }
    }

    var context: Context? = null

    @TargetApi(21)
    override fun startScan(context: Context) {
        logExtreme(TAG, "scan21:start")

        val settings: Any

        this.context = context

        scanCount++

        val bluetoothAdapter = getBluetoothManager(context).adapter

        if (bluetoothAdapter == null) {
            logInfo(TAG, "Bluetooth Disabled")
            return
        }

        if (Build.VERSION.SDK_INT >= 23) {
            settings = getSettings23()
        } else {
            settings = getSettings21()
        }

        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            logInfo(TAG, "startScan:Failed to get Bluetooth Scanner. Disabled?")
            return
        }

        _pulseCountForScan = 0

        scanner.startScan(ArrayList(), settings as android.bluetooth.le.ScanSettings, scanCallback)
        dump(context)
        logExtreme(TAG, "scan21:finish")
    }

    @TargetApi(21)
    override fun stopScan() {
        val bluetoothAdapter = getBluetoothManager(context!!).adapter

        if (bluetoothAdapter == null) {
            logInfo(TAG, "stopScan: Bluetooth Disabled")
            return
        }

        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            logInfo(TAG, "stopScan:Failed to get Bluetooth Scanner. Disabled?")
            return
        }

        scanner.stopScan(scanCallback)
        this.context = null
    }

    override fun statusChanged(status: Status) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun processScanResult21(xyId: String, scanResult: android.bluetooth.le.ScanResult) {
        _processedPulseCount++
        val device = deviceFromId(xyId)
        if (device == null) {
            XYBase.logError(TAG, "connTest-Failed to Create Device", true)
            return
        }
        device.pulse21(scanResult)
    }

    private fun processAppleResult(manufacturerData: ByteArray,  scanResult: android.bluetooth.le.ScanResult) {
        if (manufacturerData[0].toInt() == 0x02 && manufacturerData[1].toInt() == 0x15) {
            processIbeaconResult(manufacturerData, scanResult)
        } else {
            if (reportUnknownDevices) {
                reportUnknownDevice(scanResult.scanRecord.bytes)
            }
        }
    }

    private fun processIbeaconResult(manufacturerData: ByteArray, scanResult: android.bluetooth.le.ScanResult) {
        val xyId = xyIdFromAppleBytes(manufacturerData)
        if (xyId != null) {
            processScanResult21(xyId, scanResult)
        } else {
            if (reportUnknownDevices) {
                reportUnknownDevice(scanResult.scanRecord.bytes)
            }
        }
    }

    private fun processScanResult21(scanResult: android.bluetooth.le.ScanResult) {
        _processedPulseCount++
        val scanRecord = scanResult.scanRecord
        if (scanRecord != null) {
            val manufacturerData = scanRecord.getManufacturerSpecificData(0x004c)
            if (manufacturerData != null) {
                processAppleResult(manufacturerData, scanResult)
            } else {
                if (reportUnknownDevices) {
                    reportUnknownDevice(scanResult.scanRecord.bytes)
                }
            }
        } else {
            logError("No ScanResult", true)
        }
    }

    private fun restartBle21(context: Context) {
        logError(TAG, "restartBle21", false)
        try {
            val bluetoothAdapter = getBluetoothManager(context).adapter

            if (bluetoothAdapter == null) {
                logError(TAG, "Bluetooth Disallowed or Non-Existant", false)
                return
            }
            pendingBleRestart21 = true
            val shutdown = bluetoothAdapter.javaClass.getMethod("shutdown")
            shutdown.invoke(bluetoothAdapter)
        } catch (ex: NoSuchMethodException) {
            XYBase.logException(TAG, ex, true)
        } catch (ex: InvocationTargetException) {
            XYBase.logException(TAG, ex, true)
        } catch (ex: IllegalAccessException) {
            XYBase.logException(TAG, ex, true)
        }
    }

    private fun restartBle21Plus(context: Context) {
        logError(TAG, "restartBle21Plus", false)
        val bluetoothAdapter = getBluetoothManager(context).adapter

        if (bluetoothAdapter == null) {
            logError(TAG, "Bluetooth Disallowed or Non-Existant", false)
            return
        }
        pendingBleRestart21Plus = true
        bluetoothAdapter.disable()
        bluetoothAdapter.enable()
    }

    private fun reset(context: Context) {
        logInfo(TAG, "reset")
        if (Build.VERSION.SDK_INT == 21) {
            if (!pendingBleRestart21) {
                restartBle21(context)
            }
        } else if (Build.VERSION.SDK_INT > 21) {
            if (!pendingBleRestart21Plus) {
                restartBle21Plus(context)
            }
        }
    }


    companion object {
        private val TAG = XYSmartScanModern::class.java.simpleName
    }
}
