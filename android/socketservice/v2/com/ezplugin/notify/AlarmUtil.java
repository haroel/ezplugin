package com.ezplugin.notify;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by hehao on 2017/7/13.
 *
 * 工具类
 */

public class AlarmUtil {

  public static String md5(String str ){
    MessageDigest messageDigest = null;
    try {
      messageDigest = MessageDigest.getInstance("MD5");
      messageDigest.reset();
      messageDigest.update(str.getBytes("UTF-8"));
    } catch (NoSuchAlgorithmException e) {
      System.exit(-1);
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    byte[] byteArray = messageDigest.digest();
    StringBuffer md5StrBuff = new StringBuffer();
    for (int i = 0; i < byteArray.length; i++) {
      if (Integer.toHexString(0xFF & byteArray[i]).length() == 1)
        md5StrBuff.append("0").append(
                Integer.toHexString(0xFF & byteArray[i]));
      else
        md5StrBuff.append(Integer.toHexString(0xFF & byteArray[i]));
    }
    return md5StrBuff.toString();
  }

  public static String encode(String str ) {
    String ret = "";
    try{
      ret = URLEncoder.encode(str,"UTF-8");
    }catch (UnsupportedEncodingException e){
      e.printStackTrace();
    }
    return ret;
  }
  public static String decode(String str ) {
    String ret = "";
    try{
      ret = URLDecoder.decode(str,"UTF-8");
    }catch (UnsupportedEncodingException e){
      e.printStackTrace();
    }
    return ret;
  }

}
