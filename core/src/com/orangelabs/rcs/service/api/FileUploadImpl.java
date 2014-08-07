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

import com.gsma.services.rcs.ext.upload.FileUpload;
import com.gsma.services.rcs.ext.upload.FileUploadInfo;
import com.gsma.services.rcs.ext.upload.IFileUpload;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.HttpUploadTransferEventListener;
import com.orangelabs.rcs.core.ims.service.upload.FileUploadSession;
import com.orangelabs.rcs.service.broadcaster.IFileUploadEventBroadcaster;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * File upload implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileUploadImpl extends IFileUpload.Stub implements HttpUploadTransferEventListener {
	
	/**
	 * Core session
	 */
	private FileUploadSession session;

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
		// TODO
		return null;
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
	 * Notify the start of the HTTP Upload transfer (once the thumbnail transfer is done).
	 * <br>The upload resume is only possible once thumbnail is transferred 
	 */
    public void uploadStarted() {
		// Not used
	}
	
    /**
     * HTTP transfer started
     */
    public void httpTransferStarted() {
    	synchronized(lock) {
    		state = FileUpload.State.STARTED;

    		// Notify event listeners
			mFileUploadEventBroadcaster.broadcastFileUploadStateChanged(getUploadId(),
					FileUpload.State.STARTED);
	    }
    }
    
    /**
     * HTTP transfer paused
     */
    public void httpTransferPaused() {
		// Not supported
    }
    
    /**
     * HTTP transfer resumed
     */
    public void httpTransferResumed() {
		// Not supported
    }

    /**
     * HTTP transfer progress
     *
     * @param currentSize Current transfered size in bytes
     * @param totalSize Total size in bytes
     */
    public void httpTransferProgress(long currentSize, long totalSize) {
    	synchronized(lock) {
			// Notify event listeners
			mFileUploadEventBroadcaster.broadcastFileUploadProgress(getUploadId(), currentSize, totalSize);
	     }
    }	
}
