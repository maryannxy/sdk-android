package com.xyfindables.sdk.services.xy3

import com.xyfindables.sdk.devices.XYBluetoothDevice
import com.xyfindables.sdk.services.Service
import java.util.*

class ControlService(device: XYBluetoothDevice) : Service(device) {

    override val serviceUuid: UUID
        get() {
            return ControlService.uuid
        }

    companion object {
        val uuid = UUID.fromString("F014ED15-0439-3000-E001-00001001FFFF")

        enum class characteristics(val uuid: UUID) {
            Buzzer(UUID.fromString("F014FFF1-0439-3000-E001-00001001FFFF")),
            Handshake(UUID.fromString("F014FFF2-0439-3000-E001-00001001FFFF")),
            Version(UUID.fromString("F014FFF4-0439-3000-E001-00001001FFFF")),
            BuzzerSelect(UUID.fromString("F014FFF6-0439-3000-E001-00001001FFFF")),
            Surge(UUID.fromString("F014FFF7-0439-3000-E001-00001001FFFF")),
            Button(UUID.fromString("F014FFF8-0439-3000-E001-00001001FFFF")),
            Disconnect(UUID.fromString("F014FFF9-0439-3000-E001-00001001FFFF"))
        }
    }
}