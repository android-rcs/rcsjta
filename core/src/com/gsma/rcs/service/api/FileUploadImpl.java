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
import com.gsma.services.rcs.upload.FileUpload;
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

    private final FileUploadStorageAccessor mFileUploadStorageAccessor;

    private final Object mLock = new Object();

    private final Logger mLogger = Logger.getLogger(getClass().getName());

    /**
     * Constructor
     * 
     * @param uploadId Unique ID of FileUpload
     * @param broadcaster Event broadcaster
     * @param imService InstantMessagingService
     * @param fileUploadService FileUploadServiceImpl
     * @param file the URI of file to upload
     */
    public FileUploadImpl(String uploadId, IFileUploadEventBroadcaster broadcaster,
            InstantMessagingService imService, FileUploadServiceImpl fileUploadService, Uri file) {
        mUploadId = uploadId;
        mBroadcaster = broadcaster;
        mImService = imService;
        mFileUploadService = fileUploadService;
        mFileUploadStorageAccessor = new FileUploadStorageAccessor(file);
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
        if (session == null) {
            return mFileUploadStorageAccessor.getInfo();
        }
        if ((file = session.getFileInfoDocument()) == null) {
            return null;
        }
        return createFileUploadInfo(file);
    }

    /**
     * Returns the URI of the file to upload
     * 
     * @return the file URI
     */
    public Uri getFile() {
        FileUploadSession session = mImService.getFileUploadSession(mUploadId);
        if (session == null) {
            return mFileUploadStorageAccessor.getFile();
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
            return mFileUploadStorageAccessor.getState().toInt();
        }
        synchronized (mLock) {
            if (FileUploadSession.State.PENDING == session.getSessionState()) {
                return FileUpload.State.INACTIVE.toInt();
            }
            return FileUpload.State.STARTED.toInt();
        }
    }

    /**
     * Aborts the upload
     */
    public void abortUpload() {
        if (mLogger.isActivated()) {
            mLogger.info("Cancel session");
        }
        final FileUploadSession session = mImService.getFileUploadSession(mUploadId);
        if (session == null) {
            /*
             * TODO: Throw proper exception as part of CR037 implementation
             */
            throw new IllegalStateException("Cannot abot session with ID=".concat(mUploadId));
        }

        // Abort the session
        new Thread() {
            public void run() {
                session.interrupt();
            }
        }.start();
    }

    private FileUploadInfo createFileUploadInfo(FileTransferHttpInfoDocument file) {
        FileTransferHttpThumbnail fileicon = file.getFileThumbnail();
        if (fileicon != null) {
            return new FileUploadInfo(file.getUri(), file.getExpiration(), file.getFilename(),
                    file.getSize(), file.getMimeType(), fileicon.getUri(),
                    fileicon.getExpiration(), fileicon.getSize(), fileicon.getMimeType());
        }
        return new FileUploadInfo(file.getUri(), file.getExpiration(), file.getFilename(),
                file.getSize(), file.getMimeType());
    }

    /*------------------------------- SESSION EVENTS ----------------------------------*/

    private void setStateAndBroadcast(String uploadId, FileUpload.State state) {
        mFileUploadStorageAccessor.setState(state);
        mBroadcaster.broadcastStateChanged(mUploadId, state);
    }

    /**
     * Upload started
     */
    public void handleUploadStarted() {
        if (mLogger.isActivated()) {
            mLogger.debug("File upload started");
        }
        synchronized (mLock) {
            setStateAndBroadcast(mUploadId, FileUpload.State.STARTED);
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

    private void setStateAndInfoThenBroadcast(String uploadId, FileUpload.State state,
            FileUploadInfo info) {
        mFileUploadStorageAccessor.setInfo(info);
        setStateAndBroadcast(uploadId, state);
        mBroadcaster.broadcastUploaded(uploadId, info);
    }
    
    /**
     * Upload terminated with success
     * 
     * @param info File info document
     */
    public void handleUploadTerminated(FileTransferHttpInfoDocument info) {
        if (mLogger.isActivated()) {
            mLogger.debug("File upload terminated");
        }
        synchronized (mLock) {
            mFileUploadService.removeFileUpload(mUploadId);
            setStateAndInfoThenBroadcast(mUploadId, FileUpload.State.TRANSFERRED,
                    createFileUploadInfo(info));

        }
    }

    /**
     * Upload error
     * 
     * @param error Error
     */
    public void handleUploadError(int error) {
        if (mLogger.isActivated()) {
            mLogger.debug("File upload failed");
        }
        synchronized (mLock) {
            mFileUploadService.removeFileUpload(mUploadId);
            setStateAndBroadcast(mUploadId, FileUpload.State.FAILED);
        }
    }

    /**
     * Upload aborted
     */
    public void handleUploadAborted() {
        if (mLogger.isActivated()) {
            mLogger.debug("File upload aborted");
        }
        synchronized (mLock) {
            mFileUploadService.removeFileUpload(mUploadId);
            setStateAndBroadcast(mUploadId, FileUpload.State.ABORTED);
        }
    }

    @Override
    public void handleUploadNotAllowedToSend() {
        if (mLogger.isActivated()) {
            mLogger.debug("File upload not allowed");
        }
        synchronized (mLock) {
            mFileUploadService.removeFileUpload(mUploadId);
            setStateAndBroadcast(mUploadId, FileUpload.State.FAILED);
        }
    }
}
