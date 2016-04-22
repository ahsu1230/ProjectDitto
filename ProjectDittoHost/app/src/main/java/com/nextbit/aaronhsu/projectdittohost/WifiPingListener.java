package com.nextbit.aaronhsu.projectdittohost;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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

    public int getCurrentPage() {
        return mCurrentPage;
    }

    public void setCurrentPage(int page) {
        Log.d(TAG, "Setting current page to: " + page);
        mCurrentPage = page;
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

    public void close() {
        Log.d(TAG, "Server socket closed");
        try {
            mServerSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing server socket ", e);
        }
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

            Log.d(TAG, "Responding to message: " + message);
            client.getOutputStream().write(message);
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

    private void onMimic(Socket clientSocket) {
        Log.d(TAG, "onMimic()");

        String filePath = null;
        if (mCurrentPage == 1) {
            filePath = FileStreamer.HOST_PAGE_PATH_1;
        } else if (mCurrentPage == 2) {
            filePath = FileStreamer.HOST_PAGE_PATH_2;
        } else if (mCurrentPage == 3) {
            filePath = FileStreamer.HOST_PAGE_PATH_3;
        }

        if (filePath == null) {
            Log.e(TAG, "Unknown page right now... " + mCurrentPage);
            return;
        }

        Log.d(TAG, "Commence streaming file " + filePath);
        File f = new File(filePath);
        try (FileInputStream fis = new FileInputStream(f)) {
            // write file length (this tell client when we know when to stop reading)
            DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
            dos.writeLong(f.length());
            // write file stream
            FileStreamer.copyStream(fis, clientSocket.getOutputStream());
        } catch (IOException e) {
            Log.e(TAG, "Error with InputStream ", e);
        }
        Log.d(TAG, "Done streaming file!");
    }

    private void onClick(Socket clientSocket) {
        Log.d(TAG, "onClick()");

        // Read coordinates
        int x = 0, y = 0;
        try {
            DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
            x = dis.readInt();
            y = dis.readInt();
            Log.d(TAG, "Read click coordinates: (" + x + ", " + y + ")");
        } catch (IOException e) {
            Log.e(TAG, "Problem reading socket InputStream ", e);
            return;
        }

        // Apply Click action!
        Log.d(TAG, "Sending intent for click! on page " + mCurrentPage);
        Intent intent = new Intent(MainActivity.ACTION_APPLY_CLICK);
        intent.putExtra(MainActivity.EXTRA_CLICK_X, x);
        intent.putExtra(MainActivity.EXTRA_CLICK_Y, y);
        mContext.sendBroadcast(intent);

        // Expecting a change during click? Set mCurrentPage
        boolean changed = false;
        if (mCurrentPage == 1) {
            if (x >= 350 && x <= 650 && y >= 700 && y <= 1000) {
                changed = true;
                mCurrentPage = 2;
            }
        } else if (mCurrentPage == 2) {
            if (x >= 200 && x <= 1000 && y >= 1350 && y <= 1450) {
                changed = true;
                mCurrentPage = 3;
            }
        } else if (mCurrentPage == 3) {
            if (x >= 150 && x <= 400 && y >= 150 && y <= 400) {
                changed = true;
                mCurrentPage = 2;
            }
        }
        if (changed) {
            Log.d(TAG, "Something changed! " + mCurrentPage);
        }

        // send response if something clicked or not, client should call REQUEST_MIMIC
        try {
            OutputStream os = clientSocket.getOutputStream();
            os.write(changed ? 1 : 0);
        } catch (IOException e) {
            Log.e(TAG, "Problem writing to socket OutputStream", e);
            return;
        }
    }
}
