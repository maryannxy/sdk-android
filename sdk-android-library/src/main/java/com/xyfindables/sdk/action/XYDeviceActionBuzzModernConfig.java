package com.xyfindables.sdk.action;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.provider.Settings;
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
                byte[] slotPlusOffset = {value[0], (byte)0};
                byte[] slice = Arrays.copyOfRange(value, 1, 19);
                byte[] packet = new byte[slotPlusOffset.length + slice.length];
                System.arraycopy(slotPlusOffset, 0, packet, 0, slotPlusOffset.length);
                System.arraycopy(slice, 0, packet, slotPlusOffset.length, slice.length);
                characteristic.setValue(packet);
                if (!gatt.writeCharacteristic(characteristic)) {
                    statusChanged(STATUS_COMPLETED, gatt, characteristic, false);
                }
                break;
            case STATUS_CHARACTERISTIC_WRITE:
                counter++;
                slotPlusOffset = new byte[]{value[0], (byte)(counter*20)};
                slice = Arrays.copyOfRange(value, counter * 18, counter * 18 + 18);
                if (counter == 6) {
                    slice = Arrays.copyOfRange(value, 121, 129);
                }
                packet = new byte[slotPlusOffset.length + slice.length];
                System.arraycopy(slotPlusOffset, 0, packet, 0, slotPlusOffset.length);
                System.arraycopy(slice, 0, packet, slotPlusOffset.length, slice.length);
                characteristic.setValue(packet);
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
