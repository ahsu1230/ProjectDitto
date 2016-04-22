package com.nextbit.aaronhsu.projectdittoimposter;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.IOException;

/**
 * Created by Aaron Hsu
 * 4/18/2016
 */
public class TransformActivity extends Activity {
    private static final String TAG = "Ditto.TransformActivity";

    public static final String IMAGE_PATH_1_1 = "/storage/emulated/0/DittoLayouts/pics/ditto_look.png";
    public static final String IMAGE_PATH_3_1 = "/storage/emulated/0/DittoLayouts/pics/ditto_gen1.png";
    public static final String IMAGE_PATH_3_2 = "/storage/emulated/0/DittoLayouts/pics/ditto_gen2.png";
    public static final String IMAGE_PATH_3_3 = "/storage/emulated/0/DittoLayouts/pics/ditto_gen3.png";
    public static final String IMAGE_PATH_3_4 = "/storage/emulated/0/DittoLayouts/pics/ditto_gen5.png";
    public static final String IMAGE_PATH_3_5 = "/storage/emulated/0/DittoLayouts/pics/ditto_gen5shiny.png";

    static final int MIN_DISTANCE = 100;

    // Intents
    public static final String ACTION_REQUEST_UPDATE = "com.nextbit.aaronhsu.projectdittoimposter.REQUEST_UPDATE";
    public static final String ACTION_UPDATE_LAYOUT = "com.nextbit.aaronhsu.projectdittoimposter.UPDATE_LAYOUT";
    public static final String EXTRA_LAYOUT_PATH = "extra_layout_path";
    private InnerReceiver mInnerReceiver = new InnerReceiver();

    // Permissions
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private final int PERM_REQUEST_CODE = 200;

    // WifiDirect
    protected final IntentFilter mWifiIntentFilter = new IntentFilter();
    private WifiDirectReceiver mReceiver;
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;

    // Touch
    private float lastDownX, lastDownY, lastUpX, lastUpY;

    //
    // Overriden methods
    //
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() started ");

        setContentView(R.layout.activity_transform);

        Button button = (Button) findViewById(R.id.rando_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new PingToMimicTask(mReceiver).execute();
            }
        });

        if (shouldAskPermission()) {
            requestPermissions(PERMISSIONS_STORAGE, PERM_REQUEST_CODE);
        }

        // Prepare setup for P2P Wifi-Direct Manager
        // Indicates a change in the Wi-Fi Peer-to-Peer status.
        mWifiIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        // Indicates a change in the list of available peers.
        mWifiIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        // Indicates the state of Wi-Fi P2P connectivity has changed.
        mWifiIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        // Indicates this device's details have changed.
        mWifiIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Activity resumed");
        if (mReceiver == null) {
            Log.d(TAG, "register WifiReceiver");
            mReceiver = new WifiDirectReceiver(this, mManager, mChannel);
            registerReceiver(mReceiver, mWifiIntentFilter);
            Log.d(TAG, "initiate wifi discovery peers");
            discoverWifiDirectPeers();
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_UPDATE_LAYOUT);
        filter.addAction(ACTION_REQUEST_UPDATE);
        registerReceiver(mInnerReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "Activity paused");
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        unregisterReceiver(mInnerReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Activity destroyed!");
        if (mManager != null) {
            mManager = null;
        }
        if (mChannel != null) {
            mChannel = null;
        }
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
                    Toast.makeText(getApplicationContext(), "Permission Granted!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "onConfigurationChanged ");
        final int orientation = getResources().getConfiguration().orientation;
        new PingToRotateTask(mReceiver).execute(orientation);
    }

    //
    // Activity Layout
    //

    public View getRootView() {
        //return this.getWindow().getDecorView().findFocus();
        return findViewById(android.R.id.content);
    }

    private void updateLayoutWithXml(String xmlFilePath) {
        Log.d(TAG, "updateLayout... " + xmlFilePath);

        File layoutFile = new File(xmlFilePath);
        if (layoutFile == null) {
            Log.d(TAG, "file null...");
        } else if (layoutFile.exists()) {
            Log.d(TAG, "file exists!");
        } else {
            Log.d(TAG, "file does not exist...");
        }

        // Use Custom XmlParser & Inflater
        try {
            XmlPullParser parser = new MyXmlParser(xmlFilePath);

            ViewInflater viewInflater = new ViewInflater(this);
            View newRoot = viewInflater.createRootView(parser);

            Log.d(TAG, "Finished view group");
            viewInflater.printViewGroup((ViewGroup) newRoot);

            Log.d(TAG, "Setting new content view");
            setContentView(newRoot);

            Log.d(TAG, "Setup click listener on new root " + newRoot);
            setupTouchListenerOnRoot(newRoot);
//            Log.d(TAG, "Setup click listener on getRootView() " + getRootView());
//            setupTouchListenerOnRoot(getRootView());

        } catch (IOException e) {
            Log.e(TAG, "Error parsing xml file " + xmlFilePath, e);
        }
    }

    private void setupTouchListenerOnRoot(View view) {
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                int x = (int) event.getX();
                int y = (int) event.getY();
                Log.d(TAG, "Touch: " + action + " (" + x + ", " + y + ")");

                float downX = 0, downY = 0, upX = 0, upY = 0;

                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getX();
                        downY = event.getY();
                        if (lastDownX == 0 && lastDownY == 0) {
                            lastDownX = downX;
                            lastDownY = downY;
                            return true;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        upX = event.getX();
                        upY = event.getY();
                        break;
                    default:
                        break;
                }

                float deltaX = upX - lastDownX;
                float deltaY = upY - lastDownY;
                Log.d(TAG, "Click deltas: (" + deltaX + ", " + deltaY + ")");

                if (Math.abs(deltaX) < MIN_DISTANCE && Math.abs(deltaY) < MIN_DISTANCE) {
                    Log.d(TAG, "DETECTED CLICK!");
                    new PingToClickTask(mReceiver).execute((int) upX, (int) upY);
                    lastDownX = 0;
                    lastDownY = 0;
                } else if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > MIN_DISTANCE) {
                    if (deltaX > 0) {
                        Log.d(TAG, "DETECTED SWIPE RIGHT!");
                        new PingToSwipeTask(mReceiver).execute(
                                (int) lastDownX, (int) lastDownY,
                                (int) upX, (int) upY);
                        lastDownX = 0;
                        lastDownY = 0;
                    } else {
                        Log.d(TAG, "DETECTED SWIPE LEFT!");
                        new PingToSwipeTask(mReceiver).execute(
                                (int) lastDownX, (int) lastDownY,
                                (int) upX, (int) upY);
                        lastDownX = 0;
                        lastDownY = 0;
                    }
                } else if (Math.abs(deltaY) > Math.abs(deltaX) && Math.abs(deltaY) > MIN_DISTANCE) {
                    if (deltaY > 0) {
                        Log.d(TAG, "DETECTED SWIPE UP!");
                        new PingToSwipeTask(mReceiver).execute(
                                (int) lastDownX, (int) lastDownY,
                                (int) upX, (int) upY);
                        lastDownX = 0;
                        lastDownY = 0;
                    } else {
                        Log.d(TAG, "DETECTED SWIPE DOWN!");
                        new PingToSwipeTask(mReceiver).execute(
                                (int) lastDownX, (int) lastDownY,
                                (int) upX, (int) upY);
                        lastDownX = 0;
                        lastDownY = 0;
                    }
                }
                return true;
            }
        });
    }

    //
    // WifiDirect methods
    //
    private void discoverWifiDirectPeers() {
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Discovering peers successful!");
                // code for when discovery initiation is successful
                requestPeers();
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "Discovering peers failed " + reason);
                // code for when discovery initiation fails
                // alert the user something went wrong?
            }
        });
    }

    private void requestPeers() {
        Log.d(TAG, "Request for peers");
        if (mReceiver != null) {
            mReceiver.requestPeers();
        }
    }

    private class PingToMimicTask extends AsyncTask<Void, Void, Void> {
        final WifiDirectReceiver mReceiver;

        public PingToMimicTask(WifiDirectReceiver receiver) {
            mReceiver = receiver;
        }

        @Override
        protected Void doInBackground(Void[] params) {
            Log.d(TAG, "PingToMimicTask... doInBackground()");
            mReceiver.getWifiPinger().pingToRequestMimic();
            return null;
        }

        @Override
        protected void onPostExecute(Void o) {
            Log.d(TAG, "PingToMimicTask... postExecute()");
        }
    }

    private class PingToClickTask extends AsyncTask<Integer, Void, Void> {
        final WifiDirectReceiver mReceiver;

        public PingToClickTask(WifiDirectReceiver receiver) {
            mReceiver = receiver;
        }

        @Override
        protected Void doInBackground(Integer[] params) {
            Log.d(TAG, "PingToClickTask... doInBackground()");
            int x = params[0].intValue();
            int y = params[1].intValue();
            mReceiver.getWifiPinger().pingToRequestClick(x, y);
            return null;
        }

        @Override
        protected void onPostExecute(Void o) {
            Log.d(TAG, "PingToClickTask... postExecute()");
        }
    }

    private class PingToSwipeTask extends AsyncTask<Integer, Void, Void> {
        final WifiDirectReceiver mReceiver;

        public PingToSwipeTask(WifiDirectReceiver receiver) {
            mReceiver = receiver;
        }

        @Override
        protected Void doInBackground(Integer[] params) {
            Log.d(TAG, "PingToSwipeTask... doInBackground()");
            int x1 = params[0].intValue();
            int y1 = params[1].intValue();
            int x2 = params[2].intValue();
            int y2 = params[3].intValue();
            mReceiver.getWifiPinger().pingToRequestSwipe(x1, y1, x2, y2);
            return null;
        }

        @Override
        protected void onPostExecute(Void o) {
            Log.d(TAG, "PingToSwipeTask... postExecute()");
        }
    }

    private class PingToRotateTask extends AsyncTask<Integer, Void, Void> {
        final WifiDirectReceiver mReceiver;

        public PingToRotateTask(WifiDirectReceiver receiver) {
            mReceiver = receiver;
        }

        @Override
        protected Void doInBackground(Integer[] params) {
            Log.d(TAG, "PingToRotateTask... doInBackground()");
            int orientation = params[0].intValue();
            mReceiver.getWifiPinger().pingToRequestRotate(orientation);
            return null;
        }

        @Override
        protected void onPostExecute(Void o) {
            Log.d(TAG, "PingToRotateTask... postExecute()");
        }
    }

    public class InnerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_UPDATE_LAYOUT.equals(action)) {
                String nextPagePath = intent.getStringExtra(EXTRA_LAYOUT_PATH);
                Log.d(TAG, "updating layout to page " + nextPagePath);
                updateLayoutWithXml(nextPagePath);
            } else if (ACTION_REQUEST_UPDATE.equals(action)) {
                new PingToMimicTask(mReceiver).execute();
            }
        }
    }
}
