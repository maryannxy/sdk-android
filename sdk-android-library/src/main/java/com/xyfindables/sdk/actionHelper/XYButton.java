package com.xyfindables.sdk.actionHelper;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.action.XYDeviceActionGetButtonState;
import com.xyfindables.sdk.action.XYDeviceActionGetButtonStateModern;
import com.xyfindables.sdk.action.XYDeviceActionSubscribeButton;
import com.xyfindables.sdk.action.XYDeviceActionSubscribeButtonModern;

import static com.xyfindables.core.XYBase.logError;

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
                    logExtreme(TAG, "statusChanged:" + status + ":" + success);
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    switch (status) {
                        case STATUS_CHARACTERISTIC_FOUND:
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
            action = new XYDeviceActionGetButtonState(device) {
                @Override
                public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                    logExtreme(TAG, "statusChanged:" + status + ":" + success);
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    switch (status) {
                        case STATUS_CHARACTERISTIC_FOUND:
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

    private BluetoothGatt _gatt = null;
    private BluetoothGattCharacteristic _characteristic = null;

    protected XYButton(XYDevice device, final Notification notification) {
        if (device.getFamily() == XYDevice.Family.XY4) {
            action = new XYDeviceActionSubscribeButtonModern(device) {
                @Override
                public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                    logExtreme(TAG, "statusChanged:" + status + ":" + success);
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    switch (status) {
                        case STATUS_CHARACTERISTIC_UPDATED: {
                            notification.updated(success);
                            break;
                        }
                    }
                    return result;
                }
            };
        } else {
            action = new XYDeviceActionSubscribeButton(device) {
                @Override
                public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                    logExtreme(TAG, "statusChanged:" + status + ":" + success);
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    switch (status) {
                        case STATUS_CHARACTERISTIC_UPDATED: {
                            notification.updated(success);
                            break;
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
            logError(TAG, "connTest-Stopping non-started button notifications", true);
        }
    }
}
