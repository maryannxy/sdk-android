package com.xyfindables.sdk.actionHelper;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.util.Log;

import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.action.XYDeviceAction;
import com.xyfindables.sdk.action.XYDeviceActionBuzzModern;
import com.xyfindables.sdk.action.XYDeviceActionBuzz;

import java.util.UUID;

/**
 * Created by alex.mcelroy on 9/5/2017.
 */

public class XYBeep extends XYActionHelper {

    private static final String TAG = XYBeep.class.getSimpleName();

    // verify values we should use for standard beep of xy4, also create custom variables containing different configurations
    protected static final byte[] value = {(byte) 0x0b, (byte) 0x04, (byte) 0x02};

    protected XYBeep(XYDevice device, final Callback callback) {
        if (device.getFamily() == XYDevice.Family.XY4) {
            action = new XYDeviceActionBuzzModern(device, value) {
                @Override
                public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                    Log.v(TAG, "statusChanged:" + status + ":" + success);
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    switch (status) {
                        case STATUS_CHARACTERISTIC_FOUND:
                            callback.started(success);
                            characteristic.setValue(value);
                            if (!gatt.writeCharacteristic(characteristic)) {
                                statusChanged(STATUS_COMPLETED, gatt, characteristic, false);
                            }
                            break;
                        case STATUS_COMPLETED:
                            callback.completed(success);
                            break;
                    }
                    return result;
                }
            };
        } else {
            action = new XYDeviceActionBuzz(device) {
                @Override
                public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                    Log.v(TAG, "statusChanged:" + status + ":" + success);
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    switch (status) {
                        case STATUS_CHARACTERISTIC_FOUND:
                            callback.started(success);
                            characteristic.setValue(1, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                            if (!gatt.writeCharacteristic(characteristic)) {
                                statusChanged(STATUS_COMPLETED, gatt, characteristic, false);
                            }
                            break;
                        case STATUS_COMPLETED:
                            callback.completed(success);
                            break;
                    }
                    return result;
                }
            };
        }
    }
}
