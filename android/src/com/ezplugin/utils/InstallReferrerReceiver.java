package com.ezplugin.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.ezplugin.core.PluginCore;

/**
 * 追踪googleplay安装消息
 * https://developers.google.com/app-conversion-tracking/third-party-trackers/android?hl=zh-cn
 */
public class InstallReferrerReceiver extends BroadcastReceiver {
    private static String TAG = "InstallReferrerReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent!= null){
            String referrer = intent.getStringExtra("referrer");
            Log.e(TAG, "install referrer:" + referrer);
            if(referrer != null && referrer.length() > 0){
                PluginCore.getInstance().setGlobalVariable("referrer",referrer);
            }
        }
    }
}
