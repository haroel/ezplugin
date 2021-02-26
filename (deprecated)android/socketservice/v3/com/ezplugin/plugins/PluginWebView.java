package com.ezplugin.plugins;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.ezplugin.core.PluginBase;
import com.ezplugin.utils.FileUtils;
import com.ezplugin.utils.OkHttpUtils;
import com.ezplugin.utils.ZipUtils;

import org.cocos2dx.lib.Cocos2dxWebView;
import org.cocos2dx.lib.ImageWebChromeClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;

public class PluginWebView extends PluginBase {

    final public static String EVENT_GAME_DOWNLOAD_START = "EVENT_GAME_DOWNLOAD_START";
    final public static String EVENT_GAME_DOWNLOAD_PROGRESS = "EVENT_GAME_DOWNLOAD_PROGRESS";
    final public static String EVENT_GAME_DOWNLOAD_COMPLETE = "EVENT_GAME_DOWNLOAD_COMPLETE";
    final public static String EVENT_GAME_DOWNLOAD_ERROR = "EVENT_GAME_DOWNLOAD_ERROR";

    final private static String TAG = "PluginWebView";

    private HashMap<String,String>  downloadHashMap= new HashMap<String,String>();

    @Override
    public void initPlugin(Context context, JSONObject jobj) {
        super.initPlugin(context, jobj);
        msgHandler.plugin = this;
    }
    public void onDestroy(){
        msgHandler.plugin = null;

    }
    @Override
    public  void onActivityResult(int requestCode, int resultCode, Intent data){
        if (ImageWebChromeClient.instance != null){
            ImageWebChromeClient.instance.handlerOnActivityResult(requestCode,resultCode,data);
        }
    }
    @Override
    public void excute(String action, String params, int callback) {
        super.excute(action, params, callback);

        switch (action){
            case "init":{
                String[] arr = params.split("\\|");
                Log.d(TAG,"init" + params);
//                Cocos2dxWebView.filterPrefixUrl =arr[0];
//                Cocos2dxWebView.webviewCachePath = arr[1];
//                File ff = new File(arr[1]);
//                if (!ff.exists()){
//                    ff.mkdirs();
//                }
                break;
            }
            case "updateGame":{
                String[] arr = params.split("\\|");
//                String downloadUrl  = arr[0];
//                String gameID = arr[1];
//                if (downloadHashMap.get(gameID) != null){
//                    Log.d(TAG,"正在下载" + gameID);
//                }else{
//                    this.updateGame(downloadUrl,gameID);
//                }
                break;
            }
        }
        this.nativeCallbackHandler(callback,"");
    }

    private static class MyHandler extends Handler {
        public PluginWebView plugin = null;
        @Override
        public void handleMessage(Message msg) {
            if (plugin == null ){
                return;
            }
            JSONObject jj = (JSONObject)(msg.obj);
            switch (msg.what){
                case 2:{
                    plugin.nativeEventHandler(EVENT_GAME_DOWNLOAD_COMPLETE,jj.toString());
                    break;
                }
                case 1:{
                    plugin.nativeEventHandler(EVENT_GAME_DOWNLOAD_PROGRESS,jj.toString());
                    break;
                }
                case 0:{
                    plugin.nativeEventHandler(EVENT_GAME_DOWNLOAD_START,jj.toString());
                    break;
                }
                case -1:{
                    plugin.nativeEventHandler(EVENT_GAME_DOWNLOAD_ERROR,jj.toString());
                    break;
                }

            }
        }
    }

    private final MyHandler msgHandler = new MyHandler();

    private void sendMesssage( int evtType,JSONObject jobj){
        Message msg = msgHandler.obtainMessage();
        msg.obj = jobj;
        msg.what = evtType;
        msgHandler.sendMessage(msg);
    }

//    private void updateGame(String downloadUrl, final String gameID){
//        String savePath = String.format("%s/temp_%s.zip",Cocos2dxWebView.webviewCachePath,gameID);
//        FileUtils.deleteFile(new File(savePath));
//        OkHttpUtils.OnDownloadListener listener = new OkHttpUtils.OnDownloadListener() {
//            @Override
//            public void onDownloadSuccess(String url, String savePath) {
//                Log.e(TAG,"下载成功 savePath = " + savePath );
//                downloadHashMap.remove(gameID);
//                String unzipPath = String.format("%s/%s",Cocos2dxWebView.webviewCachePath,gameID);
//                FileUtils.deleteFile(new File(unzipPath));
//                try{
//                    ZipUtils.UnZipFolder(savePath,unzipPath);
//                    Log.d(TAG,"文件解压"+unzipPath);
//                    FileUtils.deleteFile(new File(savePath));
//                }catch (Exception e){
//                    e.printStackTrace();
//                    FileUtils.deleteFile(new File(savePath));
//                }
//                try{
//                    JSONObject jobj = new JSONObject();
//                    jobj.put("gameID",gameID);
//                    jobj.put("state",2);
//                    sendMesssage(2,jobj);
////                    plugin.nativeEventHandler(EVENT_GAME_DOWNLOAD_COMPLETE,jobj.toString());
//                }catch (JSONException e){
//                    e.printStackTrace();
//                }
//            }
//            @Override
//            public void onDownloading(int progress) {
////                Log.d(TAG,"下载进度 "+progress);
//                try{
//                    JSONObject jobj = new JSONObject();
//                    jobj.put("gameID",gameID);
//                    jobj.put("state",1);
//                    jobj.put("progress",1.0*progress/100);
//                    sendMesssage(1,jobj);
////                    plugin.nativeEventHandler(EVENT_GAME_DOWNLOAD_PROGRESS,jobj.toString());
//                }catch (JSONException e){
//                    e.printStackTrace();
//                }
//            }
//            @Override
//            public void onDownloadFailed(String url, String savePath) {
//                downloadHashMap.remove(gameID);
//                Log.e(TAG,"下载失败 url = " + url );
//                try{
//                    JSONObject jobj = new JSONObject();
//                    jobj.put("gameID",gameID);
//                    jobj.put("state",-1);
//                    sendMesssage(-1,jobj);
////                    plugin.nativeEventHandler(EVENT_GAME_DOWNLOAD_ERROR,jobj.toString());
//                }catch (JSONException e){
//                    e.printStackTrace();
//                }
//            }
//        };
//        try{
//            JSONObject jobj = new JSONObject();
//            jobj.put("gameID",gameID);
//            jobj.put("state",0);
//            sendMesssage(0,jobj);
////            this.nativeEventHandler(EVENT_GAME_DOWNLOAD_START,jobj.toString());
//        }catch (JSONException e){
//            e.printStackTrace();
//        }
//        OkHttpUtils.getInstance().download(downloadUrl,savePath,2,listener);
//        downloadHashMap.put(gameID,downloadUrl);
//    }

}
