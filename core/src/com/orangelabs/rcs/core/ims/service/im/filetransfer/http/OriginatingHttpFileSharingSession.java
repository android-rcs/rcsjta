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
package com.orangelabs.rcs.core.ims.service.im.filetransfer.http;

import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import com.gsma.services.rcs.ft.FileTransfer;
import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpSession;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceError;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatUtils;
import com.orangelabs.rcs.core.ims.service.im.chat.FileTransferMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.OneOneChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.orangelabs.rcs.provider.fthttp.FtHttpResumeDaoImpl;
import com.orangelabs.rcs.provider.fthttp.FtHttpResumeUpload;
import com.orangelabs.rcs.provider.messaging.RichMessagingHistory;
import com.orangelabs.rcs.service.api.ChatImpl;
import com.orangelabs.rcs.service.api.ChatServiceImpl;
import com.orangelabs.rcs.utils.IdGenerator;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Originating file transfer HTTP session
 *
 * @author vfml3370
 */
public class OriginatingHttpFileSharingSession extends HttpFileTransferSession implements HttpUploadTransferEventListener {

    /**
     * HTTP upload manager
     */
    protected HttpUploadManager uploadManager;

	/**
     * The logger
     */
    private final static Logger logger = Logger.getLogger(OriginatingHttpFileSharingSession.class.getSimpleName());

    /**
     * fired a boolean value updated atomically to notify only once
     */
	private AtomicBoolean fired = new AtomicBoolean(false);

	/**
	 * Constructor
	 *
	 * @param parent IMS service
	 * @param content Content to be shared
	 * @param contact Remote contact
	 * @param thumbnail Thumbnail
	 * @param chatSessionId Chat session ID
	 * @param chatContributionId Chat contribution Id
	 */
	public OriginatingHttpFileSharingSession(ImsService parent, MmContent content, String contact, byte[] thumbnail, String chatSessionId, String chatContributionId) {
		super(parent, content, contact, thumbnail, chatSessionId, chatContributionId);
		if (logger.isActivated()) {
			logger.debug("OriginatingHttpFileSharingSession contact="+contact+" chatSessionId="+chatSessionId+" chatContributionId="+chatContributionId );
		}
		// Instantiate the upload manager
		uploadManager = new HttpUploadManager(getContent(), getThumbnail(), this);
	}

	/**
	 * Background processing
	 */
	public void run() {
		try {
	    	if (logger.isActivated()) {
	    		logger.info("Initiate a new HTTP file transfer session as originating");
	    	}
	    	// Upload the file to the HTTP server 
            byte[] result = uploadManager.uploadFile();
            sendResultToContact(result);
        } catch(Exception e) {
	    	if (logger.isActivated()) {
	    		logger.error("File transfer has failed", e);
	    	}
        	// Unexpected error
			handleError(new FileSharingError(FileSharingError.UNEXPECTED_EXCEPTION, e.getMessage()));
		}
	}

    protected void sendResultToContact(byte[] result){
		// Check if upload has been cancelled
        if (uploadManager.isCancelled()) {
        	return;
        }

        if ((result != null) && (ChatUtils.parseFileTransferHttpDocument(result) != null)) {
        	String fileInfo = new String(result);
            if (logger.isActivated()) {
                logger.debug("Upload done with success: " + fileInfo);
            }

			// Send the file transfer info via a chat message
            ChatSession chatSession = (ChatSession) Core.getInstance().getImService().getSession(getChatSessionID());
            if (chatSession == null) {
            	 Vector<ChatSession> chatSessions = Core.getInstance().getImService().getImSessionsWith(getRemoteContact());
            	 try {
            		 chatSession = chatSessions.lastElement();
            		 setChatSessionID(chatSession.getSessionID());
            		 setContributionID(chatSession.getContributionID());
            	 } catch(NoSuchElementException nsee) {
                     chatSession = null;
                 }
            }
            if (chatSession != null) {
				// A chat session exists
                if (logger.isActivated()) {
                    logger.debug("Send file transfer info via an existing chat session");
                }

                // Get the last chat session in progress to send file transfer info
				String mime = CpimMessage.MIME_TYPE;
				String from = ChatUtils.ANOMYNOUS_URI;
				String to = ChatUtils.ANOMYNOUS_URI;
				String msgId = IdGenerator.generateMessageID();

				// Send file info in CPIM message
				String content = ChatUtils.buildCpimMessageWithImdn(from, to, msgId, fileInfo, FileTransferHttpInfoDocument.MIME_TYPE);
				
				// Send content
				chatSession.sendDataChunks(IdGenerator.generateMessageID(), content, mime, MsrpSession.TypeMsrpChunk.HttpFileSharing);
                RichMessagingHistory.getInstance().updateFileTransferChatId(getSessionID(), chatSession.getContributionID(), msgId);
			} else {
				// A chat session should be initiated
                if (logger.isActivated()) {
                    logger.debug("Send file transfer info via a new chat session");
                }

                // Initiate a new chat session to send file transfer info in the first message, session does not need to be retrieved since it is not used
                try {
                	FileTransferMessage firstMsg = ChatUtils.createFileTransferMessage(getRemoteContact(), fileInfo, false);
					chatSession = Core.getInstance().getImService().initiateOne2OneChatSession(getRemoteContact(), firstMsg);
				} catch (CoreException e) {
					if (logger.isActivated()) {
	                    logger.debug("Couldn't initiate One to one session :"+e);
	                }
                    // Upload error
                    handleError(new FileSharingError(FileSharingError.MEDIA_UPLOAD_FAILED));
					return;
				}
                setChatSessionID(chatSession.getSessionID());
                setContributionID(chatSession.getContributionID());
                RichMessagingHistory.getInstance().updateFileTransferChatId(getSessionID(), chatSession.getContributionID(), chatSession.getFirstMessage().getMessageId());

                // Update rich messaging history
                // TODO: should be done in API server part
                //RichMessagingHistory.getInstance().addFileTransfer(getRemoteContact(), getSessionID(), FileTransfer.Direction.OUTGOING, getContent());
    			
    			// Add session in the list
				ChatImpl sessionApi = new ChatImpl(getRemoteContact(), (OneOneChatSession)chatSession);
				ChatServiceImpl.addChatSession(getRemoteContact(), sessionApi); // TODO: method is normally protected, use a callback event instead to separate layers
                // TODO : Check session response ?
			}

            // File transfered
            handleFileTransfered();
		} else {
            // Don't call handleError in case of Pause or Cancel
            if (uploadManager.isCancelled() || uploadManager.isPaused()) {
                return;
            }

            if (logger.isActivated()) {
                logger.debug("Upload has failed");
            }
            // Upload error
            handleError(new FileSharingError(FileSharingError.MEDIA_UPLOAD_FAILED));
		}
	}

	@Override
	public void handleError(ImsServiceError error) {
		super.handleError(error);
		if (fired.compareAndSet(false, true)) {
			if (resumeFT != null) {
				FtHttpResumeDaoImpl.getInstance().delete(resumeFT);
			}
		}
	}
	
	@Override
	public void handleFileTransfered() {
		super.handleFileTransfered();
		if (fired.compareAndSet(false, true)) {
			if (resumeFT != null) {
                FtHttpResumeDaoImpl.getInstance().delete(resumeFT);
			}
		}
	}
	
	/**
     * Posts an interrupt request to this Thread
     */
    @Override
    public void interrupt(){
		super.interrupt();

		// Interrupt the upload
		uploadManager.interrupt();
	}

    /**
	 * Pausing the transfer
	 */
	@Override
	public void pauseFileTransfer() {
		fileTransferPaused();
		interruptSession();
		uploadManager.pauseTransfer();
	}

	/**
	 * Resuming the transfer
	 */
	@Override
	public void resumeFileTransfer() {
		fileTransferResumed();
		new Thread(new Runnable() {
			public void run() {
				try {
					FtHttpResumeUpload upload = FtHttpResumeDaoImpl.getInstance().queryUpload(uploadManager.getTid());
					if (upload != null) {
						sendResultToContact(uploadManager.resumeUpload());
					} else {
						sendResultToContact(null);
					}
				} catch (Exception e) {
                    handleError(new FileSharingError(FileSharingError.MEDIA_UPLOAD_FAILED));
				}
			}
		}).start();
	}

	@Override
	public void uploadStarted() {
        // Create upload entry in fthttp table
	    resumeFT = new FtHttpResumeUpload(this, uploadManager.getTid(),getThumbnail(),false);
        FtHttpResumeDaoImpl.getInstance().insert(resumeFT);
	}

	public HttpUploadManager getUploadManager() {
		return uploadManager;
	}
}
