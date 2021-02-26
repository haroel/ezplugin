package com.ezplugin.notify;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
//import android.support.v4.app.NotificationCompat;

import androidx.core.app.NotificationCompat;

import org.json.JSONException;
import org.json.JSONObject;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by hehao on 2017/7/13.
 */

public class NotifyObject {

    private static String NOTIFY_TAG = "_local_notify_";

    public int key = 0; // 标识通知id ，同一类id的通知只会显示一次
    public long time = 0;
    public String title;
    public String body;
    /*
    * key: 通知类型id
    * time: 通知显示时间  毫秒级
    * title：标题
    * body：内容
    * */
    public NotifyObject(int key, long time, String title, String body)
    {
        this.key = key;
        this.time = time;
        this.title = title;
        this.body = body;
    }

    public NotifyObject( String jsonStr )
    {
        try{
            JSONObject jobj = new JSONObject(jsonStr);
            _initWithJson(jobj);
        }catch (JSONException e){
            e.printStackTrace();
        }
    }
    public NotifyObject( JSONObject jobj )
    {
        _initWithJson(jobj);
    }
    private void _initWithJson(JSONObject jobj )
    {
        try{
            this.key = jobj.getInt("key");
            this.time = jobj.getLong("time");
            this.title = AlarmUtil.decode(jobj.getString("title") ) ;
            this.body = AlarmUtil.decode(jobj.getString("body") ) ;
        }catch (JSONException e){
            e.printStackTrace();
        }
    }
    @Override
    public String toString() {
        return String.format("{" +
                        "\"%s\" : %d," +
                        "\"%s\" : %d," +
                        "\"%s\" : %s," +
                        "\"%s\" : %s"  +
                        "}",
                "key", key,"time", time,
                "title", AlarmUtil.encode( title ),
                "body",  AlarmUtil.encode( body ));
    }
//    @Override
//    public boolean equals(Object o) {
//        NotifyObject plan = (NotifyObject) o;
//        if (plan.time != time){
//            return false;
//        }
//        return this.body.equals(plan.body);
//    }
//
//    @Override
//    public int hashCode() {
//
//        return this.toString().hashCode();
//    }

    public NotificationCompat.Builder toNotificationCompat(Context mContext )
    {
        String name = this.title;
        String msg = this.body;
        // 通知栏点击处理，直接打开主Activity
        Intent notificationIntent =  mContext.getPackageManager().getLaunchIntentForPackage(mContext.getPackageName());
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity( mContext , 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        // 读取通知栏icon、游戏名称
        SharedPreferences preferences = mContext.getSharedPreferences( NOTIFY_TAG ,MODE_PRIVATE);
        String GAME_NAME = preferences.getString("game_name", name );
        int BIG_ICON = preferences.getInt("big_icon",0);
        int SMALL_ICON = preferences.getInt("small_icon",0);

        Bitmap bm = BitmapFactory.decodeResource( mContext.getResources(), BIG_ICON);

        NotificationCompat.Builder builder = new NotificationCompat.Builder( mContext );
        builder.setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setContentTitle(name)
                .setTicker(GAME_NAME)
                .setLargeIcon(bm)
                .setSmallIcon( SMALL_ICON )
                .setVisibility( NotificationCompat.VISIBILITY_PRIVATE )
                .setDefaults(   NotificationCompat.DEFAULT_ALL)
                .setContentText(msg);
        return builder;
    }

}
