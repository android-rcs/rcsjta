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

package com.gsma.rcs.core.ims.protocol.http;

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.ims.security.HttpDigestMd5Authentication;
import com.gsma.rcs.utils.Base64;

/**
 * HTTP Digest MD5 authentication agent
 * 
 * @author JM. Auffret
 * @author Deutsche Telekom
 */
public class HttpAuthenticationAgent {

    private final String mServerLogin;

    private final String mServerPwd;

    /**
     * HTTP Digest MD5 agent
     */
    private HttpDigestMd5Authentication mDigest = new HttpDigestMd5Authentication();

    /**
     * Controls if its a HTTP Digest authentication or Basic
     */
    private boolean mIsDigestAuthentication;

    /**
     * Constructor
     * 
     * @param login Server login
     * @param pwd Server pwd
     */
    public HttpAuthenticationAgent(String login, String pwd) {
        mServerLogin = login;
        mServerPwd = pwd;
    }

    /**
     * Generate the authorization header
     * 
     * @param method Method used
     * @param requestUri Request Uri
     * @param body Entity body
     * @return authorizationHeader Authorization header
     */
    public String generateAuthorizationHeader(String method, String requestUri, String body) {
        return "Authorization: ".concat(generateAuthorizationHeaderValue(method, requestUri, body));
    }

    /**
     * Generate the authorization header value
     * 
     * @param method Method used
     * @param requestUri Request Uri
     * @param body Entity body
     * @return authorizationHeader Authorization header value
     */
    public String generateAuthorizationHeaderValue(String method, String requestUri, String body) {
        /*
         * According to
         * "Rich Communication Suite 5.1 Advanced Communications - Services and Client Specification - Version 2.0 - 03 May 2013"
         * , the authentication should be performed using basic authentication or HTTP digest as
         * per[RFC2617]
         */
        if (!mIsDigestAuthentication) {
            /* Build the Basic Authorization header */
            return "Basic ".concat(Base64.encodeBase64ToString((new StringBuilder(mServerLogin)
                    .append(':').append(mServerPwd).toString()).getBytes(UTF8)));
        }

        mDigest.updateNonceParameters();

        /* Build the Authorization header */
        StringBuilder authValue = new StringBuilder("Digest username=\"").append(mServerLogin)
                .append("\"").append(",realm=\"").append(mDigest.getRealm()).append("\"")
                .append(",nonce=\"").append(mDigest.getNonce()).append("\"").append(",uri=\"")
                .append(requestUri).append("\"").append(",nc=").append(mDigest.buildNonceCounter())
                .append(",cnonce=\"").append(mDigest.getCnonce()).append("\"");

        String opaque = mDigest.getOpaque();
        if (opaque != null) {
            authValue.append(",opaque=\"").append(opaque).append("\"");
        }

        String qop = mDigest.getQop();
        if (qop != null && qop.startsWith("auth")) {
            authValue
                    .append(",qop=\"")
                    .append(qop)
                    .append("\"")
                    .append(",response=\"")
                    .append(mDigest.calculateResponse(mServerLogin, mServerPwd, method, requestUri,
                            mDigest.buildNonceCounter(), body)).append("\"");

        } else {
            authValue
                    .append(",response=\"")
                    .append(mDigest.calculateResponse(mServerLogin, mServerPwd, method, requestUri,
                            mDigest.buildNonceCounter(), "")).append("\"");
        }

        return authValue.toString();

    }

    /**
     * Read the WWW-Authenticate header
     * 
     * @param header WWW-Authenticate header
     */
    public void readWwwAuthenticateHeader(String header) {
        if (header != null) {
            // According to
            // "Rich Communication Suite 5.1 Advanced Communications - Services and Client Specification - Version 2.0 - 03 May 2013",
            // the authentication should be performed using basic authentication or HTTP digest as
            // per [RFC2617]
            mIsDigestAuthentication = header
                    .startsWith(HttpDigestMd5Authentication.HTTP_DIGEST_SCHEMA);
            if (!mIsDigestAuthentication) {
                return;
            }

            // Get domain name
            String value = getValue(header, "realm");
            mDigest.setRealm(value);

            // Get opaque parameter
            value = getValue(header, "opaque");
            mDigest.setOpaque(value);

            // Get qop
            value = getValue(header, "qop");
            mDigest.setQop(value);

            // Get nonce to be used
            value = getValue(header, "nonce");
            mDigest.setNextnonce(value);
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
            if (end == -1)
                end = header.length();
            value = header.substring(begin, end);
        } else {
            begin = header.toLowerCase().indexOf(key + "=");
            if (begin != -1) {
                begin += key.length() + 1;
                end = header.indexOf(",", begin);
                if (end == -1)
                    end = header.length();
                value = header.substring(begin, end);
            }
        }
        return value;
    }
}
