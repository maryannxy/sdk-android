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

    private int _index;

    protected XYDeviceActionBuzzSelectModern(XYDevice device, int index) {
        super(device);
        _index = index;
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
            case STATUS_CHARACTERISTIC_FOUND:
                characteristic.setValue(_index, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                if (!gatt.writeCharacteristic(characteristic)) {
                    statusChanged(STATUS_COMPLETED, gatt, characteristic, false);
                }
                break;
        }
        return result;
    }
}
