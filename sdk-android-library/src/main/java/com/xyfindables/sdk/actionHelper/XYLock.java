package com.xyfindables.sdk.actionHelper;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.action.XYDeviceActionSetLock;
import com.xyfindables.sdk.action.XYDeviceActionSetLockModern;

/**
 * Created by alex.mcelroy on 9/6/2017.
 */

public class XYLock extends XYActionHelper {
    private static final String TAG = XYLock.class.getSimpleName();

    public XYLock(XYDevice device, byte[] value, final Callback callback) {
        if (device.getFamily() == XYDevice.Family.XY4) {
            action = new XYDeviceActionSetLockModern(device, value) {
                @Override
                public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                    logExtreme(TAG, "statusChanged:" + status + ":" + success);
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    switch (status) {
                        case STATUS_COMPLETED:
                            callback.completed(success);
                            break;
                    }
                    return result;
                }
            };
        } else {
            action = new XYDeviceActionSetLock(device, value) {
                @Override
                public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                    logExtreme(TAG, "statusChanged:" + status + ":" + success);
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    switch (status) {
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
