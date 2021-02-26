//package com.ezplugin.plugins;
//
//import android.content.Context;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.net.Uri;
//import android.os.RemoteException;
//import android.util.Log;
//
//import com.android.installreferrer.api.InstallReferrerClient;
//import com.android.installreferrer.api.InstallReferrerClient.InstallReferrerResponse;
//import com.android.installreferrer.api.InstallReferrerStateListener;
//import com.android.installreferrer.api.ReferrerDetails;
//import com.ezplugin.core.PluginBase;
//import com.ezplugin.core.PluginCore;
//
//import org.json.JSONObject;
//
//import static android.content.Context.MODE_PRIVATE;
//
//public class PluginInstallReferrer extends PluginBase {
//    private static String TAG = "PluginInstallReferrer";
//    @Override
//    public void initPlugin(Context context, JSONObject jobj) {
//        super.initPlugin(context, jobj);
//
//        // 判断是否是首次安装
//        final SharedPreferences preferences = context.getSharedPreferences(TAG,MODE_PRIVATE);
//        String installtag = preferences.getString( "install" ,"");
//        if (installtag.equals("") || installtag.isEmpty()){
//            final InstallReferrerClient referrerClient = InstallReferrerClient.newBuilder(context).build();
//            referrerClient.startConnection(new InstallReferrerStateListener() {
//                @Override
//                public void onInstallReferrerSetupFinished(int responseCode) {
//                    Log.d(TAG,"onInstallReferrerSetupFinished code = " +responseCode);
//                    SharedPreferences.Editor editor = preferences.edit();
//                    editor.putString("install","1");
//                    editor.apply();
//                    switch (responseCode) {
//                        case InstallReferrerResponse.OK:
//                            // Connection established.
//                            try{
//                                ReferrerDetails response = referrerClient.getInstallReferrer();
//                                String referrerUrl = response.getInstallReferrer();
//                                long referrerClickTime = response.getReferrerClickTimestampSeconds();
//                                long appInstallTime = response.getInstallBeginTimestampSeconds();
////                                boolean instantExperienceLaunched = response.getGooglePlayInstantParam();
//                                if(referrerUrl != null && referrerUrl.length() > 0){
//                                    PluginCore.getInstance().setGlobalVariable("referrer",referrerUrl);
//                                    editor.putString("referrerUrl",referrerUrl);
//                                    Log.d(TAG,"获取到了 分享邀请参数" + referrerUrl );
////                                    Toast toast=Toast.makeText( getContext(),referrerUrl ,Toast.LENGTH_LONG);
////                                    toast.setGravity( Gravity.CENTER, 0, 0);
////                                    toast.show();
//                                }
//                            }catch (RemoteException e){
//                                e.printStackTrace();
//                            }
//                            break;
//                        case InstallReferrerResponse.FEATURE_NOT_SUPPORTED:
//                            // API not available on the current Play Store app.
//                            break;
//                        case InstallReferrerResponse.SERVICE_UNAVAILABLE:
//                            // Connection couldn't be established.
//                            break;
//                    }
//                }
//                @Override
//                public void onInstallReferrerServiceDisconnected() {
//                    // Try to restart the connection on the next request to
//                    // Google Play by calling the startConnection() method.
//                    Log.e(TAG,"onInstallReferrerServiceDisconnected  ");
//
//                }
//            });
//        }
//    }
//
//    @Override
//    public void excute(String action, String params, int callback) {
//        super.excute(action, params, callback);
//        switch (action){
//            case "link":{
//                String referrer = PluginCore.getInstance().getGlobalVariable("referrer");
//                if (referrer!= null && referrer.length() >0 ){
//                    nativeCallbackHandler(callback,referrer);
//                    PluginCore.getInstance().removeGlobalVariable("referrer");
//                }else{
//                    nativeCallbackHandler(callback,"");
//                }
//                break;
//            }
//        }
//    }
//    @Override
//    public void onNewIntent(Intent intent){
//        if (intent != null){
//            Uri uri = intent.getData();
//            if (uri != null){
//                nativeEventHandler("event_link",uri.toString());
//            }else{
//                nativeEventHandler("event_link","");
//            }
//        }
//    }
//
//}
