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

import android.os.RemoteException;

import java.io.IOException;

import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.rtp.media.MediaException;
import com.gsma.rcs.core.ims.protocol.sdp.SdpUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipNetworkException;
import com.gsma.rcs.core.ims.protocol.sip.SipPayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;

public class IPCall_HoldInactive extends CallHoldManager {

    public IPCall_HoldInactive(IPCallSession session) {
        super(session);
    }

    @Override
    public void setCallHold(boolean callHoldAction) throws SipPayloadException, SipNetworkException {
        try {
            // Build SDP
            String sdp = buildCallHoldSdpProposal(callHoldAction);

            // Set SDP proposal as the local SDP part in the dialog path
            session.getDialogPath().setLocalContent(sdp);

            // get feature tags
            String[] featureTags = new String[] {};
            if (session.isTagPresent(sdp, "m=video")) { // audio+ video
                featureTags = IPCallService.FEATURE_TAGS_IP_VIDEO_CALL;
            } else { // audio only
                featureTags = IPCallService.FEATURE_TAGS_IP_VOICE_CALL;
            }

            // Create re-INVITE
            SipRequest reInvite = session.getUpdateSessionManager()
                    .createReInvite(featureTags, sdp);

            // Send re-INVITE
            int requestType = (callHoldAction) ? IPCallSession.SET_ON_HOLD
                    : IPCallSession.SET_ON_RESUME;
            session.getUpdateSessionManager().sendReInvite(reInvite, requestType);
        } catch (MediaException e) {
            throw new SipNetworkException("Failed to set call on hold!", e);
        }
    }

    private String buildCallHoldSdpProposal(boolean action) throws MediaException {
        try {
            StringBuilder sdpBuilder = new StringBuilder("v=0").append(SipUtils.CRLF)
                    .append("o=- ");

            final String ntpTime = SipUtils.constructNTPtime(System.currentTimeMillis());
            sdpBuilder.append(ntpTime).append(SipUtils.WHITESPACE).append(ntpTime)
                    .append(SipUtils.WHITESPACE);

            final String ipAddress = SdpUtils.formatAddressType(session.getDialogPath()
                    .getSipStack().getLocalIpAddress());
            sdpBuilder.append(ipAddress).append(SipUtils.CRLF).append("s=-").append(SipUtils.CRLF)
                    .append("c=").append(ipAddress).append(SipUtils.CRLF).append("t=0 0")
                    .append(SipUtils.CRLF);

            final String aVar = (action) ? "a=inactive" : "a=sendrcv";
            StringBuilder audioSdp = new StringBuilder(AudioSdpBuilder.buildSdpOffer(session
                    .getPlayer().getSupportedAudioCodecs(), session.getPlayer()
                    .getLocalAudioRtpPort())).append(aVar).append(SipUtils.CRLF);
            sdpBuilder.append(audioSdp);

            if ((session.getVideoContent() != null) && (session.getPlayer() != null)
                    && (session.getRenderer() != null)) {
                /* video sdp */
                sdpBuilder
                        .append(VideoSdpBuilder.buildSdpOfferWithOrientation(session.getPlayer()
                                .getSupportedVideoCodecs(), session.getRenderer()
                                .getLocalVideoRtpPort())).append(aVar).append(SipUtils.CRLF);
            }
            return sdpBuilder.toString();

        } catch (RemoteException e) {
            throw new MediaException("Failed to build sdp for audio content!", e);
        }
    }

    public void prepareSession() {

    }

    @Override
    public void setCallHold(boolean callHoldAction, SipRequest reInvite) {
        // Not used in IPCall_HoldInactive class

    }

}
