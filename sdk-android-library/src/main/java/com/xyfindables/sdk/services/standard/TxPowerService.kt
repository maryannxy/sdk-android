package com.xyfindables.sdk.services.standard

import com.xyfindables.sdk.devices.XYBluetoothDevice
import com.xyfindables.sdk.services.Service
import java.util.*

class TxPowerService(device: XYBluetoothDevice) : Service(device) {

    override val serviceUuid: UUID
        get() {
            return TxPowerService.uuid
        }

    companion object {
        val uuid = UUID.fromString("a44eacf4-0104-0001-0000-5f784c9977b5")

        enum class characteristics(val uuid: UUID) {
            DeviceName(                                 UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb")),
            Appearance(                                 UUID.fromString("00002a01-0000-1000-8000-00805f9b34fb")),
            PrivacyFlag(                                UUID.fromString("00002a02-0000-1000-8000-00805f9b34fb")),
            ReconnectionAddress(                        UUID.fromString("00002a03-0000-1000-8000-00805f9b34fb")),
            PeripheralPreferredConnectionParameters(    UUID.fromString("00002a04-0000-1000-8000-00805f9b34fb"))

        }
    }
}