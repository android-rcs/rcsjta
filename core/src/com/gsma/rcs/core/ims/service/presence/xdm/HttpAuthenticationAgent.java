/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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

import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.security.HttpDigestMd5Authentication;
import com.gsma.rcs.core.ims.userprofile.UserProfile;

import javax2.sip.InvalidArgumentException;

/**
 * HTTP Digest MD5 authentication agent
 * 
 * @author JM. Auffret
 */
public class HttpAuthenticationAgent {

    /**
     * HTTP Digest MD5 agent
     */
    private HttpDigestMd5Authentication digest = new HttpDigestMd5Authentication();

    /**
     * Constructor
     */
    public HttpAuthenticationAgent() {
    }

    /**
     * Generate the authorization header
     * 
     * @param method Method used
     * @param requestUri Request Uri
     * @param body Entity body
     * @return authorizationHeader Authorization header
     * @throws InvalidArgumentException
     */
    private String generateAuthorizationHeader(String method, String requestUri, String body)
            throws InvalidArgumentException {
        digest.updateNonceParameters();
        UserProfile profile = ImsModule.getImsUserProfile();
        String user = profile.getXdmServerLogin();
        String password = profile.getXdmServerPassword();
        String response = digest.calculateResponse(user, password, method, requestUri,
                digest.buildNonceCounter(), body);
        StringBuilder auth = new StringBuilder("Authorization: Digest username=\"").append(user)
                .append("\",realm=\"").append(digest.getRealm()).append("\",nonce=\"")
                .append(digest.getNonce()).append("\",uri=\"").append(requestUri).append("\"");
        String opaque = digest.getOpaque();
        if (opaque != null) {
            auth.append(",opaque=\"").append(opaque).append("\"");
        }
        String qop = digest.getQop();
        if ((qop != null) && qop.startsWith("auth")) {
            auth.append(",qop=\"").append(qop).append("\",nc=").append(digest.buildNonceCounter())
                    .append(",cnonce=\"").append(digest.getCnonce()).append("\",response=\"")
                    .append(response).append("\"");
        }
        return auth.toString();
    }

    /**
     * Read the WWW-Authenticate header
     * 
     * @param header WWW-Authenticate header
     */
    public void readWwwAuthenticateHeader(String header) {
        if (header != null) {
            // Get domain name
            String value = null;
            int end = -1;
            int begin = header.toLowerCase().indexOf("realm=\"");
            if (begin != -1) {
                begin += 7;
                end = header.indexOf("\"", begin);
                value = header.substring(begin, end);
            }
            digest.setRealm(value);

            // Get opaque parameter
            value = null;
            end = -1;
            begin = header.toLowerCase().indexOf("opaque=\"");
            if (begin != -1) {
                begin += 8;
                end = header.indexOf("\"", begin);
                value = header.substring(begin, end);
            }
            digest.setOpaque(value);

            // Get qop
            value = null;
            end = -1;
            begin = header.toLowerCase().indexOf("qop=\"");
            if (begin != -1) {
                begin += 5;
                end = header.indexOf("\"", begin);
                value = header.substring(begin, end);
            }
            digest.setQop(value);

            // Get nonce to be used
            value = null;
            end = -1;
            begin = header.toLowerCase().indexOf("nonce=\"");
            if (begin != -1) {
                begin += 7;
                end = header.indexOf("\"", begin);
                value = header.substring(begin, end);
            }
            digest.setNextnonce(value);
        }
    }
}
