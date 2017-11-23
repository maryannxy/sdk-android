package com.xyfindables.sdk.action;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.XYDeviceCharacteristic;
import com.xyfindables.sdk.XYDeviceService;

import java.util.UUID;

/**
 * Created by alex.mcelroy on 6/12/2017.
 */

public abstract class XYDeviceActionReboot extends XYDeviceAction {

    private static final String TAG = XYDeviceActionReboot.class.getSimpleName();

    public int value;

    public XYDeviceActionReboot(XYDevice device, int value) {
        super(device);
        this.value = value;
        Log.v(TAG, TAG);
    }

    @Override
    public UUID getServiceId() {
        return XYDeviceService.BasicConfig;
    }

    @Override
    public UUID getCharacteristicId() {
        return XYDeviceCharacteristic.BasicConfigReboot;
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
                Log.i(TAG, "testOta-rebootFound: " + success);
                break;
            }
        }
        return result;
    }
}
