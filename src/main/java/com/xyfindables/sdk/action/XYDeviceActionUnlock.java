package com.xyfindables.sdk.action;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.xyfindables.sdk.XYDeviceCharacteristic;
import com.xyfindables.sdk.XYDeviceService;
import com.xyfindables.sdk.XYDevice;

import java.util.UUID;

/**
 * Created by arietrouw on 1/2/17.
 */

public abstract class XYDeviceActionUnlock extends XYDeviceAction {

    public static final byte[] _defaultUnlockCode = {(byte) 0x2f, (byte) 0xbe, (byte) 0xa2, (byte) 0x07, (byte) 0x52, (byte) 0xfe, (byte) 0xbf, (byte) 0x31, (byte) 0x1d, (byte) 0xac, (byte) 0x5d, (byte) 0xfa, (byte) 0x7d, (byte) 0x77, (byte) 0x76, (byte) 0x80};

    private static final String TAG = XYDeviceActionUnlock.class.getSimpleName();

    public byte[] value;

    public XYDeviceActionUnlock(XYDevice device, byte[] value) {
        super(device);
        this.value = value.clone();
        Log.v(TAG, TAG);
    }

    @Override
    public UUID getServiceId() {
        return XYDeviceService.BasicConfig;
    }

    @Override
    public UUID getCharacteristicId() {
        return XYDeviceCharacteristic.BasicConfigUnlock;
    }

    @Override
    public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
        Log.v(TAG, "statusChanged:" + status + ":" + success);
        boolean result = super.statusChanged(status, gatt, characteristic, success);
        switch (status) {
            case STATUS_CHARACTERISTIC_FOUND:
                characteristic.setValue(value);
                gatt.writeCharacteristic(characteristic);
                break;
        }
        return result;
    }
}