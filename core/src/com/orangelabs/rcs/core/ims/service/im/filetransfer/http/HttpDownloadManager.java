package com.orangelabs.rcs.core.ims.service.im.filetransfer.http;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * HTTP upload manager
 *
 * @author jexa7410
 */
public class HttpDownloadManager extends HttpTransferManager {
    /**
     * Maximum value of retry
     */
    private final static int RETRY_MAX = 3;

    /**
     * File content to download
     */
    private MmContent content;

    /**
     * File name
     */
    private String contentName;

    /**
     * Retry counter
     */
    private int retryCount = 0;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     *
     * @param content File content to download
     * @param listener HTTP transfer event listener
     * @param filename Filename to download 
     */
    public HttpDownloadManager(MmContent content, HttpTransferEventListener listener, String filename) {
        super(listener);
        
        this.content = content;
        this.contentName = filename;
    }

    /**
     * Returns complete filename
     *
     * @return Filename
     */
    public String getFilename() {
        return RcsSettings.getInstance().getFileRootDirectory() + contentName;
    }

    /**
     * Download file
     * 
     * @return Returns true if successful. Data are saved during the transfer in the content object.  
     */
    public boolean downloadFile() {
        try {
            if (logger.isActivated()) {
                logger.debug("Download file " + content.getUrl());
            }

            // Send GET request
            HttpGet request = new HttpGet(content.getUrl());
            if (HTTP_TRACE_ENABLED) {
                String trace = ">>> Send HTTP request:";
                trace += "\n" + request.getMethod() + " " + request.getRequestLine().getUri();
                System.out.println(trace);
            }

            // Execute request with retry procedure
            if (!getFile(request)) {
                if (retryCount < RETRY_MAX && !isCancelled()) {
                    retryCount++;
                    return downloadFile();
                } else {
                    if (logger.isActivated()) {
                    	if(isCancelled()) {
                    		if (logger.isActivated()) {
                    			logger.debug("Download file cancelled");
                    		}
                    	} else {
                    		if (logger.isActivated()) {
                    			logger.debug("Failed to download file");
                    		}
                    	}
                    }
                    return false;
                }
            }

            return true;
        } catch(Exception e) {
        	if (logger.isActivated()) {
        		logger.error("Donwload file exception", e);
        	}
            return false;
        }
    }

    /**
     * Get the file and save it
     *
     * @param request HTTP request
     * @return Returns true if successful
     */
    private boolean getFile(HttpGet request) {
        try {
            // Init file
            File file = new File(RcsSettings.getInstance().getFileRootDirectory(), contentName);

            // Execute HTTP request
            HttpResponse response = getHttpClient().execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            if (HTTP_TRACE_ENABLED) {
                String trace = "<<< Resceive HTTP response:";
                trace += "\n" + statusCode + " " + response.getStatusLine().getReasonPhrase();
                System.out.println(trace);
            }
            
            // Analyze HTTP response
            if (statusCode == 200) {
                BufferedOutputStream bOutputStream = new BufferedOutputStream(new FileOutputStream(file));
                byte[] buffer = new byte[CHUNK_MAX_SIZE];
                HttpEntity entity = response.getEntity();
                InputStream input = entity.getContent();
                int calclength = 0;
                int num;
                while ((num = input.read(buffer)) != -1 && !isCancelled()) {
                    calclength += num;
                    getListener().httpTransferProgress(calclength, content.getSize());
                    bOutputStream.write(buffer, 0, num);
                }

    	        bOutputStream.flush();
                bOutputStream.close();

                if (isCancelled()) {
    	        	file.delete();
    	        	return false;
            	} else {
            		return true;
            	}
            } else {
            	return false;
            }
        } catch(Exception e) {
        	if (logger.isActivated()) {
        		logger.error("Donwload file exception", e);
        	}
        	return false;
        }
    }

	/**
	 * Download the thumbnail
	 * 
	 * @param thumbnailInfo Thumbnail info
	 * @return Thumbnail picture data or null in case of error
	 */
	public byte[] downloadThumbnail(FileTransferHttpThumbnail thumbnailInfo) {
		try {
            if (logger.isActivated()) {
                logger.debug("Download Thumbnail" + content.getUrl());
            }

            // Send GET request
            HttpGet request = new HttpGet(thumbnailInfo.getThumbnailUrl());
            if (HTTP_TRACE_ENABLED) {
                String trace = ">>> Send HTTP request:";
                trace += "\n" + request.getMethod() + " " + request.getRequestLine().getUri();
                System.out.println(trace);
            }
            
            // Execute request with retry procedure
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if ((baos = getThumbnail(request)) == null) {
                if (retryCount < RETRY_MAX) {
                    retryCount++;
                    return downloadThumbnail(thumbnailInfo);
                } else {
                    if (logger.isActivated()) {
                        logger.debug("Failed to download Thumbnail");
                    }
                    return null;
                }
            }

            return baos.toByteArray();
        } catch(Exception e) {
        	if (logger.isActivated()) {
        		logger.error("Download thumbnail exception", e);
        	}
            return null;
        }
	}
	
	/**
     * Get the thumbnail and save it
     *
     * @param request HTTP request
	 * @return Thumbnail picture data or null in case of error
     */
    private ByteArrayOutputStream getThumbnail(HttpGet request) {
		try {
		    // Execute HTTP request
		    HttpResponse response = getHttpClient().execute(request);
		    int statusCode = response.getStatusLine().getStatusCode();
		    if (HTTP_TRACE_ENABLED) {
		        String trace = "<<< Resceive HTTP response:";
		        trace += "\n" + statusCode + " " + response.getStatusLine().getReasonPhrase();
		        System.out.println(trace);
		    }
		    
            // Analyze HTTP response
		    if (statusCode == 200) {
				int calclength = 0;
			    byte[] buffer = new byte[CHUNK_MAX_SIZE];
				ByteArrayOutputStream bOutputStream = new ByteArrayOutputStream();
		        HttpEntity entity = response.getEntity();
		        InputStream input = entity.getContent();
		        int num;
		        while ((num = input.read(buffer)) != -1 && !isCancelled()) {
		            calclength += num;
		            getListener().httpTransferProgress(calclength, content.getSize());
		            bOutputStream.write(buffer, 0, num);
		        }
		        
		        bOutputStream.flush();
		        bOutputStream.close();

		        if(isCancelled()) {
		        	return null;
	        	} else {
		        	return bOutputStream;
		        }
		    } else  {
		    	return null;
		    }
		} catch (Exception e) {
        	if (logger.isActivated()) {
        		logger.error("Download thumbnail exception", e);
        	}
		    return null;
		}
    }
}
