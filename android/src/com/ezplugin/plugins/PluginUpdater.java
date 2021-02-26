package com.ezplugin.plugins;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.ezplugin.core.PluginBase;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.OnCompleteListener;
import com.google.android.play.core.tasks.Task;

import org.json.JSONObject;

public class PluginUpdater extends PluginBase {
    public static String TAG = "PluginUpdater";

    final int REQUEST_CODE_UPDATE = 9001;

   private AppUpdateManager appUpdateManager = null;
    @Override
    public void initPlugin(Context context, JSONObject jobj) {
        super.initPlugin(context, jobj);
        if (appUpdateManager == null){
            appUpdateManager = AppUpdateManagerFactory.create(context);
        }
        // this.update();
    }

    @Override
    public void excute(String action, String params, int callback) {
        super.excute(action, params, callback);
        switch (action){
            case "update":{
                update();
                break;
            }
        }
    }
    private void update(){
        // Returns an intent object that you use to check for an update.
        Task<AppUpdateInfo> appUpdateInfo = appUpdateManager.getAppUpdateInfo();
        final Activity activity = (Activity)getContext();

        appUpdateInfo.addOnCompleteListener(new OnCompleteListener<AppUpdateInfo>() {
            @Override
            public void onComplete(Task task) {
                if (task.isSuccessful()) {
                    // 监听成功，不一定检测到更新
                    AppUpdateInfo it = (AppUpdateInfo)task.getResult();
                    Log.d(TAG,"updateAvailability = " + it.updateAvailability() + ", isUpdateTypeAllowed = " + it.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE));
                    if (it.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && it.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                        // 检测到更新可用且支持即时更新
                        try {
                            // 启动即时更新
                            appUpdateManager.startUpdateFlowForResult(it,AppUpdateType.IMMEDIATE,activity,REQUEST_CODE_UPDATE);
                        } catch (Exception e) {
                            Log.e(TAG,e.toString());
                            e.printStackTrace();
                        }
                    }
                } else {
                    // 监听失败
                    Log.e(TAG,task.toString());
                }
            }
        });
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_UPDATE) {
            if (resultCode == Activity.RESULT_OK) {
                Log.w(TAG, "应用内更新成功");
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.w(TAG, "应用内更新, 用户取消");
            } else {
                Log.w(TAG, "应用内更新，遇到错误");
            }
        }
    }
}
