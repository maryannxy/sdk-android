package com.xyfindables.sdk.action.dialog;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.XYDeviceCharacteristic;
import com.xyfindables.sdk.XYDeviceService;
import com.xyfindables.sdk.action.XYDeviceAction;

import java.util.UUID;

/**
 * Created by alex.mcelroy on 11/15/2017.
 */

public abstract class SetSpotaMemDev extends XYDeviceAction {
    private static final String TAG = SetSpotaMemDev.class.getSimpleName();

    private int value;

    protected SetSpotaMemDev(XYDevice device, int value) {
        super(device);
        this.value = value;
        Log.v(TAG, TAG);
    }

    @Override
    public UUID getServiceId() {
        return XYDeviceService.SPOTA_SERVICE_UUID;
    }

    @Override
    public UUID getCharacteristicId() {
        return XYDeviceCharacteristic.SPOTA_MEM_DEV_UUID;
    }

    @Override
    public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
        Log.v(TAG, "statusChanged:" + status + ":" + success);
        boolean result = super.statusChanged(status, gatt, characteristic, success);
        switch (status) {
            case STATUS_CHARACTERISTIC_FOUND: {
                characteristic.setValue(value, BluetoothGattCharacteristic.FORMAT_UINT32, 0);
                if (!gatt.writeCharacteristic(characteristic)) {
                    Log.e(TAG, "testOta-SetSpotaMemDev write failed");
                    result = true;
                } else {
                    Log.i(TAG, "testOta-SetSpotaMemDev write succeed");
                    result = false;
                }
            }
        }
        return result;
    }
}
