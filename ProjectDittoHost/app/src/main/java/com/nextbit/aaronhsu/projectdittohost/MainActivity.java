package com.nextbit.aaronhsu.projectdittohost;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by aaronhsu on 4/18/16.
 */
public class MainActivity extends Activity {
    private static final String TAG = "Ditto.MainActivity";

    public static final String ACTION_APPLY_CLICK = "com.nextbit.aaronhsu.ProjectDittoHost.APPLY_CLICK";
    public static final String EXTRA_CLICK_X = "click_x";
    public static final String EXTRA_CLICK_Y = "click_y";
    private BroadcastReceiver mReceiver = new InnerReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() started");

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_APPLY_CLICK);
        registerReceiver(mReceiver, filter);

        startService(new Intent(this, NetworkService.class));
        setHostPage1();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent() " + intent.getAction());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
        unregisterReceiver(mReceiver);
    }

    private void setHostPage1() {
        setContentView(R.layout.host_page1);
        findViewById(R.id.host1_next_button).setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setHostPage2();
                }
            }
        );
    }

    private void setHostPage2() {
        setContentView(R.layout.host_page2);
        findViewById(R.id.host2_next_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setHostPage3();
                    }
                }
        );
    }

    private void setHostPage3() {
        setContentView(R.layout.host_page3);
    }

    private View getRootView() {
        return this.findViewById(android.R.id.content);
    }

    public class InnerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Internal BroadcastReceiver received: " + action);
            if (ACTION_APPLY_CLICK.equals(action)) {
                int x = intent.getIntExtra(EXTRA_CLICK_X, 0);
                int y = intent.getIntExtra(EXTRA_CLICK_Y, 0);
                Log.d(TAG, "Applying click! (" + x + ", " + y + ")");

                // Apply DOWN
                int eventAction = MotionEvent.ACTION_DOWN;
                long downTime = SystemClock.uptimeMillis();
                long eventTime = downTime + 100;
                int metaState = 0;
                ((ViewGroup) getRootView()).dispatchTouchEvent(MotionEvent.obtain(
                        downTime, eventTime, eventAction, x, y, metaState));

                // Apply UP
                eventAction = MotionEvent.ACTION_UP;
                downTime += 500;
                eventTime += 500;
                ((ViewGroup) getRootView()).dispatchTouchEvent(MotionEvent.obtain(
                        downTime, eventTime, eventAction, x, y, metaState));
            }
        }
    }
}
