package com.xyfindables.sdk.actionHelper;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.action.XYDeviceActionUnlock;
import com.xyfindables.sdk.action.XYDeviceActionUnlockModern;

/**
 * Created by alex.mcelroy on 9/6/2017.
 */

public class XYUnlock extends XYActionHelper {

    private final static String TAG = XYUnlock.class.getSimpleName();

    public interface Callback extends XYActionHelper.Callback {

    }

    public static final byte[] _defaultUnlockCode = {(byte) 0x2f, (byte) 0xbe, (byte) 0xa2, (byte) 0x07, (byte) 0x52, (byte) 0xfe, (byte) 0xbf, (byte) 0x31, (byte) 0x1d, (byte) 0xac, (byte) 0x5d, (byte) 0xfa, (byte) 0x7d, (byte) 0x77, (byte) 0x76, (byte) 0x80};
    public static final byte[] _defaultXy4UnlockCode = {(byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x05, (byte) 0x06, (byte) 0x07, (byte) 0x08, (byte) 0x09, (byte) 0x0a, (byte) 0x0b, (byte) 0x0c, (byte) 0x0d, (byte) 0x0e, (byte) 0x0f};

    public XYUnlock(XYDevice device, byte[] value, final Callback callback) {
        if (device.getFamily() == XYDevice.Family.XY4) {
            action = new XYDeviceActionUnlockModern(device, _defaultXy4UnlockCode) {
                @Override
                public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                    logExtreme(TAG, "statusChanged:" + status + ":" + success);
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    switch (status) {
                        case STATUS_COMPLETED:
                            callback.completed(success);
                            break;
                    }
                    return result;
                }
            };
        } else {
            action = new XYDeviceActionUnlock(device, value) {
                @Override
                public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
                    logExtreme(TAG, "statusChanged:" + status + ":" + success);
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    switch (status) {
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
