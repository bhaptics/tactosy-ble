package com.bhaptics.ble.util;

import java.util.UUID;

public class Constants {
    public final static int MESSAGE_REPLY =             0;
    public final static int MESSAGE_CONNECT =           1;
    public final static int MESSAGE_DISCONNECT =        2;
    public final static int MESSAGE_CONNECT_RESPONSE =  3;
    public final static int MESSAGE_READ =              4;
    public final static int MESSAGE_READ_SUCCESS =      5;
    public final static int MESSAGE_READ_ERROR =        6;
    public final static int MESSAGE_WRITE =             7;
    public final static int MESSAGE_WRITE_SUCCESS =     8;
    public final static int MESSAGE_WRITE_ERROR =       9;
    public final static int MESSAGE_SET_NOTIFICATION =  10;
    public final static int MESSAGE_WRITE_V2 =          11;

    public final static int MODE_DOT =  0;
    public final static int MODE_PATH = 1;

    public static final String KEY_SERVICE_ID = "SERVICE_ID";
    public static final String KEY_CHAR_ID =    "CHAR_ID";
    public static final String KEY_VALUES =     "VALUES";
    public static final String KEY_ADDR =       "ADDRESS";
    public static final String KEY_CONNECTED =  "CONNECTED";
    public static final String KEY_EXTRA_NAME = "NAME";

    public static final UUID GATT_SERVICE =     UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
    public static final UUID GATT_DEVICE_NAME = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb");

    public static final UUID MOTOR_SERVICE =    UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID MOTOR_CHAR =       UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID MOTOR_CHAR_MAPP =  UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID MOTOR_CHAR_CONFIG =UUID.fromString("6e400004-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID MOTOR_CONFIG_CUST =UUID.fromString("6e400005-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID MOTOR_DEVICE_NAME =UUID.fromString("6e400006-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID MOTOR_DEVICE_VER  =UUID.fromString("6e400007-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID BATTERY_CHAR =     UUID.fromString("6e400008-b5a3-f393-e0a9-e50e24dcca9e");

    public static final UUID BATTERY_SERVICE =  MOTOR_SERVICE;

    public static final UUID DFU_SERVICE =      UUID.fromString("00001530-1212-efde-1523-785feabcd123");
    public static final UUID DFU_CONTROL =      UUID.fromString("00001531-1212-efde-1523-785feabcd123");
    public static final UUID DFU_VERSION =      UUID.fromString("00001534-1212-efde-1523-785feabcd123");
}
