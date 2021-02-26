package com.ezplugin.plugins;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.anythink.core.api.ATAdInfo;
import com.anythink.core.api.ATSDK;
import com.anythink.core.api.AdError;
import com.anythink.core.api.NetTrafficeCallback;
import com.anythink.rewardvideo.api.ATRewardVideoAd;
import com.anythink.rewardvideo.api.ATRewardVideoListener;
import com.ezplugin.core.PluginBase;
import com.ezplugin.utils.SysUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PluginTopon extends PluginBase {
    final public static String TAG = "PluginTopon";
    private int adInLoading = 0;

    private String placementID = "";

    private List<RewardAdVO> rewardAds = new ArrayList<>();
    private static int RewardAdVOCount = 1;
    private  boolean isDebug = false;

    private RewardAdVO getRewardVO(int idcount){
        for (int i = 0; i < rewardAds.size(); i++) {
            RewardAdVO vo = rewardAds.get(i);
            if (vo.ID == idcount){
                return vo;
            }
        }
        return null;
    }
    @Override
    public void initPlugin(Context context, JSONObject jobj) {
        super.initPlugin(context, jobj);
        if (this.isInited){
            return;
        }
        isDebug = SysUtils.isApkInDebug(context);

        String appid = "";
        String appkey = "";
        try{
            appid = jobj.getString("appID");
            appkey = jobj.getString("appKey");
            placementID = jobj.getString("placementID");

        }catch (JSONException e){
            e.printStackTrace();
            return;
        }
        if (isDebug){
            Log.e(TAG,"调试模式下使用测试广告 https://docs.toponad.com/#/zh-cn/android/android_doc/android_access_doc?id=_27-%e5%b9%bf%e5%91%8a%e6%b5%8b%e8%af%95%e8%af%b4%e6%98%8e ");
            appid = "a5aa1f9deda26d";
            appkey = "4f7b9ac17decb9babec83aac078742c7";
            placementID = "b5b449f97e0b5f";
            // TopOn SDK的日志功能
            ATSDK.setNetworkLogDebug(true);
            // 验证第三方SDK集成是否正确
            ATSDK.integrationChecking(context);
        }

        final Activity mActivity = (Activity) (getContext());
        // 检查是否为欧盟用户并给出弹窗提示
        ATSDK.checkIsEuTraffic(mActivity, new NetTrafficeCallback() {
            @Override
            public void onResultCallback(boolean isEU) {
                if (isEU && ATSDK.getGDPRDataLevel(mActivity) == ATSDK.UNKNOWN) {
                    ATSDK.showGdprAuth(mActivity);
                }
                Log.i(TAG, "checkIsEuTraffic :" + isEU);
            }
            @Override
            public void onErrorCallback(String errorMsg) {
                Log.i(TAG, "checkIsEuTraffic error:" + errorMsg);
            }
        });
        ATSDK.init(context,appid,appkey);
        Log.d(TAG,"topon sdk初始化！ appid = " +appid + "  appkey = " + appkey);
        createAndLoadRewardedAd();
    }

    @Override
    public void excute(String action, String params, int callback) {
        super.excute(action, params, callback);

        switch (action){
            case "hasReward":{
                this.nativeCallbackHandler(callback, rewardAds.size()>0?"1":"0" );
                break;
            }
            case "loadAd":{
                Log.d(TAG,"加载广告");
                if (adInLoading<1){
                    createAndLoadRewardedAd();
                }
                this.nativeCallbackHandler(callback, rewardAds.size()>0?"1":"0" );
                break;
            }
            case "play":{
                final JSONObject jobj = new JSONObject();
                try{
                    jobj.put("placementID",this.placementID);
                    jobj.put("rewardId",params);
                    jobj.put("isDebug",isDebug);
                    if (rewardAds.size() < 1){
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
                    for (int i = 0; i < rewardAds.size(); i++) {
                        RewardAdVO vo = rewardAds.get(i);
                        if (vo.state == 1 && vo.rewardedAd.isAdReady()){
                            vo.rewardID = params;
                            vo.state = 2;
                            Activity mActivity = (Activity) (getContext());
                            vo.rewardedAd.show(mActivity);
                            createAndLoadRewardedAd();
                            return;
                        }
                    }
                    rewardAds.clear();
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

    private void createAndLoadRewardedAd(){
        adInLoading++;
        Log.d(TAG,"createAndLoadRewardedAd 加载广告");
        Activity mActivity = (Activity) (getContext());
        final ATRewardVideoAd mRewardVideoAd = new ATRewardVideoAd(mActivity,this.placementID);
        final int rewardIDcount = RewardAdVOCount++;
        final PluginTopon plugin = this;
        mRewardVideoAd.setAdListener(new ATRewardVideoListener() {
            @Override
            public void onRewardedVideoAdLoaded() {
                adInLoading--;
                Log.d(TAG,"onRewardedVideoAdLoaded: 广告加载成功！");
                //广告加载成功回调
                RewardAdVO rewardAdVO = new RewardAdVO();
                rewardAdVO.rewardedAd = mRewardVideoAd;
                rewardAdVO.state = 1;
                rewardAdVO.ID = rewardIDcount;
                rewardAds.add(rewardAdVO);
                plugin.nativeEventHandler("hasRewardAd", "1" );
            }

            @Override
            public void onRewardedVideoAdFailed(AdError errorCode) {
                // 广告加载失败回调
                adInLoading--;
                errorCode.printStackTrace();
                Log.e(TAG,"onRewardedVideoAdFailed:" + errorCode.getDesc());
            }

            @Override
            public void onRewardedVideoAdPlayStart(ATAdInfo entity) {
                final JSONObject jobj = new JSONObject();
                try{
                    jobj.put("placementID",plugin.placementID);
                    RewardAdVO vo = getRewardVO(rewardIDcount);
                    if (vo!=null){
                        jobj.put("rewardId",vo.rewardID);
                    }
                    jobj.put("isDebug",isDebug);
                    plugin.nativeEventHandler("open",  jobj.toString() );
                }catch (JSONException e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onRewardedVideoAdPlayEnd(ATAdInfo entity) {
            }

            @Override
            public void onRewardedVideoAdPlayFailed(AdError errorCode, ATAdInfo entity) {
                Log.e(TAG,"onRewardedVideoAdFailed:" + errorCode.getDesc());
                final JSONObject jobj = new JSONObject();
                try{
                    RewardAdVO vo = getRewardVO(rewardIDcount);
                    if (vo!=null){
                        jobj.put("rewardId",vo.rewardID);
                        vo.state = -1;
                        rewardAds.remove(vo);
                    }
                    jobj.put("placementID",plugin.placementID);
                    jobj.put("isDebug",isDebug);
                    jobj.put("code",errorCode.getCode());
                    jobj.put("json",entity.toString());
                    plugin.nativeEventHandler("close",  jobj.toString() );
                }catch (JSONException e){
                    e.printStackTrace();
                }

            }

            @Override
            public void onRewardedVideoAdClosed(ATAdInfo entity) {
                RewardAdVO vo = getRewardVO(rewardIDcount);
                //建议在此回调中调用load进行广告的加载，方便下一次广告的展示
                final JSONObject jobj = new JSONObject();
                try{
                    if (vo!=null){
                        vo.state = 4;
                        jobj.put("rewardId",vo.rewardID);
                        if (vo.sendreward){
                            jobj.put("rewardAmount",entity.getScenarioRewardName());
                            jobj.put("rewardType",entity.getScenarioRewardNumber());
                        }
                        rewardAds.remove(vo);
                    }
                    jobj.put("placementID",plugin.placementID);
                    jobj.put("isDebug",isDebug);
                    jobj.put("networkFirmId", entity.getNetworkFirmId());
                    jobj.put("adSourceId", entity.getAdsourceId());
                    jobj.put("showId", entity.getShowId());
                    jobj.put("json",entity.toString());
                    plugin.nativeEventHandler("close",  jobj.toString() );
                }catch (JSONException e){
                    e.printStackTrace();
                }
                plugin.createAndLoadRewardedAd();
            }

            @Override
            public void onReward(ATAdInfo entity) {
                final JSONObject jobj = new JSONObject();
                try{
                    RewardAdVO vo = getRewardVO(rewardIDcount);
                    if (vo!=null){
                        vo.state = 3;
                        vo.sendreward = true;
                        jobj.put("rewardId",vo.rewardID);
                    }
                    jobj.put("placementID",plugin.placementID);
                    jobj.put("isDebug",isDebug);
                    jobj.put("rewardAmount",entity.getScenarioRewardName());
                    jobj.put("rewardType",entity.getScenarioRewardNumber());
                    jobj.put("json",entity.toString());
                    plugin.nativeEventHandler("earned",  jobj.toString() );
                }catch (JSONException e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onRewardedVideoAdPlayClicked(ATAdInfo entity) {
                // 广告点击，
                final JSONObject jobj = new JSONObject();
                try{
                    RewardAdVO vo = getRewardVO(rewardIDcount);
                    if (vo!=null){
                        jobj.put("rewardId",vo.rewardID);
                        if (vo.sendreward){
                            jobj.put("rewardAmount",entity.getScenarioRewardName());
                            jobj.put("rewardType",entity.getScenarioRewardNumber());
                        }
                    }
                    jobj.put("placementID",plugin.placementID);
                    jobj.put("isDebug",isDebug);
                    jobj.put("json",entity.toString());
                    plugin.nativeEventHandler("click",  jobj.toString() );
                }catch (JSONException e){
                    e.printStackTrace();
                }
            }
        });
        mRewardVideoAd.load();
    }
    @Override
    public void onDestroy(){
        rewardAds.clear();

    }
    private class RewardAdVO{
        ATRewardVideoAd rewardedAd = null;
        int state = 0;
        String rewardID = "";
        // 是否已发送奖励
        boolean sendreward = false;
        // 0 表示初始化，1表示加载完成，2表示正在播放，3表示已发奖励，4表示播放结束或关闭,-1表示播放错误
        int ID = 0;
    }
}
