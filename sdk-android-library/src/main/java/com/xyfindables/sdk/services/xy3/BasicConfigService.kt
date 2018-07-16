package com.xyfindables.sdk.services.xy3

import com.xyfindables.sdk.devices.XYBluetoothDevice
import com.xyfindables.sdk.services.Service
import java.util.*

class BasicConfigService(device: XYBluetoothDevice) : Service(device) {

    override val serviceUuid: UUID
        get() {
            return BasicConfigService.uuid
        }

    companion object {
        val uuid = UUID.fromString("F014EE00-0439-3000-E001-00001001FFFF")

        enum class characteristics(val uuid: UUID) {
            LockStatus(UUID.fromString("F014EE01-0439-3000-E001-00001001FFFF")),
            Lock(UUID.fromString("F014EE02-0439-3000-E001-00001001FFFF")),
            Unlock(UUID.fromString("F014EE03-0439-3000-E001-00001001FFFF")),
            Uuid(UUID.fromString("F014EE04-0439-3000-E001-00001001FFFF")),
            Major(UUID.fromString("F014EE05-0439-3000-E001-00001001FFFF")),
            Minor(UUID.fromString("F014EE06-0439-3000-E001-00001001FFFF")),
            Interval(UUID.fromString("F014EE07-0439-3000-E001-00001001FFFF")),
            OtaWrite(UUID.fromString("F014EE09-0439-3000-E001-00001001FFFF")),
            Reboot(UUID.fromString("F014EE0A-0439-3000-E001-00001001FFFF"))
        }
    }
}