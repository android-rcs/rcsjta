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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.net.Uri;
import android.os.IBinder;

import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsCommon.Direction;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.ish.IImageSharing;
import com.gsma.services.rcs.ish.IImageSharingListener;
import com.gsma.services.rcs.ish.IImageSharingService;
import com.gsma.services.rcs.ish.ImageSharing;
import com.gsma.services.rcs.ish.ImageSharing.ReasonCode;
import com.gsma.services.rcs.ish.ImageSharingServiceConfiguration;
import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.content.ContentManager;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.service.SessionIdGenerator;
import com.orangelabs.rcs.core.ims.service.richcall.RichcallService;
import com.orangelabs.rcs.core.ims.service.richcall.image.ImageSharingPersistedStorageAccessor;
import com.orangelabs.rcs.core.ims.service.richcall.image.ImageTransferSession;
import com.orangelabs.rcs.platform.file.FileDescription;
import com.orangelabs.rcs.platform.file.FileFactory;
import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provider.sharing.RichCallHistory;
import com.orangelabs.rcs.service.broadcaster.ImageSharingEventBroadcaster;
import com.orangelabs.rcs.service.broadcaster.RcsServiceRegistrationEventBroadcaster;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Image sharing service implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class ImageSharingServiceImpl extends IImageSharingService.Stub {

	private final ImageSharingEventBroadcaster mBroadcaster = new ImageSharingEventBroadcaster();

	private final RcsServiceRegistrationEventBroadcaster mRcsServiceRegistrationEventBroadcaster = new RcsServiceRegistrationEventBroadcaster();

	private final RichcallService mRichcallService;

	private final RichCallHistory mRichCallLog;

	private final RcsSettings mRcsSettings;

	private final ContactsManager mContactsManager;

	private final Map<String, IImageSharing> mImageSharingCache = new HashMap<String, IImageSharing>();

	/**
	 * The logger
	 */
	private static final Logger logger = Logger.getLogger(ImageSharingServiceImpl.class.getSimpleName());

	/**
	 * Lock used for synchronization
	 */
	private Object lock = new Object();

	/**
	 * Constructor
	 * 
	 * @param richcallService RichcallService
	 * @param richCallLog RichCallHistory
	 * @param rcsSettings RcsSettings
	 * @param contactsManager ContactsManager
	 */
	public ImageSharingServiceImpl(RichcallService richcallService, RichCallHistory richCallLog,
			RcsSettings rcsSettings, ContactsManager contactsManager) {
		if (logger.isActivated()) {
			logger.info("Image sharing service API is loaded");
		}
		mRichcallService = richcallService;
		mRichCallLog = richCallLog;
		mRcsSettings = rcsSettings;
		mContactsManager = contactsManager;
	}

	/**
	 * Close API
	 */
	public void close() {
		// Clear list of sessions
		mImageSharingCache.clear();
		
		if (logger.isActivated()) {
			logger.info("Image sharing service API is closed");
		}
	}

	/**
	 * Add an image sharing in the list
	 * 
	 * @param imageSharing Image sharing
	 */
	private void addImageSharing(ImageSharingImpl imageSharing) {
		if (logger.isActivated()) {
			logger.debug("Add an image sharing in the list (size=" + mImageSharingCache.size() + ")");
		}
		
		mImageSharingCache.put(imageSharing.getSharingId(), imageSharing);
	}

	/**
	 * Remove an image sharing from the list
	 * 
	 * @param sharingId Sharing ID
	 */
	/* package private */ void removeImageSharing(String sharingId) {
		if (logger.isActivated()) {
			logger.debug("Remove an image sharing from the list (size=" + mImageSharingCache.size() + ")");
		}
		
		mImageSharingCache.remove(sharingId);
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
	public void addEventListener(IRcsServiceRegistrationListener listener) {
		if (logger.isActivated()) {
			logger.info("Add a service listener");
		}
		synchronized (lock) {
			mRcsServiceRegistrationEventBroadcaster.addEventListener(listener);
		}
	}

	/**
	 * Unregisters a listener on service registration events
	 *
	 * @param listener Service registration listener
	 */
	public void removeEventListener(IRcsServiceRegistrationListener listener) {
		if (logger.isActivated()) {
			logger.info("Remove a service listener");
		}
		synchronized (lock) {
			mRcsServiceRegistrationEventBroadcaster.removeEventListener(listener);
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
				mRcsServiceRegistrationEventBroadcaster.broadcastServiceRegistered();
			} else {
				mRcsServiceRegistrationEventBroadcaster.broadcastServiceUnRegistered();
			}
		}
	}

    /**
     * Receive a new image sharing invitation
     * 
     * @param session Image sharing session
     */
    public void receiveImageSharingInvitation(ImageTransferSession session) {
		if (logger.isActivated()) {
			logger.info("Receive image sharing invitation from " + session.getRemoteContact() + " displayName="
					+ session.getRemoteDisplayName());
		}
		ContactId contact = session.getRemoteContact();

		// Update displayName of remote contact
		 mContactsManager.setContactDisplayName(contact, session.getRemoteDisplayName());

		String sharingId = session.getSessionID();
		ImageSharingPersistedStorageAccessor storageAccessor = new ImageSharingPersistedStorageAccessor(
				sharingId, mRichCallLog);
		ImageSharingImpl imageSharing = new ImageSharingImpl(sharingId, mRichcallService,
				mBroadcaster, storageAccessor, this);
		addImageSharing(imageSharing);
		session.addListener(imageSharing);
    }

    /**
     * Returns the configuration of image sharing service
     * 
     * @return Configuration
     */
	public ImageSharingServiceConfiguration getConfiguration() {
		return new ImageSharingServiceConfiguration(mRcsSettings.getMaxImageSharingSize());
	}    
    
    /**
     * Shares an image with a contact. The parameter file contains the URI
     * of the image to be shared(for a local or a remote image). An exception if thrown if there is
     * no ongoing CS call. The parameter contact supports the following formats: MSISDN
     * in national or international format, SIP address, SIP-URI or Tel-URI. If the format
     * of the contact is not supported an exception is thrown.
     * 
     * @param contact Contact ID
     * @param file Uri of file to share
     * @return Image sharing
     * @throws ServerApiException
     */
    public IImageSharing shareImage(ContactId contact, Uri file) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Initiate an image sharing session with " + contact);
		}

		// Test IMS connection
		ServerApiUtils.testIms();

		try {
			FileDescription desc = FileFactory.getFactory().getFileDescription(file);
			MmContent content = ContentManager.createMmContent(file, desc.getSize(), desc.getName());

			final ImageTransferSession session = mRichcallService.initiateImageSharingSession(contact, content, null);

			String sharingId = session.getSessionID();
			mRichCallLog.addImageSharing(session.getSessionID(), contact,
					Direction.OUTGOING, session.getContent(),
					ImageSharing.State.INITIATED, ReasonCode.UNSPECIFIED);
			mBroadcaster.broadcastStateChanged(contact, sharingId,
					ImageSharing.State.INITIATED, ReasonCode.UNSPECIFIED);

			ImageSharingPersistedStorageAccessor storageAccessor = new ImageSharingPersistedStorageAccessor(
					sharingId, mRichCallLog);
			ImageSharingImpl imageSharing = new ImageSharingImpl(sharingId, mRichcallService,
					mBroadcaster, storageAccessor, this);

			addImageSharing(imageSharing);
			session.addListener(imageSharing);

	        new Thread() {
	    		public void run() {
	    			session.startSession();
	    		}
	    	}.start();	
			return imageSharing;

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
			List<IBinder> imageSharings = new ArrayList<IBinder>(mImageSharingCache.size());
			for (IImageSharing imageSharing : mImageSharingCache.values()) {
				imageSharings.add(imageSharing.asBinder());
			}
			return imageSharings;

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
		IImageSharing imageSharing = mImageSharingCache.get(sharingId);
		if (imageSharing != null) {
			return imageSharing;
		}
		ImageSharingPersistedStorageAccessor storageAccessor = new ImageSharingPersistedStorageAccessor(
				sharingId, mRichCallLog);
		return new ImageSharingImpl(sharingId, mRichcallService, mBroadcaster, storageAccessor,
				this);
	}

	/**
	 * Add and broadcast image sharing invitation rejection
	 * invitation.
	 *
	 * @param contact Contact
	 * @param content Image content
	 * @param reasonCode Reason code
	 */
	public void addAndBroadcastImageSharingInvitationRejected(ContactId contact,
			MmContent content, int reasonCode) {
		String sessionId = SessionIdGenerator.getNewId();
		mRichCallLog.addImageSharing(sessionId, contact,
				Direction.INCOMING, content, ImageSharing.State.REJECTED, reasonCode);
		mBroadcaster.broadcastInvitation(sessionId);
	}

    /**
	 * Adds a listener on image sharing events
	 * 
	 * @param listener Listener
	 */
	public void addEventListener2(IImageSharingListener listener) {
		if (logger.isActivated()) {
			logger.info("Add an Image sharing event listener");
		}
		synchronized (lock) {
			mBroadcaster.addEventListener(listener);
		}
	}

	/**
	 * Removes a listener on image sharing events
	 * 
	 * @param listener Listener
	 */
	public void removeEventListener2(IImageSharingListener listener) {
		if (logger.isActivated()) {
			logger.info("Remove an Image sharing event listener");
		}
		synchronized (lock) {
			mBroadcaster.removeEventListener(listener);
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
