package com.ezplugin.core;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONObject;

/**
 * 插件基类
 * 抽象类，禁止实例化，必须继承
 * Created by hehao on 2017/7/18.
 */

public abstract class PluginBase {

  private Context mContext = null;

  public Context getContext(){
    return this.mContext;
  }

  public Application getApplication(){
    return ((Activity)(this.mContext)).getApplication();
  }
  /*
   * 获取类名
   * */
  public String pluginName(){
    return this.getClass().getSimpleName();
  }

  /**
   * 判断是否已初始化过，防止某些SDK初始化重复调用可能出现问题
   */
  public boolean isInited = false;

  /**
   * 插件初始化
   * @param context
   * @param jobj 插件初始化参数
   */
  public void initPlugin(Context context, JSONObject jobj){
    this.mContext = context;

    Log.d(this.getClass().getName(),"initPlugin");
  }

  /**
   * 执行插件方法
   * @param action
   * @param params
   * @param callback
   */
  public void excute(String action,String params,final int callback){

  }

  public void onResume(Intent intent){

  }
  public void onPause(Intent intent){

  }
  public void onDestroy(){

  }
  public void onNewIntent(Intent intent){

  }
  public void onRestart(){}
  public void onStop(){}
  public void onBackPressed(){}
  public void onConfigurationChanged(Configuration newConfig){}
  public void onRestoreInstanceState(Bundle savedInstanceState){}
  public void onSaveInstanceState(Bundle outState){}
  public void onStart(){}

  public  void onActivityResult(int requestCode, int resultCode, Intent data){

  }
  public  void onRequestPermissionsResult(int requestCode,  final String[] permissions, int[] grantResults) {

  }
  /*
   * 调用此方法将事件传给JS层
   * */
  final public void nativeEventHandler( String evt , String params){
    String PluginName = this.getClass().getSimpleName();
    PluginCore.getInstance().nativeEventHandler(PluginName,evt,params);
  }
  /*
   * 调用此方法将参数通过callback函数形式传给JS层，只会一次有效。注意与$eventToJS区别！
   * */
  final public void nativeCallbackHandler(final int callbackId,final String params){
    PluginCore.getInstance().nativeCallbackHandler(callbackId,params);
  }
  /*
   * 调用此方法将错误通过callback函数形式传给JS层，只会一次有效。注意与$eventToJS区别！
   * */
  final public void nativeCallbackErrorHandler(final int callbackId,final String params){
    PluginCore.getInstance().nativeCallbackErrorHandler(callbackId,params);
  }

}
