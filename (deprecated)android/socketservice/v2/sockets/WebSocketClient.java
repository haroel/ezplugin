package sockets;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import org.cocos2dx.lib.Cocos2dxJavascriptJavaBridge;
import static org.cocos2dx.lib.Cocos2dxHelper.runOnGLThread;
/**
 *
 * WebSocket 对外接口组件
 *
 * date: 20200514
 * author: heh
 * email:  ihowe@outlook.com
 */


public class WebSocketClient {
    private static String TAG = "WebSocketClient";

    private static android.os.Handler mHandler = new android.os.Handler();

    Context mContext = null;
    SocketReceiver socketReceiver = null;
    private static WebSocketClient _instance = null;
    public static WebSocketClient getInstance(){
        if (WebSocketClient._instance == null){
            WebSocketClient._instance = new WebSocketClient();
        }
        return WebSocketClient._instance;
    }

    public void init(Context contex){
        mContext = contex;
        socketReceiver = new SocketReceiver();
        IntentFilter socketIntentFilter = new IntentFilter();
        socketIntentFilter.addAction(SocketService.SOCKER_RCV);
        contex.registerReceiver(socketReceiver,socketIntentFilter);

        Intent socketIntent = new Intent();
        socketIntent.setClass(contex, SocketService.class);
        contex.startService(socketIntent);
        Log.d(TAG,"WebSocketClient inited!");
    }

    public void destroy(){
        if (mContext!=null){
            Intent intentFour = new Intent(mContext, SocketService.class);
            mContext.stopService(intentFour);
            mContext.unregisterReceiver(socketReceiver);
            socketReceiver = null;
        }
        Log.d(TAG,"WebSocketClient destroy!");
    }

    public static void sendPacket(String _strdata){
        final String strdata = _strdata;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Context mContext = _instance.mContext;
                if (mContext == null){
                    return;
                }
                String[] arr = strdata.split("\\|");
                int len = arr.length;
                byte[] barr = new byte[len];
                for (int i=0;i<len;i++){
                    barr[i] = (byte)(Integer.parseInt(arr[i]));
                }
                Intent sendIntent = new Intent(SocketService.SOCKER_ACTION);
                sendIntent.putExtra("ACTION", "send");
                sendIntent.putExtra("DATA", barr );
                mContext.sendBroadcast(sendIntent);
            }
        });
    }

    public static void disconnect(){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Context mContext = _instance.mContext;
                if (mContext == null){
                    return;
                }
                Intent sendIntent = new Intent(SocketService.SOCKER_ACTION);
                sendIntent.putExtra("ACTION", "close");
                mContext.sendBroadcast(sendIntent);
            }
        });
    }

    public static void createWS(final String wsurl){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Context mContext = _instance.mContext;
                if (mContext == null){
                    return;
                }
                Intent sendIntent = new Intent(SocketService.SOCKER_ACTION);
                sendIntent.putExtra("ACTION", "connect");
                sendIntent.putExtra("WSURL", wsurl);
                mContext.sendBroadcast(sendIntent);
            }
        });
    }

    public class SocketReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            try{
                if(action.equals(SocketService.SOCKER_RCV)) {
                    Bundle bundle = intent.getExtras();
                    String dataaction = bundle.getString("action");
                    switch (dataaction){
                        case "onopen":{
                            final String msg = bundle.getString("content");
                            runOnGLThread(new Runnable() {
                                @Override
                                public void run() {
                                    String _safeStr = String.format("wshandler.event('onopen','%s');", msg);
                                    Cocos2dxJavascriptJavaBridge.evalString(_safeStr);
                                }
                            });
                            break;
                        }
                        case "onmessage":{
                            byte[] bytes =bundle.getByteArray("content");
                            int len = bytes.length;
                            StringBuilder sb = new StringBuilder();
                            for (int i=0;i<len;i++){
                                sb.append(bytes[i]);
                                if (i < (len-1)){
                                    sb.append("|");
                                }
                            }
                            final String msg = sb.toString();
                            runOnGLThread(new Runnable() {
                                @Override
                                public void run() {
                                    String _safeStr = String.format("wshandler.event('onmessage','%s');", msg);
                                    Cocos2dxJavascriptJavaBridge.evalString(_safeStr);
                                }
                            });
                            break;
                        }
                        case "onclose":{
                            final String msg = bundle.getString("content");
                            runOnGLThread(new Runnable() {
                                @Override
                                public void run() {
                                    String _safeStr = String.format("wshandler.event('onclose','%s');", msg);
                                    Cocos2dxJavascriptJavaBridge.evalString(_safeStr);
                                }
                            });
                            break;
                        }
                        case "onerror":{
                            final String msg = bundle.getString("content");
                            runOnGLThread(new Runnable() {
                                @Override
                                public void run() {
                                    String _safeStr = String.format("wshandler.event('onerror','%s');", msg);
                                    Cocos2dxJavascriptJavaBridge.evalString(_safeStr);
                                }
                            });
                            break;
                        }
                        case "heartBeatTimeout":{
                            runOnGLThread(new Runnable() {
                                @Override
                                public void run() {
                                    Cocos2dxJavascriptJavaBridge.evalString("wshandler.event('heartBeatTimeout','');");
                                }
                            });
                            break;
                        }
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}