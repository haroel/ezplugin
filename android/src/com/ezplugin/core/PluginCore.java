package com.ezplugin.core;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.ezplugin.utils.DeviceIdUtils;
import com.ezplugin.utils.SysUtils;

import org.cocos2dx.lib.Cocos2dxJavascriptJavaBridge;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;

import sockets.WebSocketClient;

import static org.cocos2dx.lib.Cocos2dxHelper.runOnGLThread;

/**
 *
 * Created by hehao on 2017/7/17.
 */

public class PluginCore {

    private static final String TAG = "PluginCore";
    // 包名
    private static final String PACKAGE = "com.ezplugin.plugins";


    private static PluginCore _instance = null;

    /**
     * 系统信息
     */
    private static String sysInfo = "";

    private Activity mContext = null;
    public Activity getContext(){
        return mContext;
    }

    private HashMap<String,PluginBase> pluginMap = new HashMap<String,PluginBase>();



    private HashMap<String,String> _globalVariables = new HashMap<String,String>();

    public void setGlobalVariable(String key,String value){
        if (!TextUtils.isEmpty(key)){
            _globalVariables.put(key,value);
        }
    }
    public String getGlobalVariable(String key){
        if (!TextUtils.isEmpty(key)){
            return _globalVariables.get(key);
        }
        return "";
    }
    public void removeGlobalVariable(String key){
        if (!TextUtils.isEmpty(key)){
             _globalVariables.remove(key);
        }
    }
    public void cleanGlobalVariables(){
        _globalVariables.clear();
    }

    public static PluginCore getInstance(){
        if (PluginCore._instance == null){
            PluginCore._instance = new PluginCore();
        }
        return PluginCore._instance;
    }

    private PluginCore(){
    }

    public void init(final Activity context){
        WebSocketClient.getInstance().init(context);
        String rets = "";
        try{
            JSONObject jobj = new JSONObject();
            jobj.put("appVersion", SysUtils.getVersionName(context) );
            jobj.put("packageName",context.getPackageName());
            jobj.put("versionCode",SysUtils.getAppVersionCode(context));
            jobj.put("osVersion",android.os.Build.VERSION.RELEASE);
            jobj.put("deviceModel",android.os.Build.MODEL);
            jobj.put("appName",SysUtils.getVersionName(context));
            jobj.put("uuid", DeviceIdUtils.getUUID(context));
            String language = Locale.getDefault().getLanguage();
            jobj.put("lang",language);
            Intent intent = context.getIntent();
            Uri uri = intent.getData();
            if (uri != null) {
                jobj.put("launchURL",uri.toString());
            }
            if (_globalVariables.containsKey("referrer")){
                jobj.put("referrer",_globalVariables.get("referrer"));
            }
            Log.e(TAG,jobj.toString());
            rets = jobj.toString();
        }
        catch(JSONException e){
            e.printStackTrace();
            rets = "{}";
        }
        PluginCore.sysInfo = rets;

        mContext = context;
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(),
                    PackageManager.GET_META_DATA);
            String initialPlugins = appInfo.metaData.getString("com.ezplugin.initialPlugins","");
            Log.d(TAG,"initialPlugins = "+initialPlugins);
            String[] arr = initialPlugins.split(",");
            for (String pluginName:arr){
                this.__createPlugin(pluginName,null);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void onResume(Intent intent){
        for (HashMap.Entry<String, PluginBase> entry : pluginMap.entrySet()) {
            entry.getValue().onResume(intent);
        }
    }
    public void onPause(Intent intent){
        for (HashMap.Entry<String, PluginBase> entry : pluginMap.entrySet()) {
            entry.getValue().onPause(intent);
        }
    }
    public void onDestroy(){
        WebSocketClient.getInstance().destroy();

        for (HashMap.Entry<String, PluginBase> entry : pluginMap.entrySet()) {
            entry.getValue().onDestroy();
        }
    }
    public void onRestart(){
        for (HashMap.Entry<String, PluginBase> entry : pluginMap.entrySet()) {
            entry.getValue().onRestart();
        }
    }
    public void onStart(){
        for (HashMap.Entry<String, PluginBase> entry : pluginMap.entrySet()) {
            entry.getValue().onStart();
        }
    }
    public void onStop(){
        for (HashMap.Entry<String, PluginBase> entry : pluginMap.entrySet()) {
            entry.getValue().onStop();
        }
    }
    public void onNewIntent(Intent intent){
        for (HashMap.Entry<String, PluginBase> entry : pluginMap.entrySet()) {
            entry.getValue().onNewIntent(intent);
        }
    }
    public void onBackPressed(){
        for (HashMap.Entry<String, PluginBase> entry : pluginMap.entrySet()) {
            entry.getValue().onBackPressed();
        }
    }
    public void onConfigurationChanged(Configuration newConfig){
        for (HashMap.Entry<String, PluginBase> entry : pluginMap.entrySet()) {
            entry.getValue().onConfigurationChanged(newConfig);
        }
    }
    public void onRestoreInstanceState(Bundle savedInstanceState){
        for (HashMap.Entry<String, PluginBase> entry : pluginMap.entrySet()) {
            entry.getValue().onRestoreInstanceState(savedInstanceState);
        }
    }
    public void onSaveInstanceState(Bundle outState){
        for (HashMap.Entry<String, PluginBase> entry : pluginMap.entrySet()) {
            entry.getValue().onSaveInstanceState(outState);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        for (HashMap.Entry<String, PluginBase> entry : pluginMap.entrySet()) {
            entry.getValue().onActivityResult(requestCode,resultCode,data);
        }
    }

    public void onRequestPermissionsResult(int requestCode,  final String[] permissions, int[] grantResults) {
        for (HashMap.Entry<String, PluginBase> entry : pluginMap.entrySet()) {
            entry.getValue().onRequestPermissionsResult(requestCode,permissions,grantResults);
        }
    }

    public PluginBase getPluginByName(String pluginName){
        return pluginMap.get(pluginName);
    }


    /**
     * 返回app系统信息
     * @return
     */
    public static String sysInfo(){
        return sysInfo;
    }

    /**
     * 初始化多个插件
     * @param params
     * @param callback
     */
    public static void initAllPlugins( final String params,final int callback )
    {
        final PluginCore core = PluginCore.getInstance();
        core.getContext().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONArray jsonArray = new JSONArray(params);
                    for(int i = 0; i < jsonArray.length(); i++) {
                        JSONObject oj = jsonArray.getJSONObject(i);
                        String pluginName = oj.getString("pluginName");
                        core.__createPlugin(pluginName,oj.getJSONObject("params"));
                    }
                    StringBuilder result = new StringBuilder();
                    for (String pluginName : core.pluginMap.keySet()){
                        if (result.length() >1){
                            result.append("&");
                        }
                        result.append(pluginName);
                    }
                    Log.d(TAG,"initAllPlugins finish!" + result);
                    core.nativeCallbackHandler(callback,result.toString());
                    return;
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.d(TAG,"initAllPlugins finish!");
                core.nativeCallbackErrorHandler(callback,"initAllPlugins error");
            }
        });
    }

    /**
     * 初始化单个插件
     * @param pluginName
     * @param params
     * @param callback
     */
    public static void initPlugin( final String pluginName ,final String params ,final int callback )
    {
        final PluginCore core = PluginCore.getInstance();
        core.getContext().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jobj = new JSONObject(params);
                    PluginBase plugin = core.__createPlugin(pluginName,jobj);
                    if (plugin != null){
                        core.nativeCallbackHandler(callback,"1");
                        return;
                    }
                }
                catch (JSONException e)
                {
                    e.printStackTrace();
                }
                catch(NullPointerException e){
                    e.printStackTrace();
                }
                core.nativeCallbackErrorHandler(callback,"initPlugin error");
            }
        });
    }

    private PluginBase __createPlugin(final String pluginName,final JSONObject params)
    {
        PluginBase plugin = pluginMap.get(pluginName);
        if (plugin == null){
            try {
                Class<?> Cls = Class.forName(PACKAGE +"." + pluginName);
                plugin = (PluginBase)(Cls.newInstance());
                plugin.isInited = false;
                pluginMap.put(pluginName,plugin);
            }
            catch (ClassNotFoundException e){
                e.printStackTrace();
            }
            catch (IllegalAccessException e){
                e.printStackTrace();
            }
            catch (InstantiationException e){
                e.printStackTrace();
            }
        }
        if (plugin == null){
            Log.e(TAG,"__createPlugin 失败"+pluginName);
            return null;
        }
        plugin.initPlugin(mContext,params);
        plugin.isInited = true;
        Log.d(TAG,"__createPlugin " + pluginName);
        return plugin;
    }
    /*
     * 执行插件方法
     * pluginName : 插件类名
     * action ： 执行动作类型
     * params ：参数
     * */
    public static void excute(final String pluginName,final String action, final String params,final int callback)
    {
        PluginCore core = PluginCore.getInstance();
        Log.d(TAG," excute: " + pluginName + "  action:" +action);
        PluginBase plugin = core.getPluginByName(pluginName);
        if (plugin != null){
            try{
                plugin.excute( action,params,callback);
            }catch (Exception e){
                e.printStackTrace();
                Log.e(TAG,"excute Error ：" + pluginName + " ：" + params);
                core.nativeCallbackErrorHandler(callback,"error, " + pluginName + " excutePluginAction");
                return;
            }
        }else{
            Log.e(TAG," Not found ：" + pluginName);
            core.nativeCallbackErrorHandler(callback,"error, " + pluginName + " Not found!");
        }
    }
    /*
     * 在Android UI主线程中执行插件方法。
     * 相当于异步调用
     * pluginName : 插件类名
     * action ： 执行动作类型
     * params ：参数
     * */
    public static void excuteInUIThread(final String pluginName,final String action, final String params,final int callback)
    {
        final PluginCore core = PluginCore.getInstance();
        core.getContext().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                excute( pluginName,action,params,callback );
            }
        });
    }

    /**
     * 字符串转换成为16进制(无需Unicode编码)
     * @param str
     * @return
     */
    public static String _stringToParams(String str) {
        if (str==null){
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int len = str.length();
        for (int i = 0; i < len; i++) {
            int v= str.codePointAt(i);
            sb.append(v);
            if (i < len - 1) {
                sb.append("|");
            }
        }
        return sb.toString().trim();
    }

//  String toBase64(String params){
//    String _params = Base64.encodeToString(params.getBytes(), Base64.DEFAULT);
//    return _params.replaceAll("[\\s*\t\n\r]", "");
//  }

    // 事件机制 Java回调js
    public void nativeEventHandler(final String pluginName,final String event,final String params){
        if (pluginName.isEmpty()){
            Log.e(TAG,"nativeEventHandler 错误,pluginName is Empty! " + event + " -- " +params);
            return;
        }
        runOnGLThread(new Runnable() {
            @Override
            public void run() {
                String _params = _stringToParams(params);
                String _safeStr = String.format("ezplugin.nativeEventHandler('%s','%s','%s');",pluginName,event,_params);
                Cocos2dxJavascriptJavaBridge.evalString(_safeStr);
            }
        });
    }
    // 回调函数机制，一次有效 Java回调js
    public void nativeCallbackHandler(final int callbackId,final String params){
        if (callbackId == 0){
            return;
        }
        runOnGLThread(new Runnable() {
            @Override
            public void run() {
                String _params = _stringToParams(params);
                String _safeStr = String.format("ezplugin.nativeCallbackHandler('%s','%s');",callbackId,_params);
                Cocos2dxJavascriptJavaBridge.evalString(_safeStr);
            }
        });
    }
    // 错误回调函数机制，一次有效 Java回调js
    public void nativeCallbackErrorHandler(final int callbackId, final String params){
        if (callbackId == 0){
            return;
        }
        runOnGLThread(new Runnable() {
            @Override
            public void run() {
                String _params = _stringToParams(params);
                String _safeStr = String.format("ezplugin.nativeCallbackErrorHandler('%s','%s');",callbackId,_params);
                Cocos2dxJavascriptJavaBridge.evalString(_safeStr);
            }
        });
    }
}
