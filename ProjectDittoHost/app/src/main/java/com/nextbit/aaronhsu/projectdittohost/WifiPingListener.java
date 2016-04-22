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

    public static final String HOST_PAGE_PATH_1 = "/storage/emulated/0/DittoHostLayouts/host_page1.xml";
    public static final String HOST_PAGE_PATH_2 = "/storage/emulated/0/DittoHostLayouts/host_page2.xml";
    public static final String HOST_PAGE_PATH_3 = "/storage/emulated/0/DittoHostLayouts/host_page3.xml";
    public static final String IMAGE_PATH_1_1 = "/storage/emulated/0/DittoHostLayouts/ditto_look.png";
    public static final String IMAGE_PATH_3_1 = "/storage/emulated/0/DittoHostLayouts/ditto_gen1.png";
    public static final String IMAGE_PATH_3_2 = "/storage/emulated/0/DittoHostLayouts/ditto_gen2.png";
    public static final String IMAGE_PATH_3_3 = "/storage/emulated/0/DittoHostLayouts/ditto_gen3.png";
    public static final String IMAGE_PATH_3_4 = "/storage/emulated/0/DittoHostLayouts/ditto_gen5.png";
    public static final String IMAGE_PATH_3_5 = "/storage/emulated/0/DittoHostLayouts/ditto_gen5shiny.png";

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
                case MSG_REQUEST_ROTATE:
                    onRotate(client);
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
            filePath = HOST_PAGE_PATH_1;
        } else if (mCurrentPage == 2) {
            filePath = HOST_PAGE_PATH_2;
        } else if (mCurrentPage == 3) {
            filePath = HOST_PAGE_PATH_3;
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
            copyStream(fis, clientSocket.getOutputStream());
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

        int pageBeforeSleep = mCurrentPage;

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {

        }

        boolean changed = pageBeforeSleep != mCurrentPage;
        Log.d(TAG, "Something changed? " + changed);

        // send response if something clicked or not, client should call REQUEST_MIMIC
        try {
            OutputStream os = clientSocket.getOutputStream();
            os.write(changed ? 1 : 0);
        } catch (IOException e) {
            Log.e(TAG, "Problem writing to socket OutputStream", e);
            return;
        }
    }

    private void onRotate(Socket clientSocket) {
        Log.d(TAG, "onRotate()");

        // Read orientation
        int orientation;
        try {
            DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
            orientation = dis.readInt();
            Log.d(TAG, "Read rotate orientation: " + orientation);
        } catch (IOException e) {
            Log.e(TAG, "Problem reading socket InputStream ", e);
            return;
        }

        // Apply rotate action!
        Log.d(TAG, "Sending intent for rotate! on page " + mCurrentPage);
        Intent intent = new Intent(MainActivity.ACTION_APPLY_ROTATE);
        intent.putExtra(MainActivity.EXTRA_ORIENTATION, orientation);
        mContext.sendBroadcast(intent);
    }

    public void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        long totalSizeRead = 0;
        int len = 0;
        while ((len = in.read(buf)) != -1) {
            totalSizeRead += len;
            Log.d(TAG, "copying stream... " + totalSizeRead);
            out.write(buf, 0, len);
        }
    }
}
