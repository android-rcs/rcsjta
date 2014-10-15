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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;

import com.gsma.services.rcs.GroupDeliveryInfoLog;
import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.RcsCommon.Direction;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.ft.FileTransfer;
import com.gsma.services.rcs.ft.FileTransfer.State;
import com.gsma.services.rcs.ft.FileTransfer.ReasonCode;
import com.gsma.services.rcs.ft.FileTransferServiceConfiguration;
import com.gsma.services.rcs.ft.IFileTransfer;
import com.gsma.services.rcs.ft.IFileTransferService;
import com.gsma.services.rcs.ft.IGroupFileTransferListener;
import com.gsma.services.rcs.ft.IOneToOneFileTransferListener;
import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.core.content.ContentManager;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.service.im.InstantMessagingService;
import com.orangelabs.rcs.core.ims.service.im.chat.GroupChatPersistedStorageAccessor;
import com.orangelabs.rcs.core.ims.service.im.chat.GroupChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileTransferPersistedStorageAccessor;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.orangelabs.rcs.platform.file.FileDescription;
import com.orangelabs.rcs.platform.file.FileFactory;
import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.provider.messaging.MessagingLog;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provider.settings.RcsSettingsData.ImageResizeOption;
import com.orangelabs.rcs.service.broadcaster.GroupChatEventBroadcaster;
import com.orangelabs.rcs.service.broadcaster.GroupFileTransferBroadcaster;
import com.orangelabs.rcs.service.broadcaster.OneToOneFileTransferBroadcaster;
import com.orangelabs.rcs.service.broadcaster.RcsServiceRegistrationEventBroadcaster;
import com.orangelabs.rcs.utils.IdGenerator;
import com.orangelabs.rcs.utils.MimeManager;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * File transfer service implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileTransferServiceImpl extends IFileTransferService.Stub {

	private final OneToOneFileTransferBroadcaster mOneToOneFileTransferBroadcaster = new OneToOneFileTransferBroadcaster();

	private final GroupFileTransferBroadcaster mGroupFileTransferBroadcaster = new GroupFileTransferBroadcaster();

	private final RcsServiceRegistrationEventBroadcaster mRcsServiceRegistrationEventBroadcaster = new RcsServiceRegistrationEventBroadcaster();

	private final InstantMessagingService mImService;

	private final MessagingLog mMessagingLog;

	private final RcsSettings mRcsSettings;

	private final ContactsManager mContactsManager;

	private final Core mCore;

	private final Map<String, IFileTransfer> mFileTransferCache = new HashMap<String, IFileTransfer>();

	/**
	 * The logger
	 */
	private static final Logger logger = Logger.getLogger(FileTransferServiceImpl.class.getSimpleName());

	/**
	 * Lock used for synchronization
	 */
	private final Object lock = new Object();

	/**
	 * Constructor
	 * 
	 * @param imService InstantMessagingService
	 * @param messagingLog MessagingLog
	 * @param rcsSettings RcsSettings
	 * @param contactsManager ContactsManager
	 * @param core Core
	 */
	public FileTransferServiceImpl(InstantMessagingService imService, MessagingLog messagingLog,
			RcsSettings rcsSettings, ContactsManager contactsManager, Core core) {
		if (logger.isActivated()) {
			logger.info("File transfer service API is loaded");
		}
		mImService = imService;
		mMessagingLog = messagingLog;
		mRcsSettings = rcsSettings;
		mContactsManager = contactsManager;
		mCore = core;
	}

	private int imdnToFileTransferFailedReasonCode(ImdnDocument imdn) {
		String notificationType = imdn.getNotificationType();
		if (ImdnDocument.DELIVERY_NOTIFICATION.equals(notificationType)) {
			return ReasonCode.FAILED_DELIVERY;

		} else if (ImdnDocument.DISPLAY_NOTIFICATION.equals(notificationType)) {
			return ReasonCode.FAILED_DISPLAY;
		}

		throw new IllegalArgumentException(new StringBuilder(
				"Received invalid imdn notification type:'").append(notificationType).append("'")
				.toString());
	}

	/**
	 * Close API
	 */
	public void close() {
		// Clear list of sessions
		mFileTransferCache.clear();
		
		if (logger.isActivated()) {
			logger.info("File transfer service API is closed");
		}
	}

	/**
	 * Add a file transfer in the list
	 * 
	 * @param fileTransfer File transfer
	 */
	public void addFileTransfer(IFileTransfer fileTransfer) {
		if (logger.isActivated()) {
			logger.debug("Add a file transfer in the list (size=" + mFileTransferCache.size() + ")");
		}

		try {
			mFileTransferCache.put(fileTransfer.getTransferId(), fileTransfer);
		} catch (RemoteException e) {
			if (logger.isActivated()) {
				logger.info("Unable to add file transfer to the list! " +e);
			}
		}
	}

	/**
	 * Remove a file transfer from the list
	 * 
	 * @param fileTransferId File transfer ID
	 */
	/* package private */ void removeFileTransfer(String fileTransferId) {
		if (logger.isActivated()) {
			logger.debug("Remove a file transfer from the list (size=" + mFileTransferCache.size() + ")");
		}
		
		mFileTransferCache.remove(fileTransferId);
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
	 * Receive a new file transfer invitation
	 * 
	 * @param session File transfer session
	 * @param isGroup is group file transfer
	 * @param contact Contact ID
	 * @param displayName the display name of the remote contact
	 */
	public void receiveFileTransferInvitation(FileSharingSession session, boolean isGroup, ContactId contact, String displayName) {
		if (logger.isActivated()) {
			logger.info("Receive FT invitation from " + contact + " file=" + session.getContent().getName()
					+ " size=" + session.getContent().getSize() + " displayName=" + displayName);
		}


		// Update displayName of remote contact
		mContactsManager.setContactDisplayName(contact,  displayName);

		// Add session in the list
		String fileTransferId = session.getFileTransferId();
		FileTransferPersistedStorageAccessor storageAccessor = new FileTransferPersistedStorageAccessor(
				fileTransferId, mMessagingLog);
		if (isGroup) {
			GroupFileTransferImpl groupFileTransfer = new GroupFileTransferImpl(fileTransferId,
					session.getContributionID(), mGroupFileTransferBroadcaster, mImService,
					storageAccessor, this);
			session.addListener(groupFileTransfer);
			addFileTransfer(groupFileTransfer);
		} else {
			OneToOneFileTransferImpl oneToOneFileTransfer = new OneToOneFileTransferImpl(
					fileTransferId, mOneToOneFileTransferBroadcaster, mImService, storageAccessor,
					this);
			session.addListener(oneToOneFileTransfer);
			addFileTransfer(oneToOneFileTransfer);
		}
    }

    /**
     * Returns the configuration of the file transfer service
     * 
     * @return Configuration
     */
    public FileTransferServiceConfiguration getConfiguration() {
    	return new FileTransferServiceConfiguration(
    			mRcsSettings.getWarningMaxFileTransferSize(),
    			mRcsSettings.getMaxFileTransferSize(),
    			mRcsSettings.isFtAutoAcceptedModeChangeable(),
    			mRcsSettings.isFileTransferAutoAccepted(),
    			mRcsSettings.isFileTransferAutoAcceptedInRoaming(),
    			mRcsSettings.getMaxFileTransferSessions()	,
    			mRcsSettings.getImageResizeOption().toInt());
    }    

	/**
	 * Add outgoing file transfer to DB
	 *
	 * @param fileTransferId File transfer ID
	 * @param contact ContactId
	 * @param content Content of file
	 * @param fileicon Content of fileicon
	 * @param state state of the file transfer
	 */
	private void addOutgoingFileTransfer(String fileTransferId, ContactId contact,
			MmContent content, MmContent fileicon, int state) {
		mMessagingLog.addFileTransfer(fileTransferId, contact, Direction.OUTGOING,
				content, fileicon, state, ReasonCode.UNSPECIFIED);
		mOneToOneFileTransferBroadcaster.broadcastStateChanged(contact, fileTransferId,
				state, ReasonCode.UNSPECIFIED);
	}

	/**
	 * Add outgoing group file transfer to DB
	 * 
	 * @param fileTransferId File transfer ID
	 * @param chatId Chat ID of group chat
	 * @param content Content of file
	 * @param fileicon Content of fileicon
	 * @param state state of file transfer
	 */
	private void addOutgoingGroupFileTransfer(String fileTransferId, String chatId,
			MmContent content, MmContent fileicon, int state) {
		mMessagingLog.addOutgoingGroupFileTransfer(fileTransferId, chatId, content,
				fileicon, state, FileTransfer.ReasonCode.UNSPECIFIED);
		mGroupFileTransferBroadcaster.broadcastStateChanged(chatId, fileTransferId, state,
				ReasonCode.UNSPECIFIED);
	}

	/**
	 * 1-1 file send operation initiated
	 *
	 * @param contact
	 * @param file
	 * @param fileIcon
	 * @param fileTransferId
	 * return IFileTransfer OneToOneFileTransferImpl
	 * @throws ServerApiException
	 */
	private IFileTransfer sendOneToOneFile(ContactId contact, MmContent file, MmContent fileIcon,
			String fileTransferId) throws ServerApiException {
		try {
			mImService.assertFileSizeNotExceedingMaxLimit(file.getSize(), "File exceeds max size");

			if (!mImService.isFileTransferSessionAvailable()
					|| mImService.isMaxConcurrentOutgoingFileTransfersReached()) {
				if (logger.isActivated()) {
					logger.debug("The max number of file transfer sessions is achieved: queue the file transfer.");
				}
				addOutgoingFileTransfer(fileTransferId, contact, file, fileIcon,
						FileTransfer.State.QUEUED);
				return new OneToOneFileTransferImpl(fileTransferId,
						mOneToOneFileTransferBroadcaster, mImService,
						new FileTransferPersistedStorageAccessor(fileTransferId, mMessagingLog),
						this);
			}
			addOutgoingFileTransfer(fileTransferId, contact, file, fileIcon,
					FileTransfer.State.INITIATING);
			final FileSharingSession session = mImService.initiateFileTransferSession(
					fileTransferId, contact, file, fileIcon);

			OneToOneFileTransferImpl oneToOneFileTransfer = new OneToOneFileTransferImpl(
					fileTransferId, mOneToOneFileTransferBroadcaster, mImService,
					new FileTransferPersistedStorageAccessor(fileTransferId, mMessagingLog), this);
			session.addListener(oneToOneFileTransfer);
			addFileTransfer(oneToOneFileTransfer);

			new Thread() {
				public void run() {
					session.startSession();
				}
			}.start();
			return oneToOneFileTransfer;

		} catch (Exception e) {
			/*
			 * TODO: This is not correct implementation. It will be fixed
			 * properly in CR037
			 */
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e);
		}
	}

	/**
     * Transfers a file to a contact. The parameter file contains the URI of the
     * file to be transferred (for a local or a remote file). The parameter
     * contact supports the following formats: MSISDN in national or
     * international format, SIP address, SIP-URI or Tel-URI. If the format of
     * the contact is not supported an exception is thrown.
	 * 
	 * @param contact
	 *            Contact
	 * @param file
	 *            URI of file to transfer
	 * @param attachfileIcon
	 *            true if the stack must try to attach fileIcon
	 * @return FileTransfer
	 * @throws ServerApiException
	 */
    public IFileTransfer transferFile(ContactId contact, Uri file, boolean attachfileIcon) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Transfer file " + file + " to " + contact + " (fileicon=" + attachfileIcon + ")");
		}
		try {
			FileDescription fileDescription = FileFactory.getFactory().getFileDescription(file);
			MmContent fileIconContent = null;
			MmContent content = ContentManager.createMmContent(file, fileDescription.getSize(),
					fileDescription.getName());

			String fileTransferId = IdGenerator.generateMessageID();
			if (attachfileIcon && MimeManager.isImageType(content.getEncoding())) {
				fileIconContent = FileTransferUtils.createFileicon(file, fileTransferId);
			}

			/* If the IMS is connected at this time then send this one to one file. */
			if (ServerApiUtils.isImsConnected()) {
				return sendOneToOneFile(contact, content, fileIconContent, fileTransferId);
			}
			/* If the IMS is NOT connected at this time then queue this one to one file. */
			addOutgoingFileTransfer(fileTransferId, contact, content, fileIconContent,
					FileTransfer.State.QUEUED);
			return new OneToOneFileTransferImpl(fileTransferId, mOneToOneFileTransferBroadcaster,
					mImService, new FileTransferPersistedStorageAccessor(fileTransferId,
							mMessagingLog), this);

		} catch (Exception e) {
			/*
			 * TODO: This is not the correct way to handle this exception, and
			 * will be fixed in CR037
			 */
			if (logger.isActivated()) {
				logger.error("Unexpected exception", e);
			}
			throw new ServerApiException(e);
		}
	}

	/**
	 * Group file send operation initiated
	 *
	 * @param participants
	 * @param content
	 * @param fileIcon
	 * @param chatId
	 * @param fileTransferId
	 * @throws ServerApiException
	 */
	private IFileTransfer sendGroupFile(Set<ParticipantInfo> participants, MmContent content,
			MmContent fileIcon, String chatId, String fileTransferId) throws ServerApiException {
		try {
			mImService.assertFileSizeNotExceedingMaxLimit(content.getSize(),
					"File exceeds max size.");

			if (!mImService.isFileTransferSessionAvailable()
					|| mImService.isMaxConcurrentOutgoingFileTransfersReached()) {
				if (logger.isActivated()) {
					logger.debug("The max number of file transfer sessions is achieved: queue the file transfer.");
				}
				addOutgoingGroupFileTransfer(fileTransferId, chatId, content, fileIcon,
						State.QUEUED);
				return new GroupFileTransferImpl(fileTransferId, chatId,
						mGroupFileTransferBroadcaster, mImService,
						new FileTransferPersistedStorageAccessor(fileTransferId, mMessagingLog),
						this);
			}
			final GroupChatSession groupChatSession = mImService.getGroupChatSession(chatId);
			String chatSessionId = groupChatSession != null ? groupChatSession.getSessionID()
					: null;
			/* If groupChatSession is established send this group file transfer. */
			if (chatSessionId != null && groupChatSession.isMediaEstablished()) {
				addOutgoingGroupFileTransfer(fileTransferId, chatId, content, fileIcon,
						State.INITIATING);
				final FileSharingSession session = mImService.initiateGroupFileTransferSession(
						fileTransferId, participants, content, fileIcon, chatId, chatSessionId);

				GroupFileTransferImpl groupFileTransfer = new GroupFileTransferImpl(
						session.getFileTransferId(), chatId, mGroupFileTransferBroadcaster,
						mImService, new FileTransferPersistedStorageAccessor(fileTransferId,
								mMessagingLog), this);
				session.addListener(groupFileTransfer);
				addFileTransfer(groupFileTransfer);

				new Thread() {
					public void run() {
						session.startSession();
					}
				}.start();
				return groupFileTransfer;
			}
			/*
			 * If groupChatSession is NOT established then queue this file
			 * transfer and try to rejoin group chat.
			 */
			addOutgoingGroupFileTransfer(fileTransferId, chatId, content, fileIcon,
					State.QUEUED);
			if (groupChatSession != null) {
				if (groupChatSession.isInitiatedByRemote()) {
					if (logger.isActivated()) {
						logger.debug("Group chat session is pending: auto accept it.");
					}
					new Thread() {
						public void run() {
							groupChatSession.acceptSession();
						}
					}.start();
				}
			} else {
				try {
					mCore.getListener().handleRejoinGroupChatAsPartOfSendOperation(chatId);

				} catch (ServerApiException e) {
					/*
					 * failed to rejoin group chat session. Ignoring this
					 * exception because we want to try again later.
					 */
				}
			}
			return new GroupFileTransferImpl(fileTransferId, chatId, mGroupFileTransferBroadcaster,
					mImService, new FileTransferPersistedStorageAccessor(fileTransferId,
							mMessagingLog), this);

		} catch (Exception e) {
			/*
			 * TODO: Handle Security exception in Exception handling CR037
			 */
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}

    /**
     * Transfers a file to participants. The parameter file contains the URI of the
     * file to be transferred (for a local or a remote file).
	 *
	 * @param chatId ChatId of group chat
	 * @param file
	 *            Uri of file to transfer
	 * @param attachfileIcon
	 *            true if the stack must try to attach fileIcon
	 * @return FileTransfer
	 * @throws ServerApiException
	 */
	public IFileTransfer transferFileToGroupChat(String chatId, Uri file, boolean attachfileIcon) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("sendFile (file=" + file + ") (fileicon=" + attachfileIcon + ")");
		}
		try {
			FileDescription fileDescription = FileFactory.getFactory().getFileDescription(file);
			MmContent content = ContentManager.createMmContent(file, fileDescription.getSize(),
					fileDescription.getName());

			Set<ParticipantInfo> participants = mMessagingLog
					.getGroupChatConnectedParticipants(chatId);
			String fileTransferId = IdGenerator.generateMessageID();
			MmContent fileIconContent = null;
			if (attachfileIcon && MimeManager.isImageType(content.getEncoding())) {
				fileIconContent = FileTransferUtils
						.createFileicon(content.getUri(), fileTransferId);
			}

			/* If the IMS is connected at this time then send this group file. */
			if (ServerApiUtils.isImsConnected()) {
				return sendGroupFile(participants, content, fileIconContent, chatId, fileTransferId);
			}
			/* If the IMS is NOT connected at this time then queue this group file. */
			addOutgoingGroupFileTransfer(fileTransferId, chatId, content, fileIconContent,
					State.QUEUED);
			return new GroupFileTransferImpl(fileTransferId, chatId, mGroupFileTransferBroadcaster,
					mImService, new FileTransferPersistedStorageAccessor(fileTransferId,
							mMessagingLog), this);

		} catch (Exception e) {
			/*
			 * TODO: Handle Security exception in CR037
			 */
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}

    /**
     * Returns the list of file transfers in progress
     * 
     * @return List of file transfer
     * @throws ServerApiException
     */
    public List<IBinder> getFileTransfers() throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get file transfer sessions");
		}

		try {
			List<IBinder> fileTransfers = new ArrayList<IBinder>(mFileTransferCache.size());
			for (IFileTransfer fileTransfer : mFileTransferCache.values()) {
				fileTransfers.add(fileTransfer.asBinder());
			}
			return fileTransfers;

		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
    }    

    /**
     * Returns a current file transfer from its unique ID
     * 
     * @return File transfer
     * @throws ServerApiException
     */
	public IFileTransfer getFileTransfer(String transferId) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get file transfer session " + transferId);
		}

		IFileTransfer fileTransfer = mFileTransferCache.get(transferId);
		if (fileTransfer != null) {
			return fileTransfer;
		}
		FileTransferPersistedStorageAccessor storageAccessor = new FileTransferPersistedStorageAccessor(
				transferId, mMessagingLog);
		if (mMessagingLog.isGroupFileTransfer(transferId)) {
			return new GroupFileTransferImpl(transferId, mGroupFileTransferBroadcaster, mImService,
					storageAccessor, this);
		}
		return new OneToOneFileTransferImpl(transferId, mOneToOneFileTransferBroadcaster,
				mImService, storageAccessor, this);
	}
    
    /**
	 * Adds a listener on file transfer events
	 * 
	 * @param listener OneToOne file transfer listener
	 * @throws ServerApiException
	 */
	public void addEventListener2(IOneToOneFileTransferListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Add a OneToOne file transfer invitation listener");
		}
		synchronized (lock) {
			mOneToOneFileTransferBroadcaster.addOneToOneFileTransferListener(listener);
		}
	}

	/**
	 * Removes a listener on file transfer events
	 * 
	 * @param listener OneToOne file transfer listener
	 * @throws ServerApiException
	 */
	public void removeEventListener2(IOneToOneFileTransferListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Remove a OneToOne file transfer invitation listener");
		}
		synchronized (lock) {
			mOneToOneFileTransferBroadcaster.removeOneToOneFileTransferListener(listener);
		}
	}

    /**
	 * Adds a listener on group file transfer events
	 *
	 * @param listener Group file transfer listener
	 * @throws ServerApiException
	 */
	public void addEventListener3(IGroupFileTransferListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Add a group file transfer invitation listener");
		}
		synchronized (lock) {
			mGroupFileTransferBroadcaster.addGroupFileTransferListener(listener);
		}
	}

	/**
	 * Removes a listener on group file transfer events
	 *
	 * @param listener Group file transfer listener
	 * @throws ServerApiException
	 */
	public void removeEventListener3(IGroupFileTransferListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Remove a group file transfer invitation listener");
		}
		synchronized (lock) {
			mGroupFileTransferBroadcaster.removeGroupFileTransferListener(listener);
		}
	}

	
	/**
	 * File Transfer delivery status. In FToHTTP, Delivered status is done just
	 * after download information are received by the terminating, and Displayed
	 * status is done when the file is downloaded. In FToMSRP, the two status
	 * are directly done just after MSRP transfer complete.
	 * 
	 * @param imdn Imdn document
	 * @param contact contact who received file
	 */
	public void handleFileDeliveryStatus(ImdnDocument imdn, ContactId contact) {
		String status = imdn.getStatus();
		/*Note: File transfer ID always corresponds to message ID in the imdn pay-load*/
		String fileTransferId = imdn.getMsgId();
		if (ImdnDocument.DELIVERY_STATUS_DELIVERED.equals(status)) {
			mMessagingLog.setFileTransferStateAndReasonCode(fileTransferId,
					FileTransfer.State.DELIVERED, ReasonCode.UNSPECIFIED);

			mOneToOneFileTransferBroadcaster.broadcastStateChanged(contact, fileTransferId,
					FileTransfer.State.DELIVERED, ReasonCode.UNSPECIFIED);
		} else if (ImdnDocument.DELIVERY_STATUS_DISPLAYED.equals(status)) {
			mMessagingLog.setFileTransferStateAndReasonCode(fileTransferId,
					FileTransfer.State.DISPLAYED, ReasonCode.UNSPECIFIED);

			mOneToOneFileTransferBroadcaster.broadcastStateChanged(contact, fileTransferId,
					FileTransfer.State.DISPLAYED, ReasonCode.UNSPECIFIED);
		} else if (ImdnDocument.DELIVERY_STATUS_ERROR.equals(status)
				|| ImdnDocument.DELIVERY_STATUS_FAILED.equals(status)
				|| ImdnDocument.DELIVERY_STATUS_FORBIDDEN.equals(status)) {
			int reasonCode = imdnToFileTransferFailedReasonCode(imdn);

			mMessagingLog.setFileTransferStateAndReasonCode(fileTransferId,
					FileTransfer.State.FAILED, reasonCode);

			mOneToOneFileTransferBroadcaster.broadcastStateChanged(contact, fileTransferId,
					FileTransfer.State.FAILED, reasonCode);
		}
	}

    private void handleGroupFileDeliveryStatusDelivered(String chatId, String fileTransferId,
            ContactId contact) {
        mMessagingLog.setGroupChatDeliveryInfoStatusAndReasonCode(fileTransferId, contact,
                GroupDeliveryInfoLog.Status.DELIVERED, GroupDeliveryInfoLog.ReasonCode.UNSPECIFIED);
        mGroupFileTransferBroadcaster.broadcastGroupDeliveryInfoStateChanged(chatId, contact,
                fileTransferId, GroupDeliveryInfoLog.Status.DELIVERED,
                GroupDeliveryInfoLog.ReasonCode.UNSPECIFIED);
        if (mMessagingLog.isDeliveredToAllRecipients(fileTransferId)) {
            mMessagingLog.setFileTransferStateAndReasonCode(fileTransferId,
                    FileTransfer.State.DELIVERED, ReasonCode.UNSPECIFIED);
            mGroupFileTransferBroadcaster.broadcastStateChanged(chatId, fileTransferId,
                    FileTransfer.State.DELIVERED, ReasonCode.UNSPECIFIED);
        }
    }

    private void handleGroupFileDeliveryStatusDisplayed(String chatId, String fileTransferId,
            ContactId contact) {
        mMessagingLog.setGroupChatDeliveryInfoStatusAndReasonCode(fileTransferId, contact,
                GroupDeliveryInfoLog.Status.DISPLAYED, GroupDeliveryInfoLog.ReasonCode.UNSPECIFIED);
        mGroupFileTransferBroadcaster.broadcastGroupDeliveryInfoStateChanged(chatId, contact,
                fileTransferId, GroupDeliveryInfoLog.Status.DISPLAYED,
                GroupDeliveryInfoLog.ReasonCode.UNSPECIFIED);
        if (mMessagingLog.isDisplayedByAllRecipients(fileTransferId)) {
            mMessagingLog.setFileTransferStateAndReasonCode(fileTransferId,
                    FileTransfer.State.DISPLAYED, ReasonCode.UNSPECIFIED);
            mGroupFileTransferBroadcaster.broadcastStateChanged(chatId, fileTransferId,
                    FileTransfer.State.DISPLAYED, ReasonCode.UNSPECIFIED);
        }
    }

    private void handleGroupFileDeliveryStatusFailed(String chatId, String fileTransferId,
            ContactId contact, int reasonCode) {
        if (ReasonCode.FAILED_DELIVERY == reasonCode) {
            mMessagingLog.setGroupChatDeliveryInfoStatusAndReasonCode(fileTransferId, contact,
                    GroupDeliveryInfoLog.Status.FAILED,
                    GroupDeliveryInfoLog.ReasonCode.FAILED_DELIVERY);
            mGroupFileTransferBroadcaster.broadcastGroupDeliveryInfoStateChanged(chatId, contact,
                    fileTransferId, GroupDeliveryInfoLog.Status.FAILED,
                    GroupDeliveryInfoLog.ReasonCode.FAILED_DELIVERY);
            return;
        }
        mMessagingLog.setGroupChatDeliveryInfoStatusAndReasonCode(fileTransferId, contact,
                GroupDeliveryInfoLog.Status.FAILED, GroupDeliveryInfoLog.ReasonCode.FAILED_DISPLAY);
        mGroupFileTransferBroadcaster.broadcastGroupDeliveryInfoStateChanged(chatId, contact,
                fileTransferId, GroupDeliveryInfoLog.Status.FAILED,
                GroupDeliveryInfoLog.ReasonCode.FAILED_DISPLAY);
    }

    /**
     * Handles group file transfer delivery status.
     * 
     * @param chatId Chat ID
     * @param imdn Imdn Document
     * @param contact Contact ID
     */
    public void handleGroupFileDeliveryStatus(String chatId, ImdnDocument imdn, ContactId contact) {
        String status = imdn.getStatus();
        String msgId = imdn.getMsgId();
        if (logger.isActivated()) {
            logger.info(new StringBuilder("Handling group file delivery status; contact=")
                    .append(contact).append(", msgId=").append(msgId).append(", status=")
                    .append(status).append(", notificationType=")
                    .append(imdn.getNotificationType()).toString());
        }
        if (ImdnDocument.DELIVERY_STATUS_DELIVERED.equals(status)) {
            handleGroupFileDeliveryStatusDelivered(chatId, msgId, contact);
        } else if (ImdnDocument.DELIVERY_STATUS_DISPLAYED.equals(status)) {
            handleGroupFileDeliveryStatusDisplayed(chatId, msgId, contact);
        } else if (ImdnDocument.DELIVERY_STATUS_ERROR.equals(status)
                || ImdnDocument.DELIVERY_STATUS_FAILED.equals(status)
                || ImdnDocument.DELIVERY_STATUS_FORBIDDEN.equals(status)) {
            int reasonCode = imdnToFileTransferFailedReasonCode(imdn);
            handleGroupFileDeliveryStatusFailed(chatId, msgId, contact, reasonCode);
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
	
	 /**
     * Resume an outgoing HTTP file transfer
     *
     * @param session File transfer session
     * @param isGroup is group file transfer
     */
	public void resumeOutgoingFileTransfer(FileSharingSession session, boolean isGroup) {
		if (logger.isActivated()) {
			logger.info("Resume outgoing file transfer from " + session.getRemoteContact());
		}

		// Add session in the list
		String fileTransferId = session.getFileTransferId();
		FileTransferPersistedStorageAccessor storageAccessor = new FileTransferPersistedStorageAccessor(
				fileTransferId, mMessagingLog);
		if (isGroup) {
			GroupFileTransferImpl groupFileTransfer = new GroupFileTransferImpl(fileTransferId,
					session.getContributionID(), mGroupFileTransferBroadcaster, mImService,
					storageAccessor, this);
			session.addListener(groupFileTransfer);
			addFileTransfer(groupFileTransfer);
		} else {
			OneToOneFileTransferImpl oneToOneFileTransfer = new OneToOneFileTransferImpl(
					fileTransferId, mOneToOneFileTransferBroadcaster, mImService, storageAccessor,
					this);
			session.addListener(oneToOneFileTransfer);
			addFileTransfer(oneToOneFileTransfer);
		}
	}

	
	/**
     * Resume an incoming HTTP file transfer
     *
     * @param session File transfer session
     * @param isGroup is group file transfer
     * @param chatSessionId corresponding chatSessionId
     * @param chatId corresponding chatId
     */
    public void resumeIncomingFileTransfer(FileSharingSession session, boolean isGroup, String chatSessionId, String chatId) {
        if (logger.isActivated()) {
            logger.info("Resume incoming file transfer from " + session.getRemoteContact());
        }
		String fileTransferId = session.getFileTransferId();
		FileTransferPersistedStorageAccessor storageAccessor = new FileTransferPersistedStorageAccessor(
				fileTransferId, mMessagingLog);
		if (isGroup) {
			GroupFileTransferImpl groupFileTransfer = new GroupFileTransferImpl(fileTransferId,
					session.getContributionID(), mGroupFileTransferBroadcaster, mImService,
					storageAccessor, this);
			session.addListener(groupFileTransfer);
			addFileTransfer(groupFileTransfer);
		} else {
			OneToOneFileTransferImpl oneToOneFileTransfer = new OneToOneFileTransferImpl(
					fileTransferId, mOneToOneFileTransferBroadcaster, mImService, storageAccessor,
					this);
			session.addListener(oneToOneFileTransfer);
			addFileTransfer(oneToOneFileTransfer);
		}
    }
	
	/**
     * Mark a received file transfer as read (i.e. the invitation or the file has been displayed in the UI).
     *
     * @param transferID File transfer ID
     */
	@Override
	public void markFileTransferAsRead(String transferId) throws RemoteException {
		//No notification type corresponds currently to mark as read
		mMessagingLog.markFileTransferAsRead(transferId);
	}

	/**
	 * Set Auto accept mode
	 * @param enable true is AA is enabled in normal conditions
	 */
	@Override
	public void setAutoAccept(boolean enable) throws RemoteException {
		if (!mRcsSettings.isFtAutoAcceptedModeChangeable()) {
			throw new IllegalArgumentException("Auto accept mode is not changeable");
		}
		mRcsSettings.setFileTransferAutoAccepted(enable);
		if (!enable) {
			// If AA is disabled in normal conditions then it must be disabled while roaming
			mRcsSettings.setFileTransferAutoAcceptedInRoaming(false);
		}
	}

	/**
	 * Set Auto accept mode in roaming
	 * @param enable true is AA is enabled in roaming
	 */
	@Override
	public void setAutoAcceptInRoaming(boolean enable) throws RemoteException {
		if (!mRcsSettings.isFtAutoAcceptedModeChangeable()) {
			throw new IllegalArgumentException("Auto accept mode in roaming is not changeable");
		}
		if (!mRcsSettings.isFileTransferAutoAccepted()) {
			throw new IllegalArgumentException("Auto accept mode in normal conditions must be enabled");
		}
		mRcsSettings.setFileTransferAutoAcceptedInRoaming(enable);
	}

	/**
	 * Set the image resize option
	 * 
	 * @param option
	 *            the image resize option (0: ALWAYS_PERFORM, 1: ONLY_ABOVE_MAX_SIZE, 2: ASK)
	 */
	@Override
	public void setImageResizeOption(int option) throws RemoteException {
		try {
			// TODO CR031
			ImageResizeOption imageResizeOption = ImageResizeOption.valueOf(option);
			mRcsSettings.setImageResizeOption(imageResizeOption);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	/**
	 * Add and broadcast file transfer invitation rejections
	 *
	 * @param contact Contact
	 * @param content File content
	 * @param fileIcon File content
	 * @param reasonCode Reason code
	 */
	public void addAndBroadcastFileTransferInvitationRejected(ContactId contact,
			MmContent content, MmContent fileIcon, int reasonCode) {
		String fileTransferId = IdGenerator.generateMessageID();
		mMessagingLog.addFileTransfer(fileTransferId, contact, Direction.INCOMING,
				content, fileIcon, FileTransfer.State.REJECTED, reasonCode);

		mOneToOneFileTransferBroadcaster.broadcastInvitation(fileTransferId);
	}
}
