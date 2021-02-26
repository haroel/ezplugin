//package com.ezplugin.services;
//
//import android.util.Log;
//
//import androidx.annotation.NonNull;
//
//import com.google.firebase.messaging.FirebaseMessagingService;
//import com.google.firebase.messaging.RemoteMessage;
//
//public class GameFirebaseMessagingService extends FirebaseMessagingService {
//
//    private  static String TAG = "GameFirebaseMessagingService";
//    @Override
//    public void onNewToken(String token) {
//        Log.d(TAG, "Refreshed token: " + token);
//
//        // If you want to send messages to this application instance or
//        // manage this apps subscriptions on the server side, send the
//        // Instance ID token to your app server.
////        sendRegistrationToServer(token);
//    }
//    @Override
//    public void onMessageReceived(@NonNull RemoteMessage remoteMessage){
//        // 当APP未被kill时，推送消息在这里进行处理
//        Log.d(TAG, "From: " + remoteMessage.getFrom());
//
//        // Check if message contains a data payload.
//        if (remoteMessage.getData().size() > 0) {
//            // 推送中所含的键值对都可以在这里进行获取
//            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
//        }
//
//        // Check if message contains a notification payload.
//        if (remoteMessage.getNotification() != null) {
//            // 如果推送消息仅为通知消息，这里将获取通知消息的内容
//            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
//        }
//    }
//}
