package com.xyfindables.sdk.gatt

import android.annotation.TargetApi
import android.bluetooth.*
import android.content.Context
import android.os.Build
import android.os.Handler
import com.xyfindables.core.XYBase
import com.xyfindables.sdk.CallByVersion
import com.xyfindables.sdk.scanner.XYFilteredSmartScan
import kotlinx.coroutines.experimental.*
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.HashMap
import kotlin.coroutines.experimental.suspendCoroutine

open class XYBluetoothGatt protected constructor(
        protected val context:Context,
        protected var device: BluetoothDevice?,
        private val autoConnect: Boolean,
        private val callback: BluetoothGattCallback?,
        private val transport: Int?,
        private val phy: Int?,
        private val handler: Handler?
) : XYBase() {

    protected var gatt: BluetoothGatt? = null

    private val gattListeners = HashMap<String, WeakReference<BluetoothGattCallback>>()

    private val bluetoothManager = getBluetoothManager(context)

    protected var references = 0

    protected var _stayConnected = false

    var stayConnected : Boolean
        get() {
            return _stayConnected
        }
        set(value) {
            _stayConnected = value
            if (!_stayConnected) {
                cleanUpIfNeeded()
            }
        }

    val connectionState: Int
        get() {
            return _connectionState
        }

    //last time this device was accessed (connected to)
    var lastAccessTime = 0L

    //last time we heard a ad from this device
    var lastAdTime = 0L

    fun updateBluetoothDevice(device: BluetoothDevice?) {
        this.device = device
    }

    private var _connectionState = bluetoothManager.getConnectionState(device, BluetoothProfile.GATT)

    val closed: Boolean
        get() {
            return (gatt == null)
        }

    fun addGattListener(key: String, listener: BluetoothGattCallback) {
        logInfo("addListener")
        synchronized(gattListeners) {
            gattListeners[key] = WeakReference(listener)
        }
    }

    fun removeGattListener(key: String) {
        logInfo("removeListener")
        synchronized(gattListeners) {
            gattListeners.remove(key)
        }
    }

    fun asyncClose() : Deferred<Unit>{
        return async(GattThread) {
            logInfo("asyncClose")
            asyncDisconnect().await()
            logInfo("asyncClose: Disconnected")
            safeClose().await()
            logInfo("asyncClose: Closed")
            removeGattListener("default")
            gatt = null
            return@async
        }
    }

    private fun safeConnect() : Deferred<Boolean> {
        return async(GattThread) {
            logInfo("safeConnect")
            return@async gatt?.connect() ?: false
        }
    }

    private fun safeDisconnect() : Deferred<Unit> {
        return async(GattThread) {
            logInfo("safeDisconnect")
            gatt?.disconnect()
            return@async
        }
    }

    private fun safeClose() : Deferred<Unit> {
        return async(GattThread) {
            logInfo("safeClose")
            gatt?.close()
            return@async
        }
    }

    fun asyncDiscover() : Deferred<XYBluetoothResult<List<BluetoothGattService>>> {
        return asyncBle {
            var error: XYBluetoothError? = null
            var value: List<BluetoothGattService>? = null

            lastAccessTime = now

            val gatt = this@XYBluetoothGatt.gatt

            if (gatt == null) {
                error = XYBluetoothError("asyncDiscover: No Gatt")
            }

            lastAccessTime = now

            if (gatt != null) {

                logInfo("asyncDiscover:${gatt.services.size}")
                if (gatt.services.size == 0) {
                    value = suspendCoroutine { cont ->
                        logInfo("asyncDiscover:CoRoutine")
                        val listener = object : BluetoothGattCallback() {
                            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                                super.onServicesDiscovered(gatt, status)
                                if (status != BluetoothGatt.GATT_SUCCESS) {
                                    error = XYBluetoothError("asyncDiscover: discoverStatus: $status")
                                } else {
                                    if (gatt == null) {
                                        error = XYBluetoothError("asyncDiscover: gatt: NULL")
                                        cont.resume(null)
                                    } else {
                                        cont.resume(gatt.services)
                                    }
                                }
                            }

                            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                                super.onConnectionStateChange(gatt, status, newState)
                                if (newState != BluetoothGatt.STATE_CONNECTED) {
                                    error = XYBluetoothError("asyncDiscover: connection dropped")
                                    cont.resume(null)
                                }
                            }
                        }
                        addGattListener("asyncDiscover", listener)
                        if (!gatt.discoverServices()) {
                            error = XYBluetoothError("asyncDiscover: gatt.discoverServices failed to start")
                            cont.resume(null)
                        }
                    }
                    removeGattListener("asyncDiscover")
                } else {
                    value = gatt.services
                }
            }

            return@asyncBle XYBluetoothResult(value, error)
        }
    }

    fun asyncWriteCharacteristic(characteristicToWrite: BluetoothGattCharacteristic) : Deferred<XYBluetoothResult<ByteArray>>{
        return asyncBle {
            logInfo("asyncWriteCharacteristic")
            var error: XYBluetoothError? = null
            var value: ByteArray? = null

            val gatt = this@XYBluetoothGatt.gatt

            if (gatt == null) {
                error = XYBluetoothError("asyncWriteCharacteristic: No Gatt")
            }

            lastAccessTime = now

            if (gatt != null) {
                value = suspendCoroutine { cont ->
                    val listener = object : BluetoothGattCallback() {
                        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
                            super.onCharacteristicWrite(gatt, characteristic, status)
                            //since it is always possible to have a rogue callback, make sure it is the one we are looking for
                            if (characteristicToWrite == characteristic) {
                                if (status == BluetoothGatt.GATT_SUCCESS) {
                                    cont.resume(characteristicToWrite.value)
                                } else {
                                    error = XYBluetoothError("asyncWriteCharacteristic: onCharacteristicWrite failed: $status")
                                    cont.resume(null)
                                }
                            }
                        }

                        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                            super.onConnectionStateChange(gatt, status, newState)
                            if (newState != BluetoothGatt.STATE_CONNECTED) {
                                error = XYBluetoothError("asyncWriteCharacteristic: connection dropped")
                                cont.resume(null)
                            }
                        }
                    }
                    addGattListener("asyncWriteCharacteristic", listener)
                    if (!gatt.writeCharacteristic(characteristicToWrite)) {
                        error = XYBluetoothError("asyncWriteCharacteristic: gatt.writeCharacteristic failed to start")
                        cont.resume(null)
                    }
                }
                removeGattListener("asyncWriteCharacteristic")
            }

            return@asyncBle XYBluetoothResult(value, error)
        }
    }

    fun asyncReadCharacteristicInt(characteristicToRead: BluetoothGattCharacteristic, formatType:Int, offset:Int) : Deferred<XYBluetoothResult<Int>>{
        return asyncBle {
            logInfo("asyncReadCharacteristicInt")
            var error: XYBluetoothError? = null
            var value: Int? = null

            val gatt = this@XYBluetoothGatt.gatt

            if (gatt == null) {
                error = XYBluetoothError("asyncReadCharacteristicInt: No Gatt")
            }

            lastAccessTime = now

            if (gatt != null) {
                value = suspendCoroutine { cont ->
                    val listener = object : BluetoothGattCallback() {
                        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
                            super.onCharacteristicRead(gatt, characteristic, status)
                            //since it is always possible to have a rogue callback, make sure it is the one we are looking for
                            if (characteristicToRead == characteristic) {
                                if (status == BluetoothGatt.GATT_SUCCESS) {
                                    cont.resume(characteristicToRead.getIntValue(formatType, offset))
                                } else {
                                    error = XYBluetoothError("asyncReadCharacteristicInt: onCharacteristicRead failed: $status")
                                    cont.resume(null)
                                }
                            }
                        }

                        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                            super.onConnectionStateChange(gatt, status, newState)
                            if (newState != BluetoothGatt.STATE_CONNECTED) {
                                error = XYBluetoothError("asyncReadCharacteristicInt: connection dropped")
                                cont.resume(null)
                            }
                        }
                    }
                    addGattListener("asyncReadCharacteristicInt", listener)
                    if (!gatt.readCharacteristic(characteristicToRead)) {
                        error = XYBluetoothError("asyncReadCharacteristicInt: gatt.readCharacteristic failed to start")
                        cont.resume(null)
                    }
                }

                removeGattListener("asyncReadCharacteristicInt")
            }

            return@asyncBle XYBluetoothResult(value, error)
        }
    }

    fun asyncReadCharacteristicString(characteristicToRead: BluetoothGattCharacteristic, offset:Int) : Deferred<XYBluetoothResult<String>>{
        return asyncBle {
            logInfo("asyncReadCharacteristicString")
            var error: XYBluetoothError? = null
            var value: String? = null

            val gatt = this@XYBluetoothGatt.gatt

            if (gatt == null) {
                error = XYBluetoothError("asyncReadCharacteristicString: No Gatt")
            }

            lastAccessTime = now

            if (gatt != null) {
                value = suspendCoroutine { cont ->
                    val listener = object : BluetoothGattCallback() {
                        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
                            super.onCharacteristicRead(gatt, characteristic, status)
                            //since it is always possible to have a rogue callback, make sure it is the one we are looking for
                            if (characteristicToRead == characteristic) {
                                if (status == BluetoothGatt.GATT_SUCCESS) {
                                    cont.resume(characteristicToRead.getStringValue(offset))
                                } else {
                                    error = XYBluetoothError("asyncReadCharacteristicString: onCharacteristicRead failed: $status")
                                    cont.resume(null)
                                }
                            }
                        }

                        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                            super.onConnectionStateChange(gatt, status, newState)
                            if (newState != BluetoothGatt.STATE_CONNECTED) {
                                error = XYBluetoothError("asyncReadCharacteristicString: connection dropped")
                                cont.resume(null)
                            }
                        }
                    }
                    addGattListener("asyncReadCharacteristicString", listener)
                    if (!gatt.readCharacteristic(characteristicToRead)) {
                        error = XYBluetoothError("asyncReadCharacteristicString: gatt.readCharacteristic failed to start")
                        cont.resume(null)
                    }
                }

                removeGattListener("asyncReadCharacteristicString")
            }

            return@asyncBle XYBluetoothResult(value, error)
        }
    }

    fun asyncReadCharacteristicFloat(characteristicToRead: BluetoothGattCharacteristic, formatType:Int, offset:Int) : Deferred<XYBluetoothResult<Float>>{
        return asyncBle {
            logInfo("asyncReadCharacteristicFloat")
            var error: XYBluetoothError? = null
            var value: Float? = null

            val gatt = this@XYBluetoothGatt.gatt

            if (gatt == null) {
                error = XYBluetoothError("asyncReadCharacteristicFloat: No Gatt")
            }

            lastAccessTime = now

            if (gatt != null) {
                value = suspendCoroutine { cont ->
                    val listener = object : BluetoothGattCallback() {
                        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
                            super.onCharacteristicRead(gatt, characteristic, status)
                            //since it is always possible to have a rogue callback, make sure it is the one we are looking for
                            if (characteristicToRead == characteristic) {
                                if (status == BluetoothGatt.GATT_SUCCESS) {
                                    cont.resume(characteristicToRead.getFloatValue(formatType, offset))
                                } else {
                                    error = XYBluetoothError("asyncReadCharacteristicFloat: onCharacteristicRead failed: $status")
                                    cont.resume(null)
                                }
                            }
                        }

                        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                            super.onConnectionStateChange(gatt, status, newState)
                            if (newState != BluetoothGatt.STATE_CONNECTED) {
                                error = XYBluetoothError("asyncReadCharacteristicFloat: connection dropped")
                                cont.resume(null)
                            }
                        }
                    }
                    addGattListener("asyncReadCharacteristicString", listener)
                    if (!gatt.readCharacteristic(characteristicToRead)) {
                        error = XYBluetoothError("asyncReadCharacteristicString: gatt.readCharacteristic failed to start")
                        cont.resume(null)
                    }
                }

                removeGattListener("asyncReadCharacteristicString")
            }

            return@asyncBle XYBluetoothResult(value, error)
        }
    }

    fun asyncReadCharacteristicBytes(characteristicToRead: BluetoothGattCharacteristic) : Deferred<XYBluetoothResult<ByteArray>>{
        return asyncBle {
            logInfo("asyncReadCharacteristicBytes")
            var error: XYBluetoothError? = null
            var value: ByteArray? = null

            val gatt = this@XYBluetoothGatt.gatt

            if (gatt == null) {
                error = XYBluetoothError("asyncReadCharacteristicBytes: No Gatt")
            }

            lastAccessTime = now

            if (gatt != null) {
                value = suspendCoroutine { cont ->
                    val listener = object : BluetoothGattCallback() {
                        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
                            super.onCharacteristicRead(gatt, characteristic, status)
                            //since it is always possible to have a rogue callback, make sure it is the one we are looking for
                            if (characteristicToRead == characteristic) {
                                if (status == BluetoothGatt.GATT_SUCCESS) {
                                    cont.resume(characteristicToRead.value)
                                } else {
                                    error = XYBluetoothError("asyncReadCharacteristicBytes: onCharacteristicRead failed: $status")
                                    cont.resume(null)
                                }
                            }
                        }

                        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                            super.onConnectionStateChange(gatt, status, newState)
                            if (newState != BluetoothGatt.STATE_CONNECTED) {
                                error = XYBluetoothError("asyncReadCharacteristicBytes: connection dropped")
                                cont.resume(null)
                            }
                        }

                    }
                    addGattListener("asyncReadCharacteristicBytes", listener)
                    if (!gatt.readCharacteristic(characteristicToRead)) {
                        error = XYBluetoothError("asyncReadCharacteristicBytes: gatt.readCharacteristic failed to start")
                        cont.resume(null)
                    }
                }

                removeGattListener("asyncReadCharacteristicBytes")
            }

            return@asyncBle XYBluetoothResult(value, error)
        }
    }

    fun asyncFindAndReadCharacteristicInt(service: UUID, characteristic: UUID, formatType:Int, offset:Int) : Deferred<XYBluetoothResult<Int>> {
        return asyncBle {
            logInfo("asyncFindAndReadCharacteristicInt")
            var error: XYBluetoothError? = null
            var value: Int? = null

            val findResult = asyncFindCharacteristic(service, characteristic).await()
            val characteristicToRead = findResult.value
            if (findResult.error == null) {
                if (characteristicToRead != null) {
                    val readResult = asyncReadCharacteristicInt(characteristicToRead, formatType, offset).await()
                    value = readResult.value
                    error = readResult.error
                } else {
                    error = XYBluetoothError("asyncFindAndReadCharacteristicInt: Got Null Value")
                }
            }

            lastAccessTime = now

            return@asyncBle XYBluetoothResult(value, error)
        }
    }

    fun asyncFindAndReadCharacteristicFloat(service: UUID, characteristic: UUID, formatType:Int, offset:Int) : Deferred<XYBluetoothResult<Float>> {
        return asyncBle {
            logInfo("asyncFindAndReadCharacteristicFloat")
            var error: XYBluetoothError? = null
            var value: Float? = null

            val findResult = asyncFindCharacteristic(service, characteristic).await()
            val characteristicToRead = findResult.value
            if (findResult.error == null) {
                if (characteristicToRead != null) {
                    val readResult = asyncReadCharacteristicFloat(characteristicToRead, formatType, offset).await()
                    value = readResult.value
                    error = readResult.error
                } else {
                    error = XYBluetoothError("asyncFindAndReadCharacteristicFloat: Got Null Value")
                }
            }

            lastAccessTime = now

            return@asyncBle XYBluetoothResult(value, error)
        }
    }

    fun asyncFindAndReadCharacteristicString(service: UUID, characteristic: UUID, offset:Int) : Deferred<XYBluetoothResult<String>> {
        return asyncBle {
            logInfo("asyncFindAndReadCharacteristicString")
            var error: XYBluetoothError? = null
            var value: String? = null

            val findResult = asyncFindCharacteristic(service, characteristic).await()
            val characteristicToRead = findResult.value
            if (findResult.error == null) {
                if (characteristicToRead != null) {
                    val readResult = asyncReadCharacteristicString(characteristicToRead, offset).await()
                    value = readResult.value
                    error = readResult.error
                } else {
                    error = XYBluetoothError("asyncFindAndReadCharacteristicString: Got Null Value")
                }
            }

            lastAccessTime = now

            return@asyncBle XYBluetoothResult(value, error)
        }
    }

    fun asyncFindAndReadCharacteristicBytes(service: UUID, characteristic: UUID) : Deferred<XYBluetoothResult<ByteArray>> {
        return asyncBle {
            logInfo("asyncFindAndReadCharacteristicBytes")
            var error: XYBluetoothError? = null
            var value: ByteArray? = null

            val findResult = asyncFindCharacteristic(service, characteristic).await()
            val characteristicToRead = findResult.value
            if (findResult.error == null) {
                if (characteristicToRead != null) {
                    val readResult = asyncReadCharacteristicBytes(characteristicToRead).await()
                    value = readResult.value
                    error = readResult.error
                } else {
                    error = XYBluetoothError("asyncFindAndReadCharacteristicBytes: Got Null Value")
                }
            }

            lastAccessTime = now

            return@asyncBle XYBluetoothResult(value, error)
        }
    }

    fun asyncFindAndWriteCharacteristic(service: UUID, characteristic: UUID, valueToWrite:Int, formatType:Int, offset:Int) : Deferred<XYBluetoothResult<Int>> {
        return asyncBle {
            logInfo("asyncFindAndWriteCharacteristic")
            var error: XYBluetoothError? = null
            var value: Int? = null

            val findResult = asyncFindCharacteristic(service, characteristic).await()
            logInfo("asyncFindAndWriteCharacteristic: Found")
            val characteristicToWrite = findResult.value
            if (findResult.error == null) {
                logInfo("asyncFindAndWriteCharacteristic: $characteristicToWrite")
                if (characteristicToWrite != null) {
                    characteristicToWrite.setValue(valueToWrite, formatType, offset)
                    logInfo("asyncFindAndWriteCharacteristic: Set")
                    val writeResult = asyncWriteCharacteristic(characteristicToWrite).await()
                    logInfo("asyncFindAndWriteCharacteristic: Write Complete: $writeResult")
                    value = valueToWrite
                    error = writeResult.error
                } else {
                    error = XYBluetoothError("asyncFindAndWriteCharacteristic: Got Null Value")
                }
            }

            lastAccessTime = now

            return@asyncBle XYBluetoothResult(value, error)
        }
    }

    fun asyncFindAndWriteCharacteristicFloat(service: UUID, characteristic: UUID, mantissa: Int, exponent: Int, formatType:Int, offset:Int) : Deferred<XYBluetoothResult<ByteArray>> {
        return asyncBle {
            logInfo("asyncFindAndWriteCharacteristicFloat")
            var error: XYBluetoothError? = null
            var value: ByteArray? = null

            val findResult = asyncFindCharacteristic(service, characteristic).await()
            val characteristicToWrite = findResult.value
            if (findResult.error == null) {
                if (characteristicToWrite != null) {
                    characteristicToWrite.setValue(mantissa, exponent, formatType, offset)
                    val writeResult = asyncWriteCharacteristic(characteristicToWrite).await()
                    value = writeResult.value
                    error = writeResult.error
                } else {
                    error = XYBluetoothError("asyncFindAndWriteCharacteristic: Got Null Value")
                }
            }

            lastAccessTime = now

            return@asyncBle XYBluetoothResult(value, error)
        }
    }

    fun asyncFindAndWriteCharacteristic(service: UUID, characteristic: UUID, valueToWrite:String) : Deferred<XYBluetoothResult<String>> {
        return asyncBle {
            logInfo("asyncFindAndWriteCharacteristic")
            var error: XYBluetoothError? = null
            var value: String? = null

            val findResult = asyncFindCharacteristic(service, characteristic).await()
            val characteristicToWrite = findResult.value
            if (findResult.error == null) {
                if (characteristicToWrite != null) {
                    characteristicToWrite.setValue(valueToWrite)
                    val writeResult = asyncWriteCharacteristic(characteristicToWrite).await()
                    value = valueToWrite
                    error = writeResult.error
                } else {
                    error = XYBluetoothError("asyncFindAndWriteCharacteristic: Got Null Value")
                }
            }

            lastAccessTime = now

            return@asyncBle XYBluetoothResult(value, error)
        }
    }

    fun asyncFindAndWriteCharacteristic(service: UUID, characteristic: UUID, bytes:ByteArray) : Deferred<XYBluetoothResult<ByteArray>> {
        return asyncBle {
            logInfo("asyncFindAndWriteCharacteristic")
            var error: XYBluetoothError? = null
            var value: ByteArray? = null

            val findResult = asyncFindCharacteristic(service, characteristic).await()
            val characteristicToWrite = findResult.value
            if (findResult.error == null) {
                if (characteristicToWrite != null) {
                    characteristicToWrite.setValue(bytes)
                    val writeResult = asyncWriteCharacteristic(characteristicToWrite).await()
                    value = writeResult.value
                    error = writeResult.error
                } else {
                    error = XYBluetoothError("asyncFindAndWriteCharacteristic: Got Null Value")
                }
            }

            lastAccessTime = now

            return@asyncBle XYBluetoothResult(value, error)
        }
    }

    fun asyncFindAndWriteCharacteristicNotify(service: UUID, characteristic: UUID, enable:Boolean) : Deferred<XYBluetoothResult<Boolean>> {
        return asyncBle {
            logInfo("asyncFindAndWriteCharacteristic")
            var error: XYBluetoothError? = null
            var value: Boolean? = null

            val findResult = asyncFindCharacteristic(service, characteristic).await()
            val characteristicToWrite = findResult.value
            if (findResult.error == null) {
                if (characteristicToWrite != null) {
                    if (enable) {
                        characteristicToWrite.setValue(1, BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                    }
                    else {
                        characteristicToWrite.setValue(0, BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                    }
                    val writeResult = asyncWriteCharacteristic(characteristicToWrite).await()
                    value = enable
                    error = writeResult.error
                } else {
                    error = XYBluetoothError("asyncFindAndWriteCharacteristic: Got Null Value")
                }
            }

            lastAccessTime = now

            return@asyncBle XYBluetoothResult(value, error)
        }
    }

    private fun getBluetoothManager(context: Context): BluetoothManager {
        return context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    fun asyncConnect() : Deferred<XYBluetoothResult<Boolean>> {
        return asyncBle {
            logInfo("asyncConnect")
            var error: XYBluetoothError? = null
            var value: Boolean? = null

            val gatt = this@XYBluetoothGatt.gatt

            if (gatt == null) {
                error = XYBluetoothError("asyncConnect: No Gatt")
            }

            if (gatt != null) {

                value = suspendCoroutine { cont ->
                    val listener = object : BluetoothGattCallback() {
                        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                            super.onConnectionStateChange(gatt, status, newState)
                            if (status == BluetoothGatt.GATT_FAILURE) {
                                error = XYBluetoothError("asyncConnect: connection failed(status): $status : $newState")
                                cont.resume(null)
                            } else if (newState == BluetoothGatt.STATE_CONNECTED) {
                                cont.resume(true)
                            } else if (newState == BluetoothGatt.STATE_CONNECTING) {
                                //wait some more
                            } else {
                                error = XYBluetoothError("asyncConnect: connection failed(state): $status : $newState")
                                cont.resume(null)
                            }
                        }
                    }
                    addGattListener("asyncReadCharacteristicBytes", listener)

                    if (connectionState == BluetoothGatt.STATE_CONNECTED) {
                        logInfo("asyncConnect:already connected")
                        cont.resume(true)
                    } else if (connectionState == BluetoothGatt.STATE_CONNECTING) {
                        logInfo("asyncConnect:connecting")
                        //dont call connect since already in progress
                    } else if (!gatt.connect()) {
                        error = XYBluetoothError("asyncReadCharacteristicBytes: gatt.readCharacteristic failed to start")
                        cont.resume(null)
                    }
                }
            }
            return@asyncBle XYBluetoothResult(value, error)
        }
    }

    //the goal is to leave connections hanging for a little bit in the case
    //that they need to be reestablished in short notice
    private fun cleanUpIfNeeded() {
        launch(CommonPool) {
            logInfo("cleanUpIfNeeded")

            while(!closed) {
                //if the global and local last connection times do not match
                //after the delay, that means a newer connection is now responsible for closing it
                val localAccessTime = now

                delay(CLEANUP_DELAY)

                //the goal is to close the connection if the ref count is
                //down to zero.  We have to check the lastAccess to make sure the delay is after
                //the last guy, not an earlier one

                logInfo("cleanUpIfNeeded: Checking")

                if (!stayConnected && !closed && references == 0 && lastAccessTime == localAccessTime) {
                    logInfo("cleanUpIfNeeded: Cleaning")
                    asyncClose().await()
                }
            }
        }
    }

    fun asyncDisconnect() : Deferred<Unit>{
        return async(CommonPool) {
            logInfo("asyncDisconnect")
            if (connectionState == BluetoothGatt.STATE_DISCONNECTED) {
                return@async
            }

            safeDisconnect().await()

            //this has to be on new thread to allow BT to process
            return@async async(CommonPool) {
                //this assumes that we will *always* get to disconnected
                while (connectionState != BluetoothGatt.STATE_DISCONNECTED) {
                    delay(WAIT_RESOLUTION)
                }
                logInfo("asyncDisconnect: Complete")
                delay(SAFE_DELAY)
            }.await()
        }
    }

    //this can only be called after a successful discover
    private fun asyncFindCharacteristic(service: UUID, characteristic: UUID) : Deferred<XYBluetoothResult<BluetoothGattCharacteristic>> {

        return asyncBle {

            logInfo("asyncFindCharacteristic")
            var error: XYBluetoothError? = null
            var value: BluetoothGattCharacteristic? = null

            val gatt = this@XYBluetoothGatt.gatt

            if (gatt == null) {
                error = XYBluetoothError("asyncReadCharacteristicBytes: No Gatt")
            }

            lastAccessTime = now

            if (gatt != null) {
                value = suspendCoroutine { cont ->
                    //error and throw exception if discovery not done yet
                    if (gatt.services?.size == 0) {
                        error = XYBluetoothError("Services Not Discovered Yet")

                    }

                    logInfo("findCharacteristic")
                    var foundCharacteristic: BluetoothGattCharacteristic? = null
                    val foundService = gatt.getService(service)
                    logInfo("findCharacteristic:service:$foundService")
                    if (foundService != null) {
                        foundCharacteristic = foundService.getCharacteristic(characteristic)
                        logInfo("findCharacteristic:characteristic:$foundCharacteristic")
                        cont.resume(foundCharacteristic)
                    } else {
                        error = XYBluetoothError("asyncReadCharacteristicBytes: Characteristic not Found!")
                        cont.resume(null)
                    }
                }
            }
            logInfo("findCharacteristic: Returning: $value")
            return@asyncBle XYBluetoothResult(value, error)
        }
    }

    private val centralCallback = object : BluetoothGattCallback() {
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicChanged(gatt, characteristic)
            logInfo("onCharacteristicChanged: $characteristic")
            synchronized(gattListeners) {
                for((key, listener) in gattListeners) {
                    val innerListener = listener.get()
                    if (innerListener != null) {
                        launch(CommonPool) {
                            logInfo("onCharacteristicChanged: $key")
                            innerListener.onCharacteristicChanged(gatt, characteristic)
                        }
                    }
                }
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            logInfo("onCharacteristicRead: $characteristic : $status")
            synchronized(gattListeners) {
                for((_, listener) in gattListeners) {
                    val innerListener = listener.get()
                    if (innerListener != null) {
                        launch(CommonPool) {
                            innerListener.onCharacteristicRead(gatt, characteristic, status)
                        }
                    }
                }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            logInfo("onCharacteristicWrite: $characteristic : $status")
            synchronized(gattListeners) {
                for((_, listener) in gattListeners) {
                    val innerListener = listener.get()
                    if (innerListener != null) {
                        launch(CommonPool) {
                            innerListener.onCharacteristicWrite(gatt, characteristic, status)
                        }
                    }
                }
            }
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            logInfo("onConnectionStateChange: ${gatt?.device?.address} $newState : $status")
            if (status != BluetoothGatt.GATT_SUCCESS) {
                logError("onConnectionStateChange Failed", false)
            }

            _connectionState = newState
            synchronized(gattListeners) {
                for ((_, listener) in gattListeners) {
                    val innerListener = listener.get()
                    if (innerListener != null) {
                        launch(CommonPool) {
                            innerListener.onConnectionStateChange(gatt, status, newState)
                        }
                    }
                }
            }
        }

        override fun onDescriptorRead(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            super.onDescriptorRead(gatt, descriptor, status)
            logInfo("onDescriptorRead: $descriptor : $status")
            synchronized(gattListeners) {
                for((_, listener) in gattListeners) {
                    val innerListener = listener.get()
                    if (innerListener != null) {
                        launch(CommonPool) {
                            innerListener.onDescriptorRead(gatt, descriptor, status)
                        }
                    }
                }
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            logInfo("onDescriptorWrite: $descriptor : $status")
            synchronized(gattListeners) {
                for((_, listener) in gattListeners) {
                    val innerListener = listener.get()
                    if (innerListener != null) {
                        launch(CommonPool) {
                            innerListener.onDescriptorWrite(gatt, descriptor, status)
                        }
                    }
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            logInfo("onMtuChanged: $mtu : $status")
            synchronized(gattListeners) {
                for((_, listener) in gattListeners) {
                    val innerListener = listener.get()
                    if (innerListener != null) {
                        launch(CommonPool) {
                            @Suppress()
                            innerListener.onMtuChanged(gatt, mtu, status)
                        }
                    }
                }
            }
        }

        override fun onPhyRead(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            super.onPhyRead(gatt, txPhy, rxPhy, status)
            logInfo("onPhyRead: $txPhy : $rxPhy : $status")
            synchronized(gattListeners) {
                for((_, listener) in gattListeners) {
                    val innerListener = listener.get()
                    if (innerListener != null) {
                        launch(CommonPool) {
                            @Suppress()
                            innerListener.onPhyRead(gatt, txPhy, rxPhy, status)
                        }
                    }
                }
            }
        }

        @TargetApi(26)
        override fun onPhyUpdate(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status)
            logInfo("onPhyUpdate: $txPhy : $rxPhy : $status")
            synchronized(gattListeners) {
                for((_, listener) in gattListeners) {
                    val innerListener = listener.get()
                    if (innerListener != null) {
                        launch(CommonPool) {
                            @Suppress()
                            innerListener.onPhyUpdate(gatt, txPhy, rxPhy, status)
                        }
                    }
                }
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            super.onReadRemoteRssi(gatt, rssi, status)
            logInfo("onReadRemoteRssi: $rssi : $status")
            synchronized(gattListeners) {
                for((_, listener) in gattListeners) {
                    val innerListener = listener.get()
                    if (innerListener != null) {
                        launch(CommonPool) {
                            innerListener.onReadRemoteRssi(gatt, rssi, status)
                        }
                    }
                }
            }
        }

        override fun onReliableWriteCompleted(gatt: BluetoothGatt?, status: Int) {
            super.onReliableWriteCompleted(gatt, status)
            logInfo("onReliableWriteCompleted: $status")
            synchronized(gattListeners) {
                for((_, listener) in gattListeners) {
                    val innerListener = listener.get()
                    if (innerListener != null) {
                        launch(CommonPool) {
                            innerListener.onReliableWriteCompleted(gatt, status)
                        }
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            logInfo("onServicesDiscovered: $status")
            synchronized(gattListeners) {
                for((_, listener) in gattListeners) {
                    val innerListener = listener.get()
                    if (innerListener != null) {
                        launch(CommonPool) {
                            innerListener.onServicesDiscovered(gatt, status)
                        }
                    }
                }
            }
        }
    }

    fun connectGatt() : Deferred<XYBluetoothResult<Boolean>> {
        return asyncBle {
            logInfo("connectGatt")
            var error: XYBluetoothError? = null
            var value: Boolean? = null

            val device = this@XYBluetoothGatt.device
            if (device == null) {
                error = XYBluetoothError("connectGatt: No Device")
            } else {
                var gatt = this@XYBluetoothGatt.gatt

                if (callback != null) {
                    addGattListener("default", callback)
                }
                if (gatt == null) {
                    CallByVersion()
                            .add(Build.VERSION_CODES.O) {
                                connectGatt26(device, autoConnect, transport, phy, handler)
                            }
                            .add(Build.VERSION_CODES.M) {
                                connectGatt23(device, autoConnect, transport)
                            }
                            .add(Build.VERSION_CODES.KITKAT) {
                                connectGatt19(device, autoConnect)
                            }.call()
                    gatt = this@XYBluetoothGatt.gatt
                    if (gatt == null) {
                        error = XYBluetoothError("connectGatt: Failed to get gatt")
                    }
                } else {
                    value = true
                }
            }
            return@asyncBle XYBluetoothResult(value, error)
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private fun connectGatt19(device: BluetoothDevice,
               autoConnect: Boolean) {
        logInfo("connectGatt19")
        gatt = device.connectGatt(context, autoConnect, centralCallback)
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun connectGatt23(device: BluetoothDevice,
               autoConnect: Boolean,
               transport: Int?) {
        logInfo("connectGatt23")
        if (transport == null) {
            gatt = device.connectGatt(context, autoConnect, centralCallback)
        } else {
            gatt = device.connectGatt(context, autoConnect, centralCallback, transport)
        }
    }
    @TargetApi(Build.VERSION_CODES.O)
    private fun connectGatt26(device: BluetoothDevice,
               autoConnect: Boolean,
               transport: Int?,
               phy: Int?,
               handler: Handler?) {
        logInfo("connectGatt26")
        if (transport == null) {
            gatt = device.connectGatt(context, autoConnect, centralCallback)
        } else if (phy == null){
            gatt = device.connectGatt(context, autoConnect, centralCallback, transport)
        } else if (handler == null) {
            gatt = device.connectGatt(context, autoConnect, centralCallback, transport, phy)
        } else {
            gatt = device.connectGatt(context, autoConnect, centralCallback, transport, phy, handler)
        }
    }

    companion object {

        val TAG = XYBluetoothGatt.javaClass.simpleName

        //gap after last connection that we wait to close the connection
        private const val CLEANUP_DELAY = 5000

        //this is the thread that all calls should happen on for gatt calls.  Using a single thread
        //it is documented that for 4.4, we should consider using the UIThread
        val GattThread = XYFilteredSmartScan.BluetoothThread

        private val WAIT_RESOLUTION = 100
        private val CONNECT_TIMEOUT = 15000
        private val SAFE_DELAY = 100 //this is how long we pause between actions to prevent 133 errors

        val CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        private fun safeCreateGatt(context:Context, device: BluetoothDevice, callback: BluetoothGattCallback?) : Deferred<XYBluetoothGatt> {
            return async(GattThread) {
                logInfo(TAG, "safeCreateGatt")
                return@async XYBluetoothGatt(context.applicationContext, device, false, callback, null, null, null)
            }
        }
    }
}