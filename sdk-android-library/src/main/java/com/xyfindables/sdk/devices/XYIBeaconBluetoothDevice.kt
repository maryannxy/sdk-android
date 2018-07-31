package com.xyfindables.sdk.devices

import android.content.Context
import com.xyfindables.sdk.scanner.XYScanResult
import unsigned.Ubyte
import unsigned.Ushort
import java.nio.ByteBuffer
import java.util.*

open class XYIBeaconBluetoothDevice(context: Context, scanResult: XYScanResult?, hash: Int) : XYBluetoothDevice(context, scanResult?.device, hash) {

    protected val _uuid : UUID
    open val uuid : UUID
        get() {
            return _uuid
        }

    protected var _major : Ushort
    open val major : Ushort
        get() {
            return _major
        }

    protected var _minor : Ushort
    open val minor : Ushort
        get() {
            return _minor
        }

    protected val _power : Ubyte
    open val power : Ubyte
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

            _major = Ushort(buffer.getShort())
            _minor = Ushort(buffer.getShort())
            _power = Ubyte(buffer.get())
        } else {
            _uuid = UUID(0,0)
            _major = Ushort(0)
            _minor = Ushort(0)
            _power = Ubyte(0)
        }
    }

    open class Listener : XYAppleBluetoothDevice.Listener() {
        open fun onIBeaconDetect(uuid: String, major: Ushort, minor: Ushort) {

        }
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