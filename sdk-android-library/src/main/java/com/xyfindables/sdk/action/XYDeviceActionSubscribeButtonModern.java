package com.xyfindables.sdk.action;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Log;

import com.xyfindables.core.XYBase;
import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.XYDeviceCharacteristic;
import com.xyfindables.sdk.XYDeviceService;

import java.util.UUID;

/**
 * Created by alex.mcelroy on 9/6/2017.
 */

public abstract class XYDeviceActionSubscribeButtonModern extends XYDeviceAction {
    private static final String TAG = XYDeviceActionSubscribeButtonModern.class.getSimpleName();

    public static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final int BUTTONPRESS_SINGLE = 1;
    public static final int BUTTONPRESS_DOUBLE = 2;
    public static final int BUTTONPRESS_LONG = 3;

    private BluetoothGatt _gatt = null;
    private BluetoothGattCharacteristic _characteristic = null;

    public XYDeviceActionSubscribeButtonModern(XYDevice device) {
        super(device);
        Log.v(TAG, TAG);
    }

    public void stop() {
        if (_gatt != null && _characteristic != null) {
            _gatt.setCharacteristicNotification(_characteristic, false);
            _gatt = null;
            _characteristic = null;
        } else {
            XYBase.logError(TAG, "Stopping non-started button notifications");
        }
    }

    @Override
    public UUID getServiceId() {
        return XYDeviceService.XY4Primary;
    }

    @Override
    public UUID getCharacteristicId() {
        return XYDeviceCharacteristic.XY4PrimaryButtonState;
    }

    @Override
    public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
        Log.v(TAG, "statusChanged:" + status + ":" + success);
        boolean result = super.statusChanged(status, gatt, characteristic, success);
        switch (status) {
            case STATUS_CHARACTERISTIC_UPDATED: {
                Log.i(TAG, "statusChanged:Updated:" + characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
                return false;
            }
            case STATUS_CHARACTERISTIC_FOUND: {
                Log.i(TAG, "statusChanged:Characteristic Found");
                if (!gatt.setCharacteristicNotification(characteristic, true)) {
                    XYBase.logError(TAG, "Characteristic Notification Failed");
                } else {
                    _gatt = gatt;
                    _characteristic = characteristic;
                }
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                if (!gatt.writeDescriptor(descriptor)) {
                    result = true;
                }
                break;
            }
        }
        return result;
    }
}
