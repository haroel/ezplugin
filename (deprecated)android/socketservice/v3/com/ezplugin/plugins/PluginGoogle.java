package com.ezplugin.plugins;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.ezplugin.core.PluginBase;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import static android.content.Context.MODE_PRIVATE;

public class PluginGoogle extends PluginBase {
    final public static String TAG = "PluginGoogle";

    private final int LOGIN_IN = 1;

    private GoogleSignInClient mGoogleSignInClient;

    @Override
    public void initPlugin(Context context, JSONObject jobj) {
        if (this.isInited){
            return;
        }
        super.initPlugin(context, jobj);

        Activity mActivity = (Activity) (getContext());


//        String clientId = String.valueOf(R.string.default_web_client_id);        // 转服务器校验预留内容
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                .requestIdToken(clientId)                                        // 转服务器校验预留内容
                .requestEmail()
                .requestProfile()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(mActivity, gso);
    }

    @Override
    public void excute(String action, String params, int callback) {
        super.excute(action, params, callback);
        Activity mActivity = (Activity) (getContext());
        switch(action){
            case "login":{
                try {
                    // 读取保存的通知信息
                    SharedPreferences preferences = mActivity.getPreferences(MODE_PRIVATE);
                    String userInfo = preferences.getString(TAG + "UserInfo","");
                    if (!userInfo.isEmpty()){
                        Log.d(TAG,"使用上次保存的Google账号信息登录"+userInfo);
                        nativeEventHandler("logined", userInfo);          // 暂时由此处返回玩家数据到cocos中
                        return;
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
                GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(mActivity);
                if(account != null){
                    returnInfoToCocos1(account);
                }
                else{
                    Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                    mActivity.startActivityForResult(signInIntent, LOGIN_IN);
                }
                break;
            }
            case "logout":{
                mGoogleSignInClient.signOut()
                .addOnCompleteListener(mActivity, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // ...
                        nativeEventHandler("logout", "google");          // 暂时由此处返回玩家数据到cocos中
                    }
                });
                try {
                    // 读取保存的通知信息
                    SharedPreferences preferences = mActivity.getPreferences(MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.remove(TAG + "UserInfo");
                    editor.apply();
                }catch (Exception e){
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == LOGIN_IN) {
            // The Task returned from this call is always completed, no need to attach
            Log.d(TAG, "login success" );
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }else{
            nativeEventHandler("login_error", "signInResult:failed code=" + requestCode);          // 暂时由此处返回玩家数据到cocos中
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            returnInfoToCocos1(account);
        } catch (ApiException e) {
            Log.w("google-login", "signInResult:failed code=" + e.getStatusCode());
            nativeEventHandler("login_error", "signInResult:failed code=" + e.getStatusCode());          // 暂时由此处返回玩家数据到cocos中
        }
    }

    private void returnInfoToCocos1(GoogleSignInAccount account){
        String idToken = account.getIdToken();
        String uid = account.getId();
        Uri uri = account.getPhotoUrl();
        String name = account.getDisplayName();
        JSONObject result = new JSONObject();
        try{
            result.putOpt("id", uid);
            result.putOpt("token", idToken);
//            result.putOpt("avatar", avatar);
            result.putOpt("name", name);
            if(uri != null){
                result.putOpt("avatar", uri.toString());
            }
        }catch(JSONException e){
            nativeEventHandler("login_error", "result OBJECT create error" + e.getMessage());          // 暂时由此处返回玩家数据到cocos中
            Log.w("google-login", "result OBJECT create error" + e.getMessage());
            e.printStackTrace();
            nativeEventHandler("login_error", "result OBJECT create error" + e.getMessage());          // 暂时由此处返回玩家数据到cocos中
            return;
        }
        String ret = result.toString();
        nativeEventHandler("logined", ret);          // 暂时由此处返回玩家数据到cocos中
        Activity mActivity = (Activity) (getContext());
        try {
            // 保存用户的信息
            SharedPreferences preferences = mActivity.getPreferences(MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(TAG + "UserInfo", ret);
            //提交保存数据
            editor.apply();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
