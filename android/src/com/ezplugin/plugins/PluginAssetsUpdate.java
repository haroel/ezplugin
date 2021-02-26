package com.ezplugin.plugins;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.ezplugin.core.PluginBase;
import com.ezplugin.utils.FileUtils;
import com.ezplugin.utils.OkHttpUtils;
import com.ezplugin.utils.SysUtils;

import org.cocos2dx.lib.Cocos2dxHelper;
import org.cocos2dx.okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


import static android.content.Context.MODE_PRIVATE;

public class PluginAssetsUpdate extends PluginBase {
  final public static String TAG = "PluginAssetsUpdate";


  final private static  String MD5FILE_NAME = "md5fileList.txt";


  final private static String EVENT_PATCH_START = "EVENT_PATCH_START";
  final private static String EVENT_PATCH_PROGRESS = "EVENT_PATCH_PROGRESS";
  final private static String EVENT_PATCH_FAILED = "EVENT_PATCH_FAILED";
  final private static String EVENT_PATCH_FINISHED = "EVENT_PATCH_FINISHED";
  final private static String EVENT_PATCH_NONEED = "EVENT_PATCH_NONEED";

  //资源服务器地址
  private String res_patch_url = "";

  // 下载的所有文件先放在缓存目录
  private String tempCacheDir = "";

  private String assetsPatchDir = "";

  private String versionName = "";

  // 需要下载的文件列表
  private ArrayList<String> needDownloadResHashMap = new ArrayList<String>();
//
//  // 需要删除的本地文件列表 needDeleteResHashMap
//  private ArrayList<String> needDeleteResHashMap = new ArrayList<String>();

  public int downloadIndex = 0;

  @Override
  public void initPlugin(Context context, JSONObject jobj) {
    if (this.isInited){
      return;
    }
    super.initPlugin(context, jobj);

    this.tempCacheDir = context.getCacheDir().getPath() + "/assetsUpdateTemp";
    FileUtils.deleteFile(new File(this.tempCacheDir));
    FileUtils.mkdir(this.tempCacheDir);

    this.assetsPatchDir = Cocos2dxHelper.getWritablePath() + "/assetspatch";

    versionName = SysUtils.getVersionName(context);
    long appBuild = SysUtils.getAppVersionCode(context);

    SharedPreferences preferences = context.getSharedPreferences(context.getPackageName()+"_ver",MODE_PRIVATE);
    String lastVersison = preferences.getString("lastversion","");
    long lastBuild = preferences.getLong("lastbuild",0);
    boolean isVersionChange = !lastVersison.equals(versionName);
    boolean isBuildChange = lastBuild != appBuild;
    if ( isVersionChange || isBuildChange ){
      // 将版本号写入version文件
      SharedPreferences.Editor editor = preferences.edit();
      editor.putString("lastversion",versionName);
      editor.putLong("lastbuild",appBuild);
      editor.apply();
      Log.d(TAG,"删除 " + this.assetsPatchDir);
      FileUtils.deleteFile(new File(this.assetsPatchDir));
      FileUtils.mkdir(this.assetsPatchDir);
    }else{
      FileUtils.mkdir(this.assetsPatchDir);
    }
  }

  @Override
  public void excute(String action, String params, int callback) {
    super.excute(action, params, callback);
    try{
      switch (action){
        case "init":{
          JSONObject jobj = new JSONObject(params);
          this.res_patch_url = jobj.getString("res_patch_url");
          this.nativeCallbackHandler(callback,"1");
          break;
        }
        case "assetsPatch":{
          this.downloadMd5file(callback);
          this.nativeCallbackHandler(callback,"1");
          break;
        }
        default:{
          this.nativeCallbackHandler(callback,"0");
        }
      }
    }catch (JSONException e){
      e.printStackTrace();
    }
  }

  private void downloadMd5file(final int callback){
    needDownloadResHashMap.clear();
//    needDeleteResHashMap.clear();
    final PluginAssetsUpdate self = this;
    String localMd5FileListPath = this.assetsPatchDir + "/" + MD5FILE_NAME;

    String localMd5FileListContent = "";
    File localMd5FileListFile = new File(localMd5FileListPath);
    if ( localMd5FileListFile.exists() ){
      localMd5FileListContent = FileUtils.getFileContent(localMd5FileListFile);
    }else{
      // 如果缓存目录没有则从assets读取
      localMd5FileListContent = FileUtils.readAssetsTxt(this.getContext(),MD5FILE_NAME);
//            FileUtils.writeFileContent(localMd5FileListFile,localMd5FileListContent,false);
    }
    final String __local = localMd5FileListContent;
    Log.d(TAG,"localMd5FileListContent ="+localMd5FileListContent);


    OkHttpUtils.OnRequestListener listener = new OkHttpUtils.OnRequestListener() {
      @Override
      public void onDownloadSuccess(String url, Response response) {
        try{
          String remoteMd5FileListContent = response.body().string();
//          Log.d(TAG,"remoteMd5FileListContent ="+remoteMd5FileListContent);
          if (remoteMd5FileListContent.equals(__local)){
            Log.d(TAG,"本地md5和远程md5内容一致，不需要热更新！");
            self.nativeEventHandler(EVENT_PATCH_NONEED,"0");
            return;
          }
          ArrayList<String> needDownloadResHashMap = self.compareLocalAndRemote(__local,remoteMd5FileListContent);
          self.nativeEventHandler(EVENT_PATCH_START, needDownloadResHashMap.size()+"" );
          self.downloadIndex = 0;
          Log.d(TAG,"需要下载的文件 " + needDownloadResHashMap.toString());
          self.downloadFile();
        }catch (IOException e){
          e.printStackTrace();
          self.nativeEventHandler(EVENT_PATCH_FAILED,e.toString());
        }
      }
      @Override
      public void onDownloadFailed(String url) {
        self.nativeEventHandler(EVENT_PATCH_FAILED,"404");
      }
    };
    final String remoteMd5FileListUrl = this.res_patch_url+"/" + MD5FILE_NAME + "?t="+System.currentTimeMillis();
    OkHttpUtils.getInstance().get( remoteMd5FileListUrl, 2,listener );
  }

  private ArrayList<String> compareLocalAndRemote(String localMd5FileListContent,String remoteMd5FileListContent){
    HashMap<String,String> localMd5HashMap = new HashMap<String, String>();

    for (String line:FileUtils.stringToLines(localMd5FileListContent)){
      String[] datas = line.split("\\|");
      if (datas.length > 1 && !datas[0].isEmpty()){
        localMd5HashMap.put(datas[0],datas[1]);
      }
    }

    HashMap<String,String> remoteMd5HashMap = new HashMap<String, String>();
    for (String line:FileUtils.stringToLines(remoteMd5FileListContent)){
      String[] datas = line.split("\\|");
      if (datas.length > 1 && !datas[0].isEmpty()){
        remoteMd5HashMap.put(datas[0],datas[1]);
      }
    }
    needDownloadResHashMap.clear();
//    needDeleteResHashMap.clear();
    for( Map.Entry<String, String> entry: remoteMd5HashMap.entrySet()){
      String remoteMd5Key = entry.getKey();
      if (!localMd5HashMap.containsKey(remoteMd5Key)){
        needDownloadResHashMap.add(entry.getValue());
      }
    }
//    for( Map.Entry<String, String> entry: localMd5HashMap.entrySet()){
//      String localMd5Key = entry.getKey();
//      if (!remoteMd5HashMap.containsKey(localMd5Key)){
//        needDeleteResHashMap.add(entry.getValue());
//      }
//    }
    return needDownloadResHashMap;
  }

  private void downloadFile(){
    if ( this.needDownloadResHashMap.size() == 0 ){
      Log.d(TAG,"没有需要下载的文件，不进行patch");
      this.nativeEventHandler(EVENT_PATCH_NONEED,"0");
      return;
    }
    final int loadIndex = this.downloadIndex;
    String downloadFilePath = "";
    if ( loadIndex < this.needDownloadResHashMap.size()){
      downloadFilePath = this.needDownloadResHashMap.get(loadIndex);
    }else{
      Log.d(TAG,"所有文件下载完成！");
      this.exchangeFiles();
      this.nativeEventHandler(EVENT_PATCH_FINISHED,this.needDownloadResHashMap.size()+"");
      return;
    }
    final PluginAssetsUpdate plugin = this;
    OkHttpUtils.OnDownloadListener listener = new OkHttpUtils.OnDownloadListener() {
      @Override
      public void onDownloadSuccess(String url, String savePath) {
        Log.e(TAG,"下载成功 savePath = " + savePath );
        plugin.downloadIndex = loadIndex + 1;
        plugin.downloadFile();
      }
      @Override
      public void onDownloading(int progress) {
      }
      @Override
      public void onDownloadFailed(String url, String savePath) {
        Log.e(TAG,"下载失败 url = " + url );
        Log.e(TAG,"下载失败 savePath = " + savePath );
        plugin.downloadIndex = loadIndex + 1;
        plugin.downloadFile();
      }
    };
    this.nativeEventHandler(EVENT_PATCH_PROGRESS,loadIndex + "|" + this.needDownloadResHashMap.size());
    // 远程文件下载地址
    String downloadUrl = this.res_patch_url + "/" + downloadFilePath;
    // 文件下载本地保存地址
    String tempSavePath = this.tempCacheDir + "/" + downloadFilePath;
    int retry = 2;
    if (downloadFilePath.endsWith("js") || downloadFilePath.endsWith("jsc") ){
      retry = 5;
    }
    OkHttpUtils.getInstance().download(downloadUrl,tempSavePath,retry,listener);
  }

  private void exchangeFiles(){
//    Log.d(TAG,"需要删除的文件数"+this.needDeleteResHashMap.size());
//    for (int i=0;i<this.needDeleteResHashMap.size();i++){
//      String localfile = this.assetsPatchDir + "/" + this.needDeleteResHashMap.get(i);
//      FileUtils.deleteFile(new File(localfile));
//    }
    Log.d(TAG,"把缓存temp下的文件转移到cache目录下"+this.needDownloadResHashMap.size());
    for (int i=0;i<this.needDownloadResHashMap.size();i++){
      String tempFile = this.tempCacheDir + "/" + this.needDownloadResHashMap.get(i);
      File resFile = new File(tempFile);
      if (resFile.exists() && resFile.isFile()){
        String destFilePath = this.assetsPatchDir + "/" + this.needDownloadResHashMap.get(i);
        File destFile = new File(destFilePath);
        FileUtils.createFileDirectorys( destFile.getParent() );
        if (destFile.exists() && destFile.isFile()) {
          destFile.delete();
        }
        if (!resFile.renameTo( destFile )){
          Log.e(TAG,"转移失败"+destFilePath);
        }
      }else{
        File md5CacheFile = new File(this.assetsPatchDir + "/" + MD5FILE_NAME);
        if (md5CacheFile.exists()){
          md5CacheFile.delete();
        }
      }
    }
  }
}
