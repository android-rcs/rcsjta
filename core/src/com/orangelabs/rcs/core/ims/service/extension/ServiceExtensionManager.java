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
package com.orangelabs.rcs.core.ims.service.extension;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import com.orangelabs.rcs.provider.settings.RcsSettings;

/**
 * Service extension manager which adds supported extension after having
 * verified some authorization rules.
 * 
 * @author Jean-Marc AUFFRET
 */
public class ServiceExtensionManager {
	/**
	 * Update supported extensions
	 * 
	 * @param context Context
	 */
	public static void updateSupportedExtensions(Context context) {
		try {
			// Intent query on current installed activities
			PackageManager packageManager = context.getPackageManager();
			Intent intent = new Intent(com.gsma.services.rcs.capability.CapabilityService.INTENT_EXTENSIONS);
			String mime = com.gsma.services.rcs.capability.CapabilityService.EXTENSION_MIME_TYPE + "/*"; 
			intent.setType(mime);			
			List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER);
			StringBuffer extensions = new StringBuffer();
			for(int i=0; i < list.size(); i++) {
				ResolveInfo info = list.get(i);
				for(int j =0; j < info.filter.countDataTypes(); j++) {
					String tag = info.filter.getDataType(j);
					String[] value = tag.split("/");
					String ext = value[1];
					if (isExtensionAuthorized(context, info, ext)) {
						// Add the extension in the supported list
						extensions.append("," + ext);
					}
				}
			}
			if ((extensions.length() > 0) && (extensions.charAt(0) == ',')) {
				extensions.deleteCharAt(0);
			}
	
			// Save extensions in the local supported capabilities
			RcsSettings.getInstance().setSupportedRcsExtensions(extensions.toString());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Is extension authorized
	 * 
	 * @param context Context
	 * @param appInfo Application info
	 * @param ext Extension
	 * @return Boolean
	 */
	public static boolean isExtensionAuthorized(Context context, ResolveInfo appInfo, String ext) {
		// TODO
		return true;
	}
}
