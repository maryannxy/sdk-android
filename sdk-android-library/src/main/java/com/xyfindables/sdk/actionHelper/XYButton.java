package com.xyfindables.sdk.actionHelper;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.telecom.Call;
import android.util.Log;

import com.xyfindables.core.XYBase;
import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.action.XYDeviceActionGetButtonState;
import com.xyfindables.sdk.action.XYDeviceActionGetButtonStateModern;
import com.xyfindables.sdk.action.XYDeviceActionSubscribeButton;
import com.xyfindables.sdk.action.XYDeviceActionSubscribeButtonModern;

import java.util.UUID;

/**
 * Created by alex.mcelroy on 9/6/2017.
 */

public class XYButton extends XYActionHelper {

    private static final String TAG = XYBeep.class.getSimpleName();

    public interface Callback extends XYActionHelper.Callback {
        void started(boolean success, int value);
    }

    protected XYButton(XYDevice device, final Callback callback) {
        if (device.getFamily() == XYDevice.Family.XY4) {
            action = new XYDeviceActionGetButtonStateModern(device) {
                @Override
                public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                    Log.v(TAG, "statusChanged:" + status + ":" + success);
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    switch (status) {
                        case STATUS_CHARACTERISTIC_READ:
                            value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                            break;
                        case STATUS_CHARACTERISTIC_FOUND:
                            callback.started(success, value);
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
            action = new XYDeviceActionGetButtonState(device) {
                @Override
                public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                    Log.v(TAG, "statusChanged:" + status + ":" + success);
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    switch (status) {
                        case STATUS_CHARACTERISTIC_READ:
                            value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                            break;
                        case STATUS_CHARACTERISTIC_FOUND:
                            callback.started(success, value);
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

    private BluetoothGatt _gatt = null;
    private BluetoothGattCharacteristic _characteristic = null;

    protected XYButton(XYDevice device, final Notification notification) {
        if (device.getFamily() == XYDevice.Family.XY4) {
            action = new XYDeviceActionSubscribeButtonModern(device) {
                @Override
                public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                    Log.v(TAG, "statusChanged:" + status + ":" + success);
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    switch (status) {
                        case STATUS_CHARACTERISTIC_UPDATED: {
                            notification.updated(success);
                            Log.i(TAG, "statusChanged:Updated:" + characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
                            return true;
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
                                statusChanged(STATUS_COMPLETED, gatt, characteristic, false);
                            }
                            return true;
                        }
                    }
                    return result;
                }
            };
        } else {
            action = new XYDeviceActionSubscribeButton(device) {
                @Override
                public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                    Log.v(TAG, "statusChanged:" + status + ":" + success);
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    switch (status) {
                        case STATUS_CHARACTERISTIC_UPDATED: {
                            notification.updated(success);
                            Log.i(TAG, "statusChanged:Updated:" + characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
                            return true;
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
                                statusChanged(STATUS_COMPLETED, gatt, characteristic, false);
                            }
                            return true;
                        }
                    }
                    return result;
                }
            };
        }
    }

    protected void stop() {
        if (_gatt != null && _characteristic != null) {
            _gatt.setCharacteristicNotification(_characteristic, false);
            _gatt = null;
            _characteristic = null;
        } else {
            XYBase.logError(TAG, "Stopping non-started button notifications");
        }
    }
}
