package com.xyfindables.sdk.devices

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.xyfindables.core.XYBase
import com.xyfindables.sdk.scanner.XYScanResult
import java.util.*

open class XYAppleBluetoothDevice(context: Context, device: BluetoothDevice, hash: Int) : XYBluetoothDevice(context, device, hash) {

    open class Listener : XYBluetoothDevice.Listener() {
    }

    companion object : XYBase() {

        const val MANUFACTURER_ID = 0x004c

        var canCreate = false

        fun enable(enable: Boolean) {
            if (enable) {
                manufacturerToCreator[MANUFACTURER_ID] = this.creator
            } else {
                manufacturerToCreator.remove(MANUFACTURER_ID)
            }
        }

        internal val typeToCreator = HashMap<Byte, XYCreator>()

        internal val creator = object : XYCreator() {
            override fun getDevicesFromScanResult(context: Context, scanResult: XYScanResult, globalDevices: HashMap<Int, XYBluetoothDevice>, foundDevices: HashMap<Int, XYBluetoothDevice>) {
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
                val device = scanResult.device

                if (canCreate && hash != null && device != null) {
                    foundDevices[hash] = globalDevices[hash] ?: XYAppleBluetoothDevice(context, device, hash)
                }
            }
        }

        fun hashFromScanResult(scanResult: XYScanResult): Int? {
            return scanResult.address.hashCode()
        }
    }
}