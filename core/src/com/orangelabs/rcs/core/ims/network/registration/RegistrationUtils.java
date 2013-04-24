package com.orangelabs.rcs.core.ims.network.registration;

import java.util.ArrayList;
import java.util.List;

import com.orangelabs.rcs.core.ims.network.sip.FeatureTags;
import com.orangelabs.rcs.provider.settings.RcsSettings;

/**
 * Registration utility functions
 * 
 * @author jexa7410
 */
public class RegistrationUtils {
	/**
	 * Get supported feature tags for registration
	 *
	 * @return List of tags
	 */
	public static List<String> getSupportedFeatureTags() {
		List<String> tags = new ArrayList<String>();
		
		// IM support
		if (RcsSettings.getInstance().isImSessionSupported()) {
			tags.add(FeatureTags.FEATURE_OMA_IM);
		}

		// Video share support
		if (RcsSettings.getInstance().isVideoSharingSupported()) {
			tags.add(FeatureTags.FEATURE_3GPP_VIDEO_SHARE);
		}
		
		String supported = "";

		// Image share support
		if (RcsSettings.getInstance().isImageSharingSupported()) {
			supported += FeatureTags.FEATURE_RCSE_IMAGE_SHARE + ",";
		}
		
		// Geoloc push support
		if (RcsSettings.getInstance().isGeoLocationPushSupported()) {
			supported += FeatureTags.FEATURE_RCSE_GEOLOCATION_PUSH + ",";
		}

		// File transfer HTTP support
		if (RcsSettings.getInstance().isFileTransferHttpSupported()) {
			supported += FeatureTags.FEATURE_RCSE_FT_HTTP;
		}
		
		// Add RCS-e prefix
		if (supported.length() != 0) {
			supported = FeatureTags.FEATURE_RCSE + "=\"" + supported + "\"";
			tags.add(supported);
		}
		
		return tags;
	}	
}
