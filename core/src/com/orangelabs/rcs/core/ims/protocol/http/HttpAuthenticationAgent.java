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

package com.orangelabs.rcs.core.ims.protocol.http;

import static com.orangelabs.rcs.utils.StringUtils.UTF8;

import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.core.ims.security.HttpDigestMd5Authentication;
import com.orangelabs.rcs.utils.Base64;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * HTTP Digest MD5 authentication agent
 *
 * @author JM. Auffret
 * @author Deutsche Telekom
 */
public class HttpAuthenticationAgent {
	/**
	 * Login
	 */
	private String serverLogin;

	/**
	 * Password
	 */
	private String serverPwd;

	/**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * HTTP Digest MD5 agent
	 */
	private HttpDigestMd5Authentication digest = new HttpDigestMd5Authentication();

    /**
     * Controls if its a HTTP Digest authentication or Basic
     */
	private boolean isDigestAuthentication;

	/**
	 * Constructor
	 *
	 * @param login Server login
	 * @param pwd Server pwd
	 */
	public HttpAuthenticationAgent(String login, String pwd) {
		this.serverLogin = login;
		this.serverPwd = pwd;
	}

	/**
	 * Generate the authorization header
	 *
	 * @param method Method used
	 * @param requestUri Request Uri
	 * @param body Entity body
	 * @return authorizationHeader Authorization header
	 * @throws CoreException
	 */
	public String generateAuthorizationHeader(String method, String requestUri, String body) throws CoreException {
		return "Authorization: ".concat(generateAuthorizationHeaderValue(method, requestUri, body));
    }

	/**
	 * Generate the authorization header value
	 *
	 * @param method Method used
	 * @param requestUri Request Uri
	 * @param body Entity body
	 * @return authorizationHeader Authorization header value
	 * @throws CoreException
	 */
	public String generateAuthorizationHeaderValue(String method, String requestUri, String body) throws CoreException {
		try {
            // According to "Rich Communication Suite 5.1 Advanced Communications - Services and Client Specification - Version 2.0 - 03 May 2013",
            // the authentication should be performed using basic authentication or HTTP digest as per [RFC2617]
		    if (!isDigestAuthentication) {
	            // Build the Basic Authorization header
                return "Basic ".concat(Base64.encodeBase64ToString((new StringBuilder(serverLogin)
                        .append(':').append(serverPwd).toString())
                        .getBytes(UTF8)));
		    }

			digest.updateNonceParameters();

            // Build the Authorization header
            StringBuilder authValue = new StringBuilder("Digest username=\"").append(serverLogin)
                    .append("\"").append(",realm=\"").append(digest.getRealm()).append("\"")
                    .append(",nonce=\"").append(digest.getNonce()).append("\"").append(",uri=\"")
                    .append(requestUri).append("\"").append(",nc=")
                    .append(digest.buildNonceCounter()).append(",cnonce=\"")
                    .append(digest.getCnonce()).append("\"");

            String opaque = digest.getOpaque();
            if (opaque != null) {
                authValue.append(",opaque=\"").append(opaque).append("\"");
            }

            String qop = digest.getQop();
            if (qop != null && qop.startsWith("auth")) {
                authValue.append(",qop=\"").append(qop).append("\"")
                         .append(",response=\"")
                         .append(digest.calculateResponse(serverLogin, serverPwd, method, requestUri,
                                 digest.buildNonceCounter(), body)).append("\"");

            } else {
                authValue
                .append(",response=\"")
                .append(digest.calculateResponse(serverLogin, serverPwd, method, requestUri,
                        digest.buildNonceCounter(), "")).append("\"");
            }

            return authValue.toString();

		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Can't create the authorization value", e);
			}
			throw new CoreException("Can't create the authorization value");
		}
    }

	/**
	 * Read the WWW-Authenticate header
	 *
	 * @param header WWW-Authenticate header
	 */
	public void readWwwAuthenticateHeader(String header) {
        if (header != null) {
        	// According to "Rich Communication Suite 5.1 Advanced Communications - Services and Client Specification - Version 2.0 - 03 May 2013",
            // the authentication should be performed using basic authentication or HTTP digest as per [RFC2617]
            isDigestAuthentication = header.startsWith(HttpDigestMd5Authentication.HTTP_DIGEST_SCHEMA);
            if (!isDigestAuthentication) {
              return;
            }

            // Get domain name
            String value = getValue(header, "realm");
            digest.setRealm(value);

            // Get opaque parameter
            value = getValue(header, "opaque");
            digest.setOpaque(value);

            // Get qop
            value = getValue(header, "qop");
            digest.setQop(value);

            // Get nonce to be used
            value = getValue(header, "nonce");
            digest.setNextnonce(value);
        }
	}

    /**
     * Get the value of key in header
     *
     * @param header
     * @param key
     * @return value
     */
    private String getValue(String header, String key) {
        String value = null;
        int end = -1;
        int begin = header.toLowerCase().indexOf(key + "=\"");
        if (begin != -1) {
            begin += key.length() + 2;
            end = header.indexOf("\"", begin);
            if (end == -1) end = header.length();
            value = header.substring(begin, end);
        } else {
            begin = header.toLowerCase().indexOf(key + "=");
            if (begin != -1) {
                begin += key.length() + 1;
                end = header.indexOf(",", begin);
                if (end == -1) end = header.length();
                value = header.substring(begin, end);
            }
        }
        return value;
    }
}
