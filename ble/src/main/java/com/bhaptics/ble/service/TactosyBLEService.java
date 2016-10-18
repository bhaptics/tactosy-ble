package com.bhaptics.ble.service;

import android.os.Looper;

public class TactosyBLEService extends BaseBLEService {
    @Override
    protected ServiceHandler getServiceHandler(Looper looper) {
        return new ServiceHandler(looper, this);
    }
}
