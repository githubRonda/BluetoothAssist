package com.ronda.bluetoothassist;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.ronda.bluetoothassist.base.AppConst;
import com.ronda.bluetoothassist.utils.CloseUtils;
import com.ronda.bluetoothassist.utils.SPUtils;
import com.socks.library.KLog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;

/**
 * Author: Ronda(1575558177@qq.com)
 * Date: 2017/02/28
 * Version: v1.0
 * <p>
 * 蓝牙连接要点：
 * 当前显示设备的连接状态，只能由当前最近传入的连接设备来确定，与上次的连接设备无关。这种情况就是为了避免先后连接两个设备导致显示状态紊乱的情况
 */

public class BluetoothChatService {
    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "BluetoothChatSecure";
    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // Member fields
    private final BluetoothAdapter mBtAdapter;
    private final Handler mHandler;  // 由 UI Activity 通过构造器传过来的
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;


    public static final int STATE_NONE = 0; // we're doing nothing
    public static final int STATE_CONNECTING = 1; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 2; // now connected to a remote device

    private String deviceAddress;

    public BluetoothChatService(Handler handler) {
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
    }

    private synchronized void setState(int state) {
        mState = state;
        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(AppConst.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    private synchronized void setState(int state, String deviceName) {
        mState = state;
        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(AppConst.MESSAGE_STATE_CHANGE, state, -1, deviceName).sendToTarget();
    }

    public synchronized int getState() {
        return mState;
    }


    /**
     * Stop all threads
     * <p>
     * 取消并置空本类中所有的线程（共3个）
     */
    public void stop() {
        setState(STATE_NONE);
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        KLog.d("mConnectThread != null --> " + (mConnectThread != null));
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
            KLog.d("stop --> mConnectThread = null");
        }
    }


    public synchronized void connect(String adrress) {
        if (deviceAddress != null && deviceAddress.equals(adrress) && mState != STATE_NONE) { // 说明再次连接的是同一个设备，并且上次的连接请求还正在进行中或者已经正在通信中
            return;
        }
        deviceAddress = adrress; // 存储最近一次传进来的数据


        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        BluetoothDevice device = mBtAdapter.getRemoteDevice(adrress);
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }


    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (mConnectThread != null) {
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_CONNECTED, device.getName());

        //当连接成功时, 持久化存储Mac地址
        SPUtils.setMainBluetoothAddr(device.getAddress());

        mConnectedThread = new ConnectedThread(socket, device);
        mConnectedThread.start();
    }


    private void sendConnectionFailed() {
        Message msg = mHandler.obtainMessage(AppConst.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(AppConst.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        KLog.d("sendConnectionFailed");
    }


    private void sendConnectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(AppConst.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(AppConst.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }


    public void write(byte[] out) {
        ConnectedThread r;
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        r.write(out);
    }

    private class ConnectThread extends Thread {
        private final BluetoothDevice mmDevice;
        private final BluetoothSocket mmSocket;

        private ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmSocket = tmp;
        }

        @Override
        public void run() {
            mBtAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
            } catch (IOException e) {
                CloseUtils.close(mmSocket);

                // 保证当前连接中断异常的原因是自身的原因(距离太远，蓝牙关闭等)。
                // 并不是由于选择连接了另一个设备而导致的（这种情况不出意外会连接成功，若所以不加判断的话，就会产生实际连接成功，但是却可能显示连接失败的情况）
                // 根本目的就是：保证当前显示设备的连接状态，只能由当前最近传入的连接设备来确定，与上次的连接设备无关。这种情况就是为了避免先后连接两个设备导致显示状态紊乱的情况
                if (mmDevice.getAddress().equals(deviceAddress)) {
                    sendConnectionFailed();
                    setState(STATE_NONE);
                }
                return;
            }
            synchronized (BluetoothChatService.this) {
                mConnectThread = null;
            }
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private class ConnectedThread extends Thread {
        private final BluetoothDevice mmDevice;
        private final BluetoothSocket mmSocket;
        private final InputStream mmInputStream;
        private final OutputStream mmOutputStream;

        private StringBuilder sb = new StringBuilder();
        private int startIndex, endIndex;

        private ConnectedThread(BluetoothSocket socket, BluetoothDevice device) {
            mmDevice = device;
            this.mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmInputStream = tmpIn;
            mmOutputStream = tmpOut;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[128];
            int len;
            KLog.d("[ConnectedThread]before while, STATE_CONNECTED --> " + (mState == STATE_CONNECTED) + ", mState" + mState);
            while (mState == STATE_CONNECTED) {
                try {
                    len = mmInputStream.read(buffer);
                    KLog.d("read:" + new String(buffer, 0, len).toString());

                    byte[] buf_data = Arrays.copyOf(buffer, len);

                    mHandler.obtainMessage(AppConst.MESSAGE_READ, -1, -1, buf_data).sendToTarget();

//                    sb.append(new String(buffer, 0, len));

//                    String tmp = sb.toString().toLowerCase();
//                    if ((startIndex = tmp.indexOf("aa")) != -1 && (endIndex = tmp.indexOf("bb", startIndex + 1)) != -1) {
//                        //String t = tmp.substring(startIndex, endIndex+2);//aa010030911225bb
//                        String t = tmp.substring(startIndex+2, endIndex);
//                        mHandler.obtainMessage(AppConst.MESSAGE_READ, -1, -1, t).sendToTarget();
//                        sb.delete(startIndex, endIndex+2);
//                    }

                } catch (IOException e) {
                    KLog.d("ConnectedThread --> IOException : " + e.toString() + " mState : " + mState);

                    CloseUtils.close(mmSocket);

                    synchronized (BluetoothChatService.this) {
                        mConnectedThread = null;
                    }

                    // 保证当前连接中断异常的原因是自身的原因(距离太远，蓝牙关闭等)。
                    // 并不是由于选择连接了另一个设备而导致的（这种情况不出意外会连接成功，若所以不加判断的话，就会产生实际连接成功，但是却可能显示连接失败的情况）
                    // 根本目的就是：保证当前显示设备的连接状态，只能由当前最近传入的连接设备来确定，与上次的连接设备无关。这种情况就是为了避免先后连接两个设备导致显示状态紊乱的情况
                    if (mmDevice.getAddress().equals(deviceAddress)) {
                        sendConnectionLost();
                        setState(STATE_NONE);
                    }
                    break; // 跳出循环
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                mmOutputStream.write(buffer);
                KLog.d("[ConnectedThread]write, STATE_CONNECTED --> " + (mState == STATE_CONNECTED) + ", mState:" + mState);
                KLog.d("write:" + new String(buffer).toString());
                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(AppConst.MESSAGE_WRITE, -1, -1, new String(buffer)).sendToTarget();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}