/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
 ******************************************************************************/

package com.orangelabs.rcs.core.ims.service.presence.xdm;

import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.security.HttpDigestMd5Authentication;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * HTTP Digest MD5 authentication agent
 * 
 * @author JM. Auffret
 */
public class HttpAuthenticationAgent {

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

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
     * @throws CoreException
     */
    public String generateAuthorizationHeader(String method, String requestUri, String body)
            throws CoreException {
        try {
            // Update nonce parameters
            digest.updateNonceParameters();

            // Calculate response
            String user = ImsModule.IMS_USER_PROFILE.getXdmServerLogin();
            String password = ImsModule.IMS_USER_PROFILE.getXdmServerPassword();
            String response = digest.calculateResponse(user, password, method, requestUri,
                    digest.buildNonceCounter(), body);

            // Build the Authorization header
            String auth = "Authorization: Digest username=\""
                    + ImsModule.IMS_USER_PROFILE.getXdmServerLogin() + "\"" + ",realm=\""
                    + digest.getRealm() + "\"" + ",nonce=\"" + digest.getNonce() + "\"" + ",uri=\""
                    + requestUri + "\"";
            String opaque = digest.getOpaque();
            if (opaque != null) {
                auth += ",opaque=\"" + opaque + "\"";
            }
            String qop = digest.getQop();
            if ((qop != null) && qop.startsWith("auth")) {
                auth += ",qop=\"" + qop + "\"" + ",nc=" + digest.buildNonceCounter() + ",cnonce=\""
                        + digest.getCnonce() + "\"" + ",response=\"" + response + "\"";
            }
            return auth;
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Can't create the authorization header", e);
            }
            throw new CoreException("Can't create the authorization header");
        }
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
