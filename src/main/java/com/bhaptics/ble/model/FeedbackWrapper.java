package com.bhaptics.ble.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/**
 * Created by bhaptics-x on 2017. 12. 20..
 */

public class FeedbackWrapper {
    @SerializedName("intervalMillis")
    public int mInterval;

    @SerializedName("size")
    public int mArraySize;

    @SerializedName("durationMillis")
    public int mDuration;

    @SerializedName("feedback")
    public Map<String, List<Feedback>> mFeedbacks;
}
