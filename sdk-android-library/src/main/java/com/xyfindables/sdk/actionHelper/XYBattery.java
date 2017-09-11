package com.xyfindables.sdk.actionHelper;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.action.XYDeviceAction;
import com.xyfindables.sdk.action.XYDeviceActionGetBatteryLevel;
import com.xyfindables.sdk.action.XYDeviceActionGetBatteryLevelModern;

/**
 * Created by alex.mcelroy on 9/6/2017.
 */

public class XYBattery extends XYActionHelper {

    private static final String TAG = XYBeep.class.getSimpleName();

    protected XYBattery(XYDevice device, final Callback callback) {
        if (device.getFamily() == XYDevice.Family.XY4) {
            action = new XYDeviceActionGetBatteryLevelModern(device) {
                @Override
                public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                    Log.v(TAG, "statusChanged:" + status + ":" + success);
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    switch (status) {
                        case STATUS_CHARACTERISTIC_READ:
                            callback.started(success);
                            value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                            break;
                        case STATUS_CHARACTERISTIC_FOUND:
                            if (!gatt.readCharacteristic(characteristic)) {
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
            action = new XYDeviceActionGetBatteryLevel(device) {
                @Override
                public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                    Log.v(TAG, "statusChanged:" + status + ":" + success);
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    switch (status) {
                        case STATUS_CHARACTERISTIC_READ:
                            callback.started(success);
                            value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                            break;
                        case STATUS_CHARACTERISTIC_FOUND:
                            if (!gatt.readCharacteristic(characteristic)) {
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
