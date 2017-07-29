package com.ronda.bluetoothassist.base;

import android.app.Application;

import com.ronda.bluetoothassist.BuildConfig;
import com.socks.library.KLog;


/**
 * 自定义Application,用来初始化全局信息等
 * Created by Dragon on 2016-1-15.
 */
public class MyApplication extends Application {

    public static final String TAG = MyApplication.class.getSimpleName();

    private static MyApplication myApplication;


    @Override
    public void onCreate() {
        super.onCreate();
        myApplication = this;

       init();
    }

    private void init() {
        KLog.init(BuildConfig.LOG_DEBUG, "ronda");

    }

    public static synchronized MyApplication getInstance() {
        return myApplication;
    }
}