package com.xyfindables.sdk.services.xy3

import com.xyfindables.sdk.devices.XYBluetoothDevice
import com.xyfindables.sdk.services.Service
import java.util.*

class CsrOtaService(device: XYBluetoothDevice) : Service(device) {

    override val serviceUuid: UUID
        get() {
            return CsrOtaService.uuid
        }

    companion object {
        val uuid = UUID.fromString("00001016-D102-11E1-9B23-00025B00A5A5")

        enum class characteristics(val uuid: UUID) {
        }
    }
}