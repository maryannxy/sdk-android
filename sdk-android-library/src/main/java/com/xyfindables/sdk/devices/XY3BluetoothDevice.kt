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

open class XY3BluetoothDevice(context: Context, scanResult: XYScanResult) : XYFinderBluetoothDevice(context, scanResult) {

    val primary = PrimaryService(this)

    init {
        addGattListener("xy3", object: BluetoothGattCallback() {
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
                val xy3Listener = listener as? XY3BluetoothDevice.Listener
                if (xy3Listener != null) {
                    launch(CommonPool) {
                        xy3Listener.buttonSinglePressed()
                    }
                }
            }
        }
    }

    interface Listener : XYFinderBluetoothDevice.Listener {
        fun buttonSinglePressed()
    }

    companion object {

        val FAMILY_UUID = UUID.fromString("08885dd0-111b-11e4-9191-0800200c9a66")

        val DEFAULT_LOCK_CODE = byteArrayOf(0x2f.toByte(), 0xbe.toByte(), 0xa2.toByte(), 0x07.toByte(), 0x52.toByte(), 0xfe.toByte(), 0xbf.toByte(), 0x31.toByte(), 0x1d.toByte(), 0xac.toByte(), 0x5d.toByte(), 0xfa.toByte(), 0x7d.toByte(), 0x77.toByte(), 0x76.toByte(), 0x80.toByte())

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
                    XY3BluetoothDevice(context, scanResult)
                }
            } else {
                XYFinderBluetoothDevice.uuidToCreator.remove(FAMILY_UUID)
            }
        }


    }
}