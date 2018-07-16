package com.xyfindables.sdk.services.xy3

import com.xyfindables.sdk.devices.XYBluetoothDevice
import com.xyfindables.sdk.services.Service
import java.util.*

class ExtendedConfigService(device: XYBluetoothDevice) : Service(device) {

    override val serviceUuid: UUID
        get() {
            return ExtendedConfigService.uuid
        }

    companion object {
        val uuid = UUID.fromString("F014FF00-0439-3000-E001-00001001FFFF")

        enum class characteristics(val uuid: UUID) {
            VirtualBeaconSettings(UUID.fromString("F014FF02-0439-3000-E001-00001001FFFF")),
            Tone(UUID.fromString("F014FF03-0439-3000-E001-00001001FFFF")),
            Registration(UUID.fromString("F014FF05-0439-3000-E001-00001001FFFF")),
            InactiveVirtualBeaconSettings(UUID.fromString("F014FF06-0439-3000-E001-00001001FFFF")),
            InactiveInterval(UUID.fromString("F014FF07-0439-3000-E001-00001001FFFF")),
            GpsInterval(UUID.fromString("2ABBAA00-0439-3000-E001-00001001FFFF")),
            GpsMode(UUID.fromString("2A99AA00-0439-3000-E001-00001001FFFF")),
            SimId(UUID.fromString("2ACCAA00-0439-3000-E001-00001001FFFF"))
        }
    }
}