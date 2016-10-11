package com.bhaptics.ble.model;

public class Device {
    public static final int DEVICETYPE_TACTOSY = 0;
    public static final int DEVICETYPE_OTHER = 1;

    private boolean mConnected;
    private String mMacAddress;
    private String mDeviceName;
    private int mBattery;
    private int mType;

    public Device(String macAddress, String deviceName, int type) {
        mMacAddress = macAddress;
        mDeviceName = deviceName;
        mConnected = false;
        mBattery = -1;
        mType = type;
    }

    public void setConnected(boolean conn) {
        mConnected = conn;
    }

    public boolean getConnected() {
        return mConnected;
    }

    public String getMacAddress() {
        return mMacAddress;
    }

    public String getDeviceName() {
        return mDeviceName;
    }

    public void setDeviceName(String newName) {
        mDeviceName = newName;
    }

    public void setBattery(int battery) {
        mBattery = battery;
    }

    public int getBattery() {
        return mBattery;
    }

    public int getType() {
        return mType;
    }

    @Override
    public String toString() {
        String connected = mConnected ? "connected" : "disconnected";
        return "Tactosy {" +
                "addr: " + mMacAddress + ", " +
                "name: " + mDeviceName + ", " +
                "battery: " + mBattery + "% remains, " +
                connected + "}";
    }
}
