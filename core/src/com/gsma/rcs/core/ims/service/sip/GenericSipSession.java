/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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

import com.gsma.rcs.core.ims.network.sip.FeatureTags;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipException;
import com.gsma.rcs.core.ims.protocol.sip.SipPayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.service.ImsService;
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

import java.io.IOException;
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

    private static final Logger sLogger = Logger.getLogger(GenericSipSession.class.getSimpleName());

    /**
     * Constructor
     * 
     * @param parent IMS service
     * @param contact Remote contactId
     * @param featureTag Feature tag
     * @param rcsSettings RCS settings accessor
     * @param timestamp Local timestamp for the session
     * @param contactManager Contact manager accessor
     */
    public GenericSipSession(ImsService parent, ContactId contact, String featureTag,
            RcsSettings rcsSettings, long timestamp, ContactManager contactManager) {
        super(parent, contact, PhoneUtils.formatContactIdToUri(contact), rcsSettings, timestamp,
                contactManager);

        // Set the service feature tag
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
     * @throws SipPayloadException
     */
    public SipRequest createInvite() throws SipPayloadException {
        String ext = new StringBuilder(FeatureTags.FEATURE_3GPP).append("=\"")
                .append(FeatureTags.FEATURE_3GPP_EXTENSION).append("\"").toString();
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
            throw new SipPayloadException(
                    "Can't add SIP headertype ".concat(FeatureTags.FEATURE_3GPP_SERVICE_EXTENSION),
                    e);
        }

        return invite;
    }

    /**
     * Create 200 OK response
     * 
     * @return Response
     * @throws SipException
     */
    public SipResponse create200OKResponse() throws SipException {
        String ext = FeatureTags.FEATURE_3GPP + "=\"" + FeatureTags.FEATURE_3GPP_EXTENSION + "\"";
        SipResponse resp = SipMessageFactory.create200OkInviteResponse(getDialogPath(),
                new String[] {
                        getFeatureTag(), ext
                }, new String[] {
                        getFeatureTag(), ext, SipUtils.EXPLICIT_REQUIRE
                }, getDialogPath().getLocalContent());
        return resp;
    }

    /**
     * Prepare media session
     * 
     * @throws IOException
     */
    public abstract void prepareMediaSession() throws IOException;

    /**
     * Start media transfer
     * 
     * @throws IOException
     */
    public abstract void startMediaTransfer() throws IOException;

    /**
     * Close media session
     */
    public abstract void closeMediaSession();

    /**
     * Handle error
     * 
     * @param error Error
     */
    public void handleError(ImsServiceError error) {
        if (isSessionInterrupted()) {
            return;
        }

        sLogger.error(new StringBuilder("Session error: ").append(error.getErrorCode())
                .append(", reason=").append(error.getMessage()).toString());

        // Close media session
        closeMediaSession();

        // Remove the current session
        removeSession();

        ContactId contact = getRemoteContact();
        for (ImsSessionListener listener : getListeners()) {
            ((SipSessionListener) listener).handleSessionError(contact, new SipSessionError(error));
        }
    }

    @Override
    public void receiveBye(SipRequest bye) {
        super.receiveBye(bye);
        ContactId remote = getRemoteContact();
        for (ImsSessionListener listener : getListeners()) {
            listener.handleSessionAborted(remote, TerminationReason.TERMINATION_BY_REMOTE);
        }
        getImsService().getImsModule().getCapabilityService().requestContactCapabilities(remote);
    }

    @Override
    public void receiveCancel(SipRequest cancel) {
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
}
