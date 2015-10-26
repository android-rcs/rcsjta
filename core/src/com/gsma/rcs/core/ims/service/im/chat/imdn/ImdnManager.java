/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications AB.
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
 * NOTE: This file has been modified by Sony Mobile Communications AB.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.core.ims.service.im.chat.imdn;

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.sip.FeatureTags;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.gsma.rcs.core.ims.service.SessionAuthenticationAgent;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.FifoBuffer;
import com.gsma.rcs.utils.PhoneUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import java.text.ParseException;

import javax2.sip.InvalidArgumentException;
import javax2.sip.message.Response;

/**
 * IMDN manager (see RFC5438)
 * 
 * @author jexa7410
 */
public class ImdnManager extends Thread {

    private final InstantMessagingService mImService;

    private FifoBuffer mBuffer = new FifoBuffer();

    private final RcsSettings mRcsSettings;

    private final static Logger sLogger = Logger.getLogger(ImdnManager.class.getSimpleName());

    /**
     * Constructor
     * 
     * @param imsService IMS service
     * @param rcsSettings
     */
    public ImdnManager(InstantMessagingService imService, RcsSettings rcsSettings) {
        mImService = imService;
        mRcsSettings = rcsSettings;
    }

    /**
     * Terminate manager
     */
    public void terminate() {
        if (sLogger.isActivated()) {
            sLogger.info("Terminate the IMDN manager");
        }
        mBuffer.close();
    }

    /**
     * Should we request and send delivery delivered reports
     */
    public boolean isDeliveryDeliveredReportsEnabled() {
        return mRcsSettings.isImReportsActivated();
    }

    /**
     * Should we send one to one delivery displayed reports
     */
    public boolean isSendOneToOneDeliveryDisplayedReportsEnabled() {
        return mRcsSettings.isImReportsActivated() && !mRcsSettings.isAlbatrosRelease()
                && mRcsSettings.isRespondToDisplayReports();
    }

    /**
     * Should we request one to one delivery displayed reports
     */
    public boolean isRequestOneToOneDeliveryDisplayedReportsEnabled() {
        return mRcsSettings.isImReportsActivated() && !mRcsSettings.isAlbatrosRelease();
    }

    /**
     * Should we send group delivery displayed reports
     */
    public boolean isSendGroupDeliveryDisplayedReportsEnabled() {
        return mRcsSettings.isImReportsActivated() && !mRcsSettings.isAlbatrosRelease()
                && mRcsSettings.isRespondToDisplayReports()
                && mRcsSettings.isRequestAndRespondToGroupDisplayReportsEnabled();
    }

    /**
     * Should we send group delivery displayed reports
     */
    public boolean isRequestGroupDeliveryDisplayedReportsEnabled() {
        return mRcsSettings.isImReportsActivated() && !mRcsSettings.isAlbatrosRelease()
                && mRcsSettings.isRequestAndRespondToGroupDisplayReportsEnabled();
    }

    /**
     * Background processing
     */
    public void run() {
        DeliveryStatus delivery = null;
        while ((delivery = (DeliveryStatus) mBuffer.getObject()) != null) {
            try {
                sendSipMessageDeliveryStatus(delivery, null); // TODO: add sip.instance
                /*
                 * Update rich messaging history when sending DISPLAYED report Since the requested
                 * display report was now successfully send we mark this message as fully received
                 */
                if (ImdnDocument.DELIVERY_STATUS_DISPLAYED.equals(delivery.getStatus())) {
                    mImService.onChatMessageDisplayReportSent(delivery.getChatId(),
                            delivery.getRemote(), delivery.getMsgId());
                }
            } catch (PayloadException e) {
                sLogger.error(new StringBuilder("Failed to send delivery status for chatId : ")
                        .append(delivery.getChatId()).toString(), e);
            } catch (NetworkException e) {
                if (sLogger.isActivated()) {
                    sLogger.debug(e.getMessage());
                }
            } catch (RuntimeException e) {
                /*
                 * Intentionally catch runtime exceptions as else it will abruptly end the thread
                 * and eventually bring the whole system down, which is not intended.
                 */
                sLogger.error(new StringBuilder("Failed to send delivery status for chatId : ")
                        .append(delivery.getChatId()).toString(), e);
            }
        }
    }

    /**
     * Send a message delivery status
     * 
     * @param chatId ChatId
     * @param remote Remote contact
     * @param msgId Message ID
     * @param status Delivery status
     * @param timestamp Timestamp sent in payload for IMDN datetime
     */
    public void sendMessageDeliveryStatus(String chatId, ContactId remote, String msgId,
            String status, long timestamp) {
        // Add request in the buffer for background processing
        DeliveryStatus delivery = new DeliveryStatus(chatId, remote, msgId, status, timestamp);
        mBuffer.addObject(delivery);
    }

    /**
     * Send a message delivery status immediately
     * 
     * @param chatId ChatId when targeting a group chat message, otherwise null
     * @param remote Remote contact
     * @param msgId Message ID
     * @param status Delivery status
     * @param remoteInstanceId
     * @param timestamp Timestamp sent in payload for IMDN datetime
     * @throws PayloadException
     * @throws NetworkException
     */
    public void sendMessageDeliveryStatusImmediately(String chatId, ContactId remote, String msgId,
            String status, final String remoteInstanceId, long timestamp) throws PayloadException,
            NetworkException {
        // Execute request in background
        final DeliveryStatus delivery = new DeliveryStatus(chatId, remote, msgId, status, timestamp);
        sendSipMessageDeliveryStatus(delivery, remoteInstanceId);
    }

    private void analyzeSipResponse(SipTransactionContext ctx,
            SessionAuthenticationAgent authenticationAgent, SipDialogPath dialogPath, String cpim)
            throws NetworkException, PayloadException, InvalidArgumentException, ParseException {
        int statusCode = ctx.getStatusCode();
        switch (statusCode) {
            case Response.PROXY_AUTHENTICATION_REQUIRED:

                if (sLogger.isActivated()) {
                    sLogger.info("407 response received");
                }

                /* Set the Proxy-Authorization header */
                authenticationAgent.readProxyAuthenticateHeader(ctx.getSipResponse());

                /* Increment the Cseq number of the dialog path */
                dialogPath.incrementCseq();

                /* Create a second MESSAGE request with the right token */
                if (sLogger.isActivated()) {
                    sLogger.info("Send second MESSAGE.");
                }
                SipRequest msg = SipMessageFactory.createMessage(dialogPath,
                        FeatureTags.FEATURE_OMA_IM, CpimMessage.MIME_TYPE, cpim.getBytes(UTF8));

                /* Set the Authorization header */
                authenticationAgent.setProxyAuthorizationHeader(msg);

                ctx = mImService.getImsModule().getSipManager().sendSipMessageAndWait(msg);

                analyzeSipResponse(ctx, authenticationAgent, dialogPath, cpim);
                break;
            case Response.OK:
            case Response.ACCEPTED:
                if (sLogger.isActivated()) {
                    sLogger.info("20x OK response received");
                }
                break;
            default:
                throw new NetworkException(new StringBuilder("Delivery report has failed: ")
                        .append(statusCode).append(" response received").toString());
        }
    }

    /**
     * Send message delivery status via SIP MESSAGE
     * 
     * @param deliveryStatus Delivery status
     * @param remoteInstanceId Remote SIP instance
     * @throws PayloadException
     * @throws NetworkException
     */
    private void sendSipMessageDeliveryStatus(DeliveryStatus deliveryStatus, String remoteInstanceId)
            throws PayloadException, NetworkException {
        try {
            if (sLogger.isActivated()) {
                sLogger.debug("Send delivery status " + deliveryStatus.getStatus()
                        + " for message " + deliveryStatus.getMsgId());
            }

            // Create CPIM/IDMN document
            String from = ChatUtils.ANONYMOUS_URI;
            String to = ChatUtils.ANONYMOUS_URI;
            /* Timestamp for IMDN datetime */
            String imdn = ChatUtils.buildImdnDeliveryReport(deliveryStatus.getMsgId(),
                    deliveryStatus.getStatus(), deliveryStatus.getTimestamp());
            /* Timestamp for CPIM DateTime */
            String cpim = ChatUtils.buildCpimDeliveryReport(from, to, imdn,
                    System.currentTimeMillis());

            // Create authentication agent
            SessionAuthenticationAgent authenticationAgent = new SessionAuthenticationAgent(
                    mImService.getImsModule());
            // @FIXME: This should be an URI instead of String
            String toUri = PhoneUtils.formatContactIdToUri(deliveryStatus.getRemote()).toString();
            // Create a dialog path
            SipDialogPath dialogPath = new SipDialogPath(mImService.getImsModule().getSipManager()
                    .getSipStack(), mImService.getImsModule().getSipManager().getSipStack()
                    .generateCallId(), 1, toUri, ImsModule.getImsUserProfile().getPublicUri(),
                    toUri, mImService.getImsModule().getSipManager().getSipStack()
                            .getServiceRoutePath(), mRcsSettings);
            dialogPath.setRemoteSipInstance(remoteInstanceId);

            // Create MESSAGE request
            if (sLogger.isActivated()) {
                sLogger.info("Send first MESSAGE.");
            }
            SipRequest msg = SipMessageFactory.createMessage(dialogPath,
                    FeatureTags.FEATURE_OMA_IM, CpimMessage.MIME_TYPE, cpim.getBytes(UTF8));

            // Send MESSAGE request
            SipTransactionContext ctx = mImService.getImsModule().getSipManager()
                    .sendSipMessageAndWait(msg);

            // Analyze received message
            analyzeSipResponse(ctx, authenticationAgent, dialogPath, cpim);

        } catch (InvalidArgumentException e) {
            throw new PayloadException(new StringBuilder(
                    "Unable to set authorization header for remoteInstanceId : ").append(
                    remoteInstanceId).toString(), e);

        } catch (ParseException e) {
            throw new PayloadException(new StringBuilder(
                    "Unable to set authorization header for remoteInstanceId : ").append(
                    remoteInstanceId).toString(), e);
        }
    }

    /**
     * Delivery status
     */
    private static class DeliveryStatus {
        private String mChatId;
        private ContactId mRemote;
        private String mMsgId;
        private String mStatus;
        private long mTimestamp;

        public DeliveryStatus(String chatId, ContactId remote, String msgId, String status,
                long timestamp) {
            mChatId = chatId;
            mRemote = remote;
            mMsgId = msgId;
            mStatus = status;
            mTimestamp = timestamp;
        }

        public String getChatId() {
            return mChatId;
        }

        public ContactId getRemote() {
            return mRemote;
        }

        public String getMsgId() {
            return mMsgId;
        }

        public String getStatus() {
            return mStatus;
        }

        public long getTimestamp() {
            return mTimestamp;
        }
    }

}
