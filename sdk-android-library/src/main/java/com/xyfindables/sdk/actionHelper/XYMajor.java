package com.xyfindables.sdk.actionHelper;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.xyfindables.core.XYBase;
import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.action.XYDeviceActionGetMajor;
import com.xyfindables.sdk.action.XYDeviceActionGetMajorModern;
import com.xyfindables.sdk.action.XYDeviceActionSetMajor;
import com.xyfindables.sdk.action.XYDeviceActionSetMajorModern;

/**
 * Created by alex.mcelroy on 9/6/2017.
 */

public class XYMajor extends XYActionHelper {

    private static final String TAG = XYMajor.class.getSimpleName();

    public XYMajor(XYDevice device, final Callback callback) {
        if (device.getFamily() == XYDevice.Family.XY4) {
            action = new XYDeviceActionGetMajorModern(device) {
                public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                    Log.v(TAG, "statusChanged:" + status + ":" + success);
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    switch (status) {
                        case STATUS_CHARACTERISTIC_READ:
                            value = characteristic.getValue();
                            break;
                        case STATUS_CHARACTERISTIC_FOUND:
                            if (!gatt.readCharacteristic(characteristic)) {
                                XYBase.logError(TAG, "Characteristic Read Failed");
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
            action = new XYDeviceActionGetMajor(device) {
                @Override
                public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                    Log.v(TAG, "statusChanged:" + status + ":" + success);
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    switch (status) {
                        case STATUS_CHARACTERISTIC_READ:
                            value = characteristic.getValue();
                            break;
                        case STATUS_CHARACTERISTIC_FOUND:
                            if (!gatt.readCharacteristic(characteristic)) {
                                XYBase.logError(TAG, "Characteristic Read Failed");
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

    public XYMajor(XYDevice device, int value, final Callback callback) {
        if (device.getFamily() == XYDevice.Family.XY4) {
            action = new XYDeviceActionSetMajorModern(device, value) {
                @Override
                public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                    Log.v(TAG, "statusChanged:" + status + ":" + success);
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    switch (status) {
                        case STATUS_CHARACTERISTIC_FOUND: {
                            characteristic.setValue(value, BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                            if (!gatt.writeCharacteristic(characteristic)) {
                                statusChanged(STATUS_COMPLETED, gatt, characteristic, false);
                            }
                            break;
                        }
                        case STATUS_COMPLETED:
                            callback.completed(success);
                            break;
                    }
                    return result;
                }
            };
        } else {
            action = new XYDeviceActionSetMajor(device, value) {
                @Override
                public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                    Log.v(TAG, "statusChanged:" + status + ":" + success);
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    switch (status) {
                        case STATUS_CHARACTERISTIC_FOUND: {
                            characteristic.setValue(value, BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                            if (!gatt.writeCharacteristic(characteristic)) {
                                statusChanged(STATUS_COMPLETED, gatt, characteristic, false);
                            }
                            break;
                        }
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
