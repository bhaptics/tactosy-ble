package com.bhaptics.ble.core;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;

import com.bhaptics.ble.util.Constants;
import com.bhaptics.ble.model.Device;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class ResponseHandler extends Handler {
    private static final int TACTOSY_APPEARANCE = 508;

    public static final int EXTRA_FLAG_CONNECTED = 1;

    private ArrayList<TactosyManager.ConnectCallback> mConnectCallbacks;
    private ArrayList<TactosyManager.DataCallback> mDataCallbacks;
    private ScanCallbackInner mScanCallback;

    interface ScanCallbackInner {
        void onTactosyScan(BluetoothDevice device, int type, int flags);
    }

    ResponseHandler(ScanCallbackInner scanCallback) {
        super();
        mConnectCallbacks = new ArrayList<>();
        mDataCallbacks = new ArrayList<>();
        mScanCallback = scanCallback;
    }

    private static List<Parcelable> safe(List<Parcelable> list) {
        return list == null ? new ArrayList<Parcelable>() : list;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case Constants.MESSAGE_REPLY:
                Bundle data = msg.getData();

                if (data == null) {
                    break;
                }
                for (Parcelable p: safe(data.getParcelableArrayList(Constants.KEY_CONNECTED))) {
                    BluetoothDevice device = (BluetoothDevice) p;
                    mScanCallback.onTactosyScan(device, Device.DEVICETYPE_TACTOSY, EXTRA_FLAG_CONNECTED);

                    for (TactosyManager.ConnectCallback callback: mConnectCallbacks) {
                        callback.onConnect(device.getAddress());
                    }
                }

                break;
            case Constants.MESSAGE_SCAN_RESPONSE:
                BluetoothDevice device = (BluetoothDevice) msg.obj;
                int type = msg.arg1 == TACTOSY_APPEARANCE ? Device.DEVICETYPE_TACTOSY :
                                                            Device.DEVICETYPE_OTHER;
                mScanCallback.onTactosyScan(device, type, 0);
                break;
            case Constants.MESSAGE_CONNECT_RESPONSE:
                int status = msg.arg1;
                data = msg.getData();
                String address = data.getString(Constants.KEY_ADDR);

                for (TactosyManager.ConnectCallback callback: mConnectCallbacks) {
                    if (status == BluetoothProfile.STATE_CONNECTED) {
                        callback.onConnect(address);
                    } else if (status == BluetoothProfile.STATE_DISCONNECTED) {
                        callback.onDisconnect(address);
                    } else {
                        callback.onConnectionError(address);
                    }
                }

                break;
            case Constants.MESSAGE_READ_SUCCESS:
                status = msg.arg1;
                data = msg.getData();
                address = data.getString(Constants.KEY_ADDR);
                UUID uuid = UUID.fromString(data.getString(Constants.KEY_CHAR_ID));
                byte[] bytes = data.getByteArray(Constants.KEY_VALUES);

                for (TactosyManager.DataCallback callback: mDataCallbacks) {
                    callback.onRead(address, uuid, bytes, status);
                }
                break;
            case Constants.MESSAGE_WRITE_SUCCESS:
                status = msg.arg1;
                data = msg.getData();
                address = data.getString(Constants.KEY_ADDR);
                uuid = UUID.fromString(data.getString(Constants.KEY_CHAR_ID));

                for (TactosyManager.DataCallback callback: mDataCallbacks) {
                    callback.onWrite(address, uuid, status);
                }
                break;

            case Constants.MESSAGE_READ_ERROR:
                int errCode = Constants.MESSAGE_READ_ERROR;
                data = msg.getData();
                address = data.getString(Constants.KEY_ADDR);
                String charId = data.getString(Constants.KEY_CHAR_ID);

                for (TactosyManager.DataCallback callback: mDataCallbacks) {
                    callback.onDataError(address, charId, errCode);
                }
            case Constants.MESSAGE_WRITE_ERROR:
                errCode = Constants.MESSAGE_WRITE_ERROR;
                data = msg.getData();
                address = data.getString(Constants.KEY_ADDR);
                charId = data.getString(Constants.KEY_CHAR_ID);

                for (TactosyManager.DataCallback callback: mDataCallbacks) {
                    callback.onDataError(address, charId, errCode);
                }
                break;
            default:
                break;
        }
    }

    void addConnectCallback(TactosyManager.ConnectCallback callback) {
        mConnectCallbacks.add(callback);
    }

    void addDataCallback(TactosyManager.DataCallback callback) {
        mDataCallbacks.add(callback);
    }
}
