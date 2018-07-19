package com.xyfindables.sdk.devices

import android.content.Context
import com.xyfindables.sdk.gatt.XYBluetoothError
import com.xyfindables.sdk.gatt.XYBluetoothResult
import com.xyfindables.sdk.gatt.asyncBle
import com.xyfindables.sdk.scanner.XYScanResult
import kotlinx.coroutines.experimental.Deferred
import java.nio.ByteBuffer
import java.util.*

open class XYFinderBluetoothDevice(context: Context, scanResult: XYScanResult, hash: Int) : XYIBeaconBluetoothDevice(context, scanResult, hash) {

    enum class Family {
        Unknown,
        XY1,
        XY2,
        XY3,
        Mobile,
        Gps,
        Near,
        XY4,
        Webble
    }

    override val id : String
        get() {
            return "$prefix:$uuid:$major.${minor.and(0xfff0).or(0x0004)}"
        }

    val family: Family
        get () {
            return familyFromUuid(uuid)
        }

    val prefix: String
        get () {
            return prefixFromFamily(family)
        }

    //signal the user to where it is, usually make it beep
    open fun find() : Deferred<XYBluetoothResult<Int>> {
        logException(UnsupportedOperationException(), true)
        return asyncBle {
            return@asyncBle XYBluetoothResult<Int>(XYBluetoothError("Not Implemented"))
        }
    }

    open fun lock() : Deferred<XYBluetoothResult<ByteArray>> {
        logException(UnsupportedOperationException(), true)
        return asyncBle {
            return@asyncBle XYBluetoothResult<ByteArray>(XYBluetoothError("Not Implemented"))
        }
    }

    open fun unlock() : Deferred<XYBluetoothResult<ByteArray>> {
        logException(UnsupportedOperationException(), true)
        return asyncBle {
            return@asyncBle XYBluetoothResult<ByteArray>(XYBluetoothError("Not Implemented"))
        }
    }

    open fun stayAwake() : Deferred<XYBluetoothResult<Int>> {
        logException(UnsupportedOperationException(), true)
        return asyncBle {
            return@asyncBle XYBluetoothResult<Int>(XYBluetoothError("Not Implemented"))
        }
    }

    open fun fallAsleep() : Deferred<XYBluetoothResult<Int>> {
        logException(UnsupportedOperationException(), true)
        return asyncBle {
            return@asyncBle XYBluetoothResult<Int>(XYBluetoothError("Not Implemented"))
        }
    }

    open fun restart() : Deferred<XYBluetoothResult<Int>> {
        logException(UnsupportedOperationException(), true)
        return asyncBle {
            return@asyncBle XYBluetoothResult<Int>(XYBluetoothError("Not Implemented"))
        }
    }

    open fun batteryLevel() : Deferred<XYBluetoothResult<Int>> {
        logException(UnsupportedOperationException(), true)
        return asyncBle {
            return@asyncBle XYBluetoothResult<Int>(XYBluetoothError("Not Implemented"))
        }
    }

    open val distance : Float
        get() {
            return 0.0f
        }

    open class Listener : XYIBeaconBluetoothDevice.Listener() {
    }

    companion object : XYCreator() {

        var canCreate = false

        val uuid2family: HashMap<UUID, Family>

        val family2uuid: HashMap<Family, UUID>

        val family2prefix: HashMap<Family, String>

        init {
            uuid2family = HashMap()
            uuid2family[UUID.fromString("a500248c-abc2-4206-9bd7-034f4fc9ed10")] = Family.XY1
            uuid2family[UUID.fromString("07775dd0-111b-11e4-9191-0800200c9a66")] = Family.XY2
            uuid2family[UUID.fromString("08885dd0-111b-11e4-9191-0800200c9a66")] = Family.XY3
            uuid2family[UUID.fromString("a44eacf4-0104-0000-0000-5f784c9977b5")] = Family.XY4
            uuid2family[UUID.fromString("735344c9-e820-42ec-9da7-f43a2b6802b9")] = Family.Mobile
            uuid2family[UUID.fromString("9474f7c6-47a4-11e6-beb8-9e71128cae77")] = Family.Gps
            uuid2family[UUID.fromString("eab24c98-8117-4f69-ba1b-45f4e1875858")] = Family.Webble

            family2uuid = HashMap()
            family2uuid[Family.XY1] = UUID.fromString("a500248c-abc2-4206-9bd7-034f4fc9ed10")
            family2uuid[Family.XY2] = UUID.fromString("07775dd0-111b-11e4-9191-0800200c9a66")
            family2uuid[Family.XY3] = UUID.fromString("08885dd0-111b-11e4-9191-0800200c9a66")
            family2uuid[Family.XY4] = UUID.fromString("a44eacf4-0104-0000-0000-5f784c9977b5")
            family2uuid[Family.Mobile] = UUID.fromString("735344c9-e820-42ec-9da7-f43a2b6802b9")
            family2uuid[Family.Gps] = UUID.fromString("9474f7c6-47a4-11e6-beb8-9e71128cae77")
            family2uuid[Family.Webble] = UUID.fromString("eab24c98-8117-4f69-ba1b-45f4e1875858")

            family2prefix = HashMap()
            family2prefix[Family.XY1] = "ibeacon"
            family2prefix[Family.XY2] = "ibeacon"
            family2prefix[Family.XY3] = "ibeacon"
            family2prefix[Family.XY4] = "ibeacon"
            family2prefix[Family.Mobile] = "mobiledevice"
            family2prefix[Family.Gps] = "gps"
            family2prefix[Family.Webble] = "webble"
        }

        fun familyFromUuid(uuid: UUID?): Family {
            return uuid2family[uuid] ?: return Family.Unknown
        }

        fun uuidFromFamily(family: Family): UUID? {
            return family2uuid[family]
        }

        fun prefixFromFamily(family: Family): String {
            return family2prefix[family] ?: return "unknown"
        }

        fun enable(enable: Boolean) {
            if (enable) {
                XYIBeaconBluetoothDevice.enable(true)
            }
        }

        fun addCreator(uuid: UUID, creator: XYCreator) {
            XYIBeaconBluetoothDevice.uuidToCreator[uuid] = this
            uuidToCreator[uuid] = creator
        }

        fun removeCreator(uuid: UUID) {
            uuidToCreator.remove(uuid)
        }

        private val uuidToCreator = HashMap<UUID, XYCreator>()

        override fun getDevicesFromScanResult(context:Context, scanResult: XYScanResult, globalDevices: HashMap<Int, XYBluetoothDevice>, foundDevices: HashMap<Int, XYBluetoothDevice>) {

            val bytes = scanResult.scanRecord?.getManufacturerSpecificData(XYAppleBluetoothDevice.MANUFACTURER_ID)
            if (bytes != null) {
                val buffer = ByteBuffer.wrap(bytes)
                buffer.position(2) //skip the type and size

                // get uuid
                val high = buffer.getLong()
                val low = buffer.getLong()
                val uuidFromScan = UUID(high, low)

                for ((uuid, creator) in uuidToCreator) {
                    if (uuid.equals(uuidFromScan)) {
                        creator.getDevicesFromScanResult(context, scanResult, globalDevices, foundDevices)
                        return
                    }
                }
            }

            val hash = hashFromScanResult(scanResult)

            if (canCreate && hash != null) {
                foundDevices[hash] = globalDevices[hash] ?: XYFinderBluetoothDevice(context, scanResult, hash)
            }
        }

        fun hashFromScanResult(scanResult: XYScanResult): Int? {
            return XYIBeaconBluetoothDevice.hashFromScanResult(scanResult)
        }
    }
}