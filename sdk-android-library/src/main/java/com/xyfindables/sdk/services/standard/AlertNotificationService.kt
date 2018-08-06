package com.xyfindables.sdk.services.standard

import com.xyfindables.sdk.devices.XYBluetoothDevice
import com.xyfindables.sdk.services.Service
import java.util.*

class AlertNotificationService(device: XYBluetoothDevice) : Service(device) {

    override val serviceUuid: UUID
        get() {
            return AlertNotificationService.uuid
        }

    /**
     * Control point of the Alert Notification server.
     * Client can write the command here to request the several functions toward the server.
     */
    val controlPoint = IntegerCharacteristic(this, characteristics.ControlPoint.uuid)

    /**
     * Number of unread alerts that exist in the specific category in the device.
     */
    val unreadAlertStatus = IntegerCharacteristic(this, characteristics.UnreadAlertStatus.uuid)

    /**
     * Category of the alert and how many new alerts of that category have occurred on the server device
     */
    val newAlert = IntegerCharacteristic(this, characteristics.NewAlert.uuid)

    /**
     * Category that the server supports for new alerts
     */
    val supportedNewAlertCategory = IntegerCharacteristic(this, characteristics.SupportedNewAlertCategory.uuid)

    /**
     *  Number of unread alerts that exist in the specific category in the device
     */
    val supportedUnreadAlertCategory = IntegerCharacteristic(this, characteristics.SupportedUnreadAlertCategory.uuid)

    companion object {
        val uuid = UUID.fromString("00001811-0000-1000-8000-00805F9B34FB")

        enum class characteristics(val uuid: UUID) {
            ControlPoint(                   UUID.fromString("00002a44-0000-1000-8000-00805f9b34fb")),
            UnreadAlertStatus(              UUID.fromString("00002a45-0000-1000-8000-00805f9b34fb")),
            NewAlert(                       UUID.fromString("00002a46-0000-1000-8000-00805f9b34fb")),
            SupportedNewAlertCategory(      UUID.fromString("00002a47-0000-1000-8000-00805f9b34fb")),
            SupportedUnreadAlertCategory(   UUID.fromString("00002a48-0000-1000-8000-00805f9b34fb"))
        }
    }
}