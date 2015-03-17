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
     * File content type
     */
    private String mType;

    /**
     * URI of the file
     */
    private Uri mFile;

    /**
     * Validity of the file
     */
    private long mValidity = 0;

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
     * Set file thumbnail
     * 
     * @param thumbnail Thumbnail
     */
    public void setFileThumbnail(FileTransferHttpThumbnail thumbnail) {
        mThumbnail = thumbnail;
    }

    /**
     * Get file thumbnail
     * 
     * @return File thumbnail
     */
    public FileTransferHttpThumbnail getFileThumbnail() {
        return mThumbnail;
    }

    /**
     * Get file transfer validity in milliseconds
     * 
     * @return Validity
     */
    public long getTransferValidity() {
        return mValidity;
    }

    /**
     * Set file transfer validity in milliseconds
     * 
     * @param validity
     */
    public void setTransferValidity(long validity) {
        mValidity = validity;
    }

    /**
     * Get file URI
     * 
     * @return File URI
     */
    public Uri getFileUri() {
        return mFile;
    }

    /**
     * Set file URI
     * 
     * @param file
     */
    public void setFileUri(Uri file) {
        mFile = file;
    }

    /**
     * Get file content type
     * 
     * @return File content type
     */
    public String getFileType() {
        return mType;
    }

    /**
     * Set file content type
     * 
     * @param type File content type
     */
    public void setFileType(String type) {
        mType = type;
    }

    /**
     * Get file size
     * 
     * @return File size
     */
    public int getFileSize() {
        return mSize;
    }

    /**
     * Set file size
     * 
     * @param size File size
     */
    public void setFileSize(int size) {
        mSize = size;
    }

    /**
     * Set the fileName
     * 
     * @param fileName FileName
     */
    public void setFilename(String fileName) {
        mFileName = fileName;
    }

    /**
     * Return the fileName
     * 
     * @return FileName
     */
    public String getFilename() {
        return mFileName;
    }
}
