package com.bhaptics.ble.util;

import com.bhaptics.ble.client.BaseClient.OnReadyListener;

import java.util.ArrayList;

public class ReadyQueue extends ArrayList<OnReadyListener> {
    private boolean mReady = false;

    public void put(OnReadyListener listener) {
        super.add(listener);
        if (mReady) {
            listener.onReady();
            clear();
        }
    }

    public void notifyReady() {
        mReady = true;
        for (OnReadyListener listener: this) {
            listener.onReady();
        }

        clear();
    }

    public boolean isReady() {
        return mReady;
    }
}
