package com.nextbit.aaronhsu.projectdittohost;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

/**
 * Created by aaronhsu on 4/18/16.
 */
public class MainActivity extends Activity {
    private static final String TAG = "Ditto.MainActivity";

    // Intents
    public static final String ACTION_CHANGE_PAGE = "com.nextbit.aaronhsu.ProjectDittoHost.CHANGE_PAGE";
    public static final String EXTRA_PAGE = "page";

    public static final String ACTION_APPLY_CLICK = "com.nextbit.aaronhsu.ProjectDittoHost.APPLY_CLICK";
    public static final String EXTRA_CLICK_X = "click_x";
    public static final String EXTRA_CLICK_Y = "click_y";

    public static final String ACTION_APPLY_ROTATE = "com.nextbit.aaronhsu.ProjectDittoHost.APPLY_ROTATE";
    public static final String EXTRA_ORIENTATION = "orientation";

    // Permissions
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private final int PERM_REQUEST_CODE = 200;

    private BroadcastReceiver mReceiver = new InnerReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() started");

        if (shouldAskPermission()) {
            requestPermissions(PERMISSIONS_STORAGE, PERM_REQUEST_CODE);
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_APPLY_CLICK);
        filter.addAction(ACTION_APPLY_ROTATE);
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
                        Intent intent = new Intent(getApplicationContext(), NetworkService.class);
                        intent.setAction(ACTION_CHANGE_PAGE);
                        intent.putExtra(EXTRA_PAGE, 2);
                        startService(intent);
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
                        Intent intent = new Intent(getApplicationContext(), NetworkService.class);
                        intent.setAction(ACTION_CHANGE_PAGE);
                        intent.putExtra(EXTRA_PAGE, 3);
                        startService(intent);
                    }
                }
        );
    }

    private void setHostPage3() {
        setContentView(R.layout.host_page3);
        findViewById(R.id.host3_prev_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setHostPage2();
                        Intent intent = new Intent(getApplicationContext(), NetworkService.class);
                        intent.setAction(ACTION_CHANGE_PAGE);
                        intent.putExtra(EXTRA_PAGE, 2);
                        startService(intent);
                    }
                }
        );
    }

    private View getRootView() {
        return this.findViewById(android.R.id.content);
    }

    // Permissions
    private boolean shouldAskPermission() {
        return(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
            case PERM_REQUEST_CODE:
                boolean permAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                if (permAccepted) {
                    Toast.makeText(getApplicationContext(), "Permission Granted!",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                }
                break;
        }
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
            } else if (ACTION_APPLY_ROTATE.equals(action)) {
                int orientation = intent.getIntExtra(EXTRA_ORIENTATION, 0);
                Log.d(TAG, "Applying rotate! " + orientation);
                if (ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE == orientation) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                } else if (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT == orientation) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                } else if (ActivityInfo.SCREEN_ORIENTATION_USER == orientation) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
            }
        }
    }
}
