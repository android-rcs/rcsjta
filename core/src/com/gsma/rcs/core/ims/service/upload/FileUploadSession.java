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

import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.HttpUploadManager;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.HttpUploadTransferEventListener;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

import java.io.IOException;
import java.util.UUID;

/**
 * File upload session
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileUploadSession extends Thread implements HttpUploadTransferEventListener {

    private String mUploadId;

    private MmContent mFile;

    private boolean mFileIcon = false;

    protected HttpUploadManager mUploadManager;

    private FileUploadSessionListener mListener;

    private FileTransferHttpInfoDocument mFileInfoDoc;

    private final RcsSettings mRcsSettings;

    private static final Logger sLogger = Logger.getLogger(FileUploadSession.class.getName());

    /**
     * FileUploadSession state
     */
    public enum State {

        /**
         * Session is pending (not yet accepted by a final response to the first POST)
         */
        PENDING,

        /**
         * Session has been established (i.e. response OK to the first POST)
         */
        ESTABLISHED;
    }

    private State mSessionState;

    /**
     * Constructor
     * 
     * @param file Content of file to upload
     * @param fileIcon True if the stack must try to attach file icon
     * @param rcsSettings
     */
    public FileUploadSession(MmContent file, boolean fileIcon, RcsSettings rcsSettings) {
        super();
        mSessionState = State.PENDING;
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
     * Gets the session state
     * 
     * @return the session state
     */
    public State getSessionState() {
        return mSessionState;
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
            /* Create fileIcon content is requested */
            MmContent fileIconContent = null;
            if (mFileIcon) {
                fileIconContent = FileTransferUtils.createFileicon(mFile.getUri(), mUploadId,
                        mRcsSettings);
            }
            mUploadManager = new HttpUploadManager(mFile, fileIconContent, this, mUploadId,
                    mRcsSettings);
            byte[] result = mUploadManager.uploadFile();
            storeResult(result);
        } catch (SecurityException e) {
            sLogger.error(
                    new StringBuilder(
                            "File icon creation has failed as the file is not accessible for HTTP uploadId ")
                            .append(mUploadId).toString(), e);
            removeSession();
            mListener.handleUploadNotAllowedToSend();
        } catch (IOException e) {
            if (sLogger.isActivated()) {
                sLogger.debug(e.getMessage());
            }
            removeSession();
            mListener.handleUploadError(FileSharingError.MEDIA_UPLOAD_FAILED);
        } catch (FileAccessException e) {
            sLogger.error(new StringBuilder("Failed to initiate session for HTTP uploadId ")
                    .append(mUploadId).toString(), e);
            removeSession();
            mListener.handleUploadError(FileSharingError.MEDIA_UPLOAD_FAILED);
        } catch (PayloadException e) {
            sLogger.error(new StringBuilder("Failed to initiate session for HTTP uploadId ")
                    .append(mUploadId).toString(), e);
            removeSession();
            mListener.handleUploadError(FileSharingError.MEDIA_UPLOAD_FAILED);
        } catch (NetworkException e) {
            if (sLogger.isActivated()) {
                sLogger.debug(e.getMessage());
            }
            removeSession();
            mListener.handleUploadError(FileSharingError.MEDIA_UPLOAD_FAILED);
        } catch (RuntimeException e) {
            /*
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            sLogger.error(new StringBuilder("Failed to initiate session for HTTP uploadId ")
                    .append(mUploadId).toString(), e);
            removeSession();
            mListener.handleUploadError(FileSharingError.MEDIA_UPLOAD_FAILED);
        }
    }

    /**
     * Analyze the result
     * 
     * @param result Byte array result
     * @throws PayloadException
     */
    private void storeResult(byte[] result) throws PayloadException {
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
            mFileInfoDoc = FileTransferUtils.parseFileTransferHttpDocument(result, mRcsSettings);
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
            mListener.handleUploadError(FileSharingError.MEDIA_UPLOAD_FAILED);
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
        mSessionState = State.ESTABLISHED;
    }

    /**
     * HTTP transfer started
     */
    public void onHttpTransferStarted() {
        // Notify listener
        mListener.handleUploadStarted();
    }

    /**
     * HTTP transfer paused by user
     */
    public void onHttpTransferPausedByUser() {
        // Not used
    }

    /**
     * HTTP transfer paused by system
     */
    public void onHttpTransferPausedBySystem() {
        /*
         * Paused by system will be called for generic exceptions occurring in the lower layers and
         * in the scope of file upload this corresponds to failure since pause/resume does not exist
         * for file upload
         */
        removeSession();
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
    public void onHttpTransferResumed() {
        // Not used
    }

    /**
     * HTTP transfer progress
     * 
     * @param currentSize Current transfered size in bytes
     * @param totalSize Total size in bytes
     */
    public void onHttpTransferProgress(long currentSize, long totalSize) {
        // Notify listener
        mListener.handleUploadProgress(currentSize, totalSize);
    }

    @Override
    public void onHttpTransferNotAllowedToSend() {
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
