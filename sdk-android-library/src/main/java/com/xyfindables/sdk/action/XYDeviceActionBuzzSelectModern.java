package com.xyfindables.sdk.action;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.XYDeviceCharacteristic;
import com.xyfindables.sdk.XYDeviceService;

import java.util.UUID;

/**
 * Created by alex.mcelroy on 9/28/2017.
 */

public abstract class XYDeviceActionBuzzSelectModern extends XYDeviceAction {
    private static final String TAG = XYDeviceActionBuzzSelectModern.class.getSimpleName();

    private byte[] _value;

    protected XYDeviceActionBuzzSelectModern(XYDevice device, byte[] value) {
        super(device);
        _value = value;
        Log.v(TAG, TAG);
    }

    @Override
    public UUID getServiceId() {
        return XYDeviceService.XY4Primary;
    }

    @Override
    public UUID getCharacteristicId() {
        return XYDeviceCharacteristic.XY4PrimaryBuzzer;
    }

    @Override
    public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean succes) {
        Log.v(TAG, "statusChanged:" + status + ":" + succes);
        boolean result = super.statusChanged(status, gatt, characteristic, succes);
        switch (status) {
            case STATUS_CHARACTERISTIC_FOUND: {
                characteristic.setValue(_value);
                if (!gatt.writeCharacteristic(characteristic)) {
                    Log.v(TAG, "testSoundConfig-writeCharacteristic failed");
                    result = true;
                }
                break;
            }
        }
        return result;
    }
}
