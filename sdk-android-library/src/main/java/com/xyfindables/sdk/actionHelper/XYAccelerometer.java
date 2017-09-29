package com.xyfindables.sdk.actionHelper;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.xyfindables.core.XYBase;
import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.action.XYDeviceActionGetAccelerometerInactive;
import com.xyfindables.sdk.action.XYDeviceActionGetAccelerometerMovementCount;
import com.xyfindables.sdk.action.XYDeviceActionGetAccelerometerRaw;
import com.xyfindables.sdk.action.XYDeviceActionGetAccelerometerThreshold;
import com.xyfindables.sdk.action.XYDeviceActionGetAccelerometerTimeout;

/**
 * Created by alex.mcelroy on 9/29/2017.
 */

public class XYAccelerometer extends XYActionHelper {

    private static final String TAG = XYAccelerometer.class.getSimpleName();

    public interface Raw extends XYActionHelper.Callback {
        void read(boolean success, byte[] value);
    }

    public interface Inactive extends XYActionHelper.Callback {
        void read(boolean success, int value);
    }

    public interface MovementCount extends XYActionHelper.Callback {
        void read(boolean success, int value);
    }

    public interface Threshold extends XYActionHelper.Callback {
        void read(boolean success, byte[] value);
    }

    public interface Timeout extends XYActionHelper.Callback {
        void read(boolean success, byte[] value);
    }

    public XYDevice _device;

    public XYAccelerometer(XYDevice device) {
        _device = device;
    }

    public void getRaw(final Raw callback) {
        action = new XYDeviceActionGetAccelerometerRaw(_device) {
            @Override
            public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                Log.v(TAG, "statusChanged:" + status + ":" + success);
                boolean result = super.statusChanged(status, gatt, characteristic, success);
                switch (status) {
                    case STATUS_CHARACTERISTIC_READ:
                        value = characteristic.getValue();
                        callback.read(success, value);
                        break;
                    case STATUS_CHARACTERISTIC_FOUND:
                        if (!gatt.readCharacteristic(characteristic)) {
                            XYBase.logError(TAG, "Characteristic Read Failed");
                            statusChanged(STATUS_COMPLETED, gatt, characteristic, false);
                        }
                        break;
                }
                return result;
            }
        };
    }

    public void getInactive(final Inactive callback) {
        action = new XYDeviceActionGetAccelerometerInactive(_device) {
            @Override
            public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                Log.v(TAG, "statusChanged:" + status + ":" + success);
                boolean result = super.statusChanged(status, gatt, characteristic, success);
                switch (status) {
                    case STATUS_CHARACTERISTIC_READ:
                        value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                        callback.read(success, value);
                        break;
                    case STATUS_CHARACTERISTIC_FOUND:
                        if (!gatt.readCharacteristic(characteristic)) {
                            XYBase.logError(TAG, "Characteristic Read Failed");
                            statusChanged(STATUS_COMPLETED, gatt, characteristic, false);
                        }
                        break;
                }
                return result;
            }
        };
    }

    public void getMovementCount(final MovementCount callback) {
        action = new XYDeviceActionGetAccelerometerMovementCount(_device) {
            @Override
            public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                Log.v(TAG, "statusChanged:" + status + ":" + success);
                boolean result = super.statusChanged(status, gatt, characteristic, success);
                switch (status) {
                    case STATUS_CHARACTERISTIC_READ:
                        value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                        callback.read(success, value);
                        break;
                    case STATUS_CHARACTERISTIC_FOUND:
                        if (!gatt.readCharacteristic(characteristic)) {
                            XYBase.logError(TAG, "Characteristic Read Failed");
                            statusChanged(STATUS_COMPLETED, gatt, characteristic, false);
                        }
                        break;
                }
                return result;
            }
        };
    }

    public void getThreshold(final Threshold callback) {
        action = new XYDeviceActionGetAccelerometerThreshold(_device) {
            @Override
            public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                Log.v(TAG, "statusChanged:" + status + ":" + success);
                boolean result = super.statusChanged(status, gatt, characteristic, success);
                switch (status) {
                    case STATUS_CHARACTERISTIC_READ:
                        value = characteristic.getValue();
                        callback.read(success, value);
                        break;
                    case STATUS_CHARACTERISTIC_FOUND:
                        if (!gatt.readCharacteristic(characteristic)) {
                            XYBase.logError(TAG, "Characteristic Read Failed");
                            statusChanged(STATUS_COMPLETED, gatt, characteristic, false);
                        }
                        break;
                }
                return result;
            }
        };
    }

    public void getTimeout(final Timeout callback) {
        action = new XYDeviceActionGetAccelerometerTimeout(_device) {
            @Override
            public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                Log.v(TAG, "statusChanged:" + status + ":" + success);
                boolean result = super.statusChanged(status, gatt, characteristic, success);
                switch (status) {
                    case STATUS_CHARACTERISTIC_READ:
                        value = characteristic.getValue();
                        callback.read(success, value);
                        break;
                    case STATUS_CHARACTERISTIC_FOUND:
                        if (!gatt.readCharacteristic(characteristic)) {
                            XYBase.logError(TAG, "Characteristic Read Failed");
                            statusChanged(STATUS_COMPLETED, gatt, characteristic, false);
                        }
                        break;
                }
                return result;
            }
        };
    }
}
