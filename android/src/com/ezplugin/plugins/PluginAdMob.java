package com.ezplugin.plugins;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.ezplugin.core.PluginBase;

import com.ezplugin.utils.SysUtils;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdCallback;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Set;

import static com.google.android.gms.ads.AdRequest.ERROR_CODE_INTERNAL_ERROR;

/**
 * https://developers.google.com/admob/android/rewarded-ads
 *
 *
 * onRewardedAdFailedToLoad()	广告加载失败时，系统会调用此方法。此方法包含一个 errorCode 参数，该参数会指明发生了何种类型的失败。系统将这些可能的类型值定义为AdRequest类中的如下常量：
 * ERROR_CODE_INTERNAL_ERROR - 内部出现问题；例如，收到广告服务器的无效响应。
 * ERROR_CODE_INVALID_REQUEST - 广告请求无效；例如，广告单元 ID 不正确。
 * ERROR_CODE_NETWORK_ERROR - 由于网络连接问题，广告请求失败。
 * ERROR_CODE_NO_FILL - 广告请求成功，但由于缺少广告资源，未返回广告。
 *
 *
 * onRewardedAdFailedToShow()	广告显示失败时，系统会调用此方法。此方法包含一个 errorCode 参数，该参数会指明发生了何种类型的失败。系统将这些可能的类型值定义为 RewardedAdCallback 类中的如下常量：
 * ERROR_CODE_INTERNAL_ERROR - 内部出现问题。
 * ERROR_CODE_AD_REUSED - 激励广告已展示。 RewardedAd 对象是一次性对象，且仅可展示一次。将新的 RewardedAd 实例化并进行加载，即可展示新的广告。
 * ERROR_CODE_NOT_READY - 广告尚未成功加载。
 * ERROR_CODE_APP_NOT_FOREGROUND - 应用未在前台运行时，广告无法展示。
 *
 */
public class PluginAdMob extends PluginBase {
    private final static String TAG = "PluginAdMob";

//    private RewardedAd rewardedAd = null;

    private String adUnitID = "";

    /*
     * 连续加载失败错误次数，超过三次提示失败
     * */
//    private int faileLoadedCount = 0;

//    public boolean hasRewardAd = false;

    private HashMap<String,RewardAdVO> admobHashMap = new HashMap<String,RewardAdVO>();

    private int adInLoading = 0;

    public  boolean isDebug = false;
    @Override
    public void initPlugin(Context context, JSONObject jobj) {
        super.initPlugin(context, jobj);
        isDebug = SysUtils.isApkInDebug(context);
        try{
            String releaseAdUnitID = jobj.getString("adUnitID");
            String debugAdUnitID = jobj.getString("debugAdUnitID");

            String pkName = context.getPackageName();
            ApplicationInfo appInfo = getContext().getPackageManager().getApplicationInfo(pkName, PackageManager.GET_META_DATA);
            String admobAppID = appInfo.metaData.getString("com.google.android.gms.ads.APPLICATION_ID");
            MobileAds.initialize(context, admobAppID);

            if (isDebug){
                this.adUnitID = debugAdUnitID;
                Log.d(TAG,"使用调试的广告单元" + debugAdUnitID );
            }else{
                this.adUnitID = releaseAdUnitID;
            }
        }catch (Exception e){
            e.printStackTrace();
            Log.e(TAG,e.getMessage());
        }
    }
    private void createAndLoadRewardedAd() {
        final PluginAdMob plugin = this;
        final RewardedAd rewardedAd = new RewardedAd(getContext(), this.adUnitID);
        adInLoading++;
        Log.d(TAG,"load rewardedAd unitID"+adUnitID+"adInLoading=" + adInLoading);
        RewardedAdLoadCallback adLoadCallback = new RewardedAdLoadCallback() {
            @Override
            public void onRewardedAdLoaded() {
                // Ad successfully loaded.
                adInLoading = adInLoading-1;
                Log.d(TAG,"onRewardedAdLoaded  ResponseId = " + rewardedAd.getResponseInfo().getResponseId());
                RewardAdVO rewardAdVO = new RewardAdVO();
                rewardAdVO.rewardedAd = rewardedAd;
                rewardAdVO.state = 1;
                plugin.admobHashMap.put( rewardedAd.getResponseInfo().getResponseId(),rewardAdVO );
                plugin.nativeEventHandler("hasRewardAd", "1" );
            }

            @Override
            public void onRewardedAdFailedToLoad(int errorCode) {
                adInLoading = adInLoading-1;
                // Ad failed to load.
                Log.e(TAG,"onRewardedAdFailedToLoad errorCode = " + errorCode);
                // Toast toast=Toast.makeText( getContext(),"Google AdMob loading failed." ,Toast.LENGTH_SHORT    );
                // toast.setGravity( Gravity.BOTTOM, 0, 0);
                // toast.show();
            }
        };
        rewardedAd.loadAd(new AdRequest.Builder().build(), adLoadCallback);
    }
    @Override
    public void onDestroy(){
        admobHashMap.clear();

    }
    @Override
    public void excute(String action, String params, int callback) {
        super.excute(action, params, callback);

        switch (action){
            case "hasReward":{
                this.nativeCallbackHandler(callback, admobHashMap.size()>0?"1":"0" );
                break;
            }
            case "loadAd":{
                Log.d(TAG,"加载广告");
                if (adInLoading<1){
                    createAndLoadRewardedAd();
                }
                this.nativeCallbackHandler(callback, admobHashMap.size()>0?"1":"0" );
                break;
            }
            case "play":{
                final JSONObject jobj = new JSONObject();
                try{
                    jobj.put("adUnitID",this.adUnitID);
                    jobj.put("rewardId",params);
                    jobj.put("isDebug",isDebug);
                    if (admobHashMap.size() < 1){
                        if (adInLoading < 1){
                            createAndLoadRewardedAd();
                        }
                        jobj.put("msg","NotLoadedYet");
                        this.nativeEventHandler("error",  jobj.toString()  );
                        return;
                    }
                }catch (JSONException e){
                    e.printStackTrace();
                }
                try{
                    Set<String> adkeys = admobHashMap.keySet();
                    for (String key : adkeys){
                        RewardAdVO rewardAdVO = admobHashMap.get(key);
                        if (rewardAdVO.state == 1 && rewardAdVO.rewardedAd.isLoaded() ){
                            rewardAdVO.state = 2;
                            final PluginAdMob plugin = this;
                            Activity mActivity = (Activity) (getContext());
                            final String responseIdentifier = key;
                            jobj.put("responseIdentifier",responseIdentifier);
                            RewardedAdCallback adCallback = new RewardedAdCallback() {
                                @Override
                                public void onRewardedAdOpened() {
                                    // Ad opened.
                                    Log.d(TAG,"onRewardedAdOpened");
                                    plugin.nativeEventHandler("open",  jobj.toString() );
                                }
                                @Override
                                public void onRewardedAdClosed() {
                                    // Ad closed.
                                    Log.d(TAG,"onRewardedAdClosed");
                                    plugin.nativeEventHandler("close",  jobj.toString() );
                                    plugin.admobHashMap.remove(responseIdentifier);
                                }
                                @Override
                                public void onUserEarnedReward(@NonNull com.google.android.gms.ads.rewarded.RewardItem reward) {
                                    Log.d(TAG,"onUserEarnedReward type=" + reward.getType() + "，amount=" + reward.getAmount());
                                    try{
                                        jobj.put("rewardType",reward.getType());
                                        jobj.put("rewardAmount",reward.getAmount());
                                    }catch (JSONException e){
                                        e.printStackTrace();
                                    }
                                    plugin.nativeEventHandler("earned",  jobj.toString() );
                                }
                                @Override
                                public void onRewardedAdFailedToShow(int errorCode) {
                                    Log.e(TAG,"onRewardedAdFailedToShow errorCode = " + errorCode);
                                    try{
                                        jobj.put("code",errorCode);
                                        plugin.nativeEventHandler("error",   jobj.toString() );
                                    }catch (JSONException e){
                                        e.printStackTrace();
                                    }
                                    plugin.admobHashMap.remove(responseIdentifier);
                                }
                            };
                            rewardAdVO.rewardedAd.show( mActivity, adCallback);
                            createAndLoadRewardedAd(); // 加载下一个广告
                            return;
                        }
                    }
                    admobHashMap.clear();
                    createAndLoadRewardedAd();
                    Log.d("TAG", "The rewarded ad wasn't loaded yet.");
                    jobj.put("msg","NotLoadedYet");
                }catch (JSONException e){
                    e.printStackTrace();
                }
                this.nativeEventHandler("error",  jobj.toString()  );
                break;
            }
        }
    }
    private class RewardAdVO{
        RewardedAd rewardedAd = null;
        int state = 0;
    }
}
