package com.orangelabs.rcs.service.api.server.gsma;

import com.orangelabs.rcs.service.api.client.gsma.GsmaClientConnector;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * GSMA utility functions
 * 
 * @author jexa7410
 */
public class GsmaUtils {
	
    /**
     * Set RCS client activation state
     * 
     * @param ctx Context
     * @param state Activation state
     */
    public static void setClientActivationState(Context ctx, boolean state) {
		SharedPreferences preferences = ctx.getSharedPreferences(GsmaClientConnector.GSMA_PREFS_NAME, Activity.MODE_WORLD_READABLE);
		Editor editor = preferences.edit();
		editor.putBoolean(GsmaClientConnector.GSMA_CLIENT_ENABLED, state);
		editor.commit();
    }
}