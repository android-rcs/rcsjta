package com.orangelabs.rcs.watch;

import android.os.Message;
import android.util.Log;

import com.orangelabs.rcs.watch.Main.WatchHandler;
import com.samsung.android.sdk.accessory.SASocket;

/**
 * Watch consumer connection
 *  
 * @author Gilles LeBrun
 */
public class WatchConsumerConnection extends SASocket {
	private static final String TAG = "joynwatch";

    public WatchConsumerConnection() {
    	super(WatchConsumerConnection.class.getName());
    }

    public void onError(int arg0, String arg1, int arg2) {
    	Log.e(TAG, "onError:" + arg1);
    }

    protected void onServiceConnectionLost(int errorCode) {
    	Log.e(TAG, "onServiceConectionLost error:" + errorCode);
    }

    public void onReceive(int channelId, byte[] data) {
    	final String strToUpdateUI = new String(data);
        WatchHandler handler = Main.getWatchHandler();
        Message msg = handler.obtainMessage(Main.REMOTE_CMD);
        msg.obj = strToUpdateUI;
        handler.sendMessage(msg);
    }
}