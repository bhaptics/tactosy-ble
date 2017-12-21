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
import com.bhaptics.ble.client.TactosyClient;
import com.bhaptics.ble.model.Device;
import com.bhaptics.ble.model.Feedback;
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
 * @see BaseGattCallback
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
            case Constants.MESSAGE_CONNECT:
                connect(msg);
                break;
            case Constants.MESSAGE_DISCONNECT:
                disconnect(msg);
                break;
            case Constants.MESSAGE_READ:
                readCharacteristic(msg);
                break;
            case Constants.MESSAGE_WRITE:
                writeCharacteristic(msg);
                break;
            case Constants.MESSAGE_WRITE_V2:
                tactFeedBack.onFeedBack(msg);
                break;
            case Constants.MESSAGE_SET_NOTIFICATION:
                setNotification(msg);
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

    protected BluetoothGattCallback getGattCallback(Service service, Messenger client,
                                                    Map<String, LinkedBlockingQueue<Object>> lock) {
        return new BaseGattCallback(service, client, lock);
    }

    private void replyToClient(Messenger client) {
        mCallback = getGattCallback(mParent, client, mDeviceLock);
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

    private void connect(Message msg) {
        Bundle data = msg.getData();
        connect(data.getString(Constants.KEY_ADDR));
    }

    protected void connect(String addr) {
        if (addr == null || addr.isEmpty()) {
            throw new BLEException("Address not provided");
        }

        BluetoothDevice device = mAdapter.getRemoteDevice(addr);

        if (device == null) {
            throw new BLEException("Address is invalid");
        }

        int connState = mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT);

        if (connState != BluetoothProfile.STATE_DISCONNECTED) {
            Log.w(TAG, "Connection is now established, extablishing or trying to finish.");
            return;
        }

        BluetoothGatt conn = device.connectGatt(mParent, false, mCallback);

        mConnections.put(addr, conn);
    }

    private void disconnect(Message msg) {
        Bundle data = msg.getData();
        disconnect(data.getString(Constants.KEY_ADDR));
    }

    protected void disconnect(String addr) {
        if (addr == null || addr.isEmpty()) {
            throw new BLEException("Address not provided");
        }

        BluetoothGatt conn = mConnections.get(addr);

        if (conn == null) {
            Log.w(TAG, "Maybe connection is broken.");
            return;
        }

        conn.disconnect();
        mConnections.remove(addr);

    }

    private void readCharacteristic(Message msg) {
        Bundle data = msg.getData();
        String addr = data.getString(Constants.KEY_ADDR);

        if (addr == null || addr.isEmpty()) {
            throw new BLEException("Address not provided");
        }

        LinkedBlockingQueue<Object> queue = mDeviceLock.get(addr);
        if (queue == null) {
            queue = new LinkedBlockingQueue<>(1);
            mDeviceLock.put(addr, queue);
        } else {
            try {
                // TODO more refined impl of blocking behavior.
                // it blocks all other (devices') requests when it blocks.
                queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
        }

        UUID serviceId = UUID.fromString(data.getString(Constants.KEY_SERVICE_ID));
        UUID charId = UUID.fromString(data.getString(Constants.KEY_CHAR_ID));

        BluetoothGatt gatt = mConnections.get(addr);
        if (gatt == null) {
            throw new BLEException("Attempt to reading broken connection");
        }

        BluetoothGattService service = gatt.getService(serviceId);
        if (service == null) {
            throw new BLEException("Attempt to reading non-existing service: " + serviceId);
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(charId);
        if (characteristic == null) {
            throw new BLEException("Attempt to reading non-existing characteristic");
        }

        gatt.readCharacteristic(characteristic);
    }

    private void writeCharacteristic(Message msg) {
        Bundle data = msg.getData();
        String addr = data.getString(Constants.KEY_ADDR);

        if (addr == null || addr.isEmpty()) {
            throw new BLEException("Address not provided");
        }

        LinkedBlockingQueue<Object> queue = mDeviceLock.get(addr);
        if (queue == null) {
            queue = new LinkedBlockingQueue<>(1);
            mDeviceLock.put(addr, queue);
        } else {
            try {
                // TODO more refined impl of blocking behavior.
                // it blocks all other (devices') requests when it blocks.
                queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
        }
        Log.e(TAG, msg.getTarget().toString());

        try {
            UUID serviceId = UUID.fromString(data.getString(Constants.KEY_SERVICE_ID));
            UUID charId = UUID.fromString(data.getString(Constants.KEY_CHAR_ID));

            BluetoothGatt gatt = mConnections.get(addr);
            if (gatt == null) {
                Log.e(TAG, "writeCharacteristic: Attempt to reading broken connection: " + serviceId);
                return;
            }

            BluetoothGattService service = gatt.getService(serviceId);
            if (service == null) {
                Log.e(TAG, "writeCharacteristic: Attempt to writing non-existing service: " + serviceId);
                return;
            }

            BluetoothGattCharacteristic characteristic = service.getCharacteristic(charId);

            if (characteristic == null) {
                Log.e(TAG, "writeCharacteristic: Attempt to writing non-existing characteristic: " + serviceId);
                characteristic = service.getCharacteristic(UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e"));
                //return;
            }

            byte[] values = data.getByteArray(Constants.KEY_VALUES);
            characteristic.setValue(values);

            gatt.writeCharacteristic(characteristic);
        } catch (Exception e) {
            Log.e(TAG, "writeCharacteristic: ", e);
        }

    }


    private void setNotification(Message msg) {
        Bundle data = msg.getData();
        String addr = data.getString(Constants.KEY_ADDR);

        UUID serviceId = UUID.fromString(data.getString(Constants.KEY_SERVICE_ID));
        UUID charId = UUID.fromString(data.getString(Constants.KEY_CHAR_ID));
        boolean boolValue = data.getBoolean(Constants.KEY_VALUES);

        LinkedBlockingQueue<Object> queue = mDeviceLock.get(addr);
        if (queue == null) {
            queue = new LinkedBlockingQueue<>(1);
            mDeviceLock.put(addr, queue);
        } else {
            try {
                // TODO more refined impl of blocking behavior.
                // it blocks all other (devices') requests when it blocks.
                queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
        }

        BluetoothGatt gatt = mConnections.get(addr);
        if (gatt == null) {
            throw new BLEException("Attempt to reading broken connection");
        }

        BluetoothGattService service = gatt.getService(serviceId);
        if (service == null) {
            throw new BLEException("Attempt to writing non-existing service");
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(charId);
        Log.e(TAG, " - - " + charId.toString());
        if (characteristic == null) {
            throw new BLEException("Attempt to writing non-existing characteristic");
        }

        gatt.setCharacteristicNotification(characteristic, boolValue);
        BluetoothGattDescriptor desc =
                characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        desc.setValue(boolValue ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                : new byte[]{0, 0});
        gatt.writeDescriptor(desc);
    }
}
