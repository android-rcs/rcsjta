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

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.service.ContactInfo.RcsStatus;
import com.gsma.rcs.core.ims.service.ContactInfo.RegistrationState;
import com.gsma.rcs.core.ims.service.ImsServiceSession.TerminationReason;
import com.gsma.rcs.core.ims.service.SessionNotEstablishedException;
import com.gsma.rcs.core.ims.service.capability.Capabilities;
import com.gsma.rcs.core.ims.service.capability.Capabilities.CapabilitiesBuilder;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.ChatError;
import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.im.chat.GroupChatInfo;
import com.gsma.rcs.core.ims.service.im.chat.GroupChatSession;
import com.gsma.rcs.core.ims.service.im.chat.GroupChatSessionListener;
import com.gsma.rcs.core.ims.service.im.chat.RejoinGroupChatSession;
import com.gsma.rcs.core.ims.service.im.chat.RestartGroupChatSession;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.contact.ContactManagerException;
import com.gsma.rcs.provider.history.HistoryLog;
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
import com.gsma.services.rcs.chat.GroupChat;
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

    private final GroupChatPersistedStorageAccessor mPersistedStorage;

    private final RcsSettings mRcsSettings;

    private final ContactManager mContactManager;

    private final ChatServiceImpl mChatService;

    private final MessagingLog mMessagingLog;

    private final HistoryLog mHistoryLog;

    private boolean mGroupChatRejoinedAsPartOfSendOperation = false;

    private static final Set<ParticipantStatus> RECIPIENT_STATUSES = new HashSet<>();
    static {
        RECIPIENT_STATUSES.add(ParticipantStatus.INVITE_QUEUED);
        RECIPIENT_STATUSES.add(ParticipantStatus.INVITING);
        RECIPIENT_STATUSES.add(ParticipantStatus.INVITED);
        RECIPIENT_STATUSES.add(ParticipantStatus.CONNECTED);
        RECIPIENT_STATUSES.add(ParticipantStatus.DISCONNECTED);
    }
    /**
     * Lock used for synchronization
     */
    private final Object mLock = new Object();

    private static final Logger sLogger = Logger.getLogger(GroupChatImpl.class.getName());

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
     * @param historyLog HistoryLog
     */
    public GroupChatImpl(InstantMessagingService imService, String chatId,
            IGroupChatEventBroadcaster broadcaster,
            GroupChatPersistedStorageAccessor persistentStorage, RcsSettings rcsSettings,
            ChatServiceImpl chatService, ContactManager contactManager, MessagingLog messagingLog,
            HistoryLog historyLog) {
        mImService = imService;
        mChatId = chatId;
        mBroadcaster = broadcaster;
        mPersistedStorage = persistentStorage;
        mRcsSettings = rcsSettings;
        mContactManager = contactManager;
        mChatService = chatService;
        mMessagingLog = messagingLog;
        mHistoryLog = historyLog;
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
        if (mPersistedStorage.setStateAndReasonCode(state, reasonCode)) {
            mBroadcaster.broadcastStateChanged(mChatId, state, reasonCode);
        }
    }

    private void handleSessionRejected(ReasonCode reasonCode) {
        setRejoinedAsPartOfSendOperation(false);
        synchronized (mLock) {
            mChatService.removeGroupChat(mChatId);
            setStateAndReasonCode(State.REJECTED, reasonCode);
        }
        /*
         * Try to dequeue all one-one chat messages and file transfers as a chat session is torn
         * down now.
         */
        mImService.tryToDequeueAllOneToOneChatMessagesAndOneToOneFileTransfers();
    }

    private void onMessageDeliveryStatusDelivered(ContactId contact, String msgId,
            long timestampDelivered) {
        String mimeType = mMessagingLog.getMessageMimeType(msgId);
        synchronized (mLock) {
            if (mPersistedStorage.setGroupChatDeliveryInfoDelivered(mChatId, contact, msgId,
                    timestampDelivered)) {
                mBroadcaster.broadcastMessageGroupDeliveryInfoChanged(mChatId, contact, mimeType,
                        msgId, GroupDeliveryInfo.Status.DELIVERED,
                        GroupDeliveryInfo.ReasonCode.UNSPECIFIED);
                if (mPersistedStorage.isDeliveredToAllRecipients(msgId)) {
                    if (mPersistedStorage.setMessageStatusDelivered(msgId, timestampDelivered)) {
                        mBroadcaster.broadcastMessageStatusChanged(mChatId, mimeType, msgId,
                                Status.DELIVERED, Content.ReasonCode.UNSPECIFIED);
                    }
                }
            }
        }
    }

    private void onMessageDeliveryStatusDisplayed(ContactId contact, String msgId,
            long timestampDisplayed) {
        String mimeType = mMessagingLog.getMessageMimeType(msgId);
        synchronized (mLock) {
            if (mPersistedStorage.setDeliveryInfoDisplayed(mChatId, contact, msgId,
                    timestampDisplayed)) {
                mBroadcaster.broadcastMessageGroupDeliveryInfoChanged(mChatId, contact, mimeType,
                        msgId, GroupDeliveryInfo.Status.DISPLAYED,
                        GroupDeliveryInfo.ReasonCode.UNSPECIFIED);
                if (mPersistedStorage.isDisplayedByAllRecipients(msgId)) {
                    if (mPersistedStorage.setMessageStatusDisplayed(msgId, timestampDisplayed)) {
                        mBroadcaster.broadcastMessageStatusChanged(mChatId, mimeType, msgId,
                                Status.DISPLAYED, Content.ReasonCode.UNSPECIFIED);
                    }
                }
            }
        }
    }

    private void onMessageDeliveryStatusFailed(ContactId contact, String msgId,
            Content.ReasonCode reasonCode) {
        String mimeType = mMessagingLog.getMessageMimeType(msgId);
        synchronized (mLock) {
            if (Content.ReasonCode.FAILED_DELIVERY == reasonCode) {
                if (!mPersistedStorage.setGroupDeliveryInfoStatusAndReasonCode(mChatId, contact,
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
                if (!mPersistedStorage.setGroupDeliveryInfoStatusAndReasonCode(mChatId, contact,
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

    public boolean isGroupChatAbandoned() {
        GroupChatSession session = mImService.getGroupChatSession(mChatId);
        if (session != null) {
            /* Group chat is not abandoned if there exists a session */
            return false;
        }
        ReasonCode reasonCode = mPersistedStorage.getReasonCode();
        if (reasonCode == null) {
            return false;
        }
        switch (reasonCode) {
            case ABORTED_BY_USER:
            case ABORTED_BY_REMOTE:
            case FAILED_INITIATION:
            case REJECTED_BY_REMOTE:
            case REJECTED_MAX_CHATS:
            case REJECTED_SPAM:
            case REJECTED_BY_TIMEOUT:
            case REJECTED_BY_SYSTEM:
                if (sLogger.isActivated()) {
                    sLogger.debug(new StringBuilder("Group chat with chatId '").append(mChatId)
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
            if (sLogger.isActivated()) {
                sLogger.debug(new StringBuilder(
                        "Cannot add participants to on group chat with group chat Id '")
                        .append(mChatId)
                        .append("' as group chat feature has been disabled by the operator.")
                        .toString());
            }
            return false;
        }
        if (isGroupChatAbandoned()) {
            if (sLogger.isActivated()) {
                sLogger.debug(new StringBuilder(
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
            if (sLogger.isActivated()) {
                sLogger.debug(new StringBuilder("Group chat with group chat Id '").append(mChatId)
                        .append("' is not rejoinable as the group chat does not exist in DB.")
                        .toString());
            }
            return false;
        }
        if (groupChat.getRejoinId() == null) {
            if (sLogger.isActivated()) {
                sLogger.debug(new StringBuilder("Group chat with group chat Id '")
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
                        if (sLogger.isActivated()) {
                            sLogger.debug(new StringBuilder(
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
            if (sLogger.isActivated()) {
                sLogger.debug(new StringBuilder(
                        "Cannot invite participant to group chat with group chat Id '")
                        .append(mChatId).append("' as the capabilities of participant '")
                        .append(participant).append("' are not known.").toString());
            }
            return false;
        }
        if (!remoteCapabilities.isImSessionSupported()) {
            if (sLogger.isActivated()) {
                sLogger.debug(new StringBuilder(
                        "Cannot invite participant to group chat with group chat Id '")
                        .append(mChatId).append("' as the participant '").append(participant)
                        .append("' does not have IM capabilities.").toString());
            }
            return false;
        }
        if (inviteOnlyFullSF && !remoteCapabilities.isGroupChatStoreForwardSupported()) {
            if (sLogger.isActivated()) {
                sLogger.debug(new StringBuilder(
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

    private void addOutgoingGroupChatMessage(ChatMessage msg, Status status,
            Content.ReasonCode reasonCode) throws PayloadException {
        Set<ContactId> recipients = getRecipients();
        if (recipients == null) {
            throw new ServerApiPersistentStorageException(
                    "Unable to determine recipients of the group chat " + mChatId
                            + " to set as recipients for the the group chat message "
                            + msg.getMessageId() + "!");
        }
        mPersistedStorage.addOutgoingGroupChatMessage(msg, recipients, status, reasonCode);
    }

    /**
     * Checks if the group chat is active
     * 
     * @return boolean
     */
    public boolean isGroupChatActive() {
        GroupChatSession session = mImService.getGroupChatSession(mChatId);
        return session != null || State.STARTED == mPersistedStorage.getState();
    }

    /**
     * Get the participants of a group chat matching any of the specified statuses
     * 
     * @param statuses PatricipantStatues to match
     * @return Set of ContactIds
     */
    public Map<ContactId, ParticipantStatus> getParticipants(Set<ParticipantStatus> statuses) {
        GroupChatSession session = mImService.getGroupChatSession(mChatId);
        if (session == null) {
            return mPersistedStorage.getParticipants(statuses);
        }
        return session.getParticipants(statuses);
    }

    /**
     * Get the recipients of a group chat message or a group file transfer in this group chat
     * 
     * @return Set of ContactIds
     */
    public Set<ContactId> getRecipients() {
        return getParticipants(RECIPIENT_STATUSES).keySet();
    }

    /**
     * Get chat ID
     * 
     * @return Chat ID
     */
    @Override
    public String getChatId() {
        return mChatId;
    }

    /**
     * Get remote contact identifier
     * 
     * @return ContactId
     * @throws RemoteException
     */
    @Override
    public ContactId getRemoteContact() throws RemoteException {
        try {
            GroupChatSession session = mImService.getGroupChatSession(mChatId);
            if (session == null) {
                return mPersistedStorage.getRemoteContact();
            }
            return session.getRemoteContact();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }

    }

    /**
     * Returns the direction of the group chat (incoming or outgoing)
     * 
     * @return Direction
     * @throws RemoteException
     */
    @Override
    public int getDirection() throws RemoteException {
        try {
            GroupChatSession session = mImService.getGroupChatSession(mChatId);
            if (session == null) {
                return mPersistedStorage.getDirection().toInt();
            }
            if (session.isInitiatedByRemote()) {
                return Direction.INCOMING.toInt();
            }
            return Direction.OUTGOING.toInt();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }

    }

    /**
     * Returns the state of the group chat
     * 
     * @return State
     * @throws RemoteException
     */
    @Override
    public int getState() throws RemoteException {
        try {
            GroupChatSession session = mImService.getGroupChatSession(mChatId);
            if (session == null) {
                return mPersistedStorage.getState().toInt();
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
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Returns the reason code of the state of the group chat
     * 
     * @return ReasonCode
     * @throws RemoteException
     */
    @Override
    public int getReasonCode() throws RemoteException {
        try {
            GroupChatSession session = mImService.getGroupChatSession(mChatId);
            if (session == null) {
                return mPersistedStorage.getReasonCode().toInt();
            }
            return ReasonCode.UNSPECIFIED.toInt();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
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
    @Override
    public long getTimestamp() throws RemoteException {
        try {
            GroupChatSession session = mImService.getGroupChatSession(mChatId);
            if (session == null) {
                return mPersistedStorage.getTimestamp();
            }
            return session.getTimestamp();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Get subject associated to the session
     * 
     * @return String
     * @throws RemoteException
     */
    @Override
    public String getSubject() throws RemoteException {
        try {
            GroupChatSession session = mImService.getGroupChatSession(mChatId);
            if (session == null) {
                return mPersistedStorage.getSubject();
            }
            return session.getSubject();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }

    }

    /**
     * Returns true if it is possible to leave this group chat.
     * 
     * @return boolean
     * @throws RemoteException
     */
    @Override
    public boolean isAllowedToLeave() throws RemoteException {
        try {
            if (isGroupChatAbandoned()) {
                if (sLogger.isActivated()) {
                    sLogger.debug(new StringBuilder("Cannot leave group chat with group chat Id '")
                            .append(mChatId).append("'").toString());
                }
                return false;
            }
            return true;

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Quits a group chat conversation. The conversation will continue between other participants if
     * there are enough participants.
     * 
     * @throws RemoteException
     */
    @Override
    public void leave() throws RemoteException {
        if (isGroupChatAbandoned()) {
            throw new ServerApiUnsupportedOperationException(new StringBuilder(
                    "Cannot leave group chat with group chat Id : ").append(mChatId).toString());
        }
        mImService.scheduleImOperation(new Runnable() {
            public void run() {
                try {
                    final GroupChatSession session = mImService.getGroupChatSession(mChatId);
                    if (session == null || !ServerApiUtils.isImsConnected()) {
                        /*
                         * Quitting group chat that is inactive/ not available due to network drop
                         * should reject the next group chat invitation that is received
                         */
                        mPersistedStorage.setStateAndReasonCode(State.ABORTED,
                                ReasonCode.ABORTED_BY_USER);
                        mPersistedStorage.setRejectNextGroupChatNextInvitation();
                        mImService
                                .tryToMarkQueuedGroupChatMessagesAndGroupFileTransfersAsFailed(mChatId);
                        return;
                    }

                    if (sLogger.isActivated()) {
                        sLogger.info("Cancel session");
                    }

                    /* Terminate the session */
                    session.terminateSession(TerminationReason.TERMINATION_BY_USER);

                } catch (PayloadException e) {
                    sLogger.error(
                            new StringBuilder("Failed to terminate session with sessionId : ")
                                    .append(mChatId).toString(), e);
                } catch (NetworkException e) {
                    if (sLogger.isActivated()) {
                        sLogger.debug(e.getMessage());
                    }
                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error(
                            new StringBuilder("Failed to terminate session with sessionId : ")
                                    .append(mChatId).toString(), e);
                }
            }
        });
    }

    /**
     * Returns the participants. A participant is identified by its MSISDN in national or
     * international format, SIP address, SIP-URI or Tel-URI.
     * 
     * @return Participants
     * @throws RemoteException
     */
    @Override
    public Map<ContactId, Integer> getParticipants() throws RemoteException {
        try {
            Map<ContactId, Integer> apiParticipants = new HashMap<ContactId, Integer>();
            Map<ContactId, ParticipantStatus> participants;

            GroupChatSession session = mImService.getGroupChatSession(mChatId);
            if (session == null) {
                participants = mPersistedStorage.getParticipants();
                if (participants == null) {
                    throw new ServerApiPersistentStorageException(new StringBuilder(
                            "No participants found for chatId : ").append(mChatId).toString());
                }
            } else {
                participants = session.getParticipants();
            }

            for (Map.Entry<ContactId, ParticipantStatus> participant : participants.entrySet()) {
                apiParticipants.put(participant.getKey(), participant.getValue().toInt());
            }

            return apiParticipants;

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
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
    @Override
    public int getMaxParticipants() throws RemoteException {
        try {
            GroupChatSession session = mImService.getGroupChatSession(mChatId);
            if (session == null) {
                return mPersistedStorage.getMaxParticipants();
            }
            return session.getMaxParticipants();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
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
    @Override
    public boolean isAllowedToInviteParticipants() throws RemoteException {
        try {
            if (!isGroupChatCapableOfReceivingParticipantInvitations()) {
                return false;
            }
            if (!isAllowedToInviteAdditionalParticipants(1)) {
                if (sLogger.isActivated()) {
                    sLogger.debug(new StringBuilder(
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
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
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
    @Override
    public boolean isAllowedToInviteParticipant(ContactId participant) throws RemoteException {
        if (participant == null) {
            throw new ServerApiIllegalArgumentException("participant must not be null!");
        }
        if (!isAllowedToInviteParticipants()) {
            return false;
        }
        try {
            return isParticipantEligibleToBeInvited(participant)
                    && isParticipantCapableToBeInvited(participant);

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Invite additional participants to this group chat.
     * 
     * @param participants Set of participants
     * @throws RemoteException
     */
    @Override
    public void inviteParticipants(final List<ContactId> participants) throws RemoteException {
        if (participants == null || participants.isEmpty()) {
            throw new ServerApiIllegalArgumentException(
                    "participants list must not be null or empty!");
        }
        if (!isGroupChatCapableOfReceivingParticipantInvitations()) {
            throw new ServerApiUnsupportedOperationException(
                    "Not capable of receiving participant invitations!");
        }
        try {
            for (ContactId participant : participants) {
                if (!isParticipantEligibleToBeInvited(participant)) {
                    throw new ServerApiPermissionDeniedException(
                            "Participant not eligible to be invited!");
                }
            }

            mImService.scheduleImOperation(new Runnable() {
                public void run() {
                    GroupChatSession session = mImService.getGroupChatSession(mChatId);
                    try {
                        boolean mediaEstablished = (session != null && session.isMediaEstablished());
                        if (mediaEstablished) {
                            inviteParticipants(session, new HashSet<>(participants));
                            return;
                        }
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
                        if (session == null) {
                            if (isGroupChatRejoinable() && ServerApiUtils.isImsConnected()) {
                                rejoinGroupChat();
                            }
                        }

                    } catch (PayloadException | NetworkException e) {
                        if (session != null) {
                            session.handleError(new ChatError(ChatError.SEND_RESPONSE_FAILED, e));
                        } else {
                            sLogger.error(ExceptionUtil.getFullStackTrace(e));
                        }

                    } catch (RuntimeException e) {
                        /*
                         * Normally we are not allowed to catch runtime exceptions as these are
                         * genuine bugs which should be handled/fixed within the code. However the
                         * cases when we are executing operations on a thread unhandling such
                         * exceptions will eventually lead to exit the system and thus can bring the
                         * whole system down, which is not intended.
                         */
                        if (session != null) {
                            session.handleError(new ChatError(ChatError.SEND_RESPONSE_FAILED, e));
                        } else {
                            sLogger.error(ExceptionUtil.getFullStackTrace(e));
                        }
                    }
                }
            });

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Invite additional participants to this group chat inviteParticipants
     * 
     * @param session Group session
     * @param participants Set of participants
     * @throws NetworkException
     * @throws PayloadException
     */
    public void inviteParticipants(final GroupChatSession session, final Set<ContactId> participants)
            throws PayloadException, NetworkException {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Adding ")
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

        session.inviteParticipants(participants);
    }

    private void sendChatMessageWithinSession(final GroupChatSession session, final ChatMessage msg)
            throws NetworkException {
        session.sendChatMessage(msg);
    }

    /**
     * Set chat message status and timestamp
     * 
     * @param msg Chat message
     * @param status status of message
     */
    private void setChatMessageStatusAndTimestamp(ChatMessage msg, Status status) {
        String msgId = msg.getMessageId();
        synchronized (mLock) {
            if (mMessagingLog.setChatMessageStatusAndTimestamp(msgId, status,
                    Content.ReasonCode.UNSPECIFIED, msg.getTimestamp(), msg.getTimestampSent())) {
                mBroadcaster.broadcastMessageStatusChanged(mChatId, msg.getMimeType(),
                        msg.getMessageId(), Status.SENDING, Content.ReasonCode.UNSPECIFIED);
            }
        }
    }

    /**
     * Dequeue group chat message
     * 
     * @param msg Chat message
     * @throws NetworkException
     * @throws PayloadException
     * @throws SessionNotEstablishedException
     */
    public void dequeueGroupChatMessage(ChatMessage msg) throws PayloadException, NetworkException,
            SessionNotEstablishedException {
        final GroupChatSession session = mImService.getGroupChatSession(mChatId);
        if (session == null) {
            mImService.rejoinGroupChatAsPartOfSendOperation(mChatId);

        } else if (session.isMediaEstablished()) {
            setChatMessageStatusAndTimestamp(msg, Status.SENDING);
            sendChatMessageWithinSession(session, msg);

        } else if (session.isInitiatedByRemote()) {
            if (sLogger.isActivated()) {
                sLogger.debug(new StringBuilder("Group chat session with chatId '").append(mChatId)
                        .append("' is pending for acceptance, accept it.").toString());
            }
            session.acceptSession();

        } else {
            throw new SessionNotEstablishedException(new StringBuilder(
                    "The existing group chat session with chatId '").append(mChatId)
                    .append("' is not established right now!").toString());
        }
    }

    /**
     * Dequeue group file info
     * 
     * @param fileTransferId File transfer ID
     * @param fileInfo File information
     * @param displayedReportEnabled Display report enabled
     * @param deliveredReportEnabled Delivery report enabled
     * @param groupFileTransfer Group file implementation
     * @throws NetworkException
     * @throws PayloadException
     * @throws SessionNotEstablishedException
     */
    public void dequeueGroupFileInfo(String fileTransferId, String fileInfo,
            boolean displayedReportEnabled, boolean deliveredReportEnabled,
            GroupFileTransferImpl groupFileTransfer) throws PayloadException, NetworkException,
            SessionNotEstablishedException {
        GroupChatSession session = mImService.getGroupChatSession(mChatId);
        if (session == null) {
            mImService.rejoinGroupChatAsPartOfSendOperation(mChatId);

        } else if (session.isMediaEstablished()) {
            session.sendFileInfo(groupFileTransfer, fileTransferId, fileInfo,
                    displayedReportEnabled, deliveredReportEnabled);

        } else if (session.isInitiatedByRemote()) {
            if (sLogger.isActivated()) {
                sLogger.debug(new StringBuilder("Group chat session with chatId '").append(mChatId)
                        .append("' is pending for acceptance, accept it.").toString());
            }
            session.acceptSession();

        } else {
            throw new SessionNotEstablishedException(new StringBuilder(
                    "The existing group chat session with chatId '").append(mChatId)
                    .append("' is not established right now!").toString());
        }
    }

    /**
     * Returns true if it is possible to send messages in the group chat right now, else returns
     * false.
     * 
     * @return boolean
     * @throws RemoteException
     */
    @Override
    public boolean isAllowedToSendMessage() throws RemoteException {
        try {
            if (!mRcsSettings.isGroupChatActivated()) {
                if (sLogger.isActivated()) {
                    sLogger.debug(new StringBuilder(
                            "Cannot send message on group chat with group chat Id '")
                            .append(mChatId).append("' as group chat feature is not supported.")
                            .toString());
                }
                return false;
            }
            if (isGroupChatAbandoned()) {
                if (sLogger.isActivated()) {
                    sLogger.debug(new StringBuilder(
                            "Cannot send message on group chat with group chat Id '")
                            .append(mChatId).append("'").toString());
                }
                return false;
            }
            if (!mRcsSettings.getMyCapabilities().isImSessionSupported()) {
                if (sLogger.isActivated()) {
                    sLogger.debug(new StringBuilder(
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
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
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
    @Override
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
            final ChatMessage msg = ChatUtils.createTextMessage(null, text, timestamp, timestamp);
            ChatMessagePersistedStorageAccessor persistedStorage = new ChatMessagePersistedStorageAccessor(
                    mMessagingLog, msg.getMessageId(), msg.getRemoteContact(), text,
                    msg.getMimeType(), mChatId, Direction.OUTGOING);
            /* Always insert message with status QUEUED */
            addOutgoingGroupChatMessage(msg, Content.Status.QUEUED, Content.ReasonCode.UNSPECIFIED);
            if (!mChatService.isGroupChatActive(mChatId)) {
                /*
                 * Set inactive group chat as active as it now has a queued entry that has to be
                 * dequeued after rejoining to the group chat on regaining IMS connection.
                 */
                mChatService.setGroupChatStateAndReasonCode(mChatId, GroupChat.State.STARTED,
                        GroupChat.ReasonCode.UNSPECIFIED);
            }

            mImService.tryToDequeueGroupChatMessagesAndGroupFileTransfers(mChatId);
            return new ChatMessageImpl(persistedStorage);

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
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
            final ChatMessage geolocMsg = ChatUtils.createGeolocMessage(null, geoloc, timestamp,
                    timestamp);
            ChatMessagePersistedStorageAccessor persistedStorage = new ChatMessagePersistedStorageAccessor(
                    mMessagingLog, geolocMsg.getMessageId(), geolocMsg.getRemoteContact(),
                    geolocMsg.getContent(), geolocMsg.getMimeType(), mChatId, Direction.OUTGOING);
            addOutgoingGroupChatMessage(geolocMsg, Content.Status.QUEUED,
                    Content.ReasonCode.UNSPECIFIED);
            if (!mChatService.isGroupChatActive(mChatId)) {
                /*
                 * Set inactive group chat as active as it now has a queued entry that has to be
                 * dequeued after rejoining to the group chat on regaining IMS connection.
                 */
                mChatService.setGroupChatStateAndReasonCode(mChatId, GroupChat.State.STARTED,
                        GroupChat.ReasonCode.UNSPECIFIED);
            }

            mImService.tryToDequeueGroupChatMessagesAndGroupFileTransfers(mChatId);
            return new ChatMessageImpl(persistedStorage);

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Sends an is-composing event. The status is set to true when typing a message, else it is set
     * to false.
     * 
     * @param status Composing status
     * @throws RemoteException
     */
    @Override
    public void setComposingStatus(final boolean status) throws RemoteException {
        mImService.scheduleImOperation(new Runnable() {
            public void run() {
                try {
                    mImService.removeGroupChatComposingStatus(mChatId);
                    final GroupChatSession session = mImService.getGroupChatSession(mChatId);
                    if (session == null) {
                        if (sLogger.isActivated()) {
                            sLogger.debug("Unable to send composing event '" + status
                                    + "' since Group chat session found with ChatId '" + mChatId
                                    + "' does not exist for now.");
                        }
                        mImService.addGroupChatComposingStatus(mChatId, status);
                    } else if (session.getDialogPath().isSessionEstablished()) {
                        session.sendIsComposingStatus(status);
                    } else if (!session.isInitiatedByRemote()) {
                        if (sLogger.isActivated()) {
                            sLogger.debug("Unable to send composing event '" + status
                                    + "' since Group chat session found with ChatId '" + mChatId
                                    + "' is initiated locally.");
                        }
                        mImService.addGroupChatComposingStatus(mChatId, status);
                    } else {
                        ImSessionStartMode imSessionStartMode = mRcsSettings
                                .getImSessionStartMode();
                        switch (imSessionStartMode) {
                            case ON_OPENING:
                            case ON_COMPOSING:
                                if (sLogger.isActivated()) {
                                    sLogger.debug("Group chat session found with ChatId '"
                                            + mChatId
                                            + "' is not established and imSessionStartMode = "
                                            + imSessionStartMode
                                            + " so accepting it and sending composing event '"
                                            + status + "'");
                                }
                                session.acceptSession();
                                session.sendIsComposingStatus(status);
                                break;
                            default:
                                if (sLogger.isActivated()) {
                                    sLogger.debug("Group chat session found with ChatId '"
                                            + mChatId
                                            + "' is not established and imSessionStartMode = "
                                            + imSessionStartMode
                                            + " so can't accept it and sending composing event '"
                                            + status + "' yet.");
                                }
                                mImService.addGroupChatComposingStatus(mChatId, status);
                                break;
                        }
                    }
                } catch (NetworkException e) {
                    if (sLogger.isActivated()) {
                        sLogger.debug(e.getMessage());
                    }
                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error(new StringBuilder(
                            "Failed to send composing status in group chat : ").append(mChatId)
                            .toString(), e);
                }
            }
        });
    }

    /**
     * Rejoins an existing group chat from its unique chat ID
     * 
     * @throws NetworkException
     * @throws PayloadException
     */
    public void rejoinGroupChat() throws PayloadException, NetworkException {
        if (sLogger.isActivated()) {
            sLogger.info("Rejoin group chat session related to the conversation " + mChatId);
        }
        ServerApiUtils.testIms();
        RejoinGroupChatSession session = mImService.rejoinGroupChatSession(mChatId);
        session.addListener(this);
        mChatService.addGroupChat(this);
        session.startSession();
    }

    /**
     * Restarts a previous group chat from its unique chat ID
     * 
     * @throws NetworkException
     * @throws PayloadException
     */
    public void restartGroupChat() throws PayloadException, NetworkException {
        if (sLogger.isActivated()) {
            sLogger.info("Restart group chat session related to the conversation " + mChatId);
        }
        ServerApiUtils.testIms();
        RestartGroupChatSession session = mImService.restartGroupChatSession(mChatId);
        session.addListener(this);
        mChatService.addGroupChat(this);
        session.startSession();
    }

    @Override
    public void openChat() throws RemoteException {
        mImService.scheduleImOperation(new Runnable() {
            public void run() {
                if (sLogger.isActivated()) {
                    sLogger.info("Open a group chat session with chatId " + mChatId);
                }
                try {
                    final GroupChatSession session = mImService.getGroupChatSession(mChatId);
                    if (session == null) {
                        /*
                         * If there is no session ongoing right now then we do not need to open
                         * anything right now so we just return here. A sending of a new message on
                         * this group chat will anyway result in a rejoin attempt if this group chat
                         * has not been left by choice so we do not need to do anything more here
                         * for now.
                         */
                        return;
                    }
                    if (session.getDialogPath().isSessionEstablished()) {
                        return;
                    }
                    ImSessionStartMode imSessionStartMode = mRcsSettings.getImSessionStartMode();
                    if (!session.isInitiatedByRemote()) {
                        /*
                         * This method needs to accept pending invitation if IM_SESSION_START_MODE
                         * is 0, which is not applicable if session is remote originated so we
                         * return here.
                         */
                        return;
                    }
                    if (ImSessionStartMode.ON_OPENING == imSessionStartMode) {
                        if (sLogger.isActivated()) {
                            sLogger.debug("Core chat session is pending: auto accept it, as IM_SESSION_START mode = 0");
                        }
                        session.acceptSession();
                    }
                } catch (ServerApiBaseException e) {
                    if (!e.shouldNotBeLogged()) {
                        sLogger.error(ExceptionUtil.getFullStackTrace(e));
                    }
                    throw e;

                } catch (Exception e) {
                    sLogger.error(ExceptionUtil.getFullStackTrace(e));
                    throw new ServerApiGenericException(e);
                }
            }
        });
    }

    /**
     * Try to restart group chat session on failure of restart
     * 
     * @throws NetworkException
     * @throws PayloadException
     */
    private void handleGroupChatRejoinAsPartOfSendOperationFailed() throws PayloadException,
            NetworkException {
        restartGroupChat();
    }

    /**
     * @param enable Enable rejoin as part of send operation
     */
    public void setRejoinedAsPartOfSendOperation(boolean enable) {
        mGroupChatRejoinedAsPartOfSendOperation = enable;
    }

    /*------------------------------- SESSION EVENTS ----------------------------------*/

    @Override
    public void onSessionStarted(ContactId contact) {
        boolean loggerActivated = sLogger.isActivated();
        if (loggerActivated) {
            sLogger.info("Session started");
        }
        setRejoinedAsPartOfSendOperation(false);
        synchronized (mLock) {
            GroupChatSession session = mImService.getGroupChatSession(mChatId);
            Boolean composingStatus = mImService.getGroupChatComposingStatus(mChatId);
            if (composingStatus != null) {
                if (loggerActivated) {
                    sLogger.debug("Sending isComposing command with status :"
                            .concat(composingStatus.toString()));
                }
                try {
                    session.sendIsComposingStatus(composingStatus);
                    mImService.removeGroupChatComposingStatus(mChatId);
                } catch (NetworkException e) {
                    /*
                     * Nothing to be handled here as we are not able to send composing status for
                     * now, should try later and hence we don't remove it from the map.
                     */
                    if (loggerActivated) {
                        sLogger.debug(e.getMessage());
                    }
                }
            }
            boolean updateStateToStarted = State.STARTED != mPersistedStorage.getState();
            mPersistedStorage.setRejoinId(session.getImSessionIdentity(), updateStateToStarted);
            if (updateStateToStarted) {
                mChatService.broadcastGroupChatStateChange(mChatId, State.STARTED,
                        ReasonCode.UNSPECIFIED);
            }
        }
        mImService.tryToInviteQueuedGroupChatParticipantInvitations(mChatId);
        mImService.tryToDequeueGroupChatMessagesAndGroupFileTransfers(mChatId);
    }

    @Override
    public void onSessionAborted(ContactId contact, TerminationReason reason) {
        GroupChatSession session = mImService.getGroupChatSession(mChatId);
        if (session != null && session.isPendingForRemoval()) {
            /*
             * If there is an ongoing group chat session with same chatId, this session has to be
             * silently aborted so after aborting the session we make sure to not call the rest of
             * this method that would otherwise abort the "current" session also and the GroupChat
             * as a whole which is of course not the intention here
             */
            if (sLogger.isActivated()) {
                sLogger.info(new StringBuilder("Session marked pending for removal status ")
                        .append(State.ABORTED).append(" terminationReason ").append(reason)
                        .toString());
            }
            return;
        }
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("Session status ").append(State.ABORTED)
                    .append(" terminationReason ").append(reason).toString());
        }
        setRejoinedAsPartOfSendOperation(false);
        synchronized (mLock) {
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
                    mImService
                            .tryToMarkQueuedGroupChatMessagesAndGroupFileTransfersAsFailed(mChatId);
                    break;
                case TERMINATION_BY_REMOTE:
                    setStateAndReasonCode(State.ABORTED, ReasonCode.ABORTED_BY_REMOTE);
                    mImService
                            .tryToMarkQueuedGroupChatMessagesAndGroupFileTransfersAsFailed(mChatId);
                    break;
                case TERMINATION_BY_TIMEOUT:
                case TERMINATION_BY_INACTIVITY:
                    setStateAndReasonCode(State.ABORTED, ReasonCode.ABORTED_BY_INACTIVITY);
                    break;
                default:
                    throw new IllegalArgumentException(
                            new StringBuilder(
                                    "Unknown reason in GroupChatImpl.handleSessionAborted; terminationReason=")
                                    .append(reason).append("!").toString());
            }
        }
        /*
         * Try to dequeue all one-one chat messages and file transfers as a chat session is torn
         * down now.
         */
        mImService.tryToDequeueAllOneToOneChatMessagesAndOneToOneFileTransfers();
    }

    @Override
    public void onMessageReceived(ChatMessage msg, boolean imdnDisplayedRequested,
            boolean deliverySuccess) {
        String msgId = null;
        ContactId remote = null;
        try {
            msgId = msg.getMessageId();
            remote = msg.getRemoteContact();
            if (sLogger.isActivated()) {
                sLogger.info(new StringBuilder("New IM with Id '").append(msgId)
                        .append("' received from ").append(remote).toString());
            }
            synchronized (mLock) {
                if (deliverySuccess) {
                    mPersistedStorage.addIncomingGroupChatMessage(msg, imdnDisplayedRequested);
                    if (remote != null) {
                        mContactManager.mergeContactCapabilities(remote, new CapabilitiesBuilder()
                                .setImSession(true).setTimestampOfLastResponse(msg.getTimestamp())
                                .build(), RcsStatus.RCS_CAPABLE, RegistrationState.ONLINE,
                                msg.getDisplayName());
                    }
                } else {
                    mPersistedStorage.addGroupChatFailedDeliveryMessage(msg);
                }
                mBroadcaster.broadcastMessageReceived(msg.getMimeType(), msgId);
            }

        } catch (ContactManagerException | FileAccessException e) {
            sLogger.error(new StringBuilder("Failed to handle new IM with Id '").append(msgId)
                    .append("' received from ").append(remote).toString(), e);

        } catch (RuntimeException e) {
            /*
             * Normally we are not allowed to catch runtime exceptions as these are genuine bugs
             * which should be handled/fixed within the code. However the cases when we are
             * executing operations on a thread unhandling such exceptions will eventually lead to
             * exit the system and thus can bring the whole system down, which is not intended.
             */
            sLogger.error(new StringBuilder("Failed to handle new IM with Id '").append(msgId)
                    .append("' received from ").append(remote).toString(), e);
        }
    }

    @Override
    public void onImError(ChatError error) {
        try {
            GroupChatSession session = mImService.getGroupChatSession(mChatId);
            int chatErrorCode = error.getErrorCode();
            if (session != null && session.isPendingForRemoval()) {
                /*
                 * If there is an ongoing group chat session with same chatId, this session has to
                 * be silently aborted so after aborting the session we make sure to not call the
                 * rest of this method that would otherwise abort the "current" session also and the
                 * GroupChat as a whole which is of course not the intention here
                 */
                if (sLogger.isActivated()) {
                    sLogger.info(new StringBuilder("Session marked pending for removal - Error ")
                            .append(chatErrorCode).toString());
                }
                return;
            }
            if (sLogger.isActivated()) {
                sLogger.info(new StringBuilder("IM error ").append(chatErrorCode).toString());
            }
            synchronized (mLock) {
                mChatService.removeGroupChat(mChatId);
                int chatError = error.getErrorCode();
                switch (chatError) {
                    case ChatError.SESSION_INITIATION_CANCELLED:
                        /* Intentional fall through */
                    case ChatError.SESSION_INITIATION_DECLINED:
                        setStateAndReasonCode(State.REJECTED, ReasonCode.REJECTED_BY_REMOTE);
                        mImService
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
                        mImService
                                .tryToMarkQueuedGroupChatMessagesAndGroupFileTransfersAsFailed(mChatId);
                        break;
                    /*
                     * For cases where rejoin has failed or send response failed due to no ACK/200
                     * OK response, we should not change Chat state.
                     */
                    /* Intentional fall through */
                    case ChatError.SESSION_REJOIN_FAILED:
                        /* Intentional fall through */
                    case ChatError.SEND_RESPONSE_FAILED:
                        break;
                    /*
                     * This error is caused because of a network drop so the group chat is not set
                     * to ABORTED state in this case as it will be auto-rejoined when network
                     * connection is regained
                     */
                    case ChatError.MEDIA_SESSION_FAILED:
                    case ChatError.MEDIA_SESSION_BROKEN:
                        break;
                    default:
                        throw new IllegalArgumentException(new StringBuilder(
                                "Unknown reason; chatError=").append(chatError).append("!")
                                .toString());
                }
            }
            setRejoinedAsPartOfSendOperation(false);
            /*
             * Try to dequeue all one-one chat messages and file transfers as a chat session is torn
             * down now.
             */
            mImService.tryToDequeueAllOneToOneChatMessagesAndOneToOneFileTransfers();
        } catch (PayloadException e) {
            sLogger.error(new StringBuilder("Failed to handle error").append(error).append("!")
                    .toString(), e);
        } catch (NetworkException e) {
            if (sLogger.isActivated()) {
                sLogger.debug(e.getMessage());
            }
        } catch (RuntimeException e) {
            /*
             * Normally we are not allowed to catch runtime exceptions as these are genuine bugs
             * which should be handled/fixed within the code. However the cases when we are
             * executing operations on a thread unhandling such exceptions will eventually lead to
             * exit the system and thus can bring the whole system down, which is not intended.
             */
            sLogger.error(new StringBuilder("Failed to handle error").append(error).append("!")
                    .toString(), e);
        }
    }

    @Override
    public void onIsComposingEventReceived(ContactId contact, boolean status) {
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder().append(contact).append(" is composing status set to ")
                    .append(status).toString());
        }
        synchronized (mLock) {
            // Notify event listeners
            mBroadcaster.broadcastComposingEvent(mChatId, contact, status);
        }
    }

    @Override
    public void onMessageFailedSend(String msgId, String mimeType) {
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("Message sending failed; msgId=").append(msgId)
                    .append("mimeType=").append(mimeType).append(".").toString());
        }

        synchronized (mLock) {
            if (mPersistedStorage.setMessageStatusAndReasonCode(msgId, Status.FAILED,
                    Content.ReasonCode.FAILED_SEND)) {
                mBroadcaster.broadcastMessageStatusChanged(getChatId(), mimeType, msgId,
                        Status.FAILED, Content.ReasonCode.FAILED_SEND);
            }
        }
    }

    @Override
    public void onMessageSent(String msgId, String mimeType) {
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("Text message sent; msgId=").append(msgId)
                    .append("mimeType=").append(mimeType).append(".").toString());
        }

        synchronized (mLock) {
            if (mPersistedStorage.setMessageStatusAndReasonCode(msgId, Status.SENT,
                    Content.ReasonCode.UNSPECIFIED)) {
                mBroadcaster.broadcastMessageStatusChanged(getChatId(), mimeType, msgId,
                        Status.SENT, Content.ReasonCode.UNSPECIFIED);
            }
        }
    }

    @Override
    public void onConferenceEventReceived(ContactId contact, ParticipantStatus status,
            long timestamp) {
        if (sLogger.isActivated()) {
            sLogger.info("New conference event " + status.toString() + " for " + contact);
        }
        synchronized (mLock) {
            if (ParticipantStatus.CONNECTED.equals(status)) {
                mPersistedStorage.addGroupChatEvent(contact, GroupChatEvent.Status.JOINED,
                        timestamp);

            } else if (ParticipantStatus.DEPARTED.equals(status)) {
                mPersistedStorage.addGroupChatEvent(contact, GroupChatEvent.Status.DEPARTED,
                        timestamp);
            }
        }
    }

    @Override
    public void onMessageDeliveryStatusReceived(ContactId contact, ImdnDocument imdn) {
        String status = imdn.getStatus();
        String msgId = imdn.getMsgId();
        long timestamp = imdn.getDateTime();
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("Handling message delivery status; contact=")
                    .append(contact).append(", msgId=").append(msgId).append(", status=")
                    .append(status).append(", notificationType=")
                    .append(imdn.getNotificationType()).toString());
        }
        switch (status) {
            case ImdnDocument.DELIVERY_STATUS_DELIVERED:
                onMessageDeliveryStatusDelivered(contact, msgId, timestamp);
                break;
            case ImdnDocument.DELIVERY_STATUS_DISPLAYED:
                onMessageDeliveryStatusDisplayed(contact, msgId, timestamp);
                break;
            case ImdnDocument.DELIVERY_STATUS_ERROR:
            case ImdnDocument.DELIVERY_STATUS_FAILED:
            case ImdnDocument.DELIVERY_STATUS_FORBIDDEN:
                Content.ReasonCode reasonCode = imdnToMessageFailedReasonCode(imdn);
                onMessageDeliveryStatusFailed(contact, msgId, reasonCode);
                break;
        }
    }

    @Override
    public void onDeliveryStatusReceived(String contributionId, ContactId contact, ImdnDocument imdn) {
        String msgId = imdn.getMsgId();

        // TODO: Potential race condition, after we've checked that the message is persisted
        // it may be removed before the handle method executes.
        if (mMessagingLog.isMessagePersisted(msgId)) {
            onMessageDeliveryStatusReceived(contact, imdn);
            return;
        }
        if (mMessagingLog.isFileTransfer(msgId)) {
            mImService.receiveGroupFileDeliveryStatus(contributionId, contact, imdn);
            return;
        }
        sLogger.error(new StringBuilder(
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
    public void onAddParticipantFailed(ContactId contact, String reason) {
        if (sLogger.isActivated()) {
            sLogger.info("Add participant request has failed " + reason);
        }
        synchronized (mLock) {
            mBroadcaster.broadcastParticipantStatusChanged(mChatId, contact,
                    ParticipantStatus.FAILED);
        }
    }

    @Override
    public void onSessionAccepting(ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.info("Accepting group chat session");
        }
        synchronized (mLock) {
            setStateAndReasonCode(State.ACCEPTING, ReasonCode.UNSPECIFIED);
        }
    }

    @Override
    public void onSessionRejected(ContactId contact, TerminationReason reason) {
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
                mImService.tryToMarkQueuedGroupChatMessagesAndGroupFileTransfersAsFailed(mChatId);
                break;
            default:
                throw new IllegalArgumentException(new StringBuilder(
                        "Unknown reason RejectedReason=").append(reason).append("!").toString());
        }
    }

    @Override
    public void onSessionInvited(ContactId contact, String subject,
            Map<ContactId, ParticipantStatus> participants, long timestamp) {
        if (sLogger.isActivated()) {
            sLogger.info("Invited to group chat session");
        }
        synchronized (mLock) {
            if (mMessagingLog.isGroupChatPersisted(mChatId)
                    && mPersistedStorage.setParticipantsStateAndReasonCode(participants,
                            State.INVITED, ReasonCode.UNSPECIFIED)) {
                mBroadcaster.broadcastInvitation(mChatId);
            } else {
                mPersistedStorage.addGroupChat(contact, subject, participants, State.INVITED,
                        ReasonCode.UNSPECIFIED, Direction.INCOMING, timestamp);
                mBroadcaster.broadcastInvitation(mChatId);
            }
        }
    }

    @Override
    public void onSessionAutoAccepted(ContactId contact, String subject,
            Map<ContactId, ParticipantStatus> participants, long timestamp) {
        if (sLogger.isActivated()) {
            sLogger.info("Session auto accepted");
        }
        synchronized (mLock) {
            if (mMessagingLog.isGroupChatPersisted(mChatId)
                    && mPersistedStorage.setParticipantsStateAndReasonCode(participants,
                            State.ACCEPTING, ReasonCode.UNSPECIFIED)) {
                mBroadcaster.broadcastInvitation(mChatId);
            } else {
                mPersistedStorage.addGroupChat(contact, subject, participants, State.ACCEPTING,
                        ReasonCode.UNSPECIFIED, Direction.INCOMING, timestamp);
                mBroadcaster.broadcastInvitation(mChatId);
            }
        }
    }

    @Override
    public void onParticipantsUpdated(Map<ContactId, ParticipantStatus> updatedParticipants,
            Map<ContactId, ParticipantStatus> allParticipants) {
        synchronized (mLock) {
            if (!mMessagingLog.setGroupChatParticipants(mChatId, allParticipants)) {
                return;
            }
        }
        for (Map.Entry<ContactId, ParticipantStatus> updatedParticipant : updatedParticipants
                .entrySet()) {
            ContactId contact = updatedParticipant.getKey();
            ParticipantStatus status = updatedParticipant.getValue();

            if (sLogger.isActivated()) {
                sLogger.info("ParticipantUpdate for: " + contact + " status: " + status);
            }

            mBroadcaster.broadcastParticipantStatusChanged(mChatId, contact, status);
        }
    }

    @Override
    public void onChatMessageDisplayReportSent(String msgId) {
        synchronized (mLock) {
            if (mMessagingLog.setChatMessageStatusAndReasonCode(msgId, Content.Status.RECEIVED,
                    Content.ReasonCode.UNSPECIFIED)) {
                String apiMimeType = mMessagingLog.getMessageMimeType(msgId);
                mBroadcaster.broadcastMessageStatusChanged(mChatId, apiMimeType, msgId,
                        Content.Status.RECEIVED, Content.ReasonCode.UNSPECIFIED);
            }
        }
    }

    @Override
    public void onDeliveryReportSendViaMsrpFailure(String msgId, String chatId,
            TypeMsrpChunk typeMsrpChunk) {
        ContactId remote = mHistoryLog.getRemoteContact(msgId);
        if (TypeMsrpChunk.MessageDeliveredReport.equals(typeMsrpChunk)) {
            if (sLogger.isActivated()) {
                sLogger.debug(new StringBuilder(
                        "Failed to send delivered message via MSRP, so try to send via SIP message to ")
                        .append(remote).append("(msgId = ").append(msgId).toString());
            }
            mImService.getImdnManager().sendMessageDeliveryStatus(chatId, remote, msgId,
                    ImdnDocument.DELIVERY_STATUS_DELIVERED, System.currentTimeMillis());
        } else if (TypeMsrpChunk.MessageDisplayedReport.equals(typeMsrpChunk)) {
            if (sLogger.isActivated()) {
                sLogger.debug(new StringBuilder(
                        "Failed to send displayed message via MSRP, so try to send via SIP message to ")
                        .append(remote).append("(msgId = ").append(msgId).toString());
            }
            mImService.getImdnManager().sendMessageDeliveryStatus(chatId, remote, msgId,
                    ImdnDocument.DELIVERY_STATUS_DISPLAYED, System.currentTimeMillis());
        }
    }

}
