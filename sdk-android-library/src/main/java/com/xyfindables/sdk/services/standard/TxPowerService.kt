package com.xyfindables.sdk.services.standard

import com.xyfindables.sdk.devices.XYBluetoothDevice
import com.xyfindables.sdk.services.Service
import java.util.*

class TxPowerService(device: XYBluetoothDevice) : Service(device) {

    override val serviceUuid: UUID
        get() {
            return GenericAccessService.uuid
        }

    val txPowerLevel = IntegerCharacteristic(this, characteristics.TxPowerLevel.uuid)

    companion object {
        val uuid = UUID.fromString("00001800-0000-1000-8000-00805F9B34FB")

        enum class characteristics(val uuid: UUID) {
            TxPowerLevel(UUID.fromString("00002a07-0000-1000-8000-00805f9b34fb"))
        }
    }
}