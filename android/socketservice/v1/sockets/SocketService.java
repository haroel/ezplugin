package sockets;

import android.app.Service;
import android.content.Intent;
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
import java.util.concurrent.TimeUnit;

public class SocketService extends Service {

    private static String TAG = "SocketService";
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
    @Override
    public void onCreate() {
        myBinder = new SocketServiceBinder();
        mOkHttpClient = new OkHttpClient.Builder()
                .readTimeout(3, TimeUnit.SECONDS)//设置读取超时时间
                .writeTimeout(3, TimeUnit.SECONDS)//设置写的超时时间
                .connectTimeout(3, TimeUnit.SECONDS)//设置连接超时时间
                .build();
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent,flags,startId);
    }
    @Override
    public void onDestroy() {
        if (mWebSocket != null) {
            mWebSocket.close(1000, null);
            mWebSocket = null;
        }
        super.onDestroy();
    }
    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "onBind");
        return myBinder;
    }

    /**
     * 创建ws连接
     * @param wsurl
     */
    public void createWSocket(String wsurl){
        ws_url = wsurl;
        if (mWebSocket != null) {
            mWebSocket.close(1000, null);
            mWebSocket = null;
        }
        new InitSocketThread().start();
    }

    /**
     * send data
     * @param bytearr
     */
    public void send(ByteString bytearr){
        if (mWebSocket !=null){
            mWebSocket.send(bytearr);
        }
    }

    /**
     * close ws
     */
    public void disconnect(){
        if (mWebSocket != null) {
            mWebSocket.close(1000, null);
            mWebSocket = null;
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
        Request request = new Request.Builder().url(ws_url).build();
        mOkHttpClient.connectionPool().evictAll();
        mOkHttpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {//开启长连接成功的回调
                super.onOpen(webSocket, response);
                mWebSocket = webSocket;
                mHandler.postDelayed(heartBeatRunnable, HEART_BEAT_RATE);//开启心跳检测
                if (listener!=null){
                    listener.onOpen(webSocket,response);
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {//接收消息的回调
                super.onMessage(webSocket, text);
                //收到服务器端传过来的消息text
                if (listener!=null){
                    listener.onMessage(webSocket,text);
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                super.onMessage(webSocket, bytes);
                if (listener!=null){
                    listener.onMessage(webSocket,bytes);
                }
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                super.onClosing(webSocket, code, reason);
                if (listener!=null){
                    listener.onClosing(webSocket, code, reason);
                }
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                super.onClosed(webSocket, code, reason);
                if (listener!=null){
                    listener.onClosed(webSocket,code,reason);
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {//长连接连接失败的回调
                super.onFailure(webSocket, t, response);
                if (listener!=null){
                    listener.onFailure(webSocket,t,response);
                }
            }
        });
        mOkHttpClient.dispatcher().executorService().shutdown();
    }

    /**
     * 心跳检测时间
     */
    private static final long HEART_BEAT_RATE = 3 * 1000;//每隔15秒进行一次对长连接的心跳检测
    private long sendTime = 0L;
    // 发送心跳包
    private Handler mHandler = new Handler();
    private Runnable heartBeatRunnable = new Runnable() {
        @Override
        public void run() {
            if (System.currentTimeMillis() - sendTime >= HEART_BEAT_RATE) {
                if (mWebSocket != null){
                    byte[] barr = new byte[1];
                    barr[0] = 1;
                    ByteString bys = ByteString.of(barr);
                    boolean isSuccess = mWebSocket.send(bys);//发送一个空消息给服务器，通过发送消息的成功失败来判断长连接的连接状态
                    if (!isSuccess) {//长连接已断开
                        mHandler.removeCallbacks(heartBeatRunnable);
                        mWebSocket.cancel();//取消掉以前的长连接
                        mWebSocket.close(1000,null);
                        mWebSocket = null;
                    } else {//长连接处于连接状态

                    }
                }
                sendTime = System.currentTimeMillis();
            }
            mHandler.postDelayed(this, HEART_BEAT_RATE);//每隔一定的时间，对长连接进行一次心跳检测
        }
    };

}
