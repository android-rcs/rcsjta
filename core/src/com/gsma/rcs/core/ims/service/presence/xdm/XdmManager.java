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

package com.gsma.rcs.core.ims.service.presence.xdm;

import static com.gsma.rcs.utils.StringUtils.UTF8;
import static com.gsma.rcs.utils.StringUtils.UTF8_STR;

import com.gsma.rcs.core.CoreException;
import com.gsma.rcs.core.TerminalInfo;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.protocol.http.HttpAuthenticationAgent;
import com.gsma.rcs.core.ims.protocol.http.HttpDeleteRequest;
import com.gsma.rcs.core.ims.protocol.http.HttpGetRequest;
import com.gsma.rcs.core.ims.protocol.http.HttpPutRequest;
import com.gsma.rcs.core.ims.protocol.http.HttpRequest;
import com.gsma.rcs.core.ims.protocol.http.HttpResponse;
import com.gsma.rcs.core.ims.protocol.sip.SipPayloadException;
import com.gsma.rcs.core.ims.service.presence.PhotoIcon;
import com.gsma.rcs.core.ims.service.presence.directory.Folder;
import com.gsma.rcs.core.ims.service.presence.directory.XcapDirectoryParser;
import com.gsma.rcs.platform.network.NetworkFactory;
import com.gsma.rcs.platform.network.SocketConnection;
import com.gsma.rcs.utils.Base64;
import com.gsma.rcs.utils.CloseableUtils;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.PhoneUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax2.sip.InvalidArgumentException;

/**
 * XDM manager
 * 
 * @author Jean-Marc AUFFRET
 */
public class XdmManager {

    /**
     * CRLF constant
     */
    private static final String CRLF = "\r\n";

    private static final char FORWARD_SLASH = '/';

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

    private Context mCtx;

    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(XdmManager.class.getName());

    /**
     * Constructor
     * 
     * @param parent IMS module
     * @param ctx Context
     */
    public XdmManager(ImsModule parent, Context ctx) {
        xdmServerAddr = ImsModule.IMS_USER_PROFILE.getXdmServerAddr();
        xdmServerLogin = ImsModule.IMS_USER_PROFILE.getXdmServerLogin();
        xdmServerPwd = ImsModule.IMS_USER_PROFILE.getXdmServerPassword();
        mCtx = ctx;
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
    private HttpResponse sendRequestToXDMS(HttpRequest request,
            HttpAuthenticationAgent authenticationAgent) throws CoreException {
        try {
            // Send first request
            HttpResponse response = sendHttpRequest(request, authenticationAgent);

            // Analyze the response
            if (response.getResponseCode() == 401) {
                // 401 response received
                if (sLogger.isActivated()) {
                    sLogger.debug("401 Unauthorized response received");
                }

                if (authenticationAgent != null) {
                    // Update the authentication agent
                    authenticationAgent.readWwwAuthenticateHeader(response
                            .getHeader("www-authenticate"));
                }

                // Set the cookie from the received response
                String cookie = response.getHeader("set-cookie");
                request.setCookie(cookie);

                // Send second request with authentification header
                response = sendRequestToXDMS(request, authenticationAgent);
            } else if (response.getResponseCode() == 412) {
                // 412 response received
                if (sLogger.isActivated()) {
                    sLogger.debug("412 Precondition failed");
                }

                // Reset the etag
                documents.remove(request.getAUID());

                // Send second request with authentification header
                response = sendRequestToXDMS(request);
            } else {
                // Other response received
                if (sLogger.isActivated()) {
                    sLogger.debug(response.getResponseCode() + " response received");
                }
            }
            return response;
        } catch (CoreException e) {
            throw e;
        } catch (Exception e) {
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
     * @throws InvalidArgumentException
     * @throws SipPayloadException
     */
    // @FIXME: This method needs a complete refactor, However at this moment due to other prior
    // tasks the refactoring task has been kept in backlog.
    private HttpResponse sendHttpRequest(HttpRequest request,
            HttpAuthenticationAgent authenticationAgent) throws IOException,
            InvalidArgumentException, SipPayloadException {
        SocketConnection conn = null;
        InputStream is = null;
        OutputStream os = null;
        try {
            /* Extract host & port */
            String[] parts = xdmServerAddr.substring(7).split(":|/");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            StringBuilder serviceRoot = new StringBuilder();
            if (parts.length > 2) {
                serviceRoot.append(FORWARD_SLASH).append(parts[2]);
            }

            /* Open connection with the XCAP server */
            conn = NetworkFactory.getFactory().createSocketClientConnection();
            conn.open(host, port);
            is = conn.getInputStream();
            os = conn.getOutputStream();

            /* Create the HTTP request */
            String requestUri = serviceRoot.append(request.getUrl()).toString();
            StringBuilder httpRequest = new StringBuilder(request.getMethod()).append(" ")
                    .append(requestUri).append(" HTTP/1.1").append(CRLF).append("Host: ")
                    .append(host).append(":").append(port).append(CRLF).append("User-Agent: ")
                    .append(TerminalInfo.getProductName()).append(" ")
                    .append(TerminalInfo.getProductVersion(mCtx)).append(CRLF);

            if (authenticationAgent != null) {
                String authorizationHeader = authenticationAgent.generateAuthorizationHeader(
                        request.getMethod(), requestUri, request.getContent());
                httpRequest.append(authorizationHeader).append(CRLF);
            }

            String cookie = request.getCookie();
            if (cookie != null) {
                httpRequest.append("Cookie: ").append(cookie).append(CRLF);
            }

            httpRequest.append("X-3GPP-Intended-Identity: \"")
                    .append(ImsModule.IMS_USER_PROFILE.getXdmServerLogin()).append("\"")
                    .append(CRLF);

            /* Set the If-match header */
            Folder folder = (Folder) documents.get(request.getAUID());
            if ((folder != null) && (folder.getEntry() != null)
                    && (folder.getEntry().getEtag() != null)) {
                httpRequest.append("If-match: \"").append(folder.getEntry().getEtag()).append("\"")
                        .append(CRLF);
            }

            if (request.getContent() != null) {
                httpRequest.append("Content-type: ").append(request.getContentType()).append(CRLF);
                httpRequest.append("Content-Length:").append(request.getContentLength())
                        .append(CRLF).append(CRLF);
            } else {
                httpRequest.append("Content-Length: 0").append(CRLF).append(CRLF);
            }

            /* Write HTTP request headers */
            os.write(httpRequest.toString().getBytes(UTF8));
            os.flush();

            /* Write HTTP content */
            if (request.getContent() != null) {
                os.write(request.getContent().getBytes(UTF8));
                os.flush();
            }

            if (sLogger.isActivated()) {
                if (request.getContent() != null) {
                    sLogger.debug("Send HTTP request:\n" + httpRequest + request.getContent());
                } else {
                    sLogger.debug("Send HTTP request:\n" + httpRequest);
                }
            }

            /* Read HTTP headers response */
            StringBuffer respTrace = new StringBuffer();
            HttpResponse response = new HttpResponse();
            int ch = -1;
            String line = "";
            while ((ch = is.read()) != -1) {
                line += (char) ch;

                if (line.endsWith(CRLF)) {
                    if (line.equals(CRLF)) {
                        /* All headers has been read */
                        break;
                    }

                    if (sLogger.isActivated()) {
                        respTrace.append(line);
                    }

                    /* Remove CRLF */
                    line = line.substring(0, line.length() - 2);

                    if (line.startsWith("HTTP/")) {
                        response.setStatusLine(line);
                    } else {
                        int index = line.indexOf(":");
                        String name = line.substring(0, index).trim().toLowerCase();
                        String value = line.substring(index + 1).trim();
                        response.addHeader(name, value);
                    }

                    line = "";
                }
            }

            int contentLength = -1;
            try {
                String value = response.getHeader("content-length");
                contentLength = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                contentLength = -1;
            }

            if (contentLength > 0) {
                byte[] content = new byte[contentLength];
                int nb = -1;
                int pos = 0;
                byte[] buffer = new byte[1024];
                while ((nb = is.read(buffer)) != -1) {
                    System.arraycopy(buffer, 0, content, pos, nb);
                    pos += nb;

                    if (pos >= contentLength) {
                        break;
                    }
                }
                if (sLogger.isActivated()) {
                    respTrace.append(CRLF).append(new String(content, UTF8));
                }
                response.setContent(content);
            }

            if (sLogger.isActivated()) {
                sLogger.debug("Receive HTTP response:\n" + respTrace.toString());
            }

            /* Save the Etag from the received response */
            String etag = response.getHeader("etag");
            if ((etag != null) && (folder != null) && (folder.getEntry() != null)) {
                folder.getEntry().setEtag(etag);
            }
            return response;

        } finally {
            CloseableUtils.close(conn);
            CloseableUtils.close(is);
            CloseableUtils.close(os);
        }
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
                Folder folder = (Folder) documents.get("rls-services");
                if ((folder == null) || (folder.getEntry() == null)) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("The rls-services document does not exist");
                    }

                    // Set RCS list document
                    setRcsList();
                } else {
                    if (sLogger.isActivated()) {
                        sLogger.debug("The rls-services document already exists");
                    }
                }

                // Check resource list document
                folder = (Folder) documents.get("resource-lists");
                if ((folder == null) || (folder.getEntry() == null)) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("The resource-lists document does not exist");
                    }

                    // Set resource list document
                    setResourcesList();
                } else {
                    if (sLogger.isActivated()) {
                        sLogger.debug("The resource-lists document already exists");
                    }
                }

                // Check presence rules document
                folder = (Folder) documents.get("org.openmobilealliance.pres-rules");
                if ((folder == null) || (folder.getEntry() == null)) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("The org.openmobilealliance.pres-rules document does not exist");
                    }

                    // Set presence rules document
                    setPresenceRules();
                } else {
                    if (sLogger.isActivated()) {
                        sLogger.debug("The org.openmobilealliance.pres-rules document already exists");
                    }
                }
            }
        } catch (Exception e) {
            if (sLogger.isActivated()) {
                sLogger.error("Can't parse the XCAP directory document", e);
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
            if (sLogger.isActivated()) {
                sLogger.info("Get XCAP documents");
            }

            // URL
            String url = "/org.openmobilealliance.xcap-directory/users/"
                    + Uri.encode(ImsModule.IMS_USER_PROFILE.getPublicUri()) + "/directory.xml";

            // Create the request
            HttpGetRequest request = new HttpGetRequest(url);

            // Send the request
            HttpResponse response = sendRequestToXDMS(request);
            if (response.isSuccessfullResponse()) {
                if (sLogger.isActivated()) {
                    sLogger.info("XCAP documents has been read with success");
                }
            } else {
                if (sLogger.isActivated()) {
                    sLogger.info("Can't read XCAP documents: " + response.getResponseCode()
                            + " error");
                }
            }
            return response;
        } catch (CoreException e) {
            if (sLogger.isActivated()) {
                sLogger.error("Can't read XCAP documents: unexpected exception", e);
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
            if (sLogger.isActivated()) {
                sLogger.info("Get RCS list");
            }

            // URL
            String url = "/rls-services/users/"
                    + Uri.encode(ImsModule.IMS_USER_PROFILE.getPublicUri()) + "/index";

            // Create the request
            HttpGetRequest request = new HttpGetRequest(url);

            // Send the request
            HttpResponse response = sendRequestToXDMS(request);
            if (response.isSuccessfullResponse()) {
                if (sLogger.isActivated()) {
                    sLogger.info("RCS list has been read with success");
                }
            } else {
                if (sLogger.isActivated()) {
                    sLogger.info("Can't read RCS list: " + response.getResponseCode() + " error");
                }
            }
            return response;
        } catch (CoreException e) {
            if (sLogger.isActivated()) {
                sLogger.error("Can't read RCS list: unexpected exception", e);
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
            if (sLogger.isActivated()) {
                sLogger.info("Set RCS list");
            }

            // URL
            String url = "/rls-services/users/"
                    + Uri.encode(ImsModule.IMS_USER_PROFILE.getPublicUri()) + "/index";

            // Content
            String user = ImsModule.IMS_USER_PROFILE.getPublicUri();
            String resList = xdmServerAddr + "/resource-lists/users/" + Uri.encode(user)
                    + "/index/~~/resource-lists/list%5B@name=%22rcs%22%5D";
            String content = new StringBuilder("<?xml version=\"1.0\" encoding=\"")
                    .append(UTF8_STR)
                    .append("\"?>")
                    .append(CRLF)
                    .append("<rls-services xmlns=\"urn:ietf:params:xml:ns:rls-services\" xmlns:rl=\"urn:ietf:params:xml:ns:resource-lists\">")
                    .append(CRLF).append("<service uri=\"").append(user)
                    .append(";pres-list=rcs\">").append(CRLF).append("<resource-list>")
                    .append(resList).append("</resource-list>").append(CRLF).append("<packages>")
                    .append(CRLF).append(" <package>presence</package>").append(CRLF)
                    .append("</packages>").append(CRLF).append("</service></rls-services>")
                    .toString();

            // Create the request
            HttpPutRequest request = new HttpPutRequest(url, content,
                    "application/rls-services+xml");

            // Send the request
            HttpResponse response = sendRequestToXDMS(request);
            if (response.isSuccessfullResponse()) {
                if (sLogger.isActivated()) {
                    sLogger.info("RCS list has been set with success");
                }
            } else {
                if (sLogger.isActivated()) {
                    sLogger.info("Can't set RCS list: " + response.getResponseCode() + " error");
                }
            }
            return response;
        } catch (CoreException e) {
            if (sLogger.isActivated()) {
                sLogger.error("Can't set RCS list: unexpected exception", e);
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
            if (sLogger.isActivated()) {
                sLogger.info("Get resources list");
            }

            // URL
            String url = "/resource-lists/users/"
                    + Uri.encode(ImsModule.IMS_USER_PROFILE.getPublicUri()) + "/index";

            // Create the request
            HttpGetRequest request = new HttpGetRequest(url);

            // Send the request
            HttpResponse response = sendRequestToXDMS(request);
            if (response.isSuccessfullResponse()) {
                if (sLogger.isActivated()) {
                    sLogger.info("Resources list has been read with success");
                }
            } else {
                if (sLogger.isActivated()) {
                    sLogger.info("Can't read resources list: " + response.getResponseCode()
                            + " error");
                }
            }
            return response;
        } catch (CoreException e) {
            if (sLogger.isActivated()) {
                sLogger.error("Can't read resources list: unexpected exception", e);
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
            if (sLogger.isActivated()) {
                sLogger.info("Set resources list");
            }

            // URL
            String url = "/resource-lists/users/"
                    + Uri.encode(ImsModule.IMS_USER_PROFILE.getPublicUri()) + "/index";

            // Content
            String user = ImsModule.IMS_USER_PROFILE.getPublicUri();
            String resList = xdmServerAddr + "/resource-lists/users/" + Uri.encode(user)
                    + "/index/~~/resource-lists/list%5B";
            String content = new StringBuilder("<?xml version=\"1.0\" encoding=\"")
                    .append(UTF8_STR).append("\"?>").append(CRLF)
                    .append("<resource-lists xmlns=\"urn:ietf:params:xml:ns:resource-lists\">")
                    .append(CRLF).append("<list name=\"oma_buddylist\">").append(CRLF)
                    .append(" <external anchor=\"").append(resList)
                    .append("@name=%22rcs%22%5D\"/>").append(CRLF).append("</list>").append(CRLF)
                    .append("<list name=\"oma_grantedcontacts\">").append(CRLF)
                    .append(" <external anchor=\"").append(resList)
                    .append("@name=%22rcs%22%5D\"/>").append(CRLF).append("</list>").append(CRLF)
                    .append("<list name=\"oma_blockedcontacts\">").append(CRLF)
                    .append(" <external anchor=\"").append(resList)
                    .append("@name=%22rcs_blockedcontacts%22%5D\"/>").append(CRLF)
                    .append(" <external anchor=\"").append(resList)
                    .append("@name=%22rcs_revokedcontacts%22%5D\"/>").append(CRLF)
                    .append("</list>").append(CRLF).append("<list name=\"rcs\">").append(CRLF)
                    .append(" <display-name>My presence buddies</display-name>").append(CRLF)
                    .append("</list>").append(CRLF).append("<list name=\"rcs_blockedcontacts\">")
                    .append(CRLF).append(" <display-name>My blocked contacts</display-name>")
                    .append(CRLF).append("</list>").append(CRLF)
                    .append("<list name=\"rcs_revokedcontacts\">").append(CRLF)
                    .append(" <display-name>My revoked contacts</display-name>").append(CRLF)
                    .append("</list>").append(CRLF).append("</resource-lists>").toString();

            // Create the request
            HttpPutRequest request = new HttpPutRequest(url, content,
                    "application/resource-lists+xml");

            // Send the request
            HttpResponse response = sendRequestToXDMS(request);
            if (response.isSuccessfullResponse()) {
                if (sLogger.isActivated()) {
                    sLogger.info("Resources list has been set with success");
                }
            } else {
                if (sLogger.isActivated()) {
                    sLogger.info("Can't set resources list: " + response.getResponseCode()
                            + " error");
                }
            }
            return response;
        } catch (CoreException e) {
            if (sLogger.isActivated()) {
                sLogger.error("Can't set resources list: unexpected exception", e);
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
            if (sLogger.isActivated()) {
                sLogger.info("Get presence rules");
            }

            // URL
            String url = "/org.openmobilealliance.pres-rules/users/"
                    + Uri.encode(ImsModule.IMS_USER_PROFILE.getPublicUri()) + "/pres-rules";

            // Create the request
            HttpGetRequest request = new HttpGetRequest(url);

            // Send the request
            HttpResponse response = sendRequestToXDMS(request);
            if (response.isSuccessfullResponse()) {
                if (sLogger.isActivated()) {
                    sLogger.info("Get presence rules has been requested with success");
                }
            } else {
                if (sLogger.isActivated()) {
                    sLogger.info("Can't get the presence rules: " + response.getResponseCode()
                            + " error");
                }
            }
            return response;
        } catch (CoreException e) {
            if (sLogger.isActivated()) {
                sLogger.error("Can't get the presence rules: unexpected exception", e);
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
            if (sLogger.isActivated()) {
                sLogger.info("Set presence rules");
            }

            // URL
            String url = "/org.openmobilealliance.pres-rules/users/"
                    + Uri.encode(ImsModule.IMS_USER_PROFILE.getPublicUri()) + "/pres-rules";

            // Content
            String user = ImsModule.IMS_USER_PROFILE.getPublicUri();
            String blockedList = xdmServerAddr + "/resource-lists/users/" + user
                    + "/index/~~/resource-lists/list%5B@name=%22oma_blockedcontacts%22%5D";
            String grantedList = xdmServerAddr + "/resource-lists/users/" + user
                    + "/index/~~/resource-lists/list%5B@name=%22oma_grantedcontacts%22%5D";
            String content = new StringBuilder("<?xml version=\"1.0\" encoding=\"")
                    .append(UTF8_STR)
                    .append("\"?>")
                    .append(CRLF)
                    .append("<cr:ruleset xmlns:ocp=\"urn:oma:xml:xdm:common-policy\" xmlns:pr=\"urn:ietf:params:xml:ns:pres-rules\" xmlns:cr=\"urn:ietf:params:xml:ns:common-policy\">")
                    .append(CRLF).append("<cr:rule id=\"wp_prs_allow_own\">").append(CRLF)
                    .append(" <cr:conditions>").append(CRLF).append("  <cr:identity><cr:one id=\"")
                    .append(ImsModule.IMS_USER_PROFILE.getPublicUri()).append("\"/></cr:identity>")
                    .append(CRLF).append(" </cr:conditions>").append(CRLF)
                    .append(" <cr:actions><pr:sub-handling>allow</pr:sub-handling></cr:actions>")
                    .append(CRLF).append(" <cr:transformations>").append(CRLF)
                    .append("  <pr:provide-services><pr:all-services/></pr:provide-services>")
                    .append(CRLF)
                    .append("  <pr:provide-persons><pr:all-persons/></pr:provide-persons>")
                    .append(CRLF)
                    .append("  <pr:provide-devices><pr:all-devices/></pr:provide-devices>")
                    .append(CRLF).append("  <pr:provide-all-attributes/>").append(CRLF)
                    .append(" </cr:transformations>").append(CRLF).append("</cr:rule>")
                    .append(CRLF).append("<cr:rule id=\"rcs_allow_services_anonymous\">")
                    .append(CRLF)
                    .append(" <cr:conditions><ocp:anonymous-request/></cr:conditions>")
                    .append(CRLF)
                    .append(" <cr:actions><pr:sub-handling>allow</pr:sub-handling></cr:actions>")
                    .append(CRLF).append(" <cr:transformations>").append(CRLF)
                    .append("  <pr:provide-services><pr:all-services/></pr:provide-services>")
                    .append(CRLF).append("  <pr:provide-all-attributes/>").append(CRLF)
                    .append(" </cr:transformations>").append(CRLF).append("</cr:rule>")
                    .append(CRLF).append("<cr:rule id=\"wp_prs_unlisted\">").append(CRLF)
                    .append(" <cr:conditions><ocp:other-identity/></cr:conditions>").append(CRLF)
                    .append(" <cr:actions><pr:sub-handling>confirm</pr:sub-handling></cr:actions>")
                    .append(CRLF).append("</cr:rule>").append(CRLF)
                    .append("<cr:rule id=\"wp_prs_grantedcontacts\">").append(CRLF)
                    .append(" <cr:conditions>").append(CRLF).append(" <ocp:external-list>")
                    .append(CRLF).append("  <ocp:entry anc=\"").append(grantedList).append("\"/>")
                    .append(CRLF).append(" </ocp:external-list>").append(CRLF)
                    .append(" </cr:conditions>").append(CRLF)
                    .append(" <cr:actions><pr:sub-handling>allow</pr:sub-handling></cr:actions>")
                    .append(CRLF).append(" <cr:transformations>").append(CRLF)
                    .append("   <pr:provide-services><pr:all-services/></pr:provide-services>")
                    .append(CRLF)
                    .append("   <pr:provide-persons><pr:all-persons/></pr:provide-persons>")
                    .append(CRLF)
                    .append("   <pr:provide-devices><pr:all-devices/></pr:provide-devices>")
                    .append(CRLF).append("   <pr:provide-all-attributes/>").append(CRLF)
                    .append(" </cr:transformations>").append(CRLF).append("</cr:rule>")
                    .append(CRLF).append("<cr:rule id=\"wp_prs_blockedcontacts\">").append(CRLF)
                    .append(" <cr:conditions>").append(CRLF).append("  <ocp:external-list>")
                    .append(CRLF).append("  <ocp:entry anc=\"").append(blockedList).append("\"/>")
                    .append(CRLF).append(" </ocp:external-list>").append(CRLF)
                    .append(" </cr:conditions>").append(CRLF)
                    .append(" <cr:actions><pr:sub-handling>block</pr:sub-handling></cr:actions>")
                    .append(CRLF).append("</cr:rule>").append(CRLF).append("</cr:ruleset>")
                    .toString();

            // Create the request
            HttpPutRequest request = new HttpPutRequest(url, content, "application/auth-policy+xml");

            // Send the request
            HttpResponse response = sendRequestToXDMS(request);
            if (response.isSuccessfullResponse()) {
                if (sLogger.isActivated()) {
                    sLogger.info("Presence rules has been set with success");
                }
            } else {
                if (sLogger.isActivated()) {
                    sLogger.info("Can't set presence rules: " + response.getResponseCode()
                            + " error");
                }
            }
            return response;
        } catch (CoreException e) {
            if (sLogger.isActivated()) {
                sLogger.error("Can't set presence rules: unexpected exception", e);
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
            if (sLogger.isActivated()) {
                sLogger.info("Add " + contact + " to granted list");
            }

            // URL
            String url = "/resource-lists/users/"
                    + Uri.encode(ImsModule.IMS_USER_PROFILE.getPublicUri())
                    + "/index/~~/resource-lists/list%5B@name=%22rcs%22%5D/entry%5B@uri=%22"
                    + Uri.encode(PhoneUtils.formatContactIdToUri(contact)) + "%22%5D";

            // Content
            String content = "<entry uri='" + contact + "'></entry>";

            // Create the request
            HttpPutRequest request = new HttpPutRequest(url, content, "application/xcap-el+xml");

            // Send the request
            HttpResponse response = sendRequestToXDMS(request);
            if (response.isSuccessfullResponse()) {
                if (sLogger.isActivated()) {
                    sLogger.info(contact + " has been added with success to granted list");
                }
            } else {
                if (sLogger.isActivated()) {
                    sLogger.info("Can't add " + contact + " to granted list: "
                            + response.getResponseCode() + " error");
                }
            }
            return response;
        } catch (CoreException e) {
            if (sLogger.isActivated()) {
                sLogger.error("Can't add " + contact + " to granted list: unexpected exception", e);
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
            if (sLogger.isActivated()) {
                sLogger.info("Remove " + contact + " from granted list");
            }

            // URL
            String url = "/resource-lists/users/"
                    + Uri.encode(ImsModule.IMS_USER_PROFILE.getPublicUri())
                    + "/index/~~/resource-lists/list%5B@name=%22rcs%22%5D/entry%5B@uri=%22"
                    + Uri.encode(PhoneUtils.formatContactIdToUri(contact)) + "%22%5D";

            // Create the request
            HttpDeleteRequest request = new HttpDeleteRequest(url);

            // Send the request
            HttpResponse response = sendRequestToXDMS(request);
            if (response.isSuccessfullResponse()) {
                if (sLogger.isActivated()) {
                    sLogger.info(contact + " has been removed with success from granted list");
                }
            } else {
                if (sLogger.isActivated()) {
                    sLogger.info("Can't remove " + contact + " from granted list: "
                            + response.getResponseCode() + " error");
                }
            }
            return response;

        } catch (CoreException e) {
            if (sLogger.isActivated()) {
                sLogger.error("Can't remove " + contact
                        + " from granted list: unexpected exception", e);
            }
            return null;
        }
    }

    private Set<ContactId> convertListOfUrisToSetOfContactId(List<String> uris) {
        Set<ContactId> result = new HashSet<ContactId>();
        if (uris != null) {
            for (String uri : uris) {
                PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(uri);
                if (number == null) {
                    if (sLogger.isActivated()) {
                        sLogger.warn("Cannot parse uri " + uri);
                    }
                    continue;
                }
                result.add(ContactUtil.createContactIdFromValidatedData(number));
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
            if (sLogger.isActivated()) {
                sLogger.info("Get granted contacts list");
            }

            // URL
            String url = "/resource-lists/users/"
                    + Uri.encode(ImsModule.IMS_USER_PROFILE.getPublicUri())
                    + "/index/~~/resource-lists/list%5B@name=%22rcs%22%5D";

            // Create the request
            HttpGetRequest request = new HttpGetRequest(url);

            // Send the request
            HttpResponse response = sendRequestToXDMS(request);
            if (response.isSuccessfullResponse()) {
                if (sLogger.isActivated()) {
                    sLogger.info("Granted contacts list has been read with success");
                }

                // Parse response
                InputSource input = new InputSource(new ByteArrayInputStream(response.getContent()));
                XcapResponseParser parser = new XcapResponseParser(input);
                return convertListOfUrisToSetOfContactId(parser.getUris());
            } else {
                if (sLogger.isActivated()) {
                    sLogger.info("Can't get granted contacts list: " + response.getResponseCode()
                            + " error");
                }
            }
        } catch (Exception e) {
            if (sLogger.isActivated()) {
                sLogger.error("Can't get granted contacts list: unexpected exception", e);
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
            if (sLogger.isActivated()) {
                sLogger.info("Add " + contact + " to blocked list");
            }

            // URL
            String url = "/resource-lists/users/"
                    + Uri.encode(ImsModule.IMS_USER_PROFILE.getPublicUri())
                    + "/index/~~/resource-lists/list%5B@name=%22rcs_blockedcontacts%22%5D/entry%5B@uri=%22"
                    + Uri.encode(contact) + "%22%5D";

            // Content
            String content = "<entry uri='" + contact + "'></entry>";

            // Create the request
            HttpPutRequest request = new HttpPutRequest(url, content, "application/xcap-el+xml");

            // Send the request
            HttpResponse response = sendRequestToXDMS(request);
            if (response.isSuccessfullResponse()) {
                if (sLogger.isActivated()) {
                    sLogger.info(contact + " has been added with success to blocked list");
                }
            } else {
                if (sLogger.isActivated()) {
                    sLogger.info("Can't add " + contact + " to granted list: "
                            + response.getResponseCode() + " error");
                }
            }
            return response;
        } catch (CoreException e) {
            if (sLogger.isActivated()) {
                sLogger.error("Can't add " + contact + " to blocked list: unexpected exception", e);
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
            if (sLogger.isActivated()) {
                sLogger.info("Remove " + contact + " from blocked list");
            }

            // URL
            String url = "/resource-lists/users/"
                    + Uri.encode(ImsModule.IMS_USER_PROFILE.getPublicUri())
                    + "/index/~~/resource-lists/list%5B@name=%22rcs_blockedcontacts%22%5D/entry%5B@uri=%22"
                    + Uri.encode(PhoneUtils.formatContactIdToUri(contact)) + "%22%5D";

            // Create the request
            HttpDeleteRequest request = new HttpDeleteRequest(url);

            // Send the request
            HttpResponse response = sendRequestToXDMS(request);
            if (response.isSuccessfullResponse()) {
                if (sLogger.isActivated()) {
                    sLogger.info(contact + " has been removed with success from blocked list");
                }
            } else {
                if (sLogger.isActivated()) {
                    sLogger.info("Can't remove " + contact + " from blocked list: "
                            + response.getResponseCode() + " error");
                }
            }
            return response;
        } catch (CoreException e) {
            if (sLogger.isActivated()) {
                sLogger.error("Can't remove " + contact
                        + " from blocked list: unexpected exception", e);
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
            if (sLogger.isActivated()) {
                sLogger.info("Get blocked contacts list");
            }

            // URL
            String url = "/resource-lists/users/"
                    + Uri.encode(ImsModule.IMS_USER_PROFILE.getPublicUri())
                    + "/index/~~/resource-lists/list%5B@name=%22rcs_blockedcontacts%22%5D";

            // Create the request
            HttpGetRequest request = new HttpGetRequest(url);

            // Send the request
            HttpResponse response = sendRequestToXDMS(request);
            if (response.isSuccessfullResponse()) {
                if (sLogger.isActivated()) {
                    sLogger.info("Blocked contacts list has been read with success");
                }

                // Parse response
                InputSource input = new InputSource(new ByteArrayInputStream(response.getContent()));
                XcapResponseParser parser = new XcapResponseParser(input);
                return convertListOfUrisToSetOfContactId(parser.getUris());
            } else {
                if (sLogger.isActivated()) {
                    sLogger.info("Can't get blocked contacts list: " + response.getResponseCode()
                            + " error");
                }
            }
        } catch (Exception e) {
            if (sLogger.isActivated()) {
                sLogger.error("Can't get blocked contacts list: unexpected exception", e);
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
            if (sLogger.isActivated()) {
                sLogger.info("Add " + contact + " to revoked list");
            }

            String url = "/resource-lists/users/"
                    + Uri.encode(ImsModule.IMS_USER_PROFILE.getPublicUri())
                    + "/index/~~/resource-lists/list%5B@name=%22rcs_revokedcontacts%22%5D/entry%5B@uri=%22"
                    + Uri.encode(PhoneUtils.formatContactIdToUri(contact)) + "%22%5D";

            String content = "<entry uri='" + contact + "'></entry>";

            HttpPutRequest request = new HttpPutRequest(url, content, "application/xcap-el+xml");

            HttpResponse response = sendRequestToXDMS(request);
            if (response.isSuccessfullResponse()) {
                if (sLogger.isActivated()) {
                    sLogger.info(contact + " has been added with success to revoked list");
                }
            } else {
                if (sLogger.isActivated()) {
                    sLogger.info("Can't add " + contact + " to revoked list: "
                            + response.getResponseCode() + " error");
                }
            }
            return response;

        } catch (CoreException e) {
            if (sLogger.isActivated()) {
                sLogger.error("Can't add " + contact + " to revoked list: unexpected exception", e);
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
            if (sLogger.isActivated()) {
                sLogger.info("Remove " + contact + " from revoked list");
            }

            String url = "/resource-lists/users/"
                    + Uri.encode(ImsModule.IMS_USER_PROFILE.getPublicUri())
                    + "/index/~~/resource-lists/list%5B@name=%22rcs_revokedcontacts%22%5D/entry%5B@uri=%22"
                    + Uri.encode(PhoneUtils.formatContactIdToUri(contact)) + "%22%5D";

            HttpDeleteRequest request = new HttpDeleteRequest(url);

            HttpResponse response = sendRequestToXDMS(request);
            if (response.isSuccessfullResponse()) {
                if (sLogger.isActivated()) {
                    sLogger.info(contact + " has been removed with success from revoked list");
                }
            } else {
                if (sLogger.isActivated()) {
                    sLogger.info("Can't remove " + contact + " from revoked list: "
                            + response.getResponseCode() + " error");
                }
            }
            return response;

        } catch (CoreException e) {
            if (sLogger.isActivated()) {
                sLogger.error("Can't remove " + contact
                        + " from revoked list: unexpected exception", e);
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
            if (sLogger.isActivated()) {
                sLogger.info("Get revoked contacts list");
            }

            String url = "/resource-lists/users/"
                    + Uri.encode(ImsModule.IMS_USER_PROFILE.getPublicUri())
                    + "/index/~~/resource-lists/list%5B@name=%22rcs_revokedcontacts%22%5D";

            HttpGetRequest request = new HttpGetRequest(url);

            HttpResponse response = sendRequestToXDMS(request);
            if (response.isSuccessfullResponse()) {
                if (sLogger.isActivated()) {
                    sLogger.info("Revoked contacts list has been read with success");
                }

                InputSource input = new InputSource(new ByteArrayInputStream(response.getContent()));
                XcapResponseParser parser = new XcapResponseParser(input);
                result = parser.getUris();
            } else {
                if (sLogger.isActivated()) {
                    sLogger.info("Can't get revoked contacts list: " + response.getResponseCode()
                            + " error");
                }
            }
        } catch (Exception e) {
            if (sLogger.isActivated()) {
                sLogger.error("Can't get revoked contacts list: unexpected exception", e);
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
        return xdmServerAddr + "/org.openmobilealliance.pres-content/users/"
                + Uri.encode(ImsModule.IMS_USER_PROFILE.getPublicUri())
                + "/oma_status-icon/rcs_status_icon";
    }

    /**
     * Upload the end user photo
     * 
     * @param photo Photo icon
     * @return Response
     */
    public HttpResponse uploadEndUserPhoto(PhotoIcon photo) {
        try {
            if (sLogger.isActivated()) {
                sLogger.info("Upload the end user photo");
            }

            // Content
            String data = Base64.encodeBase64ToString(photo.getContent());
            String content = new StringBuilder("<?xml version=\"1.0\" encoding=\"")
                    .append(UTF8_STR).append("\"?>").append(CRLF)
                    .append("<content xmlns=\"urn:oma:xml:prs:pres-content\">").append(CRLF)
                    .append("<mime-type>").append(photo.getType()).append("</mime-type>")
                    .append(CRLF).append("<encoding>base64</encoding>").append(CRLF)
                    .append("<data>").append(data).append("</data>").append(CRLF)
                    .append("</content>").toString();

            // URL
            String url = "/org.openmobilealliance.pres-content/users/"
                    + Uri.encode(ImsModule.IMS_USER_PROFILE.getPublicUri())
                    + "/oma_status-icon/rcs_status_icon";

            // Create the request
            HttpPutRequest request = new HttpPutRequest(url, content,
                    "application/vnd.oma.pres-content+xml");

            // Send the request
            HttpResponse response = sendRequestToXDMS(request);
            if (response.isSuccessfullResponse()) {
                if (sLogger.isActivated()) {
                    sLogger.info("Photo has been uploaded with success");
                }
            } else {
                if (sLogger.isActivated()) {
                    sLogger.info("Can't upload the photo: " + response.getResponseCode() + " error");
                }
            }
            return response;
        } catch (CoreException e) {
            if (sLogger.isActivated()) {
                sLogger.error("Can't upload the photo: unexpected exception", e);
            }
            return null;
        }
    }

    /**
     * Delete the end user photo
     * 
     * @return Response
     */
    public HttpResponse deleteEndUserPhoto() {
        try {
            if (sLogger.isActivated()) {
                sLogger.info("Delete the end user photo");
            }

            // URL
            String url = "/org.openmobilealliance.pres-content/users/"
                    + Uri.encode(ImsModule.IMS_USER_PROFILE.getPublicUri())
                    + "/oma_status-icon/rcs_status_icon";

            // Create the request
            HttpDeleteRequest request = new HttpDeleteRequest(url);

            // Send the request
            HttpResponse response = sendRequestToXDMS(request);
            if (response.isSuccessfullResponse()) {
                if (sLogger.isActivated()) {
                    sLogger.info("Photo has been deleted with success");
                }
            } else {
                if (sLogger.isActivated()) {
                    sLogger.info("Can't delete the photo: " + response.getResponseCode() + " error");
                }
            }
            return response;

        } catch (CoreException e) {
            if (sLogger.isActivated()) {
                sLogger.error("Can't delete the photo: unexpected exception", e);
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
            if (sLogger.isActivated()) {
                sLogger.info("Download the photo at " + url);
            }

            // Remove the beginning of the URL
            url = url.substring(url.indexOf("/org.openmobilealliance.pres-content"));

            // Create the request
            HttpGetRequest request = new HttpGetRequest(url);

            // Send the request
            HttpResponse response = sendRequestToXDMS(request);
            if (response.isSuccessfullResponse()) {
                if (sLogger.isActivated()) {
                    sLogger.info("Download photo with success");
                }

                // Parse response
                InputSource input = new InputSource(new ByteArrayInputStream(response.getContent()));
                XcapPhotoIconResponseParser parser = new XcapPhotoIconResponseParser(input);

                // Return data
                byte[] data = parser.getData();
                if (data != null) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Received photo: encoding=" + parser.getEncoding()
                                + ", mime=" + parser.getMime() + ", encoded size=" + data.length);
                    }
                    byte[] dataArray = Base64.decodeBase64(data);

                    // Create a bitmap from the received photo data
                    Bitmap bitmap = BitmapFactory.decodeByteArray(dataArray, 0, dataArray.length);
                    if (bitmap != null) {
                        if (sLogger.isActivated()) {
                            sLogger.debug("Photo width=" + bitmap.getWidth() + " height="
                                    + bitmap.getHeight());
                        }

                        return new PhotoIcon(dataArray, bitmap.getWidth(), bitmap.getHeight(), etag);
                    } else {
                        return null;
                    }
                } else {
                    if (sLogger.isActivated()) {
                        sLogger.warn("Can't download the photo: photo is null");
                    }
                    return null;
                }
            } else {
                if (sLogger.isActivated()) {
                    sLogger.warn("Can't download the photo: " + response.getResponseCode()
                            + " error");
                }
                return null;
            }
        } catch (Exception e) {
            if (sLogger.isActivated()) {
                sLogger.error("Can't download the photo: unexpected exception", e);
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
            if (sLogger.isActivated()) {
                sLogger.info("Update presence info");
            }

            String url = "/pidf-manipulation/users/"
                    + Uri.encode(ImsModule.IMS_USER_PROFILE.getPublicUri()) + "/perm-presence";

            HttpPutRequest request = new HttpPutRequest(url, info, "application/pidf+xml");

            HttpResponse response = sendRequestToXDMS(request);
            if (response.isSuccessfullResponse()) {
                if (sLogger.isActivated()) {
                    sLogger.info("Presence info has been updated with succes");
                }
            } else {
                if (sLogger.isActivated()) {
                    sLogger.info("Can't update the presence info: " + response.getResponseCode()
                            + " error");
                }
            }
            return response;

        } catch (CoreException e) {
            if (sLogger.isActivated()) {
                sLogger.error("Can't update the presence info: unexpected exception", e);
            }
            return null;
        }
    }
}
