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

package com.gsma.rcs.core.ims.service.im.chat.event;

import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.ParseFailureException;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.SessionAuthenticationAgent;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.ChatError;
import com.gsma.rcs.core.ims.service.im.chat.GroupChatSession;
import com.gsma.rcs.core.ims.service.im.chat.GroupChatSessionListener;
import com.gsma.rcs.platform.registry.RegistryFactory;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.PeriodicRefresher;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.chat.ChatLog.Message.GroupChatEvent;
import com.gsma.services.rcs.chat.ChatLog.Message.GroupChatEvent.Status;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.contact.ContactId;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import javax2.sip.InvalidArgumentException;
import javax2.sip.header.ExpiresHeader;
import javax2.sip.header.SubscriptionStateHeader;
import javax2.sip.message.Response;

/**
 * Conference event subscribe manager
 * 
 * @author jexa7410
 */
public class ConferenceEventSubscribeManager extends PeriodicRefresher {
    /**
     * Rate to convert from seconds to milliseconds
     */
    private static final long SECONDS_TO_MILLISECONDS_CONVERSION_RATE = 1000;

    /**
     * Last min expire period (in milliseconds)
     */
    private static final String REGISTRY_MIN_EXPIRE_PERIOD = "MinSubscribeConferenceEventExpirePeriod";

    /**
     * IMS module
     */
    private ImsModule mImsModule;

    /**
     * Group chat session
     */
    private GroupChatSession mSession;

    /**
     * Dialog path
     */
    private SipDialogPath mDialogPath;

    /**
     * Expire period in milliseconds
     */
    private long mExpirePeriod;

    /**
     * Subscription flag
     */
    private boolean mSubscribed = false;

    /**
     * Authentication agent
     */
    private SessionAuthenticationAgent mAuthenticationAgent;

    private final RcsSettings mRcsSettings;

    private final MessagingLog mMessagingLog;

    private final static Logger sLogger = Logger.getLogger(ConferenceEventSubscribeManager.class
            .getSimpleName());

    /**
     * Constructor
     * 
     * @param session Group chat session
     * @param rcsSettings
     * @param messagingLog
     */
    public ConferenceEventSubscribeManager(GroupChatSession session, RcsSettings rcsSettings,
            MessagingLog messagingLog) {
        mSession = session;
        mImsModule = session.getImsService().getImsModule();
        mAuthenticationAgent = new SessionAuthenticationAgent(mImsModule);
        mRcsSettings = rcsSettings;
        mMessagingLog = messagingLog;

        long defaultExpirePeriod = mRcsSettings.getSubscribeExpirePeriod();
        long minExpireValue = RegistryFactory.getFactory().readLong(REGISTRY_MIN_EXPIRE_PERIOD, -1);
        if ((minExpireValue != -1) && (defaultExpirePeriod < minExpireValue)) {
            mExpirePeriod = minExpireValue;
        } else {
            mExpirePeriod = defaultExpirePeriod;
        }
    }

    /**
     * Is subscribed
     * 
     * @return Boolean
     */
    public boolean isSubscribed() {
        return mSubscribed;
    }

    /**
     * Returns the identity.
     * 
     * @return Identity
     */
    public String getIdentity() {
        return mSession.getImSessionIdentity();
    }

    /**
     * Returns the dialog path of the conference subscriber
     * 
     * @return SipDialogPath
     */
    public SipDialogPath getDialogPath() {
        return mDialogPath;
    }

    /**
     * Receive a notification
     * 
     * @param notify Received notify
     * @param timestamp Local timestamp when got SipRequest
     * @throws PayloadException
     */
    public void receiveNotification(SipRequest notify, long timestamp) throws PayloadException {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.debug("New conference event notification received");
        }

        // Parse XML part
        byte[] content = notify.getContentBytes();
        if (content != null) {
            try {
                InputSource pidfInput = new InputSource(new ByteArrayInputStream(content));
                ConferenceInfoParser confParser = new ConferenceInfoParser(pidfInput).parse();
                ConferenceInfoDocument conference = confParser.getConferenceInfo();
                if (conference != null) {
                    int maxParticipants = conference.getMaxUserCount();
                    if (maxParticipants > 0) {
                        if (logActivated) {
                            sLogger.debug("Set max number of participants to " + maxParticipants);
                        }
                        mSession.setMaxParticipants(maxParticipants);
                    }

                    Map<ContactId, ParticipantStatus> participants = new HashMap<ContactId, ParticipantStatus>();
                    Vector<User> users = conference.getUsers();
                    for (User user : users) {
                        String phonenumber = user.getEntity();
                        ContactId contact;
                        PhoneNumber validPhoneNumber = ContactUtil
                                .getValidPhoneNumberFromUri(phonenumber);
                        if (validPhoneNumber != null) {
                            contact = ContactUtil
                                    .createContactIdFromValidatedData(validPhoneNumber);
                        } else {
                            // Invalid entity
                            continue;
                        }

                        if (logActivated) {
                            sLogger.debug("Conference info notification for " + contact);
                        }

                        if (user.isMe()
                                || contact.equals(ImsModule.getImsUserProfile().getUsername())) {
                            // By-pass me
                            continue;
                        }

                        if (logActivated) {
                            sLogger.debug("User conference info: " + user);
                        }

                        /*
                         * Collect contact updates to be able to apply them in a one-shot operation
                         * outside the loop.
                         */
                        participants.put(contact, getStatus(user));

                    }

                    if (!participants.isEmpty()) {
                        updateParticipantStatus(participants, timestamp);
                    }
                }
            } catch (ParserConfigurationException e) {
                throw new PayloadException("Can't parse XML notification", e);

            } catch (SAXException e) {
                throw new PayloadException("Can't parse XML notification", e);

            } catch (ParseFailureException e) {
                throw new PayloadException("Can't parse XML notification", e);
            }
        }

        // Check subscription state
        SubscriptionStateHeader stateHeader = (SubscriptionStateHeader) notify
                .getHeader(SubscriptionStateHeader.NAME);
        if ((stateHeader != null) && stateHeader.getState().equalsIgnoreCase("terminated")) {
            if (logActivated) {
                sLogger.info("Conference event subscription has been terminated by server");
            }
            terminatedByServer();
        }
    }

    private void updateParticipantStatus(Map<ContactId, ParticipantStatus> participants,
            long timestamp) {
        Map<ContactId, ParticipantStatus> participantsToUpdate = mSession
                .getParticipantsToUpdate(participants);

        if (participantsToUpdate.isEmpty()) {
            return;
        }
        Map<ContactId, GroupChatEvent.Status> groupChatEventsInDB = mMessagingLog
                .getGroupChatEvents(mSession.getContributionID());

        for (Map.Entry<ContactId, ParticipantStatus> participant : participantsToUpdate.entrySet()) {
            ContactId contact = participant.getKey();
            ParticipantStatus status = participant.getValue();
            if (isGroupChatEventRequired(contact, status, groupChatEventsInDB))
                for (ImsSessionListener listener : mSession.getListeners()) {
                    ((GroupChatSessionListener) listener).onConferenceEventReceived(contact,
                            status, timestamp);
                }
        }
        mSession.updateParticipants(participantsToUpdate);
    }

    /*
     * Check if a new group chat event is required. It is required if there was none before for this
     * contact or if switch from JOINED to DEPARTED (or reverse) is detected.
     */
    private boolean isGroupChatEventRequired(ContactId contact, ParticipantStatus status,
            Map<ContactId, Status> groupChatEvents) {
        if (groupChatEvents == null) {
            /* there is no group chat event in provider for the session */
            return true;
        }
        if (!groupChatEvents.containsKey(contact)) {
            /* there is no group chat event in provider for the contact */
            return true;
        }
        GroupChatEvent.Status statusInDB = groupChatEvents.get(contact);

        if (ParticipantStatus.CONNECTED == status) {
            if (GroupChatEvent.Status.JOINED != statusInDB) {
                /* Contact is not already marked as joined in provider */
                return true;
            }
        } else if (ParticipantStatus.DEPARTED == status) {
            if (GroupChatEvent.Status.DEPARTED != statusInDB) {
                /* Contact is already marked as departed in provider */
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the received notification if for this subscriber
     * 
     * @param notify
     * @return Boolean
     */
    public boolean isNotifyForThisSubscriber(SipRequest notify) {
        boolean result = false;
        if ((mDialogPath != null) && notify.getCallId().equals(mDialogPath.getCallId())) {
            result = true;
        }
        return result;
    }

    /**
     * Subscription has been terminated by server
     */
    public synchronized void terminatedByServer() {
        if (!mSubscribed) {
            // Already unsubscribed
            return;
        }

        if (sLogger.isActivated()) {
            sLogger.info("Subscription has been terminated by server");
        }

        // Stop periodic subscription
        stopTimer();

        // Reset dialog path attributes
        resetDialogPath();

        // Force subscription flag to false
        mSubscribed = false;
    }

    /**
     * Terminate manager
     * 
     * @throws NetworkException
     * @throws PayloadException
     */
    public void terminate() throws PayloadException, NetworkException {
        if (sLogger.isActivated()) {
            sLogger.info("Terminate the subscribe manager");
        }

        // Stop periodic subscription
        stopTimer();

        // Unsubscribe before to quit
        if ((mImsModule.getCurrentNetworkInterface() != null)
                && mImsModule.getCurrentNetworkInterface().isRegistered() && mSubscribed) {
            unSubscribe();
        }

        if (sLogger.isActivated()) {
            sLogger.info("Subscribe manager is terminated");
        }
    }

    /**
     * Create a SUBSCRIBE request
     * 
     * @param dialog SIP dialog path
     * @param expirePeriod Expiration period in milliseconds
     * @return SIP request
     * @throws PayloadException
     */
    private SipRequest createSubscribe(SipDialogPath dialog, long expirePeriod)
            throws PayloadException {
        try {
            // Create SUBSCRIBE message
            SipRequest subscribe = SipMessageFactory.createSubscribe(dialog, expirePeriod);

            // Set feature tags
            SipUtils.setFeatureTags(subscribe.getStackMessage(),
                    InstantMessagingService.CHAT_FEATURE_TAGS);

            // Set the Event header
            subscribe.addHeader("Event", "conference");

            // Set the Accept header
            subscribe.addHeader("Accept", "application/conference-info+xml");

            return subscribe;

        } catch (ParseException e) {
            throw new PayloadException(
                    "Unable to form subscribe message with featureTags : ".concat(Arrays.asList(
                            InstantMessagingService.CHAT_FEATURE_TAGS).toString()), e);
        }
    }

    /**
     * Subscription refresh processing
     * 
     * @throws PayloadException
     * @throws NetworkException
     */
    public void periodicProcessing() throws PayloadException, NetworkException {
        // Make a subscribe
        if (sLogger.isActivated()) {
            sLogger.info("Execute re-subscribe");
        }

        // Send SUBSCRIBE request
        subscribe();
    }

    /**
     * Subscribe
     * 
     * @throws PayloadException
     * @throws NetworkException
     */
    public synchronized void subscribe() throws PayloadException, NetworkException {
        if (sLogger.isActivated()) {
            sLogger.info("Subscribe to " + getIdentity());
        }
        if (mDialogPath == null) {
            String callId = mImsModule.getSipManager().getSipStack().generateCallId();

            String target = getIdentity();

            String localParty = ImsModule.getImsUserProfile().getPublicUri();

            String remoteParty = getIdentity();

            Vector<String> route = mImsModule.getSipManager().getSipStack().getServiceRoutePath();

            mDialogPath = new SipDialogPath(mImsModule.getSipManager().getSipStack(), callId, 1,
                    target, localParty, remoteParty, route, mRcsSettings);
        } else {
            mDialogPath.incrementCseq();
        }

        SipRequest subscribe = createSubscribe(mDialogPath, mExpirePeriod);

        sendSubscribe(subscribe);
    }

    /**
     * Unsubscribe
     * 
     * @throws PayloadException
     * @throws NetworkException
     */
    public synchronized void unSubscribe() throws PayloadException, NetworkException {
        if (!mSubscribed) {
            return;
        }
        if (sLogger.isActivated()) {
            sLogger.info("Unsubscribe to ".concat(getIdentity()));
        }
        stopTimer();
        mDialogPath.incrementCseq();
        /* Create a SUBSCRIBE with expire 0 */
        SipRequest subscribe = createSubscribe(mDialogPath, 0);
        sendSubscribe(subscribe);
        mSubscribed = false;
        resetDialogPath();
    }

    /**
     * Reset the dialog path
     */
    private void resetDialogPath() {
        if (mDialogPath != null) {
            Core.getInstance().getImService()
                    .removeGroupChatConferenceSubscriber(mDialogPath.getCallId());
            mDialogPath = null;
        }
    }

    /**
     * Retrieve the expire period
     * 
     * @param resp SIP response
     */
    private void retrieveExpirePeriod(SipResponse response) {
        // Extract expire value from Expires header
        ExpiresHeader expiresHeader = (ExpiresHeader) response.getHeader(ExpiresHeader.NAME);
        if (expiresHeader != null) {
            int expires = expiresHeader.getExpires();
            if (expires != -1) {
                mExpirePeriod = expires * SECONDS_TO_MILLISECONDS_CONVERSION_RATE;
            }
        }
    }

    /**
     * Send SUBSCRIBE message
     * 
     * @param subscribe SIP SUBSCRIBE
     * @throws PayloadException
     * @throws NetworkException
     */
    private void sendSubscribe(SipRequest subscribe) throws PayloadException, NetworkException {
        try {
            if (sLogger.isActivated()) {
                sLogger.info(new StringBuilder("Send SUBSCRIBE, expire=")
                        .append(subscribe.getExpires()).append("ms").toString());
            }

            if (mSubscribed) {
                // Set the Authorization header
                mAuthenticationAgent.setProxyAuthorizationHeader(subscribe);
            }

            // Send SUBSCRIBE request
            SipTransactionContext ctx = mImsModule.getSipManager().sendSipMessageAndWait(subscribe);

            // Analyze the received response
            if (ctx.isSipResponse()) {
                final int statusCode = ctx.getStatusCode();
                switch (statusCode) {
                    case Response.OK:
                        if (subscribe.getExpires() != 0) {
                            handle200OK(ctx);
                        } else {
                            handle200OkUnsubscribe(ctx);
                        }
                        break;
                    case Response.ACCEPTED:
                        handle200OK(ctx);
                        break;
                    case Response.PROXY_AUTHENTICATION_REQUIRED:
                        handle407Authentication(ctx);
                        break;
                    case Response.INTERVAL_TOO_BRIEF:
                        handle423IntervalTooBrief(ctx);
                        break;
                    default:
                        handleError(new ChatError(ChatError.SUBSCRIBE_CONFERENCE_FAILED,
                                new StringBuilder(String.valueOf(statusCode)).append(' ')
                                        .append(ctx.getReasonPhrase()).toString()));
                        break;
                }
            } else {
                if (sLogger.isActivated()) {
                    sLogger.debug("No response received for SUBSCRIBE");
                }

                // No response received: timeout
                handleError(new ChatError(ChatError.SUBSCRIBE_CONFERENCE_FAILED));
            }
        } catch (InvalidArgumentException e) {
            throw new PayloadException("Unable to set authorization header for subscribe!", e);

        } catch (ParseException e) {
            throw new PayloadException("Unable to set authorization header for subscribe!", e);
        }
    }

    /**
     * Handle 200 0K response
     * 
     * @param ctx SIP transaction context
     */
    private void handle200OK(SipTransactionContext ctx) {
        // 200 OK response received
        if (sLogger.isActivated()) {
            sLogger.info("200 OK response received");
        }
        mSubscribed = true;

        SipResponse resp = ctx.getSipResponse();

        // Set the route path with the Record-Route header
        Vector<String> newRoute = SipUtils.routeProcessing(resp, true);
        mDialogPath.setRoute(newRoute);

        // Set the remote tag
        mDialogPath.setRemoteTag(resp.getToTag());

        // Set the target
        mDialogPath.setTarget(resp.getContactURI());

        // Set the Proxy-Authorization header
        mAuthenticationAgent.readProxyAuthenticateHeader(resp);

        // Retrieve the expire value in the response
        retrieveExpirePeriod(resp);

        // Start the periodic subscribe
        startTimer(System.currentTimeMillis(), mExpirePeriod, 0.5);

        Core.getInstance().getImService()
                .addGroupChatConferenceSubscriber(mDialogPath.getCallId(), mSession);
    }

    /**
     * Handle 200 0K response of UNSUBSCRIBE
     * 
     * @param ctx SIP transaction context
     */
    private void handle200OkUnsubscribe(SipTransactionContext ctx) {
        // 200 OK response received
        if (sLogger.isActivated()) {
            sLogger.info("200 OK response received");
        }
    }

    /**
     * Handle 407 response
     * 
     * @param ctx SIP transaction context
     * @throws InvalidArgumentException
     * @throws PayloadException
     * @throws NetworkException
     */
    private void handle407Authentication(SipTransactionContext ctx) throws PayloadException,
            NetworkException {
        try {
            if (sLogger.isActivated()) {
                sLogger.info("407 response received");
            }

            SipResponse resp = ctx.getSipResponse();
            mAuthenticationAgent.readProxyAuthenticateHeader(resp);
            mDialogPath.incrementCseq();

            if (sLogger.isActivated()) {
                sLogger.info("Send second SUBSCRIBE");
            }
            SipRequest subscribe = createSubscribe(mDialogPath, ctx.getTransaction().getRequest()
                    .getExpires().getExpires()
                    * SECONDS_TO_MILLISECONDS_CONVERSION_RATE);

            mAuthenticationAgent.setProxyAuthorizationHeader(subscribe);
            sendSubscribe(subscribe);
        } catch (InvalidArgumentException e) {
            throw new PayloadException("Failed to handle 407 authentication response!", e);

        } catch (ParseException e) {
            throw new PayloadException("Failed to handle 407 authentication response!", e);
        }
    }

    /**
     * Handle 423 response
     * 
     * @param ctx SIP transaction context
     * @throws PayloadException
     * @throws NetworkException
     */
    private void handle423IntervalTooBrief(SipTransactionContext ctx) throws PayloadException,
            NetworkException {
        try {
            if (sLogger.isActivated()) {
                sLogger.info("423 interval too brief response received");
            }

            SipResponse resp = ctx.getSipResponse();
            mDialogPath.incrementCseq();

            long minExpire = SipUtils.getMinExpiresPeriod(resp);
            if (minExpire == -1) {
                if (sLogger.isActivated()) {
                    sLogger.error("Can't read the Min-Expires value");
                }
                handleError(new ChatError(ChatError.SUBSCRIBE_CONFERENCE_FAILED,
                        "No Min-Expires value found"));
                return;
            }

            RegistryFactory.getFactory().writeLong(REGISTRY_MIN_EXPIRE_PERIOD, minExpire);

            mExpirePeriod = minExpire;

            SipRequest subscribe = createSubscribe(mDialogPath, mExpirePeriod);

            mAuthenticationAgent.setProxyAuthorizationHeader(subscribe);

            sendSubscribe(subscribe);
        } catch (InvalidArgumentException e) {
            throw new PayloadException("Failed to handle interval too brief response!", e);

        } catch (ParseException e) {
            throw new PayloadException("Failed to handle interval too brief response!", e);
        }
    }

    /**
     * Handle error response
     * 
     * @param error Error
     */
    private void handleError(ChatError error) {
        // Error
        if (sLogger.isActivated()) {
            sLogger.info("Subscribe has failed: " + error.getErrorCode() + ", reason="
                    + error.getMessage());
        }
        mSubscribed = false;

        // Subscribe has failed, stop the periodic subscribe
        stopTimer();

        // Reset dialog path attributes
        resetDialogPath();
    }

    /**
     * Convert the user status into integer
     * 
     * @param user the user
     * @return the integer status
     */
    private static ParticipantStatus getStatus(User user) {
        String state = user.getState();
        /*
         * Manage "pending-out" and "pending-in" status like "pending" status. See RFC 4575
         * dialing-in: Endpoint is dialing into the conference, not yet in the roster (probably
         * being authenticated). dialing-out: Focus has dialed out to connect the endpoint to the
         * conference, but the endpoint is not yet in the roster (probably being authenticated).
         */
        if ("dialing-out".equalsIgnoreCase(state)) {
            return ParticipantStatus.INVITED;

        } else if ("dialing-in".equalsIgnoreCase(state)) {
            return ParticipantStatus.INVITED;

        } else if (User.STATE_DISCONNECTED.equals(state)) {
            /*
             * For the disconnected state, override state with the more detailed
             * disconnection-method field (if available).
             */
            String disconnectionMethod = user.getDisconnectionMethod();
            if (disconnectionMethod != null) {
                /* Detect declined by remote from failure-reason field. */
                if (User.STATE_FAILED.equals(disconnectionMethod)) {
                    String reason = user.getFailureReason();
                    if ((reason != null) && reason.contains("603")) {
                        return ParticipantStatus.DECLINED;
                    }
                }
                return getStatus(disconnectionMethod);
            }
        }
        return getStatus(state);
    }

    /**
     * Convert the status into integer
     * 
     * @param status the string status
     * @return the integer status
     */
    private static ParticipantStatus getStatus(final String status) {
        if (User.STATE_CONNECTED.equals(status)) {
            return ParticipantStatus.CONNECTED;

        } else if (User.STATE_DISCONNECTED.equals(status)) {
            return ParticipantStatus.DISCONNECTED;

        } else if (User.STATE_DEPARTED.equals(status)) {
            return ParticipantStatus.DEPARTED;

        } else if (User.STATE_BOOTED.equals(status)) {
            return ParticipantStatus.DISCONNECTED;

        } else if (User.STATE_FAILED.equals(status)) {
            return ParticipantStatus.FAILED;

        } else if (User.STATE_BUSY.equals(status)) {
            return ParticipantStatus.DISCONNECTED;

        } else if (User.STATE_DECLINED.equals(status)) {
            return ParticipantStatus.DECLINED;

        } else if (User.STATE_PENDING.equals(status)) {
            return ParticipantStatus.INVITED;
        }
        throw new IllegalArgumentException(new StringBuilder("Unknown status ").append(status)
                .append(" passed to getStatus!").toString());
    }
}
