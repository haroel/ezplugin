package com.ezplugin.plugins;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.android.billingclient.api.AccountIdentifiers;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClient.SkuType;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.Purchase.PurchasesResult;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import com.ezplugin.core.PluginBase;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

public class PluginGooglePay extends PluginBase implements PurchasesUpdatedListener {
    final public static String TAG = "PluginGooglePay";

    private BillingClient billingClient = null;
    /**
     * True if billing service is connected now.
     */
    private boolean mIsServiceConnected = false;

    @Override
    public void initPlugin(Context context, JSONObject jobj) {
        if (this.isInited){
            return;
        }
        super.initPlugin(context, jobj);

        billingClient = BillingClient.newBuilder(context).enablePendingPurchases().setListener(this).build();
        startServiceConnection(new Runnable() {
            @Override
            public void run() {
// Notifying the listener that billing client is ready
// IAB is fully set up. Now, let's get an inventory of stuff we own.
                Log.d(TAG, "Setup successful. Querying inventory.");
                queryPurchases();
            }
        });
    }

    public void startServiceConnection(final Runnable executeOnSuccess) {
        mIsServiceConnected = false;
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                Log.d(TAG, "Setup finished. Response code: " + billingResult.getResponseCode());
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    mIsServiceConnected = true;
                    if (executeOnSuccess != null) {
                        executeOnSuccess.run();
                    }
                }else{
                    // Toast toast=Toast.makeText( getContext(),"Google Pay BillingSetup failed! code = " +billingResult.getResponseCode() ,Toast.LENGTH_SHORT    );
                    // toast.setGravity( Gravity.BOTTOM, 0, 0);
                    // toast.show();
                    logBillResult("onBillingSetupFinished",billingResult);
                }
            }
            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                mIsServiceConnected = false;
            }
        });
    }

    public void queryPurchases() {
        Runnable queryToExecute = new Runnable() {
            @Override
            public void run() {
                long time = System.currentTimeMillis();
                PurchasesResult purchasesResult = billingClient.queryPurchases(SkuType.INAPP);
                Log.i(TAG, "Querying purchases elapsed time: " + (System.currentTimeMillis() - time)
                        + "ms");
                List<Purchase> purchases = purchasesResult.getPurchasesList();
                if (purchases!=null){
                    for (Purchase purchase : purchases ) {
                        handlePurchase(purchase);
                    }
                }
            }
        };
        executeServiceRequest(queryToExecute);
    }

    private void executeServiceRequest(Runnable runnable) {
        if (mIsServiceConnected) {
            runnable.run();
        } else {
            // If billing service was disconnected, we try to reconnect 1 time.
            // (feel free to introduce your retry policy here).
            startServiceConnection(runnable);
        }
    }
    @Override
    public void excute(String action, String params, int callback) {
        super.excute(action, params, callback);
        switch (action){
            case "pay":{
                String[] arr = params.split("\\|");
                Log.d(TAG,params);
                this.querySkuDetailsAsync(arr[0],arr[1]);
                break;
            }
        }
    }
    /**
    *
    * skuId   : productId,对应GP后台配置的标识
    * billNo ： 自有订单号
    *
     */
    private void querySkuDetailsAsync(final String skuId ,final String billNo){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                List<String> skuList = new ArrayList<> ();
                skuList.add(skuId);
                SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                params.setSkusList(skuList).setType(SkuType.INAPP);
                billingClient.querySkuDetailsAsync(params.build(),
                        new SkuDetailsResponseListener() {
                            @Override
                            public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetailsList) {
                                // Process the result.
                                Log.d(TAG,"onSkuDetailsResponse skuDetailsList = " + skuDetailsList.toString());
                                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                    SkuDetails skuDetails = null;
                                    for (SkuDetails details : skuDetailsList) {
                                        if (skuId.equals(details.getSku())) {
                                            skuDetails = details;
                                        }
                                    }
                                    if (skuDetails!= null){
                                        paySku(skuDetails,skuId,billNo);
                                        return;
                                    }else{
                                        Log.e(TAG,"无法购买所选商品，Play Console没有该商品 skuId "+ skuId);
                                        logBillResult("onSkuDetailsResponse",billingResult);
                                        Toast toast = Toast.makeText( getContext(),"Sorry, the selected item cannot be purchased.",Toast.LENGTH_LONG);
                                        toast.setGravity( Gravity.BOTTOM, 0, 0);
                                        toast.show();
                                    }
                                }else{
                                    logBillResult("onSkuDetailsResponse",billingResult);
                                }
                                try{
                                    JSONObject jobj = new JSONObject();
                                    jobj.put("billNO",billNo);
                                    jobj.put("productID",skuId);
                                    jobj.put("code",billingResult.getResponseCode());
                                    nativeEventHandler("pay_error", jobj.toString() );
                                }catch (JSONException e){
                                    e.printStackTrace();
                                }
                            }
                        });
            }
        };
        executeServiceRequest(runnable);
    }

    public void paySku(final SkuDetails details, final String skuId, final String billNo) {
        Runnable purchaseFlowRequest = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "弹出GooglePay窗口");
                Activity mActivity = (Activity) (getContext());
                BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                        .setSkuDetails(details)
                        .setObfuscatedAccountId(billNo) // 把订单号存入用户数据中
                        .build();
                BillingResult billingResult = billingClient.launchBillingFlow( mActivity, flowParams);
                if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                    try{
                        JSONObject jobj = new JSONObject();
                        jobj.put("billNO",billNo);
                        jobj.put("productID",skuId);
                        jobj.put("code",billingResult.getResponseCode());
                        nativeEventHandler("pay_error", jobj.toString() );
                    }catch (JSONException e){
                        e.printStackTrace();
                    }
                }
            }
        };
        executeServiceRequest(purchaseFlowRequest);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {
        //当支付状态改变时调用，ok为支付成功，否则为支付失败
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            if (purchases!=null){
                for (Purchase purchase : purchases ) {
                    handlePurchase(purchase);
                }
            }
        } else {
            // Toast toast=Toast.makeText( getContext(),"Google Pay PurchasesUpdated failed! code:" +billingResult.getResponseCode() ,Toast.LENGTH_SHORT    );
            // toast.setGravity( Gravity.BOTTOM, 0, 0);
            // toast.show();

            logBillResult("onPurchasesUpdated",billingResult);
        }
    }

    private void handlePurchase(final Purchase purchase ){
        final PluginGooglePay self = this;
        String pp = purchase.getOriginalJson();
        Log.w(TAG, "handlePurchase : "+pp);
        try {
            AccountIdentifiers accIdinfo = purchase.getAccountIdentifiers();
            final String billNo = accIdinfo.getObfuscatedAccountId();
            billingClient.consumeAsync(ConsumeParams.newBuilder()
                            .setPurchaseToken(purchase.getPurchaseToken()).build(),
                    new ConsumeResponseListener() {
                        @Override
                        public void onConsumeResponse(BillingResult billingResult, String purchaseToken) {
                            try {
                                JSONObject jobj = new JSONObject(purchase.getOriginalJson());
                                jobj.put("billNO",billNo);
                                jobj.put("productID",purchase.getSku());
                                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                    Log.d(TAG,"consumeAsync 成功！"+purchase.getOriginalJson());
                                    self.nativeEventHandler("pay_success", jobj.toString() );
                                }else {
                                    // 消费失败,后面查询消费记录后再次消费，否则，就只能等待退款
                                    logBillResult("onConsumeResponse",billingResult);
                                    jobj.put("code",billingResult.getResponseCode());
                                    self.nativeEventHandler("pay_error", jobj.toString() );
                                }
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    });
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void logBillResult(String logtype,BillingResult billingResult ){
       // code https://developer.android.com/reference/com/android/billingclient/api/BillingClient.BillingResponse
        Log.e(TAG,logtype + " 错误码code = " + billingResult.getResponseCode() + " ,  msg = " + billingResult.getDebugMessage());
    }
}
