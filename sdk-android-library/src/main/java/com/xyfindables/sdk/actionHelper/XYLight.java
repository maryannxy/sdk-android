package com.xyfindables.sdk.actionHelper;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.xyfindables.core.XYBase;
import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.action.XYDeviceActionGetLED;
import com.xyfindables.sdk.action.XYDeviceActionSetLED;

/**
 * Created by alex.mcelroy on 9/29/2017.
 */

public class XYLight extends XYActionHelper {

    private static final String TAG = XYLight.class.getSimpleName();

    public interface Callback extends XYActionHelper.Callback {
        void started(boolean success, byte[] value);
    }

    public XYLight(XYDevice device, final Callback callback) {
        action = new XYDeviceActionGetLED(device) {
            @Override
            public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                Log.v(TAG, "statusChanged:" + status + ":" + success);
                boolean result = super.statusChanged(status, gatt, characteristic, success);
                switch (status) {
                    case STATUS_CHARACTERISTIC_READ:
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

    public XYLight(XYDevice device, int value, final Callback callback) {
        action = new XYDeviceActionSetLED(device, value) {
            @Override
            public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                Log.v(TAG, "statusChanged:" + status + ":" + success);
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
