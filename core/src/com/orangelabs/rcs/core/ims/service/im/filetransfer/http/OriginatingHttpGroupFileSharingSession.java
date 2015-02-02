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
package com.orangelabs.rcs.core.ims.service.im.filetransfer.http;

import static com.orangelabs.rcs.utils.StringUtils.UTF8;

import java.util.Set;

import com.gsma.services.rcs.chat.ParticipantInfo;
import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceError;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatUtils;
import com.orangelabs.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.orangelabs.rcs.provider.fthttp.FtHttpResumeDaoImpl;
import com.orangelabs.rcs.provider.fthttp.FtHttpResumeUpload;
import com.orangelabs.rcs.utils.IdGenerator;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Originating file transfer HTTP session
 *
 * @author vfml3370
 */
public class OriginatingHttpGroupFileSharingSession extends HttpFileTransferSession implements HttpUploadTransferEventListener {

	private final Core mCore;

    /**
     * HTTP upload manager
     */
    private HttpUploadManager uploadManager;

    /**
     * File information to send via chat
     */
    private String mFileInfo;

    /**
     * Chat session used to send file info
     */
    private ChatSession mChatSession;

    /**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(OriginatingHttpGroupFileSharingSession.class.getName());

	/**
	 * Constructor
	 *
	 * @param fileTransferId
	 *            File transfer Id
	 * @param parent
	 *            IMS service
	 * @param content
	 *            The file content to share
	 * @param fileIcon
	 *            Content of fileicon
	 * @param conferenceId
	 *            Conference ID
	 * @param participants
	 *            Set of participants
	 * @param chatSessionId
	 *            Chat session ID
	 * @param chatContributionId
	 *            Chat contribution Id
	 * @param tId
	 *            TID of the upload
	 * @param core Core
	 */
	public OriginatingHttpGroupFileSharingSession(String fileTransferId, ImsService parent,
			MmContent content, MmContent fileIcon, String conferenceId,
			Set<ParticipantInfo> participants, String chatSessionId, String chatContributionId,
			String tId, Core core) {
		super(parent, content, null, conferenceId, fileIcon, chatSessionId, chatContributionId, fileTransferId);
		mCore = core;
		mParticipants = participants;

		// Instantiate the upload manager
		uploadManager = new HttpUploadManager(getContent(), fileIcon, this, tId);
	}

	/**
	 * Background processing
	 */
	public void run() {
		try {
	    	if (logger.isActivated()) {
	    		logger.info("Initiate a new HTTP group file transfer session as originating");
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

	@Override
	public void handleError(ImsServiceError error) {
		super.handleError(error);
	}

	@Override
	public void handleFileTransfered() {
		super.handleFileTransfered();
	}

	@Override
	public void interrupt() {
		super.interrupt();
		uploadManager.interrupt();
	}

    /**
     * Send the file transfer information
     */
	private void sendFileTransferInfo() {
        String mime = CpimMessage.MIME_TYPE;
        String from = ImsModule.IMS_USER_PROFILE.getPublicAddress();
        String to = ChatUtils.ANOMYNOUS_URI;
        // Note: FileTransferId is always generated to equal the associated msgId of a FileTransfer invitation message.
        String msgId = getFileTransferId();

        String content = ChatUtils.buildCpimMessageWithImdn(from, to, msgId, mFileInfo, FileTransferHttpInfoDocument.MIME_TYPE);

		mChatSession.sendDataChunks(IdGenerator.generateMessageID(), content, mime, TypeMsrpChunk.FileSharing);
    }


    /**
     * Prepare to send the info to terminating side
     *
     * @param result byte[] which contains the result of the 200 OK from the content server
     */
	private void sendResultToContact(byte[] result) {
        if (uploadManager.isCancelled()) {
        	return;
        }
        if (result != null &&  FileTransferUtils.parseFileTransferHttpDocument(result) != null) {
        	mFileInfo = new String(result, UTF8);
            if (logger.isActivated()) {
                logger.debug(new StringBuilder("Upload done with success: ").append(mFileInfo)
                        .append(".").toString());
            }
            mChatSession = mCore.getImService().getGroupChatSession(getContributionID());
            if (mChatSession != null) {
                if (logger.isActivated()) {
                    logger.debug("Send file transfer info via an existing chat session");
                }
                sendFileTransferInfo();
                handleFileTransfered();

            } else {
                handleError(new FileSharingError(FileSharingError.NO_CHAT_SESSION));
            }
        } else {
            if (logger.isActivated()) {
                logger.debug("Upload has failed");
            }
			handleError(new FileSharingError(FileSharingError.MEDIA_UPLOAD_FAILED));
		}
	}

	/**
	 * Pausing the transfer
	 */
	@Override
	public void pauseFileTransfer() {
        fileTransferPaused();
		interruptSession();
		uploadManager.pauseTransferByUser();
	}

	/**
	 * Resuming the transfer
	 */
	@Override
	public void resumeFileTransfer() {
		new Thread(new Runnable() {
			public void run() {
				try {
					FtHttpResumeUpload upload = FtHttpResumeDaoImpl.getInstance().queryUpload(uploadManager.getTId());
					if (upload != null) {
						sendResultToContact(uploadManager.resumeUpload());
					} else {
						sendResultToContact(null);
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();
	}

	@Override
	public void uploadStarted() {
		// TODO restriction : FT HTTP upload resume is not managed
//        // Create upload entry in fthttp table
//        resumeFT = new FtHttpResumeUpload(this, uploadManager.getTid(),getThumbnail(),false);
//        FtHttpResumeDaoImpl.getInstance().insert(resumeFT);
	}

	@Override
	public boolean isInitiatedByRemote() {
		return false;
	}
}
