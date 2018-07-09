package com.xyfindables.sdk

import android.bluetooth.BluetoothDevice
import android.content.Context
import java.util.*

class XYFinderBluetoothDevice(context: Context, device: BluetoothDevice) : XYIBeaconBluetoothDevice(context, device) {

    enum class Family {
        Unknown,
        XY1,
        XY2,
        XY3,
        Mobile,
        Gps,
        Near,
        XY4
    }

    val family: Family
        get () {
            return familyFromUuid(uuid)
        }
    companion object {

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

            family2uuid = HashMap()
            family2uuid[Family.XY1] = UUID.fromString("a500248c-abc2-4206-9bd7-034f4fc9ed10")
            family2uuid[Family.XY2] = UUID.fromString("07775dd0-111b-11e4-9191-0800200c9a66")
            family2uuid[Family.XY3] = UUID.fromString("08885dd0-111b-11e4-9191-0800200c9a66")
            family2uuid[Family.XY4] = UUID.fromString("a44eacf4-0104-0000-0000-5f784c9977b5")
            family2uuid[Family.Mobile] = UUID.fromString("735344c9-e820-42ec-9da7-f43a2b6802b9")
            family2uuid[Family.Gps] = UUID.fromString("9474f7c6-47a4-11e6-beb8-9e71128cae77")

            family2prefix = HashMap()
            family2prefix[Family.XY1] = "ibeacon"
            family2prefix[Family.XY2] = "ibeacon"
            family2prefix[Family.XY3] = "ibeacon"
            family2prefix[Family.XY4] = "ibeacon"
            family2prefix[Family.Mobile] = "mobiledevice"
            family2prefix[Family.Gps] = "gps"
            family2prefix[Family.Near] = "near"
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
    }
}