package com.ronda.bluetoothassist.base;

/**
 * Author: Ronda(1575558177@qq.com)
 * Date: 2017/02/28
 * Version: v1.0
 * <p>
 * 定义了一些常量
 */

public interface AppConst {
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5; // 连接时失败 和 通讯过程中 中断 的情况. 表示 message 中的 what 值
    // Key names received from the BluetoothChatService Handler
    public static final String TOAST = "toast";
}