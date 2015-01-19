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

package com.orangelabs.rcs.core.ims.service.presence.xdm;

import static com.orangelabs.rcs.utils.StringUtils.UTF8;
import static com.orangelabs.rcs.utils.StringUtils.UTF8_STR;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.xml.sax.InputSource;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.core.TerminalInfo;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.protocol.http.HttpAuthenticationAgent;
import com.orangelabs.rcs.core.ims.protocol.http.HttpDeleteRequest;
import com.orangelabs.rcs.core.ims.protocol.http.HttpGetRequest;
import com.orangelabs.rcs.core.ims.protocol.http.HttpPutRequest;
import com.orangelabs.rcs.core.ims.protocol.http.HttpRequest;
import com.orangelabs.rcs.core.ims.protocol.http.HttpResponse;
import com.orangelabs.rcs.core.ims.service.presence.PhotoIcon;
import com.orangelabs.rcs.core.ims.service.presence.directory.Folder;
import com.orangelabs.rcs.core.ims.service.presence.directory.XcapDirectoryParser;
import com.orangelabs.rcs.platform.network.NetworkFactory;
import com.orangelabs.rcs.platform.network.SocketConnection;
import com.orangelabs.rcs.utils.Base64;
import com.orangelabs.rcs.utils.ContactUtils;
import com.orangelabs.rcs.utils.HttpUtils;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * XDM manager
 *
 * @author Jean-Marc AUFFRET
 */
public class XdmManager {
	/**
	 * XDM server address
	 */
	private String xdmServerAddr;

	/**
	 * XDM server login
	 */
	private String xdmServerLogin;

	/**
	 * XDM server password
	 */
	private String xdmServerPwd;

	/**
	 * Managed documents
	 */
	private Hashtable<String, Folder> documents = new Hashtable<String, Folder>();

	/**
	 * The logger
	 */
	private Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * Constructor
	 *
	 * @param parent IMS module
	 */
	public XdmManager(ImsModule parent) {
		xdmServerAddr = ImsModule.IMS_USER_PROFILE.getXdmServerAddr();
		xdmServerLogin = ImsModule.IMS_USER_PROFILE.getXdmServerLogin();
		xdmServerPwd = ImsModule.IMS_USER_PROFILE.getXdmServerPassword();
	}

	/**
	 * Send HTTP PUT request
	 *
	 * @param request HTTP request
	 * @return HTTP response
	 * @throws CoreException
	 */
	private HttpResponse sendRequestToXDMS(HttpRequest request) throws CoreException {
		return sendRequestToXDMS(request, new HttpAuthenticationAgent(xdmServerLogin, xdmServerPwd));
	}

	/**
	 * Send HTTP PUT request with authentication
	 *
	 * @param request HTTP request
	 * @param authenticationAgent Authentication agent
	 * @return HTTP response
	 * @throws CoreException
	 */
	private HttpResponse sendRequestToXDMS(HttpRequest request, HttpAuthenticationAgent authenticationAgent) throws CoreException {
		try {
			// Send first request
			HttpResponse response = sendHttpRequest(request, authenticationAgent);

			// Analyze the response
			if (response.getResponseCode() == 401) {
				// 401 response received
				if (logger.isActivated()) {
					logger.debug("401 Unauthorized response received");
				}

				if (authenticationAgent != null) {
					// Update the authentication agent
					authenticationAgent.readWwwAuthenticateHeader(response.getHeader("www-authenticate"));
				}

				// Set the cookie from the received response
				String cookie = response.getHeader("set-cookie");
				request.setCookie(cookie);

				// Send second request with authentification header
				response = sendRequestToXDMS(request, authenticationAgent);
			} else
			if (response.getResponseCode() == 412) {
				// 412 response received
				if (logger.isActivated()) {
					logger.debug("412 Precondition failed");
				}

				// Reset the etag
				documents.remove(request.getAUID());

				// Send second request with authentification header
				response = sendRequestToXDMS(request);
			} else {
				// Other response received
				if (logger.isActivated()) {
					logger.debug(response.getResponseCode() + " response received");
				}
			}
			return response;
		} catch(CoreException e) {
			throw e;
		} catch(Exception e) {
			throw new CoreException("Can't send HTTP request: " + e.getMessage());
		}
	}

	/**
	 * Send HTTP PUT request
	 *
	 * @param request HTTP request
	 * @param authenticationAgent Authentication agent
	 * @return HTTP response
	 * @throws IOException
	 * @throws CoreException
	 */
	private HttpResponse sendHttpRequest(HttpRequest request, HttpAuthenticationAgent authenticationAgent) throws IOException, CoreException {
		// Extract host & port
		String[] parts = xdmServerAddr.substring(7).split(":|/");
		String host = parts[0];
		int port = Integer.parseInt(parts[1]);
		String serviceRoot = "";
		if (parts.length > 2) {
			serviceRoot = "/" + parts[2];
		}

		// Open connection with the XCAP server
		SocketConnection conn = NetworkFactory.getFactory().createSocketClientConnection();
		conn.open(host, port);
		InputStream is = conn.getInputStream();
		OutputStream os = conn.getOutputStream();

		// Create the HTTP request
		String requestUri = serviceRoot + request.getUrl();
		String httpRequest = request.getMethod() + " " + requestUri + " HTTP/1.1" + HttpUtils.CRLF +
				"Host: " + host + ":" + port + HttpUtils.CRLF +
				"User-Agent: " + TerminalInfo.getProductName() + " " + TerminalInfo.getProductVersion() + HttpUtils.CRLF;

		if (authenticationAgent != null) {
			// Set the Authorization header
			String authorizationHeader = authenticationAgent.generateAuthorizationHeader(
					request.getMethod(), requestUri, request.getContent());
			httpRequest += authorizationHeader + HttpUtils.CRLF;
		}

		String cookie = request.getCookie();
		if (cookie != null){
			// Set the cookie header
			httpRequest += "Cookie: " + cookie + HttpUtils.CRLF;
		}

		httpRequest += "X-3GPP-Intended-Identity: \"" + ImsModule.IMS_USER_PROFILE.getXdmServerLogin() + "\"" + HttpUtils.CRLF;

		// Set the If-match header
		Folder folder = (Folder)documents.get(request.getAUID());
		if ((folder != null) && (folder.getEntry() != null) && (folder.getEntry().getEtag() != null)) {
			httpRequest += "If-match: \"" + folder.getEntry().getEtag() + "\"" + HttpUtils.CRLF;
		}

		if (request.getContent() != null) {
			// Set the content type
			httpRequest += "Content-type: " + request.getContentType() + HttpUtils.CRLF;
			httpRequest += "Content-Length:" + request.getContentLength() + HttpUtils.CRLF + HttpUtils.CRLF;
		} else {
			httpRequest += "Content-Length: 0" + HttpUtils.CRLF + HttpUtils.CRLF;
		}

		// Write HTTP request headers
		os.write(httpRequest.getBytes(UTF8));
		os.flush();

		// Write HTTP content
		if (request.getContent() != null) {
			os.write(request.getContent().getBytes(UTF8));
			os.flush();
		}

		if (logger.isActivated()){
			if (request.getContent() != null) {
				logger.debug("Send HTTP request:\n" + httpRequest + request.getContent());
			} else {
				logger.debug("Send HTTP request:\n" + httpRequest);
			}
		}

		// Read HTTP headers response
		StringBuffer respTrace = new StringBuffer();
		HttpResponse response = new HttpResponse();
		int ch = -1;
		String line = "";
		while((ch = is.read()) != -1) {
			line += (char)ch;

			if (line.endsWith(HttpUtils.CRLF)) {
				if (line.equals(HttpUtils.CRLF)) {
					// All headers has been read
					break;
				}

				if (logger.isActivated()) {
					respTrace.append(line);
				}

				// Remove CRLF
				line = line.substring(0, line.length()-2);

				if (line.startsWith("HTTP/")) {
					// Status line
					response.setStatusLine(line);
				} else {
					// Header
					int index = line.indexOf(":");
					String name = line.substring(0, index).trim().toLowerCase();
					String value = line.substring(index+1).trim();
					response.addHeader(name, value);
				}

				line = "";
			}
		}

		// Extract content length
		int length = -1;
		try {
			String value = response.getHeader("content-length");
			length = Integer.parseInt(value);
		} catch(Exception e) {
			length = -1;
		}

		// Read HTTP content response
		if (length > 0) {
			byte[] content = new byte[length];
			int nb = -1;
			int pos = 0;
			byte[] buffer = new byte[1024];
			while((nb = is.read(buffer)) != -1) {
				System.arraycopy(buffer, 0, content, pos, nb);
				pos += nb;

				if (pos >= length) {
					// End of content
					break;
				}
			}
			if (logger.isActivated()) {
				respTrace.append(HttpUtils.CRLF).append(new String(content, UTF8));
			}
			response.setContent(content);
		}

		if (logger.isActivated()){
			logger.debug("Receive HTTP response:\n" + respTrace.toString());
		}

		// Close the connection
		is.close();
		os.close();
		conn.close();

		// Save the Etag from the received response
		String etag = response.getHeader("etag");
		if ((etag != null) && (folder != null) && (folder.getEntry() != null)) {
			folder.getEntry().setEtag(etag);
		}

		return response;
	}

	/**
	 * Initialize the XDM interface
	 */
	public void initialize() {
    	// Get the existing XCAP documents on the XDM server
		try {
			HttpResponse response = getXcapDocuments();
			if ((response != null) && response.isSuccessfullResponse()) {
				// Analyze the XCAP directory
				InputSource input = new InputSource(new ByteArrayInputStream(response.getContent()));
				XcapDirectoryParser parser = new XcapDirectoryParser(input);
				documents = parser.getDocuments();

				// Check RCS list document
				Folder folder = (Folder)documents.get("rls-services");
				if ((folder == null) || (folder.getEntry() == null)) {
					if (logger.isActivated()){
						logger.debug("The rls-services document does not exist");
					}

					// Set RCS list document
			    	setRcsList();
				} else {
					if (logger.isActivated()){
						logger.debug("The rls-services document already exists");
					}
				}

				// Check resource list document
				folder = (Folder)documents.get("resource-lists");
				if ((folder == null) || (folder.getEntry() == null)) {
					if (logger.isActivated()){
						logger.debug("The resource-lists document does not exist");
					}

					// Set resource list document
			    	setResourcesList();
				} else {
					if (logger.isActivated()){
						logger.debug("The resource-lists document already exists");
					}
				}

				// Check presence rules document
				folder = (Folder)documents.get("org.openmobilealliance.pres-rules");
				if ((folder == null) || (folder.getEntry() == null)) {
					if (logger.isActivated()){
						logger.debug("The org.openmobilealliance.pres-rules document does not exist");
					}

					// Set presence rules document
			    	setPresenceRules();
				} else {
					if (logger.isActivated()){
						logger.debug("The org.openmobilealliance.pres-rules document already exists");
					}
				}
			}
		} catch(Exception e) {
        	if (logger.isActivated()) {
        		logger.error("Can't parse the XCAP directory document", e);
        	}
		}
	}

	/**
	 * Get XCAP managed documents
	 *
	 * @return Response
	 */
	public HttpResponse getXcapDocuments() {
		try {
			if (logger.isActivated()){
				logger.info("Get XCAP documents");
			}

			// URL
			String url = "/org.openmobilealliance.xcap-directory/users/" +
				HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) + "/directory.xml";

			// Create the request
			HttpGetRequest request = new HttpGetRequest(url);

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info("XCAP documents has been read with success");
				}
			} else {
				if (logger.isActivated()){
					logger.info("Can't read XCAP documents: " + response.getResponseCode() + " error");
				}
			}
			return response;
		} catch(CoreException e) {
			if (logger.isActivated()) {
				logger.error("Can't read XCAP documents: unexpected exception", e);
			}
			return null;
		}
	}

	/**
	 * Get RCS list
	 *
	 * @return Response
	 */
	public HttpResponse getRcsList() {
		try {
			if (logger.isActivated()){
				logger.info("Get RCS list");
			}

			// URL
			String url = "/rls-services/users/" +
				HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) + "/index";

			// Create the request
			HttpGetRequest request = new HttpGetRequest(url);

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info("RCS list has been read with success");
				}
			} else {
				if (logger.isActivated()){
					logger.info("Can't read RCS list: " + response.getResponseCode() + " error");
				}
			}
			return response;
		} catch(CoreException e) {
			if (logger.isActivated()) {
				logger.error("Can't read RCS list: unexpected exception", e);
			}
			return null;
		}
	}

	/**
	 * Set RCS list
	 *
	 * @return Response
	 */
	public HttpResponse setRcsList() {
		try {
			if (logger.isActivated()){
				logger.info("Set RCS list");
			}

			// URL
			String url = "/rls-services/users/" +
				HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) + "/index";

			// Content
			String user = ImsModule.IMS_USER_PROFILE.getPublicUri();
			String resList = xdmServerAddr + "/resource-lists/users/" + HttpUtils.encodeURL(user) + "/index/~~/resource-lists/list%5B@name=%22rcs%22%5D";
			String content = new StringBuilder("<?xml version=\"1.0\" encoding=\"")
					.append(UTF8_STR)
					.append("\"?>")
					.append(HttpUtils.CRLF)
					.append("<rls-services xmlns=\"urn:ietf:params:xml:ns:rls-services\" xmlns:rl=\"urn:ietf:params:xml:ns:resource-lists\">")
					.append(HttpUtils.CRLF).append("<service uri=\"").append(user)
					.append(";pres-list=rcs\">").append(HttpUtils.CRLF).append("<resource-list>")
					.append(resList).append("</resource-list>").append(HttpUtils.CRLF)
					.append("<packages>").append(HttpUtils.CRLF)
					.append(" <package>presence</package>").append(HttpUtils.CRLF)
					.append("</packages>").append(HttpUtils.CRLF)
					.append("</service></rls-services>").toString();

			// Create the request
			HttpPutRequest request = new HttpPutRequest(url, content, "application/rls-services+xml");

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info("RCS list has been set with success");
				}
			} else {
				if (logger.isActivated()){
					logger.info("Can't set RCS list: " + response.getResponseCode() + " error");
				}
			}
			return response;
		} catch(CoreException e) {
			if (logger.isActivated()) {
				logger.error("Can't set RCS list: unexpected exception", e);
			}
			return null;
		}
	}

	/**
	 * Get resources list
	 *
	 * @return Response
	 */
	public HttpResponse getResourcesList() {
		try {
			if (logger.isActivated()){
				logger.info("Get resources list");
			}

			// URL
			String url = "/resource-lists/users/" +
				HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) + "/index";

			// Create the request
			HttpGetRequest request = new HttpGetRequest(url);

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info("Resources list has been read with success");
				}
			} else {
				if (logger.isActivated()){
					logger.info("Can't read resources list: " + response.getResponseCode() + " error");
				}
			}
			return response;
		} catch(CoreException e) {
			if (logger.isActivated()) {
				logger.error("Can't read resources list: unexpected exception", e);
			}
			return null;
		}
	}

	/**
	 * Set resources list
	 *
	 * @return Response
	 */
	public HttpResponse setResourcesList() {
		try {
			if (logger.isActivated()){
				logger.info("Set resources list");
			}

			// URL
			String url = "/resource-lists/users/" +
				HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) + "/index";

			// Content
			String user = ImsModule.IMS_USER_PROFILE.getPublicUri();
			String resList = xdmServerAddr + "/resource-lists/users/" + HttpUtils.encodeURL(user) + "/index/~~/resource-lists/list%5B";
			String content = new StringBuilder("<?xml version=\"1.0\" encoding=\"")
					.append(UTF8_STR).append("\"?>").append(HttpUtils.CRLF)
					.append("<resource-lists xmlns=\"urn:ietf:params:xml:ns:resource-lists\">")
					.append(HttpUtils.CRLF).append("<list name=\"oma_buddylist\">")
					.append(HttpUtils.CRLF).append(" <external anchor=\"").append(resList)
					.append("@name=%22rcs%22%5D\"/>").append(HttpUtils.CRLF).append("</list>")
					.append(HttpUtils.CRLF).append("<list name=\"oma_grantedcontacts\">")
					.append(HttpUtils.CRLF).append(" <external anchor=\"").append(resList)
					.append("@name=%22rcs%22%5D\"/>").append(HttpUtils.CRLF).append("</list>")
					.append(HttpUtils.CRLF).append("<list name=\"oma_blockedcontacts\">")
					.append(HttpUtils.CRLF).append(" <external anchor=\"").append(resList)
					.append("@name=%22rcs_blockedcontacts%22%5D\"/>").append(HttpUtils.CRLF)
					.append(" <external anchor=\"").append(resList)
					.append("@name=%22rcs_revokedcontacts%22%5D\"/>").append(HttpUtils.CRLF)
					.append("</list>").append(HttpUtils.CRLF).append("<list name=\"rcs\">")
					.append(HttpUtils.CRLF)
					.append(" <display-name>My presence buddies</display-name>")
					.append(HttpUtils.CRLF).append("</list>").append(HttpUtils.CRLF)
					.append("<list name=\"rcs_blockedcontacts\">").append(HttpUtils.CRLF)
					.append(" <display-name>My blocked contacts</display-name>")
					.append(HttpUtils.CRLF).append("</list>").append(HttpUtils.CRLF)
					.append("<list name=\"rcs_revokedcontacts\">").append(HttpUtils.CRLF)
					.append(" <display-name>My revoked contacts</display-name>")
					.append(HttpUtils.CRLF).append("</list>").append(HttpUtils.CRLF)
					.append("</resource-lists>").toString();

			// Create the request
			HttpPutRequest request = new HttpPutRequest(url, content, "application/resource-lists+xml");

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info("Resources list has been set with success");
				}
			} else {
				if (logger.isActivated()){
					logger.info("Can't set resources list: " + response.getResponseCode() + " error");
				}
			}
			return response;
		} catch(CoreException e) {
			if (logger.isActivated()) {
				logger.error("Can't set resources list: unexpected exception", e);
			}
			return null;
		}
	}

	/**
	 * Get presence rules
	 *
	 * @return Response
	 */
	public HttpResponse getPresenceRules() {
		try {
			if (logger.isActivated()){
				logger.info("Get presence rules");
			}

			// URL
			String url = "/org.openmobilealliance.pres-rules/users/" +
				HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) + "/pres-rules";

			// Create the request
			HttpGetRequest request = new HttpGetRequest(url);

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info("Get presence rules has been requested with success");
				}
			} else {
				if (logger.isActivated()){
					logger.info("Can't get the presence rules: " + response.getResponseCode() + " error");
				}
			}
			return response;
		} catch(CoreException e) {
			if (logger.isActivated()) {
				logger.error("Can't get the presence rules: unexpected exception", e);
			}
			return null;
		}
	}

	/**
	 * Set presence rules
	 *
	 * @return Response
	 */
	public HttpResponse setPresenceRules() {
		try {
			if (logger.isActivated()){
				logger.info("Set presence rules");
			}

			// URL
			String url = "/org.openmobilealliance.pres-rules/users/" +
				HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) + "/pres-rules";

			// Content
			String user = ImsModule.IMS_USER_PROFILE.getPublicUri();
			String blockedList = xdmServerAddr + "/resource-lists/users/" + user + "/index/~~/resource-lists/list%5B@name=%22oma_blockedcontacts%22%5D";
			String grantedList = xdmServerAddr + "/resource-lists/users/" + user + "/index/~~/resource-lists/list%5B@name=%22oma_grantedcontacts%22%5D";
			String content = new StringBuilder("<?xml version=\"1.0\" encoding=\"")
					.append(UTF8_STR)
					.append("\"?>")
					.append(HttpUtils.CRLF)
					.append("<cr:ruleset xmlns:ocp=\"urn:oma:xml:xdm:common-policy\" xmlns:pr=\"urn:ietf:params:xml:ns:pres-rules\" xmlns:cr=\"urn:ietf:params:xml:ns:common-policy\">")
					.append(HttpUtils.CRLF).append("<cr:rule id=\"wp_prs_allow_own\">")
					.append(HttpUtils.CRLF).append(" <cr:conditions>").append(HttpUtils.CRLF)
					.append("  <cr:identity><cr:one id=\"")
					.append(ImsModule.IMS_USER_PROFILE.getPublicUri()).append("\"/></cr:identity>")
					.append(HttpUtils.CRLF).append(" </cr:conditions>").append(HttpUtils.CRLF)
					.append(" <cr:actions><pr:sub-handling>allow</pr:sub-handling></cr:actions>")
					.append(HttpUtils.CRLF).append(" <cr:transformations>").append(HttpUtils.CRLF)
					.append("  <pr:provide-services><pr:all-services/></pr:provide-services>")
					.append(HttpUtils.CRLF)
					.append("  <pr:provide-persons><pr:all-persons/></pr:provide-persons>")
					.append(HttpUtils.CRLF)
					.append("  <pr:provide-devices><pr:all-devices/></pr:provide-devices>")
					.append(HttpUtils.CRLF).append("  <pr:provide-all-attributes/>")
					.append(HttpUtils.CRLF).append(" </cr:transformations>").append(HttpUtils.CRLF)
					.append("</cr:rule>").append(HttpUtils.CRLF)
					.append("<cr:rule id=\"rcs_allow_services_anonymous\">").append(HttpUtils.CRLF)
					.append(" <cr:conditions><ocp:anonymous-request/></cr:conditions>")
					.append(HttpUtils.CRLF)
					.append(" <cr:actions><pr:sub-handling>allow</pr:sub-handling></cr:actions>")
					.append(HttpUtils.CRLF).append(" <cr:transformations>").append(HttpUtils.CRLF)
					.append("  <pr:provide-services><pr:all-services/></pr:provide-services>")
					.append(HttpUtils.CRLF).append("  <pr:provide-all-attributes/>")
					.append(HttpUtils.CRLF).append(" </cr:transformations>").append(HttpUtils.CRLF)
					.append("</cr:rule>").append(HttpUtils.CRLF)
					.append("<cr:rule id=\"wp_prs_unlisted\">").append(HttpUtils.CRLF)
					.append(" <cr:conditions><ocp:other-identity/></cr:conditions>")
					.append(HttpUtils.CRLF)
					.append(" <cr:actions><pr:sub-handling>confirm</pr:sub-handling></cr:actions>")
					.append(HttpUtils.CRLF).append("</cr:rule>").append(HttpUtils.CRLF)
					.append("<cr:rule id=\"wp_prs_grantedcontacts\">").append(HttpUtils.CRLF)
					.append(" <cr:conditions>").append(HttpUtils.CRLF)
					.append(" <ocp:external-list>").append(HttpUtils.CRLF)
					.append("  <ocp:entry anc=\"").append(grantedList).append("\"/>")
					.append(HttpUtils.CRLF).append(" </ocp:external-list>").append(HttpUtils.CRLF)
					.append(" </cr:conditions>").append(HttpUtils.CRLF)
					.append(" <cr:actions><pr:sub-handling>allow</pr:sub-handling></cr:actions>")
					.append(HttpUtils.CRLF).append(" <cr:transformations>").append(HttpUtils.CRLF)
					.append("   <pr:provide-services><pr:all-services/></pr:provide-services>")
					.append(HttpUtils.CRLF)
					.append("   <pr:provide-persons><pr:all-persons/></pr:provide-persons>")
					.append(HttpUtils.CRLF)
					.append("   <pr:provide-devices><pr:all-devices/></pr:provide-devices>")
					.append(HttpUtils.CRLF).append("   <pr:provide-all-attributes/>")
					.append(HttpUtils.CRLF).append(" </cr:transformations>").append(HttpUtils.CRLF)
					.append("</cr:rule>").append(HttpUtils.CRLF)
					.append("<cr:rule id=\"wp_prs_blockedcontacts\">").append(HttpUtils.CRLF)
					.append(" <cr:conditions>").append(HttpUtils.CRLF)
					.append("  <ocp:external-list>").append(HttpUtils.CRLF)
					.append("  <ocp:entry anc=\"").append(blockedList).append("\"/>")
					.append(HttpUtils.CRLF).append(" </ocp:external-list>").append(HttpUtils.CRLF)
					.append(" </cr:conditions>").append(HttpUtils.CRLF)
					.append(" <cr:actions><pr:sub-handling>block</pr:sub-handling></cr:actions>")
					.append(HttpUtils.CRLF).append("</cr:rule>").append(HttpUtils.CRLF)
					.append("</cr:ruleset>").toString();

			// Create the request
			HttpPutRequest request = new HttpPutRequest(url, content, "application/auth-policy+xml");

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info("Presence rules has been set with success");
				}
			} else {
				if (logger.isActivated()){
					logger.info("Can't set presence rules: " + response.getResponseCode() + " error");
				}
			}
			return response;
		} catch(CoreException e) {
			if (logger.isActivated()) {
				logger.error("Can't set presence rules: unexpected exception", e);
			}
			return null;
		}
	}

	/**
	 * Add a contact to the granted contacts list
	 *
	 * @param contact Contact
	 * @return Response
	 */
	public HttpResponse addContactToGrantedList(ContactId contact) {
		try {
			if (logger.isActivated()){
				logger.info("Add " + contact + " to granted list");
			}

			// URL
			String url = "/resource-lists/users/" +
					HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) +
					"/index/~~/resource-lists/list%5B@name=%22rcs%22%5D/entry%5B@uri=%22" +
					HttpUtils.encodeURL(PhoneUtils.formatContactIdToUri(contact)) + "%22%5D";

			// Content
			String content = "<entry uri='" + contact + "'></entry>";

			// Create the request
			HttpPutRequest request = new HttpPutRequest(url, content, "application/xcap-el+xml");

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info(contact + " has been added with success to granted list");
				}
			} else {
				if (logger.isActivated()){
					logger.info("Can't add " + contact + " to granted list: " + response.getResponseCode() + " error");
				}
			}
			return response;
		} catch(CoreException e) {
			if (logger.isActivated()) {
				logger.error("Can't add " + contact + " to granted list: unexpected exception", e);
			}
			return null;
		}
	}

	/**
	 * Remove a contact from the granted contacts list
	 *
	 * @param contact Contact
	 * @return Response
	 */
	public HttpResponse removeContactFromGrantedList(ContactId contact) {
		try {
			if (logger.isActivated()){
				logger.info("Remove " + contact + " from granted list");
			}

			// URL
			String url = "/resource-lists/users/" +
					HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) +
					"/index/~~/resource-lists/list%5B@name=%22rcs%22%5D/entry%5B@uri=%22" +
					HttpUtils.encodeURL(PhoneUtils.formatContactIdToUri(contact)) + "%22%5D";

			// Create the request
			HttpDeleteRequest request = new HttpDeleteRequest(url);

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info(contact + " has been removed with success from granted list");
				}
			} else {
				if (logger.isActivated()){
					logger.info("Can't remove " + contact + " from granted list: " + response.getResponseCode() + " error");
				}
			}
			return response;
		} catch(CoreException e) {
			if (logger.isActivated()) {
				logger.error("Can't remove " + contact + " from granted list: unexpected exception", e);
			}
			return null;
		}
	}

	private Set<ContactId> convertListOfUrisToSetOfContactId(List<String> uris) {
		Set<ContactId> result = new HashSet<ContactId>();
		if (uris != null) {
			for (String uri : uris) {
				try {
					result.add(ContactUtils.createContactId(uri));
				} catch (RcsContactFormatException e) {
					if (logger.isActivated()) {
						logger.warn("Cannot parse uri "+uri);
					}
				}
			}
		}
		return result;
	}

	/**
	 * Returns the list of granted contacts
	 *
	 * @return List
	 */
	public Set<ContactId> getGrantedContacts() {
		try {
			if (logger.isActivated()){
				logger.info("Get granted contacts list");
			}

			// URL
			String url = "/resource-lists/users/" +
					HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) +
					"/index/~~/resource-lists/list%5B@name=%22rcs%22%5D";

			// Create the request
			HttpGetRequest request = new HttpGetRequest(url);

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info("Granted contacts list has been read with success");
				}

				// Parse response
				InputSource input = new InputSource(new ByteArrayInputStream(response.getContent()));
				XcapResponseParser parser = new XcapResponseParser(input);
				return convertListOfUrisToSetOfContactId(parser.getUris());
			} else {
				if (logger.isActivated()){
					logger.info("Can't get granted contacts list: " + response.getResponseCode() + " error");
				}
			}
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Can't get granted contacts list: unexpected exception", e);
			}
		}
		return new HashSet<ContactId>();
	}

	/**
	 * Add a contact to the blocked contacts list
	 *
	 * @param contact Contact
	 * @return Response
	 */
	public HttpResponse addContactToBlockedList(String contact) {
		try {
			if (logger.isActivated()){
				logger.info("Add " + contact + " to blocked list");
			}

			// URL
			String url = "/resource-lists/users/" +
					HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) +
					"/index/~~/resource-lists/list%5B@name=%22rcs_blockedcontacts%22%5D/entry%5B@uri=%22" +
					HttpUtils.encodeURL(contact) + "%22%5D";

			// Content
			String content = "<entry uri='" + contact + "'></entry>";

			// Create the request
			HttpPutRequest request = new HttpPutRequest(url, content, "application/xcap-el+xml");

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info(contact + " has been added with success to blocked list");
				}
			} else {
				if (logger.isActivated()){
					logger.info("Can't add " + contact + " to granted list: " + response.getResponseCode() + " error");
				}
			}
			return response;
		} catch(CoreException e) {
			if (logger.isActivated()) {
				logger.error("Can't add " + contact + " to blocked list: unexpected exception", e);
			}
			return null;
		}
	}

	/**
	 * Remove a contact from the blocked contacts list
	 *
	 * @param contact Contact
	 * @return Response
	 */
	public HttpResponse removeContactFromBlockedList(ContactId contact) {
		try {
			if (logger.isActivated()){
				logger.info("Remove " + contact + " from blocked list");
			}

			// URL
			String url = "/resource-lists/users/" +
					HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) +
					"/index/~~/resource-lists/list%5B@name=%22rcs_blockedcontacts%22%5D/entry%5B@uri=%22" +
					HttpUtils.encodeURL(PhoneUtils.formatContactIdToUri(contact)) + "%22%5D";

			// Create the request
			HttpDeleteRequest request = new HttpDeleteRequest(url);

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info(contact + " has been removed with success from blocked list");
				}
			} else {
				if (logger.isActivated()){
					logger.info("Can't remove " + contact + " from blocked list: " + response.getResponseCode() + " error");
				}
			}
			return response;
		} catch(CoreException e) {
			if (logger.isActivated()) {
				logger.error("Can't remove " + contact + " from blocked list: unexpected exception", e);
			}
			return null;
		}
	}

	/**
	 * Returns the list of blocked contacts
	 *
	 * @return List
	 */
	public Set<ContactId> getBlockedContacts() {
		try {
			if (logger.isActivated()){
				logger.info("Get blocked contacts list");
			}

			// URL
			String url = "/resource-lists/users/" +
					HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) +
					"/index/~~/resource-lists/list%5B@name=%22rcs_blockedcontacts%22%5D";

			// Create the request
			HttpGetRequest request = new HttpGetRequest(url);

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info("Blocked contacts list has been read with success");
				}

				// Parse response
				InputSource input = new InputSource(new ByteArrayInputStream(response.getContent()));
				XcapResponseParser parser = new XcapResponseParser(input);
				return convertListOfUrisToSetOfContactId(parser.getUris());
			} else {
				if (logger.isActivated()){
					logger.info("Can't get blocked contacts list: " + response.getResponseCode() + " error");
				}
			}
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Can't get blocked contacts list: unexpected exception", e);
			}
		}
		return new HashSet<ContactId>();
	}

	/**
	 * Add a contact to the revoked contacts list
	 *
	 * @param contact Contact
	 * @return Response
	 */
	public HttpResponse addContactToRevokedList(ContactId contact) {
		try {
			if (logger.isActivated()){
				logger.info("Add " + contact + " to revoked list");
			}

			// URL
			String url = "/resource-lists/users/" +
					HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) +
					"/index/~~/resource-lists/list%5B@name=%22rcs_revokedcontacts%22%5D/entry%5B@uri=%22" +
					HttpUtils.encodeURL(PhoneUtils.formatContactIdToUri(contact)) + "%22%5D";

			// Content
			String content = "<entry uri='" + contact + "'></entry>";

			// Create the request
			HttpPutRequest request = new HttpPutRequest(url, content, "application/xcap-el+xml");

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info(contact + " has been added with success to revoked list");
				}
			} else {
				if (logger.isActivated()){
					logger.info("Can't add " + contact + " to revoked list: " + response.getResponseCode() + " error");
				}
			}
			return response;
		} catch(CoreException e) {
			if (logger.isActivated()) {
				logger.error("Can't add " + contact + " to revoked list: unexpected exception", e);
			}
			return null;
		}
	}

	/**
	 * Remove a contact from the revoked contacts list
	 *
	 * @param contact Contact
	 * @return Response
	 */
	public HttpResponse removeContactFromRevokedList(ContactId contact) {
		try {
			if (logger.isActivated()){
				logger.info("Remove " + contact + " from revoked list");
			}

			// URL
			String url = "/resource-lists/users/" +
					HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) +
					"/index/~~/resource-lists/list%5B@name=%22rcs_revokedcontacts%22%5D/entry%5B@uri=%22" +
					HttpUtils.encodeURL(PhoneUtils.formatContactIdToUri(contact)) + "%22%5D";

			// Create the request
			HttpDeleteRequest request = new HttpDeleteRequest(url);

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info(contact + " has been removed with success from revoked list");
				}
			} else {
				if (logger.isActivated()){
					logger.info("Can't remove " + contact + " from revoked list: " + response.getResponseCode() + " error");
				}
			}
			return response;
		} catch(CoreException e) {
			if (logger.isActivated()) {
				logger.error("Can't remove " + contact + " from revoked list: unexpected exception", e);
			}
			return null;
		}
	}

	/**
	 * Returns the list of revoked contacts
	 *
	 * @return List
	 */
	public List<String> getRevokedContacts() {
		List<String> result = new ArrayList<String>();
		try {
			if (logger.isActivated()){
				logger.info("Get revoked contacts list");
			}

			// URL
			String url = "/resource-lists/users/" +
					HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) +
					"/index/~~/resource-lists/list%5B@name=%22rcs_revokedcontacts%22%5D";

			// Create the request
			HttpGetRequest request = new HttpGetRequest(url);

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info("Revoked contacts list has been read with success");
				}

				// Parse response
				InputSource input = new InputSource(
						new ByteArrayInputStream(response.getContent()));
				XcapResponseParser parser = new XcapResponseParser(input);
				result = parser.getUris();
			} else {
				if (logger.isActivated()){
					logger.info("Can't get revoked contacts list: " + response.getResponseCode() + " error");
				}
			}
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Can't get revoked contacts list: unexpected exception", e);
			}
		}
		return result;
	}

	/**
	 * Returns the photo icon URL
	 *
	 * @return URL
	 */
	public String getEndUserPhotoIconUrl() {
		return xdmServerAddr + "/org.openmobilealliance.pres-content/users/" +
			HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) +
			"/oma_status-icon/rcs_status_icon";
	}

	/**
	 * Upload the end user photo
	 *
	 * @param photo Photo icon
	 * @return Response
	 */
	public HttpResponse uploadEndUserPhoto(PhotoIcon photo) {
		try {
			if (logger.isActivated()){
				logger.info("Upload the end user photo");
			}

			// Content
			String data = Base64.encodeBase64ToString(photo.getContent());
			String content = new StringBuilder("<?xml version=\"1.0\" encoding=\"")
					.append(UTF8_STR).append("\"?>").append(HttpUtils.CRLF)
					.append("<content xmlns=\"urn:oma:xml:prs:pres-content\">")
					.append(HttpUtils.CRLF).append("<mime-type>").append(photo.getType())
					.append("</mime-type>").append(HttpUtils.CRLF)
					.append("<encoding>base64</encoding>").append(HttpUtils.CRLF).append("<data>")
					.append(data).append("</data>").append(HttpUtils.CRLF).append("</content>")
					.toString();

			// URL
			String url = "/org.openmobilealliance.pres-content/users/" +
				HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) +
				"/oma_status-icon/rcs_status_icon";

			// Create the request
			HttpPutRequest request = new HttpPutRequest(url, content, "application/vnd.oma.pres-content+xml");

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info("Photo has been uploaded with success");
				}
			} else {
				if (logger.isActivated()){
					logger.info("Can't upload the photo: " + response.getResponseCode() + " error");
				}
			}
			return response;
		} catch(CoreException e) {
			if (logger.isActivated()) {
				logger.error("Can't upload the photo: unexpected exception", e);
			}
			return null;
		}
	}

	/**
	 * Delete the end user photo
	 *
	 * @param photo Photo icon
	 * @return Response
	 */
	public HttpResponse deleteEndUserPhoto() {
		try {
			if (logger.isActivated()){
				logger.info("Delete the end user photo");
			}

			// URL
			String url = "/org.openmobilealliance.pres-content/users/" +
				HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) +
				"/oma_status-icon/rcs_status_icon";

			// Create the request
			HttpDeleteRequest request = new HttpDeleteRequest(url);

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info("Photo has been deleted with success");
				}
			} else {
				if (logger.isActivated()){
					logger.info("Can't delete the photo: " + response.getResponseCode() + " error");
				}
			}
			return response;
		} catch(CoreException e) {
			if (logger.isActivated()) {
				logger.error("Can't delete the photo: unexpected exception", e);
			}
			return null;
		}
	}

	/**
	 * Download photo of a remote contact
	 *
	 * @param url URL of the photo to be downloaded
	 * @param etag Etag of the photo
	 * @return Icon data as photo icon
	 */
	public PhotoIcon downloadContactPhoto(String url, String etag) {
		try {
			if (logger.isActivated()){
				logger.info("Download the photo at " + url);
			}

			// Remove the beginning of the URL
			url = url.substring(url.indexOf("/org.openmobilealliance.pres-content"));

			// Create the request
			HttpGetRequest request = new HttpGetRequest(url);

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info("Download photo with success");
				}

				// Parse response
				InputSource input = new InputSource(
						new ByteArrayInputStream(response.getContent()));
				XcapPhotoIconResponseParser parser = new XcapPhotoIconResponseParser(input);

				// Return data
				byte[] data = parser.getData();
				if (data != null) {
					if (logger.isActivated()){
						logger.debug("Received photo: encoding=" + parser.getEncoding() + ", mime=" + parser.getMime() + ", encoded size=" + data.length);
					}
					byte[] dataArray = Base64.decodeBase64(data);

	    			// Create a bitmap from the received photo data
	    			Bitmap bitmap = BitmapFactory.decodeByteArray(dataArray, 0, dataArray.length);
	    			if (bitmap != null) {
	    				if (logger.isActivated()){
	    					logger.debug("Photo width="+bitmap.getWidth() + " height="+bitmap.getHeight());
	    				}

						return new PhotoIcon(dataArray, bitmap.getWidth(), bitmap.getHeight(), etag);
	    			}else{
	    				return null;
	    			}
				} else {
					if (logger.isActivated()){
						logger.warn("Can't download the photo: photo is null");
					}
					return null;
				}
			} else {
				if (logger.isActivated()){
					logger.warn("Can't download the photo: " + response.getResponseCode() + " error");
				}
				return null;
			}
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Can't download the photo: unexpected exception", e);
			}
			return null;
		}
	}

	/**
	 * Set presence info
	 *
	 * @param info Presence info
	 * @return Response
	 */
	public HttpResponse setPresenceInfo(String info) {
		try {
			if (logger.isActivated()){
				logger.info("Update presence info");
			}

			// URL
			String url = "/pidf-manipulation/users/" +
					HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) +
					"/perm-presence";

			// Create the request
			HttpPutRequest request = new HttpPutRequest(url, info, "application/pidf+xml");

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info("Presence info has been updated with succes");
				}
			} else {
				if (logger.isActivated()){
					logger.info("Can't update the presence info: " + response.getResponseCode() + " error");
				}
			}
			return response;
		} catch(CoreException e) {
			if (logger.isActivated()) {
				logger.error("Can't update the presence info: unexpected exception", e);
			}
			return null;
		}
	}
}
