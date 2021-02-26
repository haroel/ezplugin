package com.ezplugin.services;

import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
//import android.support.v4.content.FileProvider;
import android.util.Log;
//import android.support.v4.content.LocalBroadcastManager;

import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.ezplugin.plugins.PluginLog;
import com.sixgamers.brainstorm.R;

import java.io.File;
/**
 * Created by howe on 2017/11/2.
 *
 * apk下载自安装 服务
 */

public class ApkDownLoadService extends Service {

    public static final String INTENT_DOWNLOAD = "INTENT_DOWNLOAD";

    public static final String INTENT_ACTION_CANCEL = "com.apkdownload.cancel";
    public static final String INTENT_ACTION_DOWNLOAD= "com.apkdownload.RECEIVER";

    public static final String INTENT_PROGRESS = "INTENT_PROGRESS";

    public static final String INTENT_APK_URL = "INTENT_APK_URL";

    private static final String TAG = "ApkDownLoadService";

    /**广播接受者*/
    private BroadcastReceiver receiver;

    /**系统下载器分配的唯一下载任务id，可以通过这个id查询或者处理下载任务*/
    private long enqueue = 0;

    private Intent intent = new Intent(INTENT_ACTION_DOWNLOAD);

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    private String getAppInfo() {
        try {
            String pkName = this.getPackageName();
            return pkName ;
        } catch (Exception e) {
        }
        return "_temp";
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String downloadUrl;
        if (intent != null && intent.hasExtra(ApkDownLoadService.INTENT_APK_URL)){
            downloadUrl = intent.getStringExtra(ApkDownLoadService.INTENT_APK_URL);
        }else{
            Log.e(TAG,"service启动错误，需要传入下载链接");
            return Service.START_NOT_STICKY;
        }
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                DownloadManager manager = (DownloadManager)context.getSystemService(Context.DOWNLOAD_SERVICE);
                String act = intent.getAction();
                PluginLog.d("收到广播"+act);
                if (act.equals(ApkDownLoadService.INTENT_ACTION_CANCEL)){
                    manager.remove(enqueue);
                    PluginLog.d(TAG+"下载取消"+enqueue);
                }else if ( act.equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
                {
                    long completeDownloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                    PluginLog.d(TAG+"下载完成completeDownloadId"+completeDownloadId);
                    if (enqueue != completeDownloadId){
                        return;
                    }
                    if(DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                        DownloadManager.Query query = new DownloadManager.Query();
                        //在广播中取出下载任务的id
                        long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                        query.setFilterById(id);
                        Cursor c = manager.query(query);
                        if(c.moveToFirst()) {
                            installApk(context,c);
                            c.close();
                        }
                    }
                }
                //销毁当前的Service
                stopSelf();
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        intentFilter.addAction(ApkDownLoadService.INTENT_ACTION_CANCEL);
        registerReceiver(receiver, intentFilter);

        startDownload(downloadUrl);
        return Service.START_NOT_STICKY;
    }

    private void startDownload(String downUrl) {

        // check if support download manager
        if (!isDownloadManagerAvailable( this )) {
            PluginLog.d("下载管理器已关闭，调用浏览器下载功能");
            Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            Uri content_url = Uri.parse(downUrl);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(content_url);
            this.startActivity(intent);
            return;
        }
        String fileName = this.getAppInfo()+".apk";
        Uri desUri = Uri.withAppendedPath(Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)), fileName);
        File f = new File(desUri.getPath());
        if (f.exists()){
            f.delete();
        }
        final DownloadManager manager = (DownloadManager)this.getSystemService(Context.DOWNLOAD_SERVICE);
        //获得系统下载器
        //设置下载地址
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downUrl));
        //设置下载文件的类型
        request.setMimeType("application/vnd.android.package-archive");
        //设置下载存放的文件夹和文件名字
//        request.setDestinationInExternalPublicDir( Environment.DIRECTORY_DOWNLOADS , this.getAppInfo()+".apk" );
        request.setDestinationUri(desUri);
        //设置下载时或者下载完成时，通知栏是否显示
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setTitle("downloading "+this.getResources().getString(R.string.app_name));
        //执行下载，并返回任务唯一id
        enqueue = manager.enqueue(request);
        PluginLog.d(TAG+"开始下载apk");

        final Context context = this;
        intent.putExtra(INTENT_DOWNLOAD,0);

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        new Thread(new Runnable() {
            @Override
            public void run() {

                boolean downloading = true;
                while (downloading) {
                    SystemClock.sleep(500);
                    DownloadManager.Query q = new DownloadManager.Query();
                    q.setFilterById(enqueue);
                    Cursor cursor = manager.query(q);
                    if (cursor.moveToFirst()){
                        int bytes_downloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        final int dl_progress = (int) ((bytes_downloaded * 100) / bytes_total);
                        int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
//                        Log.d(TAG,"status:" +status +" dl_progress:" +dl_progress);
                        switch (status) {
                            //下载暂停
                            case DownloadManager.STATUS_PAUSED:
                            case DownloadManager.STATUS_RUNNING:
                            case DownloadManager.STATUS_PENDING:
                            {
                                intent.putExtra(INTENT_DOWNLOAD,1);
                                intent.putExtra(INTENT_PROGRESS,dl_progress);
                                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                                break;
                            }
                            //下载完成
                            case DownloadManager.STATUS_SUCCESSFUL:
                                PluginLog.d(TAG+"DownloadManager.STATUS_SUCCESSFUL");
                                downloading = false;
                                intent.putExtra(INTENT_DOWNLOAD,2);
                                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                                break;
                            //下载失败
                            case DownloadManager.STATUS_FAILED:
                                PluginLog.d(TAG+"DownloadManager.STATUS_FAILED");
                                downloading = false;
                                intent.putExtra(INTENT_DOWNLOAD,2);
                                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                                break;
                        }
                    }
                    cursor.close();
                }

            }
        }).start();
    }

    private static boolean installApk(Context context,Cursor cursor ){

        PluginLog.d("执行安装");
        //执行安装
        Intent intent_ins = new Intent(Intent.ACTION_VIEW);
        intent_ins.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        String filename = "";
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            // Android 7.0 安装方式
            int fileUriIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
            String fileUri = cursor.getString(fileUriIdx);
            if (fileUri != null) {
                filename = Uri.parse(fileUri).getPath();
            }
            PluginLog.d(TAG+"下载完成的文件路径："+filename);
            File apkFile = new File(filename);
            Uri apkUri = FileProvider.getUriForFile(context, "com.game.apkinstall.fileprovider", apkFile);
            //添加这一句表示对目标应用临时授权该Uri所代表的文件
            intent_ins.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent_ins.setDataAndType(apkUri, "application/vnd.android.package-archive");
        } else {
            //Android 7.0以下
            int fileNameIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME);
            filename = cursor.getString(fileNameIdx);
            PluginLog.d(TAG+"下载完成的文件路径："+filename);
            intent_ins.setDataAndType(Uri.parse("file://" + filename),"application/vnd.android.package-archive");
        }
        context.getApplicationContext().startActivity(intent_ins);
        return false;
    }
    public static boolean isDownloadManagerAvailable(Context context) {
        try {
            PackageManager pkm = context.getPackageManager();
            if ( pkm.getApplicationEnabledSetting( "com.android.providers.downloads") == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                    || pkm.getApplicationEnabledSetting( "com.android.providers.downloads") == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    || pkm.getApplicationEnabledSetting( "com.android.providers.downloads") == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED ) {

                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    @Override
    public void onDestroy() {
        //服务销毁的时候 反注册广播
        unregisterReceiver(receiver);
        super.onDestroy();
    }
}

