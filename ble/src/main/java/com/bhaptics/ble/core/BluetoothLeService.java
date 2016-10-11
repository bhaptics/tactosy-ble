package com.bhaptics.ble.core;

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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

import com.bhaptics.ble.util.ScanRecordParser;
import com.bhaptics.ble.util.Constants;
import com.bhaptics.ble.model.BluetoothLeRequest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

public class BluetoothLeService extends Service implements BluetoothAdapter.LeScanCallback {

    public static final String TAG = BluetoothLeService.class.getSimpleName();
    private static final String KEY_SAVED_ADDRS = "com.bhaptics.ble.core.BluetoothLeService.KEY_SAVED_ADDRS";

    private Messenger mClient;

    private Looper mLooper;
    private BluetoothHandler mBluetoothHandler;

    private Messenger mMessenger;

    private BluetoothAdapter mAdapter = null;
    private BluetoothManager mBluetoothManager = null;

    private Map<String, BluetoothGatt> mBluetoothGattMap;
    private Map<String, LinkedBlockingQueue<Object>> mCurrentRequestMap;

    private SharedPreferences mSharedPref;
    private Set<String> mPreviousAddrs;

    private final class BluetoothHandler extends Handler {
        public BluetoothHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            String addr;
            LinkedBlockingQueue<Object> queue;

            Bundle data;
            UUID serviceId;
            UUID charId;
            byte[] values;

            BluetoothGattCharacteristic characteristic;
            BluetoothLeRequest req;

            switch (msg.what) {
                case Constants.MESSAGE_REPLY:
                    mClient = msg.replyTo;

                    Message response = new Message();
                    List<BluetoothDevice> devices =
                            mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);

                    data = new Bundle();
                    data.putParcelableArrayList(Constants.KEY_CONNECTED, (ArrayList<BluetoothDevice>) devices);

                    response.setData(data);
                    response.what = Constants.MESSAGE_REPLY;

                    try {
                        mClient.send(response);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    for (String _addr: mPreviousAddrs) {
                        connect(_addr);
                    }

                    break;
                case Constants.MESSAGE_SCAN:
                    scan();
                    break;
                case Constants.MESSAGE_STOPSCAN:
                    stopScan();
                    break;
                case Constants.MESSAGE_CONNECT:
                    data = msg.getData();
                    addr = data.getString(Constants.KEY_ADDR);
                    connect(addr);
                    break;
                case Constants.MESSAGE_DISCONNECT:
                    data = msg.getData();
                    addr = data.getString(Constants.KEY_ADDR);
                    disconnect(addr);
                    break;
                case Constants.MESSAGE_READ:
                    data = msg.getData();
                    addr = data.getString(Constants.KEY_ADDR);

                    queue = mCurrentRequestMap.get(addr);
                    if (queue == null) {
                        queue = new LinkedBlockingQueue<>(1);
                        mCurrentRequestMap.put(addr, queue);
                    } else {
                        try {
                            queue.take();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                    }

                    serviceId = UUID.fromString(data.getString(Constants.KEY_SERVICE_ID));
                    charId = UUID.fromString(data.getString(Constants.KEY_CHAR_ID));

                    characteristic = characteristic(addr, serviceId, charId);
                    if (characteristic == null) {
                        Message err = new Message();
                        err.what = Constants.MESSAGE_READ_ERROR;

                        Bundle err_data = new Bundle();
                        err_data.putString(Constants.KEY_ADDR, addr);
                        err_data.putString(Constants.KEY_CHAR_ID, charId.toString());

                        err.setData(err_data);

                        try {
                            mClient.send(err);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                    req = new BluetoothLeRequest();

                    req.address = addr;
                    req.characteristic = characteristic;
                    req.status = BluetoothLeRequest.bleRequestStatus.NOT_QUEUED;
                    req.operation = BluetoothLeRequest.bleRequestOperation.RD;

                    executeRequest(req);
                    break;
                case Constants.MESSAGE_WRITE:
                    data = msg.getData();
                    addr = data.getString(Constants.KEY_ADDR);
                    queue = mCurrentRequestMap.get(addr);
                    if (queue == null) {
                        queue = new LinkedBlockingQueue<>(1);
                        mCurrentRequestMap.put(addr, queue);
                    } else {
                        try {
                            queue.take();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                    serviceId = UUID.fromString(data.getString(Constants.KEY_SERVICE_ID));
                    charId = UUID.fromString(data.getString(Constants.KEY_CHAR_ID));
                    values = data.getByteArray(Constants.KEY_VALUES);

                    characteristic = characteristic(addr, serviceId, charId);
                    Log.e(TAG, addr + " " + serviceId + " " + charId);
                    req = new BluetoothLeRequest();

                    if (characteristic == null) {
                        Message err = new Message();
                        err.what = Constants.MESSAGE_WRITE_ERROR;

                        Bundle err_data = new Bundle();
                        err_data.putString(Constants.KEY_ADDR, addr);
                        err_data.putString(Constants.KEY_CHAR_ID, charId.toString());

                        err.setData(err_data);

                        try {
                            mClient.send(err);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        break;
                    }

                    characteristic.setValue(values);
                    req.address = addr;
                    req.characteristic = characteristic;
                    req.status = BluetoothLeRequest.bleRequestStatus.NOT_QUEUED;
                    req.operation = BluetoothLeRequest.bleRequestOperation.WR;

                    executeRequest(req);
                    break;
                case Constants.MESSAGE_SET_NOTIFICATION:
                    data = msg.getData();

                    addr = data.getString(Constants.KEY_ADDR);
                    serviceId = UUID.fromString(data.getString(Constants.KEY_SERVICE_ID));
                    charId = UUID.fromString(data.getString(Constants.KEY_CHAR_ID));
                    boolean boolValue = data.getBoolean(Constants.KEY_VALUES);

                    characteristic = characteristic(addr, serviceId, charId);
                    BluetoothGatt gatt = getBluetoothGatt(addr);

                    if (characteristic == null) {
                        //TODO
                    }

                    queue = mCurrentRequestMap.get(addr);
                    if (queue == null) {
                        queue = new LinkedBlockingQueue<>(1);
                        mCurrentRequestMap.put(addr, queue);
                    } else {
                        try {
                            queue.take();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                    }

                    gatt.setCharacteristicNotification(characteristic, boolValue);
                    BluetoothGattDescriptor desc =
                            characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                    desc.setValue(boolValue ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                            : new byte[] {0, 0});
                    gatt.writeDescriptor(desc);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand: " + intent + ", " + flags + "," + startId);

        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Cannot initialize BluetoothManager.");
                stopSelf();
            }
        }

        mAdapter = mBluetoothManager.getAdapter();
        if (mAdapter == null) {
            Log.e(TAG, "Cannot obtain a BluetoothAdapter.");
            stopSelf();
        }

        mClient = null;

        mBluetoothGattMap = new HashMap<>();
        mCurrentRequestMap = new HashMap<>();

        HandlerThread thread = new HandlerThread("BluetoothHandlerThread",
                                                 Process.THREAD_PRIORITY_FOREGROUND);
        thread.start();

        mLooper = thread.getLooper();
        mBluetoothHandler = new BluetoothHandler(mLooper);
        mMessenger = new Messenger(mBluetoothHandler);

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        // Check if there are saved addrs
        // (, which is connected previously and isn't disconnected before stopService(this))
        // if exist, try to connect them.
        mPreviousAddrs = mSharedPref.getStringSet(KEY_SAVED_ADDRS, new HashSet<String>());

        Log.e(TAG, "prev addrs: " + mPreviousAddrs);

        // Clear saved addrs.
        mSharedPref.edit().putStringSet(KEY_SAVED_ADDRS, new HashSet<String>()).apply();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "onBind");
        return mMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.e(TAG, "onUnbind");
        return true;
    }

    @Override
    public void onDestroy() {
        // Save currently connected devices' addr
        // so that re-connect after service starts soon...
        Set<String> addrs = new HashSet<>();

        for (BluetoothDevice device: mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT)) {
            if (mBluetoothGattMap.containsKey(device.getAddress())) {
                addrs.add(device.getAddress());
            }
        }

        mSharedPref.edit().putStringSet(KEY_SAVED_ADDRS, addrs).apply();
        Log.e(TAG, addrs + "");

        for (BluetoothGatt bluetoothGatt : mBluetoothGattMap.values()) {
            bluetoothGatt.close();
        }

        mBluetoothGattMap.clear();

        mBluetoothHandler.removeCallbacksAndMessages(null);
        mLooper.quit();

        Log.i(TAG, "onDestroy()");
        super.onDestroy();
    }

    private void scan() {
        mAdapter.startLeScan(this);
    }

    private void stopScan() {
        mAdapter.stopLeScan(this);
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

        int appearance = ScanRecordParser.getAppearance(scanRecord);

        Message msg = new Message();
        msg.what = Constants.MESSAGE_SCAN_RESPONSE;
        msg.obj = device;
        msg.arg1 = appearance;

        try {
            mClient.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void connect(String address) {
        final BluetoothDevice device = mAdapter.getRemoteDevice(address);

        if (device == null) {
            Log.w(TAG, "Device not found. Unable to connect. " + address);
            removeBluetoothGatt(address);
            return;
        }

        int connectionState = mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT);
        if (connectionState == BluetoothProfile.STATE_DISCONNECTED) {
            if (hasConnectionBefore(address) && getBluetoothGatt(address) != null) {
                // Suddenly disconnected device. Try to reconnect.
                Log.i(TAG, "Re-connect GATT - " + address);
                getBluetoothGatt(address).connect();
            } else {
                // We want to directly connect to the device, so we are setting the
                // autoConnect parameter to false.
                Log.d(TAG, "Create a new GATT connection. " + address);
                TactosyBluetoothGattCallback callback = new TactosyBluetoothGattCallback();
                BluetoothGatt bluetoothGatt = device.connectGatt(this, false, callback);
                if (bluetoothGatt != null) {
                    addBluetoothGatt(address, bluetoothGatt);
                }
            }
        }
    }

    private void disconnect(String address) {
        if (mAdapter == null) {
            Log.w(TAG, "disconnect: BluetoothAdapter not initialized");
            return;
        }
        final BluetoothDevice device = mAdapter.getRemoteDevice(address);
        int connectionState = mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT);

        if (getBluetoothGatt(address) != null) {
            if (connectionState != BluetoothProfile.STATE_DISCONNECTED) {
                getBluetoothGatt(address).disconnect();
            } else {
                Log.w(TAG, "Attempt to disconnect in state: " + connectionState);
            }
        }
    }

    private BluetoothGatt addBluetoothGatt(String address, BluetoothGatt bluetoothGatt) {
        return mBluetoothGattMap.put(address, bluetoothGatt);
    }

    private BluetoothGatt removeBluetoothGatt(String address) {
        return mBluetoothGattMap.remove(address);
    }

    private BluetoothGatt getBluetoothGatt(String address) {
        return mBluetoothGattMap.get(address);
    }

    private boolean hasConnectionBefore(String address) {
        return mBluetoothGattMap.keySet().contains(address);
    }

    private void onConnectionResponse(BluetoothGatt gatt, int state) {
        Message msg = new Message();
        Bundle data = new Bundle();

        msg.what = Constants.MESSAGE_CONNECT_RESPONSE;
        msg.arg1 = state;

        data.putString(Constants.KEY_ADDR, gatt.getDevice().getAddress());
        msg.setData(data);

        try {
            mClient.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private class TactosyBluetoothGattCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (gatt == null) {
                Log.e(TAG, "mBluetoothGatt not created!");
                return;
            }

            BluetoothDevice device = gatt.getDevice();
            String address = device.getAddress();
            Log.i(TAG, "onConnectionStateChange (" + address + ") " + newState + " status: " + status);

            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i(TAG, "onConnectionStateChange: " + address + " connected.");
                    discoverServices(gatt.getDevice().getAddress());
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.i(TAG, "onConnectionStateChange: " + address + " disconnected.");

                    Set<String> addrs = new HashSet<>();

                    for (BluetoothDevice _device: mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT)) {
                        if (mBluetoothGattMap.containsKey(_device.getAddress())) {
                            addrs.add(_device.getAddress());
                        }
                    }

                    mSharedPref.edit().putStringSet(KEY_SAVED_ADDRS, addrs).apply();

                    onConnectionResponse(gatt, newState);
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

            Set<String> addrs = new HashSet<>();

            for (BluetoothDevice device: mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT)) {
                if (mBluetoothGattMap.containsKey(device.getAddress())) {
                    addrs.add(device.getAddress());
                }
            }

            mSharedPref.edit().putStringSet(KEY_SAVED_ADDRS, addrs).apply();

            onConnectionResponse(gatt, BluetoothProfile.STATE_CONNECTED);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            LinkedBlockingQueue<Object> queue = mCurrentRequestMap.get(gatt.getDevice().getAddress());
            assert queue != null;

            Message msg = new Message();
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
            LinkedBlockingQueue<Object> queue = mCurrentRequestMap.get(gatt.getDevice().getAddress());
            assert queue != null;

            Message msg = new Message();
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

            LinkedBlockingQueue<Object> queue = mCurrentRequestMap.get(gatt.getDevice().getAddress());
            assert queue != null;

            try {
                queue.put(new Object());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            LinkedBlockingQueue<Object> queue = mCurrentRequestMap.get(gatt.getDevice().getAddress());
            assert queue != null;

            Message msg = new Message();
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
    }

    public boolean discoverServices(String address) {
        Log.i(TAG, "discoverServices(): " + address);

        BluetoothGatt mBluetoothGatt = mBluetoothGattMap.get(address);

        if (mBluetoothGatt != null) {
            mBluetoothGatt.discoverServices();
            return true;
        }

        return false;
    }

    public boolean checkGatt(String address) {

        if (mAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized. " + address);
            return false;
        }

        if (getBluetoothGatt(address) == null) {
            Log.w(TAG, "BluetoothGatt not initialized. " + address);
            return false;
        }

        return true;
    }

    public void executeRequest(BluetoothLeRequest request) {
        if (request == null) {
            return;
        }

        BluetoothGatt bluetoothGatt = getBluetoothGatt(request.address);
        if (bluetoothGatt == null) {
            Log.e(TAG, "executeRequest: gatt is null");
            return;
        }

        Log.v(TAG, "executeRequest: " + request);

        switch (request.operation) {
            case WR:
                request.status = BluetoothLeRequest.bleRequestStatus.PROCESSING;
                if (!checkGatt(request.address)) {
                    request.status = BluetoothLeRequest.bleRequestStatus.FAILED;
                    break;
                }
                bluetoothGatt.writeCharacteristic(request.characteristic);
                break;
            case RD:
                request.status = BluetoothLeRequest.bleRequestStatus.PROCESSING;

                if (!checkGatt(request.address)) {
                    request.status = BluetoothLeRequest.bleRequestStatus.FAILED;
                    break;
                }
                bluetoothGatt.readCharacteristic(request.characteristic);
                break;
            default:
                break;
        }
    }

    BluetoothGattCharacteristic characteristic(String address, UUID serviceId, UUID charId) {
        BluetoothGatt bluetoothGatt = getBluetoothGatt(address);

        if (bluetoothGatt == null) {
            return null;
        }

        BluetoothGattService service = bluetoothGatt.getService(serviceId);
        if (service == null) {
            return null;
        }

        return service.getCharacteristic(charId);
    }
}
