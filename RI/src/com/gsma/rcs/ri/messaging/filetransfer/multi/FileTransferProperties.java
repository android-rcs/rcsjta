/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.ri.messaging.filetransfer.multi;

import android.net.Uri;

/**
 * FileTransferProperties
 */
public class FileTransferProperties {

    private final Uri mUri;
    private final String mMimeType;
    private boolean mFileIcon;
    private boolean mAudioMessage;
    private final String mFilename;
    private final long mSize;
    private String mStatus;
    private int mProgress;
    private String mReasonCode;

    /**
     * Default constructor
     *
     * @param uri the file URI
     * @param filename the file name
     * @param size the file size
     * @param mimeType the mime type
     */
    public FileTransferProperties(Uri uri, String filename, long size, String mimeType) {
        mUri = uri;
        mFilename = filename;
        mSize = size;
        mMimeType = mimeType;
    }

    /**
     * Gets the file URI
     *
     * @return the file URI
     */
    public Uri getUri() {
        return mUri;
    }

    /**
     * Gets the file icon flag
     *
     * @return the file icon flag
     */
    public boolean isFileicon() {
        return mFileIcon;
    }

    /**
     * Sets file icon flag
     *
     * @param fileicon the file icon flag
     */
    public void setFileicon(boolean fileicon) {
        mFileIcon = fileicon;
    }

    /**
     * @return the FileName
     */
    public String getFilename() {
        return mFilename;
    }

    /**
     * Gets the file transfer status
     *
     * @return the Status
     */
    public String getStatus() {
        return mStatus;
    }

    /**
     * Sets the file transfer status
     *
     * @param status the status
     */
    public void setStatus(String status) {
        mStatus = status;
    }

    /**
     * Gets the file transfer progress (in percentage)
     *
     * @return the file transfer progress
     */
    public int getProgress() {
        return mProgress;
    }

    /**
     * Sets the file transfer progress (in percentage)
     *
     * @param progress the file transfer progress
     */
    public void setProgress(int progress) {
        mProgress = progress;
    }

    /**
     * Gets the file size
     *
     * @return the size
     */
    public long getSize() {
        return mSize;
    }

    /**
     * Gets the reason code
     *
     * @return the reason code
     */
    public String getReasonCode() {
        return mReasonCode;
    }

    /**
     * Sets the reason code
     *
     * @param reasonCode the reason code
     */
    public void setReasonCode(String reasonCode) {
        mReasonCode = reasonCode;
    }

    /**
     * Gets the mime type
     *
     * @return the mime type
     */
    public String getMimeType() {
        return mMimeType;
    }

    /**
     * Gets the audio message flag
     *
     * @return the audio message flag
     */
    public boolean isAudioMessage() {
        return mAudioMessage;
    }

    /**
     * Sets the audio message flag
     *
     * @param audioMessage the audio message flag
     */
    public void setAudioMessage(boolean audioMessage) {
        mAudioMessage = audioMessage;
    }
}
