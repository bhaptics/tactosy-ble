package com.bhaptics.ble.model;

public class Device {
    public enum DeviceType {
        Tactosy(508), Tactot(509), Tactal(510), Others(-1);
        private int appearance;
        DeviceType(int appearance) {
            this.appearance = appearance;
        }

        public static DeviceType ToDeviceType(int appearance) {
            DeviceType[] values = DeviceType.class.getEnumConstants();
            for(DeviceType type : values) {
                if (type.appearance == appearance) {
                    return type;
                }
            }

            return Others;
        }
    }

    private boolean mConnected;
    private String mAddress;
    private String mDeviceName;
    private int mBattery;
    private DeviceType mType;

    public Device(String macAddress, String deviceName, DeviceType deviceType) {
        mAddress = macAddress;
        mDeviceName = deviceName;
        mConnected = false;
        mBattery = -1;
        mType = deviceType;
    }

    public void setConnected(boolean conn) {
        mConnected = conn;
    }

    public boolean getConnected() {
        return mConnected;
    }

    public String getAddress() {
        return mAddress;
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

    public DeviceType getType() {
        return mType;
    }

    @Override
    public String toString() {
        String connected = mConnected ? "connected" : "disconnected";
        return "Device {" +
                "addr: " + mAddress + ", " +
                "name: " + mDeviceName + ", " +
                "battery: " + mBattery + "% remains, " +
                connected + "}";
    }
}
