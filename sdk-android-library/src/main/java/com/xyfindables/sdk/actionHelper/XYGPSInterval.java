package com.xyfindables.sdk.actionHelper;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.xyfindables.core.XYBase;
import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.action.XYDeviceActionGetGPSInterval;
import com.xyfindables.sdk.action.XYDeviceActionSetGPSInterval;

/**
 * Created by alex.mcelroy on 9/29/2017.
 */

public class XYGPSInterval extends XYActionHelper {

    private static final String TAG = XYGPSInterval.class.getSimpleName();

    public interface Callback extends XYActionHelper.Callback {
        void started (boolean success, byte[] value);
    }

    public XYGPSInterval(XYDevice device, final Callback callback) {
        action = new XYDeviceActionGetGPSInterval(device) {
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

    public XYGPSInterval(XYDevice device, byte[] value, final Callback callback) {
        action = new XYDeviceActionSetGPSInterval(device, value) {
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
