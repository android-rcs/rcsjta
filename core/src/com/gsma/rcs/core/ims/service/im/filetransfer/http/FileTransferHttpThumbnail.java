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

    /**
     * Thumbnail size
     */
    private int size = 0;

    /**
     * Thumbnail content type
     */
    private String contentType = null;

    /**
     * Thumbnail URI
     */
    private Uri thumbnail = null;

    /**
     * Validity of the file thumbnail
     */
    private long validity = 0;

    /**
     * Constructor
     */
    public FileTransferHttpThumbnail() {
    }

    /**
     * Get thumbnail validity
     * 
     * @return Thumbnail validity
     */
    public long getThumbnailValidity() {
        return validity;
    }

    /**
     * Set thumbnail validity
     * 
     * @param validity Thumbnail validity
     */
    public void setThumbnailValidity(long validity) {
        this.validity = validity;
    }

    /**
     * Get thumbnail URI
     * 
     * @return Thumbnail URI
     */
    public Uri getThumbnailUri() {
        return thumbnail;
    }

    /**
     * Set thumbnail URI
     * 
     * @param thumbnail Thumbnail URI
     */
    public void setThumbnailUri(Uri thumbnail) {
        this.thumbnail = thumbnail;
    }

    /**
     * Get thumbnail content type
     * 
     * @return Thumbnail content type
     */
    public String getThumbnailType() {
        return contentType;
    }

    /**
     * Set thumbnail content type
     * 
     * @param type
     */
    public void setThumbnailType(String type) {
        this.contentType = type;
    }

    /**
     * Get thumbnail size
     * 
     * @return Thumbnail size
     */
    public int getThumbnailSize() {
        return size;
    }

    /**
     * Set thumbnail size
     * 
     * @param size Thumbnail size
     */
    public void setThumbnailSize(int size) {
        this.size = size;
    }
}
