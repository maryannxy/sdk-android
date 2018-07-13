package com.xyfindables.sdk.devices.router

import android.content.Context
import com.xyfindables.sdk.devices.XYBluetoothDevice
import com.xyfindables.sdk.scanner.XYScanResult


class Router {
    var routers = ArrayList<RouterInterface>()

    fun run (context: Context, scanResult: XYScanResult) : XYBluetoothDevice? {
        for (router in routers){
            val device = router.run(context, scanResult)
            if (device != null){
                return device
            }
        }
        return null
    }
}