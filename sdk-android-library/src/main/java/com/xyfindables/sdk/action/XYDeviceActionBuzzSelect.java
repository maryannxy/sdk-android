package com.xyfindables.sdk.action;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.xyfindables.sdk.XYDeviceCharacteristic;
import com.xyfindables.sdk.XYDeviceService;
import com.xyfindables.sdk.XYDevice;

import java.util.UUID;

/**
 * Created by arietrouw on 1/1/17.
 */

public abstract class XYDeviceActionBuzzSelect extends XYDeviceAction {

    private static final String TAG = XYDeviceActionBuzzSelect.class.getSimpleName();

    private int _index;

    protected XYDeviceActionBuzzSelect(XYDevice device, int index) {
        super(device);
        _index = index;
        logExtreme(TAG, TAG);
    }

    @Override
    public UUID getServiceId() {
        return XYDeviceService.Control;
    }

    @Override
    public UUID getCharacteristicId() {
        return XYDeviceCharacteristic.ControlBuzzerSelect;
    }

    @Override
    public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
        logExtreme(TAG, "statusChanged:" + status + ":" + success);
        boolean result = super.statusChanged(status, gatt, characteristic, success);
        switch (status) {
            case STATUS_CHARACTERISTIC_FOUND:
                characteristic.setValue(_index, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                if (!gatt.writeCharacteristic(characteristic)) {
                    result = true;
                }
                break;
        }
        return result;
    }
}
