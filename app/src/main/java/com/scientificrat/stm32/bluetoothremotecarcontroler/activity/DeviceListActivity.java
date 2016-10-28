package com.scientificrat.stm32.bluetoothremotecarcontroler.activity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.scientificrat.stm32.bluetoothremotecarcontroler.R;
import com.scientificrat.stm32.bluetoothremotecarcontroler.adapter.DeviceListAdapter;
import com.scientificrat.stm32.bluetoothremotecarcontroler.connection.BluetoothConnection;
import com.scientificrat.stm32.bluetoothremotecarcontroler.connection.OnConnectListener;

public class DeviceListActivity extends AppCompatActivity {

    //成功开启蓝牙后，onActivityResult 收到的code
    private final int BLUETOOTH_REQUEST_ENABLE_BT = 2;
    //成功开启蓝牙标志
    private boolean bluetoothOpen = false;
    //蓝牙适配器
    private BluetoothAdapter bluetoothAdapter;

    //控件 widgets
    private Switch discoverDeviceSwitch;
    private LinearLayout functionalPanel;
    private ListView deviceListView;
    private LinearLayout controlPanel;
    private ToggleButton toggleConnect;
    private TextView selectedDeviceTextView;
    private LinearLayout selectActivityPanel;
    private ProgressBar connectionProgressBar;
    private TextView connectionStateTextView;
    private Button enterRockerControlButton;
    private Button enterCharacterControlButton;

    private DeviceListAdapter deviceListAdapter;

    /**
     * 这个将被注册到系统广播
     * Create a BroadcastReceiver for BluetoothDevice.ACTION_FOUND
     */
    private final BroadcastReceiver mBlueToothDeviceFoundReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add to the device listView
                DeviceListActivity.this.deviceListAdapter.addDevice(device);
            }
        }
    };

    /**
     * activity 创建时调用
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        //扫描蓝牙设备开关
        discoverDeviceSwitch = (Switch) findViewById(R.id.discoverDeviceSwitch);
        //功能面板
        functionalPanel = (LinearLayout) findViewById(R.id.functionalPanel);
        //设备列表
        deviceListView = (ListView) findViewById(R.id.deviceList);
        //下方控制面板
        controlPanel = (LinearLayout) findViewById(R.id.controlPanel);
        //连接按钮
        toggleConnect = (ToggleButton) findViewById(R.id.toggle_connect);
        //选择具体控制方式面板
        selectActivityPanel = (LinearLayout) findViewById(R.id.selectActivityPanel);
        //选择的设备文本框
        selectedDeviceTextView = (TextView) findViewById(R.id.selectedDeviceTextView);
        //连接等待进度
        connectionProgressBar = (ProgressBar) findViewById(R.id.connectionProgressBar);
        //连接状态提示
        connectionStateTextView = (TextView) findViewById(R.id.connectionStateTextView);
        //进入摇杆控制界面
        enterRockerControlButton = (Button) findViewById(R.id.enterRockerControlButton);
        //进入字符控制界面
        enterCharacterControlButton = (Button) findViewById(R.id.enterCharacterControlButton);

        //设置设备列表
        this.deviceListAdapter = new DeviceListAdapter(this);
        deviceListView.setAdapter(this.deviceListAdapter);

        //设备列表响应
        deviceListView.setOnItemClickListener((parent, view, position, id) -> {
            deviceListAdapter.setSelectedPosition(position);
            deviceListAdapter.notifyDataSetInvalidated();
            controlPanel.setVisibility(View.VISIBLE);
            String selectedDeviceAddress = this.deviceListAdapter.getDeviceAddress(position);
            //显示当前选择的设备
            selectedDeviceTextView.setText(selectedDeviceAddress);
            this.deviceListAdapter.getDeviceUUID(position);

        });

        //扫描设备前先隐藏功能区域
        functionalPanel.setVisibility(View.GONE);
        //下方控制面板，一开始隐藏，选择设备后才显示
        controlPanel.setVisibility(View.GONE);
        //扫描设备开关
        discoverDeviceSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!bluetoothOpen) {
                    if (!initBluetooth()) {
                        buttonView.setChecked(false);
                        return;
                    }
                }
                //开启扫描,如果已经开启，先关闭
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }
                this.bluetoothAdapter.startDiscovery();
                // 注册 BroadcastReceiver
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(mBlueToothDeviceFoundReceiver, filter);
                // 显示功能区
                functionalPanel.setVisibility(View.VISIBLE);
                buttonView.setText("停止程序");
            } else {
                //先清空列表，表示刷新的意思
                deviceListAdapter.clear();
                buttonView.setText("扫描设备");
                //停止扫描蓝牙设备
                unregisterReceiver(mBlueToothDeviceFoundReceiver);
                this.bluetoothAdapter.cancelDiscovery();
                //下方控制面板
                controlPanel.setVisibility(View.GONE);
                //选择控制方式面板隐藏
                selectActivityPanel.setVisibility(View.GONE);
                //连接状态
                connectionProgressBar.setVisibility(View.GONE);
                connectionStateTextView.setText("(未连接)");
                //连接按钮
                toggleConnect.setChecked(false);
            }
        });

        //隐藏进度条
        connectionProgressBar.setVisibility(View.GONE);

        //连接按钮
        toggleConnect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                connectionProgressBar.setVisibility(View.VISIBLE);
                connectionStateTextView.setText("(连接中)");

                BluetoothConnection bluetoothConnection = BluetoothConnection.createInstance(deviceListAdapter.getSelectedDevice());
                bluetoothConnection.start();

                bluetoothConnection.setOnConnectListener(new OnConnectListener() {
                    @Override
                    public void onConnect() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                DeviceListActivity.this.connectionProgressBar.setVisibility(View.GONE);
                                DeviceListActivity.this.connectionStateTextView.setText("(已连接)");
                                DeviceListActivity.this.selectActivityPanel.setVisibility(View.VISIBLE);
                            }
                        });

                    }
                });


            } else {
                //关闭当前连接
                BluetoothConnection bluetoothConnection = BluetoothConnection.getInstance();
                if (bluetoothConnection != null) {
                    //关闭连接
                    bluetoothConnection.cancel();
                }
                selectActivityPanel.setVisibility(View.GONE);
                connectionProgressBar.setVisibility(View.GONE);
                connectionStateTextView.setText("(未连接)");
            }
        });


        //选择控制方式面板隐藏,连接后显示
        selectActivityPanel.setVisibility(View.GONE);

        //进入摇杆控制设置listener
        enterRockerControlButton.setOnClickListener(view -> {
            //开启摇杆控制activity
            Log.e("fuck:","ready to start");
            Intent intent = new Intent(DeviceListActivity.this, RockerControlActivity.class);
            startActivity(intent);
        });

    }


    /**
     * 初始化蓝牙设备
     */
    private boolean initBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Device does not support Bluetooth then alert
            Toast.makeText(getApplicationContext(), "你的手机没有蓝牙2333～", Toast.LENGTH_LONG).show();
            return false;
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, BLUETOOTH_REQUEST_ENABLE_BT);
            return false;
        } else {
            this.bluetoothOpen = true;
        }

        // 请求定位权限，垃圾安卓在6.0 不开启这个权限不能扫描周边蓝牙设备
        int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

        return true;
    }

    /**
     * 开启蓝牙外部程序返回会调用这个函数
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 如果蓝牙开启成功, 设置当前状态为开启
        if (requestCode == BLUETOOTH_REQUEST_ENABLE_BT) {
            // FIXME: 2016/10/28 有可能返回失败者，还没有处理resultCode
            this.bluetoothOpen = true;
        }
    }
}
