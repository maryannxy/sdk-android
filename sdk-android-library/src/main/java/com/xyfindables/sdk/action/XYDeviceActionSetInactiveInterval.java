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

public abstract class XYDeviceActionSetInactiveInterval extends XYDeviceAction {

    private static final String TAG = XYDeviceActionGetInactiveInterval.class.getSimpleName();

    public int value;

    public XYDeviceActionSetInactiveInterval(XYDevice device, int value) {
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
        return XYDeviceCharacteristic.ExtendedConfigInactiveInterval;
    }

    @Override
    public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
        logExtreme(TAG, "statusChanged:" + status + ":" + success);
        boolean result = super.statusChanged(status, gatt, characteristic, success);
        switch (status) {
            case STATUS_CHARACTERISTIC_FOUND: {
                characteristic.setValue(value, BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                if (!gatt.writeCharacteristic(characteristic)) {
                    result = true;
                }
                break;
            }
        }
        return result;
    }
}