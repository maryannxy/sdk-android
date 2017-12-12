package com.xyfindables.sdk.action.dialog;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.XYDeviceCharacteristic;
import com.xyfindables.sdk.XYDeviceService;
import com.xyfindables.sdk.action.XYDeviceAction;

import java.util.UUID;

/**
 * Created by alex.mcelroy on 11/22/2017.
 */

public abstract class SubscribeSpotaNotifications extends XYDeviceAction {
    private static final String TAG = SubscribeSpotaNotifications.class.getSimpleName();

    private BluetoothGatt _gatt = null;
    private BluetoothGattCharacteristic _characteristic = null;

    public SubscribeSpotaNotifications(XYDevice device) {
        super(device);
        logAction(TAG, TAG);
    }

    public void stop() {
        if (_gatt != null && _characteristic != null) {
            _gatt.setCharacteristicNotification(_characteristic, false);
            _gatt = null;
            _characteristic = null;
        } else {
            logError(TAG, "connTest-Stopping non-started notifications", false);
        }
    }

    @Override
    public UUID getServiceId() {
        return XYDeviceService.SPOTA_SERVICE_UUID;
    }

    @Override
    public UUID getCharacteristicId() {
        return XYDeviceCharacteristic.SPOTA_SERV_STATUS_UUID;
    }

    @Override
    public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
        logExtreme(TAG, "statusChanged:" + status + ":" + success);
        boolean result = super.statusChanged(status, gatt, characteristic, success);
        switch (status) {
            case STATUS_CHARACTERISTIC_UPDATED: {
//                Log.i(TAG, "testOta-statusChanged:Updated:" + characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
                return true;
            }
            case STATUS_CHARACTERISTIC_FOUND: {
                logExtreme(TAG, "testOta-subscribeSpotaNotifications:statusChanged:Characteristic Found");
                if (!gatt.setCharacteristicNotification(characteristic, true)) {
                    logError(TAG, "testOta-notifications-Characteristic Notification Failed", false);
                    return true;
                } else {
                    logExtreme(TAG, "testOta-notifications-Characteristic Notification Succeeded");
                    _gatt = gatt;
                    _characteristic = characteristic;
                }
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(XYDeviceCharacteristic.SPOTA_DESCRIPTOR_UUID);
                descriptor.setValue(new byte[] {0x01, 0x00});
                if (!gatt.writeDescriptor(descriptor)) {
                    logError(TAG, "testOta-notifications-Write Descriptor failed", false);
                    return true;
                } else {
                    logExtreme(TAG, "testOta-notifications-Write Descriptor succeeded");
                }
                return false;
            }
        }
        return result;
    }
}
