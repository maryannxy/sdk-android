package com.xyfindables.sdk.action.dialog;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.xyfindables.core.XYBase;
import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.XYDeviceCharacteristic;
import com.xyfindables.sdk.XYDeviceService;
import com.xyfindables.sdk.action.XYDeviceAction;

import java.util.UUID;

/**
 * Created by alex on 11/17/2017.
 */

public abstract class GetSpotaServStatus extends XYDeviceAction {
    private static final String TAG = GetSpotaServStatus.class.getSimpleName();

    public byte[] value;

    public GetSpotaServStatus(XYDevice device) {
        super(device);
        Log.v(TAG, TAG);
    }

    @Override
    public UUID getServiceId() {
        return XYDeviceService.SPOTA_SERVICE_UUID;
    }

    @Override
    public UUID getCharacteristicId() {
        return XYDeviceCharacteristic.SPOTA_SERV_STATUS_UUID;
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
                    XYBase.logError(TAG, "Characteristic Read Failed");
                    statusChanged(STATUS_COMPLETED, gatt, characteristic, false);
                }
                break;
        }
        return result;
    }
}
