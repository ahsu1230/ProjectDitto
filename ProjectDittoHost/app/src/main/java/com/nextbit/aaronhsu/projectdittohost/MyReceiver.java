package com.nextbit.aaronhsu.projectdittohost;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by aaronhsu on 4/18/16.
 */
public class MyReceiver extends BroadcastReceiver {
    private static final String TAG = "Ditto.MyReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive started");
        // do anything with intent?

        Intent serviceIntent = new Intent(context, InspectService.class);
        serviceIntent.setAction(intent.getAction());
        context.startService(serviceIntent);
    }
}
