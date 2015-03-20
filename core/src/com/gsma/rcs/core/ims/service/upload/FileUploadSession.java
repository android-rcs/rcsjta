/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.core.ims.service.upload;

import java.util.UUID;

import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.HttpUploadManager;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.HttpUploadTransferEventListener;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

/**
 * File upload session
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileUploadSession extends Thread implements HttpUploadTransferEventListener {

    private final static int UPLOAD_ERROR_UNSPECIFIED = -1;

    private String mUploadId;

    private MmContent mFile;

    private boolean mFileIcon = false;

    protected HttpUploadManager mUploadManager;

    private FileUploadSessionListener mListener;

    private FileTransferHttpInfoDocument mFileInfoDoc;

    private final RcsSettings mRcsSettings;

    private final static Logger sLogger = Logger.getLogger(FileUploadSession.class.getSimpleName());

    /**
     * Constructor
     * 
     * @param file Content of file to upload
     * @param fileIcon True if the stack must try to attach file icon
     * @param rcsSettings
     */
    public FileUploadSession(MmContent file, boolean fileIcon, RcsSettings rcsSettings) {
        super();

        mFile = file;
        mFileIcon = fileIcon;
        mUploadId = UUID.randomUUID().toString();
        mRcsSettings = rcsSettings;
    }

    /**
     * Add a listener event
     * 
     * @param listener Listener
     */
    public void addListener(FileUploadSessionListener listener) {
        mListener = listener;
    }

    /**
     * Returns the unique upload ID
     * 
     * @return ID
     */
    public String getUploadID() {
        return mUploadId;
    }

    /**
     * Returns the content to be uploaded
     * 
     * @return Content
     */
    public MmContent getContent() {
        return mFile;
    }

    /**
     * Returns the file info document of the uploaded file on the content server
     * 
     * @return XML document
     */
    public FileTransferHttpInfoDocument getFileInfoDocument() {
        return mFileInfoDoc;
    }

    /**
     * Background processing
     */
    public void run() {
        try {
            if (sLogger.isActivated()) {
                sLogger.info("Initiate a new HTTP upload ".concat(mUploadId));
            }

            // Create fileIcon content is requested
            MmContent fileIconContent = null;
            if (mFileIcon) {
                // Create the file icon
                try {
                    fileIconContent = FileTransferUtils.createFileicon(mFile.getUri(), mUploadId,
                            mRcsSettings);
                } catch (SecurityException e) {
                    /*
                     * TODO: This is not the proper way to handle the exception thrown. Will be
                     * taken care of in CR037
                     */
                    if (sLogger.isActivated()) {
                        sLogger.error(
                                "File icon creation has failed due to that the file is not accessible!",
                                e);
                    }
                    removeSession();
                    mListener.handleUploadNotAllowedToSend();
                    return;
                }
            }

            // Instantiate the upload manager
            mUploadManager = new HttpUploadManager(mFile, fileIconContent, this, mUploadId,
                    mRcsSettings);

            // Upload the file to the HTTP server
            byte[] result = mUploadManager.uploadFile();
            storeResult(result);
        } catch (Exception e) {
            if (sLogger.isActivated()) {
                sLogger.error("File transfer has failed", e);
            }
            removeSession();
            // Unexpected error
            mListener.handleUploadError(UPLOAD_ERROR_UNSPECIFIED);
        }
    }

    /**
     * Analyze the result
     * 
     * @param result Byte array result
     */
    private void storeResult(byte[] result) {
        // Check if upload has been cancelled
        if (mUploadManager.isCancelled()) {
            return;
        }

        // Parse the result:
        // <?xml version="1.0" encoding="UTF-8"?>
        // <file>
        // <file-info type="thumbnail">
        // <file-size>6208</file-size>
        // <content-type>image/jpeg</content-type>
        // <data url = "https://ftcontentserver.rcs/download?id=001"
        // until="2014-08-13T17:42:10.000+02:00"/>
        // </file-info>
        //
        // <file-info type="file">
        // <file-size>1699846</file-size>
        // <file-name>IMG_20140805_134311.jpg</file-name>
        // <content-type>image/jpeg</content-type>
        // <data url = "https://ftcontentserver.rcs/download?id=abb"
        // until="2014-08-13T17:42:10.000+02:00"/>
        // </file-info>
        // </file>
        if (result != null) {
            mFileInfoDoc = FileTransferUtils.parseFileTransferHttpDocument(result);
        }
        if (mFileInfoDoc != null) {
            // File uploaded with success
            if (sLogger.isActivated()) {
                sLogger.debug("Upload done with success: ".concat(mFileInfoDoc.getUri().toString()));
            }

            removeSession();
            mListener.handleUploadTerminated(mFileInfoDoc);
        } else {
            // Upload error
            if (sLogger.isActivated()) {
                sLogger.debug("Upload has failed");
            }
            removeSession();
            // Notify listener
            mListener.handleUploadError(UPLOAD_ERROR_UNSPECIFIED);
        }
    }

    /**
     * Posts an interrupt request to this Thread
     */
    public void interrupt() {
        super.interrupt();

        // Interrupt the upload
        mUploadManager.interrupt();

        if (mFileInfoDoc == null) {
            removeSession();
            mListener.handleUploadAborted();
        }
    }

    /**
     * Notify the start of the HTTP Upload transfer (once the thumbnail transfer is done). <br>
     * The upload resume is only possible once thumbnail is transferred
     */
    public void uploadStarted() {
        // Not used
    }

    /**
     * HTTP transfer started
     */
    public void httpTransferStarted() {
        // Notify listener
        mListener.handleUploadStarted();
    }

    /**
     * HTTP transfer paused by user
     */
    public void httpTransferPausedByUser() {
        // Not used
    }

    /**
     * HTTP transfer paused by system
     */
    public void httpTransferPausedBySystem() {
        /*
         * Paused by system will be called for generic exceptions occurring in the lower layers and
         * in the scope of file upload this corresponds to failure since pause/resume does not exist
         * for file upload
         */
        removeSession();
        mListener.handleUploadError(UPLOAD_ERROR_UNSPECIFIED);
    }

    /**
     * HTTP transfer paused
     */
    public void httpTransferPausedByRemote() {
        // Not used
    }

    /**
     * HTTP transfer resumed
     */
    public void httpTransferResumed() {
        // Not used
    }

    /**
     * HTTP transfer progress
     * 
     * @param currentSize Current transfered size in bytes
     * @param totalSize Total size in bytes
     */
    public void httpTransferProgress(long currentSize, long totalSize) {
        // Notify listener
        mListener.handleUploadProgress(currentSize, totalSize);
    }

    @Override
    public void httpTransferNotAllowedToSend() {
        removeSession();
        mListener.handleUploadNotAllowedToSend();
    }

    /**
     * Start session
     */
    public void startSession() {
        Core.getInstance().getImService().addSession(this);
        start();
    }

    /**
     * Remove session
     */
    public void removeSession() {
        Core.getInstance().getImService().removeSession(this);
    }
}
