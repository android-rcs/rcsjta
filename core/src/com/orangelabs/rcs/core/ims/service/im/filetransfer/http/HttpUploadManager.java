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
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;

import android.net.Uri;

import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.http.HttpAuthenticationAgent;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatUtils;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.utils.CloseableUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * HTTP upload manager
 * 
 * @author jexa7410
 * @author hhff3235
 * @author YPLO6403
 *
 */
public class HttpUploadManager extends HttpTransferManager {
	/**
	 * Boundary tag
	 */
	private final static String BOUNDARY_TAG = "boundary1";

	/**
	 * Two hyphens
	 */
	private final static String twoHyphens = "--";

	/**
	 * End of line
	 */
	private final static String lineEnd = "\r\n";

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
	private MmContent content;

	/**
	 * Fileicon to upload
	 */
	private MmContent fileIcon;

	/**
	 * TID of the upload
	 */
	private String mTId;

	/**
	 * TID flag
	 */
	private boolean tIdFlag = true;

	/**
	 * Authentication flag
	 */
	private boolean authenticationFlag = true;

	/**
	 * The targeted URL
	 */
	private URL url;

	/**
	 * Retry counter
	 */
	private int retryCount = 0;

	/**
	 * Http Authentication Agent
	 */
	private HttpAuthenticationAgent auth;
	/**
	 * The logger
	 */
	private final static Logger logger = Logger.getLogger(HttpUploadManager.class.getSimpleName());

	/**
	 * Constructor
	 * 
	 * @param content
	 *            File content to upload
	 * @param fileIcon
	 *            content of the file icon
	 * @param listener
	 *            HTTP transfer event listener
	 * @param tId
	 *            TID of the upload
	 */
	public HttpUploadManager(MmContent content, MmContent fileIcon,
			HttpUploadTransferEventListener listener, String tId) {
		super(listener);
		this.content = content;
		this.fileIcon = fileIcon;
		mTId = tId;
	}

	/**
	 * Upload a file
	 * 
	 * @return XML result or null if fails
	 */
	public byte[] uploadFile() {
		try {
			if (logger.isActivated()) {
				logger.debug("Upload file " + content.getUri());
			}

			// Send a first POST request
			HttpPost post = generatePost();
			HttpResponse resp = executeRequest(post);

			// Check response status code
			int statusCode = resp.getStatusLine().getStatusCode();
			if (logger.isActivated()) {
				logger.debug("First POST response: " + resp.getStatusLine());
			}
			switch (statusCode) {
			case 401:
				// AUTHENTICATION REQUIRED
				authenticationFlag = true;
				break;
			case 204:
				// NO CONTENT
				authenticationFlag = false;
				break;
			case 503:
				// INTERNAL ERROR - check retry-after header
				Header[] headers = resp.getHeaders("Retry-After");
				int retryAfter = 0;
				if (headers.length > 0) {
					try {
						retryAfter = Integer.parseInt(headers[0].getValue());
					} catch (NumberFormatException e) {
						// Nothing to do
					}
				}
				if (retryAfter > 0) {
					try {
						Thread.sleep(retryAfter * 1000);
					} catch (InterruptedException e) {
						// Nothing to do
					}
				}
				// No break to do the retry
			default:
				// Retry procedure
				if (retryCount < RETRY_MAX) {
					retryCount++;
					return uploadFile();
				} else {
					return null;
				}
			}

			// Notify listener
			getListener().httpTransferStarted();

			// Send a second POST request
			return sendMultipartPost(resp);
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Upload file has failed", e);
			}
			return null;
		}
	}

	/**
	 * Generate First POST
	 * 
	 * @return POST request
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 * @throws UnsupportedEncodingException
	 */
	private HttpPost generatePost() throws MalformedURLException, URISyntaxException, UnsupportedEncodingException {
		// Check server address
		url = new URL(getHttpServerAddr().toString());
		String protocol = url.getProtocol(); // TODO : exit if not HTTPS
		String host = url.getHost();
		String serviceRoot = url.getPath();

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
	 * @param resp
	 *            response of the first request
	 * @return Content of the response
	 * @throws CoreException
	 * @throws IOException
	 * @throws Exception
	 */
	private byte[] sendMultipartPost(HttpResponse resp) throws CoreException, IOException, Exception {
		DataOutputStream outputStream = null;
		Uri file = content.getUri();

		// Get the connection
		HttpsURLConnection connection = null;
		HttpsURLConnection.setDefaultHostnameVerifier(new NullHostNameVerifier());
		connection = (HttpsURLConnection) url.openConnection();

		try {
			connection.setSSLSocketFactory(FileTransSSLFactory.getFileTransferSSLContext().getSocketFactory());
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Failed to initiate SSL for connection:", e);
			}
		}

		connection.setDoInput(true);
		connection.setDoOutput(true);
		connection.setReadTimeout(5000);
		connection.setChunkedStreamingMode(CHUNK_MAX_SIZE);
		
		// POST construction
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Connection", "Keep-Alive");
		connection.setRequestProperty("User-Agent", SipUtils.userAgentString());
		connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY_TAG);

		// Construct the Body
		String body = "";

		// Add tid
		if (tIdFlag) {
			body += generateTidMultipart();
		}

		// Update authentication agent from response
		if (authenticationFlag) {
			Header[] authHeaders = resp.getHeaders("www-authenticate");
			if (authHeaders.length == 0) {
				throw new IOException("headers malformed in 401 response");
			}
			auth = new HttpAuthenticationAgent(getHttpServerLogin(), getHttpServerPwd());
			auth.readWwwAuthenticateHeader(authHeaders[0].getValue());

			String authValue = auth.generateAuthorizationHeaderValue(connection.getRequestMethod(), url.getPath(), body);
			if (authValue != null) {
				connection.setRequestProperty("Authorization", authValue);
			}
		}

		// Trace
		if (HTTP_TRACE_ENABLED) {
			String trace = ">>> Send HTTP request:";
			trace += "\n " + connection.getRequestMethod() + " " + url.toString();
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
		if (fileIcon != null) {
			writeThumbnailMultipart(outputStream);
		}
		// From this point, resuming is possible
		((HttpUploadTransferEventListener)getListener()).uploadStarted();
		try {
			// Add File
			writeFileMultipart(outputStream, file);
			if (!isCancelled()) {
				// if the upload is cancelled, we don't send the last boundary to get bad request
				outputStream.writeBytes(twoHyphens + BOUNDARY_TAG + twoHyphens);
				
				// Check response status code
				int responseCode = connection.getResponseCode();
				if (logger.isActivated()) {
					logger.debug("Second POST response " + responseCode + " "+connection.getResponseMessage());
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
				case 200:
					// 200 OK
					success = true;
					InputStream inputStream = connection.getInputStream();
					result = convertStreamToString(inputStream);
					inputStream.close();
					if (HTTP_TRACE_ENABLED) {
						System.out.println("\n " + new String(result));
					}
					break;
				case 503:
					// INTERNAL ERROR
					String header = connection.getHeaderField("Retry-After");
					int retryAfter = 0;
					if (header != null) {
						try {
							retryAfter = Integer.parseInt(header);
						} catch (NumberFormatException e) {
							// Nothing to do
						}
						if (retryAfter >= 0) {
							try {
								Thread.sleep(retryAfter * 1000);
								// Retry procedure
								if (retryCount < RETRY_MAX) {
									retryCount++;
									retry = true;
								}
							} catch (InterruptedException e) {
								// Nothing to do
							}
						}
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
					return sendMultipartPost(resp);
				} else {
                    if (logger.isActivated()) {
                        logger.warn("File Upload aborted, Received " + responseCode
                                + " from server");
                    }
                    return null;
				}
			} else {
				if (isPaused()) {
					if (logger.isActivated()) {
						logger.debug("File transfer paused by user");
					}
					// Sent data are bufferized. Must wait for response to enable sending to server.
					int responseCode = connection.getResponseCode();
					if (logger.isActivated()) {
						logger.debug("Second POST response " + responseCode + " " + connection.getResponseMessage());
					}
				} else {
					if (logger.isActivated()) {
						logger.debug("File transfer cancelled by user");
					}
				}
				// Close streams
				outputStream.flush();
				outputStream.close();
				connection.disconnect();
				return null;
			}
		} catch (SecurityException e) {
			/*Note! This is needed since this can be called during dequeuing as will be implemented in CR018.*/
			if (logger.isActivated()) {
				logger.error("Upload has failed due to that the file is not accessible!", e);
			}
			getListener().httpTransferNotAllowedToSend();
			return null;
		} catch (Exception e) {
			e.printStackTrace();

			if (logger.isActivated()) {
				logger.warn("File Upload aborted due to " + e.getLocalizedMessage() + " now in state pause, waiting for resume...");
			}
			pauseTransferBySystem();
			return null;
		}
	}

	/**
	 * Write the thumbnail multipart
	 * 
	 * @param outputStream
	 *            DataOutputStream to write to
	 */
	private void writeThumbnailMultipart(DataOutputStream outputStream) throws IOException {
		if (logger.isActivated()) {
			logger.debug("write file icon " + fileIcon.getName() + " (size=" + fileIcon.getSize() + ")");
		}
		if (fileIcon.getSize() > 0) {
			outputStream.writeBytes(twoHyphens + BOUNDARY_TAG + lineEnd);
			outputStream.writeBytes("Content-Disposition: form-data; name=\"Thumbnail\"; filename=\"thumb_" + content.getName() + "\""
					+ lineEnd);
			outputStream.writeBytes("Content-Type: image/jpeg" + lineEnd);
			outputStream.writeBytes("Content-Length: " + fileIcon.getSize());
			outputStream.writeBytes(lineEnd + lineEnd);
			// Are thumbnail data available ?
			if (fileIcon.getData() != null) {
				// Thumbnail data were loaded upon creation.
				// Write thumbnail content
				outputStream.write(fileIcon.getData());
			} else {
				// Thumbnail must be loaded from file.
				FileInputStream fileInputStream = null;
				try {
					fileInputStream = (FileInputStream)AndroidFactory.getApplicationContext()
							.getContentResolver().openInputStream(fileIcon.getUri());
					byte[] buffer = new byte[(int)fileIcon.getSize()];
					int bytesRead = fileInputStream.read(buffer, 0, (int)fileIcon.getSize());
					if (bytesRead > 0) {
						outputStream.write(buffer);
					}
				} catch (Exception e) {
					if (logger.isActivated()) {
						logger.error(e.getMessage(), e);
					}
				} finally {
					if (fileInputStream != null) {
						fileInputStream.close();
					}
				}
			}
			outputStream.writeBytes(lineEnd);
		}
	}

	/**
	 * Generate the TID multipart
	 * 
	 * @return tid TID header
	 */
	private String generateTidMultipart() {
		String tidPartHeader = twoHyphens + BOUNDARY_TAG + lineEnd;
		tidPartHeader += "Content-Disposition: form-data; name=\"tid\"" + lineEnd;
		tidPartHeader += "Content-Type: text/plain" + lineEnd;
		tidPartHeader += "Content-Length: " + mTId.length();

		return tidPartHeader + lineEnd + lineEnd + mTId + lineEnd;
	}

	/**
	 * Write the file multipart
	 * 
	 * @param outputStream
	 *            DataOutputStream to write to
	 * @param file
	 *            File Uri
	 * @throws IOException
	 */
	private void writeFileMultipart(DataOutputStream outputStream, Uri file)
			throws IOException {
		// Check file path
		String filename = content.getName();
		long fileSize = content.getSize();

		// Build and write headers
		String filePartHeader = twoHyphens + BOUNDARY_TAG + lineEnd;
		filePartHeader += "Content-Disposition: form-data; name=\"File\"; filename=\"" + URLEncoder.encode(filename, "UTF-8")
				+ "\"" + lineEnd;
		filePartHeader += "Content-Type: " + content.getEncoding() + lineEnd;
		filePartHeader += "Content-Length: " + fileSize + lineEnd + lineEnd;

		outputStream.writeBytes(filePartHeader);

		// Write file content
		InputStream fileInputStream = null;
		try {
			fileInputStream = (FileInputStream)AndroidFactory.getApplicationContext()
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
		} catch (SecurityException e) {
			/*TODO: WIll be changed in CR037*/
			throw e;
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error(e.getMessage(), e);
			}
		} finally {
			if (fileInputStream != null) {
				fileInputStream.close();
			}
		}
		if (!isCancelled())
			outputStream.writeBytes(lineEnd);
	}

	/**
	 * Stream conversion
	 * 
	 * @param is
	 *            Input stream
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
		return sb.toString().getBytes();
	}

	/**
	 * Blank host verifier
	 */
	private class NullHostNameVerifier implements HostnameVerifier {
		/**
		 * Verifies that the specified hostname is allowed within the specified SSL session.
		 * 
		 * @param hostname
		 *            Hostname to check
		 * @param session
		 *            Current SSL session
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
	 * @throws ParseException
	 *             , IOException
	 */
	public byte[] resumeUpload() throws ParseException, IOException {
		// Try to get upload info
		HttpResponse resp = null;
		try {
			resp = sendGetUploadInfo();
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Could not get upload info", e);
			}
		}
		resetParamForResume();

		if (resp == null) {
			if (logger.isActivated()) {
				logger.debug("Unexpected Server response, will restart upload from begining");
			}
			return uploadFile();
		} else {
			String content = EntityUtils.toString(resp.getEntity());
			byte[] bytes = content.getBytes("UTF8");
			if (HTTP_TRACE_ENABLED) {
				String trace = "Get Upload Info response:";
				trace += "\n " + content;
				System.out.println(trace);
			}
			FileTransferHttpResumeInfo ftResumeInfo = ChatUtils.parseFileTransferHttpResumeInfo(bytes);

			if (ftResumeInfo == null) {
				if (logger.isActivated()) {
					logger.error( "Cannot parse resume info! restart upload");
				}
				return uploadFile();
			}
			if ((ftResumeInfo.getEnd() - ftResumeInfo.getStart()) >= (this.content.getSize() - 1)) {
				if (logger.isActivated()) {
					logger.info( "Nothing to resume: uploaded complete");
				}
				return getDownloadInfo(); // The file has already been uploaded completely
			}
			try {
				if (sendPutForResumingUpload(ftResumeInfo) != null)  {
					return getDownloadInfo();
				}
				return null;
			} catch (Exception e) {
				if (logger.isActivated()) {
					logger.error( "Exception occurred",e);
				}
				return null;
			}
		}
	}

	/**
	 * Write a part of the file in a PUT request for resuming upload
	 * 
	 * @param resumeInfo
	 *            info on already uploaded content
	 * @return byte[] containing the server's response
	 * @throws Exception
	 */
	private byte[] sendPutForResumingUpload(FileTransferHttpResumeInfo resumeInfo) throws Exception {
		if (logger.isActivated()) {
			logger.debug("sendPutForResumingUpload. Already sent from "+resumeInfo.getStart()+" to "+resumeInfo.getEnd());
		}
		DataOutputStream outputStream = null;
		Uri file = content.getUri();

		// Get the connection
		HttpsURLConnection connection = null;
		HttpsURLConnection.setDefaultHostnameVerifier(new NullHostNameVerifier());
		connection = (HttpsURLConnection) new URL(resumeInfo.getUri().toString()).openConnection();

		try {
			connection.setSSLSocketFactory(FileTransSSLFactory.getFileTransferSSLContext().getSocketFactory());
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Failed to initiate SSL for connection:", e);
			}
		}

		connection.setDoInput(true);
		connection.setDoOutput(true);
		connection.setReadTimeout(2000);

		// POST construction
		connection.setRequestMethod("PUT");
		connection.setRequestProperty("Connection", "Keep-Alive");
		connection.setRequestProperty("User-Agent", SipUtils.userAgentString());
		connection.setRequestProperty("Content-Type", this.content.getEncoding());
		connection.setRequestProperty("Content-Length", String.valueOf(content.getSize() - (resumeInfo.getEnd()+1)));
        // according to RFC 2616, section 14.16 the Content-Range header must contain an element bytes-unit
        connection.setRequestProperty("Content-Range", "bytes " + (resumeInfo.getEnd()+1) + "-" + (content.getSize()-1) + "/"
                + content.getSize());

		// Construct the Body
		String body = "";

		// Update authentication agent from response
		if (authenticationFlag && auth != null) {
			String authValue = auth.generateAuthorizationHeaderValue(connection.getRequestMethod(), url.getPath(), body);
			if (authValue != null) {
				connection.setRequestProperty("Authorization", authValue);
			}
		}

		// Trace
		if (HTTP_TRACE_ENABLED) {
			String trace = ">>> Send HTTP request:";
			trace += "\n " + connection.getRequestMethod() + " " + url.toString();
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

		try {
			// Add File
			writeRemainingFileData(outputStream, file, resumeInfo.getEnd());
			if (!isCancelled()) {
				// Check response status code
				int responseCode = connection.getResponseCode();
				if (logger.isActivated()) {
					logger.debug("PUT response " + responseCode+" "+connection.getResponseMessage());
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
					if (logger.isActivated()) {
						logger.warn("File transfer paused by user");
					}
					// Sent data are bufferized. Must wait for response to enable sending to server.
					int responseCode = connection.getResponseCode();
					if (logger.isActivated()) {
						logger.debug("PUT response " + responseCode + " " + connection.getResponseMessage());
					}
				} else {
					if (logger.isActivated()) {
						logger.warn("File transfer cancelled by user");
					}
				}
				// Close streams
				outputStream.flush();
				outputStream.close();
				connection.disconnect();
				return null;
			}
		} catch (SecurityException e) {
			/*Note! This is needed since this can be called during dequeuing as will be implemented in CR018.*/
			if (logger.isActivated()) {
				logger.error("Upload reasume has failed due to that the file is not accessible!", e);
			}
			getListener().httpTransferNotAllowedToSend();
			return null;
		} catch (Exception e) {
			e.printStackTrace();

			if (logger.isActivated()) {
				logger.warn("File Upload aborted due to " + e.getLocalizedMessage() + " now in state pause, waiting for resume...");
			}
			pauseTransferBySystem();
			return null;
		}
	}

	/**
	 * write remaining file data
	 * 
	 * @param outputStream
	 *            the output stream
	 * @param file
	 *            the Uri of file to be uploaded
	 * @param endingByte
	 *            the offset in bytes
	 * @throws IOException
	 */
	private void writeRemainingFileData(DataOutputStream outputStream, Uri file, int offset) throws IOException {
		// Write file content
		FileInputStream fileInputStream = (FileInputStream)AndroidFactory.getApplicationContext()
				.getContentResolver().openInputStream(file);
		// Skip bytes already received
		int bytesRead = (int) fileInputStream.skip(offset+1);
		int bytesAvailable = fileInputStream.available();
		int bufferSize = Math.min(bytesAvailable, CHUNK_MAX_SIZE);
		byte[] buffer = new byte[bufferSize];
		int progress = bytesRead;
		bytesRead = fileInputStream.read(buffer, 0, bufferSize);
		if (logger.isActivated()) {
			logger.debug("Send "+bytesAvailable+" remaining bytes starting from " + progress);
		}
		// Send remaining bytes 
		while (bytesRead > 0 && !isCancelled()) {
			progress += bytesRead;
			outputStream.write(buffer, 0, bytesRead);
			bytesAvailable = fileInputStream.available();
			getListener().httpTransferProgress(progress, content.getSize());
			bufferSize = Math.min(bytesAvailable, CHUNK_MAX_SIZE);
			buffer = new byte[bufferSize];
			bytesRead = fileInputStream.read(buffer, 0, bufferSize);
			
		}
		fileInputStream.close();
	}

	/**
	 * Send a get for info on the upload
	 * 
	 * @param suffix
	 *            String that specifies if it is for upload or download info
	 * @param authRequired
	 *            Boolean that indicates whether or not the request has to be authenticated with a authorization header
	 * @return HttpResponse The last response of the server (401 are hidden)
	 */
	private HttpResponse sendGetInfo(String suffix, boolean authRequired) throws Exception {
		// Check server address
		url = new URL(getHttpServerAddr().toString());
		String protocol = url.getProtocol(); // TODO : exit if not HTTPS
		String host = url.getHost();
		String serviceRoot = url.getPath();

		// Build POST request
		HttpGet get = new HttpGet(new URI(protocol + "://" + host + serviceRoot + "?tid=" + mTId + suffix));
		get.addHeader("User-Agent", SipUtils.userAgentString());
		if (authRequired && auth != null) {
			get.addHeader("Authorization", auth.generateAuthorizationHeaderValue(get.getMethod(), get.getURI().toString(), ""));
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

		HttpResponse resp = executeRequest(get);

		// Check response status code
		int statusCode = resp.getStatusLine().getStatusCode();
		if (logger.isActivated()) {
			logger.debug("Get Info ("+suffix+") Response " + resp.getStatusLine());
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
		case 401:
			if (authRequired) {
				throw new Exception("Unexpected response from server, got " + statusCode
						+ " for the second time. Authentication rejected.");
			}
			Header[] authHeaders = resp.getHeaders("www-authenticate");
			if (authHeaders.length == 0) {
				throw new IOException("headers malformed in 401 response");
			}
			if (auth == null) {
				auth = new HttpAuthenticationAgent(getHttpServerLogin(), getHttpServerPwd());
				auth.readWwwAuthenticateHeader(authHeaders[0].getValue());
			}
			auth.readWwwAuthenticateHeader(authHeaders[0].getValue());
			return sendGetInfo(suffix, true);
		case 200:
			return resp;
		default:
			return null;
		}
	}

	/**
	 * Send a request to get the download info and bring the response
	 * 
	 * @return byte[] contains the response of the server to the upload
	 */
	private byte[] getDownloadInfo() {
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
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.warn("Could not get upload info due to " + e.getLocalizedMessage());
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
	 */
	private HttpResponse sendGetDownloadInfo() throws Exception {
		return sendGetInfo(DOWNLOAD_INFO_REQUEST, false);
	}

	/**
	 * Send a get for info on the upload
	 * 
	 * @return HttpResponse The last response of the server (401 are hidden)
	 */
	private HttpResponse sendGetUploadInfo() throws Exception {
		return sendGetInfo(UPLOAD_INFO_REQUEST, false);
	}

	public String getTId() {
		return mTId;
	}
}
