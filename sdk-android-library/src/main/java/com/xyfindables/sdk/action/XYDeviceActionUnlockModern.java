package com.xyfindables.sdk.action;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.XYDeviceCharacteristic;
import com.xyfindables.sdk.XYDeviceService;

import java.util.UUID;

/**
 * Created by alex.mcelroy on 9/6/2017.
 */

public abstract class XYDeviceActionUnlockModern extends XYDeviceAction {

    private static final String TAG = XYDeviceActionUnlockModern.class.getSimpleName();

    public byte[] value;

    public XYDeviceActionUnlockModern(XYDevice device, byte[] value) {
        super(device);
        this.value = value.clone();
        Log.v(TAG, TAG);
    }

    @Override
    public UUID getServiceId() {
        return XYDeviceService.XY4Primary;
    }

    @Override
    public UUID getCharacteristicId() {
        return XYDeviceCharacteristic.XY4PrimaryUnlock;
    }

    @Override
    public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
        Log.v(TAG, "statusChanged:" + status + ":" + success);
        boolean result = super.statusChanged(status, gatt, characteristic, success);
        switch (status) {

            case STATUS_CHARACTERISTIC_FOUND:
                characteristic.setValue(value);
                if (!gatt.writeCharacteristic(characteristic)) {
                    result = true;
                }
                break;
        }
        return result;
    }
}
