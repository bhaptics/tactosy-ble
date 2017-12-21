package com.bhaptics.ble.model;

import android.util.Log;

public class Device {
    public enum DeviceType {
        TactosyV1(508, "tactosy_v1", "tactosy-v1"),
        TactosyV2(508, "tactosy_v2", "tactosy-v2"),
        Tactot(508, "tactot_", "tactot"),
        Tactal(508, "tactal_", "tactal"),
        Racket(508, "tactosy10_", "tactosy10"),
        Others(-1, "unknwon", "unkown");
        private int appearance;
        private String deviceNamePrefix;
        private String updateDeviceName;

        public String getUpdateDeviceName() {
            return updateDeviceName;
        }

        DeviceType(int appearance, String deviceNamePrefix, String updateDeviceName) {
            this.appearance = appearance;
            this.deviceNamePrefix = deviceNamePrefix;
            this.updateDeviceName = updateDeviceName;
        }

        public static DeviceType ToDeviceType(String deviceName) {
            try {
                String name = deviceName.toLowerCase();

                DeviceType[] values = DeviceType.class.getEnumConstants();
                for(DeviceType type : values) {
                    if (name.startsWith(type.deviceNamePrefix)) {
                        return type;
                    }
                }
            } catch (Exception e) {

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
