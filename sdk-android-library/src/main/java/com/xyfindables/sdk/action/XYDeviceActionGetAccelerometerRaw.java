package com.xyfindables.sdk.action;

        import android.bluetooth.BluetoothGatt;
        import android.bluetooth.BluetoothGattCharacteristic;
        import com.xyfindables.sdk.XYDevice;
        import com.xyfindables.sdk.XYDeviceCharacteristic;
        import com.xyfindables.sdk.XYDeviceService;

        import java.util.UUID;

/**
 * Created by alex.mcelroy on 5/4/2017.
 */

public abstract class XYDeviceActionGetAccelerometerRaw extends XYDeviceAction {

    private static final String TAG = XYDeviceActionGetAccelerometerRaw.class.getSimpleName();

    public byte[] value;

    public XYDeviceActionGetAccelerometerRaw(XYDevice device) {
        super(device);
        logAction(TAG, TAG);
    }

    @Override
    public UUID getServiceId() {
        return XYDeviceService.Sensor;
    }

    @Override
    public UUID getCharacteristicId() {
        return XYDeviceCharacteristic.SensorRaw;
    }

    @Override
    public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
        logExtreme(TAG, "statusChanged:" + status + ":" + success);
        boolean result = super.statusChanged(status, gatt, characteristic, success);
        switch (status) {
            case STATUS_CHARACTERISTIC_READ:
                value = characteristic.getValue();
                break;
            case STATUS_CHARACTERISTIC_FOUND:
                if (!gatt.readCharacteristic(characteristic)) {
                    logError(TAG, "connTest-Characteristic Read Failed", false);
                    result = true;
                }
                break;
        }
        return result;
    }
}
