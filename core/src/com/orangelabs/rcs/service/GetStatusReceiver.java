package com.orangelabs.rcs.service;

import org.gsma.joyn.intent.ClientIntents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.orangelabs.rcs.provider.settings.RcsSettings;

/**
 * Get status intent receiver
 * 
 * @author Jean-Marc AUFFRET
 */
public class GetStatusReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
    	if (intent.getAction().endsWith(ClientIntents.ACTION_CLIENT_GET_STATUS)) {
	    	RcsSettings.createInstance(context);
	    	Bundle results = getResultExtras(true);
	        results.putString(ClientIntents.EXTRA_CLIENT, context.getPackageName());
	        results.putBoolean(ClientIntents.EXTRA_STATUS, RcsSettings.getInstance().isServiceActivated());
	        setResultExtras(results);	  
    	}
    }
}
