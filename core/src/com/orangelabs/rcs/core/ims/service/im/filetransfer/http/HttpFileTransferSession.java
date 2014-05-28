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

package com.orangelabs.rcs.core.ims.service.im.filetransfer.http;

import java.util.List;

import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceError;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingSessionListener;
import com.orangelabs.rcs.provider.fthttp.FtHttpResume;
import com.orangelabs.rcs.provider.fthttp.FtHttpResumeDaoImpl;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Abstract file transfer HTTP session
 *
 * @author jexa7410
 */
public abstract class HttpFileTransferSession extends FileSharingSession {

    /**
     * Chat session ID
     */
    private String chatSessionId = null;

    /**
     * Session state
     */
    private int sessionState;

    /**
     * Data object to access the resume FT instance in DB
     */
    protected FtHttpResume resumeFT = null;

    /**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(HttpFileTransferSession.class.getName());
    
    /**
	 * Constructor
	 *
	 * @param parent IMS service
	 * @param content Content to share
	 * @param contact Remote contact
	 * @param fileicon Content of fileicon
	 * @param chatSessionId Chat session ID
	 * @param chatContributionId Chat contribution Id
	 * @param fileTransferId File transfer Id
	 */
	public HttpFileTransferSession(ImsService parent, MmContent content, String contact, MmContent fileicon, String chatSessionID,
			String chatContributionId, String fileTransferId) {
		super(parent, content, contact, fileicon, fileTransferId);
		this.chatSessionId = chatSessionID;
		setContributionID(chatContributionId);
		this.sessionState = HttpTransferState.PENDING;
	}
	

	/**
	 * Returns the chat session ID associated to the transfer
	 * 
	 * @return the chatSessionID
	 */
	public String getChatSessionID() {
		return chatSessionId;
	}

    /**
     * Set the chatSessionId
     *
     * @param chatSessionID
     */
    public void setChatSessionID(String chatSessionID) {
       this.chatSessionId = chatSessionID;
    }

    /**
     * Create an INVITE request
     *
     * @return the INVITE request
     * @throws SipException 
     */
    public SipRequest createInvite() throws SipException {
    	// Not used here
    	return null;
    }

    @Override
    public void abortSession(int reason) {
        FtHttpResumeDaoImpl dao = FtHttpResumeDaoImpl.getInstance();

        // If reason is TERMINATION_BY_SYSTEM and session already started, then it's a pause
        if (reason == ImsServiceSession.TERMINATION_BY_SYSTEM) {
            // Check if the session is not in created status. In this status,
            // the thumbnail is not yet sent and the resume is not possible.
            if (dao != null) {
                boolean found = false;
                List<FtHttpResume> createdFileTransfers = dao.queryAll();
                for (FtHttpResume ftHttpResume : createdFileTransfers) {
                    if (ftHttpResume.getFileTransferId().equals(getFileTransferId())) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    // If the session has been terminated by system and already started, then
                    // Pause the session
                    if (logger.isActivated()) {
                        logger.info("Pause the session (session terminated, but can be resumed)");
                    }

                    // Interrupt the session
                    interruptSession();

                    // Terminate session
                    terminateSession(reason);

                    // Remove the current session
                    getImsService().removeSession(this);

                    // Notify listeners
                    for(int i=0; i < getListeners().size(); i++) {
                        ((FileSharingSessionListener)getListeners().get(i)).handleFileTransferPaused();
                    }
                    return;
                }
            }
        }
        
        // in others case, call the normal abortSession and remove session from resumable sessions
        if (dao != null) {
            dao.delete(resumeFT);
        }
        super.abortSession(reason);
    }

    /**
     * Handle error 
     * 
     * @param error Error
     */
    public void handleError(ImsServiceError error) {
        if (isSessionInterrupted()) {
            return;
        }

        // Error
        if (logger.isActivated()) {
            logger.info("Transfer error: " + error.getErrorCode() + ", reason=" + error.getMessage());
        }

        // Remove the current session
        getImsService().removeSession(this);
        this.sessionState = HttpTransferState.TERMINATED;

        // Notify listeners
        for(int j=0; j < getListeners().size(); j++) {
            ((FileSharingSessionListener)getListeners().get(j)).handleTransferError(new FileSharingError(error));
        }
    }
	
    /**
     * Prepare media session
     * 
     * @throws Exception 
     */
    public void prepareMediaSession() throws Exception {
    	// Not used here
    }

    /**
     * Start media session
     * 
     * @throws Exception 
     */
    public void startMediaSession() throws Exception {
    	// Not used here
    }

    /**
     * Close media session
     */
    public void closeMediaSession() {
    	// Not used here
    }

    /**
     * Handle file transfered. 
     * In case of file transfer over MSRP, the terminating side has received the file, 
     * but in case of file transfer over HTTP, only the content server has received the
     * file.
     */
    public void handleFileTransfered() {
        // File has been transfered
        fileTransfered();

        // Remove the current session
        getImsService().removeSession(this);
        this.sessionState = HttpTransferState.TERMINATED;

		// Notify listeners
		for (int j = 0; j < getListeners().size(); j++) {
			((FileSharingSessionListener) getListeners().get(j)).handleFileTransfered(getContent());
		}
    }
    
    /**
     * HTTP transfer progress
     * HttpTransferEventListener implementation
     *
     * @param currentSize Current transfered size in bytes
     * @param totalSize Total size in bytes
     */
    public void httpTransferProgress(long currentSize, long totalSize) {
        // Notify listeners
        for(int j=0; j < getListeners().size(); j++) {
            ((FileSharingSessionListener)getListeners().get(j)).handleTransferProgress(currentSize, totalSize);
        }
    }

    /**
     * HTTP transfer started
     * HttpTransferEventListener implementation
     */
    public void httpTransferStarted() {
        this.sessionState = HttpTransferState.ESTABLISHED;
        // Notify listeners
        for(int j=0; j < getListeners().size(); j++) {
            ((FileSharingSessionListener)getListeners().get(j)).handleSessionStarted();
        }
    }
    
    /**
     * Handle file transfer paused
     */
    public void httpTransferPaused() {
    	// Notify listeners
        for (int j = 0; j < getListeners().size(); j++) {
            ((FileSharingSessionListener) getListeners().get(j))
                    .handleFileTransferPaused();
        }
    }
    
    /**
     * Handle file transfer paused
     */
    public void httpTransferResumed() {
    	// Notify listeners
        for (int j = 0; j < getListeners().size(); j++) {
            ((FileSharingSessionListener) getListeners().get(j))
                    .handleFileTransferResumed();
        }
    }
    
    
	/**
     * Get session state
     *
     * @return State 
     * @see SessionState
     */
    public int getSessionState() {
        return sessionState;
    }
    
    /**
     * Pausing file transfer
     * Implementation should be overridden in subclasses
     */
	public void pauseFileTransfer() {
		if (logger.isActivated()){
			logger.debug("Pausing is not available");
		}
	}
	
	 /**
     * Resuming file transfer
     * Implementation should be overridden in subclasses
     */
	public void resumeFileTransfer() {
		if (logger.isActivated()){
			logger.debug("Resuming is not available");
		}
	}
}