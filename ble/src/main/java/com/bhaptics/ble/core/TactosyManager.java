package com.bhaptics.ble.core;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.bhaptics.ble.util.ReadyQueue;
import com.bhaptics.ble.util.Constants;
import com.bhaptics.ble.model.Device;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.bhaptics.ble.util.Constants.KEY_ADDR;

public class TactosyManager implements ResponseHandler.ScanCallbackInner {

    // Static variables. tag string, static instance.
    private static final String TAG = TactosyManager.class.getSimpleName();

    private static TactosyManager instance = null;

    // Constructors and Factory methods.
    /**
     * Singleton getter method.
     * NOTE This method should be called after instantiation of singleton instance.
     * @see {@link #instantiate(Context)}
     *
     * @return TactosyManager instance.
     */
    public static TactosyManager getInstance() {
        assert instance != null;
        return instance;
    }

    /**
     * Instantiates TactosyManager's singleton instance and return it.
     * This should be called in firstly launched activity, which can have singleton as member.
     *
     * @param context Context of firstly launched activity.
     * @return TactosyManager instance.
     */
    public static TactosyManager instantiate(Context context) {
        if (instance == null) {
            instance = new TactosyManager(context);
        }

        return instance;
    }

    /**
     * TactosyManager's constructor.
     * This is private because only singleton is used for this class.
     *
     * @param _context
     */
    public TactosyManager(Context _context) {
        mContext = _context;
        mBluetoothDeviceItemMap = new HashMap<>();
        mResponseHandler = new ResponseHandler(this);

        addConnectCallback(new ConnectCallback() {
            @Override
            public void onConnect(String addr) {
                if (mBluetoothDeviceItemMap.containsKey(addr)) {
                    return;
                }

                Device device = new Device(addr, "", Device.DEVICETYPE_TACTOSY);
                device.setConnected(true);
                mBluetoothDeviceItemMap.put(addr, device);

                readName(addr);

                for (ScanCallback callback: mScanCallbacks) {
                    callback.onTactosyScan(mBluetoothDeviceItemMap.values());
                }
            }

            @Override
            public void onDisconnect(String addr) {}

            @Override
            public void onConnectionError(String addr) {}
        });

        addDataCallback(new DataCallback() {
            @Override
            public void onRead(String address, UUID charUUID, byte[] data, int status) {
                if (data == null || !charUUID.equals(Constants.MOTOR_DEVICE_NAME)) {
                    return;
                }

                Device target = mBluetoothDeviceItemMap.get(address);

                if (target != null && target.getDeviceName().isEmpty()) {
                    target.setDeviceName(new String(data));
                }
            }

            @Override
            public void onWrite(String address, UUID charUUID, int status) {}

            @Override
            public void onDataError(String address, String charId, int errCode) {}
        });
    }

    /**
     * Interface definition for scanner callbacks.
     * Callbacks are called only after {@link #scan()} is called.
     */
    public interface ScanCallback {
        /**
         * Called when {@link BluetoothLeService} sends a scan result.
         *
         * @param device Scanned results as {@link Device} object.
         */
        void onTactosyScan(Collection<Device> device);
    }

    /**
     * Interface definition for mConnection callbacks.
     * Callbacks are called only when {@link #connect(Device)} or
     * {@link #disconnect(Device)} is called.
     */
    public interface ConnectCallback {
        /*
         * Called when BLE mConnection is created successfully.
         *
         * @param addr MAC address of connected device.
         */
        void onConnect(String addr);

        /**
         * Called when BLE mConnection is disconnected.
         * sudden disconnections are also called it.
         *
         * @param addr MAC address of disconnected device.
         */
        void onDisconnect(String addr);

        /**
         * Called when mConnection cannot be constructed or
         * disconnection is arised from error.
         *
         * @param addr MAC address of device.
         */
        void onConnectionError(String addr);
    }

    /**
     * Interface definition for read/write callbacks on connected devices.
     */
    public interface DataCallback {
        /**
         * Called after read request processed successfully on device.
         * @see {@link #sendMessageSafe(Message)}.
         *
         * @param address MAC address of device to read data.
         * @param charUUID Characteristic UUID to read data.
         * @param data data read
         * @param status
         */
        void onRead(String address, UUID charUUID, byte[] data, int status);

        /**
         * Called after write request processed successfully on device.
         * @see {@link #sendMessageSafe(Message)}
         *
         * @param address MAC address of device to write data.
         * @param charUUID Characteristic UUID to write data.
         * @param status
         */
        void onWrite(String address, UUID charUUID, int status);

        /**
         * Called when error occurred reading/writing data.
         *
         * @param errCode one of {@link Constants#MESSAGE_READ_ERROR} or {@link Constants#MESSAGE_WRITE_ERROR}
         */
        void onDataError(String address, String charId, int errCode);
    }

    public interface OnReadyListener {
        void onReady();
    }

    // Member variables.
    private Context mContext;
    private Messenger mService = null;

    /**
     * Response handler to handle reponses from {@link BluetoothLeService}.
     * @see {@link ResponseHandler}
     */
    private ResponseHandler mResponseHandler;

    private ReadyQueue mReadyQ = new ReadyQueue();

    private ArrayList<ScanCallback> mScanCallbacks = new ArrayList<>();

    /**
     * Mapping for `address`:`{@link Device}`
     */
    private Map<String, Device> mBluetoothDeviceItemMap;

    /**
     * Service connection for binding & inter-communicating.
     * @see <a href=https://developer.android.com/guide/components/bound-services.html?hl=ko>this</a>
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder _service) {
            Log.e(TAG, "onServiceConnected: " + name);
            mService = new Messenger(_service);

            Message msg = new Message();
            msg.what = Constants.MESSAGE_REPLY;
            msg.replyTo = new Messenger(mResponseHandler);

            sendMessageSafe(msg);
            mReadyQ.notifyReady();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG, "Service disconnected suddenly");
            mService = null;
        }
    };

    public boolean isReady() {
        return mReadyQ.isReady();
    }

    public void bindService() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.bhaptics.tactosy", "com.bhaptics.ble.core.BluetoothLeService"));
        if (!mContext.bindService(intent,
                mConnection,
                            0 /*Context.BIND_IMPORTANT*/)) {
            Log.e(TAG, "Cannot connect to mService");
        } else {
            Log.e(TAG, "Connected");
        }
    }

    public void unbindService() {
        Log.e(TAG, "unbindService");
        mContext.unbindService(mConnection);
    }

    /**
     * Send message to {@link BluetoothLeService} for start scanning.
     */
    public void scan() {
        Log.i(TAG, "scan start");

        Message msg = new Message();
        msg.what = Constants.MESSAGE_SCAN;
        sendMessageSafe(msg);
    }

    /**
     * Send message to {@link BluetoothLeService} for stop scanning.
     */
    public void stopScan() {
        Message msg = new Message();
        msg.what = Constants.MESSAGE_STOPSCAN;
        sendMessageSafe(msg);
    }

    @Override
    public void onTactosyScan(BluetoothDevice device, int type, int flag) {
        if (!mBluetoothDeviceItemMap.containsKey(device.getAddress()) && type == Device.DEVICETYPE_TACTOSY) {
            Device tDevice = new Device(device.getAddress(), device.getName(), type);
            mBluetoothDeviceItemMap.put(tDevice.getMacAddress(), tDevice);

            if ((flag & ResponseHandler.EXTRA_FLAG_CONNECTED) != 0) {
                tDevice.setConnected(true);
            }
        }

        for (ScanCallback callback: mScanCallbacks) {
            callback.onTactosyScan(mBluetoothDeviceItemMap.values());
        }
    }

    private void sendMessageSafe(Message msg) {
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "sendMessageSafe: ", e);
        }
    }

    public List<Device> getConnectedDevices() {
        List<Device> result = new ArrayList<>();
        for (Device tactosyDevice : mBluetoothDeviceItemMap.values()) {
            if (tactosyDevice.getConnected()) {
                result.add(tactosyDevice);
            }
        }
        return result;
    }

    public Device getBleDeviceItem(String address) {
        return mBluetoothDeviceItemMap.get(address);
    }

    /**
     * Send message to {@link BluetoothLeService} for connecting device.
     *
     * @param tDevice {@link Device} scanned but not connected.
     * @return true if message is sent successfully.
     */
    public boolean connect(Device tDevice) {
        if (tDevice != null) {
            Message msg = new Message();
            Bundle data = new Bundle();

            msg.what = Constants.MESSAGE_CONNECT;
            data.putString(KEY_ADDR, tDevice.getMacAddress());
            msg.setData(data);

            sendMessageSafe(msg);

            return true;
        }
        return false;
    }

    /**
     * Send message to {@link BluetoothLeService} for disconnecting device.
     *
     * @param tDevice
     */
    public void disconnect(Device tDevice) {
        if (tDevice != null) {
            Message msg = new Message();
            Bundle data = new Bundle();

            msg.what = Constants.MESSAGE_DISCONNECT;

            data.putString(KEY_ADDR, tDevice.getMacAddress());
            msg.setData(data);

            sendMessageSafe(msg);
        }
    }

    public void setMotor(String addr, byte[] values) {
        setMotor(addr, values, Constants.MOTOR_CHAR);
    }

    public void setMotor(String addr, byte[] values, UUID charUUID) {
        Message msg = new Message();
        Bundle data = new Bundle();

        data.putString(Constants.KEY_SERVICE_ID, Constants.MOTOR_SERVICE.toString());
        data.putString(Constants.KEY_CHAR_ID, charUUID.toString());
        data.putByteArray(Constants.KEY_VALUES, values);
        data.putString(KEY_ADDR, addr);

        msg.what = Constants.MESSAGE_WRITE;

        msg.setData(data);
        sendMessageSafe(msg);
    }

    public void setMotorConfig(String addr, byte[] raw_data) {
        Message msg = new Message();
        Bundle data = new Bundle();

        data.putString(Constants.KEY_SERVICE_ID, Constants.MOTOR_SERVICE.toString());
        data.putString(Constants.KEY_CHAR_ID, Constants.MOTOR_CONFIG_CUST.toString());
        data.putString(KEY_ADDR, addr);
        data.putByteArray(Constants.KEY_VALUES, raw_data);

        msg.what = Constants.MESSAGE_WRITE;

        msg.setData(data);
        sendMessageSafe(msg);
    }

    public void getMotorConfig(String addr) {
        Message msg = new Message();
        Bundle data = new Bundle();

        data.putString(Constants.KEY_SERVICE_ID, Constants.MOTOR_SERVICE.toString());
        data.putString(Constants.KEY_CHAR_ID, Constants.MOTOR_CONFIG_CUST.toString());
        data.putString(KEY_ADDR, addr);

        msg.what = Constants.MESSAGE_READ;
        msg.setData(data);

        sendMessageSafe(msg);
    }

    public void readBattery(String addr) {
        Message msg = new Message();
        Bundle data = new Bundle();

        data.putString(Constants.KEY_SERVICE_ID, Constants.BATTERY_SERVICE.toString());
        data.putString(Constants.KEY_CHAR_ID, Constants.BATTERY_CHAR.toString());
        data.putString(KEY_ADDR, addr);

        msg.what = Constants.MESSAGE_READ;

        msg.setData(data);
        sendMessageSafe(msg);
    }

    public void readName(String addr) {
        Message msg = new Message();
        Bundle data = new Bundle();

        data.putString(Constants.KEY_SERVICE_ID, Constants.MOTOR_SERVICE.toString());
        data.putString(Constants.KEY_CHAR_ID, Constants.MOTOR_DEVICE_NAME.toString());
        data.putString(KEY_ADDR, addr);

        msg.what = Constants.MESSAGE_READ;

        msg.setData(data);
        sendMessageSafe(msg);
    }

    public void setBatteryNotification(String addr) {
        Message msg = new Message();
        Bundle data = new Bundle();

        data.putString(Constants.KEY_SERVICE_ID, Constants.BATTERY_SERVICE.toString());
        data.putString(Constants.KEY_CHAR_ID, Constants.BATTERY_CHAR.toString());
        data.putString(KEY_ADDR, addr);

        data.putBoolean(Constants.KEY_VALUES, true);

        msg.what = Constants.MESSAGE_SET_NOTIFICATION;

        msg.setData(data);
        sendMessageSafe(msg);
    }

    public void readVersion(String addr) {
        Message msg = new Message();
        Bundle data = new Bundle();

        data.putString(Constants.KEY_SERVICE_ID, Constants.MOTOR_SERVICE.toString());
        data.putString(Constants.KEY_CHAR_ID, Constants.MOTOR_DEVICE_VER.toString());
        data.putString(KEY_ADDR, addr);

        msg.what = Constants.MESSAGE_READ;

        msg.setData(data);
        sendMessageSafe(msg);
    }

    public void setDeviceName(String addr, String deviceName) {
        Message msg = new Message();
        Bundle data = new Bundle();

        data.putString(Constants.KEY_SERVICE_ID, Constants.MOTOR_SERVICE.toString());
        data.putString(Constants.KEY_CHAR_ID, Constants.MOTOR_DEVICE_NAME.toString());
        data.putString(KEY_ADDR, addr);
        data.putByteArray(Constants.KEY_VALUES, deviceName.getBytes());

        msg.what = Constants.MESSAGE_WRITE;

        msg.setData(data);
        sendMessageSafe(msg);
    }

    public void addScanCallback(ScanCallback callback) {
        mScanCallbacks.add(callback);
    }

    public void addConnectCallback(ConnectCallback callback) {
        mResponseHandler.addConnectCallback(callback);
    }

    public void addDataCallback(DataCallback callback) {
        mResponseHandler.addDataCallback(callback);
    }

    public void addOnReadyListener(OnReadyListener listener) {
        mReadyQ.put(listener);
    }
}
