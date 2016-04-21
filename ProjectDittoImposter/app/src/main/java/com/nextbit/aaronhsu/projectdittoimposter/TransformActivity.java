package com.nextbit.aaronhsu.projectdittoimposter;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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

//    public static final String HOST_PAGE_PATH_1 = "/sdcard/DittoLayouts/imposter_page1.xml";
    public static final String HOST_PAGE_PATH_1 = "/storage/emulated/0/DittoLayouts/host_page1.xml";
    public static final String HOST_PAGE_PATH_2 = "/storage/emulated/0/DittoLayouts/host_page2.xml";
    public static final String HOST_PAGE_PATH_3 = "/storage/emulated/0/DittoLayouts/host_page3.xml";
    public static final String IMAGE_PATH_1 = "/storage/emulated/0/DittoLayouts/ditto_look.png";
    static final int MIN_DISTANCE = 100;

    // Intents
    public static final String ACTION_UPDATE_LAYOUT = "com.nextbit.aaronhsu.projectdittoimposter.UPDATE_LAYOUT";
    public static final String EXTRA_LAYOUT_NUM = "extra_layout_num";
    public static final String EXTRA_LAYOUT_PATH = "extra_layout_path";

    // Permissions
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private final int PERM_REQUEST_CODE = 200;

    // WifiDirect
    protected final IntentFilter mIntentFilter = new IntentFilter();
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
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        // Indicates a change in the list of available peers.
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        // Indicates the state of Wi-Fi P2P connectivity has changed.
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        // Indicates this device's details have changed.
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);

        onHandleIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Activity resumed");
        if (mReceiver == null) {
            Log.d(TAG, "register WifiReceiver");
            mReceiver = new WifiDirectReceiver(this, mManager, mChannel);
            registerReceiver(mReceiver, mIntentFilter);
            Log.d(TAG, "initiate wifi discovery peers");
            discoverWifiDirectPeers();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "Activity paused");
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
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

    private void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Handling intent... " + action);

        if (ACTION_UPDATE_LAYOUT.equals(action)) {
            int nextPage = intent.getIntExtra(EXTRA_LAYOUT_NUM, 0);
            Log.d(TAG, "updating layout to page " + nextPage);
            updateLayout(nextPage);
        }
    }

    // Activity Layout
    public View getRootView() {
        //return this.getWindow().getDecorView().findFocus();
        return findViewById(android.R.id.content);
    }

    public void updateLayout(int page) {
        switch (page) {
            case 1:
                updateLayoutWithXml(HOST_PAGE_PATH_1);
                break;
            case 2:
                updateLayoutWithXml(HOST_PAGE_PATH_2);
                break;
            case 3:
                updateLayoutWithXml(HOST_PAGE_PATH_3);
                break;
            default:
                Log.w(TAG, "Unknown page number...");
        }

        getRootView().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                int x = (int)event.getX();
                int y = (int)event.getY();
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
                    new PingToClickTask(mReceiver).execute((int)upX, (int)upY);
                    lastDownX = 0;
                    lastDownY = 0;
                } else if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > MIN_DISTANCE) {
                    if (deltaX > 0) {
                        Log.d(TAG, "DETECTED SCROLL RIGHT!");
                    } else {
                        Log.d(TAG, "DETECTED SCROLL LEFT!");
                    }
                } else if (Math.abs(deltaY) > Math.abs(deltaX) && Math.abs(deltaY) > MIN_DISTANCE) {
                    if (deltaY > 0) {
                        Log.d(TAG, "DETECTED SCROLL UP!");
                    } else {
                        Log.d(TAG, "DETECTED SCROLL DOWN!");
                    }
                }
                return true;
            }
        });

        //setOnScrollStateChanged
        //setOnClickListener
        //setOnLongClickListener
    }

    public void updateLayoutWithXml(String xmlFilePath) {
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
        } catch (IOException e) {
            Log.e(TAG, "Error parsing xml file " + xmlFilePath, e);
        }
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
}