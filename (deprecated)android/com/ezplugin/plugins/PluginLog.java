package com.ezplugin.plugins;

import android.content.Context;
import android.util.Log;

import com.ezplugin.core.PluginBase;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;


/**
 * Created by howe on 2017/10/20.
 */

public class PluginLog extends PluginBase {

  // 日志文件名称
  private final static String LOG_NAME = "log.txt";

  private final static long LOG_SIZE = 1024 * 1024*3;// 大于这个数则新建一个log文件

  private String log_dir_path = "";

  private String log_path = "";

  private static PluginLog pluginLog = null;

  public PluginLog(){
    pluginLog = this;

  }
  @Override
  public void initPlugin(Context context, JSONObject jobj) {
    if (this.isInited){
      return;
    }
    super.initPlugin(context, jobj);
    if (getContext().getExternalCacheDir()!= null){
      log_dir_path = getContext().getExternalCacheDir().getPath() + "/_log";
    }else{
      log_dir_path = getContext().getCacheDir().getPath() + "/_log";
    }
    File dirFile = new File(log_dir_path);
    if (!dirFile.exists()){
      dirFile.mkdirs();
    }
    Log.d("PluginLog","日志保存目录"+log_dir_path);
    log_path = log_dir_path + "/" + LOG_NAME;
  }
  public void saveToFile(String logs){
    try{
      File file = new File( log_path );
      if (!file.exists()){
        if (!file.createNewFile()){
          Log.e("PluginLog",log_path+"创建失败");
        }else{
          FileWriter writer = new FileWriter(file, true);
          writer.write(logs);
          writer.close();
        }
      }else{
        if (file.length() < LOG_SIZE)
        {
          FileWriter writer = new FileWriter(file, true);
          writer.write(logs);
          writer.close();
        }else
        {
          // 获取文件内容并另存为其他
          InputStream instream = new FileInputStream(file);
          InputStreamReader inputreader = new InputStreamReader(instream);
          BufferedReader buffreader = new BufferedReader(inputreader);
          String content = "";
          String line;
          //分行读取
          while (( line = buffreader.readLine()) != null) {
            content += line + "\n";
          }
          instream.close();

          SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd-hh:mm:ss");
          String date = sDateFormat.format(new java.util.Date());
          String oldFilePath = log_dir_path + "/" +date+".txt";
          File oldFile = new File(oldFilePath);
          if (oldFile.createNewFile()){
            FileWriter writer = new FileWriter( oldFile, false);
            writer.write(content);
            writer.close();
          }else{
            Log.e("PluginLog",oldFilePath+"创建失败");
          }
          FileWriter writer = new FileWriter(file, false);
          writer.write(logs);
          writer.close();
        }
      }
    }catch (IOException e){
      e.printStackTrace();
    }
  }
  @Override
  public void excute(String type, String params, int callback) {
    super.excute(type, params, callback);
    if (type.equals("log")){
      d(params);
    }else if(type.equals("warn")){
      w(params);
    }else if(type.equals("error")){
      e(params);
    }
  }

  public static void d(String params){
    SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd-hh:mm:ss");
    String date = sDateFormat.format(new java.util.Date());
    String log = date + params + "\n";
    pluginLog.saveToFile(log);
    Log.d("PluginLog",params);
  }
  public static void w(String params){
    SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd-hh:mm:ss");
    String date = sDateFormat.format(new java.util.Date());
    String log = date + params + "\n";
    pluginLog.saveToFile(log);
    Log.w("PluginLog",params);
  }
  public static void e(String params){
    SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd-hh:mm:ss");
    String date = sDateFormat.format(new java.util.Date());
    String log = date + params + "\n";
    pluginLog.saveToFile(log);
    Log.e("PluginLog",params);
  }
}
