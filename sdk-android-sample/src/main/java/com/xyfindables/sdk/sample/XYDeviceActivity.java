package com.xyfindables.sdk.sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.XYSmartScan;
import com.xyfindables.ui.XYBaseActivity;
import com.xyfindables.ui.views.XYButton;

/**
 * Created by arietrouw on 12/28/17.
 */

public class XYDeviceActivity extends XYBaseActivity {
    public static String EXTRA_DEVICEID = "DeviceId";
    private static String TAG = XYDeviceActivity.class.getSimpleName();

    private XYDevice _device;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String deviceId = intent.getStringExtra(XYDeviceActivity.EXTRA_DEVICEID);
        _device = XYSmartScan.instance.deviceFromId(deviceId);
        if (_device == null) {
            showToast("Failed to Find Device");
            finish();

        }
        setContentView(R.layout.device_activity);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (_device != null) {
            _device.addListener(TAG, new XYDevice.Listener() {
                @Override
                public void entered(XYDevice device) {
                    update();
                    showToast("Entered");
                }

                @Override
                public void exited(XYDevice device) {
                    update();
                    showToast("Exited");
                }

                @Override
                public void detected(XYDevice device) {
                    update();
                }

                @Override
                public void buttonPressed(XYDevice device, XYDevice.ButtonType buttonType) {
                    update();
                    showToast("Button Pressed");
                }

                @Override
                public void buttonRecentlyPressed(XYDevice device, XYDevice.ButtonType buttonType) {

                }

                @Override
                public void connectionStateChanged(XYDevice device, int newState) {
                    update();
                    if (_device.isConnected()) {
                        showToast("Connected");
                    } else {
                        showToast("Disconnected");
                    }
                }

                @Override
                public void readRemoteRssi(XYDevice device, int rssi) {
                    update();
                }

                @Override
                public void updated(XYDevice device) {
                    update();
                }

                @Override
                public void statusChanged(XYSmartScan.Status status) {

                }
            });
            update();
        }

        final XYButton connectButton = findViewById(R.id.connect);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectButton.setEnabled(false);
                _device.stayConnected(XYDeviceActivity.this, true);
            }
        });

        XYButton disconnectButton = findViewById(R.id.disconnect);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectButton.setEnabled(true);
                _device.stayConnected(XYDeviceActivity.this, false);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void update() {
        runOnUiThread(new Runnable() {
            public void run() {
                if (_device != null) {
                    TextView nameView = findViewById(R.id.name);
                    nameView.setText(_device.getFamily().name());

                    TextView rssiView = findViewById(R.id.rssi);
                    rssiView.setText(String.valueOf(_device.getRssi()));

                    TextView majorView = findViewById(R.id.major);
                    majorView.setText(String.valueOf(_device.getMajor()));

                    TextView minorView = findViewById(R.id.minor);
                    minorView.setText(String.valueOf(_device.getMinor()));

                    TextView pulsesView = findViewById(R.id.pulseCount);
                    pulsesView.setText(String.valueOf(_device.getDetectCount()));

                    TextView enterView = findViewById(R.id.enterCount);
                    enterView.setText(String.valueOf(_device.getEnterCount()));

                    TextView exitView = findViewById(R.id.exitCount);
                    exitView.setText(String.valueOf(_device.getExitCount()));

                    TextView actionSuccessView = findViewById(R.id.actionSuccessCount);
                    actionSuccessView.setText(String.valueOf(_device.getActionSuccessCount()));

                    TextView actionFailCount = findViewById(R.id.actionFailCount);
                    actionFailCount.setText(String.valueOf(_device.getActionFailCount()));

                    TextView actionQueueCount = findViewById(R.id.actionQueueCount);
                    actionQueueCount.setText(String.valueOf(_device.getActionQueueCount()));

                    XYButton connectButton = findViewById(R.id.connect);
                    connectButton.setVisibility(_device.isConnected() ? View.INVISIBLE : View.VISIBLE);

                    XYButton disconnectButton = findViewById(R.id.disconnect);
                    disconnectButton.setVisibility(_device.isConnected() ? View.VISIBLE : View.INVISIBLE);
                }
            }
        });
    }
}
