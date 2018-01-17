package com.xyfindables.sdk.actionHelper;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.action.XYDeviceActionGetSIMId;

/*
 Created by alex.mcelroy on 1/16/2018.
*/

public class XYSIM extends XYActionHelper {
    private static final String TAG = XYSIM.class.getSimpleName();

    public interface Callback extends XYActionHelper.Callback {
        void started(boolean success, String value);
    }

    public XYSIM(XYDevice device, final Callback callback) {
        if (device.getFamily() == XYDevice.Family.Gps) {
            action = new XYDeviceActionGetSIMId(device) {
                @Override
                public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                    logExtreme(TAG, "statusChanged:" + status + ":" + success);
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    switch (status) {
                        case STATUS_CHARACTERISTIC_READ:
                            value = characteristic.getValue();
                            StringBuilder valueString = new StringBuilder();
                            for (byte b : value) {
                                int i = (int) b;
                                valueString.append(String.valueOf((char) i));
                            }
                            callback.started(success, valueString.toString());
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
