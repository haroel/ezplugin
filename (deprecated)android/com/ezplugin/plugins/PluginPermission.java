package com.ezplugin.plugins;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.net.Uri;
import android.provider.Settings;
//import android.support.annotation.NonNull;
//import android.support.v4.app.ActivityCompat;
//import android.support.v4.content.ContextCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.ezplugin.core.PluginBase;

import org.json.JSONObject;

/**
 * 权限管理
 */

public class PluginPermission extends PluginBase {

    final public static String TAG = "PluginPermission";

    private static final int PERMISSION_ALL_GRANTED    = 0;     // 申请的权限全部授予
    private static final int PERMISSION_DENIED          = 1;     // 申请的权限被拒绝但没有永久拒绝
    private static final int PERMISSION_DENIED_FOREVER = 2;     // 申请的权限被永久拒绝,只能通过设置进行手动设置

    public static final int REQUEST_PERMISSION_CODE          = 0;
    public static final int REQUEST_PERMISSION_SETTING_CODE = 1;

    private String[] lackPermissions = {};                     // 跳转到设置之前先记录申请的权限

    @Override
    public void initPlugin(Context context, JSONObject jobj) {
        super.initPlugin(context, jobj);

    }

    @Override
    public void excute(String type, String params, int callback) {

        switch (type){
            case "requestAllPermission":{
                this.requestAllPermission();
                break;
            }
            case "requestPermissions":{
                this.requestPermissions(params);
                break;
            }
            case "goToSetting":{
                this.jumpToSetting();
                break;
            }
        }
        this.nativeCallbackHandler(callback,"1");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == REQUEST_PERMISSION_SETTING_CODE) {
            onSettingResult();
        }
    }

    /**
     * 申请权限的结果
     * @param requestCode
     * @param permissions 申请的权限
     * @param grantResults 申请的结果
     */
    public void onRequestPermissionsResult(int requestCode, @NonNull final String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != REQUEST_PERMISSION_CODE) {
            return;
        }

        int result = PERMISSION_ALL_GRANTED;

        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) { // 用户选了拒绝, 但是可以再次弹框申请
                result = PERMISSION_DENIED;
                Activity mActivity = (Activity) (getContext());
                if (!ActivityCompat.shouldShowRequestPermissionRationale(mActivity, permissions[i])) { // 拒绝的同时还勾选了不再提示, 无法弹框申请, 只能引导用户进入设置页面
                    lackPermissions = permissions;
                    result = PERMISSION_DENIED_FOREVER;
                    break;
                }
            }
        }
        this.nativeEventHandler("requestPermissionsResult",REQUEST_PERMISSION_CODE + "|" + result);
    }

    /**
     * 申请所有需要的权限
     * @return  false 代表所有权限都有了, true 则需要,结果在回调中传回 或者这个函数不能使用
     */
    public boolean requestAllPermission() {
        Activity mActivity = (Activity) (getContext());

        if (mActivity == null) {
          return true;
    }

        StringBuilder permissions = new StringBuilder();
        PackageManager pm = mActivity.getPackageManager();
        PackageInfo packageInfo;

        try {
            packageInfo = pm.getPackageInfo(mActivity.getPackageName(), PackageManager.GET_PERMISSIONS);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        if (packageInfo.requestedPermissions != null) {
            for (String requestedPermission : packageInfo.requestedPermissions) {
                try {
                    PermissionInfo permissionInfo = pm.getPermissionInfo(requestedPermission, 0);
                    if (permissionInfo.protectionLevel == 1) {
                        permissions.append(requestedPermission + "/");
                    }
                } catch (PackageManager.NameNotFoundException ignored) {
                    Log.e("requestAllPermission", ignored.toString());
                }
            }
        }
        return requestPermissions(permissions.toString());
    }

    /**
     * 申请权限
     * @param permissionStr 用 / 分割 @example READ_CALENDAR/READ_CONTACTS {@link android.Manifest.permission}
     * @return  false 代表所有权限都有了, true 则需要,结果在回调中传回 或者这个函数不能使用
     */
    public boolean requestPermissions(String permissionStr) {
        Activity mActivity = (Activity) (getContext());
        if (mActivity == null) {
            return true;
        }

        String[] permissions = permissionStr.split("/");

        int size = 0;

        for (String permission: permissions) {
            if (isLackPermission(permission)) {
                size++;
            }
        }

        if (size == 0) { // 不缺少权限
            this.nativeEventHandler("requestPermissionsResult",REQUEST_PERMISSION_CODE + "|" + PERMISSION_ALL_GRANTED);
            return false;
        }

        String lackPermissions[] = new String[size];
        int i = 0;

        for (String permission: permissions) {

            if (isLackPermission(permission)) {
                lackPermissions[i++] = permission;
            }
        }
        Activity activity = (Activity) (getContext());
        ActivityCompat.requestPermissions(activity, lackPermissions, REQUEST_PERMISSION_CODE);
        return true;
    }

    /**
     * 跳转至设置页面
     */
    public void jumpToSetting () {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + this.getContext().getPackageName()));
        Activity activity = (Activity) (getContext());
        activity.startActivityForResult(intent, REQUEST_PERMISSION_SETTING_CODE);
    }

    /**
     * 设置是否全部完成
     */
    private void onSettingResult() {
        int result = PERMISSION_ALL_GRANTED;

        for (String permission: lackPermissions) {
            if (isLackPermission(permission)) {
                result = PERMISSION_DENIED_FOREVER;
                break;
            }
        }
        this.nativeEventHandler("requestPermissionsResult",REQUEST_PERMISSION_CODE + "|" + result);
    }

    /**
     * @param permission {@link android.Manifest.permission}
     * @return 是否缺少权限
     */
    public boolean isLackPermission (String permission) {
        Activity mActivity = (Activity) (getContext());
        return ContextCompat.checkSelfPermission(mActivity, permission) == PackageManager.PERMISSION_DENIED;
    }

}
