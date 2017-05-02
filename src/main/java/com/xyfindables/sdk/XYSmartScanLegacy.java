package com.xyfindables.sdk;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import com.xyfindables.core.XYBase;
import com.xyfindables.sdk.bluetooth.ScanRecordLegacy;
import com.xyfindables.sdk.bluetooth.ScanResultLegacy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

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
        Log.v(TAG, TAG);
        _scanResults = new ConcurrentLinkedQueue<>();
    }

    private void processScanResult(String xyId, ScanResultLegacy scanResult) {
        _processedPulseCount++;
        XYDevice device = deviceFromId(xyId);
        if (device == null) {
            XYBase.logError(TAG, "Failed to Create Device");
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
        Log.v(TAG, "scan:start:" + period);
        if (!_scanningControl.tryAcquire()) {
            return;
        }

        _scanCount++;

        final BluetoothAdapter bluetoothAdapter = getBluetoothManager(context).getAdapter();

        if (bluetoothAdapter == null) {
            Log.i(TAG, "Bluetooth Disabled");
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
                Log.v(TAG, "stopTimerTask");
                pumpTimer.cancel();
                bluetoothAdapter.stopLeScan(_scanCallback);
                pumpScanResults();
                notifyDevicesOfScanComplete();
                dump(context);
            }
        };

        final Timer stopTimer = new Timer();
        stopTimer.schedule(stopTimerTask, period);

        bluetoothAdapter.startLeScan(_scanCallback);
        _scanningControl.release();
        Log.v(TAG, "scan:finish");
    }

}
