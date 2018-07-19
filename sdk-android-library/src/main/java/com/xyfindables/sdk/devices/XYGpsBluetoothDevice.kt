package com.xyfindables.sdk.devices

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import com.xyfindables.sdk.gatt.XYBluetoothResult
import com.xyfindables.sdk.scanner.XYScanResult
import com.xyfindables.sdk.services.standard.*
import com.xyfindables.sdk.services.xy3.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.launch
import java.nio.ByteBuffer
import java.util.*

open class XYGpsBluetoothDevice(context: Context, scanResult: XYScanResult, hash: Int) : XYFinderBluetoothDevice(context, scanResult, hash) {

    val alertNotification = AlertNotificationService(this)
    val battery = BatteryService(this)
    val currentTime = CurrentTimeService(this)
    val deviceInformation = DeviceInformationService(this)
    val genericAccess = GenericAccessService(this)
    val genericAttribute = GenericAttributeService(this)
    val linkLoss = LinkLossService(this)
    val txPower = TxPowerService(this)
    val basicConfig = BasicConfigService(this)
    val control = ControlService(this)
    val csrOta = CsrOtaService(this)
    val extendedConfig = ExtendedConfigService(this)
    val extendedControl = ExtendedControlService(this)
    val sensor = SensorService(this)

    init {
        addGattListener("xy3", object: BluetoothGattCallback() {
            override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
                logInfo("onCharacteristicChanged")
                super.onCharacteristicChanged(gatt, characteristic)
                if (characteristic?.uuid == control.button.uuid) {
                    reportButtonPressed(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0))
                }
            }
        })
    }

    override fun find() : Deferred<XYBluetoothResult<Int>> {
        logInfo("find")
        return control.buzzerSelect.set(1)
    }

    fun reportButtonPressed(state: Int) {
        logInfo("reportButtonPressed")
        synchronized(listeners) {
            for (listener in listeners) {
                val xy3Listener = listener as? XYGpsBluetoothDevice.Listener
                if (xy3Listener != null) {
                    launch(CommonPool) {
                        xy3Listener.buttonSinglePressed()
                    }
                }
            }
        }
    }

    open class Listener : XYFinderBluetoothDevice.Listener() {
        fun buttonSinglePressed() {}
    }

    companion object : XYCreator() {

        val FAMILY_UUID = UUID.fromString("9474f7c6-47a4-11e6-beb8-9e71128cae77")

        val DEFAULT_LOCK_CODE = byteArrayOf(0x2f.toByte(), 0xbe.toByte(), 0xa2.toByte(), 0x07.toByte(), 0x52.toByte(), 0xfe.toByte(), 0xbf.toByte(), 0x31.toByte(), 0x1d.toByte(), 0xac.toByte(), 0x5d.toByte(), 0xfa.toByte(), 0x7d.toByte(), 0x77.toByte(), 0x76.toByte(), 0x80.toByte())

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
                foundDevices[hash] = globalDevices[hash] ?: XYGpsBluetoothDevice(context, scanResult, hash)
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