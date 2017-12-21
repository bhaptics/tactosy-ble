package com.bhaptics.ble.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by bhaptics-x on 2017. 12. 20..
 */

public class Feedback {
    @SerializedName("position")
    public String mPosition;

    @SerializedName("mode")
    public String mType;

    @SerializedName("values")
    public byte[] mValues;
}
