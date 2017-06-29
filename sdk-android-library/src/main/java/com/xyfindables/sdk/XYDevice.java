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
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;

import com.xyfindables.core.XYSemaphore;
import com.xyfindables.core.XYBase;
import com.xyfindables.sdk.action.XYDeviceAction;
import com.xyfindables.sdk.action.XYDeviceActionGetBatteryLevel;
import com.xyfindables.sdk.action.XYDeviceActionGetBatterySinceCharged;
import com.xyfindables.sdk.action.XYDeviceActionGetSIMId;
import com.xyfindables.sdk.action.XYDeviceActionGetVersion;
import com.xyfindables.sdk.action.XYDeviceActionOtaWrite;
import com.xyfindables.sdk.action.XYDeviceActionSubscribeButton;
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

    private int _connectionCount = 0;
    private boolean _stayConnected = false;
    private boolean _stayConnectedActive = false;
    private boolean _isInOtaMode = false;

    private static int _missedPulsesForOutOfRange = 20;
    private static int _actionTimeout = 60000;

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

    static {
        _threadPool = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        _instanceCount = 0;
    }

    static {
        uuid2family = new HashMap<>();
        uuid2family.put(UUID.fromString("a500248c-abc2-4206-9bd7-034f4fc9ed10"), XYDevice.Family.XY1);
        uuid2family.put(UUID.fromString("07775dd0-111b-11e4-9191-0800200c9a66"), XYDevice.Family.XY2);
        uuid2family.put(UUID.fromString("08885dd0-111b-11e4-9191-0800200c9a66"), XYDevice.Family.XY3);
        uuid2family.put(UUID.fromString("735344c9-e820-42ec-9da7-f43a2b6802b9"), XYDevice.Family.Mobile);
        uuid2family.put(UUID.fromString("9474f7c6-47a4-11e6-beb8-9e71128cae77"), XYDevice.Family.Gps);

        family2uuid = new HashMap<>();
        family2uuid.put(XYDevice.Family.XY1, UUID.fromString("a500248c-abc2-4206-9bd7-034f4fc9ed10"));
        family2uuid.put(XYDevice.Family.XY2, UUID.fromString("07775dd0-111b-11e4-9191-0800200c9a66"));
        family2uuid.put(XYDevice.Family.XY3, UUID.fromString("08885dd0-111b-11e4-9191-0800200c9a66"));
        family2uuid.put(XYDevice.Family.Mobile, UUID.fromString("735344c9-e820-42ec-9da7-f43a2b6802b9"));
        family2uuid.put(XYDevice.Family.Gps, UUID.fromString("9474f7c6-47a4-11e6-beb8-9e71128cae77"));

        family2prefix = new HashMap<>();
        family2prefix.put(XYDevice.Family.XY1, "ibeacon");
        family2prefix.put(XYDevice.Family.XY2, "ibeacon");
        family2prefix.put(XYDevice.Family.XY3, "ibeacon");
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

    private HashMap<String, Listener> _listeners = new HashMap<>();
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
            XYBase.logError(TAG, "Invalid Family");
            return null;
        }
        if ((family == Family.XY2) || (family == Family.XY3) || (family == Family.Gps)) {
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
            if ((family == Family.XY2) || (family == Family.XY3) || (family == Family.Gps)) {
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
        _gatt = gatt;
    }

    public int getRssi() {
        if (XYSmartScan.instance.legacy()) {
            return getRssi18();
        } else {
            return getRssi21();
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

    public int getTxPowerLevel() {
        if (XYSmartScan.instance.legacy()) {
            return getRssi18();
        } else {
            return getRssi21();
        }
    }

    @TargetApi(18)
    private int getTxPowerLevel18() {
        if (_currentScanResult18 == null) {
            return 0;
        } else {
            return _currentScanResult18.getScanRecord().getTxPowerLevel();
        }
    }

    @TargetApi(21)
    private int getTxPowerLevel21() {
        if (_currentScanResult21 == null) {
            return 0;
        } else {
            return _currentScanResult21.getScanRecord().getTxPowerLevel();
        }
    }

    private XYDeviceActionSubscribeButton _subscribeButton = null;

    public void otaMode(boolean value) {
        Log.v(TAG, "testOta-otaMode set: " + value);
        if (value == _isInOtaMode) {
            return;
        }
        _isInOtaMode = value;
    }

    public void stayConnected(final Context context, boolean value) {
        Log.v(TAG, "stayConnected:" + value + ":" + getId());
        if (value == _stayConnected) {
            return;
        }
        _stayConnected = value;
        if (_stayConnected) {
            if (!_stayConnectedActive) {
                _stayConnectedActive = true;
                pushConnection();
                _subscribeButton = new XYDeviceActionSubscribeButton(this) {
                    @Override
                    public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                        boolean result = super.statusChanged(status, gatt, characteristic, success);
                        if (status == STATUS_CHARACTERISTIC_UPDATED) {
                            int buttonValue = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                            Log.v(TAG, "ButtonCharacteristicUpdated:" + buttonValue);
                            _buttonRecentlyPressed = true;
                            TimerTask timerTask = new TimerTask() {
                                @Override
                                public void run() {
                                    _buttonRecentlyPressed = false;
                                }
                            };
                            Timer timer = new Timer();
                            timer.schedule(timerTask, 40000);
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
                                    XYBase.logError(TAG, "Invalid Button Value:" + buttonValue);
                                    break;
                            }
                        }
                        return result;
                    }
                };
                _subscribeButton.start(context.getApplicationContext());
            }
        } else {
            if (_stayConnectedActive) {
                _stayConnectedActive = false;
                _subscribeButton.stop();
                _subscribeButton = null;
                popConnection();
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
                    XYBase.logError(TAG, "Null Action timed out", false);
                } else {
                    XYBase.logError(TAG, "Action Timeout:" + _currentAction.getClass().getSuperclass().getSimpleName(), false);
                    endActionFrame(_currentAction, false);
                }
                if (_actionFrameTimer != null) {
                    _actionFrameTimer.cancel();
                    _actionFrameTimer = null;
                }
            }
        };

        _actionFrameTimer = new Timer("ActionTimer");
        _actionFrameTimer.schedule(timerTask, _actionTimeout);
        _actionTimeout = 60000;
    }

    private void cancelActionTimer() {
        if (_actionFrameTimer == null) {
            XYBase.logError(TAG, "Null _actionFrameTimer", false);
        } else {
            _actionFrameTimer.cancel();
            _actionFrameTimer = null;
        }
    }

    private void releaseBleLock() {
        Log.i(TAG, "_bleAccess released[" + getId() + "]: " + _bleAccess.availablePermits() + "/" + MAX_BLECONNECTIONS + ":" + getId());
        _bleAccess.release();
    }

    private int startActionFrame(XYDeviceAction action) {
        Log.v(TAG, "testOta-startActionFrame");
        pushConnection();
        action.statusChanged(XYDeviceAction.STATUS_QUEUED, null, null, true);
        try {
            if (!_actionLock.tryAcquire(_actionTimeout, TimeUnit.MILLISECONDS)) {
                XYBase.logError(TAG, "testOta-_actionLock:failed:" + action.getClass().getSuperclass().getSimpleName(), false);
                return 0;
            } else {
                Log.v(TAG, "_actionLock[" + getId() + "]:acquired:" + action.getClass().getSuperclass().getSimpleName());
                action.statusChanged(XYDeviceAction.STATUS_STARTED, null, null, true);
                Log.i(TAG, "testOta-startActionFrame-action started");
                _currentAction = action;
                startActionTimer();
                return action.hashCode();
            }
        } catch (InterruptedException ex) {
            XYBase.logError(TAG, "testOta-Service Load Semaphore interrupted");
            return 0;
        }
    }

    private void endActionFrame(XYDeviceAction action, boolean success) {
        Log.v(TAG, "testOta-endActionFrame: success = " + success + ": otaMode = " + _isInOtaMode);
        if (action == null) {
            XYBase.logError(TAG, "Ending Null Action", false);
            return;
        }
        if (success) {
            _actionSuccessCount++;
        } else {
            _actionFailCount++;
        }
        cancelActionTimer();
        action.statusChanged(XYDeviceAction.STATUS_COMPLETED, null, null, success);
        _currentAction = null;
        Log.v(TAG, "_actionLock[" + getId() + "]:release:" + action.getClass().getSuperclass().getSimpleName());
        _actionLock.release();
        XYSmartScan.instance.pauseAutoScan(false);
        popConnection();
    }

    // pass timeout param if need be, otherwise default at 60000 MS
    public AsyncTask queueAction(final Context context, final XYDeviceAction action) {
        queueAction(context, action, 0);
        return null;
    }

    public final void runOnUiThread(final Context context, Runnable action) {
        Handler handler = new Handler(context.getMainLooper());
        handler.post(action);
    }

    public AsyncTask queueAction(final Context context, final XYDeviceAction action, final int timeout) {

        if (getBluetoothDevice() == null) {
            return null;
        }

        if (timeout > 0) {
            _actionTimeout = timeout;
        }

        _actionQueueCount++;

        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Log.i(TAG, "connect[" + getId() + "]:" + _connectionCount);
                int actionLock = startActionFrame(action);
                if (actionLock == 0) {
                    XYDeviceAction currentAction = _currentAction;
                    closeGatt();
                    if (currentAction != null) {
                        XYBase.logError(TAG, "statusChanged:failed to get actionLock", false);
                        endActionFrame(currentAction, false);
                    }
                    return null;
                }

                final BluetoothGattCallback callback = new BluetoothGattCallback() {
                    @Override
                    public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
                        super.onConnectionStateChange(gatt, status, newState);
                        if (_currentAction != null) {
                            Log.i(TAG, "Action:" + _currentAction.getClass().getSuperclass().getSimpleName());
                        }
                        Log.i(TAG, "onConnectionStateChange:" + status + ":" + newState + ":" + getId());
                        switch (newState) {
                            case BluetoothGatt.STATE_CONNECTED:
                                Log.i(TAG, "onConnectionStateChange:Connected: " + getId());
                                reportConnectionStateChanged(STATE_CONNECTED);
                                gatt.discoverServices();
                                Log.v(TAG, "stateConnected: gatt object = " + gatt.hashCode());
                                break;
                            case BluetoothGatt.STATE_DISCONNECTED: {
                                if (status == 133) {
                                    /* Ignoring the 133 seems to keep the connection alive.
                                    No idea why, but it does on Android 5.1 */
                                    XYBase.logError(TAG, "Disconnect with 133", false);
                                    //XYSmartScan.instance.refresh(gatt);
                                } else {
                                    Log.i(TAG, "onConnectionStateChange:Disconnected: " + getId());
                                    XYDeviceAction currentAction = _currentAction;
                                    closeGatt();
                                    if (currentAction != null) {
                                        XYBase.logError(TAG, "statusChanged:disconnected", false);
                                        endActionFrame(currentAction, false);
                                    }
                                }
                                break;
                            }
                            case BluetoothGatt.STATE_DISCONNECTING:
                                Log.i(TAG, "onConnectionStateChange:Disconnecting: " + getId());
                                reportConnectionStateChanged(STATE_DISCONNECTING);
                                break;
                            default:
                                XYBase.logError(TAG, "onConnectionStateChange:Unknown State: " + newState + ":" + getId());
                                XYDeviceAction currentAction = _currentAction;
                                closeGatt();
                                if (currentAction != null) {
                                    XYBase.logError(TAG, "statusChanged:unknown", false);
                                    endActionFrame(currentAction, false);
                                }
                                break;
                        }
                        if (status != BluetoothGatt.GATT_SUCCESS) {
                            XYBase.logError(TAG, "Bad Status: " + status, false);
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
                        XYDeviceAction currentAction = _currentAction;
                        if (currentAction != null) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                Log.i(TAG, "onServicesDiscovered:" + status);
                                currentAction.statusChanged(XYDeviceAction.STATUS_SERVICE_FOUND, gatt, null, true);
                                BluetoothGattService service = gatt.getService(currentAction.getServiceId());
                                if (service != null) {
                                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(currentAction.getCharacteristicId());
                                    if (characteristic != null) {
                                        if (currentAction.statusChanged(XYDeviceAction.STATUS_CHARACTERISTIC_FOUND, gatt, characteristic, true)) {
                                            endActionFrame(currentAction, false);
                                        }
                                    } else {
                                        XYBase.logError(TAG, "statusChanged:characteristic null", false);
                                        endActionFrame(currentAction, false);
                                    }
                                } else {
                                    XYBase.logError(TAG, "statusChanged:service null", false);
                                    endActionFrame(currentAction, false);
                                }
                            } else {
                                XYBase.logError(TAG, "statusChanged:onServicesDiscovered Failed: " + status);
                                endActionFrame(currentAction, false);
                            }
                        } else {
                            logError(TAG, "null _currentAction");
                        }
                    }

                    @Override
                    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                        super.onCharacteristicRead(gatt, characteristic, status);
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            Log.i(TAG, "onCharacteristicRead:" + status);
                            if (_currentAction.statusChanged(XYDeviceAction.STATUS_CHARACTERISTIC_READ, gatt, characteristic, true)) {
                                endActionFrame(_currentAction, true);
                            }
                        } else {
                            XYBase.logError(TAG, "onCharacteristicRead Failed: " + status);
                            endActionFrame(_currentAction, false);
                        }
                    }

                    @Override
                    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                        super.onCharacteristicWrite(gatt, characteristic, status);
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            Log.i(TAG, "onCharacteristicWrite:" + status);
                            if (_currentAction.statusChanged(XYDeviceAction.STATUS_CHARACTERISTIC_WRITE, gatt, characteristic, true)) {
                                endActionFrame(_currentAction, true);
                            }
                        } else {
                            XYBase.logError(TAG, "onCharacteristicWrite Failed: " + status);
                            endActionFrame(_currentAction, false);
                        }
                    }

                    @Override
                    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                        super.onCharacteristicChanged(gatt, characteristic);
                        Log.i(TAG, "onCharacteristicChanged");
                        if (_subscribeButton != null) {
                            _subscribeButton.statusChanged(XYDeviceAction.STATUS_CHARACTERISTIC_UPDATED, gatt, characteristic, true);
                        }
                    }

                    @Override
                    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                        super.onDescriptorRead(gatt, descriptor, status);
                        Log.i(TAG, "onDescriptorRead:" + status);
                    }

                    @Override
                    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                        super.onDescriptorWrite(gatt, descriptor, status);
                        Log.i(TAG, "onDescriptorWrite:" + status);
                    }

                    @Override
                    public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
                        super.onReliableWriteCompleted(gatt, status);
                        Log.i(TAG, "onReliableWriteCompleted:" + status);
                    }

                    @Override
                    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                        super.onReadRemoteRssi(gatt, rssi, status);
                        Log.i(TAG, "onReadRemoteRssi:" + status);
                    }

                    @Override
                    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                        super.onMtuChanged(gatt, mtu, status);
                        Log.i(TAG, "onMtuChanged:" + status);
                    }

                };
                if (getGatt() == null) {
                    //stopping the scan and running the connect in ui thread required for 4.x
                    XYSmartScan.instance.pauseAutoScan(true);

                    try {
                        Log.i(TAG, "_bleAccess acquiring[" + getId() + "]:" + _bleAccess.availablePermits() + "/" + MAX_BLECONNECTIONS + ":" + getId());
                        if (_bleAccess.tryAcquire(10, TimeUnit.SECONDS)) {
                            Log.i(TAG, "_bleAccess acquired[" + getId() + "]: " + _bleAccess.availablePermits() + "/" + MAX_BLECONNECTIONS + ":" + getId());
                            // below is commented out to prevent release being called in UI
                            //stopping the scan and running the connect in ui thread required for 4.x
//                            Handler handler = new Handler(context.getApplicationContext().getMainLooper());
//                            handler.post(new Runnable() {
//                                @Override
//                                public void run() {
                            final BluetoothDevice bluetoothDevice = getBluetoothDevice();
                            if (bluetoothDevice == null) {
                                XYBase.logError(TAG, "No Bluetooth Adapter!", false);
                                endActionFrame(_currentAction, false);
                                releaseBleLock();
                            } else {
                                BluetoothGatt gatt = bluetoothDevice.connectGatt(context.getApplicationContext(), false, callback);
                                setGatt(gatt);
                                if (gatt == null) {
                                    Log.i(TAG, "gatt is null");
                                    endActionFrame(_currentAction, false);
                                    releaseBleLock();
                                } else {
                                    boolean connected = gatt.connect();
                                    Log.v(TAG, "Connect:" + connected);
                                    gatt.discoverServices();
                                    Log.v(TAG, "Connect:" + connected + " - gatt object = " + gatt.hashCode());
                                }
                            }
//                                }
//                            });
                        } else {
                            XYBase.logError(TAG, "_bleAccess not acquired", false);
                            endActionFrame(_currentAction, false);
                        }
                    } catch (InterruptedException ex) {
                        XYBase.logError(TAG, "not acquired: interrupted", false);
                        endActionFrame(_currentAction, false);
                    }

                } else {
                    BluetoothGatt gatt = getGatt();
                    setGatt(gatt);
                    if (gatt == null) {
                        Log.i(TAG, "gatt is null");
                        endActionFrame(_currentAction, false);
                        releaseBleLock();
                    } else {
                        boolean connected = gatt.connect();
                        Log.v(TAG, "Connect:" + connected);
                        gatt.discoverServices();
                        Log.v(TAG, "Connect:" + connected + " - gatt object = " + gatt.hashCode());
                    }
//                    Log.i(TAG, "GATT already connect[" + getId() + "]:" + _connectionCount);
//                    List<BluetoothGattService> services = gatt.getServices();
//                    if (services.size() > 0) {
//                        callback.onServicesDiscovered(gatt, BluetoothGatt.GATT_SUCCESS);
//                    } else {
//                        Log.v(TAG, "gatt already connect, serivceSize = 0 - gatt object = " + gatt.hashCode());
//                    }
                }
                return null;
            }
        };
        asyncTask.executeOnExecutor(_threadPool);
        return asyncTask;
    }

    public double getDistance() {
        int tx = getTxPowerLevel();
        double rssi = getRssi();

        if (tx == 0 || rssi == 0 || rssi == outOfRangeRssi) {
            return -1;
        } else {
            double ratio = rssi * 1.0 / tx;
            if (ratio < 1.0) {
                return Math.pow(ratio, 10);
            } else {
                return (0.89976) * Math.pow(ratio, 7.7095) + 0.111;
            }
        }
    }

    public String getBeaconAddress() {
        return _beaconAddress;
    }

    public String getFirmwareVersion() {
        return _firmwareVersion;
    }

    private void pushConnection() {
        Log.v(TAG, "arie-pushConnection[" + _connectionCount + "->" + (_connectionCount + 1) + "]:" + getId());
        _connectionCount++;
    }

    private void popConnection() {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                Log.v(TAG, "arie-popConnection[" + _connectionCount + "->" + (_connectionCount - 1) + "]:" + getId());
                _connectionCount--;
                if (_connectionCount < 0) {
                    XYBase.logError(TAG, "Negative Connection Count:" + getId(), false);
                    _connectionCount = 0;
                }
                if (_connectionCount == 0) {
                    if (_stayConnectedActive) {
                        _subscribeButton = null;
                        _stayConnectedActive = false;
                    }
                    BluetoothGatt gatt = getGatt();
                    if (gatt != null) {
                        gatt.disconnect();
                        closeGatt();
                    }
                }
            }
        };
        Timer timer = new Timer();
        timer.schedule(timerTask, 6000);
    }

    public boolean isConnected() {
        return (_connectionCount > 0);
    }

    private void closeGatt() {
        Log.v(TAG, "arie-closeGatt");
        if (_connectionCount > 0) {
            XYBase.logError(TAG, "Closing GATT with open connections!", false);
            _connectionCount = 0;
            BluetoothGatt gatt = getGatt();
            if (gatt != null) {
                gatt.disconnect();
            }
        }
        if (_connectionCount < 0) {
            XYBase.logError(TAG, "Closing GATT with negative connections!", false);
            _connectionCount = 0;
        }
        BluetoothGatt gatt = getGatt();
        if (gatt == null) {
            // no false because debug should throw runtime
            XYBase.logError(TAG, "Closing Null Gatt");
            Log.e(TAG, "Closing Null Gatt");
            releaseBleLock();
        } else {
            gatt.close();
            setGatt(null);
            releaseBleLock();
        }
        _currentAction = null; //just to make sure
        _actionLock.release(MAX_ACTIONS);
        reportConnectionStateChanged(STATE_DISCONNECTED);
    }

    public static final int STATE_CONNECTED = BluetoothProfile.STATE_CONNECTED;
    public static final int STATE_DISCONNECTED = BluetoothProfile.STATE_DISCONNECTED;
    public static final int STATE_DISCONNECTING = BluetoothProfile.STATE_DISCONNECTING;

    private BluetoothManager getBluetoothManager(Context context) {
        return (BluetoothManager) context.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
    }

    public BluetoothDevice getBluetoothDevice() {
        BluetoothDevice bluetoothDevice;
        bluetoothDevice = getBluetoothDevice21();
        if (bluetoothDevice == null) {
            bluetoothDevice = getBluetoothDevice18();
        }
        if (bluetoothDevice == null) {
            Log.i(TAG, "getBluetoothDevice - None Found");
        }

        return bluetoothDevice;
    }

    public void pulseOutOfRange() {
        if (_stayConnectedActive) {
            _stayConnectedActive = false;
            popConnection();
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

    public void checkBatteryAndVersionInFuture(final Context context) {
//        Log.v(TAG, "checkBatteryInFuture");
        if (_batteryLevel == BATTERYLEVEL_NOTCHECKED) {
            _batteryLevel = BATTERYLEVEL_SCHEDULED;
            final TimerTask checkTimerTask = new TimerTask() {
                @Override
                public void run() {
                    checkBattery(context.getApplicationContext());
                    checkVersion(context.getApplicationContext());
                    checkTimeSinceCharged(context.getApplicationContext());
                }
            };

            final Timer pumpTimer = new Timer();
            Random random = new Random(new Date().getTime());
            //random check in next 6-12 minutes
            int delay = random.nextInt(3600000) + 3600000;
            Log.v(TAG, "checkBatteryInFuture:" + delay);
            pumpTimer.schedule(checkTimerTask, delay);
        }
    }

    public void checkBattery(final Context context) {
        checkBattery(context.getApplicationContext(), false);
    }

    public void checkBattery(final Context context, boolean force) {
        Log.v(TAG, "checkBattery");
        if (_batteryLevel < BATTERYLEVEL_CHECKED || force == true) {
            _batteryLevel = BATTERYLEVEL_CHECKED;
            XYDeviceActionGetBatteryLevel getBatteryLevel = new XYDeviceActionGetBatteryLevel(this) {
                public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    if (status == STATUS_CHARACTERISTIC_READ) {
                        if (success) {
                            _batteryLevel = this.value;
                            reportDetected();
                        }
                    }
                    return result;
                }
            };
            getBatteryLevel.start(context.getApplicationContext());
        }
    }

    public void checkTimeSinceCharged(final Context context) {
        Log.v(TAG, "checkTimeSinceCharged");
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

    public void checkVersion(final Context context) {
        Log.v(TAG, "checkFirmware");
        if (_firmwareVersion == null) {
            _firmwareVersion = "";
            XYDeviceActionGetVersion getVersion = new XYDeviceActionGetVersion(this) {
                public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    if (status == STATUS_CHARACTERISTIC_READ) {
                        if (success) {
                            _firmwareVersion = this.value;
                            reportDetected();
                        }
                    }
                    return result;
                }
            };
            getVersion.start(context.getApplicationContext());
        }
    }

    @TargetApi(18)
    private BluetoothDevice getBluetoothDevice18() {
        Log.v(TAG, "getBluetoothDevice18");
        ScanResultLegacy scanResult = _currentScanResult18;
        if (scanResult == null) {
            return null;
        } else {
            return scanResult.getDevice();
        }
    }

    @TargetApi(21)
    private BluetoothDevice getBluetoothDevice21() {
        Log.v(TAG, "getBluetoothDevice21");
        ScanResult scanResult = _currentScanResult21;
        if (scanResult == null) {
            return null;
        } else {
            return scanResult.getDevice();
        }
    }

    @TargetApi(18)
    private void pulseOutOfRange18() {
        Log.v(TAG, "pulseOutOfRange18");
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
    public void pulse18(ScanResultLegacy scanResult) {
        XYBase.logExtreme(TAG, "pulse18: " + _id + ":" + scanResult.getRssi());
        _scansMissed = 0;

        if (getFamily() == Family.XY3 || getFamily() == Family.Gps) {
            ScanRecordLegacy scanRecord = scanResult.getScanRecord();
            if (scanRecord != null) {
                byte[] manufacturerData = scanResult.getScanRecord().getManufacturerSpecificData(0x004c);
                if (manufacturerData != null) {
                    if ((manufacturerData[21] & 0x08) == 0x08) {
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
                _stayConnectedActive = true;
                pushConnection();
            }
        } else if ((_currentScanResult18.getRssi() != outOfRangeRssi) && (scanResult.getRssi() == outOfRangeRssi)) {
            _currentScanResult18 = null;
            reportExited();
        } else if (scanResult.getRssi() != outOfRangeRssi) {
            // & 0x02 is used to check if the advertiseFlag is connectable
//            if ((scanResult.getScanRecord().getAdvertiseFlags() & 0x02) == 0x02) {
//                _currentScanResult18 = scanResult;
//            }
            _currentScanResult18 = scanResult;
            reportDetected();
            if (!_stayConnectedActive && _stayConnected) {
                _stayConnectedActive = true;
                pushConnection();
            }
        }

        if (_beaconAddress == null) {
            _beaconAddress = scanResult.getDevice().getAddress();
        }
    }

    @TargetApi(21)
    public void pulse21(Object scanResultObject) {
        android.bluetooth.le.ScanResult scanResult = (android.bluetooth.le.ScanResult) scanResultObject;
        XYBase.logExtreme(TAG, "pulse21: " + _id + ":" + scanResult.getRssi());
        _scansMissed = 0;

        if (getFamily() == Family.XY3 || getFamily() == Family.Gps) {
            android.bluetooth.le.ScanRecord scanRecord = scanResult.getScanRecord();
            if (scanRecord != null) {
                byte[] manufacturerData = scanResult.getScanRecord().getManufacturerSpecificData(0x004c);

                if (manufacturerData != null) {
                    if ((manufacturerData[21] & 0x08) == 0x08) {
                        handleButtonPulse();
                        return;
                    }
                }
            }
        }

        if ((_currentScanResult21 == null) || ((_currentScanResult21.getRssi() == outOfRangeRssi) && (scanResult.getRssi() != outOfRangeRssi))) {
            _currentScanResult21 = scanResult;
            reportEntered();
            reportDetected();
        } else if ((_currentScanResult21.getRssi() != outOfRangeRssi) && (scanResult.getRssi() == outOfRangeRssi)) {
            _currentScanResult21 = null;
            reportExited();
        } else if (scanResult.getRssi() != outOfRangeRssi) {
            // & 0x02 is used to check if the advertiseFlag is connectable
//            if ((scanResult.getScanRecord().getAdvertiseFlags() & 0x02) == 0x02) {
//                _currentScanResult21 = scanResult;
//            }
            _currentScanResult21 = scanResult;
            reportDetected();
        }

        if (_beaconAddress == null) {
            _beaconAddress = scanResult.getDevice().getAddress();
        }
    }

    public String getId() {
        return _id;
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
            XYBase.logError(TAG, "getUUID: wrong number of parts [" + id + "] : " + parts.length, false);
            return null;
        }

        String[] parts2 = parts[2].split("\\.");
        if (parts2.length != 3) {
            XYBase.logError(TAG, "getUUID: wrong number of parts2 [" + id + "] : " + parts2.length, false);
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
            XYBase.logError(TAG, "getPrefix: wrong number of parts [" + id + "] : " + parts.length, false);
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
            XYBase.logError(TAG, "getMajor: wrong number of parts [" + id + "] : " + parts.length, false);
            return -1;
        }

        String[] parts2 = parts[2].split("\\.");
        if (parts2.length != 3) {
            XYBase.logError(TAG, "getMajor: wrong number of parts2 [" + id + "] : " + parts2.length, false);
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
            XYBase.logError(TAG, "getMinor: wrong number of parts [" + id + "] : " + parts.length, false);
            return -1;
        }

        String[] parts2 = parts[2].split("\\.");
        if (parts2.length != 3) {
            XYBase.logError(TAG, "getMinor: wrong number of parts2 [" + id + "] : " + parts2.length, false);
            return -1;
        }
        return Integer.parseInt(parts2[2]);
    }

    private int _updateCounter = 0;

    public boolean isUpdateSignificant() {
        if (_updateCounter >= 100) {
            _updateCounter = 0;
            return true;
        } else {
            _updateCounter++;
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
                case "gps":
                    return Family.Gps;
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
        int currentRssi = getRssi();
        if (currentRssi == 0) {
            return Proximity.None;
        } else if (currentRssi >= -40) {
            return Proximity.Touching;
        } else if (currentRssi >= -60) {
            return Proximity.VeryNear;
        } else if (currentRssi >= -70) {
            return Proximity.Near;
        } else if (currentRssi >= -80) {
            return Proximity.Medium;
        } else if (currentRssi >= -90) {
            return Proximity.Far;
        } else if (currentRssi > outOfRangeRssi) {
            return Proximity.VeryFar;
        }

        return Proximity.OutOfRange;
    }

    public void scanComplete() {
        if (isConnected()) {
            _scansMissed = 0;
        } else {
            _scansMissed++;
        }
        if (_scansMissed > _missedPulsesForOutOfRange) {
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
        _enterCount++;
        synchronized (_listeners) {
            for (Map.Entry<String, Listener> entry : _listeners.entrySet()) {
                entry.getValue().entered(this);
            }
        }
    }

    private void reportExited() {
        _exitCount++;
        synchronized (_listeners) {
            for (Map.Entry<String, Listener> entry : _listeners.entrySet()) {
                entry.getValue().exited(this);
            }
        }
    }

    private void reportDetected() {
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
            reportButtonRecentlyPressed(ButtonType.Single);
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
            timer.schedule(timerTask, 40000);
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
        Log.v(TAG, "reportConnectionStateChanged[" + getId() + "]:" + newState);
        synchronized (_listeners) {
            for (Map.Entry<String, Listener> entry : _listeners.entrySet()) {
                entry.getValue().connectionStateChanged(this, newState);
            }
        }
    }

    private void reportUpdated() {
        Log.v(TAG, "reportUpdated[" + getId() + "]");
        synchronized (_listeners) {
            for (Map.Entry<String, Listener> entry : _listeners.entrySet()) {
                entry.getValue().updated(this);
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
        Near
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

        void updated(final XYDevice device);
    }
    // endregion =========== Listeners ============
}
