package com.xyfindables.sdk.devices

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import com.xyfindables.sdk.scanner.XYScanResult
import com.xyfindables.sdk.services.xy4.PrimaryService
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.launch
import java.util.*

open class XY4BluetoothDevice(context: Context, scanResult: XYScanResult) : XYFinderBluetoothDevice(context, scanResult) {

    val primary = PrimaryService(this)

    init {
        addGattListener("xy4", object: BluetoothGattCallback() {
            override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
                logInfo("onCharacteristicChanged")
                super.onCharacteristicChanged(gatt, characteristic)
                if (characteristic?.uuid == primary.buttonState.uuid) {
                    reportButtonPressed(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0))
                }
            }
        })
    }

    override fun find() : Deferred<Boolean?> {
        logInfo("find")
        return primary.buzzer.set(11)
    }

    fun reportButtonPressed(state: Int) {
        logInfo("reportButtonPressed")
        synchronized(listeners) {
            for (listener in listeners) {
                val xy4Listener = listener as? XY4BluetoothDevice.Listener
                if (xy4Listener != null) {
                    launch(CommonPool) {
                        xy4Listener.buttonSinglePressed()
                    }
                }
            }
        }
    }

    interface Listener : XYFinderBluetoothDevice.Listener {
        fun buttonSinglePressed()
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