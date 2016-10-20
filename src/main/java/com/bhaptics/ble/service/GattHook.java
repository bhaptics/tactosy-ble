package com.bhaptics.ble.service;

import android.bluetooth.BluetoothGatt;

public interface GattHook {
    void onConnect(BluetoothGatt gatt);
    void onDisconnect(BluetoothGatt gatt);
}
