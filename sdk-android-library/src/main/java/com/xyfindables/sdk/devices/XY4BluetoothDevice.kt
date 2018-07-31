package com.xyfindables.sdk.devices

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
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

open class XY4BluetoothDevice(context: Context, scanResult: XYScanResult, hash:Int) : XYFinderBluetoothDevice(context, scanResult, hash) {

    val alertNotification = AlertNotificationService(this)
    val batteryService = BatteryService(this)
    val currentTimeService = CurrentTimeService(this)
    val deviceInformationService = DeviceInformationService(this)
    val genericAccess = GenericAccessService(this)
    val genericAccessService = GenericAttributeService(this)
    val linkLossService = LinkLossService(this)
    val txPowerService = TxPowerService(this)
    val eddystoneService = EddystoneService(this)
    val eddystoneConfigService = EddystoneConfigService(this)

    val primary = PrimaryService(this)

    val buttonListener = object: XYBluetoothGattCallback() {
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            logInfo("onCharacteristicChanged")
            super.onCharacteristicChanged(gatt, characteristic)
            if (characteristic?.uuid == primary.buttonState.uuid) {
                reportButtonPressed(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0))
            }
        }
    }

    init {
        addGattListener("xy4", buttonListener)
    }

    override fun find() : Deferred<XYBluetoothResult<Int>> {
        logInfo("find")
        return primary.buzzer.set(11)
    }

    override fun lock() : Deferred<XYBluetoothResult<ByteArray>> {
        logInfo("lock")
        return primary.lock.set(XY4BluetoothDevice.DEFAULT_LOCK_CODE)
    }

    override fun unlock() : Deferred<XYBluetoothResult<ByteArray>> {
        logInfo("unlock")
        return primary.unlock.set(XY4BluetoothDevice.DEFAULT_LOCK_CODE)
    }

    override fun stayAwake() : Deferred<XYBluetoothResult<Int>> {
        logInfo("stayAwake")
        return primary.stayAwake.set(1)
    }

    override fun fallAsleep() : Deferred<XYBluetoothResult<Int>> {
        logInfo("fallAsleep")
        return primary.stayAwake.set(0)
    }

    fun reportButtonPressed(state: Int) {
        logInfo("reportButtonPressed")
        synchronized(listeners) {
            for (listener in listeners) {
                val xy4Listener = listener.value as? XY4BluetoothDevice.Listener
                if (xy4Listener != null) {
                    launch(CommonPool) {
                        xy4Listener.pressed(this@XY4BluetoothDevice)
                        when (state) {
                            1 -> xy4Listener.buttonSinglePressed(this@XY4BluetoothDevice)
                            2 -> xy4Listener.buttonDoublePressed(this@XY4BluetoothDevice)
                            3 -> xy4Listener.buttonLongPressed(this@XY4BluetoothDevice)
                        }
                        //everytime a notify fires, we have to re-enable it
                        primary.buttonState.enableNotify(true)
                    }
                }
            }
        }
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

    companion object : XYCreator() {

        val FAMILY_UUID = UUID.fromString("a44eacf4-0104-0000-0000-5f784c9977b5")

        val DEFAULT_LOCK_CODE = byteArrayOf(0x00.toByte(), 0x01.toByte(), 0x02.toByte(), 0x03.toByte(), 0x04.toByte(), 0x05.toByte(), 0x06.toByte(), 0x07.toByte(), 0x08.toByte(), 0x09.toByte(), 0x0a.toByte(), 0x0b.toByte(), 0x0c.toByte(), 0x0d.toByte(), 0x0e.toByte(), 0x0f.toByte())
        val DEFAULT_LOCK_CODE_XY3 = byteArrayOf(0x2f.toByte(), 0xbe.toByte(), 0xa2.toByte(), 0x07.toByte(), 0x52.toByte(), 0xfe.toByte(), 0xbf.toByte(), 0x31.toByte(), 0x1d.toByte(), 0xac.toByte(), 0x5d.toByte(), 0xfa.toByte(), 0x7d.toByte(), 0x77.toByte(), 0x76.toByte(), 0x80.toByte())

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
                XYFinderBluetoothDevice.addCreator(FAMILY_UUID, this)
            } else {
                XYFinderBluetoothDevice.removeCreator(FAMILY_UUID)
            }
        }

        override fun getDevicesFromScanResult(context:Context, scanResult: XYScanResult, globalDevices: HashMap<Int, XYBluetoothDevice>, foundDevices: HashMap<Int, XYBluetoothDevice>) {
            val hash = hashFromScanResult(scanResult)
            if (hash != null) {
                foundDevices[hash] = globalDevices[hash] ?: XY4BluetoothDevice(context, scanResult, hash)
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

        fun hashFromScanResult(scanResult: XYScanResult): Int? {
            val uuid = XYIBeaconBluetoothDevice.iBeaconUuidFromScanResult(scanResult)
            val major = majorFromScanResult(scanResult)
            val minor = minorFromScanResult(scanResult)
            return "$uuid:$major:$minor".hashCode()
        }

    }
}