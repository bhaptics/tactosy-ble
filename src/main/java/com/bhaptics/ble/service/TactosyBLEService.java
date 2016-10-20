package com.bhaptics.ble.service;

import android.os.Looper;

public class TactosyBLEService extends BaseBLEService {

    private ServiceHandler mHandler;

    @Override
    protected ServiceHandler getServiceHandler(Looper looper) {
        if (mHandler == null) {
            mHandler = new ServiceHandler(looper, this);
        }

        return mHandler;
    }
}
