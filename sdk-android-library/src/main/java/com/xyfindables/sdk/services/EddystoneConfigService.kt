package com.xyfindables.sdk.services

import com.xyfindables.sdk.devices.XYBluetoothDevice
import java.util.*

class EddystoneConfigService(device: XYBluetoothDevice) : Service(device) {

    override val serviceUuid: UUID
        get() {
            return EddystoneConfigService.uuid
        }

    companion object {
        val uuid = UUID.fromString("ee0c2080-8786-40ba-ab96-99b91ac981d8")

        enum class characteristics(val uuid: UUID) {
        }
    }
}