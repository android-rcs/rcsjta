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

package com.gsma.rcs.core.ims.service.im.filetransfer.http;

import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

import android.net.Uri;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
    private MmContent mContent;

    /**
     * File to be created
     */
    private File mFile;

    /**
     * URI of file to be created
     */
    private Uri mDownloadedFile;

    /**
     * Stream that writes the file
     */
    /* package private */BufferedOutputStream mStreamForFile;

    /**
     * number of received bytes calculated
     */
    private int mCalcLength = 0;

    /**
     * Retry counter
     */
    private int mRetryCount = 0;

    private static final Logger sLogger = Logger.getLogger(HttpDownloadManager.class
            .getSimpleName());

    /**
     * Constructor
     * 
     * @param content File content to download
     * @param listener HTTP transfer event listener
     * @param httpServerAddress Server address from where file is downloaded
     * @param rcsSettings
     */
    public HttpDownloadManager(MmContent content, HttpTransferEventListener listener,
            Uri httpServerAddress, RcsSettings rcsSettings) {
        super(listener, httpServerAddress, rcsSettings);
        mContent = content;
        mDownloadedFile = content.getUri();
        mFile = new File(mDownloadedFile.getPath());
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("HttpDownloadManager file from ")
                    .append(httpServerAddress).append(" length=").append(content.getSize())
                    .toString());
        }
        mStreamForFile = openStreamForFile(mFile);
    }

    /**
     * Open output stream for download file
     * 
     * @param file file path
     * @return BufferedOutputStream or null
     */
    static BufferedOutputStream openStreamForFile(File file) {
        try {
            return new BufferedOutputStream(new FileOutputStream(file, true));
        } catch (FileNotFoundException e) {
            if (sLogger.isActivated()) {
                sLogger.error("Could not open stream: file does not exists");
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
        return mDownloadedFile;
    }

    /**
     * Download file
     * 
     * @return Returns true if successful. Data are saved during the transfer in the content object.
     */
    public boolean downloadFile() {
        try {
            if (sLogger.isActivated()) {
                sLogger.debug("Download file " + getHttpServerAddr());
            }
            if (mStreamForFile == null) {
                mStreamForFile = openStreamForFile(mFile);
                if (mStreamForFile == null)
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
                if (mRetryCount < RETRY_MAX && !isCancelled() && !isPaused()) {
                    mRetryCount++;
                    return downloadFile();
                } else {
                    if (sLogger.isActivated()) {
                        if (isPaused()) {
                            sLogger.debug("Download file paused");
                        } else if (isCancelled()) {
                            sLogger.debug("Download file cancelled");
                        } else {
                            sLogger.debug("Failed to download file");
                        }
                    }
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            if (sLogger.isActivated()) {
                sLogger.error("Donwload file exception", e);
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
                mCalcLength = 0;
            } else if (statusCode == 206) {
                mCalcLength = Long.valueOf(mFile.length()).intValue();
            } else {
                return false;
            }
        } catch (Exception e) {
            if (sLogger.isActivated()) {
                sLogger.error("Download file exception", e);
            }
            return false;
        }

        boolean isSuccess = false;

        try {
            // Read content
            byte[] buffer = new byte[CHUNK_MAX_SIZE];
            HttpEntity entity = response.getEntity();
            InputStream input = entity.getContent();
            int num;
            while ((num = input.read(buffer)) != -1 && !isCancelled() && !isPaused()) {
                mCalcLength += num;
                getListener().httpTransferProgress(mCalcLength, mContent.getSize());
                mStreamForFile.write(buffer, 0, num);
            }
        } catch (Exception e) {
            if (sLogger.isActivated()) {
                sLogger.error("Download file exception. Set in paused", e);
            }
            pauseTransferBySystem();
        }

        if (!isPaused() && !isCancelled()) {
            /* Check length of received data */
            if (mCalcLength == mContent.getSize()) {
                isSuccess = true;
            } else {
                if (sLogger.isActivated()) {
                    sLogger.error("Download file error, the file is not complete.");
                }
            }
        }

        try {
            mStreamForFile.flush();
        } catch (Exception e) {
            /* Nothing to do */
        }

        /* If paused, keep the streamForFile and the file */
        if (isPaused()) {
            return false;
        }

        try {
            mStreamForFile.close();
        } catch (Exception e) {
            /* Nothing to do */
        }
        mStreamForFile = null;

        /* Delete file if not successful */
        if (!isSuccess) {
            mFile.delete();
        }
        return isSuccess;
    }

    /**
     * Download the thumbnail and save it
     * 
     * @param iconUri the remote URI
     * @param fileIcon the local descriptor
     * @throws IOException
     */
    public void downloadThumbnail(Uri iconUri, MmContent fileIcon) throws IOException {
        if (sLogger.isActivated()) {
            sLogger.debug("Download file icon from ".concat(getHttpServerAddr().toString()));
        }
        // Send GET request
        HttpGet request = new HttpGet(iconUri.toString());
        if (HTTP_TRACE_ENABLED) {
            String trace = ">>> Send HTTP request:";
            trace += "\n" + request.getMethod() + " " + request.getRequestLine().getUri();
            System.out.println(trace);
        }

        // Execute request
        ByteArrayOutputStream baos;
        baos = getThumbnail(request);
        try {
            // Save data to file
            fileIcon.writeData2File(baos.toByteArray());
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
     * Get the thumbnail
     * 
     * @param request HTTP request
     * @return Thumbnail picture data
     * @throws IOException
     */
    private ByteArrayOutputStream getThumbnail(HttpGet request) throws IOException {
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
            byte[] buffer = new byte[CHUNK_MAX_SIZE];
            ByteArrayOutputStream bOutputStream = new ByteArrayOutputStream();
            HttpEntity entity = response.getEntity();
            InputStream input = entity.getContent();
            int num;
            while ((num = input.read(buffer)) != -1) {
                bOutputStream.write(buffer, 0, num);
                if (isCancelled()) {
                    break;
                }
            }
            bOutputStream.flush();
            bOutputStream.close();
            return bOutputStream;
        }
        throw new IOException(new StringBuilder("Received '").append(statusCode)
                .append("' from server").toString());
    }

    /**
     * Resume FToHTTP download
     * 
     * @return True if successful
     */
    public boolean resumeDownload() {
        if (mStreamForFile == null) {
            mStreamForFile = openStreamForFile(mFile);
            if (mStreamForFile == null)
                return false;
        }
        resetParamForResume();
        try {
            Uri serverAddress = getHttpServerAddr();
            if (sLogger.isActivated()) {
                sLogger.debug("Resume Download file " + serverAddress + " from byte "
                        + mFile.length());
            }

            // Send GET request
            HttpGet request = new HttpGet(serverAddress.toString());
            long downloadedLength = mFile.length();
            long completeSize = mContent.getSize();
            request.addHeader("User-Agent", SipUtils.userAgentString());
            request.addHeader("Range", "bytes=" + downloadedLength + "-" + completeSize);
            if (HTTP_TRACE_ENABLED) {
                String trace = ">>> Send HTTP request:";
                trace += "\n" + request.getMethod() + " " + request.getRequestLine().getUri();
                System.out.println(trace);
            }

            // Execute request with retry procedure
            if (!getFile(request)) {
                if (mRetryCount < RETRY_MAX && !isCancelled() && !isPaused()) {
                    mRetryCount++;
                    return downloadFile();
                } else {
                    if (sLogger.isActivated()) {
                        if (isPaused()) {
                            sLogger.debug("Download file paused");
                        } else if (isCancelled()) {
                            sLogger.debug("Download file cancelled");
                        } else {
                            sLogger.debug("Failed to download file");
                        }
                    }
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            if (sLogger.isActivated()) {
                sLogger.error("Download file exception", e);
            }
            return false;
        }
    }
}
