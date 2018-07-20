package com.xyfindables.sdk

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.xyfindables.core.XYBase
import kotlinx.coroutines.experimental.newFixedThreadPoolContext

open class XYBluetoothBase(context: Context) : XYBase() {

    //we want to use the application context for everything
    protected val context = context.applicationContext

    protected val bluetoothManager : BluetoothManager?
        get() {
            return context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        }

    protected val bluetoothAdapter : BluetoothAdapter?
        get() {
            return bluetoothManager?.adapter
        }

    companion object {
        //this is the thread that all calls should happen on for gatt calls.
        val BluetoothThread = newFixedThreadPoolContext(1, "BluetoothThread")
    }

}