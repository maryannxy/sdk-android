package com.xyfindables.sdk.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.crashlytics.android.Crashlytics;
import com.xyfindables.core.XYBase;
import com.xyfindables.core.XYPermissions;
import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.XYSmartScan;

import io.fabric.sdk.android.Fabric;

public class XYFindablesSdkSampleActivity extends AppCompatActivity {

    private static String TAG = XYFindablesSdkSampleActivity.class.getSimpleName();
    private BaseAdapter _adapter;
    private ListView _listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        XYBase.init(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xyfindables_sdk_sample);
        Crashlytics crashlytics = new Crashlytics();
        Fabric.with(this, crashlytics, new Crashlytics());
        _listView = findViewById(R.id.listview);
        XYSmartScan.instance.addListener("XYFindablesSdkSampleActivity", new XYSmartScan.Listener() {
            @Override
            public void entered(XYDevice device) {
                runOnUiThread (new Thread(new Runnable() {
                    public void run() {
                        _adapter.notifyDataSetChanged();
                    }
                }));
            }

            @Override
            public void exited(XYDevice device) {

            }

            @Override
            public void detected(XYDevice device) {
                runOnUiThread (new Thread(new Runnable() {
                    public void run() {
                        _adapter.notifyDataSetChanged();
                    }
                }));
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

            }
        });
        _adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                int deviceCount = XYSmartScan.instance.getDeviceCount();
                XYBase.logInfo(TAG, "BaseAdapter:getCount: " + deviceCount);
                return deviceCount;
            }

            @Override
            public Object getItem(int position) {
                return XYSmartScan.instance.getDevice(position);
            }

            @Override
            public long getItemId(int position) {
                return XYSmartScan.instance.getDevices().get(position).hashCode();
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.device_item, parent, false);
                }
                ((XYDeviceItem)convertView).setDevice((XYDevice)getItem(position));

                return convertView;
            }
        };
        _listView.setAdapter(_adapter);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        XYPermissions permissions = new XYPermissions(this);
        permissions.requestPermission(android.Manifest.permission.ACCESS_FINE_LOCATION, "Location services are needed to access and track your finders.", XYPermissions.LOCATION_PERMISSIONS_REQ_CODE);
        XYSmartScan.instance.startAutoScan(this, 5000, 10000);
    }
}
