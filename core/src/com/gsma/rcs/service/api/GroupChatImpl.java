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

package com.gsma.rcs.service.api;

import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.CoreException;
import com.gsma.rcs.core.CoreListener;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.service.ImsServiceSession.TerminationReason;
import com.gsma.rcs.core.ims.service.capability.Capabilities;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.ChatError;
import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.rcs.core.ims.service.im.chat.ChatSession;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.im.chat.GroupChatInfo;
import com.gsma.rcs.core.ims.service.im.chat.GroupChatSession;
import com.gsma.rcs.core.ims.service.im.chat.GroupChatSessionListener;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.ChatMessagePersistedStorageAccessor;
import com.gsma.rcs.provider.messaging.GroupChatPersistedStorageAccessor;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData.ImSessionStartMode;
import com.gsma.rcs.service.broadcaster.IGroupChatEventBroadcaster;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.ChatLog.Message.Content;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.Status;
import com.gsma.services.rcs.chat.ChatLog.Message.GroupChatEvent;
import com.gsma.services.rcs.chat.ChatLog.Message.MimeType;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.chat.GroupChat.ReasonCode;
import com.gsma.services.rcs.chat.GroupChat.State;
import com.gsma.services.rcs.chat.IChatMessage;
import com.gsma.services.rcs.chat.IGroupChat;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo;

import android.os.RemoteException;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Group chat implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class GroupChatImpl extends IGroupChat.Stub implements GroupChatSessionListener {
    private final String mChatId;

    private final IGroupChatEventBroadcaster mBroadcaster;

    private final InstantMessagingService mImService;

    private final GroupChatPersistedStorageAccessor mPersistentStorage;

    private final ChatServiceImpl mChatService;

    private final RcsSettings mRcsSettings;

    private final ContactManager mContactManager;

    private final MessagingLog mMessagingLog;

    private final Core mCore;

    private boolean mGroupChatRejoinedAsPartOfSendOperation = false;

    /**
     * Lock used for synchronization
     */
    private final Object lock = new Object();

    /**
     * The logger
     */
    private final Logger logger = Logger.getLogger(getClass().getName());

    /**
     * Constructor
     * 
     * @param chatId Chat Id
     * @param broadcaster IGroupChatEventBroadcaster
     * @param imService InstantMessagingService
     * @param persistentStorage GroupChatPersistedStorageAccessor
     * @param rcsSettings RcsSettings
     * @param contactManager ContactManager
     * @param chatService ChatServiceImpl
     * @param messagingLog MessagingLog
     * @param core Core
     */
    public GroupChatImpl(String chatId, IGroupChatEventBroadcaster broadcaster,
            InstantMessagingService imService, GroupChatPersistedStorageAccessor persistentStorage,
            RcsSettings rcsSettings, ContactManager contactManager, ChatServiceImpl chatService,
            MessagingLog messagingLog, Core core) {
        mChatId = chatId;
        mBroadcaster = broadcaster;
        mImService = imService;
        mPersistentStorage = persistentStorage;
        mChatService = chatService;
        mRcsSettings = rcsSettings;
        mContactManager = contactManager;
        mMessagingLog = messagingLog;
        mCore = core;
    }

    private Content.ReasonCode imdnToMessageFailedReasonCode(ImdnDocument imdn) {
        String notificationType = imdn.getNotificationType();
        if (ImdnDocument.DELIVERY_NOTIFICATION.equals(notificationType)) {
            return Content.ReasonCode.FAILED_DELIVERY;

        } else if (ImdnDocument.DISPLAY_NOTIFICATION.equals(notificationType)) {
            return Content.ReasonCode.FAILED_DISPLAY;
        }
        throw new IllegalArgumentException(new StringBuilder(
                "Received invalid imdn notification type:'").append(notificationType).append("'")
                .toString());
    }

    private void setStateAndReasonCode(State state, ReasonCode reasonCode) {
        if (mPersistentStorage.setStateAndReasonCode(state, reasonCode)) {
            mBroadcaster.broadcastStateChanged(mChatId, state, reasonCode);
        }
    }

    private void handleSessionRejected(ReasonCode reasonCode) {
        setRejoinedAsPartOfSendOperation(false);
        synchronized (lock) {
            mChatService.removeGroupChat(mChatId);
            setStateAndReasonCode(State.REJECTED, reasonCode);
        }
    }

    private void handleMessageDeliveryStatusDelivered(ContactId contact, String msgId,
            long timestampDelivered) {
        String mimeType = mMessagingLog.getMessageMimeType(msgId);
        synchronized (lock) {
            mPersistentStorage.setGroupChatDeliveryInfoDelivered(mChatId, contact, msgId,
                    timestampDelivered);
            mBroadcaster.broadcastMessageGroupDeliveryInfoChanged(mChatId, contact, mimeType,
                    msgId, GroupDeliveryInfo.Status.DELIVERED,
                    GroupDeliveryInfo.ReasonCode.UNSPECIFIED);
            if (mPersistentStorage.isDeliveredToAllRecipients(msgId)) {
                mPersistentStorage.setMessageStatusDelivered(msgId, timestampDelivered);
                mBroadcaster.broadcastMessageStatusChanged(mChatId, mimeType, msgId,
                        Status.DELIVERED, Content.ReasonCode.UNSPECIFIED);
            }
        }
    }

    private void handleMessageDeliveryStatusDisplayed(ContactId contact, String msgId,
            long timestampDisplayed) {
        String mimeType = mMessagingLog.getMessageMimeType(msgId);
        synchronized (lock) {
            mPersistentStorage
                    .setDeliveryInfoDisplayed(mChatId, contact, msgId, timestampDisplayed);
            mBroadcaster.broadcastMessageGroupDeliveryInfoChanged(mChatId, contact, mimeType,
                    msgId, GroupDeliveryInfo.Status.DISPLAYED,
                    GroupDeliveryInfo.ReasonCode.UNSPECIFIED);
            if (mPersistentStorage.isDisplayedByAllRecipients(msgId)) {
                mPersistentStorage.setMessageStatusDisplayed(msgId, timestampDisplayed);
                mBroadcaster.broadcastMessageStatusChanged(mChatId, mimeType, msgId,
                        Status.DISPLAYED, Content.ReasonCode.UNSPECIFIED);
            }
        }
    }

    private void handleMessageDeliveryStatusFailed(ContactId contact, String msgId,
            Content.ReasonCode reasonCode) {
        String mimeType = mMessagingLog.getMessageMimeType(msgId);
        synchronized (lock) {
            if (Content.ReasonCode.FAILED_DELIVERY == reasonCode) {
                if (!mPersistentStorage.setGroupDeliveryInfoStatusAndReasonCode(mChatId, contact,
                        msgId, GroupDeliveryInfo.Status.FAILED,
                        GroupDeliveryInfo.ReasonCode.FAILED_DELIVERY)) {
                    /* Add entry with delivered and displayed timestamps set to 0. */
                    mMessagingLog.addGroupChatDeliveryInfoEntry(mChatId, contact, msgId,
                            GroupDeliveryInfo.Status.FAILED,
                            GroupDeliveryInfo.ReasonCode.FAILED_DELIVERY, 0, 0);
                }
                mBroadcaster.broadcastMessageGroupDeliveryInfoChanged(mChatId, contact, mimeType,
                        msgId, GroupDeliveryInfo.Status.FAILED,
                        GroupDeliveryInfo.ReasonCode.FAILED_DELIVERY);
            } else {
                if (!mPersistentStorage.setGroupDeliveryInfoStatusAndReasonCode(mChatId, contact,
                        msgId, GroupDeliveryInfo.Status.FAILED,
                        GroupDeliveryInfo.ReasonCode.FAILED_DISPLAY)) {
                    /* Add entry with delivered and displayed timestamps set to 0. */
                    mMessagingLog.addGroupChatDeliveryInfoEntry(mChatId, contact, msgId,
                            GroupDeliveryInfo.Status.FAILED,
                            GroupDeliveryInfo.ReasonCode.FAILED_DISPLAY, 0, 0);
                }
                mBroadcaster.broadcastMessageGroupDeliveryInfoChanged(mChatId, contact, mimeType,
                        msgId, GroupDeliveryInfo.Status.FAILED,
                        GroupDeliveryInfo.ReasonCode.FAILED_DISPLAY);
            }
        }
    }

    private boolean isGroupChatAbandoned() {
        GroupChatSession session = mImService.getGroupChatSession(mChatId);
        if (session != null) {
            /* Group chat is not abandoned if there exists a session */
            return false;
        }
        ReasonCode reasonCode = mPersistentStorage.getReasonCode();
        if (reasonCode == null) {
            return false;
        }
        switch (reasonCode) {
            case ABORTED_BY_USER:
            case FAILED_INITIATION:
            case REJECTED_BY_REMOTE:
            case REJECTED_MAX_CHATS:
            case REJECTED_SPAM:
            case REJECTED_BY_TIMEOUT:
            case REJECTED_BY_SYSTEM:
                if (logger.isActivated()) {
                    logger.debug(new StringBuilder("Group chat with chatId '").append(mChatId)
                            .append("' is ").append(reasonCode).toString());
                }
                return true;
            default:
                break;
        }
        return false;
    }

    private boolean isAllowedToInviteAdditionalParticipants(int additionalParticipants)
            throws RemoteException {
        int nrOfParticipants = getParticipants().size() + additionalParticipants;
        int maxNrOfAllowedParticipants = mRcsSettings.getMaxChatParticipants();
        return nrOfParticipants < maxNrOfAllowedParticipants;
    }

    private boolean isGroupChatCapableOfReceivingParticipantInvitations() {
        if (!mRcsSettings.isGroupChatActivated()) {
            if (logger.isActivated()) {
                logger.debug(new StringBuilder(
                        "Cannot add participants to on group chat with group chat Id '")
                        .append(mChatId)
                        .append("' as group chat feature has been disabled by the operator.")
                        .toString());
            }
            return false;
        }
        if (isGroupChatAbandoned()) {
            if (logger.isActivated()) {
                logger.debug(new StringBuilder(
                        "Cannot invite participants to group chat with group chat Id '")
                        .append(mChatId).append("'").toString());
            }
            return false;
        }
        return true;
    }

    private boolean isGroupChatRejoinable() {
        GroupChatInfo groupChat = mMessagingLog.getGroupChatInfo(mChatId);
        if (groupChat == null) {
            if (logger.isActivated()) {
                logger.debug(new StringBuilder("Group chat with group chat Id '").append(mChatId)
                        .append("' is not rejoinable as the group chat does not exist in DB.")
                        .toString());
            }
            return false;
        }
        if (TextUtils.isEmpty(groupChat.getRejoinId())) {
            if (logger.isActivated()) {
                logger.debug(new StringBuilder("Group chat with group chat Id '")
                        .append(mChatId)
                        .append("' is not rejoinable as there is no ongoing session with "
                                + "corresponding chatId and there exists no rejoinId to "
                                + "rejoin the group chat.").toString());
            }
            return false;
        }
        return true;
    }

    private boolean isParticipantEligibleToBeInvited(ContactId participant) {
        Map<ContactId, ParticipantStatus> currentParticipants = mMessagingLog
                .getParticipants(mChatId);
        for (Map.Entry<ContactId, ParticipantStatus> currentParticipant : currentParticipants
                .entrySet()) {
            if (currentParticipant.getKey().equals(participant)) {
                ParticipantStatus status = currentParticipant.getValue();
                switch (status) {
                    case INVITE_QUEUED:
                    case INVITED:
                    case INVITING:
                    case CONNECTED:
                    case DISCONNECTED:
                        if (logger.isActivated()) {
                            logger.debug(new StringBuilder(
                                    "Cannot invite participant to group chat with group chat Id '")
                                    .append(mChatId).append("' as the participant '")
                                    .append(participant).append("' is .").append(status).toString());
                        }
                        return false;
                    default:
                        break;
                }
            }
        }
        return true;
    }

    private boolean isParticipantCapableToBeInvited(ContactId participant) {
        boolean inviteOnlyFullSF = mRcsSettings.isGroupChatInviteIfFullStoreForwardSupported();
        Capabilities remoteCapabilities = mContactManager.getContactCapabilities(participant);
        if (remoteCapabilities == null) {
            if (logger.isActivated()) {
                logger.debug(new StringBuilder(
                        "Cannot invite participant to group chat with group chat Id '")
                        .append(mChatId).append("' as the capabilities of participant '")
                        .append(participant).append("' are not known.").toString());
            }
            return false;
        }
        if (!remoteCapabilities.isImSessionSupported()) {
            if (logger.isActivated()) {
                logger.debug(new StringBuilder(
                        "Cannot invite participant to group chat with group chat Id '")
                        .append(mChatId).append("' as the participant '").append(participant)
                        .append("' does not have IM capabilities.").toString());
            }
            return false;
        }
        if (inviteOnlyFullSF && !remoteCapabilities.isGroupChatStoreForwardSupported()) {
            if (logger.isActivated()) {
                logger.debug(new StringBuilder(
                        "Cannot invite participant to group chat with group chat Id '")
                        .append(mChatId)
                        .append("' as full store and forward is required and the participant '")
                        .append(participant).append("' does not have that feature supported.")
                        .toString());
            }
            return false;
        }
        return true;
    }

    /**
     * Get chat ID
     * 
     * @return Chat ID
     */
    public String getChatId() {
        return mChatId;
    }

    /**
     * Get remote contact identifier
     * 
     * @return ContactId
     * @throws RemoteException
     */
    public ContactId getRemoteContact() throws RemoteException {
        try {
            GroupChatSession session = mImService.getGroupChatSession(mChatId);
            if (session == null) {
                return mPersistentStorage.getRemoteContact();
            }
            return session.getRemoteContact();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                logger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            logger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }

    }

    /**
     * Returns the direction of the group chat (incoming or outgoing)
     * 
     * @return Direction
     * @throws RemoteException
     */
    public int getDirection() throws RemoteException {
        try {
            GroupChatSession session = mImService.getGroupChatSession(mChatId);
            if (session == null) {
                return mPersistentStorage.getDirection().toInt();
            }
            if (session.isInitiatedByRemote()) {
                return Direction.INCOMING.toInt();
            }
            return Direction.OUTGOING.toInt();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                logger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            logger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }

    }

    /**
     * Returns the state of the group chat
     * 
     * @return State
     * @throws RemoteException
     */
    public int getState() throws RemoteException {
        try {
            GroupChatSession session = mImService.getGroupChatSession(mChatId);
            if (session == null) {
                return mPersistentStorage.getState().toInt();
            }
            SipDialogPath dialogPath = session.getDialogPath();
            if (dialogPath != null && dialogPath.isSessionEstablished()) {
                return State.STARTED.toInt();

            } else if (session.isInitiatedByRemote()) {
                if (session.isSessionAccepted()) {
                    return State.ACCEPTING.toInt();
                }
                return State.INVITED.toInt();
            }
            return State.INITIATING.toInt();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                logger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            logger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Returns the reason code of the state of the group chat
     * 
     * @return ReasonCode
     * @throws RemoteException
     */
    public int getReasonCode() throws RemoteException {
        try {
            GroupChatSession session = mImService.getGroupChatSession(mChatId);
            if (session == null) {
                return mPersistentStorage.getReasonCode().toInt();
            }
            return ReasonCode.UNSPECIFIED.toInt();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                logger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            logger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Returns the local timestamp of when the group chat invitation was initiated for outgoing
     * group chats or the local timestamp of when the group chat invitation was received for
     * incoming group chat invitations.
     * 
     * @return Timestamp
     * @throws RemoteException
     */
    public long getTimestamp() throws RemoteException {
        try {
            GroupChatSession session = mImService.getGroupChatSession(mChatId);
            if (session == null) {
                return mPersistentStorage.getTimestamp();
            }
            return session.getTimestamp();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                logger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            logger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Is Store & Forward
     * 
     * @return Boolean
     */
    public boolean isStoreAndForward() {
        GroupChatSession session = mImService.getGroupChatSession(mChatId);
        if (session == null) {
            /*
             * no session means always not "store and forward" as we do not persist this
             * information.
             */
            return false;
        }
        return session.isStoreAndForward();
    }

    /**
     * Get subject associated to the session
     * 
     * @return String
     * @throws RemoteException
     */
    public String getSubject() throws RemoteException {
        try {
            GroupChatSession session = mImService.getGroupChatSession(mChatId);
            if (session == null) {
                return mPersistentStorage.getSubject();
            }
            return session.getSubject();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                logger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            logger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }

    }

    /**
     * Returns true if it is possible to leave this group chat.
     * 
     * @return boolean
     * @throws RemoteException
     */
    public boolean isAllowedToLeave() throws RemoteException {
        try {
            if (isGroupChatAbandoned()) {
                if (logger.isActivated()) {
                    logger.debug(new StringBuilder("Cannot leave group chat with group chat Id '")
                            .append(mChatId).append("'").toString());
                }
                return false;
            }
            return true;

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                logger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            logger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Quits a group chat conversation. The conversation will continue between other participants if
     * there are enough participants.
     * 
     * @throws RemoteException
     */
    public void leave() throws RemoteException {
        try {
            if (isGroupChatAbandoned()) {
                throw new ServerApiUnsupportedOperationException(
                        "Cannot leave group chat with group chat Id : ".concat(mChatId));
            }
            final GroupChatSession session = mImService.getGroupChatSession(mChatId);
            if (session == null || !ServerApiUtils.isImsConnected()) {
                /*
                 * Quitting group chat that is inactive/ not available due to network drop should
                 * reject the next group chat invitation that is received
                 */
                mPersistentStorage.setStateAndReasonCode(State.ABORTED, ReasonCode.ABORTED_BY_USER);
                mPersistentStorage.setRejectNextGroupChatNextInvitation();
                return;
            }

            if (logger.isActivated()) {
                logger.info("Cancel session");
            }

            /* Terminate the session */
            new Thread() {
                public void run() {
                    session.terminateSession(TerminationReason.TERMINATION_BY_USER);
                }
            }.start();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                logger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            logger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Returns the participants. A participant is identified by its MSISDN in national or
     * international format, SIP address, SIP-URI or Tel-URI.
     * 
     * @return Participants
     * @throws RemoteException
     */
    public Map<ContactId, Integer> getParticipants() throws RemoteException {
        try {
            Map<ContactId, Integer> apiParticipants = new HashMap<ContactId, Integer>();
            Map<ContactId, ParticipantStatus> participants;

            GroupChatSession session = mImService.getGroupChatSession(mChatId);
            if (session == null) {
                participants = mPersistentStorage.getParticipants();
            } else {
                participants = session.getParticipants();
            }

            for (Map.Entry<ContactId, ParticipantStatus> participant : participants.entrySet()) {
                apiParticipants.put(participant.getKey(), participant.getValue().toInt());
            }

            return apiParticipants;

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                logger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            logger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Returns the max number of participants for a group chat from the group chat info subscription
     * (this value overrides the provisioning parameter)
     * 
     * @return Number
     * @throws RemoteException
     */
    public int getMaxParticipants() throws RemoteException {
        try {
            GroupChatSession session = mImService.getGroupChatSession(mChatId);
            if (session == null) {
                return mPersistentStorage.getMaxParticipants();
            }
            return session.getMaxParticipants();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                logger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            logger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Returns true if it is possible to invite additional participants to the group chat right now,
     * else returns false.
     * 
     * @return boolean
     * @throws RemoteException
     */
    public boolean isAllowedToInviteParticipants() throws RemoteException {
        try {
            if (!isGroupChatCapableOfReceivingParticipantInvitations()) {
                return false;
            }
            if (!isAllowedToInviteAdditionalParticipants(1)) {
                if (logger.isActivated()) {
                    logger.debug(new StringBuilder(
                            "Cannot invite participants to group chat with group chat Id '")
                            .append(mChatId)
                            .append("' as max number of participants has been reached already.")
                            .toString());
                }
                return false;
            }
            return true;

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                logger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            logger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Returns true if it is possible to invite the specified participants to the group chat right
     * now, else returns false.
     * 
     * @param participant ContactId
     * @return boolean
     * @throws RemoteException
     */
    public boolean isAllowedToInviteParticipant(ContactId participant) throws RemoteException {
        if (participant == null) {
            throw new ServerApiIllegalArgumentException("participant must not be null!");
        }
        if (!isAllowedToInviteParticipants()) {
            return false;
        }
        try {
            if (!isParticipantEligibleToBeInvited(participant)) {
                return false;
            }
            if (!isParticipantCapableToBeInvited(participant)) {
                return false;
            }
            return true;

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                logger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            logger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Invite additional participants to this group chat.
     * 
     * @param participants Set of participants
     * @throws RemoteException
     */
    public void inviteParticipants(final List<ContactId> participants) throws RemoteException {
        if (participants == null || participants.isEmpty()) {
            throw new ServerApiIllegalArgumentException(
                    "participants list must not be null or empty!");
        }
        if (!isGroupChatCapableOfReceivingParticipantInvitations()) {
            throw new ServerApiUnsupportedOperationException(
                    "Not capable of receiving particpant invitations!");
        }
        try {
            for (ContactId participant : participants) {
                if (!isParticipantEligibleToBeInvited(participant)) {
                    throw new ServerApiPermissionDeniedException(
                            "Participant not eligible to be invited!");
                }
            }

            final GroupChatSession session = mImService.getGroupChatSession(mChatId);

            boolean mediaEstablished = (session != null && session.isMediaEstablished());

            if (mediaEstablished) {
                inviteParticipants(session, new HashSet<ContactId>(participants));
                return;
            }

            if (session == null || !mediaEstablished) {
                Map<ContactId, ParticipantStatus> participantsToStore = mMessagingLog
                        .getParticipants(mChatId);

                for (ContactId contact : participants) {
                    participantsToStore.put(contact, ParticipantStatus.INVITE_QUEUED);
                }

                if (session != null) {
                    session.updateParticipants(participantsToStore);
                } else {
                    mMessagingLog.setGroupChatParticipants(mChatId, participantsToStore);
                }
            }

            if (session == null) {
                try {
                    if (isGroupChatRejoinable() && ServerApiUtils.isImsConnected()) {
                        rejoinGroupChat();
                    }
                } catch (ServerApiException e) {
                    if (logger.isActivated()) {
                        logger.warn(new StringBuilder(
                                "Could not auto-rejoin group chat with chatID '").append(mChatId)
                                .append("'").toString());
                    }
                }
            }
        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                logger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            logger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Invite additional participants to this group chat
     * 
     * @param session
     * @param participants
     */
    public void inviteParticipants(final GroupChatSession session, final Set<ContactId> participants) {
        if (logger.isActivated()) {
            logger.debug(new StringBuilder("Adding ")
                    .append(Arrays.toString(participants.toArray())).append(" to the session.")
                    .toString());
        }

        int maxNumberOfAdditionalParticipants = session.getMaxNumberOfAdditionalParticipants();
        if (maxNumberOfAdditionalParticipants < participants.size()) {
            throw new ServerApiPermissionDeniedException(new StringBuilder().append("Invite of ")
                    .append(participants.size())
                    .append(" participants failed, max number of additional participants: ")
                    .append(maxNumberOfAdditionalParticipants).append("!").toString());
        }

        new Thread() {
            public void run() {
                session.inviteParticipants(participants);
            }
        }.start();
    }

    /**
     * Actual send operation of message performed
     * 
     * @param msg Chat message
     */
    private void sendChatMessage(final ChatMessage msg) {
        final GroupChatSession groupChatSession = mImService.getGroupChatSession(mChatId);
        if (groupChatSession == null) {
            /*
             * If groupChatSession is not established, queue message and try to rejoin group chat
             * session
             */
            mPersistentStorage.addOutgoingGroupChatMessage(msg, Content.Status.QUEUED,
                    Content.ReasonCode.UNSPECIFIED);
            try {
                setRejoinedAsPartOfSendOperation(true);
                rejoinGroupChat();
                /*
                 * Observe that the queued message above will be dequeued on the trigger of
                 * established rejoined group chat and so the sendChatMessage method is finished
                 * here for now
                 */
                return;

            } catch (ServerApiException e) {
                /*
                 * Failed to rejoin group chat session. Ignoring this exception because we want to
                 * try again later.
                 */
                return;
            }
        }
        SipDialogPath chatSessionDialogPath = groupChatSession.getDialogPath();
        if (chatSessionDialogPath.isSessionEstablished()) {
            mPersistentStorage.addOutgoingGroupChatMessage(msg, Content.Status.SENDING,
                    Content.ReasonCode.UNSPECIFIED);
            groupChatSession.sendChatMessage(msg);
            return;
        }
        mPersistentStorage.addOutgoingGroupChatMessage(msg, Content.Status.QUEUED,
                Content.ReasonCode.UNSPECIFIED);
        if (!groupChatSession.isInitiatedByRemote()) {
            return;
        }
        if (logger.isActivated()) {
            logger.debug("Core chat session is pending: auto accept it.");
        }
        new Thread() {
            public void run() {
                groupChatSession.acceptSession();
            }
        }.start();
    }

    /**
     * Dequeue group chat message
     * 
     * @param message Chat message
     * @throws ServerApiException
     */
    public void dequeueChatMessage(ChatMessage message) throws ServerApiException {
        String msgId = message.getMessageId();
        String mimeType = message.getMimeType();
        if (logger.isActivated()) {
            logger.debug("Dequeue chat message msgId=".concat(msgId));
        }
        synchronized (lock) {
            mPersistentStorage.dequeueChatMessage(message);
            String apiMimeType = ChatUtils.networkMimeTypeToApiMimeType(mimeType);
            mBroadcaster.broadcastMessageStatusChanged(mChatId, apiMimeType, msgId, Status.SENDING,
                    Content.ReasonCode.UNSPECIFIED);
            final GroupChatSession groupChatSession = mImService.getGroupChatSession(mChatId);
            groupChatSession.sendChatMessage(message);
        }
    }

    /**
     * Returns true if it is possible to send messages in the group chat right now, else returns
     * false.
     * 
     * @return boolean
     * @throws RemoteException
     */
    public boolean isAllowedToSendMessage() throws RemoteException {
        try {
            if (!mRcsSettings.isGroupChatActivated()) {
                if (logger.isActivated()) {
                    logger.debug(new StringBuilder(
                            "Cannot send message on group chat with group chat Id '")
                            .append(mChatId).append("' as group chat feature is not supported.")
                            .toString());
                }
                return false;
            }
            if (isGroupChatAbandoned()) {
                if (logger.isActivated()) {
                    logger.debug(new StringBuilder(
                            "Cannot send message on group chat with group chat Id '")
                            .append(mChatId).append("'").toString());
                }
                return false;
            }
            if (!mRcsSettings.getMyCapabilities().isImSessionSupported()) {
                if (logger.isActivated()) {
                    logger.debug(new StringBuilder(
                            "Cannot send message on group chat with group chat Id '")
                            .append(mChatId)
                            .append("' as IM capabilities are not supported for self.").toString());
                }
                return false;
            }
            GroupChatSession session = mImService.getGroupChatSession(mChatId);
            if (session == null) {
                if (!isGroupChatRejoinable()) {
                    return false;
                }
            }
            return true;

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                logger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            logger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Sends a text message to the group
     * 
     * @param text Message
     * @return Chat message
     * @throws RemoteException
     */
    public IChatMessage sendMessage(final String text) throws RemoteException {
        if (TextUtils.isEmpty(text)) {
            throw new ServerApiIllegalArgumentException(
                    "GroupChat message must not be null or empty!");
        }
        int messageLength = text.length();
        int maxMessageLength = mRcsSettings.getMaxGroupChatMessageLength();
        if (messageLength > maxMessageLength) {
            throw new ServerApiIllegalArgumentException(new StringBuilder()
                    .append("chat message length: ").append(messageLength)
                    .append(" exeeds max group chat message length: ").append(maxMessageLength)
                    .append("!").toString());
        }
        if (!isAllowedToSendMessage()) {
            throw new ServerApiPermissionDeniedException(
                    "Not allowed to send GroupChat message on the connected IMS server!");
        }
        try {
            mImService.removeGroupChatComposingStatus(mChatId); /* clear cache */
            long timestamp = System.currentTimeMillis();
            /* For outgoing message, timestampSent = timestamp */
            ChatMessage msg = ChatUtils.createTextMessage(null, text, timestamp, timestamp);
            ChatMessagePersistedStorageAccessor persistentStorage = new ChatMessagePersistedStorageAccessor(
                    mMessagingLog, msg.getMessageId(), msg.getRemoteContact(), text,
                    MimeType.TEXT_MESSAGE, mChatId, Direction.OUTGOING);

            /* If the IMS is connected at this time then send this message. */
            if (ServerApiUtils.isImsConnected()) {
                sendChatMessage(msg);
            } else {
                /* If the IMS is NOT connected at this time then queue message. */
                mPersistentStorage.addOutgoingGroupChatMessage(msg, Content.Status.QUEUED,
                        Content.ReasonCode.UNSPECIFIED);
            }
            return new ChatMessageImpl(persistentStorage);

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                logger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            logger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Sends a geoloc message
     * 
     * @param geoloc Geoloc
     * @return ChatMessage
     * @throws RemoteException
     */
    public IChatMessage sendMessage2(Geoloc geoloc) throws RemoteException {
        if (geoloc == null) {
            throw new ServerApiIllegalArgumentException("Geoloc message must not be null!");
        }
        if (!isAllowedToSendMessage()) {
            throw new ServerApiPermissionDeniedException(
                    "Not allowed to send Geoloc message on the connected IMS server!");
        }
        String label = geoloc.getLabel();
        if (label != null) {
            int labelLength = label.length();
            int labelMaxLength = mRcsSettings.getMaxGeolocLabelLength();
            if (labelLength > labelMaxLength) {
                throw new ServerApiIllegalArgumentException(new StringBuilder()
                        .append("geoloc message label length: ").append(labelLength)
                        .append(" exeeds max length: ").append(labelMaxLength).append("!")
                        .toString());
            }
        }
        try {
            long timestamp = System.currentTimeMillis();
            /** For outgoing message, timestampSent = timestamp */
            ChatMessage geolocMsg = ChatUtils.createGeolocMessage(null, geoloc, timestamp,
                    timestamp);
            ChatMessagePersistedStorageAccessor persistentStorage = new ChatMessagePersistedStorageAccessor(
                    mMessagingLog, geolocMsg.getMessageId(), geolocMsg.getRemoteContact(),
                    geolocMsg.toString(), MimeType.GEOLOC_MESSAGE, mChatId, Direction.OUTGOING);

            /* If the IMS is connected at this time then send this message. */
            if (ServerApiUtils.isImsConnected()) {
                sendChatMessage(geolocMsg);
            } else {
                /* If the IMS is NOT connected at this time then queue message. */
                mPersistentStorage.addOutgoingGroupChatMessage(geolocMsg, Content.Status.QUEUED,
                        Content.ReasonCode.UNSPECIFIED);
            }
            return new ChatMessageImpl(persistentStorage);

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                logger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            logger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Sends an is-composing event. The status is set to true when typing a message, else it is set
     * to false.
     * 
     * @param status
     * @throws RemoteException
     */
    public void setComposingStatus(final boolean status) throws RemoteException {
        try {
            mImService.removeGroupChatComposingStatus(mChatId);
            final GroupChatSession session = mImService.getGroupChatSession(mChatId);
            if (session == null) {
                if (logger.isActivated()) {
                    logger.debug("Unable to send composing event '" + status
                            + "' since group chat session found with chatId '" + mChatId
                            + "' does not exist for now");
                }
                mImService.addGroupChatComposingStatus(mChatId, status);
                return;
            }
            if (session.getDialogPath().isSessionEstablished()) {
                if (!session.sendIsComposingStatus(status)) {
                    mImService.addGroupChatComposingStatus(mChatId, status);
                }
                return;
            }
            if (!session.isInitiatedByRemote()) {
                mImService.addGroupChatComposingStatus(mChatId, status);
                return;
            }
            ImSessionStartMode imSessionStartMode = mRcsSettings.getImSessionStartMode();
            switch (imSessionStartMode) {
                case ON_OPENING:
                case ON_COMPOSING:
                    if (logger.isActivated()) {
                        logger.debug("Core chat session is pending: auto accept it.");
                    }
                    session.acceptSession();
                    if (!session.sendIsComposingStatus(status)) {
                        mImService.addGroupChatComposingStatus(mChatId, status);
                    }
                    break;
                default:
                    break;
            }
        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                logger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            logger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Rejoins an existing group chat from its unique chat ID
     * 
     * @return Group chat
     * @throws ServerApiException
     */
    public IGroupChat rejoinGroupChat() throws ServerApiException {
        if (logger.isActivated()) {
            logger.info("Rejoin group chat session related to the conversation " + mChatId);
        }

        ServerApiUtils.testIms();

        try {
            final ChatSession session = mImService.rejoinGroupChatSession(mChatId);
            session.addListener(this);
            new Thread() {
                public void run() {
                    session.startSession();
                }
            }.start();
            mChatService.addGroupChat(this);
            return this;

        } catch (CoreException e) {
            throw new ServerApiException(e);
        }
    }

    /**
     * Restarts a previous group chat from its unique chat ID
     * 
     * @return Group chat
     * @throws ServerApiException
     */
    public IGroupChat restartGroupChat() throws ServerApiException {
        if (logger.isActivated()) {
            logger.info("Restart group chat session related to the conversation " + mChatId);
        }

        ServerApiUtils.testIms();

        try {
            final GroupChatSession session = mImService.restartGroupChatSession(mChatId);
            session.addListener(this);
            new Thread() {
                public void run() {
                    session.startSession();
                }
            }.start();
            mChatService.addGroupChat(this);
            return this;

        } catch (CoreException e) {
            throw new ServerApiException(e);
        }
    }

    /**
     * open the chat conversation. Note: if its an incoming pending chat session and the parameter
     * IM SESSION START is 0 then the session is accepted now.
     * 
     * @see ImSessionStartMode
     * @throws RemoteException
     */
    public void openChat() throws RemoteException {
        if (logger.isActivated()) {
            logger.info("Open a group chat session with chatId " + mChatId);
        }
        try {
            final GroupChatSession session = mImService.getGroupChatSession(mChatId);
            if (session == null) {
                /*
                 * If there is no session ongoing right now then we do not need to open anything
                 * right now so we just return here. A sending of a new message on this group chat
                 * will anyway result in a rejoin attempt if this group chat has not been left by
                 * choice so we do not need to do anything more here for now.
                 */
                return;
            }
            if (session.getDialogPath().isSessionEstablished()) {
                return;
            }
            ImSessionStartMode imSessionStartMode = mRcsSettings.getImSessionStartMode();
            if (!session.isInitiatedByRemote()) {
                /*
                 * This method needs to accept pending invitation if IM_SESSION_START_MODE is 0,
                 * which is not applicable if session is remote originated so we return here.
                 */
                return;
            }
            if (ImSessionStartMode.ON_OPENING == imSessionStartMode) {
                if (logger.isActivated()) {
                    logger.debug("Core chat session is pending: auto accept it, as IM_SESSION_START mode = 0");
                }
                session.acceptSession();
            }
        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                logger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            logger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Try to restart group chat session on failure of restart
     */
    private void handleGroupChatRejoinAsPartOfSendOperationFailed() {
        try {
            restartGroupChat();

        } catch (ServerApiException e) {
            // failed to restart group chat session. Ignoring this
            // exception because we want to try again later.
        }
    }

    /**
     * @param enable
     */
    public void setRejoinedAsPartOfSendOperation(boolean enable) {
        mGroupChatRejoinedAsPartOfSendOperation = enable;
    }

    /*------------------------------- SESSION EVENTS ----------------------------------*/

    @Override
    public void handleSessionStarted(ContactId contact) {
        boolean loggerActivated = logger.isActivated();
        if (loggerActivated) {
            logger.info("Session started");
        }
        setRejoinedAsPartOfSendOperation(false);
        synchronized (lock) {
            GroupChatSession session = mImService.getGroupChatSession(mChatId);
            Boolean composingStatus = mImService.getGroupChatComposingStatus(mChatId);
            if (composingStatus != null) {
                if (loggerActivated) {
                    logger.debug("Sending isComposing command with status :".concat(composingStatus
                            .toString()));
                }
                if (session.sendIsComposingStatus(composingStatus)) {
                    mImService.removeGroupChatComposingStatus(mChatId);
                }
            }
            if (mPersistentStorage.setRejoinId(session.getImSessionIdentity())) {
                mBroadcaster.broadcastStateChanged(mChatId, State.STARTED, ReasonCode.UNSPECIFIED);
            }
        }
        CoreListener listener = mCore.getListener();
        listener.tryToInviteQueuedGroupChatParticipantInvitations(mChatId, mImService);
        listener.tryToDequeueGroupChatMessagesAndGroupFileTransfers(mChatId, mImService);
    }

    @Override
    public void handleSessionAborted(ContactId contact, TerminationReason reason) {
        GroupChatSession session = mImService.getGroupChatSession(mChatId);
        if (session != null && session.isPendingForRemoval()) {
            /*
             * If there is an ongoing group chat session with same chatId, this session has to be
             * silently aborted so after aborting the session we make sure to not call the rest of
             * this method that would otherwise abort the "current" session also and the GroupChat
             * as a whole which is of course not the intention here
             */
            if (logger.isActivated()) {
                logger.info(new StringBuilder("Session marked pending for removal status ")
                        .append(State.ABORTED).append(" terminationReason ").append(reason)
                        .toString());
            }
            return;
        }
        if (logger.isActivated()) {
            logger.info(new StringBuilder("Session status ").append(State.ABORTED)
                    .append(" terminationReason ").append(reason).toString());
        }
        setRejoinedAsPartOfSendOperation(false);
        synchronized (lock) {
            mChatService.removeGroupChat(mChatId);
            switch (reason) {
                case TERMINATION_BY_CONNECTION_LOST:
                case TERMINATION_BY_SYSTEM:
                    /*
                     * This error is caused because of a network drop so the group chat is not set
                     * to ABORTED state in this case as it will try to be auto-rejoined when IMS
                     * connection is regained
                     */
                    break;
                case TERMINATION_BY_USER:
                    setStateAndReasonCode(State.ABORTED, ReasonCode.ABORTED_BY_USER);
                    break;
                case TERMINATION_BY_TIMEOUT:
                case TERMINATION_BY_INACTIVITY:
                    setStateAndReasonCode(State.ABORTED, ReasonCode.ABORTED_BY_INACTIVITY);
                    break;
                case TERMINATION_BY_REMOTE:
                    setStateAndReasonCode(State.ABORTED, ReasonCode.ABORTED_BY_REMOTE);
                    break;
                default:
                    throw new IllegalArgumentException(
                            new StringBuilder(
                                    "Unknown reason in GroupChatImpl.handleSessionAborted; terminationReason=")
                                    .append(reason).append("!").toString());
            }
        }
    }

    @Override
    public void handleReceiveMessage(ChatMessage msg, boolean imdnDisplayedRequested) {
        String msgId = msg.getMessageId();
        ContactId remote = msg.getRemoteContact();
        if (logger.isActivated()) {
            logger.info("New IM with Id '" + msgId + "' received from " + remote);
        }
        synchronized (lock) {
            mPersistentStorage.addIncomingGroupChatMessage(msg, imdnDisplayedRequested);
            if (remote != null) {
                mContactManager.setContactDisplayName(remote, msg.getDisplayName());
            }
            String apiMimeType = ChatUtils.networkMimeTypeToApiMimeType(msg.getMimeType());
            mBroadcaster.broadcastMessageReceived(apiMimeType, msgId);
        }
    }

    @Override
    public void handleImError(ChatError error, ChatMessage message) {
        GroupChatSession session = mImService.getGroupChatSession(mChatId);
        int chatErrorCode = error.getErrorCode();
        if (session != null && session.isPendingForRemoval()) {
            /*
             * If there is an ongoing group chat session with same chatId, this session has to be
             * silently aborted so after aborting the session we make sure to not call the rest of
             * this method that would otherwise abort the "current" session also and the GroupChat
             * as a whole which is of course not the intention here
             */
            if (logger.isActivated()) {
                logger.info(new StringBuilder("Session marked pending for removal - Error ")
                        .append(chatErrorCode).toString());
            }
            return;
        }
        if (logger.isActivated()) {
            logger.info(new StringBuilder("IM error ").append(chatErrorCode).toString());
        }
        synchronized (lock) {
            mChatService.removeGroupChat(mChatId);
            int chatError = error.getErrorCode();
            switch (chatError) {
                case ChatError.SESSION_INITIATION_CANCELLED:
                    /* Intentional fall through */
                case ChatError.SESSION_INITIATION_DECLINED:
                    setStateAndReasonCode(State.REJECTED, ReasonCode.REJECTED_BY_REMOTE);
                    mCore.getListener()
                            .tryToMarkQueuedGroupChatMessagesAndGroupFileTransfersAsFailed(mChatId);
                    break;
                case ChatError.SESSION_NOT_FOUND:
                    if (mGroupChatRejoinedAsPartOfSendOperation) {
                        handleGroupChatRejoinAsPartOfSendOperationFailed();
                    }
                    break;
                case ChatError.SESSION_INITIATION_FAILED:
                    /* Intentional fall through */
                case ChatError.SESSION_RESTART_FAILED:
                    /* Intentional fall through */
                case ChatError.SUBSCRIBE_CONFERENCE_FAILED:
                    /* Intentional fall through */
                case ChatError.UNEXPECTED_EXCEPTION:
                    setStateAndReasonCode(State.FAILED, ReasonCode.FAILED_INITIATION);
                    mCore.getListener()
                            .tryToMarkQueuedGroupChatMessagesAndGroupFileTransfersAsFailed(mChatId);
                    break;
                /*
                 * For cases where rejoin has failed or send response failed due to no ACK/200 OK
                 * response, we should not change Chat state.
                 */
                /* Intentional fall through */
                case ChatError.SESSION_REJOIN_FAILED:
                    /* Intentional fall through */
                case ChatError.SEND_RESPONSE_FAILED:
                    break;
                /*
                 * This error is caused because of a network drop so the group chat is not set to
                 * ABORTED state in this case as it will be auto-rejoined when network connection is
                 * regained
                 */
                case ChatError.MEDIA_SESSION_FAILED:
                case ChatError.MEDIA_SESSION_BROKEN:
                    break;
                default:
                    throw new IllegalArgumentException(new StringBuilder(
                            "Unknown reason; chatError=").append(chatError).append("!").toString());
            }
        }
        setRejoinedAsPartOfSendOperation(false);
    }

    @Override
    public void handleIsComposingEvent(ContactId contact, boolean status) {
        if (logger.isActivated()) {
            logger.info(new StringBuilder().append(contact).append(" is composing status set to ")
                    .append(status).toString());
        }
        synchronized (lock) {
            // Notify event listeners
            mBroadcaster.broadcastComposingEvent(mChatId, contact, status);
        }
    }

    @Override
    public void handleMessageFailedSend(String msgId, String mimeType) {
        if (logger.isActivated()) {
            logger.info(new StringBuilder("Message sending failed; msgId=").append(msgId)
                    .append("mimeType=").append(mimeType).append(".").toString());
        }

        synchronized (lock) {
            if (mPersistentStorage.setMessageStatusAndReasonCode(msgId, Status.FAILED,
                    Content.ReasonCode.FAILED_SEND)) {
                mBroadcaster.broadcastMessageStatusChanged(getChatId(), mimeType, msgId,
                        Status.FAILED, Content.ReasonCode.FAILED_SEND);
            }
        }
    }

    @Override
    public void handleMessageSent(String msgId, String mimeType) {
        if (logger.isActivated()) {
            logger.info(new StringBuilder("Text message sent; msgId=").append(msgId)
                    .append("mimeType=").append(mimeType).append(".").toString());
        }

        synchronized (lock) {
            if (mPersistentStorage.setMessageStatusAndReasonCode(msgId, Status.SENT,
                    Content.ReasonCode.UNSPECIFIED)) {
                mBroadcaster.broadcastMessageStatusChanged(getChatId(), mimeType, msgId,
                        Status.SENT, Content.ReasonCode.UNSPECIFIED);
            }
        }
    }

    @Override
    public void handleConferenceEvent(ContactId contact, ParticipantStatus status, long timestamp) {
        if (logger.isActivated()) {
            logger.info("New conference event " + status.toString() + " for " + contact);
        }
        synchronized (lock) {
            if (ParticipantStatus.CONNECTED.equals(status)) {
                mPersistentStorage.addGroupChatEvent(mChatId, contact,
                        GroupChatEvent.Status.JOINED, timestamp);
            } else if (ParticipantStatus.DEPARTED.equals(status)) {
                mPersistentStorage.addGroupChatEvent(mChatId, contact,
                        GroupChatEvent.Status.DEPARTED, timestamp);
            }
        }
    }

    @Override
    public void handleMessageDeliveryStatus(ContactId contact, ImdnDocument imdn) {
        String status = imdn.getStatus();
        String msgId = imdn.getMsgId();
        long timestamp = imdn.getDateTime();

        if (logger.isActivated()) {
            logger.info(new StringBuilder("Handling message delivery status; contact=")
                    .append(contact).append(", msgId=").append(msgId).append(", status=")
                    .append(status).append(", notificationType=")
                    .append(imdn.getNotificationType()).toString());
        }
        if (ImdnDocument.DELIVERY_STATUS_DELIVERED.equals(status)) {
            handleMessageDeliveryStatusDelivered(contact, msgId, timestamp);
        } else if (ImdnDocument.DELIVERY_STATUS_DISPLAYED.equals(status)) {
            handleMessageDeliveryStatusDisplayed(contact, msgId, timestamp);
        } else if (ImdnDocument.DELIVERY_STATUS_ERROR.equals(status)
                || ImdnDocument.DELIVERY_STATUS_FAILED.equals(status)
                || ImdnDocument.DELIVERY_STATUS_FORBIDDEN.equals(status)) {
            Content.ReasonCode reasonCode = imdnToMessageFailedReasonCode(imdn);
            handleMessageDeliveryStatusFailed(contact, msgId, reasonCode);
        }
    }

    @Override
    public void handleDeliveryStatus(String contributionId, ContactId contact, ImdnDocument imdn) {
        String msgId = imdn.getMsgId();

        // TODO: Potential race condition, after we've checked that the message is persisted
        // it may be removed before the handle method executes.
        if (mMessagingLog.isMessagePersisted(msgId)) {
            handleMessageDeliveryStatus(contact, imdn);
            return;
        }

        if (mMessagingLog.isFileTransfer(msgId)) {
            mImService.receiveGroupFileDeliveryStatus(contributionId, contact, imdn);
            return;
        }

        logger.error(new StringBuilder(
                "Imdn delivery report received referencing an entry that was ")
                .append("not found in our database. Message id ").append(msgId)
                .append(", ignoring.").toString());
    }

    /**
     * Request to add participant has failed
     * 
     * @param contact Contact ID
     * @param reason Error reason
     */
    public void handleAddParticipantFailed(ContactId contact, String reason) {
        if (logger.isActivated()) {
            logger.info("Add participant request has failed " + reason);
        }

        synchronized (lock) {
            mBroadcaster.broadcastParticipantStatusChanged(mChatId, contact,
                    ParticipantStatus.FAILED);
        }
    }

    @Override
    public void handleSessionAccepted(ContactId contact) {
        if (logger.isActivated()) {
            logger.info("Accepting group chat session");
        }
        synchronized (lock) {
            setStateAndReasonCode(State.ACCEPTING, ReasonCode.UNSPECIFIED);
        }
    }

    @Override
    public void handleSessionRejected(ContactId contact, TerminationReason reason) {
        switch (reason) {
            case TERMINATION_BY_SYSTEM:
                /* Intentional fall through */
            case TERMINATION_BY_CONNECTION_LOST:
                handleSessionRejected(ReasonCode.REJECTED_BY_SYSTEM);
                break;
            case TERMINATION_BY_TIMEOUT:
                handleSessionRejected(ReasonCode.REJECTED_BY_TIMEOUT);
                break;
            case TERMINATION_BY_REMOTE:
                handleSessionRejected(ReasonCode.REJECTED_BY_REMOTE);
                break;
            default:
                throw new IllegalArgumentException(new StringBuilder(
                        "Unknown reason RejectedReason=").append(reason).append("!").toString());
        }
    }

    @Override
    public void handleSessionInvited(ContactId contact, String subject,
            Map<ContactId, ParticipantStatus> participants, long timestamp) {
        if (logger.isActivated()) {
            logger.info("Invited to group chat session");
        }
        synchronized (lock) {
            if (mMessagingLog.isGroupChatPersisted(mChatId)
                    && mPersistentStorage.setParticipantsStateAndReasonCode(participants,
                            State.INVITED, ReasonCode.UNSPECIFIED)) {
                mBroadcaster.broadcastInvitation(mChatId);
            } else {
                mPersistentStorage.addGroupChat(contact, subject, participants, State.INVITED,
                        ReasonCode.UNSPECIFIED, Direction.INCOMING, timestamp);
                mBroadcaster.broadcastInvitation(mChatId);
            }
        }
    }

    @Override
    public void handleSessionAutoAccepted(ContactId contact, String subject,
            Map<ContactId, ParticipantStatus> participants, long timestamp) {
        if (logger.isActivated()) {
            logger.info("Session auto accepted");
        }
        synchronized (lock) {
            if (mMessagingLog.isGroupChatPersisted(mChatId)
                    && mPersistentStorage.setParticipantsStateAndReasonCode(participants,
                            State.ACCEPTING, ReasonCode.UNSPECIFIED)) {
                mBroadcaster.broadcastInvitation(mChatId);
            } else {
                mPersistentStorage.addGroupChat(contact, subject, participants, State.ACCEPTING,
                        ReasonCode.UNSPECIFIED, Direction.INCOMING, timestamp);
                mBroadcaster.broadcastInvitation(mChatId);
            }
        }
    }

    @Override
    public void handleParticipantUpdates(Map<ContactId, ParticipantStatus> updatedParticipants,
            Map<ContactId, ParticipantStatus> allParticipants) {
        synchronized (lock) {
            if (!mMessagingLog.setGroupChatParticipants(mChatId, allParticipants)) {
                return;
            }
        }

        for (Map.Entry<ContactId, ParticipantStatus> updatedParticipant : updatedParticipants
                .entrySet()) {
            ContactId contact = updatedParticipant.getKey();
            ParticipantStatus status = updatedParticipant.getValue();

            if (logger.isActivated()) {
                logger.info("ParticipantUpdate for: " + contact + " status: " + status);
            }

            mBroadcaster.broadcastParticipantStatusChanged(mChatId, contact, status);
        }
    }
}
