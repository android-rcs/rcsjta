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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import android.net.Uri;
import android.os.IBinder;

import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.upload.FileUploadServiceConfiguration;
import com.gsma.services.rcs.upload.IFileUpload;
import com.gsma.services.rcs.upload.IFileUploadListener;
import com.gsma.services.rcs.upload.IFileUploadService;
import com.orangelabs.rcs.core.content.ContentManager;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.orangelabs.rcs.core.ims.service.upload.FileUploadSession;
import com.orangelabs.rcs.platform.file.FileDescription;
import com.orangelabs.rcs.platform.file.FileFactory;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.service.broadcaster.FileUploadEventBroadcaster;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * File upload service implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileUploadServiceImpl extends IFileUploadService.Stub {

	/**
	 * List of file upload sessions
	 */
	private static Hashtable<String, IFileUpload> uploadSessions = new Hashtable<String, IFileUpload>();

	/***
	 * Event broadcaster
	 */
	private final FileUploadEventBroadcaster mFileUploadEventBroadcaster = new FileUploadEventBroadcaster();

	/**
	 * Max file upload sessions
	 */
	private int maxUploadSessions;
	
	/**
	 * Max file upload size
	 */
	private int maxUploadSize;	
	
	/**
	 * The logger
	 */
	private static final Logger logger = Logger.getLogger(FileUploadServiceImpl.class.getSimpleName());

	/**
	 * Lock used for synchronization
	 */
	private Object lock = new Object();

	/**
	 * Constructor
	 */
	public FileUploadServiceImpl() {
		if (logger.isActivated()) {
			logger.info("File upload service API is loaded");
		}
		
		// Get configuration
		maxUploadSessions = RcsSettings.getInstance().getMaxFileTransferSessions();
		maxUploadSize = FileSharingSession.getMaxFileSharingSize();
	}

	/**
	 * Close API
	 */
	public void close() {
		// Clear list of sessions
		uploadSessions.clear();
		
		if (logger.isActivated()) {
			logger.info("File upload service API is closed");
		}
	}

	/**
	 * Add a file upload session in the list
	 * 
	 * @param session File upload session
	 */
	protected static void addFileUploadSession(FileUploadImpl session) {
		if (logger.isActivated()) {
			logger.debug("Add a file upload session in the list (size=" + uploadSessions.size() + ")");
		}
		
		uploadSessions.put(session.getUploadId(), session);
	}

	/**
	 * Remove a file upload session from the list
	 * 
	 * @param sessionId Session ID
	 */
	protected static void removeFileUploadSession(String sessionId) {
		if (logger.isActivated()) {
			logger.debug("Remove a file upload session from the list (size=" + uploadSessions.size() + ")");
		}
		
		uploadSessions.remove(sessionId);
	}

    /**
     * Returns the configuration of the file upload service
     * 
     * @return Configuration
     */
    public FileUploadServiceConfiguration getConfiguration() {
    	return new FileUploadServiceConfiguration(
    			maxUploadSize);
    }    	
	
    /**
     * Uploads a file to the RCS content server. The parameter file contains the URI
     * of the file to be uploaded (for a local or a remote file).
     * 
     * @param file Uri of file to upload
	 * @param fileicon File icon option. If true and if it's an image, a file icon is attached.
	 * @return File upload
     * @throws ServerApiException
     */
    public IFileUpload uploadFile(Uri file, boolean fileicon) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Initiate a file upload session (thumbnail option " + fileicon + ")");
		}

		// Test IMS connection
		ServerApiUtils.testCore();

		try {
			// Test number of sessions
			if ((maxUploadSessions != 0) && (uploadSessions.size() >= maxUploadSessions)) {
				if (logger.isActivated()) {
					logger.debug("The max number of file upload sessions is achieved: cancel the initiation");
				}
				throw new ServerApiException("Max file transfer sessions achieved");
			}

			// Create a file content
			FileDescription desc = FileFactory.getFactory().getFileDescription(file);
			MmContent content = ContentManager.createMmContent(file, desc.getSize(), desc.getName());

			// Test max size
	        if (maxUploadSize > 0 && content.getSize() > maxUploadSize) {
	            if (logger.isActivated()) {
	                logger.debug("File exceeds max size: cancel the initiation");
	            }
	            throw new ServerApiException("File exceeds max size");
	        }

			// Initiate an upload session
			final FileUploadSession session = new FileUploadSession(content, fileicon);

			// Add session listener
			FileUploadImpl sessionApi = new FileUploadImpl(session, mFileUploadEventBroadcaster);

			// Start the session
			session.start();
			
			// Add session in the list
			addFileUploadSession(sessionApi);
			return sessionApi;
		} catch(Exception e) {
			// TODO:Handle Security exception in CR026
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}

    /**
     * Can a file be uploaded now
     * 
     * @return Returns true if a file can be uploaded, else returns false
     * @throws RcsServiceException
     */
    public boolean canUploadFile() throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Check if a file can be uploaded");
		}

		// Test IMS connection
		ServerApiUtils.testCore();

		// Test number of ongoing sessions
		if ((maxUploadSessions != 0) && (uploadSessions.size() >= maxUploadSessions)) {
			return false;
		} else {
			return true;
		}
    }
    
    /**
     * Returns the list of file uploads in progress
     * 
     * @return List of file uploads
     * @throws ServerApiException
     */
    public List<IBinder> getFileUploads() throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get file upload sessions");
		}

		try {
			ArrayList<IBinder> result = new ArrayList<IBinder>(uploadSessions.size());
			for (Enumeration<IFileUpload> e = uploadSessions.elements() ; e.hasMoreElements() ;) {
				IFileUpload sessionApi = e.nextElement() ;
				result.add(sessionApi.asBinder());
			}
			return result;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
    }    

    /**
     * Returns a current file upload from its unique ID
     * 
     * @return File upload
     * @throws ServerApiException
     */
    public IFileUpload getFileUpload(String uploadId) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get file upload session " + uploadId);
		}

		return uploadSessions.get(uploadId);
    }    

	/**
	 * Adds a listener on file upload events
	 * 
	 * @param listener Listener
	 */
	public void addEventListener(IFileUploadListener listener) {
		if (logger.isActivated()) {
			logger.info("Add a file upload event listener");
		}
		synchronized (lock) {
			mFileUploadEventBroadcaster.addEventListener(listener);
		}
	}

	/**
	 * Removes a listener on file upload events
	 * 
	 * @param listener Listener
	 */
	public void removeEventListener(IFileUploadListener listener) {
		if (logger.isActivated()) {
			logger.info("Remove a file upload event listener");
		}
		synchronized (lock) {
			mFileUploadEventBroadcaster.removeEventListener(listener);
		}
	}

	/**
	 * Returns service version
	 * 
	 * @return Version
	 * @see RcsService.Build.VERSION_CODES
	 * @throws ServerApiException
	 */
	public int getServiceVersion() throws ServerApiException {
		return RcsService.Build.API_VERSION;
	}
}
