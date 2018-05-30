package com.xyfindables.sdk

import android.annotation.TargetApi

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.AsyncTask
import android.os.Handler

import com.xyfindables.core.XYBase
import com.xyfindables.sdk.action.XYDeviceAction
import com.xyfindables.sdk.action.XYDeviceActionGetBatterySinceCharged
import com.xyfindables.sdk.action.XYDeviceActionSubscribeButton
import com.xyfindables.sdk.action.XYDeviceActionSubscribeButtonModern
import com.xyfindables.sdk.action.dialog.SubscribeSpotaNotifications
import com.xyfindables.sdk.actionHelper.XYBattery
import com.xyfindables.sdk.actionHelper.XYFirmware
import com.xyfindables.sdk.bluetooth.ScanResultLegacy

import java.util.Comparator
import java.util.Date
import java.util.HashMap
import java.util.Random
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Created by arietrouw on 12/20/16.
 */

class XYDevice internal constructor(id: String) : XYBase() {
    private val _actionLock = XYSemaphore(MAX_ACTIONS, true)

    private var _connectIntent = false

    private var _rssi: Int = 0

    var isConnected = false
        private set
    private var _connectionCount = 0
    private var _stayConnected = false
    private var _stayConnectedActive = false
    private var _isInOtaMode = false

    // if _gatt is not closed, and we set new _gatt, then we keep reference to old _gatt connection which still exists in ble stack
    // we could set a timer here to null the scanResult if new scanResult is not seen in x seconds
    var gatt: BluetoothGatt? = null
        private set(gatt) {

            if (gatt == this.gatt) {
                logError(TAG, "connTest-trying to set same gatt", false)
                return
            }
            if (this.gatt != null) {
                logExtreme(TAG, "connTest-_gatt.close!!!")
                this.gatt!!.close()
                releaseBleLock()
            }
            XYBase.logExtreme(TAG, "connTest-setGatt = " + gatt + ": previous _gatt = " + this.gatt)
            field = gatt
        }

    var enterCount = 0
        private set
    var exitCount = 0
        private set
    var detectCount = 0
        private set
    var actionQueueCount = 0
        private set
    var actionFailCount = 0
        private set
    var actionSuccessCount = 0
        private set
    private var _firstDetectedTime: Long = 0
    private var _connectedContext: Context? = null

    var id: String? = null
        private set

    var batteryLevel = BATTERYLEVEL_NOTCHECKED
        private set
    var timeSinceCharged: Long = -1
        private set
    var firmwareVersion: String? = null
        private set
    var beaconAddress: String? = null
        private set
    var simActivated: Boolean? = false

    private var _scansMissed: Int = 0
    private var _buttonRecentlyPressed = false

    private var _currentScanResult18: ScanResultLegacy? = null
    private var _currentScanResult21: ScanResult? = null

    private val _listeners = HashMap<String, Listener>()
    private var _currentAction: XYDeviceAction? = null

    val rssi: Int
        get() {
            if (gatt != null) {
                gatt!!.readRemoteRssi()
                return _rssi
            } else {
                return if (XYSmartScan.instance.legacy) {
                    rssi18
                } else {
                    rssi21
                }
            }
        }

    private val rssi18: Int
        @TargetApi(18)
        get() = if (_currentScanResult18 == null) {
            outOfRangeRssi
        } else {
            _currentScanResult18!!.rssi
        }

    private val rssi21: Int
        @TargetApi(21)
        get() = if (_currentScanResult21 == null) {
            outOfRangeRssi
        } else {
            _currentScanResult21!!.rssi
        }

    private val txPowerLevel: Int
        get() = if (XYSmartScan.instance.legacy) {
            txPowerLevel18
        } else {
            txPowerLevel21
        }

    private val txPowerLevel18: Int
        @TargetApi(18)
        get() = if (_currentScanResult18 == null) {
            0
        } else {
            txPowerLevelFromScanResult18(_currentScanResult18)
        }

    private val txPowerLevel21: Int
        @TargetApi(21)
        get() = if (_currentScanResult21 == null) {
            0
        } else {
            txPowerLevelFromScanResult21(_currentScanResult21)
        }

    var spotaNotifications: SubscribeSpotaNotifications? = null

    private var _subscribeButton: XYDeviceActionSubscribeButton? = null
    private var _subscribeButtonModern: XYDeviceActionSubscribeButtonModern? = null

    private var _actionFrameTimer: Timer? = null

    // this could cause ui "searching" because rssi = 0 when no values returning
    val distance: Double
        get() {
            var tx = txPowerLevel

            if (family == Family.XY4) {
                tx = -60
            } else {
                tx = -60
            }

            val rssi = rssi.toDouble()

            if (rssi == outOfRangeRssi.toDouble()) {
                return -2.0
            }

            if (tx == 0 || rssi == 0.0) {
                return -1.0
            } else {
                val ratio = rssi * 1.0 / tx
                return if (ratio < 1.0) {
                    Math.pow(ratio, 10.0)
                } else {
                    0.89976 * Math.pow(ratio, 7.7095) + 0.111
                }
            }
        }

    private val bluetoothDevice: BluetoothDevice?
        get() {
            var bluetoothDevice: BluetoothDevice?
            bluetoothDevice = bluetoothDevice21
            if (bluetoothDevice == null) {
                bluetoothDevice = bluetoothDevice18
            }
            return bluetoothDevice
        }

    private val bluetoothDevice18: BluetoothDevice?
        @TargetApi(18)
        get() {
            logExtreme(TAG, "getBluetoothDevice18")
            val scanResult = _currentScanResult18
            return scanResult?.device
        }

    private val bluetoothDevice21: BluetoothDevice?
        @TargetApi(21)
        get() {
            logExtreme(TAG, "getBluetoothDevice21")
            val scanResult = _currentScanResult21
            return scanResult?.device
        }

    private var _buttonPressedTimer: Timer? = null
    private var _buttonPressedTimerTask: TimerTask? = null

    val uuid: UUID?
        get() = getUUID(id!!)

    val prefix: String
        get() = getPrefix(id!!)

    val major: Int
        get() = getMajor(id!!)

    private var _lastUpdateTime: Long = 0

    val isUpdateSignificant: Boolean
        get() {
            if (_lastUpdateTime == 0L) {
                _lastUpdateTime = System.currentTimeMillis()
                return false
            }
            val currentTime = System.currentTimeMillis()
            if (currentTime - _lastUpdateTime > 1800000) {
                _lastUpdateTime = currentTime
                return true
            } else {
                return false
            }
        }

    val minor: Int
        get() = getMinor(id!!)

    val family: Family
        get() {
            val familyFromUUID = getFamily(uuid)
            if (familyFromUUID != Family.Unknown) {
                return familyFromUUID
            } else {
                val prefix = prefix
                when (prefix) {
                    "near" -> return Family.Near
                    "mobiledevice" -> return Family.Mobile
                    else -> return Family.Unknown
                }
            }
        }

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
    val proximity: Proximity
        get() {

            val distance = distance

            if (distance < -1.0) {
                return Proximity.OutOfRange
            }
            if (distance < 0.0) {
                return Proximity.None
            }
            if (distance < 0.0173) {
                return Proximity.Touching
            }
            if (distance < 1.0108) {
                return Proximity.VeryNear
            }
            if (distance < 3.0639) {
                return Proximity.Near
            }
            if (distance < 8.3779) {
                return Proximity.Medium
            }
            return if (distance < 20.6086) {
                Proximity.Far
            } else Proximity.VeryFar
        }

    fun setConnectIntent(value: Boolean) {
        _connectIntent = value
    }

    init {
        _missedPulsesForOutOfRange = XYSmartScan.instance.missedPulsesForOutOfRange
        instanceCount++
        this.id = normalizeId(id)
        if (this.id == null) {
            this.id = id
        }
    }

    protected fun finalize() {
        instanceCount--
    }

    @TargetApi(18)
    protected fun txPowerLevelFromScanResult18(scanResult: ScanResultLegacy?): Int {
        var tx = 0
        if (scanResult != null) {
            val scanRecord = scanResult.scanRecord
            if (scanRecord != null) {
                val manufacturerData = scanResult.scanRecord!!.getManufacturerSpecificData(0x004c)
                if (manufacturerData != null) {
                    tx = manufacturerData[22].toInt()
                }
            }
        }
        return tx
    }

    @TargetApi(21)
    protected fun txPowerLevelFromScanResult21(scanResult: ScanResult?): Int {
        var tx = 0
        if (scanResult != null) {
            val scanRecord = scanResult.scanRecord
            if (scanRecord != null) {
                val manufacturerData = scanResult.scanRecord!!.getManufacturerSpecificData(0x004c)
                if (manufacturerData != null) {
                    tx = manufacturerData[22].toInt()
                }
            }
        }
        return tx
    }

    fun startSpotaNotifications(context: Context) {
        if (spotaNotifications != null) {
            spotaNotifications!!.start(context)
        }
    }

    fun stopSpotaNotifications() {
        if (spotaNotifications != null) {
            spotaNotifications!!.stop()
        }
    }

    fun otaMode(value: Boolean) {
        logExtreme(TAG, "otaMode set: $value")
        if (value == _isInOtaMode) {
            return
        }
        XYSmartScan.instance.pauseAutoScan(value)
        _isInOtaMode = value
    }

    fun stayConnected(context: Context, value: Boolean) {
        _connectedContext = context

        logExtreme(TAG, "stayConnected:$value:$id _stayConnectedActive = $_stayConnectedActive")
        if (value == _stayConnected) {
            return
        }
        _stayConnected = value
        if (_stayConnected) {
            if (!_stayConnectedActive) {
                startSubscribeButton()
            }
        } else {
            if (_stayConnectedActive) {
                _stayConnectedActive = false
                stopSubscribeButton()
            }
        }
    }

    private fun startActionTimer() {
        val timerTask = object : TimerTask() {
            override fun run() {
                if (_isInOtaMode) {
                    return
                }
                if (_currentAction == null) {
                    logError(TAG, "connTest-Null Action timed out", false)
                    // this will be called in endActionFrame so only needs to be called if somehow _currentAction is null and timer is not
                    cancelActionTimer()
                } else {
                    logError(TAG, "connTest-Action Timeout", false)
                    endActionFrame(_currentAction, false)
                }
                // below should not be necessary since cancelActionTimer is called in endActionFrame
                // this may have been protecting vs somehow having null action inside endActionFrame, but currently seems to cause crash (race condition?)
                //                if (_actionFrameTimer != null) {
                //                    _actionFrameTimer.cancel();
                //                    _actionFrameTimer = null;
                //                }
            }
        }

        _actionFrameTimer = Timer("ActionTimer")
        try {
            _actionFrameTimer!!.schedule(timerTask, _actionTimeout.toLong())
        } catch (ex: IllegalStateException) {
            logError(TAG, ex.toString(), true)
        }

        _actionTimeout = 30000
    }

    private fun cancelActionTimer() {
        if (_actionFrameTimer == null) {
            logError(TAG, "connTest-Null _actionFrameTimer", false)
        } else {
            logExtreme(TAG, "connTest-_actionFrameTimer being cancelled")
            _actionFrameTimer!!.cancel()
            _actionFrameTimer = null
        }
    }

    private fun releaseBleLock() {
        _bleAccess.release()
        XYBase.logInfo(TAG, "connTest_bleAccess released[" + id + "]: " + _bleAccess.availablePermits() + "/" + MAX_BLECONNECTIONS + ":" + id)
    }

    private fun releaseActionLock() {
        logExtreme(TAG, "connTest-_actionLock" + _actionLock.availablePermits())
        _actionLock.release()
    }

    private fun startActionFrame(action: XYDeviceAction): Int {

        logExtreme(TAG, "startActionFrame")
        action.statusChanged(XYDeviceAction.STATUS_QUEUED, null, null, true)
        //        try {
        //            if (!_actionLock.tryAcquire(_actionTimeout, TimeUnit.MILLISECONDS)) {
        //                XYBase.logError(TAG, "_actionLock:failed:" + action.getClass().getSuperclass().getSimpleName(), false);
        //                return 0;
        //            } else {

        // acquireUninterruptibly is used so lock is not release when thread expires (due to calling discoverServices)
        _actionLock.acquireUninterruptibly()
        logExtreme(TAG, "_actionLock[$id]:acquired")
        action.statusChanged(XYDeviceAction.STATUS_STARTED, null, null, true)
        logExtreme(TAG, "startActionFrame-action started")
        _currentAction = action
        pushConnection()
        startActionTimer()
        return action.hashCode()
        //            }
        //        } catch (InterruptedException ex) {
        //            XYBase.logError(TAG, "Service Load Semaphore interrupted");
        //            return 0;
        //        }
    }

    fun endOta() {
        otaMode(false)
        //        popConnection();
        XYBase.logExtreme(TAG, "connTest-popConnection2")
    }

    private fun endActionFrame(action: XYDeviceAction?, success: Boolean) {
        logExtreme(TAG, "connTest-endActionFrame: success = $success: otaMode = $_isInOtaMode")
        if (action == null) {
            logError(TAG, "connTest-Ending Null Action", false)
            return
        }
        if (success) {
            actionSuccessCount++
        } else {
            actionFailCount++

        }
        cancelActionTimer()
        action.statusChanged(XYDeviceAction.STATUS_COMPLETED, null, null, success)
        logExtreme(TAG, "connTest-_actionLock[$id]:release")
        popConnection()
        _currentAction = null
        //        _connectIntent = false;
        releaseActionLock()
        XYSmartScan.instance.pauseAutoScan(false)
        logExtreme(TAG, "connTest-popConnection1-pauseAutoScan set back to false")
    }

    fun runOnUiThread(context: Context, action: Runnable) {
        val handler = Handler(context.mainLooper)
        handler.post(action)
    }

    // pass timeout param if need be, otherwise default at 60000 MS
    fun queueAction(context: Context, action: XYDeviceAction): AsyncTask<*, *, *>? {
        queueAction(context, action, 0)
        return null
    }

    fun queueAction(context: Context, action: XYDeviceAction, timeout: Int): AsyncTask<*, *, *> {

        if (BuildConfig.DEBUG) {
            logExtreme(TAG, "connTest-queueAction-action = " + action.javaClass.superclass.simpleName)
        }

        if (timeout > 0) {
            _actionTimeout = timeout
        }

        actionQueueCount++

        val asyncTask = object : AsyncTask<Void, Void, Void>() {

            override fun doInBackground(vararg params: Void): Void? {

                if (bluetoothDevice == null) {
                    logError(TAG, "connTest-getBluetoothDevice() == null", false)
                    // need to complete action here
                    action.statusChanged(XYDeviceAction.STATUS_COMPLETED, null, null, false)
                    return null
                }

                XYBase.logInfo(TAG, "connTest-connect[$id]:$_connectionCount")
                val actionLock = startActionFrame(action)
                if (actionLock == 0) {
                    val currentAction = _currentAction
                    closeGatt()
                    XYBase.logExtreme(TAG, "connTest-closeGatt2")
                    if (currentAction != null) {
                        logError(TAG, "connTest-statusChanged:failed to get actionLock", false)
                        endActionFrame(currentAction, false)
                    }
                    return null
                }

                val callback = object : BluetoothGattCallback() {
                    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                        super.onConnectionStateChange(gatt, status, newState)

                        XYBase.logInfo(TAG, "connTest-onConnectionStateChange:$status:$newState:$id")
                        when (newState) {
                            BluetoothGatt.STATE_CONNECTED -> {
                                isConnected = true
                                logExtreme(TAG, "connTest-STATE_CONNECTED status is $status")
                                logExtreme(TAG, "connTest-_connectIntent = $_connectIntent")
                                //                                _connectIntent = false;
                                logInfo(TAG, "onConnectionStateChange:Connected: " + id!!)
                                reportConnectionStateChanged(STATE_CONNECTED)
                                // call gatt.discoverServices() in ui thread?
                                gatt.discoverServices()
                                logExtreme(TAG, "connTest-discoverServices called in STATE_CONNECTED")
                            }
                            BluetoothGatt.STATE_DISCONNECTED -> {
                                logInfo(TAG, "connTest-onConnectionStateChange:Disconnected: " + id!!)
                                if (status == 133) {
                                    if (!_connectIntent) {
                                        isConnected = false
                                    }
                                    // 133 may disconnect device -> in this case stayConnected can leave us with surplus connectionCount and no way to re-subscribe to button
                                    //                                    stayConnected(null, false); // hacky?
                                    /* Ignoring the 133 seems to keep the connection alive.
                                    No idea why, but it does on Android 5.1 */
                                    logError(TAG, "connTest-Disconnect with 133", false)
                                    if (!_isInOtaMode && !_connectIntent /*&& (_connectionCount <= 1)*/) {
                                        reportConnectionStateChanged(STATE_DISCONNECTED)
                                        //popConnection();  //hacky?
                                        logError(TAG, "connTest-disconnect inside 133", false)
                                        val currentAction = _currentAction
                                        if (currentAction != null) {
                                            endActionFrame(currentAction, false)
                                        } else {
                                            logError(TAG, "connTest-trying to disconnect inside 133 with null currentAction", false)
                                        }
                                        gatt.close() // need to close gatt on device we no longer see, this will cause gatt to be null when pop connection is called since it is delayed 6 seconds
                                    }
                                    //XYSmartScan.instance.refresh(gatt);
                                } else {
                                    isConnected = false
                                    reportConnectionStateChanged(STATE_DISCONNECTED)  // redundant bc called inside closeGatt inside endActionFrame as well? Just add inside gatt!=null ?
                                    val currentAction = _currentAction
                                    if (currentAction != null) {
                                        endActionFrame(currentAction, false)
                                    } else {
                                        logError(TAG, "connTest-trying to disconnect with null currentAction", false)
                                    }
                                    gatt.close() // need to close gatt on device we no longer see, this will cause gatt to be null when pop connection is called since it is delayed 6 seconds

                                }
                            }
                            BluetoothGatt.STATE_DISCONNECTING -> {
                                logInfo(TAG, "onConnectionStateChange:Disconnecting: " + id!!)
                                reportConnectionStateChanged(STATE_DISCONNECTING)
                            }
                            else -> {
                                logError(TAG, "onConnectionStateChange:Unknown State: $newState:$id", false)
                                val currentAction = _currentAction
                                closeGatt()
                                logExtreme(TAG, "connTest-closeGatt4")
                                if (currentAction != null) {
                                    logError(TAG, "statusChanged:unknown", false)
                                    endActionFrame(currentAction, false)
                                }
                            }
                        }
                        if (status != BluetoothGatt.GATT_SUCCESS) {
                            logError(TAG, "connTest-Bad Status: $status", false)
                            XYSmartScan.instance.setStatus(XYSmartScan.Status.BluetoothUnstable)
                            /*XYDeviceAction currentAction = _currentAction;
                            if (currentAction != null) {
                                logError(TAG, "statusChanged:badstatus:" + status, false);
                                endActionFrame(currentAction, false);
                            }*/
                        }
                    }

                    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {

                        super.onServicesDiscovered(gatt, status)

                        val currentAction = _currentAction
                        XYBase.logExtreme(TAG, "connTest-onServicesDiscovered")
                        if (currentAction != null) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                logInfo(TAG, "connTest-onServicesDiscovered:$status")
                                currentAction.statusChanged(XYDeviceAction.STATUS_SERVICE_FOUND, gatt, null, true)
                                // concurrent mod ex on many devices with below line
                                val service = gatt.getService(currentAction.serviceId)
                                if (service != null) {
                                    val characteristic = service.getCharacteristic(currentAction.characteristicId)
                                    logExtreme(TAG, "connTest-onServicesDiscovered-service not null")
                                    if (characteristic != null) {
                                        if (currentAction.statusChanged(XYDeviceAction.STATUS_CHARACTERISTIC_FOUND, gatt, characteristic, true)) {
                                            logExtreme(TAG, "connTest-onServicesDiscovered-characteristic not null")
                                            endActionFrame(currentAction, false)
                                        } else {
                                            logExtreme(TAG, "connTest-do nothing") // herein lies the issue- something in endActionFrame is triggering 133-timing? executing actions too close together?
                                        }
                                    } else {
                                        logError(TAG, "connTest-statusChanged:characteristic null", false) // this happens a decent amount. What is causing this?
                                        endActionFrame(currentAction, false)
                                    }
                                } else {
                                    logError(TAG, "connTest-statusChanged:service null, gatt = $gatt currentAction = $currentAction", false) // this happens a decent amount. What is causing this?
                                    endActionFrame(currentAction, false)
                                }
                            } else {
                                logError(TAG, "connTest-statusChanged:onServicesDiscovered Failed: $status", true)
                                endActionFrame(currentAction, false)
                            }
                        } else {
                            logError(TAG, "connTest-null _currentAction", true)
                        }
                    }

                    override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                        super.onCharacteristicRead(gatt, characteristic, status)

                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            logInfo(TAG, "onCharacteristicRead:$status")
                            if (_currentAction != null && _currentAction!!.statusChanged(XYDeviceAction.STATUS_CHARACTERISTIC_READ, gatt, characteristic, true)) {
                                endActionFrame(_currentAction, true)
                            }
                        } else {
                            logError(TAG, "onCharacteristicRead Failed: $status", false)
                            endActionFrame(_currentAction, false)
                        }
                    }

                    override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                        super.onCharacteristicWrite(gatt, characteristic, status)

                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            logInfo(TAG, "onCharacteristicWrite:$status")
                            if (_currentAction != null && _currentAction!!.statusChanged(XYDeviceAction.STATUS_CHARACTERISTIC_WRITE, gatt, characteristic, true)) {
                                endActionFrame(_currentAction, true)
                            }
                        } else {
                            logError(TAG, "onCharacteristicWrite Failed: $status", false)
                            endActionFrame(_currentAction, false)
                        }
                    }

                    override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                        super.onCharacteristicChanged(gatt, characteristic)

                        logInfo(TAG, "onCharacteristicChanged")
                        if (_subscribeButton != null && !_isInOtaMode) {
                            _subscribeButton!!.statusChanged(XYDeviceAction.STATUS_CHARACTERISTIC_UPDATED, gatt, characteristic, true)
                        }
                        if (_subscribeButtonModern != null && !_isInOtaMode) {
                            _subscribeButtonModern!!.statusChanged(XYDeviceAction.STATUS_CHARACTERISTIC_UPDATED, gatt, characteristic, true)
                        }
                        if (spotaNotifications != null && _isInOtaMode) {
                            spotaNotifications!!.statusChanged(XYDeviceAction.STATUS_CHARACTERISTIC_UPDATED, gatt, characteristic, true)
                        }
                    }

                    override fun onDescriptorRead(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
                        super.onDescriptorRead(gatt, descriptor, status)

                        logInfo(TAG, "onDescriptorRead:$status")
                    }

                    override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
                        super.onDescriptorWrite(gatt, descriptor, status)

                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            logInfo(TAG, "onDescriptorWrite:$status")
                            if (_currentAction != null && _currentAction!!.statusChanged(descriptor, XYDeviceAction.STATUS_CHARACTERISTIC_WRITE, gatt, true)) {
                                endActionFrame(_currentAction, true)
                            }
                        } else {
                            logError(TAG, "onDescriptorWrite Failed: $status", false)
                            endActionFrame(_currentAction, false)
                        }
                        logInfo(TAG, "onDescriptorWrite: $descriptor : status = $status")
                    }

                    override fun onReliableWriteCompleted(gatt: BluetoothGatt, status: Int) {
                        super.onReliableWriteCompleted(gatt, status)

                        logInfo(TAG, "onReliableWriteCompleted:$status")
                    }

                    override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
                        super.onReadRemoteRssi(gatt, rssi, status)

                        logExtreme(TAG, "testRssi-onReadRemoteRssi rssi = $rssi")
                        _rssi = rssi
                        reportReadRemoteRssi(rssi)
                    }

                    override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                        super.onMtuChanged(gatt, mtu, status)

                        logInfo(TAG, "onMtuChanged:$status")
                    }

                }
                if (gatt == null) {
                    //stopping the scan and running the connect in ui thread required for 4.x
                    XYSmartScan.instance.pauseAutoScan(true)
                    logExtreme(TAG, "connTest-pauseAutoScan(true)")

                    try {
                        XYBase.logInfo(TAG, "connTest-_bleAccess acquiring[" + id + "]:" + _bleAccess.availablePermits() + "/" + MAX_BLECONNECTIONS + ":" + id)
                        if (_bleAccess.tryAcquire(10, TimeUnit.SECONDS)) {
                            XYBase.logInfo(TAG, "connTest_bleAccess acquired[" + id + "]: " + _bleAccess.availablePermits() + "/" + MAX_BLECONNECTIONS + ":" + id)
                            // below is commented out to prevent release being called in UI
                            //stopping the scan and running the connect in ui thread required for 4.x - also required for Samsung Galaxy s7 7.0- and likely other phones as well

                            val handler = Handler(context.applicationContext.mainLooper)
                            handler.post {
                                val bluetoothDevice = bluetoothDevice
                                if (bluetoothDevice == null) {
                                    logError(TAG, "connTest-No Bluetooth Adapter!", false)
                                    endActionFrame(_currentAction, false)
                                    releaseBleLock()
                                    logExtreme(TAG, "connTest-release3")
                                } else {
                                    var gatt: BluetoothGatt?
                                    if (android.os.Build.VERSION.SDK_INT >= 23) {
                                        gatt = bluetoothDevice.connectGatt(context.applicationContext, false, callback, android.bluetooth.BluetoothDevice.TRANSPORT_LE)
                                    } else {
                                        gatt = bluetoothDevice.connectGatt(context.applicationContext, false, callback)
                                    }
                                    gatt = gatt
                                    if (gatt == null) {
                                        logExtreme(TAG, "gatt is null")
                                        endActionFrame(_currentAction, false)
                                        releaseBleLock()
                                        logExtreme(TAG, "connTest-release4")
                                    } else {
                                        val connected = gatt!!.connect()
                                        logExtreme(TAG, "connTest-Connect:$connected")
                                        // some sources say must wait 600 ms after connect before discoverServices
                                        // other sources say call gatt.discoverServices in UI thread
                                        // 133s seem to start after gatt.connect is called
                                        //                                        gatt.discoverServices();
                                    }
                                }
                            }
                        } else {
                            logError(TAG, "connTest-_bleAccess not acquired", false)
                            endActionFrame(_currentAction, false)
                        }
                    } catch (ex: InterruptedException) {
                        logError(TAG, "connTest-not acquired: interrupted", true)
                        endActionFrame(_currentAction, false)
                    }

                } else {
                    logExtreme(TAG, "connTest-already have Gatt")
                    val gatt = gatt
                    if (gatt == null) {
                        logExtreme(TAG, "gatt is null")
                        endActionFrame(_currentAction, false)
                        releaseBleLock()
                        logExtreme(TAG, "connTest-release5")
                    } else {
                        // should already be connected but just in case -> is now commented out because cause issues calling this when already connected on some phones
                        //                            boolean connected = gatt.connect();
                        // null pointer exception here, somehow gatt is null?
                        val services = gatt.services
                        if (services.size > 0) {
                            callback.onServicesDiscovered(gatt, BluetoothGatt.GATT_SUCCESS)
                        } else {
                            if (!gatt.discoverServices()) {
                                logExtreme(TAG, "connTest-FAIL discoverServices")
                                endActionFrame(_currentAction, false)
                            } else {
                                logExtreme(TAG, "connTest-discoverServices called inside callback if gatt not null")
                            }
                        }
                    }
                }
                return null
            }
        }
        asyncTask.executeOnExecutor(_threadPool)
        return asyncTask
    }

    fun getDistanceWithCustomTx(tx: Int): Double {

        val rssi = rssi.toDouble()

        if (tx == 0 || rssi == 0.0 || rssi == outOfRangeRssi.toDouble()) {
            return -1.0
        } else {
            val ratio = rssi * 1.0 / tx
            return if (ratio < 1.0) {
                Math.pow(ratio, 10.0)
            } else {
                0.89976 * Math.pow(ratio, 7.7095) + 0.111
            }
        }
    }

    private fun getDistance(rssi: Int): Double {

        var tx = txPowerLevel

        if (family == Family.XY4) {
            tx = -60
        } else {
            tx = -60
        }

        if (rssi == outOfRangeRssi) {
            return -2.0
        }

        if (tx == 0 || rssi == 0) {
            return -1.0
        } else {
            val ratio = rssi * 1.0 / tx
            return if (ratio < 1.0) {
                Math.pow(ratio, 10.0)
            } else {
                0.89976 * Math.pow(ratio, 7.7095) + 0.111 // made for tx of -59 I believe so works with most our devices, but does not work with xy4 of tx -75
            }
        }
    }

    fun addTemporaryConnection() {
        _connectionCount++
    }

    fun removeTemporaryConnection() {
        _connectionCount--
    }

    private fun pushConnection() {
        if (_currentScanResult21 != null || _currentScanResult18 != null) {
            if (BuildConfig.DEBUG) {
                var action: String? = null
                if (_currentAction != null) {
                    action = _currentAction!!.javaClass.superclass.simpleName
                }
                logExtreme(TAG, "connTest-pushConnection[" + _connectionCount + "->" + (_connectionCount + 1) + "]:" + id + ": " + action)
            }
            if (_currentAction == null) {
                logError(TAG, "connTest-null currentAction", true)
            }
            _connectionCount++
        } else {
            logExtreme(TAG, "connTest-pushConnection-no scan result")
        }
    }

    fun popConnection() {
        val timerTask = object : TimerTask() {
            override fun run() {
                if (BuildConfig.DEBUG) {
                    var action: String? = null
                    if (_currentAction != null) {
                        action = _currentAction!!.javaClass.superclass.simpleName
                    }
                    logExtreme(TAG, "connTest-popConnection[" + _connectionCount + "->" + (_connectionCount - 1) + "]:" + id + ": " + action)
                }
                _connectionCount--
                if (_connectionCount < 0) {
                    logError(TAG, "connTest-Negative Connection Count:" + id!!, false)
                    _connectionCount = 0
                    // logic flaw??? if connCount < 0, its just 0, we do not close or other
                } else {
                    if (_connectionCount == 0) {
                        if (_stayConnectedActive) {
                            _subscribeButton = null
                            _subscribeButtonModern = null
                            //                            // did not call stop?
                            //                            stopSubscribeButton(); do this instead of above two lines?
                            _stayConnectedActive = false
                        }
                        val gatt = gatt
                        if (gatt != null) {
                            logExtreme(TAG, "connTest-gatt.disconnect!!!!!!!!!!")
                            gatt.disconnect()
                            logExtreme(TAG, "connTest-closeGatt1")
                            closeGatt()
                        } else {
                            // this should occur when out of range or take battery out since there is 6 second delay, and gatt will be set null in disconnect
                            logError(TAG, "connTest-popConnection gat is null!", false)
                        }
                    }
                }
            }
        }
        val timer = Timer()
        timer.schedule(timerTask, 6000)
    }

    private fun closeGatt() {
        logExtreme(TAG, "closeGatt")
        if (_connectionCount > 0) {
            logError(TAG, "Closing GATT with open connections!", true)
            _connectionCount = 0
            /* try moving just the disconnect into below block
            in case close without disconnect causes issue unregistering client inside close

            BluetoothGatt gatt = getGatt();
            if (gatt != null) {
                gatt.disconnect();
                Log.v(TAG, "gatt.disconnect");
            } */
        }
        if (_connectionCount < 0) {
            logError(TAG, "Closing GATT with negative connections!", false)
            _connectionCount = 0
        }
        val gatt = gatt
        if (gatt == null) {
            logError(TAG, "Closing Null Gatt", true)
            releaseBleLock()
            logExtreme(TAG, "connTest-release1")
        } else {
            // trying to add disconnect here to see if improved behavior, read above comment
            // could make difference in case connectionCount is 0 when it should be 1
            // this was already called-try removing below
            //            gatt.disconnect();
            //            logExtreme(TAG, "connTest-gatt.disconnect!!!!!!!!!!!");
            // may be a timing issue calling close immediately after disconnect - try waiting 100 ms
            // changing this seems to have fixed many unexpected disconnections after executing actions
            try {
                Thread.sleep(100)
            } catch (ex: InterruptedException) {
                logError(TAG, "connTest-" + ex.toString(), true)
            }

            gatt.close()
            XYBase.logExtreme(TAG, "connTest-gatt.close inside closeGatt")
            //            setGatt(null);
            //            releaseBleLock();
            //            logExtreme(TAG, "connTest-release2");
        }
        _currentAction = null //just to make sure
        if (_actionLock.availablePermits() == 0) {
            _actionLock.release(MAX_ACTIONS)
            logExtreme(TAG, "_actionLock releaseMAX")
        }
        reportConnectionStateChanged(STATE_DISCONNECTED)
    }

    private fun getBluetoothManager(context: Context): BluetoothManager {
        return context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    internal fun pulseOutOfRange() {
        logExtreme(TAG, "connTest-pulseOutOfRange device = " + id!!)
        if (_stayConnectedActive) {
            _stayConnectedActive = false
            popConnection()
            isConnected = false
            // disconnect should trigger onConnectionStateChange which calls setGatt(null)
            if (gatt != null) {
                gatt!!.disconnect()
                // calling this in case disconnect callback is not called when ble is off
                gatt = null
            }
            logExtreme(TAG, "connTest-popConnection3")
        }
        if (XYSmartScan.instance.legacy) {
            pulseOutOfRange18()
        } else {
            pulseOutOfRange21()
        }
    }

    fun checkBatteryAndVersion(context: Context) {
        logExtreme(TAG, "checkBatteryAndVersion")
        checkBattery(context, true)
        checkVersion(context)
        checkTimeSinceCharged(context)
    }

    fun checkBatteryAndVersionInFuture(context: Context, repeat: Boolean) {
        logExtreme(TAG, "checkBatteryInFuture")
        if (batteryLevel == BATTERYLEVEL_NOTCHECKED) {
            batteryLevel = BATTERYLEVEL_SCHEDULED
            val checkTimerTask = object : TimerTask() {
                override fun run() {
                    if (repeat) {
                        checkBattery(context, true)
                    } else {
                        checkBattery(context)
                    }
                    checkVersion(context)
                    checkTimeSinceCharged(context)
                }
            }

            val pumpTimer = Timer()
            val random = Random(Date().time)
            //random check in next 6-12 minutes
            val delay = random.nextInt(360000) + 360000
            logExtreme(TAG, "checkBatteryInFuture:$delay")
            if (repeat) {
                pumpTimer.schedule(checkTimerTask, delay.toLong(), delay.toLong())
            } else {
                pumpTimer.schedule(checkTimerTask, delay.toLong())
            }
        }
    }

    private fun checkVersion(context: Context) {
        logExtreme(TAG, "checkFirmware")
        if (firmwareVersion == null) {
            firmwareVersion = ""
            val getVersion = XYFirmware(this, object : XYFirmware.Callback {
                override fun started(success: Boolean, value: String) {
                    if (success) {
                        firmwareVersion = value
                        reportDetected()
                    }
                }

                override fun completed(success: Boolean) {

                }
            })
            getVersion.start(context)
        }
    }

    private fun checkBattery(context: Context) {
        checkBattery(context.applicationContext, false)
    }

    fun checkBattery(context: Context, force: Boolean) {
        logExtreme(TAG, "checkBattery")
        if (batteryLevel < BATTERYLEVEL_CHECKED || force) {
            batteryLevel = BATTERYLEVEL_CHECKED
            logExtreme(TAG, "batteryTest-read battery level for id = " + id!!)
            val battery = XYBattery(this, object : XYBattery.Callback {
                override fun started(success: Boolean, value: Int) {
                    if (success) {
                        batteryLevel = value
                        reportDetected()
                    }
                }

                override fun completed(success: Boolean) {

                }
            })
            battery.start(context)
        }
    }

    private fun checkTimeSinceCharged(context: Context) {
        var device = this
        if (family == Family.Gps) {
            logExtreme(TAG, "checkTimeSinceCharged")
            val getTimeSinceCharged = object : XYDeviceActionGetBatterySinceCharged(device) {
                override fun statusChanged(status: Int, gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, success: Boolean): Boolean {
                    val result = super.statusChanged(status, gatt, characteristic, success)
                    if (status == XYDeviceAction.STATUS_CHARACTERISTIC_READ) {
                        if (success) {
                            val value = this.value
                            timeSinceCharged = 0
                            for (i in 0..3) {
                                timeSinceCharged = timeSinceCharged shl 8
                                timeSinceCharged = timeSinceCharged xor (value!![i].toLong() and 0xFF)
                            }
                            reportDetected()
                        }
                    }
                    return result
                }
            }
            getTimeSinceCharged.start(context.applicationContext)
        }
    }

    @TargetApi(18)
    private fun pulseOutOfRange18() {
        logExtreme(TAG, "pulseOutOfRange18")
        val scanResult = _currentScanResult18
        if (scanResult != null) {
            val newScanResult = ScanResultLegacy(scanResult.device, scanResult.scanRecord, outOfRangeRssi, scanResult.timestampNanos)
            pulse18(newScanResult)
        }
    }

    @TargetApi(21)
    private fun pulseOutOfRange21() {
        //        Log.v(TAG, "pulseOutOfRange21: " + _id);
        val scanResult = _currentScanResult21
        if (scanResult != null) {
            val newScanResult = ScanResult(scanResult.device, scanResult.scanRecord, outOfRangeRssi, scanResult.timestampNanos)
            pulse21(newScanResult)
        }
    }

    @TargetApi(18)
    internal fun pulse18(scanResult: ScanResultLegacy) {
        logExtreme(TAG, "pulse18: " + id + ":" + scanResult.rssi)
        _scansMissed = 0

        if (family == Family.XY3 || family == Family.Gps || family == Family.XY4) {
            val scanRecord = scanResult.scanRecord
            if (scanRecord != null) {
                val manufacturerData = scanResult.scanRecord!!.getManufacturerSpecificData(0x004c)
                if (manufacturerData != null) {
                    if (manufacturerData[21].toInt() and 0x08 == 0x08 && scanResult.rssi != outOfRangeRssi) {
                        if (family == Family.Gps || family == Family.XY4) {
                            if (_currentScanResult18 == null) {
                                _currentScanResult18 = scanResult
                                reportEntered()
                                reportDetected()
                            }
                        }
                        handleButtonPulse()
                        return
                    }
                }
            }
        }

        if (_currentScanResult18 == null || _currentScanResult18!!.rssi == outOfRangeRssi && scanResult.rssi != outOfRangeRssi) {
            _currentScanResult18 = scanResult
            reportEntered()
            reportDetected()
            if (!_stayConnectedActive && _stayConnected) {
                startSubscribeButton()
            }
        } else if (_currentScanResult18!!.rssi != outOfRangeRssi && scanResult.rssi == outOfRangeRssi) {
            _currentScanResult18 = null
            reportExited()
        } else if (scanResult.rssi != outOfRangeRssi) {
            _currentScanResult18 = scanResult
            reportDetected()
            if (!_stayConnectedActive && _stayConnected) {
                startSubscribeButton()
            }
        }
        if (beaconAddress == null) {
            beaconAddress = scanResult.device.address
        }
    }

    @TargetApi(21)
    internal fun pulse21(scanResultObject: Any) {

        val scanResult = scanResultObject as android.bluetooth.le.ScanResult

        //        logExtreme(TAG, "pulse21: " + _id + ":" + scanResult.getRssi());
        _scansMissed = 0

        if (family == Family.XY3 || family == Family.Gps || family == Family.XY4) {
            val scanRecord = scanResult.scanRecord
            if (scanRecord != null) {
                val manufacturerData = scanResult.scanRecord!!.getManufacturerSpecificData(0x004c)

                if (manufacturerData != null) {
                    if (manufacturerData[21].toInt() and 0x08 == 0x08 && scanResult.rssi != outOfRangeRssi) {
                        if (family == Family.Gps || family == Family.XY4) {
                            if (_currentScanResult21 == null) {
                                _currentScanResult21 = scanResult
                                reportEntered()
                                reportDetected()
                            }
                        }
                        handleButtonPulse()
                        logExtreme(TAG, "handleButtonPulse")
                        return
                    }
                }
            }
        }

        if (_currentScanResult21 == null || _currentScanResult21!!.rssi == outOfRangeRssi && scanResult.rssi != outOfRangeRssi) {
            _currentScanResult21 = scanResult
            reportEntered()
            reportDetected()
            if (!_stayConnectedActive && _stayConnected) {
                startSubscribeButton()
            }
        } else if (_currentScanResult21!!.rssi != outOfRangeRssi && scanResult.rssi == outOfRangeRssi) {
            _currentScanResult21 = null
            reportExited()
        } else if (scanResult.rssi != outOfRangeRssi) {
            _currentScanResult21 = scanResult
            reportDetected()
            if (!_stayConnectedActive && _stayConnected) {
                startSubscribeButton()
            }
        }
        if (beaconAddress == null) {
            beaconAddress = scanResult.device.address
        }
    }

    private fun stopSubscribeButton() {
        if (_subscribeButton != null) {
            _subscribeButton!!.stop()
            _subscribeButton = null
        }

        if (_subscribeButtonModern != null) {
            _subscribeButtonModern!!.stop()
            _subscribeButtonModern = null
        }
        XYBase.logExtreme(TAG, "connTest-stopSubscribeButton[" + _connectionCount + "->" + (_connectionCount - 1) + "]:" + id)
        // erase fake connection count we used before to keep connection alive
        popConnection()
    }

    private fun startSubscribeButton() {
        var device = this
        if (_connectedContext == null) {
            return
        }
        if (_bleAccess.availablePermits() <= 1) {
            _stayConnected = false
            return
        }
        _stayConnectedActive = true
        logExtreme(TAG, "connTest-startSubscribeButton[" + _connectionCount + "->" + (_connectionCount + 1) + "]:" + id)
        _connectionCount++ // do not use pushConnection here because then this action will be null inside pushConnection and throw error

        if (family == Family.XY4) {
            _subscribeButtonModern = object : XYDeviceActionSubscribeButtonModern(device) {
                override fun statusChanged(status: Int, gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, success: Boolean): Boolean {
                    val result = super.statusChanged(status, gatt, characteristic, success)
                    if (status == XYDeviceAction.STATUS_CHARACTERISTIC_UPDATED) {
                        val buttonValue = characteristic?.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)!!
                        logExtreme(TAG, "ButtonCharacteristicUpdated:$buttonValue")
                        _buttonRecentlyPressed = true
                        if (_buttonPressedTimer != null) {
                            _buttonPressedTimer!!.cancel()
                            _buttonPressedTimer = null
                        }
                        if (_buttonPressedTimerTask != null) {
                            _buttonPressedTimerTask!!.cancel()
                            _buttonPressedTimerTask = null
                        }
                        _buttonPressedTimer = Timer()
                        _buttonPressedTimerTask = object : TimerTask() {
                            override fun run() {
                                _buttonRecentlyPressed = false
                            }
                        }
                        _buttonPressedTimer!!.schedule(_buttonPressedTimerTask!!, 40000)
                        when (buttonValue) {
                            XYDeviceActionSubscribeButton.BUTTONPRESS_SINGLE -> reportButtonPressed(ButtonType.Single)
                            XYDeviceActionSubscribeButton.BUTTONPRESS_DOUBLE -> reportButtonPressed(ButtonType.Double)
                            XYDeviceActionSubscribeButton.BUTTONPRESS_LONG -> reportButtonPressed(ButtonType.Long)
                            else -> logError(TAG, "Invalid Button Value:$buttonValue", true)
                        }
                    }
                    return result
                }
            }
            _subscribeButtonModern!!.start(_connectedContext!!.applicationContext)
        } else {
            _subscribeButton = object : XYDeviceActionSubscribeButton(device) {
                override fun statusChanged(status: Int, gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, success: Boolean): Boolean {
                    val result = super.statusChanged(status, gatt, characteristic, success)
                    if (status == XYDeviceAction.STATUS_CHARACTERISTIC_UPDATED) {
                        val buttonValue = characteristic?.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)!!
                        logExtreme(TAG, "ButtonCharacteristicUpdated:$buttonValue")
                        _buttonRecentlyPressed = true
                        _buttonRecentlyPressed = true
                        if (_buttonPressedTimer != null) {
                            _buttonPressedTimer!!.cancel()
                            _buttonPressedTimer = null
                        }
                        if (_buttonPressedTimerTask != null) {
                            _buttonPressedTimerTask!!.cancel()
                            _buttonPressedTimerTask = null
                        }
                        _buttonPressedTimer = Timer()
                        _buttonPressedTimerTask = object : TimerTask() {
                            override fun run() {
                                _buttonRecentlyPressed = false
                            }
                        }
                        _buttonPressedTimer!!.schedule(_buttonPressedTimerTask!!, 40000)
                        when (buttonValue) {
                            XYDeviceActionSubscribeButton.BUTTONPRESS_SINGLE -> reportButtonPressed(ButtonType.Single)
                            XYDeviceActionSubscribeButton.BUTTONPRESS_DOUBLE -> reportButtonPressed(ButtonType.Double)
                            XYDeviceActionSubscribeButton.BUTTONPRESS_LONG -> reportButtonPressed(ButtonType.Long)
                            else -> logError(TAG, "Invalid Button Value:$buttonValue", true)
                        }
                    }
                    return result
                }
            }
            _subscribeButton!!.start(_connectedContext!!.applicationContext)
        }
    }

    fun clearScanResults() {
        _currentScanResult18 = null
        _currentScanResult21 = null
        pulseOutOfRange()
    }

    fun getProximity(currentRssi: Int): Proximity {

        val distance = getDistance(currentRssi)

        if (distance < -1.0) {
            return Proximity.OutOfRange
        }
        if (distance < 0.0) {
            return Proximity.None
        }
        if (distance < 0.0173) {
            return Proximity.Touching
        }
        if (distance < 1.0108) {
            return Proximity.VeryNear
        }
        if (distance < 3.0639) {
            return Proximity.Near
        }
        if (distance < 8.3779) {
            return Proximity.Medium
        }
        return if (distance < 20.6086) {
            Proximity.Far
        } else Proximity.VeryFar


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

    internal fun scanComplete() {
        if (isConnected) {
            _scansMissed = 0
        } else {
            _scansMissed++
        }
        if (_scansMissed > _missedPulsesForOutOfRange) {
            logExtreme(TAG, "connTest-_scansMissed > _missedPulsesForOutOfRange(20)")
            _scansMissed = -999999999 //this is here to prevent double exits
            pulseOutOfRange()
        }
    }

    // region =========== Listeners ============

    fun addListener(key: String, listener: Listener) {
        synchronized(_listeners) {
            _listeners.put(key, listener)
        }
    }

    fun removeListener(key: String) {
        synchronized(_listeners) {
            _listeners.remove(key)
        }
    }

    private fun reportEntered() {
        //        Log.i(TAG, "reportEntered");
        enterCount++
        synchronized(_listeners) {
            for ((_, value) in _listeners) {
                value.entered(this)
            }
        }
    }

    private fun reportExited() {
        //        Log.i(TAG, "reportExit");
        exitCount++
        synchronized(_listeners) {
            for ((_, value) in _listeners) {
                value.exited(this)
            }
        }
    }

    private fun reportDetected() {
        //        Log.i(TAG, "reportDetected");
        if (_firstDetectedTime == 0L) {
            _firstDetectedTime = System.currentTimeMillis()
        }
        detectCount++
        synchronized(_listeners) {
            for ((_, value) in _listeners) {
                value.detected(this)
            }
        }
    }

    private fun handleButtonPulse() {
        //        Log.v(TAG, "handleButtonPulse");
        if (_buttonRecentlyPressed) {
            //reportButtonRecentlyPressed(ButtonType.Single);
        } else {
            reportButtonPressed(ButtonType.Single)
            _buttonRecentlyPressed = true

            val timerTask = object : TimerTask() {
                override fun run() {
                    _buttonRecentlyPressed = false
                }
            }
            val timer = Timer()
            timer.schedule(timerTask, 30000)
        }
    }

    private fun reportButtonPressed(buttonType: ButtonType) {
        //        Log.v(TAG, "reportButtonPressed");
        synchronized(_listeners) {
            for ((_, value) in _listeners) {
                value.buttonPressed(this, buttonType)
            }
        }
    }

    private fun reportButtonRecentlyPressed(buttonType: ButtonType) {
        //        Log.v(TAG, "reportButtonRecentlyPressed");
        synchronized(_listeners) {
            for ((_, value) in _listeners) {
                value.buttonRecentlyPressed(this, buttonType)
            }
        }
    }

    private fun reportConnectionStateChanged(newState: Int) {
        logExtreme(TAG, "reportConnectionStateChanged[$id]:$newState")
        synchronized(_listeners) {
            for ((_, value) in _listeners) {
                value.connectionStateChanged(this, newState)
            }
        }
    }

    private fun reportUpdated() {
        logExtreme(TAG, "reportUpdated[$id]")
        synchronized(_listeners) {
            for ((_, value) in _listeners) {
                value.updated(this)
            }
        }
    }

    private fun reportReadRemoteRssi(rssi: Int) {
        logExtreme(TAG, "reportReadRemoteRssi[$id]:$rssi")
        synchronized(_listeners) {
            for ((_, value) in _listeners) {
                value.readRemoteRssi(this, rssi)
            }
        }
    }

    enum class Family {
        Unknown,
        XY1,
        XY2,
        XY3,
        Mobile,
        Gps,
        Near,
        XY4
    }

    enum class ButtonType {
        None,
        Single,
        Double,
        Long
    }

    enum class Proximity {
        None,
        OutOfRange,
        VeryFar,
        Far,
        Medium,
        Near,
        VeryNear,
        Touching
    }

    interface Listener {
        fun entered(device: XYDevice)

        fun exited(device: XYDevice)

        fun detected(device: XYDevice)

        fun buttonPressed(device: XYDevice, buttonType: ButtonType)

        fun buttonRecentlyPressed(device: XYDevice, buttonType: ButtonType)

        fun connectionStateChanged(device: XYDevice, newState: Int)

        fun readRemoteRssi(device: XYDevice, rssi: Int)

        fun updated(device: XYDevice)

        fun statusChanged(status: XYSmartScan.Status)
    }

    companion object {

        val BATTERYLEVEL_INVALID = 0
        val BATTERYLEVEL_CHECKED = -1
        val BATTERYLEVEL_NOTCHECKED = -2
        val BATTERYLEVEL_SCHEDULED = -3

        val uuid2family: HashMap<UUID, XYDevice.Family>

        val family2uuid: HashMap<XYDevice.Family, UUID>

        val family2prefix: HashMap<XYDevice.Family, String>

        private val MAX_BLECONNECTIONS = 4
        private val _bleAccess = XYSemaphore(MAX_BLECONNECTIONS, true)

        private val MAX_ACTIONS = 1

        private var _missedPulsesForOutOfRange = 20
        private var _actionTimeout = 30000

        private var _threadPool: ThreadPoolExecutor? = null
        var instanceCount: Int = 0
            private set

        init {
            _threadPool = ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, LinkedBlockingQueue())
            instanceCount = 0
        }

        init {
            uuid2family = HashMap()
            uuid2family[UUID.fromString("a500248c-abc2-4206-9bd7-034f4fc9ed10")] = XYDevice.Family.XY1
            uuid2family[UUID.fromString("07775dd0-111b-11e4-9191-0800200c9a66")] = XYDevice.Family.XY2
            uuid2family[UUID.fromString("08885dd0-111b-11e4-9191-0800200c9a66")] = XYDevice.Family.XY3
            uuid2family[UUID.fromString("a44eacf4-0104-0000-0000-5f784c9977b5")] = XYDevice.Family.XY4
            uuid2family[UUID.fromString("735344c9-e820-42ec-9da7-f43a2b6802b9")] = XYDevice.Family.Mobile
            uuid2family[UUID.fromString("9474f7c6-47a4-11e6-beb8-9e71128cae77")] = XYDevice.Family.Gps

            family2uuid = HashMap()
            family2uuid[XYDevice.Family.XY1] = UUID.fromString("a500248c-abc2-4206-9bd7-034f4fc9ed10")
            family2uuid[XYDevice.Family.XY2] = UUID.fromString("07775dd0-111b-11e4-9191-0800200c9a66")
            family2uuid[XYDevice.Family.XY3] = UUID.fromString("08885dd0-111b-11e4-9191-0800200c9a66")
            family2uuid[XYDevice.Family.XY4] = UUID.fromString("a44eacf4-0104-0000-0000-5f784c9977b5")
            family2uuid[XYDevice.Family.Mobile] = UUID.fromString("735344c9-e820-42ec-9da7-f43a2b6802b9")
            family2uuid[XYDevice.Family.Gps] = UUID.fromString("9474f7c6-47a4-11e6-beb8-9e71128cae77")

            family2prefix = HashMap()
            family2prefix[XYDevice.Family.XY1] = "ibeacon"
            family2prefix[XYDevice.Family.XY2] = "ibeacon"
            family2prefix[XYDevice.Family.XY3] = "ibeacon"
            family2prefix[XYDevice.Family.XY4] = "ibeacon"
            family2prefix[XYDevice.Family.Mobile] = "mobiledevice"
            family2prefix[XYDevice.Family.Gps] = "gps"
            family2prefix[XYDevice.Family.Near] = "near"
        }

        var Comparator: Comparator<XYDevice> = Comparator { lhs, rhs -> lhs.id!!.compareTo(rhs.id!!) }

        private val TAG = XYDevice::class.java.simpleName

        private val outOfRangeRssi = -999
        val DistanceComparator: Comparator<XYDevice> = Comparator { lhs, rhs -> Integer.compare(lhs.rssi, rhs.rssi) }

        fun buildId(family: Family, major: Int, minor: Int): String? {
            val uuid = getUUID(family)
            if (uuid == null) {
                logError(TAG, "Invalid Family", true)
                return null
            }
            return if (family == Family.XY2 || family == Family.XY3 || family == Family.Gps || family == Family.XY4) {
                "xy:" + getPrefix(family) + ":" + uuid.toString() + "." + major + "." + (minor or 0x0004)
            } else {
                "xy:" + getPrefix(family) + ":" + uuid.toString() + "." + major + "." + minor
            }
        }

        fun normalizeId(id: String): String {
            var normalId: String? = null
            val uuid = getUUID(id)
            if (uuid != null) {
                val family = getFamily(uuid)
                if (family == Family.XY2 || family == Family.XY3 || family == Family.Gps || family == Family.XY4) {
                    normalId = "xy:" + getPrefix(id) + ":" + uuid.toString() + "." + getMajor(id) + "." + (getMinor(id) and 0xfff0 or 0x0004)
                } else {
                    normalId = id
                }
            }
            return if (normalId != null) {
                normalId.toLowerCase()
            } else {
                id.toLowerCase()
            }
        }

        private val STATE_CONNECTED = BluetoothProfile.STATE_CONNECTED
        private val STATE_DISCONNECTED = BluetoothProfile.STATE_DISCONNECTED
        private val STATE_DISCONNECTING = BluetoothProfile.STATE_DISCONNECTING

        fun getUUID(id: String): UUID? {
            val parts = id.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (parts.size != 3) {
                logError(TAG, "getUUID: wrong number of parts [" + id + "] : " + parts.size, true)
                return null
            }

            val parts2 = parts[2].split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (parts2.size != 3) {
                logError(TAG, "getUUID: wrong number of parts2 [" + id + "] : " + parts2.size, true)
                return null
            }
            try {
                return UUID.fromString(parts2[0])
            } catch (ex: NumberFormatException) {
                return null
            }

        }

        fun getPrefix(id: String): String {
            val parts = id.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (parts.size < 3) {
                logError(TAG, "getPrefix: wrong number of parts [" + id + "] : " + parts.size, true)
                return "unknown"
            }

            return parts[1]
        }

        fun getPrefix(family: Family): String {
            return family2prefix[family]!!
        }

        fun getMajor(id: String): Int {
            val parts = id.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (parts.size != 3) {
                logError(TAG, "getMajor: wrong number of parts [" + id + "] : " + parts.size, true)
                return -1
            }

            val parts2 = parts[2].split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (parts2.size != 3) {
                logError(TAG, "getMajor: wrong number of parts2 [" + id + "] : " + parts2.size, true)
                return -1
            }
            return Integer.parseInt(parts2[1])
        }

        fun getMinor(id: String): Int {
            val parts = id.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (parts.size != 3) {
                logError(TAG, "getMinor: wrong number of parts [" + id + "] : " + parts.size, true)
                return -1
            }

            val parts2 = parts[2].split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (parts2.size != 3) {
                logError(TAG, "getMinor: wrong number of parts2 [" + id + "] : " + parts2.size, true)
                return -1
            }
            return Integer.parseInt(parts2[2])
        }

        fun getFamily(uuid: UUID?): Family {
            return uuid2family[uuid] ?: return Family.Unknown
        }

        fun getUUID(family: Family): UUID? {
            return family2uuid[family]
        }
    }
    // endregion =========== Listeners ============
}
