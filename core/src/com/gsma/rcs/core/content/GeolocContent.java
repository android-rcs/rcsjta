/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.core.content;

import com.gsma.rcs.core.ims.service.im.chat.geoloc.GeolocInfoDocument;

import android.net.Uri;

/**
 * Geoloc content
 * 
 * @author vfml3370
 */
public class GeolocContent extends MmContent {

    private byte[] mData;

    /**
     * Encoding type
     */
    public static final String ENCODING = GeolocInfoDocument.MIME_TYPE;

    /**
     * Constructor
     * 
     * @param fileName File name
     * @param size Content size
     * @param data Geoloc
     */
    public GeolocContent(String fileName, long size, byte[] data) {
        super(fileName, size, ENCODING);
        mData = data;
    }

    /**
     * Constructor
     * 
     * @param geolocFile URI
     * @param size Content size
     * @param fileName Filename
     */
    public GeolocContent(Uri geolocFile, long size, String fileName) {
        super(geolocFile, ENCODING, size, fileName);
    }

    public byte[] getData() {
        return mData;
    }
}
