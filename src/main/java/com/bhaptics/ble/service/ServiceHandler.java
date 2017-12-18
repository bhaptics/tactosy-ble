package com.bhaptics.ble.service;

import android.app.Service;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.bhaptics.ble.util.Constants;
import com.bhaptics.ble.util.LogUtils;

/**
 * Service handler implementation to be used in {@link BaseBLEService}.</br>
 * </br>
 * This handler contains connect/disconnect read/write and notification calls to tactosy device.</br>
 * </br>
 * You can extend this handler to add new characteristic and handle it by</br>
 * Overriding {@link #handleMessage(Message)}</br>
 *
 */
public class ServiceHandler extends Handler {

    private static final String TAG = LogUtils.makeLogTag(ServiceHandler.class);

    private static ServiceHandler instance = null;
    public static ServiceHandler getInstance() {
        return instance;
    }

    public ServiceHandler(Looper looper, Service service) {
        super(looper);

        instance = this;
    }

    @Override
    public void handleMessage(Message msg) {
        Log.e(TAG, "handleMessage : " + msg);
        switch (msg.what) {
            case Constants.MESSAGE_WRITE_V2:
            case Constants.MESSAGE_TURN_OFF_ALL:
                requestReceive.onReceived(msg.what, msg);
                break;
            default:
                break;
        }
    }
    public interface RequestReceiveCallback {
        void onReceived(int messageType, Message msg);
    }
    private RequestReceiveCallback requestReceive;
    public void setRequestReceiveCallback(RequestReceiveCallback requestReceived) { this.requestReceive = requestReceived; }


    private void replyToClient(Messenger client) {
        Message response = obtainMessage();

        Bundle data = new Bundle();
//        data.putParcelableArrayList(Constants.KEY_CONNECTED, (ArrayList<BluetoothDevice>) devices);

        response.setData(data);
        response.what = Constants.MESSAGE_REPLY;

        try {
            client.send(response);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

}
