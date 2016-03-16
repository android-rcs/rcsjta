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

package com.gsma.rcs.core.ims.service;

import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.registration.HttpDigestRegistrationProcedure;
import com.gsma.rcs.core.ims.network.registration.RegistrationProcedure;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.security.HttpDigestMd5Authentication;
import com.gsma.rcs.core.ims.userprofile.UserProfile;

import java.text.ParseException;

import javax2.sip.InvalidArgumentException;
import javax2.sip.header.ProxyAuthenticateHeader;
import javax2.sip.header.ProxyAuthorizationHeader;

/**
 * HTTP Digest MD5 authentication agent for sessions
 * 
 * @author JM. Auffret
 */
public class SessionAuthenticationAgent {

    /**
     * HTTP Digest MD5 agent for session
     */
    private HttpDigestMd5Authentication mDigest = new HttpDigestMd5Authentication();

    /**
     * HTTP Digest MD5 agent for register (nonce caching procedure)
     */
    private HttpDigestMd5Authentication mRegisterDigest;

    /**
     * Constructor
     * 
     * @param imsModule IMS module
     */
    public SessionAuthenticationAgent(ImsModule imsModule) {
        /* Re-use the registration authentication (nonce caching) */
        RegistrationProcedure procedure = imsModule.getCurrentNetworkInterface()
                .getRegistrationManager().getRegistrationProcedure();
        if (procedure instanceof HttpDigestRegistrationProcedure) {
            mRegisterDigest = ((HttpDigestRegistrationProcedure) procedure).getHttpDigest();
        }
    }

    /**
     * Set the proxy authorization header on the INVITE request
     * 
     * @param request SIP request
     * @throws InvalidArgumentException
     * @throws ParseException
     */
    public void setProxyAuthorizationHeader(SipRequest request) throws InvalidArgumentException,
            ParseException {
        String realm = mDigest.getRealm();
        if (realm == null || mDigest.getNextnonce() == null) {
            return;
        }
        mDigest.updateNonceParameters();
        UserProfile profile = ImsModule.getImsUserProfile();
        String user = profile.getPrivateID();
        String password = profile.getPassword();
        String requestUri = request.getRequestURI();
        String nonceCounter = mDigest.buildNonceCounter();
        String response = mDigest.calculateResponse(user, password, request.getMethod(),
                requestUri, nonceCounter, request.getContent());

        /* Build the Proxy-Authorization header */
        StringBuilder auth = new StringBuilder("Digest username=\"").append(user)
                .append("\",uri=\"").append(requestUri).append("\",algorithm=MD5,realm=\"")
                .append(realm).append("\",nc=").append(nonceCounter).append(",nonce=\"")
                .append(mDigest.getNonce()).append("\",response=\"").append(response)
                .append("\",cnonce=\"").append(mDigest.getCnonce()).append("\"");

        String qop = mDigest.getQop();
        if (qop != null) {
            auth.append(",qop=").append(qop);
        }
        request.addHeader(ProxyAuthorizationHeader.NAME, auth.toString());
    }

    /**
     * Read parameters of the Proxy-Authenticate header
     * 
     * @param response SIP response
     */
    public void readProxyAuthenticateHeader(SipResponse response) {
        ProxyAuthenticateHeader header = (ProxyAuthenticateHeader) response
                .getHeader(ProxyAuthenticateHeader.NAME);
        if (header != null) {
            mDigest.setRealm(header.getRealm());
            mDigest.setQop(header.getQop());
            mDigest.setNextnonce(header.getNonce());
        }
    }

    /**
     * Set the authorization header on the INVITE request
     * 
     * @param request SIP request
     * @throws InvalidArgumentException
     * @throws ParseException
     */
    public void setAuthorizationHeader(SipRequest request) throws InvalidArgumentException,
            ParseException {
        String nextNonce = mRegisterDigest.getNextnonce();
        /* Re-use the registration authentication (nonce caching) */
        if (mRegisterDigest == null || nextNonce == null) {
            return;
        }
        mRegisterDigest.updateNonceParameters();

        /* Calculate response */
        UserProfile profile = ImsModule.getImsUserProfile();
        String user = profile.getPrivateID();
        String password = profile.getPassword();
        String requestUri = request.getRequestURI();
        String nonCounter = mRegisterDigest.buildNonceCounter();
        String response = mRegisterDigest.calculateResponse(user, password, request.getMethod(),
                requestUri, nonCounter, request.getContent());

        /* Build the Authorization header */
        StringBuilder auth = new StringBuilder("Digest username=\"").append(user)
                .append("\",uri=\"").append(requestUri).append("\",algorithm=MD5,realm=\"")
                .append(mRegisterDigest.getRealm()).append("\",nc=").append(nonCounter)
                .append(",nonce=\"").append(nextNonce).append("\",response=\"").append(response)
                .append("\",cnonce=\"").append(mRegisterDigest.getCnonce()).append("\"");
        String qop = mRegisterDigest.getQop();
        if (qop != null) {
            auth.append(",qop=").append(qop);
        }

        /* Set header in the SIP message */
        request.addHeader(ProxyAuthorizationHeader.NAME, auth.toString());
    }
}
