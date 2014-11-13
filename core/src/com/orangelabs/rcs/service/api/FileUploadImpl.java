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
package com.orangelabs.rcs.service.api;

import android.net.Uri;

import com.gsma.services.rcs.upload.FileUpload;
import com.gsma.services.rcs.upload.FileUploadInfo;
import com.gsma.services.rcs.upload.IFileUpload;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpThumbnail;
import com.orangelabs.rcs.core.ims.service.upload.FileUploadSession;
import com.orangelabs.rcs.core.ims.service.upload.FileUploadSessionListener;
import com.orangelabs.rcs.service.broadcaster.IFileUploadEventBroadcaster;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * File upload implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileUploadImpl extends IFileUpload.Stub implements FileUploadSessionListener {
	
	/**
	 * Core session
	 */
	private FileUploadSession session;

	/**
	 * File upload listener
	 */
	private final IFileUploadEventBroadcaster mFileUploadEventBroadcaster;
	
	/**
	 * Upload state	
	 */
	private int state = FileUpload.State.INACTIVE;

	/**
	 * Lock used for synchronisation
	 */
	private final Object lock = new Object();

	/**
	 * The logger
	 */
	private final Logger logger = Logger.getLogger(getClass().getName());

	/**
	 * Constructor
	 *
	 * @param session Session
	 * @param broadcaster Event broadcaster
	 */
	public FileUploadImpl(FileUploadSession session, IFileUploadEventBroadcaster broadcaster) {
		this.session = session;
		this.mFileUploadEventBroadcaster = broadcaster;
		
		session.addListener(this);
	}

	/**
	 * Returns the upload ID of the upload
	 * 
	 * @return Upload ID
	 */
	public String getUploadId() {
		return session.getUploadID();
	}
	
	/**
	 * Returns info related to upload file
	 *
	 * @return Upload info or null if not yet upload or in case of error
	 * @see FileUploadInfo
	 */
	public FileUploadInfo getUploadInfo() {
		if (session != null) {
			FileTransferHttpInfoDocument file = session.getFileInfoDocument();
			FileTransferHttpThumbnail fileIcon = file.getFileThumbnail();
			if (fileIcon != null) {
				return new FileUploadInfo(
						file.getFileUri(),
						file.getTransferValidity(),
						file.getFilename(),
						file.getFileSize(),
						file.getFileType(),
						fileIcon.getThumbnailUri(),
						fileIcon.getThumbnailValidity(),
						fileIcon.getThumbnailSize(),
						fileIcon.getThumbnailType());
			} else {
				return new FileUploadInfo(
						file.getFileUri(),
						file.getTransferValidity(),
						file.getFilename(),
						file.getFileSize(),
						file.getFileType(),
						Uri.EMPTY,
						0,
						0,
						"");
			}
		} else {
			return null;
		}
	}	
	
	/**
	 * Returns the URI of the file to upload
	 * 
	 * @return Uri
	 */
	public Uri getFile() {
		return session.getContent().getUri();
	}

	/**
	 * Returns the state of the file upload
	 * 
	 * @return State 
	 */
	public int getState() {
		return state;
	}

	/**
	 * Aborts the upload
	 */
	public void abortUpload() {
		if (logger.isActivated()) {
			logger.info("Cancel session");
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
    	synchronized(lock) {
	    	if (logger.isActivated()) {
	    		logger.debug("File upload started");
	    	}
    		state = FileUpload.State.STARTED;
			mFileUploadEventBroadcaster.broadcastStateChanged(getUploadId(), state);
	    }
    }

	/**
	 * Upload progress
	 * 
	 * @param currentSize Data size transfered 
	 * @param totalSize Total size to be transfered
	 */
    public void handleUploadProgress(long currentSize, long totalSize) {
    	synchronized(lock) {
            mFileUploadEventBroadcaster.broadcastProgressUpdate(getUploadId(), currentSize, totalSize);
	     }
    }
    
    /**
     * Upload terminated with success
     * 
     * @param info File info document
     */
    public void handleUploadTerminated(FileTransferHttpInfoDocument info) {
    	synchronized(lock) {
	    	if (logger.isActivated()) {
	    		logger.debug("File upload terminated");
	    	}
    		state = FileUpload.State.TRANSFERRED;

            String uploadId = getUploadId();
            mFileUploadEventBroadcaster.broadcastStateChanged(uploadId, state);
			
            FileUploadServiceImpl.removeFileUploadSession(uploadId);
	    }
    }

    /**
     * Upload error
     * 
     * @param error Error
     */
    public void handleUploadError(int error) {
    	synchronized(lock) {
	    	if (logger.isActivated()) {
	    		logger.debug("File upload failed");
	    	}
    		state = FileUpload.State.FAILED;

            String uploadId = getUploadId();
            mFileUploadEventBroadcaster.broadcastStateChanged(uploadId, state);
			
            FileUploadServiceImpl.removeFileUploadSession(uploadId);
	    }
    }

    /**
     * Upload aborted
     */
    public void handleUploadAborted() {
    	synchronized(lock) {
	    	if (logger.isActivated()) {
	    		logger.debug("File upload aborted");
	    	}
    		state = FileUpload.State.ABORTED;

            String uploadId = getUploadId();
            mFileUploadEventBroadcaster.broadcastStateChanged(uploadId, state);
			
            FileUploadServiceImpl.removeFileUploadSession(uploadId);
	    }
    }

	@Override
	public void handleUploadNotAllowedToSend() {
		if (logger.isActivated()) {
			logger.debug("File upload not allowed");
		}
		String uploadId = getUploadId();
		synchronized (lock) {
			mFileUploadEventBroadcaster.broadcastStateChanged(uploadId,
					FileUpload.State.FAILED);
			FileUploadServiceImpl.removeFileUploadSession(uploadId);
		}
	}
}
