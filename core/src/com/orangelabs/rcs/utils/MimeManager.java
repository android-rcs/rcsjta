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

package com.orangelabs.rcs.utils;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * MIME manager
 * 
 * @author jexa7410
 */
public class MimeManager {
	/**
	 * List of supported MIME type per extension
	 */
    private static Hashtable<String, String> mimeTable = new Hashtable<String, String>();
    static {
    	// Image type
    	mimeTable.put("jpg", "image/jpeg");
    	mimeTable.put("jpeg", "image/jpeg");
    	mimeTable.put("png", "image/png");
    	mimeTable.put("bmp", "image/bmp");

    	// Video type
    	mimeTable.put("3gp", "video/3gpp");    	
    	mimeTable.put("mp4", "video/mp4");
    	mimeTable.put("mp4a", "video/mp4");
    	mimeTable.put("mpeg4", "video/mp4");
    	mimeTable.put("mpeg", "video/mpeg");
    	mimeTable.put("mpg", "video/mpeg");
    	
    	// Visit Card type
    	mimeTable.put("vcf", "text/vcard");
    	
    	// Geoloc type
    	mimeTable.put("xml", "application/vnd.gsma.rcspushlocation+xml");
    }    
    
    /**
     * Is MIME type supported 
     * 
     * @param mime MIME-Type
     * @return Boolean
     */
    public static boolean isMimeTypeSupported(String mime) {
    	if (mime.equals("*")) { // Changed by Deutsche Telekom AG    		
    		return true;
    	} else {
    		return mimeTable.containsValue(mime);
    	}
    }    
    
    /**
     * Returns the supported MIME types 
     * 
     * @return List
     */
    public static Vector<String> getSupportedMimeTypes() {
    	Vector<String> result = new Vector<String>();
		for (Enumeration<String> e = mimeTable.elements() ; e.hasMoreElements() ;) {
			String mime = e.nextElement();
			result.addElement(mime);
	    }    	    	
		return result;
    }
    
    /**
     * Returns the supported image MIME types 
     * 
     * @return List
     */
    public static Vector<String> getSupportedImageMimeTypes() {
    	Vector<String> result = new Vector<String>();
		for (Enumeration<String> e = mimeTable.elements() ; e.hasMoreElements() ;) {
			String mime = e.nextElement();
			if (mime.startsWith("image") && (!result.contains(mime))) {
				result.addElement(mime);
			}
	    }    	    	
		return result;
    }
    
    /**
	 * Returns the MIME type associated to a given file extension
	 * 
	 * @param ext File extension
	 * @return MIME type
	 */
    public static String getMimeType(String ext) {
    	if (ext != null) {
    		return mimeTable.get(ext.toLowerCase());
    	} else {
    		return null;
    	}
    }
    
	/**
	 * Returns URL extension
	 * 
	 * @param url URL
	 * @return Extension
	 */
	public static String getFileExtension(String url) {
		if ((url != null) && (url.indexOf('.')!=-1)) {
			return url.substring(url.lastIndexOf('.')+1);
		} else {
			return null;
		}
	}	
    
	/**
	 * Returns MIME type extension
	 * 
	 * @param mime MIME type
	 * @return Extension
	 */
	public static String getMimeExtension(String mime) {
		if ((mime != null) && (mime.indexOf('/')!=-1)) {
    		return mime.substring(mime.indexOf('/')+1);
		}
		
		return "";
	}	

    /**
     * Is a image type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isImageType(String mime ){
    	if (mime.toLowerCase().startsWith("image/")){
    		return true;
    	} else {
    		return false;
    	}
    }
    
    /**
     * Is a video type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isVideoType(String mime) {
    	if (mime.toLowerCase().startsWith("video/")){
    		return true;
    	} else {
    		return false;
    	}
    }
    
    /**
     * Is an audio type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isAudioType(String mime) {
    	if (mime.toLowerCase().startsWith("audio/")) {
    		return true;
    	} else {
    		return false;
    	}
    }

    /**
     * Is a text type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isTextType(String mime) {
    	if (mime.toLowerCase().startsWith("text/")) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    /**
     * Is an application type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isApplicationType(String mime) {
    	if (mime.toLowerCase().startsWith("application/")) {
    		return true;
    	} else {
    		return false;
    	}
    }

    /**
     * Is a VCard type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isVCardType(String mime) {
    	if (mime.toLowerCase().equalsIgnoreCase("text/vcard")) {
    		return true;
    	} else {
    		return false;
    	}
    }    
    
    /**
     * Is a geoloc type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isGeolocType(String mime) {
    	if (mime.toLowerCase().equalsIgnoreCase("application/vnd.gsma.rcspushlocation+xml")) {
    		return true;
    	} else {
    		return false;
    	}
    }
}
