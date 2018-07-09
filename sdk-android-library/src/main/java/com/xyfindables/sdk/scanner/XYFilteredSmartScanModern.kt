package com.xyfindables.sdk.scanner

import android.annotation.TargetApi
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import com.xyfindables.core.guard
import java.util.ArrayList

@TargetApi(21)
class XYFilteredSmartScanModern(context: Context) : XYFilteredSmartScan(context) {
    override fun start() {
        logInfo("start")
        super.start()

        val bluetoothAdapter = getBluetoothManager(context).adapter
        bluetoothAdapter.guard {
            logInfo("Bluetooth Disabled")
            return
        }

        val scanner = bluetoothAdapter.bluetoothLeScanner
        scanner.guard {
            logInfo("startScan:Failed to get Bluetooth Scanner. Disabled?")
            return
        }

        val filters = ArrayList<ScanFilter>()

        filters.add(getXY4ScanFilter())

        scanner.startScan(filters, getSettings(), callback)
    }

    val callback = object : ScanCallback() {
        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            results.guard { return }
            val xyResults = ArrayList<XYScanResult>()
            for (result in results!!) {
                xyResults.add(XYScanResultModern(result))
            }
            onScanResult(xyResults)
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            logError("onScanFailed: $errorCode", true)
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result.guard { return }
            val xyResults = ArrayList<XYScanResult>()
            xyResults.add(XYScanResultModern(result!!))
            onScanResult(xyResults)
        }
    }

    val APPLE_CORPORATION_ID = 0x004c
    val APPLE_IBEACON_ID = 0x02.toByte()
    val APPLE_IBEACON_LEN = 0x15.toByte()

    private fun getXY4ScanFilter() : ScanFilter {
        val data = byteArrayOf(APPLE_IBEACON_ID, APPLE_IBEACON_LEN)
        val mask = byteArrayOf(0xf0.toByte())
        return ScanFilter.Builder().setManufacturerData(APPLE_CORPORATION_ID, data, mask).build()
    }

    private fun getSettings(): ScanSettings {
        return ScanSettings.Builder().setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY).build()
    }

    override fun stop() {
        logInfo("stop")
        super.stop()
        val bluetoothAdapter = getBluetoothManager(context!!).adapter

        if (bluetoothAdapter == null) {
            logInfo("stop: Bluetooth Disabled")
            return
        }

        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            logInfo("stop:Failed to get Bluetooth Scanner. Disabled?")
            return
        }

        scanner.stopScan(callback)
    }
}