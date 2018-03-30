package com.xyfindables.sdk.sample;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.XYSmartScan;

/**
 * Created by arietrouw on 12/28/17.
 */

public class XYBLEStatsView extends LinearLayout {
    private static String TAG = XYBLEStatsView.class.getSimpleName();

    public XYBLEStatsView(final Context context, AttributeSet attrs) {
        super(context, attrs);
        XYSmartScan.instance.addListener(TAG, new XYDevice.Listener() {
            @Override
            public void entered(XYDevice device) {
                update();
            }

            @Override
            public void exited(XYDevice device) {
                update();
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
            public void statusChanged(XYSmartScan.Status status) {

            }

            @Override
            public void updated(XYDevice device) {
                update();
            }

            @Override
            public void connectionStateChanged(XYDevice device, int newState) {

            }

            @Override
            public void readRemoteRssi(XYDevice device, int rssi) {

            }

        });
    }

    public void update() {
        post(new Runnable() {
            public void run() {
                TextView pulseCountView = findViewById(R.id.pulses);
                if (pulseCountView != null) {
                    pulseCountView.setText(String.valueOf(XYSmartScan.instance.getPulseCount()));
                }

                TextView pulsePerSecView = findViewById(R.id.pulsesPerSecond);
                if (pulsePerSecView != null) {
                    pulsePerSecView.setText(String.valueOf(XYSmartScan.instance.getPulsesPerSecond()));
                }
            }
        });
    }
}
