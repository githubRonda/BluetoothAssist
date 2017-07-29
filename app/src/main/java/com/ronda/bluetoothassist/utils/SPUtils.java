package com.ronda.bluetoothassist.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.ronda.bluetoothassist.base.MyApplication;

/**
 * SharedPreferences 的帮助类
 * <p>
 * Author: Ronda(1575558177@qq.com)
 * Date: 2016/11/25
 * Version: v1.0
 */

public class SPUtils {

    private static SharedPreferences preferences = MyApplication.getInstance().getSharedPreferences("userinfo", Context.MODE_PRIVATE);
    private static SharedPreferences.Editor editor = preferences.edit();



    /********************** value --> String ***********************/

    /**
     * 蓝牙mac地址
     */
    private static final String MAIN_BLUETOOTH_ADDR = "main_bluetooth_addr";
    private static final String defMainBluetoothAddr = "";



    /*************************** 对外提供更简单的方法 **********************************/

    /**
     * 主界面中，持久化保存的一些信息
     */
    public static void setMainBluetoothAddr (String bluetoothAddr){
        putString(MAIN_BLUETOOTH_ADDR, bluetoothAddr);
    }

    public static String getMainBluetoothAddr(){
        return getString(MAIN_BLUETOOTH_ADDR, defMainBluetoothAddr);
    }

    /************************** 保存和读取基本类型的数据 *****************************/

    /**
     * 保存值为 String 类型的数据
     *
     * @param key
     * @param value
     */
    private static void putString(String key, String value) {
        editor.putString(key, value);
        editor.apply();
    }

    private static String getString(String key, String defValue) {
        return preferences.getString(key, defValue);
    }

    /**
     * 保存值为 boolean 类型的数据
     *
     * @param key
     * @param value
     */
    private static void putBoolean(String key, boolean value) {
        editor.putBoolean(key, value);
        editor.apply();
    }

    private static boolean getBoolean(String key, boolean defvalue) {
        return preferences.getBoolean(key, defvalue);
    }



    /**
     * 清除SharedPreference
     */
    public static void clearSharedPreference(){
        editor.clear();
        editor.apply();
    }
}
