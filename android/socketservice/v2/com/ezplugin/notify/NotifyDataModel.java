package com.ezplugin.notify;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.util.SparseArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by hehao on 2017/7/13.
 */

public class NotifyDataModel {
  private final static String TAG = "NotifyDataModel";

  private final static String NOTIFY_MSG_TAG = "_local_notify_msgs_";

  private static String NOTIFY_TAG = "_local_notify_";

  private SparseArray<NotifyObject> notifyMap = new SparseArray<>();
  private Context context;

  public NotifyDataModel(Context mContext)
  {
    context = mContext;
    // 读取本地配置信息
    try{
      // 读取保存的通知信息
      SharedPreferences preferences   = context.getSharedPreferences(NOTIFY_TAG,MODE_PRIVATE);
      String notifyMsgs = preferences.getString( NOTIFY_MSG_TAG ,"");
      if (!notifyMsgs.isEmpty()){
        try {
          long time = System.currentTimeMillis();
          String decodedString = AlarmUtil.decode(notifyMsgs);
          JSONArray jsonArray = new JSONArray(decodedString);
          for(int i = 0; i < jsonArray.length(); i++) {
            JSONObject oj = jsonArray.getJSONObject(i);
            NotifyObject obj = new NotifyObject(oj);
            if (obj.time > time){
              notifyMap.put( obj.key,obj );
            }
          }
          Log.d(TAG,String.format(" 初始化本地通知数据 %d",notifyMap.size() )  );
        } catch (Exception e) {
          e.printStackTrace();
          Log.e( TAG ,e.toString());
        }
      }
      ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo( context.getPackageName(), PackageManager.GET_META_DATA);
      int big_icon      = appInfo.metaData.getInt("com.game.push.gameicon");
      int small_icon    = appInfo.metaData.getInt("com.game.push.notifyicon");
      String game_name  = appInfo.metaData.getString("com.game.push.title");
      SharedPreferences.Editor editor = preferences.edit();
      //存储数据时选用对应类型的方法
      editor.putInt("big_icon",big_icon);
      editor.putInt("small_icon",small_icon);
      editor.putString("game_name",game_name);
      //提交保存数据
      editor.commit();
    }catch (PackageManager.NameNotFoundException e){
      e.printStackTrace();
      Log.e( TAG ,e.toString());
    }
  }

  public SparseArray<NotifyObject> getNotifyMap()
  {
    return notifyMap;
  }
  public NotifyObject getNotifyObject(int _key){
    return notifyMap.get(_key);
  }
  public void putNotifyObject( NotifyObject obj)
  {
    if (obj == null){
      return;
    }
    notifyMap.put( obj.key ,obj); // 如果key相同则会把之前的覆盖
    this._saveToFile();
  }
  public void removeNotifyObject( int key)
  {
    notifyMap.remove(key);
    this._saveToFile();
  }

  public void clear()
  {
    notifyMap.clear();
    this._saveToFile();
  }
  /*
   * 将注册的通知信息保存到配置文件里
   * */
  private void _saveToFile(){
    long time = System.currentTimeMillis();
    ArrayList deleteList= new ArrayList();
    JSONArray jsonArray = new JSONArray();
    for(int i = 0; i < notifyMap.size(); i++)
    {
      int key = notifyMap.keyAt(i); // get the object by the key.
      NotifyObject data = notifyMap.get(key);
      if (data.time > time){
        try {
          JSONObject jobj = new JSONObject(data.toString());
          jsonArray.put(jobj);
        }catch (JSONException e){
          e.printStackTrace();
        }
      }else{
        deleteList.add(key);
      }
    }
    for (int j =0;j<deleteList.size();j++)
    {
      notifyMap.remove((int)deleteList.get(j));
    }
    deleteList = null;
    Log.d( TAG ,"saveToFile");
    Log.d( TAG ,jsonArray.toString());
    String encodedString = AlarmUtil.encode(jsonArray.toString());
    SharedPreferences preferences   = context.getSharedPreferences( NOTIFY_TAG,MODE_PRIVATE);
    SharedPreferences.Editor editor = preferences.edit();
    editor.putString(NOTIFY_MSG_TAG, encodedString );
    editor.commit();
  }
}
