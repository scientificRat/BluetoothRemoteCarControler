package com.scientificrat.stm32.bluetoothremotecarcontroler.activity;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.scientificrat.stm32.bluetoothremotecarcontroler.R;
import com.scientificrat.stm32.bluetoothremotecarcontroler.connection.BluetoothConnection;
import com.scientificrat.stm32.bluetoothremotecarcontroler.widget.OnRockerChangeListener;
import com.scientificrat.stm32.bluetoothremotecarcontroler.widget.Rocker;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.TimerTask;

public class RockerControlActivity extends AppCompatActivity {

    //widgets
    private Button configureButton;
    private Rocker rockerLeft;
    private Rocker rockerRight;
    private TextView deviceInfoTextView;
    private ToggleButton emergencyToggleButton;


    private BluetoothConnection bluetoothConnection = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //隐藏状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_rocker_control);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        //设置按钮
        configureButton = (Button) findViewById(R.id.configure_button);
        //左摇杆
        rockerLeft = (Rocker) findViewById(R.id.rockerLeft);
        //右摇杆
        rockerRight = (Rocker) findViewById(R.id.rockerRight);
        //设备信息textView
        deviceInfoTextView = (TextView) findViewById(R.id.deviceInfoTextView);
        //紧急制动 按钮
        emergencyToggleButton = (ToggleButton) findViewById(R.id.emergencyToggleButton);

        this.bluetoothConnection = BluetoothConnection.getInstance();
        //如果蓝牙没有连接，说明错误进入activity, 返回
        if (this.bluetoothConnection == null) {
            Intent intent = new Intent(RockerControlActivity.this, DeviceListActivity.class);
            startActivity(intent);
            return;
        }
        //显示连接的设备
        BluetoothDevice connectedDevice = bluetoothConnection.getBluetoothDevice();
        deviceInfoTextView.setText(connectedDevice.getAddress() + "\n" + connectedDevice.getName());
//        //左摇杆
//        rockerLeft.setOnRockerChangeListener(new OnRockerChangeListener() {
//            @Override
//            public void onRockerChange(float xShittingRatio, float yShittingRatio) {
//                String sendStr = "left:(" + Float.toString(xShittingRatio * 100) + "," + Float.toString(yShittingRatio * 100) + ")\n";
//                try {
//                    bluetoothConnection.sendRawData(sendStr.getBytes());
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//        //右摇杆
//        rockerRight.setOnRockerChangeListener(new OnRockerChangeListener() {
//            @Override
//            public void onRockerChange(float xShittingRatio, float yShittingRatio) {
//                String sendStr = "right:(" + Float.toString(xShittingRatio * 100) + "," + Float.toString(yShittingRatio * 100) + ")\n";
//                try {
//                    bluetoothConnection.sendRawData(sendStr.getBytes());
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
        // 不使用上面注释的代码，因为rocker的OnRockerChangeListener 触发太频繁，无法正常使用
        java.util.Timer timer = new java.util.Timer(true);
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                short left_x = (short) (rockerLeft.getOutputX() / rockerLeft.getRange() * 1000);
                short left_y = (short) (-rockerLeft.getOutputY() / rockerLeft.getRange() * 1000);
                short right_x = (short) (rockerRight.getOutputX() / rockerRight.getRange() * 1000);
                short right_y = (short) (-rockerRight.getOutputY() / rockerRight.getRange() * 1000);
                ByteBuffer byteBuffer = ByteBuffer.allocate(9);
                //小端序
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                byteBuffer.put((byte) 'c');
                byteBuffer.putShort(left_x);
                byteBuffer.putShort(left_y);
                byteBuffer.putShort(right_x);
                byteBuffer.putShort(right_y);
                try {
                    bluetoothConnection.sendRawData(byteBuffer.array());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        // 从现在起50ms 发送一次指令
        timer.schedule(timerTask, 1, 50);
        //单片机还不支持这个指令

//        // 紧急制动
//        emergencyToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                String sendStr;
//                if (isChecked) {
//                    sendStr = "S\nS\n";
//
//                } else {
//                    sendStr = "B\n";
//                }
//                try {
//                    bluetoothConnection.sendRawData(sendStr.getBytes());
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        });


        configureButton.setOnClickListener(view -> {
            Intent intent = new Intent(RockerControlActivity.this, DeviceListActivity.class);
            startActivity(intent);
        });
        Log.e("fuck:", "started");
    }
}
