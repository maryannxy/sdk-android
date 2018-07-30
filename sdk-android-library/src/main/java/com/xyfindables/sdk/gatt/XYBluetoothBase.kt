package com.xyfindables.sdk.gatt

import android.bluetooth.*
import android.content.Context
import com.xyfindables.core.XYBase
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlin.coroutines.experimental.suspendCoroutine

open class XYBluetoothBase(context: Context) : XYBase() {

    //we store this since on initial creation, the applicationContext may not yet be available
    private val _context = context

    //we want to use the application context for everything
    protected val context : Context
        get() {
            return _context.applicationContext
        }

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