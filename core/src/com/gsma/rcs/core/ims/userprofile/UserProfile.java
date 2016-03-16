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

package com.gsma.rcs.core.ims.userprofile;

import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.PhoneUtils;
import com.gsma.services.rcs.contact.ContactId;

import android.net.Uri;
import android.text.TextUtils;

import java.util.ListIterator;

import javax2.sip.header.ExtensionHeader;
import javax2.sip.header.Header;

/**
 * User profile
 * 
 * @author JM. Auffret
 */
public class UserProfile {
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
    private Uri mXdmServerAddr;

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
    private Uri mImConference;

    /**
     * Preferred URI
     */
    private Uri mPreferred;

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
            String realm, Uri xdmServerAddr, String xdmServerLogin, String xdmServerPassword,
            Uri imConference, RcsSettings rcsSettings) {
        mContact = contact;
        mHomeDomain = homeDomain;
        mPrivateID = privateID;
        mPassword = password;
        mRealm = realm;
        mXdmServerAddr = xdmServerAddr;
        mXdmServerLogin = xdmServerLogin;
        mXdmServerPassword = xdmServerPassword;
        mImConference = imConference;
        mRcsSettings = rcsSettings;
        mPreferred = PhoneUtils.formatContactIdToUri(mContact);
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
    public Uri getPreferredUri() {
        return mPreferred;
    }

    /**
     * Get the user public URI
     * 
     * @return Public URI
     */
    // @FIXME: This method should return URI instead of String
    public String getPublicUri() {
        if (mPreferred == null) {
            return PhoneUtils.formatContactIdToUri(mContact).toString();
        }
        return mPreferred.toString();
    }

    private String formatAddressWithDisplayName(String displayName, String address) {
        return new StringBuilder("\"").append(displayName).append("\" <").append(address)
                .append(">").toString();
    }

    private String formatAddressWithDisplayName(String displayName, ContactId contact) {
        return new StringBuilder("\"").append(displayName).append("\" <")
                .append(PhoneUtils.formatContactIdToUri(contact)).append(">").toString();
    }

    /**
     * Get the user public address
     * 
     * @return Public address
     */
    public String getPublicAddress() {
        String addr = getPublicUri();
        String displayName = mRcsSettings.getUserProfileImsDisplayName();
        if (TextUtils.isEmpty(displayName)) {
            return addr;
        }
        PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(addr);
        if (number == null) {
            return formatAddressWithDisplayName(displayName, addr);
        }
        ContactId me = ContactUtil.createContactIdFromValidatedData(number);
        if (displayName.equals(me.toString())) {
            /* Do no insert display name if it is equal to the international number */
            return addr;
        }
        return formatAddressWithDisplayName(displayName, me);
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

            if (value.startsWith(PhoneUtils.SIP_URI_HEADER)) {
                sipUri = value;
            } else if (value.startsWith(PhoneUtils.TEL_URI_HEADER)) {
                telUri = value;
            }
        }

        if ((sipUri != null) && (telUri != null)) {
            mPreferred = Uri.parse(telUri);
        } else if (telUri != null) {
            mPreferred = Uri.parse(telUri);
        } else if (sipUri != null) {
            mPreferred = Uri.parse(sipUri);
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
    public void setXdmServerAddr(Uri addr) {
        mXdmServerAddr = addr;
    }

    /**
     * Returns the XDM server address
     * 
     * @return Server address
     */
    public Uri getXdmServerAddr() {
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
     * Returns the IM conference URI
     * 
     * @return URI
     */
    public Uri getImConferenceUri() {
        return mImConference;
    }

    /**
     * Returns the profile value as string
     * 
     * @return String
     */
    public String toString() {
        return new StringBuilder("IMS username=").append(mContact).append(", ")
                .append("IMS private ID=").append(mPrivateID).append(", ").append("IMS password=")
                .append(mPassword).append(", ").append("IMS home domain=").append(mHomeDomain)
                .append(", ").append("XDM server=").append(mXdmServerAddr).append(", ")
                .append("XDM login=").append(mXdmServerLogin).append(", ").append("XDM password=")
                .append(mXdmServerPassword).append(", ").append("IM Conference URI=")
                .append(mImConference).toString();
    }
}
