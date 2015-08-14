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

package com.gsma.rcs.core.ims.service.ipcall;

import com.gsma.rcs.core.content.AudioContent;
import com.gsma.rcs.core.content.VideoContent;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.protocol.sip.SipNetworkException;
import com.gsma.rcs.core.ims.protocol.sip.SipPayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.ipcalldraft.IIPCallPlayer;
import com.gsma.rcs.service.ipcalldraft.IIPCallRenderer;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import javax2.sip.InvalidArgumentException;

/**
 * Originating IP call session
 * 
 * @author opob7414
 */
public class OriginatingIPCallSession extends IPCallSession {

    /**
     * The sLogger
     */
    private static final Logger sLogger = Logger.getLogger(OriginatingIPCallSession.class
            .getSimpleName());

    /**
     * Constructor
     * 
     * @param parent IMS service
     * @param contact Remote contact identifier
     * @param audioContent Audio content
     * @param videoContent Video content
     * @param player IP call player
     * @param renderer IP call renderer
     * @param rcsSettings
     * @param timestamp Local timestamp for the session
     * @param contactManager
     */
    public OriginatingIPCallSession(ImsService parent, ContactId contact,
            AudioContent audioContent, VideoContent videoContent, IIPCallPlayer player,
            IIPCallRenderer renderer, RcsSettings rcsSettings, long timestamp,
            ContactManager contactManager) {
        super(parent, contact, audioContent, videoContent, rcsSettings, timestamp, contactManager);

        // Set the player
        setPlayer(player);

        // Set the renderer
        setRenderer(renderer);

        // Create dialog path
        createOriginatingDialogPath();
    }

    /**
     * Background processing
     */
    public void run() {
        try {
            if (sLogger.isActivated()) {
                sLogger.info("Initiate a new IP call session as originating");
            }
            if (getAudioContent() == null) {
                handleError(new IPCallError(IPCallError.UNSUPPORTED_AUDIO_TYPE,
                        "Audio codec not supported"));
                return;
            }
            String sdp = buildAudioVideoSdpProposal();
            getDialogPath().setLocalContent(sdp);
            if (sLogger.isActivated()) {
                sLogger.info("Send INVITE");
            }
            SipRequest invite;
            if (getVideoContent() == null) {
                invite = SipMessageFactory.createInvite(getDialogPath(),
                        IPCallService.FEATURE_TAGS_IP_VOICE_CALL, sdp);
            } else {
                invite = SipMessageFactory.createInvite(getDialogPath(),
                        IPCallService.FEATURE_TAGS_IP_VIDEO_CALL, sdp);
            }
            getAuthenticationAgent().setAuthorizationHeader(invite);
            getDialogPath().setInvite(invite);
            sendInvite(invite);
        } catch (SipPayloadException e) {
            sLogger.error("Session initiation has failed!", e);
            handleError(new IPCallError(IPCallError.UNEXPECTED_EXCEPTION, e.getMessage()));
        } catch (SipNetworkException e) {
            if (sLogger.isActivated()) {
                sLogger.debug(e.getMessage());
            }
            handleError(new IPCallError(IPCallError.UNEXPECTED_EXCEPTION, e.getMessage()));
        } catch (InvalidArgumentException e) {
            sLogger.error("Session initiation has failed!", e);
            handleError(new IPCallError(IPCallError.UNEXPECTED_EXCEPTION, e.getMessage()));
        } catch (RuntimeException e) {
            /*
             * Normally we are not allowed to catch runtime exceptions as these are genuine bugs
             * which should be handled/fixed within the code. However the cases when we are
             * executing operations on a thread unhandling such exceptions will eventually lead to
             * exit the system and thus can bring the whole system down, which is not intended.
             */
            sLogger.error("Session initiation has failed!", e);
            handleError(new IPCallError(IPCallError.UNEXPECTED_EXCEPTION, e.getMessage()));
        }
    }

    @Override
    public SipRequest createInvite() throws SipPayloadException {
        if (getVideoContent() == null) {
            // Voice call
            return SipMessageFactory.createInvite(getDialogPath(),
                    IPCallService.FEATURE_TAGS_IP_VOICE_CALL, getDialogPath().getLocalContent());
        } else {
            // Visio call
            return SipMessageFactory.createInvite(getDialogPath(),
                    IPCallService.FEATURE_TAGS_IP_VIDEO_CALL, getDialogPath().getLocalContent());
        }

    }

    @Override
    public boolean isInitiatedByRemote() {
        return false;
    }
}
