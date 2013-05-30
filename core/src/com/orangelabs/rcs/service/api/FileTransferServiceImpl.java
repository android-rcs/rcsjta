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
import java.util.Vector;

import org.gsma.joyn.ft.IFileTransfer;
import org.gsma.joyn.ft.FileTransferIntent;
import org.gsma.joyn.ft.IFileTransferListener;
import org.gsma.joyn.ft.IFileTransferService;
import org.gsma.joyn.ft.INewFileTransferListener;

import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;

import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.content.ContentManager;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatSession;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.platform.file.FileDescription;
import com.orangelabs.rcs.platform.file.FileFactory;
import com.orangelabs.rcs.provider.messaging.RichMessaging;
import com.orangelabs.rcs.service.api.server.ServerApiException;
import com.orangelabs.rcs.service.api.server.ServerApiUtils;
import com.orangelabs.rcs.utils.PhoneUtils;
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
		// Clear lists of sessions
		ftSessions.clear();
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
	 * Receive a new file transfer invitation
	 * 
	 * @param session File transfer session
	 */
    public void receiveFileTransferInvitation(FileSharingSession session) {
		if (logger.isActivated()) {
			logger.info("Receive file transfer invitation from " + session.getRemoteContact());
		}

		// Extract number from contact 
		String number = PhoneUtils.extractNumberFromUri(session.getRemoteContact());

		// Set the file transfer session ID from the chat session if a chat already exist
		String ftSessionId = session.getSessionID();
		String chatSessionId = ftSessionId;
		Vector<ChatSession> chatSessions = Core.getInstance().getImService().getImSessionsWith(number);
		if (chatSessions.size() > 0) {
			ChatSession chatSession = chatSessions.lastElement();
			chatSessionId = chatSession.getSessionID();
		}
		
		// Update rich messaging history
    	RichMessaging.getInstance().addIncomingFileTransfer(number, chatSessionId, ftSessionId, session.getContent());

		// Add session in the list
		FileTransferImpl sessionApi = new FileTransferImpl(session);
		FileTransferServiceImpl.addFileTransferSession(sessionApi);
    	
		// Broadcast intent related to the received invitation
    	Intent intent = new Intent(FileTransferIntent.ACTION_NEW_FILE_TRANSFER);
    	intent.putExtra(FileTransferIntent.EXTRA_CONTACT, number);
    	intent.putExtra(FileTransferIntent.EXTRA_DISPLAY_NAME, session.getRemoteDisplayName());
    	intent.putExtra(FileTransferIntent.EXTRA_TRANSFER_ID, session.getSessionID());
    	intent.putExtra(FileTransferIntent.EXTRA_FILENAME, session.getContent().getName());
    	intent.putExtra(FileTransferIntent.EXTRA_FILESIZE, session.getContent().getSize());
    	intent.putExtra(FileTransferIntent.EXTRA_FILETYPE, session.getContent().getEncoding());
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
     * Transfers a file to a contact. The parameter file contains the complete filename
     * including the path to be transfered. The parameter contact supports the following
     * formats: MSISDN in national or international format, SIP address, SIP-URI or
     * Tel-URI. If the format of the contact is not supported an exception is thrown.
     * 
     * @param contact Contact
     * @param filename Filename to transfer
     * @param listenet File transfer event listener
     * @return File transfer
     * @throws ServerApiException
     */
    public IFileTransfer transferFile(String contact, String filename, IFileTransferListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Transfer file " + filename + " to " + contact);
		}

		// Test IMS connection
		ServerApiUtils.testIms();

		try {
			// Initiate the session
			FileDescription desc = FileFactory.getFactory().getFileDescription(filename);
			MmContent content = ContentManager.createMmContentFromUrl(filename, desc.getSize());
			FileSharingSession session = Core.getInstance().getImService().initiateFileTransferSession(contact, content, false);

			// Add session in the list
			FileTransferImpl sessionApi = new FileTransferImpl(session);
			sessionApi.addEventListener(listener);

			// Start the session
			session.startSession();
						
			// Set the file transfer session ID from the chat session if a chat already exist
			String ftSessionId = session.getSessionID();
			String chatSessionId = ftSessionId;
			Vector<ChatSession> chatSessions = Core.getInstance().getImService().getImSessionsWith(contact);
			if (chatSessions.size() > 0) {
				ChatSession chatSession = chatSessions.lastElement();
				chatSessionId = chatSession.getSessionID();
			}
			
			// Update rich messaging history
			RichMessaging.getInstance().addOutgoingFileTransfer(contact, chatSessionId, ftSessionId, filename, session.getContent());

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

		// Test core availability
		ServerApiUtils.testCore();
		
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

		// Test core availability
		ServerApiUtils.testCore();
		
		// Return a session instance
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
}
