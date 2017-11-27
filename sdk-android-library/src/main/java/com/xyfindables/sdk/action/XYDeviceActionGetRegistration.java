package com.xyfindables.sdk.action;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.support.annotation.NonNull;
import android.util.Log;

import com.xyfindables.sdk.XYDeviceCharacteristic;
import com.xyfindables.sdk.XYDeviceService;
import com.xyfindables.sdk.XYDevice;

import java.util.UUID;

/**
 * Created by arietrouw on 1/2/17.
 */

public abstract class XYDeviceActionGetRegistration extends XYDeviceAction {

    private static final String TAG = XYDeviceActionGetRegistration.class.getSimpleName();

    @NonNull
    public Boolean value = false;

    public XYDeviceActionGetRegistration(XYDevice device) {
        super(device);
        Log.v(TAG, TAG);
    }

    @Override
    public UUID getServiceId() {
        return XYDeviceService.ExtendedConfig;
    }

    @Override
    public UUID getCharacteristicId() {
        return XYDeviceCharacteristic.ExtendedConfigRegistration;
    }

    @Override
    public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
        Log.v(TAG, "statusChanged:" + status + ":" + success);
        boolean result = super.statusChanged(status, gatt, characteristic, success);
        switch (status) {
            case STATUS_CHARACTERISTIC_READ:
                value = (characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0) != 0);
                break;
            case STATUS_CHARACTERISTIC_FOUND:
                if (!gatt.readCharacteristic(characteristic)) {
                    result = false;
                }
                break;
        }
        return result;
    }
}