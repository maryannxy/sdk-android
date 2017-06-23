package com.xyfindables.sdk.action;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.xyfindables.core.XYBase;
import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.XYDeviceCharacteristic;
import com.xyfindables.sdk.XYDeviceService;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by alex.mcelroy on 6/12/2017.
 */

public abstract class XYDeviceActionOtaWrite extends XYDeviceAction {

    private static final String TAG = XYDeviceActionOtaWrite.class.getSimpleName();

    public byte[][] value;
    private int counter = 0;

    public XYDeviceActionOtaWrite(XYDevice device, byte[][] value) {
        super(device);
        this.value = value;
        _device = device;
        Log.v(TAG, TAG);
    }

    private XYDevice _device;

    @Override
    public void start(final Context context) {
        Log.i(TAG, this.getClass().getSuperclass().getSimpleName() + ":starting...");
        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Log.i(TAG, "running...");
                _device.queueAction(context.getApplicationContext(), XYDeviceActionOtaWrite.this);
                return null;
            }
        };
        asyncTask.executeOnExecutor(_threadPool);
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

        Log.v(TAG, "testOta-statusChanged:" + status + ":" + success);
        boolean result = super.statusChanged(status, gatt, characteristic, success);
        switch (status) {
            case STATUS_CHARACTERISTIC_FOUND:
                _device.otaMode(true);
                Log.i(TAG, "testOta-found: " + counter + " : " + success + ": length: " + value.length);
                characteristic.setValue(value[counter]);
                gatt.writeCharacteristic(characteristic);
                Log.i(TAG, "testOta-value = " + bytesToHex(value[counter]));
                break;
            case STATUS_CHARACTERISTIC_WRITE:
                counter++;
                if (counter < value.length) {
                    Log.i(TAG, "testOta-write: " + counter + " : " + success);
                    characteristic.setValue(value[counter]);
                    gatt.writeCharacteristic(characteristic);
                    Log.i(TAG, "testOta-value = " + bytesToHex(value[counter]));
                    result = false;
                } else {
                    Log.i(TAG, "testOta-write-FINISHED: " + success + ": otaMode set to false");
                    _device.otaMode(false);
                    result = true;
                }
                break;
        }
        return result;
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
