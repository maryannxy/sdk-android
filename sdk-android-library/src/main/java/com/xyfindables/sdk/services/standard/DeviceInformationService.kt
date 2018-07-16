package com.xyfindables.sdk.services.standard

import com.xyfindables.sdk.devices.XYBluetoothDevice
import com.xyfindables.sdk.services.Service
import java.util.*

class DeviceInformationService(device: XYBluetoothDevice) : Service(device) {

    override val serviceUuid: UUID
        get() {
            return DeviceInformationService.uuid
        }

    companion object {
        val uuid = UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB")

        enum class characteristics(val uuid: UUID) {
            SystemId(                                       UUID.fromString("00002a23-0000-1000-8000-00805f9b34fb")),
            ModelNumberString(                              UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")),
            SerialNumberString(                             UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb")),
            FirmwareRevisionString(                         UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")),
            HardwareRevisionString(                         UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb")),
            SoftwareRevisionString(                         UUID.fromString("00002a28-0000-1000-8000-00805f9b34fb")),
            ManufacturerNameString(                         UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb")),
            IEEE11073_20601RegulatoryCertificationDataList( UUID.fromString("00002a2a-0000-1000-8000-00805f9b34fb")),
            PnPId(                                          UUID.fromString("00002a50-0000-1000-8000-00805f9b34fb"))

        }
    }
}