package com.xyfindables.sdk.action;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.xyfindables.core.XYBase;
import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.XYDeviceCharacteristic;
import com.xyfindables.sdk.XYDeviceService;

import java.util.UUID;

/**
 * Created by alex.mcelroy on 10/16/2017.
 */

public abstract class XYDeviceActionGetHardware extends XYDeviceAction {
    private static final String TAG = XYDeviceActionGetHardware.class.getSimpleName();

    public String value;

    public XYDeviceActionGetHardware(XYDevice device) {
        super(device);
        logAction(TAG, TAG);
    }

    @Override
    public UUID getServiceId() {
        return XYDeviceService.DeviceStandard;
    }

    @Override
    public UUID getCharacteristicId() {
        return XYDeviceCharacteristic.DeviceHardware;
    }

    @Override
    public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
        logExtreme(TAG, "statusChanged:" + status + ":" + success);
        boolean result = super.statusChanged(status, gatt, characteristic, success);
        switch (status) {
            case STATUS_CHARACTERISTIC_READ:
                byte[] versionBytes = characteristic.getValue();
                if (versionBytes.length > 0) {
                    value = "";
                    for (byte b : versionBytes) {
                        value += (char)b;
                    }
                }
                break;
            case STATUS_CHARACTERISTIC_FOUND:
                if (!gatt.readCharacteristic(characteristic)) {
                    logError(TAG, "connTest-Characteristic Read Failed", false);
                    result = true;
                }
                break;
        }
        return result;
    }
}
