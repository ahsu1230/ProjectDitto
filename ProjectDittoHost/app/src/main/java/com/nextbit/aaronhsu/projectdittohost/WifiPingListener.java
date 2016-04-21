package com.nextbit.aaronhsu.projectdittohost;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by aaronhsu on 4/20/16.
 */
public class WifiPingListener {
    private static final String TAG = "Ditto.PingListener";
    private static final int HOST_PORT = 9999;

    public static final String HOST_DEVICE_ADDRESS = "cc:f3:a5:88:22:2a";
    public static final String HOST_DEVICE_IP_ADDRESS = "10.1.12.104";

    private static final byte MSG_REQUEST_MIMIC = 10;
    private static final byte MSG_REQUEST_CLICK = 11;
    private static final byte MSG_REQUEST_HOLD = 12;
    private static final byte MSG_REQUEST_SCROLL = 13;
    private static final byte MSG_REQUEST_ROTATE = 14;

    private int mCurrentPage;
    private final Context mContext;
    private ServerSocket mServerSocket;

    public WifiPingListener(Context context) {
        mContext = context;
        try {
            mServerSocket = new ServerSocket(HOST_PORT);
        } catch (IOException e) {
            Log.e(TAG, "Error initializing server socket " + HOST_PORT, e);
        }
        mCurrentPage = 1;
    }

    private Socket getClientSocket() {
        try {
            Log.d(TAG, "Blocking until client accepted...");
            Socket client = mServerSocket.accept();
            Log.d(TAG, "Client connected! "
                    + client.getInetAddress().getHostName() + " "
                    + client.getInetAddress().getHostAddress() + " "
                    + client.getPort());
            return client;
        } catch (IOException e) {
            Log.e(TAG, "Error accepting client ", e);
        }
        return null;
    }

    public void listenToRespond() {
        Socket client = getClientSocket();
        if (client == null) {
            return;
        }

        try {
            DataInputStream dis = new DataInputStream(client.getInputStream());
            int message = dis.readByte();
            Log.d(TAG, "Message: " + message + "*********");

            switch (message) {
                case MSG_REQUEST_MIMIC:
                    onMimic(client);
                    break;
                case MSG_REQUEST_CLICK:
                    onClick(client);
                    break;
                default:
                    Log.d(TAG, "Message type unknown! " + message);
                    break;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error with Streams", e);
        }
    }

    public void close() {
        Log.d(TAG, "Server socket closed");
        try {
            mServerSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing server socket ", e);
        }
    }

    private void onMimic(Socket clientSocket) {
        Log.d(TAG, "onMimic");
        // Generate response code
        try {
            OutputStream os = clientSocket.getOutputStream();
            os.write(MSG_REQUEST_MIMIC);
            os.write(mCurrentPage);
            os.flush();
            Log.d(TAG, "wrote page number " + mCurrentPage);
        } catch (IOException e) {
            Log.e(TAG, "Problem output streaming response", e);
        }
    }

    private void onClick(Socket clientSocket) {
        Log.d(TAG, "onClick");

        // Read what's needed
        int x = 0, y = 0;
        try {
            DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
            x = dis.readInt();
            y = dis.readInt();
            Log.d(TAG, "Read click coordinates: (" + x + ", " + y + ")");
        } catch (IOException e) {
            Log.e(TAG, "Problem reading InputStream ", e);
        }

        // Apply Click action!
        Log.d(TAG, "Sending intent for click! on page " + mCurrentPage);
        Intent intent = new Intent(MainActivity.ACTION_APPLY_CLICK);
        intent.putExtra(MainActivity.EXTRA_CLICK_X, x);
        intent.putExtra(MainActivity.EXTRA_CLICK_Y, y);
        mContext.sendBroadcast(intent);

        // Did Next button get clicked? If so, nextPage is a number. Otherwise 0.
        int nextPage = 0;
        if (mCurrentPage == 1) {
            if (x >= 350 && x <= 650 && y >= 700 && y <= 1000) {
                nextPage = 2;
                sendResourceOverStream(clientSocket);
            }
        } else if (mCurrentPage == 2) {
            if (x >= 200 && x <= 1000 && y >= 1350 && y <= 1450) {
                nextPage = 3;
                sendResourceOverStream(clientSocket);
            }
        } else if (mCurrentPage == 3) {

        }
        if (nextPage > 0) {
            mCurrentPage = nextPage;
        }

        // Generate response code
        Log.d(TAG, "Generating response " + nextPage);
        try {
            OutputStream os = clientSocket.getOutputStream();
            os.write(MSG_REQUEST_CLICK);
            byte action = (byte) nextPage;
            os.write(action);
            os.flush();
            Log.d(TAG, "wrote action " + action);
        } catch (IOException e) {
            Log.e(TAG, "Problem output streaming response", e);
        }
    }

    public void sendResourceOverStream(Socket clientSocket) {
        String path = null;
        String packageName = mContext.getApplicationContext().getPackageName();
        if (mCurrentPage == 1) {
            Uri uri = Uri.parse("android.resource://" + packageName + "/" + R.layout.host_page1);
            path = uri.getPath();
        } else if (mCurrentPage == 2) {
            Uri uri = Uri.parse("android.resource://" + packageName + "/" + R.layout.host_page2);
            path = uri.getPath();
        } else if (mCurrentPage == 3) {
            Uri uri = Uri.parse("android.resource://" + packageName + "/" + R.layout.host_page3);
            path = uri.getPath();
        }
        if (path != null) {
            File f = new File(path);
            Log.d(TAG, "****** Preparing to send Resource with path: " + f.getAbsolutePath() + " " + f.exists());
        }
    }
}
