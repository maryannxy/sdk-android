package com.xyfindables.sdk.action;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.XYDeviceCharacteristic;
import com.xyfindables.sdk.XYDeviceService;

import java.util.UUID;

/**
 * Created by alex.mcelroy on 9/6/2017.
 */

public abstract class XYDeviceActionUnlockModern extends XYDeviceAction {

    public static final byte[] _defaultUnlockCode = {(byte) 0x2f, (byte) 0xbe, (byte) 0xa2, (byte) 0x07, (byte) 0x52, (byte) 0xfe, (byte) 0xbf, (byte) 0x31, (byte) 0x1d, (byte) 0xac, (byte) 0x5d, (byte) 0xfa, (byte) 0x7d, (byte) 0x77, (byte) 0x76, (byte) 0x80};

    private static final String TAG = XYDeviceActionUnlockModern.class.getSimpleName();

    public byte[] value;

    public XYDeviceActionUnlockModern(XYDevice device, byte[] value) {
        super(device);
        this.value = value.clone();
        Log.v(TAG, TAG);
    }

    @Override
    public UUID getServiceId() {
        return XYDeviceService.XY4Primary;
    }

    @Override
    public UUID getCharacteristicId() {
        return XYDeviceCharacteristic.XY4PrimaryUnlock;
    }

    @Override
    public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
        Log.v(TAG, "statusChanged:" + status + ":" + success);
        boolean result = super.statusChanged(status, gatt, characteristic, success);
        switch (status) {

            case STATUS_CHARACTERISTIC_FOUND:
                characteristic.setValue(value);
                if (!gatt.writeCharacteristic(characteristic)) {
                    statusChanged(STATUS_COMPLETED, gatt, characteristic, false);
                }
                break;
        }
        return result;
    }
}
