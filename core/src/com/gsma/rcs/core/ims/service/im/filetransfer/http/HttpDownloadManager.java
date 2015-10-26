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

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.platform.file.FileFactory;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.CloseableUtils;
import com.gsma.rcs.utils.logger.Logger;

import android.net.Uri;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLHandshakeException;

/**
 * HTTP upload manager
 * 
 * @author jexa7410
 * @author yplo6403
 */
public class HttpDownloadManager extends HttpTransferManager {
    /**
     * Maximum value of retry
     */
    private final static int RETRY_MAX = 3;

    /**
     * File content to download
     */
    private final MmContent mContent;

    /**
     * File to be created
     */
    private final File mFile;

    /**
     * URI of file to be created
     */
    private final Uri mDownloadedFile;

    /**
     * Stream that writes the file
     */
    private BufferedOutputStream mFileDownloadStream;

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
            sLogger.debug("HttpDownloadManager file from " + httpServerAddress + " length="
                    + content.getSize());
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
     * @throws NetworkException
     */
    public void downloadFile() throws FileNotFoundException, IOException,
            FileNotDownloadedException, NetworkException {
        if (sLogger.isActivated()) {
            sLogger.debug("Download file " + getHttpServerAddr());
        }
        if (mFileDownloadStream == null) {
            mFileDownloadStream = openStreamForFile(mFile);
        }
        /* Send GET request */
        if (isHttpTraceEnabled()) {
            System.out.println(">>> Send HTTP request:\nGET " + getHttpServerAddr());
        }

        try {
            writeHttpContentToFile(new URL(getHttpServerAddr().toString()),
                    new HashMap<String, String>());
        } catch (SSLHandshakeException e) {
            /*
             * If there are issues during handshake between UE and server then we should not proceed
             * any further with file download, we should immediately cancel the transfer, One on the
             * possible case would be a certificate mismatch
             */
            throw e;
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
     * Write the content fetched from HTTP request onto file
     * 
     * @param url the URL of the file to download on the content server
     * @param properties the HTTP properties
     * @throws IOException
     * @throws FileNotDownloadedException
     * @throws NetworkException
     */
    private void writeHttpContentToFile(URL url, Map<String, String> properties)
            throws IOException, FileNotDownloadedException, NetworkException {
        HttpURLConnection urlConnection = null;
        try {
            /* Execute HTTP Request */
            urlConnection = openHttpConnection(url, properties);
            int statusCode = urlConnection.getResponseCode();
            String message = urlConnection.getResponseMessage();
            if (sLogger.isActivated()) {
                sLogger.debug("HTTP get file response: " + statusCode + " (" + message + ")");
            }
            if (isHttpTraceEnabled()) {
                System.out.println("<<< Receive HTTP response: \n" + statusCode + " " + statusCode);
            }
            int receivedBytes = 0;
            /* Analyze HTTP response */
            switch (statusCode) {
                case HttpURLConnection.HTTP_OK:
                    break;
                case HttpURLConnection.HTTP_PARTIAL:
                    receivedBytes = Long.valueOf(mFile.length()).intValue();
                    break;
                default:
                    throw new FileNotDownloadedException("Unhandled http response code : "
                            + statusCode + " for file download from server!");
            }
            /* Read content */
            byte[] buffer = new byte[CHUNK_MAX_SIZE];
            InputStream input = urlConnection.getInputStream();
            int num;
            while ((num = input.read(buffer)) != -1 && !isCancelled() && !isPaused()) {
                receivedBytes += num;
                getListener().onHttpTransferProgress(receivedBytes, mContent.getSize());
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
            if (!isCancelled() && receivedBytes != mContent.getSize()) {
                /* Delete file as download is not successful */
                mFile.delete();
                throw new FileNotDownloadedException(
                        "Download file error, the file is not complete!");
            }
            FileFactory.getFactory().updateMediaStorage(mDownloadedFile.getEncodedPath());
        } finally {
            CloseableUtils.tryToClose(mFileDownloadStream);
            mFileDownloadStream = null;
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    /**
     * Download the thumbnail and save it
     * 
     * @param iconUri the remote URI of the file icon
     * @param fileIcon the local descriptor
     * @throws NetworkException
     * @throws FileAccessException
     */
    public void downloadThumbnail(Uri iconUri, MmContent fileIcon) throws NetworkException,
            FileAccessException {
        if (sLogger.isActivated()) {
            sLogger.debug("Download file icon from ".concat(getHttpServerAddr().toString()));
        }
        if (isHttpTraceEnabled()) {
            System.out.println(">>> Send HTTP request:\nGET " + iconUri);
        }
        ByteArrayOutputStream baos = null;
        try {
            baos = getThumbnail(new URL(iconUri.toString()));
            /* Save data to file on disk */
            fileIcon.writeData2File(baos.toByteArray());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(new StringBuilder(
                    "Failed to download thumbnail for uri : ").append(iconUri).toString(), e);

        } finally {
            CloseableUtils.tryToClose(baos);
            if (fileIcon != null) {
                fileIcon.closeFile();
            }
        }
    }

    /**
     * Get the thumbnail
     * 
     * @param url the URL of the file icon on the content server
     * @throws NetworkException
     */
    private ByteArrayOutputStream getThumbnail(URL url) throws NetworkException {
        HttpURLConnection urlConnection = null;
        ByteArrayOutputStream bOutputStream = null;
        try {
            urlConnection = openHttpConnection(url, new HashMap<String, String>());
            int statusCode = urlConnection.getResponseCode();
            String message = urlConnection.getResponseMessage();
            if (sLogger.isActivated()) {
                sLogger.debug("HTTP get thumbnail response: " + statusCode + " (" + message + ")");
            }
            if (isHttpTraceEnabled()) {
                System.out.println("<<< Receive HTTP response:\n" + statusCode + " " + statusCode);
            }
            if (HttpURLConnection.HTTP_OK == statusCode) {
                byte[] buffer = new byte[CHUNK_MAX_SIZE];
                bOutputStream = new ByteArrayOutputStream();
                InputStream input = urlConnection.getInputStream();
                int num;
                while ((num = input.read(buffer)) != -1) {
                    bOutputStream.write(buffer, 0, num);
                    if (isCancelled()) {
                        break;
                    }
                }
                return bOutputStream;
            }
            throw new NetworkException(new StringBuilder("Invalid statuscode '").append(statusCode)
                    .append("' received from server!").toString());

        } catch (IOException e) {
            throw new NetworkException("Failed to get thumbnail!", e);

        } finally {
            CloseableUtils.tryToClose(bOutputStream);
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    /**
     * Resume FToHTTP download
     * 
     * @throws FileNotFoundException
     * @throws IOException
     * @throws FileNotDownloadedException
     * @throws NetworkException
     */
    public void resumeDownload() throws FileNotFoundException, IOException,
            FileNotDownloadedException, NetworkException {
        if (mFileDownloadStream == null) {
            mFileDownloadStream = openStreamForFile(mFile);
        }
        resumeTransfer();
        Uri serverAddress = getHttpServerAddr();
        if (sLogger.isActivated()) {
            sLogger.debug("Resume Download file " + serverAddress + " from byte " + mFile.length());
        }

        /* Send GET request */
        long downloadedLength = mFile.length();
        long completeSize = mContent.getSize();
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("Range", "bytes=" + downloadedLength + "-" + completeSize);
        if (isHttpTraceEnabled()) {
            System.out.println(">>> Send HTTP request:\n GET " + serverAddress);
        }

        try {
            writeHttpContentToFile(new URL(serverAddress.toString()), properties);
        } catch (SSLHandshakeException e) {
            /*
             * If there are issues during handshake between UE and server then we should not proceed
             * any further with file download, we should immediately cancel the transfer, One on the
             * possible case would be a certificate mismatch
             */
            throw e;
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
     * checks if the stream is already available for usage
     * 
     * @return True if Stream is NOT NULL
     */
    /* package private */boolean isFileStreamAllocated() {
        return mFileDownloadStream != null;
    }
}
