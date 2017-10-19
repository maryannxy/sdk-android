package com.xyfindables.sdk.actionHelper;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.action.XYDeviceActionGetBatteryLevel;

/**
 * Created by alex.mcelroy on 9/6/2017.
 */

public class XYBattery extends XYActionHelper {

    private static final String TAG = XYBeep.class.getSimpleName();

    public interface Callback extends XYActionHelper.Callback {
        void started(boolean success, int value);
    }

    public XYBattery(XYDevice device, final Callback callback) {
        action = new XYDeviceActionGetBatteryLevel(device) {
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
}
