package com.bhaptics.ble.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.bhaptics.ble.BLEException;
import com.bhaptics.ble.util.Constants;
import com.bhaptics.ble.util.LogUtils;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

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

    private BluetoothGattCallback mCallback;

    private BluetoothManager mBluetoothManager = null;
    private BluetoothAdapter mAdapter = null;

    private Map<String, BluetoothGatt> mConnections;

    private Map<String, LinkedBlockingQueue<Object>> mDeviceLock;

    //debug
    public Map<String , LinkedBlockingQueue<Object>> getmDeviceLock () {
        return this.mDeviceLock;
    }
    public Map<String, BluetoothGatt> getmConnections () {
        return this.mConnections;
    }

    private Service mParent;

    // 싱글톤 적용 예정
    private static ServiceHandler instance = null;
    public static ServiceHandler getInstance() {
        return instance;
    }

    public ServiceHandler(Looper looper, Service service) {
        super(looper);

        mParent = service;
        mBluetoothManager = (BluetoothManager) mParent.getSystemService(Context.BLUETOOTH_SERVICE);
        mAdapter = mBluetoothManager.getAdapter();

        mDeviceLock = new HashMap<>();
        mConnections = new HashMap<>();

        instance = this;
    }



    @Override
    public void handleMessage(Message msg) {
        Log.e(TAG, "handleMessage : " + msg);
        switch (msg.what) {
            case Constants.MESSAGE_REPLY:
                replyToClient(msg.replyTo);
                break;
            case Constants.MESSAGE_WRITE_V2:
                if (tactFeedBack == null) {

                    Log.i(TAG, "handleMessage: failed");
                    break;
                }
                tactFeedBack.onFeedBack(msg);
                break;
            default:
                break;
        }
    }
    public interface TactFeedBack {
        void onFeedBack(Message msg);
    }
    private TactFeedBack tactFeedBack;
    public void setTactFeedBack(TactFeedBack tactFeedBack) { this.tactFeedBack = tactFeedBack; }

    private void replyToClient(Messenger client) {
        Message response = obtainMessage();

        List<BluetoothDevice> devices =
                mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);

        Bundle data = new Bundle();
        data.putParcelableArrayList(Constants.KEY_CONNECTED, (ArrayList<BluetoothDevice>) devices);

        response.setData(data);
        response.what = Constants.MESSAGE_REPLY;

        try {
            client.send(response);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

}
