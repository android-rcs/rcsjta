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

package com.gsma.rcs.service.api;

import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpThumbnail;
import com.gsma.rcs.core.ims.service.upload.FileUploadSession;
import com.gsma.rcs.core.ims.service.upload.FileUploadSessionListener;
import com.gsma.rcs.service.broadcaster.IFileUploadEventBroadcaster;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.filetransfer.FileTransferLog;
import com.gsma.services.rcs.upload.FileUpload;
import com.gsma.services.rcs.upload.FileUpload.State;
import com.gsma.services.rcs.upload.FileUploadInfo;
import com.gsma.services.rcs.upload.IFileUpload;

import android.net.Uri;

/**
 * File upload implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileUploadImpl extends IFileUpload.Stub implements FileUploadSessionListener {

    private final String mUploadId;

    private final IFileUploadEventBroadcaster mBroadcaster;

    private final InstantMessagingService mImService;

    private final FileUploadServiceImpl mFileUploadService;

    /**
     * Upload state
     */
    private State mState;

    /**
     * mLock used for synchronization
     */
    private final Object mLock = new Object();

    /**
     * The logger
     */
    private final Logger logger = Logger.getLogger(getClass().getName());

    /**
     * Constructor
     * 
     * @param uploadId Unique ID of FileUpload
     * @param broadcaster Event broadcaster
     * @param imService InstantMessagingService
     * @param fileUploadService FileUploadServiceImpl
     */
    public FileUploadImpl(String uploadId, IFileUploadEventBroadcaster broadcaster,
            InstantMessagingService imService, FileUploadServiceImpl fileUploadService) {
        mUploadId = uploadId;
        mBroadcaster = broadcaster;
        mImService = imService;
        mFileUploadService = fileUploadService;
        mState = State.INACTIVE;
    }

    /**
     * Returns the upload ID of the upload
     * 
     * @return Upload ID
     */
    public String getUploadId() {
        return mUploadId;
    }

    /**
     * Returns info related to upload file
     * 
     * @return Upload info or null if not yet uploaded or in case of error
     * @see FileUploadInfo
     */
    public FileUploadInfo getUploadInfo() {
        FileUploadSession session = mImService.getFileUploadSession(mUploadId);
        FileTransferHttpInfoDocument file;
        if (session == null || (file = session.getFileInfoDocument()) == null) {
            /*
             * TODO: Throw proper exception as part of CR037 as persisted storage not available for
             * this service!
             */
            throw new IllegalStateException(
                    "File upload info not found for session ID=".concat(mUploadId));
        }

        FileTransferHttpThumbnail fileicon = file.getFileThumbnail();
        if (fileicon != null) {
            return new FileUploadInfo(file.getUri(), file.getExpiration(), file.getFilename(),
                    file.getSize(), file.getMimeType(), fileicon.getUri(),
                    fileicon.getExpiration(), fileicon.getSize(), fileicon.getMimeType());
        }
        return new FileUploadInfo(file.getUri(), file.getExpiration(), file.getFilename(),
                file.getSize(), file.getMimeType(), Uri.EMPTY, FileTransferLog.UNKNOWN_EXPIRATION,
                0, "");
    }

    /**
     * Returns the URI of the file to upload
     * 
     * @return Uri
     */
    public Uri getFile() {
        FileUploadSession session = mImService.getFileUploadSession(mUploadId);
        if (session == null) {
            /*
             * TODO: Throw proper exception as part of CR037 as persisted storage not available for
             * this service!
             */
            throw new IllegalStateException("File not found for session ID=".concat(mUploadId));
        }
        return session.getContent().getUri();
    }

    /**
     * Returns the state of the file upload
     * 
     * @return State
     */
    public int getState() {
        FileUploadSession session = mImService.getFileUploadSession(mUploadId);
        if (session == null) {
            /*
             * TODO: Throw proper exception as part of CR037 as persisted storage not available for
             * this service!
             */
            throw new IllegalStateException("State not found for session ID=".concat(mUploadId));
        }
        synchronized (mLock) {
            return mState.toInt();
        }
    }

    /**
     * Aborts the upload
     */
    public void abortUpload() {
        if (logger.isActivated()) {
            logger.info("Cancel session");
        }
        final FileUploadSession session = mImService.getFileUploadSession(mUploadId);
        if (session == null) {
            /*
             * TODO: Throw proper exception as part of CR037 implementation
             */
            throw new IllegalStateException("Cannot abot session with ID=".concat(mUploadId));
        }

        // Abort the session
        Thread t = new Thread() {
            public void run() {
                session.interrupt();
            }
        };
        t.start();
    }

    /*------------------------------- SESSION EVENTS ----------------------------------*/

    /**
     * Upload started
     */
    public void handleUploadStarted() {
        if (logger.isActivated()) {
            logger.debug("File upload started");
        }
        synchronized (mLock) {
            mState = FileUpload.State.STARTED;
            mBroadcaster.broadcastStateChanged(mUploadId, mState);
        }
    }

    /**
     * Upload progress
     * 
     * @param currentSize Data size transfered
     * @param totalSize Total size to be transfered
     */
    public void handleUploadProgress(long currentSize, long totalSize) {
        synchronized (mLock) {
            mBroadcaster.broadcastProgressUpdate(mUploadId, currentSize, totalSize);
        }
    }

    /**
     * Upload terminated with success
     * 
     * @param info File info document
     */
    public void handleUploadTerminated(FileTransferHttpInfoDocument info) {
        if (logger.isActivated()) {
            logger.debug("File upload terminated");
        }
        synchronized (mLock) {
            mState = FileUpload.State.TRANSFERRED;
            mFileUploadService.removeFileUpload(mUploadId);
            mBroadcaster.broadcastStateChanged(mUploadId, mState);
        }
    }

    /**
     * Upload error
     * 
     * @param error Error
     */
    public void handleUploadError(int error) {
        if (logger.isActivated()) {
            logger.debug("File upload failed");
        }
        synchronized (mLock) {
            mState = FileUpload.State.FAILED;
            mFileUploadService.removeFileUpload(mUploadId);
            mBroadcaster.broadcastStateChanged(mUploadId, mState);
        }
    }

    /**
     * Upload aborted
     */
    public void handleUploadAborted() {
        if (logger.isActivated()) {
            logger.debug("File upload aborted");
        }
        synchronized (mLock) {
            mState = FileUpload.State.ABORTED;
            mFileUploadService.removeFileUpload(mUploadId);
            mBroadcaster.broadcastStateChanged(mUploadId, mState);
        }
    }

    @Override
    public void handleUploadNotAllowedToSend() {
        if (logger.isActivated()) {
            logger.debug("File upload not allowed");
        }
        synchronized (mLock) {
            mState = State.FAILED;
            mFileUploadService.removeFileUpload(mUploadId);
            mBroadcaster.broadcastStateChanged(mUploadId, mState);
        }
    }
}
