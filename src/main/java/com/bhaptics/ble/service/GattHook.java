package com.bhaptics.ble.service;

import android.bluetooth.BluetoothGatt;

/**
 * Gatt hook for {@link BaseBLEService}</br>
 *
 * You can implement it in subclass of {@link BaseBLEService} if you need more actions
 * when devices are connected/disconnecte.</br>
 */
public interface GattHook {
    void onConnect(BluetoothGatt gatt);
    void onDisconnect(BluetoothGatt gatt);
}
