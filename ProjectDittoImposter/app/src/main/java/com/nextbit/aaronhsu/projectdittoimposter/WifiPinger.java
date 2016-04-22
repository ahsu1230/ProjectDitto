package com.nextbit.aaronhsu.projectdittoimposter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.UUID;

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

    public static final String LAYOUT_DIR_PATH = "/storage/emulated/0/DittoLayouts/Layouts";

    public static final byte MSG_REQUEST_MIMIC = 10;
    public static final byte MSG_REQUEST_CLICK = 11;
    public static final byte MSG_REQUEST_HOLD = 12;
    public static final byte MSG_REQUEST_SCROLL = 13;
    public static final byte MSG_REQUEST_ROTATE = 14;

    private final Context mContext;
    private String mHost;
    private int mPort;
    private Socket mSocket;
    private File mLayoutDir;

    public WifiPinger(Context context, String host, int port) {
        mContext = context;
        mSocket = new Socket();
        mHost = host;
        mPort = port;
        mLayoutDir = new File(LAYOUT_DIR_PATH);
        clearDirectory();
    }

    private void clearDirectory() {
        for (File f : mLayoutDir.listFiles()) {
            if (f != null && f.exists()) {
                f.delete();
            }
        }
        Log.d(TAG, "Directory cleared... " + mLayoutDir.listFiles().length);
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

    private boolean connectSocket() {
        if (mSocket != null && mSocket.isConnected()) {
            return true;
        }
        for (int retry = 1; retry <= 3; retry++) {
            try {
                bindSocket();
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
        return mSocket.isConnected();
    }

    public void pingToRequestMimic() {
        Log.d(TAG, "*** ping to mimic host! ********");
        pingMessage(MSG_REQUEST_MIMIC, null);
    }

    public void pingToRequestClick(int x, int y) {
        Log.d(TAG, "*** ping to interact! click (" + x + ", " + y + ") *********");
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

    public void pingToRequestScroll(int downX, int downY, int upX, int upY) {
        Log.d(TAG, "*** ping to interact! scroll (" + downX + ", " + downY + ") to (" + upX + ", " + upY +") *********");
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream outStream = new DataOutputStream(baos)) {
            outStream.writeInt(downX);
            outStream.writeInt(downY);
            outStream.writeInt(upX);
            outStream.writeInt(upY);
            outStream.flush();
            byte[] data = baos.toByteArray();
            pingMessage(MSG_REQUEST_SCROLL, data);
        } catch (IOException e) {
            Log.e(TAG, "Error creating output streams", e);
        }
    }

    public void pingToRequestRotate(int orientation) {
        Log.d(TAG, "*** ping to interact! rotate " + orientation + " *********");
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream outStream = new DataOutputStream(baos)) {
            outStream.writeInt(orientation);
            outStream.flush();
            byte[] data = baos.toByteArray();
            pingMessage(MSG_REQUEST_ROTATE, data);
        } catch (IOException e) {
            Log.e(TAG, "Error creating output streams", e);
        }
    }

    private void pingMessage(int message, byte[] data) {
        if (!connectSocket()) {
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
            awaitResponse();
        } catch (IOException e) {
            Log.e(TAG, "Problem with sending stream ", e);
        } finally {
            try {
                unbindSocket();
            } catch (IOException e) {
                Log.e(TAG, "Problem unbinding socket", e);
            }
        }
    }

    private void awaitResponse() {
        try {
            Log.d(TAG, "Awaiting response...");
            InputStream is = mSocket.getInputStream();
            int respMsg = is.read();
            Log.d(TAG, "Retrieved response for message: " + respMsg);
            if (MSG_REQUEST_MIMIC == respMsg) {
                DataInputStream dis = new DataInputStream(is);
                long expectedFileSize = dis.readLong();
                Log.d(TAG, "Expected file size: " + expectedFileSize);
                File f = extractFileStream(expectedFileSize, mSocket.getInputStream());
                if (f != null) {
                    handleResponseToMimic(f);
                }
            } else if (MSG_REQUEST_CLICK == respMsg) {
                int changed = is.read();
                handleResponseToClick(changed == 1 ? true : false);
            } else if (MSG_REQUEST_SCROLL == respMsg) {
                handleResponseToScroll();
            } else if (MSG_REQUEST_ROTATE == respMsg) {
                handleResponseToRotate();
            } else {
                Log.d(TAG, "Unknown response " + respMsg);
            }
        } catch (IOException e) {
            Log.e(TAG, "Problem with response stream ", e);
        }
    }

    private void handleResponseToMimic(File f) {
        Log.d(TAG, "Handling response from MIMIC " + f.getAbsolutePath());
        Intent intent = new Intent(TransformActivity.ACTION_UPDATE_LAYOUT);
        intent.putExtra(TransformActivity.EXTRA_LAYOUT_PATH, f.getAbsolutePath());
        mContext.sendBroadcast(intent);
    }

    private void handleResponseToClick(boolean changed) {
        Log.d(TAG, "Handling response from CLICK " + changed);
        if (changed) {
            Log.d(TAG, "Something changed! Request to MIMIC!");
            mContext.sendBroadcast(new Intent(TransformActivity.ACTION_REQUEST_UPDATE));
        } else {
            Log.d(TAG, "Nothing changed... do nothing");
        }
    }

    private void handleResponseToScroll() {
        Log.d(TAG, "Handling response from SCROLL");
    }

    private void handleResponseToRotate() {
        Log.d(TAG, "Handling response from ROTATE");
    }

    public File extractFileStream(long expectedFileSize, InputStream is) {
        // Create new file
        File f = null;
        try {
            f = File.createTempFile(UUID.randomUUID().toString(), ".xml", new File(LAYOUT_DIR_PATH));
        } catch (IOException e) {
            Log.e(TAG, "Error creating new temp file");
        }
        if (f == null || !f.exists()) {
            Log.e(TAG, "problem extracting file!");
            return null;
        }
        Log.d(TAG, "Created file on disk " + f.getAbsolutePath());

        // Load socket's input stream into newly created file
        Log.d(TAG, "Loading stream into new file...");
        try (FileOutputStream os = new FileOutputStream(f)) {
            byte[] buf = new byte[8192];
            long totalSizeRead = 0;
            while (totalSizeRead < expectedFileSize) {
                int len = is.read(buf);
                if (len != -1) {
                    os.write(buf, 0, len);
                }
                totalSizeRead += len;
                Log.d(TAG, "copying stream..."
                        + " totalSize: " + totalSizeRead
                        + " expected: " + expectedFileSize);
            }
            return f;
        } catch (IOException e) {
            Log.e(TAG, "Error extracting file " + f.getAbsolutePath());
        }

        return null;
    }
}
