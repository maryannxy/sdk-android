package com.xyfindables.sdk;

import android.bluetooth.BluetoothAdapter;
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

import com.xyfindables.core.XYBase;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
    static final int scansWithOutPulsesBeforeRestart = 100;
    private static int _missedPulsesForOutOfRange = 20;

    private static TimeUnit defaultLockTimeUnits = TimeUnit.SECONDS;
    private static long startTime = Calendar.getInstance().getTimeInMillis();

    private Status _status = Status.None;

    private final ReentrantLock _devicesLock = new ReentrantLock();

    private int _autoScanPeriod = 0;
    private int _autoScanInterval = 0;
    //private GoogleApiClient _googleApiClient;

    private TimerTask _autoScanTimerTask;

    private Timer _autoScanTimer;
    private boolean paused = false;

    private HashMap<String, XYDevice> _devices;
    int _scanCount = 0;
    int _pulseCount = 0;

    int _processedPulseCount = 0;
    int _scansWithoutPulses = 0;

    private long _currenDeviceId;

    Semaphore _scanningControl = new Semaphore(1, true);

    private final HashMap<String, XYDevice.Listener> _listeners = new HashMap<>();

    BluetoothManager getBluetoothManager(Context context) {
        return (BluetoothManager)context.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
    }

    private boolean _receiverRegistered = false;

    final BroadcastReceiver _receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action == null) {
                logError(TAG, "connTest-_receiver action is null!", false);
                return;
            }

            switch (action) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR);
                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:
                            logInfo(TAG, "BluetoothAdapter.ACTION_STATE_CHANGED:STATE_OFF");
                            setStatus(Status.BluetoothDisabled);
                            setAllToOutOfRange();
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            logInfo(TAG, "BluetoothAdapter.ACTION_STATE_CHANGED:STATE_TURNING_OFF");
                            break;
                        case BluetoothAdapter.STATE_ON:
                            logInfo(TAG, "BluetoothAdapter.ACTION_STATE_CHANGED:STATE_ON");
                            setStatus(Status.Enabled);
                            break;
                        case BluetoothAdapter.STATE_TURNING_ON:
                            logInfo(TAG, "BluetoothAdapter.ACTION_STATE_CHANGED:STATE_TURNING_ON");
                            break;
                        default:
                            logError(TAG, "BluetoothAdapter.ACTION_STATE_CHANGED:Unknwon State:" + state, true);
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
                            logInfo(TAG, "BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:SCAN_MODE_NONE");
                            break;
                        case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                            logInfo(TAG, "BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:SCAN_MODE_CONNECTABLE");
                            break;
                        case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                            logInfo(TAG, "BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:SCAN_MODE_CONNECTABLE_DISCOVERABLE");
                            break;
                        default:
                            logError(TAG, "BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:????:" + scanMode, true);
                            break;
                    }
                    switch (prevScanMode) {
                        case BluetoothAdapter.SCAN_MODE_NONE:
                            logInfo(TAG, "BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:PREV:SCAN_MODE_NONE");
                            break;
                        case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                            logInfo(TAG, "BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:PREV:SCAN_MODE_CONNECTABLE");
                            break;
                        case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                            logInfo(TAG, "BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:PREV:SCAN_MODE_CONNECTABLE_DISCOVERABLE");
                            break;
                        default:
                            logError(TAG, "BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:PREV:????:" + prevScanMode, false); // occurs too often?
                            break;
                    }
                    break;
                default:
                    logError(TAG, "Unknown Action:" + action, false);
                    break;
            }
        }
    };

    XYSmartScan() {
        super();
        logInfo(TAG, TAG);
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
                XYBase.logError(TAG, "getDevices failed due to lock:" + _devicesLock.getHoldCount(), true);
            }
        } catch (InterruptedException ex) {
            XYBase.logException(TAG, ex, true);
        }
    }

    public void init(Context context, long currentDeviceId, int missedPulsesForOutOfRange) {
        logInfo(TAG, "init");

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
            logInfo(TAG, "Bluetooth Disabled");
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
            XYBase.logError(TAG, "Tried to get a null device", true);
            return null;
        }

        id = id.toLowerCase();

        XYDevice device;
        try {
            if (_devicesLock.tryLock(defaultLockTimeout, defaultLockTimeUnits)) {
                device = _devices.get(id);
                _devicesLock.unlock();
            } else {
                logError(TAG, "deviceFromId failed due to lock (getUniqueId):" + _devicesLock.getHoldCount(), true);
                return null;
            }
        } catch (InterruptedException ex) {
            logException(TAG, ex, true);
            return null;
        }

        if (device == null) {
            device = new XYDevice(id);
            try {
                if (_devicesLock.tryLock(defaultLockTimeout, defaultLockTimeUnits)) {
                    _devices.put(id, device);
                    _devicesLock.unlock();
                } else {
                    logError(TAG, "deviceFromId failed due to lock (put):" + _devicesLock.getHoldCount(), true);
                    return null;
                }
            } catch (InterruptedException ex) {
                logException(TAG, ex, true);
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
        logExtreme(TAG, "startAutoScan:" + interval + ":" + period);
        if (_autoScanTimer != null) {
            //XYData.logError(TAG, "startAutoScan already Started (_autoScanTimer != null)");
            stopAutoScan();
        }
        if (_autoScanTimerTask != null) {
            XYBase.logError(TAG, "connTest-startAutoScan already Started (_autoScanTimerTask != null)", true);
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
        logExtreme(TAG, "stopAutoScan");
        if (_autoScanTimer != null) {
            _autoScanTimer.cancel();
            _autoScanTimer = null;
        } else {
            XYBase.logError(TAG, "stopAutoScan already Stopped (_autoScanTimer == null)", false);
        }
        if (_autoScanTimerTask != null) {
            _autoScanTimerTask.cancel();
            _autoScanTimerTask = null;
        } else {
            XYBase.logError(TAG, "stopAutoScan already Stopped (_autoScanTimerTask == null)", false);
        }
    }

    public void pauseAutoScan(boolean pause) {
        logExtreme(TAG, "pauseAutoScan:" + pause);
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
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        if (lm == null) {
            logError(TAG, "connTest-lm = null!");
            return false;
        }

        boolean gps_enabled, network_enabled;

        gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

        network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (gps_enabled && network_enabled) {
            logInfo(TAG, "returning 3");
            setStatus(Status.None);
            return true;
        } else if (gps_enabled) {
            logInfo(TAG, "returning 2");
            setStatus(Status.LocationDisabled);
            return false;
        } else if (network_enabled) {
            logInfo(TAG, "returning 1");
            setStatus(Status.LocationDisabled);
            return false;
        } else {
            logInfo(TAG, "returning 0");
            setStatus(Status.LocationDisabled);
            return false;
        }
    }

    public static boolean isLocationAvailable(@NonNull Context context) {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        if (lm == null) {
            return false;
        }

        boolean gps_enabled, network_enabled;

        gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

        network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (gps_enabled && network_enabled) {
            logInfo(TAG, "returning 3");
            return true;
        } else if (gps_enabled) {
            logInfo(TAG, "returning 2");
            return false;
        } else if (network_enabled) {
            logInfo(TAG, "returning 1");
            return false;
        } else {
            logInfo(TAG, "returning 0");
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

    public int getDeviceCount() {
        return _devices.size();
    }

    public XYDevice getDevice(int index) {
        try {
            int count = 0;
            XYDevice device = null;
            if (_devicesLock.tryLock(defaultLockTimeout, defaultLockTimeUnits)) {
                for (Map.Entry<String, XYDevice> entry : _devices.entrySet()) {
                    if (count == index) {
                        device = entry.getValue();
                        break;
                    }
                    count++;
                }
                _devicesLock.unlock();
                return device;
            } else {
                XYBase.logError(TAG, "getDevices failed due to lock:" + _devicesLock.getHoldCount(), true);
                return null;
            }
        } catch (InterruptedException ex) {
            XYBase.logException(TAG, ex, true);
            return null;
        }
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
                XYBase.logError(TAG, "getDevices failed due to lock:" + _devicesLock.getHoldCount(), true);
                return null;
            }
        } catch (InterruptedException ex) {
            XYBase.logException(TAG, ex, true);
            return null;
        }
    }

    protected String xyIdFromAppleBytes(byte[] iBeaconBytes) {
        //check for ibeacon header
        if (iBeaconBytes[0] == 0x02 && iBeaconBytes[1] == 0x15) {
            if (iBeaconBytes.length != 23) {
                XYBase.logError(TAG, "iBeacon should have 23 bytes", true);
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

            if (family == XYDevice.Family.XY2 || family == XYDevice.Family.XY3 || family == XYDevice.Family.Gps || family == XYDevice.Family.XY4) {
                minor = minor & 0xfff0 | 0x0004;
            }

            return String.format(Locale.getDefault(), "xy:%1$s:%2$s.%3$d.%4$d", XYDevice.family2prefix.get(family), uuid, major, minor).toLowerCase();
        } else {
            return null;
        }
    }

    protected void notifyDevicesOfScanComplete() {
        logExtreme(TAG, "notifyDevicesOfScanComplete");
        List<XYDevice> deviceList = getDevices();

        for (XYDevice device : deviceList) {
            device.scanComplete();
        }
    }

    public int getPulseCount() {
        return _pulseCount;
    }

    public long getPulsesPerSecond() {
        long seconds = ((Calendar.getInstance().getTimeInMillis() - startTime) / 1000);
        if (seconds == 0) {
            return _pulseCount;
        }
        return _pulseCount / seconds;
    }

    public int getScanCount() {
        return _scanCount;
    }

    protected void dump(Context context) {

        final BluetoothAdapter bluetoothAdapter = getBluetoothManager(context.getApplicationContext()).getAdapter();

        if (bluetoothAdapter == null) {
            logInfo(TAG, "Bluetooth Disabled");
            return;
        }

        logExtreme(TAG, "=========================");
        logExtreme(TAG, "bluetoothAdapter:scanMode:" + bluetoothAdapter.getScanMode());
        logExtreme(TAG, "bluetoothAdapter:state:" + bluetoothAdapter.getState());
        logExtreme(TAG, "bluetoothAdapter:name:" + bluetoothAdapter.getName());
        logExtreme(TAG, "bluetoothAdapter:address:" + bluetoothAdapter.getAddress());
        logExtreme(TAG, "bluetoothAdapter:GATT Connection State:" + bluetoothAdapter.getProfileConnectionState(BluetoothProfile.GATT));
        logExtreme(TAG, "scanCount:" + _scanCount);
        logExtreme(TAG, "pulseCount:" + _pulseCount);
        logExtreme(TAG, "processedPulseCount:" + _pulseCount);
        logExtreme(TAG, "mainLooper:" + Looper.getMainLooper().toString());
        logExtreme(TAG, "=========================");
    }

    // region ============= Listeners =============

    abstract protected void scan(final Context context, int period);

    private static class AddListenerTask extends AsyncTask<Object, Void, Void> {
        @Override
        protected Void doInBackground(Object... params) {
            synchronized (XYSmartScan.instance._listeners) {
                XYSmartScan.instance._listeners.put((String)params[0], (XYDevice.Listener)params[1]);
            }
            return null;
        }
    }

    public void addListener(final String key, final XYDevice.Listener listener) {
        Object[] params = new Object[2];
        params[0] = key;
        params[1] = listener;
        new AddListenerTask().executeOnExecutor(_threadPoolListeners, params);
    }

    private static class RemoveListenerTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            synchronized (XYSmartScan.instance._listeners) {
                XYSmartScan.instance._listeners.remove(params[0]);
            }
            return null;
        }
    }

    public void removeListener(final String key) {
        new RemoveListenerTask().executeOnExecutor(_threadPoolListeners, key);
    }

    private static class ReportEnteredTask extends AsyncTask<XYDevice, Void, Void> {
        @Override
        protected Void doInBackground(XYDevice... params) {
            synchronized (XYSmartScan.instance._listeners) {
                for (Map.Entry<String, XYDevice.Listener> entry : XYSmartScan.instance._listeners.entrySet()) {
                    entry.getValue().entered(params[0]);
                }
            }
            return null;
        }
    }

    private void reportEntered(final XYDevice device) {
        new ReportEnteredTask().executeOnExecutor(_threadPoolListeners, device);
    }

    private static class ReportExitedTask extends AsyncTask<XYDevice, Void, Void> {
        @Override
        protected Void doInBackground(XYDevice... params) {
            synchronized (XYSmartScan.instance._listeners) {
                for (Map.Entry<String, XYDevice.Listener> entry : XYSmartScan.instance._listeners.entrySet()) {
                    entry.getValue().exited(params[0]);
                }
            }
            return null;
        }
    }

    private void reportExited(final XYDevice device) {
        new ReportExitedTask().executeOnExecutor(_threadPoolListeners, device);
    }

    private static class ReportDetectedTask extends AsyncTask<XYDevice, Void, Void> {
        @Override
        protected Void doInBackground(XYDevice... params) {
            synchronized (XYSmartScan.instance._listeners) {
                for (Map.Entry<String, XYDevice.Listener> entry : XYSmartScan.instance._listeners.entrySet()) {
                    entry.getValue().detected(params[0]);
                }
            }
            return null;
        }
    }

    private void reportDetected(final XYDevice device) {
        new ReportDetectedTask().executeOnExecutor(_threadPoolListeners, device);
    }

    private static class ReportButtonPressedTask extends AsyncTask<Object, Void, Void> {
        @Override
        protected Void doInBackground(Object... params) {
            synchronized (XYSmartScan.instance._listeners) {
                for (Map.Entry<String, XYDevice.Listener> entry : XYSmartScan.instance._listeners.entrySet()) {
                    entry.getValue().buttonPressed((XYDevice)params[0], (XYDevice.ButtonType)params[1]);
                }
            }
            return null;
        }
    }

    private void reportButtonPressed(final XYDevice device, final XYDevice.ButtonType buttonType) {
        Object[] params = new Object[2];
        params[0] = device;
        params[1] = buttonType;
        new ReportButtonPressedTask().executeOnExecutor(_threadPoolListeners, params);
    }

    private static class ReportButtonRecentlyPressedTask extends AsyncTask<Object, Void, Void> {
        @Override
        protected Void doInBackground(Object... params) {
            synchronized (XYSmartScan.instance._listeners) {
                for (Map.Entry<String, XYDevice.Listener> entry : XYSmartScan.instance._listeners.entrySet()) {
                    entry.getValue().buttonRecentlyPressed((XYDevice)params[0], (XYDevice.ButtonType)params[1]);
                }
            }
            return null;
        }
    }

    private void reportButtonRecentlyPressed(final XYDevice device, final XYDevice.ButtonType buttonType) {
        Object[] params = new Object[2];
        params[0] = device;
        params[1] = buttonType;
        new ReportButtonRecentlyPressedTask().executeOnExecutor(_threadPoolListeners, params);
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
    public void readRemoteRssi(XYDevice device, int rssi) {

    }

    @Override
    public void updated(XYDevice device) {

    }

    @Override
    public void statusChanged(Status status) {

    }

    // endregion ============= XYDevice Listener =============

    public void refresh(BluetoothGatt gatt) {
        XYBase.logError(TAG, "connTest-Calling refresh", true);
        try {
            Method refresh = gatt.getClass().getMethod("refresh");
            refresh.invoke(gatt);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
            XYBase.logException(TAG, ex, true);
        }
    }

    private void reportStatusChanged(Status status) {
        synchronized (_listeners) {
            for (Map.Entry<String, XYDevice.Listener> entry : _listeners.entrySet()) {
                entry.getValue().statusChanged(status);
            }
        }
    }

}
