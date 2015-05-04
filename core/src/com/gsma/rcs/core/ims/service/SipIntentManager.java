/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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

package com.gsma.rcs.core.ims.service;

import java.util.List;
import java.util.Set;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.capability.CapabilityUtils;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.capability.CapabilityService;
import com.gsma.services.rcs.extension.MultimediaMessagingSessionIntent;
import com.gsma.services.rcs.extension.MultimediaStreamingSessionIntent;

/**
 * SIP intent manager
 * 
 * @author Jean-Marc AUFFRET
 */
public class SipIntentManager {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     */
    public SipIntentManager() {
    }

    /**
     * Is the SIP request may be resolved
     * 
     * @param request Incoming request
     * @return Resolved intent or null if not resolved
     */
    public Intent isSipRequestResolved(SipRequest request) {
        Set<String> tags = request.getFeatureTags();
        for (String featureTag : tags) {
            Intent intent = generateSipIntent(request, featureTag);
            if (intent == null) {
                continue;
            }
            if (logger.isActivated()) {
                logger.debug("SIP intent: " + intent.getAction() + ", " + intent.getType());
            }
            if (isSipIntentResolvedByBroadcastReceiver(intent)) {
                return intent;
            }
        }
        return null;
    }

    /**
     * Generate a SIP Intent
     * 
     * @param request SIP request
     * @param featureTag Feature tag
     */
    private Intent generateSipIntent(SipRequest request, String featureTag) {
        String content = request.getContent();
        if (content == null) {
            return null;
        }

        Intent intent = null;
        if (content.toLowerCase().contains("msrp")) {
            intent = new Intent(MultimediaMessagingSessionIntent.ACTION_NEW_INVITATION);
        } else if (content.toLowerCase().contains("rtp")) {
            intent = new Intent(MultimediaStreamingSessionIntent.ACTION_NEW_INVITATION);
        }

        if (intent != null) {
            String mime = formatIntentMimeType(featureTag);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setType(mime.toLowerCase());
        }

        return intent;
    }

    /**
     * Is the SIP intent may be resolved by at least broadcast receiver
     * 
     * @param intent The Intent to resolve
     * @return Returns true if the intent has been resolved, else returns false
     */
    private boolean isSipIntentResolvedByBroadcastReceiver(Intent intent) {
        PackageManager packageManager = AndroidFactory.getApplicationContext().getPackageManager();
        List<ResolveInfo> list = packageManager.queryBroadcastReceivers(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return (list.size() > 0);
    }

    /**
     * Format intent MIME type
     * 
     * @param featureTag Feature tag
     * @return Intent MIME type
     */
    private String formatIntentMimeType(String featureTag) {
        String serviceId = CapabilityUtils.extractServiceId(featureTag);
        return CapabilityService.EXTENSION_MIME_TYPE + "/" + serviceId;
    }
}
