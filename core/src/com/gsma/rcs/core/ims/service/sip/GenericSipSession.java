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

package com.gsma.rcs.core.ims.service.sip;

import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.sip.FeatureTags;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.service.ImsServiceError;
import com.gsma.rcs.core.ims.service.ImsServiceSession;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.capability.CapabilityUtils;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.PhoneUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import gov2.nist.javax2.sip.header.ims.PPreferredServiceHeader;

import java.text.ParseException;
import java.util.Set;

import javax2.sip.header.ExtensionHeader;

/**
 * Abstract generic SIP session
 * 
 * @author jexa7410
 */
public abstract class GenericSipSession extends ImsServiceSession {

    private final String mFeatureTag;

    private final SipService mSipService;

    private static final Logger sLogger = Logger.getLogger(GenericSipSession.class.getSimpleName());

    /**
     * Constructor
     * 
     * @param parent SIP service
     * @param contact Remote contactId
     * @param featureTag Feature tag
     * @param rcsSettings RCS settings accessor
     * @param timestamp Local timestamp for the session
     * @param contactManager Contact manager accessor
     */
    public GenericSipSession(SipService parent, ContactId contact, String featureTag,
            RcsSettings rcsSettings, long timestamp, ContactManager contactManager) {
        super(parent, contact, PhoneUtils.formatContactIdToUri(contact), rcsSettings, timestamp,
                contactManager);
        mSipService = parent;
        mFeatureTag = featureTag;
    }

    /**
     * Returns feature tag of the service
     * 
     * @return Feature tag
     */
    public String getFeatureTag() {
        return mFeatureTag;
    }

    /**
     * Returns the service ID
     * 
     * @return Service ID
     */
    public String getServiceId() {
        return CapabilityUtils.extractServiceId(mFeatureTag);
    }

    /**
     * Create an INVITE request
     * 
     * @return Request
     * @throws PayloadException
     */
    public SipRequest createInvite() throws PayloadException {
        String ext = FeatureTags.FEATURE_3GPP + "=\"" + FeatureTags.FEATURE_3GPP_EXTENSION + "\"";
        SipRequest invite = SipMessageFactory.createInvite(getDialogPath(), new String[] {
                getFeatureTag(), ext
        }, new String[] {
                getFeatureTag(), ext, SipUtils.EXPLICIT_REQUIRE
        }, getDialogPath().getLocalContent());

        try {
            ExtensionHeader header = (ExtensionHeader) SipUtils.HEADER_FACTORY.createHeader(
                    PPreferredServiceHeader.NAME, FeatureTags.FEATURE_3GPP_SERVICE_EXTENSION);
            invite.getStackMessage().addHeader(header);

        } catch (ParseException e) {
            throw new PayloadException(
                    "Can't add SIP headertype ".concat(FeatureTags.FEATURE_3GPP_SERVICE_EXTENSION),
                    e);
        }

        return invite;
    }

    /**
     * Create 200 OK response
     * 
     * @return Response
     * @throws PayloadException
     */
    public SipResponse create200OKResponse() throws PayloadException {
        String ext = FeatureTags.FEATURE_3GPP + "=\"" + FeatureTags.FEATURE_3GPP_EXTENSION + "\"";
        return SipMessageFactory.create200OkInviteResponse(getDialogPath(), new String[] {
                getFeatureTag(), ext
        }, new String[] {
                getFeatureTag(), ext, SipUtils.EXPLICIT_REQUIRE
        }, getDialogPath().getLocalContent());
    }

    /**
     * Prepare media session
     * 
     * @throws NetworkException
     */
    public abstract void prepareMediaSession() throws NetworkException;

    /**
     * Start media transfer
     */
    public abstract void startMediaTransfer();

    /**
     * Close media session
     */
    public abstract void closeMediaSession();

    @Override
    public void handleError(ImsServiceError error) {
        if (isSessionInterrupted()) {
            return;
        }
        sLogger.error("Session error: " + error.getErrorCode() + ", reason=" + error.getMessage());
        closeMediaSession();
        removeSession();
        ContactId contact = getRemoteContact();
        for (ImsSessionListener listener : getListeners()) {
            ((SipSessionListener) listener).onSessionError(contact, new SipSessionError(error));
        }
    }

    @Override
    public void receiveBye(SipRequest bye) throws PayloadException, NetworkException {
        super.receiveBye(bye);
        ContactId remote = getRemoteContact();
        for (ImsSessionListener listener : getListeners()) {
            listener.onSessionAborted(remote, TerminationReason.TERMINATION_BY_REMOTE);
        }
        getImsService().getImsModule().getCapabilityService().requestContactCapabilities(remote);
    }

    @Override
    public void receiveCancel(SipRequest cancel) throws NetworkException, PayloadException {
        super.receiveCancel(cancel);
        // Request capabilities to the remote
        getImsService().getImsModule().getCapabilityService()
                .requestContactCapabilities(getRemoteContact());
    }

    /**
     * Gets the IARI feature tag from the set of feature tags
     * 
     * @param featureTags The set of feature tags
     * @return the IARI feature tag or null
     */
    public static String getIariFeatureTag(Set<String> featureTags) {
        for (String tag : featureTags) {
            if (tag.startsWith(FeatureTags.FEATURE_RCSE)) {
                return tag;
            }
        }
        return null;
    }

    /**
     * Gets SIP service
     * 
     * @return SIP service
     */
    public SipService getSipService() {
        return mSipService;
    }
}
