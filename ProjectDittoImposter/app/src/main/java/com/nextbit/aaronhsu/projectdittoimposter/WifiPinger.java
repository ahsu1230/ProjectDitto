package com.nextbit.aaronhsu.projectdittoimposter;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by aaronhsu on 4/20/16.
 */
public class WifiPinger {
    private static final String TAG = "Ditto.WifiPinger";
    private static final int TIMEOUT_MILLIS = 2000;

    public static final String CLIENT_DEVICE_ADDRESS = "fe80::cef3:a5ff:fe88:24c8";
    public static final String CLIENT_DEVICE_IP_ADDRESS = "10.1.12.27";
    public static final String HOST_DEVICE_ADDRESS = "cc:f3:a5:88:22:2a";
    public static final String HOST_DEVICE_IP_ADDRESS = "10.1.12.104";

    public static final byte MSG_REQUEST_MIMIC = 10;
    public static final byte MSG_REQUEST_CLICK = 11;
    public static final byte MSG_REQUEST_HOLD = 12;
    public static final byte MSG_REQUEST_SCROLL = 13;
    public static final byte MSG_REQUEST_ROTATE = 14;

    private final Context mContext;
    private String mHost;
    private int mPort;
    private Socket mSocket;

    public WifiPinger(Context context, String host, int port) {
        mContext = context;
        mSocket = new Socket();
        mHost = host;
        mPort = port;
    }

    private void bindSocket() throws IOException {
        if (mSocket == null) {
            mSocket = new Socket();
        }

        mSocket.bind(null);
        mSocket.connect(new InetSocketAddress(InetAddress.getByName(mHost), mPort), TIMEOUT_MILLIS);
    }

    private void unbindSocket() throws IOException {
        if (mSocket != null) {
            if (mSocket.isConnected()) {
                mSocket.close();
            }
            mSocket = null;
        }
    }

    public void pingToRequestMimic() {
        Log.d(TAG, "*** ping to mimic host!");
        pingMessage(MSG_REQUEST_MIMIC, null);
    }

    public void handleResponseToMimic(int nextPage) {
        Log.d(TAG, "*** handling response to mimic! " + nextPage);
        Intent intent = new Intent(TransformActivity.ACTION_UPDATE_LAYOUT);
        intent.putExtra(TransformActivity.EXTRA_LAYOUT_NUM, nextPage);
        mContext.startActivity(intent);
    }

    public void handleResponseToMimic(String nextPagePath) {
        Log.d(TAG, "*** handling response to mimic! " + nextPagePath);
        Intent intent = new Intent(TransformActivity.ACTION_UPDATE_LAYOUT);
        intent.putExtra(TransformActivity.EXTRA_LAYOUT_PATH, nextPagePath);
        mContext.startActivity(intent);
    }

    public void pingToRequestClick(int x, int y) {
        Log.d(TAG, "*** ping to interact! (" + x + ", " + y + ")");
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream outStream = new DataOutputStream(baos)) {
            outStream.writeInt(x);
            outStream.writeInt(y);
            outStream.flush();
            byte[] data = baos.toByteArray();
            pingMessage(MSG_REQUEST_CLICK, data);
        } catch (IOException e) {
            Log.e(TAG, "Error creating output streams", e);
        }
    }

    public void handleResponseToClick(int action) {
        if (action == 0) {
            Log.d(TAG, "No action... do nothing");
            return;
        } else if (action <= 3) {
            Log.d(TAG, "Go to page " + action);
            Intent intent = new Intent(TransformActivity.ACTION_UPDATE_LAYOUT);
            intent.putExtra(TransformActivity.EXTRA_LAYOUT_NUM, action);
            mContext.startActivity(intent);
        }
    }

    /**
     * Used for small messages
     * @param message
     * @param data
     * @return byte[] for response
     */
    private void pingMessage(byte message, byte[] data) {
        boolean connected = false;
        for (int retry = 1; retry <= 3; retry++) {
            try {
                bindSocket();
                connected = true;
                break;
            } catch (BindException e) {
                Log.d(TAG, "Socket already bounded... ");
                try {
                    mSocket.connect(new InetSocketAddress(InetAddress.getByName(mHost), mPort),
                            TIMEOUT_MILLIS);
                } catch (IOException ioe) {
                    Log.e(TAG, "Problem connecting socket... retry again... " + retry, e);
                }
            } catch (IOException e) {
                Log.e(TAG, "Problem binding socket retry again... " + retry, e);
            }
        }

        if (!connected) {
            return;
        }

        try {
            // Send ping message with socket's outputstream
            Log.d(TAG, "Writing request... " + message);
            OutputStream os = mSocket.getOutputStream();
            os.write(message);
            if (data != null) {
                os.write(data, 0, data.length);
            }
            os.flush();

            // Await for response from server
            Log.d(TAG, "Awaiting response...");
            InputStream is = mSocket.getInputStream();
            int respMsg = is.read();
            if (MSG_REQUEST_MIMIC == respMsg) {
                int nextPage = is.read();
                handleResponseToMimic(nextPage);
            } else if (MSG_REQUEST_CLICK == respMsg) {
                int action = is.read();
                handleResponseToClick(action);
            } else {
                Log.d(TAG, "Unknown response " + respMsg);
            }
        } catch (IOException e) {
            Log.e(TAG, "Problem with InputStreams", e);
        } finally {
            try {
                unbindSocket();
            } catch (IOException e) {
                Log.e(TAG, "Problem unbinding socket", e);
            }
        }
    }

//    private byte[] getBytesFromInputStream(InputStream is) throws IOException {
//        Log.d(TAG, "get bytes from inputstream...");
//        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
//            byte[] buffer = new byte[1024];
//            int len = 0;
//            Log.d(TAG, "Start reading..");
//            while ((len = is.read(buffer)) != -1) {
//                Log.d(TAG, "read len " + len);
//                baos.write(buffer, 0, len);
//            }
//            baos.flush();
//            byte[] res = baos.toByteArray();
//            Log.d(TAG, "finished reading and writing... " + res.length);
//            return res;
//        }
//    }

    public void extractStream(Uri uri) {
        ContentResolver cr = mContext.getContentResolver();
        int len = 0;
        byte[] buffer = new byte[1024];
        try {
            bindSocket();

            // Output stream from server
            OutputStream outputStream = mSocket.getOutputStream();
            // Input stream to read content
            InputStream inputStream = cr.openInputStream(uri);

            // Read content
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }

            // Close streams
            outputStream.close();
            inputStream.close();

        } catch (IOException e) {
            Log.e(TAG, "Problem binding socket", e);
        } finally {
            try {
                unbindSocket();
            } catch (IOException e) {
                Log.e(TAG, "Problem unbinding socket", e);
            }
        }
    }
}
