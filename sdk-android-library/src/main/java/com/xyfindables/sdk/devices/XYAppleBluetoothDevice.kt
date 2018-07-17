package com.xyfindables.sdk.devices

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.xyfindables.sdk.scanner.XYScanResult
import java.util.*

open class XYAppleBluetoothDevice(context: Context, device: BluetoothDevice, hash: Int) : XYBluetoothDevice(context, device, hash) {

    interface Listener : XYBluetoothDevice.Listener {
    }

    companion object : XYCreator() {

        val MANUFACTURER_ID = 0x004c

        var canCreate = false

        fun enable(enable: Boolean) {
            if (enable) {
                manufacturerToCreator[MANUFACTURER_ID] = this
            } else {
                manufacturerToCreator.remove(MANUFACTURER_ID)
            }
        }

        val typeToCreator = HashMap<Byte, XYCreator>()
        override fun getDevicesFromScanResult(context:Context, scanResult: XYScanResult, globalDevices: HashMap<Int, XYBluetoothDevice>, foundDevices: HashMap<Int, XYBluetoothDevice>) {
            for ((typeId, creator) in typeToCreator) {
                val bytes = scanResult.scanRecord?.getManufacturerSpecificData(MANUFACTURER_ID)
                if (bytes != null) {
                    if (bytes[0] == typeId) {
                        creator.getDevicesFromScanResult(context, scanResult, globalDevices, foundDevices)
                        return
                    }
                }
            }

            val hash = hashFromScanResult(scanResult)

            if (canCreate && hash != null) {
                foundDevices[hash] = globalDevices[hash] ?: XYAppleBluetoothDevice(context, scanResult.device, hash)
            }
        }

        fun hashFromScanResult(scanResult: XYScanResult): Int? {
            return scanResult.address.hashCode()
        }
    }
}