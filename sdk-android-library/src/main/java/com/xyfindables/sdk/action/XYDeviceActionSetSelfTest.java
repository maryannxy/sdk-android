package com.xyfindables.sdk.action;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.XYDeviceCharacteristic;
import com.xyfindables.sdk.XYDeviceService;

import java.util.UUID;

/**
 * Created by alex.mcelroy on 7/24/2017.
 */

public class XYDeviceActionSetSelfTest extends XYDeviceAction {

    private static final String TAG = com.xyfindables.sdk.action.XYDeviceActionSetSelfTest.class.getSimpleName();

    public int value;

    public XYDeviceActionSetSelfTest(XYDevice device, int value) {
        super(device);
        this.value = value;
        Log.v(TAG, TAG);
    }

    @Override
    public UUID getServiceId() {
        return XYDeviceService.ExtendedControl;
    }

    @Override
    public UUID getCharacteristicId() {
        return XYDeviceCharacteristic.ExtendedControlSelfTest;
    }

    @Override
    public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
        Log.v(TAG, "statusChanged:" + status + ":" + success);
        boolean result = super.statusChanged(status, gatt, characteristic, success);
        switch (status) {
            case STATUS_CHARACTERISTIC_FOUND: {
                characteristic.setValue(value, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                if (!gatt.writeCharacteristic(characteristic)) {
                    result = false;
                }
                break;
            }
        }
        return result;
    }
}