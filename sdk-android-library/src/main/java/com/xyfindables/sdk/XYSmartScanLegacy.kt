package com.xyfindables.sdk

import android.annotation.TargetApi
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import com.xyfindables.core.XYBase
import com.xyfindables.sdk.bluetooth.ScanRecordLegacy
import com.xyfindables.sdk.bluetooth.ScanResultLegacy
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Created by arietrouw on 12/20/16.
 */

@TargetApi(18)
open class XYSmartScanLegacy internal constructor() : XYSmartScan() {

    private var scanResults = ConcurrentLinkedQueue<ScanResultLegacy>()

    private val scanCallback = BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
        scanResults.add(ScanResultLegacy(device, ScanRecordLegacy.parseFromBytes(scanRecord), rssi, System.currentTimeMillis()))
        pulseCount++
    }

    init {
        logExtreme(TAG, TAG)
        scanResults = ConcurrentLinkedQueue()

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action

                if (action == null) {
                    XYBase.logError(TAG, "connTest-_receiver action is null!", false)
                    return
                }

                when (action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                                BluetoothAdapter.ERROR)
                        when (state) {
                            BluetoothAdapter.STATE_OFF -> {
                                XYBase.logInfo(TAG, "BluetoothAdapter.ACTION_STATE_CHANGED:STATE_OFF")
                                setStatus(Status.BluetoothDisabled)
                                setAllToOutOfRange()
                            }
                            BluetoothAdapter.STATE_TURNING_OFF -> XYBase.logInfo(TAG, "BluetoothAdapter.ACTION_STATE_CHANGED:STATE_TURNING_OFF")
                            BluetoothAdapter.STATE_ON -> {
                                XYBase.logInfo(TAG, "BluetoothAdapter.ACTION_STATE_CHANGED:STATE_ON")
                                setStatus(Status.Enabled)
                            }
                            BluetoothAdapter.STATE_TURNING_ON -> XYBase.logInfo(TAG, "BluetoothAdapter.ACTION_STATE_CHANGED:STATE_TURNING_ON")
                            else -> XYBase.logError(TAG, "BluetoothAdapter.ACTION_STATE_CHANGED:Unknwon State:$state", true)
                        }
                    }
                    BluetoothAdapter.ACTION_SCAN_MODE_CHANGED -> {
                        val scanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE,
                                BluetoothAdapter.ERROR)
                        val prevScanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE,
                                BluetoothAdapter.ERROR)
                        when (scanMode) {
                            BluetoothAdapter.SCAN_MODE_NONE -> XYBase.logInfo(TAG, "BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:SCAN_MODE_NONE")
                            BluetoothAdapter.SCAN_MODE_CONNECTABLE -> XYBase.logInfo(TAG, "BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:SCAN_MODE_CONNECTABLE")
                            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE -> XYBase.logInfo(TAG, "BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:SCAN_MODE_CONNECTABLE_DISCOVERABLE")
                            else -> XYBase.logError(TAG, "BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:????:$scanMode", true)
                        }
                        when (prevScanMode) {
                            BluetoothAdapter.SCAN_MODE_NONE -> XYBase.logInfo(TAG, "BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:PREV:SCAN_MODE_NONE")
                            BluetoothAdapter.SCAN_MODE_CONNECTABLE -> XYBase.logInfo(TAG, "BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:PREV:SCAN_MODE_CONNECTABLE")
                            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE -> XYBase.logInfo(TAG, "BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:PREV:SCAN_MODE_CONNECTABLE_DISCOVERABLE")
                            else -> XYBase.logError(TAG, "BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:PREV:????:$prevScanMode", false)
                        }
                    }
                    else -> XYBase.logError(TAG, "Unknown Action:$action", false)
                }
            }
        }
    }

    private fun processScanResult(xyId: String, scanResult: ScanResultLegacy) {
        _processedPulseCount++
        val device = deviceFromId(xyId)
        if (device == null) {
            XYBase.logError(TAG, "connTest-Failed to Create Device", true)
            return
        }
        device.pulse18(scanResult)
    }

    private fun processScanResult(scanResult: ScanResultLegacy) {
        _processedPulseCount++
        val scanRecord = scanResult.scanRecord
        val manufacturerData = scanRecord!!.getManufacturerSpecificData(0x004c)
        if (manufacturerData != null) {
            if (manufacturerData[0].toInt() == 0x02 && manufacturerData[1].toInt() == 0x15) {
                val xyId = xyIdFromAppleBytes(manufacturerData)
                if (xyId != null) {
                    processScanResult(xyId, scanResult)
                }
            }
        }
    }

    private fun pumpScanResults() {
        var result: ScanResultLegacy? = scanResults.poll()
        while (result != null) {
            processScanResult(result)
            result = scanResults.poll()
        }
    }

    var context : Context? = null

    override fun startScan(context: Context) {
        logExtreme(TAG, "scan:start:$period")
        if (!_scanningControl.tryAcquire()) {
            return
        }

        this.context = context

        scanCount++

        val bluetoothAdapter = getBluetoothManager(context.applicationContext).adapter

        if (bluetoothAdapter == null) {
            logInfo(TAG, "Bluetooth Disabled")
            return
        }

        val pumpTimerTask = object : TimerTask() {
            override fun run() {
                pumpScanResults()
            }
        }

        val pumpTimer = Timer()
        pumpTimer.schedule(pumpTimerTask, 0, 250)

        val stopTimerTask = object : TimerTask() {
            override fun run() {
                logExtreme(TAG, "stopTimerTask")
                pumpTimer.cancel()

                @Suppress("DEPRECATION")
                bluetoothAdapter.stopLeScan(scanCallback)
                pumpScanResults()

                notifyDevicesOfScanComplete()
                dump(context.applicationContext)
            }
        }

        val stopTimer = Timer()
        stopTimer.schedule(stopTimerTask, period.toLong())

        @Suppress("DEPRECATION")
        bluetoothAdapter.startLeScan(scanCallback)
        _scanningControl.release()
        logExtreme(TAG, "scan:finish")
    }

    override fun stopScan() {
        val bluetoothAdapter = getBluetoothManager(context!!.applicationContext).adapter

        if (bluetoothAdapter == null) {
            logInfo(TAG, "Bluetooth Disabled")
            return
        }

        @Suppress("DEPRECATION")
        bluetoothAdapter.stopLeScan(scanCallback)
        context = null
    }

    override fun statusChanged(status: Status) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object {

        private val TAG = XYSmartScanLegacy::class.java.simpleName
    }
}
