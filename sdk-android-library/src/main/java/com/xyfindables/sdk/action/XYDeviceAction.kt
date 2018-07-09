package com.xyfindables.sdk.action

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.Context
import android.os.AsyncTask

import com.xyfindables.core.XYBase
import com.xyfindables.sdk.XYDevice
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async

import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Created by arietrouw on 1/1/17.
 */

abstract class XYDeviceAction(val device: XYDevice) : XYBase() {
    private var _servicesDiscovered = false
    private var _characteristicFound = false

    val key: String
        get() = TAG + hashCode()

    abstract val serviceId: UUID
    abstract val characteristicId: UUID

    fun start(context: Context) {
        logInfo(TAG, this.javaClass.superclass.simpleName + ":starting...")
        async (CommonPool) {
            logInfo(TAG, "running...")
        }
    }

    open fun statusChanged(status: Int, gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, success: Boolean): Boolean {
        if (!success) {
            logError(TAG, this.javaClass.superclass.simpleName + ":statusChanged(failed):" + status, false)
        }
        when (status) {
            STATUS_QUEUED -> {
            }
            STATUS_STARTED -> {
            }
            STATUS_SERVICE_FOUND -> if (!_servicesDiscovered) {
                _servicesDiscovered = true
            } else {
                logError(TAG, "connTest-" + this.javaClass.superclass.simpleName + ":Second Service Found Received", true)
            }
            STATUS_CHARACTERISTIC_FOUND ->

                if (!_characteristicFound) {
                    _characteristicFound = true
                } else {
                    logError(TAG, "connTest-" + this.javaClass.superclass.simpleName + ":Second Characteristic Found Received", true)
                }
            STATUS_CHARACTERISTIC_READ -> return true
            STATUS_CHARACTERISTIC_WRITE -> return true
            STATUS_COMPLETED -> if (!success) {
                logError(TAG, this.javaClass.superclass.simpleName + ":Completed with Failure", false)
            }
        }
        return false
    }

    open fun statusChanged(descriptor: BluetoothGattDescriptor?, status: Int, gatt: BluetoothGatt?, success: Boolean): Boolean {
        if (!success) {
            logError(TAG, this.javaClass.superclass.simpleName + ":statusChanged(failed):" + status, false)
        }
        when (status) {
            STATUS_QUEUED -> {
            }
            STATUS_STARTED -> {
            }
            STATUS_SERVICE_FOUND -> if (!_servicesDiscovered) {
                _servicesDiscovered = true
            } else {
                logError(TAG, this.javaClass.superclass.simpleName + ":Second Service Found Received", true)
            }
            STATUS_CHARACTERISTIC_FOUND ->

                if (!_characteristicFound) {
                    _characteristicFound = true
                } else {
                    logError(TAG, this.javaClass.superclass.simpleName + ":Second Characteristic Found Received", true)
                }
            STATUS_CHARACTERISTIC_READ -> return true
            STATUS_CHARACTERISTIC_WRITE -> return true
            STATUS_COMPLETED -> if (!success) {
                logError(TAG, this.javaClass.superclass.simpleName + ":Completed with Failure", false)
            }
        }
        return false
    }

    companion object {

        private val TAG = XYDeviceAction::class.java.simpleName

        val STATUS_QUEUED = 1
        val STATUS_STARTED = 2
        val STATUS_SERVICE_FOUND = 3
        val STATUS_CHARACTERISTIC_FOUND = 4
        val STATUS_CHARACTERISTIC_READ = 5
        val STATUS_CHARACTERISTIC_WRITE = 6
        val STATUS_CHARACTERISTIC_UPDATED = 7
        val STATUS_COMPLETED = 8

        private var _threadPool: ThreadPoolExecutor? = null

        init {
            _threadPool = ThreadPoolExecutor(10, 50, 30, TimeUnit.SECONDS, LinkedBlockingQueue())
        }
    }
}
