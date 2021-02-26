package com.ezplugin.plugins;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.ezplugin.core.PluginBase;
import com.ezplugin.core.PluginCore;
import com.ezplugin.utils.SysUtils;
//import com.google.android.gms.tasks.OnCompleteListener;
//import com.google.android.gms.tasks.OnFailureListener;
//import com.google.android.gms.tasks.OnSuccessListener;
//import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
//import com.google.firebase.dynamiclinks.DynamicLink;
//import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
//import com.google.firebase.dynamiclinks.PendingDynamicLinkData;
//import com.google.firebase.dynamiclinks.ShortDynamicLink;
//import com.google.firebase.iid.FirebaseInstanceId;
//import com.google.firebase.iid.InstanceIdResult;

import org.json.JSONObject;

/*
 * FireBase
 *
 * */
public class PluginFirebase extends PluginBase {

    final public static String TAG = "PluginFirebase";

    private FirebaseAnalytics mFirebaseAnalytics = null;
//    private FirebaseDynamicLinks mFirebaseDynamicLinks = null;

    private String deepLinkURL = "";
    @Override
    public void initPlugin(Context context, JSONObject jobj) {
        super.initPlugin(context, jobj);
        if (!isInited){
            mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
//            mFirebaseDynamicLinks = FirebaseDynamicLinks.getInstance();
            mFirebaseAnalytics.setAnalyticsCollectionEnabled(true);
//            Activity mActivity = (Activity) (context);
//            getDynamicLink(mActivity.getIntent());
//            final PluginBase self = this;
//            FirebaseInstanceId.getInstance().getInstanceId()
//                    .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
//                        @Override
//                        public void onComplete(@NonNull Task<InstanceIdResult> task) {
//                            if (!task.isSuccessful()) {
//                                Log.w(TAG, "getInstanceId failed", task.getException());
//                                return;
//                            }
//                            // Get new Instance ID token
//                            String token = task.getResult().getToken();
//                            // Log and toast
//                            Log.d(TAG,"InstanceIdToken: " + token);
//                            self.nativeEventHandler("fcm_instanceid",token);
//                        }
//                    });
        }
    }

    @Override
    public void excute(String action, String params, int callback) {
        super.excute(action, params, callback);
        switch (action){
            case "createDynamicLink":{
                createDynamicLink(params,callback);
                break;
            }
            case "getDynamicLink":{
                nativeCallbackHandler(callback,this.deepLinkURL);
                break;
            }
            // 登录设置属性
            case "setUserProperty":{
                try{
                    JSONObject jobj = new JSONObject(params);
                    mFirebaseAnalytics.setUserProperty("uid",jobj.getString("uid"));
                    mFirebaseAnalytics.setUserProperty("accountID",jobj.getString("accountID"));
                    mFirebaseAnalytics.setUserProperty("accountPlatform",jobj.getString("accountPlatform"));
                    mFirebaseAnalytics.setUserProperty("level",jobj.getString("level"));

                }catch (Exception e){
                    e.printStackTrace();
                }
                nativeCallbackHandler(callback,"");
                break;
            }
            case "log_event":{
                try{
                    JSONObject jobj = new JSONObject(params);
                    String event = jobj.getString("event");
                    String key = jobj.getString("key");
                    String value = jobj.getString("value");
                    Bundle bundle = new Bundle();
                    bundle.putString(key,value);
                    mFirebaseAnalytics.logEvent(event, bundle);

                }catch (Exception e){
                    e.printStackTrace();
                }
                nativeCallbackHandler(callback,"");
                break;
            }
            default:{
                break;
            }
        }
    }
    public void onNewIntent(Intent intent){
        this.getDynamicLink(intent);
    }
    private void getDynamicLink(Intent intent){
//        String referrer = PluginCore.getInstance().getGlobalVariable("referrer");
//        if (referrer!= null && referrer.length() >0 ){
//            deepLinkURL = referrer;
//            nativeEventHandler("DynamicLinkChanged",deepLinkURL);
//            PluginCore.getInstance().removeGlobalVariable("referrer");
//            return;
//        }
//        Activity mActivity = (Activity) (getContext());
//        if (intent != null){
//            Uri uri = intent.getData();
//            if (uri != null){
//                deepLinkURL = uri.toString();
//            }else{
//                deepLinkURL = "";
//            }
//            nativeEventHandler("DynamicLinkChanged",deepLinkURL);
//
//            Log.d(TAG,"getDynamicLink intent = " + intent.getDataString());
////            mFirebaseDynamicLinks.getDynamicLink(intent)
////                    .addOnSuccessListener(mActivity, new OnSuccessListener<PendingDynamicLinkData>() {
////                        @Override
////                        public void onSuccess(PendingDynamicLinkData pendingDynamicLinkData) {
////                            // Get deep link from result (may be null if no link is found)
////                            Uri deepLink = null;
////                            if (pendingDynamicLinkData != null) {
////                                deepLink = pendingDynamicLinkData.getLink();
////                                Log.e(TAG," 初始化获取的DeepLink = " + deepLink.toString() );
////                                deepLinkURL = deepLink.toString();
////                            }
////                            nativeEventHandler("DynamicLinkChanged",deepLinkURL);
////                        }
////                    })
////                    .addOnFailureListener(mActivity, new OnFailureListener() {
////                        @Override
////                        public void onFailure(@NonNull Exception e) {
////                            Log.w(TAG, "getDynamicLink:onFailure", e);
////                            nativeEventHandler("DynamicLinkChanged",deepLinkURL);
////                        }
////                    });
//        }

    }

    private void createDynamicLink(String params,final int callback){
//        try {
//            Activity mActivity = (Activity) (getContext());
//            JSONObject jobj = new JSONObject(params);
//            String domainUrl = jobj.getString("domainLink");
//            String link = jobj.getString("link");
//            String packname = getContext().getPackageName();
//            DynamicLink.Builder builder = mFirebaseDynamicLinks.createDynamicLink()
//                    .setLink(Uri.parse(link))
//                    .setDomainUriPrefix(domainUrl)
//                    .setAndroidParameters(new DynamicLink.AndroidParameters.Builder(packname)
//                            .setMinimumVersion(1).build());
//
//            if (jobj.has("AppStoreId") && jobj.has("bundleID")){
//                builder.setIosParameters(new DynamicLink.IosParameters.Builder(jobj.getString("bundleID"))
//                        .setAppStoreId(jobj.getString("AppStoreId"))
//                        .setMinimumVersion("1.0.0")
//                        .build());
//
//            }
//            Uri dynamicLinkUri = builder.buildDynamicLink().getUri();
//            final String dynamicLinkUris = dynamicLinkUri.toString();
//            Log.d(TAG,dynamicLinkUris);
//            Task<ShortDynamicLink> shortLinkTask = mFirebaseDynamicLinks.createDynamicLink()
//                    .setLongLink(dynamicLinkUri)
//                    .buildShortDynamicLink()
//                    .addOnCompleteListener(mActivity, new OnCompleteListener<ShortDynamicLink>() {
//                        @Override
//                        public void onComplete(@NonNull Task<ShortDynamicLink> task) {
//                            if (task.isSuccessful()) {
//                                // Short link created
//                                Uri shortLink = task.getResult().getShortLink();
//                                Uri flowchartLink = task.getResult().getPreviewLink();
//                                Log.d(TAG,"短链 = " + shortLink.toString());
//                                Log.d(TAG,"getPreviewLink = " + flowchartLink.toString());
//                                nativeCallbackHandler(callback,shortLink.toString());
////                                nativeCallbackHandler(callback,dynamicLinkUris);
//                            } else {
//                                // Error
//                                Log.w(TAG,"生成短链错误" + task.getResult().toString());
//                                nativeCallbackHandler(callback,dynamicLinkUris);
//                            }
//                        }
//                    });
//
//        }catch (Exception e){
//            e.printStackTrace();
//            nativeCallbackErrorHandler(callback,e.toString());
//        }
    }

    private  void test(Context context){
//        Activity mActivity = (Activity) (getContext());
//        int vc = (int) SysUtils.getAppVersionCode(context);
//        String packname = context.getPackageName();
//        DynamicLink dynamicLink = mFirebaseDynamicLinks.createDynamicLink()
//                .setLink(Uri.parse("https://ps6up.com:8083/client/share.html?inviteid=8888"))
//                .setDomainUriPrefix("https://epicslots.page.link/")
//                // Open links with this app on Android
//                .setAndroidParameters(new DynamicLink.AndroidParameters.Builder(packname).setMinimumVersion(1).build())
//                .buildDynamicLink();
//
//        Uri dynamicLinkUri = dynamicLink.getUri();
//        Log.d(TAG+" TEST","创建的动态链接 = "+dynamicLinkUri.toString());
//
//        Task<ShortDynamicLink> shortLinkTask = mFirebaseDynamicLinks.createDynamicLink()
//                .setLongLink(dynamicLinkUri)
//                .buildShortDynamicLink()
//                .addOnCompleteListener(mActivity, new OnCompleteListener<ShortDynamicLink>() {
//                    @Override
//                    public void onComplete(@NonNull Task<ShortDynamicLink> task) {
//                        if (task.isSuccessful()) {
//                            // Short link created
//                            Uri shortLink = task.getResult().getShortLink();
//                            Uri flowchartLink = task.getResult().getPreviewLink();
//                            Log.d(TAG+" TEST","短链 = " + shortLink.toString());
//                            Log.d(TAG+" TEST","getPreviewLink = " + flowchartLink.toString());
//
//                        } else {
//                            // Error
//                            // ...
//                        }
//                    }
//                });
    }

}
