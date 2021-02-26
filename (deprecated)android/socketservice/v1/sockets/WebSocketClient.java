package sockets;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;


import androidx.annotation.Nullable;

import org.cocos2dx.lib.Cocos2dxJavascriptJavaBridge;
import org.cocos2dx.okhttp3.Response;
import org.cocos2dx.okhttp3.WebSocket;
import org.cocos2dx.okhttp3.WebSocketListener;
import org.cocos2dx.okio.ByteString;
import org.json.JSONException;
import org.json.JSONObject;

import static org.cocos2dx.lib.Cocos2dxHelper.runOnGLThread;


public class WebSocketClient {
    private static String TAG = "WebSocketClient";

    private static android.os.Handler mHandler = new android.os.Handler();
    private boolean mIsBound;

    SocketService mBoundService = null;

    Context mContext = null;

    private static WebSocketClient _instance = null;
    public static WebSocketClient getInstance(){
        if (WebSocketClient._instance == null){
            WebSocketClient._instance = new WebSocketClient();
        }
        return WebSocketClient._instance;
    }

    public void init(Context contex){
        mContext = contex;
        doBindService();
    }

    private WebSocketListener listener = new WebSocketListener(){
        @Override
        public void onOpen(WebSocket webSocket, Response response){
            Log.d("SocketServiceListener","onopen:" + response.message());
            runOnGLThread(new Runnable() {
                @Override
                public void run() {
                    Cocos2dxJavascriptJavaBridge.evalString("wshandler.event('onopen','');");
                }
            });
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString _bytes){
            Log.d("SocketServiceListener","onmessage, size = " + _bytes.size());
            final ByteString bytes = _bytes;
            runOnGLThread(new Runnable() {
                @Override
                public void run() {
                    StringBuilder sb = new StringBuilder();
                    for (int i=0;i<bytes.size();i++){
                        sb.append(bytes.getByte(i));
                        if (i < (bytes.size()-1)){
                            sb.append("|");
                        }
                    }
                    String _safeStr = String.format("wshandler.event('onmessage','%s');", sb.toString());
                    Cocos2dxJavascriptJavaBridge.evalString(_safeStr);
                }
            });
        }
        @Override
        public void onClosed(WebSocket webSocket, int code, String reason){
            Log.w("SocketServiceListener","onclose, code = " + code + " reason = "+ reason);
            final int ccode = code;
            final String creason = reason;
            runOnGLThread(new Runnable() {
                @Override
                public void run() {
                    try{
                        JSONObject jj = new JSONObject();
                        jj.put("code",ccode);
                        jj.put("reson",creason);
                        String _safeStr = String.format("wshandler.event('onclose','%s');", jj.toString());
                        Cocos2dxJavascriptJavaBridge.evalString(_safeStr);
                    }catch (JSONException e){
                        e.printStackTrace();
                    }
                }
            });
        }
        @Override
        public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response){
            if (response != null){
                Log.e("SocketServiceListener","onerror:" + response.message());
            }else{
                Log.e("SocketServiceListener","onerror:" + t.getMessage());
                t.printStackTrace();
            }
            runOnGLThread(new Runnable() {
                @Override
                public void run() {
                    Cocos2dxJavascriptJavaBridge.evalString("wshandler.event('onerror','');");
                }
            });
        }
    };

    public static void sendPacket(String strdata){
        String[] arr = strdata.split("\\|");
        int len = arr.length;
        byte[] barr = new byte[len];
        for (int i=0;i<len;i++){
            barr[i] = (byte)(Integer.parseInt(arr[i]));
        }
        final ByteString bytearr = ByteString.of(barr);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if ( _instance.mBoundService != null){
                    _instance.mBoundService.send(bytearr);
                }
            }
        });
    }

    public static void  disconnect(){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if ( _instance.mBoundService != null){
                    _instance.mBoundService.disconnect();
                }
            }
        });
    }

    public static void createWS(final String wsurl){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if ( _instance.mBoundService != null){
                    _instance.mBoundService.createWSocket(wsurl);
                }
            }
        });
    }
    /**
     * Call this to connect your activity to your service
     */
    private void doBindService() {
        // Establish a connection with our service
        mContext.bindService(new Intent(mContext, SocketService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }
    /**
     * When you have finished with the service call this method to stop it
     * releasing your connection and resources
     */
    public void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            mContext.unbindService(mConnection);
            mIsBound = false;
        }
    }

    /**
     * When you attempt to connect to the service, this connection will be called with the result.
     * If we have successfully connected we instantiate our service object so that we can call methods on it.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with our service has been established,
            // giving us the service object we can use to interact with our service.
            mBoundService = ((SocketService.SocketServiceBinder) service).getService();
            mBoundService.listener = listener;
            Log.d(TAG,"ServiceConnection onServiceConnected");
        }

        public void onServiceDisconnected(ComponentName className) {
            mBoundService = null;
            Log.d(TAG,"ServiceConnection onServiceDisconnected");
        }
    };

}
