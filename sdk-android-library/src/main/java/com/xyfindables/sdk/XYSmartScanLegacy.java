package com.xyfindables.sdk;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.xyfindables.core.XYBase;
import com.xyfindables.sdk.bluetooth.ScanRecordLegacy;
import com.xyfindables.sdk.bluetooth.ScanResultLegacy;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by arietrouw on 12/20/16.
 */

@TargetApi(18)
public class XYSmartScanLegacy extends XYSmartScan {

    final private static String TAG = XYSmartScanLegacy.class.getSimpleName();

    private ConcurrentLinkedQueue<ScanResultLegacy> _scanResults;

    private final BluetoothAdapter.LeScanCallback _scanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    _scanResults.add(new ScanResultLegacy(device, ScanRecordLegacy.parseFromBytes(scanRecord), rssi, System.currentTimeMillis()));
                    _pulseCount++;
                }
            };

    protected XYSmartScanLegacy() {
        super();
        logExtreme(TAG, TAG);
        _scanResults = new ConcurrentLinkedQueue<>();
    }

    private void processScanResult(String xyId, ScanResultLegacy scanResult) {
        _processedPulseCount++;
        XYDevice device = deviceFromId(xyId);
        if (device == null) {
            XYBase.logError(TAG, "connTest-Failed to Create Device", true);
            return;
        }
        device.pulse18(scanResult);
    }

    private void processScanResult(ScanResultLegacy scanResult) {
        _processedPulseCount++;
        ScanRecordLegacy scanRecord = scanResult.getScanRecord();
        byte[] manufacturerData = scanRecord.getManufacturerSpecificData(0x004c);
        if (manufacturerData != null) {
            if ((manufacturerData[0] == 0x02) && (manufacturerData[1] == 0x15)) {
                String xyId = xyIdFromAppleBytes(manufacturerData);
                if (xyId != null) {
                    processScanResult(xyId, scanResult);
                }
            }
        }
    }

    private void pumpScanResults() {
        ScanResultLegacy result = _scanResults.poll();
        while (result != null) {
            processScanResult(result);
            result = _scanResults.poll();
        }
    }

    @Override
    protected void scan(final Context context, int period) {
        logExtreme(TAG, "scan:start:" + period);
        if (!_scanningControl.tryAcquire()) {
            return;
        }

        _scanCount++;

        final BluetoothAdapter bluetoothAdapter = getBluetoothManager(context.getApplicationContext()).getAdapter();

        if (bluetoothAdapter == null) {
            logInfo(TAG, "Bluetooth Disabled");
            return;
        }

        final TimerTask pumpTimerTask = new TimerTask() {
            @Override
            public void run() {
                pumpScanResults();
            }
        };

        final Timer pumpTimer = new Timer();
        pumpTimer.schedule(pumpTimerTask, 0, 250);

        final TimerTask stopTimerTask = new TimerTask() {
            @Override
            public void run() {
                logExtreme(TAG, "stopTimerTask");
                pumpTimer.cancel();
                if (bluetoothAdapter != null) {
                    bluetoothAdapter.stopLeScan(_scanCallback);
                    pumpScanResults();
                }
                notifyDevicesOfScanComplete();
                dump(context.getApplicationContext());
            }
        };

        final Timer stopTimer = new Timer();
        stopTimer.schedule(stopTimerTask, period);

        bluetoothAdapter.startLeScan(_scanCallback);
        _scanningControl.release();
        logExtreme(TAG, "scan:finish");
    }
}
