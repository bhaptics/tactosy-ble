package com.bhaptics.ble.service;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.bhaptics.ble.util.Constants;
import com.bhaptics.ble.util.LogUtils;

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

public class BaseGattCallback extends BluetoothGattCallback {

    private static final String TAG = LogUtils.makeLogTag(BaseGattCallback.class);
    private Messenger mClient;
    private Map<String, LinkedBlockingQueue<Object>> mDeviceLock;

    public BaseGattCallback(Messenger client, Map<String, LinkedBlockingQueue<Object>> lock) {
        mClient = client;
        mDeviceLock = lock;
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

        BluetoothDevice device = gatt.getDevice();
        String address = device.getAddress();

        switch (newState) {
            case BluetoothProfile.STATE_CONNECTED:
                Log.i(TAG, "onConnectionStateChange: " + address + " connected.");
                gatt.discoverServices();
                break;
            case BluetoothProfile.STATE_DISCONNECTED:
                Log.i(TAG, "onConnectionStateChange: " + address + " disconnected.");
                onConnectionResponse(gatt, newState);

//                Set<String> addrs = new HashSet<>();

//                for (BluetoothDevice _device: mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT)) {
//                    if (mBluetoothGattMap.containsKey(_device.getAddress())) {
//                        addrs.add(_device.getAddress());
//                    }
//                }

//                mSharedPref.edit().putStringSet(KEY_SAVED_ADDRS, addrs).apply();
                break;
            default:
                Log.d(TAG, "New state not processed: " + newState);
                break;
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        // NOTE It is assumed when characteristic only contained by tactosy is read.
        // TODO Filter only tactosys using uuid of advertising.
        Log.i(TAG, "onServicesDiscovered() " + status);

//        Set<String> addrs = new HashSet<>();

//        for (BluetoothDevice device: mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT)) {
//            if (mBluetoothGattMap.containsKey(device.getAddress())) {
//                addrs.add(device.getAddress());
//            }
//        }
//
//        mSharedPref.edit().putStringSet(KEY_SAVED_ADDRS, addrs).apply();

        onConnectionResponse(gatt, BluetoothProfile.STATE_CONNECTED);
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        LinkedBlockingQueue<Object> queue = mDeviceLock.get(gatt.getDevice().getAddress());
        assert queue != null;

        Message msg = Message.obtain();
        msg.what = Constants.MESSAGE_READ_SUCCESS;
        try {
            queue.put(new Object());
        } catch (InterruptedException e) {
            e.printStackTrace();
            msg.what = Constants.MESSAGE_READ_ERROR;
        }

        Bundle data = new Bundle();
        data.putString(Constants.KEY_ADDR, gatt.getDevice().getAddress());
        data.putString(Constants.KEY_CHAR_ID, characteristic.getUuid().toString());
        data.putByteArray(Constants.KEY_VALUES, characteristic.getValue());

        msg.arg1 = status;
        msg.setData(data);

        try {
            mClient.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        LinkedBlockingQueue<Object> queue = mDeviceLock.get(gatt.getDevice().getAddress());
        assert queue != null;

        Message msg = Message.obtain();
        msg.what = Constants.MESSAGE_WRITE_SUCCESS;
        try {
            queue.put(new Object());
        } catch (InterruptedException e) {
            e.printStackTrace();
            msg.what = Constants.MESSAGE_WRITE_ERROR;
        }

        Bundle data = new Bundle();
        data.putString(Constants.KEY_ADDR, gatt.getDevice().getAddress());
        data.putString(Constants.KEY_CHAR_ID, characteristic.getUuid().toString());
        msg.arg1 = status;
        msg.setData(data);

        try {
            mClient.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);

        LinkedBlockingQueue<Object> queue = mDeviceLock.get(gatt.getDevice().getAddress());
        assert queue != null;

        try {
            queue.put(new Object());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        Message msg = Message.obtain();
        msg.what = Constants.MESSAGE_READ_SUCCESS;

        Bundle data = new Bundle();
        data.putString(Constants.KEY_ADDR, gatt.getDevice().getAddress());
        data.putString(Constants.KEY_CHAR_ID, characteristic.getUuid().toString());
        data.putByteArray(Constants.KEY_VALUES, characteristic.getValue());

        msg.setData(data);

        try {
            mClient.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    protected void onConnectionResponse(BluetoothGatt gatt, int newState) {
        Message msg = Message.obtain();
        msg.what = Constants.MESSAGE_CONNECT_RESPONSE;
        msg.arg1 = newState;

        Bundle data = new Bundle();
        data.putString(Constants.KEY_ADDR, gatt.getDevice().getAddress());

        msg.setData(data);

        try {
            mClient.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
