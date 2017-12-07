package com.xyfindables.sdk.action;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.media.audiofx.AudioEffect;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import com.xyfindables.core.XYBase;
import com.xyfindables.sdk.XYDevice;

import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by arietrouw on 1/1/17.
 */

public abstract class XYDeviceAction extends XYBase {

    private static final String TAG = XYDeviceAction.class.getSimpleName();

    private XYDevice _device;
    private boolean _servicesDiscovered = false;
    private boolean _characteristicFound = false;

    public static final int STATUS_QUEUED = 1;
    public static final int STATUS_STARTED = 2;
    public static final int STATUS_SERVICE_FOUND = 3;
    public static final int STATUS_CHARACTERISTIC_FOUND = 4;
    public static final int STATUS_CHARACTERISTIC_READ = 5;
    public static final int STATUS_CHARACTERISTIC_WRITE = 6;
    public static final int STATUS_CHARACTERISTIC_UPDATED = 7;
    public static final int STATUS_COMPLETED = 8;

    private static ThreadPoolExecutor _threadPool;

    public XYDeviceAction(XYDevice device) {
        _device = device;
    }

    static {
        _threadPool = new ThreadPoolExecutor(10, 50, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    }

    public XYDevice getDevice() {
        return _device;
    }

    public String getKey() {
        return TAG + hashCode();
    }

    public void start(final Context context) {
        Log.i(TAG, this.getClass().getSuperclass().getSimpleName() + ":starting...");
        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Log.i(TAG, "running...");
                _device.queueAction(context.getApplicationContext(), XYDeviceAction.this);
                return null;
            }
        };
        asyncTask.executeOnExecutor(_threadPool);
    }

    abstract public UUID getServiceId();
    abstract public UUID getCharacteristicId();

    public boolean statusChanged(int status, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean success) {
        if (!success) {
            XYBase.logError(TAG, this.getClass().getSuperclass().getSimpleName() + ":statusChanged(failed):" + status, false);
        }
        switch (status) {
            case STATUS_QUEUED:
                break;
            case STATUS_STARTED:
                break;
            case STATUS_SERVICE_FOUND:
                if (!_servicesDiscovered) {
                    _servicesDiscovered = true;
                } else {
                    XYBase.logError(TAG, "connTest-" + this.getClass().getSuperclass().getSimpleName() + ":Second Service Found Received", false);
                }
                break;
            case STATUS_CHARACTERISTIC_FOUND:

                if (!_characteristicFound) {
                    _characteristicFound = true;
                } else {
                    XYBase.logError(TAG, "connTest-" + this.getClass().getSuperclass().getSimpleName() + ":Second Characteristic Found Received", false);
                }
                break;
            case STATUS_CHARACTERISTIC_READ:
                return true;
            case STATUS_CHARACTERISTIC_WRITE:
                return true;
            case STATUS_COMPLETED:
                if (!success) {
                    XYBase.logError(TAG, this.getClass().getSuperclass().getSimpleName() + ":Completed with Failure", false);
                }
                break;
        }
        return false;
    }

    public boolean statusChanged(BluetoothGattDescriptor descriptor ,int status, BluetoothGatt gatt, boolean success) {
        if (!success) {
            XYBase.logError(TAG, this.getClass().getSuperclass().getSimpleName() + ":statusChanged(failed):" + status, false);
        }
        switch (status) {
            case STATUS_QUEUED:
                break;
            case STATUS_STARTED:
                break;
            case STATUS_SERVICE_FOUND:
                if (!_servicesDiscovered) {
                    _servicesDiscovered = true;
                } else {
                    XYBase.logError(TAG, this.getClass().getSuperclass().getSimpleName() + ":Second Service Found Received", false);
                }
                break;
            case STATUS_CHARACTERISTIC_FOUND:

                if (!_characteristicFound) {
                    _characteristicFound = true;
                } else {
                    XYBase.logError(TAG, this.getClass().getSuperclass().getSimpleName() + ":Second Characteristic Found Received", false);
                }
                break;
            case STATUS_CHARACTERISTIC_READ:
                return true;
            case STATUS_CHARACTERISTIC_WRITE:
                return true;
            case STATUS_COMPLETED:
                if (!success) {
                    XYBase.logError(TAG, this.getClass().getSuperclass().getSimpleName() + ":Completed with Failure", false);
                }
                break;
        }
        return false;
    }
}
