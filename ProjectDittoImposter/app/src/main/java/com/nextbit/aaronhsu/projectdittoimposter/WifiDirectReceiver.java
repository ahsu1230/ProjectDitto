package com.nextbit.aaronhsu.projectdittoimposter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by aaronhsu on 4/20/16.
 */
public class WifiDirectReceiver extends BroadcastReceiver {
    private static final String TAG = "Ditto.WifiP2pReceiver";

    private static final String TARGET_DEVICE_NAME = "Aaron Midnight Robin";

    private final Context mContext;
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private List<WifiP2pDevice> mPeers;
    private WifiP2pManager.PeerListListener mPeerListListener;
    private WifiP2pDevice mDevice;
    private WifiPinger mPinger;

    public WifiDirectReceiver(Context context, WifiP2pManager manager, WifiP2pManager.Channel channel) {
        mContext = context;
        mManager = manager;
        mChannel = channel;

        mDevice = null;
        mPeers = new ArrayList();
        mPeerListListener = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peers) {
                Log.d(TAG, peers.getDeviceList().size() + " peers available!");
                mPeers.clear();
                mPeers.addAll(peers.getDeviceList());
                if (mPeers.size() == 0) {
                    Log.d(TAG, "No Devices found!");
                    disconnect();
                } else {
                    Log.d(TAG, "Peer devices loaded!");
                    if (mDevice == null) {
                        Log.d(TAG, "Connect to device!");
                        connect();
                    } else {
                        Log.d(TAG, "Already connected to " + mDevice.deviceName);
                    }
                }
            }
        };
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Intent received! " + action);
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            Log.d(TAG, "Wifi p2p state changed");
            // Determine if Wifi Direct mode is enabled or not
//            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
//            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
//                activity.setIsWifiP2pEnabled(true);
//            } else {
//                activity.setIsWifiP2pEnabled(false);
//            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // The peer list has changed!  We should probably do something about
            // that.
            Log.d(TAG, "Wifi p2p peers changed");
            requestPeers();
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Connection state changed!  We should probably do something about
            // that.
            Log.d(TAG, "Wifi p2p connection changed");
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // This device details have changed!
            Log.d(TAG, "Wifi p2p device details changed");
        }
    }

    public void requestPeers() {
        Log.d(TAG, "Requesting for peers! " + mManager + " " + mChannel + " " + mPeerListListener);
        if (mManager != null) {
            mManager.requestPeers(mChannel, mPeerListListener);
        }
    }

    public void connect() {
        Log.d(TAG, "Attempting to connect to target peer device!");

        WifiP2pDevice device = null;
        for (WifiP2pDevice peer : mPeers) {
            if (TARGET_DEVICE_NAME.equals(peer.deviceName)) {
                device = peer;
                break;
            }
        }

        if (device == null) {
            Log.d(TAG, "Target device not found... " + TARGET_DEVICE_NAME);
            return;
        } else {
            Log.d(TAG, "Target device found! " + device.deviceName + " " + device.deviceAddress);
        }

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        final WifiP2pDevice targetDevice = device;
        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
                Log.d(TAG, "Successfully connected to peer ");
                mDevice = targetDevice;
                mPinger = new WifiPinger(mContext, WifiPinger.HOST_DEVICE_IP_ADDRESS, 9999);
            }

            @Override
            public void onFailure(int reason) {
                // WifiDirect Connection failed
                Log.d(TAG, "Failed to connect to peer " + reason);
                disconnect();
            }
        });
    }

    public void disconnect() {
        mDevice = null;
    }

    public WifiP2pDevice getTargetDevice() {
        return mDevice;
    }

    public WifiPinger getWifiPinger() {
        return mPinger;
    }
}
