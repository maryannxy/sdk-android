package com.xyfindables.sdk.services.standard

import com.xyfindables.sdk.devices.XYBluetoothDevice
import com.xyfindables.sdk.services.Service
import java.util.*

class GenericAttributeService(device: XYBluetoothDevice) : Service(device) {

    override val serviceUuid: UUID
        get() {
            return GenericAttributeService.uuid
        }

    val serviceChanged = IntegerCharacteristic(this, characteristics.ServiceChanged.uuid)

    companion object {
        val uuid = UUID.fromString("00001801-0000-1000-8000-00805F9B34FB")

        enum class characteristics(val uuid: UUID) {
            ServiceChanged(  UUID.fromString("00002a05-0000-1000-8000-00805f9b34fb"))
        }
    }
}