package com.xyfindables.sdk

import android.annotation.TargetApi
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context

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
class XYSmartScanLegacy internal constructor() : XYSmartScan() {

    private val _scanResults: ConcurrentLinkedQueue<ScanResultLegacy>

    private val _scanCallback = BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
        _scanResults.add(ScanResultLegacy(device, ScanRecordLegacy.parseFromBytes(scanRecord), rssi, System.currentTimeMillis()))
        _pulseCount++
    }

    init {
        logExtreme(TAG, TAG)
        _scanResults = ConcurrentLinkedQueue()
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
        var result: ScanResultLegacy? = _scanResults.poll()
        while (result != null) {
            processScanResult(result)
            result = _scanResults.poll()
        }
    }

    override fun scan(context: Context, period: Int) {
        logExtreme(TAG, "scan:start:$period")
        if (!_scanningControl.tryAcquire()) {
            return
        }

        _scanCount++

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
                if (bluetoothAdapter != null) {
                    bluetoothAdapter.stopLeScan(_scanCallback)
                    pumpScanResults()
                }
                notifyDevicesOfScanComplete()
                dump(context.applicationContext)
            }
        }

        val stopTimer = Timer()
        stopTimer.schedule(stopTimerTask, period.toLong())

        bluetoothAdapter.startLeScan(_scanCallback)
        _scanningControl.release()
        logExtreme(TAG, "scan:finish")
    }

    companion object {

        private val TAG = XYSmartScanLegacy::class.java.simpleName
    }
}
