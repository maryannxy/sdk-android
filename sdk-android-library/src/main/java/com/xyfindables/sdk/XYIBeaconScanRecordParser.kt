package com.xyfindables.sdk

import com.xyfindables.sdk.scanner.XYScanRecord
import com.xyfindables.sdk.scanner.XYScanRecordParser
import java.nio.ByteBuffer
import java.util.*

class XYIBeaconScanRecordParser(scanRecord: XYScanRecord) : XYScanRecordParser(scanRecord) {

    val APPLE_CORPORATION_ID = 0x004c
    val APPLE_IBEACON_ID = 0x02
    val APPLE_IBEACON_LEN = 0x15

    override fun isValid() : Boolean {
        val manufacturerData = scanRecord.getManufacturerSpecificData(APPLE_CORPORATION_ID)
        if (manufacturerData != null) {
            if (manufacturerData[0].toInt() == APPLE_IBEACON_ID && manufacturerData[1].toInt() == APPLE_IBEACON_LEN) {
                return true
            }
        }
        return false
    }

    val proximityId : UUID
        get() {
            val buffer = ByteBuffer.wrap(scanRecord.bytes)
            buffer.position(4) //skip the header
            val high = buffer.getLong()
            val low = buffer.getLong()

            return UUID(high, low)
        }

    val major : Int
        get() {
            return scanRecord.bytes[20] * 0x100 + scanRecord.bytes[21]
        }

    val minor : Int
        get() {
            return scanRecord.bytes[22] * 0x100 + scanRecord.bytes[23]
        }

    val power : Short
        get() {
            return scanRecord.bytes[24].toShort()
        }
}