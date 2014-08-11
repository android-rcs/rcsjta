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

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;

import com.gsma.services.rcs.capability.CapabilityService;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Service extension manager which adds supported extension after having
 * verified some authorization rules.
 * 
 * @author Jean-Marc AUFFRET
 */
public class ServiceExtensionManager {
	/**
     * The logger
     */
    private static Logger logger = Logger.getLogger(ServiceExtensionManager.class.getName());
    
	/**
	 * Save supported extensions in database
	 * 
	 * @param supportedExts List of supported extensions
	 */
	private static void saveSupportedExtensions(List<String> supportedExts) {
		try {
			// Update supported extensions in database
		    StringBuffer result = new StringBuffer();
		    for(int i =0; i < supportedExts.size(); i++) {
		    	result.append(";" + supportedExts.get(i));
		    }
		    if (result.length() > 0) {
		    	result.deleteCharAt(0);
		    }
			RcsSettings.getInstance().setSupportedRcsExtensions(result.toString());
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
		}
	}
	
	/**
	 * Check if the extensions are valid. Each valid extension is saved in the cache.  
	 * 
	 * @param context Context
	 * @param supportedExts List of supported extensions
	 * @param newExts New extensions to be checked
	 * @return Returns true if at least one valid extension has been found
	 */
	private static boolean checkExtensions(Context context, List<String> supportedExts, String newExts) {
		boolean result = false;
		try {
			// Check each new extension
    		String[] extensions = new String[0];
    		if (!TextUtils.isEmpty(newExts)) {
    			extensions = newExts.split(";");
    		}
    		for(int i=0; i < extensions.length; i++) {
    			if (isExtensionAuthorized(context, extensions[i])) {
    				if (supportedExts.contains(extensions[i])) {
	    				if (logger.isActivated()) {
	    					logger.debug("Extension " + extensions[i] + " is already in the list");
	    				}
    				} else {
	    				// Add the extension in the supported list if authorized and not yet in the list
    					supportedExts.add(extensions[i]);
	    				if (logger.isActivated()) {
	    					logger.debug("Extension " + extensions[i] + " is added in the list");
	    				}
    				}
    			}
    		}
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
		}
		return result;
	}	
	
	/**
	 * Update supported extensions at boot
	 * 
	 * @param context Context
	 */
	public static void updateSupportedExtensions(Context context) {
		try {
			if (logger.isActivated()) {
				logger.debug("Update supported extensions");
			}

			List<String> supportedExts = new ArrayList<String>(); 
			
			// Intent query on current installed activities
    		PackageManager pm = context.getPackageManager();
    		List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
    		for (ApplicationInfo appInfo : apps) {
				Bundle appMeta = appInfo.metaData;
				if (appMeta != null) {
		    		String exts = appMeta.getString(CapabilityService.INTENT_EXTENSIONS);
		    		if (exts != null) {
			    		// Check extensions
			    		checkExtensions(context, supportedExts, exts);    		
		    		}
				}
			}
			
			// Update supported extensions in database
    		saveSupportedExtensions(supportedExts);
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
		}
	}	
	
	/**
	 * Is extension authorized
	 * 
	 * @param context Context
	 * @param ext Extension ID
	 * @return Boolean
	 */
	public static boolean isExtensionAuthorized(Context context, String ext) {
		try {
			if (!RcsSettings.getInstance().isExtensionsAllowed()) {
				if (logger.isActivated()) {
					logger.debug("Extensions are not allowed");
				}
				return false;
			}

			if (logger.isActivated()) {
				logger.debug("No control on extensions");
			}
			return true;
			
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Internal exception", e);
			}
			return false;
		}
	}
	
	/**
	 * Remove supported extensions  
	 * 
	 * @param context Context
	 */
	public static void removeSupportedExtensions(Context context) {
		updateSupportedExtensions(context);
	}
	
	/**
	 * Add supported extensions  
	 * 
	 * @param context Context
	 */
	public static void addNewSupportedExtensions(Context context) {
		updateSupportedExtensions(context);
	}
}
