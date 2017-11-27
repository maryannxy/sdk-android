
package com.xyfindables.sdk.action;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.XYDeviceCharacteristic;
import com.xyfindables.sdk.XYDeviceService;

import java.util.UUID;

/**
 * Created by alex.mcelroy on 7/21/2017.
 */

public class XYDeviceActionGetSelfTest extends XYDeviceAction {

    private static final String TAG = com.xyfindables.sdk.action.XYDeviceActionGetSelfTest.class.getSimpleName();

    public byte[] value;

    public XYDeviceActionGetSelfTest(XYDevice device) {
        super(device);
        Log.v(TAG, TAG);
    }

    @Override
    public UUID getServiceId() {
        return XYDeviceService.ExtendedControl;
    }

    @Override
    public UUID getCharacteristicId() {
        return XYDeviceCharacteristic.ExtendedControlSelfTest;
    }

    @Override
    public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
        Log.v(TAG, "statusChanged:" + status + ":" + success);
        boolean result = super.statusChanged(status, gatt, characteristic, success);
        switch (status) {
            case STATUS_CHARACTERISTIC_READ:
                value = characteristic.getValue();
                break;
            case STATUS_CHARACTERISTIC_FOUND:
                if (!gatt.readCharacteristic(characteristic)) {
                    result = false;
                }
                break;
        }
        return result;
    }
}