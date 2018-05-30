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

    private var _pendingBleRestart21 = false
    private var _pendingBleRestart21Plus = false

    private var _pumpScanResults21Active = false

    private var _pulseCountForScan = 0

    private val _scanResults21: ConcurrentLinkedQueue<android.bluetooth.le.ScanResult>

    init {
        logExtreme(TAG, "XYSmartScanModern")

        legacy = false

        _scanResults21 = ConcurrentLinkedQueue()

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action

                if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR)
                    when (state) {
                        BluetoothAdapter.STATE_OFF -> {
                            logInfo(TAG, "BluetoothAdapter.ACTION_STATE_CHANGED:STATE_OFF")
                            if (_pendingBleRestart21) {
                                val bluetoothAdapter = getBluetoothManager(context).adapter

                                if (bluetoothAdapter == null) {
                                    logInfo(TAG, "Bluetooth Disabled")
                                    return
                                }
                                bluetoothAdapter.enable()
                                _pendingBleRestart21 = false
                            } else if (_pendingBleRestart21Plus) {
                                val bluetoothAdapter = getBluetoothManager(context).adapter

                                if (bluetoothAdapter == null) {
                                    logInfo(TAG, "Bluetooth Disabled")
                                    return
                                }
                                bluetoothAdapter.enable()
                                _pendingBleRestart21Plus = false
                            } else {
                                setStatus(XYSmartScan.Status.BluetoothDisabled)
                            }
                        }
                        else -> {
                        }
                    }
                }

                receiver?.onReceive(context, intent)
            }
        }
    }

    @TargetApi(21)
    private fun getSettings21(adapter: BluetoothAdapter): Any {
        var builder = android.bluetooth.le.ScanSettings.Builder()
        /*if (adapter.isOffloadedScanBatchingSupported()) {
            builder = builder.setReportDelay(150);
        }*/
        builder = builder.setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY)
        return builder.build()
    }

    @TargetApi(23)
    private fun getSettings23(adapter: BluetoothAdapter): Any {
        var builder = android.bluetooth.le.ScanSettings.Builder()
        /*if (adapter.isOffloadedScanBatchingSupported()) {
            builder = builder.setReportDelay(150);
        }*/
        builder = builder.setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY)
        return builder.build()
    }

    @TargetApi(21)
    override fun scan(context: Context, period: Int) {
        logExtreme(TAG, "scan21:start:$period")

        val settings: Any

        scanCount++

        val bluetoothAdapter = getBluetoothManager(context).adapter

        if (bluetoothAdapter == null) {
            logInfo(TAG, "Bluetooth Disabled")
            _scanningControl.release()
            return
        }

        if (Build.VERSION.SDK_INT >= 23) {
            settings = getSettings23(bluetoothAdapter)
        } else {
            settings = getSettings21(bluetoothAdapter)
        }

        val scanCallback = object : android.bluetooth.le.ScanCallback() {

            override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult) {

                val scanRecord = result.scanRecord
                if (scanRecord != null) {
                    val manufacturerData = scanRecord.getManufacturerSpecificData(0x004c)
                    if (manufacturerData != null) {
                        if (manufacturerData[0].toInt() == 0x02 && manufacturerData[1].toInt() == 0x15) {
                            val xyId = xyIdFromAppleBytes(manufacturerData)
                            if (xyId != null) {
                                //logExtreme(TAG, "scan21:onScanResult: " + xyId);
                            }
                        }
                    }
                    if (_scanResults21.contains(result)) {
                        logExtreme(TAG, "connTest-getting same scan result !!!!!!!!!!!!!!!!!!!!!!!!!!")
                    }
                    _scanResults21.add(result)
                    pulseCount++
                    _pulseCountForScan++
                }
            }

            override fun onBatchScanResults(results: List<ScanResult>) {
                logExtreme(TAG, "scan21:onBatchScanResults:" + results.size)
                for (result in results) {
                    _scanResults21.add(result)
                    pulseCount++
                    _pulseCountForScan++
                }
            }

            override fun onScanFailed(errorCode: Int) {
                when (errorCode) {
                    ScanCallback.SCAN_FAILED_ALREADY_STARTED -> XYBase.logError(TAG, "scan21:onScanFailed:SCAN_FAILED_ALREADY_STARTED", true)
                    ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> {
                        XYBase.logError(TAG, "scan21:onScanFailed:SCAN_FAILED_APPLICATION_REGISTRATION_FAILED", true)
                        reset(context) //Seems to happen when stopped in debugger several times?
                    }
                    ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> XYBase.logError(TAG, "scan21:onScanFailed:SCAN_FAILED_FEATURE_UNSUPPORTED", true)
                    ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> {
                        XYBase.logError(TAG, "scan21:onScanFailed:SCAN_FAILED_INTERNAL_ERROR", true)
                        reset(context)
                    }
                    else -> XYBase.logError(TAG, "scan21:onScanFailed:Unknown:$errorCode", true)
                }
            }
        }

        try {
            _scanningControl.acquire()
        } catch (ex: InterruptedException) {
            return
        }

        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            logInfo(TAG, "Failed to get Bluetooth Scanner. Disabled?")
            _scanningControl.release()
            return
        }

        _pulseCountForScan = 0

        val pumpTimerTask = object : TimerTask() {
            override fun run() {
                pumpScanResults21()
            }
        }

        val pumpTimer = Timer()
        pumpTimer.schedule(pumpTimerTask, 0, 250)

        val stopTimerTask = object : TimerTask() {
            override fun run() {
                logInfo(TAG, "stopTimerTask")
                pumpTimer.cancel()
                try {
                    if (scanner != null) {
                        scanner.stopScan(scanCallback)
                        scanner.flushPendingScanResults(scanCallback)
                    }
                } catch (ex: IllegalStateException) {
                    //this happens if the bt adapter was turned off after previous check
                    //effectivly, we treat it as no scan results found
                } catch (ex: NullPointerException) {
                } catch (ex: ConcurrentModificationException) {
                }

                pumpScanResults21()
                if (Build.VERSION.SDK_INT < 25) {
                    if (_pulseCountForScan == 0) {
                        _scansWithoutPulses++
                        if (_scansWithoutPulses >= XYSmartScan.scansWithOutPulsesBeforeRestart) {
                            reset(context)
                            _scansWithoutPulses = 0
                        }
                    } else {
                        _scansWithoutPulses = 0
                    }
                }
                notifyDevicesOfScanComplete()
            }
        }

        val stopTimer = Timer()
        stopTimer.schedule(stopTimerTask, period.toLong())

        scanner.startScan(ArrayList(), settings as android.bluetooth.le.ScanSettings, scanCallback)
        dump(context)
        _scanningControl.release()
        logExtreme(TAG, "scan21:finish")
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

    private fun processScanResult21(scanResult: android.bluetooth.le.ScanResult) {
        //Log.v(TAG, "processScanResult21");
        _processedPulseCount++
        val scanRecord = scanResult.scanRecord
        if (scanRecord != null) {
            val manufacturerData = scanRecord.getManufacturerSpecificData(0x004c)
            if (manufacturerData != null) {
                if (manufacturerData[0].toInt() == 0x02 && manufacturerData[1].toInt() == 0x15) {
                    val xyId = xyIdFromAppleBytes(manufacturerData)
                    if (xyId != null) {
                        processScanResult21(xyId, scanResult)
                    }
                }
            }
        }
    }

    private fun pumpScanResults21() {
        if (_pumpScanResults21Active) {
            return
        }
        _pumpScanResults21Active = true
        var result: android.bluetooth.le.ScanResult? = _scanResults21.poll()
        while (result != null) {
            processScanResult21(result)
            result = _scanResults21.poll()
        }
        _pumpScanResults21Active = false
        /*_scanner.flushPendingScanResults(new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                processScanResult21(result);
                _pulseCount++;
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                for (android.bluetooth.le.ScanResult result : results) {
                    processScanResult21(result);
                    _pulseCount++;
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
            }
        });*/
    }

    private fun reset(context: Context) {
        logInfo(TAG, "reset")
        if (Build.VERSION.SDK_INT == 21) {
            if (!_pendingBleRestart21) {
                restartBle21(context)
            }
        } else if (Build.VERSION.SDK_INT > 21) {
            if (!_pendingBleRestart21Plus) {
                restartBle21Plus(context)
            }
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
            _pendingBleRestart21 = true
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
        _pendingBleRestart21Plus = true
        bluetoothAdapter.disable()
        bluetoothAdapter.enable()
    }

    companion object {

        private val TAG = XYSmartScanModern::class.java.simpleName
    }
}
