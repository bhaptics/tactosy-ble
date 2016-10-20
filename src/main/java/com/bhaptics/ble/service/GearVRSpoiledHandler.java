package com.bhaptics.ble.service;

import android.app.Service;
import android.os.Looper;
import android.os.Message;

import com.bhaptics.ble.util.Constants;

import java.util.HashSet;
import java.util.Set;

public class GearVRSpoiledHandler extends ServiceHandler {

    private Set<String> mReservedAddrs = new HashSet<>();

    public GearVRSpoiledHandler(Looper looper, Service service, Set<String> addrs) {
        super(looper, service);
        mReservedAddrs = addrs;
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);

        switch (msg.what) {
            case Constants.MESSAGE_REPLY:
                for (String addr : mReservedAddrs) {
                    connect(addr);
                }
                break;
            default:
                break;
        }
    }
}
