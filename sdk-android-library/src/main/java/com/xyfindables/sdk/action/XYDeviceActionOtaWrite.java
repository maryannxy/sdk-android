package com.xyfindables.sdk.action;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.XYDeviceCharacteristic;
import com.xyfindables.sdk.XYDeviceService;

import java.util.UUID;

/**
 * Created by alex.mcelroy on 6/12/2017.
 */

public abstract class XYDeviceActionOtaWrite extends XYDeviceAction {

    private static final String TAG = XYDeviceActionOtaWrite.class.getSimpleName();

    public byte[][] value;
    private int counter = 0;
    private XYDevice _device;

    public XYDeviceActionOtaWrite(XYDevice device, byte[][] value) {
        super(device);
        this.value = value;
        _device = device;
        logAction(TAG, TAG);
    }

    @Override
    public UUID getServiceId() {
        return XYDeviceService.BasicConfig;
    }

    @Override
    public UUID getCharacteristicId() {
        return XYDeviceCharacteristic.BasicConfigOtaWrite;
    }

    @Override
    public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {

        logExtreme(TAG, "testOta-statusChanged:" + status + ":" + success);
        boolean result = super.statusChanged(status, gatt, characteristic, success);
        switch (status) {
            case STATUS_CHARACTERISTIC_FOUND:
                _device.otaMode(true);
                logExtreme(TAG, "testOta-found: " + counter + " : " + success + ": length: " + value.length);
                characteristic.setValue(value[counter]);
                gatt.writeCharacteristic(characteristic);
                logExtreme(TAG, "testOta-value = " + bytesToHex(value[counter]));
                break;
            case STATUS_CHARACTERISTIC_WRITE:
                counter++;
                if (counter < value.length) {
                    logExtreme(TAG, "testOta-write: " + counter + " : " + success);
                    characteristic.setValue(value[counter]);
                    gatt.writeCharacteristic(characteristic);
                    logExtreme(TAG, "testOta-value = " + bytesToHex(value[counter]));
                    result = false;
                } else {
                    logExtreme(TAG, "testOta-write-FINISHED: " + success + ": otaMode set to false");
                    _device.otaMode(false);
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
