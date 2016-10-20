package com.bhaptics.ble.service;

import android.bluetooth.BluetoothGatt;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.HashSet;
import java.util.Set;

public class GearVRSpoiledService extends TactosyBLEService implements GattHook {

    private static final String KEY_SAVED_ADDRS = TactosyBLEService.class.getName() + ".KEY_SAVED_ADDRS";

    private SharedPreferences mSharedPref;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int ret = super.onStartCommand(intent, flags, startId);

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        Set<String> addrs = mSharedPref.getStringSet(KEY_SAVED_ADDRS, new HashSet<String>());
        for (String addr : addrs) {
            getServiceHandler(getThreadLooper()).connect(addr);
        }

        // clear reserved addrs after trying to conenct.
        mSharedPref.edit().putStringSet(KEY_SAVED_ADDRS, new HashSet<String>()).apply();

        return ret;
    }

    @Override
    public void onConnect(BluetoothGatt gatt) {
        Set<String> addrs = mSharedPref.getStringSet(KEY_SAVED_ADDRS, new HashSet<String>());

        addrs.add(gatt.getDevice().getAddress());

        mSharedPref.edit().putStringSet(KEY_SAVED_ADDRS, addrs).apply();
    }

    @Override
    public void onDisconnect(BluetoothGatt gatt) {
        Set<String> addrs = mSharedPref.getStringSet(KEY_SAVED_ADDRS, new HashSet<String>());

        addrs.remove(gatt.getDevice().getAddress());

        mSharedPref.edit().putStringSet(KEY_SAVED_ADDRS, addrs).apply();
    }
}
