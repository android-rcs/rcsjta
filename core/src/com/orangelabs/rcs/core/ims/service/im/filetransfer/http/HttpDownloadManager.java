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

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import android.net.Uri;

import com.orangelabs.rcs.core.content.ContentManager;
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
	 * URI of file to be created
	 */
	Uri downloadedFile;

	/**
	 * Stream that writes the file
	 */
	BufferedOutputStream streamForFile = null;

	/**
	 * number of received bytes calculated
	 */
	int calclength = 0;

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
	 * @param httpServerAddress
	 *            Server address from where file is downloaded
	 */
	public HttpDownloadManager(MmContent content, HttpTransferEventListener listener, Uri httpServerAddress) {
		super(listener, httpServerAddress);
		this.content = content;
		downloadedFile = content.getUri();
		file = new File(downloadedFile.getPath());
		if (logger.isActivated()) {
			logger.debug("HttpDownloadManager file from " + httpServerAddress + " length=" + content.getSize());
		}
		streamForFile = openStreamForFile(file);
	}

	/**
	 * Open output stream for download file
	 * 
	 * @param file
	 *            file path
	 * @return BufferedOutputStream or null
	 */
	static BufferedOutputStream openStreamForFile(File file) {
		try {
			return new BufferedOutputStream(new FileOutputStream(file, true));
		} catch (FileNotFoundException e) {
			if (logger.isActivated()) {
				logger.error("Could not open stream: file does not exists");
			}
			return null;
		}
	}

	/**
	 * Returns complete file URI
	 *
	 * @return Uri of downloaded file
	 */
	public Uri getDownloadedFileUri() {
		return downloadedFile;
	}

	/**
	 * Download file
	 * 
	 * @return Returns true if successful. Data are saved during the transfer in the content object.
	 */
	public boolean downloadFile() {
		try {
			if (logger.isActivated()) {
				logger.debug("Download file " + getHttpServerAddr());
			}
			if (streamForFile == null) {
				streamForFile = openStreamForFile(file);
				if (streamForFile == null)
					return false;
			}
			// Send GET request
			HttpGet request = new HttpGet(getHttpServerAddr().toString());
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
            pauseTransferBySystem();
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
	 * @param fileIcon
	 *            file icon info
	 * @param fileName the file icon filename
	 * @return fileIcon picture content or null in case of error
	 */
	public MmContent downloadThumbnail(FileTransferHttpThumbnail thumbnailInfo, String fileName) {
		MmContent fileIcon = null;
		try {
			if (logger.isActivated()) {
				logger.debug("Download file icon" + getHttpServerAddr());
			}

			// Send GET request
			HttpGet request = new HttpGet(thumbnailInfo.getThumbnailUri().toString());
			if (HTTP_TRACE_ENABLED) {
				String trace = ">>> Send HTTP request:";
				trace += "\n" + request.getMethod() + " " + request.getRequestLine().getUri();
				System.out.println(trace);
			}

			// Execute request
			ByteArrayOutputStream baos;
			if ((baos = getThumbnail(request)) == null) {
				if (logger.isActivated()) {
					logger.debug("Failed to download Thumbnail");
				}
				return null;
			}
			// Create the content for filename
			Uri fileIconUri = ContentManager.generateUriForReceivedContent(fileName, thumbnailInfo.getThumbnailType());
			fileIcon = ContentManager.createMmContent(fileIconUri, baos.size(), fileName);
			// Save data to file
			fileIcon.writeData2File(baos.toByteArray());
			return fileIcon;
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Download thumbnail exception", e);
			}
			return null;
		} finally {
			if (fileIcon != null) {
				try {
					fileIcon.closeFile();
				} catch (Exception e2) {
				}
			}
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
			streamForFile = openStreamForFile(file);
			if (streamForFile == null)
				return false;
		}
		resetParamForResume();
		try {
			Uri serverAddress = getHttpServerAddr();
			if (logger.isActivated()) {
				logger.debug("Resume Download file " + serverAddress + " from byte " + file.length());
			}

			// Send GET request
			HttpGet request = new HttpGet(serverAddress.toString());
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
