package com.scientificrat.stm32.bluetoothremotecarcontroler.connection;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by huangzhengyue on 2016/10/27.
 */

public class BluetoothConnection extends Thread {
    // singleton 单例模式
    private static BluetoothConnection instance;

    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket bluetoothSocket;

    private OutputStream outputStream;
    private InputStream inputStream;

    // 为了效率只设定了一个onDataInListener
    private OnDataInListener onDataInListener = null;

    private OnConnectListener onConnectListener = null;

    private OnConnectionBreakListener onConnectionBreakListener = null;


    // FIXME: 2016/10/28 注意这里线程不安全  warning !!! it's not thread-safe
    public static BluetoothConnection createInstance(BluetoothDevice bluetoothDevice) {
        //每次调用直接创建新的连接，因为可能连接多次到不同对象
        instance = new BluetoothConnection(bluetoothDevice);
        return instance;
    }

    public static BluetoothConnection getInstance() {
        return instance;
    }

    // 私有化构造函数
    private BluetoothConnection(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
        try {
            bluetoothSocket = this.bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            Log.e("connect:","start to connect");
            // block until connect
            bluetoothSocket.connect();
            Log.e("connect:","connected");
            //连接成功
            if (onConnectListener != null) {
                onConnectListener.onConnect();
            }
            this.outputStream = bluetoothSocket.getOutputStream();
            this.inputStream = bluetoothSocket.getInputStream();

            while (true) {
                byte[] buffer = new byte[1024];
                int bytes = inputStream.read(buffer);
                //listener method callback
                if (onDataInListener != null) {
                    onDataInListener.onDataIn(buffer, bytes);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // notify listeners
            if (onConnectionBreakListener != null) {
                onConnectionBreakListener.onConnectionBreak();
            }
        }
    }

    /**
     * 发送数据接口
     *
     * @param data
     * @throws IOException
     */
    public synchronized void sendRawData(byte[] data) throws IOException {
        this.outputStream.write(data);
    }

    /**
     * 关闭接口
     */
    public void cancel() {
        try {
            this.bluetoothSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //setters
    public void setOnDataInListener(OnDataInListener onDataInListener) {
        this.onDataInListener = onDataInListener;
    }

    public void setOnConnectionBreakListener(OnConnectionBreakListener onConnectionBreakListener) {
        this.onConnectionBreakListener = onConnectionBreakListener;
    }

    public void setOnConnectListener(OnConnectListener onConnectListener) {
        this.onConnectListener = onConnectListener;
    }

    //getters
    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }
}
