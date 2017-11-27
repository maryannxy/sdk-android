package com.xyfindables.sdk.action;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.xyfindables.core.XYBase;
import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.XYDeviceCharacteristic;
import com.xyfindables.sdk.XYDeviceService;

import java.util.UUID;

/**
 * Created by alex.mcelroy on 5/4/2017.
 */

public abstract class XYDeviceActionGetAccelerometerMovementCount extends XYDeviceAction {

    private static final String TAG = XYDeviceActionGetAccelerometerTimeout.class.getSimpleName();

    public int value;

    public XYDeviceActionGetAccelerometerMovementCount(XYDevice device) {
        super(device);
        Log.v(TAG, TAG);
    }

    @Override
    public UUID getServiceId() {
        return XYDeviceService.Sensor;
    }

    @Override
    public UUID getCharacteristicId() {
        return XYDeviceCharacteristic.SensorMovementCount;
    }

    @Override
    public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
        Log.v(TAG, "statusChanged:" + status + ":" + success);
        boolean result = super.statusChanged(status, gatt, characteristic, success);
        switch (status) {
            case STATUS_CHARACTERISTIC_READ:
                value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                break;
            case STATUS_CHARACTERISTIC_FOUND:
                if (!gatt.readCharacteristic(characteristic)) {
                    XYBase.logError(TAG, "Characteristic Read Failed");
                    result = false;
                }
                break;
        }
        return result;
    }
}
