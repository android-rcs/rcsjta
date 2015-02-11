/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications AB.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications AB.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import android.text.TextUtils;
import android.webkit.MimeTypeMap;

/**
 * MIME manager
 * 
 * @author jexa7410
 * @author LEMORDANT Philippe
 */
public class MimeManager {

    private static final String DEFAULT_FILE_ENCONDING = "application/octet-stream";
    /**
     * Singleton instance to access Two-way map that maps MIME-types to file extensions and vice
     * versa.
     */
    private static MimeManager instance;

    /**
     * Mime-Type to file extension mapping:
     */
    private HashMap<String, String> mimeTypeToExtensionMap;

    /**
     * File extension to Mime-Type mapping:
     */
    private HashMap<String, String> extensionToMimeTypeMap;

    /**
     * Set of image MIME-type:
     */
    private Set<String> imageMimeTypeSet;

    /**
     * Creates a new MIME-type map.
     */
    private MimeManager() {
        mimeTypeToExtensionMap = new HashMap<String, String>();
        extensionToMimeTypeMap = new HashMap<String, String>();
        imageMimeTypeSet = new HashSet<String>();
    }

    /**
     * @return The singleton instance of the MIME-type map.
     */
    public static MimeManager getInstance() {
        if (instance == null) {
            instance = new MimeManager();

            // Image type
            instance.loadMap("jpg", "image/jpeg");
            instance.loadMap("jpeg", "image/jpeg");
            instance.loadMap("png", "image/png");
            instance.loadMap("bmp", "image/bmp");

            // Video type
            instance.loadMap("3gp", "video/3gpp");
            instance.loadMap("mp4", "video/mp4");
            instance.loadMap("mp4a", "video/mp4");
            instance.loadMap("mpeg4", "video/mp4");
            instance.loadMap("mpeg", "video/mpeg");
            instance.loadMap("mpg", "video/mpeg");

            // Visit Card type
            instance.loadMap("vcf", "text/vcard");

            // Geoloc type
            instance.loadMap("xml", "application/vnd.gsma.rcspushlocation+xml");
        }
        return instance;
    }

    /**
     * Load an entry into the map.
     */
    private void loadMap(String extension, String mimeType) {
        // Do not override extension if already inserted.
        // First extension inserted should be the most representative one.
        if (!mimeTypeToExtensionMap.containsKey(mimeType)) {
            mimeTypeToExtensionMap.put(mimeType, extension);
        }

        extensionToMimeTypeMap.put(extension, mimeType);

        // Insert into imageMimeType set if image
        if (isImageType(mimeType)) {
            imageMimeTypeSet.add(mimeType);
        }
    }

    /**
     * Is MIME-type supported
     * 
     * @param mimeType the MIME-Type
     * @return True if there is a MIME-type entry in the map
     */
    public boolean isMimeTypeSupported(String mimeType) {
        if (mimeType.equals("*")) { // Changed by Deutsche Telekom AG
            return true;
        }
        if (TextUtils.isEmpty(mimeType)) {
            return false;
        }
        return mimeTypeToExtensionMap.containsKey(mimeType);
    }

    private String getMimeTypeFromMap(String extension) {
        return extensionToMimeTypeMap.get(extension.toLowerCase());
    }

    /**
     * Returns the MIME-type associated to a given file extension
     * 
     * @param extension The file extension
     * @return The MIME-type for the extension or null if there is none.
     */
    public String getMimeType(String extension) {
        if (TextUtils.isEmpty(extension)) {
            return null;
        }
        String mimeType = getMimeTypeFromMap(extension);
        if (mimeType != null) {
            return mimeType;
        }
        mimeType = (MimeTypeMap.getSingleton()).getMimeTypeFromExtension(extension);
        return (mimeType != null) ? mimeType : DEFAULT_FILE_ENCONDING;
    }

    /**
     * Returns the supported image MIME types
     * 
     * @return Set of image MIME-types
     */
    public Set<String> getSupportedImageMimeTypes() {
        return instance.imageMimeTypeSet;
    }

    /**
     * Get extension having MIME-type
     * 
     * @param mimeType the MIME-type
     * @return The extension for the MIME-type or null if there is none.
     */
    public String getExtensionFromMimeType(String mimeType) {
        if (TextUtils.isEmpty(mimeType)) {
            return null;
        }
        return mimeTypeToExtensionMap.get(mimeType);
    }

    /**
     * Returns URL extension
     * 
     * @param url URL
     * @return Extension
     */
    public static String getFileExtension(String url) {
        if ((url != null) && (url.indexOf('.') != -1)) {
            return url.substring(url.lastIndexOf('.') + 1);
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
        if ((mime != null) && (mime.indexOf('/') != -1)) {
            return mime.substring(mime.indexOf('/') + 1);
        }

        return "";
    }

    /**
     * Is a image type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isImageType(String mime) {
        if (mime.toLowerCase().startsWith("image/")) {
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
        if (mime.toLowerCase().startsWith("video/")) {
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
