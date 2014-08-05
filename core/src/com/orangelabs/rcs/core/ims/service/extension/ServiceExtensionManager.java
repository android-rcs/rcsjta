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
import android.content.Intent;
import android.text.TextUtils;

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
	 * Update supported extensions after installing or removing a third
	 * party application on the device  
	 * 
	 * @param context Context
	 * @param action Action
	 * @param ext Extension
	 * @param package Package name
	 */
	public static void updateSupportedExtensions(Context context, String action, String ext, String packageName) {
		try {
    		if (logger.isActivated()) {
    			logger.debug("Update supported extensions: " + action + " " + ext);
    		}
            
            // Read current extensions in the database
    		String exts = RcsSettings.getInstance().getSupportedRcsExtensions();
    		String[] extensions = new String[0];
    		if (!TextUtils.isEmpty(exts)) {
    			extensions = exts.split(",");
    		}
    		List<String> listExts = new ArrayList<String>(); 
			for(int i=0; i < extensions.length; i++) {
				listExts.add(extensions[i]);
			}
    		
			// Update the current extensions with the new one
            if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
                // Add an extension
				if (isExtensionAuthorized(context, ext) &&
						!listExts.contains(ext)) {
					// Add the extension in the supported list if authorized
					// and not yet in the list
					listExts.add(ext);
					if (logger.isActivated()) {
						logger.debug("Extension " + ext + " is added");
					}
				}
            } else
            if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                // Remove an extension
            	listExts.remove(ext);
				if (logger.isActivated()) {
					logger.debug("Extension " + ext + " is removed");
				}
            }
			
			// Update current extensions in database
            StringBuffer result = new StringBuffer();
            for(int i =0; i < listExts.size(); i++) {
            	result.append("," + listExts.get(i));
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
	 * Update supported extensions at boot
	 * 
	 * @param context Context
	 */
	public static void updateSupportedExtensions(Context context) {
		if (logger.isActivated()) {
			logger.debug("Update supported extensions");
		}
		// TODO
		
        // Read current extensions in the database
		String exts = RcsSettings.getInstance().getSupportedRcsExtensions();
		String[] extensions = new String[0];
		if (!TextUtils.isEmpty(exts)) {
			extensions = exts.split(",");
		}
		List<String> listExts = new ArrayList<String>(); 
		for(int i=0; i < extensions.length; i++) {
			listExts.add(extensions[i]);
			if (logger.isActivated()) {
				logger.debug("Extension " + extensions[i] + " is loaded");
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
}
