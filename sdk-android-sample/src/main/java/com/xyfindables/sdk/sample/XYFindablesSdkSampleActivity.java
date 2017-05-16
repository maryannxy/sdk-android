package com.xyfindables.sdk.sample;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.xyfindables.core.XYBase;

import io.fabric.sdk.android.Fabric;

public class XYFindablesSdkSampleActivity extends Activity {

    private static String TAG = XYFindablesSdkSampleActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        XYBase.init(this);
        super.onCreate(savedInstanceState);
        Crashlytics crashlytics = new Crashlytics();
        Fabric.with(this, crashlytics, new Crashlytics());

        setContentView(R.layout.activity_xyfindables_sdk_sample);

        TextView txtCrashlyticsVersion = (TextView) findViewById(R.id.txtCrashlyticsVersion);
        txtCrashlyticsVersion.setText(crashlytics.getVersion());

        TextView txtCrashlyticsStatus = (TextView) findViewById(R.id.txtCrashlyticsStatus);
        txtCrashlyticsStatus.setText("Initialized");

        Button btnTestLogging = (Button) findViewById(R.id.btnTestLogging);
        btnTestLogging.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                XYBase.logError(TAG, "logError Test");
                XYBase.logException(TAG, new Exception("XYFindablesCoreSampleActivity Test Exception"), false);
                XYBase.logAction(TAG, "logAction Test");
                XYBase.logError(TAG, "logError", false);
                XYBase.logExtreme(TAG, "logExtreme Test");
                XYBase.logStatus(TAG, "logStatus Test");
            }
        });

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }
}