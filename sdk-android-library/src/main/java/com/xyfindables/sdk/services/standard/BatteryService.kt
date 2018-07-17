package com.xyfindables.sdk.services.standard

import com.xyfindables.sdk.devices.XYBluetoothDevice
import com.xyfindables.sdk.services.Service
import java.util.*

class BatteryService(device: XYBluetoothDevice) : Service(device) {

    override val serviceUuid: UUID
        get() {
            return BatteryService.uuid
        }

    val level = IntegerCharacteristic(this, characteristics.Level.uuid)

    companion object {
        val uuid = UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB")

        enum class characteristics(val uuid: UUID) {
            Level(UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb"))
        }
    }
}