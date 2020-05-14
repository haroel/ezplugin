package sockets;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
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
    public static final String SOCKER_ACTION = "com.Socket.Control";
    public static final String SOCKER_RCV = "com.Socket.ReceiveData";

    private IBinder myBinder = null;
    public class SocketServiceBinder extends Binder {
        SocketService getService() {
            return SocketService.this;
        }
    }

    private OkHttpClient mOkHttpClient = null;
    private WebSocket mWebSocket = null;

    private String ws_url = "";

    public WebSocketListener listener = null;
    private SocketActionReceiver mSocketActionReceiver = null;

    /**
     * 心跳包数据，支持pomelo
     */
    ByteString heartBeatData = null;
    boolean mConnected = false;
    @Override
    public void onCreate() {
        heartBeatData = ByteString.of(new byte[]{3,0,0,0});
        myBinder = new SocketServiceBinder();

        mSocketActionReceiver = new SocketActionReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(SOCKER_ACTION);
        registerReceiver(mSocketActionReceiver, filter);
        Log.d(TAG,"SocketService onCreate");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent,flags,startId);
    }
    @Override
    public void onDestroy() {
        mConnected = false;
        if (mWebSocket != null) {
            mWebSocket.cancel();
            mWebSocket.close(1000, null);
            mWebSocket = null;
        }
        if (mSocketActionReceiver!=null){
            unregisterReceiver(mSocketActionReceiver);
            mSocketActionReceiver = null;
        }
        Log.d(TAG,"SocketService onDestroy");
        super.onDestroy();
    }
    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "onBind");
//        return myBinder;
        return null;
    }

    /**
//     * 创建ws连接
//     * @param wsurl
//     */
//    public void createWSocket(String wsurl){
//        ws_url = wsurl;
//        if (mWebSocket != null) {
//            mWebSocket.close(1000, null);
//            mWebSocket = null;
//        }
//        new InitSocketThread().start();
//    }
//
//    /**
//     * send data
//     * @param bytearr
//     */
//    public void send(ByteString bytearr){
//        if (mWebSocket !=null){
//            mWebSocket.send(bytearr);
//        }
//    }
//
//    /**
//     * close ws
//     */
//    public void disconnect(){
//        if (mWebSocket != null) {
//            mWebSocket.close(1000, null);
//            mWebSocket = null;
//        }
//    }
    private class SocketActionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG,"收到消息BroadcastReceiver : "+action);
            try{
                if(action.equals(SOCKER_ACTION)) {
                    String sub_action = intent.getExtras().getString("ACTION");
                    Log.d(TAG," SOCKER_ACTION: "+sub_action);
                    switch (sub_action){
                        case "connect":{
                            ws_url = intent.getExtras().getString("WSURL");
                            if (mWebSocket != null) {
                                mWebSocket.cancel();
                                mWebSocket.close(1000, null);
                                mWebSocket = null;
                            }
                            new InitSocketThread().start();
                            break;
                        }
                        case "send":{
                            if (!mConnected){
                                Log.e(TAG,"ws 未连接，无法发送数据");
                                return;
                            }
                            if (mWebSocket !=null){
                                ByteString bytearr = ByteString.of(intent.getExtras().getByteArray("DATA"));
                                mWebSocket.send(bytearr);
                            }
                            break;

                        }
                        case "close":{
                            mConnected = false;
                            if (mWebSocket != null) {
                                mWebSocket.cancel();
                                mWebSocket.close(1000, null);
                                mWebSocket = null;
                            }
                            break;
                        }
//                        case "sendHeartBeat":{
//                            heartBeatData = ByteString.of(intent.getExtras().getByteArray("DATA"));
//                            Log.d(TAG,"心跳数据长度 : "+heartBeatData.size());
//                            break;
//                        }
                    }
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
        @Override
        public void run() {
            super.run();
            try {
                initSocket();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    // 初始化socket
    private void initSocket() throws UnknownHostException, IOException {
        mConnected = false;
        if (mWebSocket != null){
            mWebSocket.cancel();
            mWebSocket.close(1000,null);
            mWebSocket = null;
        }
        if (mOkHttpClient != null){
            mOkHttpClient.dispatcher().cancelAll();
            mOkHttpClient.dispatcher().executorService().shutdown();
            mOkHttpClient.cache();
            mOkHttpClient = null;
        }
        mOkHttpClient = new OkHttpClient.Builder()
                .readTimeout(3, TimeUnit.SECONDS)//设置读取超时时间
                .writeTimeout(3, TimeUnit.SECONDS)//设置写的超时时间
                .connectTimeout(3, TimeUnit.SECONDS)//设置连接超时时间
                .build();
        Log.d(TAG,"OkHttpClient 开始连接ws : "+ws_url);
        Request request = new Request.Builder().url(ws_url).build();
        mOkHttpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {//开启长连接成功的回调
                super.onOpen(webSocket, response);
                mWebSocket = webSocket;
                mConnected = true;
                mHandler.postDelayed(heartBeatRunnable, HEART_BEAT_RATE);//开启心跳检测
                if (listener!=null){
                    listener.onOpen(webSocket,response);
                }
                Log.d("SocketServiceListener","onopen:" + response.message());

                Intent sendIntent = new Intent(SOCKER_RCV);
                sendIntent.putExtra("action", "onopen");
                sendIntent.putExtra("content", response.message());
                // 发送广播，将被Activity组件中的BroadcastReceiver接收到
                sendBroadcast(sendIntent);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {//接收消息的回调
                super.onMessage(webSocket, text);
                if (mWebSocket == null || mWebSocket != webSocket){
                    return;
                }
                //收到服务器端传过来的消息text
                if (listener!=null){
                    listener.onMessage(webSocket,text);
                }
                Intent sendIntent = new Intent(SOCKER_RCV);
                sendIntent.putExtra("action", "onmessage");
                sendIntent.putExtra("content", text);
                // 发送广播，将被Activity组件中的BroadcastReceiver接收到
                sendBroadcast(sendIntent);
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                super.onMessage(webSocket, bytes);
                if (mWebSocket == null || mWebSocket != webSocket){
                    return;
                }
                if (listener!=null){
                    listener.onMessage(webSocket,bytes);
                }
                Log.d("SocketServiceListener","onmessage, size = " + bytes.size());

                Intent sendIntent = new Intent(SOCKER_RCV);
                sendIntent.putExtra("action", "onmessage");
                sendIntent.putExtra("content", bytes.toByteArray());
                // 发送广播，将被Activity组件中的BroadcastReceiver接收到
                sendBroadcast(sendIntent);
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                super.onClosing(webSocket, code, reason);
                if (mWebSocket == null || mWebSocket != webSocket){
                    return;
                }
                mConnected = false;
                if (listener!=null){
                    listener.onClosing(webSocket, code, reason);
                }

                Intent sendIntent = new Intent(SOCKER_RCV);
                sendIntent.putExtra("action", "onclosing");
                sendIntent.putExtra("content", reason);
                sendIntent.putExtra("code", code);
                // 发送广播，将被Activity组件中的BroadcastReceiver接收到
                sendBroadcast(sendIntent);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                super.onClosed(webSocket, code, reason);
                if (mWebSocket == null || mWebSocket != webSocket){
                    return;
                }
                mConnected = false;
                if (listener!=null){
                    listener.onClosed(webSocket,code,reason);
                }
                Log.w("SocketServiceListener","onclose, code = " + code + " reason = "+ reason);

                Intent sendIntent = new Intent(SOCKER_RCV);
                sendIntent.putExtra("action", "onclose");
                sendIntent.putExtra("content", reason);
                sendIntent.putExtra("code", code);
                // 发送广播，将被Activity组件中的BroadcastReceiver接收到
                sendBroadcast(sendIntent);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {//长连接连接失败的回调
                super.onFailure(webSocket, t, response);
                if (listener!=null){
                    listener.onFailure(webSocket,t,response);
                }
                if (response != null){
                    Log.e("SocketServiceListener","onerror:" + response.message());
                }else{
                    Log.e("SocketServiceListener","onerror:" + t.getMessage());
                    t.printStackTrace();
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
                sendIntent.putExtra("content", msg);
                // 发送广播，将被Activity组件中的BroadcastReceiver接收到
                sendBroadcast(sendIntent);
            }
        });
        mOkHttpClient.dispatcher().executorService().shutdown();
    }

    /**
     * 心跳检测时间
     */
    private static final long HEART_BEAT_RATE = 3 * 1000;//每隔进行一次对长连接的心跳检测
    private long sendTime = 0L;
    // 发送心跳包
    private Handler mHandler = new Handler();
    private Runnable heartBeatRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mConnected){
                Log.d(TAG,"连接已中断，停止发送 heartBeatRunnable");
                return;
            }
            if (System.currentTimeMillis() - sendTime >= HEART_BEAT_RATE) {
                if (mWebSocket != null){
                    if (heartBeatData == null){
                        byte[] barr = new byte[1];
                        barr[0] = 1;
                        heartBeatData = ByteString.of(barr);
                    }
                    Log.d(TAG,"send heartbeat "+heartBeatData.size());
                    boolean isSuccess = mWebSocket.send(heartBeatData);//发送一个空消息给服务器，通过发送消息的成功失败来判断长连接的连接状态
                    if (!isSuccess) {//长连接已断开
                        mHandler.removeCallbacks(heartBeatRunnable);
                        mWebSocket.cancel();//取消掉以前的长连接
                        mWebSocket.close(1000,null);
                        mWebSocket = null;
                        Log.d(TAG,"heartbeat failed, the ws will be closed！");

                        Intent sendIntent = new Intent(SOCKER_RCV);
                        sendIntent.putExtra("action", "heartBeatTimeout");
                        // 发送广播，将被Activity组件中的BroadcastReceiver接收到
                        sendBroadcast(sendIntent);
                        return;

                    } else {//长连接处于连接状态

                    }
                }
                sendTime = System.currentTimeMillis();
            }
            mHandler.postDelayed(this, HEART_BEAT_RATE);//每隔一定的时间，对长连接进行一次心跳检测
        }
    };

}
