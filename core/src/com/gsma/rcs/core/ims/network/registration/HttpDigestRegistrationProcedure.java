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

package com.gsma.rcs.core.ims.network.registration;

import javax2.sip.header.AuthenticationInfoHeader;
import javax2.sip.header.AuthorizationHeader;
import javax2.sip.header.WWWAuthenticateHeader;

import com.gsma.rcs.core.CoreException;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.security.HttpDigestMd5Authentication;
import com.gsma.rcs.utils.PhoneUtils;
import com.gsma.rcs.utils.logger.Logger;

/**
 * HTTP Digest MD5 registration procedure
 * 
 * @author jexa7410
 * @author Deutsche Telekom AG
 */
public class HttpDigestRegistrationProcedure extends RegistrationProcedure {
    /**
     * HTTP Digest MD5 agent
     */
    private HttpDigestMd5Authentication mDigest;

    private Logger mLogger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     */
    public HttpDigestRegistrationProcedure() {
    }

    /**
     * Initialize procedure
     */
    public void init() {
        mDigest = new HttpDigestMd5Authentication();
    }

    /**
     * Returns the home domain name
     * 
     * @return Domain name
     */
    public String getHomeDomain() {
        return ImsModule.IMS_USER_PROFILE.getHomeDomain();
    }

    /**
     * Returns the public URI or IMPU for registration
     * 
     * @return Public URI
     */
    public String getPublicUri() {
        return PhoneUtils.SIP_URI_HEADER + ImsModule.IMS_USER_PROFILE.getUsername() + "@"
                + ImsModule.IMS_USER_PROFILE.getHomeDomain();
    }

    /**
     * Write security header to REGISTER request
     * 
     * @param request Request
     * @throws CoreException
     */
    public void writeSecurityHeader(SipRequest request) throws CoreException {
        if (mDigest == null) {
            return;
        }

        try {
            // Get Realm
            String realm = "";
            if (mDigest.getRealm() != null) {
                realm = mDigest.getRealm();
            } else {
                realm = ImsModule.IMS_USER_PROFILE.getRealm();
            }

            // Update nonce parameters
            String nonce = "";
            if (mDigest.getNextnonce() != null) {
                mDigest.updateNonceParameters();
                nonce = mDigest.getNonce();
            }

            // Calculate response
            String response = "";
            if (nonce.length() > 0) {
                String user = ImsModule.IMS_USER_PROFILE.getPrivateID();
                String password = ImsModule.IMS_USER_PROFILE.getPassword();
                response = mDigest.calculateResponse(user, password, request.getMethod(),
                        request.getRequestURI(), mDigest.buildNonceCounter(), request.getContent());
            }

            // Build the Authorization header
            String auth = "Digest username=\"" + ImsModule.IMS_USER_PROFILE.getPrivateID() + "\""
                    + ",uri=\"" + request.getRequestURI() + "\"" + ",algorithm=MD5" + ",realm=\""
                    + realm + "\"" + ",nonce=\"" + nonce + "\"" + ",response=\"" + response + "\"";
            String opaque = mDigest.getOpaque();
            if (opaque != null) {
                auth += ",opaque=\"" + opaque + "\"";
            }
            String qop = mDigest.getQop();
            if ((qop != null) && qop.startsWith("auth")) {
                auth += ",nc=" + mDigest.buildNonceCounter() + ",qop=" + qop + ",cnonce=\""
                        + mDigest.getCnonce() + "\"";
            }

            // Set header in the SIP message
            request.addHeader(AuthorizationHeader.NAME, auth);
        } catch (Exception e) {
            if (mLogger.isActivated()) {
                mLogger.error("Can't create the authorization header", e);
            }
            throw new CoreException("Can't write the security header");
        }
    }

    /**
     * Read security header from REGISTER response
     * 
     * @param response SIP response
     * @throws CoreException
     */
    public void readSecurityHeader(SipResponse response) throws CoreException {
        if (mDigest == null) {
            return;
        }

        WWWAuthenticateHeader wwwHeader = (WWWAuthenticateHeader) response
                .getHeader(WWWAuthenticateHeader.NAME);
        AuthenticationInfoHeader infoHeader = (AuthenticationInfoHeader) response
                .getHeader(AuthenticationInfoHeader.NAME);

        if (wwwHeader != null) {
            // Retrieve data from the header WWW-Authenticate (401 response)
            try {
                // Get domain name
                mDigest.setRealm(wwwHeader.getRealm());

                // Get opaque parameter
                mDigest.setOpaque(wwwHeader.getOpaque());

                // Get qop
                mDigest.setQop(wwwHeader.getQop());

                // Get nonce to be used
                mDigest.setNextnonce(wwwHeader.getNonce());
            } catch (Exception e) {
                if (mLogger.isActivated()) {
                    mLogger.error("Can't read the WWW-Authenticate header", e);
                }
                throw new CoreException("Can't read the security header");
            }
        } else if (infoHeader != null) {
            // Retrieve data from the header Authentication-Info (200 OK response)
            try {
                // Check if 200 OK really included Authentication-Info: nextnonce=""
                if (infoHeader.getNextNonce() != null) {
                    // Get nextnonce to be used
                    mDigest.setNextnonce(infoHeader.getNextNonce());
                }
            } catch (Exception e) {
                if (mLogger.isActivated()) {
                    mLogger.error("Can't read the authentication-info header", e);
                }
                throw new CoreException("Can't read the security header");
            }
        }
    }

    /**
     * Returns HTTP digest
     * 
     * @return HTTP digest
     */
    public HttpDigestMd5Authentication getHttpDigest() {
        return mDigest;
    }
}
