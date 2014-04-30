package com.orangelabs.rcs.core.ims.service.im.filetransfer.http;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
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
	 * File to be created
	 */
	File file;

	/**
	 * Stream that writes the file
	 */
	BufferedOutputStream streamForFile = null;

	/**
	 * number of received bytes calculated
	 */
	int calclength = 0;

	/**
	 * the URL of the downloaded file
	 */
	private String localUrl;

	private byte[] thumbnail;

	/**
	 * Retry counter
	 */
	private int retryCount = 0;

	/**
	 * The logger
	 */
	private static final Logger logger = Logger.getLogger(HttpDownloadManager.class.getSimpleName());

	/**
	 * Constructor
	 * 
	 * @param content
	 *            File content to download
	 * @param listener
	 *            HTTP transfer event listener
	 * @param filename
	 *            Filename to download
	 */
	public HttpDownloadManager(MmContent content, HttpTransferEventListener listener, String filename) {
		super(listener, content.getUrl());
		this.content = content;
		this.localUrl = filename;
		// Init file
		file = new File(localUrl);
		if (logger.isActivated()) {
			logger.debug("HttpDownloadManager file=" + filename + " length=" + file.length());
		}
		streamForFile = openStremForFile(file);
	}

	/**
	 * Open output stream for download file
	 * 
	 * @param file
	 *            file path
	 * @return BufferedOutputStream or null
	 */
	static BufferedOutputStream openStremForFile(File file) {
		try {
			return new BufferedOutputStream(new FileOutputStream(file, true));
		} catch (FileNotFoundException e) {
			if (logger.isActivated()) {
				logger.error("Could not open stream, file does not exists.");
			}
		}
		return null;
	}
	
	/**
	 * Returns the local Url
	 * 
	 * @return localUrl
	 */
	public String getLocalUrl() {
		return localUrl;
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
			if (streamForFile == null) {
				streamForFile = openStremForFile(file);
				if (streamForFile == null)
					return false;
			}
			// Send GET request
			HttpGet request = new HttpGet(content.getUrl());
            request.addHeader("User-Agent", SipUtils.userAgentString());
			if (HTTP_TRACE_ENABLED) {
				String trace = ">>> Send HTTP request:";
				trace += "\n" + request.getMethod() + " " + request.getRequestLine().getUri();
				System.out.println(trace);
			}
			// Execute request with retry procedure
			if (!getFile(request)) {
				if (retryCount < RETRY_MAX && !isCancelled() && !isPaused()) {
					retryCount++;
					return downloadFile();
				} else {
                    if (logger.isActivated()) {
                        if (isPaused()) {
                            logger.debug("Download file paused");
                        } else if (isCancelled()) {
                            logger.debug("Download file cancelled");
                        } else {
                            logger.debug("Failed to download file");
                        }
                    }
					return false;
				}
			}
			return true;
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Donwload file exception", e);
			}
			return false;
		}
	}

	/**
	 * Get the file and save it
	 * 
	 * @param request
	 *            HTTP request
	 * @return Returns true if successful
	 */
	private boolean getFile(HttpGet request) {
        HttpResponse response = null;
		try {
			// Execute HTTP request
			response = getHttpClient().execute(request);
			int statusCode = response.getStatusLine().getStatusCode();
			if (HTTP_TRACE_ENABLED) {
				String trace = "<<< Receive HTTP response:";
				trace += "\n" + statusCode + " " + response.getStatusLine().getReasonPhrase();
				System.out.println(trace);
			}

			// Analyze HTTP response
			if (statusCode == 200) { // TODO need to check other responses ?
				calclength = 0;
			} else if (statusCode == 206) {
				calclength = Long.valueOf(file.length()).intValue();
			} else {
				return false;
			}
        } catch (Exception e) {
                if (logger.isActivated()) {
                    logger.error("Download file exception", e);
                }
                return false;
        }

        try {
            // Read content
			byte[] buffer = new byte[CHUNK_MAX_SIZE];
			HttpEntity entity = response.getEntity();
			InputStream input = entity.getContent();
			int num;
			while ((num = input.read(buffer)) != -1 && !isCancelled() && !isPaused()) {
				calclength += num;
				getListener().httpTransferProgress(calclength, content.getSize());
				streamForFile.write(buffer, 0, num);
			}
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Download file exception. Set in paused", e);
            }
            pauseTransfer();
            return false;
        }

        try {
            streamForFile.flush();
			if (isPaused()) {
				return false;
			}
			streamForFile.close();
			streamForFile = null;

			if (isCancelled()) {
				file.delete();
				return false;
			} else {
				return true;
			}
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Download file exception", e);
			}
			return false;
		}
	}

	/**
	 * Download the thumbnail
	 * 
	 * @param thumbnailInfo
	 *            Thumbnail info
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

			// Execute request
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			if ((baos = getThumbnail(request)) == null) {
				if (logger.isActivated()) {
					logger.debug("Failed to download Thumbnail");
				}
			}
			thumbnail = baos.toByteArray();
			return thumbnail;
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Download thumbnail exception", e);
			}
			return null;
		}
	}

	/**
	 * Get the thumbnail and save it
	 * 
	 * @param request
	 *            HTTP request
	 * @return Thumbnail picture data or null in case of error
	 */
	private ByteArrayOutputStream getThumbnail(HttpGet request) {
		try {
			// Execute HTTP request
			HttpResponse response = getHttpClient().execute(request);
			int statusCode = response.getStatusLine().getStatusCode();
			if (HTTP_TRACE_ENABLED) {
				String trace = "<<< Receive HTTP response:";
				trace += "\n" + statusCode + " " + response.getStatusLine().getReasonPhrase();
				System.out.println(trace);
			}

			// Analyze HTTP response
			if (statusCode == 200) {
				calclength = 0;
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

				if (isCancelled()) {
					return null;
				} else {
					return bOutputStream;
				}
			} else {
				return null;
			}
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Download thumbnail exception", e);
			}
			return null;
		}
	}

	/**
	 * Resume FToHTTP download
	 * @return True if successful
	 */
	public boolean resumeDownload() {
		if (streamForFile == null) {
			streamForFile = openStremForFile(file);
			if (streamForFile == null)
				return false;
		}
		resetParamForResume();
		try {
			if (logger.isActivated()) {
				logger.debug("Resume Download file " + content.getUrl() + " from byte " + file.length());
			}

			// Send GET request
			HttpGet request = new HttpGet(content.getUrl());
			long downloadedLength = file.length();
			long completeSize = content.getSize();
            request.addHeader("User-Agent", SipUtils.userAgentString());
			request.addHeader("Range", "bytes=" + downloadedLength + "-" + completeSize);
			if (HTTP_TRACE_ENABLED) {
				String trace = ">>> Send HTTP request:";
				trace += "\n" + request.getMethod() + " " + request.getRequestLine().getUri();
				System.out.println(trace);
			}

			// Execute request with retry procedure
            if (!getFile(request)) {
                if (retryCount < RETRY_MAX && !isCancelled() && !isPaused()) {
                    retryCount++;
                    return downloadFile();
                } else {
                    if (logger.isActivated()) {
                        if (isPaused()) {
                            logger.debug("Download file paused");
                        } else if (isCancelled()) {
                            logger.debug("Download file cancelled");
                        } else {
                            logger.debug("Failed to download file");
                        }
                    }
                    return false;
                }
            }

			return true;
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Download file exception", e);
			}
			return false;
		}
	}
}
