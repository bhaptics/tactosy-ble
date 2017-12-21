package com.bhaptics.ble.client;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.bhaptics.ble.service.TactosyBLEService;
import com.bhaptics.ble.util.Constants;
import com.bhaptics.ble.model.Device;
import com.bhaptics.ble.util.ScanRecordParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detail implementation of {@link BaseClient}.</br>
 * </br>
 * There are scanning, reading and writing tactosies' characteristics.</br>
 */
public class TactosyClient extends BaseClient {
    private static final String TAG = "TactosyClient";
    private static TactosyClient sInstance = null;

    public static TactosyClient getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new TactosyClient(context);
        }

        return sInstance;
    }

    /**
     * Response handler to handle reponses from {@link TactosyBLEService}.
     * @see ClientHandler
     */
    private ClientHandler mClientHandler;

    private BluetoothAdapter mBluetoothAdapter;

    private ScanCallback mScanCallback;

    @Override
    protected ClientHandler getClientHandler() {
        if (mClientHandler == null) {
            mClientHandler = new ClientHandler();
        }
        return mClientHandler;
    }

    /**
     * Devices list selectable by address.
     * This is filled when devices are scanned. (after {@link TactosyClient#scan})
     */
    private ConcurrentHashMap<String, Device> mDevices;

    // This callback is just for backward compatibility,
    // This should be deprecated.
    @Deprecated
    private BluetoothAdapter.LeScanCallback mOlderScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            onDeviceScanned(device, rssi, scanRecord);
        }
    };

    private android.bluetooth.le.ScanCallback mNewerScanCallback;

    // NOTE To get backward compatibility, only devices above lollipop can use this callbacks,
    // and to apply TargetApi annotation this getter function should be used.
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private android.bluetooth.le.ScanCallback getNewerScanCallback() {
        if (mNewerScanCallback == null) {
            mNewerScanCallback = new android.bluetooth.le.ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    if (callbackType == ScanSettings.CALLBACK_TYPE_MATCH_LOST) {
                        return;
                    }

                    BluetoothDevice device = result.getDevice();
                    int rssi = result.getRssi();
                    ScanRecord record = result.getScanRecord();
                    byte[] scanRecord = record != null ? record.getBytes() : new byte[0];

                    onDeviceScanned(device, rssi, scanRecord);
                }
            };
        }

        return mNewerScanCallback;
    }

    protected void onDeviceScanned(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (mDevices.containsKey(device.getAddress())) {
            return;
        }

        int appearance = ScanRecordParser.getAppearance(scanRecord);

        mDevices.put(device.getAddress(), new Device(device.getAddress(), device.getName(), Device.DeviceType.ToDeviceType( device.getName())));

        mScanCallback.onScan(mDevices.values());
    }

    /**
     * Interface definition for scanner callbacks.
     * Callbacks are called only after {@link #scan()} is called.
     */
    public interface ScanCallback {
        /**
         * Called when {@link TactosyBLEService} sends a scan result.
         *
         * @param device Scanned results as {@link Device} object.
         */
        void onScan(Collection<Device> device);
    }

    /**
     * Interface definition for mConnection callbacks.
     * Callbacks are called only when {@link #connect(Device)} or
     * {@link #disconnect(Device)} is called.
     */
    public interface ConnectCallback {
        /**
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
         *
         * @param address MAC address of device to read data.
         * @param charUUID Characteristic UUID to read data.
         * @param data data read
         * @param status
         */
        void onRead(String address, UUID charUUID, byte[] data, int status);

        /**
         * Called after write request processed successfully on device.
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

    /**
     * TactosyClient's constructor.
     * This is private because only singleton is used for this class.
     */
    protected TactosyClient(Context context) {
        super();
        mDevices = new ConcurrentHashMap<>();
        mClientHandler = getClientHandler();
        BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    @Override
    public void bindService(Context context) {
        addConnectCallback(new ConnectCallback() {
            @Override
            public void onConnect(String addr) {
                if (mDevices.containsKey(addr)) {
                    return;
                }

                Log.e(TAG, "onConnect: " + addr);
                Device device = new Device(addr, "", Device.DeviceType.TactosyV1);
                device.setConnected(true);
                mDevices.put(addr, device);

                readName(addr);

                if (mScanCallback != null) {
                    mScanCallback.onScan(mDevices.values());
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

                Device target = mDevices.get(address);

                if (target != null && target.getDeviceName().isEmpty()) {
                    target.setDeviceName(new String(data));
                }
            }

            @Override
            public void onWrite(String address, UUID charUUID, int status) {}

            @Override
            public void onDataError(String address, String charId, int errCode) {}
        });

        super.bindService(context);
    }

    public void scan() {
        for (Device device : mDevices.values()) {
            if (!device.getConnected()) {
                mDevices.remove(device.getAddress());
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mBluetoothAdapter.startLeScan(mOlderScanCallback);
        } else {
            BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
            scanner.startScan(getNewerScanCallback());
        }
    }

    public void stopScan() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mBluetoothAdapter.stopLeScan(mOlderScanCallback);
        } else {
            BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
            scanner.stopScan(getNewerScanCallback());
        }
    }

    public void setScanCallback(ScanCallback callback) {
        mScanCallback = callback;
    }

    public List<Device> getConnectedDevices() {
        List<Device> result = new ArrayList<>();
        for (Device tactosyDevice : mDevices.values()) {
            if (tactosyDevice.getConnected()) {
                result.add(tactosyDevice);
            }
        }
        return result;
    }

    public Device getDevice(String address) {
        return mDevices.get(address);
    }

    /**
     * Send message to {@link TactosyBLEService} for connecting device.
     *
     * @param device {@link Device} scanned but not connected.
     * @return true if message is sent successfully.
     */
    public boolean connect(Device device) {
        if (device != null) {
            Message msg = new Message();
            Bundle data = new Bundle();

            msg.what = Constants.MESSAGE_CONNECT;
            data.putString(Constants.KEY_ADDR, device.getAddress());
            msg.setData(data);

            try {
                getService().send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            return true;
        }
        return false;
    }

    /**
     * Send message to {@link TactosyBLEService} for disconnecting device.
     *
     * @param device to disconnect
     */
    public void disconnect(Device device) {
        if (device != null) {
            Message msg = new Message();
            Bundle data = new Bundle();

            msg.what = Constants.MESSAGE_DISCONNECT;

            data.putString(Constants.KEY_ADDR, device.getAddress());
            msg.setData(data);

            try {
                getService().send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }


    public void setMotor(String values) {
        Message msg = new Message();
        Bundle data = new Bundle();

        data.putString(Constants.KEY_SERVICE_ID, Constants.MOTOR_SERVICE.toString());

        data.putString(Constants.KEY_VALUES, values);


        msg.what = Constants.MESSAGE_WRITE_V2;//Constants.MESSAGE_WRITE;
        msg.setData(data);

        try {
            getService().send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
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
        data.putString(Constants.KEY_ADDR, addr);

        msg.what = Constants.MESSAGE_WRITE;
        msg.setData(data);

        try {
            getService().send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void setMotorConfig(String addr, byte[] raw_data) {
        Message msg = new Message();
        Bundle data = new Bundle();

        data.putString(Constants.KEY_SERVICE_ID, Constants.MOTOR_SERVICE.toString());
        data.putString(Constants.KEY_CHAR_ID, Constants.MOTOR_CONFIG_CUST.toString());
        data.putString(Constants.KEY_ADDR, addr);
        data.putByteArray(Constants.KEY_VALUES, raw_data);

        msg.what = Constants.MESSAGE_WRITE;

        msg.setData(data);
        try {
            getService().send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void getMotorConfig(String addr) {
        Message msg = new Message();
        Bundle data = new Bundle();

        data.putString(Constants.KEY_SERVICE_ID, Constants.MOTOR_SERVICE.toString());
        data.putString(Constants.KEY_CHAR_ID, Constants.MOTOR_CONFIG_CUST.toString());
        data.putString(Constants.KEY_ADDR, addr);

        msg.what = Constants.MESSAGE_READ;
        msg.setData(data);

        try {
            getService().send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void readBattery(String addr) {
        Message msg = new Message();
        Bundle data = new Bundle();

        data.putString(Constants.KEY_SERVICE_ID, Constants.BATTERY_SERVICE.toString());
        data.putString(Constants.KEY_CHAR_ID, Constants.BATTERY_CHAR.toString());
        data.putString(Constants.KEY_ADDR, addr);

        msg.what = Constants.MESSAGE_READ;

        msg.setData(data);
        try {
            getService().send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void readName(String addr) {
        Message msg = new Message();
        Bundle data = new Bundle();

        data.putString(Constants.KEY_SERVICE_ID, Constants.MOTOR_SERVICE.toString());
        data.putString(Constants.KEY_CHAR_ID, Constants.MOTOR_DEVICE_NAME.toString());
        data.putString(Constants.KEY_ADDR, addr);

        msg.what = Constants.MESSAGE_READ;

        msg.setData(data);
        try {
            getService().send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void setBatteryNotification(String addr) {
        Message msg = new Message();
        Bundle data = new Bundle();

        data.putString(Constants.KEY_SERVICE_ID, Constants.BATTERY_SERVICE.toString());
        data.putString(Constants.KEY_CHAR_ID, Constants.BATTERY_CHAR.toString());
        data.putString(Constants.KEY_ADDR, addr);

        data.putBoolean(Constants.KEY_VALUES, true);

        msg.what = Constants.MESSAGE_SET_NOTIFICATION;

        msg.setData(data);
        try {
            getService().send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void readVersion(String addr) {
        Message msg = new Message();
        Bundle data = new Bundle();

        data.putString(Constants.KEY_SERVICE_ID, Constants.MOTOR_SERVICE.toString());
        data.putString(Constants.KEY_CHAR_ID, Constants.MOTOR_DEVICE_VER.toString());
        data.putString(Constants.KEY_ADDR, addr);

        msg.what = Constants.MESSAGE_READ;

        msg.setData(data);
        try {
            getService().send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void setDeviceName(String addr, String deviceName) {
        Message msg = new Message();
        Bundle data = new Bundle();

        data.putString(Constants.KEY_SERVICE_ID, Constants.MOTOR_SERVICE.toString());
        data.putString(Constants.KEY_CHAR_ID, Constants.MOTOR_DEVICE_NAME.toString());
        data.putString(Constants.KEY_ADDR, addr);
        data.putByteArray(Constants.KEY_VALUES, deviceName.getBytes());

        msg.what = Constants.MESSAGE_WRITE;

        msg.setData(data);
        try {
            getService().send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void addConnectCallback(ConnectCallback callback) {
        mClientHandler.addConnectCallback(callback);
    }

    public void addDataCallback(DataCallback callback) {
        mClientHandler.addDataCallback(callback);
    }
}
