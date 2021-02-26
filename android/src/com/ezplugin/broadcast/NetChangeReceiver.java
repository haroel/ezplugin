package com.ezplugin.broadcast;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

import com.ezplugin.core.PluginCore;
import com.ezplugin.utils.NetAssistUtil;

public class NetChangeReceiver extends BroadcastReceiver {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            int nettype = NetAssistUtil.netType(context);
            PluginCore.getInstance().nativeEventHandler("PluginOS","network_change",nettype+"");
        } catch (Exception e) {
            //ignore
        }
    }
}