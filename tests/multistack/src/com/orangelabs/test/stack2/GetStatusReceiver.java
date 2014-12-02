package com.orangelabs.test.stack2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.gsma.services.rcs.Intents;

/**
 * Get status intent receiver
 * 
 * @author Jean-Marc AUFFRET
 */
public class GetStatusReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
    	if (intent.getAction().endsWith(Intents.Service.ACTION_GET_STATUS)) {
	    	Bundle results = getResultExtras(true);
	        results.putString(Intents.Service.EXTRA_PACKAGENAME, context.getPackageName());
	        results.putBoolean(Intents.Service.EXTRA_STATUS, false);
	        setResultExtras(results);	  
    	}
    }
}
