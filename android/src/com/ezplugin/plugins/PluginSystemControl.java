package com.ezplugin.plugins;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.OrientationEventListener;

import com.ezplugin.core.PluginBase;
import com.ezplugin.utils.ShakeListener;

import org.json.JSONObject;

public class PluginSystemControl extends PluginBase {
    final public static String TAG = "PluginSystemControl";
    public boolean isOpenVibrate = true;
    public void initPlugin(Context context, JSONObject jobj) {
        if (this.isInited){
            return;
        }
        super.initPlugin(context, jobj);

        // 音量变化监听
        registerVolumeChangeReceiver();
        // 手机晃动监听
        setSharkListener();
        // 设备翻转检测
        startOrientationChangeListener();
        // 充电检测
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        context.registerReceiver(new PowerConnectionReceiver(), ifilter);
        // 手机屏幕亮度检测
        screenLightChange();
    }

    @Override
    public void excute(String type, String params, int callback) {
        switch(type){
            case "toggleVibrate": {
                isOpenVibrate = params.equals("1");
                break;
            }
        }
    }

    // 监听音量变化
    SettingsContentObserver mSettingsContentObserver;
    private void registerVolumeChangeReceiver() {
        mSettingsContentObserver = new SettingsContentObserver(getContext(), new Handler());
        getContext().getContentResolver().registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, mSettingsContentObserver);
    }

    private void unregisterVolumeChangeReceiver(){
        getContext().getContentResolver().unregisterContentObserver(mSettingsContentObserver);
    }

    public class SettingsContentObserver extends ContentObserver {
        Context context;

        public SettingsContentObserver(Context c, Handler handler) {
            super(handler);
            context = c;
        }

        @Override
        public boolean deliverSelfNotifications() {
            return super.deliverSelfNotifications();
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            nativeEventHandler("volume_change", currentVolume + "");
            Log.i("volume_change", "value = " +  currentVolume);
            //TODO
        }
    }

    // 监听手机晃动
    public void setSharkListener(){
        ShakeListener shakeListener = new ShakeListener(getContext());//创建一个对象
        shakeListener.setOnShakeListener(new ShakeListener.OnShakeListener(){//调用setOnShakeListener方法进行监听

            public void onShake() {
                //对手机摇晃后的处理（如换歌曲，换图片，震动……）
                //onVibrator();
                if(isOpenVibrate){
                    Vibrator vib = (Vibrator)getContext().getSystemService(Service.VIBRATOR_SERVICE);
                    vib.vibrate(1000);
                }
                nativeEventHandler("shake",  "");
            }

        });
    }

    // 监听手机旋转
    private OrientationEventListener mOrientationListener;
    private int screenCurOrient = 2; //1表示正竖屏，2表示正横屏，3表示反竖屏，4表示反横屏
    private final void startOrientationChangeListener() {
        mOrientationListener = new OrientationEventListener(getContext()) {
            @Override
            public void onOrientationChanged(int rotation) {
                //判断四个方向
                if (rotation == -1) {
                    Log.d(TAG, "手机平放:" + rotation);
                } else if (rotation < 10 || rotation > 350) {
                    screenOrientChange(1);
                } else if (rotation < 100 && rotation > 80) {
                    screenOrientChange(4);
                } else if (rotation < 190 && rotation > 170) {
                    screenOrientChange(3);
                } else if (rotation < 280 && rotation > 260) {
                    screenOrientChange(2);
                }
                else
                {
                }
            }
        };
        mOrientationListener.enable();
    }
    private void screenOrientChange(int Orient) {
        if(Orient != screenCurOrient)
        {
            screenCurOrient = Orient;
            nativeEventHandler("screen_change",  "" + Orient);
        }
    }

    // 监听手机充电状态
    public class PowerConnectionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;
            nativeEventHandler("power_charge",  isCharging ? "1" : "0");
        }
    }

    // 监听手机屏幕亮度变化
    public void screenLightChange(){
        ContentObserver mBrightnessObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                int nowBrightnessValue = 0;
                ContentResolver resolver = getContext().getContentResolver();
                try {
                    nowBrightnessValue = Settings.System.getInt(
                            resolver, Settings.System.SCREEN_BRIGHTNESS);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.i("screen_light_change", nowBrightnessValue + "");
                nativeEventHandler("screen_light_change",  nowBrightnessValue + "");
            }
        };

        getContext().getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), true,
                mBrightnessObserver);
    }

}
