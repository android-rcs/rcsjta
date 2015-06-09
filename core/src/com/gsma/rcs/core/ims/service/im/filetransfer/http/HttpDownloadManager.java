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
import com.gsma.rcs.platform.file.FileFactory;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

import android.net.Uri;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
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
    private BufferedOutputStream mFileDownloadStream;

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
    }

    /**
     * Open output stream for download file
     * 
     * @param file file path
     * @return BufferedOutputStream
     * @throws FileNotFoundException
     */
    private BufferedOutputStream openStreamForFile(File file) throws FileNotFoundException {
        return new BufferedOutputStream(new FileOutputStream(file, true));
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
     * @throws FileNotFoundException
     * @throws IOException
     * @throws FileNotDownloadedException
     */
    public void downloadFile() throws FileNotFoundException, IOException,
            FileNotDownloadedException {
        if (sLogger.isActivated()) {
            sLogger.debug("Download file " + getHttpServerAddr());
        }
        if (mFileDownloadStream == null) {
            mFileDownloadStream = openStreamForFile(mFile);

        }
        /* Send GET request */
        HttpGet request = new HttpGet(getHttpServerAddr().toString());
        request.addHeader("User-Agent", SipUtils.userAgentString());
        if (HTTP_TRACE_ENABLED) {
            System.out.println(new StringBuilder(">>> Send HTTP request:").append("\n"
                    + request.getMethod() + " " + request.getRequestLine().getUri()));
        }

        try {
            writeHttpContentToFile(request);
        } catch (IOException e) {
            /*
             * Either the stream is currently not open or there has been a connection time out, In
             * either cases we should pause downloading the file.
             */
            if (!isPaused() && !isCancelled()) {
                pauseTransferBySystem();
            }
            throw e;
        } catch (FileNotDownloadedException e) {
            /* Execute request with retry procedure */
            /*
             * Something went wrong during file download, either the HTTP response was not as
             * expected one or the file was corrupted, In either case we should retry file download
             * for RETRY_MAX
             */
            if (mRetryCount < RETRY_MAX && !isCancelled() && !isPaused()) {
                mRetryCount++;
                downloadFile();
            } else {
                throw e;
            }
        }
    }

    /**
     * Write the content fetched from http request onto file
     * 
     * @param request HTTP request
     * @throws IOException
     * @throws FileNotDownloadedException
     */
    private void writeHttpContentToFile(HttpGet request) throws IOException,
            FileNotDownloadedException {
        try {
            /* Execute Http Request */
            HttpResponse response = getHttpClient().execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            if (HTTP_TRACE_ENABLED) {
                System.out.println(new StringBuilder("<<< Receive HTTP response:").append("\n"
                        + statusCode + " " + response.getStatusLine().getReasonPhrase()));
            }
            /* Analyze HTTP response */
            switch (statusCode) {
                case HttpStatus.SC_OK:
                    mCalcLength = 0;
                    break;
                case HttpStatus.SC_PARTIAL_CONTENT:
                    mCalcLength = Long.valueOf(mFile.length()).intValue();
                    break;
                default:
                    throw new FileNotDownloadedException(new StringBuilder(
                            "Unhandled http response code : ").append(statusCode)
                            .append(" for file download from server!").toString());
            }
            /* Read content */
            byte[] buffer = new byte[CHUNK_MAX_SIZE];
            HttpEntity entity = response.getEntity();
            InputStream input = entity.getContent();
            int num;
            while ((num = input.read(buffer)) != -1 && !isCancelled() && !isPaused()) {
                mCalcLength += num;
                getListener().httpTransferProgress(mCalcLength, mContent.getSize());
                mFileDownloadStream.write(buffer, 0, num);
            }

            /*
             * Check if the file is already paused, If it is then its still a partial download and
             * hence we should not delete the file.
             */
            if (isPaused()) {
                throw new FileNotDownloadedException(
                        "Download file paused, the file is not complete!");
            }

            /*
             * Check if we are able to download the file properly by comparing the file content
             * size, also make sure that the download is not cancelled
             */
            if (!isCancelled() && mCalcLength != mContent.getSize()) {
                /* Delete file as download is not successful */
                mFile.delete();
                throw new FileNotDownloadedException(
                        "Download file error, the file is not complete!");
            }
            FileFactory.getFactory().updateMediaStorage(mDownloadedFile.getEncodedPath());
        } finally {
            if (mFileDownloadStream != null) {
                try {
                    mFileDownloadStream.flush();
                    mFileDownloadStream.close();
                } catch (IOException ignore) {
                    /* Nothing to be handled here */
                }
                mFileDownloadStream = null;
            }
        }
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
            System.out.println(new StringBuilder(">>> Send HTTP request:").append("\n"
                    + request.getMethod() + " " + request.getRequestLine().getUri()));
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
            System.out.println(new StringBuilder("<<< Receive HTTP response:").append("\n"
                    + statusCode + " " + response.getStatusLine().getReasonPhrase()));
        }

        // Analyze HTTP response
        if (HttpStatus.SC_OK == statusCode) {
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
     * @throws FileNotFoundException
     * @throws IOException
     * @throws FileNotDownloadedException
     */
    public void resumeDownload() throws FileNotFoundException, IOException,
            FileNotDownloadedException {
        if (mFileDownloadStream == null) {
            mFileDownloadStream = openStreamForFile(mFile);
        }
        resetParamForResume();
        Uri serverAddress = getHttpServerAddr();
        if (sLogger.isActivated()) {
            sLogger.debug("Resume Download file " + serverAddress + " from byte " + mFile.length());
        }

        // Send GET request
        HttpGet request = new HttpGet(serverAddress.toString());
        long downloadedLength = mFile.length();
        long completeSize = mContent.getSize();
        request.addHeader("User-Agent", SipUtils.userAgentString());
        request.addHeader("Range", "bytes=" + downloadedLength + "-" + completeSize);
        if (HTTP_TRACE_ENABLED) {
            System.out.println(new StringBuilder(">>> Send HTTP request:").append("\n"
                    + request.getMethod() + " " + request.getRequestLine().getUri()));
        }

        try {
            writeHttpContentToFile(request);
        } catch (IOException e) {
            /*
             * Either the stream is curently not open or there has been a connection time out, In
             * either cases we should pause downloading the file.
             */
            if (!isPaused() && !isCancelled()) {
                pauseTransferBySystem();
            }
            throw e;
        } catch (IllegalStateException e) {
            /* Execute request with retry procedure */
            /*
             * Something went wrong during file download, either the HTTP response was not as
             * expected one or the file was corrupted, In either case we should retry file download
             * for RETRY_MAX
             */
            if (mRetryCount < RETRY_MAX && !isCancelled() && !isPaused()) {
                mRetryCount++;
                downloadFile();
            } else {
                throw e;
            }
        }
    }

    /**
     * checks if the stream is already available for usage
     * 
     * @return True if Stream is NOT NULL
     */
    /* package private */boolean isFileStreamAllocated() {
        return mFileDownloadStream != null;
    }
}
