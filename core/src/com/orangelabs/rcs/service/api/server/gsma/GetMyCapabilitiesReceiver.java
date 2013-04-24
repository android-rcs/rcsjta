package com.orangelabs.rcs.service.api.server.gsma;

import java.util.ArrayList;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.orangelabs.rcs.core.ims.service.capability.Capabilities;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.service.api.client.gsma.GsmaUiConnector;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Get my capabilities receiver
 *  
 * @author jexa7410
 */
public class GetMyCapabilitiesReceiver extends BroadcastReceiver {
    /**
	 * The logger
	 */
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (logger.isActivated()) {
			logger.info("Get my capabilities");
		}

		// Read capabilities
		RcsSettings.createInstance(context);
		Capabilities capabilities = RcsSettings.getInstance().getMyCapabilities();
		
		// Send intent result
		Bundle extras = new Bundle();
    	if (capabilities != null) {
    		extras.putBoolean(GsmaUiConnector.EXTRA_CAPABILITY_CHAT, capabilities.isImSessionSupported());
    		extras.putBoolean(GsmaUiConnector.EXTRA_CAPABILITY_FT, capabilities.isFileTransferSupported());
    		extras.putBoolean(GsmaUiConnector.EXTRA_CAPABILITY_IMAGE_SHARE, capabilities.isImageSharingSupported());
    		extras.putBoolean(GsmaUiConnector.EXTRA_CAPABILITY_VIDEO_SHARE, capabilities.isVideoSharingSupported());
    		extras.putBoolean(GsmaUiConnector.EXTRA_CAPABILITY_GEOLOCATION_PUSH, capabilities.isGeolocationPushSupported());
    		extras.putBoolean(GsmaUiConnector.EXTRA_CAPABILITY_CS_VIDEO, capabilities.isCsVideoSupported());
    		extras.putBoolean(GsmaUiConnector.EXTRA_CAPABILITY_PRESENCE_DISCOVERY, capabilities.isPresenceDiscoverySupported());
    		extras.putBoolean(GsmaUiConnector.EXTRA_CAPABILITY_SOCIAL_PRESENCE, capabilities.isSocialPresenceSupported());
    		extras.putBoolean(GsmaUiConnector.EXTRA_CAPABILITY_SF, RcsSettings.getInstance().isImAlwaysOn());
    		ArrayList<String> listExts = capabilities.getSupportedExtensions();
    		String[] exts = new String[listExts.size()];
    		listExts.toArray(exts);
    		extras.putStringArray(GsmaUiConnector.EXTRA_CAPABILITY_EXTENSIONS, exts);
    		// TODO: presence info
    	}
		setResult(Activity.RESULT_OK, null, extras);		
    }
}
