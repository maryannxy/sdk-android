package com.xyfindables.sdk.services.standard

import com.xyfindables.sdk.devices.XYBluetoothDevice
import com.xyfindables.sdk.services.Service
import java.util.*

class GenericAccessService(device: XYBluetoothDevice) : Service(device) {

    override val serviceUuid: UUID
        get() {
            return TxPowerService.uuid
        }

    val deviceName = IntegerCharacteristic(this, characteristics.DeviceName.uuid)
    val appearance = IntegerCharacteristic(this, characteristics.Appearance.uuid)
    val privacyFlag = IntegerCharacteristic(this, characteristics.PrivacyFlag.uuid)
    val reconnectionAddress = IntegerCharacteristic(this, characteristics.ReconnectionAddress.uuid)
    val peripheralPreferredConnectionParameters = IntegerCharacteristic(this, characteristics.PeripheralPreferredConnectionParameters.uuid)


    companion object {
        val uuid = UUID.fromString("00001800-0000-1000-8000-00805F9B34FB")

        enum class characteristics(val uuid: UUID) {
            DeviceName(                                 UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb")),
            Appearance(                                 UUID.fromString("00002a01-0000-1000-8000-00805f9b34fb")),
            PrivacyFlag(                                UUID.fromString("00002a02-0000-1000-8000-00805f9b34fb")),
            ReconnectionAddress(                        UUID.fromString("00002a03-0000-1000-8000-00805f9b34fb")),
            PeripheralPreferredConnectionParameters(    UUID.fromString("00002a04-0000-1000-8000-00805f9b34fb"))
        }
    }
}