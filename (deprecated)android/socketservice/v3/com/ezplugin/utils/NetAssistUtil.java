package com.ezplugin.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.widget.Toast;

/**
 * 网络状态判断辅助类
 * 参考：https://www.jianshu.com/p/83c28dcc6f75
 */
public class NetAssistUtil {

    /**
     * 获取网络连接状态是否可用
     *
     * @param context
     * @return
     */
    public static boolean isNetWorkCAvailable(Context context) {
        if (context != null) {
            ConnectivityManager connectivityManager = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                if (activeNetworkInfo.isConnected()){
                    return activeNetworkInfo.isAvailable();
                }else{
                    return false;
                }
            } else {
                return false;
            }
        }
        return false;
    }

    /**
     * 获取网络是否已经连接
     * @param context
     * @return
     */
    public static boolean isNetWorkConnected(Context context){
        try {
            ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivity != null) {

                NetworkInfo info = connectivity.getActiveNetworkInfo();
                if (info != null && info.isConnected()) {
                    if (info.getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    /**
     * 判断已连接的WIFI是否可用
     *
     * @param context
     * @return
     */
    public static boolean isWiFiAvailable(Context context) {
        if (context != null) {
            ConnectivityManager connectivityManager = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkInfo wiFiInfo = connectivityManager.
                        getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                boolean connected = wiFiInfo.isConnected();
                if (connected) {
                    return wiFiInfo.isAvailable();
                } else {
                    Toast.makeText(context, "Wifi网络没有连接", Toast.LENGTH_SHORT).show();
                    return false;
                }
            } else {
                return false;
            }
        }
        return false;
    }

    /**
     * 判断Wifi网络是否已经连接
     *
     * @param context
     * @return
     */
    public static boolean isWifiConnected(Context context) {
        if (context != null) {
            ConnectivityManager connectivityManager = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkInfo wiFiInfo = connectivityManager.
                        getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                return wiFiInfo.isConnected();
            } else {
                return false;
            }
        }
        return false;
    }

    /**
     * 判断移动数据是否已经连接
     *
     * @param context
     * @return
     */
    public static boolean isMobileConnected(Context context) {
        if (context != null) {
            ConnectivityManager connectivityManager = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkInfo mobileInfo = connectivityManager.
                        getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                return mobileInfo.isConnected();
            } else {
                return false;
            }
        }
        return false;
    }

    /**
     * 判断移动数据是否可用
     *
     * @param context
     * @return
     */
    public static boolean isMobileAvailable(Context context) {
        if (context != null) {
            ConnectivityManager connectivityManager = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkInfo mobileInfo = connectivityManager.
                        getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                boolean connected = mobileInfo.isConnected();
                if (connected) {
                    return mobileInfo.isAvailable();
                } else {
                    Toast.makeText(context, "移动数据没有连接", Toast.LENGTH_SHORT).show();
                    return false;
                }
            } else {
                return false;
            }
        }
        return false;
    }

    /**
     * 获取连接的网络类型
     * @param context
     * @return
     */
    public static Integer netType(Context context) {
        if (context != null) {
            ConnectivityManager connectivityManager = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
                    return activeNetworkInfo.getType();
                } else {
                    return -1;
                }
            } else {
                return -1;
            }
        }
        return -1;
    }

    /**
     * 获取当前 详细的连接网络类型
     * @param context
     * @return
     */
    public static int getAPNType(Context context) {
        int netType = 0;
        ConnectivityManager connMgr = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo == null) {
            return netType;
        }
        int nType = networkInfo.getType();
        if (nType == ConnectivityManager.TYPE_WIFI) {
            netType = 1;// wifi
        } else if (nType == ConnectivityManager.TYPE_MOBILE) {
            int nSubType = networkInfo.getSubtype();
            TelephonyManager mTelephony = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);
            if (nSubType == TelephonyManager.NETWORK_TYPE_UMTS
                    && !mTelephony.isNetworkRoaming()) {
                netType = 2;// 2G
            } else {
                netType = 3;// 3G
            }
        }
        return netType;
    }
}