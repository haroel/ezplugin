package sockets;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import org.cocos2dx.okhttp3.OkHttpClient;
import org.cocos2dx.okhttp3.Request;
import org.cocos2dx.okhttp3.Response;
import org.cocos2dx.okhttp3.WebSocket;
import org.cocos2dx.okhttp3.WebSocketListener;
import org.cocos2dx.okio.ByteString;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 *
 * WebSocket 服务，请在AndroidManifest.xml里注册该服务
 *
 * date: 20200514
 * author: heh
 * email:  ihowe@outlook.com
 */

public class SocketService extends Service {

    private static String TAG = "SocketService";
    public static final String SOCKER_ACTION = "com.SocketService.Control";
    public static final String SOCKER_RCV = "com.SocketService.ReceiveData";

    private IBinder myBinder = null;
    public class SocketServiceBinder extends Binder {
        SocketService getService() {
            return SocketService.this;
        }
    }

    private HashMap<String,SocketVO> socketHashMap = new HashMap<String,SocketVO>();
    public WebSocketListener listener = null;
    private SocketActionReceiver mSocketActionReceiver = null;

    /**
     * 心跳包数据，支持pomelo
     */
    ByteString heartBeatData = null;

    @Override
    public void onCreate() {
        Log.d(TAG,"onCreate");
        super.onCreate();

        heartBeatData = ByteString.of(new byte[]{3,0,0,0});
        myBinder = new SocketServiceBinder();

        mSocketActionReceiver = new SocketActionReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(SOCKER_ACTION);
        registerReceiver(mSocketActionReceiver, filter);

//        Log.d(TAG,getPackageName());
//        String[] packageNames = getPackageName().split("\\.");
//        String channelId = "socketservice_" + packageNames[packageNames.length-1];
//
//        //适配8.0service
//        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        NotificationChannel mChannel = null;
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
//            mChannel = new NotificationChannel( channelId, packageNames[packageNames.length-1],
//                    NotificationManager.IMPORTANCE_LOW);
//            notificationManager.createNotificationChannel(mChannel);
//            Notification notification = new Notification.Builder(getApplicationContext(), channelId).build();
//            startForeground(1, notification);
//        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        return super.onStartCommand(intent,flags,startId);
        if (intent != null){
            Bundle bundle = intent.getExtras();
            if (bundle != null){
                Log.d(TAG,"onStartCommand" + bundle.toString());
                this.intentActionHandler(bundle);
            }
        }
        return START_STICKY;
    }
    @Override
    public void onDestroy() {
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
//            stopForeground(true);
//        }
        mHandler.removeCallbacks(heartBeatRunnable);
        isOpenHeartBeat = false;
        for (HashMap.Entry<String, SocketVO> entry : socketHashMap.entrySet()) {
            entry.getValue().destroy("destroy");
        }
        socketHashMap.clear();

        if (mSocketActionReceiver!=null){
            unregisterReceiver(mSocketActionReceiver);
            mSocketActionReceiver = null;
        }
        Log.d(TAG,"onDestroy");
        Intent sendIntent = new Intent(SOCKER_RCV);
        sendIntent.putExtra("action", "destroy");
        sendBroadcast(sendIntent);
        super.onDestroy();
    }
    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "onBind");
//        return myBinder;
        return null;
    }

    private void intentActionHandler(Bundle bundle ){
        if (bundle == null){
            Log.e(TAG,"intentActionHandler, bundle is null");
            return;
        }
        String sub_action = bundle.getString("ACTION","");
        String ws_url = bundle.getString("WSURL","");
        Log.d(TAG,"ws_url:" + ws_url +" " + " :"+sub_action);
        SocketVO socketVO = socketHashMap.get(ws_url);
        switch (sub_action){
            case "connect":{
                new InitSocketThread(ws_url).start();
                break;
            }
            case "send":{
                if (socketVO == null ){
                    Log.e(TAG,"socketVO is null  无法发送数据");
                    Intent sendIntent = new Intent(SOCKER_RCV);
                    sendIntent.putExtra("action", "onclose");
                    sendIntent.putExtra("url",ws_url);
                    sendIntent.putExtra("content", "not connected!");
                    // 发送广播，将被Activity组件中的BroadcastReceiver接收到
                    sendBroadcast(sendIntent);
                    return;
                }
                if (socketVO.webSocket !=null && socketVO.getState() == 2 ){
                    ByteString bytearr = ByteString.of(bundle.getByteArray("DATA"));
                    socketVO.webSocket.send(bytearr);
                }else{
                    Log.e(TAG,"socket can not work！ wsurl :"+ws_url);
                }
                break;

            }
            case "close":{
                if (socketVO != null){
                    socketVO.destroy("close connection.");
                }
                socketHashMap.remove(ws_url);
                break;
            }
        }
    }

    private class SocketActionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try{
                String action = intent.getAction();
                if(action.equals(SOCKER_ACTION)) {
                    intentActionHandler(intent.getExtras());
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    /**
     * 启一个线程来创建ws
     */
    class InitSocketThread extends Thread {
        String id = "";
        InitSocketThread(String tid){
            this.id = tid;
        }
        @Override
        public void run() {
            super.run();
            try {
                initSocket(this.id);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    // 初始化socket
    private void initSocket(final String ws_url) throws UnknownHostException, IOException {
        if (socketHashMap.containsKey(ws_url)){
            socketHashMap.get(ws_url).destroy("reconnect.");
            socketHashMap.remove(ws_url);
        }
        SocketVO socketVO = new SocketVO(ws_url);
        socketHashMap.put(ws_url,socketVO);
        socketVO.setState(1);
        socketVO.okHttpClient = new OkHttpClient.Builder()
                .readTimeout(5, TimeUnit.SECONDS)//设置读取超时时间
                .writeTimeout(5, TimeUnit.SECONDS)//设置写的超时时间
                .connectTimeout(5, TimeUnit.SECONDS)//设置连接超时时间
                .build();
        Log.d(TAG,"OkHttpClient start connect ws : "+ws_url);
        Request request = new Request.Builder().url(ws_url).build();
        socketVO.webSocket = socketVO.okHttpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {//开启长连接成功的回调
                super.onOpen(webSocket, response);
                if (!isOpenHeartBeat){
                    isOpenHeartBeat = true;
                    if (heartBeatData == null) {
                        heartBeatData = ByteString.of(new byte[]{1});
                    }
                    mHandler.postDelayed(heartBeatRunnable, HEART_BEAT_RATE);//开启心跳检测
                }
                Log.d(TAG,"onopen url:" + ws_url + " msg:"+ response.message());
                SocketVO svo = socketHashMap.get(ws_url);
                if (svo!=null){
                    svo.webSocket = webSocket;
                    svo.setState(2);
                }
                if (listener!=null){
                    listener.onOpen(webSocket,response);
                }
                Intent sendIntent = new Intent(SOCKER_RCV);
                sendIntent.putExtra("action", "onopen");
                sendIntent.putExtra("url", ws_url);
                sendIntent.putExtra("content", response.message());
                // 发送广播，将被Activity组件中的BroadcastReceiver接收到
                sendBroadcast(sendIntent);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {//接收消息的回调
                super.onMessage(webSocket, text);
                //收到服务器端传过来的消息text
//                Log.d(TAG,"onmessage text url: " +  ws_url + " size:" + text.length());
                SocketVO svo = socketHashMap.get(ws_url);
                if (svo!=null){
                    svo.receiveTime = System.currentTimeMillis();;
                }
                if (listener!=null){
                    listener.onMessage(webSocket,text);
                }
                Intent sendIntent = new Intent(SOCKER_RCV);
                sendIntent.putExtra("action", "onmessagetext");
                sendIntent.putExtra("url", ws_url);
                sendIntent.putExtra("content", text);
                // 发送广播，将被Activity组件中的BroadcastReceiver接收到
                sendBroadcast(sendIntent);
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                super.onMessage(webSocket, bytes);
//                Log.d(TAG,"onmessage url: " +  ws_url + " size:" + bytes.size());
                SocketVO svo = socketHashMap.get(ws_url);
                if (svo!=null){
                    svo.receiveTime = System.currentTimeMillis();;
                }

                if (bytes.hashCode() == heartBeatData.hashCode()){
//                    Log.d(TAG,"心跳协议回复，不做任何处理");
                    return;
                }

                if (listener!=null){
                    listener.onMessage(webSocket,bytes);
                }
                Intent sendIntent = new Intent(SOCKER_RCV);
                sendIntent.putExtra("action", "onmessage");
                sendIntent.putExtra("url", ws_url);
                sendIntent.putExtra("content", bytes.toByteArray());
                // 发送广播，将被Activity组件中的BroadcastReceiver接收到
                sendBroadcast(sendIntent);
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                super.onClosing(webSocket, code, reason);
                Log.d(TAG,"onClosing url: " +  ws_url + " code = " + code);
                SocketVO socketVO = socketHashMap.get(ws_url);
                if (socketVO != null){
                    socketVO.setState(-1);
                }
                if (listener!=null){
                    listener.onClosing(webSocket, code, reason);
                }
                Intent sendIntent = new Intent(SOCKER_RCV);
                sendIntent.putExtra("action", "onclosing");
                sendIntent.putExtra("url", ws_url);
                sendIntent.putExtra("content", reason);
                sendIntent.putExtra("code", code);
                // 发送广播，将被Activity组件中的BroadcastReceiver接收到
                sendBroadcast(sendIntent);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                super.onClosed(webSocket, code, reason);
                Log.w(TAG,"onclose url: "+ ws_url+" code = " + code + " reason = "+ reason);
                SocketVO socketVO = socketHashMap.get(ws_url);
                if (socketVO != null){
                    socketVO.destroy("Connection closed");
                    socketHashMap.remove(ws_url);
                }
                if (listener!=null){
                    listener.onClosed(webSocket,code,reason);
                }
                Intent sendIntent = new Intent(SOCKER_RCV);
                sendIntent.putExtra("action", "onclose");
                sendIntent.putExtra("url", ws_url);
                sendIntent.putExtra("content", reason);
                sendIntent.putExtra("code", code);
                // 发送广播，将被Activity组件中的BroadcastReceiver接收到
                sendBroadcast(sendIntent);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {//长连接连接失败的回调
                super.onFailure(webSocket, t, response);
                if (response != null){
                    Log.e(TAG,"onerror url: " + ws_url + "  msg:" + response.message());
                }else{
                    Log.e(TAG,"onerror url: " + ws_url + "  msg:" + t.getMessage());
                    t.printStackTrace();
                }
                if (listener!=null){
                    listener.onFailure(webSocket,t,response);
                }
                String msg = "";
                if (response != null){
                    msg = response.message();
                }else{
                    msg = t.getMessage();
                    t.printStackTrace();
                }

                Intent sendIntent = new Intent(SOCKER_RCV);
                sendIntent.putExtra("action", "onerror");
                sendIntent.putExtra("url", ws_url);
                sendIntent.putExtra("content", msg);
                // 发送广播，将被Activity组件中的BroadcastReceiver接收到
                sendBroadcast(sendIntent);
            }
        });
        socketVO.okHttpClient.dispatcher().executorService().shutdown();
    }

    public boolean isOpenHeartBeat = false;

    /**
     * 心跳检测时间
     */
    private static final long HEART_BEAT_RATE = 2000;//每隔 s进行一次对长连接的心跳检测
    // 发送心跳包
    private Handler mHandler = new Handler();
    private Runnable heartBeatRunnable = new Runnable() {
        @Override
        public void run() {
            long nowtime = System.currentTimeMillis();
            for (HashMap.Entry<String, SocketVO> entry : socketHashMap.entrySet()) {
                SocketVO svo = entry.getValue();
                String url = svo.url;
                boolean isTimeout = false;
                if (svo.getState() == 2 ) {
                    if (nowtime -svo.receiveTime >= 3 * HEART_BEAT_RATE){
                        isTimeout = true;
                        Log.w(TAG, "url: " + url + " 长时间未收到消息，主动断开ws！ ");
                    }else if (nowtime - svo.sendTime >= HEART_BEAT_RATE) {
//                        Log.d(TAG, "url: " + url + " send heartbeat " + heartBeatData.size());
                        //发送一个空消息给服务器，通过发送消息的成功失败来判断长连接的连接状态
                        boolean isSuccess = svo.webSocket.send(heartBeatData);
                        if (!isSuccess) {//长连接已断开
                            isTimeout = true;
                        } else {
                            //长连接处于连接状态
                        }
                        svo.sendTime = nowtime;
                    }
                }
                if (isTimeout){
                    Log.w(TAG, "url=" + url + "heartbeat failed, the ws will be closed！");
                    svo.setState(0);
                    Intent sendIntent = new Intent(SOCKER_RCV);
                    sendIntent.putExtra("action", "heartBeatTimeout");
                    sendIntent.putExtra("url", url);
                    // 发送广播，将被Activity组件中的BroadcastReceiver接收到
                    sendBroadcast(sendIntent);
                }
            }
            try{
                Set<String> keys = socketHashMap.keySet();
                for (String url : keys) {
                    SocketVO svo = socketHashMap.get(url);
                    if (svo.getState() <= 0){
                        svo.destroy("HeartBeat timeout.");
                        socketHashMap.remove(url);
//                        Log.d(TAG,"remove ws "+url);
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            if ( socketHashMap.size() == 0){
                Log.d(TAG,"所有ws连接都已中断，停止心跳循环");
                mHandler.removeCallbacks(heartBeatRunnable);
                isOpenHeartBeat = false;
                return;
            }
            mHandler.postDelayed(this, HEART_BEAT_RATE);//每隔一定的时间，对长连接进行一次心跳检测
        }
    };

    private class SocketVO{

        public OkHttpClient okHttpClient = null;

        public WebSocket webSocket = null;

        public String url = "";
        SocketVO(String wsurl){
            url = wsurl;
            setState(1);
        }

        private long sendTime = 0L;
        private long receiveTime = 0L;
        /**
         * 状态
         * -1表示关闭，0：未连接；1表示正在连接；2表示已连上
         */
        private int _state = 0;

        public  int getState(){
            return _state;
        }

        public void setState(int val){
            if (this._state == val){
                return;
            }
            this._state = val;
//            Log.d("SocketVO", "state:" +val + " url:"+url);
        }

        public void destroy(String reason){
            setState(0);
            try{
                if (webSocket!=null){
                    webSocket.cancel();//取消掉以前的长连接
                    webSocket.close(1000,reason);
                    webSocket = null;
                }
                if (okHttpClient!=null){
                    okHttpClient.dispatcher().cancelAll();
                    okHttpClient.dispatcher().executorService().shutdown();
                    okHttpClient.cache();
                    okHttpClient = null;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            Log.d("SocketVO","url:"+url + " destroy!");
        }
    }
}
