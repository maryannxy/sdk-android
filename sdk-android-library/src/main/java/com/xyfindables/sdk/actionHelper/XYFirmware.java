package com.xyfindables.sdk.actionHelper;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.xyfindables.core.XYBase;
import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.action.XYDeviceActionGetVersion;
import com.xyfindables.sdk.action.XYDeviceActionGetVersionModern;

/**
 * Created by alex.mcelroy on 9/6/2017.
 */

public class XYFirmware extends XYActionHelper {

    private static final String TAG = XYFirmware.class.getSimpleName();

    public XYFirmware(XYDevice device, final Callback callback) {
        if (device.getFamily() == XYDevice.Family.XY4) {
            action = new XYDeviceActionGetVersionModern(device) {
                @Override
                public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                    Log.v(TAG, "statusChanged:" + status + ":" + success);
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    switch (status) {
                        case STATUS_CHARACTERISTIC_READ:
                            byte[] versionBytes = characteristic.getValue();
                            if (versionBytes.length > 0) {
                                value = "";
                                for (byte b : versionBytes) {
                                    value += String.format("%x", b);
                                }
                            }
                            break;
                        case STATUS_CHARACTERISTIC_FOUND:
                            callback.started(success);
                            if (!gatt.readCharacteristic(characteristic)) {
                                XYBase.logError(TAG, "Characteristic Read Failed");
                                statusChanged(STATUS_COMPLETED, gatt, characteristic, false);
                            }
                            break;
                        case STATUS_COMPLETED:
                            callback.completed(success);
                            break;
                    }
                    return result;
                }
            };
        } else {
            action = new XYDeviceActionGetVersion(device) {
                @Override
                public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                    Log.v(TAG, "statusChanged:" + status + ":" + success);
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    switch (status) {
                        case STATUS_CHARACTERISTIC_READ:
                            byte[] versionBytes = characteristic.getValue();
                            if (versionBytes.length > 0) {
                                value = "";
                                for (byte b : versionBytes) {
                                    value += String.format("%x", b);
                                }
                            }
                            break;
                        case STATUS_CHARACTERISTIC_FOUND:
                            callback.started(success);
                            if (!gatt.readCharacteristic(characteristic)) {
                                XYBase.logError(TAG, "Characteristic Read Failed");
                                statusChanged(STATUS_COMPLETED, gatt, characteristic, false);
                            }
                            break;
                        case STATUS_COMPLETED:
                            callback.completed(success);
                            break;
                    }
                    return result;
                }
            };
        }
    }
}
