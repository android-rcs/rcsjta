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

import com.gsma.rcs.core.content.ContentManager;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.gsma.rcs.provider.messaging.FileTransferData;
import com.gsma.rcs.provider.settings.RcsSettings;

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
    private Uri mFile;

    /**
     * Expiration of the file
     */
    private long mExpiration = FileTransferData.UNKNOWN_EXPIRATION;

    /**
     * File thumbnail
     */
    private FileTransferHttpThumbnail mFileIcon;

    /**
     * Filename
     */
    private String mFileName;

    /**
     * File disposition
     */
    private String mFileDispo = FileSharingSession.FILE_DISPOSITION_ATTACH;

    /**
     * Playing length
     */
    private int mPlayingLength = 0;

    private final RcsSettings mRcsSettings;

    /**
     * Constructor
     * 
     * @param rcsSettings the RCS settings accessor
     */
    public FileTransferHttpInfoDocument(RcsSettings rcsSettings) {
        mRcsSettings = rcsSettings;
    }

    public FileTransferHttpInfoDocument(RcsSettings rcsSettings, Uri file, String fileName,
            int size, String mimeType, long expiration, FileTransferHttpThumbnail fileIcon) {
        this(rcsSettings);
        mFile = file;
        mFileName = fileName;
        mSize = size;
        mMimeType = mimeType;
        mExpiration = expiration;
        mFileIcon = fileIcon;
    }

    /**
     * Sets file thumbnail
     * 
     * @param thumbnail Thumbnail
     */
    public void setFileThumbnail(FileTransferHttpThumbnail thumbnail) {
        mFileIcon = thumbnail;
    }

    /**
     * Gets file thumbnail
     * 
     * @return File thumbnail
     */
    public FileTransferHttpThumbnail getFileThumbnail() {
        return mFileIcon;
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
        return mFile;
    }

    /**
     * Sets file URI
     * 
     * @param file the file Uri
     */
    public void setUri(Uri file) {
        mFile = file;
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

    /**
     * Sets the file disposition
     *
     * @param fileDispo File disposition
     */
    public void setFileDisposition(String fileDispo) {
        mFileDispo = fileDispo;
    }

    /**
     * Gets the file disposition
     *
     * @return File disposition
     */
    public String getFileDisposition() {
        return mFileDispo;
    }

    /**
     * Sets the playing length
     *
     * @param length Length in seconds
     */
    public void setPlayingLength(int length) {
        mPlayingLength = length;
    }

    /**
     * Gets the playing length or 0 is not set
     *
     * @return Length in seconds
     */
    public int getPlayingLength() {
        return mPlayingLength;
    }

    /**
     * Gets local MmContent
     * 
     * @return local MmCOntent
     */
    public MmContent getLocalMmContent() {
        Uri file = ContentManager.generateUriForReceivedContent(mFileName, mMimeType, mRcsSettings);
        MmContent content = ContentManager.createMmContent(file, mMimeType, mSize,
                mFileName);
        if (FileSharingSession.FILE_DISPOSITION_RENDER.equals(mFileDispo)) {
            content.setPlayable(true);
        }
        return content;
    }
}
