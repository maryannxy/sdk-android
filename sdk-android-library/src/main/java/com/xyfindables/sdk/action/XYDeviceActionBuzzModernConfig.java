package com.xyfindables.sdk.action;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.XYDeviceCharacteristic;
import com.xyfindables.sdk.XYDeviceService;

import java.util.Arrays;
import java.util.UUID;

/**
 * Created by alex.mcelroy on 10/20/2017.
 */

public class XYDeviceActionBuzzModernConfig extends XYDeviceAction {
    private static final String TAG = XYDeviceActionBuzzModernConfig.class.getSimpleName();

    private byte[] value;
    private int counter = 0;

    protected XYDeviceActionBuzzModernConfig(XYDevice device, byte[] value) {
        super(device);
        this.value = value;
        Log.v(TAG, TAG);
    }

    @Override
    public UUID getServiceId() {
        return XYDeviceService.XY4Primary;
    }

    @Override
    public UUID getCharacteristicId() {
        return XYDeviceCharacteristic.XY4PrimaryBuzzerConfig;
    }

    @Override
    public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
        Log.v(TAG, "statusChanged:" + status + ":" + success);
        boolean result = super.statusChanged(status, gatt, characteristic, success);
        switch (status) {
            case STATUS_CHARACTERISTIC_FOUND:
                byte[] sliceOne = Arrays.copyOfRange(value, 0, 19);
                characteristic.setValue(sliceOne);
                if (!gatt.writeCharacteristic(characteristic)) {
                    statusChanged(STATUS_COMPLETED, gatt, characteristic, false);
                }
                break;
            case STATUS_CHARACTERISTIC_WRITE:
                counter++;
                byte[] sliceN = Arrays.copyOfRange(value, counter*20, counter*20 + 19);
                if (counter == 6) {
                    sliceN = Arrays.copyOfRange(value, 120, 129);
                }
                characteristic.setValue(sliceN);
                if (!gatt.writeCharacteristic(characteristic)) {
                    statusChanged(STATUS_COMPLETED, gatt, characteristic, false);
                }
                if (counter == 6) {
                    result = true;
                } else {
                    result = false;
                }
                break;
        }
        return result;
    }
}
