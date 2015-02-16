/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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

package com.gsma.rcs.core.ims.userprofile;

import java.util.ListIterator;
import java.util.Vector;

import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.PhoneUtils;
import com.gsma.services.rcs.contact.ContactId;

import javax2.sip.header.ExtensionHeader;
import javax2.sip.header.Header;

/**
 * User profile
 * 
 * @author JM. Auffret
 */
public class UserProfile {

    private static final String TEL_URI = "tel:";

    private static final String SIP_URI = "sip:";

    private static final char AT = '@';

    /**
     * User name
     */
    private ContactId mContact;

    /**
     * Private ID for HTTP digest
     */
    private String mPrivateID;

    /**
     * Password for HTTP digest
     */
    private String mPassword;

    /**
     * Realm for HTTP digest
     */
    private String mRealm;

    /**
     * Home domain
     */
    private String mHomeDomain;

    /**
     * XDM server address
     */
    private String mXdmServerAddr;

    /**
     * XDM server login
     */
    private String mXdmServerLogin;

    /**
     * XDM server password
     */
    private String mXdmServerPassword;

    /**
     * IM conference URI
     */
    private String mImConferenceUri;

    /**
     * Associated URIs
     */
    private Vector<String> mAssociatedUriList = new Vector<String>();

    /**
     * Preferred URI
     */
    private String mPreferredUri;

    private final RcsSettings mRcsSettings;

    /**
     * Constructor
     * 
     * @param contact Username
     * @param homeDomain Home domain
     * @param privateID Private id
     * @param password Password
     * @param realm Realm
     * @param xdmServerAddr XDM server address
     * @param xdmServerLogin Outbound proxy address
     * @param xdmServerPassword Outbound proxy address
     * @param imConferenceUri IM conference factory URI
     * @param rcsSettings
     */
    public UserProfile(ContactId contact, String homeDomain, String privateID, String password,
            String realm, String xdmServerAddr, String xdmServerLogin, String xdmServerPassword,
            String imConferenceUri, RcsSettings rcsSettings) {
        mContact = contact;
        mHomeDomain = homeDomain;
        mPrivateID = privateID;
        mPassword = password;
        mRealm = realm;
        mXdmServerAddr = xdmServerAddr;
        mXdmServerLogin = xdmServerLogin;
        mXdmServerPassword = xdmServerPassword;
        mImConferenceUri = imConferenceUri;
        mRcsSettings = rcsSettings;
        // Changed by Deutsche Telekom
        // Continuation from the changes done by "AS" on "2012-09-01"
        // this.preferredUri = "sip:" + username + "@" + homeDomain;
        mPreferredUri = getPublicUriForRegistration();
    }

    /**
     * Get the user name
     * 
     * @return Username
     */
    public ContactId getUsername() {
        return mContact;
    }

    /**
     * Set the user name
     * 
     * @param contact Contact Id
     */
    public void setUsername(ContactId contact) {
        mContact = contact;
    }

    /**
     * Get the user preferred URI
     * 
     * @return Preferred URI
     */
    public String getPreferredUri() {
        return mPreferredUri;
    }

    /**
     * Get the user public URI
     * 
     * @return Public URI
     */
    public String getPublicUri() {
        if (mPreferredUri == null) {
            // Changed by Deutsche Telekom
            // Continuation from the changes done by "AS" on "2012-09-01"
            return getPublicUriForRegistration();
        } else {
            return mPreferredUri;
        }
    }

    /**
     * Get the user public URI for registration
     * 
     * @return Public URI
     */
    public String getPublicUriForRegistration() {
        if (mRcsSettings.isTelUriFormatUsed()) {
            return new StringBuilder(TEL_URI).append(mContact).toString();
        }
        return new StringBuilder(SIP_URI).append(mContact).append(AT).append(mHomeDomain)
                .toString();
    }

    /**
     * Get the user public address
     * 
     * @return Public address
     */
    public String getPublicAddress() {
        String addr = getPublicUri();
        String displayName = mRcsSettings.getUserProfileImsDisplayName();
        if ((displayName != null) && (displayName.length() > 0)) {
            String number = PhoneUtils.extractNumberFromUri(addr);
            if (number != null && number.equals(displayName)) {
                // Do no insert display name if it is equal to the international number
                return addr;
            }
            addr = "\"" + displayName + "\" <" + addr + ">";
        }
        return addr;
    }

    /**
     * Set the user associated URIs
     * 
     * @param uris List of URIs
     */
    public void setAssociatedUri(ListIterator<Header> uris) {
        if (uris == null) {
            return;
        }

        String sipUri = null;
        String telUri = null;
        while (uris.hasNext()) {
            ExtensionHeader header = (ExtensionHeader) uris.next();
            String value = header.getValue();
            value = SipUtils.extractUriFromAddress(value);
            mAssociatedUriList.addElement(value);

            if (value.startsWith("sip:")) {
                sipUri = value;
            } else if (value.startsWith("tel:")) {
                telUri = value;
            }
        }

        if ((sipUri != null) && (telUri != null)) {
            mPreferredUri = telUri;
        } else if (telUri != null) {
            mPreferredUri = telUri;
        } else if (sipUri != null) {
            mPreferredUri = sipUri;
        }
    }

    /**
     * Get the private ID used for HTTP Digest authentication
     * 
     * @return Private ID
     */
    public String getPrivateID() {
        return mPrivateID;
    }

    /**
     * Returns the password used for HTTP Digest authentication
     * 
     * @return Password
     */
    public String getPassword() {
        return mPassword;
    }

    /**
     * Returns the realm used for HTTP Digest authentication
     * 
     * @return Realm
     */
    public String getRealm() {
        return mRealm;
    }

    /**
     * Returns the home domain
     * 
     * @return Home domain
     */
    public String getHomeDomain() {
        return mHomeDomain;
    }

    /**
     * Set the home domain
     * 
     * @param domain Home domain
     */
    public void setHomeDomain(String domain) {
        mHomeDomain = domain;
    }

    /**
     * Set the XDM server address
     * 
     * @param addr Server address
     */
    public void setXdmServerAddr(String addr) {
        mXdmServerAddr = addr;
    }

    /**
     * Returns the XDM server address
     * 
     * @return Server address
     */
    public String getXdmServerAddr() {
        return mXdmServerAddr;
    }

    /**
     * Set the XDM server login
     * 
     * @param login Login
     */
    public void setXdmServerLogin(String login) {
        mXdmServerLogin = login;
    }

    /**
     * Returns the XDM server login
     * 
     * @return Login
     */
    public String getXdmServerLogin() {
        return mXdmServerLogin;
    }

    /**
     * Set the XDM server password
     * 
     * @param pwd Password
     */
    public void setXdmServerPassword(String pwd) {
        mXdmServerPassword = pwd;
    }

    /**
     * Returns the XDM server password
     * 
     * @return Password
     */
    public String getXdmServerPassword() {
        return mXdmServerPassword;
    }

    /**
     * Set the IM conference URI
     * 
     * @param uri URI
     */
    public void setImConferenceUri(String uri) {
        mImConferenceUri = uri;
    }

    /**
     * Returns the IM conference URI
     * 
     * @return URI
     */
    public String getImConferenceUri() {
        return mImConferenceUri;
    }

    /**
     * Returns the profile value as string
     * 
     * @return String
     */
    public String toString() {
        String result = "IMS username=" + mContact + ", " + "IMS private ID=" + mPrivateID + ", "
                + "IMS password=" + mPassword + ", " + "IMS home domain=" + mHomeDomain + ", "
                + "XDM server=" + mXdmServerAddr + ", " + "XDM login=" + mXdmServerLogin + ", "
                + "XDM password=" + mXdmServerPassword + ", " + "IM Conference URI="
                + mImConferenceUri;
        return result;
    }
}
