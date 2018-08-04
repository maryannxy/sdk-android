package com.xyfindables.sdk.devices

import android.content.Context
import com.xyfindables.core.XYBase
import com.xyfindables.sdk.gatt.XYBluetoothResult
import com.xyfindables.sdk.scanner.XYScanResult
import com.xyfindables.sdk.services.standard.*
import com.xyfindables.sdk.services.xy3.*
import kotlinx.coroutines.experimental.Deferred
import java.nio.ByteBuffer
import java.util.*

open class XY2BluetoothDevice(context: Context, scanResult: XYScanResult, hash: Int) : XYFinderBluetoothDevice(context, scanResult, hash) {

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

    override fun find() : Deferred<XYBluetoothResult<Int>> {
        logInfo("find")
        return control.buzzerSelect.set(1)
    }

    override val prefix = "ibeacon"

    open class Listener : XYFinderBluetoothDevice.Listener() {
    }

    companion object : XYBase() {

        private val FAMILY_UUID = UUID.fromString("07775dd0-111b-11e4-9191-0800200c9a66")!!

        private val DEFAULT_LOCK_CODE = byteArrayOf(0x2f.toByte(), 0xbe.toByte(), 0xa2.toByte(), 0x07.toByte(), 0x52.toByte(), 0xfe.toByte(), 0xbf.toByte(), 0x31.toByte(), 0x1d.toByte(), 0xac.toByte(), 0x5d.toByte(), 0xfa.toByte(), 0x7d.toByte(), 0x77.toByte(), 0x76.toByte(), 0x80.toByte())

        enum class StayAwake(val state: Int) {
            Off(0),
            On(1)
        }

        fun enable(enable: Boolean) {
            if (enable) {
                XYFinderBluetoothDevice.enable(true)
                XYFinderBluetoothDevice.addCreator(FAMILY_UUID, this.creator)
            } else {
                XYFinderBluetoothDevice.removeCreator(FAMILY_UUID)
            }
        }

        internal val creator = object : XYCreator() {
            override fun getDevicesFromScanResult(context: Context, scanResult: XYScanResult, globalDevices: HashMap<Int, XYBluetoothDevice>, foundDevices: HashMap<Int, XYBluetoothDevice>) {
                val hash = hashFromScanResult(scanResult)
                if (hash != null) {
                    foundDevices[hash] = globalDevices[hash] ?: XY2BluetoothDevice(context, scanResult, hash)
                }
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