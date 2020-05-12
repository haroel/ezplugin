package com.ezplugin.utils;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static android.content.Context.MODE_PRIVATE;

public class DeviceIdUtils {
    public static String TAG = "DeviceIdUtils";

    public static  String getUUID(Context context){
        try {
            // 读取保存的通知信息
            SharedPreferences preferences = context.getSharedPreferences(TAG, MODE_PRIVATE);
            String uuid = preferences.getString("uuid","");
            if (!uuid.isEmpty()){
//                Log.d(TAG,"使用缓存的uuid"+uuid);
                return uuid;
            }
            uuid = getDeviceId(context);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("uuid", uuid);
//            Log.d(TAG,"生成uuid"+uniqueID);
            //提交保存数据
            editor.apply();
            return uuid;
        }catch (Exception e){
            e.printStackTrace();
        }
        return getDeviceId(context);
    }
    /**
     * 获得设备硬件标识
     *
     * @param context 上下文
     * @return 设备硬件标识
     */
    public static String getDeviceId(Context context) {
        StringBuilder sbDeviceId = new StringBuilder();

        //获得设备默认IMEI（>=6.0 需要ReadPhoneState权限）
        String imei = getIMEI(context);
        Log.d(TAG,"imei="+imei);
        //获得AndroidId（无需权限）
        String androidid = getAndroidId(context);
        Log.d(TAG,"androidid="+androidid);

        //获得设备序列号（无需权限）
        String serial = getSERIAL();
        Log.d(TAG,"serial="+serial);
        // mac地址(无需权限)
        String macaddress = getMacAddress();
        Log.d(TAG,"macaddress="+macaddress);

        //获得硬件uuid（根据硬件相关属性，生成uuid）（无需权限）
        String buildInfos = getDeviceBuildInfo().replace("-", "");
        Log.d(TAG,"buildInfos="+buildInfos);
        //追加imei
        if (imei != null && imei.length() > 0) {
            sbDeviceId.append(imei);
            sbDeviceId.append("|");
        }
        //追加androidid
        if (androidid != null && androidid.length() > 0) {
            sbDeviceId.append(androidid);
            sbDeviceId.append("|");
        }
        //追加serial
        if (serial != null && serial.length() > 0) {
            sbDeviceId.append(serial);
            sbDeviceId.append("|");
        }
        //追加mac地址
        if (macaddress != null && macaddress.length() > 0) {
            sbDeviceId.append(macaddress);
            sbDeviceId.append("|");
        }
        //追加硬件uuid
        if (buildInfos != null && buildInfos.length() > 0) {
            sbDeviceId.append(buildInfos);
        }

        //生成SHA1，统一DeviceId长度
        if (sbDeviceId.length() > 0) {
            try {
                byte[] hash = getHashByString(sbDeviceId.toString());
                String sha1 = bytesToHex(hash);
                if (sha1 != null && sha1.length() > 0) {
                    //返回最终的DeviceId
                    Log.d(TAG,"生成DeviceID="+sha1);
                    return sha1;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        //如果以上硬件标识数据均无法获得，
        //则DeviceId默认使用系统随机数，这样保证DeviceId不为空
        return UUID.randomUUID().toString().replace("-", "");
    }

    //需要获得READ_PHONE_STATE权限，>=6.0，默认返回null
    private static String getIMEI(Context context) {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            try {
                TelephonyManager tm = (TelephonyManager)
                        context.getSystemService(Context.TELEPHONY_SERVICE);
                return tm.getDeviceId();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return "";
    }

    /**
     * 获得设备的AndroidId
     *
     * @param context 上下文
     * @return 设备的AndroidId
     */
    private static String getAndroidId(Context context) {
        try {
            final String android = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            // Use the Android ID unless it's broken, in which case
            // fallback on deviceId,
            // unless it's not available, then fallback on a random
            // number which we store to a prefs file
            if (android == null){
                return "";
            }
            if (android.equals("9774d56d682e549c")){
                return "";
            }
            return android;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    private static String getMacAddress() {
        String macadd = "";
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) {
                    continue;
                }
                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }
                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X:", b));
                }
                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                macadd= res1.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (macadd.equals("02:00:00:00:00:00")){
            return "";
        }
        return macadd;
    }
    /**
     * 获得设备序列号（如：WTK7N16923005607）, 个别设备无法获取
     *
     * @return 设备序列号
     */
    private static String getSERIAL() {
        String serial = "";
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
            serial = Build.SERIAL;
        } else {
            try{
                serial = Build.getSerial();
            } catch (Exception ex) {
                ex.printStackTrace();
                return "";
            }
        }
        if (serial != null){
            return serial;
        }
        return "";
    }
    /**
     * 获得设备硬件uuid
     * 使用硬件信息，计算出一个随机数
     *
     * @return 设备硬件uuid
     */
    private static String getDeviceBuildInfo() {
        try {
            return Build.BOARD + '-'+ Build.BRAND + "-" + Build.DEVICE + "-" +Build.HARDWARE + "-" + Build.ID +
                    Build.MODEL+ "-" + Build.PRODUCT;
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }

    /**
     * 获得设备硬件uuid
     * 使用硬件信息，计算出一个随机数
     *
     * @return 设备硬件uuid
     */
    private static String getDeviceUUID() {
        try {
            String dev = "3883756" +
                    Build.BOARD.length() % 10 +
                    Build.BRAND.length() % 10 +
                    Build.DEVICE.length() % 10 +
                    Build.HARDWARE.length() % 10 +
                    Build.ID.length() % 10 +
                    Build.MODEL.length() % 10 +
                    Build.PRODUCT.length() % 10;
            return new UUID(dev.hashCode(),
                    Build.SERIAL.hashCode()).toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }
    public static String getUniquePsuedoID()
    {
        String m_szDevIDShort = "35" + (Build.BOARD.length() % 10) + (Build.BRAND.length() % 10) +
                (Build.CPU_ABI.length() % 10) + (Build.DEVICE.length() % 10) +
                (Build.MANUFACTURER.length() % 10) + (Build.MODEL.length() % 10) +
                (Build.PRODUCT.length() % 10);

        // Thanks to @Roman SL!
        // http://stackoverflow.com/a/4789483/950427
        // Only devices with API >= 9 have android.os.Build.SERIAL
        // http://developer.android.com/reference/android/os/Build.html#SERIAL
        // If a user upgrades software or roots their phone, there will be a duplicate entry
        String serial = null;
        try
        {
            serial = android.os.Build.class.getField("SERIAL").get(null).toString();

            // Go ahead and return the serial for api => 9
            return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
        }
        catch (Exception e)
        {
            // String needs to be initialized
            serial = "serial"; // some value
        }

        // Thanks @Joe!
        // http://stackoverflow.com/a/2853253/950427
        // Finally, combine the values we have found by using the UUID class to create a unique identifier
        return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
    }
    /**
     * 取SHA1
     * @param data 数据
     * @return 对应的hash值
     */
    private static byte[] getHashByString(String data)
    {
        try{
            MessageDigest  messageDigest = MessageDigest.getInstance("SHA1");
            messageDigest.reset();
            messageDigest.update(data.getBytes("UTF-8"));
            return messageDigest.digest();
        } catch (Exception e){
            return "".getBytes();
        }
    }

    /**
     * 转16进制字符串
     * @param data 数据
     * @return 16进制字符串
     */
    private static String bytesToHex(byte[] data){
        StringBuilder sb = new StringBuilder();
        String stmp;
        for (int n = 0; n < data.length; n++){
            stmp = (Integer.toHexString(data[n] & 0xFF));
            if (stmp.length() == 1)
                sb.append("0");
            sb.append(stmp);
        }
        return sb.toString().toUpperCase(Locale.CHINA);
    }
}
