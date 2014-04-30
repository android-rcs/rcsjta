package com.orangelabs.rcs.watch;

import java.io.IOException;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

import com.samsung.android.sdk.accessory.SAAgent;
import com.samsung.android.sdk.accessory.SAPeerAgent;
import com.samsung.android.sdk.accessory.SASocket;

/**
 * Watch consumer service
 *  
 * @author Gilles LeBrun
 */
public class WatchConsumerService extends SAAgent {
	private static final String TAG = "joynwatch";
	
    private static final int CHANNEL_ID = 104;
    private static final int SERVICE_CONNECTION_RESULT_OK = 0;

    private WatchConsumerConnection mConnection;
    private WatchReceiver mReceiver;

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
    	public WatchConsumerService getService() {
    		return WatchConsumerService.this;
    	}
    }

    @Override
    public IBinder onBind(Intent arg0) {
    	return mBinder;
    }

    public WatchConsumerService() {
    	super(TAG, WatchConsumerConnection.class);
    }

    public boolean registerReceiver(WatchReceiver receiver) {
    	mReceiver = receiver;
        findPeers();
        return true;
    }

    public void sendCommand(String cmd) {
    	if (mConnection != null) {
    		try {
    			mConnection.send(CHANNEL_ID, cmd.getBytes());
    		} catch (IOException e) {
    			Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_LONG).show();
    		}
        }
     }

    public void findPeers() {
    	findPeerAgents();
    }

    @Override
    protected void onFindPeerAgentResponse(SAPeerAgent uRemoteAgent, int result) {
    	if (mReceiver != null) {
    		mReceiver.onPeerFound(uRemoteAgent);
    	} else {
    		Toast.makeText(getBaseContext(), R.string.label_no_activity, Toast.LENGTH_LONG).show();
    	}
    }

    public boolean establishConnection(SAPeerAgent peerAgent) {
    	boolean result = false;
        if (peerAgent != null) {
        	requestServiceConnection(peerAgent);
            result = true;
        }
        return result;
    }

    @Override
    protected void onServiceConnectionResponse(SASocket uThisConnection, int iConnResult) {
    	if (iConnResult == SERVICE_CONNECTION_RESULT_OK) {
    		this.mConnection = (WatchConsumerConnection) uThisConnection;
            mReceiver.onConnectionSuccess();
            Toast.makeText(getBaseContext(), R.string.label_connection_established, Toast.LENGTH_SHORT).show();
        } else {
        	Toast.makeText(getBaseContext(), R.string.label_connection_not_established, Toast.LENGTH_SHORT).show();
        }
    }

    public void closeConnection() {
    	mReceiver.onConnectionClose();
        if (mConnection != null) {
        	mConnection.close();
            mConnection = null;
        }
    }
}
