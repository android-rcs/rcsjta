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

package com.gsma.rcs.core.ims.service.sip.messaging;

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.sip.SipManager;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession;
import com.gsma.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.gsma.rcs.core.ims.protocol.sdp.MediaDescription;
import com.gsma.rcs.core.ims.protocol.sdp.SdpParser;
import com.gsma.rcs.core.ims.protocol.sdp.SdpUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.SessionTimerManager;
import com.gsma.rcs.core.ims.service.sip.GenericSipSession;
import com.gsma.rcs.core.ims.service.sip.SipService;
import com.gsma.rcs.core.ims.service.sip.SipSessionError;
import com.gsma.rcs.core.ims.service.sip.SipSessionListener;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Intent;

import java.util.Collection;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Terminating SIP MSRP session
 * 
 * @author jexa7410
 */
public class TerminatingSipMsrpSession extends GenericSipMsrpSession {

    private static final Logger sLogger = Logger.getLogger(TerminatingSipMsrpSession.class
            .getSimpleName());

    private final Intent mSessionInvite;

    private final ImsModule mImsModule;

    /**
     * Constructor
     * 
     * @param parent SIP service
     * @param invite Initial INVITE request
     * @param imsModule the IMS module
     * @param contact the remote contact
     * @param sessionInvite the session invitate
     * @param rcsSettings the ECS settings accessor
     * @param timestamp Local timestamp for the session
     * @param contactManager the contact manager
     */
    public TerminatingSipMsrpSession(SipService parent, SipRequest invite, ImsModule imsModule,
            ContactId contact, Intent sessionInvite, RcsSettings rcsSettings, long timestamp,
            ContactManager contactManager) {
        super(parent, contact, GenericSipSession.getIariFeatureTag(invite.getFeatureTags()),
                rcsSettings, timestamp, contactManager);
        mSessionInvite = sessionInvite;
        mImsModule = imsModule;
        createTerminatingDialogPath(invite);
    }

    @Override
    public void run() {
        final boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.info("Initiate a new MSRP session as terminating");
        }
        try {
            SipDialogPath dialogPath = getDialogPath();
            send180Ringing(dialogPath.getInvite(), dialogPath.getLocalTag());

            Collection<ImsSessionListener> listeners = getListeners();
            ContactId contact = getRemoteContact();
            for (ImsSessionListener listener : listeners) {
                ((SipSessionListener) listener).onInvitationReceived(contact, mSessionInvite);
            }

            InvitationStatus answer = waitInvitationAnswer();
            switch (answer) {
                case INVITATION_REJECTED_DECLINE:
                    /* Intentional fall through */
                case INVITATION_REJECTED_BUSY_HERE:
                    if (logActivated) {
                        sLogger.debug("Session has been rejected by user");
                    }
                    sendErrorResponse(dialogPath.getInvite(), dialogPath.getLocalTag(), answer);
                    removeSession();

                    for (ImsSessionListener listener : listeners) {
                        listener.onSessionRejected(contact, TerminationReason.TERMINATION_BY_USER);
                    }
                    return;

                case INVITATION_TIMEOUT:
                    if (logActivated) {
                        sLogger.debug("Session has been rejected on timeout");
                    }
                    /* Ringing period timeout */
                    send486Busy(dialogPath.getInvite(), dialogPath.getLocalTag());
                    removeSession();
                    for (ImsSessionListener listener : listeners) {
                        listener.onSessionRejected(contact,
                                TerminationReason.TERMINATION_BY_TIMEOUT);
                    }
                    return;

                case INVITATION_REJECTED_BY_SYSTEM:
                    if (logActivated) {
                        sLogger.debug("Session has been aborted by system");
                    }
                    removeSession();
                    return;

                case INVITATION_CANCELED:
                    if (logActivated) {
                        sLogger.debug("Session has been rejected by remote");
                    }
                    removeSession();
                    for (ImsSessionListener listener : listeners) {
                        listener.onSessionRejected(contact, TerminationReason.TERMINATION_BY_REMOTE);
                    }
                    return;

                case INVITATION_ACCEPTED:
                    setSessionAccepted();
                    for (ImsSessionListener listener : listeners) {
                        listener.onSessionAccepting(contact);
                    }
                    break;

                case INVITATION_DELETED:
                    if (logActivated) {
                        sLogger.debug("Session has been deleted");
                    }
                    removeSession();
                    return;

                default:
                    throw new IllegalArgumentException("Unknown invitation answer in run; answer="
                            + answer);
            }

            /* Parse the remote SDP part */
            final SipRequest invite = dialogPath.getInvite();
            String remoteSdp = invite.getSdpContent();
            SipUtils.assertContentIsNotNull(remoteSdp, invite);
            SdpParser parser = new SdpParser(remoteSdp.getBytes(UTF8));
            Vector<MediaDescription> media = parser.getMediaDescriptions();
            MediaDescription mediaDesc = media.elementAt(0);
            MediaAttribute attr1 = mediaDesc.getMediaAttribute("path");
            String remotePath = attr1.getValue();
            String remoteHost = SdpUtils.extractRemoteHost(parser.sessionDescription, mediaDesc);
            int remotePort = mediaDesc.mPort;

            /* Extract the "setup" parameter */
            String remoteSetup = "passive";
            MediaAttribute attr2 = mediaDesc.getMediaAttribute("setup");
            if (attr2 != null) {
                remoteSetup = attr2.getValue();
            }
            if (logActivated) {
                sLogger.debug("Remote setup attribute is ".concat(remoteSetup));
            }

            /* Set setup mode */
            String localSetup = createSetupAnswer(remoteSetup);
            if (logActivated) {
                sLogger.debug("Local setup attribute is ".concat(localSetup));
            }

            /* Extract "accept-type" */
            String[] acceptType = new String[0];
            MediaAttribute attr3 = mediaDesc.getMediaAttribute("accept-types");
            if (attr3 != null) {
                StringTokenizer st = new StringTokenizer(attr3.getValue(), " ");
                acceptType = new String[st.countTokens()];
                int i = 0;
                while (st.hasMoreTokens()) {
                    acceptType[i] = st.nextToken();
                    i++;
                }
            }

            /* Extract "accept-wrapped-type" */
            String[] acceptWrappedType = new String[0];
            MediaAttribute attr4 = mediaDesc.getMediaAttribute("accept-wrapped-types");
            if (attr4 != null) {
                StringTokenizer st = new StringTokenizer(attr4.getValue(), " ");
                acceptWrappedType = new String[st.countTokens()];
                int i = 0;
                while (st.hasMoreTokens()) {
                    acceptWrappedType[i] = st.nextToken();
                    i++;
                }
            }

            /* Build SDP answer */
            String sdp = generateSdp(localSetup, acceptType, acceptWrappedType);

            /* Set the local SDP part in the dialog path */
            dialogPath.setLocalContent(sdp);

            /* Test if the session should be interrupted */
            if (isInterrupted()) {
                if (logActivated) {
                    sLogger.debug("Session has been interrupted: end of processing");
                }
                return;
            }

            if (logActivated) {
                sLogger.info("Send 200 OK");
            }
            SipResponse resp = create200OKResponse();

            /* The signalisation is established */
            dialogPath.setSigEstablished();
            SipManager sipManager = mImsModule.getSipManager();
            SipTransactionContext ctx = sipManager.sendSipMessage(resp);

            /* Create the MSRP server session */
            if (localSetup.equals("passive")) {
                /* Passive mode: client wait a connection */
                MsrpSession session = getMsrpMgr().createMsrpServerSession(remotePath, this);
                session.setFailureReportOption(false);
                session.setSuccessReportOption(false);
                /* Open the MSRP session */
                getMsrpMgr().openMsrpSession();
                getMsrpMgr().sendEmptyChunk();
            }

            sipManager.waitResponse(ctx);

            /* Test if the session should be interrupted */
            if (isInterrupted()) {
                if (logActivated) {
                    sLogger.debug("Session has been interrupted: end of processing");
                }
                return;
            }

            /* Analyze the received response */
            if (ctx.isSipAck()) {
                if (logActivated) {
                    sLogger.info("ACK request received");
                }
                /* Create the MSRP client session */
                if (localSetup.equals("active")) {
                    /* Active mode: client should connect MSRP session without TLS */
                    MsrpSession session = getMsrpMgr().createMsrpClientSession(remoteHost,
                            remotePort, remotePath, this, null);
                    session.setFailureReportOption(false);
                    session.setSuccessReportOption(false);
                    getMsrpMgr().openMsrpSession();
                    getMsrpMgr().sendEmptyChunk();
                }

                /* The session is established */
                dialogPath.setSessionEstablished();

                // Start session timer
                SessionTimerManager sessionTimerManager = getSessionTimerManager();
                if (sessionTimerManager.isSessionTimerActivated(resp)) {
                    sessionTimerManager.start(SessionTimerManager.UAS_ROLE,
                            dialogPath.getSessionExpireTime());
                }

                for (ImsSessionListener listener : getListeners()) {
                    listener.onSessionStarted(contact);
                }
            } else {
                if (logActivated) {
                    sLogger.debug("No ACK received for INVITE");
                }

                /* No response received: timeout */
                handleError(new SipSessionError(SipSessionError.SESSION_INITIATION_FAILED));
            }

        } catch (PayloadException | RuntimeException e) {
            sLogger.error("Session initiation has failed for CallId=" + getDialogPath().getCallId()
                    + " ContactId=" + getRemoteContact(), e);
            handleError(new SipSessionError(SipSessionError.MEDIA_FAILED, e));

        } catch (NetworkException e) {
            handleError(new SipSessionError(SipSessionError.MEDIA_FAILED, e));

        }
    }

    @Override
    public boolean isInitiatedByRemote() {
        return true;
    }

}
