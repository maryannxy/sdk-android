package com.xyfindables.sdk.services.xy3

import com.xyfindables.sdk.devices.XYBluetoothDevice
import com.xyfindables.sdk.services.Service
import java.util.*

class ExtendedControlService(device: XYBluetoothDevice) : Service(device) {

    override val serviceUuid: UUID
        get() {
            return ExtendedControlService.uuid
        }

    companion object {
        val uuid = UUID.fromString("F014AA00-0439-3000-E001-00001001FFFF")

        enum class characteristics(val uuid: UUID) {
            ExtendedControlSIMStatus(UUID.fromString("2ADDAA00-0439-3000-E001-00001001FFFF")),
            ExtendedControlLED(UUID.fromString("2AAAAA00-0439-3000-E001-00001001FFFF")),
            ExtendedControlSelfTest(UUID.fromString("2a77AA00-0439-3000-E001-00001001FFFF"))
        }
    }
}