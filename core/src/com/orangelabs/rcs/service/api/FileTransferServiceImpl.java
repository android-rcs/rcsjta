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

import static com.gsma.services.rcs.ft.FileTransfer.State.DELIVERED;
import static com.gsma.services.rcs.ft.FileTransfer.State.DISPLAYED;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

import com.gsma.services.rcs.IJoynServiceRegistrationListener;
import com.gsma.services.rcs.JoynService;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.ft.FileTransfer;
import com.gsma.services.rcs.ft.FileTransferIntent;
import com.gsma.services.rcs.ft.FileTransferServiceConfiguration;
import com.gsma.services.rcs.ft.IFileTransfer;
import com.gsma.services.rcs.ft.IFileTransferListener;
import com.gsma.services.rcs.ft.IFileTransferService;
import com.gsma.services.rcs.ft.IGroupFileTransferListener;
import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.content.ContentManager;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.platform.file.FileDescription;
import com.orangelabs.rcs.platform.file.FileFactory;
import com.orangelabs.rcs.provider.messaging.MessagingLog;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.service.broadcaster.GroupFileTransferBroadcaster;
import com.orangelabs.rcs.service.broadcaster.JoynServiceRegistrationEventBroadcaster;
import com.orangelabs.rcs.service.broadcaster.OneToOneFileTransferBroadcaster;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * File transfer service implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileTransferServiceImpl extends IFileTransferService.Stub {

	/**
	 * List of file transfer sessions
	 */
	private static Hashtable<String, IFileTransfer> ftSessions = new Hashtable<String, IFileTransfer>();  

	private final OneToOneFileTransferBroadcaster mOneToOneFileTransferBroadcaster = new OneToOneFileTransferBroadcaster();

	private final GroupFileTransferBroadcaster mGroupFileTransferBroadcaster = new GroupFileTransferBroadcaster();

	private final JoynServiceRegistrationEventBroadcaster mJoynServiceRegistrationEventBroadcaster = new JoynServiceRegistrationEventBroadcaster();

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
	 */
	public FileTransferServiceImpl() {
		if (logger.isActivated()) {
			logger.info("File transfer service API is loaded");
		}
	}

	/**
	 * Close API
	 */
	public void close() {
		// Clear list of sessions
		ftSessions.clear();
		
		if (logger.isActivated()) {
			logger.info("File transfer service API is closed");
		}
	}

	/**
	 * Add a file transfer session in the list
	 * 
	 * @param session File transfer session
	 */
	public static void addFileTransferSession(IFileTransfer session) {
		if (logger.isActivated()) {
			logger.debug("Add a file transfer session in the list (size=" + ftSessions.size() + ")");
		}

		try {
			ftSessions.put(session.getTransferId(), session);
		} catch (RemoteException e) {
			if (logger.isActivated()) {
				logger.info("Unable to add file transfer session! " +e);
			}
		}
	}

	/**
	 * Remove a file transfer session from the list
	 * 
	 * @param fileTransferId File transfer ID
	 */
	protected static void removeFileTransferSession(String fileTransferId) {
		if (logger.isActivated()) {
			logger.debug("Remove a file transfer session from the list (size=" + ftSessions.size() + ")");
		}
		
		ftSessions.remove(fileTransferId);
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
	 * Receive a new file transfer invitation
	 * 
	 * @param session File transfer session
	 * @param isGroup is group file transfer
	 * @param contact Contact ID
	 */
    public void receiveFileTransferInvitation(FileSharingSession session, boolean isGroup, ContactId contact) {
		if (logger.isActivated()) {
			logger.info("Receive FT invitation from " + session.getRemoteContact() + " file=" + session.getContent().getName()
					+ " size=" + session.getContent().getSize());
		}

		// Update rich messaging history
		if (isGroup) {
			MessagingLog.getInstance().addIncomingGroupFileTransfer(session.getContributionID(),
					contact, session.getFileTransferId(), session.getContent(), session.getFileicon());
		} else {
			MessagingLog.getInstance().addFileTransfer(contact, session.getFileTransferId(),
					FileTransfer.Direction.INCOMING, session.getContent(), session.getFileicon());
		}

		// Add session in the list
		if (isGroup) {
			GroupFileTransferImpl groupFileTransfer = new GroupFileTransferImpl(session,
					mGroupFileTransferBroadcaster);
			FileTransferServiceImpl.addFileTransferSession(groupFileTransfer);
		} else {
			OneToOneFileTransferImpl oneToOneFileTransfer = new OneToOneFileTransferImpl(session,
					mOneToOneFileTransferBroadcaster);
			FileTransferServiceImpl.addFileTransferSession(oneToOneFileTransfer);
		}

		// Broadcast intent related to the received invitation
    	Intent intent = new Intent(FileTransferIntent.ACTION_NEW_INVITATION);
    	intent.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
    	intent.putExtra(FileTransferIntent.EXTRA_CONTACT, (Parcelable)contact);
    	intent.putExtra(FileTransferIntent.EXTRA_DISPLAY_NAME, session.getRemoteDisplayName());
    	intent.putExtra(FileTransferIntent.EXTRA_TRANSFER_ID, session.getFileTransferId());
    	intent.putExtra(FileTransferIntent.EXTRA_FILENAME, session.getContent().getName());
    	intent.putExtra(FileTransferIntent.EXTRA_FILESIZE, session.getContent().getSize());
    	intent.putExtra(FileTransferIntent.EXTRA_FILETYPE, session.getContent().getEncoding());
    	/* TODO if (session instanceof HttpFileTransferSession) {
    	    intent.putExtra("chatSessionId", ((HttpFileTransferSession)session).getChatSessionID());
    	    if (isGroup) {
    	        intent.putExtra("chatId", ((HttpFileTransferSession)session).getContributionID());
    	    }
    	    intent.putExtra("isGroupTransfer", isGroup);
    	}*/
    	AndroidFactory.getApplicationContext().sendBroadcast(intent);
    }

    /**
	 * Receive a new HTTP file transfer invitation outside of an existing chat session
	 *
	 * @param session File transfer session
	 */
	public void receiveFileTransferInvitation(FileSharingSession session, ChatSession chatSession, ContactId contact) {
		// Update rich messaging history
		if (chatSession.isGroupChat()) {
			MessagingLog.getInstance().updateFileTransferChatId(
					chatSession.getFirstMessage().getMessageId(), chatSession.getContributionID());
			GroupFileTransferImpl groupFileTransfer = new GroupFileTransferImpl(session,
					mGroupFileTransferBroadcaster);
			addFileTransferSession(groupFileTransfer);
		} else {
			OneToOneFileTransferImpl oneToOneFileTransfer = new OneToOneFileTransferImpl(session,
					mOneToOneFileTransferBroadcaster);
			addFileTransferSession(oneToOneFileTransfer);
		}

		// Display invitation
		receiveFileTransferInvitation(session, chatSession.isGroupChat(), contact);
	}    
	
    /**
     * Returns the configuration of the file transfer service
     * 
     * @return Configuration
     */
    public FileTransferServiceConfiguration getConfiguration() {
    	RcsSettings rs = RcsSettings.getInstance();
    	return new FileTransferServiceConfiguration(
    			rs.getWarningMaxFileTransferSize(),
    			rs.getMaxFileTransferSize(),
    			rs.isFtAutoAcceptedModeChangeable(),
    			rs.isFileTransferAutoAccepted(),
    			rs.isFileTransferAutoAcceptedInRoaming(),
    			rs.isFileTransferThumbnailSupported(),
    			rs.getMaxFileTransferSessions()	,
    			rs.getImageResizeOption());
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
	 * @param fileicon
	 *            true if the stack must try to attach fileicon
	 * @return File transfer
	 * @throws ServerApiException
	 */
    public IFileTransfer transferFile(ContactId contact, Uri file, boolean fileicon) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Transfer file " + file + " to " + contact + " (fileicon=" + fileicon + ")");
		}

		// Test IMS connection
		ServerApiUtils.testIms();

		try {
			// Initiate the session
			FileDescription fileDescription = FileFactory.getFactory().getFileDescription(file);
			MmContent content = ContentManager.createMmContent(file, fileDescription.getSize(), fileDescription.getName());
			if (content == null || content.getSize() <= 0 || content.getEncoding() == null || content.getName() == null) {
				throw new IllegalArgumentException("FileTransfer initiation failed: invalid file");
			}
			final FileSharingSession session = Core.getInstance().getImService().initiateFileTransferSession(contact, content, fileicon);

			OneToOneFileTransferImpl oneToOneFileTransfer = new OneToOneFileTransferImpl(session, mOneToOneFileTransferBroadcaster);

			// Update rich messaging history
			MessagingLog.getInstance().addFileTransfer(contact, session.getFileTransferId(),
					FileTransfer.Direction.OUTGOING, session.getContent(), session.getFileicon());

			// Start the session
	        new Thread() {
	    		public void run() {
	    			session.startSession();
	    		}
	    	}.start();
						
			// Add session in the list
			addFileTransferSession(oneToOneFileTransfer);
			return oneToOneFileTransfer;
		} catch(Exception e) {
			// TODO:Handle Security exception in CR026
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}

			if (e instanceof RuntimeException) {
				throw (RuntimeException)e;
			}
			throw new ServerApiException(e);
		}
    }

    /**
     * Transfers a file to participants. The parameter file contains the URI of the
     * file to be transferred (for a local or a remote file).
	 *
	 * @param chatId ChatId of group chat
	 * @param file
	 *            Uri of file to transfer
	 * @param fileicon
	 *            true if the stack must try to attach fileicon
	 * @return File transfer
	 * @throws ServerApiException
	 */
	public IFileTransfer transferFileToGroupChat(String chatId, Uri file, boolean fileicon) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("sendFile (file=" + file + ") (fileicon=" + fileicon + ")");
		}
		try {
			// Initiate the session
			FileDescription fileDescription = FileFactory.getFactory().getFileDescription(file);
			MmContent content = ContentManager.createMmContent(file, fileDescription.getSize(),
					fileDescription.getName());
			if (content == null || content.getSize() <= 0 || content.getEncoding() == null || content.getName() == null) {
				throw new IllegalArgumentException("FileTransfer initiation failed: invalid file");
			}
			Set<ParticipantInfo> participants = MessagingLog.getInstance()
					.getGroupChatConnectedParticipants(chatId);
			final FileSharingSession session = Core
					.getInstance()
					.getImService()
					.initiateGroupFileTransferSession(participants, content, fileicon,
							chatId);

			// Add session listener
			GroupFileTransferImpl groupFileTransfer = new GroupFileTransferImpl(session, mGroupFileTransferBroadcaster);

			// Update rich messaging history
			MessagingLog.getInstance().addOutgoingGroupFileTransfer(session.getContributionID(),
					session.getFileTransferId(), session.getContent(), session.getFileicon());

			// Start the session
			new Thread() {
				public void run() {
					// Start the session
					session.startSession();
				}
			}.start();

			// Add session in the list
			FileTransferServiceImpl.addFileTransferSession(groupFileTransfer);
			return groupFileTransfer;

		} catch (Exception e) {
			// TODO:Handle Security exception in CR026
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			if (e instanceof RuntimeException) {
				throw (RuntimeException)e;
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
			ArrayList<IBinder> result = new ArrayList<IBinder>(ftSessions.size());
			for (Enumeration<IFileTransfer> e = ftSessions.elements() ; e.hasMoreElements() ;) {
				IFileTransfer sessionApi = e.nextElement() ;
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
     * Returns a current file transfer from its unique ID
     * 
     * @return File transfer
     * @throws ServerApiException
     */
    public IFileTransfer getFileTransfer(String transferId) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get file transfer session " + transferId);
		}

		return ftSessions.get(transferId);
    }    
    
    /**
	 * Registers a file transfer listener
	 * 
	 * @param listener OneToOne file transfer listener
	 * @throws ServerApiException
	 */
	public void addOneToOneFileTransferListener(IFileTransferListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Add a OneToOne file transfer invitation listener");
		}
		synchronized (lock) {
			mOneToOneFileTransferBroadcaster.addOneToOneFileTransferListener(listener);
		}
	}

	/**
	 * Unregisters a file transfer listener
	 * 
	 * @param listener OneToOne file transfer listener
	 * @throws ServerApiException
	 */
	public void removeOneToOneFileTransferListener(IFileTransferListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Remove a OneToOne file transfer invitation listener");
		}
		synchronized (lock) {
			mOneToOneFileTransferBroadcaster.removeOneToOneFileTransferListener(listener);
		}
	}

    /**
	 * Adds a listener for group chat events
	 *
	 * @param listener Group file transfer listener
	 * @throws ServerApiException
	 */
	public void addGroupFileTransferListener(IGroupFileTransferListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Add a group file transfer invitation listener");
		}
		synchronized (lock) {
			mGroupFileTransferBroadcaster.addGroupFileTransferListener(listener);
		}
	}

	/**
	 * Removes a listener for group chat events
	 *
	 * @param listener Group file transfer listener
	 * @throws ServerApiException
	 */
	public void removeGroupFileTransferListener(IGroupFileTransferListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Remove a group file transfer invitation listener");
		}
		synchronized (lock) {
			mGroupFileTransferBroadcaster.removeGroupFileTransferListener(listener);
		}
	}

	
    /**
     * File Transfer delivery status.
     * In FToHTTP, Delivered status is done just after download information are received by the
     * terminating, and Displayed status is done when the file is downloaded.
     * In FToMSRP, the two status are directly done just after MSRP transfer complete.
     *
     * @param fileTransferId File transfer Id
     * @param status status of File transfer
     * @param contact contact who received file
     */
	public void handleFileDeliveryStatus(String fileTransferId, String status, ContactId contact) {
		if (status.equalsIgnoreCase(ImdnDocument.DELIVERY_STATUS_DELIVERED)) {
			// Update rich messaging history
			MessagingLog.getInstance().updateFileTransferStatus(fileTransferId, DELIVERED);

			// Notify File transfer delivery listeners
			mOneToOneFileTransferBroadcaster.broadcastTransferStateChanged(contact, fileTransferId,
					DELIVERED);
		} else if (status.equalsIgnoreCase(ImdnDocument.DELIVERY_STATUS_DISPLAYED)) {
			// Update rich messaging history
			MessagingLog.getInstance().updateFileTransferStatus(fileTransferId, DISPLAYED);

			// Notify File transfer delivery listeners
			mOneToOneFileTransferBroadcaster.broadcastTransferStateChanged(contact, fileTransferId,
					DISPLAYED);
		}
	}

    /**
     * Group File Transfer delivery status delivered
     *
     * @param chatId
     * @param fileTransferId File transfer Id
     * @param contact Contact who received file
     * @param state State to set
     */
	private void handleGroupFileDeliveryStatus(String chatId, String fileTransferId,
			ContactId contact, int state) {
		// Update rich messaging history
		MessagingLog messagingLog = MessagingLog.getInstance();
		messagingLog.updateGroupChatDeliveryInfoStatus(fileTransferId, state,
				ChatLog.GroupChatDeliveryInfo.ReasonCode.NONE, contact);
		mGroupFileTransferBroadcaster.broadcastSingleRecipientDeliveryStateChanged(chatId,
				contact, fileTransferId, state);
		if (state == DELIVERED && messagingLog.isDeliveredToAllRecipients(fileTransferId)
				|| state == DISPLAYED && messagingLog.isDisplayedByAllRecipients(fileTransferId)) {
			messagingLog.updateFileTransferStatus(fileTransferId, state);
			// Notify File transfer delivery listeners
			mGroupFileTransferBroadcaster.broadcastTransferStateChanged(chatId, fileTransferId, state);
		}
	}

    /**
	 * Group File Transfer delivery status.
	 *
	 * @param fileTransferId File transfer Id
	 * @param status status of File transfer
	 * @param contact contact who received file
	 */
	public void handleGroupFileDeliveryStatus(String chatId, String fileTransferId, String status,
			ContactId contact) {
		if (status.equalsIgnoreCase(ImdnDocument.DELIVERY_STATUS_DELIVERED)) {
			handleGroupFileDeliveryStatus(chatId, fileTransferId, contact, DELIVERED);
		} else if (status.equalsIgnoreCase(ImdnDocument.DELIVERY_STATUS_DISPLAYED)) {
			handleGroupFileDeliveryStatus(chatId, fileTransferId, contact, DISPLAYED);
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
		if (isGroup) {
			GroupFileTransferImpl sessionApi = new GroupFileTransferImpl(session,
					mGroupFileTransferBroadcaster);
			FileTransferServiceImpl.addFileTransferSession(sessionApi);
		} else {
			OneToOneFileTransferImpl sessionApi = new OneToOneFileTransferImpl(session,
					mOneToOneFileTransferBroadcaster);
			FileTransferServiceImpl.addFileTransferSession(sessionApi);
		}

		// Broadcast intent related to the received invitation
		Intent intent = new Intent(FileTransferIntent.ACTION_RESUME);
		intent.putExtra(FileTransferIntent.EXTRA_CONTACT, (Parcelable)session.getRemoteContact());
		intent.putExtra(FileTransferIntent.EXTRA_DISPLAY_NAME, session.getRemoteDisplayName());
		intent.putExtra(FileTransferIntent.EXTRA_TRANSFER_ID, session.getFileTransferId());
		if (isGroup) {
			intent.putExtra(FileTransferIntent.EXTRA_CHAT_ID, session.getContributionID());
		}
		intent.putExtra(FileTransferIntent.EXTRA_FILENAME, session.getContent().getName());
		intent.putExtra(FileTransferIntent.EXTRA_FILESIZE, session.getContent().getSize());
		intent.putExtra(FileTransferIntent.EXTRA_FILETYPE, session.getContent().getEncoding());
		// TODO FUSION change thumbnail byte array to filename
		// intent.putExtra(FileTransferIntent.EXTRA_FILEICON, session.getThumbnail());
		intent.putExtra(FileTransferIntent.EXTRA_DIRECTION, FileTransfer.Direction.OUTGOING);
		AndroidFactory.getApplicationContext().sendBroadcast(intent);
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

		if (isGroup) {
			GroupFileTransferImpl sessionApi = new GroupFileTransferImpl(session,
					mGroupFileTransferBroadcaster);
			FileTransferServiceImpl.addFileTransferSession(sessionApi);
		} else {
			OneToOneFileTransferImpl sessionApi = new OneToOneFileTransferImpl(session,
					mOneToOneFileTransferBroadcaster);
			FileTransferServiceImpl.addFileTransferSession(sessionApi);
		}

        // Broadcast intent, we reuse the File transfer invitation intent
        Intent intent = new Intent(FileTransferIntent.ACTION_RESUME);
    	intent.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);

        intent.putExtra(FileTransferIntent.EXTRA_CONTACT, (Parcelable)session.getRemoteContact());
        intent.putExtra(FileTransferIntent.EXTRA_DISPLAY_NAME, session.getRemoteDisplayName());
        intent.putExtra(FileTransferIntent.EXTRA_TRANSFER_ID, session.getFileTransferId());
        if (isGroup) {
            intent.putExtra(FileTransferIntent.EXTRA_CHAT_ID, chatId);
        }
        intent.putExtra(FileTransferIntent.EXTRA_FILENAME, session.getContent().getName());
        intent.putExtra(FileTransferIntent.EXTRA_FILESIZE, session.getContent().getSize());
        intent.putExtra(FileTransferIntent.EXTRA_FILETYPE, session.getContent().getEncoding());
        intent.putExtra(FileTransferIntent.EXTRA_DIRECTION, FileTransfer.Direction.INCOMING);

        AndroidFactory.getApplicationContext().sendBroadcast(intent);
    }
	
	/**
     * Mark a received file transfer as read (i.e. the invitation or the file has been displayed in the UI).
     *
     * @param transferID File transfer ID
     */
	@Override
	public void markFileTransferAsRead(String transferId) throws RemoteException {
		//No notification type corresponds currently to mark as read
		MessagingLog.getInstance().markFileTransferAsRead(transferId);
	}

	/**
	 * Set Auto accept mode
	 * @param enable true is AA is enabled in normal conditions
	 */
	@Override
	public void setAutoAccept(boolean enable) throws RemoteException {
		RcsSettings rs = RcsSettings.getInstance();
		if (!rs.isFtAutoAcceptedModeChangeable()) {
			throw new IllegalArgumentException("Auto accept mode is not changeable");
		}
		rs.setFileTransferAutoAccepted(enable);
		if (!enable) {
			// If AA is disabled in normal conditions then it must be disabled while roaming
			rs.setFileTransferAutoAcceptedInRoaming(false);
		}
	}

	/**
	 * Set Auto accept mode in roaming
	 * @param enable true is AA is enabled in roaming
	 */
	@Override
	public void setAutoAcceptInRoaming(boolean enable) throws RemoteException {
		RcsSettings rs = RcsSettings.getInstance();
		if (!rs.isFtAutoAcceptedModeChangeable()) {
			throw new IllegalArgumentException("Auto accept mode in roaming is not changeable");
		}
		if (!rs.isFileTransferAutoAccepted()) {
			throw new IllegalArgumentException("Auto accept mode in normal conditions must be enabled");
		}
		rs.setFileTransferAutoAcceptedInRoaming(enable);
	}

	/**
	 * Set the image resize option
	 * 
	 * @param option
	 *            the image resize option (0: ALWAYS_PERFORM, 1: ONLY_ABOVE_MAX_SIZE, 2: ASK)
	 */
	@Override
	public void setImageResizeOption(int option) throws RemoteException {
		RcsSettings.getInstance().setImageResizeOption(option);
	}

}
