package com.xyfindables.sdk.action;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.xyfindables.sdk.XYDeviceCharacteristic;
import com.xyfindables.sdk.XYDeviceService;
import com.xyfindables.sdk.XYDevice;

import java.util.UUID;

/**
 * Created by arietrouw on 1/2/17.
 */

public abstract class XYDeviceActionDisconnect extends XYDeviceAction {

    private static final String TAG = XYDeviceActionDisconnect.class.getSimpleName();

    public XYDeviceActionDisconnect(XYDevice device) {
        super(device);
        Log.v(TAG, TAG);
    }

    @Override
    public UUID getServiceId() {
        return XYDeviceService.Control;
    }

    @Override
    public UUID getCharacteristicId() {
        return XYDeviceCharacteristic.ControlDisconnect;
    }

    @Override
    public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
        Log.v(TAG, "statusChanged:" + status + ":" + success);
        boolean result = super.statusChanged(status, gatt, characteristic, success);
        switch (status) {
            case STATUS_CHARACTERISTIC_FOUND: {
                characteristic.setValue(1, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                if (!gatt.writeCharacteristic(characteristic)) {
                    result = false;
                }
                break;
            }
        }
        return result;
    }
}