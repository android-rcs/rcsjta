package com.orangelabs.rcs.watch;

import com.samsung.android.sdk.accessory.SAPeerAgent;

/**
 * Watch receiver interface
 * 
 * @author Gilles LeBrun
 */
interface WatchReceiver {
	public void onPeerFound(SAPeerAgent uRemoteAgent);
	
    public void onConnectionSuccess();
    
    public void onConnectionClose();
}

