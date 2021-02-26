package com.ezplugin.plugins;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.ezplugin.core.PluginBase;
import com.ezplugin.utils.FileUtils;
import com.ezplugin.utils.OkHttpUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class PluginFileDownload extends PluginBase {
    final private static String TAG = "PluginFileDownload";

    private final MyHandler msgHandler = new MyHandler();
    final public static String EVENT_GAME_DOWNLOAD_START = "EVENT_GAME_DOWNLOAD_START";
    final public static String EVENT_GAME_DOWNLOAD_PROGRESS = "EVENT_GAME_DOWNLOAD_PROGRESS";
    final public static String EVENT_GAME_DOWNLOAD_COMPLETE = "EVENT_GAME_DOWNLOAD_COMPLETE";
    final public static String EVENT_GAME_DOWNLOAD_ERROR = "EVENT_GAME_DOWNLOAD_ERROR";

    @Override
    public void initPlugin(Context context, JSONObject jobj) {
        super.initPlugin(context, jobj);
        msgHandler.plugin = this;
    }
    public void onDestroy(){
        msgHandler.plugin = null;
    }
    @Override
    public void excute(String action, String params, int callback) {
        super.excute(action, params, callback);
        switch(action){
            case "download":{
                try {
                    JSONObject data = new JSONObject(params);
                    String downloadUrl = data.getString("requestURL");
                    String storagePath = data.getString("storagePath");
                    this.updateGame(downloadUrl,storagePath,callback);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    private void updateGame(final String downloadUrl, final String storagePath, final int callback){
        File sf = new File(storagePath);
        if ( sf.exists() && sf.isFile() ){
            if (sf.length() > 0){
                this.nativeCallbackHandler(callback,storagePath);
                return;
            }
        }
        final int cbid = callback;
        String savePath = storagePath + ".temp";
        FileUtils.deleteFile(new File(savePath));
//        final PluginFileDownload self = this;
        OkHttpUtils.OnDownloadListener listener = new OkHttpUtils.OnDownloadListener() {
            @Override
            public void onDownloadSuccess(String url, String savePath) {
                try{
                    JSONObject jobj = new JSONObject();
                    jobj.put("callback",cbid);
                    jobj.put("storagePath",storagePath);
                    Log.d(TAG,"下载成功 savePath = " + savePath );
                    FileUtils.deleteFile(new File(storagePath));
                    File tempFile = new File(savePath);
                    if (tempFile.renameTo(new File(storagePath))){
                        sendMesssage(0,jobj);
                    }else{
                        sendMesssage(1,jobj);
                        Log.e(TAG,"文件移动错误" + savePath);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            @Override
            public void onDownloading(int progress) {
            }
            @Override
            public void onDownloadFailed(String url, String savePath) {
                Log.e(TAG,"下载失败 url = " + url );
                try{
                    JSONObject jobj = new JSONObject();
                    jobj.put("callback",cbid);
                    jobj.put("storagePath",storagePath);
                    sendMesssage(2,jobj);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        };
        OkHttpUtils.getInstance().download(downloadUrl,savePath,0,listener);
    }

    private static class MyHandler extends Handler {
        public PluginFileDownload plugin = null;
        @Override
        public void handleMessage(Message msg) {
            if (plugin == null ){
                return;
            }
            try{
                JSONObject jj = (JSONObject)(msg.obj);
                switch (msg.what){
                    case 0:{
                        plugin.nativeCallbackHandler(jj.getInt("callback"),jj.getString("storagePath"));
                        break;
                    }
                    default:{
                        plugin.nativeCallbackErrorHandler(jj.getInt("callback"),jj.getString("storagePath"));
                        break;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendMesssage( int evtType,JSONObject jobj){
        Message msg = msgHandler.obtainMessage();
        msg.obj = jobj;
        msg.what = evtType;
        msgHandler.sendMessage(msg);
    }
}
