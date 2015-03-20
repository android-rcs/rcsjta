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
 * File transfer over HTTP info document
 * 
 * @author vfml3370
 */
public class FileTransferHttpInfoDocument {
    /**
     * MIME type
     */
    public static final String MIME_TYPE = "application/vnd.gsma.rcs-ft-http+xml";

    /**
     * File size
     */
    private int mSize = 0;

    /**
     * File mime type
     */
    private String mMimeType;

    /**
     * URI of the file
     */
    private Uri mUri;

    /**
     * Expiration of the file
     */
    private long mExpiration = FileTransferLog.UNKNOWN_EXPIRATION;

    /**
     * File thumbnail
     */
    private FileTransferHttpThumbnail mThumbnail;

    /**
     * Filename
     */
    private String mFileName;

    /**
     * Constructor
     */
    public FileTransferHttpInfoDocument() {
    }

    /**
     * Sets file thumbnail
     * 
     * @param thumbnail Thumbnail
     */
    public void setFileThumbnail(FileTransferHttpThumbnail thumbnail) {
        mThumbnail = thumbnail;
    }

    /**
     * Gets file thumbnail
     * 
     * @return File thumbnail
     */
    public FileTransferHttpThumbnail getFileThumbnail() {
        return mThumbnail;
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
     * Sets file expiration
     * 
     * @param expiration in milliseconds
     */
    public void setExpiration(long expiration) {
        mExpiration = expiration;
    }

    /**
     * Gets file URI
     * 
     * @return File URI
     */
    public Uri getUri() {
        return mUri;
    }

    /**
     * Sets file URI
     * 
     * @param file
     */
    public void setUri(Uri file) {
        mUri = file;
    }

    /**
     * Gets file mime type
     * 
     * @return File mime type
     */
    public String getMimeType() {
        return mMimeType;
    }

    /**
     * Sets file mime type
     * 
     * @param mimeType File mime type
     */
    public void setMimeType(String mimeType) {
        mMimeType = mimeType;
    }

    /**
     * Gets file size
     * 
     * @return File size
     */
    public int getSize() {
        return mSize;
    }

    /**
     * Sets file size
     * 
     * @param size File size
     */
    public void setSize(int size) {
        mSize = size;
    }

    /**
     * Sets the fileName
     * 
     * @param fileName FileName
     */
    public void setFilename(String fileName) {
        mFileName = fileName;
    }

    /**
     * Gets the fileName
     * 
     * @return FileName
     */
    public String getFilename() {
        return mFileName;
    }
}
