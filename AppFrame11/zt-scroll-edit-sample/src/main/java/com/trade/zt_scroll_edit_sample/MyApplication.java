package com.trade.zt_scroll_edit_sample;

import android.app.Application;

/**
 * @author: zeting
 * @date: 2026/7/9
 *
 */
public class MyApplication extends Application {
    // 静态属性，通常用于存储Application实例，方便全局访问
    private static MyApplication instance;

    // 静态方法，用于获取Application实例
    public static MyApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this; // 在onCreate中初始化静态实例
    }
}
