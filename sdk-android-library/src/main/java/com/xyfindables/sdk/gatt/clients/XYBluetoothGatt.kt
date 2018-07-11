package com.xyfindables.sdk.gatt.clients

import android.annotation.TargetApi
import android.bluetooth.*
import android.content.Context
import android.os.Build
import android.os.Handler
import com.xyfindables.core.XYBase
import com.xyfindables.sdk.UIThread
import com.xyfindables.sdk.XYCallByVersion
import com.xyfindables.sdk.scanner.XYFilteredSmartScan
import kotlinx.coroutines.experimental.*
import java.util.*
import kotlin.collections.HashMap

open class XYBluetoothGatt protected constructor(
        val context:Context,
        val device: BluetoothDevice,
        val autoConnect: Boolean,
        val callback: BluetoothGattCallback?,
        val transport: Int?,
        val phy: Int?,
        val handler: Handler?
) : XYBase() {

    protected val gatt: BluetoothGatt
        get() {
            return _gatt!!
        }

    private var _gatt: BluetoothGatt? = null

    private val gattListeners = HashMap<String, BluetoothGattCallback>()

    private val bluetoothManager = getBluetoothManager(context)

    val connectionState: Int
        get() {
            return _connectionState
        }

    private var _connectionState = bluetoothManager.getConnectionState(device, BluetoothProfile.GATT)

    val closed: Boolean
        get() {
            return (_gatt == null)
        }

    fun addGattListener(key: String, listener: BluetoothGattCallback) {
        logInfo("addListener")
        synchronized(gattListeners) {
            gattListeners[key] = listener
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
            safeClose()
            deviceToGattMap.remove(device.hashCode())
            removeGattListener("default")
            _gatt = null
            return@async
        }
    }

    //it is recommended that all gatt calls are done in the same thread, and in the uithread for 4.4
    private fun safeConnect() : Deferred<Boolean> {
        return async(GattThread) {
            logInfo("safeConnect")
            return@async gatt.connect()
        }
    }

    //it is recommended that all gatt calls are done in the same thread, and in the uithread for 4.4
    private fun safeDisconnect() : Deferred<Unit> {
        return async(GattThread) {
            logInfo("safeDisconnect")
            gatt.disconnect()
            return@async
        }
    }

    //it is recommended that all gatt calls are done in the same thread, and in the uithread for 4.4
    private fun safeClose() : Deferred<Unit> {
        return async(GattThread) {
            logInfo("safeClose")
            gatt.close()
            return@async
        }
    }

    //it is recommended that all gatt calls are done in the same thread, and in the uithread for 4.4
    private fun safeDiscoverServices() : Deferred<Boolean> {
        return async(GattThread) {
            logInfo("safeDiscoverServices")
            return@async gatt.discoverServices()
        }
    }

    //it is recommended that all gatt calls are done in the same thread, and in the uithread for 4.4
    private fun safeWriteCharacteristic(characteristic: BluetoothGattCharacteristic) : Deferred<Boolean> {
        return async(GattThread) {
            logInfo("safeWriteCharacteristic")
            return@async gatt.writeCharacteristic(characteristic)
        }
    }

    //it is recommended that all gatt calls are done in the same thread, and in the uithread for 4.4
    private fun safeReadCharacteristic(characteristic: BluetoothGattCharacteristic) : Deferred<Boolean> {
        return async(GattThread) {
            logInfo("safeReadCharacteristic")
            return@async gatt.readCharacteristic(characteristic)
        }
    }

    fun asyncDiscover() : Deferred<Boolean>{
        return async(CommonPool) {
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

                if (result) {
                    //this assumes that we will *always* get a call back from the call, even if it is a failure
                    while(discoverStatus == -1) {
                        delay(WAIT_RESOLUTION)
                    }
                    result = BluetoothGatt.GATT_SUCCESS == discoverStatus
                }

                removeGattListener("asyncDiscover")

                return@async result

            }
        }
    }

    fun asyncWriteCharacteristic(characteristicToWrite: BluetoothGattCharacteristic) : Deferred<Boolean>{
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

            if (result) {
                //this assumes that we will *always* get a call back from the call, even if it is a failure
                while(writeStatus == -1) {
                    delay(WAIT_RESOLUTION)
                }
                result = BluetoothGatt.GATT_SUCCESS == writeStatus
            }

            removeGattListener("asyncWriteCharacteristic: $result")
            return@async result
        }
    }

    fun asyncReadCharacteristicInt(characteristicToRead: BluetoothGattCharacteristic, formatType:Int, offset:Int) : Deferred<Int?>{
        return async(CommonPool) {
            logInfo("asyncReadCharacteristic")
            var readStatus = -1
            addGattListener("asyncReadCharacteristic", object:BluetoothGattCallback() {
                override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
                    super.onCharacteristicRead(gatt, characteristic, status)
                    //since it is always possible to have a rogue callback, make sure it is the one we are looking for
                    if (characteristicToRead == characteristic) {
                        readStatus = status
                    }
                }
            })

            var readTriggered = safeReadCharacteristic(characteristicToRead).await()

            if (readTriggered) {
                //this assumes that we will *always* get a call back from the call, even if it is a failure
                while(readStatus == -1) {
                    delay(WAIT_RESOLUTION)
                }
            }

            removeGattListener("asyncReadCharacteristic: $readStatus")

            if (readStatus == BluetoothGatt.GATT_SUCCESS) {
                return@async characteristicToRead.getIntValue(formatType, offset)
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
                        readStatus = status
                    }
                }
            })

            var readTriggered = safeReadCharacteristic(characteristicToRead).await()

            if (readTriggered) {
                //this assumes that we will *always* get a call back from the call, even if it is a failure
                while(readStatus == -1) {
                    delay(WAIT_RESOLUTION)
                }
            }

            removeGattListener("asyncReadCharacteristicBytes")

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
            }
            return@async value
        }
    }

    fun asyncFindAndWriteCharacteristic(service: UUID, characteristic: UUID, value:Int, formatType:Int, offset:Int) : Deferred<Boolean> {
        return async(CommonPool) {
            logInfo("asyncFindAndWriteCharacteristic")
            var success = false
            val characteristicToWrite = asyncFindCharacteristic(service, characteristic).await()
            if (characteristicToWrite != null) {
                if (characteristicToWrite.setValue(value, formatType, offset)) {
                    success = asyncWriteCharacteristic(characteristicToWrite).await()
                }
            }
            return@async success
        }
    }

    fun asyncFindAndWriteCharacteristic(service: UUID, characteristic: UUID, bytes:ByteArray) : Deferred<Boolean> {
        return async(CommonPool) {
            logInfo("asyncFindAndWriteCharacteristic")
            var success = false
            val characteristicToWrite = asyncFindCharacteristic(service, characteristic).await()
            if (characteristicToWrite != null) {
                if (characteristicToWrite.setValue(bytes)) {
                    success = asyncWriteCharacteristic(characteristicToWrite).await()
                }
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
                removeGattListener("asyncConnect")
                return@async true
            }

            if (connectionState != BluetoothGatt.STATE_CONNECTING) {
                logInfo("asyncConnect:connecting")
                safeConnect().await()
            }

            //this has to be on new thread to allow BT to process
            return@async async(CommonPool) {
                logInfo("asyncConnect:start wait")
                while (connectionState != BluetoothGatt.STATE_CONNECTED) {
                    delay(WAIT_RESOLUTION)
                    remainingTimeout -= WAIT_RESOLUTION
                    if (remainingTimeout <= 0) {
                        logError("asyncConnect: Connection Timeout!", false)
                        removeGattListener("asyncConnect")
                        asyncDisconnect().await()
                        return@async false
                    }
                    logInfo("asyncConnect:waiting: $remainingTimeout")
                }
                removeGattListener("asyncConnect")
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
            }.await()
        }
    }

    //this can only be called after a successful discover
    fun asyncFindCharacteristic(service: UUID, characteristic: UUID) : Deferred<BluetoothGattCharacteristic?> {

        //error and throw exception if discovery not done yet
        if (gatt.services.size == 0) {
            logError("Services Not Discovered Yet", true)
        }

        return async(CommonPool) {
            logInfo("findCharacteristic")
            var foundCharacteristic : BluetoothGattCharacteristic? = null
            val foundService = gatt.getService(service)
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
                for(listener in gattListeners) {
                    launch(CommonPool) {
                        listener.value.onCharacteristicChanged(gatt, characteristic)
                    }
                }
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            logInfo("onCharacteristicRead: $characteristic : $status")
            synchronized(gattListeners) {
                for(listener in gattListeners) {
                    launch(CommonPool) {
                        listener.value.onCharacteristicRead(gatt, characteristic, status)
                    }
                }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            logInfo("onCharacteristicWrite: $characteristic : $status")
            synchronized(gattListeners) {
                for(listener in gattListeners) {
                    launch(CommonPool) {
                        listener.value.onCharacteristicWrite(gatt, characteristic, status)
                    }
                }
            }
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            logInfo("onConnectionStateChange: $newState : $status")
            if (status != BluetoothGatt.GATT_SUCCESS) {
                logError("onConnectionStateChange Failed", false)
            } else {
                _connectionState = newState
                synchronized(gattListeners) {
                    for (listener in gattListeners) {
                        launch(CommonPool) {
                            listener.value.onConnectionStateChange(gatt, status, newState)
                        }
                    }
                }
            }
        }

        override fun onDescriptorRead(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            super.onDescriptorRead(gatt, descriptor, status)
            logInfo("onDescriptorRead: $descriptor : $status")
            synchronized(gattListeners) {
                for(listener in gattListeners) {
                    launch(CommonPool) {
                        listener.value.onDescriptorRead(gatt, descriptor, status)
                    }
                }
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            logInfo("onDescriptorWrite: $descriptor : $status")
            synchronized(gattListeners) {
                for(listener in gattListeners) {
                    launch(CommonPool) {
                        listener.value.onDescriptorWrite(gatt, descriptor, status)
                    }
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            logInfo("onMtuChanged: $mtu : $status")
            synchronized(gattListeners) {
                for(listener in gattListeners) {
                    launch(CommonPool) {
                        @Suppress()
                        listener.value.onMtuChanged(gatt, mtu, status)
                    }
                }
            }
        }

        override fun onPhyRead(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            super.onPhyRead(gatt, txPhy, rxPhy, status)
            logInfo("onPhyRead: $txPhy : $rxPhy : $status")
            synchronized(gattListeners) {
                for(listener in gattListeners) {
                    launch(CommonPool) {
                        @Suppress()
                        listener.value.onPhyRead(gatt, txPhy, rxPhy, status)
                    }
                }
            }
        }

        override fun onPhyUpdate(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status)
            logInfo("onPhyUpdate: $txPhy : $rxPhy : $status")
            synchronized(gattListeners) {
                for(listener in gattListeners) {
                    launch(CommonPool) {
                        @Suppress()
                        listener.value.onPhyUpdate(gatt, txPhy, rxPhy, status)
                    }
                }
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            super.onReadRemoteRssi(gatt, rssi, status)
            logInfo("onReadRemoteRssi: $rssi : $status")
            synchronized(gattListeners) {
                for(listener in gattListeners) {
                    launch(CommonPool) {
                        listener.value.onReadRemoteRssi(gatt, rssi, status)
                    }
                }
            }
        }

        override fun onReliableWriteCompleted(gatt: BluetoothGatt?, status: Int) {
            super.onReliableWriteCompleted(gatt, status)
            logInfo("onReliableWriteCompleted: $status")
            synchronized(gattListeners) {
                for(listener in gattListeners) {
                    launch(CommonPool) {
                        listener.value.onReliableWriteCompleted(gatt, status)
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            logInfo("onServicesDiscovered: $status")
            synchronized(gattListeners) {
                for(listener in gattListeners) {
                    launch(CommonPool) {
                        listener.value.onServicesDiscovered(gatt, status)
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
            if (_gatt == null) {
                XYCallByVersion()
                        .add(Build.VERSION_CODES.O) {
                            connectGatt26(device, autoConnect, transport, phy, handler)
                        }
                        .add(Build.VERSION_CODES.M) {
                            connectGatt23(device, autoConnect, transport)
                        }
                        .add(Build.VERSION_CODES.KITKAT) {
                            connectGatt19(device, autoConnect)
                        }.call()
                if (_gatt == null) {
                    logError("Failed to connect Gatt!", true)
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
        _gatt = device.connectGatt(context, autoConnect, centralCallback)
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun connectGatt23(device: BluetoothDevice,
               autoConnect: Boolean,
               transport: Int?) {
        logInfo("connectGatt23")
        if (transport == null) {
            _gatt = device.connectGatt(context, autoConnect, centralCallback)
        } else {
            _gatt = device.connectGatt(context, autoConnect, centralCallback, transport)
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
            _gatt = device.connectGatt(context, autoConnect, centralCallback)
        } else if (phy == null){
            _gatt = device.connectGatt(context, autoConnect, centralCallback, transport)
        } else if (handler == null) {
            _gatt = device.connectGatt(context, autoConnect, centralCallback, transport, phy)
        } else {
            _gatt = device.connectGatt(context, autoConnect, centralCallback, transport, phy, handler)
        }
    }

    companion object {

        val TAG = XYBluetoothGatt.javaClass.simpleName

        //this is the thread that all calls should happen on for gatt calls.  Using a single thread
        //it is documented that for 4.4, we should consider using the UIThread
        val GattThread = XYFilteredSmartScan.BluetoothThread

        private val WAIT_RESOLUTION = 100
        private val CONNECT_TIMEOUT = 15000

        private val deviceToGattMap = HashMap<Int, XYBluetoothGatt>()

        fun from(context:Context, device: BluetoothDevice, callback: BluetoothGattCallback?) : Deferred<XYBluetoothGatt> {
            return async(CommonPool) {
                logInfo(TAG, "from")
                var gatt = deviceToGattMap[device.hashCode()]
                if (gatt == null) {
                    gatt = safeCreateGatt(context.applicationContext, device, callback).await()
                }
                val resultingGatt: XYBluetoothGatt = gatt
                deviceToGattMap[device.hashCode()] = resultingGatt
                return@async resultingGatt
            }
        }

        private fun safeCreateGatt(context:Context, device: BluetoothDevice, callback: BluetoothGattCallback?) : Deferred<XYBluetoothGatt> {
            return async(GattThread) {
                logInfo(TAG, "safeCreateGatt")
                return@async XYBluetoothGatt(context.applicationContext, device, false, callback, null, null, null)
            }
        }
    }
}