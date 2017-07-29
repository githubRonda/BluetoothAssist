package com.ronda.bluetoothassist;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.ronda.bluetoothassist.base.AppConst;
import com.ronda.bluetoothassist.utils.HexUtils;
import com.ronda.bluetoothassist.utils.SPUtils;
import com.socks.library.KLog;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private BluetoothChatService mChatService;

    private TextView tv_label;
    private EditText et_send, et_receive;
    private Button btn_connect, btn_clear, btn_send;
    private CheckBox   cb_hex;
    private ScrollView mScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        mChatService = new BluetoothChatService(mHandler);


        // 连接蓝牙时，会有对话框提示，所以不能在onCreate()中，只能是所有View绘制完之后才可以
        String addr = SPUtils.getMainBluetoothAddr();

        if (addr.isEmpty()) {
            showDialog();
        } else {//自动连接蓝牙
            mChatService.connect(addr);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mChatService.stop();
    }

    private void initView() {
        tv_label = (TextView) findViewById(R.id.tv_label);

        et_send = (EditText) findViewById(R.id.et_send);
        et_receive = (EditText) findViewById(R.id.et_receive);

        cb_hex = (CheckBox) findViewById(R.id.cb_hex);

        btn_connect = (Button) findViewById(R.id.btn_connect);
        btn_clear = (Button) findViewById(R.id.btn_clear);
        btn_send = (Button) findViewById(R.id.btn_send);

        mScrollView = (ScrollView) findViewById(R.id.scroll_view);

        btn_connect.setOnClickListener(this);
        btn_clear.setOnClickListener(this);
        btn_send.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_connect:
                if (btn_connect.getText().toString().equals("连接")) {
                    showDialog();
                } else {
                    mChatService.stop();
                }
                break;
            case R.id.btn_clear:
                et_receive.setText("");
                break;
            case R.id.btn_send:
                mChatService.write(et_send.getText().toString().getBytes());
                break;
        }
    }


    private void showDialog() {
        DeviceListDialogFragment dialog = new DeviceListDialogFragment();
        dialog.setCallback(new DeviceListDialogFragment.Callback() {
            @Override
            public void onSelectedItem(String address) {
                // 连接远程蓝牙
                mChatService.connect(address);
            }
        });
        dialog.show(MainActivity.this.getFragmentManager(), "deviceListDialogFragment");
    }


    /**
     * 多行显示 始终定位到底部
     *
     * 根据scrolview 和子view去测量滑动的位置
     *
     * @param scrollView
     * @param view
     */
    private void scrollToBottom(final ScrollView scrollView, final View view) {

        mHandler.post(new Runnable() {

            @Override
            public void run() {
                if (scrollView == null || view == null) {
                    return;
                }
                // offset偏移量。是指当textview中内容超出 scrollview的高度，那么超出部分就是偏移量
                int offset = view.getMeasuredHeight() - scrollView.getMeasuredHeight();
                if (offset < 0) {
                    offset = 0;
                }
                //scrollview开始滚动
                scrollView.scrollTo(0, offset);
            }
        });
    }


    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AppCompatActivity activity = MainActivity.this;
            switch (msg.what) {
                case AppConst.MESSAGE_STATE_CHANGE:

                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            String deviceName = msg.obj.toString();// 此时 obj 的值是 remoteDeviceName, 不为 null
                            tv_label.setText("connected to " + deviceName);
                            Toast.makeText(activity, "connected to " + deviceName, Toast.LENGTH_SHORT).show();
                            btn_connect.setText("断开");
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            tv_label.setText("connecting...");
                            Toast.makeText(activity, "connecting...", Toast.LENGTH_SHORT).show();
                            btn_connect.setText("断开");
                            break;
                        case BluetoothChatService.STATE_NONE:
                            tv_label.setText("not connected");
                            btn_connect.setText("连接");
                            Toast.makeText(activity, "not connected", Toast.LENGTH_SHORT).show();
                            break;
                    }
                    break;
                case AppConst.MESSAGE_WRITE:
                    String writeMessage = (String) msg.obj;
                    //mViewData.setText(writeMessage);
                    KLog.e("write: " + writeMessage);
                    break;
                case AppConst.MESSAGE_READ:
                    //String readMessage = (String) msg.obj;
                    byte[] buf = (byte[]) msg.obj;
                    String readTxt = new String(buf, 0, buf.length);
                    if (cb_hex.isChecked()) {
                        readTxt = HexUtils.bytesToHexStringWithSpace(buf);
                    }

                    et_receive.append(readTxt);

                    scrollToBottom(mScrollView, et_receive);

                    KLog.e("read: " + readTxt);
                    break;
                case AppConst.MESSAGE_TOAST: // 接收连接时失败 和 已连接后又中断 的情况
                    Toast.makeText(activity, msg.getData().getString(AppConst.TOAST), Toast.LENGTH_SHORT).show();
            }
        }
    };
}
