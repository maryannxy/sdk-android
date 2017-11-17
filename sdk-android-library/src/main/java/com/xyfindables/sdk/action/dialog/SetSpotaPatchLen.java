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

public abstract class SetSpotaPatchLen extends XYDeviceAction {
    private static final String TAG = SetSpotaPatchLen.class.getSimpleName();

    public byte[] value;

    public SetSpotaPatchLen(XYDevice device, byte[] value) {
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
        return XYDeviceCharacteristic.SPOTA_PATCH_LEN_UUID;
    }

    @Override
    public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
        Log.v(TAG, "statusChanged:" + status + ":" + success);
        boolean result = super.statusChanged(status, gatt, characteristic, success);
        switch (status) {
            case STATUS_CHARACTERISTIC_FOUND: {
                characteristic.setValue(value);
                if (!gatt.writeCharacteristic(characteristic)) {
                    statusChanged(STATUS_COMPLETED, gatt, characteristic, false);
                }
                break;
            }
        }
        return result;
    }
}
