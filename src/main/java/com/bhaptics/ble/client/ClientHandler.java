package com.bhaptics.ble.client;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;

import com.bhaptics.ble.util.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * {@link BaseClient}'s handler to handle tactosies' callbacks.
 */
public class ClientHandler extends Handler {


    public ClientHandler() {
        super();
    }

    private static List<Parcelable> safe(List<Parcelable> list) {
        return list == null ? new ArrayList<Parcelable>() : list;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case Constants.MESSAGE_REPLY:
                replyToService(msg);
                break;
            default:
                break;
        }
    }

    private void replyToService(Message msg) {
        Bundle data = msg.getData();

        if (data == null) {
            return;
        }

        for (Parcelable p : safe(data.getParcelableArrayList(Constants.KEY_CONNECTED))) {
            BluetoothDevice device = (BluetoothDevice) p;
        }
    }

}
