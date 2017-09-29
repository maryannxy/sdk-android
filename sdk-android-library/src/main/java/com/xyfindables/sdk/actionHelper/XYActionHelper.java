package com.xyfindables.sdk.actionHelper;

import android.content.Context;

import com.xyfindables.sdk.action.XYDeviceAction;

/**
 * Created by alex.mcelroy on 9/6/2017.
 */

public abstract class XYActionHelper {

    public static int GET = 0;
    public static int SET = 1;

    public XYDeviceAction action = null;

    public interface Callback {
        void completed(boolean success);
    }

    public interface Notification {
        void updated(boolean status);
    }

    public void start(Context context) {
        action.start(context);
    }
}
