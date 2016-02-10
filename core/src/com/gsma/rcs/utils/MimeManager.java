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

import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

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
    private static volatile MimeManager sInstance;

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
        mimeTypeToExtensionMap = new HashMap<>();
        extensionToMimeTypeMap = new HashMap<>();
        imageMimeTypeSet = new HashSet<>();
    }

    /**
     * @return The singleton instance of the MIME-type map.
     */
    public static MimeManager getInstance() {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (MimeManager.class) {
            if (sInstance == null) {
                sInstance = new MimeManager();

                // Image type
                sInstance.loadMap("jpg", "image/jpeg");
                sInstance.loadMap("jpeg", "image/jpeg");
                sInstance.loadMap("png", "image/png");
                sInstance.loadMap("bmp", "image/bmp");

                // Audio type
                sInstance.loadMap("3gp", "audio/3gpp");
                sInstance.loadMap("mp4", "audio/mp4");

                // Video type
                sInstance.loadMap("3gp", "video/3gpp");
                sInstance.loadMap("mp4", "video/mp4");
                sInstance.loadMap("mp4a", "video/mp4");
                sInstance.loadMap("mpeg4", "video/mp4");
                sInstance.loadMap("mpeg", "video/mpeg");
                sInstance.loadMap("mpg", "video/mpeg");

                // Visit Card type
                sInstance.loadMap("vcf", "text/vcard");

                // Geoloc type
                sInstance.loadMap("xml", "application/vnd.gsma.rcspushlocation+xml");
            }
            return sInstance;
        }
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
        return "*".equals(mimeType) || !TextUtils.isEmpty(mimeType)
                && mimeTypeToExtensionMap.containsKey(mimeType);
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
        return sInstance.imageMimeTypeSet;
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
        String extension = mimeTypeToExtensionMap.get(mimeType);
        if (extension != null) {
            return extension;
        }
        return (MimeTypeMap.getSingleton()).getExtensionFromMimeType(mimeType);
    }

    /**
     * Returns path extension
     * 
     * @param path The path or filename
     * @return Extension
     */
    public static String getFileExtension(String path) {
        if (path.indexOf('.') != -1) {
            return path.substring(path.lastIndexOf('.') + 1);
        }
        return null;
    }

    /**
     * Is a image type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isImageType(String mime) {
        return mime.toLowerCase().startsWith("image/");
    }

    /**
     * Is a video type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isVideoType(String mime) {
        return mime.toLowerCase().startsWith("video/");
    }

    /**
     * Is an audio type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isAudioType(String mime) {
        return mime.toLowerCase().startsWith("audio/");
    }

    /**
     * Is a VCard type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isVCardType(String mime) {
        return mime.toLowerCase().equalsIgnoreCase("text/vcard");
    }

    /**
     * Is a geoloc type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isGeolocType(String mime) {
        return mime.toLowerCase().equalsIgnoreCase("application/vnd.gsma.rcspushlocation+xml");
    }
}
