package com.orangelabs.rcs.service;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.gsma.services.rcs.Intents;
import com.orangelabs.rcs.provider.settings.RcsSettings;

/**
 * Get status intent receiver
 * 
 * @author Jean-Marc AUFFRET
 */
public class GetStatusReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
    	if (intent.getAction().endsWith(Intents.Client.ACTION_CLIENT_GET_STATUS)) {
	    	RcsSettings.createInstance(context);
	    	Bundle results = getResultExtras(true);
	        results.putString(Intents.Client.EXTRA_CLIENT, context.getPackageName());
	        results.putBoolean(Intents.Client.EXTRA_STATUS, RcsSettings.getInstance().isServiceActivated());
	        setResultExtras(results);	  
    	}
    }
}
