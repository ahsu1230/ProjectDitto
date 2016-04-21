package com.nextbit.aaronhsu.projectdittohost;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.DataInputStream;
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

    private final Context mContext;
    private ServerSocket mServerSocket;

    public WifiPingListener(Context context) {
        mContext = context;
        try {
            mServerSocket = new ServerSocket(HOST_PORT);
        } catch (IOException e) {
            Log.e(TAG, "Error initializing server socket " + HOST_PORT, e);
        }
    }

    private Socket getClientSocket() {
        try {
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
            Log.d(TAG, "Message: " + message);

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
            byte page = 1;
            os.write(page);
            os.flush();
            Log.d(TAG, "wrote page number " + page);
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
            Log.d(TAG, "Read: (" + x + ", " + y + ")");
        } catch (IOException e) {
            Log.e(TAG, "Problem reading InputStream ", e);
        }

        // Apply Click action!
        Log.d(TAG, "Sending intent for click!");
        Intent intent = new Intent(mContext, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setAction(MainActivity.ACTION_APPLY_CLICK);
        intent.putExtra(MainActivity.EXTRA_CLICK_X, x);
        intent.putExtra(MainActivity.EXTRA_CLICK_Y, y);
        mContext.startActivity(intent);

        // Did Next button get clicked?
        boolean nextButtonClicked = true;

        // Generate response code
        Log.d(TAG, "Generating response " + nextButtonClicked);
        try {
            OutputStream os = clientSocket.getOutputStream();
            os.write(MSG_REQUEST_CLICK);
            byte action = nextButtonClicked ? (byte)2 : (byte)0;
            os.write(action);
            os.flush();
            Log.d(TAG, "wrote action " + action);
        } catch (IOException e) {
            Log.e(TAG, "Problem output streaming response", e);
        }
    }
}
