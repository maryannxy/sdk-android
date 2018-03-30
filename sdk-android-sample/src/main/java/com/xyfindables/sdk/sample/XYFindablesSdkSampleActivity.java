package com.xyfindables.sdk.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.crashlytics.android.Crashlytics;
import com.xyfindables.core.XYBase;
import com.xyfindables.core.XYPermissions;
import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.XYSmartScan;
import com.xyfindables.ui.XYBaseActivity;

import io.fabric.sdk.android.Fabric;

public class XYFindablesSdkSampleActivity extends XYBaseActivity {

    private static String TAG = XYFindablesSdkSampleActivity.class.getSimpleName();
    private BaseAdapter _adapter;
    private ListView _listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Crashlytics crashlytics = new Crashlytics();
        Fabric.with(this, crashlytics, new Crashlytics());
        XYBase.init(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xyfindables_sdk_sample);
        _listView = findViewById(R.id.listview);
        _adapter = new XYDeviceAdapter(this);
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
        XYSmartScan.instance.startAutoScan(this, 0, 30000);
    }
}
