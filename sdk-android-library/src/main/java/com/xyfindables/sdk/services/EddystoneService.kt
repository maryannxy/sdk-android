package com.xyfindables.sdk.services

import com.xyfindables.sdk.devices.XYBluetoothDevice
import java.util.*

class EddystoneService(device: XYBluetoothDevice) : Service(device) {

    override val serviceUuid: UUID
        get() {
            return EddystoneService.uuid
        }

    companion object {
        val uuid = UUID.fromString("0000feaa-0000-1000-8000-00805F9B34FB")

        enum class characteristics(val uuid: UUID) {
        }
    }
}