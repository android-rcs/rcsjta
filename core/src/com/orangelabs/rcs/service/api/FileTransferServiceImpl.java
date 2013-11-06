/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
 ******************************************************************************/
package com.orangelabs.rcs.service.api;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.gsma.joyn.IJoynServiceRegistrationListener;
import org.gsma.joyn.JoynService;
import org.gsma.joyn.ft.FileTransfer;
import org.gsma.joyn.ft.FileTransferIntent;
import org.gsma.joyn.ft.FileTransferServiceConfiguration;
import org.gsma.joyn.ft.IFileTransfer;
import org.gsma.joyn.ft.IFileTransferListener;
import org.gsma.joyn.ft.IFileTransferService;
import org.gsma.joyn.ft.INewFileTransferListener;

import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.content.ContentManager;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.platform.file.FileDescription;
import com.orangelabs.rcs.platform.file.FileFactory;
import com.orangelabs.rcs.provider.messaging.RichMessagingHistory;
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
	private static Logger logger = Logger.getLogger(FileTransferServiceImpl.class.getName());

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
	protected static void addFileTransferSession(FileTransferImpl session) {
		if (logger.isActivated()) {
			logger.debug("Add a file transfer session in the list (size=" + ftSessions.size() + ")");
		}
		
		ftSessions.put(session.getTransferId(), session);
	}

	/**
	 * Remove a file transfer session from the list
	 * 
	 * @param sessionId Session ID
	 */
	protected static void removeFileTransferSession(String sessionId) {
		if (logger.isActivated()) {
			logger.debug("Remove a file transfer session from the list (size=" + ftSessions.size() + ")");
		}
		
		ftSessions.remove(sessionId);
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
			logger.info("Receive file transfer invitation from " + session.getRemoteContact());
		}

		// Extract number from contact 
		String number = PhoneUtils.extractNumberFromUri(session.getRemoteContact());

		// Update rich messaging history
    	RichMessagingHistory.getInstance().addFileTransfer(number, session.getSessionID(), FileTransfer.Direction.INCOMING, session.getContent(), FileTransfer.State.INVITED);

		// Add session in the list
		FileTransferImpl sessionApi = new FileTransferImpl(session);
		FileTransferServiceImpl.addFileTransferSession(sessionApi);
    	
		// Broadcast intent related to the received invitation
    	Intent intent = new Intent(FileTransferIntent.ACTION_NEW_INVITATION);
    	intent.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
    	intent.putExtra(FileTransferIntent.EXTRA_CONTACT, number);
    	intent.putExtra(FileTransferIntent.EXTRA_DISPLAY_NAME, session.getRemoteDisplayName());
    	intent.putExtra(FileTransferIntent.EXTRA_TRANSFER_ID, session.getSessionID());
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
	            	listeners.getBroadcastItem(i).onNewFileTransfer(session.getSessionID());
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
/* TODO		receiveFileTransferInvitation(session, chatSession.isGroupChat());
		
		// Update rich messaging history
		RichMessaging.getInstance().addIncomingChatSessionByFtHttp(chatSession);
		
		// Add session in the list
		ImSession sessionApi = new ImSession(chatSession);
		MessagingApiService.addChatSession(sessionApi); */
	}    
	
    /**
     * Returns the configuration of the file transfer service
     * 
     * @return Configuration
     */
    public FileTransferServiceConfiguration getConfiguration() {
    	return new FileTransferServiceConfiguration(
    			RcsSettings.getInstance().getWarningMaxFileTransferSize(),
    			RcsSettings.getInstance().getMaxFileTransferSize(),
    			RcsSettings.getInstance().isFileTransferAutoAccepted(),
    			RcsSettings.getInstance().isFileTransferThumbnailSupported(),
    			RcsSettings.getInstance().getMaxFileIconSize());
    }    
    
    /**
     * Transfers a file to a contact. The parameter file contains the complete filename
     * including the path to be transferred. The parameter contact supports the following
     * formats: MSISDN in national or international format, SIP address, SIP-URI or
     * Tel-URI. If the format of the contact is not supported an exception is thrown.
     * 
     * @param contact Contact
     * @param filename Filename to transfer
     * @param fileicon Filename of the file icon associated to the file to be transfered
     * @param listenet File transfer event listener
     * @return File transfer
     * @throws ServerApiException
     */
    public IFileTransfer transferFile(String contact, String filename, String fileicon, IFileTransferListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Transfer file " + filename + " to " + contact);
		}

		// Test IMS connection
		ServerApiUtils.testIms();

		try {
			// Initiate the session
			FileDescription desc = FileFactory.getFactory().getFileDescription(filename);
			MmContent content = ContentManager.createMmContentFromUrl(filename, desc.getSize());
			FileSharingSession session = Core.getInstance().getImService().initiateFileTransferSession(contact, content, fileicon, null, null); // TODO

			// Add session listener
			FileTransferImpl sessionApi = new FileTransferImpl(session);
			sessionApi.addEventListener(listener);

			// Update rich messaging history
			RichMessagingHistory.getInstance().addFileTransfer(contact, session.getSessionID(), FileTransfer.Direction.OUTGOING, session.getContent(), FileTransfer.State.INITIATED);

			// Start the session
			session.startSession();
						
			// Add session in the list
			addFileTransferSession(sessionApi);
			return sessionApi;
		} catch(Exception e) {
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
     * @param ftSessionId File transfer session Id
     * @param status status of File transfer
     */
    public void handleFileDeliveryStatus(String ftSessionId, String status) {
        if (status.equalsIgnoreCase(ImdnDocument.DELIVERY_STATUS_DELIVERED)) {
            // Update rich messaging history
        	// TODO RichMessagingHistory.getInstance().updateFileTransferStatus(ftSessionId, FileTransfer.State.DELIVERED);
            
            // Notify File transfer delivery listeners
            final int N = listeners.beginBroadcast();
            for (int i=0; i < N; i++) {
                try {
                    listeners.getBroadcastItem(i).onReportFileDelivered(ftSessionId);
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
        	// TODO RichMessagingHistory.getInstance().updateFileTransferStatus(ftSessionId, FileTransfer.State.DISPLAYED);
            
            // Notify File transfer delivery listeners
            final int N = listeners.beginBroadcast();
            for (int i=0; i < N; i++) {
                try {
                    listeners.getBroadcastItem(i).onReportFileDisplayed(ftSessionId);
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
	 * Returns service version.
	 */
	@Override
	public int getServiceVersion() throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Service Version:" + JoynService.Build.GSMA_VERSION);
		}
		return JoynService.Build.GSMA_VERSION;
	}
}
