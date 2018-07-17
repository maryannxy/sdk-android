package com.xyfindables.sdk.services.xy3

import com.xyfindables.sdk.devices.XYBluetoothDevice
import com.xyfindables.sdk.services.Service
import java.util.*

class ExtendedControlService(device: XYBluetoothDevice) : Service(device) {

    override val serviceUuid: UUID
        get() {
            return ExtendedControlService.uuid
        }

    val simStatus = IntegerCharacteristic(this, characteristics.SimStatus.uuid)
    val led = IntegerCharacteristic(this, characteristics.Led.uuid)
    val selfTest = IntegerCharacteristic(this, characteristics.SelfTest.uuid)

    companion object {
        val uuid = UUID.fromString("F014AA00-0439-3000-E001-00001001FFFF")

        enum class characteristics(val uuid: UUID) {
            SimStatus(UUID.fromString("2ADDAA00-0439-3000-E001-00001001FFFF")),
            Led(UUID.fromString("2AAAAA00-0439-3000-E001-00001001FFFF")),
            SelfTest(UUID.fromString("2a77AA00-0439-3000-E001-00001001FFFF"))
        }
    }
}