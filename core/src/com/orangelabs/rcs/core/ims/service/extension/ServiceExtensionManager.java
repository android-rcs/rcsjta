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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.Provider;
import java.security.Security;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;

import com.android.org.bouncycastle.jce.provider.BouncyCastleProvider;
import com.gsma.iariauth.validator.PackageProcessor;
import com.gsma.iariauth.validator.ProcessingResult;
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
	 * Update supported extensions after third party application installation  
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
	 * @param ext Extension ID
	 * @return Boolean
	 */
	public static boolean isExtensionAuthorized(Context context, ResolveInfo appInfo, String ext) {
		if ((appInfo == null) || (appInfo.activityInfo== null)) {
			return false;
		}

		try {
			if (!RcsSettings.getInstance().isExtensionsAllowed()) {
				if (logger.isActivated()) {
					logger.debug("Extensions are not allowed");
				}
				return false;
			}

			if (!RcsSettings.getInstance().isExtensionsControlled()) {
				if (logger.isActivated()) {
					logger.debug("No control on extensions");
				}
				return true;
			}
					
			String pkgName = appInfo.activityInfo.packageName;
			if (logger.isActivated()) {
				logger.debug("Check extension " + ext + " for package " + pkgName);
			}

			// Checking procedure
			boolean authorized = false;
			PackageInfo pkg = context.getPackageManager().getPackageInfo(pkgName, PackageManager.GET_SIGNATURES);
			Signature[] signs = pkg.signatures;
			if (signs.length > 0) {
				String sha1Sign = getFingerprint(signs[0].toByteArray());
				if (logger.isActivated()) {
					logger.debug("Check application fingerprint: " + sha1Sign);
				}
				PackageProcessor processor = new PackageProcessor(ks, ext, sha1Sign);
				ProcessingResult result = processor.processIARIauthorization(authDocument);
				if (result.getStatus() == ProcessingResult.STATUS_OK) {
					authorized = true;
					if (logger.isActivated()) {
						logger.debug("Extension is authorized");
					}
				} else {
					if (logger.isActivated()) {
						logger.debug("Extension " + ext + " is not authorized: " + result.getStatus() + " " + result.getError().toString());
					}
				}
			} else {
				if (logger.isActivated()) {
					logger.debug("Extension is not authorized: no signature found");
				}
			}
			return authorized;
			
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Internal exception", e);
			}
			return false;
		}
	}
	
	/* static init */
	private static Provider bcProvider = new BouncyCastleProvider();
	private static KeyStore ks = null;
	private static File authDocument = null;
	static {
		org.apache.xml.security.Init.init();
		Security.addProvider(bcProvider);
		
		String authDocumentPath = "/sdcard/iari-authorization.xml"; // TODO: get from provisioning
		String ksPath = "/sdcard/range-root-truststore.bks"; // TODO: get from provisioning
		String ksPasswd = "secret"; // TODO: get from provisioning
		authDocument = new File(authDocumentPath);
		ks = loadKeyStore(ksPath, ksPasswd);
	}	

	/**
	 * Load the keystore
	 * 
	 * @param path Path
	 * @param password Password
	 * @return Keystore
	 */
	private static KeyStore loadKeyStore(String path, String password) {
		KeyStore ks = null;
		File certKeyFile = new File(path);
		if(!certKeyFile.exists() || !certKeyFile.isFile()) {
			return null;
		}
		char[] pass = password.toCharArray();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(path);
			try {
				ks = KeyStore.getInstance("jks");
				ks.load(fis, pass);
			} catch (Exception e1) {
				try {
					ks = KeyStore.getInstance("bks", bcProvider);
					ks.load(fis, pass);
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}
		} catch(FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			try { if(fis != null) fis.close(); } catch(Throwable t) {}
		}
		
		return ks;
	}
	
    /**
     * Returns the fingerprint of a certificate
     * 
     * @param cert Certificate
     * @param algorithm hash algorithm to be used
     * @return String as xx:yy:zz
     */
    public static String getFingerprint(byte[] cert) throws Exception {
    	MessageDigest md = MessageDigest.getInstance("SHA-1");
    	md.update(cert);
    	byte[] digest = md.digest();
    	
    	String toRet = "";
    	for (int i = 0; i < digest.length; i++) {
    		if (i != 0)
    			toRet += ":";
    		int b = digest[i] & 0xff;
    		String hex = Integer.toHexString(b);
    		if (hex.length() == 1)
    			toRet += "0";
    		toRet += hex;
    	}
    	return toRet.toUpperCase();
	}
}
