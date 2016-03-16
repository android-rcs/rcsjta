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

package com.gsma.rcs.core.ims.network.registration;

import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.userprofile.UserProfile;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.PhoneUtils;
import com.gsma.rcs.utils.logger.Logger;

import android.content.Context;
import android.telephony.TelephonyManager;

import java.text.ParseException;
import java.util.ListIterator;

import javax2.sip.address.Address;
import javax2.sip.address.SipURI;
import javax2.sip.address.URI;
import javax2.sip.header.ExtensionHeader;
import javax2.sip.header.Header;

/**
 * GIBA or early-IMS registration procedure
 * 
 * @author jexa7410
 */
public class GibaRegistrationProcedure extends RegistrationProcedure {
    /**
     * IMSI
     */
    private String mImsi;

    /**
     * MNC
     */
    private String mMnc;

    /**
     * MCC
     */
    private String mMcc;

    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(GibaRegistrationProcedure.class
            .getName());

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
        mImsi = mgr.getSubscriberId();
        String mcc_mnc = mgr.getSimOperator();
        mMcc = mcc_mnc.substring(0, 3);
        mMnc = mcc_mnc.substring(3);
        if (mcc_mnc.length() == 5) {
            mMnc = "0" + mMnc;
        }
    }

    /**
     * Returns the home domain name
     * 
     * @return Domain name
     */
    public String getHomeDomain() {
        return "ims.mnc" + mMnc + ".mcc" + mMcc + ".3gppnetwork.org";
    }

    /**
     * Returns the public URI or IMPU for registration
     * 
     * @return Public URI
     */
    public String getPublicUri() {
        // Derived IMPU from IMSI: <IMSI>@mnc<MNC>.mcc<MCC>.3gppnetwork.org
        return PhoneUtils.SIP_URI_HEADER + mImsi + "@" + getHomeDomain();
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
     * @throws PayloadException
     */
    public void readSecurityHeader(SipResponse response) throws PayloadException {
        SipURI sipUri = getSipUri(response.getHeaders(SipUtils.HEADER_P_ASSOCIATED_URI));

        String user = sipUri.getUser();
        PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(user);
        if (number == null) {
            throw new PayloadException(new StringBuilder(
                    "Can't read a SIP-URI from the P-Associated-URI header: invalid user '")
                    .append(user).append("'").toString());
        }
        UserProfile profile = ImsModule.getImsUserProfile();
        profile.setUsername(ContactUtil.createContactIdFromValidatedData(number));
        profile.setHomeDomain(sipUri.getHost());
        profile.setXdmServerLogin(PhoneUtils.SIP_URI_HEADER + sipUri.getUser() + "@"
                + sipUri.getHost());
    }

    /**
     * Return the associated SIP URI fetched from the 200 OK response
     * 
     * @param list
     * @return
     * @throws PayloadException
     */
    private SipURI getSipUri(ListIterator<Header> list) throws PayloadException {
        SipURI sipUri = null;
        while (list.hasNext()) {
            ExtensionHeader associatedHeader = (ExtensionHeader) list.next();
            String unparsedAddress = associatedHeader.getValue();
            try {
                Address sipAddr = SipUtils.ADDR_FACTORY.createAddress(unparsedAddress);
                URI uri = sipAddr.getURI();
                if (uri instanceof SipURI) {
                    sipUri = (SipURI) sipAddr.getURI();
                    break;
                }
            } catch (ParseException e) {
                if (sLogger.isActivated()) {
                    sLogger.warn(new StringBuilder("Failed to parse extension header '")
                            .append(unparsedAddress).append("'").toString());
                }
            }
        }
        if (sipUri == null) {
            throw new PayloadException("No SIP-URI found in the P-Associated-URI header!");
        }
        return sipUri;
    }
}
