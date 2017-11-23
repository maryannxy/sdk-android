package com.xyfindables.sdk.action.dialog;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Log;

import com.xyfindables.core.XYBase;
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
        Log.v(TAG, TAG);
    }

    public void stop() {
        if (_gatt != null && _characteristic != null) {
            _gatt.setCharacteristicNotification(_characteristic, false);
            _gatt = null;
            _characteristic = null;
        } else {
            XYBase.logError(TAG, "Stopping non-started notifications");
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
        Log.v(TAG, "statusChanged:" + status + ":" + success);
        boolean result = super.statusChanged(status, gatt, characteristic, success);
        switch (status) {
            case STATUS_CHARACTERISTIC_UPDATED: {
//                Log.i(TAG, "testOta-statusChanged:Updated:" + characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
                return true;
            }
            case STATUS_CHARACTERISTIC_FOUND: {
                Log.i(TAG, "testOta-subscribeSpotaNotifications:statusChanged:Characteristic Found");
                if (!gatt.setCharacteristicNotification(characteristic, true)) {
                    Log.e(TAG, "testOta-notifications-Characteristic Notification Failed");
                } else {
                    _gatt = gatt;
                    _characteristic = characteristic;
                }
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(XYDeviceCharacteristic.SPOTA_DESCRIPTOR_UUID);
                descriptor.setValue(new byte[] {0x01, 0x00});
                if (!gatt.writeDescriptor(descriptor)) {
                    Log.e(TAG, "testOta-notifications-Write Descriptor failed");
                    statusChanged(STATUS_COMPLETED, gatt, characteristic, false);
                } else {
                    Log.v(TAG, "testOta-notifications-Write Descriptor succeeded");
                }
                return true;
            }
        }
        return result;
    }
}
