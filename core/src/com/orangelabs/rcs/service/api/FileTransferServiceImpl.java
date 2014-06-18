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
import java.util.Set;

import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

import com.gsma.services.rcs.IJoynServiceRegistrationListener;
import com.gsma.services.rcs.JoynService;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.ft.FileTransfer;
import com.gsma.services.rcs.ft.FileTransferIntent;
import com.gsma.services.rcs.ft.FileTransferServiceConfiguration;
import com.gsma.services.rcs.ft.IFileTransfer;
import com.gsma.services.rcs.ft.IFileTransferListener;
import com.gsma.services.rcs.ft.IFileTransferService;
import com.gsma.services.rcs.ft.INewFileTransferListener;
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
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * File transfer service implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileTransferServiceImpl extends IFileTransferService.Stub {
	/**
	 * List of service event listeners
	 */
	private RemoteCallbackList<IJoynServiceRegistrationListener> serviceListeners = new RemoteCallbackList<IJoynServiceRegistrationListener>();

	/**
	 * List of file transfer sessions
	 */
	private static Hashtable<String, IFileTransfer> ftSessions = new Hashtable<String, IFileTransfer>();  

	/**
	 * List of file transfer invitation listeners
	 */
	private RemoteCallbackList<INewFileTransferListener> listeners = new RemoteCallbackList<INewFileTransferListener>();

	/**
	 * The logger
	 */
	private static final Logger logger = Logger.getLogger(FileTransferServiceImpl.class.getName());

	/**
	 * Lock used for synchronization
	 */
	private Object lock = new Object();

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
	public static void addFileTransferSession(FileTransferImpl session) {
		if (logger.isActivated()) {
			logger.debug("Add a file transfer session in the list (size=" + ftSessions.size() + ")");
		}
		
		ftSessions.put(session.getTransferId(), session);
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
	 * Receive a new file transfer invitation
	 * 
	 * @param session File transfer session
	 * @param isGroup is group file transfer
	 */
    public void receiveFileTransferInvitation(FileSharingSession session, boolean isGroup) {
		if (logger.isActivated()) {
			logger.info("Receive FT invitation from " + session.getRemoteContact() + " file=" + session.getContent().getName()
					+ " size=" + session.getContent().getSize());
		}

		// Extract number from contact 
		String number = PhoneUtils.extractNumberFromUri(session.getRemoteContact());

		// Update rich messaging history
		if (isGroup) {
			MessagingLog.getInstance().addIncomingGroupFileTransfer(session.getContributionID(),
					number, session.getFileTransferId(), session.getContent(), session.getFileicon());
		} else {
			MessagingLog.getInstance().addFileTransfer(number, session.getFileTransferId(),
					FileTransfer.Direction.INCOMING, session.getContent(), session.getFileicon());
		}

		// Add session in the list
		FileTransferImpl sessionApi = new FileTransferImpl(session);
		FileTransferServiceImpl.addFileTransferSession(sessionApi);
    	
		// Broadcast intent related to the received invitation
    	Intent intent = new Intent(FileTransferIntent.ACTION_NEW_INVITATION);
    	intent.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
    	intent.putExtra(FileTransferIntent.EXTRA_CONTACT, number);
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
    	
    	// Notify file transfer invitation listeners
    	synchronized(lock) {
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).onNewFileTransfer(session.getFileTransferId());
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
	 * Receive a new HTTP file transfer invitation outside of an existing chat session
	 *
	 * @param session File transfer session
	 */
	public void receiveFileTransferInvitation(FileSharingSession session, ChatSession chatSession) {
		// Display invitation
		receiveFileTransferInvitation(session, chatSession.isGroupChat());
		
		// Update rich messaging history
		if (chatSession.isGroupChat()) {
			MessagingLog.getInstance().updateFileTransferChatId(
					chatSession.getFirstMessage().getMessageId(), chatSession.getContributionID());
		}

		// Add session in the list
		FileTransferImpl sessionApi = new FileTransferImpl(session);
		addFileTransferSession(sessionApi);
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
	 * @param listenet
	 *            File transfer event listener
	 * @return File transfer
	 * @throws ServerApiException
	 */
    public IFileTransfer transferFile(String contact, Uri file, boolean fileicon, IFileTransferListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Transfer file " + file + " to " + contact + " (fileicon=" + fileicon + ")");
		}

		// Test IMS connection
		ServerApiUtils.testIms();

		try {
			// Initiate the session
			FileDescription fileDescription = FileFactory.getFactory().getFileDescription(file);
			MmContent content = ContentManager.createMmContent(file, fileDescription.getSize(), fileDescription.getName());
			final FileSharingSession session = Core.getInstance().getImService().initiateFileTransferSession(contact, content, fileicon);

			// Add session listener
			FileTransferImpl sessionApi = new FileTransferImpl(session);
			sessionApi.addEventListener(listener);

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
			addFileTransferSession(sessionApi);
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
     * Transfers a file to participants. The parameter file contains the URI of the
     * file to be transferred (for a local or a remote file).
	 *
	 * @param chatId ChatId of group chat
	 * @param file
	 *            Uri of file to transfer
	 * @param fileicon
	 *            true if the stack must try to attach fileicon
	 * @param listener
	 *            File transfer event listener
	 * @return File transfer
	 * @throws ServerApiException
	 */
	public IFileTransfer transferFileToGroupChat(String chatId, Uri file, boolean fileicon,
			IFileTransferListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("sendFile (file=" + file + ") (fileicon=" + fileicon + ")");
		}
		try {
			// Initiate the session
			FileDescription fileDescription = FileFactory.getFactory().getFileDescription(file);
			MmContent content = ContentManager.createMmContent(file, fileDescription.getSize(),
					fileDescription.getName());

			Set<ParticipantInfo> participants = MessagingLog.getInstance()
					.getGroupChatConnectedParticipants(chatId);
			final FileSharingSession session = Core
					.getInstance()
					.getImService()
					.initiateGroupFileTransferSession(participants, content, fileicon,
							chatId);

			// Add session listener
			FileTransferImpl sessionApi = new FileTransferImpl(session);
			sessionApi.addEventListener(listener);

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
			FileTransferServiceImpl.addFileTransferSession(sessionApi);
			return sessionApi;

		} catch (Exception e) {
			// TODO:Handle Security exception in CR026
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
	 * Registers a file transfer invitation listener
	 * 
	 * @param listener New file transfer listener
	 * @throws ServerApiException
	 */
	public void addNewFileTransferListener(INewFileTransferListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Add a file transfer invitation listener");
		}
		
		listeners.register(listener);
	}

	/**
	 * Unregisters a file transfer invitation listener
	 * 
	 * @param listener New file transfer listener
	 * @throws ServerApiException
	 */
	public void removeNewFileTransferListener(INewFileTransferListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Remove a file transfer invitation listener");
		}
		
		listeners.unregister(listener);
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
    public void handleFileDeliveryStatus(String fileTransferId, String status, String contact) {
        if (status.equalsIgnoreCase(ImdnDocument.DELIVERY_STATUS_DELIVERED)) {
			// Update rich messaging history
			MessagingLog.getInstance().updateFileTransferStatus(fileTransferId, FileTransfer.State.DELIVERED);

			// Notify File transfer delivery listeners
            final int N = listeners.beginBroadcast();
            for (int i=0; i < N; i++) {
                try {
                    listeners.getBroadcastItem(i).onReportFileDelivered(fileTransferId);
                } catch(Exception e) {
                    if (logger.isActivated()) {
                        logger.error("Can't notify listener", e);
                    }
                }
            }
            listeners.finishBroadcast();
        } else
        if (status.equalsIgnoreCase(ImdnDocument.DELIVERY_STATUS_DISPLAYED)) {
			// Update rich messaging history
			MessagingLog.getInstance().updateFileTransferStatus(fileTransferId, FileTransfer.State.DISPLAYED);

			// Notify File transfer delivery listeners
            final int N = listeners.beginBroadcast();
            for (int i=0; i < N; i++) {
                try {
                    listeners.getBroadcastItem(i).onReportFileDisplayed(fileTransferId);
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
     * Group File Transfer delivery status delivered
     *
     * @param fileTransferId File transfer Id
     * @param contact contact who received file
     */
	private void handleGroupFileDeliveryStatusDelivered(String fileTransferId, String contact) {
		// Update rich messaging history
		MessagingLog messagingLog = MessagingLog.getInstance();
		messagingLog.updateGroupChatDeliveryInfoStatus(fileTransferId,
				ImdnDocument.DELIVERY_STATUS_DELIVERED, contact);
		// TODO : Listeners to notify group file delivery status for
		// individual contacts will be implemented as part of CR011. For now,
		// the same callback is used for sending both per contact group delivery
		// status and for the whole group message delivery status.
		// Notify File transfer delivery listeners
		final int N = listeners.beginBroadcast();
		for (int i = 0; i < N; i++) {
			try {
				listeners.getBroadcastItem(i).onReportFileDelivered(fileTransferId);
			} catch (Exception e) {
				if (logger.isActivated()) {
					logger.error("Can't notify listener per contact", e);
				}
			}
		}
		listeners.finishBroadcast();
		if (messagingLog.isDeliveredToAllRecipients(fileTransferId)) {
			messagingLog.updateFileTransferStatus(fileTransferId,
					FileTransfer.State.DELIVERED);
			// Notify File transfer delivery listeners
			final int P = listeners.beginBroadcast();
			for (int i = 0; i < P; i++) {
				try {
					listeners.getBroadcastItem(i).onReportFileDelivered(fileTransferId);
				} catch (Exception e) {
					if (logger.isActivated()) {
						logger.error("Can't notify listener", e);
					}
				}
			}
			listeners.finishBroadcast();
		}
	}

    /**
     * Group File Transfer delivery status displayed
     *
     * @param fileTransferId File transfer Id
     * @param contact contact who received file
     */
	private void handleGroupFileDeliveryStatusDisplayed(String fileTransferId, String contact) {
		// Update rich messaging history
		MessagingLog messagingLog = MessagingLog.getInstance();
		messagingLog.updateGroupChatDeliveryInfoStatus(fileTransferId,
				ImdnDocument.DELIVERY_STATUS_DISPLAYED, contact);
		// TODO : Listeners to notify group file delivery status for
		// individual contacts will be implemented as part of CR011. For now,
		// the same callback is used for sending both per contact group delivery
		// status and for the whole group message delivery status.
		// Notify File transfer delivery listeners
		final int N = listeners.beginBroadcast();
		for (int i = 0; i < N; i++) {
			try {
				listeners.getBroadcastItem(i).onReportFileDisplayed(fileTransferId);
			} catch (Exception e) {
				if (logger.isActivated()) {
					logger.error("Can't notify listener per contact", e);
				}
			}
		}
		listeners.finishBroadcast();
		if (messagingLog.isDisplayedByAllRecipients(fileTransferId)) {
			messagingLog.updateFileTransferStatus(fileTransferId,
					FileTransfer.State.DISPLAYED);

			final int P = listeners.beginBroadcast();
			for (int i = 0; i < P; i++) {
				try {
					listeners.getBroadcastItem(i).onReportFileDisplayed(fileTransferId);
				} catch (Exception e) {
					if (logger.isActivated()) {
						logger.error("Can't notify listener", e);
					}
				}
			}
			listeners.finishBroadcast();
		}
	}

    /**
     * Group File Transfer delivery status.
     *
     * @param fileTransferId File transfer Id
     * @param status status of File transfer
     * @param contact contact who received file
     */
	public void handleGroupFileDeliveryStatus(String fileTransferId, String status, String contact) {
		if (status.equalsIgnoreCase(ImdnDocument.DELIVERY_STATUS_DELIVERED)) {
			handleGroupFileDeliveryStatusDelivered(fileTransferId, contact);
		} else if (status.equalsIgnoreCase(ImdnDocument.DELIVERY_STATUS_DISPLAYED)) {
			handleGroupFileDeliveryStatusDisplayed(fileTransferId, contact);
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
		// Extract number from contact
		String number = PhoneUtils.extractNumberFromUri(session.getRemoteContact());

		// Add session in the list
		FileTransferImpl sessionApi = new FileTransferImpl(session);
		FileTransferServiceImpl.addFileTransferSession(sessionApi);

		// Broadcast intent related to the received invitation
		Intent intent = new Intent(FileTransferIntent.ACTION_RESUME);
		intent.putExtra(FileTransferIntent.EXTRA_CONTACT, number);
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
        // TODO FUSION remove chatSessionId
        
        // Extract number from contact 
        String number = PhoneUtils.extractNumberFromUri(session.getRemoteContact());

		// Add session in the list
		FileTransferImpl sessionApi = new FileTransferImpl(session);
		FileTransferServiceImpl.addFileTransferSession(sessionApi);

        // Broadcast intent, we reuse the File transfer invitation intent
        Intent intent = new Intent(FileTransferIntent.ACTION_RESUME);
    	intent.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);

        intent.putExtra(FileTransferIntent.EXTRA_CONTACT, number);
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
