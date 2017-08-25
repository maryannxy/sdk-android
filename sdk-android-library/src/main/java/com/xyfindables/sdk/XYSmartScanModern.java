package com.xyfindables.sdk;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.xyfindables.core.XYBase;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by arietrouw on 3/1/17.
 */

@TargetApi(21)
public class XYSmartScanModern extends XYSmartScan {

    final private static String TAG = XYSmartScanModern.class.getSimpleName();

    private boolean _pendingBleRestart21 = false;
    private boolean _pendingBleRestart21Plus = false;

    private boolean _pumpScanResults21Active = false;

    protected int _pulseCountForScan = 0;

    private ConcurrentLinkedQueue<android.bluetooth.le.ScanResult> _scanResults21;

    protected XYSmartScanModern() {
        super();
        Log.v(TAG, "XYSmartScanModern");

        _scanResults21 = new ConcurrentLinkedQueue<>();
    }

    @Override
    public boolean legacy() {
        return false;
    }

    protected final BroadcastReceiver _receiver21 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.i(TAG, "BluetoothAdapter.ACTION_STATE_CHANGED:STATE_OFF");
                        if (_pendingBleRestart21) {
                            final BluetoothAdapter bluetoothAdapter = getBluetoothManager(context).getAdapter();

                            if (bluetoothAdapter == null) {
                                Log.i(TAG, "Bluetooth Disabled");
                                return;
                            }
                            bluetoothAdapter.enable();
                            _pendingBleRestart21 = false;
                        } else if (_pendingBleRestart21Plus) {
                            final BluetoothAdapter bluetoothAdapter = getBluetoothManager(context).getAdapter();

                            if (bluetoothAdapter == null) {
                                Log.i(TAG, "Bluetooth Disabled");
                                return;
                            }
                            bluetoothAdapter.enable();
                            _pendingBleRestart21Plus = false;
                        } else {
                            setStatus(Status.BluetoothDisabled);
                        }
                        break;
                    default:
                        break;
                }
            }

            _receiver.onReceive(context, intent);
        }
    };

    @Override
    protected BroadcastReceiver getReceiver() {
        return _receiver21;
    }

    @TargetApi(21)
    private Object getSettings21(BluetoothAdapter adapter) {
        android.bluetooth.le.ScanSettings.Builder builder = new android.bluetooth.le.ScanSettings.Builder();
        /*if (adapter.isOffloadedScanBatchingSupported()) {
            builder = builder.setReportDelay(150);
        }*/
        builder = builder.setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY);
        return builder.build();
    }

    @TargetApi(23)
    private Object getSettings23(BluetoothAdapter adapter) {
        android.bluetooth.le.ScanSettings.Builder builder = new android.bluetooth.le.ScanSettings.Builder();
        /*if (adapter.isOffloadedScanBatchingSupported()) {
            builder = builder.setReportDelay(150);
        }*/
        builder = builder.setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY);
        return builder.build();
    }

    @TargetApi(21)
    @Override
    protected void scan(final Context context, int period) {
        Log.v(TAG, "scan21:start:" + period);

        Object settings;

        _scanCount++;

        final BluetoothAdapter bluetoothAdapter = getBluetoothManager(context).getAdapter();

        if (bluetoothAdapter == null) {
            Log.i(TAG, "Bluetooth Disabled");
            _scanningControl.release();
            return;
        }

        if (Build.VERSION.SDK_INT >= 23) {
            settings = getSettings23(bluetoothAdapter);
        } else {
            settings = getSettings21(bluetoothAdapter);
        }

        final android.bluetooth.le.ScanCallback scanCallback = new android.bluetooth.le.ScanCallback() {

            @Override
            public void onScanResult(int callbackType, android.bluetooth.le.ScanResult result) {

                android.bluetooth.le.ScanRecord scanRecord = result.getScanRecord();
                if (scanRecord != null) {
                    byte[] manufacturerData = scanRecord.getManufacturerSpecificData(0x004c);
                    if (manufacturerData != null) {
                        if ((manufacturerData[0] == 0x02) && (manufacturerData[1] == 0x15)) {
                            String xyId = xyIdFromAppleBytes(manufacturerData);
                            if (xyId != null) {
                                Log.v(TAG, "scan21:onScanResult: " + xyId);
                            }
                        }
                    }

                    _scanResults21.add(result);
                    if (_scanResults21.size() > 50) {
                        Log.e(TAG, "_scanResults large size! = " + _scanResults21.size());
                    }
                    _pulseCount++;
                    _pulseCountForScan++;
                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                Log.v(TAG, "scan21:onBatchScanResults:" + results.size());
                for (android.bluetooth.le.ScanResult result : results) {
                    _scanResults21.add(result);
                    _pulseCount++;
                    _pulseCountForScan++;
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                switch (errorCode) {
                    case SCAN_FAILED_ALREADY_STARTED:
                        Log.e(TAG, "scan21:onScanFailed:SCAN_FAILED_ALREADY_STARTED");
                        break;
                    case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                        XYBase.logError(TAG, "scan21:onScanFailed:SCAN_FAILED_APPLICATION_REGISTRATION_FAILED", false);
                        //reset(context); //Seems to happen when stopped in debugger?
                        break;
                    case SCAN_FAILED_FEATURE_UNSUPPORTED:
                        XYBase.logError(TAG, "scan21:onScanFailed:SCAN_FAILED_FEATURE_UNSUPPORTED");
                        break;
                    case SCAN_FAILED_INTERNAL_ERROR:
                        XYBase.logError(TAG, "scan21:onScanFailed:SCAN_FAILED_INTERNAL_ERROR", false);
                        reset(context);
                        break;
                    default:
                        XYBase.logError(TAG, "scan21:onScanFailed:Unknown:" + errorCode);
                        break;
                }
            }
        };

        try {
            _scanningControl.acquire();
        } catch (InterruptedException ex) {
            return;
        }

        final android.bluetooth.le.BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
        if (scanner == null) {
            Log.i(TAG, "Failed to get Bluetooth Scanner. Disabled?");
            _scanningControl.release();
            return;
        }

        _pulseCountForScan = 0;

        final TimerTask pumpTimerTask = new TimerTask() {
            @Override
            public void run() {
                pumpScanResults21();
            }
        };

        final Timer pumpTimer = new Timer();
        pumpTimer.schedule(pumpTimerTask, 0, 250);

        final TimerTask stopTimerTask = new TimerTask() {
            @Override
            public void run() {
                Log.v(TAG, "stopTimerTask");
                pumpTimer.cancel();
                try {
                    if (scanner != null) {
                        scanner.stopScan(scanCallback);
                        scanner.flushPendingScanResults(scanCallback);
                    }
                } catch(IllegalStateException | NullPointerException ex) {
                    //this happens if the bt adapter was turned off after previous check
                    //effectivly, we treat it as no scan results found
                }

                pumpScanResults21();
                if (Build.VERSION.SDK_INT < 25) {
                    if (_pulseCountForScan == 0) {
                        _scansWithoutPulses++;
                        if (_scansWithoutPulses >= scansWithOutPulsesBeforeRestart) {
                            reset(context);
                            _scansWithoutPulses = 0;
                        }
                    } else {
                        _scansWithoutPulses = 0;
                    }
                }
                notifyDevicesOfScanComplete();
            }
        };

        final Timer stopTimer = new Timer();
        stopTimer.schedule(stopTimerTask, period);

        scanner.startScan(new ArrayList<ScanFilter>(), (android.bluetooth.le.ScanSettings)settings, scanCallback);
        dump(context);
        _scanningControl.release();
        Log.v(TAG, "scan21:finish");
    }

    private void processScanResult21(String xyId, android.bluetooth.le.ScanResult scanResult) {
        _processedPulseCount++;
        XYDevice device = deviceFromId(xyId);
        if (device == null) {
            XYBase.logError(TAG, "Failed to Create Device");
            return;
        }
        device.pulse21(scanResult);
    }

    private void processScanResult21(android.bluetooth.le.ScanResult scanResult) {
        //Log.v(TAG, "processScanResult21");
        _processedPulseCount++;
        android.bluetooth.le.ScanRecord scanRecord = scanResult.getScanRecord();
        if (scanRecord != null) {
            byte[] manufacturerData = scanRecord.getManufacturerSpecificData(0x004c);
            if (manufacturerData != null) {
                if ((manufacturerData[0] == 0x02) && (manufacturerData[1] == 0x15)) {
                    String xyId = xyIdFromAppleBytes(manufacturerData);
                    if (xyId != null) {
                        processScanResult21(xyId, scanResult);
                    }
                }
            }
        }
    }

    private void pumpScanResults21() {
        if (_pumpScanResults21Active) {
            return;
        }
        _pumpScanResults21Active = true;
        android.bluetooth.le.ScanResult result = _scanResults21.poll();
        while (result != null) {
            processScanResult21(result);
            result = _scanResults21.poll();
        }
        _pumpScanResults21Active = false;
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

    public void reset(Context context) {
        Log.i(TAG, "reset");
        XYBase.logError(TAG, "resetting bluetooth adapter", false);
        /*if (Build.VERSION.SDK_INT == 21) {
            if (!_pendingBleRestart21) {
                restartBle21(context);
            }
        } else if (Build.VERSION.SDK_INT > 21) {
            if (!_pendingBleRestart21Plus) {
                restartBle21Plus(context);
            }
        }*/
    }

    private void restartBle21(Context context) {
        Log.i(TAG, "restartBle21");
        try {
            final BluetoothAdapter bluetoothAdapter = getBluetoothManager(context).getAdapter();

            if (bluetoothAdapter == null) {
                Log.i(TAG, "Bluetooth Disabled");
                return;
            }
            _pendingBleRestart21 = true;
            Method shutdown = bluetoothAdapter.getClass().getMethod("shutdown");
            shutdown.invoke(bluetoothAdapter);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
            XYBase.logException(TAG, ex);
        }
    }

    private void restartBle21Plus(Context context) {
        Log.i(TAG, "restartBle21Plus");
        final BluetoothAdapter bluetoothAdapter = getBluetoothManager(context).getAdapter();

        if (bluetoothAdapter == null) {
            Log.i(TAG, "Bluetooth Disabled");
            return;
        }
        _pendingBleRestart21Plus = true;
        bluetoothAdapter.disable();
    }
}
