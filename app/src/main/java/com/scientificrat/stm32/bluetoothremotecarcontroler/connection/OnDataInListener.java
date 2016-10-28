package com.scientificrat.stm32.bluetoothremotecarcontroler.connection;

/**
 * Created by huangzhengyue on 2016/10/27.
 */

public interface OnDataInListener {
    void onDataIn(byte[] data, int size);
}
