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

package com.gsma.rcs.core.ims.service.im.filetransfer.http;

import com.gsma.services.rcs.filetransfer.FileTransferLog;

import android.net.Uri;

/**
 * File transfer over HTTP thumbnail
 * 
 * @author vfml3370
 */
public class FileTransferHttpThumbnail {

    private int mSize = 0;

    private String mMimeType;

    private Uri mUri;

    private long mExpiration = FileTransferLog.UNKNOWN_EXPIRATION;

    /**
     * Constructor
     */
    public FileTransferHttpThumbnail() {
    }

    /**
     * Gets expiration
     * 
     * @return expiration in milliseconds
     */
    public long getExpiration() {
        return mExpiration;
    }

    /**
     * Sets expiration
     * 
     * @param expiration
     */
    public void setExpiration(long expiration) {
        mExpiration = expiration;
    }

    /**
     * Gets URI
     * 
     * @return URI
     */
    public Uri getUri() {
        return mUri;
    }

    /**
     * Sets URI
     * 
     * @param uri URI
     */
    public void setUri(Uri uri) {
        mUri = uri;
    }

    /**
     * Gets mime type
     * 
     * @return mime type
     */
    public String getMimeType() {
        return mMimeType;
    }

    /**
     * Sets mime type
     * 
     * @param mimetype
     */
    public void setMimeType(String mimetype) {
        mMimeType = mimetype;
    }

    /**
     * Gets size
     * 
     * @return size
     */
    public int getSize() {
        return mSize;
    }

    /**
     * Sets size
     * 
     * @param size
     */
    public void setSize(int size) {
        mSize = size;
    }
}
