package com.xyfindables.sdk.gatt

import android.annotation.TargetApi
import android.bluetooth.*
import android.content.Context
import android.os.Build
import android.os.Handler
import com.xyfindables.core.XYBase
import com.xyfindables.sdk.CallByVersion
import com.xyfindables.sdk.XYBluetoothBase
import com.xyfindables.sdk.scanner.XYFilteredSmartScan
import kotlinx.coroutines.experimental.*
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.HashMap
import kotlin.coroutines.experimental.suspendCoroutine

open class XYBluetoothGatt protected constructor(
        context:Context,
        protected var device: BluetoothDevice?,
        private var autoConnect: Boolean,
        private val callback: BluetoothGattCallback?,
        private val transport: Int?,
        private val phy: Int?,
        private val handler: Handler?
) : XYBluetoothBase(context) {

    protected var gatt: BluetoothGatt? = null

    private val gattListeners = HashMap<String, BluetoothGattCallback>()

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

    //last time this device was accessed (connected to)
    var lastAccessTime = 0L

    //last time we heard a ad from this device
    var lastAdTime = 0L

    fun updateBluetoothDevice(device: BluetoothDevice?) {
        this.device = device
    }

    protected val connectionState : Int?
        get() = bluetoothManager?.getConnectionState(device, BluetoothProfile.GATT)

    val closed: Boolean
        get() = (gatt == null)

    fun addGattListener(key: String, listener: BluetoothGattCallback) {
        logInfo("addGattListener: $key")
        synchronized(gattListeners) {
            gattListeners[key] = listener
        }
    }

    fun removeGattListener(key: String) {
        logInfo("removeGattListener: $key")
        synchronized(gattListeners) {
            gattListeners.remove(key)
        }
    }

    fun asyncClose() : Deferred<XYBluetoothResult<Boolean>> {
        return asyncBle {
            logInfo("asyncClose")
            asyncDisconnect().await()
            logInfo("asyncClose: Disconnected")
            gatt?.close()
            logInfo("asyncClose: Closed")
            removeGattListener("default")
            gatt = null
            return@asyncBle XYBluetoothResult(true)
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

                val listenerName = "asyncDiscover$nowNano"
                if (gatt.services.size == 0) {
                    value = suspendCoroutine { cont ->
                        logInfo("asyncDiscover:CoRoutine")
                        val listener = object : BluetoothGattCallback() {
                            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                                super.onServicesDiscovered(gatt, status)
                                if (status != BluetoothGatt.GATT_SUCCESS) {
                                    error = XYBluetoothError("asyncDiscover: discoverStatus: $status")
                                    cont.resume(null)
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
                        addGattListener(listenerName, listener)
                        if (!gatt.discoverServices()) {
                            error = XYBluetoothError("asyncDiscover: gatt.discoverServices failed to start")
                            cont.resume(null)
                        }
                        if (connectionState != BluetoothGatt.STATE_CONNECTED) {
                            error = XYBluetoothError("asyncWriteCharacteristic: connection dropped 2")
                            cont.resume(null)
                        }
                    }
                    removeGattListener(listenerName)
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
                val listenerName = "asyncWriteCharacteristic$nowNano"
                var listener : BluetoothGattCallback? = null
                value = suspendCoroutine { cont ->
                    listener = object : BluetoothGattCallback() {
                        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
                            logInfo("onCharacteristicWrite: $status")
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
                            logInfo("onCharacteristicWrite")
                            super.onConnectionStateChange(gatt, status, newState)
                            if (newState != BluetoothGatt.STATE_CONNECTED) {
                                error = XYBluetoothError("asyncWriteCharacteristic: connection dropped")
                                cont.resume(null)
                            }
                        }
                    }
                    addGattListener(listenerName, listener!!)
                    if (!gatt.writeCharacteristic(characteristicToWrite)) {
                        error = XYBluetoothError("asyncWriteCharacteristic: gatt.writeCharacteristic failed to start")
                        cont.resume(null)
                    }
                    if (connectionState != BluetoothGatt.STATE_CONNECTED) {
                        error = XYBluetoothError("asyncWriteCharacteristic: connection dropped 2")
                        cont.resume(null)
                    }
                }
                removeGattListener(listenerName)
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
                val listenerName = "asyncReadCharacteristicInt$nowNano"
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
                    addGattListener(listenerName, listener)
                    if (!gatt.readCharacteristic(characteristicToRead)) {
                        error = XYBluetoothError("asyncReadCharacteristicInt: gatt.readCharacteristic failed to start")
                        cont.resume(null)
                    }
                    if (connectionState != BluetoothGatt.STATE_CONNECTED) {
                        error = XYBluetoothError("asyncWriteCharacteristic: connection dropped 2")
                        cont.resume(null)
                    }
                }
                removeGattListener(listenerName)
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
                val listenerName = "asyncReadCharacteristicString$nowNano"
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
                    addGattListener(listenerName, listener)
                    if (!gatt.readCharacteristic(characteristicToRead)) {
                        error = XYBluetoothError("asyncReadCharacteristicString: gatt.readCharacteristic failed to start")
                        cont.resume(null)
                    }
                    if (connectionState != BluetoothGatt.STATE_CONNECTED) {
                        error = XYBluetoothError("asyncWriteCharacteristic: connection dropped 2")
                        cont.resume(null)
                    }
                }
                removeGattListener(listenerName)
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
                val listenerName = "asyncReadCharacteristicFloat$nowNano"
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
                    addGattListener(listenerName, listener)
                    if (!gatt.readCharacteristic(characteristicToRead)) {
                        error = XYBluetoothError("asyncReadCharacteristicString: gatt.readCharacteristic failed to start")
                        cont.resume(null)
                    }
                    if (connectionState != BluetoothGatt.STATE_CONNECTED) {
                        error = XYBluetoothError("asyncWriteCharacteristic: connection dropped 2")
                        cont.resume(null)
                    }
                }
                removeGattListener(listenerName)
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
                val listenerName = "asyncReadCharacteristicBytes$nowNano"
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
                    addGattListener(listenerName, listener)
                    if (!gatt.readCharacteristic(characteristicToRead)) {
                        error = XYBluetoothError("asyncReadCharacteristicBytes: gatt.readCharacteristic failed to start")
                        cont.resume(null)
                    }
                    if (connectionState != BluetoothGatt.STATE_CONNECTED) {
                        error = XYBluetoothError("asyncWriteCharacteristic: connection dropped 2")
                        cont.resume(null)
                    }
                }
                removeGattListener(listenerName)
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
                val listenerName = "asyncConnect$nowNano"
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
                                //error = XYBluetoothError("asyncConnect: connection failed(state): $status : $newState")
                                //cont.resume(null)
                            }
                        }
                    }
                    addGattListener(listenerName, listener)

                    if (connectionState == BluetoothGatt.STATE_CONNECTED) {
                        logInfo("asyncConnect:already connected")
                        cont.resume(true)
                    } else if (connectionState == BluetoothGatt.STATE_CONNECTING) {
                        logInfo("asyncConnect:connecting")
                        //dont call connect since already in progress
                    } else if (!gatt.connect()) {
                        error = XYBluetoothError("asyncConnect: gatt.readCharacteristic failed to start")
                        cont.resume(null)
                    }
                }
                removeGattListener(listenerName)
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

    fun asyncDisconnect() : Deferred<XYBluetoothResult<Boolean>>{
        return asyncBle {
            logInfo("asyncDisconnect")
            var error: XYBluetoothError? = null
            var value: Boolean? = null

            val gatt = this@XYBluetoothGatt.gatt

            if (gatt == null) {
                error = XYBluetoothError("asyncDisconnect: No Gatt")
            }

            if (gatt != null) {
                val listenerName = "asyncDisconnect$nowNano"
                value = suspendCoroutine { cont ->
                    val listener = object : BluetoothGattCallback() {
                        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                            super.onConnectionStateChange(gatt, status, newState)
                            if (status == BluetoothGatt.GATT_FAILURE) {
                                error = XYBluetoothError("asyncDisconnect: disconnection failed(status): $status : $newState")
                                cont.resume(null)
                            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                                cont.resume(true)
                            } else if (newState == BluetoothGatt.STATE_DISCONNECTING) {
                                //wait some more
                            } else {
                                //error = XYBluetoothError("asyncDisconnect: connection failed(state): $status : $newState")
                                //cont.resume(null)
                            }
                        }
                    }
                    addGattListener(listenerName, listener)

                    if (connectionState == BluetoothGatt.STATE_DISCONNECTED) {
                        logInfo("asyncDisconnect:already disconnected")
                        cont.resume(true)
                    } else if (connectionState == BluetoothGatt.STATE_DISCONNECTING) {
                        logInfo("asyncDisconnect:disconnecting")
                        //dont call connect since already in progress
                    } else {
                        logInfo("asyncDisconnect:starting disconnect")
                        gatt.disconnect()
                    }
                }
                removeGattListener(listenerName)
            }
            return@asyncBle XYBluetoothResult(value, error)
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
                    val foundService = gatt.getService(service)
                    logInfo("findCharacteristic:service:$foundService")
                    if (foundService != null) {
                        val foundCharacteristic = foundService.getCharacteristic(characteristic)
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
                    launch(CommonPool) {
                        logInfo("onCharacteristicChanged: $key")
                        listener.onCharacteristicChanged(gatt, characteristic)
                    }
                }
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            logInfo("onCharacteristicRead: $characteristic : $status")
            synchronized(gattListeners) {
                for((_, listener) in gattListeners) {
                    launch(CommonPool) {
                        listener.onCharacteristicRead(gatt, characteristic, status)
                    }
                }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            logInfo("onCharacteristicWrite: $status")
            super.onCharacteristicWrite(gatt, characteristic, status)
            synchronized(gattListeners) {
                logInfo("onCharacteristicWrite3: $status")
                for((_, listener) in gattListeners) {
                    launch(CommonPool) {
                        listener.onCharacteristicWrite(gatt, characteristic, status)
                    }
                }
            }
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            logInfo("onConnectionStateChange: ${gatt?.device?.address} $newState : $status")
            synchronized(gattListeners) {
                for ((_, listener) in gattListeners) {
                    launch(CommonPool) {
                        listener.onConnectionStateChange(gatt, status, newState)
                    }
                }
            }
        }

        override fun onDescriptorRead(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            super.onDescriptorRead(gatt, descriptor, status)
            logInfo("onDescriptorRead: $descriptor : $status")
            synchronized(gattListeners) {
                for((_, listener) in gattListeners) {
                    launch(CommonPool) {
                        listener.onDescriptorRead(gatt, descriptor, status)
                    }
                }
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            logInfo("onDescriptorWrite: $descriptor : $status")
            synchronized(gattListeners) {
                for((_, listener) in gattListeners) {
                    launch(CommonPool) {
                        listener.onDescriptorWrite(gatt, descriptor, status)
                    }
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            logInfo("onMtuChanged: $mtu : $status")
            synchronized(gattListeners) {
                for((_, listener) in gattListeners) {
                    launch(CommonPool) {
                        @Suppress()
                        listener.onMtuChanged(gatt, mtu, status)
                    }
                }
            }
        }

        override fun onPhyRead(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            super.onPhyRead(gatt, txPhy, rxPhy, status)
            logInfo("onPhyRead: $txPhy : $rxPhy : $status")
            synchronized(gattListeners) {
                for((_, listener) in gattListeners) {
                    launch(CommonPool) {
                        @Suppress()
                        listener.onPhyRead(gatt, txPhy, rxPhy, status)
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
                    launch(CommonPool) {
                        @Suppress()
                        listener.onPhyUpdate(gatt, txPhy, rxPhy, status)
                    }
                }
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            super.onReadRemoteRssi(gatt, rssi, status)
            logInfo("onReadRemoteRssi: $rssi : $status")
            synchronized(gattListeners) {
                for((_, listener) in gattListeners) {
                    launch(CommonPool) {
                        listener.onReadRemoteRssi(gatt, rssi, status)
                    }
                }
            }
        }

        override fun onReliableWriteCompleted(gatt: BluetoothGatt?, status: Int) {
            super.onReliableWriteCompleted(gatt, status)
            logInfo("onReliableWriteCompleted: $status")
            synchronized(gattListeners) {
                for((_, listener) in gattListeners) {
                    launch(CommonPool) {
                        listener.onReliableWriteCompleted(gatt, status)
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            logInfo("onServicesDiscovered: $status")
            synchronized(gattListeners) {
                for((_, listener) in gattListeners) {
                    launch(CommonPool) {
                        listener.onServicesDiscovered(gatt, status)
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
                                gatt = connectGatt26(device, autoConnect, transport, phy, handler)
                            }
                            .add(Build.VERSION_CODES.M) {
                                gatt = connectGatt23(device, autoConnect, transport)
                            }
                            .add(Build.VERSION_CODES.KITKAT) {
                                gatt = connectGatt19(device, autoConnect)
                            }.call()
                    this@XYBluetoothGatt.gatt = gatt
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
               autoConnect: Boolean) : BluetoothGatt? {
        logInfo("connectGatt19")
        return device.connectGatt(context, autoConnect, centralCallback)
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun connectGatt23(device: BluetoothDevice,
               autoConnect: Boolean,
               transport: Int?) : BluetoothGatt? {
        logInfo("connectGatt23")
        if (transport == null) {
            return device.connectGatt(context, autoConnect, centralCallback)
        } else {
            return device.connectGatt(context, autoConnect, centralCallback, transport)
        }
    }
    @TargetApi(Build.VERSION_CODES.O)
    private fun connectGatt26(device: BluetoothDevice,
               autoConnect: Boolean,
               transport: Int?,
               phy: Int?,
               handler: Handler?) : BluetoothGatt? {
        logInfo("connectGatt26")
        if (transport == null) {
            return device.connectGatt(context, autoConnect, centralCallback)
        } else if (phy == null){
            return  device.connectGatt(context, autoConnect, centralCallback, transport)
        } else if (handler == null) {
            return device.connectGatt(context, autoConnect, centralCallback, transport, phy)
        } else {
            return device.connectGatt(context, autoConnect, centralCallback, transport, phy, handler)
        }
    }

    companion object {
        //gap after last connection that we wait to close the connection
        private const val CLEANUP_DELAY = 5000
    }
}