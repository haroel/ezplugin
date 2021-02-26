package com.ezplugin.plugins;

import android.content.Context;
import android.content.Intent;

import com.ezplugin.notify.NotifyObject;
import com.ezplugin.notify.ScheduleClient;
import com.ezplugin.core.*;


import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * Android本地消息插件
 * Created by hehao on 2017/7/18.
 */
public class PluginNotify extends PluginBase {

  private ScheduleClient client = null;
  @Override
  public void initPlugin(Context context, JSONObject params) {
    super.initPlugin(context,params);
    if (client == null){
      client = ScheduleClient.getInstance();
      client.init(context);
    }
  }

  @Override
  public void excute(String type,String params,final int callback){
    try {
      if (type.equals("add")){
        JSONObject jsonObject = new JSONObject(params);
        client.addLocalNotify(new NotifyObject(jsonObject));
      }
      if(type.equals("remove")){
        int key = Integer.valueOf(params);
        client.removeLocalNotify(key);
      }
      if(type.equals("show")){
        JSONObject jsonObject = new JSONObject(params);
        client.addLocalNotify(new NotifyObject(jsonObject));
      }
      this.nativeCallbackHandler(callback,"success");
    }catch (JSONException e){
      e.printStackTrace();
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    client.doUnbindService();
  }

  @Override
  public void onResume(Intent intent) {
    super.onResume(intent);
    client.removeAllOnShow();
  }
}
