package com.xyfindables.sdk.sample;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.xyfindables.sdk.XYDevice;
import com.xyfindables.sdk.XYSmartScan;

import java.util.ArrayList;

public class XYDeviceAdapter extends BaseAdapter {

    private static String TAG = XYDeviceAdapter.class.getSimpleName();

    private Activity _activity;
    private ArrayList<XYDevice> _devices;

    public XYDeviceAdapter(Activity activity) {
        _activity = activity;
        _devices = new ArrayList<XYDevice>();
        XYSmartScan.instance.addListener(TAG, new XYDevice.Listener() {
            @Override
            public void entered(final XYDevice device) {
                _activity.runOnUiThread (new Thread(new Runnable() {
                    public void run() {
                        _devices.add(device);
                        notifyDataSetChanged();
                    }
                }));
            }

            @Override
            public void exited(final XYDevice device) {
                _activity.runOnUiThread (new Thread(new Runnable() {
                    public void run() {
                        _devices.remove(device);
                        notifyDataSetChanged();
                    }
                }));
            }

            @Override
            public void detected(XYDevice device) {
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

            @Override
            public void connectionStateChanged(XYDevice device, int newState) {

            }

            @Override
            public void readRemoteRssi(XYDevice device, int rssi) {

            }

        });
    }

    @Override
    public int getCount() {
        return _devices.size();
    }

    @Override
    public Object getItem(int position) {
        return _devices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = _activity.getLayoutInflater().inflate(R.layout.device_item, parent, false);
        }
        ((XYDeviceItemView)convertView).setDevice((XYDevice)getItem(position));

        return convertView;
    }
}
