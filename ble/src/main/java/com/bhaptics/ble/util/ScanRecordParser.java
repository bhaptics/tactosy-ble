package com.bhaptics.ble.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ScanRecordParser {
    private static final int EBLE_APPEARANCE = 0x19;

    private static Map<Integer,String> parseRecord(byte[] scanRecord) {
        Map<Integer,String> ret = new HashMap<>();
        int index = 0;
        while (index < scanRecord.length) {
            int length = scanRecord[index++];

            // Zero value indicates that we are done with the record now
            if (length == 0) {
                break;
            }

            int type = scanRecord[index];

            // If the type is zero, then we are pass the significant section of the data,
            // and we are thud done
            if (type == 0) {
                break;
            }

            byte[] data = Arrays.copyOfRange(scanRecord, index + 1, index + length);

            if (data.length > 0) {
                StringBuilder hex = new StringBuilder(data.length * 2);
                // the data appears to be there backwards
                for (int bb = data.length- 1; bb >= 0; bb--) {
                    hex.append(String.format("%02X", data[bb]));
                }
                ret.put(type, hex.toString());
            }
            index += length;
        }

        return ret;
    }

    public static int getAppearance(byte[] scanRecord) {
        HashMap<Integer, String> parsed = (HashMap<Integer, String>) parseRecord(scanRecord);
        if (!parsed.containsKey(EBLE_APPEARANCE)) {
            return -1;
        }

        String hexValue = parsed.get(EBLE_APPEARANCE);

        return Integer.valueOf(hexValue, 16);
    }
}
