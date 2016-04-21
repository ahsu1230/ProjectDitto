package com.nextbit.aaronhsu.projectdittohost;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.net.Socket;

/**
 * Created by aaronhsu on 4/20/16.
 */
public class NetworkService extends Service {
    private static final String TAG = "Ditto.NetworkService";
    private Thread mSocketThread;
    private WifiPingListener mListener;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        if (mListener == null) {
            Log.d(TAG, "Start listener!");
            mListener = new WifiPingListener(getApplicationContext());
        }
        if (mSocketThread == null) {
            mSocketThread = new Thread(new SomeRunner());
            mSocketThread.start();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (mSocketThread != null) {
            mSocketThread.interrupt();
            mSocketThread = null;
        }

        if (mListener != null) {
            Log.d(TAG, "Done listening, close!");
            mListener.close();
            mListener = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public class SomeRunner implements Runnable {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                Log.d(TAG, "Listening...");
                mListener.listenToRespond();
            }
        }
    }
}


