package com.xyfindables.sdk.devices

import android.content.Context
import com.xyfindables.sdk.scanner.XYScanResult
import java.nio.ByteBuffer
import java.util.*

open class XYIBeaconBluetoothDevice(context: Context, scanResult: XYScanResult) : XYBluetoothDevice(context, scanResult.device) {

    private val _uuid : UUID
    val uuid : UUID
        get() {
            return _uuid
        }

    private val _major : Int
    val major : Int
        get() {
            return _major
        }

    private val _minor : Int
    val minor : Int
        get() {
            return _minor
        }

    private val _power : Byte
    val power : Byte
        get() {
            return _power
        }

    override val id : String
        get() {
            return "$uuid:$major.$minor"
        }

    init {
        val bytes = scanResult.scanRecord!!.getManufacturerSpecificData(XYAppleBluetoothDevice.MANUFACTURER_ID)!!
        val buffer = ByteBuffer.wrap(bytes)
        buffer.position(2) //skip the type and size

        //get uuid
        val high = buffer.getLong()
        val low = buffer.getLong()
        _uuid = UUID(high, low)

        _major = buffer.getShort().toInt() and 0xffff
        _minor = buffer.getShort().toInt() and 0xffff
        _power = buffer.get()
    }

    interface Listener : XYAppleBluetoothDevice.Listener {
    }

    companion object {

        val APPLE_IBEACON_ID = 0x02.toByte()

        var canCreate = false

        fun enable(enable: Boolean) {
            if (enable) {
                XYAppleBluetoothDevice.enable(true)
                XYAppleBluetoothDevice.typeToCreator[APPLE_IBEACON_ID] = {
                    context: Context,
                    scanResult: XYScanResult
                    ->
                    fromScanResult(context, scanResult)
                }
            } else {
                XYAppleBluetoothDevice.typeToCreator.remove(APPLE_IBEACON_ID)
            }
        }

        val uuidToCreator = HashMap<UUID, (context:Context, scanResult: XYScanResult) -> XYIBeaconBluetoothDevice?>()
        fun fromScanResult(context:Context, scanResult: XYScanResult) : XYBluetoothDevice? {
            for ((uuid, creator) in uuidToCreator) {
                val bytes = scanResult.scanRecord?.getManufacturerSpecificData(XYAppleBluetoothDevice.MANUFACTURER_ID)
                if (bytes != null) {
                    val buffer = ByteBuffer.wrap(bytes)
                    buffer.position(2) //skip the type and size

                    //get uuid
                    val high = buffer.getLong()
                    val low = buffer.getLong()
                    val uuidFromScan = UUID(high, low)
                    if (uuid.equals(uuidFromScan)) {
                        return creator(context, scanResult)
                    }
                }
            }
            if (canCreate)
                return XYIBeaconBluetoothDevice(context, scanResult)
            else
                return null
        }
    }
}