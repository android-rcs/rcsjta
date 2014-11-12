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
package com.orangelabs.rcs.core.ims.service.upload;

import java.util.UUID;

import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.HttpUploadManager;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.HttpUploadTransferEventListener;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * File upload session
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileUploadSession extends Thread implements HttpUploadTransferEventListener {

	private final static int UPLOAD_ERROR_UNSPECIFIED = -1;

	/**
	 * Upload ID
	 */
	private String uploadId;
	
    /**
     * File
     */
    private MmContent file;

    /**
     * File icon
     */
    private boolean fileIcon = false;

    /**
     * HTTP upload manager
     */
    protected HttpUploadManager uploadManager;
    
    /**
     * Upload listener
     */
    private FileUploadSessionListener listener = null;
    
    /**
     * File info
     */
    private FileTransferHttpInfoDocument fileInfoDoc = null;
    
    /**
     * The logger
     */
    private final static Logger logger = Logger.getLogger(FileUploadSession.class.getSimpleName());
    
	/**
	 * Constructor
	 * 
	 * @param content Content of file to upload
	 * @param fileIcon True if the stack must try to attach file icon
	 */
	public FileUploadSession(MmContent file, boolean fileIcon) {
		super();
		
		this.file = file;
		this.fileIcon = fileIcon;
		this.uploadId = UUID.randomUUID().toString();
	}
	
	/**
	 * Add a listener event
	 * 
	 * @param listener Listener
	 */
	public void addListener(FileUploadSessionListener listener) {
		this.listener = listener;		
	}
	
	/**
	 * Returns the unique upload ID
	 * 
	 * @return ID
	 */
	public String getUploadID() {
		return uploadId;
	}

	/**
	 * Returns the content to be uploaded
	 * 
	 * @return Content
	 */
	public MmContent getContent() {
		return file;
	}
	
	/**
	 * Returns the file info document of the uploaded file on the content server
	 * 
	 * @return XML document
	 */
	public FileTransferHttpInfoDocument getFileInfoDocument() {
		return fileInfoDoc;
	}	

	/**
	 * Background processing
	 */
	public void run() {
		try {
	    	if (logger.isActivated()) {
	    		logger.info("Initiate a new HTTP upload " + uploadId);
	    	}

	    	// Create fileIcon content is requested
			MmContent fileIconContent = null;
			if (fileIcon) {
				// Create the file icon
				try {
					fileIconContent = FileTransferUtils.createFileicon(file.getUri(), uploadId);
				} catch (SecurityException e) {
					/*TODO: This is not the proper way to handle the exception thrown. Will be taken care of in CR037*/
					if (logger.isActivated()) {
						logger.error(
								"File icon creation has failed due to that the file is not accessible!",
								e);
					}
					listener.handleUploadNotAllowedToSend();
					return;
				}
			}
			
			// Instantiate the upload manager
			uploadManager = new HttpUploadManager(file, fileIconContent, this, uploadId);
	    	
	    	// Upload the file to the HTTP server 
            byte[] result = uploadManager.uploadFile();
            storeResult(result);
		} catch(Exception e) {
	    	if (logger.isActivated()) {
	    		logger.error("File transfer has failed", e);
	    	}
	    	
        	// Unexpected error
	    	listener.handleUploadError(UPLOAD_ERROR_UNSPECIFIED);
		}
	}

	/**
	 * Analyse the result
	 * 
	 * @param result Byte array result
	 */
    private void storeResult(byte[] result){
		// Check if upload has been cancelled
        if (uploadManager.isCancelled()) {
        	return;
        }

        // Parse the result:
        // <?xml version="1.0" encoding="UTF-8"?>
        // <file>
        // 	<file-info type="thumbnail">
        // 	  <file-size>6208</file-size>
        // 	  <content-type>image/jpeg</content-type>
        // 	  <data url = "https://ftcontentserver.rcs/download?id=001" until="2014-08-13T17:42:10.000+02:00"/>
        // 	</file-info>
        // 	
        // 	<file-info type="file">
        // 	  <file-size>1699846</file-size>
        // 	  <file-name>IMG_20140805_134311.jpg</file-name>
        // 	  <content-type>image/jpeg</content-type>
        // 	  <data url = "https://ftcontentserver.rcs/download?id=abb" until="2014-08-13T17:42:10.000+02:00"/>
        // 	</file-info>
        // </file>
        if (result != null) {
        	fileInfoDoc = FileTransferUtils.parseFileTransferHttpDocument(result);
        }
        if (fileInfoDoc != null) {
            // File uploaded with success
            if (logger.isActivated()) {
                logger.debug("Upload done with success: " + fileInfoDoc.getFileUri().toString());
            }

        	// Notify listener
	    	listener.handleUploadTerminated(fileInfoDoc);
		} else {
			// Upload error
            if (logger.isActivated()) {
                logger.debug("Upload has failed");
            }
            
        	// Notify listener
	    	listener.handleUploadError(UPLOAD_ERROR_UNSPECIFIED);
		}
	}
	
	/**
     * Posts an interrupt request to this Thread
     */
    public void interrupt(){
		super.interrupt();

		// Interrupt the upload
		uploadManager.interrupt();

		if (fileInfoDoc == null) {
			// Notify listener
			listener.handleUploadAborted();
		}
	}
    
	/**
	 * Notify the start of the HTTP Upload transfer (once the thumbnail transfer is done).
	 * <br>The upload resume is only possible once thumbnail is transferred 
	 */
    public void uploadStarted() {
		// Not used
	}
	
    /**
     * HTTP transfer started
     */
    public void httpTransferStarted() {
    	// Notify listener
    	listener.handleUploadStarted();
    }
    
    /**
     * HTTP transfer paused by user
     */
    public void httpTransferPausedByUser() {
		// Not used
    }
    
    /**
     * HTTP transfer paused by system
     */
    public void httpTransferPausedBySystem() {
        /*
         * Paused by system will be called for generic exceptions occurring in
         * the lower layers and in the scope of file upload this corresponds to
         * failure since pause/resume does not exist for file upload
         */
        listener.handleUploadError(UPLOAD_ERROR_UNSPECIFIED);
    }
    
    /**
     * HTTP transfer paused
     */
    public void httpTransferPausedByRemote() {
        // Not used
    }
    
    /**
     * HTTP transfer resumed
     */
    public void httpTransferResumed() {
		// Not used
    }

    /**
     * HTTP transfer progress
     *
     * @param currentSize Current transfered size in bytes
     * @param totalSize Total size in bytes
     */
    public void httpTransferProgress(long currentSize, long totalSize) {
    	// Notify listener
    	listener.handleUploadProgress(currentSize, totalSize);
    }

	@Override
	public void httpTransferNotAllowedToSend() {
		listener.handleUploadNotAllowedToSend();
	}
}
