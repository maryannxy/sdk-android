package com.xyfindables.sdk.devices

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import com.xyfindables.core.XYBase
import com.xyfindables.sdk.gatt.XYBluetoothResult
import com.xyfindables.sdk.scanner.XYScanResult
import com.xyfindables.sdk.services.standard.*
import com.xyfindables.sdk.services.xy3.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.launch
import unsigned.Ushort
import java.nio.ByteBuffer
import java.util.*

open class XY3BluetoothDevice(context: Context, scanResult: XYScanResult, hash: Int) : XYFinderBluetoothDevice(context, scanResult, hash) {

    val alertNotification = AlertNotificationService(this)
    val batteryService = BatteryService(this)
    val currentTimeService = CurrentTimeService(this)
    val deviceInformationService = DeviceInformationService(this)
    val genericAccessService = GenericAccessService(this)
    val genericAttributeService = GenericAttributeService(this)
    val linkLossService = LinkLossService(this)
    val txPowerService = TxPowerService(this)

    val basicConfigService = BasicConfigService(this)
    val controlService = ControlService(this)
    val csrOtaService = CsrOtaService(this)
    val extendedConfigService = ExtendedConfigService(this)
    val extendedControlService = ExtendedControlService(this)
    val sensorService = SensorService(this)

    internal val buttonListener = object: XYBluetoothGattCallback() {
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            logInfo("onCharacteristicChanged")
            super.onCharacteristicChanged(gatt, characteristic)
            if (characteristic?.uuid == controlService.button.uuid) {
                reportButtonPressed(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0))
            }
        }
    }

    init {
        addGattListener("xy3", buttonListener)
    }

    override val minor : Ushort
        get() {
            //we have to mask the low nibble for the power level
            return _minor.and(0xfff0).or(0x0004)
        }

    override val prefix = "ibeacon"

    override fun find() : Deferred<XYBluetoothResult<Int>> {
        logInfo("find")
        return controlService.buzzerSelect.set(3)
    }

    override fun lock() : Deferred<XYBluetoothResult<ByteArray>> {
        logInfo("lock")
        return basicConfigService.lock.set(XY3BluetoothDevice.DEFAULT_LOCK_CODE)
    }

    override fun unlock() : Deferred<XYBluetoothResult<ByteArray>> {
        logInfo("unlock")
        return basicConfigService.unlock.set(XY3BluetoothDevice.DEFAULT_LOCK_CODE)
    }

    override fun stayAwake() : Deferred<XYBluetoothResult<Int>> {
        logInfo("stayAwake")
        return extendedConfigService.registration.set(1)
    }

    override fun fallAsleep() : Deferred<XYBluetoothResult<Int>> {
        logInfo("fallAsleep")
        return extendedConfigService.registration.set(0)
    }

    fun reportButtonPressed(state: Int) {
        logInfo("reportButtonPressed")
        synchronized(listeners) {
            for (listener in listeners) {
                val xy3Listener = listener.value as? XY3BluetoothDevice.Listener
                if (xy3Listener != null) {
                    launch(CommonPool) {
                        when (state) {
                            1 -> xy3Listener.buttonSinglePressed()
                            2 -> xy3Listener.buttonDoublePressed()
                            3 -> xy3Listener.buttonLongPressed()
                        }
                        //everytime a notify fires, we have to re-enable it
                        controlService.button.enableNotify(true)
                    }
                }
            }
        }
    }

    open class Listener : XYFinderBluetoothDevice.Listener() {
        open fun buttonSinglePressed() {}
        open fun buttonDoublePressed() {}
        open fun buttonLongPressed() {}
    }

    companion object : XYBase() {

        private val FAMILY_UUID = UUID.fromString("08885dd0-111b-11e4-9191-0800200c9a66")!!

        private val DEFAULT_LOCK_CODE = byteArrayOf(0x2f.toByte(), 0xbe.toByte(), 0xa2.toByte(), 0x07.toByte(), 0x52.toByte(), 0xfe.toByte(), 0xbf.toByte(), 0x31.toByte(), 0x1d.toByte(), 0xac.toByte(), 0x5d.toByte(), 0xfa.toByte(), 0x7d.toByte(), 0x77.toByte(), 0x76.toByte(), 0x80.toByte())

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

        fun enable(enable: Boolean) {
            if (enable) {
                XYFinderBluetoothDevice.enable(true)
                XYFinderBluetoothDevice.addCreator(FAMILY_UUID, this.creator)
            } else {
                XYFinderBluetoothDevice.removeCreator(FAMILY_UUID)
            }
        }

        fun majorFromScanResult(scanResult: XYScanResult): Int? {
            val bytes = scanResult.scanRecord?.getManufacturerSpecificData(XYAppleBluetoothDevice.MANUFACTURER_ID)
            if (bytes != null) {
                val buffer = ByteBuffer.wrap(bytes)
                return buffer.getShort(18).toInt()
            } else {
                return null
            }
        }

        fun minorFromScanResult(scanResult: XYScanResult): Int? {
            val bytes = scanResult.scanRecord?.getManufacturerSpecificData(XYAppleBluetoothDevice.MANUFACTURER_ID)
            if (bytes != null) {
                val buffer = ByteBuffer.wrap(bytes)
                return buffer.getShort(20).toInt().and(0xfff0).or(0x0004)
            } else {
                return null
            }
        }

        internal val creator = object : XYCreator() {
            override fun getDevicesFromScanResult(context: Context, scanResult: XYScanResult, globalDevices: HashMap<Int, XYBluetoothDevice>, foundDevices: HashMap<Int, XYBluetoothDevice>) {
                val hash = hashFromScanResult(scanResult)
                if (hash != null) {
                    foundDevices[hash] = globalDevices[hash] ?: XY3BluetoothDevice(context, scanResult, hash)
                }
            }
        }

        fun hashFromScanResult(scanResult: XYScanResult): Int? {
            val uuid = XYIBeaconBluetoothDevice.iBeaconUuidFromScanResult(scanResult)
            val major = majorFromScanResult(scanResult)
            val minor = minorFromScanResult(scanResult)
            return "$uuid:$major:$minor".hashCode()
        }

    }
}