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

package com.orangelabs.rcs.core.ims.service;

import java.util.List;

import javax2.sip.message.Request;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.service.api.client.sip.SipApiIntents;
import com.orangelabs.rcs.utils.StringUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * SIP intent manager
 * 
 * @author jexa7410
 */
public class SipIntentManager {
	/**
     * The logger
     */
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
		Intent result = null;
		List<String> tags = request.getFeatureTags();
		for(int i=0; i < tags.size(); i++) {
			String featureTag = tags.get(i);
			Intent intent = generateSipIntent(request, featureTag);
			if (logger.isActivated()) {
				logger.debug("SIP intent: " + intent.getAction() + ", " + intent.getType());
			}
			if (isSipIntentResolvedByBroadcastReceiver(intent)) {
				result = intent;
				break;
			}
		}
		return result;
	}	

	/**
	 * Generate a SIP Intent
	 * 
	 * @param request SIP request
	 * @param featureTag Feature tag
	 */
	public static Intent generateSipIntent(SipRequest request, String featureTag) {
		String mime = formatIntentMimeType(featureTag);
		String action = formatIntentAction(request.getMethod());
		Intent intent = new Intent(action);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.setType(mime.toLowerCase());
		return intent;
	}

	/**
	 * Is the SIP intent may be resolved by at least broadcast receiver
	 * 
	 * @param intent The Intent to resolve
	 * @return Returns true if the intent has been resolved, else returns false
	 */
	public static boolean isSipIntentResolvedByBroadcastReceiver(Intent intent) {
		PackageManager packageManager = AndroidFactory.getApplicationContext().getPackageManager();
		List<ResolveInfo> list = packageManager.queryBroadcastReceivers(intent,
				PackageManager.MATCH_DEFAULT_ONLY);
		return (list.size() > 0);
	}
	
	/**
	 * Format intent action
	 * 
	 * @param request Request method name
	 * @return Intent action
	 */
	public static String formatIntentAction(String request) { 
		String action;
		if (request.equals(Request.MESSAGE)) {
			action = SipApiIntents.INSTANT_MESSAGE;
		} else {
			action = SipApiIntents.SESSION_INVITATION;
		}
		return action;
	}
	
	/**
	 * Format intent MIME type
	 * 
	 * @param tag Feature tag
	 * @return Intent MIME type
	 */
	public static String formatIntentMimeType(String tag) { 
		String mime;
		if (tag.contains("=")) {
			// Transform syntax +aaaa="bbbb" to +aaaa/bbbb before intent resolution
			String[] submime = tag.split("=");
			mime = submime[0] + "/" + StringUtils.removeQuotes(submime[1]);
		} else {
			// Transform syntax +aaaa to +aaaa/* before intent resolution
			mime = tag + "/*";
		}
		return mime;
	}
}
