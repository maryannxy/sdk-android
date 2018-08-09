package com.xyfindables.sdk.devices

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import com.xyfindables.core.XYBase
import com.xyfindables.sdk.gatt.XYBluetoothResult
import com.xyfindables.sdk.scanner.XYScanResult
import com.xyfindables.sdk.services.EddystoneConfigService
import com.xyfindables.sdk.services.EddystoneService
import com.xyfindables.sdk.services.standard.*
import com.xyfindables.sdk.services.xy4.PrimaryService
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.launch
import unsigned.Ushort
import java.nio.ByteBuffer
import java.util.*

open class XY4BluetoothDevice(context: Context, scanResult: XYScanResult, hash: Int) : XYFinderBluetoothDevice(context, scanResult, hash) {

    val alertNotification = AlertNotificationService(this)
    val batteryService = BatteryService(this)
    val currentTimeService = CurrentTimeService(this)
    val deviceInformationService = DeviceInformationService(this)
    val genericAccessService = GenericAccessService(this)
    val genericAttributeService = GenericAttributeService(this)
    val linkLossService = LinkLossService(this)
    val txPowerService = TxPowerService(this)

    val eddystoneService = EddystoneService(this)
    val eddystoneConfigService = EddystoneConfigService(this)

    val primary = PrimaryService(this)

    private var lastButtonPressTime = 0L

    private val buttonListener = object : XYBluetoothGattCallback() {
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            logInfo("onCharacteristicChanged")
            super.onCharacteristicChanged(gatt, characteristic)
            if (characteristic?.uuid == primary.buttonState.uuid) {
                reportButtonPressed(buttonPressFromInt(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)))
            }
        }
    }

    init {
        addGattListener("xy4", buttonListener)
    }

    override val prefix = "xy:ibeacon"

    override fun find(): Deferred<XYBluetoothResult<Int>> {
        logInfo("find")
        return primary.buzzer.set(11)
    }

    override fun lock(): Deferred<XYBluetoothResult<ByteArray>> {
        logInfo("lock")
        return primary.lock.set(XY4BluetoothDevice.DefaultLockCode)
    }

    override fun unlock(): Deferred<XYBluetoothResult<ByteArray>> {
        logInfo("unlock")
        return primary.unlock.set(XY4BluetoothDevice.DefaultLockCode)
    }

    override fun stayAwake(): Deferred<XYBluetoothResult<Int>> {
        logInfo("stayAwake")
        return primary.stayAwake.set(1)
    }

    override fun fallAsleep(): Deferred<XYBluetoothResult<Int>> {
        logInfo("fallAsleep")
        return primary.stayAwake.set(0)
    }

    override fun batteryLevel(): Deferred<XYBluetoothResult<Int>> {
        logInfo("batteryLevel")
        return batteryService.level.get()
    }

    override fun onDetect(scanResult: XYScanResult?) {
        super.onDetect(scanResult)
        logInfo("onDetect: $scanResult")
        if (scanResult != null) {
            if (pressFromScanResult(scanResult)) {
                logInfo("onDetect: pressFromScanResult: true")
                if (now - lastButtonPressTime > BUTTON_ADVERTISEMENT_LENGTH) {
                    logInfo("onDetect: pressFromScanResult: first")
                    reportButtonPressed(ButtonPress.Single)
                    lastButtonPressTime = now
                }
            }
        }
    }

    fun reportButtonPressed(state: ButtonPress) {
        logInfo("reportButtonPressed")
        launch (CommonPool) {
            synchronized(listeners) {
                for (listener in listeners) {
                    val xyFinderListener = listener.value as? XYFinderBluetoothDevice.Listener
                    if (xyFinderListener != null) {
                        logInfo("reportButtonPressed: $xyFinderListener")
                        launch(CommonPool) {
                            when (state) {
                                ButtonPress.Single -> xyFinderListener.buttonSinglePressed(this@XY4BluetoothDevice)
                                ButtonPress.Double -> xyFinderListener.buttonDoublePressed(this@XY4BluetoothDevice)
                                ButtonPress.Long -> xyFinderListener.buttonLongPressed(this@XY4BluetoothDevice)
                                else -> {
                                }
                            }
                            if (connectionState == ConnectionState.Connected) {
                                //every time a notify fires, we have to re-enable it
                                primary.buttonState.enableNotify(true)
                            }
                        }
                    }
                }
            }
        }
        XY4BluetoothDevice.reportButtonPressed(this, state)
    }

    override val minor : Ushort
        get() {
            //we have to mask the low nibble for the power level
            return _minor.and(0xfff0).or(0x0004)
        }

    open class Listener : XYFinderBluetoothDevice.Listener() {
        override fun buttonSinglePressed(device: XYFinderBluetoothDevice) {

        }

        override fun buttonDoublePressed(device: XYFinderBluetoothDevice) {

        }

        override fun buttonLongPressed(device: XYFinderBluetoothDevice) {

        }
    }

    companion object : XYBase() {

        private val FAMILY_UUID = UUID.fromString("a44eacf4-0104-0000-0000-5f784c9977b5")!!

        protected val listeners = HashMap<String, XY4BluetoothDevice.Listener>()

        val DefaultLockCode = byteArrayOf(0x00.toByte(), 0x01.toByte(), 0x02.toByte(), 0x03.toByte(), 0x04.toByte(), 0x05.toByte(), 0x06.toByte(), 0x07.toByte(), 0x08.toByte(), 0x09.toByte(), 0x0a.toByte(), 0x0b.toByte(), 0x0c.toByte(), 0x0d.toByte(), 0x0e.toByte(), 0x0f.toByte())

        //this is how long the xy4 will broadcast ads with power level 8 when a button is pressed once
        private const val BUTTON_ADVERTISEMENT_LENGTH = 30 * 1000

        enum class StayAwake(val state: Int) {
            Off(0),
            On(1)
        }

        enum class ButtonPress(val state:Int) {
            None(0),
            Single(1),
            Double(2),
            Long(3)
        }

        fun buttonPressFromInt(index: Int) : ButtonPress {
            return when(index) {
                1 -> ButtonPress.Single
                2 -> ButtonPress.Double
                3 -> ButtonPress.Long
                else -> {ButtonPress.None}
            }
        }

        fun enable(enable: Boolean) {
            if (enable) {
                XYFinderBluetoothDevice.enable(true)
                XYFinderBluetoothDevice.addCreator(FAMILY_UUID, this.creator)
            } else {
                XYFinderBluetoothDevice.removeCreator(FAMILY_UUID)
            }
        }

        fun addListener(key: String, listener: XY4BluetoothDevice.Listener) {
            launch(CommonPool){
                synchronized(listeners) {
                    listeners.put(key, listener)
                }
            }
        }

        fun removeListener(key: String) {
            launch(CommonPool){
                synchronized(listeners) {
                    listeners.remove(key)
                }
            }
        }

        fun reportButtonPressed(device: XY4BluetoothDevice, state: ButtonPress) {
            logInfo("reportButtonPressed (Global)")
            launch (CommonPool) {
                synchronized(listeners) {
                    for (listener in listeners) {
                        val xyFinderListener = listener.value as? XYFinderBluetoothDevice.Listener
                        if (xyFinderListener != null) {
                            logInfo("reportButtonPressed: $xyFinderListener")
                            launch(CommonPool) {
                                when (state) {
                                    ButtonPress.Single -> xyFinderListener.buttonSinglePressed(device)
                                    ButtonPress.Double -> xyFinderListener.buttonDoublePressed(device)
                                    ButtonPress.Long -> xyFinderListener.buttonLongPressed(device)
                                    else -> {
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        internal val creator = object : XYCreator() {
            override fun getDevicesFromScanResult(context: Context, scanResult: XYScanResult, globalDevices: HashMap<Int, XYBluetoothDevice>, foundDevices: HashMap<Int, XYBluetoothDevice>) {
                val hash = hashFromScanResult(scanResult)
                if (hash != null) {
                    foundDevices[hash] = globalDevices[hash] ?: XY4BluetoothDevice(context, scanResult, hash)
                }
            }
        }

        private fun majorFromScanResult(scanResult: XYScanResult): Ushort? {
            val bytes = scanResult.scanRecord?.getManufacturerSpecificData(XYAppleBluetoothDevice.MANUFACTURER_ID)
            return if (bytes != null) {
                val buffer = ByteBuffer.wrap(bytes)
                Ushort(buffer.getShort(18).toInt())
            } else {
                null
            }
        }

        internal fun pressFromScanResult(scanResult: XYScanResult): Boolean {
            val bytes = scanResult.scanRecord?.getManufacturerSpecificData(XYAppleBluetoothDevice.MANUFACTURER_ID)
            return if (bytes != null) {
                val buffer = ByteBuffer.wrap(bytes)
                val minor = Ushort(buffer.getShort(20))
                val buttonBit = minor.and(0x0008)
                buttonBit == Ushort(0x0008)
            } else {
                false
            }
        }

        private fun minorFromScanResult(scanResult: XYScanResult): Ushort? {
            val bytes = scanResult.scanRecord?.getManufacturerSpecificData(XYAppleBluetoothDevice.MANUFACTURER_ID)
            return if (bytes != null) {
                val buffer = ByteBuffer.wrap(bytes)
                Ushort(buffer.getShort(20).toInt()).and(0xfff0).or(0x0004)
            } else {
                null
            }
        }

        internal fun hashFromScanResult(scanResult: XYScanResult): Int? {
            val uuid = XYIBeaconBluetoothDevice.iBeaconUuidFromScanResult(scanResult)
            val major = majorFromScanResult(scanResult)
            val minor = minorFromScanResult(scanResult)
            return "$uuid:$major:$minor".hashCode()
        }

    }
}
