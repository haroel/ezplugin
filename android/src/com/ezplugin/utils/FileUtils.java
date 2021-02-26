package com.ezplugin.utils;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {

  public static void deleteFile(File file) {
    if (file.isDirectory()) {
      File[] files = file.listFiles();
      for (int i = 0; i < files.length; i++) {
        File f = files[i];
        deleteFile(f);
      }
      file.delete();//如要保留文件夹，只删除文件，请注释这行
    } else if (file.exists()) {
      file.delete();
    }
  }

  public static void mkdir(String DirPath) {
    File file = new File(DirPath);
    if (!(file.exists() && file.isDirectory())) {
      file.mkdirs();
    }
  }

  public static String getFileContent(File file) {
    String content = "";
    if (!file.isDirectory()) {  //检查此路径名的文件是否是一个目录(文件夹)
      try {
        InputStream instream = new FileInputStream(file);
        if (instream != null) {
          InputStreamReader inputreader = new InputStreamReader(instream, "UTF-8");
          BufferedReader buffreader = new BufferedReader(inputreader);
          String line = "";
          //分行读取
          while ((line = buffreader.readLine()) != null) {
            content += line + "\n";
          }
          instream.close();//关闭输入流
        }
      } catch (java.io.FileNotFoundException e) {
        Log.d("TestFile", "The File doesn't not exist.");
      } catch (IOException e) {
        Log.d("TestFile", e.getMessage());
      }

    }
    return content;
  }
  public static void writeFileContent(File file,String content,boolean append){
    RandomAccessFile raf = null;
    FileOutputStream out = null;
    try {
      if (append) {
        //如果为追加则在原来的基础上继续写文件
        raf = new RandomAccessFile(file, "rw");
        raf.seek(file.length());
        raf.write(content.getBytes());

      } else {
        //重写文件，覆盖掉原来的数据
        out = new FileOutputStream(file);
        out.write(content.getBytes());
        out.flush();
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        if (raf != null) {
          raf.close();
        }
        if (out != null) {
          out.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * 读取assets下的txt文件，返回utf-8 String
   * @param context
   * @param fileName 不包括后缀
   * @return
   */
  public static String readAssetsTxt(Context context, String fileName){
    try {
      //Return an AssetManager instance for your application's package
      InputStream is = context.getAssets().open(fileName);
      int size = is.available();
      // Read the entire asset into a local byte buffer.
      byte[] buffer = new byte[size];
      is.read(buffer);
      is.close();
      // Convert the buffer into a string.
      String text = new String(buffer, "utf-8");
      // Finally stick the string into the text view.
      return text;
    } catch (IOException e) {
      // Should never happen!
//            throw new RuntimeException(e);
      e.printStackTrace();
    }
    return "读取错误，请检查文件名";
  }
  /**
   * 创建多级文件目录
   * @param fileDir
   * @return
   */
  public static void createFileDirectorys(String fileDir) {
    if (fileDir == null){
      return;
    }
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


  public static ArrayList<String> stringToLines(String str){
    ArrayList<String> lines = new ArrayList<String>();
    try{
      BufferedReader rdr = new BufferedReader(new StringReader(str));
      for (String line = rdr.readLine(); line != null; line = rdr.readLine()) {
        lines.add(line);
      }
      rdr.close();
      return lines;
    }catch (IOException e){
      e.printStackTrace();
    }
    return null;
  }
  //判断文件是否存在
  public static  boolean fileIsExists(String strFile) {
    try {
      File f = new File(strFile);
      if (!f.exists()) {
        return false;
      }

    } catch (Exception e) {
      return false;
    }

    return true;
  }
  }
