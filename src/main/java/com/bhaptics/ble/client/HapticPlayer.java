package com.bhaptics.ble.client;

import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;

import com.bhaptics.ble.model.Feedback;
import com.bhaptics.ble.util.Constants;
import com.google.gson.Gson;

import java.util.List;

/**
 * Detail implementation of {@link BaseClient}.</br>
 * </br>
 * There are scanning, reading and writing tactosies' characteristics.</br>
 */
public class HapticPlayer extends BaseClient {
    private static final String TAG = "HapticPlayer";
    private static HapticPlayer sInstance = null;

    public static HapticPlayer getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new HapticPlayer(context);
        }
        return sInstance;
    }

    private ClientHandler mClientHandler;

    @Override
    protected ClientHandler getClientHandler() {
        if (mClientHandler == null) {
            mClientHandler = new ClientHandler();
        }
        return mClientHandler;
    }

    protected HapticPlayer(Context context) {
        super();
        mClientHandler = getClientHandler();
    }

    @Override
    public void bindService(Context context) {

        super.bindService(context);
    }

    public void submit(List<Feedback> feedback) {
        Message msg = new Message();
        Bundle data = new Bundle();

        String s = new Gson().toJson(feedback);

        data.putString(Constants.KEY_VALUES, s);

        msg.what = Constants.MESSAGE_WRITE_V2;

        msg.setData(data);
        try {
            getService().send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void turnOff() {
        Message msg = new Message();
        Bundle data = new Bundle();

        msg.what = Constants.MESSAGE_TURN_OFF_ALL;

        msg.setData(data);
        try {
            getService().send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

}
