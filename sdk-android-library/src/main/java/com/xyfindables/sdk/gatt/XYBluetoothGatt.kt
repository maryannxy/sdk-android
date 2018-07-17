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

open class XYBluetoothGatt protected constructor(
        private val context:Context,
        protected var device: BluetoothDevice,
        private val autoConnect: Boolean,
        private val callback: BluetoothGattCallback?,
        private val transport: Int?,
        private val phy: Int?,
        private val handler: Handler?
) : XYBase() {

    protected var gatt: BluetoothGatt? = null

    private val gattListeners = HashMap<String, WeakReference<BluetoothGattCallback>>()

    private val bluetoothManager = getBluetoothManager(context)

    val connectionState: Int
        get() {
            return _connectionState
        }

    fun updateBluetoothDevice(device: BluetoothDevice) {
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

    private fun safeDiscoverServices() : Deferred<Boolean?> {
        return async(GattThread) {
            logInfo("safeDiscoverServices")
            return@async gatt?.discoverServices()
        }
    }

    private fun safeWriteCharacteristic(characteristic: BluetoothGattCharacteristic) : Deferred<Boolean?> {
        return async(GattThread) {
            logInfo("safeWriteCharacteristic")
            return@async gatt?.writeCharacteristic(characteristic)
        }
    }

    private fun safeWriteDescriptor(descriptor: BluetoothGattDescriptor) : Deferred<Boolean?> {
        return async(GattThread) {
            logInfo("safeWriteDescriptor")
            return@async gatt?.writeDescriptor(descriptor)
        }
    }

    private fun safeReadCharacteristic(characteristic: BluetoothGattCharacteristic) : Deferred<Boolean?> {
        return async(GattThread) {
            logInfo("safeReadCharacteristic")
            val result = gatt?.readCharacteristic(characteristic)
            logInfo("safeReadCharacteristic: $result")
            return@async result
        }
    }

    private fun safeSetCharacteristicNotification(characteristic: BluetoothGattCharacteristic, enable: Boolean) : Deferred<Boolean?> {
        return async(GattThread) {
            logInfo("safeSetCharacteristicNotification")
            val result = gatt?.setCharacteristicNotification(characteristic, enable)
            if (result == null) {
                logError("safeSetCharacteristicNotification did not Complete", false)
            } else if (!result) {
                logError("safeSetCharacteristicNotification Failed", false)
            } else {
                logInfo("safeSetCharacteristicNotification Success")
            }
            return@async result
        }
    }

    private fun safeReadDescriptor(descriptor: BluetoothGattDescriptor) : Deferred<Boolean?> {
        return async(GattThread) {
            logInfo("safeReadDescriptor")
            return@async gatt?.readDescriptor(descriptor)
        }
    }

    private fun safeAbortReliableWrite() : Deferred<Unit> {
        return async(GattThread) {
            logInfo("safeAbortReliableWrite")
            gatt?.abortReliableWrite()
            return@async
        }
    }

    private fun safeBeginReliableWrite() : Deferred<Boolean?> {
        return async(GattThread) {
            logInfo("safeBeginReliableWrite")
            return@async gatt?.beginReliableWrite()
        }
    }

    private fun safeExecuteBeginReliableWrite() : Deferred<Boolean?> {
        return async(GattThread) {
            logInfo("safeExecuteBeginReliableWrite")
            return@async gatt?.executeReliableWrite()
        }
    }

    private fun safeGetService(uuid: UUID) : Deferred<BluetoothGattService?> {
        return async(GattThread) {
            logInfo("safeGetService")
            return@async gatt?.getService(uuid)
        }
    }

    private fun safeGetServices() : Deferred<List<BluetoothGattService>?> {
        return async(GattThread) {
            logInfo("safeGetServices")
            return@async gatt?.getServices()
        }
    }

    @TargetApi(26)
    private fun safeReadPhy() : Deferred<Unit> {
        return async(GattThread) {
            logInfo("safeReadPhy")
            gatt?.readPhy()
            return@async
        }
    }

    @TargetApi(26)
    private fun safeSetPreferredPhy(txPhy: Int, rxPhy: Int, phyOptions: Int) : Deferred<Unit> {
        return async(GattThread) {
            logInfo("safeSetPreferredPhy")
            gatt?.setPreferredPhy(txPhy, rxPhy, phyOptions)
            return@async
        }
    }

    private fun safeReadRemoteRssi() : Deferred<Boolean?> {
        return async(GattThread) {
            logInfo("safeReadRemoteRssi")
            return@async gatt?.readRemoteRssi()
        }
    }

    @TargetApi(21)
    private fun safeRequestConnectionPriority(connectionPriority: Int) : Deferred<Boolean?> {
        return async(GattThread) {
            logInfo("safeRequestConnectionPriority")
            return@async gatt?.requestConnectionPriority(connectionPriority)
        }
    }

    @TargetApi(21)
    private fun safeRequestMtu(mtu: Int) : Deferred<Boolean?> {
        return async(GattThread) {
            logInfo("safeRequestConnectionPriority")
            return@async gatt?.requestMtu(mtu)
        }
    }

    fun asyncDiscover() : Deferred<Boolean?>{
        val gatt = gatt
        return async(CommonPool) {
            if (gatt == null) {
                logError("asyncDiscover: null gatt!", false)
                return@async null
            }

            logInfo("asyncDiscover:$gatt.services.size")
            if (gatt.services.size > 0) {
                return@async true
            } else {
                var discoverStatus = -1
                addGattListener("asyncDiscover", object:BluetoothGattCallback() {
                    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                        super.onServicesDiscovered(gatt, status)
                        logInfo("onServicesDiscovered:$status")
                        discoverStatus = status
                    }
                })

                var result = safeDiscoverServices().await()

                if (result != null && result) {
                    //this assumes that we will *always* get a call back from the call, even if it is a failure
                    while(discoverStatus == -1) {
                        delay(WAIT_RESOLUTION)
                    }
                    result = BluetoothGatt.GATT_SUCCESS == discoverStatus
                }

                removeGattListener("asyncDiscover")
                delay(SAFE_DELAY)
                return@async result

            }
        }
    }

    fun asyncWriteCharacteristic(characteristicToWrite: BluetoothGattCharacteristic) : Deferred<Boolean?>{
        return async(CommonPool) {
            logInfo("asyncWriteCharacteristic")
            var writeStatus = -1

            addGattListener("asyncWriteCharacteristic", object:BluetoothGattCallback() {
                override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
                    super.onCharacteristicWrite(gatt, characteristic, status)
                    //since it is always possible to have a rogue callback, make sure it is the one we are looking for
                    if (characteristicToWrite == characteristic) {
                        writeStatus = status
                    }
                }
            })

            var result = safeWriteCharacteristic(characteristicToWrite).await()

            if (result != null && result) {
                //this assumes that we will *always* get a call back from the call, even if it is a failure
                while(writeStatus == -1) {
                    delay(WAIT_RESOLUTION)
                }
                result = BluetoothGatt.GATT_SUCCESS == writeStatus
            }

            removeGattListener("asyncWriteCharacteristic: $result")
            delay(SAFE_DELAY)
            return@async result
        }
    }

    fun asyncReadCharacteristicInt(characteristicToRead: BluetoothGattCharacteristic, formatType:Int, offset:Int) : Deferred<Int?>{
        return async(CommonPool) {
            logInfo("asyncReadCharacteristicInt")
            var readStatus = -1
            addGattListener("asyncReadCharacteristicInt", object:BluetoothGattCallback() {
                override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
                    super.onCharacteristicRead(gatt, characteristic, status)
                    //since it is always possible to have a rogue callback, make sure it is the one we are looking for
                    if (characteristicToRead == characteristic) {
                        readStatus = status
                    }
                }
            })

            val readTriggered = safeReadCharacteristic(characteristicToRead).await()

            if (readTriggered != null && readTriggered) {
                //this assumes that we will *always* get a call back from the call, even if it is a failure
                while(readStatus == -1) {
                    delay(WAIT_RESOLUTION)
                }
            }

            removeGattListener("asyncReadCharacteristicInt: $readStatus")

            if (readStatus == BluetoothGatt.GATT_SUCCESS) {
                delay(SAFE_DELAY)
                return@async characteristicToRead.getIntValue(formatType, offset)
            } else {
                return@async null
            }
        }
    }

    fun asyncReadCharacteristicString(characteristicToRead: BluetoothGattCharacteristic, offset:Int) : Deferred<String?>{
        return async(CommonPool) {
            logInfo("asyncReadCharacteristicString")
            var readStatus = -1
            addGattListener("asyncReadCharacteristicString", object:BluetoothGattCallback() {
                override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
                    super.onCharacteristicRead(gatt, characteristic, status)
                    //since it is always possible to have a rogue callback, make sure it is the one we are looking for
                    if (characteristicToRead == characteristic) {
                        readStatus = status
                    }
                }
            })

            val readTriggered = safeReadCharacteristic(characteristicToRead).await()

            if (readTriggered != null && readTriggered) {
                //this assumes that we will *always* get a call back from the call, even if it is a failure
                while(readStatus == -1) {
                    delay(WAIT_RESOLUTION)
                }
            }

            removeGattListener("asyncReadCharacteristicInt: $readStatus")

            if (readStatus == BluetoothGatt.GATT_SUCCESS) {
                delay(SAFE_DELAY)
                return@async characteristicToRead.getStringValue(offset)
            } else {
                return@async null
            }
        }
    }

    fun asyncReadCharacteristicFloat(characteristicToRead: BluetoothGattCharacteristic, formatType:Int, offset:Int) : Deferred<Float?>{
        return async(CommonPool) {
            logInfo("asyncReadCharacteristicInt")
            var readStatus = -1
            addGattListener("asyncReadCharacteristicInt", object:BluetoothGattCallback() {
                override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
                    super.onCharacteristicRead(gatt, characteristic, status)
                    //since it is always possible to have a rogue callback, make sure it is the one we are looking for
                    if (characteristicToRead == characteristic) {
                        readStatus = status
                    }
                }
            })

            val readTriggered = safeReadCharacteristic(characteristicToRead).await()

            if (readTriggered != null && readTriggered) {
                //this assumes that we will *always* get a call back from the call, even if it is a failure
                while(readStatus == -1) {
                    delay(WAIT_RESOLUTION)
                }
            }

            removeGattListener("asyncReadCharacteristicInt: $readStatus")

            if (readStatus == BluetoothGatt.GATT_SUCCESS) {
                delay(SAFE_DELAY)
                return@async characteristicToRead.getFloatValue(formatType, offset)
            } else {
                return@async null
            }
        }
    }

    fun asyncReadCharacteristicBytes(characteristicToRead: BluetoothGattCharacteristic) : Deferred<ByteArray?>{
        return async(CommonPool) {
            logInfo("asyncReadCharacteristicBytes")
            var readStatus = -1
            addGattListener("asyncReadCharacteristicBytes", object:BluetoothGattCallback() {
                override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
                    super.onCharacteristicRead(gatt, characteristic, status)
                    //since it is always possible to have a rogue callback, make sure it is the one we are looking for
                    if (characteristicToRead == characteristic) {
                        logInfo("asyncReadCharacteristicBytes: $status")
                        readStatus = status
                    }
                }
            })

            val readTriggered = safeReadCharacteristic(characteristicToRead).await()

            logInfo("asyncReadCharacteristicBytes: Read Triggered: $readTriggered")

            if (readTriggered != null && readTriggered) {
                //this assumes that we will *always* get a call back from the call, even if it is a failure
                while(readStatus == -1) {
                    delay(WAIT_RESOLUTION)
                }
                logInfo("asyncReadCharacteristicBytes: Complete: $readStatus")
            } else {
                logError("asyncReadCharacteristicBytes: Read Not Triggered", false)
            }

            removeGattListener("asyncReadCharacteristicBytes")

            delay(SAFE_DELAY)

            return@async characteristicToRead.value
        }
    }

    fun asyncFindAndReadCharacteristicInt(service: UUID, characteristic: UUID, formatType:Int, offset:Int) : Deferred<Int?> {
        return async(CommonPool) {
            logInfo("asyncFindAndReadCharacteristicInt")
            var value: Int? = null
            val characteristicToRead = asyncFindCharacteristic(service, characteristic).await()
            if (characteristicToRead != null) {
                value = asyncReadCharacteristicInt(characteristicToRead, formatType, offset).await()
                delay(SAFE_DELAY)
            }
            return@async value
        }
    }

    fun asyncFindAndReadCharacteristicFloat(service: UUID, characteristic: UUID, formatType:Int, offset:Int) : Deferred<Float?> {
        return async(CommonPool) {
            logInfo("asyncFindAndReadCharacteristicFloat")
            var value: Float? = null
            val characteristicToRead = asyncFindCharacteristic(service, characteristic).await()
            if (characteristicToRead != null) {
                value = asyncReadCharacteristicFloat(characteristicToRead, formatType, offset).await()
                delay(SAFE_DELAY)
            }
            return@async value
        }
    }

    fun asyncFindAndReadCharacteristicString(service: UUID, characteristic: UUID, offset:Int) : Deferred<String?> {
        return async(CommonPool) {
            logInfo("asyncFindAndReadCharacteristicString")
            var value: String? = null
            val characteristicToRead = asyncFindCharacteristic(service, characteristic).await()
            if (characteristicToRead != null) {
                value = asyncReadCharacteristicString(characteristicToRead, offset).await()
                delay(SAFE_DELAY)
            }
            return@async value
        }
    }

    fun asyncFindAndReadCharacteristicBytes(service: UUID, characteristic: UUID) : Deferred<ByteArray?> {
        return async(CommonPool) {
            logInfo("asyncFindAndReadCharacteristicBytes")
            var value: ByteArray? = null
            val characteristicToRead = asyncFindCharacteristic(service, characteristic).await()
            if (characteristicToRead != null) {
                value = asyncReadCharacteristicBytes(characteristicToRead).await()
                delay(SAFE_DELAY)
            }
            return@async value
        }
    }

    fun asyncFindAndWriteCharacteristic(service: UUID, characteristic: UUID, value:Int, formatType:Int, offset:Int) : Deferred<Boolean?> {
        return async(CommonPool) {
            logInfo("asyncFindAndWriteCharacteristic")
            var success : Boolean? = null
            val characteristicToWrite = asyncFindCharacteristic(service, characteristic).await()
            if (characteristicToWrite != null) {
                if (characteristicToWrite.setValue(value, formatType, offset)) {
                    success = asyncWriteCharacteristic(characteristicToWrite).await()
                    delay(SAFE_DELAY)
                } else {
                    logError("asyncFindAndWriteCharacteristic: failed to call device", false)
                    success = false
                }
            }
            return@async success
        }
    }

    fun asyncFindAndWriteCharacteristicFloat(service: UUID, characteristic: UUID, mantissa: Int, exponent: Int, formatType:Int, offset:Int) : Deferred<Boolean?> {
        return async(CommonPool) {
            logInfo("asyncFindAndWriteCharacteristic")
            var success : Boolean? = null
            val characteristicToWrite = asyncFindCharacteristic(service, characteristic).await()
            if (characteristicToWrite != null) {
                if (characteristicToWrite.setValue(mantissa, exponent, formatType, offset)) {
                    success = asyncWriteCharacteristic(characteristicToWrite).await()
                    delay(SAFE_DELAY)
                } else {
                    logError("asyncFindAndWriteCharacteristic: failed to call device", false)
                    success = false
                }
            }
            return@async success
        }
    }

    fun asyncFindAndWriteCharacteristic(service: UUID, characteristic: UUID, value:String) : Deferred<Boolean?> {
        return async(CommonPool) {
            logInfo("asyncFindAndWriteCharacteristic")
            var success : Boolean? = null
            val characteristicToWrite = asyncFindCharacteristic(service, characteristic).await()
            if (characteristicToWrite != null) {
                if (characteristicToWrite.setValue(value)) {
                    success = asyncWriteCharacteristic(characteristicToWrite).await()
                    delay(SAFE_DELAY)
                } else {
                    logError("asyncFindAndWriteCharacteristic: failed to call device", false)
                    success = false
                }
            }
            return@async success
        }
    }

    fun asyncFindAndWriteCharacteristic(service: UUID, characteristic: UUID, bytes:ByteArray) : Deferred<Boolean?> {
        return async(CommonPool) {
            logInfo("asyncFindAndWriteCharacteristic")
            var success : Boolean? = null
            val characteristicToWrite = asyncFindCharacteristic(service, characteristic).await()
            if (characteristicToWrite != null) {
                if (characteristicToWrite.setValue(bytes)) {
                    success = asyncWriteCharacteristic(characteristicToWrite).await()
                    delay(SAFE_DELAY)
                } else {
                    logError("asyncFindAndWriteCharacteristic: failed to call device", false)
                    success = false
                }
            }
            return@async success
        }
    }

    fun asyncFindAndWriteCharacteristicNotify(service: UUID, characteristic: UUID, enable:Boolean) : Deferred<Boolean?> {
        return async(CommonPool) {
            logInfo("asyncFindAndWriteCharacteristicNotify")
            var success : Boolean? = null
            val characteristicToWriteNotify = asyncFindCharacteristic(service, characteristic).await()
            if (characteristicToWriteNotify != null) {
                logInfo("asyncFindAndWriteCharacteristicNotify: Found Characteristic")
                success = safeSetCharacteristicNotification(characteristicToWriteNotify, enable).await()
                val descriptor = characteristicToWriteNotify.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
                descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                success = safeWriteDescriptor(descriptor).await()
                logInfo("asyncFindAndWriteCharacteristicNotify: $success")
            }
            return@async success
        }
    }

    private fun getBluetoothManager(context: Context): BluetoothManager {
        return context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    fun asyncConnect(timeout: Int?) : Deferred<Boolean>{
        return async(CommonPool) {
            logInfo("asyncConnect")
            var remainingTimeout = timeout ?: CONNECT_TIMEOUT
            logInfo("asyncConnect:async")
            if (connectionState == BluetoothGatt.STATE_CONNECTED) {
                logInfo("asyncConnect:already connected")
                return@async true
            }

            if (connectionState != BluetoothGatt.STATE_CONNECTING) {
                logInfo("asyncConnect:connecting")
                safeConnect().await()
            }

            //this has to be on new thread to allow BT to process
            return@async async(CommonPool) {
                while (connectionState != BluetoothGatt.STATE_CONNECTED) {
                    delay(WAIT_RESOLUTION)
                    remainingTimeout -= WAIT_RESOLUTION
                    if (remainingTimeout <= 0) {
                        logError("asyncConnect: Connection Timeout!", false)
                        asyncDisconnect().await()
                        return@async false
                    }
                }
                logInfo("asyncConnect:Complete")
                delay(SAFE_DELAY)
                return@async true
            }.await()
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
    private fun asyncFindCharacteristic(service: UUID, characteristic: UUID) : Deferred<BluetoothGattCharacteristic?> {

        //error and throw exception if discovery not done yet
        if (gatt?.services?.size == 0) {
            logError("Services Not Discovered Yet", true)
        }

        return async(CommonPool) {
            logInfo("findCharacteristic")
            var foundCharacteristic : BluetoothGattCharacteristic? = null
            val foundService = gatt?.getService(service)
            logInfo("findCharacteristic:service:$foundService")
            if (foundService != null) {
                foundCharacteristic = foundService.getCharacteristic(characteristic)
                logInfo("findCharacteristic:characteristic:$foundCharacteristic")
            }
            return@async foundCharacteristic
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

    fun connectGatt() : Deferred<Boolean> {
        if (callback != null) {
            addGattListener("default", callback)
        }
        return async(CommonPool) {
            logInfo("connectGatt")
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
                if (gatt == null) {
                    logError("Failed to connection Gatt!", true)
                    return@async false
                }
            }
            return@async asyncConnect(CONNECT_TIMEOUT).await()
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