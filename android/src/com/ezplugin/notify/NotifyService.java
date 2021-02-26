package com.ezplugin.notify;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
//import android.support.v4.app.NotificationCompat;
import android.util.Log;

import androidx.core.app.NotificationCompat;

/**
 *
 * Created by hehao on 2017/7/13.
 */
public class NotifyService extends Service {

	/**
	 * Class for clients to access
	 */
	public class ServiceBinder extends Binder {
		NotifyService getService() {
			return NotifyService.this;
		}
	}

	// Name of an intent extra we can use to identify if this service was started to create a notification	
	public static final String INTENT_NOTIFY = "NotifyService_intent";

	public static final String INTENT_NOTIFY_MSG = "INTENT_NOTIFY_MSG";

	// The system notification manager
	private NotificationManager mNM;

	@Override
	public void onCreate() {
		Log.i("NotifyService", "onCreate()");
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("LocalService", "Received start id " + startId + ": " + intent);
		// If this service was started by out AlarmTask intent then we want to show our notification
		if(intent.getBooleanExtra(INTENT_NOTIFY, false))
		{
			NotifyObject nobj = new NotifyObject(intent.getStringExtra( NotifyService.INTENT_NOTIFY_MSG));
			showNotification(nobj);
		}
		// We don't care if this service is stopped as we have already delivered our notification
		return START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	// This is the object that receives interactions from clients
	private final IBinder mBinder = new ServiceBinder();

	/**
	 * Creates a notification and shows it in the OS drag-down status bar
	 */
	private void showNotification(NotifyObject nobj) {

		NotificationCompat.Builder builder = nobj.toNotificationCompat(this);
		mNM.notify( nobj.key , builder.build());
		// Stop the service when we are finished
		stopSelf();
	}
}