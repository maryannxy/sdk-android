package com.xyfindables.sdk

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.xyfindables.sdk.scanner.XYScanResult
import java.util.*

open class XYAppleBluetoothDevice(context: Context, device: BluetoothDevice) : XYBluetoothDevice(context, device) {

    companion object {
        val MANUFACTURER_ID = 0x004c

        fun enable(enable: Boolean) {
            if (enable) {
                XYBluetoothDevice.manufacturerToCreator[MANUFACTURER_ID] = {
                    context: Context,
                    scanResult: XYScanResult
                    ->
                    fromScanResult(context, scanResult)
                }
            } else {
                XYBluetoothDevice.manufacturerToCreator.remove(MANUFACTURER_ID)
            }
        }

        val typeToCreator = HashMap<Byte, (context:Context, scanResult: XYScanResult) -> XYBluetoothDevice?>()
        fun fromScanResult(context:Context, scanResult: XYScanResult) : XYBluetoothDevice? {
            for ((typeId, creator) in typeToCreator) {
                val bytes = scanResult.scanRecord?.getManufacturerSpecificData(MANUFACTURER_ID)
                if (bytes != null) {
                    if (bytes[0] == typeId) {
                        return creator(context, scanResult)
                    }
                }
            }
            return XYAppleBluetoothDevice(context, scanResult.device)
        }
    }
}