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
package com.orangelabs.rcs.core.ims.service.upload;

import java.util.UUID;

import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.HttpUploadManager;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.HttpUploadTransferEventListener;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * File upload session
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileUploadSession extends Thread implements HttpUploadTransferEventListener {

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
    private boolean fileicon = false;

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
    private String fileInfo = null;
    
    /**
     * The logger
     */
    private final static Logger logger = Logger.getLogger(FileUploadSession.class.getSimpleName());
    
	/**
	 * Constructor
	 * 
	 * @param content Content of file to upload
	 * @param fileicon True if the stack must try to attach fileicon
	 */
	public FileUploadSession(MmContent file, boolean fileicon) {
		super();
		
		this.file = file;
		this.fileicon = fileicon;
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
	 * Background processing
	 */
	public void run() {
		try {
	    	if (logger.isActivated()) {
	    		logger.info("Initiate a new HTTP upload");
	    	}

	    	// Create fileicon content is requested
			MmContent fileiconContent = null;
			if (fileicon) {
				// Create the file icon
				fileiconContent = FileTransferUtils.createFileicon(file.getUri(), uploadId);
			}
			
			// Instantiate the upload manager
			uploadManager = new HttpUploadManager(file, fileiconContent, this, uploadId);
	    	
	    	// Upload the file to the HTTP server 
            byte[] result = uploadManager.uploadFile();
            storeResult(result);
		} catch(Exception e) {
	    	if (logger.isActivated()) {
	    		logger.error("File transfer has failed", e);
	    	}
	    	
        	// Unexpected error
	    	listener.handleUploadError(-1);
		}
	}

    protected void storeResult(byte[] result){
		// Check if upload has been cancelled
        if (uploadManager.isCancelled()) {
        	return;
        }

        if ((result != null) && (FileTransferUtils.parseFileTransferHttpDocument(result) != null)) {
            // File uploaded
        	fileInfo = new String(result);
            if (logger.isActivated()) {
                logger.debug("Upload done with success: " + fileInfo);
            }

        	// Notify listener
	    	listener.handleUploadTerminated(fileInfo);
		} else {
			// Upload error
            if (logger.isActivated()) {
                logger.debug("Upload has failed");
            }
            
        	// Notify listener
	    	listener.handleUploadError(-1);
		}
	}
	
	/**
     * Posts an interrupt request to this Thread
     */
    public void interrupt(){
		super.interrupt();

		// Interrupt the upload
		uploadManager.interrupt();

		if (fileInfo == null) {
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
     * HTTP transfer paused
     */
    public void httpTransferPaused() {
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
}
