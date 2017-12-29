package com.xyfindables.sdk.sample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.XYSmartScan;

/**
 * Created by arietrouw on 12/27/17.
 */

public class XYDeviceItemView extends RelativeLayout {

    private static String TAG = XYDeviceItemView.class.getSimpleName();

    private XYDevice _device;

    public XYDeviceItemView(final Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (_device != null) {
                    Intent intent = new Intent(context, XYDeviceActivity.class);
                    intent.putExtra(XYDeviceActivity.EXTRA_DEVICEID, _device.getId());
                    context.startActivity(intent);
                }
            }
        });
    }

    public void update() {
        post(new Runnable() {
            public void run() {
                TextView nameView = findViewById(R.id.name);
                nameView.setText(_device.getFamily().name());

                TextView rssiView = findViewById(R.id.rssi);
                rssiView.setText(String.valueOf(_device.getRssi()));

                TextView majorView = findViewById(R.id.major);
                majorView.setText(String.valueOf(_device.getMajor()));

                TextView minorView = findViewById(R.id.minor);
                minorView.setText(String.valueOf(_device.getMinor()));

                TextView pulsesView = findViewById(R.id.pulses);
                pulsesView.setText(String.valueOf(_device.getDetectCount()));
            }
        });
    }

    public void setDevice(XYDevice device) {
        if (_device != null) {
            _device.removeListener(TAG);
            _device = null;
        }
        if (device != null) {
            _device = device;
            _device.addListener(TAG, new XYDevice.Listener() {
                @Override
                public void entered(XYDevice device) {

                }

                @Override
                public void exited(XYDevice device) {

                }

                @Override
                public void detected(XYDevice device) {
                    update();
                }

                @Override
                public void buttonPressed(XYDevice device, XYDevice.ButtonType buttonType) {

                }

                @Override
                public void buttonRecentlyPressed(XYDevice device, XYDevice.ButtonType buttonType) {

                }

                @Override
                public void connectionStateChanged(XYDevice device, int newState) {

                }

                @Override
                public void readRemoteRssi(XYDevice device, int rssi) {
                    update();
                }

                @Override
                public void updated(XYDevice device) {

                }

                @Override
                public void statusChanged(XYSmartScan.Status status) {

                }
            });
            update();
        }
    }
}
