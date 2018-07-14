package com.xyfindables.sdk.devices

import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import com.xyfindables.sdk.gatt.XYDeviceCharacteristic
import com.xyfindables.sdk.gatt.XYDeviceService
import com.xyfindables.sdk.scanner.XYScanResult
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import java.util.*

open class XY4BluetoothDevice(context: Context, scanResult: XYScanResult) : XYFinderBluetoothDevice(context, scanResult) {

    override fun find() : Deferred<Boolean?> {
        logInfo("find")
        return writePrimaryBuzzer(1)
    }

    override fun lock(bytes: ByteArray) : Deferred<Boolean?> {
        logInfo("lock")
        return async(CommonPool) {
            val result = writePrimaryLock(bytes).await()
            if (result == null) {
                logError("lock: call failed to complete", false)
            } else {
                if (!result) {
                    logError("lock: call returned false", false)
                }
            }
            return@async result
        }
    }

    override fun unlock(bytes: ByteArray) : Deferred<Boolean?> {
        logInfo("unlock")
        return async(CommonPool) {
            val result = writePrimaryUnlock(bytes).await()
            if (result == null) {
                logError("unlock: call failed to complete", false)
            } else {
                if (!result) {
                    logError("unlock: call returned false", false)
                }
            }
            return@async result
        }
    }

    override fun getStayAwake() : Deferred<Boolean?> {
        return async {
            val stayAwake = readPrimaryStayAwake().await()
            return@async stayAwake == StayAwake.On.state
        }
    }

    override fun setStayAwake(stayAwake: Boolean) : Deferred<Boolean?> {
        return writePrimaryStayAwake(
                when(stayAwake){
                    true -> StayAwake.On.state
                    else -> StayAwake.Off.state
                }
        )
    }

    /* Native Characteristic Wrappers */

    fun writePrimaryBuzzer(tone: Int) : Deferred<Boolean?> {
        return access{
            return@access asyncFindAndWriteCharacteristic(
                    XYDeviceService.XY4Primary,
                    XYDeviceCharacteristic.XY4PrimaryBuzzer,
                    tone,
                    BluetoothGattCharacteristic.FORMAT_UINT8,
                    0
            ).await()
        }
    }

    fun readPrimaryStayAwake() : Deferred<Int?> {
        return access {
            return@access asyncFindAndReadCharacteristicInt(
                    XYDeviceService.XY4Primary,
                    XYDeviceCharacteristic.XY4PrimaryStayAwake,
                    BluetoothGattCharacteristic.FORMAT_UINT8,
                    0
            ).await()
        }
    }

    fun writePrimaryStayAwake(flag: Int) : Deferred<Boolean?> {
        return access {
            return@access asyncFindAndWriteCharacteristic(
                    XYDeviceService.XY4Primary,
                    XYDeviceCharacteristic.XY4PrimaryStayAwake,
                    flag,
                    BluetoothGattCharacteristic.FORMAT_UINT8,
                    0
            ).await()
        }
    }

    fun writePrimaryLock(bytes: ByteArray) : Deferred<Boolean?> {
        return access {
            logInfo("writePrimaryLock")
            val result = asyncFindAndWriteCharacteristic(
                    XYDeviceService.XY4Primary,
                    XYDeviceCharacteristic.XY4PrimaryLock,
                    bytes
            ).await()
            if (result == null) {
                logError("writePrimaryLock: call failed to complete", false)
            }
            return@access result
        }
    }

    fun readPrimaryLock() : Deferred<ByteArray?> {
        return access {
            logInfo("readPrimaryLock")
            return@access asyncFindAndReadCharacteristicBytes(
                    XYDeviceService.XY4Primary,
                    XYDeviceCharacteristic.XY4PrimaryLock
            ).await()
        }
    }

    fun writePrimaryUnlock(bytes: ByteArray) : Deferred<Boolean?> {
        return access {
            logInfo("writePrimaryUnlock")
            return@access asyncFindAndWriteCharacteristic(
                    XYDeviceService.XY4Primary,
                    XYDeviceCharacteristic.XY4PrimaryUnlock,
                    bytes
            ).await()
        }
    }

    interface Listener {
        fun buttonPressed()
    }

    companion object {

        val FAMILY_UUID = UUID.fromString("a44eacf4-0104-0000-0000-5f784c9977b5")
        val DEFAULT_LOCK_CODE = byteArrayOf(0x00.toByte(), 0x01.toByte(), 0x02.toByte(), 0x03.toByte(), 0x04.toByte(), 0x05.toByte(), 0x06.toByte(), 0x07.toByte(), 0x08.toByte(), 0x09.toByte(), 0x0a.toByte(), 0x0b.toByte(), 0x0c.toByte(), 0x0d.toByte(), 0x0e.toByte(), 0x0f.toByte())
        val DEFAULT_LOCK_CODE_XY3 = byteArrayOf(0x2f.toByte(), 0xbe.toByte(), 0xa2.toByte(), 0x07.toByte(), 0x52.toByte(), 0xfe.toByte(), 0xbf.toByte(), 0x31.toByte(), 0x1d.toByte(), 0xac.toByte(), 0x5d.toByte(), 0xfa.toByte(), 0x7d.toByte(), 0x77.toByte(), 0x76.toByte(), 0x80.toByte())

        enum class StayAwake(val state: Int) {
            Off(0),
            On(1)
        }

        enum class ButtonPress(val state:Int) {
            None(0),
            Single(1),
            Double(2),
            Long(3)
        }

        fun enable(enable: Boolean) {
            if (enable) {
                XYFinderBluetoothDevice.enable(true)
                XYFinderBluetoothDevice.uuidToCreator[FAMILY_UUID] = {
                    context: Context,
                    scanResult: XYScanResult
                    ->
                    XY4BluetoothDevice(context, scanResult)
                }
            } else {
                XYFinderBluetoothDevice.uuidToCreator.remove(FAMILY_UUID)
            }
        }


    }
}