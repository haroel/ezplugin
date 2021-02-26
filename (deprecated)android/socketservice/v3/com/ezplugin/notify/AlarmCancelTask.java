package com.ezplugin.notify;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


/**
 * Created by hehao on 2017/7/13.
 */
public class AlarmCancelTask implements Runnable{
  // The date selected for the alarm
  private final NotifyObject mNotifyObj;
  // The android system alarm manager
  private final AlarmManager am;
  // Your context to retrieve the alarm manager from
  private final Context context;

  public AlarmCancelTask(Context context, NotifyObject nObj) {
    this.context = context;
    this.am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    this.mNotifyObj = nObj;
  }
  @Override
  public void run() {
    Log.d("AlarmCancelTask", "local notify cancel " + this.mNotifyObj.title);
    // Request to start are service when the alarm date is upon us
    // We don't start an activity as we just want to pop up a notification into the system bar not a full activity
    Intent intent = new Intent(context, NotifyService.class);
    intent.putExtra(NotifyService.INTENT_NOTIFY, true);
    intent.putExtra(NotifyService.INTENT_NOTIFY_MSG,this.mNotifyObj.toString());
    PendingIntent pendingIntent = PendingIntent.getService(context, this.mNotifyObj.key , intent, PendingIntent.FLAG_UPDATE_CURRENT);
    try {
      this.am.cancel(pendingIntent);
    }catch (Exception e){
      e.printStackTrace();
    }
  }
}
