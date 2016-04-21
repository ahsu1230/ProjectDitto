package com.nextbit.aaronhsu.projectdittohost;

import android.app.Activity;
import android.content.Intent;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() started");

        startService(new Intent(this, NetworkService.class));

        Intent intent = getIntent();
        if (intent != null) {
            String action = intent.getAction();
            switch(action) {
                case ACTION_APPLY_CLICK:
                    int x = intent.getIntExtra(EXTRA_CLICK_X, 0);
                    int y = intent.getIntExtra(EXTRA_CLICK_Y, 0);
                    Log.d(TAG, "Applying click! (" + x + ", " + y + ")");
                    long downTime = SystemClock.uptimeMillis();
                    long eventTime = downTime + 100;
                    int eventAction = MotionEvent.ACTION_UP;
                    int metaState = 0;
                    ((ViewGroup) getRootView()).dispatchTouchEvent(MotionEvent.obtain(
                            downTime, eventTime, eventAction, x, y, metaState));
                    Log.d(TAG, "Applied click...");
                    break;
                default:
                    setHostPage1();
            }
        } else {
            setHostPage1();
        }
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


}
