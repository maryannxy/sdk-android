package com.xyfindables.sdk.action;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.xyfindables.core.XYBase;
import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.XYDeviceCharacteristic;
import com.xyfindables.sdk.XYDeviceService;

import java.util.UUID;

/**
 * Created by alex.mcelroy on 9/6/2017.
 */

public abstract class XYDeviceActionGetVersionModern extends XYDeviceAction {
    private static final String TAG = XYDeviceActionGetVersionModern.class.getSimpleName();

    public String value;

    public XYDeviceActionGetVersionModern(XYDevice device) {
        super(device);
        Log.v(TAG, TAG);
    }

    @Override
    public UUID getServiceId() {
        return XYDeviceService.XY4Device;
    }

    @Override
    public UUID getCharacteristicId() {
        return XYDeviceCharacteristic.XY4DeviceFirmware;
    }

    @Override
    public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
        Log.v(TAG, "statusChanged:" + status + ":" + success);
        boolean result = super.statusChanged(status, gatt, characteristic, success);
        switch (status) {
            case STATUS_CHARACTERISTIC_READ:
                byte[] versionBytes = characteristic.getValue();
                if (versionBytes.length > 0) {
                    value = "";
                    for (byte b : versionBytes) {
                        value += String.format("%x", b);
                    }
                }
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
