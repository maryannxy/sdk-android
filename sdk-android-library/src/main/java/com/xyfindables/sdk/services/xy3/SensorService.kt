package com.xyfindables.sdk.services.xy3

import com.xyfindables.sdk.devices.XYBluetoothDevice
import com.xyfindables.sdk.services.Service
import java.util.*

class SendorService(device: XYBluetoothDevice) : Service(device) {

    override val serviceUuid: UUID
        get() {
            return SendorService.uuid
        }

    companion object {
        val uuid = UUID.fromString("F014DD00-0439-3000-E001-00001001FFFF")

        enum class characteristics(val uuid: UUID) {
            Raw(UUID.fromString("F014DD01-0439-3000-E001-00001001FFFF")),
            Timeout(UUID.fromString("F014DD02-0439-3000-E001-00001001FFFF")),
            Threshold(UUID.fromString("F014DD03-0439-3000-E001-00001001FFFF")),
            Inactive(UUID.fromString("F014DD04-0439-3000-E001-00001001FFFF")),
            MovementCount(UUID.fromString("F014DD05-0439-3000-E001-00001001FFFF"))
        }
    }
}