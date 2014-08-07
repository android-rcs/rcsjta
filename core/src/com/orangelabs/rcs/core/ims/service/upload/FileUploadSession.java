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
public class FileUploadSession extends Thread {

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
     * HTTP upload listener
     */
    private HttpUploadTransferEventListener uploadListener = null;

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
	 * Add a listener on upload events
	 * 
	 * @param listener Listener
	 */
	public void addListener(HttpUploadTransferEventListener listener) {
		this.uploadListener = listener;
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
			uploadManager = new HttpUploadManager(file, fileiconContent, uploadListener, uploadId);
	    	
	    	// Upload the file to the HTTP server 
            byte[] result = uploadManager.uploadFile();
            storeResult(result);
		} catch(Exception e) {
	    	if (logger.isActivated()) {
	    		logger.error("File transfer has failed", e);
	    	}
        	// Unexpected error
			// TODO handleError(new FileSharingError(FileSharingError.UNEXPECTED_EXCEPTION, e.getMessage()));
		}
	}

    protected void storeResult(byte[] result){
		// Check if upload has been cancelled
        if (uploadManager.isCancelled()) {
        	return;
        }

        if ((result != null) && (FileTransferUtils.parseFileTransferHttpDocument(result) != null)) {
        	String fileInfo = new String(result);
            if (logger.isActivated()) {
                logger.debug("Upload done with success: " + fileInfo);
            }

            // TODO: set file info

            // File transfered
            // TODO handleFileTransfered();
		} else {
            // Don't call handleError in case of Pause or Cancel
            if (uploadManager.isCancelled() || uploadManager.isPaused()) {
                return;
            }

            if (logger.isActivated()) {
                logger.debug("Upload has failed");
            }
            // Upload error
            // TODO handleError(new FileSharingError(FileSharingError.MEDIA_UPLOAD_FAILED));
		}
	}
	
	/**
     * Posts an interrupt request to this Thread
     */
    public void interrupt(){
		super.interrupt();

		// Interrupt the upload
		uploadManager.interrupt();
	}
}
