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
package com.orangelabs.rcs.service.api;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteCallbackList;

import com.gsma.services.rcs.IJoynServiceRegistrationListener;
import com.gsma.services.rcs.JoynService;
import com.gsma.services.rcs.ish.IImageSharing;
import com.gsma.services.rcs.ish.IImageSharingListener;
import com.gsma.services.rcs.ish.IImageSharingService;
import com.gsma.services.rcs.ish.INewImageSharingListener;
import com.gsma.services.rcs.ish.ImageSharing;
import com.gsma.services.rcs.ish.ImageSharingIntent;
import com.gsma.services.rcs.ish.ImageSharingServiceConfiguration;
import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.content.ContentManager;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.service.richcall.image.ImageTransferSession;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.platform.file.FileDescription;
import com.orangelabs.rcs.platform.file.FileFactory;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provider.sharing.RichCallHistory;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Image sharing service implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class ImageSharingServiceImpl extends IImageSharingService.Stub {
	/**
	 * List of service event listeners
	 */
	private RemoteCallbackList<IJoynServiceRegistrationListener> serviceListeners = new RemoteCallbackList<IJoynServiceRegistrationListener>();

	/**
	 * List of image sharing sessions
	 */
	private static Hashtable<String, IImageSharing> ishSessions = new Hashtable<String, IImageSharing>();  

	/**
	 * List of image sharing invitation listeners
	 */
	private RemoteCallbackList<INewImageSharingListener> listeners = new RemoteCallbackList<INewImageSharingListener>();

	/**
	 * The logger
	 */
	private static final Logger logger = Logger.getLogger(ImageSharingServiceImpl.class.getName());

	/**
	 * Lock used for synchronization
	 */
	private Object lock = new Object();

	/**
	 * Constructor
	 */
	public ImageSharingServiceImpl() {
		if (logger.isActivated()) {
			logger.info("Image sharing service API is loaded");
		}
	}

	/**
	 * Close API
	 */
	public void close() {
		// Clear list of sessions
		ishSessions.clear();
		
		if (logger.isActivated()) {
			logger.info("Image sharing service API is closed");
		}
	}

	/**
	 * Add an image sharing session in the list
	 * 
	 * @param session Image sharing session
	 */
	protected static void addImageSharingSession(ImageSharingImpl session) {
		if (logger.isActivated()) {
			logger.debug("Add an image sharing session in the list (size=" + ishSessions.size() + ")");
		}
		
		ishSessions.put(session.getSharingId(), session);
	}

	/**
	 * Remove an image sharing session from the list
	 * 
	 * @param sessionId Session ID
	 */
	protected static void removeImageSharingSession(String sessionId) {
		if (logger.isActivated()) {
			logger.debug("Remove an image sharing session from the list (size=" + ishSessions.size() + ")");
		}
		
		ishSessions.remove(sessionId);
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
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Add a service listener");
			}

			serviceListeners.register(listener);
		}
	}
	
	/**
	 * Unregisters a listener on service registration events
	 * 
	 * @param listener Service registration listener
	 */
	public void removeServiceRegistrationListener(IJoynServiceRegistrationListener listener) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Remove a service listener");
			}
			
			serviceListeners.unregister(listener);
    	}	
	}    

    /**
     * Receive registration event
     * 
     * @param state Registration state
     */
    public void notifyRegistrationEvent(boolean state) {
    	// Notify listeners
    	synchronized(lock) {
			final int N = serviceListeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	if (state) {
	            		serviceListeners.getBroadcastItem(i).onServiceRegistered();
	            	} else {
	            		serviceListeners.getBroadcastItem(i).onServiceUnregistered();
	            	}
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        serviceListeners.finishBroadcast();
	    }    	    	
    }	
	
    /**
     * Receive a new image sharing invitation
     * 
     * @param session Image sharing session
     */
    public void receiveImageSharingInvitation(ImageTransferSession session) {
		if (logger.isActivated()) {
			logger.info("Receive image sharing invitation from " + session.getRemoteContact());
		}

        // Extract number from contact
		String number = PhoneUtils.extractNumberFromUri(session.getRemoteContact());

		// Update rich call history
		RichCallHistory.getInstance().addImageSharing(number, session.getSessionID(),
				ImageSharing.Direction.INCOMING,
				session.getContent(),
				ImageSharing.State.INVITED);

		// Add session in the list
		ImageSharingImpl sessionApi = new ImageSharingImpl(session);
		ImageSharingServiceImpl.addImageSharingSession(sessionApi);
    	
		// Broadcast intent related to the received invitation
    	Intent intent = new Intent(ImageSharingIntent.ACTION_NEW_INVITATION);
    	intent.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
    	intent.putExtra(ImageSharingIntent.EXTRA_CONTACT, number);
    	intent.putExtra(ImageSharingIntent.EXTRA_DISPLAY_NAME, session.getRemoteDisplayName());
    	intent.putExtra(ImageSharingIntent.EXTRA_SHARING_ID, session.getSessionID());
    	intent.putExtra(ImageSharingIntent.EXTRA_FILENAME, session.getContent().getName());
    	intent.putExtra(ImageSharingIntent.EXTRA_FILESIZE, session.getContent().getSize());
    	intent.putExtra(ImageSharingIntent.EXTRA_FILETYPE, session.getContent().getEncoding());
    	AndroidFactory.getApplicationContext().sendBroadcast(intent);
    	
    	// Notify image sharing invitation listeners
    	synchronized(lock) {
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).onNewImageSharing(session.getSessionID());
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	    }
    }

    /**
     * Returns the configuration of image sharing service
     * 
     * @return Configuration
     */
    public ImageSharingServiceConfiguration getConfiguration() {
    	return new ImageSharingServiceConfiguration(
    			RcsSettings.getInstance().getMaxImageSharingSize());
	}    
    
    /**
     * Shares an image with a contact. The parameter file contains the URI
     * of the image to be shared(for a local or a remote image). An exception if thrown if there is
     * no ongoing CS call. The parameter contact supports the following formats: MSISDN
     * in national or international format, SIP address, SIP-URI or Tel-URI. If the format
     * of the contact is not supported an exception is thrown.
     * 
     * @param contact Contact
     * @param file Uri of file to share
     * @param listener Image sharing event listener
     * @return Image sharing
     * @throws ServerApiException
     */
    public IImageSharing shareImage(String contact, Uri file, IImageSharingListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Initiate an image sharing session with " + contact);
		}

		// Test IMS connection
		ServerApiUtils.testIms();

		try {
			// Create an image content
			FileDescription desc = FileFactory.getFactory().getFileDescription(file);
			MmContent content = ContentManager.createMmContent(file, desc.getSize(), desc.getName());

			// Initiate a sharing session
			final ImageTransferSession session = Core.getInstance().getRichcallService().initiateImageSharingSession(contact, content, null);

			// Update rich call history
			RichCallHistory.getInstance().addImageSharing(contact, session.getSessionID(),
					ImageSharing.Direction.OUTGOING,
	    			session.getContent(),
	    			ImageSharing.State.INITIATED);

			// Add session listener
			ImageSharingImpl sessionApi = new ImageSharingImpl(session);
			sessionApi.addEventListener(listener);

			// Start the session
	        new Thread() {
	    		public void run() {
	    			session.startSession();
	    		}
	    	}.start();	
			
			// Add session in the list
			addImageSharingSession(sessionApi);
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
     * Returns the list of image sharings in progress
     * 
     * @return List of image sharings
     * @throws ServerApiException
     */
    public List<IBinder> getImageSharings() throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get image sharing sessions");
		}

		try {
			ArrayList<IBinder> result = new ArrayList<IBinder>(ishSessions.size());
			for (Enumeration<IImageSharing> e = ishSessions.elements() ; e.hasMoreElements() ;) {
				IImageSharing sessionApi = e.nextElement() ;
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
     * Returns a current image sharing from its unique ID
     * 
     * @return Image sharing
     * @throws ServerApiException
     */
    public IImageSharing getImageSharing(String sharingId) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get image sharing session " + sharingId);
		}

		return ishSessions.get(sharingId);
    }    
    
    /**
	 * Registers an image sharing invitation listener
	 * 
	 * @param listener New image sharing listener
	 * @throws ServerApiException
	 */
	public void addNewImageSharingListener(INewImageSharingListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Add an image sharing invitation listener");
		}
		
		listeners.register(listener);
	}

	/**
	 * Unregisters an image sharing invitation listener
	 * 
	 * @param listener New image sharing listener
	 * @throws ServerApiException
	 */
	public void removeNewImageSharingListener(INewImageSharingListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Remove an image sharing invitation listener");
		}
		
		listeners.unregister(listener);
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
