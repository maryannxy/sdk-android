package com.xyfindables.sdk

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.AsyncTask
import android.os.Build
import android.os.Looper

import com.xyfindables.core.XYBase
import kotlinx.coroutines.experimental.async

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.ArrayList
import java.util.Calendar
import java.util.Date
import java.util.HashMap
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.Semaphore
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

abstract class XYSmartScan internal constructor() : XYBase(), XYDevice.Listener {

    private var _status = Status.None

    private val _devicesLock = ReentrantLock()

    var period = 0
        private set
    var interval = 0
        private set
    //private GoogleApiClient _googleApiClient;

    private var _autoScanTimerTask: TimerTask? = null

    private var _autoScanTimer: Timer? = null
    private var paused = false

    private val _devices: HashMap<String, XYDevice>
    var scanCount = 0

    var pulseCount = 0

    internal var _processedPulseCount = 0
    internal var _scansWithoutPulses = 0

    internal var receiver: BroadcastReceiver? = null

    var currentDeviceId: Long = 0
        private set

    internal var _scanningControl = Semaphore(1, true)

    private val _listeners = HashMap<String, XYDevice.Listener>()

    private var _receiverRegistered = false

    var legacy = true

    val outOfRangePulsesMissed: Int
        get() = _missedPulsesForOutOfRange

    var missedPulsesForOutOfRange: Int
        get() = _missedPulsesForOutOfRange
        set(value) {
            _missedPulsesForOutOfRange = value
        }

    //the device we are running on
    val hostDevice: XYDevice?
        get() {
            val id = XYDevice.buildId(XYDevice.Family.Mobile, (currentDeviceId and 0xffff0000L shr 16).toInt(), (currentDeviceId and 0xffff).toInt())
            return if (id == null) {
                null
            } else {
                deviceFromId(id)
            }
        }

    val devices: List<XYDevice>?
        get() {
            try {
                val list = ArrayList<XYDevice>()
                if (_devicesLock.tryLock(defaultLockTimeout.toLong(), defaultLockTimeUnits)) {
                    for ((_, value) in _devices) {
                        list.add(value)
                    }
                    _devicesLock.unlock()
                    return list
                } else {
                    XYBase.logError(TAG, "getDevices failed due to lock:" + _devicesLock.holdCount, true)
                    return null
                }
            } catch (ex: InterruptedException) {
                XYBase.logException(TAG, ex, true)
                return null
            }

        }

    val pulsesPerSecond: Long
        get() {
            val seconds = (Calendar.getInstance().timeInMillis - startTime) / 1000
            return if (seconds == 0L) {
                pulseCount.toLong()
            } else pulseCount / seconds
        }

    enum class Status {
        None,
        Enabled,
        BluetoothUnavailable,
        BluetoothUnstable,
        BluetoothDisabled,
        LocationDisabled
    }

    internal fun getBluetoothManager(context: Context): BluetoothManager {
        return context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    init {
        logInfo(TAG, TAG)
        _devices = HashMap()
    }

    internal fun setAllToOutOfRange() {
        try {
            if (_devicesLock.tryLock(defaultLockTimeout.toLong(), defaultLockTimeUnits)) {
                for ((_, value) in _devices) {
                    value.pulseOutOfRange()
                }
                _devicesLock.unlock()
            } else {
                XYBase.logError(TAG, "getDevices failed due to lock:" + _devicesLock.holdCount, true)
            }
        } catch (ex: InterruptedException) {
            XYBase.logException(TAG, ex, true)
        }

    }

    fun init(context: Context, currentDeviceId: Long, missedPulsesForOutOfRange: Int) {
        logInfo(TAG, "init")

        _missedPulsesForOutOfRange = missedPulsesForOutOfRange

        this.currentDeviceId = currentDeviceId

        if (!_receiverRegistered) {

            val receiver = receiver

            val appContext = context.applicationContext

            appContext.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
            appContext.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED))
            appContext.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
            appContext.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED))
            appContext.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED))
            appContext.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE))
            appContext.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            appContext.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED))

            _receiverRegistered = true
        }
    }

    fun cleanup(context: Context) {
        if (_receiverRegistered) {
            context.applicationContext.unregisterReceiver(receiver)
            _receiverRegistered = false
        }
        stopAutoScan()
    }

    fun enableBluetooth(context: Context) {
        val bluetoothAdapter = getBluetoothManager(context.applicationContext).adapter

        if (bluetoothAdapter == null) {
            logInfo(TAG, "Bluetooth Disabled")
            return
        }
        bluetoothAdapter.enable()
    }

    fun deviceFromId(id: String): XYDevice? {
        if (id.isEmpty()) {
            XYBase.logError(TAG, "Tried to get a null device", true)
            return null
        }

        val lowerId = id.toLowerCase()

        var device: XYDevice?
        try {
            if (_devicesLock.tryLock(defaultLockTimeout.toLong(), defaultLockTimeUnits)) {
                device = _devices[id]
                _devicesLock.unlock()
            } else {
                logError(TAG, "deviceFromId failed due to lock (getUniqueId):" + _devicesLock.holdCount, true)
                return null
            }
        } catch (ex: InterruptedException) {
            logException(TAG, ex, true)
            return null
        }

        if (device == null) {
            device = XYDevice(lowerId)
            try {
                if (_devicesLock.tryLock(defaultLockTimeout.toLong(), defaultLockTimeUnits)) {
                    _devices[lowerId] = device
                    _devicesLock.unlock()
                } else {
                    logError(TAG, "deviceFromId failed due to lock (put):" + _devicesLock.holdCount, true)
                    return null
                }
            } catch (ex: InterruptedException) {
                logException(TAG, ex, true)
                return null
            }

        }
        device.addListener(TAG, this)
        return device
    }

    fun startAutoScan(context: Context, interval: Int, period: Int) {
        logExtreme(TAG, "startAutoScan:$interval:$period")
        if (_autoScanTimer != null) {
            //XYData.logError(TAG, "startAutoScan already Started (_autoScanTimer != null)");
            stopAutoScan()
        }
        if (_autoScanTimerTask != null) {
            XYBase.logError(TAG, "connTest-startAutoScan already Started (_autoScanTimerTask != null)", true)
            stopAutoScan()
        }
        this.period = period
        this.interval = interval
        val scanner = this
        _autoScanTimerTask = object : TimerTask() {
            override fun run() {
                scanner.scan(context.applicationContext, period)
            }
        }
        _autoScanTimer = Timer()
        _autoScanTimer!!.schedule(_autoScanTimerTask!!, 0, (interval + period).toLong())
    }

    fun stopAutoScan() {
        logExtreme(TAG, "stopAutoScan")
        if (_autoScanTimer != null) {
            _autoScanTimer!!.cancel()
            _autoScanTimer = null
        } else {
            XYBase.logError(TAG, "stopAutoScan already Stopped (_autoScanTimer == null)", false)
        }
        if (_autoScanTimerTask != null) {
            _autoScanTimerTask!!.cancel()
            _autoScanTimerTask = null
        } else {
            XYBase.logError(TAG, "stopAutoScan already Stopped (_autoScanTimerTask == null)", false)
        }
    }

    fun pauseAutoScan(pause: Boolean) {
        logExtreme(TAG, "pauseAutoScan:$pause")
        if (pause == this.paused) {
            return
        }
        if (pause) {
            //if (_scanningControl.tryAcquire()) {
            this.paused = true
            //}
        } else {
            //_scanningControl.release();
            this.paused = false
        }
    }

    fun setStatus(status: Status) {
        if (_status != status) {
            _status = status
            reportStatusChanged(status)
        }
    }

    fun areLocationServicesAvailable(context: Context): Boolean {

        var result = false

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val gps_enabled: Boolean
        val network_enabled: Boolean

        gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)

        network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (gps_enabled && network_enabled) {
            setStatus(Status.None)
            result = true
        } else {
            setStatus(Status.LocationDisabled)
        }
        logInfo(TAG, "areLocationServicesAvailable: $result")
        return result
    }

    fun getStatus(context: Context, refresh: Boolean): Status {
        if (_status == Status.None || refresh) {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null) {
                _status = Status.BluetoothUnavailable
            } else {
                if (bluetoothAdapter.isEnabled) {
                    if (isLocationAvailable(context.applicationContext)) {
                        _status = Status.Enabled
                    } else {
                        _status = Status.LocationDisabled
                    }
                } else {
                    _status = Status.BluetoothDisabled
                }
            }
        }
        return _status
    }

    fun getDevice(index: Int): XYDevice? {
        try {
            var count = 0
            var device: XYDevice? = null
            if (_devicesLock.tryLock(defaultLockTimeout.toLong(), defaultLockTimeUnits)) {
                for ((_, value) in _devices) {
                    if (count == index) {
                        device = value
                        break
                    }
                    count++
                }
                _devicesLock.unlock()
                return device
            } else {
                XYBase.logError(TAG, "getDevices failed due to lock:" + _devicesLock.holdCount, true)
                return null
            }
        } catch (ex: InterruptedException) {
            XYBase.logException(TAG, ex, true)
            return null
        }

    }

    protected fun xyIdFromAppleBytes(iBeaconBytes: ByteArray): String? {
        //check for ibeacon header
        if (iBeaconBytes[0].toInt() == 0x02 && iBeaconBytes[1].toInt() == 0x15) {
            if (iBeaconBytes.size != 23) {
                XYBase.logError(TAG, "iBeacon should have 23 bytes", true)
                return null
            }
            val hexString = bytesToHex(iBeaconBytes)
            val uuid = hexString.substring(4, 12) + "-" +
                    hexString.substring(12, 16) + "-" +
                    hexString.substring(16, 20) + "-" +
                    hexString.substring(20, 24) + "-" +
                    hexString.substring(24, 36)

            val major = (iBeaconBytes[18].toInt() and 0xff) * 0x100 + (iBeaconBytes[19].toInt() and 0xff)

            var minor = (iBeaconBytes[20].toInt() and 0xff) * 0x100 + (iBeaconBytes[21].toInt() and 0xff)

            val family = XYDevice.uuid2family[UUID.fromString(uuid)] ?: return null

            if (family == XYDevice.Family.XY2 || family == XYDevice.Family.XY3 || family == XYDevice.Family.Gps || family == XYDevice.Family.XY4) {
                minor = minor and 0xfff0 or 0x0004
            }

            return String.format(Locale.getDefault(), "xy:%1\$s:%2\$s.%3\$d.%4\$d", XYDevice.family2prefix[family], uuid, major, minor).toLowerCase()
        } else {
            return null
        }
    }

    protected fun notifyDevicesOfScanComplete() {
        logExtreme(TAG, "notifyDevicesOfScanComplete")
        val deviceList = devices

        for (device in deviceList!!) {
            device.scanComplete()
        }
    }

    protected fun dump(context: Context) {

        val bluetoothAdapter = getBluetoothManager(context.applicationContext).adapter

        if (bluetoothAdapter == null) {
            logInfo(TAG, "Bluetooth Disabled")
            return
        }

        logExtreme(TAG, "=========================")
        logExtreme(TAG, "bluetoothAdapter:scanMode:" + bluetoothAdapter.scanMode)
        logExtreme(TAG, "bluetoothAdapter:state:" + bluetoothAdapter.state)
        logExtreme(TAG, "bluetoothAdapter:name:" + bluetoothAdapter.name)
        logExtreme(TAG, "bluetoothAdapter:address:" + bluetoothAdapter.address)
        logExtreme(TAG, "bluetoothAdapter:GATT Connection State:" + bluetoothAdapter.getProfileConnectionState(BluetoothProfile.GATT))
        logExtreme(TAG, "scanCount:$scanCount")
        logExtreme(TAG, "pulseCount:$pulseCount")
        logExtreme(TAG, "processedPulseCount:$pulseCount")
        logExtreme(TAG, "mainLooper:" + Looper.getMainLooper().toString())
        logExtreme(TAG, "=========================")
    }

    // region ============= Listeners =============

    protected abstract fun scan(context: Context, period: Int)

    fun addListener(key: String, listener: XYDevice.Listener) {
        async {
            synchronized(XYSmartScan.instance._listeners) {
                XYSmartScan.instance._listeners.put(key, listener)
            }
        }
    }

    fun removeListener(key: String) {
        async {
            synchronized(XYSmartScan.instance._listeners) {
                XYSmartScan.instance._listeners.remove(key)
            }
        }
    }

    private fun reportEntered(device: XYDevice) {
        async {
            synchronized(XYSmartScan.instance._listeners) {
                for ((_, value) in XYSmartScan.instance._listeners) {
                    value.entered(device)
                }
            }
        }
    }

    private fun reportExited(device: XYDevice) {
        async {
            synchronized(XYSmartScan.instance._listeners) {
                for ((_, value) in XYSmartScan.instance._listeners) {
                    value.exited(device)
                }
            }
        }
    }

    private fun reportDetected(device: XYDevice) {
        async {
            synchronized(XYSmartScan.instance._listeners) {
                for ((_, value) in XYSmartScan.instance._listeners) {
                    value.detected(device)
                }
            }
        }
    }

    private fun reportButtonPressed(device: XYDevice, buttonType: XYDevice.ButtonType) {
        async {
            synchronized(XYSmartScan.instance._listeners) {
                for ((_, value) in XYSmartScan.instance._listeners) {
                    value.buttonPressed(device, buttonType)
                }
            }
        }
    }

    private fun reportButtonRecentlyPressed(device: XYDevice, buttonType: XYDevice.ButtonType) {
        async {
            synchronized(XYSmartScan.instance._listeners) {
                for ((_, value) in XYSmartScan.instance._listeners) {
                    value.buttonRecentlyPressed(device, buttonType)
                }
            }
        }
    }

    // endregion ============= Listeners =============

    // region ============= XYDevice Listener =============

    override fun entered(device: XYDevice) {
        reportEntered(device)
    }

    override fun exited(device: XYDevice) {
        reportExited(device)
    }

    override fun detected(device: XYDevice) {
        reportDetected(device)
    }

    override fun buttonPressed(device: XYDevice, buttonType: XYDevice.ButtonType) {
        reportButtonPressed(device, buttonType)
    }

    override fun buttonRecentlyPressed(device: XYDevice, buttonType: XYDevice.ButtonType) {
        reportButtonRecentlyPressed(device, buttonType)
    }

    override fun connectionStateChanged(device: XYDevice, newState: Int) {

    }

    override fun readRemoteRssi(device: XYDevice, rssi: Int) {

    }

    override fun updated(device: XYDevice) {

    }

    // endregion ============= XYDevice Listener =============

    fun refresh(gatt: BluetoothGatt) {
        XYBase.logError(TAG, "connTest-Calling refresh", true)
        try {
            val refresh = gatt.javaClass.getMethod("refresh")
            refresh.invoke(gatt)
        } catch (ex: NoSuchMethodException) {
            XYBase.logException(TAG, ex, true)
        } catch (ex: InvocationTargetException) {
            XYBase.logException(TAG, ex, true)
        } catch (ex: IllegalAccessException) {
            XYBase.logException(TAG, ex, true)
        }

    }

    private fun reportStatusChanged(status: Status) {
        synchronized(_listeners) {
            for ((_, value) in _listeners) {
                value.statusChanged(status)
            }
        }
    }

    companion object {

        private val hexArray = "0123456789ABCDEF".toCharArray()
        private val TAG = XYSmartScan::class.java.simpleName

        val instance: XYSmartScan

        init {
            if (Build.VERSION.SDK_INT >= 21) {
                instance = XYSmartScanModern()
            } else {
                instance = XYSmartScanLegacy()
            }
        }

        private val _threadPoolListeners = ThreadPoolExecutor(1, 5, 30, TimeUnit.SECONDS, LinkedBlockingQueue())

        private val defaultLockTimeout = 10
        internal val scansWithOutPulsesBeforeRestart = 100
        private var _missedPulsesForOutOfRange = 20

        private val defaultLockTimeUnits = TimeUnit.SECONDS
        private val startTime = Calendar.getInstance().timeInMillis

        private fun bytesToHex(bytes: ByteArray): String {
            val hexChars = CharArray(bytes.size * 2)
            for (j in bytes.indices) {
                val v = bytes[j].toInt() and 0xFF
                hexChars[j * 2] = hexArray[v.ushr(4)]
                hexChars[j * 2 + 1] = hexArray[v and 0x0F]
            }
            return String(hexChars)
        }

        fun isLocationAvailable(context: Context): Boolean {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            val gps_enabled: Boolean
            val network_enabled: Boolean

            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)

            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (gps_enabled && network_enabled) {
                logInfo(TAG, "returning 3")
                return true
            } else if (gps_enabled) {
                logInfo(TAG, "returning 2")
                return false
            } else if (network_enabled) {
                logInfo(TAG, "returning 1")
                return false
            } else {
                logInfo(TAG, "returning 0")
                return false
            }
        }
    }

}
