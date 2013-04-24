package com.orangelabs.rcs.service.api.server.gsma;

import com.orangelabs.rcs.service.api.client.gsma.GsmaUiConnector;
import com.orangelabs.rcs.service.api.server.ServerApiUtils;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * Get RCS status receiver
 *  
 * @author jexa7410
 */
public class GetRcsStatusReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		Bundle extras = new Bundle();
		extras.putBoolean(GsmaUiConnector.EXTRA_RCS_STATUS, true);
		extras.putBoolean(GsmaUiConnector.EXTRA_REGISTRATION_STATUS, ServerApiUtils.isImsConnected());
		setResult(Activity.RESULT_OK, null, extras);
    }
}
