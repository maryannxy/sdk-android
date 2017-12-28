package com.xyfindables.sdk.sample;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.xyfindables.sdk.XYDevice;

/**
 * Created by arietrouw on 12/27/17.
 */

public class XYDeviceItem extends RelativeLayout {

    private XYDevice _device;

    public XYDeviceItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setDevice(XYDevice device) {
        if (device != null) {
            TextView nameView = (TextView) findViewById(R.id.name);
            nameView.setText(device.getFamily().name());
            TextView idView = (TextView) findViewById(R.id.id);
            idView.setText(String.valueOf(device.getRssi()));
            _device = device;
        }
    }
}
