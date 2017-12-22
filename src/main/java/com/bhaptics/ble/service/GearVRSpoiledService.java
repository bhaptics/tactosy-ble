package com.bhaptics.ble.service;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Looper;

import java.util.HashSet;
import java.util.Set;

public class GearVRSpoiledService extends TactosyBLEService {

    private static final String KEY_PREF = GearVRSpoiledService.class.getName() + ".KEY_PREF";
    private static final String KEY_SAVED_ADDRS = GearVRSpoiledService.class.getName() + ".KEY_SAVED_ADDRS";

    private SharedPreferences mSharedPref;
    private Set<String> mReservedAddrs;

    @Override
    protected ServiceHandler getServiceHandler(Looper looper) {
        mSharedPref = getSharedPreferences(KEY_PREF, MODE_PRIVATE);
        mReservedAddrs = mSharedPref.getStringSet(KEY_SAVED_ADDRS, new HashSet<String>());

        return new GearVRSpoiledHandler(looper, this, mReservedAddrs);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int ret = super.onStartCommand(intent, flags, startId);

        // clear reserved addrs after trying to conenct.
        mSharedPref.edit().clear().apply();

        return ret;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSharedPref.edit().putStringSet(KEY_SAVED_ADDRS, mReservedAddrs).apply();
    }
}
