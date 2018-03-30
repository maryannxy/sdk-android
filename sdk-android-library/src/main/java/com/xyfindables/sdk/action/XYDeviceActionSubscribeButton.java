package com.xyfindables.sdk.action;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import com.xyfindables.sdk.XYDeviceCharacteristic;
import com.xyfindables.sdk.XYDeviceService;
import com.xyfindables.sdk.XYDevice;

import java.util.UUID;

/**
 * Created by arietrouw on 1/1/17.
 */

public abstract class XYDeviceActionSubscribeButton extends XYDeviceAction {

    private static final String TAG = XYDeviceActionSubscribeButton.class.getSimpleName();

    public static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final int BUTTONPRESS_SINGLE = 1;
    public static final int BUTTONPRESS_DOUBLE = 2;
    public static final int BUTTONPRESS_LONG = 3;

    private BluetoothGatt _gatt = null;
    private BluetoothGattCharacteristic _characteristic = null;

    public XYDeviceActionSubscribeButton(XYDevice device) {
        super(device);
        logAction(TAG, TAG);
    }

    public void stop() {
        if (_gatt != null && _characteristic != null) {
            _gatt.setCharacteristicNotification(_characteristic, false);
            _gatt = null;
            _characteristic = null;
        } else {
            logError(TAG, "connTest-Stopping non-started button notifications", false);
        }
    }

    @Override
    public UUID getServiceId() {
        return XYDeviceService.Control;
    }

    @Override
    public UUID getCharacteristicId() {
        return XYDeviceCharacteristic.ControlButton;
    }

    @Override
    public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
        logExtreme(TAG, "statusChanged:" + status + ":" + success);
        boolean result = super.statusChanged(status, gatt, characteristic, success);
        switch (status) {
            case STATUS_CHARACTERISTIC_UPDATED: {
                logInfo(TAG, "statusChanged:Updated:" + characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
                result = false;
                break;
            }
            case STATUS_CHARACTERISTIC_FOUND: {
                logInfo(TAG, "statusChanged:Characteristic Found");
                if (!gatt.setCharacteristicNotification(characteristic, true)) {
                    logError(TAG, "connTest-Characteristic Notification Failed", false);
                } else {
                    _gatt = gatt;
                    _characteristic = characteristic;
                }
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                if (!gatt.writeDescriptor(descriptor)) {
                    result = true;
                }
                break;
            }
        }
        return result;
    }
}
