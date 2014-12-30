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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
 * @author YPLO6403
 *
 */
public class ServiceExtensionManager {
	
	/**
	 * Singleton of ServiceExtensionManager
	 */
	private static volatile ServiceExtensionManager instance;
	
	/**
	 * Separator of extensions
	 */
	private static final String EXTENSION_SEPARATOR = ";";
	
	/**
     * The logger
     */
    private final static Logger logger = Logger.getLogger(ServiceExtensionManager.class.getSimpleName());
    
    /**
	 * Empty constructor : prevent caller from creating multiple instances
	 */
	private ServiceExtensionManager() {
	}
	
	/**
	 * Get an instance of ServiceExtensionManager.
	 * 
	 * @return the singleton instance.
	 */
	public static ServiceExtensionManager getInstance() {
		if (instance == null) {
			synchronized (ServiceExtensionManager.class) {
				if (instance == null) {
					instance = new ServiceExtensionManager();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Save supported extensions in database
	 * 
	 * @param supportedExts List of supported extensions
	 */
	private void saveSupportedExtensions(Set<String> supportedExts) {
		// Update supported extensions in database
		RcsSettings.getInstance().setSupportedRcsExtensions(supportedExts);
	}
	
	/**
	 * Check if the extensions are valid. Each valid extension is saved in the cache.  
	 * 
	 * @param context Context
	 * @param supportedExts Set of supported extensions
	 * @param newExts Set of new extensions to be checked
	 */
	private void checkExtensions(Context context, Set<String> supportedExts, Set<String> newExts) {
		// Check each new extension
		for (String extension : newExts) {
			if (isExtensionAuthorized(context, extension)) {
				if (supportedExts.contains(extension)) {
					if (logger.isActivated()) {
						logger.debug("Extension " + extension + " is already in the list");
					}
				} else {
					// Add the extension in the supported list if authorized and not yet in the list
					supportedExts.add(extension);
					if (logger.isActivated()) {
						logger.debug("Extension " + extension + " is added to the list");
					}
				}
			}
		}
	}	
	
	/**
	 * Update supported extensions at boot
	 * 
	 * @param context Context
	 */
	public void updateSupportedExtensions(Context context) {
		if (context == null) {
			if (logger.isActivated()) {
				logger.warn("Cannot update supported extension: context is null");
			}
			return;
		}
		try {
			if (logger.isActivated()) {
				logger.debug("Update supported extensions");
			}

			Set<String> supportedExts = new HashSet<String>(); 
			
			// Intent query on current installed activities
    		PackageManager pm = context.getPackageManager();
    		List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
    		for (ApplicationInfo appInfo : apps) {
				Bundle appMeta = appInfo.metaData;
				if (appMeta != null) {
		    		String exts = appMeta.getString(CapabilityService.INTENT_EXTENSIONS);
		    		if (!TextUtils.isEmpty(exts)) {
						if (logger.isActivated()) {
							logger.debug("Update supported extensions " + exts);
						}
			    		// Check extensions
			    		checkExtensions(context, supportedExts, getExtensions(exts));    		
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
	public boolean isExtensionAuthorized(Context context, String ext) {
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
	}
	
	/**
	 * Remove supported extensions  
	 * 
	 * @param context Context
	 */
	public void removeSupportedExtensions(Context context) {
		updateSupportedExtensions(context);
	}
	
	/**
	 * Add supported extensions  
	 * 
	 * @param context Context
	 */
	public void addNewSupportedExtensions(Context context) {
		updateSupportedExtensions(context);
	}
	
	/**
	 * Extract set of extensions from String
	 *
	 * @param extensions
	 *            String where extensions are concatenated with a ";" separator
	 * @return the set of extensions
	 */
	public static Set<String> getExtensions(String extensions) {
		Set<String> result = new HashSet<String>();
		if (TextUtils.isEmpty(extensions)) {
			return result;

		}
		String[] extensionList = extensions.split(ServiceExtensionManager.EXTENSION_SEPARATOR);
		for (String extension : extensionList) {
			if (!TextUtils.isEmpty(extension) && extension.trim().length() > 0) {
				result.add(extension);
			}
		}
		return result;
	}

	/**
	 * Concatenate set of extensions into a string
	 *
	 * @param extensions
	 *            set of extensions
	 * @return String where extensions are concatenated with a ";" separator
	 */
	public static String getExtensions(Set<String> extensions) {
		if (extensions == null || extensions.isEmpty()) {
			return "";

		}
		StringBuilder result = new StringBuilder();
		int size = extensions.size();
		for (String extension : extensions) {
			if (extension.trim().length() == 0) {
				--size;
				continue;

			}
			result.append(extension);
			if (--size != 0) {
				// Not last item : add separator
				result.append(EXTENSION_SEPARATOR);
			}
		}
		return result.toString();
	}

}
