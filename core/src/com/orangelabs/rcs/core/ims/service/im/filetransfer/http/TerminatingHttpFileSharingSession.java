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

import javax2.sip.header.ContactHeader;

import android.net.Uri;

import java.util.Vector;

import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.content.ContentManager;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceError;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.ImsSessionListener;
import com.orangelabs.rcs.core.ims.service.im.InstantMessagingService;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingSessionListener;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.orangelabs.rcs.provider.fthttp.FtHttpResumeDownload;
import com.orangelabs.rcs.provider.messaging.MessagingLog;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Terminating file transfer HTTP session
 * 
 * @author vfml3370
 */
public class TerminatingHttpFileSharingSession extends HttpFileTransferSession implements HttpTransferEventListener {

	/**
	 * HTTP download manager
	 */
	protected HttpDownloadManager downloadManager;

	/**
	 * Remote instance Id
	 */
	private String remoteInstanceId;

	/**
	 * The logger
	 */
	private final static Logger logger = Logger.getLogger(TerminatingHttpFileSharingSession.class.getSimpleName());

	/**
	 * Is File Transfer initiated from a GC
	 */
	protected boolean isGroup = false;
		
	/**
	 * Constructor
	 * 
	 * @param parent
	 *            IMS service
	 * @param chatSession
	 *            the chat session
	 * @param fileTransferInfo
	 *            the File transfer info document
	 * @param fileTransferId
	 *            the File transfer Id
	 * @param contact
	 *            the remote contact Id
	 * @param displayName
	 *            the display name of the remote contact
	 */
	public TerminatingHttpFileSharingSession(ImsService parent, ChatSession chatSession,
			FileTransferHttpInfoDocument fileTransferInfo, String fileTransferId, ContactId contact, String displayName) {
		super(parent, ContentManager.createMmContent(
				ContentManager.generateUriForReceivedContent(fileTransferInfo.getFilename(), fileTransferInfo.getFileType()),
				fileTransferInfo.getFileSize(), fileTransferInfo.getFilename()), contact, PhoneUtils.formatContactIdToUri(contact),
				null, chatSession.getSessionID(), chatSession.getContributionID(), fileTransferId);

		setRemoteDisplayName(displayName);
        // Build a new dialogPath with this of chatSession and an empty CallId
		setDialogPath(new SipDialogPath(chatSession.getDialogPath()));
		getDialogPath().setCallId("");

		ContactHeader inviteContactHeader = (ContactHeader) chatSession.getDialogPath().getInvite().getHeader(ContactHeader.NAME);
		if (inviteContactHeader != null) {
			this.remoteInstanceId = inviteContactHeader.getParameter(SipUtils.SIP_INSTANCE_PARAM);
		}
		isGroup = chatSession.isGroupChat();
		
		// Instantiate the download manager
		MmContent content = getContent();
		downloadManager = new HttpDownloadManager(content, this, fileTransferInfo.getFileUri());

		// Download thumbnail
		if (fileTransferInfo.getFileThumbnail() != null) {
			FileTransferHttpThumbnail thumbnailInfo = fileTransferInfo.getFileThumbnail();
			String iconName = FileTransferUtils.buildFileiconUrl(fileTransferId,thumbnailInfo.getThumbnailType());
			setFileicon(downloadManager.downloadThumbnail(thumbnailInfo, iconName));
		}

		if (shouldBeAutoAccepted()) {
			setSessionAccepted();
		}
	}

	/**
	 * Constructor
	 * 
	 * @param parent
	 *            IMS service
	 * @param content
	 *            the content to be transferred
	 * @param resume
	 *            the Data Object to access FT HTTP table in DB
	 */
	public TerminatingHttpFileSharingSession(ImsService parent, MmContent content, FtHttpResumeDownload resume) {
		super(parent, content, resume.getContact(), PhoneUtils.formatContactIdToUri(resume.getContact()),
				resume.getFileicon() != null ? FileTransferUtils.createMmContent(resume
						.getFileicon()) : null, null, resume.getChatId(),
				resume.getFileTransferId());
		this.isGroup = resume.isGroup();
		this.resumeFT = resume;
		// Instantiate the download manager
		downloadManager = new HttpDownloadManager(getContent(), this, resume.getDownloadServerAddress());

		if (shouldBeAutoAccepted()) {
			setSessionAccepted();
		}
	}

	/**
	 * Check is session should be auto accepted depending on settings and
	 * roaming conditions This method should only be called once per session
	 *
	 * @return true if file transfer should be auto accepted
	 */
	private boolean shouldBeAutoAccepted() {
		if (getImsService().getImsModule().isInRoaming()) {
			return RcsSettings.getInstance().isFileTransferAutoAcceptedInRoaming();
		}

		return RcsSettings.getInstance().isFileTransferAutoAccepted();
	}

	/**
	 * Posts an interrupt request to this Thread
	 */
	@Override
	public void interrupt() {
		super.interrupt();

		// Interrupt the download
		downloadManager.interrupt();
	}

	/**
	 * Background processing
	 */
	public void run() {
		try {
			if (logger.isActivated()) {
				logger.info("Initiate a new HTTP file transfer session as terminating");
			}

			Vector<ImsSessionListener> listeners = getListeners();
			/* Check if session should be auto-accepted once */
			if (isSessionAccepted()) {
				if (logger.isActivated()) {
					logger.debug("Received http file transfer invitation marked for auto-accept");
				}

				for (ImsSessionListener listener : listeners) {
					((FileSharingSessionListener)listener).handleSessionAutoAccepted();
				}
			} else {
				if (logger.isActivated()) {
					logger.debug("Received http file transfer invitation marked for manual accept");
				}

				for (ImsSessionListener listener : listeners) {
					listener.handleSessionInvited();
				}

				int answer = waitInvitationAnswer();
				switch (answer) {
					case ImsServiceSession.INVITATION_REJECTED:
						if (logger.isActivated()) {
							logger.debug("Transfer has been rejected by user");
						}

						getImsService().removeSession(this);

						for (ImsSessionListener listener : listeners) {
							listener.handleSessionRejectedByUser();
						}
						return;

					case ImsServiceSession.INVITATION_NOT_ANSWERED:
						if (logger.isActivated()) {
							logger.debug("Transfer has been rejected on timeout");
						}

						getImsService().removeSession(this);

						for (ImsSessionListener listener : listeners) {
							listener.handleSessionRejectedByTimeout();
						}
						return;

					case ImsServiceSession.INVITATION_CANCELED:
						if (logger.isActivated()) {
							logger.debug("Http transfer has been rejected by remote.");

							getImsService().removeSession(this);

							for (ImsSessionListener listener : listeners) {
								listener.handleSessionRejectedByRemote();
							}
						}
						return;

					case ImsServiceSession.INVITATION_ACCEPTED:
						setSessionAccepted();

						for (ImsSessionListener listener : listeners) {
							((FileSharingSessionListener)listener).handleSessionAccepted();
						}
						break;

					default:
						if (logger.isActivated()) {
							logger.debug("Unknown invitation answer in run; answer="
									.concat(String.valueOf(answer)));
						}
						return;
				}
			}

            // Reject if file is too big or size exceeds device storage capacity. This control should be done
            // on UI. It is done after end user accepts invitation to enable prior handling by the application.
            FileSharingError error = isFileCapacityAcceptable(getContent().getSize());
            if (error != null) {
                // Invitation cannot be declined in MSRP or SIP at this level
            	
                // Close session and notify listeners
                handleError(error);
                return;
            }

			// Notify listeners
			for (int j = 0; j < getListeners().size(); j++) {
				getListeners().get(j).handleSessionStarted();
			}
			Uri file = downloadManager.getDownloadedFileUri();
			Uri downloadServerAddress = downloadManager.getHttpServerAddr();

			MessagingLog.getInstance().setFileDownloadAddress(getFileTransferId(), downloadServerAddress);
			// Download file from the HTTP server
			if (downloadManager.downloadFile()) {
				if (logger.isActivated()) {
					logger.debug("Download file with success");
				}

				// Set filename
				getContent().setUri(file);

				// File transfered
				handleFileTransfered();

				// Send delivery report "displayed"
				// According to BB PDD section 6.1.4 there should be no display for GC messages.
				if (!isGroup) {
					sendDeliveryReport(ImdnDocument.DELIVERY_STATUS_DISPLAYED);
				}
			} else {
                // Don't call handleError in case of Pause or Cancel
				if (downloadManager.isCancelled() || downloadManager.isPaused()) {
					return;
				}

                // Upload error
				if (logger.isActivated()) {
					logger.info("Download file has failed");
				}
				handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED));
			}
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Transfer has failed", e);
			}

			// Unexpected error
			handleError(new FileSharingError(FileSharingError.UNEXPECTED_EXCEPTION, e.getMessage()));
		}
	}

	// If session is rejected by user, session cannot be rejected at SIP level (already accepted 200OK).
	@Override
	public void rejectSession(int code) {
		if (logger.isActivated()) {
			logger.debug("Session invitation has been rejected");
		}
		invitationStatus = INVITATION_REJECTED;

		// Unblock semaphore
		synchronized(waitUserAnswer) {
			waitUserAnswer.notifyAll();
		}
			
		// Remove the session in the session manager
		getImsService().removeSession(this);
	}

	@Override
	public void handleError(ImsServiceError error) {
		super.handleError(error);
	}

	@Override
	public void handleFileTransfered() {
		super.handleFileTransfered();
	}

	/**
	 * Send delivery report
	 * 
	 * @param status
	 *            Report status
	 */
	protected void sendDeliveryReport(String status) {
		String msgId = getFileTransferId();
		if (msgId != null) {
			if (logger.isActivated()) {
				logger.debug("Send delivery report " + status);
			}

			ChatSession chatSession = (ChatSession) Core.getInstance().getImService().getSession(getChatSessionID());
			if (chatSession != null && chatSession.isMediaEstablished()) {
				// Send message delivery status via a MSRP
				chatSession.sendMsrpMessageDeliveryStatus(getRemoteContact(),msgId, status);
			} else {
				// Send message delivery status via a SIP MESSAGE
				((InstantMessagingService) getImsService()).getImdnManager().sendMessageDeliveryStatusImmediately(
						getRemoteContact(), msgId, status, remoteInstanceId);
			}
		}
	}

	/**
	 * Resume File Transfer
	 */
	@Override
	public void resumeFileTransfer() {
		fileTransferResumed();
		downloadManager.getListener().httpTransferResumed();

		new Thread(new Runnable() {
			public void run() {
				// Download file from the HTTP server
				if (downloadManager.resumeDownload()) {
					if (logger.isActivated()) {
						logger.debug("Download file with success");
					}

					// Set filename
					getContent().setUri(downloadManager.getDownloadedFileUri());

					// File transfered
					handleFileTransfered();

					// Send delivery report "displayed"
					sendDeliveryReport(ImdnDocument.DELIVERY_STATUS_DISPLAYED);
				} else {
                    // Don't call handleError in case of Pause or Cancel
                    if (downloadManager.isCancelled() || downloadManager.isPaused()) {
                        return;
                    }

                    // Upload error
                    if (logger.isActivated()) {
                        logger.info("Download file has failed");
                    }
                    handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED));
				}
			}
		}).start();
	}

	/**
	 * Pause File Transfer
	 */
	@Override
	public void pauseFileTransfer() {
		fileTransferPaused();
		interruptSession();
		downloadManager.pauseTransferByUser();
	}

	@Override
	public boolean isInitiatedByRemote() {
		return true;
	}


}
