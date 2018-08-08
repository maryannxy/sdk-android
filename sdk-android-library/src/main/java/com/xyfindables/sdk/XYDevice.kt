package com.xyfindables.sdk

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.xyfindables.core.XYBase
import com.xyfindables.sdk.action.XYDeviceAction
import com.xyfindables.sdk.action.XYDeviceActionGetBatterySinceCharged
import com.xyfindables.sdk.action.XYDeviceActionSubscribeButton
import com.xyfindables.sdk.action.XYDeviceActionSubscribeButtonModern
import com.xyfindables.sdk.action.dialog.SubscribeSpotaNotifications
import com.xyfindables.sdk.actionHelper.XYBattery
import com.xyfindables.sdk.actionHelper.XYFirmware
import com.xyfindables.sdk.devices.XYFinderBluetoothDevice
import com.xyfindables.sdk.scanner.XYScanResult
import com.xyfindables.sdk.scanner.XYScanResultManual
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import java.util.*

/**
 * Created by arietrouw on 12/20/16.
 */

@Deprecated("Use XYBluetoothDevice and XYFilteredSmartScan instead")
class XYDevice internal constructor(id: String) : XYBase() {

    private var _connectIntent = false

    private var _rssi: Int = 0

    var isConnected = false
        private set
    private var _connectionCount = 0
    private var _stayConnected = false
    private var _stayConnectedActive = false
    private var _isInOtaMode = false

    enum class BluetoothStatus {
        None,
        Enabled,
        BluetoothUnavailable,
        BluetoothUnstable,
        BluetoothDisabled,
        LocationDisabled
    }

    private val actionPool = newFixedThreadPoolContext(1, hashCode().toString())

    fun queueAction2(context: Context, action: XYDeviceAction) : Deferred<XYDeviceAction> {
        return async(actionPool) {
            val connected = connect().await()
            if (connected) {
                action.start(context)
            }
            return@async action
        }
    }

    fun connect() : Deferred<Boolean> {
        return async {
            return@async true
        }
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

    private var scansMissed: Int = 0
    private var _buttonRecentlyPressed = false

    private var _currentScanResult: XYScanResult? = null

    private val _listeners = HashMap<String, Listener>()
    private var _currentAction: XYDeviceAction? = null

    val rssi: Int
        get() {
            var rssiToReturn = outOfRangeRssi
            if (_currentScanResult != null) {
                rssiToReturn = _currentScanResult!!.rssi
            }
            return rssiToReturn
        }

    private val txPowerLevel: Int
        get() = if (_currentScanResult == null) {
            0
        } else {
            txPowerLevelFromScanResult(_currentScanResult)
        }

    var spotaNotifications: SubscribeSpotaNotifications? = null

    private var _subscribeButton: XYDeviceActionSubscribeButton? = null
    private var _subscribeButtonModern: XYDeviceActionSubscribeButtonModern? = null

    private var _actionFrameTimer: Timer? = null

    // this could cause ui "searching" because rssi = 0 when no values returning
    val distance: Double
        get() {
            val tx = -60

            val rssi = rssi.toDouble()

            if (rssi == outOfRangeRssi.toDouble()) {
                return -2.0
            }

            val ratio = rssi * 1.0 / tx
            return if (ratio < 1.0) {
                Math.pow(ratio, 10.0)
            } else {
                0.89976 * Math.pow(ratio, 7.7095) + 0.111
            }
        }

    private val bluetoothDevice: BluetoothDevice?
        get() {
            logExtreme(TAG, "getBluetoothDevice")
            val scanResult = _currentScanResult
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
            return if (currentTime - _lastUpdateTime > 1800000) {
                _lastUpdateTime = currentTime
                true
            } else {
                false
            }
        }

    val minor: Int
        get() = getMinor(id!!)

    val family: Family
        get() {
            val familyFromUUID = getFamily(uuid)
            return if (familyFromUUID != Family.Unknown) {
                familyFromUUID
            } else {
                val prefix = prefix
                when (prefix) {
                    "near" -> Family.Near
                    "mobiledevice" -> Family.Mobile
                    else -> Family.Unknown
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
        this.id = normalizeId(id)
        if (this.id == null) {
            this.id = id
        }
    }

    protected fun txPowerLevelFromScanResult(scanResult: XYScanResult?): Int {
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

    fun otaMode(value: Boolean) {
        logExtreme(TAG, "otaMode set: $value")
        if (value == _isInOtaMode) {
            return
        }
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

    fun getDistanceWithCustomTx(tx: Int): Double {

        val rssi = rssi.toDouble()

        return if (tx == 0 || rssi == 0.0 || rssi == outOfRangeRssi.toDouble()) {
            -1.0
        } else {
            val ratio = rssi * 1.0 / tx
            if (ratio < 1.0) {
                Math.pow(ratio, 10.0)
            } else {
                0.89976 * Math.pow(ratio, 7.7095) + 0.111
            }
        }
    }

    private fun getDistance(rssi: Int): Double {
        val tx = -60

        if (rssi == outOfRangeRssi) {
            return -2.0
        }

        val ratio = rssi * 1.0 / tx
        return if (ratio < 1.0) {
            Math.pow(ratio, 10.0)
        } else {
            0.89976 * Math.pow(ratio, 7.7095) + 0.111 // made for tx of -59 I believe so works with most our devices, but does not work with xy4 of tx -75
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
        val device = this
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

    private fun pulseOutOfRange() {
        logExtreme(TAG, "pulseOutOfRange")
        val scanResult = _currentScanResult
        if (scanResult != null) {
            val newScanResult = XYScanResultManual(scanResult.device, scanResult.rssi, scanResult.scanRecord, scanResult.timestampNanos)
            pulse(newScanResult)
        }
    }

    internal fun pulse(scanResult: XYScanResult) {
        logExtreme(TAG, "pulse18: " + id + ":" + scanResult.rssi)
        scansMissed = 0

        if (family == Family.XY3 || family == Family.Gps || family == Family.XY4) {
            val scanRecord = scanResult.scanRecord
            if (scanRecord != null) {
                val manufacturerData = scanResult.scanRecord!!.getManufacturerSpecificData(0x004c)
                if (manufacturerData != null) {
                    if (manufacturerData[21].toInt() and 0x08 == 0x08 && scanResult.rssi != outOfRangeRssi) {
                        if (family == Family.Gps || family == Family.XY4) {
                            if (_currentScanResult == null) {
                                _currentScanResult = scanResult
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

        if (_currentScanResult == null || _currentScanResult!!.rssi == outOfRangeRssi && scanResult.rssi != outOfRangeRssi) {
            _currentScanResult = scanResult
            reportEntered()
            reportDetected()
            if (!_stayConnectedActive && _stayConnected) {
                startSubscribeButton()
            }
        } else if (_currentScanResult!!.rssi != outOfRangeRssi && scanResult.rssi == outOfRangeRssi) {
            _currentScanResult = null
            reportExited()
        } else if (scanResult.rssi != outOfRangeRssi) {
            _currentScanResult = scanResult
            reportDetected()
            if (!_stayConnectedActive && _stayConnected) {
                startSubscribeButton()
            }
        }
        if (beaconAddress == null) {
            beaconAddress = scanResult.device?.address
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
    }

    private fun startSubscribeButton() {
        val device = this
        if (_connectedContext == null) {
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
        _currentScanResult = null
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
            scansMissed = 0
        } else {
            scansMissed++
        }
        if (scansMissed > missedPulsesForOutOfRange) {
            logExtreme(TAG, "connTest-scansMissed > missedPulsesForOutOfRange(20)")
            scansMissed = -999999999 //this is here to prevent double exits
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
                async(CommonPool) {
                    value.entered(this@XYDevice)
                }
            }
        }
    }

    private fun reportExited() {
        //        Log.i(TAG, "reportExit");
        exitCount++
        synchronized(_listeners) {
            for ((_, value) in _listeners) {
                async(CommonPool) {
                    value.exited(this@XYDevice)
                }
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
                async(CommonPool) {
                    value.detected(this@XYDevice)
                }
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

        fun statusChanged(status: BluetoothStatus)
    }

    companion object {

        const val BATTERYLEVEL_INVALID = 0
        const val BATTERYLEVEL_CHECKED = -1
        const val BATTERYLEVEL_NOTCHECKED = -2
        const val BATTERYLEVEL_SCHEDULED = -3

        val uuid2family: HashMap<UUID, XYFinderBluetoothDevice.Family>

        val family2uuid: HashMap<XYFinderBluetoothDevice.Family, UUID>

        val family2prefix: HashMap<XYFinderBluetoothDevice.Family, String>

        private const val MAX_BLECONNECTIONS = 4

        private const val MAX_ACTIONS = 1

        private var missedPulsesForOutOfRange = 20

        init {
            uuid2family = HashMap()
            uuid2family[UUID.fromString("a500248c-abc2-4206-9bd7-034f4fc9ed10")] = XYFinderBluetoothDevice.Family.XY1
            uuid2family[UUID.fromString("07775dd0-111b-11e4-9191-0800200c9a66")] = XYFinderBluetoothDevice.Family.XY2
            uuid2family[UUID.fromString("08885dd0-111b-11e4-9191-0800200c9a66")] = XYFinderBluetoothDevice.Family.XY3
            uuid2family[UUID.fromString("a44eacf4-0104-0000-0000-5f784c9977b5")] = XYFinderBluetoothDevice.Family.XY4
            uuid2family[UUID.fromString("735344c9-e820-42ec-9da7-f43a2b6802b9")] = XYFinderBluetoothDevice.Family.Mobile
            uuid2family[UUID.fromString("9474f7c6-47a4-11e6-beb8-9e71128cae77")] = XYFinderBluetoothDevice.Family.Gps

            family2uuid = HashMap()
            family2uuid[XYFinderBluetoothDevice.Family.XY1] = UUID.fromString("a500248c-abc2-4206-9bd7-034f4fc9ed10")
            family2uuid[XYFinderBluetoothDevice.Family.XY2] = UUID.fromString("07775dd0-111b-11e4-9191-0800200c9a66")
            family2uuid[XYFinderBluetoothDevice.Family.XY3] = UUID.fromString("08885dd0-111b-11e4-9191-0800200c9a66")
            family2uuid[XYFinderBluetoothDevice.Family.XY4] = UUID.fromString("a44eacf4-0104-0000-0000-5f784c9977b5")
            family2uuid[XYFinderBluetoothDevice.Family.Mobile] = UUID.fromString("735344c9-e820-42ec-9da7-f43a2b6802b9")
            family2uuid[XYFinderBluetoothDevice.Family.Gps] = UUID.fromString("9474f7c6-47a4-11e6-beb8-9e71128cae77")

            family2prefix = HashMap()
            family2prefix[XYFinderBluetoothDevice.Family.XY1] = "ibeacon"
            family2prefix[XYFinderBluetoothDevice.Family.XY2] = "ibeacon"
            family2prefix[XYFinderBluetoothDevice.Family.XY3] = "ibeacon"
            family2prefix[XYFinderBluetoothDevice.Family.XY4] = "ibeacon"
            family2prefix[XYFinderBluetoothDevice.Family.Mobile] = "mobiledevice"
            family2prefix[XYFinderBluetoothDevice.Family.Gps] = "gps"
            family2prefix[XYFinderBluetoothDevice.Family.Near] = "near"
        }

        var Comparator: Comparator<XYDevice> = Comparator { lhs, rhs -> lhs.id!!.compareTo(rhs.id!!) }

        private val TAG = XYDevice::class.java.simpleName

        private const val outOfRangeRssi = -999
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
                normalId = if (family == Family.XY2 || family == Family.XY3 || family == Family.Gps || family == Family.XY4) {
                    "xy:" + getPrefix(id) + ":" + uuid.toString() + "." + getMajor(id) + "." + (getMinor(id) and 0xfff0 or 0x0004)
                } else {
                    id
                }
            }
            return normalId?.toLowerCase() ?: id.toLowerCase()
        }

        private const val STATE_CONNECTED = BluetoothProfile.STATE_CONNECTED
        private const val STATE_DISCONNECTED = BluetoothProfile.STATE_DISCONNECTED
        private const val STATE_DISCONNECTING = BluetoothProfile.STATE_DISCONNECTING

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
            return try {
                UUID.fromString(parts2[0])
            } catch (ex: NumberFormatException) {
                null
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
            return "" //family2prefix[family]!!
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
            return Family.Unknown //uuid2family[uuid] ?: return Family.Unknown
        }

        fun getUUID(family: Family): UUID? {
            return null //family2uuid[family]
        }
    }
    // endregion =========== Listeners ============
}
