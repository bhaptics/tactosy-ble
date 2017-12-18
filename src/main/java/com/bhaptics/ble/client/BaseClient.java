package com.bhaptics.ble.client;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.bhaptics.ble.util.Constants;
import com.bhaptics.ble.util.LogUtils;
import com.bhaptics.ble.util.ReadyQueue;

/**
 * Client to communicate with {@link com.bhaptics.ble.service.BaseBLEService}</br>
 * </br>
 * This client binds to {@link com.bhaptics.ble.service.BaseBLEService}
 * and exchange their {@link Messenger}</br>
 * </br>
 * {@link ClientHandler} should be specified to receive tactosies' callbacks (through service).</br>
 * </br>
 * If more callbacks (after binding to service) is needed,
 * call {@link #addOnReadyListener(OnReadyListener)}</br>
 */
public abstract class BaseClient {

    private static final String TAG = LogUtils.makeLogTag(BaseClient.class);

    private Messenger mService = null;

    private ReadyQueue mReadyQ = new ReadyQueue();

    protected abstract ClientHandler getClientHandler();

    public interface OnReadyListener {
        void onReady();
    }

    /**
     * Service connection for binding & inter-communicating.
     * @see <a href=https://developer.android.com/guide/components/bound-services.html?hl=ko>Bound service(android developer)</a>
     */
    protected ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder _service) {
            Log.e(TAG, "onServiceConnected: " + name);
            mService = new Messenger(_service);

            Message msg = Message.obtain();
            msg.what = Constants.MESSAGE_REPLY;
            msg.replyTo = new Messenger(getClientHandler());

            try {
                mService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            mReadyQ.notifyReady();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG, "Service disconnected suddenly");
            mService = null;
        }
    };

    protected Messenger getService() {
        return mService;
    }

    public boolean isReady() {
        return mReadyQ.isReady();
    }

    public void bindService(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.bhaptics.player", "com.bhaptics.ble.service.TactosyBLEService"));
        context.bindService(intent, mConnection, 0);
    }

    public void unbindService(Context context) {
        Log.e(TAG, "unbindService");
        context.unbindService(mConnection);
    }

    public void addOnReadyListener(OnReadyListener listener) {
        mReadyQ.put(listener);
    }
}
