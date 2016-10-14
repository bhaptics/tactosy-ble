package com.bhaptics.ble.core;

import android.app.Service;
import android.content.Intent;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.Messenger;

import com.bhaptics.ble.util.LogUtils;

public abstract class BaseBLEService extends Service {

    private static final String TAG = LogUtils.makeLogTag(BaseBLEService.class);

    private Messenger mMessenger;

    private Looper mLooper;
    private ServiceHandler mServiceHandler;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        HandlerThread thread = new HandlerThread("BluetoothHandlerThread",
                Process.THREAD_PRIORITY_FOREGROUND);
        thread.start();

        mLooper = thread.getLooper();
        mServiceHandler = getServiceHandler(mLooper);
        mMessenger = new Messenger(mServiceHandler);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public void onDestroy() {
        mServiceHandler.removeCallbacksAndMessages(null);
        mLooper.quit();

        super.onDestroy();
    }

    protected abstract ServiceHandler getServiceHandler(Looper looper);
}
