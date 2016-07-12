/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.core.ims.network.registration;

import com.gsma.rcs.core.ims.network.sip.FeatureTags;
import com.gsma.rcs.provider.settings.RcsSettings;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Registration utility functions
 * 
 * @author jexa7410
 */
public class RegistrationUtils {
    /**
     * Get supported feature tags for registration
     * 
     * @param rcsSettings RCS settings accessor
     * @return List of tags
     */
    public static String[] getSupportedFeatureTags(RcsSettings rcsSettings) {
        List<String> tags = new ArrayList<>();
        List<String> icsiTags = new ArrayList<>();
        List<String> iariTags = new ArrayList<>();

        // IM support
        if (rcsSettings.isImSessionSupported()) {
            tags.add(FeatureTags.FEATURE_OMA_IM);
        }

        // Video share support
        if (rcsSettings.isVideoSharingSupported()) {
            tags.add(FeatureTags.FEATURE_3GPP_VIDEO_SHARE);
        }

        // IP call support
        if (rcsSettings.isIPVoiceCallSupported()) {
            tags.add(FeatureTags.FEATURE_RCSE_IP_VOICE_CALL);
        }
        if (rcsSettings.isIPVideoCallSupported()) {
            tags.add(FeatureTags.FEATURE_RCSE_IP_VIDEO_CALL);
        }
        if (rcsSettings.isIPVoiceCallSupported() || rcsSettings.isIPVideoCallSupported()) {
            icsiTags.add(FeatureTags.FEATURE_3GPP_IP_VOICE_CALL);
        }

        // Automata support
        if (rcsSettings.isSipAutomata()) {
            tags.add(FeatureTags.FEATURE_SIP_AUTOMATA);
        }

        // Image share support
        if (rcsSettings.isImageSharingSupported()) {
            iariTags.add(FeatureTags.FEATURE_RCSE_IMAGE_SHARE);
        }

        // Geoloc push support
        if (rcsSettings.isGeoLocationPushSupported()) {
            iariTags.add(FeatureTags.FEATURE_RCSE_GEOLOCATION_PUSH);
        }

        // File transfer HTTP support
        if (rcsSettings.isFileTransferHttpSupported()) {
            iariTags.add(FeatureTags.FEATURE_RCSE_FT_HTTP);
        }

        // Extensions
        if (rcsSettings.isExtensionsAllowed()) {
            for (String extension : rcsSettings.getSupportedRcsExtensions()) {
                if (rcsSettings.isExtensionAuthorized(extension)) {
                    if (extension.startsWith("gsma.")) {
                        icsiTags.add(FeatureTags.FEATURE_RCSE_ICSI_EXTENSION + "." + extension);
                    } else {
                        iariTags.add(FeatureTags.FEATURE_RCSE_IARI_EXTENSION + "." + extension);
                    }
                }
            }
            icsiTags.add(FeatureTags.FEATURE_3GPP_EXTENSION);
        }

        // Add IARI prefix
        if (!iariTags.isEmpty()) {
            tags.add(FeatureTags.FEATURE_RCSE + "=\"" + TextUtils.join(",", iariTags) + "\"");
        }

        // Add ICSI prefix
        if (!icsiTags.isEmpty()) {
            tags.add(FeatureTags.FEATURE_3GPP + "=\"" + TextUtils.join(",", icsiTags) + "\"");
        }

        return tags.toArray(new String[tags.size()]);
    }
}
