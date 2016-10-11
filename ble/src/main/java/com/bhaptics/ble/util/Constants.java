package com.bhaptics.ble.util;

import java.util.UUID;

public class Constants {
    public final static int MESSAGE_REPLY =             0;
    public final static int MESSAGE_SCAN =              1;
    public final static int MESSAGE_SCAN_RESPONSE =     2;
    public final static int MESSAGE_STOPSCAN =          3;
    public final static int MESSAGE_CONNECT =           4;
    public final static int MESSAGE_DISCONNECT =        5;
    public final static int MESSAGE_CONNECT_RESPONSE =  6;
    public final static int MESSAGE_READ =              7;
    public final static int MESSAGE_READ_SUCCESS =      8;
    public final static int MESSAGE_READ_ERROR =        9;
    public final static int MESSAGE_WRITE =             10;
    public final static int MESSAGE_WRITE_SUCCESS =     11;
    public final static int MESSAGE_WRITE_ERROR =       12;
    public final static int MESSAGE_CONNECT_CHANGED =   13;
    public final static int MESSAGE_SET_NOTIFICATION =   14;

    public final static int MODE_DOT =  0;
    public final static int MODE_PATH = 1;

    public static final String KEY_SERVICE_ID = "SERVICE_ID";
    public static final String KEY_CHAR_ID =    "CHAR_ID";
    public static final String KEY_VALUES =     "VALUES";
    public static final String KEY_ADDR =       "ADDRESS";
    public static final String KEY_CONNECTED =  "CONNECTED";
    public static final String KEY_EXTRA_NAME =  "NAME";

    public static final UUID GATT_SERVICE =     UUID.fromString("00001800-0000-1000-8000-00805F9B34FB");
    public static final UUID GATT_DEVICE_NAME = UUID.fromString("00002A00-0000-1000-8000-00805F9B34FB");

    public static final UUID MOTOR_SERVICE =    UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID MOTOR_CHAR =       UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID MOTOR_CHAR_MAPP =  UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    public static final UUID MOTOR_CHAR_CONFIG =UUID.fromString("6E400004-B5A3-F393-E0A9-E50E24DCCA9E");
    public static final UUID MOTOR_CONFIG_CUST =UUID.fromString("6E400005-B5A3-F393-E0A9-E50E24DCCA9E");
    public static final UUID MOTOR_DEVICE_NAME =UUID.fromString("6E400006-B5A3-F393-E0A9-E50E24DCCA9E");
    public static final UUID MOTOR_DEVICE_VER  =UUID.fromString("6E400007-B5A3-F393-E0A9-E50E24DCCA9E");
    public static final UUID BATTERY_CHAR =     UUID.fromString("6E400008-B5A3-F393-E0A9-E50E24DCCA9E");

    public static final UUID BATTERY_SERVICE =  MOTOR_SERVICE;

    public static final UUID DFU_SERVICE =      UUID.fromString("00001530-1212-efde-1523-785feabcd123");
    public static final UUID DFU_CONTROL =      UUID.fromString("00001531-1212-efde-1523-785feabcd123");
    public static final UUID DFU_VERSION =      UUID.fromString("00001534-1212-efde-1523-785feabcd123");

    public static final int DATA_SIZE = 20;
}
