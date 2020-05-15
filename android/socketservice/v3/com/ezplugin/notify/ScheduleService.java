package com.ezplugin.notify;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

// Created by hehao on 2017/7/13.

public class ScheduleService extends Service {

  /**
   * Class for clients to access
   */
  public class ServiceBinder extends Binder {
    ScheduleService getService() {
      return ScheduleService.this;
    }
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.i("ScheduleService", "Received start id " + startId + ": " + intent);

    // We want this service to continue running until it is explicitly stopped, so return sticky.
    return START_STICKY;
  }

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  // This is the object that receives interactions from clients. See
  private final IBinder mBinder = new ServiceBinder();

  /**
   * Show an alarm for a certain date when the alarm is called it will pop up a notification
   */
  public void addNotify( NotifyObject nobj )
  {
    if (nobj == null){
      return;
    }
    new AlarmSetTask(this,nobj).run();
  }
  public void removeNotify( NotifyObject nobj)
  {
    if (nobj == null){
      return;
    }
    new AlarmCancelTask(this,nobj).run();
  }
}