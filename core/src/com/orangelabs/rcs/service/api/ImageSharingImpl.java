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

import javax2.sip.message.Response;

import android.net.Uri;

import com.gsma.services.rcs.RcsCommon.Direction;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.ish.IImageSharing;
import com.gsma.services.rcs.ish.ImageSharing;
import com.gsma.services.rcs.ish.ImageSharing.ReasonCode;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.richcall.ContentSharingError;
import com.orangelabs.rcs.core.ims.service.richcall.RichcallService;
import com.orangelabs.rcs.core.ims.service.richcall.image.ImageSharingPersistedStorageAccessor;
import com.orangelabs.rcs.core.ims.service.richcall.image.ImageTransferSession;
import com.orangelabs.rcs.core.ims.service.richcall.image.ImageTransferSessionListener;
import com.orangelabs.rcs.provider.sharing.ImageSharingStateAndReasonCode;
import com.orangelabs.rcs.provider.sharing.RichCallHistory;
import com.orangelabs.rcs.service.broadcaster.IImageSharingEventBroadcaster;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Image sharing implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class ImageSharingImpl extends IImageSharing.Stub implements ImageTransferSessionListener {

	private final String mSharingId;

	private final IImageSharingEventBroadcaster mBroadcaster;

	private final RichcallService mRichcallService;

	private final ImageSharingPersistedStorageAccessor mPersistentStorage;

	private final ImageSharingServiceImpl mImageSharingService;

	/**
	 * Lock used for synchronization
	 */
	private final Object lock = new Object();

	/**
	 * The logger
	 */
	private final Logger logger = Logger.getLogger(getClass().getName());

	/**
	 * Constructor
	 * 
	 * @param sharingId Unique Id of Image Sharing
	 * @param richcallService RichcallService
	 * @param broadcaster IImageSharingEventBroadcaster
	 * @param persistentStorage ImageSharingPersistedStorageAccessor
	 * @param imageSharingService ImageSharingServiceImpl
	 */
	public ImageSharingImpl(String sharingId, RichcallService richcallService,
			IImageSharingEventBroadcaster broadcaster,
			ImageSharingPersistedStorageAccessor persistentStorage, ImageSharingServiceImpl imageSharingService) {
		mSharingId = sharingId;
		mRichcallService = richcallService;
		mBroadcaster = broadcaster;
		mPersistentStorage = persistentStorage;
		mImageSharingService = imageSharingService;
	}

	private ImageSharingStateAndReasonCode toStateAndReasonCode(ContentSharingError error) {
		switch (error.getErrorCode()) {
			case ContentSharingError.SESSION_INITIATION_FAILED:
				return new ImageSharingStateAndReasonCode(ImageSharing.State.FAILED,
						ReasonCode.FAILED_INITIATION);
			case ContentSharingError.SESSION_INITIATION_CANCELLED:
			case ContentSharingError.SESSION_INITIATION_DECLINED:
				return new ImageSharingStateAndReasonCode(ImageSharing.State.REJECTED,
						ReasonCode.REJECTED_BY_REMOTE);
			case ContentSharingError.MEDIA_SAVING_FAILED:
				return new ImageSharingStateAndReasonCode(ImageSharing.State.FAILED,
						ReasonCode.FAILED_SAVING);
			case ContentSharingError.MEDIA_TRANSFER_FAILED:
			case ContentSharingError.MEDIA_STREAMING_FAILED:
			case ContentSharingError.UNSUPPORTED_MEDIA_TYPE:
				return new ImageSharingStateAndReasonCode(ImageSharing.State.FAILED,
						ReasonCode.FAILED_SHARING);
			case ContentSharingError.NOT_ENOUGH_STORAGE_SPACE:
				return new ImageSharingStateAndReasonCode(ImageSharing.State.REJECTED,
						ReasonCode.REJECTED_LOW_SPACE);
			case ContentSharingError.MEDIA_SIZE_TOO_BIG:
				return new ImageSharingStateAndReasonCode(ImageSharing.State.REJECTED,
						ReasonCode.REJECTED_MAX_SIZE);
			case ContentSharingError.MEDIA_RENDERER_NOT_INITIALIZED:
				return new ImageSharingStateAndReasonCode(ImageSharing.State.ABORTED,
						ReasonCode.ABORTED_BY_SYSTEM);
			default:
				throw new IllegalArgumentException(
						"Unknown reason in ImageSharingImpl.toStateAndReasonCode; error=" + error
								+ "!");
		}
	}

	private int imsServiceSessionErrorToReasonCode(int imsServiceSessionErrorCode) {
		switch (imsServiceSessionErrorCode) {
			case ImsServiceSession.TERMINATION_BY_SYSTEM:
			case ImsServiceSession.TERMINATION_BY_TIMEOUT:
				return ReasonCode.ABORTED_BY_SYSTEM;
			case ImsServiceSession.TERMINATION_BY_USER:
				return ReasonCode.ABORTED_BY_USER;
			default:
				throw new IllegalArgumentException(
						"Unknown reason in ImageSharingImpl.imsServiceSessionErrorToReasonCode; imsServiceSessionErrorCode="
								+ imsServiceSessionErrorCode + "!");
		}
	}

	private void handleSessionRejected(int reasonCode) {
		if (logger.isActivated()) {
			logger.info("Session rejected; reasonCode=" + reasonCode + ".");
		}
		synchronized (lock) {
			mImageSharingService.removeImageSharing(mSharingId);

			mPersistentStorage.setStateAndReasonCode(ImageSharing.State.REJECTED,
					reasonCode);

			mBroadcaster.broadcastStateChanged(getRemoteContact(),
					mSharingId, ImageSharing.State.REJECTED, reasonCode);
		}
	}

	/**
	 * Returns the sharing ID of the image sharing
	 * 
	 * @return Sharing ID
	 */
	public String getSharingId() {
		return mSharingId;
	}
	
	/**
	 * Returns the remote contact identifier
	 * 
	 * @return ContactId
	 */
	public ContactId getRemoteContact() {
		ImageTransferSession session = mRichcallService.getImageTransferSession(mSharingId);
		if (session == null) {
			return mPersistentStorage.getRemoteContact();
		}
		return session.getRemoteContact();
	}
	
	/**
     * Returns the complete filename including the path of the file to be transferred
     *
     * @return Filename
     */
	public String getFileName() {
		ImageTransferSession session = mRichcallService.getImageTransferSession(mSharingId);
		if (session == null) {
			return mPersistentStorage.getFileName();
		}
		return session.getContent().getName();
	}

	/**
	 * Returns the Uri of the file to be transferred
	 *
	 * @return Filename
	 */
	public Uri getFile() {
		ImageTransferSession session = mRichcallService.getImageTransferSession(mSharingId);
		if (session == null) {
			return mPersistentStorage.getFile();
		}
		return session.getContent().getUri();
	}

	/**
     * Returns the size of the file to be transferred
     *
     * @return Size in bytes
     */
	public long getFileSize() {
		ImageTransferSession session = mRichcallService.getImageTransferSession(mSharingId);
		if (session == null) {
			return mPersistentStorage.getFileSize();
		}
		return session.getContent().getSize();
	}	

    /**
     * Returns the MIME type of the file to be transferred
     * 
     * @return Type
     */
	public String getMimeType() {
		ImageTransferSession session = mRichcallService.getImageTransferSession(mSharingId);
		if (session == null) {
			return mPersistentStorage.getMimeType();
		}
		return session.getContent().getEncoding();
	}

	/**
	 * Returns the state of the image sharing
	 * 
	 * @return State
	 */
	public int getState() {
		ImageTransferSession session = mRichcallService.getImageTransferSession(mSharingId);
		if (session == null) {
			return mPersistentStorage.getState();
		}
		SipDialogPath dialogPath = session.getDialogPath();
		if (dialogPath != null && dialogPath.isSessionEstablished()) {
			return ImageSharing.State.STARTED;
		} else if (session.isInitiatedByRemote()) {
			if (session.isSessionAccepted()) {
				return ImageSharing.State.ACCEPTING;
			}
			return ImageSharing.State.INVITED;
		}
		return ImageSharing.State.INITIATED;
	}

	/**
	 * Returns the reason code of the state of the image sharing
	 *
	 * @return ReasonCode
	 */
	public int getReasonCode() {
		ImageTransferSession session = mRichcallService.getImageTransferSession(mSharingId);
		if (session == null) {
			return mPersistentStorage.getReasonCode();
		}
		return ReasonCode.UNSPECIFIED;
	}
	
	/**
	 * Returns the direction of the sharing (incoming or outgoing)
	 * 
	 * @return Direction
	 * @see Direction
	 */
	public int getDirection() {
		ImageTransferSession session = mRichcallService.getImageTransferSession(mSharingId);
		if (session == null) {
			return mPersistentStorage.getDirection();
		}
		if (session.isInitiatedByRemote()) {
			return Direction.INCOMING;
		}
		return Direction.OUTGOING;
	}		
		
	/**
	 * Accepts image sharing invitation
	 */
	public void acceptInvitation() {
		if (logger.isActivated()) {
			logger.info("Accept session invitation");
		}
		final ImageTransferSession session = mRichcallService.getImageTransferSession(mSharingId);
		if (session == null) {
			/*
			 * TODO: Throw correct exception as part of CR037 implementation
			 */
			throw new IllegalStateException("Session with sharing ID '" + mSharingId
					+ "' not available.");
		}
		// Accept invitation
        new Thread() {
    		public void run() {
    			session.acceptSession();
    		}
    	}.start();
	}
	
	/**
	 * Rejects image sharing invitation
	 */
	public void rejectInvitation() {
		if (logger.isActivated()) {
			logger.info("Reject session invitation");
		}
		final ImageTransferSession session = mRichcallService.getImageTransferSession(mSharingId);
		if (session == null) {
			/*
			 * TODO: Throw correct exception as part of CR037 implementation
			 */
			throw new IllegalStateException("Session with sharing ID '" + mSharingId
					+ "' not available.");
		}
		// Reject invitation
        new Thread() {
    		public void run() {
    			session.rejectSession(Response.DECLINE);
    		}
    	}.start();
    }

	/**
	 * Aborts the sharing
	 */
	public void abortSharing() {
		if (logger.isActivated()) {
			logger.info("Cancel session");
		}
		final ImageTransferSession session = mRichcallService.getImageTransferSession(mSharingId);
		if (session == null) {
			/*
			 * TODO: Throw correct exception as part of CR037 implementation
			 */
			throw new IllegalStateException("Session with sharing ID '" + mSharingId
					+ "' not available.");
		}
		if (session.isImageTransfered()) {
			// Automatically closed after transfer
			return;
		}
		// Abort the session
        new Thread() {
    		public void run() {
    			session.abortSession(ImsServiceSession.TERMINATION_BY_USER);
    		}
    	}.start();		
	}

    /*------------------------------- SESSION EVENTS ----------------------------------*/

	/**
	 * Session is started
	 */
    public void handleSessionStarted() {
		if (logger.isActivated()) {
			logger.info("Session started");
		}
		synchronized (lock) {
			mPersistentStorage.setStateAndReasonCode(ImageSharing.State.STARTED,
					ReasonCode.UNSPECIFIED);

			mBroadcaster.broadcastStateChanged(getRemoteContact(),
					getSharingId(), ImageSharing.State.STARTED, ReasonCode.UNSPECIFIED);
		}
    }
    
	/**
	 * * Session has been aborted
	 *
	 * @param reason Termination reason
	 */
	public void handleSessionAborted(int reason) {
		if (logger.isActivated()) {
			logger.info("Session aborted (reason " + reason + ")");
		}
		int reasonCode = imsServiceSessionErrorToReasonCode(reason);
		synchronized (lock) {
			mImageSharingService.removeImageSharing(mSharingId);

			mPersistentStorage.setStateAndReasonCode(ImageSharing.State.ABORTED,
					reasonCode);

			mBroadcaster.broadcastStateChanged(getRemoteContact(),
					mSharingId, ImageSharing.State.ABORTED, reasonCode);
		}
	}

	/**
	 * Session has been terminated by remote
	 */
	public void handleSessionTerminatedByRemote() {
		if (logger.isActivated()) {
			logger.info("Session terminated by remote");
		}

		synchronized (lock) {
			mImageSharingService.removeImageSharing(mSharingId);
			ImageTransferSession session = mRichcallService
					.getImageTransferSession(mSharingId);
			if (session != null && !session.isImageTransfered()) {
				mPersistentStorage.setStateAndReasonCode(ImageSharing.State.ABORTED,
						ReasonCode.ABORTED_BY_REMOTE);
				mBroadcaster.broadcastStateChanged(getRemoteContact(), mSharingId,
						ImageSharing.State.ABORTED, ReasonCode.ABORTED_BY_REMOTE);
			}
		}
	}
    
	/**
	 * Content sharing error
	 * 
	 * @param error Error
	 */
	public void handleSharingError(ContentSharingError error) {
		if (logger.isActivated()) {
			logger.info("Sharing error " + error.getErrorCode());
		}
		ImageSharingStateAndReasonCode stateAndReasonCode = toStateAndReasonCode(error);
		int state = stateAndReasonCode.getState();
		int reasonCode = stateAndReasonCode.getReasonCode();
		synchronized (lock) {
			mImageSharingService.removeImageSharing(mSharingId);

			mPersistentStorage.setStateAndReasonCode(state, reasonCode);

			mBroadcaster.broadcastStateChanged(getRemoteContact(),
					mSharingId, state, reasonCode);
		}
	}
    
    /**
     * Content sharing progress
     *
     * @param currentSize Data size transferred
     * @param totalSize Total size to be transferred
     */
    public void handleSharingProgress(long currentSize, long totalSize) {
    	synchronized(lock) {
			mPersistentStorage.setProgress(currentSize);

			mBroadcaster.broadcastProgressUpdate(getRemoteContact(),
					getSharingId(), currentSize, totalSize);
	     }
    }
    
    /**
     * Content has been transferred
     *
     * @param filename Filename associated to the received content
     */
    public void handleContentTransfered(Uri file) {
		if (logger.isActivated()) {
			logger.info("Image transferred");
		}
		synchronized (lock) {
			mImageSharingService.removeImageSharing(mSharingId);

			mPersistentStorage.setStateAndReasonCode(ImageSharing.State.TRANSFERRED,
					ReasonCode.UNSPECIFIED);

			mBroadcaster.broadcastStateChanged(getRemoteContact(),
					mSharingId, ImageSharing.State.TRANSFERRED, ReasonCode.UNSPECIFIED);
	    }
    }

	@Override
	public void handleSessionAccepted() {
		if (logger.isActivated()) {
			logger.info("Accepting sharing");
		}
		synchronized (lock) {
			mPersistentStorage.setStateAndReasonCode(ImageSharing.State.ACCEPTING,
					ReasonCode.UNSPECIFIED);
			mBroadcaster.broadcastStateChanged(getRemoteContact(),
					mSharingId, ImageSharing.State.ACCEPTING, ReasonCode.UNSPECIFIED);
		}
	}

	@Override
	public void handleSessionRejectedByUser() {
		handleSessionRejected(ReasonCode.REJECTED_BY_USER);
	}

	@Override
	public void handleSessionRejectedByTimeout() {
		handleSessionRejected(ReasonCode.REJECTED_TIME_OUT);
	}

	@Override
	public void handleSessionRejectedByRemote() {
		handleSessionRejected(ReasonCode.REJECTED_BY_REMOTE);
	}

	@Override
	public void handleSessionInvited() {
		if (logger.isActivated()) {
			logger.info("Invited to image sharing session");
		}
		ImageTransferSession session = mRichcallService
				.getImageTransferSession(mSharingId);
		synchronized (lock) {
			mPersistentStorage.addImageSharing(getRemoteContact(), Direction.INCOMING,
					session.getContent(), ImageSharing.State.INVITED, ReasonCode.UNSPECIFIED);
		}

		mBroadcaster.broadcastInvitation(mSharingId);
	}

	@Override
	public void handle180Ringing() {
		synchronized (lock) {
			mPersistentStorage.setStateAndReasonCode(ImageSharing.State.RINGING,
					ReasonCode.UNSPECIFIED);
			mBroadcaster.broadcastStateChanged(getRemoteContact(),
					mSharingId, ImageSharing.State.RINGING, ReasonCode.UNSPECIFIED);
		}
	}
}
