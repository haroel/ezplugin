package com.ezplugin.utils;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

import com.ezplugin.core.PluginCore;

public class NetChangeReceiver extends BroadcastReceiver {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            int nettype = NetAssistUtil.netType(context);
            Log.i("NetChangeReceiver", "网络发生变化");
            PluginCore.getInstance().nativeEventHandler("PluginOS","network_change",nettype+"");
        } catch (Exception e) {
            //ignore
        }
    }
}