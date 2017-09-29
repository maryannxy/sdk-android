package com.xyfindables.sdk.actionHelper;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;
import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.action.XYDeviceActionGetRegistration;
import com.xyfindables.sdk.action.XYDeviceActionGetRegistrationModern;
import com.xyfindables.sdk.action.XYDeviceActionSetRegistration;
import com.xyfindables.sdk.action.XYDeviceActionSetRegistrationModern;

/**
 * Created by alex.mcelroy on 9/6/2017.
 */

public class XYStayAwake extends XYActionHelper {
    private static final String TAG = XYStayAwake.class.getSimpleName();

    public interface Callback extends XYActionHelper.Callback {
        void started(boolean success, boolean value);
    }

    public XYStayAwake(XYDevice device, final Callback callback) {
        if (device.getFamily() == XYDevice.Family.XY4) {
            action = new XYDeviceActionGetRegistrationModern(device) {
                public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                    Log.v(TAG, "statusChanged:" + status + ":" + success);
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    switch (status) {
                        case STATUS_CHARACTERISTIC_FOUND:
                            if (!gatt.readCharacteristic(characteristic)) {
                                statusChanged(STATUS_COMPLETED, gatt, characteristic, false);
                            }
                            break;
                        case STATUS_CHARACTERISTIC_READ:
                            value = (characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0) != 0);
                            callback.started(success, value);
                            break;
                        case STATUS_COMPLETED:
                            callback.completed(success);
                            break;
                    }
                    return result;
                }
            };
        } else {
            action = new XYDeviceActionGetRegistration(device) {
                @Override
                public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                    Log.v(TAG, "statusChanged:" + status + ":" + success);
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    switch (status) {
                        case STATUS_CHARACTERISTIC_FOUND:
                            if (!gatt.readCharacteristic(characteristic)) {
                                statusChanged(STATUS_COMPLETED, gatt, characteristic, false);
                            }
                            break;
                        case STATUS_CHARACTERISTIC_READ:
                            value = (characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0) != 0);
                            callback.started(success, value);
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

    public XYStayAwake(XYDevice device, boolean value, final Callback callback) {
        if (device.getFamily() == XYDevice.Family.XY4) {
            action = new XYDeviceActionSetRegistrationModern(device, value) {
                public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                    Log.v(TAG, "statusChanged:" + status + ":" + success);
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    switch (status) {
                        case STATUS_CHARACTERISTIC_FOUND: {
                            if (value) {
                                characteristic.setValue(0x01, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                            } else {
                                characteristic.setValue(0x00, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                            }
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
            action = new XYDeviceActionSetRegistration(device, value) {
                @Override
                public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                    Log.v(TAG, "statusChanged:" + status + ":" + success);
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    switch (status) {
                        case STATUS_CHARACTERISTIC_FOUND: {
                            if (value) {
                                characteristic.setValue(0x01, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                            } else {
                                characteristic.setValue(0x00, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                            }
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
