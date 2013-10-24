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
		
		// IP call support
		if (RcsSettings.getInstance().isIPVoiceCallSupported()) {
			tags.add(FeatureTags.FEATURE_RCSE_IP_VOICE_CALL);
			tags.add(FeatureTags.FEATURE_3GPP_IP_VOICE_CALL);
		}
		if (RcsSettings.getInstance().isIPVideoCallSupported()) {
			tags.add(FeatureTags.FEATURE_RCSE_IP_VIDEO_CALL);
		}

		// Automata support
		if (RcsSettings.getInstance().isSipAutomata()) {
			tags.add(FeatureTags.FEATURE_SIP_AUTOMATA);
		}
		
		String additionalTags = "";

		// Image share support
		if (RcsSettings.getInstance().isImageSharingSupported()) {
			additionalTags += FeatureTags.FEATURE_RCSE_IMAGE_SHARE + ",";
		}
		
		// Geoloc push support
		if (RcsSettings.getInstance().isGeoLocationPushSupported()) {
			additionalTags += FeatureTags.FEATURE_RCSE_GEOLOCATION_PUSH + ",";
		}

		// File transfer HTTP support
		if (RcsSettings.getInstance().isFileTransferHttpSupported()) {
			additionalTags += FeatureTags.FEATURE_RCSE_FT_HTTP;
		}
		
		// Add RCS-e prefix
		if (additionalTags.length() != 0) {
        	if (additionalTags.endsWith(",")) {
        		additionalTags = additionalTags.substring(0, additionalTags.length()-1);
        	}
			additionalTags = FeatureTags.FEATURE_RCSE + "=\"" + additionalTags + "\"";
			tags.add(additionalTags);
		}
		
		return tags;
	}
}
