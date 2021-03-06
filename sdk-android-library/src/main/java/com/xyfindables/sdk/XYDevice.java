package com.xyfindables.sdk;

import android.annotation.TargetApi;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;

import com.xyfindables.core.XYSemaphore;
import com.xyfindables.core.XYBase;
import com.xyfindables.sdk.action.XYDeviceAction;
import com.xyfindables.sdk.action.XYDeviceActionGetBatterySinceCharged;
import com.xyfindables.sdk.action.XYDeviceActionSubscribeButton;
import com.xyfindables.sdk.action.XYDeviceActionSubscribeButtonModern;
import com.xyfindables.sdk.action.dialog.SubscribeSpotaNotifications;
import com.xyfindables.sdk.actionHelper.XYBattery;
import com.xyfindables.sdk.actionHelper.XYFirmware;
import com.xyfindables.sdk.bluetooth.ScanRecordLegacy;
import com.xyfindables.sdk.bluetooth.ScanResultLegacy;

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by arietrouw on 12/20/16.
 */

public class XYDevice extends XYBase {

    public static final int BATTERYLEVEL_INVALID = 0;
    public static final int BATTERYLEVEL_CHECKED = -1;
    public static final int BATTERYLEVEL_NOTCHECKED = -2;
    public static final int BATTERYLEVEL_SCHEDULED = -3;

    public static final HashMap<UUID, XYDevice.Family> uuid2family;

    public static final HashMap<XYDevice.Family, UUID> family2uuid;

    public static final HashMap<XYDevice.Family, String> family2prefix;

    private static final int MAX_BLECONNECTIONS = 4;
    private static final XYSemaphore _bleAccess = new XYSemaphore(MAX_BLECONNECTIONS, true);

    private static final int MAX_ACTIONS = 1;
    private final XYSemaphore _actionLock = new XYSemaphore(MAX_ACTIONS, true);

    private boolean _connectIntent = false;

    public void setConnectIntent(boolean value) {
        _connectIntent = value;
    }

    private int _rssi;

    private boolean _isConnected = false;
    private int _connectionCount = 0;
    private boolean _stayConnected = false;
    private boolean _stayConnectedActive = false;
    private boolean _isInOtaMode = false;

    private static int _missedPulsesForOutOfRange = 20;
    private static int _actionTimeout = 30000;

    private BluetoothGatt _gatt;

    private static ThreadPoolExecutor _threadPool;
    private static int _instanceCount;

    private int _enterCount = 0;
    private int _exitCount = 0;
    private int _detectCount = 0;
    private int _actionQueueCount = 0;
    private int _actionFailCount = 0;
    private int _actionSuccessCount = 0;
    private long _firstDetectedTime = 0;
    private Context _connectedContext;

    static {
        _threadPool = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        _instanceCount = 0;
    }

    static {
        uuid2family = new HashMap<>();
        uuid2family.put(UUID.fromString("a500248c-abc2-4206-9bd7-034f4fc9ed10"), XYDevice.Family.XY1);
        uuid2family.put(UUID.fromString("07775dd0-111b-11e4-9191-0800200c9a66"), XYDevice.Family.XY2);
        uuid2family.put(UUID.fromString("08885dd0-111b-11e4-9191-0800200c9a66"), XYDevice.Family.XY3);
        uuid2family.put(UUID.fromString("a44eacf4-0104-0000-0000-5f784c9977b5"), XYDevice.Family.XY4);
        uuid2family.put(UUID.fromString("735344c9-e820-42ec-9da7-f43a2b6802b9"), XYDevice.Family.Mobile);
        uuid2family.put(UUID.fromString("9474f7c6-47a4-11e6-beb8-9e71128cae77"), XYDevice.Family.Gps);

        family2uuid = new HashMap<>();
        family2uuid.put(XYDevice.Family.XY1, UUID.fromString("a500248c-abc2-4206-9bd7-034f4fc9ed10"));
        family2uuid.put(XYDevice.Family.XY2, UUID.fromString("07775dd0-111b-11e4-9191-0800200c9a66"));
        family2uuid.put(XYDevice.Family.XY3, UUID.fromString("08885dd0-111b-11e4-9191-0800200c9a66"));
        family2uuid.put(XYDevice.Family.XY4, UUID.fromString("a44eacf4-0104-0000-0000-5f784c9977b5"));
        family2uuid.put(XYDevice.Family.Mobile, UUID.fromString("735344c9-e820-42ec-9da7-f43a2b6802b9"));
        family2uuid.put(XYDevice.Family.Gps, UUID.fromString("9474f7c6-47a4-11e6-beb8-9e71128cae77"));

        family2prefix = new HashMap<>();
        family2prefix.put(XYDevice.Family.XY1, "ibeacon");
        family2prefix.put(XYDevice.Family.XY2, "ibeacon");
        family2prefix.put(XYDevice.Family.XY3, "ibeacon");
        family2prefix.put(XYDevice.Family.XY4, "ibeacon");
        family2prefix.put(XYDevice.Family.Mobile, "mobiledevice");
        family2prefix.put(XYDevice.Family.Gps, "gps");
        family2prefix.put(XYDevice.Family.Near, "near");
    }

    public static Comparator<XYDevice> Comparator = new Comparator<XYDevice>() {
        @Override
        public int compare(XYDevice lhs, XYDevice rhs) {
            return lhs._id.compareTo(rhs._id);
        }
    };

    private static final String TAG = XYDevice.class.getSimpleName();

    private static int outOfRangeRssi = -999;
    public final static Comparator<XYDevice> DistanceComparator = new Comparator<XYDevice>() {
        @Override
        public int compare(XYDevice lhs, XYDevice rhs) {
            return Integer.compare(lhs.getRssi(), rhs.getRssi());
        }
    };

    private String _id;

    private int _batteryLevel = BATTERYLEVEL_NOTCHECKED;
    private long _timeSinceCharged = -1;
    private String _firmwareVersion = null;
    private String _beaconAddress = null;
    private Boolean _simActivated = false;

    private int _scansMissed;
    private boolean _buttonRecentlyPressed = false;

    private ScanResultLegacy _currentScanResult18;
    private ScanResult _currentScanResult21;

    private final HashMap<String, Listener> _listeners = new HashMap<>();
    private XYDeviceAction _currentAction;

    XYDevice(String id) {
        super();
        _missedPulsesForOutOfRange = XYSmartScan.instance.getMissedPulsesForOutOfRange();
        _instanceCount++;
        _id = normalizeId(id);
        if (_id == null) {
            _id = id;
        }
    }

    public int getEnterCount() {
        return _enterCount;
    }

    public int getExitCount() {
        return _exitCount;
    }

    public int getDetectCount() {
        return _detectCount;
    }

    public int getActionFailCount() {
        return _actionFailCount;
    }

    public int getActionSuccessCount() {
        return _actionSuccessCount;
    }

    public int getActionQueueCount() {
        return _actionQueueCount;
    }

    @Override
    protected void finalize() {
        _instanceCount--;
        //noinspection EmptyCatchBlock
        try {
            super.finalize();
        } catch (Throwable ex) {
        }
    }

    public static int getInstanceCount() {
        return _instanceCount;
    }

    static public String buildId(Family family, int major, int minor) {
        UUID uuid = getUUID(family);
        if (uuid == null) {
            logError(TAG, "Invalid Family", true);
            return null;
        }
        if ((family == Family.XY2) || (family == Family.XY3) || (family == Family.Gps) || (family == Family.XY4)) {
            return "xy:" + getPrefix(family) + ":" + uuid.toString() + "." + major + "." + (minor | 0x0004);
        } else {
            return "xy:" + getPrefix(family) + ":" + uuid.toString() + "." + major + "." + minor;
        }
    }

    public static String normalizeId(String id) {
        String normalId = null;
        UUID uuid = getUUID(id);
        if (uuid != null) {
            Family family = getFamily(uuid);
            if ((family == Family.XY2) || (family == Family.XY3) || (family == Family.Gps) || (family == Family.XY4)) {
                normalId = "xy:" + getPrefix(id) + ":" + uuid.toString() + "." + getMajor(id) + "." + ((getMinor(id) & 0xfff0) | 0x0004);
            } else {
                normalId = id;
            }
        }
        if (normalId != null) {
            return normalId.toLowerCase();
        } else {
            return id.toLowerCase();
        }
    }

    public BluetoothGatt getGatt() {
        return _gatt;
    }

    private void setGatt(BluetoothGatt gatt) {

        if (gatt == _gatt) {
            logError(TAG, "connTest-trying to set same gatt", false);
            return;
        }

        // if _gatt is not closed, and we set new _gatt, then we keep reference to old _gatt connection which still exists in ble stack
        if (_gatt != null) {
            logExtreme(TAG,"connTest-_gatt.close!!!");
            _gatt.close();
            // we could set a timer here to null the scanResult if new scanResult is not seen in x seconds
            releaseBleLock();
        }
        XYBase.logExtreme(TAG, "connTest-setGatt = " + gatt + ": previous _gatt = " + _gatt);
        _gatt = gatt;
    }

    public int getRssi() {
        if (_gatt != null) {
            _gatt.readRemoteRssi();
            return _rssi;
        } else {
            if (XYSmartScan.instance.legacy()) {
                return getRssi18();
            } else {
                return getRssi21();
            }
        }
    }

    @TargetApi(18)
    private int getRssi18() {
        if (_currentScanResult18 == null) {
            return outOfRangeRssi;
        } else {
            return _currentScanResult18.getRssi();
        }
    }

    @TargetApi(21)
    private int getRssi21() {
        if (_currentScanResult21 == null) {
            return outOfRangeRssi;
        } else {
            return _currentScanResult21.getRssi();
        }
    }

    private int getTxPowerLevel() {
        if (XYSmartScan.instance.legacy()) {
            return getTxPowerLevel18();
        } else {
            return getTxPowerLevel21();
        }
    }

    @TargetApi(18)
    protected int txPowerLevelFromScanResult18(ScanResultLegacy scanResult) {
        int tx = 0;
        if (scanResult != null) {
            ScanRecordLegacy scanRecord = scanResult.getScanRecord();
            if (scanRecord != null) {
                byte[] manufacturerData = scanResult.getScanRecord().getManufacturerSpecificData(0x004c);
                if (manufacturerData != null) {
                    tx = manufacturerData[22];
                }
            }
        }
        return tx;
    }

    @TargetApi(21)
    protected int txPowerLevelFromScanResult21(ScanResult scanResult) {
        int tx = 0;
        if (scanResult != null) {
            ScanRecord scanRecord = scanResult.getScanRecord();
            if (scanRecord != null) {
                byte[] manufacturerData = scanResult.getScanRecord().getManufacturerSpecificData(0x004c);
                if (manufacturerData != null) {
                    tx = manufacturerData[22];
                }
            }
        }
        return tx;
    }

    @TargetApi(18)
    private int getTxPowerLevel18() {
        if (_currentScanResult18 == null) {
            return 0;
        } else {
            return txPowerLevelFromScanResult18(_currentScanResult18);
        }
    }

    @TargetApi(21)
    private int getTxPowerLevel21() {
        if (_currentScanResult21 == null) {
            return 0;
        } else {
            return txPowerLevelFromScanResult21(_currentScanResult21);
        }
    }

    private SubscribeSpotaNotifications _subscribeSpotaNotifications = null;

    public SubscribeSpotaNotifications getSpotaNotifications() {
        return _subscribeSpotaNotifications;
    }

    public void setSpotaNotifications(SubscribeSpotaNotifications value) {
        _subscribeSpotaNotifications = value;
    }

    public void startSpotaNotifications(Context context) {
        if (_subscribeSpotaNotifications != null) {
            _subscribeSpotaNotifications.start(context);
        }
    }

    public void stopSpotaNotifications() {
        if (_subscribeSpotaNotifications != null) {
            _subscribeSpotaNotifications.stop();
        }
    }

    private XYDeviceActionSubscribeButton _subscribeButton = null;
    private XYDeviceActionSubscribeButtonModern _subscribeButtonModern = null;

    public void otaMode(boolean value) {
        logExtreme(TAG, "otaMode set: " + value);
        if (value == _isInOtaMode) {
            return;
        }
        XYSmartScan.instance.pauseAutoScan(value);
        _isInOtaMode = value;
    }

    public void stayConnected(final Context context, boolean value) {
        _connectedContext = context;

        logExtreme(TAG, "stayConnected:" + value + ":" + getId() + " _stayConnectedActive = " + _stayConnectedActive);
        if (value == _stayConnected) {
            return;
        }
        _stayConnected = value;
        if (_stayConnected) {
            if (!_stayConnectedActive) {
                startSubscribeButton();
            }
        } else {
            if (_stayConnectedActive) {
                _stayConnectedActive = false;
                stopSubscribeButton();
                if (_subscribeButton != null) {
                    _subscribeButton.stop();
                    _subscribeButton = null;
                }
                popConnection();
                Log.v(TAG, "connTest-popConnection4");
            }
        }
    }

    private Timer _actionFrameTimer = null;

    private void startActionTimer() {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if (_isInOtaMode) {
                    return;
                }
                if (_currentAction == null) {
                    logError(TAG, "connTest-Null Action timed out", false);
                    // this will be called in endActionFrame so only needs to be called if somehow _currentAction is null and timer is not
                    cancelActionTimer();
                } else {
                    logError(TAG, "connTest-Action Timeout", false);
                    endActionFrame(_currentAction, false);
                }
                // below should not be necessary since cancelActionTimer is called in endActionFrame
                // this may have been protecting vs somehow having null action inside endActionFrame, but currently seems to cause crash (race condition?)
//                if (_actionFrameTimer != null) {
//                    _actionFrameTimer.cancel();
//                    _actionFrameTimer = null;
//                }
            }
        };

        _actionFrameTimer = new Timer("ActionTimer");
        try {
            _actionFrameTimer.schedule(timerTask, _actionTimeout);
        } catch (IllegalStateException ex) {
            logError(TAG, ex.toString(), true);
        }
        _actionTimeout = 30000;
    }

    private void cancelActionTimer() {
        if (_actionFrameTimer == null) {
            logError(TAG, "connTest-Null _actionFrameTimer", false);
        } else {
            logExtreme(TAG, "connTest-_actionFrameTimer being cancelled");
            _actionFrameTimer.cancel();
            _actionFrameTimer = null;
        }
    }

    private void releaseBleLock() {
        _bleAccess.release();
        XYBase.logInfo(TAG, "connTest_bleAccess released[" + getId() + "]: " + _bleAccess.availablePermits() + "/" + MAX_BLECONNECTIONS + ":" + getId());
    }

    private void releaseActionLock() {
        logExtreme(TAG, "connTest-_actionLock" + _actionLock.availablePermits());
        _actionLock.release();
    }

    private int startActionFrame(XYDeviceAction action) {

        logExtreme(TAG, "startActionFrame");
        action.statusChanged(XYDeviceAction.STATUS_QUEUED, null, null, true);
//        try {
//            if (!_actionLock.tryAcquire(_actionTimeout, TimeUnit.MILLISECONDS)) {
//                XYBase.logError(TAG, "_actionLock:failed:" + action.getClass().getSuperclass().getSimpleName(), false);
//                return 0;
//            } else {

        // acquireUninterruptibly is used so lock is not release when thread expires (due to calling discoverServices)
        _actionLock.acquireUninterruptibly();
        logExtreme(TAG, "_actionLock[" + getId() + "]:acquired");
        action.statusChanged(XYDeviceAction.STATUS_STARTED, null, null, true);
        logExtreme(TAG, "startActionFrame-action started");
        _currentAction = action;
        pushConnection();
        startActionTimer();
        return action.hashCode();
//            }
//        } catch (InterruptedException ex) {
//            XYBase.logError(TAG, "Service Load Semaphore interrupted");
//            return 0;
//        }
    }

    public void endOta() {
        otaMode(false);
//        popConnection();
        XYBase.logExtreme(TAG, "connTest-popConnection2");
    }

    private void endActionFrame(XYDeviceAction action, boolean success) {
        logExtreme(TAG, "connTest-endActionFrame: success = " + success + ": otaMode = " + _isInOtaMode);
        if (action == null) {
            logError(TAG, "connTest-Ending Null Action", false);
            return;
        }
        if (success) {
            _actionSuccessCount++;
        } else {
            _actionFailCount++;

        }
        cancelActionTimer();
        action.statusChanged(XYDeviceAction.STATUS_COMPLETED, null, null, success);
        logExtreme(TAG, "connTest-_actionLock[" + getId() + "]:release");
        popConnection();
        _currentAction = null;
//        _connectIntent = false;
        releaseActionLock();
        XYSmartScan.instance.pauseAutoScan(false);
        logExtreme(TAG, "connTest-popConnection1-pauseAutoScan set back to false");
    }

    public final void runOnUiThread(final Context context, Runnable action) {
        Handler handler = new Handler(context.getMainLooper());
        handler.post(action);
    }

    // pass timeout param if need be, otherwise default at 60000 MS
    public AsyncTask queueAction(final Context context, final XYDeviceAction action) {
        queueAction(context, action, 0);
        return null;
    }

    public AsyncTask queueAction(final Context context, final XYDeviceAction action, final int timeout) {

        if (BuildConfig.DEBUG) {
            logExtreme(TAG, "connTest-queueAction-action = " + action.getClass().getSuperclass().getSimpleName());
        }

        if (timeout > 0) {
            _actionTimeout = timeout;
        }

        _actionQueueCount++;

        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {

                if (getBluetoothDevice() == null) {
                    logError(TAG, "connTest-getBluetoothDevice() == null", false);
                    // need to complete action here
                    action.statusChanged(XYDeviceAction.STATUS_COMPLETED, null, null, false);
                    return null;
                }

                XYBase.logInfo(TAG, "connTest-connect[" + getId() + "]:" + _connectionCount);
                int actionLock = startActionFrame(action);
                if (actionLock == 0) {
                    XYDeviceAction currentAction = _currentAction;
                    closeGatt();
                    XYBase.logExtreme(TAG, "connTest-closeGatt2");
                    if (currentAction != null) {
                        logError(TAG, "connTest-statusChanged:failed to get actionLock", false);
                        endActionFrame(currentAction, false);
                    }
                    return null;
                }

                final BluetoothGattCallback callback = new BluetoothGattCallback() {
                    @Override
                    public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
                        super.onConnectionStateChange(gatt, status, newState);

                        XYBase.logInfo(TAG, "connTest-onConnectionStateChange:" + status + ":" + newState + ":" + getId());
                        switch (newState) {
                            case BluetoothGatt.STATE_CONNECTED:
                                _isConnected = true;
                                logExtreme(TAG, "connTest-STATE_CONNECTED status is " + status);
                                logExtreme(TAG, "connTest-_connectIntent = " + _connectIntent);
//                                _connectIntent = false;
                                logInfo(TAG, "onConnectionStateChange:Connected: " + getId());
                                reportConnectionStateChanged(STATE_CONNECTED);
                                // call gatt.discoverServices() in ui thread?
                                gatt.discoverServices();
                                logExtreme(TAG, "connTest-discoverServices called in STATE_CONNECTED");
                                break;
                            case BluetoothGatt.STATE_DISCONNECTED: {
                                logInfo(TAG, "connTest-onConnectionStateChange:Disconnected: " + getId());
                                if (status == 133) {
                                    if (!_connectIntent) {
                                        _isConnected = false;
                                    }
                                    // 133 may disconnect device -> in this case stayConnected can leave us with surplus connectionCount and no way to re-subscribe to button
//                                    stayConnected(null, false); // hacky?
                                    /* Ignoring the 133 seems to keep the connection alive.
                                    No idea why, but it does on Android 5.1 */
                                    logError(TAG, "connTest-Disconnect with 133", false);
                                    if (!_isInOtaMode && !_connectIntent /*&& (_connectionCount <= 1)*/) {
                                        reportConnectionStateChanged(STATE_DISCONNECTED);
                                        //popConnection();  //hacky?
                                        logError(TAG, "connTest-disconnect inside 133", false);
                                        XYDeviceAction currentAction = _currentAction;
                                        if (currentAction != null) {
                                            endActionFrame(currentAction, false);
                                        } else {
                                            logError(TAG, "connTest-trying to disconnect inside 133 with null currentAction", false);
                                        }
                                        setGatt(null); // need to close gatt on device we no longer see, this will cause gatt to be null when pop connection is called since it is delayed 6 seconds
                                    }
                                    //XYSmartScan.instance.refresh(gatt);
                                } else {
                                    _isConnected = false;
                                    reportConnectionStateChanged(STATE_DISCONNECTED);  // redundant bc called inside closeGatt inside endActionFrame as well? Just add inside gatt!=null ?
                                    XYDeviceAction currentAction = _currentAction;
                                    if (currentAction != null) {
                                        endActionFrame(currentAction, false);
                                    } else {
                                        logError(TAG, "connTest-trying to disconnect with null currentAction", false);
                                    }
                                    setGatt(null); // need to close gatt on device we no longer see, this will cause gatt to be null when pop connection is called since it is delayed 6 seconds

                                }
                                break;
                            }
                            case BluetoothGatt.STATE_DISCONNECTING:
                                logInfo(TAG, "onConnectionStateChange:Disconnecting: " + getId());
                                reportConnectionStateChanged(STATE_DISCONNECTING);
                                break;
                            default:
                                logError(TAG, "onConnectionStateChange:Unknown State: " + newState + ":" + getId(), false);
                                XYDeviceAction currentAction = _currentAction;
                                closeGatt();
                                logExtreme(TAG, "connTest-closeGatt4");
                                if (currentAction != null) {
                                    logError(TAG, "statusChanged:unknown", false);
                                    endActionFrame(currentAction, false);
                                }
                                break;
                        }
                        if (status != BluetoothGatt.GATT_SUCCESS) {
                            logError(TAG, "connTest-Bad Status: " + status, false);
                            XYSmartScan.instance.setStatus(XYSmartScan.Status.BluetoothUnstable);
                            /*XYDeviceAction currentAction = _currentAction;
                            if (currentAction != null) {
                                logError(TAG, "statusChanged:badstatus:" + status, false);
                                endActionFrame(currentAction, false);
                            }*/
                        }
                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {

                        super.onServicesDiscovered(gatt, status);

                        final XYDeviceAction currentAction = _currentAction;
                        XYBase.logExtreme(TAG, "connTest-onServicesDiscovered");
                        if (currentAction != null) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                logInfo(TAG, "connTest-onServicesDiscovered:" + status);
                                currentAction.statusChanged(XYDeviceAction.STATUS_SERVICE_FOUND, gatt, null, true);
                                // concurrent mod ex on many devices with below line
                                BluetoothGattService service = gatt.getService(currentAction.getServiceId());
                                if (service != null) {
                                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(currentAction.getCharacteristicId());
                                    logExtreme(TAG, "connTest-onServicesDiscovered-service not null");
                                    if (characteristic != null) {
                                        if (currentAction.statusChanged(XYDeviceAction.STATUS_CHARACTERISTIC_FOUND, gatt, characteristic, true)) {
                                            logExtreme(TAG, "connTest-onServicesDiscovered-characteristic not null");
                                            endActionFrame(currentAction, false);
                                        } else {
                                            logExtreme(TAG, "connTest-do nothing"); // herein lies the issue- something in endActionFrame is triggering 133-timing? executing actions too close together?
                                        }
                                    } else {
                                        logError(TAG, "connTest-statusChanged:characteristic null", false); // this happens a decent amount. What is causing this?
                                        endActionFrame(currentAction, false);
                                    }
                                } else {
                                    logError(TAG, "connTest-statusChanged:service null, gatt = " + gatt + " currentAction = " + currentAction, false); // this happens a decent amount. What is causing this?
                                    endActionFrame(currentAction, false);
                                }
                            } else {
                                logError(TAG, "connTest-statusChanged:onServicesDiscovered Failed: " + status, true);
                                endActionFrame(currentAction, false);
                            }
                        } else {
                            logError(TAG, "connTest-null _currentAction", true);
                        }
                    }

                    @Override
                    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                        super.onCharacteristicRead(gatt, characteristic, status);

                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            logInfo(TAG, "onCharacteristicRead:" + status);
                            if (_currentAction != null && _currentAction.statusChanged(XYDeviceAction.STATUS_CHARACTERISTIC_READ, gatt, characteristic, true)) {
                                endActionFrame(_currentAction, true);
                            }
                        } else {
                            logError(TAG, "onCharacteristicRead Failed: " + status, false);
                            endActionFrame(_currentAction, false);
                        }
                    }

                    @Override
                    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                        super.onCharacteristicWrite(gatt, characteristic, status);

                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            logInfo(TAG, "onCharacteristicWrite:" + status);
                            if (_currentAction != null && _currentAction.statusChanged(XYDeviceAction.STATUS_CHARACTERISTIC_WRITE, gatt, characteristic, true)) {
                                endActionFrame(_currentAction, true);
                            }
                        } else {
                            logError(TAG, "onCharacteristicWrite Failed: " + status, false);
                            endActionFrame(_currentAction, false);
                        }
                    }

                    @Override
                    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                        super.onCharacteristicChanged(gatt, characteristic);

                        logInfo(TAG, "onCharacteristicChanged");
                        if (_subscribeButton != null && !_isInOtaMode) {
                            _subscribeButton.statusChanged(XYDeviceAction.STATUS_CHARACTERISTIC_UPDATED, gatt, characteristic, true);
                        }
                        if (_subscribeButtonModern != null && !_isInOtaMode) {
                            _subscribeButtonModern.statusChanged(XYDeviceAction.STATUS_CHARACTERISTIC_UPDATED, gatt, characteristic, true);
                        }
                        if (_subscribeSpotaNotifications != null && _isInOtaMode) {
                            _subscribeSpotaNotifications.statusChanged(XYDeviceAction.STATUS_CHARACTERISTIC_UPDATED, gatt, characteristic, true);
                        }
                    }

                    @Override
                    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                        super.onDescriptorRead(gatt, descriptor, status);

                        logInfo(TAG, "onDescriptorRead:" + status);
                    }

                    @Override
                    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                        super.onDescriptorWrite(gatt, descriptor, status);

                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            logInfo(TAG, "onDescriptorWrite:" + status);
                            if (_currentAction != null && _currentAction.statusChanged(descriptor, XYDeviceAction.STATUS_CHARACTERISTIC_WRITE, gatt, true)) {
                                endActionFrame(_currentAction, true);
                            }
                        } else {
                            logError(TAG, "onDescriptorWrite Failed: " + status, false);
                            endActionFrame(_currentAction, false);
                        }
                        logInfo(TAG, "onDescriptorWrite: " + descriptor + " : status = " + status);
                    }

                    @Override
                    public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
                        super.onReliableWriteCompleted(gatt, status);

                        logInfo(TAG, "onReliableWriteCompleted:" + status);
                    }

                    @Override
                    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                        super.onReadRemoteRssi(gatt, rssi, status);

                        logExtreme(TAG, "testRssi-onReadRemoteRssi rssi = " + rssi);
                        _rssi = rssi;
                        reportReadRemoteRssi(rssi);
                    }

                    @Override
                    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                        super.onMtuChanged(gatt, mtu, status);

                        logInfo(TAG, "onMtuChanged:" + status);
                    }

                };
                if (getGatt() == null) {
                    //stopping the scan and running the connect in ui thread required for 4.x
                    XYSmartScan.instance.pauseAutoScan(true);
                    logExtreme(TAG, "connTest-pauseAutoScan(true)");

                    try {
                        XYBase.logInfo(TAG, "connTest-_bleAccess acquiring[" + getId() + "]:" + _bleAccess.availablePermits() + "/" + MAX_BLECONNECTIONS + ":" + getId());
                        if (_bleAccess.tryAcquire(10, TimeUnit.SECONDS)) {
                            XYBase.logInfo(TAG, "connTest_bleAccess acquired[" + getId() + "]: " + _bleAccess.availablePermits() + "/" + MAX_BLECONNECTIONS + ":" + getId());
                            // below is commented out to prevent release being called in UI
                            //stopping the scan and running the connect in ui thread required for 4.x - also required for Samsung Galaxy s7 7.0- and likely other phones as well

                            Handler handler = new Handler(context.getApplicationContext().getMainLooper());
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    BluetoothDevice bluetoothDevice = getBluetoothDevice();
                                    if (bluetoothDevice == null) {
                                        logError(TAG, "connTest-No Bluetooth Adapter!", false);
                                        endActionFrame(_currentAction, false);
                                        releaseBleLock();
                                        logExtreme(TAG, "connTest-release3");
                                    } else {
                                        final BluetoothGatt gatt;
                                        if (android.os.Build.VERSION.SDK_INT >= 23) {
                                            gatt = bluetoothDevice.connectGatt(context.getApplicationContext(), false, callback, android.bluetooth.BluetoothDevice.TRANSPORT_LE);
                                        } else {
                                            gatt = bluetoothDevice.connectGatt(context.getApplicationContext(), false, callback);
                                        }
                                        setGatt(gatt);
                                        if (gatt == null) {
                                            logExtreme(TAG, "gatt is null");
                                            endActionFrame(_currentAction, false);
                                            releaseBleLock();
                                            logExtreme(TAG, "connTest-release4");
                                        } else {
                                            final boolean connected = gatt.connect();
                                            logExtreme(TAG, "connTest-Connect:" + connected);
                                            // some sources say must wait 600 ms after connect before discoverServices
                                            // other sources say call gatt.discoverServices in UI thread
                                            // 133s seem to start after gatt.connect is called
//                                        gatt.discoverServices();
                                        }
                                    }
                                }
                            });
                        } else {
                            logError(TAG, "connTest-_bleAccess not acquired", false);
                            endActionFrame(_currentAction, false);
                        }
                    } catch (InterruptedException ex) {
                        logError(TAG, "connTest-not acquired: interrupted", true);
                        endActionFrame(_currentAction, false);
                    }
                } else {
                    logExtreme(TAG, "connTest-already have Gatt");
                    BluetoothGatt gatt = getGatt();
                    if (gatt == null) {
                        logExtreme(TAG, "gatt is null");
                        endActionFrame(_currentAction, false);
                        releaseBleLock();
                        logExtreme(TAG, "connTest-release5");
                    } else {
                        // should already be connected but just in case -> is now commented out because cause issues calling this when already connected on some phones
//                            boolean connected = gatt.connect();
                        // null pointer exception here, somehow gatt is null?
                        List<BluetoothGattService> services = gatt.getServices();
                        if (services.size() > 0) {
                            callback.onServicesDiscovered(gatt, BluetoothGatt.GATT_SUCCESS);
                        } else {
                            if (!gatt.discoverServices()) {
                                logExtreme(TAG, "connTest-FAIL discoverServices");
                                endActionFrame(_currentAction, false);
                            } else {
                                logExtreme(TAG, "connTest-discoverServices called inside callback if gatt not null");
                            }
                        }
                    }
                }
                return null;
            }
        };
        asyncTask.executeOnExecutor(_threadPool);
        return asyncTask;
    }

    public double getDistanceWithCustomTx(int tx) {

        double rssi = getRssi();

        if (tx == 0 || rssi == 0 || rssi == outOfRangeRssi) {
            return -1.0;
        } else {
            double ratio = rssi * 1.0 / tx;
            if (ratio < 1.0) {
                return Math.pow(ratio, 10);
            } else {
                return (0.89976) * Math.pow(ratio, 7.7095) + 0.111;
            }
        }
    }

    public double getDistance() {
        int tx = getTxPowerLevel();

        if (getFamily() == Family.XY4) {
            tx = -60;
        } else {
            tx = -60;
        }

        double rssi = getRssi();

        if (rssi == outOfRangeRssi) {
            return -2.0;
        }

        if (tx == 0 || rssi == 0) { // this could cause ui "searching" because rssi = 0 when no values returning
            return -1.0;
        } else {
            double ratio = rssi * 1.0 / tx;
            if (ratio < 1.0) {
                return Math.pow(ratio, 10);
            } else {
                return (0.89976) * Math.pow(ratio, 7.7095) + 0.111;
            }
        }
    }

    private double getDistance(int rssi) {

        int tx = getTxPowerLevel();

        if (getFamily() == Family.XY4) {
            tx = -60;
        } else {
            tx = -60;
        }

        if (rssi == outOfRangeRssi) {
            return -2.0;
        }

        if (tx == 0 || rssi == 0) {
            return -1.0;
        } else {
            double ratio = rssi * 1.0 / tx;
            if (ratio < 1.0) {
                return Math.pow(ratio, 10);
            } else {
                return (0.89976) * Math.pow(ratio, 7.7095) + 0.111; // made for tx of -59 I believe so works with most our devices, but does not work with xy4 of tx -75
            }
        }
    }

    public String getBeaconAddress() {
        return _beaconAddress;
    }

    public String getFirmwareVersion() {
        return _firmwareVersion;
    }

    public void addTemporaryConnection() {
        _connectionCount++;
    }

    public void removeTemporaryConnection() {
        _connectionCount--;
    }

    private void pushConnection() {
        if (_currentScanResult21 != null || _currentScanResult18 != null) {
            if (BuildConfig.DEBUG) {
                String action = null;
                if (_currentAction != null) {
                    action = _currentAction.getClass().getSuperclass().getSimpleName();
                }
                logExtreme(TAG, "connTest-pushConnection[" + _connectionCount + "->" + (_connectionCount + 1) + "]:" + getId() + ": " + action);
            }
            if (_currentAction == null) {
                logError(TAG, "connTest-null currentAction", true);
            }
            _connectionCount++;
        } else {
            logExtreme(TAG, "connTest-pushConnection-no scan result");
        }
    }

    public void popConnection() {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if (BuildConfig.DEBUG) {
                    String action = null;
                    if (_currentAction != null) {
                        action = _currentAction.getClass().getSuperclass().getSimpleName();
                    }
                    logExtreme(TAG, "connTest-popConnection[" + _connectionCount + "->" + (_connectionCount - 1) + "]:" + getId() + ": " + action);
                }
                _connectionCount--;
                if (_connectionCount < 0) {
                    logError(TAG, "connTest-Negative Connection Count:" + getId(), false);
                    _connectionCount = 0;
                    // logic flaw??? if connCount < 0, its just 0, we do not close or other
                } else {
                    if (_connectionCount == 0) {
                        if (_stayConnectedActive) {
                            _subscribeButton = null;
                            _subscribeButtonModern = null;
//                            // did not call stop?
//                            stopSubscribeButton(); do this instead of above two lines?
                            _stayConnectedActive = false;
                        }
                        BluetoothGatt gatt = getGatt();
                        if (gatt != null) {
                            logExtreme(TAG, "connTest-gatt.disconnect!!!!!!!!!!");
                            gatt.disconnect();
                            logExtreme(TAG, "connTest-closeGatt1");
                            closeGatt();
                        } else {
                            // this should occur when out of range or take battery out since there is 6 second delay, and gatt will be set null in disconnect
                            logError(TAG, "connTest-popConnection gat is null!", false);
                        }
                    }
                }
            }
        };
        Timer timer = new Timer();
        timer.schedule(timerTask, 6000);
    }

    public boolean isConnected() {
        return _isConnected;
    }

    private void closeGatt() {
        logExtreme(TAG, "closeGatt");
        if (_connectionCount > 0) {
            logError(TAG, "Closing GATT with open connections!", true);
            _connectionCount = 0;
            /* try moving just the disconnect into below block
            in case close without disconnect causes issue unregistering client inside close

            BluetoothGatt gatt = getGatt();
            if (gatt != null) {
                gatt.disconnect();
                Log.v(TAG, "gatt.disconnect");
            } */
        }
        if (_connectionCount < 0) {
            logError(TAG, "Closing GATT with negative connections!", false);
            _connectionCount = 0;
        }
        final BluetoothGatt gatt = getGatt();
        if (gatt == null) {
            logError(TAG, "Closing Null Gatt", true);
            releaseBleLock();
            logExtreme(TAG, "connTest-release1");
        } else {
            // trying to add disconnect here to see if improved behavior, read above comment
            // could make difference in case connectionCount is 0 when it should be 1
            // this was already called-try removing below
//            gatt.disconnect();
//            logExtreme(TAG, "connTest-gatt.disconnect!!!!!!!!!!!");
            // may be a timing issue calling close immediately after disconnect - try waiting 100 ms
            // changing this seems to have fixed many unexpected disconnections after executing actions
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                logError(TAG, "connTest-" + ex.toString(), true);
            }
            gatt.close();
            XYBase.logExtreme(TAG, "connTest-gatt.close inside closeGatt");
//            setGatt(null);
//            releaseBleLock();
//            logExtreme(TAG, "connTest-release2");
        }
        _currentAction = null; //just to make sure
        if (_actionLock.availablePermits() == 0) {
            _actionLock.release(MAX_ACTIONS);
            logExtreme(TAG, "_actionLock releaseMAX");
        }
        reportConnectionStateChanged(STATE_DISCONNECTED);
    }

    private static final int STATE_CONNECTED = BluetoothProfile.STATE_CONNECTED;
    private static final int STATE_DISCONNECTED = BluetoothProfile.STATE_DISCONNECTED;
    private static final int STATE_DISCONNECTING = BluetoothProfile.STATE_DISCONNECTING;

    private BluetoothManager getBluetoothManager(Context context) {
        return (BluetoothManager) context.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
    }

    private BluetoothDevice getBluetoothDevice() {
        BluetoothDevice bluetoothDevice;
        bluetoothDevice = getBluetoothDevice21();
        if (bluetoothDevice == null) {
            bluetoothDevice = getBluetoothDevice18();
        }
        return bluetoothDevice;
    }

    void pulseOutOfRange() {
        logExtreme(TAG,"connTest-pulseOutOfRange device = " + getId());
        if (_stayConnectedActive) {
            _stayConnectedActive = false;
            popConnection();
            _isConnected = false;
            // disconnect should trigger onConnectionStateChange which calls setGatt(null)
            if (_gatt != null) {
                _gatt.disconnect();
                // calling this in case disconnect callback is not called when ble is off
                setGatt(null);
            }
            logExtreme(TAG, "connTest-popConnection3");
        }
        if (XYSmartScan.instance.legacy()) {
            pulseOutOfRange18();
        } else {
            pulseOutOfRange21();
        }
    }

    public int getBatteryLevel() {
        return _batteryLevel;
    }

    public long getTimeSinceCharged() {
        return _timeSinceCharged;
    }

    public void checkBatteryAndVersion(final Context context) {
        logExtreme(TAG, "checkBatteryAndVersion");
        checkBattery(context, true);
        checkVersion(context);
        checkTimeSinceCharged(context);
    }

    public void checkBatteryAndVersionInFuture(final Context context, final boolean repeat) {
        logExtreme(TAG, "checkBatteryInFuture");
        if (_batteryLevel == BATTERYLEVEL_NOTCHECKED) {
            _batteryLevel = BATTERYLEVEL_SCHEDULED;
            final TimerTask checkTimerTask = new TimerTask() {
                @Override
                public void run() {
                    if (repeat) {
                        checkBattery(context, true);
                    } else {
                        checkBattery(context);
                    }
                    checkVersion(context);
                    checkTimeSinceCharged(context);
                }
            };

            final Timer pumpTimer = new Timer();
            Random random = new Random(new Date().getTime());
            //random check in next 6-12 minutes
            int delay = random.nextInt(360000) + 360000;
            logExtreme(TAG, "checkBatteryInFuture:" + delay);
            if (repeat) {
                pumpTimer.schedule(checkTimerTask, delay, delay);
            } else {
                pumpTimer.schedule(checkTimerTask, delay);
            }
        }
    }

    private void checkVersion(final Context context) {
        logExtreme(TAG, "checkFirmware");
        if (_firmwareVersion == null) {
            _firmwareVersion = "";
            XYFirmware getVersion = new XYFirmware(this, new XYFirmware.Callback() {
                @Override
                public void started(boolean success, String value) {
                    if (success) {
                        _firmwareVersion = value;
                        reportDetected();
                    }
                }

                @Override
                public void completed(boolean success) {

                }
            });
            getVersion.start(context);
        }
    }

    private void checkBattery(final Context context) {
        checkBattery(context.getApplicationContext(), false);
    }

    public void checkBattery(final Context context, boolean force) {
        logExtreme(TAG, "checkBattery");
        if (_batteryLevel < BATTERYLEVEL_CHECKED || force) {
            _batteryLevel = BATTERYLEVEL_CHECKED;
            logExtreme(TAG, "batteryTest-read battery level for id = " + getId());
            XYBattery battery = new XYBattery(this, new XYBattery.Callback() {
                @Override
                public void started(boolean success, int value) {
                    if (success) {
                        _batteryLevel = value;
                        reportDetected();
                    }
                }

                @Override
                public void completed(boolean success) {

                }
            });
            battery.start(context);
        }
    }

    private void checkTimeSinceCharged(final Context context) {
        if (getFamily() == Family.Gps) {
            logExtreme(TAG, "checkTimeSinceCharged");
            XYDeviceActionGetBatterySinceCharged getTimeSinceCharged = new XYDeviceActionGetBatterySinceCharged(this) {
                @Override
                public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    if (status == STATUS_CHARACTERISTIC_READ) {
                        if (success) {
                            byte[] value = this.value;
                            _timeSinceCharged = 0;
                            for (int i = 0; i < 4; i++) {
                                _timeSinceCharged <<= 8;
                                _timeSinceCharged ^= (long) value[i] & 0xFF;
                            }
                            reportDetected();
                        }
                    }
                    return result;
                }
            };
            getTimeSinceCharged.start(context.getApplicationContext());
        }
    }

    @TargetApi(18)
    private BluetoothDevice getBluetoothDevice18() {
        logExtreme(TAG, "getBluetoothDevice18");
        ScanResultLegacy scanResult = _currentScanResult18;
        if (scanResult == null) {
            return null;
        } else {
            return scanResult.getDevice();
        }
    }

    @TargetApi(21)
    private BluetoothDevice getBluetoothDevice21() {
        logExtreme(TAG, "getBluetoothDevice21");
        ScanResult scanResult = _currentScanResult21;
        if (scanResult == null) {
            return null;
        } else {
            return scanResult.getDevice();
        }
    }

    @TargetApi(18)
    private void pulseOutOfRange18() {
        logExtreme(TAG, "pulseOutOfRange18");
        ScanResultLegacy scanResult = _currentScanResult18;
        if (scanResult != null) {
            ScanResultLegacy newScanResult = new ScanResultLegacy(scanResult.getDevice(), scanResult.getScanRecord(), outOfRangeRssi, scanResult.getTimestampNanos());
            pulse18(newScanResult);
        }
    }

    @TargetApi(21)
    private void pulseOutOfRange21() {
//        Log.v(TAG, "pulseOutOfRange21: " + _id);
        ScanResult scanResult = _currentScanResult21;
        if (scanResult != null) {
            ScanResult newScanResult = new ScanResult(scanResult.getDevice(), scanResult.getScanRecord(), outOfRangeRssi, scanResult.getTimestampNanos());
            pulse21(newScanResult);
        }
    }

    @TargetApi(18)
    void pulse18(ScanResultLegacy scanResult) {
        logExtreme(TAG, "pulse18: " + _id + ":" + scanResult.getRssi());
        _scansMissed = 0;

        if (getFamily() == Family.XY3 || getFamily() == Family.Gps || getFamily() == Family.XY4) {
            ScanRecordLegacy scanRecord = scanResult.getScanRecord();
            if (scanRecord != null) {
                byte[] manufacturerData = scanResult.getScanRecord().getManufacturerSpecificData(0x004c);
                if (manufacturerData != null) {
                    if ((manufacturerData[21] & 0x08) == 0x08 && scanResult.getRssi() != outOfRangeRssi) {
                        if (getFamily() == Family.Gps || getFamily() == Family.XY4) {
                            if (_currentScanResult18 == null) {
                                _currentScanResult18 = scanResult;
                                reportEntered();
                                reportDetected();
                            }
                        }
                        handleButtonPulse();
                        return;
                    }
                }
            }
        }

        if ((_currentScanResult18 == null) || ((_currentScanResult18.getRssi() == outOfRangeRssi) && (scanResult.getRssi() != outOfRangeRssi))) {
            _currentScanResult18 = scanResult;
            reportEntered();
            reportDetected();
            if (!_stayConnectedActive && _stayConnected) {
                startSubscribeButton();
            }
        } else if ((_currentScanResult18.getRssi() != outOfRangeRssi) && (scanResult.getRssi() == outOfRangeRssi)) {
            _currentScanResult18 = null;
            reportExited();
        } else if (scanResult.getRssi() != outOfRangeRssi) {
            _currentScanResult18 = scanResult;
            reportDetected();
            if (!_stayConnectedActive && _stayConnected) {
                startSubscribeButton();
            }
        }
        if (_beaconAddress == null) {
            _beaconAddress = scanResult.getDevice().getAddress();
        }
    }

    @TargetApi(21)
    void pulse21(Object scanResultObject) {

        android.bluetooth.le.ScanResult scanResult = (android.bluetooth.le.ScanResult) scanResultObject;

//        logExtreme(TAG, "pulse21: " + _id + ":" + scanResult.getRssi());
        _scansMissed = 0;

        if (getFamily() == Family.XY3 || getFamily() == Family.Gps || getFamily() == Family.XY4) {
            android.bluetooth.le.ScanRecord scanRecord = scanResult.getScanRecord();
            if (scanRecord != null) {
                byte[] manufacturerData = scanResult.getScanRecord().getManufacturerSpecificData(0x004c);

                if (manufacturerData != null) {
                    if ((manufacturerData[21] & 0x08) == 0x08 && scanResult.getRssi() != outOfRangeRssi) {
                        if (getFamily() == Family.Gps || getFamily() == Family.XY4) {
                            if (_currentScanResult21 == null) {
                                _currentScanResult21 = scanResult;
                                reportEntered();
                                reportDetected();
                            }
                        }
                        handleButtonPulse();
                        logExtreme(TAG, "handleButtonPulse");
                        return;
                    }
                }
            }
        }

        if ((_currentScanResult21 == null) || ((_currentScanResult21.getRssi() == outOfRangeRssi) && (scanResult.getRssi() != outOfRangeRssi))) {
            _currentScanResult21 = scanResult;
            reportEntered();
            reportDetected();
            if (!_stayConnectedActive && _stayConnected) {
                startSubscribeButton();
            }
        } else if ((_currentScanResult21.getRssi() != outOfRangeRssi) && (scanResult.getRssi() == outOfRangeRssi)) {
            _currentScanResult21 = null;
            reportExited();
        } else if (scanResult.getRssi() != outOfRangeRssi) {
            _currentScanResult21 = scanResult;
            reportDetected();
            if (!_stayConnectedActive && _stayConnected) {
                startSubscribeButton();
            }
        }
        if (_beaconAddress == null) {
            _beaconAddress = scanResult.getDevice().getAddress();
        }
    }

    public String getId() {
        return _id;
    }

    private void stopSubscribeButton() {
        if (_subscribeButton != null) {
            _subscribeButton.stop();
            _subscribeButton = null;
        }

        if (_subscribeButtonModern != null) {
            _subscribeButtonModern.stop();
            _subscribeButtonModern = null;
        }
        XYBase.logExtreme(TAG, "connTest-stopSubscribeButton[" + _connectionCount + "->" + (_connectionCount - 1) + "]:" + getId());
        // erase fake connection count we used before to keep connection alive
        popConnection();
    }

    private Timer _buttonPressedTimer = null;
    private TimerTask _buttonPressedTimerTask = null;

    private void startSubscribeButton() {
        if (_connectedContext == null) {
            return;
        }
        if (_bleAccess.availablePermits() <= 1) {
            _stayConnected = false;
            return;
        }
        _stayConnectedActive = true;
        logExtreme(TAG, "connTest-startSubscribeButton[" + _connectionCount + "->" + (_connectionCount + 1) + "]:" + getId());
        _connectionCount++; // do not use pushConnection here because then this action will be null inside pushConnection and throw error

        if (getFamily() == Family.XY4) {
            _subscribeButtonModern = new XYDeviceActionSubscribeButtonModern(this) {
                @Override
                public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    if (status == STATUS_CHARACTERISTIC_UPDATED) {
                        int buttonValue = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                        logExtreme(TAG, "ButtonCharacteristicUpdated:" + buttonValue);
                        _buttonRecentlyPressed = true;
                        if (_buttonPressedTimer != null) {
                            _buttonPressedTimer.cancel();
                            _buttonPressedTimer = null;
                        }
                        if (_buttonPressedTimerTask != null) {
                            _buttonPressedTimerTask.cancel();
                            _buttonPressedTimerTask = null;
                        }
                        _buttonPressedTimer = new Timer();
                        _buttonPressedTimerTask = new TimerTask() {
                            @Override
                            public void run() {
                                _buttonRecentlyPressed = false;
                            }
                        };
                        _buttonPressedTimer.schedule(_buttonPressedTimerTask, 40000);
                        switch (buttonValue) {
                            case XYDeviceActionSubscribeButton.BUTTONPRESS_SINGLE:
                                reportButtonPressed(ButtonType.Single);
                                break;
                            case XYDeviceActionSubscribeButton.BUTTONPRESS_DOUBLE:
                                reportButtonPressed(ButtonType.Double);
                                break;
                            case XYDeviceActionSubscribeButton.BUTTONPRESS_LONG:
                                reportButtonPressed(ButtonType.Long);
                                break;
                            default:
                                logError(TAG, "Invalid Button Value:" + buttonValue, true);
                                break;
                        }
                    }
                    return result;
                }
            };
            _subscribeButtonModern.start(_connectedContext.getApplicationContext());
        } else {
            _subscribeButton = new XYDeviceActionSubscribeButton(this) {
                @Override
                public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    if (status == STATUS_CHARACTERISTIC_UPDATED) {
                        int buttonValue = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                        logExtreme(TAG, "ButtonCharacteristicUpdated:" + buttonValue);
                        _buttonRecentlyPressed = true;
                        _buttonRecentlyPressed = true;
                        if (_buttonPressedTimer != null) {
                            _buttonPressedTimer.cancel();
                            _buttonPressedTimer = null;
                        }
                        if (_buttonPressedTimerTask != null) {
                            _buttonPressedTimerTask.cancel();
                            _buttonPressedTimerTask = null;
                        }
                        _buttonPressedTimer = new Timer();
                        _buttonPressedTimerTask = new TimerTask() {
                            @Override
                            public void run() {
                                _buttonRecentlyPressed = false;
                            }
                        };
                        _buttonPressedTimer.schedule(_buttonPressedTimerTask, 40000);
                        switch (buttonValue) {
                            case XYDeviceActionSubscribeButton.BUTTONPRESS_SINGLE:
                                reportButtonPressed(ButtonType.Single);
                                break;
                            case XYDeviceActionSubscribeButton.BUTTONPRESS_DOUBLE:
                                reportButtonPressed(ButtonType.Double);
                                break;
                            case XYDeviceActionSubscribeButton.BUTTONPRESS_LONG:
                                reportButtonPressed(ButtonType.Long);
                                break;
                            default:
                                logError(TAG, "Invalid Button Value:" + buttonValue, true);
                                break;
                        }
                    }
                    return result;
                }
            };
            _subscribeButton.start(_connectedContext.getApplicationContext());
        }
    }

    public Boolean getSimActivated() {
        return _simActivated;
    }

    public void setSimActivated(Boolean active) {
        _simActivated = active;
    }

    public static UUID getUUID(String id) {
        String[] parts = id.split(":");
        if (parts.length != 3) {
            logError(TAG, "getUUID: wrong number of parts [" + id + "] : " + parts.length, true);
            return null;
        }

        String[] parts2 = parts[2].split("\\.");
        if (parts2.length != 3) {
            logError(TAG, "getUUID: wrong number of parts2 [" + id + "] : " + parts2.length, true);
            return null;
        }
        try {
            return UUID.fromString(parts2[0]);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public UUID getUUID() {
        return getUUID(_id);
    }

    public static String getPrefix(String id) {
        String[] parts = id.split(":");
        if (parts.length < 3) {
            logError(TAG, "getPrefix: wrong number of parts [" + id + "] : " + parts.length, true);
            return "unknown";
        }

        return parts[1];
    }

    public static String getPrefix(Family family) {
        return family2prefix.get(family);
    }

    public String getPrefix() {
        return getPrefix(_id);
    }

    public static int getMajor(String id) {
        String[] parts = id.split(":");
        if (parts.length != 3) {
            logError(TAG, "getMajor: wrong number of parts [" + id + "] : " + parts.length, true);
            return -1;
        }

        String[] parts2 = parts[2].split("\\.");
        if (parts2.length != 3) {
            logError(TAG, "getMajor: wrong number of parts2 [" + id + "] : " + parts2.length, true);
            return -1;
        }
        return Integer.parseInt(parts2[1]);
    }

    public int getMajor() {
        return getMajor(_id);
    }

    public static int getMinor(String id) {
        String[] parts = id.split(":");
        if (parts.length != 3) {
            logError(TAG, "getMinor: wrong number of parts [" + id + "] : " + parts.length, true);
            return -1;
        }

        String[] parts2 = parts[2].split("\\.");
        if (parts2.length != 3) {
            logError(TAG, "getMinor: wrong number of parts2 [" + id + "] : " + parts2.length, true);
            return -1;
        }
        return Integer.parseInt(parts2[2]);
    }

    private long _lastUpdateTime = 0;

    public boolean isUpdateSignificant() {
        if (_lastUpdateTime == 0) {
            _lastUpdateTime = System.currentTimeMillis();
            return false;
        }
        long currentTime = System.currentTimeMillis();
        if (currentTime - _lastUpdateTime > 1800000) {
            _lastUpdateTime = currentTime;
            return true;
        } else {
            return false;
        }
    }

    public int getMinor() {
        return getMinor(_id);
    }

    public static Family getFamily(UUID uuid) {
        Family family = uuid2family.get(uuid);
        if (family == null) {
            return Family.Unknown;
        }
        return family;
    }

    public Family getFamily() {
        Family familyFromUUID = getFamily(getUUID());
        if (familyFromUUID != Family.Unknown) {
            return familyFromUUID;
        } else {
            String prefix = getPrefix();
            switch (prefix) {
                case "near":
                    return Family.Near;
                case "mobiledevice":
                    return Family.Mobile;
                default:
                    return Family.Unknown;
            }
        }
    }

    public static UUID getUUID(Family family) {
        return family2uuid.get(family);
    }

    public void clearScanResults() {
        _currentScanResult18 = null;
        _currentScanResult21 = null;
        pulseOutOfRange();
    }

    public Proximity getProximity() {

        double distance = getDistance();

        if (distance < -1.0) {
            return Proximity.OutOfRange;
        }
        if (distance < 0.0) {
            return Proximity.None;
        }
        if (distance < 0.0173) {
            return Proximity.Touching;
        }
        if (distance < 1.0108) {
            return Proximity.VeryNear;
        }
        if (distance < 3.0639) {
            return Proximity.Near;
        }
        if (distance < 8.3779) {
            return Proximity.Medium;
        }
        if (distance < 20.6086) {
            return Proximity.Far;
        }
        return Proximity.VeryFar;

//        int currentRssi = getRssi();
//
//        if (currentRssi == 0) {
//            return Proximity.None;
//        } else if (currentRssi >= -40) { .01734153
//            return Proximity.Touching;
//        } else if (currentRssi >= -60) { 1.01076
//            return Proximity.VeryNear;
//        } else if (currentRssi >= -70) { 3.06392867
//            return Proximity.Near;
//        } else if (currentRssi >= -80) { 8.37788453900756
//            return Proximity.Medium;
//        } else if (currentRssi >= -90) { 20.6085636568
//            return Proximity.Far;
//        } else if (currentRssi > outOfRangeRssi) {
//            return Proximity.VeryFar;
//        }
//
//        return Proximity.OutOfRange;
    }

    public Proximity getProximity(int currentRssi) {

        double distance = getDistance(currentRssi);

        if (distance < -1.0) {
            return Proximity.OutOfRange;
        }
        if (distance < 0.0) {
            return Proximity.None;
        }
        if (distance < 0.0173) {
            return Proximity.Touching;
        }
        if (distance < 1.0108) {
            return Proximity.VeryNear;
        }
        if (distance < 3.0639) {
            return Proximity.Near;
        }
        if (distance < 8.3779) {
            return Proximity.Medium;
        }
        if (distance < 20.6086) {
            return Proximity.Far;
        }
        return Proximity.VeryFar;


//        if (currentRssi == 0) {
//            return Proximity.None;
//        } else if (currentRssi >= -40) {
//            return Proximity.Touching;
//        } else if (currentRssi >= -60) {
//            return Proximity.VeryNear;
//        } else if (currentRssi >= -70) {
//            return Proximity.Near;
//        } else if (currentRssi >= -80) {
//            return Proximity.Medium;
//        } else if (currentRssi >= -90) {
//            return Proximity.Far;
//        } else if (currentRssi > outOfRangeRssi) {
//            return Proximity.VeryFar;
//        }
//
//        return Proximity.OutOfRange;
    }

    void scanComplete() {
        if (isConnected()) {
            _scansMissed = 0;
        } else {
            _scansMissed++;
        }
        if (_scansMissed > _missedPulsesForOutOfRange) {
            logExtreme(TAG, "connTest-_scansMissed > _missedPulsesForOutOfRange(20)");
            _scansMissed = -999999999; //this is here to prevent double exits
            pulseOutOfRange();
        }
    }

    // region =========== Listeners ============

    public void addListener(String key, Listener listener) {
        synchronized (_listeners) {
            _listeners.put(key, listener);
        }
    }

    public void removeListener(String key) {
        synchronized (_listeners) {
            _listeners.remove(key);
        }
    }

    private void reportEntered() {
//        Log.i(TAG, "reportEntered");
        _enterCount++;
        synchronized (_listeners) {
            for (Map.Entry<String, Listener> entry : _listeners.entrySet()) {
                entry.getValue().entered(this);
            }
        }
    }

    private void reportExited() {
//        Log.i(TAG, "reportExit");
        _exitCount++;
        synchronized (_listeners) {
            for (Map.Entry<String, Listener> entry : _listeners.entrySet()) {
                entry.getValue().exited(this);
            }
        }
    }

    private void reportDetected() {
//        Log.i(TAG, "reportDetected");
        if (_firstDetectedTime == 0) {
            _firstDetectedTime = System.currentTimeMillis();
        }
        _detectCount++;
        synchronized (_listeners) {
            for (Map.Entry<String, Listener> entry : _listeners.entrySet()) {
                entry.getValue().detected(this);
            }
        }
    }

    private void handleButtonPulse() {
//        Log.v(TAG, "handleButtonPulse");
        if (_buttonRecentlyPressed) {
            //reportButtonRecentlyPressed(ButtonType.Single);
        } else {
            reportButtonPressed(ButtonType.Single);
            _buttonRecentlyPressed = true;

            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    _buttonRecentlyPressed = false;
                }
            };
            Timer timer = new Timer();
            timer.schedule(timerTask, 30000);
        }
    }

    private void reportButtonPressed(ButtonType buttonType) {
//        Log.v(TAG, "reportButtonPressed");
        synchronized (_listeners) {
            for (Map.Entry<String, Listener> entry : _listeners.entrySet()) {
                entry.getValue().buttonPressed(this, buttonType);
            }
        }
    }

    private void reportButtonRecentlyPressed(ButtonType buttonType) {
//        Log.v(TAG, "reportButtonRecentlyPressed");
        synchronized (_listeners) {
            for (Map.Entry<String, Listener> entry : _listeners.entrySet()) {
                entry.getValue().buttonRecentlyPressed(this, buttonType);
            }
        }
    }

    private void reportConnectionStateChanged(int newState) {
        logExtreme(TAG, "reportConnectionStateChanged[" + getId() + "]:" + newState);
        synchronized (_listeners) {
            for (Map.Entry<String, Listener> entry : _listeners.entrySet()) {
                entry.getValue().connectionStateChanged(this, newState);
            }
        }
    }

    private void reportUpdated() {
        logExtreme(TAG, "reportUpdated[" + getId() + "]");
        synchronized (_listeners) {
            for (Map.Entry<String, Listener> entry : _listeners.entrySet()) {
                entry.getValue().updated(this);
            }
        }
    }

    private void reportReadRemoteRssi(int rssi) {
        logExtreme(TAG, "reportReadRemoteRssi[" + getId() + "]:" + rssi);
        synchronized (_listeners) {
            for (Map.Entry<String, Listener> entry : _listeners.entrySet()) {
                entry.getValue().readRemoteRssi(this, rssi);
            }
        }
    }

    public enum Family {
        Unknown,
        XY1,
        XY2,
        XY3,
        Mobile,
        Gps,
        Near,
        XY4
    }

    public enum ButtonType {
        None,
        Single,
        Double,
        Long
    }

    public enum Proximity {
        None,
        OutOfRange,
        VeryFar,
        Far,
        Medium,
        Near,
        VeryNear,
        Touching
    }

    public interface Listener {
        void entered(final XYDevice device);

        void exited(final XYDevice device);

        void detected(final XYDevice device);

        void buttonPressed(final XYDevice device, final ButtonType buttonType);

        void buttonRecentlyPressed(final XYDevice device, final ButtonType buttonType);

        void connectionStateChanged(final XYDevice device, int newState);

        void readRemoteRssi(final XYDevice device, int rssi);

        void updated(final XYDevice device);

        void statusChanged(XYSmartScan.Status status);
    }
// endregion =========== Listeners ============
}
