package com.xyfindables.sdk.action;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.XYDeviceCharacteristic;
import com.xyfindables.sdk.XYDeviceService;

import java.util.UUID;

/**
 * Created by alex.mcelroy on 9/6/2017.
 */

public abstract class XYDeviceActionSetRegistrationModern extends XYDeviceAction {
    private static final String TAG = XYDeviceActionSetRegistrationModern.class.getSimpleName();

    public boolean value;

    public XYDeviceActionSetRegistrationModern(XYDevice device, boolean value) {
        super(device);
        this.value = value;
        logAction(TAG, TAG);
    }

    @Override
    public UUID getServiceId() {
        return XYDeviceService.XY4Primary;
    }

    @Override
    public UUID getCharacteristicId() {
        return XYDeviceCharacteristic.XY4PrimaryStayAwake;
    }

    @Override
    public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
        logExtreme(TAG, "statusChanged:" + status + ":" + success);
        boolean result = super.statusChanged(status, gatt, characteristic, success);
        switch (status) {
            case STATUS_CHARACTERISTIC_FOUND: {
                if (value) {
                    characteristic.setValue(0x01, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                } else {
                    characteristic.setValue(0x00, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                }
                if (!gatt.writeCharacteristic(characteristic)) {
                    result = true;
                }
                break;
            }
        }
        return result;
    }
}
