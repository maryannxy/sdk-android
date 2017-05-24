package com.xyfindables.sdk.action;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.XYDeviceCharacteristic;
import com.xyfindables.sdk.XYDeviceService;

import java.util.UUID;

/**
 * Created by alex.mcelroy on 5/16/2017.
 */

public abstract class XYDeviceActionGetBatterySinceCharged extends XYDeviceAction {

    private static final String TAG = XYDeviceActionGetBatterySinceCharged.class.getSimpleName();

    public byte[] value;

    public XYDeviceActionGetBatterySinceCharged(XYDevice device) {
        super(device);
        Log.v(TAG, TAG);
    }

    @Override
    public UUID getServiceId() {
        return XYDeviceService.BatteryStandard;
    }

    @Override
    public UUID getCharacteristicId() {
        return XYDeviceCharacteristic.BatterySinceCharged;
    }

    @Override
    public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
        Log.v(TAG, "statusChanged:" + status + ":" + success);
        boolean result = super.statusChanged(status, gatt, characteristic, success);
        switch (status) {
            case STATUS_CHARACTERISTIC_READ:
                value = characteristic.getValue();
                break;
            case STATUS_CHARACTERISTIC_FOUND:
                gatt.readCharacteristic(characteristic);
                break;
        }
        return result;
    }
}
