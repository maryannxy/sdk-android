package com.xyfindables.sdk;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import com.xyfindables.core.XYBase;

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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import io.fabric.sdk.android.Fabric;

import static com.xyfindables.sdk.XYSmartScan.Status.LocationDisabled;
import static com.xyfindables.sdk.XYSmartScan.Status.None;

/**
 * Created by arietrouw on 12/20/16.
 */

public abstract class XYSmartScan extends XYBase implements XYDevice.Listener {

    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();
    final private static String TAG = XYSmartScan.class.getSimpleName();

    public enum Status {
        None,
        Enabled,
        BluetoothUnavailable,
        BluetoothUnstable,
        BluetoothDisabled,
        LocationDisabled
    }

    final public static XYSmartScan instance;

    static {
        if (Build.VERSION.SDK_INT >= 21) {
            instance = new XYSmartScanModern();
        } else {
            instance = new XYSmartScanLegacy();
        }
    }

    final private static ThreadPoolExecutor _threadPoolListeners = new ThreadPoolExecutor(1, 5, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    private static int defaultLockTimeout = 10;
    protected static final int scansWithOutPulsesBeforeRestart = 100;
    private static int _missedPulsesForOutOfRange = 20;

    private static TimeUnit defaultLockTimeUnits = TimeUnit.SECONDS;

    private Status _status = Status.None;

    private final ReentrantLock _devicesLock = new ReentrantLock();

    private int _autoScanPeriod = 0;
    private int _autoScanInterval = 0;
    //private GoogleApiClient _googleApiClient;

    private TimerTask _autoScanTimerTask;

    private Timer _autoScanTimer;
    private boolean paused = false;

    protected HashMap<String, XYDevice> _devices;
    protected int _scanCount = 0;
    protected int _pulseCount = 0;

    protected int _processedPulseCount = 0;
    protected int _scansWithoutPulses = 0;

    private long _currenDeviceId;

    protected Semaphore _scanningControl = new Semaphore(1, true);

    private final HashMap<String, Listener> _listeners = new HashMap<>();

    protected BluetoothManager getBluetoothManager(Context context) {
        return (BluetoothManager)context.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
    }

    private boolean _receiverRegistered = false;

    protected final BroadcastReceiver _receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            switch (action) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR);
                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:
                            Log.i(TAG, "BluetoothAdapter.ACTION_STATE_CHANGED:STATE_OFF");
                            setStatus(Status.BluetoothDisabled);
                            setAllToOutOfRange();
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            Log.i(TAG, "BluetoothAdapter.ACTION_STATE_CHANGED:STATE_TURNING_OFF");
                            break;
                        case BluetoothAdapter.STATE_ON:
                            Log.i(TAG, "BluetoothAdapter.ACTION_STATE_CHANGED:STATE_ON");
                            setStatus(Status.Enabled);
                            break;
                        case BluetoothAdapter.STATE_TURNING_ON:
                            Log.i(TAG, "BluetoothAdapter.ACTION_STATE_CHANGED:STATE_TURNING_ON");
                            break;
                        default:
                            Log.e(TAG, "BluetoothAdapter.ACTION_STATE_CHANGED:Unknwon State:" + state);
                            break;
                    }
                    break;
                case BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:
                    final int scanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE,
                            BluetoothAdapter.ERROR);
                    final int prevScanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE,
                            BluetoothAdapter.ERROR);
                    switch (scanMode) {
                        case BluetoothAdapter.SCAN_MODE_NONE:
                            Log.i(TAG, "BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:SCAN_MODE_NONE");
                            break;
                        case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                            Log.i(TAG, "BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:SCAN_MODE_CONNECTABLE");
                            break;
                        case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                            Log.i(TAG, "BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:SCAN_MODE_CONNECTABLE_DISCOVERABLE");
                            break;
                        default:
                            Log.e(TAG, "BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:????:" + scanMode);
                            break;
                    }
                    switch (prevScanMode) {
                        case BluetoothAdapter.SCAN_MODE_NONE:
                            Log.i(TAG, "BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:PREV:SCAN_MODE_NONE");
                            break;
                        case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                            Log.i(TAG, "BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:PREV:SCAN_MODE_CONNECTABLE");
                            break;
                        case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                            Log.i(TAG, "BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:PREV:SCAN_MODE_CONNECTABLE_DISCOVERABLE");
                            break;
                        default:
                            Log.e(TAG, "BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:PREV:????:" + prevScanMode);
                            break;
                    }
                    break;
                default:
                    Log.e(TAG, "Unknown Action:" + action);
                    break;
            }
        }
    };

    protected XYSmartScan() {
        super();
        Log.v(TAG, TAG);
        _devices = new HashMap<>();
    }

    public boolean legacy() {
        return true;
    }

    public int getInterval() {
        return _autoScanInterval;
    }

    public int getPeriod() {
        return _autoScanPeriod;
    }

    public int getOutOfRangePulsesMissed() {
        return _missedPulsesForOutOfRange;
    }

    protected BroadcastReceiver getReceiver() {
        return _receiver;
    }

    private void setAllToOutOfRange() {
        try {
            if (_devicesLock.tryLock(defaultLockTimeout, defaultLockTimeUnits)) {
                for (Map.Entry<String, XYDevice> entry : _devices.entrySet()) {
                    entry.getValue().pulseOutOfRange();
                }
                _devicesLock.unlock();
            } else {
                XYBase.logError(TAG, "getDevices failed due to lock:" + _devicesLock.getHoldCount());
            }
        } catch (InterruptedException ex) {
            XYBase.logException(TAG, ex);
        }
    }

    public void init(Context context, long currentDeviceId, int missedPulsesForOutOfRange) {
        Log.v(TAG, "init");

        _missedPulsesForOutOfRange = missedPulsesForOutOfRange;

        _currenDeviceId = currentDeviceId;

        if (!_receiverRegistered) {

            BroadcastReceiver receiver = getReceiver();

            Context appContext = context.getApplicationContext();

            appContext.registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
            appContext.registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED));
            appContext.registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
            appContext.registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
            appContext.registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED));
            appContext.registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE));
            appContext.registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            appContext.registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));

            _receiverRegistered = true;
        }
    }

    public void cleanup(Context context) {
        if (_receiverRegistered) {
            context.getApplicationContext().unregisterReceiver(getReceiver());
            _receiverRegistered = false;
        }
        stopAutoScan();
    }

    public int getMissedPulsesForOutOfRange() {
        return _missedPulsesForOutOfRange;
    }

    public void setMissedPulsesForOutOfRange(int value) {
        _missedPulsesForOutOfRange = value;
    }

    public void enableBluetooth(Context context) {
        final BluetoothAdapter bluetoothAdapter = getBluetoothManager(context.getApplicationContext()).getAdapter();

        if (bluetoothAdapter == null) {
            Log.i(TAG, "Bluetooth Disabled");
            return;
        }
        bluetoothAdapter.enable();
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public XYDevice deviceFromId(String id) {
        if (id == null || id.isEmpty()) {
            XYBase.logError(TAG, "Tried to get a null device");
            return null;
        }

        id = id.toLowerCase();

        XYDevice device;
        try {
            if (_devicesLock.tryLock(defaultLockTimeout, defaultLockTimeUnits)) {
                device = _devices.get(id);
                _devicesLock.unlock();
            } else {
                XYBase.logError(TAG, "deviceFromId failed due to lock (getUniqueId):" + _devicesLock.getHoldCount());
                return null;
            }
        } catch (InterruptedException ex) {
            XYBase.logException(TAG, ex);
            return null;
        }

        if (device == null) {
            device = new XYDevice(id);
            try {
                if (_devicesLock.tryLock(defaultLockTimeout, defaultLockTimeUnits)) {
                    _devices.put(id, device);
                    _devicesLock.unlock();
                } else {
                    XYBase.logError(TAG, "deviceFromId failed due to lock (put):" + _devicesLock.getHoldCount());
                    return null;
                }
            } catch (InterruptedException ex) {
                XYBase.logException(TAG, ex);
                return null;
            }
        }
        device.addListener(TAG, this);
        return device;
    }

    public long getCurrentDeviceId() {
        return _currenDeviceId;
    }

    public XYDevice getCurrentDevice() {
        String id = XYDevice.buildId(XYDevice.Family.Mobile, (int)((_currenDeviceId & 0xffff0000L) >> 16), (int)(_currenDeviceId & 0xffff));
        if (id == null) {
            return null;
        } else {
            return deviceFromId(id);
        }
    }

    public void startAutoScan(final Context context, int interval, int period) {
        Log.v(TAG, "startAutoScan:" + interval + ":" + period);
        if (_autoScanTimer != null) {
            //XYData.logError(TAG, "startAutoScan already Started (_autoScanTimer != null)");
            stopAutoScan();
        }
        if (_autoScanTimerTask != null) {
            XYBase.logError(TAG, "startAutoScan already Started (_autoScanTimerTask != null)");
            stopAutoScan();
        }
        _autoScanPeriod = period;
        _autoScanInterval = interval;
        final XYSmartScan scanner = this;
        _autoScanTimerTask = new TimerTask() {
            @Override
            public void run() {
                scanner.scan(context.getApplicationContext(), _autoScanPeriod);
            }
        };
        _autoScanTimer = new Timer();
        _autoScanTimer.schedule(_autoScanTimerTask, 0, interval + period);
    }

    public void stopAutoScan() {
        Log.v(TAG, "stopAutoScan");
        if (_autoScanTimer != null) {
            _autoScanTimer.cancel();
            _autoScanTimer = null;
        } else {
            XYBase.logError(TAG, "stopAutoScan already Stopped (_autoScanTimer == null)");
        }
        if (_autoScanTimerTask != null) {
            _autoScanTimerTask.cancel();
            _autoScanTimerTask = null;
        } else {
            XYBase.logError(TAG, "stopAutoScan already Stopped (_autoScanTimerTask == null)");
        }
    }

    public void pauseAutoScan(boolean pause) {
        Log.v(TAG, "pauseAutoScan:" + pause);
        if (pause == this.paused) {
            return;
        }
        if (pause) {
            //if (_scanningControl.tryAcquire()) {
                this.paused = true;
            //}
        } else {
            //_scanningControl.release();
            this.paused = false;
        }
    }

    public void setStatus(Status status) {
        if (_status != status) {
            _status = status;
            reportStatusChanged(status);
        }
    }

    public boolean areLocationServicesAvailable(@NonNull Context context) {
        LocationManager lm = null;
        boolean gps_enabled, network_enabled;

        if (lm == null)
            lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

        network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (gps_enabled && network_enabled) {
            Log.w(TAG, "returning 3");
            setStatus(None);
            return true;
        } else if (gps_enabled) {
            Log.w(TAG, "returning 2");
            setStatus(LocationDisabled);
            return false;
        } else if (network_enabled) {
            Log.w(TAG, "returning 1");
            setStatus(LocationDisabled);
            return false;
        } else {
            Log.w(TAG, "returning 0");
            setStatus(LocationDisabled);
            return false;
        }
    }

    public static boolean isLocationAvailable(@NonNull Context context) {
        LocationManager lm = null;
        boolean gps_enabled, network_enabled;

        if (lm == null)
            lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

        network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (gps_enabled && network_enabled) {
            Log.w(TAG, "returning 3");
            return true;
        } else if (gps_enabled) {
            Log.w(TAG, "returning 2");
            return false;
        } else if (network_enabled) {
            Log.w(TAG, "returning 1");
            return false;
        } else {
            Log.w(TAG, "returning 0");
            return false;
        }
    }

    public Status getStatus(Context context, boolean refresh) {
        if (_status == Status.None || refresh) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                _status = Status.BluetoothUnavailable;
            } else {
                if (bluetoothAdapter.isEnabled()) {
                    if (isLocationAvailable(context.getApplicationContext())) {
                        _status = Status.Enabled;
                    } else {
                        _status = Status.LocationDisabled;
                    }
                } else {
                    _status = Status.BluetoothDisabled;
                }
            }
        }
        return _status;
    }

    public List<XYDevice> getDevices() {
        try {
            ArrayList<XYDevice> list = new ArrayList<>();
            if (_devicesLock.tryLock(defaultLockTimeout, defaultLockTimeUnits)) {
                for (Map.Entry<String, XYDevice> entry : _devices.entrySet()) {
                    list.add(entry.getValue());
                }
                _devicesLock.unlock();
                return list;
            } else {
                XYBase.logError(TAG, "getDevices failed due to lock:" + _devicesLock.getHoldCount());
                return null;
            }
        } catch (InterruptedException ex) {
            XYBase.logException(TAG, ex);
            return null;
        }
    }

    protected String xyIdFromAppleBytes(byte[] iBeaconBytes) {
        //check for ibeacon header
        if (iBeaconBytes[0] == 0x02 && iBeaconBytes[1] == 0x15) {
            if (iBeaconBytes.length != 23) {
                XYBase.logError(TAG, "iBeacon should have 23 bytes");
                return null;
            }
            String hexString = bytesToHex(iBeaconBytes);
            String uuid = hexString.substring(4, 12) + "-" +
                    hexString.substring(12, 16) + "-" +
                    hexString.substring(16, 20) + "-" +
                    hexString.substring(20, 24) + "-" +
                    hexString.substring(24, 36);

            int major = (iBeaconBytes[18] & 0xff) * 0x100 + (iBeaconBytes[19] & 0xff);

            int minor = (iBeaconBytes[20] & 0xff) * 0x100 + (iBeaconBytes[21] & 0xff);

            XYDevice.Family family = XYDevice.uuid2family.get(UUID.fromString(uuid));

            if (family == null) {
                return null;
            }

            if (family == XYDevice.Family.XY2 || family == XYDevice.Family.XY3 || family == XYDevice.Family.Gps) {
                minor = minor & 0xfff0 | 0x0004;
            }

            return String.format(Locale.getDefault(), "xy:%1$s:%2$s.%3$d.%4$d", XYDevice.family2prefix.get(family), uuid, major, minor).toLowerCase();
        } else {
            return null;
        }
    }

    protected void notifyDevicesOfScanComplete() {
        Log.v(TAG, "notifyDevicesOfScanComplete");
        List<XYDevice> deviceList = getDevices();

        for (XYDevice device : deviceList) {
            device.scanComplete();
        }
    }

    protected void dump(Context context) {

        final BluetoothAdapter bluetoothAdapter = getBluetoothManager(context.getApplicationContext()).getAdapter();

        if (bluetoothAdapter == null) {
            Log.i(TAG, "Bluetooth Disabled");
            return;
        }

        Log.v(TAG, "=========================");
        Log.v(TAG, "bluetoothAdapter:scanMode:" + bluetoothAdapter.getScanMode());
        Log.v(TAG, "bluetoothAdapter:state:" + bluetoothAdapter.getState());
        Log.v(TAG, "bluetoothAdapter:name:" + bluetoothAdapter.getName());
        Log.v(TAG, "bluetoothAdapter:address:" + bluetoothAdapter.getAddress());
        Log.v(TAG, "bluetoothAdapter:GATT Connection State:" + bluetoothAdapter.getProfileConnectionState(BluetoothProfile.GATT));
        Log.v(TAG, "scanCount:" + _scanCount);
        Log.v(TAG, "pulseCount:" + _pulseCount);
        Log.v(TAG, "processedPulseCount:" + _pulseCount);
        Log.v(TAG, "mainLooper:" + Looper.getMainLooper().toString());
        Log.v(TAG, "=========================");
    }

    // region ============= Listeners =============

    abstract protected void scan(final Context context, int period);

    public void addListener(final String key, final Listener listener) {
        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                synchronized (_listeners) {
                    _listeners.put(key, listener);
                }
                return null;
            }
        };
        asyncTask.executeOnExecutor(_threadPoolListeners);
    }

    public void removeListener(final String key) {
        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                synchronized (_listeners) {
                    _listeners.remove(key);
                }
                return null;
            }
        };
        asyncTask.executeOnExecutor(_threadPoolListeners);
    }

    private void reportEntered(final XYDevice device) {
        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                synchronized (_listeners) {
                    for (Map.Entry<String, Listener> entry : _listeners.entrySet()) {
                        entry.getValue().entered(device);
                    }
                }
                return null;
            }
        };
        asyncTask.executeOnExecutor(_threadPoolListeners);
    }

    private void reportExited(final XYDevice device) {
        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                synchronized (_listeners) {
                    for (Map.Entry<String, Listener> entry : _listeners.entrySet()) {
                        entry.getValue().exited(device);
                    }
                }
                return null;
            }
        };
        asyncTask.executeOnExecutor(_threadPoolListeners);
    }

    private void reportDetected(final XYDevice device) {
        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                synchronized (_listeners) {
                    for (Map.Entry<String, Listener> entry : _listeners.entrySet()) {
                        entry.getValue().detected(device);
                    }
                }
                return null;
            }
        };
        asyncTask.executeOnExecutor(_threadPoolListeners);
    }

    private void reportButtonPressed(final XYDevice device, final XYDevice.ButtonType buttonType) {
        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                synchronized (_listeners) {
                    for (Map.Entry<String, Listener> entry : _listeners.entrySet()) {
                        entry.getValue().buttonPressed(device, buttonType);
                    }
                }
                return null;
            }
        };
        asyncTask.executeOnExecutor(_threadPoolListeners);
    }

    private void reportButtonRecentlyPressed(final XYDevice device, final XYDevice.ButtonType buttonType) {
        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                synchronized (_listeners) {
                    for (Map.Entry<String, Listener> entry : _listeners.entrySet()) {
                        entry.getValue().buttonRecentlyPressed(device, buttonType);
                    }
                }
                return null;
            }
        };

        asyncTask.executeOnExecutor(_threadPoolListeners);
    }

    // endregion ============= Listeners =============

    // region ============= XYDevice Listener =============

    @Override
    public void entered(XYDevice device) {reportEntered(device);}

    @Override
    public void exited(XYDevice device) {reportExited(device);}

    @Override
    public void detected(XYDevice device) {reportDetected(device);}

    @Override
    public void buttonPressed(XYDevice device, XYDevice.ButtonType buttonType) {reportButtonPressed(device, buttonType);}

    @Override
    public void buttonRecentlyPressed(XYDevice device, XYDevice.ButtonType buttonType) {reportButtonRecentlyPressed(device, buttonType);}

    @Override
    public void connectionStateChanged(XYDevice device, int newState) {

    }

    @Override
    public void updated(XYDevice device) {

    }

    // endregion ============= XYDevice Listener =============

    public void refresh(BluetoothGatt gatt) {
        XYBase.logError(TAG, "Calling refresh");
        try {
            Method refresh = gatt.getClass().getMethod("refresh");
            refresh.invoke(gatt);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
            XYBase.logException(TAG, ex);
        }
    }

    private void reportStatusChanged(Status status) {
        synchronized (_listeners) {
            for (Map.Entry<String, XYSmartScan.Listener> entry : _listeners.entrySet()) {
                entry.getValue().statusChanged(status);
            }
        }
    }

    public interface Listener {
        void entered(XYDevice device);

        void exited(XYDevice device);

        void detected(XYDevice device);

        void buttonPressed(XYDevice device, XYDevice.ButtonType buttonType);

        void buttonRecentlyPressed(XYDevice device, XYDevice.ButtonType buttonType);

        void statusChanged(Status status);

        void updated(XYDevice device);
    }
}
