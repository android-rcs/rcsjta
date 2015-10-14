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

import static com.gsma.rcs.utils.StringUtils.UTF8;
import static com.gsma.rcs.utils.StringUtils.UTF8_STR;

import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.http.HttpAuthenticationAgent;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.CloseableUtils;
import com.gsma.rcs.utils.StringUtils;
import com.gsma.rcs.utils.logger.Logger;

import android.net.Uri;

import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

/**
 * HTTP upload manager
 * 
 * @author jexa7410
 * @author hhff3235
 * @author YPLO6403
 */
public class HttpUploadManager extends HttpTransferManager {
    /**
     * Rate to convert from seconds to milliseconds
     */
    private static final long SECONDS_TO_MILLISECONDS_CONVERSION_RATE = 1000;

    /**
     * Boundary tag
     */
    private final static String BOUNDARY_TAG = "boundary1";

    /**
     * Two hyphens
     */
    private final static String TWO_HYPENS = "--";

    /**
     * End of line
     */
    private final static String LINE_END = "\r\n";

    /**
     * Maximum value of retry
     */
    private final static int RETRY_MAX = 3;

    /**
     * GET is to get Download info (use for resume call flow)
     */
    private static final String DOWNLOAD_INFO_REQUEST = "&get_download_info";

    /**
     * GET is to get upload resume info
     */
    private static final String UPLOAD_INFO_REQUEST = "&get_upload_info";

    private static final int HTTP_READ_TIMEOUT = 5000;

    /**
     * File content to upload
     */
    private final MmContent mContent;

    /**
     * File icon content to upload
     */
    private final MmContent mFileIcon;

    /**
     * TID of the upload transfer
     */
    private final String mTId;

    private int mRetryCount = 0;

    private HttpAuthenticationAgent mAuth;

    private static final Logger sLogger = Logger.getLogger(HttpUploadManager.class.getSimpleName());

    /**
     * Constructor
     * 
     * @param content File content to upload
     * @param fileIcon content of the file icon
     * @param listener HTTP transfer event listener
     * @param tId TID of the upload
     * @param rcsSettings
     */
    public HttpUploadManager(MmContent content, MmContent fileIcon,
            HttpUploadTransferEventListener listener, String tId, RcsSettings rcsSettings) {
        super(listener, rcsSettings);
        mContent = content;
        mFileIcon = fileIcon;
        mTId = tId;
    }

    private long getRetryTimeout(URLConnection connection) {
        try {
            return Long.parseLong(connection.getHeaderField("Retry-After"))
                    * SECONDS_TO_MILLISECONDS_CONVERSION_RATE;
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * Upload a file
     * 
     * @return XML result or null if upload failed
     * @throws IOException
     * @throws NetworkException
     */
    public byte[] uploadFile() throws IOException, NetworkException {
        if (sLogger.isActivated()) {
            sLogger.debug("Upload file " + mContent.getUri() + " TID=" + mTId);
        }
        /* Send a first POST request */
        URL url = new URL(getHttpServerAddr().toString());
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = openHttpConnection(url, new HashMap<String, String>());
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setReadTimeout(HTTP_READ_TIMEOUT);
            urlConnection.setChunkedStreamingMode(CHUNK_MAX_SIZE);
            if (isHttpTraceEnabled()) {
                System.out.println(">>> Send HTTP request:\nPOST " + url);
            }
            int statusCode = urlConnection.getResponseCode();
            String message = urlConnection.getResponseMessage();
            /* Check response status code */
            if (sLogger.isActivated()) {
                sLogger.debug("First POST response: " + statusCode + " (" + message + ")");
            }
            switch (statusCode) {
                case HttpURLConnection.HTTP_UNAUTHORIZED:
                    /* AUTHENTICATION REQUIRED: 401 */
                    String authHeader = urlConnection.getHeaderField("www-authenticate");
                    if (StringUtils.isEmpty(authHeader)) {
                        throw new IOException("headers malformed in 401 response");
                    }
                    mAuth = new HttpAuthenticationAgent(getHttpServerLogin(), getHttpServerPwd());
                    mAuth.readWwwAuthenticateHeader(authHeader);
                    break;

                case HttpURLConnection.HTTP_NO_CONTENT:
                    /* NO CONTENT : 204 response if authentication is not required */
                    break;

                case HttpURLConnection.HTTP_UNAVAILABLE:
                    /* SERVICE_UNAVAILABLE : 503 - check retry-after header */
                    long retryAfter = getRetryTimeout(urlConnection);
                    if (retryAfter > 0) {
                        try {
                            Thread.sleep(retryAfter);
                        } catch (InterruptedException e) {
                            /* Nothing to do */
                        }
                    }
                    /* No break to do the retry */
                    //$FALL-THROUGH$
                default:
                    /* Retry procedure */
                    if (mRetryCount < RETRY_MAX) {
                        mRetryCount++;
                        return uploadFile();
                    }
                    throw new IOException("Unable to upload file URI " + mContent.getUri() + "!");
            }

            if (isCancelled()) {
                if (sLogger.isActivated()) {
                    sLogger.debug("File transfer cancelled by user");
                }
                return null;
            }
            /* Send a second POST request */
            return sendMultipartPost(url);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    /**
     * Create and Send the second POST
     * 
     * @return byte[] the response containing the download file
     * @throws IOException
     * @throws NetworkException
     */
    private byte[] sendMultipartPost(URL url) throws IOException, NetworkException {
        boolean httpTraceEnabled = isHttpTraceEnabled();
        DataOutputStream outputStream = null;
        HttpURLConnection connection = null;
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Connection", "Keep-Alive");
        headers.put("Content-Type", "multipart/form-data; boundary=" + BOUNDARY_TAG);
        try {
            connection = openHttpConnection(url, headers);
            connection.setDoInput(true);
            connection.setReadTimeout(HTTP_READ_TIMEOUT);
            connection.setChunkedStreamingMode(CHUNK_MAX_SIZE);
            connection.setRequestMethod("POST");

            /* Construct the Body */
            String body = generateTidMultipart();

            /* Update authentication agent */
            if (mAuth != null) {
                String authValue = mAuth.generateAuthorizationHeaderValue("POST", url.getPath(),
                        body);
                connection.setRequestProperty("Authorization", authValue);
            }
            if (httpTraceEnabled) {
                StringBuilder trace = new StringBuilder(">>> Send HTTP request:\nPOST ")
                        .append(url);
                Map<String, List<String>> properties = connection.getRequestProperties();
                for (Entry<String, List<String>> property : properties.entrySet()) {
                    trace.append("\n").append(property.getKey()).append(": ")
                            .append(property.getValue());
                }
                trace.append("\n").append(body);
                System.out.println(trace);
            }

            /* Create the DataOutputStream and start writing its body */
            outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes(body);

            /* Add file icon */
            if (mFileIcon != null && mFileIcon.getSize() > 0) {
                writeThumbnailMultipart(outputStream);
            }
            HttpTransferEventListener listeners = getListener();

            /* Save Transfer ID into provider: from this point, resuming is possible. */
            ((HttpUploadTransferEventListener) listeners).uploadStarted();

            /*
             * Upload resume can only be managed by HTTP Content Server if a transaction id (TID)
             * has been defined during initial upload.
             */
            listeners.onHttpTransferStarted();

            try {
                /* Add File */
                writeFileMultipart(outputStream, mContent.getUri());
                if (isCancelled() || isPaused()) {
                    return null;
                }
                /*
                 * if the upload is cancelled or paused, we don't send the last boundary to get bad
                 * request
                 */
                outputStream.writeBytes(TWO_HYPENS + BOUNDARY_TAG + TWO_HYPENS);

                /* Check response status code */
                int responseCode = connection.getResponseCode();
                String message = connection.getResponseMessage();
                if (sLogger.isActivated()) {
                    sLogger.debug("Second POST response " + responseCode + " (" + message + ")");
                }
                byte[] result = null;
                boolean success = false;
                boolean retry = false;
                if (httpTraceEnabled) {
                    String trace = "<<< Receive HTTP response:" + responseCode + " " + message;
                    System.out.println(trace);
                }
                switch (responseCode) {
                    case HttpURLConnection.HTTP_OK:
                        success = true;
                        InputStream inputStream = connection.getInputStream();
                        result = convertStreamToString(inputStream);
                        if (httpTraceEnabled) {
                            System.out.println("\n" + new String(result));
                        }
                        break;
                    case HttpURLConnection.HTTP_UNAVAILABLE:
                        long retryAfter = getRetryTimeout(connection);
                        if (retryAfter > 0) {
                            try {
                                Thread.sleep(retryAfter);
                                /* Retry procedure */
                                if (mRetryCount < RETRY_MAX) {
                                    mRetryCount++;
                                    retry = true;
                                }
                            } catch (InterruptedException ignore) {
                                /* Nothing to do, ignore the exception */
                            }
                        }
                        break;
                    default:
                        break; /* no success, no retry */
                }
                if (success) {
                    return result;
                } else if (retry) {
                    return sendMultipartPost(url);
                } else {
                    if (sLogger.isActivated()) {
                        sLogger.warn("File Upload aborted, Received " + responseCode
                                + " from server");
                    }
                    return null;
                }

            } catch (IOException e) {
                /*
                 * When there is a connection problem causing transfer terminated, state should be
                 * set to paused.
                 */
                if (!isPaused() && !isCancelled()) {
                    pauseTransferBySystem();
                }
                throw e;

            } catch (SecurityException e) {
                /*
                 * Note! This is needed since this can be called during dequeuing.
                 */
                if (sLogger.isActivated()) {
                    sLogger.error("Upload has failed due to that the file is not accessible!", e);
                }
                listeners.onHttpTransferNotAllowedToSend();
                return null;
            }
        } finally {
            CloseableUtils.tryToClose(outputStream);
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Write the thumbnail multipart
     * 
     * @param outputStream DataOutputStream to write to
     * @throws IOException
     */
    private void writeThumbnailMultipart(DataOutputStream outputStream) throws IOException {
        long size = mFileIcon.getSize();
        Uri fileIcon = mFileIcon.getUri();
        if (sLogger.isActivated()) {
            sLogger.debug("Write file icon " + fileIcon + " (size=" + size + ")");
        }
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = (FileInputStream) AndroidFactory.getApplicationContext()
                    .getContentResolver().openInputStream(fileIcon);
            int bufferSize = (int) size;
            byte[] fileIconData = new byte[bufferSize];
            if (size != fileInputStream.read(fileIconData, 0, bufferSize)) {
                throw new IOException("Unable to read fileIcon from '" + fileIcon + "'!");
            }
            outputStream.writeBytes(new StringBuilder(TWO_HYPENS).append(BOUNDARY_TAG)
                    .append(LINE_END).toString());
            outputStream.writeBytes(new StringBuilder(
                    "Content-Disposition: form-data; name=\"Thumbnail\"; filename=\"thumb_")
                    .append(mContent.getName()).append("\"").append(LINE_END).toString());
            outputStream.writeBytes("Content-Type: image/jpeg".concat(LINE_END));
            outputStream.writeBytes("Content-Length: ".concat(Long.toString(size)));
            outputStream.writeBytes(LINE_END.concat(LINE_END));
            outputStream.write(fileIconData);
            outputStream.writeBytes(LINE_END);
        } finally {
            CloseableUtils.tryToClose(fileInputStream);
        }
    }

    /**
     * Generate the TID multipart
     * 
     * @return tid TID header
     */
    private String generateTidMultipart() {
        return new StringBuilder(TWO_HYPENS).append(BOUNDARY_TAG).append(LINE_END)
                .append("Content-Disposition: form-data; name=\"tid\"").append(LINE_END)
                .append("Content-Type: text/plain").append(LINE_END).append("Content-Length: ")
                .append(mTId.length()).append(LINE_END).append(LINE_END).append(mTId)
                .append(LINE_END).toString();
    }

    /**
     * Write the file multipart
     * 
     * @param outputStream DataOutputStream to write to
     * @param file File Uri
     * @throws IOException
     */
    private void writeFileMultipart(DataOutputStream outputStream, Uri file) throws IOException {
        // Check file path
        String filename = mContent.getName();
        long fileSize = mContent.getSize();

        // Build and write headers
        StringBuilder filePartHeader = new StringBuilder(TWO_HYPENS).append(BOUNDARY_TAG)
                .append(LINE_END)
                .append("Content-Disposition: form-data; name=\"File\"; filename=\"")
                .append(URLEncoder.encode(filename, UTF8_STR)).append("\"").append(LINE_END)
                .append("Content-Type: ").append(mContent.getEncoding()).append(LINE_END)
                .append("Content-Length: ").append(fileSize).append(LINE_END).append(LINE_END);
        outputStream.writeBytes(filePartHeader.toString());

        // Write file content
        InputStream fileInputStream = null;
        try {
            fileInputStream = AndroidFactory.getApplicationContext().getContentResolver()
                    .openInputStream(file);
            int bytesAvailable = fileInputStream.available();
            int bufferSize = Math.min(bytesAvailable, CHUNK_MAX_SIZE);
            byte[] buffer = new byte[bufferSize];
            int bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            int progress = 0;

            while (bytesRead > 0 && !isCancelled() && !isPaused()) {
                progress += bytesRead;
                outputStream.write(buffer, 0, bytesRead);
                bytesAvailable = fileInputStream.available();
                getListener().onHttpTransferProgress(progress, fileSize);
                bufferSize = Math.min(bytesAvailable, CHUNK_MAX_SIZE);
                buffer = new byte[bufferSize];
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }
        } finally {
            CloseableUtils.tryToClose(fileInputStream);
        }
        if (!isCancelled()) {
            outputStream.writeBytes(LINE_END);
        }
    }

    /**
     * Stream conversion
     * 
     * @param is Input stream
     * @return Byte array
     * @throws IOException
     */
    private static byte[] convertStreamToString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString().getBytes(UTF8);

        } finally {
            CloseableUtils.tryToClose(is);
        }
    }

    /**
     * Resume the upload
     * 
     * @return byte[] contains the info to send to terminating side
     * @throws IOException
     * @throws PayloadException
     * @throws NetworkException
     */
    public byte[] resumeUpload() throws IOException, PayloadException, NetworkException {
        if (sLogger.isActivated()) {
            sLogger.debug("User resumes transfer (TID=" + mTId + ")");
        }
        /* Try to get upload info */
        byte[] resp = sendGetInfo(UPLOAD_INFO_REQUEST, false);
        resumeTransfer();

        if (resp == null) {
            if (sLogger.isActivated()) {
                sLogger.debug("Unexpected Server response, will restart upload from beginning");
            }
            return uploadFile();
        }

        try {
            if (isHttpTraceEnabled()) {
                String trace = "Get Upload Info response:\n".concat(resp.toString());
                System.out.println(trace);
            }
            FileTransferHttpResumeInfo ftResumeInfo = ChatUtils
                    .parseFileTransferHttpResumeInfo(resp);

            if (ftResumeInfo == null) {
                sLogger.error("Cannot parse resume info! restart upload");
                return uploadFile();
            }
            if ((ftResumeInfo.getEnd() - ftResumeInfo.getStart()) >= (mContent.getSize() - 1)) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Nothing to resume: uploaded complete");
                }
                return sendGetDownloadInfo(); /* The file has already been uploaded completely */
            }
            if (sendPutForResumingUpload(ftResumeInfo) != null) {
                return sendGetDownloadInfo();
            }
            return null;

        } catch (ParserConfigurationException e) {
            throw new PayloadException("Unable to parse file transfer resume info!", e);

        } catch (SAXException e) {
            throw new PayloadException("Unable to parse file transfer resume info!", e);
        }
    }

    /**
     * Write a part of the file in a PUT request for resuming upload
     * 
     * @param resumeInfo info on already uploaded content
     * @return byte[] containing the server's response
     * @throws IOException
     * @throws NetworkException
     */
    private byte[] sendPutForResumingUpload(FileTransferHttpResumeInfo resumeInfo)
            throws IOException, NetworkException {
        int endByte = resumeInfo.getEnd();
        long totalSize = mContent.getSize();
        if (sLogger.isActivated()) {
            sLogger.debug("sendPutForResumingUpload. Already sent from " + resumeInfo.getStart()
                    + " to " + endByte);
        }
        URL url = new URL(resumeInfo.getUri().toString());
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("Connection", "Keep-Alive");
        properties.put("Content-Type", mContent.getEncoding());
        properties.put("Content-Length", String.valueOf(totalSize - (endByte + 1)));
        /*
         * According to RFC 2616, section 14.16 the Content-Range header must contain an element
         * bytes-unit.
         */
        properties.put("Content-Range", "bytes " + (endByte + 1) + "-" + (totalSize - 1) + "/"
                + totalSize);
        if (mAuth != null) {
            String authValue = mAuth.generateAuthorizationHeaderValue("PUT", url.getPath(), "");
            properties.put("Authorization", authValue);
        }
        DataOutputStream outputStream = null;
        HttpURLConnection connection = null;
        boolean httpTraceEnabled = isHttpTraceEnabled();
        try {
            connection = openHttpConnection(url, properties);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setReadTimeout(HTTP_READ_TIMEOUT);
            connection.setRequestMethod("PUT");

            String body = "";
            // Update authentication agent from response

            if (httpTraceEnabled) {
                StringBuilder trace = new StringBuilder(">>> Send HTTP request:\nPUT ").append(url);
                Map<String, List<String>> headers = connection.getRequestProperties();
                for (Entry<String, List<String>> property : headers.entrySet()) {
                    trace.append("\n").append(property.getKey()).append(": ")
                            .append(property.getValue());
                }
                trace.append("\n").append(body);
                System.out.println(trace);
            }

            // Create the DataOutputStream and start writing its body
            outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes(body);

            // Add File
            writeRemainingFileData(outputStream, mContent.getUri(), endByte);
            if (!isCancelled()) {
                // Check response status code
                int responseCode = connection.getResponseCode();
                String message = connection.getResponseMessage();
                if (sLogger.isActivated()) {
                    sLogger.debug("PUT response " + responseCode + " (" + message + ")");
                }
                byte[] result = null;
                boolean success = false;
                boolean retry = false;
                switch (responseCode) {
                    case HttpURLConnection.HTTP_OK:
                        success = true;
                        InputStream inputStream = connection.getInputStream();
                        result = convertStreamToString(inputStream);
                        if (httpTraceEnabled) {
                            System.out.println("\n" + new String(result));
                        }
                        break;
                    default:
                        break; // no success, no retry
                }
                if (success) {
                    return result;
                } else if (retry) {
                    return sendPutForResumingUpload(resumeInfo);
                } else {
                    throw new IOException("Received " + responseCode + " from server");
                }
            } else if (isPaused()) {
                if (sLogger.isActivated()) {
                    sLogger.warn("File transfer paused by user");
                }
                // Sent data are bufferized. Must wait for response to enable sending to
                // server.
                int responseCode = connection.getResponseCode();
                if (sLogger.isActivated()) {
                    sLogger.debug("PUT response " + responseCode + " "
                            + connection.getResponseMessage());
                }
            } else {
                if (sLogger.isActivated()) {
                    sLogger.warn("File transfer cancelled by user");
                }
            }
            return null;

        } catch (SecurityException e) {
            /*
             * Note! This is needed since this can be called during dequeuing.
             */
            sLogger.error("Upload reasume has failed due to that the file is not accessible!", e);
            getListener().onHttpTransferNotAllowedToSend();
            return null;
        } finally {
            CloseableUtils.tryToClose(outputStream);
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * write remaining file data
     * 
     * @param outputStream the output stream
     * @param file the Uri of file to be uploaded
     * @param endingByte the offset in bytes
     * @throws IOException
     */
    private void writeRemainingFileData(DataOutputStream outputStream, Uri file, int offset)
            throws IOException {
        // Write file content
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = (FileInputStream) AndroidFactory.getApplicationContext()
                    .getContentResolver().openInputStream(file);
            // Skip bytes already received
            int bytesRead = (int) fileInputStream.skip(offset + 1);
            int bytesAvailable = fileInputStream.available();
            int bufferSize = Math.min(bytesAvailable, CHUNK_MAX_SIZE);
            byte[] buffer = new byte[bufferSize];
            int progress = bytesRead;
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            if (sLogger.isActivated()) {
                sLogger.debug("Send " + bytesAvailable + " remaining bytes starting from "
                        + progress);
            }
            // Send remaining bytes
            while (bytesRead > 0 && !isCancelled() && !isPaused()) {
                progress += bytesRead;
                outputStream.write(buffer, 0, bytesRead);
                bytesAvailable = fileInputStream.available();
                getListener().onHttpTransferProgress(progress, mContent.getSize());
                bufferSize = Math.min(bytesAvailable, CHUNK_MAX_SIZE);
                buffer = new byte[bufferSize];
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

            }
        } finally {
            CloseableUtils.tryToClose(fileInputStream);
        }
    }

    /**
     * Send a get for info on the upload
     * 
     * @param suffix String that specifies if it is for upload or download info
     * @param authRequired Boolean that indicates whether or not the request has to be authenticated
     *            with a authorization header
     * @return byte[] contains the response of the server or null (401 UNAUTHORIZED is hidden)
     * @throws IOException
     * @throws NetworkException
     */
    private byte[] sendGetInfo(String suffix, boolean authRequired) throws IOException,
            NetworkException {
        URL url = new URL(getHttpServerAddr().toString());
        String protocol = url.getProtocol();
        String host = url.getHost();
        String path = url.getPath();
        String query = "tid=" + mTId + suffix;
        Uri uri = new Uri.Builder().scheme(protocol).encodedAuthority(host).encodedPath(path)
                .encodedQuery(query).build();
        url = new URL(uri.toString());

        Map<String, String> properties = new HashMap<String, String>();
        if (authRequired && mAuth != null) {
            String authValue = mAuth.generateAuthorizationHeaderValue("GET", url.getPath(), "");
            properties.put("Authorization", authValue);
        }
        HttpURLConnection connection = null;
        boolean httpTraceEnabled = isHttpTraceEnabled();
        try {
            connection = openHttpConnection(url, properties);
            connection.setReadTimeout(HTTP_READ_TIMEOUT);
            if (httpTraceEnabled) {
                StringBuilder trace = new StringBuilder(">>> Send HTTP request:\nGET ").append(url);
                Map<String, List<String>> headers = connection.getHeaderFields();
                for (Entry<String, List<String>> header : headers.entrySet()) {
                    trace.append("\n").append(header.getKey()).append(" ")
                            .append(header.getValue());
                }
                System.out.println(trace);
            }
            int statusCode = connection.getResponseCode();
            String message = connection.getResponseMessage();
            if (sLogger.isActivated()) {
                sLogger.debug("Get info (" + suffix + ") Response: " + statusCode + "(" + message
                        + ")");
            }
            if (httpTraceEnabled) {
                StringBuilder trace = new StringBuilder("<<< Receive HTTP response: ")
                        .append(statusCode).append(" ").append(message);
                Map<String, List<String>> headers = connection.getHeaderFields();
                for (Entry<String, List<String>> header : headers.entrySet()) {
                    trace.append("\n").append(header.getKey()).append(" ")
                            .append(header.getValue());
                }
                System.out.println(trace);
            }
            switch (statusCode) {
                case HttpURLConnection.HTTP_UNAUTHORIZED:
                    if (authRequired) {
                        throw new IOException("Unexpected response from server, got " + statusCode
                                + " for the second time. Authentication rejected.");
                    }
                    String authHeader = connection.getHeaderField("www-authenticate");
                    if (StringUtils.isEmpty(authHeader)) {
                        throw new IOException("headers malformed in 401 response");
                    }
                    if (mAuth == null) {
                        mAuth = new HttpAuthenticationAgent(getHttpServerLogin(),
                                getHttpServerPwd());
                    }
                    mAuth.readWwwAuthenticateHeader(authHeader);
                    return sendGetInfo(suffix, true);

                case HttpURLConnection.HTTP_OK:
                    String resp = readStream(connection.getInputStream());
                    return resp.getBytes(UTF8);

                default:
                    return null;
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String readStream(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(in, UTF8), CHUNK_MAX_SIZE);
            for (String line = r.readLine(); line != null; line = r.readLine()) {
                sb.append(line);
            }
            return sb.toString();
        } finally {
            CloseableUtils.tryToClose(in);
        }
    }

    /**
     * Send a request to get info on the upload for download purpose on terminating
     * 
     * @return byte[] contains the response of the server to the upload
     * @throws NetworkException
     * @throws IOException
     */
    private byte[] sendGetDownloadInfo() throws IOException, NetworkException {
        return sendGetInfo(DOWNLOAD_INFO_REQUEST, false);
    }

    /**
     * Gets TId
     * 
     * @return TId
     */
    public String getTId() {
        return mTId;
    }

}
