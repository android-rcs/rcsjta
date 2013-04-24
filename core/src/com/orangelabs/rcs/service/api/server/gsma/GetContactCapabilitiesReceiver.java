package com.orangelabs.rcs.service.api.server.gsma;

import java.util.ArrayList;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.orangelabs.rcs.core.ims.service.capability.Capabilities;
import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.service.api.client.contacts.ContactInfo;
import com.orangelabs.rcs.service.api.client.gsma.GsmaUiConnector;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Get contact capabilities receiver
 *  
 * @author jexa7410
 */
public class GetContactCapabilitiesReceiver extends BroadcastReceiver {
	
    /**
	 * The logger
	 */
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	@Override
	public void onReceive(Context context, Intent intent) {
		RcsSettings.createInstance(context);
	
		// Read contact
		String contact = intent.getStringExtra(GsmaUiConnector.EXTRA_CONTACT);
		if (logger.isActivated()) {
			logger.info("Get contact capabilities for " + contact);
		}

		// Read capabilities
    	ContactInfo contactInfo = ContactsManager.getInstance().getContactInfo(contact);

    	// Send intent result
		Bundle extras = new Bundle();
    	if (contactInfo != null) {
    		Capabilities capabilities = contactInfo.getCapabilities();
    		extras.putBoolean(GsmaUiConnector.EXTRA_CAPABILITY_CHAT, capabilities.isImSessionSupported());
    		extras.putBoolean(GsmaUiConnector.EXTRA_CAPABILITY_FT, capabilities.isFileTransferSupported());
    		extras.putBoolean(GsmaUiConnector.EXTRA_CAPABILITY_IMAGE_SHARE, capabilities.isImageSharingSupported());
    		extras.putBoolean(GsmaUiConnector.EXTRA_CAPABILITY_VIDEO_SHARE, capabilities.isVideoSharingSupported());
    		extras.putBoolean(GsmaUiConnector.EXTRA_CAPABILITY_GEOLOCATION_PUSH, capabilities.isGeolocationPushSupported());
    		extras.putBoolean(GsmaUiConnector.EXTRA_CAPABILITY_CS_VIDEO, capabilities.isCsVideoSupported());
    		extras.putBoolean(GsmaUiConnector.EXTRA_CAPABILITY_PRESENCE_DISCOVERY, capabilities.isPresenceDiscoverySupported());
    		extras.putBoolean(GsmaUiConnector.EXTRA_CAPABILITY_SOCIAL_PRESENCE, capabilities.isSocialPresenceSupported());
    		ArrayList<String> listExts = capabilities.getSupportedExtensions();
    		String[] exts = new String[listExts.size()];
    		listExts.toArray(exts);
    		extras.putStringArray(GsmaUiConnector.EXTRA_CAPABILITY_EXTENSIONS, exts);
    		// TODO: presence info
    	}
		setResult(Activity.RESULT_OK, null, extras);		
    }
}
