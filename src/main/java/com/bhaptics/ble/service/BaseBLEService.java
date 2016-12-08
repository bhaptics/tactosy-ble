package com.bhaptics.ble.service;

import android.app.Service;
import android.content.Intent;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.Messenger;

/**
 * Base service for ble connections.</br>
 * Applications for tactosy should has a class based on {@link BaseBLEService}.</br>
 * This service is used for binding / IPC using {@link Messenger}.</br>
 * <b>Connections and messaging should be handled in {@link ServiceHandler},
 * which is provided in {@link #getServiceHandler(Looper)}</b></br>
 *</br>
 *</br>
 * @see <a href="https://developer.android.com/guide/components/bound-services.html">Bound service(android developers)</a>
 * @see TactosyBLEService
 * @see GearVRSpoiledService
 */
public abstract class BaseBLEService extends Service {

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

    /**
     * @return Looper of a thread that this service class have.
     */
    protected Looper getThreadLooper() {
        return mLooper;
    }

    /**
     * Abstract method to return service handler.
     * You should implement it in subclass.
     *
     * @see ServiceHandler
     *
     * @param looper Handler handles in this looper (thread).
     * @return ServiceHandler you implemented.
     */
    protected abstract ServiceHandler getServiceHandler(Looper looper);
}
