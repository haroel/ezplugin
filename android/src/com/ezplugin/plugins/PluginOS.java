/**
 * Created by howe on 2017/10/16.
 * 如需调用OS系统级方法，请全部在此增加修改！
 */

package com.ezplugin.plugins;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.Toast;

import com.ezplugin.apkupdate.ApkDownLoadService;
import com.ezplugin.core.PluginBase;
import com.ezplugin.utils.FileUtils;
import com.ezplugin.utils.NetAssistUtil;
import com.ezplugin.utils.NetChangeReceiver;
import com.ezplugin.utils.ShakeListener;
import com.ezplugin.utils.SysUtils;
import com.sixgamers.brainstorm.R;

import org.cocos2dx.lib.Cocos2dxHelper;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import android.util.Base64;

/**
 * 正常使用需要以下权限
 * 电话 需要权限<uses-permission android:name="android.permission.CALL_PHONE"/>
 * 震动 需要权限<uses-permission android:name="android.permission.VIBRATE" />
 *
 * 主流应用商店对应的包名如下：
 * Google Play com.android.vending
 *
 * 应用宝 com.tencent.android.qqdownloader
 *
 * 360手机助手 com.qihoo.appstore
 *
 * 百度手机助 com.baidu.appsearch

 * 小米应用商店 com.xiaomi.market
 *
 * 豌豆荚 com.wandoujia.phoenix2
 *
 * 华为应用市场 com.huawei.appmarket
 *
 * 淘宝手机助手 com.taobao.appcenter
 *
 * 安卓市场 com.hiapk.marketpho
 *
 * 安智市场 cn.goapk.market
 *
 * 链接：https://juejin.im/post/58b7e31e8d6d8100652932a1
 */

public class PluginOS extends PluginBase {
  final public static String TAG = "PluginOS";
  public boolean isOpenVibrate = true;
  @Override
  public void initPlugin(Context context, JSONObject jobj) {
    if (this.isInited){
      return;
    }
    super.initPlugin(context, jobj);
    NetChangeReceiver receiver = new NetChangeReceiver ();
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
    context.registerReceiver(receiver, intentFilter);

    registerVolumeChangeReceiver();
    setSharkListener();
    startOrientationChangeListener();

    // 监听手机充电状态
    IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    context.registerReceiver(new PowerConnectionReceiver(), ifilter);

    // 监听手机屏幕亮度变化
    screenLightChange();
//        this.updateApk("https://qd.myapp.com/myapp/qqteam/AndroidQQ/mobileqq_android.apk");
  }

  @Override
  public void excute(String type, String params, int callback) {

    switch (type) {
      case "getVolume":{
        AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        nativeCallbackHandler(callback, currentVolume + "");
        break;
      }
      case "networkType":{
        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkINfo = cm.getActiveNetworkInfo();
        String netType = "mobile";
        if(networkINfo != null){
          switch(networkINfo.getType()){
            case ConnectivityManager.TYPE_WIFI:{
              netType = "wifi";
              break;
            }
            case ConnectivityManager.TYPE_MOBILE:{
              netType = "mobile";
              break;
            }
          }
        }
        nativeCallbackHandler(callback, netType);
        break;
      }
      case "phoneCall": {
        this.phoneCall(params);
        this.nativeCallbackHandler(callback, "1");
        break;
      }
      case "nslog": {
        Log.d(TAG +"CCLOG",params);
        this.nativeCallbackHandler(callback, "1");
        break;
      }
      case "vibrate": {
        try {
          Vibrator vib = (Vibrator)getContext().getSystemService(Service.VIBRATOR_SERVICE);
          vib.vibrate(1000);
          nativeCallbackHandler(callback, "1");
        } catch (NumberFormatException e) {
          e.printStackTrace();
          nativeCallbackErrorHandler(callback, "2");
        }
        break;
      }
      case "toggleVibrate": {
          isOpenVibrate = params.equals("1");
        break;
      }
      case "network":{
        boolean isConnected = NetAssistUtil.isNetWorkConnected(getContext());
        nativeCallbackHandler(callback, isConnected ? "1":"0");
        break;
      }
      case "clipboard": {
        this.copyToClipboard(params);
        nativeCallbackHandler(callback, "1");
        break;
      }
      case "cutImage": {
        nativeCallbackHandler(callback, "0");
        break;
      }
      case "restart": {
        this.appRestart();
        nativeCallbackHandler(callback, "1");
        break;
      }
      case "appStore":{
        String pkName = getContext().getPackageName();
        Log.d(TAG,"打开应用商店！");
        // google play 商店是
        this.navigateToMarket(pkName,params );
        nativeCallbackHandler(callback, "1");
        break;
      }
      case "update": {
        // apk下载更新
        this.updateApk(params);
        nativeCallbackHandler(callback, "1");
        break;
      }
      case "copyDir": {
        String[] arr = params.split("\\|");
        this.copyToDir(arr[0], arr[1]);
        nativeCallbackHandler(callback, "1");
        break;
      }
      case "browser": {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        Uri content_url = Uri.parse(params);
        intent.setData(content_url);
        getContext().startActivity(intent);
        nativeCallbackHandler(callback, "1");
        break;
      }
      case "channel": {
        try {
          ApplicationInfo appInfo = getContext().getPackageManager().getApplicationInfo(getContext().getPackageName(), PackageManager.GET_META_DATA);
          String channelName = appInfo.metaData.getString("UMENG_CHANNEL");
          nativeCallbackHandler(callback, channelName);
        } catch (PackageManager.NameNotFoundException e) {
          e.printStackTrace();
          nativeCallbackHandler(callback, "-2");
        }
        break;
      }
      case "capture":{
        String filePath = params;
        Log.e(TAG,"capture" + params);
        View view  = ((Activity )getContext()).getWindow().getDecorView();
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        try{
          File f = new File(filePath);
          FileUtils.createFileDirectorys(f.getParent());
          FileUtils.deleteFile(f);
          FileOutputStream fos = new FileOutputStream(filePath);
          //压缩bitmap到输出流中
          bitmap.compress(Bitmap.CompressFormat.JPEG, 70, fos);
          fos.close();
          Toast.makeText(getContext(), "截屏成功", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
          Log.e(TAG, e.getMessage());
        }finally {
          if(bitmap!=null) {
            bitmap.recycle();
          }
        }

        break;
      }
      case "base64ToImage":{
        String[] arr = params.split("\\|");
        String filepath = Cocos2dxHelper.getWritablePath() + "/" +arr[0];
        this.base64Image(arr[1],filepath);
        nativeCallbackHandler(callback, filepath);
        break;
      }
      case "whatsapp":{
        String[] arr = params.split("\\|");
        String phoneNum = arr[0];
        String text = arr[1];
        Intent intent = new Intent();
        intent.setData(Uri.parse("https://api.whatsapp.com/send?phone="+phoneNum + "&text=" + text));
        intent.setAction(Intent.ACTION_VIEW);
        getContext().startActivity(intent); //启动浏览器
        break;
      }
      case "rate":{
        //app 评分
        JSONObject obj = null;
        try {
          obj = new JSONObject(params);
          rateApp(obj);
        } catch (JSONException e) {
          e.printStackTrace();
        }

        break;
      }
      case "email":{
        emailTo(params);
        break;
      }
      case "get_screen_light":{
        int nowBrightnessValue = 0;
        ContentResolver resolver = getContext().getContentResolver();
        try {
          nowBrightnessValue = Settings.System.getInt(
                  resolver, Settings.System.SCREEN_BRIGHTNESS);
        } catch (Exception e) {
          e.printStackTrace();
        }
        this.nativeCallbackHandler(callback, "" + nowBrightnessValue);
        break;
      }
      default: {
        nativeCallbackErrorHandler(callback, "-1");
        break;
      }
    }
  }
  /**
   * 启动到应用商店app详情界面
   *
   * @param appPkg    目标App的包名
   * @param marketPkg 应用商店包名 if null 则由系统弹出应用商店列表供用户选择,否则调转到目标市场的应用详情界面，某些应用商店可能会失败
   */
  public void navigateToMarket( String appPkg, String marketPkg) {
    try {
      Uri uri = Uri.parse("market://details?id=" + appPkg);
      Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.setData(uri);
      if (!marketPkg.isEmpty() ) {
        intent.setPackage(marketPkg);
      }
      this.getContext().startActivity(intent);
    } catch (ActivityNotFoundException e) {
      Log.e(TAG, "navigateToMarket: no market app installed", e);
    }
  }

  private void appRestart(){
    Context basecontext = getApplication().getBaseContext();
    Intent intent = basecontext.getPackageManager().getLaunchIntentForPackage( basecontext.getPackageName());
    PendingIntent restartIntent = PendingIntent.getActivity(getContext(), 0, intent, PendingIntent.FLAG_ONE_SHOT);
    AlarmManager mgr = (AlarmManager)getContext().getSystemService(Context.ALARM_SERVICE);
    mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 10, restartIntent);
    android.os.Process.killProcess(android.os.Process.myPid());
  }

  private void copyToClipboard(String text){
    //获取剪贴板管理器：
    ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
    ClipData mClipData = ClipData.newPlainText("Label", text);
    cm.setPrimaryClip(mClipData);
  }

  private void phoneCall(final String phoneNumber){
    // 拨打电话
    final PluginOS plugin = this;
    new AlertDialog.Builder(plugin.getContext()).setTitle("提示")
            .setMessage(phoneNumber)
            .setPositiveButton("拨打", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                try{
//                            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"+phoneNumber));
//                            plugin.getContext().startActivity(intent);
                }catch (SecurityException e){
                  e.printStackTrace();
                  plugin.copyToClipboard(phoneNumber);
                  Toast toast = Toast.makeText(plugin.getContext(),"由于没有拨号权限，电话号码已拷贝到剪切板。", Toast.LENGTH_SHORT);
                  toast.setGravity(Gravity.CENTER, 0, 0);
                  toast.show();
                }
              }
            })
            .setNegativeButton("取消", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                // 点击“返回”后的操作,这里不设置没有任何操作
              }
            }).show();
  }
  private void updateApk(String url){
    Log.d("PluginOS","updateApk:"+url);
    // 启动下载服务
    Intent intent = new Intent(this.getContext(), ApkDownLoadService.class);
    intent.putExtra(ApkDownLoadService.INTENT_APK_URL,url);
    this.getContext().startService(intent);
  }

  private void copyToDir(String dir,String dir2){
    PluginLog.d("PluginOS "+ dir +"拷贝至" + dir2);
    if (dir.equals(dir2)){
      PluginLog.e("PluginOS:copyToDir dir path is equal!");
      return;
    }
    File dir2File = new File(dir2);
    if ( !dir2File.exists() ){
      dir2File.mkdirs();
    }
    ArrayList<File> files = new ArrayList<File>();
    this.subpathsAtPath(dir,files);
    for (File file : files) {
      if (file.isDirectory()){
        continue;
      }
      String relativePath = file.getAbsolutePath().replace( dir, "" );
      File destFile = new File(dir2,relativePath);
      if (destFile.exists()){
        destFile.delete();
        destFile = new File(dir2,relativePath);
      }
      File ddDirFile= destFile.getParentFile();
      if (!ddDirFile.exists()){
        ddDirFile.mkdirs();
      }
      PluginLog.d("PluginOS"+ file.getAbsolutePath() +"拷贝至" + destFile.getAbsoluteFile());
      file.renameTo(destFile.getAbsoluteFile());
    }
    File directory = new File(dir);
    directory.delete();
    PluginLog.d("PluginOS:一共移动"+files.size()+"个文件");

  }
  public void subpathsAtPath(String directoryName, ArrayList<File> files) {
    File directory = new File(directoryName);
    if (!directory.exists()){
      Log.d("PluginOS",directoryName + "不存在");
      return;
    }
    File[] fList = directory.listFiles();
    if (fList == null){
      Log.d("PluginOS",directoryName + "is null");
      return;
    }
    for (File file : fList) {
      if (file.isFile()) {
        files.add(file);
      } else if (file.isDirectory()) {
        subpathsAtPath(file.getAbsolutePath(), files);
      }
    }
  }
  public void base64Image(String base64str,String filePath){
    Bitmap bitmap = null;
    try {
      byte[] bitmapArray = Base64.decode(base64str, Base64.DEFAULT);
      bitmap = BitmapFactory.decodeByteArray(bitmapArray, 0, bitmapArray.length);
    } catch (Exception e) {
      e.printStackTrace();
    }
    if (bitmap == null){
      return;
    }
    Matrix matrix = new Matrix();
    matrix.setScale(0.5f, 0.5f);
    bitmap = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
    File f = new File(filePath);
    if (f.exists()) {
      f.delete();
    }
    try {
      FileOutputStream out = new FileOutputStream(f);
      bitmap.compress(Bitmap.CompressFormat.JPEG, 75, out);
      out.flush();
      out.close();
      Log.i(TAG, "已经保存" + filePath);
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private void rateApp(JSONObject obj){
    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
    try {
      builder.setTitle(obj.getString("title"));// 设置标题
      builder.setIcon(R.mipmap.ic_launcher);//设置图标
      builder.setMessage(obj.getString("content"));// 为对话框设置内容
      // 为对话框设置取消按钮
      builder.setNegativeButton(obj.getString("cancel"), new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface arg0, int arg1) {
          // TODO Auto-generated method stub
          Log.d(TAG,"取消Rate");
        }
      });
      // 为对话框设置中立按钮
      // builder.setNeutralButton("中立", new OnClickListener() {
      //
      // @Override
      // public void onClick(DialogInterface arg0, int arg1) {
      // // TODO Auto-generated method stub
      // myToast("您点击了中立按钮");
      // }
      // });
      // 为对话框设置确定按钮
      builder.setPositiveButton(obj.getString("ok"), new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface arg0, int arg1) {
          String pkName = getContext().getPackageName();
          try {
            Uri uri = Uri.parse("market://details?id="+pkName+"");
            Intent goMarket = new Intent(Intent.ACTION_VIEW, uri);
            getContext().startActivity(goMarket);
          }catch (ActivityNotFoundException e){
            Uri uri = Uri.parse("https://play.google.com/store/apps/details?id="+pkName+"");
            Intent goMarket = new Intent(Intent.ACTION_VIEW, uri);
            getContext().startActivity(goMarket);
          }
        }
      });
      builder.create().show();// 使用show()方法显示对话框
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }
  private  void emailTo(String params){
    try{
        JSONObject jobj = new JSONObject(params);
        String email = jobj.getString("email");
        String pkName = getContext().getPackageName();

        Intent intent=new Intent(Intent.ACTION_SENDTO);
//      intent.setType("message/rfc822");
        intent.setData( Uri.parse("mailto:" + email) );
//      intent.setDataAndType(Uri.parse("mailto:" + email),"text/html");
        intent.putExtra(Intent.EXTRA_SUBJECT,jobj.getString("title"));
        intent.putExtra(Intent.EXTRA_TEXT,
                "uid: "+ jobj.getString("userId") +"\n" +
                " device: "+ jobj.getString("device") +"\n" +
                " osVersion: "+ jobj.getString("osVersion") +"\n" +
                " network: "+ jobj.getInt("network") +"\n" +
                " language: "+ jobj.getString("language") +"\n" +
                " PackageName: "+ pkName +"\n" +
                " Version: "+ SysUtils.getVersionName(getContext()) +"\n" +
                " VersionCode: "+ SysUtils.getAppVersionCode(getContext()) +"\n" );
        getContext().startActivity(intent);

    }catch (Exception e){
      e.printStackTrace();
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
