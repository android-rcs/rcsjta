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

import android.net.Uri;

/**
 * File transfer over HTTP thumbnail
 * 
 * @author vfml3370
 */
public class FileTransferHttpThumbnail {

    private int mSize = 0;

    private String mContentType;

    private Uri mUri;

    private long mValidity = 0;

    /**
     * Constructor
     */
    public FileTransferHttpThumbnail() {
    }

    /**
     * Gets validity
     * 
     * @return validity
     */
    public long getValidity() {
        return mValidity;
    }

    /**
     * Sets validity
     * 
     * @param validity validity
     */
    public void setValidity(long validity) {
        mValidity = validity;
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
     * @param thumbnail URI
     */
    public void setUri(Uri thumbnail) {
        mUri = thumbnail;
    }

    /**
     * Gets content type
     * 
     * @return content type
     */
    public String getType() {
        return mContentType;
    }

    /**
     * Sets content type
     * 
     * @param type
     */
    public void setType(String type) {
        mContentType = type;
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
