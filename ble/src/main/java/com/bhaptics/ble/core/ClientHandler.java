package com.bhaptics.ble.core;

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

public class ClientHandler extends Handler {

    private ArrayList<TactosyClient.ConnectCallback> mConnectCallbacks;
    private ArrayList<TactosyClient.DataCallback> mDataCallbacks;

    ClientHandler() {
        super();
        mConnectCallbacks = new ArrayList<>();
        mDataCallbacks = new ArrayList<>();
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
            case Constants.MESSAGE_CONNECT_RESPONSE:
                onConnectResponse(msg);
                break;
            case Constants.MESSAGE_READ_SUCCESS:
                onRead(msg);
                break;
            case Constants.MESSAGE_WRITE_SUCCESS:
                onWrite(msg);
                break;
            case Constants.MESSAGE_READ_ERROR:
            case Constants.MESSAGE_WRITE_ERROR:
                onError(msg);
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

            for (TactosyClient.ConnectCallback callback : mConnectCallbacks) {
                callback.onConnect(device.getAddress());
            }
        }
    }

    private void onConnectResponse(Message msg) {
        Bundle data = msg.getData();

        int status = msg.arg1;
        String address = data.getString(Constants.KEY_ADDR);

        for (TactosyClient.ConnectCallback callback : mConnectCallbacks) {
            if (status == BluetoothProfile.STATE_CONNECTED) {
                callback.onConnect(address);
            } else if (status == BluetoothProfile.STATE_DISCONNECTED) {
                callback.onDisconnect(address);
            } else {
                callback.onConnectionError(address);
            }
        }
    }

    private void onRead(Message msg) {
        Bundle data = msg.getData();

        int status = msg.arg1;
        String address = data.getString(Constants.KEY_ADDR);
        UUID uuid = UUID.fromString(data.getString(Constants.KEY_CHAR_ID));

        byte[] bytes = data.getByteArray(Constants.KEY_VALUES);

        for (TactosyClient.DataCallback callback : mDataCallbacks) {
            callback.onRead(address, uuid, bytes, status);
        }
    }

    private void onWrite(Message msg) {
        Bundle data = msg.getData();

        int status = msg.arg1;
        String address = data.getString(Constants.KEY_ADDR);
        UUID uuid = UUID.fromString(data.getString(Constants.KEY_CHAR_ID));

        for (TactosyClient.DataCallback callback : mDataCallbacks) {
            callback.onWrite(address, uuid, status);
        }
    }

    private void onError(Message msg) {
        Bundle data = msg.getData();

        int errCode = Constants.MESSAGE_WRITE_ERROR;
        String address = data.getString(Constants.KEY_ADDR);
        String charId = data.getString(Constants.KEY_CHAR_ID);

        for (TactosyClient.DataCallback callback : mDataCallbacks) {
            callback.onDataError(address, charId, errCode);
        }
    }

    void addConnectCallback(TactosyClient.ConnectCallback callback) {
        mConnectCallbacks.add(callback);
    }

    void addDataCallback(TactosyClient.DataCallback callback) {
        mDataCallbacks.add(callback);
    }
}
