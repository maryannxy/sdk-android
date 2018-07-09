package com.xyfindables.sdk

import android.annotation.TargetApi
import android.bluetooth.*
import android.content.Context
import android.os.Build
import android.os.Handler
import com.xyfindables.core.XYBase
import kotlinx.coroutines.experimental.*
import java.util.*
import kotlin.collections.HashMap

open class XYBluetoothGatt protected constructor(
        val context:Context,
        val device: BluetoothDevice,
        autoConnect: Boolean,
        callback: BluetoothGattCallback?,
        transport: Int?,
        phy: Int?,
        handler: Handler?
) : XYBase() {

    private val WAIT_RESOLUTION = 100

    protected val gatt: BluetoothGatt
        get() {
            return _gatt!!
        }

    private var _gatt: BluetoothGatt? = null

    private val listeners = HashMap<String, BluetoothGattCallback>()

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

    fun addListener(key: String, listener: BluetoothGattCallback) {
        logInfo("addListener")
        synchronized(listeners) {
            listeners[key] = listener
        }
    }

    fun removeListener(key: String) {
        logInfo("removeListener")
        synchronized(listeners) {
            listeners.remove(key)
        }
    }

    fun asyncClose() : Deferred<Unit>{
        logInfo("asyncClose")
        return async(CommonPool) {
            asyncDisconnect().await()
            safeClose()
            XYBluetoothGatt.deviceToGattMap.remove(device.hashCode())
            removeListener("default")
            _gatt = null
            return@async
        }
    }

    //it is recommended that all gatt calls are done in the same thread, and in the uithread for 4.4
    private fun safeConnect() : Deferred<Boolean> {
        return async(GattThread) {
            return@async gatt.connect()
        }
    }

    //it is recommended that all gatt calls are done in the same thread, and in the uithread for 4.4
    private fun safeDisconnect() : Deferred<Unit> {
        return async(GattThread) {
            gatt.disconnect()
            return@async
        }
    }

    //it is recommended that all gatt calls are done in the same thread, and in the uithread for 4.4
    private fun safeClose() : Deferred<Unit> {
        return async(GattThread) {
            gatt.close()
            return@async
        }
    }

    //it is recommended that all gatt calls are done in the same thread, and in the uithread for 4.4
    private fun safeDiscoverServices() : Deferred<Boolean> {
        return async(GattThread) {
            return@async gatt.discoverServices()
        }
    }

    //it is recommended that all gatt calls are done in the same thread, and in the uithread for 4.4
    private fun safeWriteCharacteristic(characteristic: BluetoothGattCharacteristic) : Deferred<Boolean> {
        return async(GattThread) {
            return@async gatt.writeCharacteristic(characteristic)
        }
    }

    //it is recommended that all gatt calls are done in the same thread, and in the uithread for 4.4
    private fun safeReadCharacteristic(characteristic: BluetoothGattCharacteristic) : Deferred<Boolean> {
        return async(GattThread) {
            return@async gatt.readCharacteristic(characteristic)
        }
    }

    fun asyncDiscover() : Deferred<Boolean>{
        logInfo("asyncDiscover")
        return async(CommonPool) {
            logInfo("discover:services:$gatt.services.size")
            if (gatt.services.size > 0) {
                return@async true
            } else {
                var discoverStatus = -1
                addListener("asyncDiscover", object:BluetoothGattCallback() {
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

                removeListener("asyncDiscover")

                return@async result

            }
        }
    }

    fun asyncWriteCharacteristic(characteristicToWrite: BluetoothGattCharacteristic) : Deferred<Boolean>{
        logInfo("asyncWriteCharacteristic")
        return async(CommonPool) {
            var writeStatus = -1
            addListener("asyncWriteCharacteristic", object:BluetoothGattCallback() {
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

            removeListener("asyncWriteCharacteristic: $result")
            return@async result
        }
    }

    fun asyncReadCharacteristicInt(characteristicToRead: BluetoothGattCharacteristic, formatType:Int, offset:Int) : Deferred<Int?>{
        logInfo("asyncReadCharacteristic")
        return async(CommonPool) {
            var readStatus = -1
            addListener("asyncReadCharacteristic", object:BluetoothGattCallback() {
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

            removeListener("asyncReadCharacteristic: $readStatus")

            if (readStatus == BluetoothGatt.GATT_SUCCESS) {
                return@async characteristicToRead.getIntValue(formatType, offset)
            } else {
                return@async null
            }
        }
    }

    fun asyncReadCharacteristicBytes(characteristicToRead: BluetoothGattCharacteristic) : Deferred<ByteArray?>{
        logInfo("asyncReadCharacteristicBytes")
        return async(CommonPool) {
            var readStatus = -1
            addListener("asyncReadCharacteristicBytes", object:BluetoothGattCallback() {
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

            removeListener("asyncReadCharacteristicBytes")

            return@async characteristicToRead.value
        }
    }

    fun asyncFindAndReadCharacteristicInt(service: UUID, characteristic: UUID, formatType:Int, offset:Int) : Deferred<Int?> {
        return async {
            var value: Int? = null
            val characteristicToRead = asyncFindCharacteristic(service, characteristic).await()
            if (characteristicToRead != null) {
                value = asyncReadCharacteristicInt(characteristicToRead, formatType, offset).await()
            }
            return@async value
        }
    }

    fun asyncFindAndReadCharacteristicBytes(service: UUID, characteristic: UUID) : Deferred<ByteArray?> {
        return async {
            var value: ByteArray? = null
            val characteristicToRead = asyncFindCharacteristic(service, characteristic).await()
            if (characteristicToRead != null) {
                value = asyncReadCharacteristicBytes(characteristicToRead).await()
            }
            return@async value
        }
    }

    fun asyncFindAndWriteCharacteristic(service: UUID, characteristic: UUID, value:Int, formatType:Int, offset:Int) : Deferred<Boolean> {
        return async {
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
        return async {
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

    fun asyncConnect(timeout: Int) : Deferred<Boolean>{
        logInfo("asyncConnect")
        return async(CommonPool) {

            if (connectionState == BluetoothGatt.STATE_CONNECTED) {
                return@async true
            }

            if (connectionState != BluetoothGatt.STATE_CONNECTING) {
                if (!safeConnect().await()) {
                    return@async false
                }
            }

            var remainingTimeout = timeout

            //wait for timeout or connections
            while(connectionState != BluetoothGatt.STATE_CONNECTED) {
                delay(WAIT_RESOLUTION)
                remainingTimeout -= WAIT_RESOLUTION
                if (remainingTimeout <= 0) {
                    removeListener("asyncConnect")
                    asyncDisconnect().await()
                    return@async false
                }
            }

            removeListener("asyncConnect")
            return@async true
        }
    }

    fun asyncDisconnect() : Deferred<Unit>{
        logInfo("asyncDisconnect")
        return async(CommonPool) {
            if (connectionState == BluetoothGatt.STATE_DISCONNECTED) {
                return@async
            }

            safeDisconnect().await()

            //this assumes that we will *always* get to disconnected
            while(connectionState != BluetoothGatt.STATE_DISCONNECTED) {
                delay(WAIT_RESOLUTION)
            }

            return@async
        }
    }

    //this can only be called after a successful discover
    fun asyncFindCharacteristic(service: UUID, characteristic: UUID) : Deferred<BluetoothGattCharacteristic?> {
        logInfo("findCharacteristic")

        //error and throw exception if discovery not done yet
        if (gatt.services.size == 0) {
            logError("Services Not Discovered Yet", true)
        }

        return async(CommonPool) {
            logInfo("findCharacteristic:async")
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
            synchronized(listeners) {
                for(listener in listeners) {
                    launch(CommonPool) {
                        listener.value.onCharacteristicChanged(gatt, characteristic)
                    }
                }
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            logInfo("onCharacteristicRead: $characteristic : $status")
            synchronized(listeners) {
                for(listener in listeners) {
                    launch(CommonPool) {
                        listener.value.onCharacteristicRead(gatt, characteristic, status)
                    }
                }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            logInfo("onCharacteristicWrite: $characteristic : $status")
            synchronized(listeners) {
                for(listener in listeners) {
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
                synchronized(listeners) {
                    for (listener in listeners) {
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
            synchronized(listeners) {
                for(listener in listeners) {
                    launch(CommonPool) {
                        listener.value.onDescriptorRead(gatt, descriptor, status)
                    }
                }
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            logInfo("onDescriptorWrite: $descriptor : $status")
            synchronized(listeners) {
                for(listener in listeners) {
                    launch(CommonPool) {
                        listener.value.onDescriptorWrite(gatt, descriptor, status)
                    }
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            logInfo("onMtuChanged: $mtu : $status")
            synchronized(listeners) {
                for(listener in listeners) {
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
            synchronized(listeners) {
                for(listener in listeners) {
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
            synchronized(listeners) {
                for(listener in listeners) {
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
            synchronized(listeners) {
                for(listener in listeners) {
                    launch(CommonPool) {
                        listener.value.onReadRemoteRssi(gatt, rssi, status)
                    }
                }
            }
        }

        override fun onReliableWriteCompleted(gatt: BluetoothGatt?, status: Int) {
            super.onReliableWriteCompleted(gatt, status)
            logInfo("onReliableWriteCompleted: $status")
            synchronized(listeners) {
                for(listener in listeners) {
                    launch(CommonPool) {
                        listener.value.onReliableWriteCompleted(gatt, status)
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            logInfo("onServicesDiscovered: $status")
            synchronized(listeners) {
                for(listener in listeners) {
                    launch(CommonPool) {
                        listener.value.onServicesDiscovered(gatt, status)
                    }
                }
            }
        }
    }

    init {
        if (callback != null) {
            addListener("default", callback)
        }
        XYCallByVersion()
            .add(Build.VERSION_CODES.O) {
                init26(context, device, autoConnect, transport, phy, handler)
            }
            .add(Build.VERSION_CODES.M) {
                init23(context, device, autoConnect, transport)
            }
            .add(Build.VERSION_CODES.KITKAT) {
                init19(context, device, autoConnect)
            }.call()
        if (_gatt == null) {
            logError("Failed to connect Gatt!", true)
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private fun init19(context:Context,
               device: BluetoothDevice,
               autoConnect: Boolean) {
        logInfo("init19")
        _gatt = device.connectGatt(context, autoConnect, centralCallback)
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun init23(context:Context,
               device: BluetoothDevice,
               autoConnect: Boolean,
               transport: Int?) {
        logInfo("init23")
        if (transport == null) {
            _gatt = device.connectGatt(context, autoConnect, centralCallback)
        } else {
            _gatt = device.connectGatt(context, autoConnect, centralCallback, transport)
        }
    }
    @TargetApi(Build.VERSION_CODES.O)
    private fun init26(context:Context,
               device: BluetoothDevice,
               autoConnect: Boolean,
               transport: Int?,
               phy: Int?,
               handler: Handler?) {
        logInfo("init26")
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
        //this is the thread that all calls should happen on for gatt calls.  Using UIThread
        //for now since that is needed for 4.4, but should allow non-ui thread for later
        //versions
        val GattThread = UIThread

        private val deviceToGattMap = HashMap<Int, XYBluetoothGatt>()

        fun from(context:Context, device: BluetoothDevice, callback: BluetoothGattCallback?) : Deferred<XYBluetoothGatt> {
            return async {
                var gatt = deviceToGattMap[device.hashCode()]
                if (gatt == null) {
                    gatt = safeCreateGatt(context.applicationContext, device, callback).await()
                }
                val resultingGatt:XYBluetoothGatt = gatt
                deviceToGattMap[device.hashCode()] = resultingGatt
                return@async resultingGatt
            }
        }

        private fun safeCreateGatt(context:Context, device: BluetoothDevice, callback: BluetoothGattCallback?) : Deferred<XYBluetoothGatt> {
            return async(GattThread) {
                return@async XY4Gatt(context.applicationContext, device, false, callback, null, null, null)
            }
        }
    }
}