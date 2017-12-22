package com.bhaptics.ble.client;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;

import com.bhaptics.ble.service.TactosyBLEService;

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
//        BluetoothManager bluetoothManager =
//                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        //mBluetoothAdapter = bluetoothManager.getAdapter();

        return sInstance;
    }

    /**
     * Response handler to handle reponses from {@link TactosyBLEService}.
     * @see ClientHandler
     */
    private ClientHandler mClientHandler;

    private BluetoothAdapter mBluetoothAdapter;

    //private ScanCallback mScanCallback;

    @Override
    protected ClientHandler getClientHandler() {
        if (mClientHandler == null) {
            mClientHandler = new ClientHandler();
        }
        return mClientHandler;
    }

    /**
     * Devices list selectable by address.
     * This is filled when devices are scanned. (after {@link TactosyClient#})
     */
//    private ConcurrentHashMap<String, Device> mDevices;


    // This callback is just for backward compatibility,
    // This should be deprecated.
//    @Deprecated
//    private BluetoothAdapter.LeScanCallback mOlderScanCallback = new BluetoothAdapter.LeScanCallback() {
//        @Override
//        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
//            onDeviceScanned(device, rssi, scanRecord);
//        }
//    };

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

                    //onDeviceScanned(device, rssi, scanRecord);
                }
            };
        }

        return mNewerScanCallback;
    }

//    protected void onDeviceScanned(BluetoothDevice device, int rssi, byte[] scanRecord) {
//        if (mDevices.containsKey(device.getAddress())) {
//            return;
//        }
//
//        int appearance = ScanRecordParser.getAppearance(scanRecord);
//
//        mDevices.put(device.getAddress(), new Device(device.getAddress(), device.getName(), Device.DeviceType.ToDeviceType( device.getName())));
//
//        mScanCallback.onScan(mDevices.values());
//    }

//    /**
//     * Interface definition for scanner callbacks.
//     * Callbacks are called only after {@link #scan()} is called.
//     */
//    public interface ScanCallback {
//        /**
//         * Called when {@link TactosyBLEService} sends a scan result.
//         *
//         * @param device Scanned results as {@link Device} object.
//         */
//        void onScan(Collection<Device> device);
//    }

    /**
     * TactosyClient's constructor.
     * This is private because only singleton is used for this class.
     */
    protected TactosyClient(Context context) {
        super();
//        mDevices = new ConcurrentHashMap<>();
        mClientHandler = getClientHandler();
//        BluetoothManager bluetoothManager =
//                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
//        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    @Override
    public void bindService(Context context) {
        super.bindService(context);
    }

//    public List<Device> getConnectedDevices() {
//        List<Device> result = new ArrayList<>();
//        for (Device tactosyDevice : mDevices.values()) {
//            if (tactosyDevice.getConnected()) {
//                result.add(tactosyDevice);
//            }
//        }
//        return result;
//    }

//    /**
//     * Send message to {@link TactosyBLEService} for connecting device.
//     *
//     * @param device {@link Device} scanned but not connected.
//     * @return true if message is sent successfully.
//     */
//    public boolean connect(Device device) {
//        if (device != null) {
//            Message msg = new Message();
//            Bundle data = new Bundle();
//
//            msg.what = Constants.MESSAGE_CONNECT;
//            data.putString(Constants.KEY_ADDR, device.getAddress());
//            msg.setData(data);
//
//            try {
//                getService().send(msg);
//            } catch (RemoteException e) {
//                e.printStackTrace();
//            }
//
//            return true;
//        }
//        return false;
//    }

//    /**
//     * Send message to {@link TactosyBLEService} for disconnecting device.
//     *
//     * @param device to disconnect
//     */
//    public void disconnect(Device device) {
//        if (device != null) {
//            Message msg = new Message();
//            Bundle data = new Bundle();
//
//            msg.what = Constants.MESSAGE_DISCONNECT;
//
//            data.putString(Constants.KEY_ADDR, device.getAddress());
//            msg.setData(data);
//
//            try {
//                getService().send(msg);
//            } catch (RemoteException e) {
//                e.printStackTrace();
//            }
//        }
//    }

}
