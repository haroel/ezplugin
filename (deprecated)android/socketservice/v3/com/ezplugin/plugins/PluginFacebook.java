package com.ezplugin.plugins;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.ezplugin.core.PluginBase;
import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import org.json.JSONObject;

import java.util.Arrays;

import static android.content.Context.MODE_PRIVATE;

public class PluginFacebook extends PluginBase {
    final public static String TAG = "PluginFacebook";

    private CallbackManager callbackManager = null;
    AccessTokenTracker accessTokenTracker = null;
    AccessToken accessToken = null;
    ProfileTracker profileTracker = null;

    @Override
    public void initPlugin(Context context, JSONObject jobj) {
        if (this.isInited){
            return;
        }
        super.initPlugin(context, jobj);

        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        // App code
                        Log.println(Log.INFO, "facebook", "login success");
                        accessToken = loginResult.getAccessToken();
                        getUserInfo(accessToken);
                    }

                    @Override
                    public void onCancel() {
                        // App code
                        Log.println(Log.INFO, "facebook", "login cancel");
                        nativeEventHandler("login_cancel", "");
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        // App code
                        Log.println(Log.INFO, "facebook", "login error: " + exception.getMessage());
                        nativeEventHandler("login_error", exception.getMessage());
                    }
                });

        accessTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken, AccessToken currentAccessToken) {
                // Set the access token using
                // currentAccessToken when it's loaded or set.
                if(currentAccessToken != null){
                    nativeEventHandler("access_token_Change", currentAccessToken.getToken());
                }

            }
        };
        // If the access token is available already assign it.
        accessToken = AccessToken.getCurrentAccessToken();

        profileTracker = new ProfileTracker() {
            @Override
            protected void onCurrentProfileChanged(Profile oldProfile, Profile currentProfile) {
                // App code
                if(currentProfile != null){
                    nativeEventHandler("profile_Change", currentProfile.getId());
                }
            }
        };


    }

    @Override
    public void onDestroy() {
        accessTokenTracker.stopTracking();
    }

    @Override
    public void excute(String action, String params, int callback) {
        super.excute(action, params, callback);
        Activity mActivity = (Activity) (getContext());
        switch (action) {
            case "login": {
                try {
                    // 读取保存的通知信息
                    SharedPreferences preferences = mActivity.getPreferences(MODE_PRIVATE);
                    String userInfo = preferences.getString(TAG + "UserInfo","");
                    if (!userInfo.isEmpty()){
                        Log.d(TAG,"使用上次保存的Facebook账号信息登录"+userInfo);
                        nativeEventHandler("logined", userInfo);          // 暂时由此处返回玩家数据到cocos中
                        return;
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
                AccessToken accessToken = AccessToken.getCurrentAccessToken();
                boolean isLoggedIn = accessToken != null && !accessToken.isExpired();
                if (isLoggedIn) {
                    getUserInfo(accessToken);
                } else {
                    LoginManager.getInstance().logInWithReadPermissions(mActivity, Arrays.asList("public_profile", "email"));
                    this.nativeCallbackHandler(callback, "1");
                }
                break;
            }
            case "isLogin": {
                AccessToken accessToken = AccessToken.getCurrentAccessToken();
                boolean isLoggedIn = accessToken != null && !accessToken.isExpired();
                this.nativeCallbackHandler(callback, isLoggedIn ? "true" : "false");
                break;
            }
            case "logout": {
                LoginManager.getInstance().logOut();
                nativeEventHandler("logout", "facebook");
                try {
                    // 删除用户信息
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
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void getUserInfo(AccessToken accessToken) {
        final String accToken = accessToken.getToken();
        final String uid = accessToken.getUserId();
        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,name,gender,picture,email");
        GraphRequest graphRequest = GraphRequest.newMeRequest(accessToken, new GraphRequest.GraphJSONObjectCallback() {
            @Override
            public void onCompleted(JSONObject object, GraphResponse response) {
                //这里获取的头像是50*50
                try {
                    if (object != null) {
                        JSONObject result = new JSONObject();
                        result.putOpt("id", uid);
                        result.putOpt("token", accToken);
                        result.putOpt("avatar", object.getJSONObject("picture").getJSONObject("data").get("url"));
                        result.putOpt("name", object.get("name"));
                        String ret = result.toString();
                        Log.println(Log.INFO, "facebook", ret);
                        nativeEventHandler("logined", ret);          // 暂时由此处返回玩家数据到cocos中
                        Activity mActivity = (Activity) (getContext());
                        try {
                            // 保存FB用户的信息
                            SharedPreferences preferences = mActivity.getPreferences(MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString(TAG + "UserInfo", ret);
                            //提交保存数据
                            editor.apply();
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }

                } catch (Exception e) {
                    nativeEventHandler("login_error", e.getMessage());          // 暂时由此处返回玩家数据到cocos中
                    e.printStackTrace();
                }
            }
        });

        graphRequest.setParameters(parameters);
        graphRequest.executeAsync();
    }
}
