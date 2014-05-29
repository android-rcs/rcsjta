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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.gsma.services.rcs.ft.FileTransfer;
import com.orangelabs.rcs.core.content.ContentManager;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.service.im.InstantMessagingService;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingSessionListener;
import com.orangelabs.rcs.provider.fthttp.FtHttpResume;
import com.orangelabs.rcs.provider.fthttp.FtHttpResumeDaoImpl;
import com.orangelabs.rcs.provider.fthttp.FtHttpResumeDownload;
import com.orangelabs.rcs.provider.fthttp.FtHttpResumeUpload;
import com.orangelabs.rcs.provider.messaging.MessagingLog;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * File Transfer HTTP resume manager
 */
public class FtHttpResumeManager {
	/**
	 * Interface to get access to the FtHttp table
	 */
	private FtHttpResumeDaoImpl dao = FtHttpResumeDaoImpl.getInstance();

	/**
	 * IMS service
	 */
	private InstantMessagingService imsService;

	/**
	 * List of pending sessions to resume
	 */
	private LinkedList<FtHttpResume> listOfFtHttpResume;

	/**
	 * FT HTTP session being resumed
	 */
	private FtHttpResume ftHttpResume;

	/**
	 * The logger
	 */
	private static final Logger logger = Logger.getLogger(FtHttpResumeManager.class.getSimpleName());

	private boolean terminate = false; // TODO

	/**
	 * Constructor
	 * 
	 * @param imsService
	 *            IMS service
	 */
	public FtHttpResumeManager(InstantMessagingService instantMessagingService) {
		if (dao == null) {
			if (logger.isActivated()) {
				logger.error("Cannot resume FT");
			}
			return;
		}
		imsService = instantMessagingService;
		try {
			// Retrieve all resumable sessions
			List<FtHttpResume> listFile2resume = dao.queryAll();
			if (listFile2resume.isEmpty() == false) {
				// Rich Messaging - set all "in progress" File transfer to "paused".
				// This is necessary in case of the application can't update the
				// status before device switch off.
				for (FtHttpResume ftHttpResume : listFile2resume) {
					MessagingLog.getInstance().updateFileTransferStatus(ftHttpResume.getFileTransferId(),
							FileTransfer.State.PAUSED);
				}
				listOfFtHttpResume = new LinkedList<FtHttpResume>(listFile2resume);
				processNext();
			}
		} catch (Exception e) {
			// handle exception
			if (logger.isActivated()) {
				logger.error("Exception occurred", e);
			}
		}
	}

	/**
	 * resume next pending session
	 */
	private void processNext() {
		if (listOfFtHttpResume.isEmpty())
			return;
		// Remove the oldest session from the list
		ftHttpResume = listOfFtHttpResume.poll();
		if (logger.isActivated()) {
			logger.debug("Resume FT HTTP " + ftHttpResume);
		}
		switch (ftHttpResume.getDirection()) {
		case INCOMING:
			FtHttpResumeDownload downloadInfo = (FtHttpResumeDownload) ftHttpResume;
			MmContent downloadContent = ContentManager.createMmContentFromMime(downloadInfo.getUrl(), downloadInfo.getMimetype(),
					downloadInfo.getSize());
			// Creates the Resume Download session object
			final ResumeDownloadFileSharingSession resumeDownload = new ResumeDownloadFileSharingSession(
                    imsService, downloadContent, downloadInfo);
			resumeDownload.addListener(getFileSharingSessionListener());
			// Start the download HTTP FT session object
			new Thread() {
				public void run() {
					resumeDownload.startSession();
				}
			}.start();
			// Notify the UI and update rich messaging
			imsService
					.getImsModule()
					.getCore()
					.getListener()
					.handleIncomingFileTransferResuming(resumeDownload, resumeDownload.isGroup, resumeDownload.getChatSessionID(),
							resumeDownload.getContributionID());
			break;
		case OUTGOING:
		    // TODO : only managed for 1-1 FToHTTP
            FtHttpResumeUpload uploadInfo = (FtHttpResumeUpload) ftHttpResume;
            if (!ftHttpResume.isGroup()) {
                // Get upload content
                MmContent uploadContent = ContentManager.createMmContentFromMime(uploadInfo.getFilepath(),
                        uploadInfo.getMimetype(), uploadInfo.getSize());

                // Create Resume Upload session 
                final ResumeUploadFileSharingSession resumeUpload = new ResumeUploadFileSharingSession(imsService, uploadContent,
                        uploadInfo);
                resumeUpload.addListener(getFileSharingSessionListener());

                // Start Resume Upload session
                new Thread() {
                    public void run() {
                        resumeUpload.startSession();
                    }
                }.start();

                // Notify the UI and update rich messaging
                imsService.getImsModule().getCore().getListener()
                        .handleOutgoingFileTransferResuming(resumeUpload, false);
            }
            break;
		}

	}

	/**
	 * Create an event listener to handle end of session
	 * 
	 * @return the File sharing event listener
	 */
	private FileSharingSessionListener getFileSharingSessionListener() {
		return new FileSharingSessionListener() {
			AtomicBoolean fired = new AtomicBoolean(false);

			@Override
			public void handleSessionTerminatedByRemote() {
				if (fired.compareAndSet(false, true)) {
					processNext();
				}
			}

			@Override
			public void handleSessionStarted() {
			}

			@Override
			public void handleSessionAborted(int reason) {
				if (fired.compareAndSet(false, true)) {
					processNext();
				}
			}

			@Override
			public void handleTransferProgress(long currentSize, long totalSize) {
			}

			@Override
			public void handleTransferError(FileSharingError error) {
				if (fired.compareAndSet(false, true)) {
					processNext();
				}
			}

			@Override
			public void handleFileTransfered(String filename) {
				if (fired.compareAndSet(false, true)) {
					processNext();
				}
			}

			@Override
			public void handleFileTransferResumed() {
			}

			@Override
			public void handleFileTransferPaused() {
			}
		};
	}

	public void terminate() {
		this.terminate = true;
	}

}
