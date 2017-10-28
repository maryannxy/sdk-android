package com.xyfindables.sdk.actionHelper;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.action.XYDeviceActionBuzzModern;
import com.xyfindables.sdk.action.XYDeviceActionBuzz;
import com.xyfindables.sdk.action.XYDeviceActionBuzzSelect;
import com.xyfindables.sdk.action.XYDeviceActionBuzzSelectModern;

/**
 * Created by alex.mcelroy on 9/5/2017.
 */

public class XYBeep extends XYActionHelper {

    private static final String TAG = XYBeep.class.getSimpleName();

    // verify values we should use for standard beep of xy4, also create custom variables containing different configurations
    protected static final byte[] value = {(byte) 0x0b, (byte) 0x02};

    public interface Callback extends XYActionHelper.Callback {
        void started(boolean success);
    }

    public XYBeep(XYDevice device, final Callback callback) {
        if (device.getFamily() == XYDevice.Family.XY4) {
            action = new XYDeviceActionBuzzModern(device, value) {
                @Override
                public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                    Log.v(TAG, "statusChanged:" + status + ":" + success);
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    switch (status) {
                        case STATUS_STARTED:
                            callback.started(success);
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
                        case STATUS_STARTED:
                            callback.started(success);
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

    public XYBeep(XYDevice device, final int index, final Callback callback) {
        if (device.getFamily() == XYDevice.Family.XY4) {
            byte[] value = {(byte)index, (byte)2};
            action = new XYDeviceActionBuzzSelectModern(device, value) {
                @Override
                public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                    Log.v(TAG, "statusChanged:" + status + ":" + success);
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    switch (status) {
                        case STATUS_STARTED:
                            callback.started(success);
                            break;
                        case STATUS_COMPLETED:
                            callback.completed(success);
                            break;
                    }
                    return result;
                }
            };
        } else {
            action = new XYDeviceActionBuzzSelect(device, index) {
                @Override
                public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                    Log.v(TAG, "statusChanged:" + status + ":" + success);
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    switch (status) {
                        case STATUS_STARTED:
                            callback.started(success);
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
