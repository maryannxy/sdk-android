package com.xyfindables.sdk.devices

import android.content.Context
import com.xyfindables.core.XYBase
import com.xyfindables.sdk.scanner.XYScanResult
import java.nio.ByteBuffer
import java.util.*

open class XYIBeaconBluetoothDevice(context: Context, scanResult: XYScanResult?, hash: Int) : XYBluetoothDevice(context, scanResult?.device, hash) {

    protected val _uuid : UUID
    open val uuid : UUID
        get() {
            return _uuid
        }

    protected var _major : Int
    open val major : Int
        get() {
            return _major
        }

    protected var _minor : Int
    open val minor : Int
        get() {
            return _minor
        }

    protected val _power : Byte
    open val power : Byte
        get() {
            return _power
        }

    override val id : String
        get() {
            return "$uuid:$major.$minor"
        }

    init {
        val bytes = scanResult?.scanRecord?.getManufacturerSpecificData(XYAppleBluetoothDevice.MANUFACTURER_ID)
        if (bytes != null && bytes.size >= 23) {
            val buffer = ByteBuffer.wrap(bytes)
            buffer.position(2) //skip the type and size

            //get uuid
            val high = buffer.getLong()
            val low = buffer.getLong()
            _uuid = UUID(high, low)

            _major = buffer.getShort().toInt() and 0xffff
            _minor = buffer.getShort().toInt() and 0xffff
            _power = buffer.get()
        } else {
            _uuid = UUID(0,0)
            _major = 0
            _minor = 0
            _power = 0
        }
    }

    open class Listener : XYAppleBluetoothDevice.Listener() {
    }

    companion object : XYCreator() {

        val APPLE_IBEACON_ID = 0x02.toByte()

        var canCreate = false

        fun enable(enable: Boolean) {
            if (enable) {
                XYAppleBluetoothDevice.enable(true)
                XYAppleBluetoothDevice.typeToCreator[APPLE_IBEACON_ID] = this
            } else {
                XYAppleBluetoothDevice.typeToCreator.remove(APPLE_IBEACON_ID)
            }
        }

        fun iBeaconUuidFromScanResult(scanResult: XYScanResult) : UUID? {
            val bytes = scanResult.scanRecord?.getManufacturerSpecificData(XYAppleBluetoothDevice.MANUFACTURER_ID)
            if (bytes != null) {
                val buffer = ByteBuffer.wrap(bytes)
                buffer.position(2) //skip the type and size

                //get uuid
                val high = buffer.getLong()
                val low = buffer.getLong()
                return UUID(high, low)
            } else {
                return null
            }
        }

        val uuidToCreator = HashMap<UUID, XYCreator>()
        override fun getDevicesFromScanResult(context:Context, scanResult: XYScanResult, globalDevices: HashMap<Int, XYBluetoothDevice>, foundDevices: HashMap<Int, XYBluetoothDevice>) {
            for ((uuid, creator) in uuidToCreator) {
                val bytes = scanResult.scanRecord?.getManufacturerSpecificData(XYAppleBluetoothDevice.MANUFACTURER_ID)
                if (bytes != null) {
                    if (uuid.equals(iBeaconUuidFromScanResult(scanResult))) {
                        creator.getDevicesFromScanResult(context, scanResult, globalDevices, foundDevices)
                        return
                    }
                }
            }

            val hash = hashFromScanResult(scanResult)

            if (canCreate && hash != null) {
                foundDevices[hash] = globalDevices[hash] ?: XYIBeaconBluetoothDevice(context, scanResult, hash)
            }
        }

        fun hashFromScanResult(scanResult: XYScanResult): Int? {
            return scanResult.scanRecord?.getManufacturerSpecificData(XYAppleBluetoothDevice.MANUFACTURER_ID)?.contentHashCode()
        }
    }
}