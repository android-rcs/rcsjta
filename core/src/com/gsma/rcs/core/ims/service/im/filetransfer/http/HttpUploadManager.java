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

import com.gsma.rcs.core.CoreException;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.http.HttpAuthenticationAgent;
import com.gsma.rcs.core.ims.protocol.sip.SipPayloadException;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.CloseableUtils;
import com.gsma.rcs.utils.logger.Logger;

import android.net.Uri;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.xml.parsers.ParserConfigurationException;

import javax2.sip.InvalidArgumentException;

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

    /**
     * File content to upload
     */
    private MmContent mContent;

    /**
     * Fileicon to upload
     */
    private MmContent mFileIcon;

    /**
     * TID of the upload
     */
    private String mTId;

    /**
     * TID flag
     */
    private boolean mTIdFlag = true;

    /**
     * Authentication flag
     */
    private boolean mAuthenticationFlag = true;

    /**
     * The targeted URL
     */
    private URL mUrl;

    /**
     * Retry counter
     */
    private int mRetryCount = 0;

    /**
     * Http Authentication Agent
     */
    private HttpAuthenticationAgent mAuth;
    /**
     * The logger
     */
    private final Logger mLogger = Logger.getLogger(getClass().getSimpleName());

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

    /**
     * Upload a file
     * 
     * @return XML result or null if fails
     * @throws URISyntaxException
     * @throws IOException
     */
    public byte[] uploadFile() throws IOException, URISyntaxException {
        if (mLogger.isActivated()) {
            mLogger.debug("Upload file ".concat(mContent.getUri().toString()));
        }
        /* Send a first POST request */
        HttpPost post = generatePost();
        HttpResponse resp = executeRequest(post);

        /* Check response status code */
        int statusCode = resp.getStatusLine().getStatusCode();
        if (mLogger.isActivated()) {
            mLogger.debug("First POST response: " + resp.getStatusLine());
        }
        switch (statusCode) {
            case HttpStatus.SC_UNAUTHORIZED:
                /* AUTHENTICATION REQUIRED: 401 */
                mAuthenticationFlag = true;
                break;
            case HttpStatus.SC_NO_CONTENT:
                /* NO CONTENT : 204 */
                mAuthenticationFlag = false;
                break;
            case HttpStatus.SC_SERVICE_UNAVAILABLE:
                /* SERVICE_UNAVAILABLE : 503 - check retry-after header */
                Header[] headers = resp.getHeaders("Retry-After");
                long retryAfter = 0;
                if (headers.length > 0) {
                    try {
                        retryAfter = Integer.parseInt(headers[0].getValue())
                                * SECONDS_TO_MILLISECONDS_CONVERSION_RATE;
                    } catch (NumberFormatException e) {
                        /* Nothing to do */
                    }
                }
                if (retryAfter > 0) {
                    try {
                        Thread.sleep(retryAfter);
                    } catch (InterruptedException e) {
                        /* Nothing to do */
                    }
                }
                /* No break to do the retry */
            default:
                /* Retry procedure */
                if (mRetryCount < RETRY_MAX) {
                    mRetryCount++;
                    return uploadFile();
                } else {
                    throw new IOException("Unable to upload file for URI ".concat(mContent.getUri()
                            .toString()));
                }
        }

        if (isCancelled()) {
            if (mLogger.isActivated()) {
                mLogger.debug("File transfer cancelled by user");
            }
            return null;
        }
        // Notify listener
        getListener().httpTransferStarted();

        // Send a second POST request
        return sendMultipartPost(resp);
    }

    /**
     * Generate First POST
     * 
     * @return POST request
     * @throws MalformedURLException
     * @throws URISyntaxException
     * @throws UnsupportedEncodingException
     */
    private HttpPost generatePost() throws MalformedURLException, URISyntaxException,
            UnsupportedEncodingException {
        // Check server address
        mUrl = new URL(getHttpServerAddr().toString());
        String protocol = mUrl.getProtocol(); // TODO : exit if not HTTPS
        String host = mUrl.getHost();
        String serviceRoot = mUrl.getPath();

        // Build POST request
        HttpPost post = new HttpPost(new URI(protocol + "://" + host + serviceRoot));
        post.addHeader("User-Agent", SipUtils.userAgentString());
        if (HTTP_TRACE_ENABLED) {
            String trace = ">>> Send HTTP request:";
            trace += "\n " + post.getMethod() + " " + post.getRequestLine().getUri();
            System.out.println(trace);
        }

        return post;
    }

    /**
     * Create and Send the second POST
     * 
     * @param resp response of the first request
     * @return Content of the response
     * @throws CoreException
     * @throws IOException
     */
    private byte[] sendMultipartPost(HttpResponse resp) throws IOException {
        DataOutputStream outputStream = null;
        Uri file = mContent.getUri();

        // Get the connection
        HttpsURLConnection connection = null;
        HttpsURLConnection.setDefaultHostnameVerifier(new NullHostNameVerifier());
        connection = (HttpsURLConnection) mUrl.openConnection();

        connection.setSSLSocketFactory(FileTransSSLFactory.getFileTransferSSLContext()
                .getSocketFactory());

        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setReadTimeout(5000);
        connection.setChunkedStreamingMode(CHUNK_MAX_SIZE);

        // POST construction
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("User-Agent", SipUtils.userAgentString());
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary="
                + BOUNDARY_TAG);

        // Construct the Body
        String body = "";

        // Add tid
        if (mTIdFlag) {
            body += generateTidMultipart();
        }

        // Update authentication agent from response
        if (mAuthenticationFlag) {
            Header[] authHeaders = resp.getHeaders("www-authenticate");
            if (authHeaders.length == 0) {
                throw new IOException("headers malformed in 401 response");
            }
            mAuth = new HttpAuthenticationAgent(getHttpServerLogin(), getHttpServerPwd());
            mAuth.readWwwAuthenticateHeader(authHeaders[0].getValue());

            String authValue;
            try {
                authValue = mAuth.generateAuthorizationHeaderValue(connection.getRequestMethod(),
                        mUrl.getPath(), body);
                if (authValue != null) {
                    connection.setRequestProperty("Authorization", authValue);
                }
            } catch (InvalidArgumentException e) {
                throw new IOException("Error when generating authorization header value", e);
            }
        }

        // Trace
        if (HTTP_TRACE_ENABLED) {
            String trace = ">>> Send HTTP request:";
            trace += "\n " + connection.getRequestMethod() + " " + mUrl.toString();
            Map<String, List<String>> properties = connection.getRequestProperties();
            for (String property : properties.keySet()) {
                trace += "\n " + property + ": " + connection.getRequestProperty((String) property);
            }
            trace += "\n" + body;
            System.out.println(trace);
        }

        // Create the DataOutputStream and start writing its body
        outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.writeBytes(body);

        // Add file icon
        if (mFileIcon != null) {
            writeThumbnailMultipart(outputStream);
        }
        /* From this point, resuming is possible */
        ((HttpUploadTransferEventListener) getListener()).uploadStarted();
        try {
            /* Add File */
            writeFileMultipart(outputStream, file);
            if (!isCancelled()) {
                try {
                    /*
                     * if the upload is cancelled, we don't send the last boundary to get bad
                     * request
                     */
                    outputStream.writeBytes(TWO_HYPENS + BOUNDARY_TAG + TWO_HYPENS);

                    /* Check response status code */
                    int responseCode = connection.getResponseCode();
                    if (mLogger.isActivated()) {
                        mLogger.debug("Second POST response " + responseCode + " "
                                + connection.getResponseMessage());
                    }
                    byte[] result = null;
                    boolean success = false;
                    boolean retry = false;
                    if (HTTP_TRACE_ENABLED) {
                        String trace = "<<< Receive HTTP response:";
                        trace += "\n " + responseCode + " " + connection.getResponseMessage();
                        System.out.println(trace);
                    }
                    switch (responseCode) {
                        case HttpStatus.SC_OK:
                            success = true;
                            InputStream inputStream = connection.getInputStream();
                            result = convertStreamToString(inputStream);
                            inputStream.close();
                            if (HTTP_TRACE_ENABLED) {
                                System.out.println("\n " + new String(result));
                            }
                            break;
                        case HttpStatus.SC_SERVICE_UNAVAILABLE:
                            String header = connection.getHeaderField("Retry-After");
                            long retryAfter = 0;
                            if (header != null) {
                                try {
                                    retryAfter = Integer.parseInt(header)
                                            * SECONDS_TO_MILLISECONDS_CONVERSION_RATE;
                                } catch (NumberFormatException ignore) {
                                    /* Nothing to do, ignore the exception */
                                }
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
                            }
                            break;
                        default:
                            break; /* no success, no retry */
                    }

                    if (success) {
                        return result;
                    } else if (retry) {
                        return sendMultipartPost(resp);
                    } else {
                        if (mLogger.isActivated()) {
                            mLogger.warn("File Upload aborted, Received " + responseCode
                                    + " from server");
                        }
                        return null;
                    }
                } catch (IOException e) {
                    if (mLogger.isActivated()) {
                        mLogger.warn("File Upload paused due to " + e.getLocalizedMessage()
                                + " and is now in state paused.");
                    }
                    /*
                     * When there is a connection problem causing transfer terminated, state should
                     * be set to paused.
                     */
                    if (!isPaused() && !isCancelled()) {
                        pauseTransferBySystem();
                    }
                    return null;
                }
            } else {
                /* Check if user has paused transfer */
                if (isPaused()) {
                    if (mLogger.isActivated()) {
                        mLogger.debug("File transfer paused by user");
                    }
                    try {
                        /*
                         * Sent data are bufferized. Must wait for response to enable sending to
                         * server.
                         */
                        int responseCode = connection.getResponseCode();
                        if (mLogger.isActivated()) {
                            mLogger.debug("Second POST response " + responseCode + " "
                                    + connection.getResponseMessage());
                        }
                    } catch (IOException e) {
                        if (mLogger.isActivated()) {
                            mLogger.warn("File Upload aborted due to " + e.getLocalizedMessage()
                                    + " now in state pause, waiting for resume...");
                        }
                        return null;
                    }
                } else {
                    if (mLogger.isActivated()) {
                        mLogger.debug("File transfer cancelled by user");
                    }
                }
                return null;
            }
        } catch (IOException e) {
            if (mLogger.isActivated()) {
                mLogger.warn("File Upload paused due to " + e.getLocalizedMessage()
                        + " and is now in state paused.");
            }
            /*
             * When there is a connection problem causing transfer terminated, state should be set
             * to paused.
             */
            if (!isPaused() && !isCancelled()) {
                pauseTransferBySystem();
            }
            return null;
        } catch (SecurityException e) {
            if (mLogger.isActivated()) {
                mLogger.error("Upload has failed due to that the file is not accessible!", e);
            }
            getListener().httpTransferNotAllowedToSend();
            return null;
        } finally {
            /* Close streams */
            if (outputStream != null) {
                try {
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException ignore) {
                    /* Nothing to do, ignore the exception */
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Write the thumbnail multipart
     * 
     * @param outputStream DataOutputStream to write to
     */
    private void writeThumbnailMultipart(DataOutputStream outputStream) throws IOException {
        long size = mFileIcon.getSize();
        Uri fileIcon = mFileIcon.getUri();
        if (mLogger.isActivated()) {
            mLogger.debug(new StringBuilder("write file icon ").append(fileIcon).append(" (size=")
                    .append(size).append(")").toString());
        }
        if (size > 0) {
            FileInputStream fileInputStream = null;
            try {
                fileInputStream = (FileInputStream) AndroidFactory.getApplicationContext()
                        .getContentResolver().openInputStream(fileIcon);
                int bufferSize = (int) size;
                byte[] fileIconData = new byte[bufferSize];
                if (size != fileInputStream.read(fileIconData, 0, bufferSize)) {
                    throw new IOException("Unable to read fileIcon from ".concat(fileIcon
                            .toString()));
                }
                outputStream.writeBytes(new StringBuilder(TWO_HYPENS).append(BOUNDARY_TAG)
                        .append(LINE_END).toString());
                outputStream.writeBytes(new StringBuilder(
                        "Content-Disposition: form-data; name=\"Thumbnail\"; filename=\"thumb_")
                        .append(mContent.getName()).append("\"").append(LINE_END).toString());
                outputStream.writeBytes("Content-Type: image/jpeg".concat(LINE_END));
                outputStream.writeBytes("Content-Length: " + size);
                outputStream.writeBytes(LINE_END.concat(LINE_END));
                outputStream.write(fileIconData);
                outputStream.writeBytes(LINE_END);
            } finally {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            }
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
            fileInputStream = (FileInputStream) AndroidFactory.getApplicationContext()
                    .getContentResolver().openInputStream(file);
            int bytesAvailable = fileInputStream.available();
            int bufferSize = Math.min(bytesAvailable, CHUNK_MAX_SIZE);
            byte[] buffer = new byte[bufferSize];
            int bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            int progress = 0;

            while (bytesRead > 0 && !isCancelled()) {
                progress += bytesRead;
                outputStream.write(buffer, 0, bytesRead);
                bytesAvailable = fileInputStream.available();
                getListener().httpTransferProgress(progress, fileSize);
                bufferSize = Math.min(bytesAvailable, CHUNK_MAX_SIZE);
                buffer = new byte[bufferSize];
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }
        } finally {
            if (fileInputStream != null) {
                fileInputStream.close();
            }
        }
        if (!isCancelled())
            outputStream.writeBytes(LINE_END);
    }

    /**
     * Stream conversion
     * 
     * @param is Input stream
     * @return Byte array
     */
    private static byte[] convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            // Nothing to do
        } finally {
            CloseableUtils.close(is);
        }
        return sb.toString().getBytes(UTF8);
    }

    /**
     * Blank host verifier
     */
    private class NullHostNameVerifier implements HostnameVerifier {
        /**
         * Verifies that the specified hostname is allowed within the specified SSL session.
         * 
         * @param hostname Hostname to check
         * @param session Current SSL session
         * @return Always returns true
         */
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    /**
     * Resume the upload
     * 
     * @return byte[] contains the info to send to terminating side
     * @throws IOException
     * @throws URISyntaxException
     * @throws SipPayloadException
     */
    public byte[] resumeUpload() throws IOException, URISyntaxException, SipPayloadException {
        // Try to get upload info
        HttpResponse resp = null;
        resp = sendGetUploadInfo();
        resetParamForResume();

        if (resp == null) {
            if (mLogger.isActivated()) {
                mLogger.debug("Unexpected Server response, will restart upload from begining");
            }
            return uploadFile();
        }

        try {
            String content = EntityUtils.toString(resp.getEntity());
            byte[] bytes = content.getBytes(UTF8);
            if (HTTP_TRACE_ENABLED) {
                String trace = "Get Upload Info response:";
                trace += "\n " + content;
                System.out.println(trace);
            }
            FileTransferHttpResumeInfo ftResumeInfo = ChatUtils
                    .parseFileTransferHttpResumeInfo(bytes);

            if (ftResumeInfo == null) {
                if (mLogger.isActivated()) {
                    mLogger.error("Cannot parse resume info! restart upload");
                }
                return uploadFile();
            }
            if ((ftResumeInfo.getEnd() - ftResumeInfo.getStart()) >= (this.mContent.getSize() - 1)) {
                if (mLogger.isActivated()) {
                    mLogger.info("Nothing to resume: uploaded complete");
                }
                return getDownloadInfo(); // The file has already been uploaded completely
            }
            if (sendPutForResumingUpload(ftResumeInfo) != null) {
                return getDownloadInfo();
            }
            return null;

        } catch (ParserConfigurationException e) {
            throw new SipPayloadException("Unable to parse file transfer resume info!", e);

        } catch (SAXException e) {
            throw new SipPayloadException("Unable to parse file transfer resume info!", e);
        }
    }

    /**
     * Write a part of the file in a PUT request for resuming upload
     * 
     * @param resumeInfo info on already uploaded content
     * @return byte[] containing the server's response
     * @throws IOException
     * @throws MalformedURLException
     * @throws CoreException
     */
    private byte[] sendPutForResumingUpload(FileTransferHttpResumeInfo resumeInfo)
            throws IOException {
        if (mLogger.isActivated()) {
            mLogger.debug("sendPutForResumingUpload. Already sent from " + resumeInfo.getStart()
                    + " to " + resumeInfo.getEnd());
        }
        DataOutputStream outputStream = null;
        Uri file = mContent.getUri();

        // Get the connection
        HttpsURLConnection connection = null;
        HttpsURLConnection.setDefaultHostnameVerifier(new NullHostNameVerifier());
        connection = (HttpsURLConnection) new URL(resumeInfo.getUri().toString()).openConnection();

        connection.setSSLSocketFactory(FileTransSSLFactory.getFileTransferSSLContext()
                .getSocketFactory());

        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setReadTimeout(2000);

        // POST construction
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("User-Agent", SipUtils.userAgentString());
        connection.setRequestProperty("Content-Type", this.mContent.getEncoding());
        connection.setRequestProperty("Content-Length",
                String.valueOf(mContent.getSize() - (resumeInfo.getEnd() + 1)));
        // according to RFC 2616, section 14.16 the Content-Range header must contain an element
        // bytes-unit
        connection.setRequestProperty("Content-Range", "bytes " + (resumeInfo.getEnd() + 1) + "-"
                + (mContent.getSize() - 1) + "/" + mContent.getSize());

        // Construct the Body
        String body = "";
        try {
            // Update authentication agent from response
            if (mAuthenticationFlag && mAuth != null) {
                String authValue = mAuth.generateAuthorizationHeaderValue(
                        connection.getRequestMethod(), mUrl.getPath(), body);
                if (authValue != null) {
                    connection.setRequestProperty("Authorization", authValue);
                }
            }

            // Trace
            if (HTTP_TRACE_ENABLED) {
                String trace = ">>> Send HTTP request:";
                trace += "\n " + connection.getRequestMethod() + " " + mUrl.toString();
                Map<String, List<String>> properties = connection.getRequestProperties();
                for (String property : properties.keySet()) {
                    trace += "\n " + property + ": "
                            + connection.getRequestProperty((String) property);
                }
                trace += "\n" + body;
                System.out.println(trace);
            }

            // Create the DataOutputStream and start writing its body
            outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes(body);

            // Add File
            writeRemainingFileData(outputStream, file, resumeInfo.getEnd());
            if (!isCancelled()) {
                // Check response status code
                int responseCode = connection.getResponseCode();
                if (mLogger.isActivated()) {
                    mLogger.debug("PUT response " + responseCode + " "
                            + connection.getResponseMessage());
                }
                byte[] result = null;
                boolean success = false;
                boolean retry = false;
                switch (responseCode) {
                    case 200:
                        // 200 OK
                        success = true;
                        InputStream inputStream = connection.getInputStream();
                        result = convertStreamToString(inputStream);
                        inputStream.close();
                        if (HTTP_TRACE_ENABLED) {
                            System.out.println("\n" + new String(result));
                        }
                        break;
                    default:
                        break; // no success, no retry
                }

                // Close streams
                outputStream.flush();
                outputStream.close();
                connection.disconnect();

                if (success) {
                    return result;
                } else if (retry) {
                    return sendPutForResumingUpload(resumeInfo);
                } else {
                    throw new IOException("Received " + responseCode + " from server");
                }
            } else {
                if (isPaused()) {
                    if (mLogger.isActivated()) {
                        mLogger.warn("File transfer paused by user");
                    }
                    // Sent data are bufferized. Must wait for response to enable sending to server.
                    int responseCode = connection.getResponseCode();
                    if (mLogger.isActivated()) {
                        mLogger.debug("PUT response " + responseCode + " "
                                + connection.getResponseMessage());
                    }
                } else {
                    if (mLogger.isActivated()) {
                        mLogger.warn("File transfer cancelled by user");
                    }
                }
                // Close streams
                outputStream.flush();
                outputStream.close();
                connection.disconnect();
                return null;
            }
        } catch (SecurityException e) {
            mLogger.error("Upload reasume has failed due to that the file is not accessible!", e);
            getListener().httpTransferNotAllowedToSend();
            return null;
        } catch (InvalidArgumentException e) {
            throw new IOException("Error when authentication agent from response!", e);
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
        FileInputStream fileInputStream = (FileInputStream) AndroidFactory.getApplicationContext()
                .getContentResolver().openInputStream(file);
        // Skip bytes already received
        int bytesRead = (int) fileInputStream.skip(offset + 1);
        int bytesAvailable = fileInputStream.available();
        int bufferSize = Math.min(bytesAvailable, CHUNK_MAX_SIZE);
        byte[] buffer = new byte[bufferSize];
        int progress = bytesRead;
        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
        if (mLogger.isActivated()) {
            mLogger.debug("Send " + bytesAvailable + " remaining bytes starting from " + progress);
        }
        // Send remaining bytes
        while (bytesRead > 0 && !isCancelled()) {
            progress += bytesRead;
            outputStream.write(buffer, 0, bytesRead);
            bytesAvailable = fileInputStream.available();
            getListener().httpTransferProgress(progress, mContent.getSize());
            bufferSize = Math.min(bytesAvailable, CHUNK_MAX_SIZE);
            buffer = new byte[bufferSize];
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);

        }
        fileInputStream.close();
    }

    /**
     * Send a get for info on the upload
     * 
     * @param suffix String that specifies if it is for upload or download info
     * @param authRequired Boolean that indicates whether or not the request has to be authenticated
     *            with a authorization header
     * @return HttpResponse The last response of the server (401 are hidden)
     * @throws IOException
     */
    private HttpResponse sendGetInfo(String suffix, boolean authRequired) throws IOException,
            URISyntaxException {
        // Check server address
        mUrl = new URL(getHttpServerAddr().toString());
        String protocol = mUrl.getProtocol(); // TODO : exit if not HTTPS
        String host = mUrl.getHost();
        String serviceRoot = mUrl.getPath();
        HttpGet get;
        try {
            // Build POST request
            get = new HttpGet(new URI(protocol + "://" + host + serviceRoot + "?tid=" + mTId
                    + suffix));
            get.addHeader("User-Agent", SipUtils.userAgentString());
            if (authRequired && mAuth != null) {
                get.addHeader("Authorization", mAuth.generateAuthorizationHeaderValue(
                        get.getMethod(), get.getURI().toString(), ""));
            }

            // Trace
            if (HTTP_TRACE_ENABLED) {
                String trace = ">>> Send HTTP request:";
                trace += "\n " + get.getMethod() + " " + get.getRequestLine().getUri();
                Header headers[] = get.getAllHeaders();
                for (Header header : headers) {
                    trace += "\n " + header.getName() + ": " + header.getValue();
                }
                System.out.println(trace);
            }
        } catch (InvalidArgumentException e) {
            throw new IOException("Error when generating authorization header value!", e);
        }
        HttpResponse resp = executeRequest(get);

        // Check response status code
        int statusCode = resp.getStatusLine().getStatusCode();
        if (mLogger.isActivated()) {
            mLogger.debug("Get Info (" + suffix + ") Response " + resp.getStatusLine());
        }
        if (HTTP_TRACE_ENABLED) {
            String trace = "<<< Receive HTTP response:";
            trace += "\n " + resp.getStatusLine().toString();
            Header[] headers = resp.getAllHeaders();
            for (Header header : headers) {
                trace += "\n" + header.getName() + " " + header.getValue();
            }
            System.out.println(trace);
        }
        switch (statusCode) {
            case HttpStatus.SC_UNAUTHORIZED:
                if (authRequired) {
                    throw new IOException("Unexpected response from server, got " + statusCode
                            + " for the second time. Authentication rejected.");
                }
                Header[] authHeaders = resp.getHeaders("www-authenticate");
                if (authHeaders.length == 0) {
                    throw new IOException("headers malformed in 401 response");
                }
                if (mAuth == null) {
                    mAuth = new HttpAuthenticationAgent(getHttpServerLogin(), getHttpServerPwd());
                    mAuth.readWwwAuthenticateHeader(authHeaders[0].getValue());
                }
                mAuth.readWwwAuthenticateHeader(authHeaders[0].getValue());
                return sendGetInfo(suffix, true);
            case HttpStatus.SC_OK:
                return resp;
            default:
                return null;
        }
    }

    /**
     * Send a request to get the download info and bring the response
     * 
     * @return byte[] contains the response of the server to the upload
     * @throws URISyntaxException
     */
    private byte[] getDownloadInfo() throws URISyntaxException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            HttpResponse resp = sendGetDownloadInfo();

            InputStream is = resp.getEntity().getContent();

            int nRead;
            byte[] data = new byte[16384];

            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();
        } catch (IOException e) {
            if (mLogger.isActivated()) {
                mLogger.warn("Could not get upload info due to " + e.getLocalizedMessage());
            }
            getListener().httpTransferPausedBySystem();
            return null;
        }
        return buffer.toByteArray();
    }

    /**
     * Send a request to get info on the upload for download purpose on terminating
     * 
     * @return HttpResponse The last response of the server (401 are hidden)
     * @throws IOException
     * @throws URISyntaxException
     */
    private HttpResponse sendGetDownloadInfo() throws IOException, URISyntaxException {
        return sendGetInfo(DOWNLOAD_INFO_REQUEST, false);
    }

    /**
     * Send a get for info on the upload
     * 
     * @return HttpResponse The last response of the server (401 are hidden)
     * @throws IOException
     * @throws URISyntaxException
     */
    private HttpResponse sendGetUploadInfo() throws IOException, URISyntaxException {
        return sendGetInfo(UPLOAD_INFO_REQUEST, false);
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
