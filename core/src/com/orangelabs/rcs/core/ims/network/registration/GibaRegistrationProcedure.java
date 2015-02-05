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

package com.orangelabs.rcs.core.ims.network.registration;

import java.util.ListIterator;

import javax2.sip.address.Address;
import javax2.sip.address.SipURI;
import javax2.sip.address.URI;
import javax2.sip.header.ExtensionHeader;
import javax2.sip.header.Header;
import android.content.Context;
import android.telephony.TelephonyManager;

import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.utils.ContactUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * GIBA or early-IMS registration procedure
 * 
 * @author jexa7410
 */
public class GibaRegistrationProcedure extends RegistrationProcedure {
    /**
     * IMSI
     */
    private String imsi = null;

    /**
     * MNC
     */
    private String mnc = null;

    /**
     * MCC
     */
    private String mcc = null;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     */
    public GibaRegistrationProcedure() {
    }

    /**
     * Initialize procedure
     */
    public void init() {
        TelephonyManager mgr = (TelephonyManager) AndroidFactory.getApplicationContext()
                .getSystemService(Context.TELEPHONY_SERVICE);
        imsi = mgr.getSubscriberId();
        String mcc_mnc = mgr.getSimOperator();
        mcc = mcc_mnc.substring(0, 3);
        mnc = mcc_mnc.substring(3);
        if (mcc_mnc.length() == 5) {
            mnc = "0" + mnc;
        }
    }

    /**
     * Returns the home domain name
     * 
     * @return Domain name
     */
    public String getHomeDomain() {
        return "ims.mnc" + mnc + ".mcc" + mcc + ".3gppnetwork.org";
    }

    /**
     * Returns the public URI or IMPU for registration
     * 
     * @return Public URI
     */
    public String getPublicUri() {
        // Derived IMPU from IMSI: <IMSI>@mnc<MNC>.mcc<MCC>.3gppnetwork.org
        return "sip:" + imsi + "@" + getHomeDomain();
    }

    /**
     * Write the security header to REGISTER request
     * 
     * @param request Request
     */
    public void writeSecurityHeader(SipRequest request) {
        // Nothing to do here
    }

    /**
     * Read the security header from REGISTER response
     * 
     * @param response Response
     * @throws CoreException
     */
    public void readSecurityHeader(SipResponse response) throws CoreException {
        try {
            // Read the associated-URI from the 200 OK response
            ListIterator<Header> list = response.getHeaders(SipUtils.HEADER_P_ASSOCIATED_URI);
            SipURI sipUri = null;
            while (list.hasNext()) {
                ExtensionHeader associatedHeader = (ExtensionHeader) list.next();
                Address sipAddr = SipUtils.ADDR_FACTORY.createAddress(associatedHeader.getValue());
                URI uri = sipAddr.getURI();
                if (uri instanceof SipURI) {
                    // SIP-URI
                    sipUri = (SipURI) sipAddr.getURI();
                }
            }
            if (sipUri == null) {
                throw new CoreException("No SIP-URI found in the P-Associated-URI header");
            }

            // Update the user profile
            ImsModule.IMS_USER_PROFILE.setUsername(ContactUtils.createContactId(sipUri.getUser()));
            ImsModule.IMS_USER_PROFILE.setHomeDomain(sipUri.getHost());
            ImsModule.IMS_USER_PROFILE.setXdmServerLogin("sip:" + sipUri.getUser() + "@"
                    + sipUri.getHost());
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Can't read a SIP-URI from the P-Associated-URI header", e);
            }
            throw new CoreException("Bad P-Associated-URI header");
        }
    }
}
