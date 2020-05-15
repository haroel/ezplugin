package com.ezplugin.utils;


import android.os.Environment;
//import android.support.annotation.NonNull;
import android.util.Log;

import androidx.annotation.NonNull;

import org.cocos2dx.okhttp3.Call;
import org.cocos2dx.okhttp3.Callback;
import org.cocos2dx.okhttp3.OkHttpClient;
import org.cocos2dx.okhttp3.Request;
import org.cocos2dx.okhttp3.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

//import okhttp3.Call;
//import okhttp3.Callback;
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.Response;

public class OkHttpUtils {
    final public static String TAG = "OkHttpUtils";
    private static OkHttpUtils downloadUtil = null;
    private final OkHttpClient okHttpClient;

    public static OkHttpUtils getInstance() {
        if (downloadUtil == null) {
            downloadUtil = new OkHttpUtils();
        }
        return downloadUtil;
    }

    private OkHttpUtils() {
        okHttpClient = new OkHttpClient();
    }

    public void get(final String url,final int retry,final OnRequestListener listener){
        Log.d(TAG,"http get " + url);
        Request request = new Request.Builder().url(url).build();
        final OkHttpUtils self = this;
        okHttpClient.newCall(request).enqueue(new Callback() {
              @Override
              public void onFailure(Call call, IOException e) {
                  e.printStackTrace();
                  // 下载失败
                  if (retry > 0){
                      self.get(url,retry-1,listener);
                  }else{
                      listener.onDownloadFailed( url);
                  }
              }
              @Override
              public void onResponse(Call call, Response response) throws IOException {
                  if (response.isSuccessful()){
                      listener.onDownloadSuccess(url,response);
                      return;
                  }
                  Log.w(TAG,"onResponse failed"+response.message());
                  if (retry > 0){
                      self.get(url,retry-1,listener);
                  }else{
                      listener.onDownloadFailed( url);
                  }
              }
        });
    }

    /**
     * @param url 下载连接
     * @param savePath 储存下载文件的目录
     * @param retry  下载失败重试次数
     * @param listener 下载监听
     */
    public void download(final String url, final String savePath, final int retry, final OnDownloadListener listener) {
        Request request = new Request.Builder().url(url).build();
        final OkHttpUtils self = this;
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 下载失败
                listener.onDownloadFailed( url, savePath);
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()){
                    if (retry > 0){
                        self.download(url,savePath,retry-1,listener);
                    }else{
                        Log.w(TAG,"重试结束！，onResponse failed "+url + response.message());
                        listener.onDownloadFailed( url, savePath);
                    }
                    return;
                }
                InputStream is = null;
                byte[] buf = new byte[2048];
                int len = 0;
                FileOutputStream fos = null;
                boolean isok = false;
                // 储存下载文件的目录
                try {
                    is = response.body().byteStream();
                    long total = response.body().contentLength();
                    File file = new File( savePath);
                    self.createFileDirectorys(file.getParent());
                    fos = new FileOutputStream(file);
                    long sum = 0;
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                        sum += len;
                        int progress = (int) (sum * 1.0f / total * 100);
                        // 下载中
                        listener.onDownloading(progress);
                    }
                    fos.flush();
                    isok = true;

                } catch (Exception e) {
                    e.printStackTrace();
                    isok = false;

                } finally {
                    try {
                        if (is != null)
                            is.close();
                    } catch (IOException e) {
                    }
                    try {
                        if (fos != null)
                            fos.close();
                    } catch (IOException e) {
                    }
                    if (isok){
                        // 下载完成
                        listener.onDownloadSuccess(url,savePath);
                    }else{
                        if (retry > 0){
                            self.download(url,savePath,retry-1,listener);
                        }else{
                            listener.onDownloadFailed( url, savePath);
                        }
                    }
                }
            }
        });
    }

    /**
     * @param saveDir
     * @return
     * @throws IOException
     * 判断下载目录是否存在
     */
    private String isExistDir(String saveDir) throws IOException {
        // 下载位置
        File downloadFile = new File(Environment.getExternalStorageDirectory(), saveDir);
        if (!downloadFile.mkdirs()) {
            downloadFile.createNewFile();
        }
        String savePath = downloadFile.getAbsolutePath();
        return savePath;
    }
    public void createFileDirectorys(String fileDir) {
        String[] fileDirs=fileDir.split("\\/");
        String topPath="";
        for (int i = 0; i < fileDirs.length; i++) {
            topPath+="/"+fileDirs[i];
            File file = new File(topPath);
            if (file.exists()) {
                continue;
            }else {
                file.mkdir();
            }
        }
    }
    /**
     * @param url
     * @return
     * 从下载连接中解析出文件名
     */
    @NonNull
    private String getNameFromUrl(String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }

    public interface OnDownloadListener {
        /**
         * 下载成功
         */
        void onDownloadSuccess(String url,String savePath);

        /**
         * @param progress
         * 下载进度
         */
        void onDownloading(int progress);

        /**
         * 下载失败
         */
        void onDownloadFailed(String url,String savePath);
    }

    public interface OnRequestListener {
        /**
         * 下载成功
         */
        void onDownloadSuccess(String url,  Response response);


        /**
         * 下载失败
         */
        void onDownloadFailed(String url);
    }
}
