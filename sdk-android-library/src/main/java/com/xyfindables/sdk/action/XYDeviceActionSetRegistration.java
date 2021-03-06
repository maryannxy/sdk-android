package com.xyfindables.sdk.action;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.xyfindables.sdk.XYDeviceCharacteristic;
import com.xyfindables.sdk.XYDeviceService;
import com.xyfindables.sdk.XYDevice;

import java.util.UUID;

/**
 * Created by arietrouw on 1/2/17.
 */

public abstract class XYDeviceActionSetRegistration extends XYDeviceAction {

    private static final String TAG = XYDeviceActionSetRegistration.class.getSimpleName();

    public boolean value;

    public XYDeviceActionSetRegistration(XYDevice device, boolean value) {
        super(device);
        this.value = value;
        logAction(TAG, TAG);
    }

    @Override
    public UUID getServiceId() {
        return XYDeviceService.ExtendedConfig;
    }

    @Override
    public UUID getCharacteristicId() {
        return XYDeviceCharacteristic.ExtendedConfigRegistration;
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