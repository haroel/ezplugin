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
//    private OkHttpClient mOkHttpClient = null;
//    private WebSocket mWebSocket = null;

//    private String ws_url = "";

    public WebSocketListener listener = null;
    private SocketActionReceiver mSocketActionReceiver = null;

    /**
     * 心跳包数据，支持pomelo
     */
    ByteString heartBeatData = null;
    //    boolean mConnected = false;
    @Override
    public void onCreate() {
        heartBeatData = ByteString.of(new byte[]{3,0,0,0});
        myBinder = new SocketServiceBinder();

        mSocketActionReceiver = new SocketActionReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(SOCKER_ACTION);
        registerReceiver(mSocketActionReceiver, filter);
        Log.d(TAG,"onCreate");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent,flags,startId);
    }
    @Override
    public void onDestroy() {
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
            try{
                if(action.equals(SOCKER_ACTION)) {
                    String sub_action = intent.getExtras().getString("ACTION","");
                    String ws_url = intent.getExtras().getString("WSURL","");
                    Log.d(TAG,"ws_url:" + ws_url +" "+ SOCKER_ACTION + " :"+sub_action);
                    SocketVO socketVO = socketHashMap.get(ws_url);
                    switch (sub_action){
                        case "connect":{
                            new InitSocketThread(ws_url).start();
//                            initSocket(ws_url);
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
                                ByteString bytearr = ByteString.of(intent.getExtras().getByteArray("DATA"));
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
                .readTimeout(3, TimeUnit.SECONDS)//设置读取超时时间
                .writeTimeout(3, TimeUnit.SECONDS)//设置写的超时时间
                .connectTimeout(3, TimeUnit.SECONDS)//设置连接超时时间
                .build();
        Log.d(TAG,"OkHttpClient start connect ws : "+ws_url);
        Request request = new Request.Builder().url(ws_url).build();
        socketVO.webSocket = socketVO.okHttpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {//开启长连接成功的回调
                super.onOpen(webSocket, response);
                if (!isOpenHeartBeat){
                    isOpenHeartBeat = true;
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
                Log.d(TAG,"onmessage text url: " +  ws_url + " size = " + text.length());

                if (listener!=null){
                    listener.onMessage(webSocket,text);
                }
//                Intent sendIntent = new Intent(SOCKER_RCV);
//                sendIntent.putExtra("action", "onmessage");
//                sendIntent.putExtra("url", ws_url);
//                sendIntent.putExtra("content", text);
//                // 发送广播，将被Activity组件中的BroadcastReceiver接收到
//                sendBroadcast(sendIntent);
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                super.onMessage(webSocket, bytes);
                Log.d(TAG,"onmessage url: " +  ws_url + " size = " + bytes.size());
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

    private long sendTime = 0L;

    /**
     * 心跳检测时间
     */
    private static final long HEART_BEAT_RATE = 3 * 1000;//每隔进行一次对长连接的心跳检测
    // 发送心跳包
    private Handler mHandler = new Handler();
    private Runnable heartBeatRunnable = new Runnable() {
        @Override
        public void run() {
            if (  System.currentTimeMillis() - sendTime >= HEART_BEAT_RATE) {
                for (HashMap.Entry<String, SocketVO> entry : socketHashMap.entrySet()) {
                    SocketVO svo = entry.getValue();
                    if (svo.getState() == 2) {
                        WebSocket mWebSocket = svo.webSocket;
                        if (heartBeatData == null) {
                            byte[] barr = new byte[1];
                            barr[0] = 1;
                            heartBeatData = ByteString.of(barr);
                        }
                        String url = svo.url;
                        Log.d(TAG, "url: " + url + " send heartbeat " + heartBeatData.size());
                        //发送一个空消息给服务器，通过发送消息的成功失败来判断长连接的连接状态
                        boolean isSuccess = mWebSocket.send(heartBeatData);
                        if (!isSuccess) {//长连接已断开
                            Log.d(TAG, "url=" + url + "heartbeat failed, the ws will be closed！");
                            svo.setState(0);
                            Intent sendIntent = new Intent(SOCKER_RCV);
                            sendIntent.putExtra("action", "heartBeatTimeout");
                            sendIntent.putExtra("url", url);
                            // 发送广播，将被Activity组件中的BroadcastReceiver接收到
                            sendBroadcast(sendIntent);
                        } else {
                            //长连接处于连接状态
                        }
                    }
                }
                sendTime = System.currentTimeMillis();
            }
            try{
                Set<String> keys = socketHashMap.keySet();
                for (String url : keys) {
                    SocketVO svo = socketHashMap.get(url);
                    if (svo.getState() == 0){
                        svo.destroy("HeartBeat timeout.");
                        socketHashMap.remove(url);
//                        Log.d(TAG,"remove ws "+url);
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            if ( socketHashMap.size() == 0){
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

        /**
         * 状态
         * 0：未连接；1表示正在连接；2表示已连上
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
