package com.xyfindables.sdk.actionHelper;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.action.XYDeviceActionBuzzModernConfig;

import java.util.Arrays;

/**
 * Created by alex.mcelroy on 10/20/2017.
 */

public class XYBeepConfig extends XYActionHelper {
    private static final String TAG = XYBeep.class.getSimpleName();

    public interface Callback extends XYActionHelper.Callback {
        void started(boolean success);
    }

    public XYBeepConfig(XYDevice device, int slot, byte[] song, final Callback callback) {
        if (device.getFamily() == XYDevice.Family.XY4) {
            byte[] slotArray = {(byte)slot};
            byte[] config = new byte[257];
            System.arraycopy(slotArray, 0, config, 0, 1);
            System.arraycopy(song, 0, config, 1, song.length);
            action = new XYDeviceActionBuzzModernConfig(device, config) {
                @Override
                public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic
                        characteristic, boolean success) {
                    logExtreme(TAG, "statusChanged:" + status + ":" + success);
                    boolean result = super.statusChanged(status, gatt, characteristic, success);
                    switch (status) {
                        case STATUS_STARTED:
                            callback.started(success);
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