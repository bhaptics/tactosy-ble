package com.bhaptics.ble.model;

import android.bluetooth.BluetoothGattCharacteristic;

public class BluetoothLeRequest {
    public int id;
    public String address;
    public BluetoothGattCharacteristic characteristic;
    public bleRequestOperation operation = bleRequestOperation.UNDEFINED;
    public volatile bleRequestStatus status;
    public int timeout;
    public int curTimeout;
    public boolean notifyenable;

    public enum bleRequestOperation {
        WRBLOCKING,
        WR,
        RDBLOCKING,
        RD,
        NT,
        UNDEFINED
    }

    public enum bleRequestStatus {
        NOT_QUEUED,
        QUEUED,
        PROCESSING,
        TIMEOUT,
        DONE,
        NO_SUCH_REQUEST,
        FAILED,
    }

    @Override
    public String toString() {
        return "BluetoothLeRequest{" +
                "id=" + id +
                ", address='" + address + '\'' +
                ", characteristic=" + // TODO
                ", operation=" + operation +
                ", status=" + status +
                ", timeout=" + timeout +
                ", curTimeout=" + curTimeout +
                ", notifyenable=" + notifyenable +
                '}';
    }
}
