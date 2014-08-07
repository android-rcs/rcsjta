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

import com.gsma.services.rcs.IJoynServiceRegistrationListener;
import com.gsma.services.rcs.JoynService;
import com.gsma.services.rcs.ext.upload.IFileUpload;
import com.gsma.services.rcs.ext.upload.IFileUploadListener;
import com.gsma.services.rcs.ext.upload.IFileUploadService;
import com.orangelabs.rcs.core.content.ContentManager;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.service.upload.FileUploadSession;
import com.orangelabs.rcs.platform.file.FileDescription;
import com.orangelabs.rcs.platform.file.FileFactory;
import com.orangelabs.rcs.service.broadcaster.FileUploadEventBroadcaster;
import com.orangelabs.rcs.service.broadcaster.JoynServiceRegistrationEventBroadcaster;
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

	private final FileUploadEventBroadcaster mFileUploadEventBroadcaster = new FileUploadEventBroadcaster();

	private final JoynServiceRegistrationEventBroadcaster mJoynServiceRegistrationEventBroadcaster = new JoynServiceRegistrationEventBroadcaster();

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
     * Returns true if the service is registered to the platform, else returns false
     * 
	 * @return Returns true if registered else returns false
     */
    public boolean isServiceRegistered() {
    	return ServerApiUtils.isImsConnected();
    }

	/**
	 * Registers a listener on service registration events
	 *
	 * @param listener Service registration listener
	 */
	public void addServiceRegistrationListener(IJoynServiceRegistrationListener listener) {
		if (logger.isActivated()) {
			logger.info("Add a service listener");
		}
		synchronized (lock) {
			mJoynServiceRegistrationEventBroadcaster.addServiceRegistrationListener(listener);
		}
	}

	/**
	 * Unregisters a listener on service registration events
	 *
	 * @param listener Service registration listener
	 */
	public void removeServiceRegistrationListener(IJoynServiceRegistrationListener listener) {
		if (logger.isActivated()) {
			logger.info("Remove a service listener");
		}
		synchronized (lock) {
			mJoynServiceRegistrationEventBroadcaster.removeServiceRegistrationListener(listener);
		}
	}

	/**
	 * Receive registration event
	 *
	 * @param state Registration state
	 */
	public void notifyRegistrationEvent(boolean state) {
		// Notify listeners
		synchronized (lock) {
			if (state) {
				mJoynServiceRegistrationEventBroadcaster.broadcastServiceRegistered();
			} else {
				mJoynServiceRegistrationEventBroadcaster.broadcastServiceUnRegistered();
			}
		}
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
			logger.info("Initiate a file upload session");
		}

		// Test IMS connection
		ServerApiUtils.testIms();

		try {
	        // Test max size
			// TODO

	        // Test max upload in progress
			// TODO

			// Create a file content
			FileDescription desc = FileFactory.getFactory().getFileDescription(file);
			MmContent content = ContentManager.createMmContent(file, desc.getSize(), desc.getName());

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
	 * Adds an event listener on file upload events
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
	 * Removes an event listener on file upload events
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
	 * @see JoynService.Build.VERSION_CODES
	 * @throws ServerApiException
	 */
	public int getServiceVersion() throws ServerApiException {
		return JoynService.Build.API_VERSION;
	}
}
