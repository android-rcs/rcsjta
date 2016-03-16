/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

import com.gsma.rcs.core.TerminalInfo;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.http.HttpAuthenticationAgent;
import com.gsma.rcs.core.ims.protocol.http.HttpDeleteRequest;
import com.gsma.rcs.core.ims.protocol.http.HttpGetRequest;
import com.gsma.rcs.core.ims.protocol.http.HttpPutRequest;
import com.gsma.rcs.core.ims.protocol.http.HttpRequest;
import com.gsma.rcs.core.ims.protocol.http.HttpResponse;
import com.gsma.rcs.core.ims.service.presence.PhotoIcon;
import com.gsma.rcs.core.ims.service.presence.directory.Folder;
import com.gsma.rcs.core.ims.service.presence.directory.XcapDirectoryParser;
import com.gsma.rcs.core.ims.userprofile.UserProfile;
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
import android.net.Uri;
import android.text.TextUtils;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import javax2.sip.message.Response;

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

    private static final String XCAP_SCHEME = "/org.openmobilealliance.xcap-directory/users/";

    private static final String PRES_RULES_SCHEME = "/org.openmobilealliance.pres-rules/users/";

    private static final String RLS_SCHEME = "/rls-services/users/";

    private static final String RESOURCE_SCHEME = "/resource-lists/users/";

    private static final String XCAP_FRAGMENT = "/directory.xml";

    private static final String PRES_RULES_FRAGMENT = "/pres-rules";

    private static final String INDEX_FRAGMENT = "/index";

    private static final String CONTENT_TYPE_RLS = "application/rls-services+xml";

    private static final String CONTENT_TYPE_RESOURCE = "application/resource-lists+xml";

    private static final String CONTENT_TYPE_AUTH = "application/auth-policy+xml";

    private static final String PROTOCOL_HTTPS = "https";

    private static final int DEFAULT_HTTPS_PORT = 443;

    private static final int DEFAULT_HTTP_PORT = 80;

    private Uri xdmServerAddr;

    private String xdmServerLogin;

    private String xdmServerPwd;

    /**
     * Managed documents
     */
    private Hashtable<String, Folder> documents = new Hashtable<String, Folder>();

    private Context mCtx;

    private static final Logger sLogger = Logger.getLogger(XdmManager.class.getName());

    /**
     * Constructor
     * 
     * @param parent IMS module
     * @param ctx Context
     */
    public XdmManager(Context ctx) {
        mCtx = ctx;
    }

    /**
     * Send HTTP PUT request
     * 
     * @param request HTTP request
     * @return HTTP response
     * @throws PayloadException
     * @throws NetworkException
     */
    private HttpResponse sendRequestToXDMS(HttpRequest request) throws PayloadException,
            NetworkException {
        return sendRequestToXDMS(request, new HttpAuthenticationAgent(xdmServerLogin, xdmServerPwd));
    }

    /**
     * Send HTTP PUT request with authentication
     * 
     * @param request HTTP request
     * @param authenticationAgent Authentication agent
     * @return HTTP response
     * @throws PayloadException
     * @throws NetworkException
     */
    private HttpResponse sendRequestToXDMS(HttpRequest request,
            HttpAuthenticationAgent authenticationAgent) throws PayloadException, NetworkException {
        HttpResponse response = sendHttpRequest(request, authenticationAgent);
        final int responseCode = response.getResponseCode();
        switch (responseCode) {
            case Response.UNAUTHORIZED:
                if (sLogger.isActivated()) {
                    sLogger.debug("401 Unauthorized response received");
                }
                if (authenticationAgent != null) {
                    authenticationAgent.readWwwAuthenticateHeader(response
                            .getHeader("www-authenticate"));
                }
                String cookie = response.getHeader("set-cookie");
                request.setCookie(cookie);
                return sendRequestToXDMS(request, authenticationAgent);

            case Response.CONDITIONAL_REQUEST_FAILED:
                if (sLogger.isActivated()) {
                    sLogger.debug("412 Precondition failed");
                }
                documents.remove(request.getAUID());
                return sendRequestToXDMS(request);

            default:
                throw new NetworkException(new StringBuilder("Invalid response : ").append(
                        responseCode).toString());
        }
    }

    /**
     * Send HTTP PUT request
     * 
     * @param request HTTP request
     * @param authenticationAgent Authentication agent
     * @return HTTP response
     * @throws PayloadException
     * @throws NetworkException
     */
    // @FIXME: This method needs a complete refactor, However at this moment due to other prior
    // tasks the refactoring task has been kept in backlog.
    private HttpResponse sendHttpRequest(HttpRequest request,
            HttpAuthenticationAgent authenticationAgent) throws PayloadException, NetworkException {
        SocketConnection conn = null;
        InputStream is = null;
        OutputStream os = null;
        try {
            URL url = new URL(xdmServerAddr.toString());
            StringBuilder serviceRoot = new StringBuilder();
            final String path = url.getPath();
            if (!TextUtils.isEmpty(path)) {
                serviceRoot.append(path);
            }
            /* Open connection with the XCAP server */
            conn = NetworkFactory.getFactory().createSocketClientConnection();
            final String host = url.getHost();
            int port = url.getPort();
            if (port == -1) {
                port = PROTOCOL_HTTPS.equals(url.getProtocol()) ? DEFAULT_HTTPS_PORT
                        : DEFAULT_HTTP_PORT;
            }
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
                    .append(ImsModule.getImsUserProfile().getXdmServerLogin()).append("\"")
                    .append(CRLF);

            /* Set the If-match header */
            Folder folder = documents.get(request.getAUID());
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

        } catch (MalformedURLException e) {
            throw new PayloadException(new StringBuilder(
                    "Failed to send http request, malformed uri: ").append(xdmServerAddr)
                    .toString(), e);

        } catch (IOException e) {
            throw new NetworkException("Failed to send http request!", e);

        } finally {
            CloseableUtils.tryToClose(conn);
            CloseableUtils.tryToClose(is);
            CloseableUtils.tryToClose(os);
        }
    }

    /**
     * Initialize the XDM interface
     * 
     * @throws PayloadException
     * @throws NetworkException
     */
    public void initialize() throws PayloadException, NetworkException {
        try {
            UserProfile profile = ImsModule.getImsUserProfile();
            xdmServerAddr = profile.getXdmServerAddr();
            xdmServerLogin = profile.getXdmServerLogin();
            xdmServerPwd = profile.getXdmServerPassword();

            HttpResponse response = getXcapDocuments();
            if (!response.isSuccessfullResponse()) {
                throw new NetworkException(
                        "Failed to get successfull response from presence server!");
            }
            // Analyze the XCAP directory
            InputSource input = new InputSource(new ByteArrayInputStream(response.getContent()));
            XcapDirectoryParser parser = new XcapDirectoryParser(input);
            documents = parser.getDocuments();

            // Check RCS list document
            Folder folder = documents.get("rls-services");
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
            folder = documents.get("resource-lists");
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
            folder = documents.get("org.openmobilealliance.pres-rules");
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
        } catch (ParserConfigurationException e) {
            throw new PayloadException("Can't parse the XCAP directory document!", e);

        } catch (SAXException e) {
            throw new PayloadException("Can't parse the XCAP directory document!", e);

        } catch (IOException e) {
            throw new NetworkException("Can't parse the XCAP directory document!", e);
        }
    }

    /**
     * Get XCAP managed documents
     * 
     * @return Response
     * @throws PayloadException
     * @throws NetworkException
     */
    private HttpResponse getXcapDocuments() throws PayloadException, NetworkException {
        return sendRequestToXDMS(new HttpGetRequest(new StringBuilder(XCAP_SCHEME)
                .append(ImsModule.getImsUserProfile().getPublicUri()).append(XCAP_FRAGMENT)
                .toString()));
    }

    /**
     * Set RCS list
     * 
     * @return Response
     * @throws PayloadException
     * @throws NetworkException
     */
    private HttpResponse setRcsList() throws PayloadException, NetworkException {
        if (sLogger.isActivated()) {
            sLogger.info("Set RCS list");
        }
        String user = ImsModule.getImsUserProfile().getPublicUri();
        String resList = xdmServerAddr + "/resource-lists/users/" + Uri.encode(user)
                + "/index/~~/resource-lists/list%5B@name=%22rcs%22%5D";
        String content = new StringBuilder("<?xml version=\"1.0\" encoding=\"")
                .append(UTF8_STR)
                .append("\"?>")
                .append(CRLF)
                .append("<rls-services xmlns=\"urn:ietf:params:xml:ns:rls-services\" xmlns:rl=\"urn:ietf:params:xml:ns:resource-lists\">")
                .append(CRLF).append("<service uri=\"").append(user).append(";pres-list=rcs\">")
                .append(CRLF).append("<resource-list>").append(resList).append("</resource-list>")
                .append(CRLF).append("<packages>").append(CRLF)
                .append(" <package>presence</package>").append(CRLF).append("</packages>")
                .append(CRLF).append("</service></rls-services>").toString();

        return sendRequestToXDMS(new HttpPutRequest(Uri.fromParts(RLS_SCHEME, user, INDEX_FRAGMENT)
                .getEncodedPath(), content, CONTENT_TYPE_RLS));

    }

    /**
     * Set resources list
     * 
     * @return Response
     * @throws PayloadException
     * @throws NetworkException
     */
    private HttpResponse setResourcesList() throws PayloadException, NetworkException {
        if (sLogger.isActivated()) {
            sLogger.info("Set resources list");
        }
        String user = ImsModule.getImsUserProfile().getPublicUri();
        String resList = xdmServerAddr + "/resource-lists/users/" + Uri.encode(user)
                + "/index/~~/resource-lists/list%5B";
        String content = new StringBuilder("<?xml version=\"1.0\" encoding=\"").append(UTF8_STR)
                .append("\"?>").append(CRLF)
                .append("<resource-lists xmlns=\"urn:ietf:params:xml:ns:resource-lists\">")
                .append(CRLF).append("<list name=\"oma_buddylist\">").append(CRLF)
                .append(" <external anchor=\"").append(resList).append("@name=%22rcs%22%5D\"/>")
                .append(CRLF).append("</list>").append(CRLF)
                .append("<list name=\"oma_grantedcontacts\">").append(CRLF)
                .append(" <external anchor=\"").append(resList).append("@name=%22rcs%22%5D\"/>")
                .append(CRLF).append("</list>").append(CRLF)
                .append("<list name=\"oma_blockedcontacts\">").append(CRLF)
                .append(" <external anchor=\"").append(resList)
                .append("@name=%22rcs_blockedcontacts%22%5D\"/>").append(CRLF)
                .append(" <external anchor=\"").append(resList)
                .append("@name=%22rcs_revokedcontacts%22%5D\"/>").append(CRLF).append("</list>")
                .append(CRLF).append("<list name=\"rcs\">").append(CRLF)
                .append(" <display-name>My presence buddies</display-name>").append(CRLF)
                .append("</list>").append(CRLF).append("<list name=\"rcs_blockedcontacts\">")
                .append(CRLF).append(" <display-name>My blocked contacts</display-name>")
                .append(CRLF).append("</list>").append(CRLF)
                .append("<list name=\"rcs_revokedcontacts\">").append(CRLF)
                .append(" <display-name>My revoked contacts</display-name>").append(CRLF)
                .append("</list>").append(CRLF).append("</resource-lists>").toString();

        return sendRequestToXDMS(new HttpPutRequest(Uri.fromParts(RESOURCE_SCHEME, user,
                INDEX_FRAGMENT).getEncodedPath(), content, CONTENT_TYPE_RESOURCE));

    }

    /**
     * Set presence rules
     * 
     * @return Response
     * @throws PayloadException
     * @throws NetworkException
     */
    private HttpResponse setPresenceRules() throws PayloadException, NetworkException {
        if (sLogger.isActivated()) {
            sLogger.info("Set presence rules");
        }
        String user = ImsModule.getImsUserProfile().getPublicUri();
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
                .append(user).append("\"/></cr:identity>").append(CRLF).append(" </cr:conditions>")
                .append(CRLF)
                .append(" <cr:actions><pr:sub-handling>allow</pr:sub-handling></cr:actions>")
                .append(CRLF).append(" <cr:transformations>").append(CRLF)
                .append("  <pr:provide-services><pr:all-services/></pr:provide-services>")
                .append(CRLF)
                .append("  <pr:provide-persons><pr:all-persons/></pr:provide-persons>")
                .append(CRLF)
                .append("  <pr:provide-devices><pr:all-devices/></pr:provide-devices>")
                .append(CRLF).append("  <pr:provide-all-attributes/>").append(CRLF)
                .append(" </cr:transformations>").append(CRLF).append("</cr:rule>").append(CRLF)
                .append("<cr:rule id=\"rcs_allow_services_anonymous\">").append(CRLF)
                .append(" <cr:conditions><ocp:anonymous-request/></cr:conditions>").append(CRLF)
                .append(" <cr:actions><pr:sub-handling>allow</pr:sub-handling></cr:actions>")
                .append(CRLF).append(" <cr:transformations>").append(CRLF)
                .append("  <pr:provide-services><pr:all-services/></pr:provide-services>")
                .append(CRLF).append("  <pr:provide-all-attributes/>").append(CRLF)
                .append(" </cr:transformations>").append(CRLF).append("</cr:rule>").append(CRLF)
                .append("<cr:rule id=\"wp_prs_unlisted\">").append(CRLF)
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
                .append(" </cr:transformations>").append(CRLF).append("</cr:rule>").append(CRLF)
                .append("<cr:rule id=\"wp_prs_blockedcontacts\">").append(CRLF)
                .append(" <cr:conditions>").append(CRLF).append("  <ocp:external-list>")
                .append(CRLF).append("  <ocp:entry anc=\"").append(blockedList).append("\"/>")
                .append(CRLF).append(" </ocp:external-list>").append(CRLF)
                .append(" </cr:conditions>").append(CRLF)
                .append(" <cr:actions><pr:sub-handling>block</pr:sub-handling></cr:actions>")
                .append(CRLF).append("</cr:rule>").append(CRLF).append("</cr:ruleset>").toString();

        return sendRequestToXDMS(new HttpPutRequest(Uri.fromParts(PRES_RULES_SCHEME, user,
                PRES_RULES_FRAGMENT).getEncodedPath(), content, CONTENT_TYPE_AUTH));
    }

    /**
     * Add a contact to the granted contacts list
     * 
     * @param contact Contact
     * @return Response
     * @throws PayloadException
     * @throws NetworkException
     */
    public HttpResponse addContactToGrantedList(ContactId contact) throws PayloadException,
            NetworkException {
        if (sLogger.isActivated()) {
            sLogger.info("Add " + contact + " to granted list");
        }

        // URL
        String url = "/resource-lists/users/"
                + Uri.encode(ImsModule.getImsUserProfile().getPublicUri())
                + "/index/~~/resource-lists/list%5B@name=%22rcs%22%5D/entry%5B@uri=%22"
                + Uri.encode(PhoneUtils.formatContactIdToUri(contact).toString()) + "%22%5D";

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
    }

    /**
     * Remove a contact from the granted contacts list
     * 
     * @param contact Contact
     * @return Response
     * @throws PayloadException
     * @throws NetworkException
     */
    public HttpResponse removeContactFromGrantedList(ContactId contact) throws PayloadException,
            NetworkException {
        if (sLogger.isActivated()) {
            sLogger.info("Remove " + contact + " from granted list");
        }

        // URL
        String url = "/resource-lists/users/"
                + Uri.encode(ImsModule.getImsUserProfile().getPublicUri())
                + "/index/~~/resource-lists/list%5B@name=%22rcs%22%5D/entry%5B@uri=%22"
                + Uri.encode(PhoneUtils.formatContactIdToUri(contact).toString()) + "%22%5D";

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
     * @throws NetworkException
     * @throws PayloadException
     */
    public Set<ContactId> getGrantedContacts() throws PayloadException, NetworkException {
        try {
            if (sLogger.isActivated()) {
                sLogger.info("Get granted contacts list");
            }
            String url = "/resource-lists/users/"
                    + Uri.encode(ImsModule.getImsUserProfile().getPublicUri())
                    + "/index/~~/resource-lists/list%5B@name=%22rcs%22%5D";
            HttpResponse response = sendRequestToXDMS(new HttpGetRequest(url));
            if (!response.isSuccessfullResponse()) {
                throw new PayloadException(new StringBuilder(
                        "Can't get granted contacts list, Error Response :  ")
                        .append(response.getResponseCode()).append("!").toString());
            }
            if (sLogger.isActivated()) {
                sLogger.info("Granted contacts list has been read with success");
            }
            InputSource input = new InputSource(new ByteArrayInputStream(response.getContent()));
            XcapResponseParser parser = new XcapResponseParser(input);
            return convertListOfUrisToSetOfContactId(parser.getUris());

        } catch (ParserConfigurationException e) {
            throw new PayloadException("Unable to get granted contacts list!", e);

        } catch (SAXException e) {
            throw new PayloadException("Unable to get granted contacts list!", e);

        } catch (IOException e) {
            throw new NetworkException("Unable to get granted contacts list!", e);
        }
    }

    /**
     * Remove a contact from the blocked contacts list
     * 
     * @param contact Contact
     * @return Response
     * @throws PayloadException
     * @throws NetworkException
     */
    public HttpResponse removeContactFromBlockedList(ContactId contact) throws PayloadException,
            NetworkException {
        if (sLogger.isActivated()) {
            sLogger.info("Remove " + contact + " from blocked list");
        }
        String url = "/resource-lists/users/"
                + Uri.encode(ImsModule.getImsUserProfile().getPublicUri())
                + "/index/~~/resource-lists/list%5B@name=%22rcs_blockedcontacts%22%5D/entry%5B@uri=%22"
                + Uri.encode(PhoneUtils.formatContactIdToUri(contact).toString()) + "%22%5D";
        return sendRequestToXDMS(new HttpDeleteRequest(url));
    }

    /**
     * Returns the list of blocked contacts
     * 
     * @return List
     * @throws PayloadException
     * @throws NetworkException
     */
    public Set<ContactId> getBlockedContacts() throws PayloadException, NetworkException {
        try {
            if (sLogger.isActivated()) {
                sLogger.info("Get blocked contacts list");
            }
            String url = "/resource-lists/users/"
                    + Uri.encode(ImsModule.getImsUserProfile().getPublicUri())
                    + "/index/~~/resource-lists/list%5B@name=%22rcs_blockedcontacts%22%5D";
            HttpResponse response = sendRequestToXDMS(new HttpGetRequest(url));
            if (!response.isSuccessfullResponse()) {
                throw new PayloadException(new StringBuilder(
                        "Can't get blocked contacts list, Error Response :  ")
                        .append(response.getResponseCode()).append("!").toString());
            }
            if (sLogger.isActivated()) {
                sLogger.info("Blocked contacts list has been read with success");
            }
            InputSource input = new InputSource(new ByteArrayInputStream(response.getContent()));
            XcapResponseParser parser = new XcapResponseParser(input);
            return convertListOfUrisToSetOfContactId(parser.getUris());

        } catch (ParserConfigurationException e) {
            throw new PayloadException("Unable to get blocked contacts list!", e);

        } catch (SAXException e) {
            throw new PayloadException("Unable to get blocked contacts list!", e);

        } catch (IOException e) {
            throw new NetworkException("Unable to get blocked contacts list!", e);
        }
    }

    /**
     * Add a contact to the revoked contacts list
     * 
     * @param contact Contact
     * @return Response
     * @throws PayloadException
     * @throws NetworkException
     */
    public HttpResponse addContactToRevokedList(ContactId contact) throws PayloadException,
            NetworkException {
        if (sLogger.isActivated()) {
            sLogger.info("Add " + contact + " to revoked list");
        }
        String url = "/resource-lists/users/"
                + Uri.encode(ImsModule.getImsUserProfile().getPublicUri())
                + "/index/~~/resource-lists/list%5B@name=%22rcs_revokedcontacts%22%5D/entry%5B@uri=%22"
                + Uri.encode(PhoneUtils.formatContactIdToUri(contact).toString()) + "%22%5D";
        String content = "<entry uri='" + contact + "'></entry>";
        return sendRequestToXDMS(new HttpPutRequest(url, content, "application/xcap-el+xml"));
    }

    /**
     * Remove a contact from the revoked contacts list
     * 
     * @param contact Contact
     * @return Response
     * @throws PayloadException
     * @throws NetworkException
     */
    public HttpResponse removeContactFromRevokedList(ContactId contact) throws PayloadException,
            NetworkException {
        if (sLogger.isActivated()) {
            sLogger.info("Remove " + contact + " from revoked list");
        }
        String url = "/resource-lists/users/"
                + Uri.encode(ImsModule.getImsUserProfile().getPublicUri())
                + "/index/~~/resource-lists/list%5B@name=%22rcs_revokedcontacts%22%5D/entry%5B@uri=%22"
                + Uri.encode(PhoneUtils.formatContactIdToUri(contact).toString()) + "%22%5D";
        return sendRequestToXDMS(new HttpDeleteRequest(url));
    }

    /**
     * Returns the photo icon URL
     * 
     * @return URL
     */
    public String getEndUserPhotoIconUrl() {
        return xdmServerAddr + "/org.openmobilealliance.pres-content/users/"
                + Uri.encode(ImsModule.getImsUserProfile().getPublicUri())
                + "/oma_status-icon/rcs_status_icon";
    }

    /**
     * Upload the end user photo
     * 
     * @param photo Photo icon
     * @return Response
     * @throws PayloadException
     * @throws NetworkException
     */
    public HttpResponse uploadEndUserPhoto(PhotoIcon photo) throws PayloadException,
            NetworkException {
        if (sLogger.isActivated()) {
            sLogger.info("Upload the end user photo");
        }
        String data = Base64.encodeBase64ToString(photo.getContent());
        String content = new StringBuilder("<?xml version=\"1.0\" encoding=\"").append(UTF8_STR)
                .append("\"?>").append(CRLF)
                .append("<content xmlns=\"urn:oma:xml:prs:pres-content\">").append(CRLF)
                .append("<mime-type>").append(photo.getType()).append("</mime-type>").append(CRLF)
                .append("<encoding>base64</encoding>").append(CRLF).append("<data>").append(data)
                .append("</data>").append(CRLF).append("</content>").toString();
        String url = "/org.openmobilealliance.pres-content/users/"
                + Uri.encode(ImsModule.getImsUserProfile().getPublicUri())
                + "/oma_status-icon/rcs_status_icon";
        return sendRequestToXDMS(new HttpPutRequest(url, content,
                "application/vnd.oma.pres-content+xml"));
    }

    /**
     * Delete the end user photo
     * 
     * @return Response
     * @throws PayloadException
     * @throws NetworkException
     */
    public HttpResponse deleteEndUserPhoto() throws PayloadException, NetworkException {
        if (sLogger.isActivated()) {
            sLogger.info("Delete the end user photo");
        }
        String url = "/org.openmobilealliance.pres-content/users/"
                + Uri.encode(ImsModule.getImsUserProfile().getPublicUri())
                + "/oma_status-icon/rcs_status_icon";
        return sendRequestToXDMS(new HttpDeleteRequest(url));
    }

    /**
     * Set presence info
     * 
     * @param info Presence info
     * @return Response
     * @throws PayloadException
     * @throws NetworkException
     */
    public HttpResponse setPresenceInfo(String info) throws PayloadException, NetworkException {
        if (sLogger.isActivated()) {
            sLogger.info("Update presence info");
        }
        String url = "/pidf-manipulation/users/"
                + Uri.encode(ImsModule.getImsUserProfile().getPublicUri()) + "/perm-presence";
        return sendRequestToXDMS(new HttpPutRequest(url, info, "application/pidf+xml"));
    }
}
