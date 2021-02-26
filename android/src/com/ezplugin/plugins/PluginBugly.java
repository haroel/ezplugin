package com.ezplugin.plugins;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ezplugin.core.PluginBase;
import com.ezplugin.utils.SysUtils;
import com.tencent.bugly.crashreport.BuglyLog;
import com.tencent.bugly.crashreport.CrashReport;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PluginBugly extends PluginBase {

    final static int CATEGORY_JS_EXCEPTION  = 5;

    /**
     * js 异常捕获
     * @param location
     * @param Message
     * @param stack
     */
    public static void exceptionCallbackFromCpp(String location, String Message, String stack){
        Log.w("PluginBugly exception",location);
        Log.w("PluginBugly exception",Message);
        Log.w("PluginBugly exception",stack);
        Map<String, String> map = new HashMap<String, String>();
        map.put("location", location);
        map.put("Message", Message);
        map.put("stack", stack);
        CrashReport.postException(CATEGORY_JS_EXCEPTION, location, Message,stack, map);
    }

    public void initPlugin(Context context, JSONObject jobj) {
        super.initPlugin(context, jobj);
        if (isInited){
            return;
        }
        try{
            ApplicationInfo appInfo = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(),
                            PackageManager.GET_META_DATA);
            String appID=appInfo.metaData.getString("BUGLY_APPID");

            boolean isDebug = SysUtils.isApkInDebug(context);
            CrashReport.initCrashReport( getContext(), appID, isDebug);
        }catch (PackageManager.NameNotFoundException e){
            e.printStackTrace();
        }
    }

    @Override
    public void excute(String action, String params, int callback) {
        super.excute(action, params, callback);
        switch (action){
            case "userid":{
                CrashReport.setUserId(params);
                break;
            }
            case "log":{
                try{
                    JSONObject jobj = new JSONObject(params);
                    BuglyLog.e(jobj.getString("file"),
                            jobj.getString("msg") + " *************** \n" + jobj.getString("error"));
                }catch (JSONException e){
                    CrashReport.postCatchedException(e);  // bugly会将这个throwable上报
                }
                Log.d("PluginCrash",params);
                break;
            }
        }
    }
}
