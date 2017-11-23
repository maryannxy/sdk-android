package com.xyfindables.sdk.action.dialog;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.XYDeviceCharacteristic;
import com.xyfindables.sdk.XYDeviceService;
import com.xyfindables.sdk.action.XYDeviceAction;

import java.util.UUID;

/**
 * Created by alex.mcelroy on 11/15/2017.
 */

public abstract class SetSpotaPatchData extends XYDeviceAction {
    private static final String TAG = SetSpotaPatchData.class.getSimpleName();

    private byte[][] value;
    private int counter = 0;
    private XYDevice _device;

    public SetSpotaPatchData(XYDevice device, byte[][] value) {
        super(device);
        this.value = value;
        _device = device;
        Log.v(TAG, TAG);
    }

    @Override
    public UUID getServiceId() {
        return XYDeviceService.SPOTA_SERVICE_UUID;
    }

    @Override
    public UUID getCharacteristicId() {
        return XYDeviceCharacteristic.SPOTA_PATCH_DATA_UUID;
    }

    @Override
    public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
//        Log.v(TAG, "testOta-statusChanged:" + status + ":" + success);
        boolean result = super.statusChanged(status, gatt, characteristic, success);
        switch (status) {
            case STATUS_CHARACTERISTIC_FOUND: {
//                Log.i(TAG, "testOta-found: " + counter + " : " + success + ": length: " + value.length);
                characteristic.setValue(value[counter]);
                gatt.writeCharacteristic(characteristic);
//                Log.i(TAG, "testOta-value = " + bytesToHex(value[counter]));
                break;
            }
            case STATUS_CHARACTERISTIC_WRITE:
                counter++;
                if (counter < value.length) {
                    characteristic.setValue(value[counter]);
                    gatt.writeCharacteristic(characteristic);
//                    Log.i(TAG, "testOta-value = " + bytesToHex(value[counter]));
                    result = false;
                } else {
//                    Log.i(TAG, "testOta-write-FINISHED: " + success + ": otaMode set to false");
                    result = true;
                }
                break;
        }
        return result;
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
