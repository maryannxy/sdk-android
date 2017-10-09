package com.xyfindables.sdk.actionHelper;

import android.content.Context;

import com.xyfindables.sdk.action.XYDeviceAction;

/**
 * Created by alex.mcelroy on 9/6/2017.
 */

public abstract class XYActionHelper {

    public XYDeviceAction action = null;

    protected interface Callback {
        void completed(boolean success);
    }

    protected interface Notification {
        void updated(boolean status);
    }

    public void start(Context context) {
        action.start(context);
    }
}
