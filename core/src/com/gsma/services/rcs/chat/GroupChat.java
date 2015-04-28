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

package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsIllegalArgumentException;
import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.RcsPersistentStorageException;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsUnsupportedOperationException;
import com.gsma.services.rcs.contact.ContactId;

import android.util.SparseArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Group chat
 * 
 * @author Jean-Marc AUFFRET
 */
public class GroupChat {
    /**
     * Group chat state
     */
    public enum State {

        /**
         * Chat invitation received
         */
        INVITED(0),

        /**
         * Chat invitation sent
         */
        INITIATING(1),

        /**
         * Chat is started
         */
        STARTED(2),

        /**
         * Chat has been aborted
         */
        ABORTED(3),

        /**
         * Chat has failed
         */
        FAILED(4),

        /**
         * Chat has been accepted and is in the process of becoming started.
         */
        ACCEPTING(5),

        /**
         * Chat invitation was rejected.
         */
        REJECTED(6);

        private final int mValue;

        private static SparseArray<State> mValueToEnum = new SparseArray<State>();
        static {
            for (State state : State.values()) {
                mValueToEnum.put(state.toInt(), state);
            }
        }

        private State(int value) {
            mValue = value;
        }

        /**
         * Gets integer value associated to State instance
         * 
         * @return value
         */
        public final int toInt() {
            return mValue;
        }

        /**
         * Returns a State instance for the specified integer value.
         * 
         * @param value
         * @return instance
         */
        public final static State valueOf(int value) {
            State state = mValueToEnum.get(value);
            if (state != null) {
                return state;
            }
            throw new IllegalArgumentException(new StringBuilder("No enum const class ")
                    .append(State.class.getName()).append(".").append(value).append("!").toString());
        }
    }

    /**
     * Group chat participant status
     */
    public enum ParticipantStatus {
        /**
         * Invite can not be sent, instead it has been queued
         */
        INVITE_QUEUED(0),
        /**
         * Participant is about to be invited
         */
        INVITING(1),
        /**
         * Participant is invited
         */
        INVITED(2),
        /**
         * Participant is connected
         */
        CONNECTED(3),
        /**
         * Participant disconnected
         */
        DISCONNECTED(4),
        /**
         * Participant has departed
         */
        DEPARTED(5),
        /**
         * Participant status is failed
         */
        FAILED(6),
        /**
         * Participant declined invitation
         */
        DECLINED(7),
        /**
         * Participant invitation has timed-out
         */
        TIMEOUT(8);

        private final int mValue;

        private static SparseArray<ParticipantStatus> mValueToEnum = new SparseArray<ParticipantStatus>();
        static {
            for (ParticipantStatus status : ParticipantStatus.values()) {
                mValueToEnum.put(status.toInt(), status);
            }
        }

        private ParticipantStatus(int value) {
            mValue = value;
        }

        public final int toInt() {
            return mValue;
        }

        public final static ParticipantStatus valueOf(int value) {
            ParticipantStatus status = mValueToEnum.get(value);
            if (status != null) {
                return status;
            }
            throw new IllegalArgumentException(new StringBuilder("No enum const class ")
                    .append(ParticipantStatus.class.getName()).append(".").append(value)
                    .append("!").toString());
        }
    }

    /**
     * Group chat state reason code
     */
    public enum ReasonCode {

        /**
         * No specific reason code specified.
         */
        UNSPECIFIED(0),

        /**
         * Group chat is aborted by local user.
         */
        ABORTED_BY_USER(1),

        /**
         * Group chat is aborted by remote user.
         */
        ABORTED_BY_REMOTE(2),

        /**
         * Group chat is aborted by inactivity.
         */
        ABORTED_BY_INACTIVITY(3),

        /**
         * Group chat is rejected because already taken by the secondary device.
         */
        REJECTED_BY_SECONDARY_DEVICE(4),

        /**
         * Group chat invitation was rejected as it was detected as spam.
         */
        REJECTED_SPAM(5),

        /**
         * Group chat invitation was rejected due to max number of chats open already.
         */
        REJECTED_MAX_CHATS(6),

        /**
         * Group chat invitation was rejected by remote.
         */
        REJECTED_BY_REMOTE(7),

        /**
         * Group chat invitation was rejected by timeout.
         */
        REJECTED_BY_TIMEOUT(8),

        /**
         * Group chat invitation was rejected by system.
         */
        REJECTED_BY_SYSTEM(9),

        /**
         * Group chat initiation failed.
         */
        FAILED_INITIATION(10);

        private final int mValue;

        private static SparseArray<ReasonCode> mValueToEnum = new SparseArray<ReasonCode>();
        static {
            for (ReasonCode reasonCode : ReasonCode.values()) {
                mValueToEnum.put(reasonCode.toInt(), reasonCode);
            }
        }

        private ReasonCode(int value) {
            mValue = value;
        }

        /**
         * Gets integer value associated to ReasonCode instance
         * 
         * @return value
         */
        public final int toInt() {
            return mValue;
        }

        /**
         * Returns a ReasonCode instance for the specified integer value.
         * 
         * @param value
         * @return instance
         */
        public final static ReasonCode valueOf(int value) {
            ReasonCode reasonCode = mValueToEnum.get(value);
            if (reasonCode != null) {
                return reasonCode;
            }
            throw new IllegalArgumentException(new StringBuilder("No enum const class ")
                    .append(ReasonCode.class.getName()).append(".").append(value).append("!")
                    .toString());
        }
    }

    /**
     * Group chat interface
     */
    private final IGroupChat mGroupChatInf;

    /**
     * Constructor
     * 
     * @param chatIntf Group chat interface
     */
    /* package private */GroupChat(IGroupChat chatIntf) {
        mGroupChatInf = chatIntf;
    }

    /**
     * Returns the chat ID
     * 
     * @return Chat ID
     * @throws RcsServiceException
     */
    public String getChatId() throws RcsServiceException {
        try {
            return mGroupChatInf.getChatId();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the direction of the group chat
     * 
     * @return Direction
     * @see Direction
     * @throws RcsServiceException
     */
    public Direction getDirection() throws RcsServiceException {
        try {
            return Direction.valueOf(mGroupChatInf.getDirection());

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the state of the group chat
     * 
     * @return State
     * @see State
     * @throws RcsServiceException
     */
    public State getState() throws RcsServiceException {
        try {
            return State.valueOf(mGroupChatInf.getState());

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the reason code of the state of the group chat
     * 
     * @return ReasonCode
     * @see ReasonCode
     * @throws RcsServiceException
     */
    public ReasonCode getReasonCode() throws RcsServiceException {
        try {
            return ReasonCode.valueOf(mGroupChatInf.getReasonCode());

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the remote contact
     * 
     * @return Contact
     * @throws RcsServiceException
     */
    public ContactId getRemoteContact() throws RcsServiceException {
        try {
            return mGroupChatInf.getRemoteContact();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the subject of the group chat
     * 
     * @return Subject
     * @throws RcsServiceException
     */
    public String getSubject() throws RcsServiceException {
        try {
            return mGroupChatInf.getSubject();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the list of connected participants. A participant is identified by its MSISDN in
     * national or international format, SIP address, SIP-URI or Tel-URI.
     * 
     * @return List of participants
     * @throws RcsServiceException
     */
    /*
     * Unchecked cast must be suppressed since AIDL provides a raw Map type that must be cast.
     */
    @SuppressWarnings("unchecked")
    public Map<ContactId, ParticipantStatus> getParticipants() throws RcsServiceException {
        try {
            Map<ContactId, Integer> apiParticipants = mGroupChatInf.getParticipants();
            Map<ContactId, ParticipantStatus> participants = new HashMap<ContactId, ParticipantStatus>();

            for (Map.Entry<ContactId, Integer> apiParticipant : apiParticipants.entrySet()) {
                participants.put(apiParticipant.getKey(),
                        ParticipantStatus.valueOf(apiParticipant.getValue()));
            }

            return participants;

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the local timestamp of when the group chat invitation was initiated for outgoing
     * group chats or the local timestamp of when the group chat invitation was received for
     * incoming group chat invitations.
     * 
     * @return timestamp
     * @throws RcsServiceException
     */
    public long getTimestamp() throws RcsServiceException {
        try {
            return mGroupChatInf.getTimestamp();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns true if it is possible to send messages in the group chat right now, else returns
     * false.
     * 
     * @return boolean
     * @throws RcsServiceException
     */
    public boolean isAllowedToSendMessage() throws RcsServiceException {
        try {
            return mGroupChatInf.isAllowedToSendMessage();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Sends a text message to the group
     * 
     * @param text Message
     * @return ChatMessage
     * @throws RcsServiceException
     */
    public ChatMessage sendMessage(String text) throws RcsServiceException {
        try {
            return new ChatMessage(mGroupChatInf.sendMessage(text));

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPermissionDeniedException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Sends a geoloc message
     * 
     * @param geoloc Geoloc info
     * @return ChatMessage
     * @throws RcsServiceException
     */
    public ChatMessage sendMessage(Geoloc geoloc) throws RcsServiceException {
        try {
            return new ChatMessage(mGroupChatInf.sendMessage2(geoloc));

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPermissionDeniedException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Called when is composing a chat message
     * 
     * @param enabled It should be set to true if user is composing and set to false when the
     *            client application is leaving the chat UI
     * @throws RcsServiceException
     */
    public void onComposing(final boolean enabled) throws RcsServiceException {
        try {
            mGroupChatInf.onComposing(enabled);
        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns true if it is possible to invite additional participants to the group chat right now,
     * else returns false.
     * 
     * @return boolean
     * @throws RcsServiceException
     */
    public boolean isAllowedToInviteParticipants() throws RcsServiceException {
        try {
            return mGroupChatInf.isAllowedToInviteParticipants();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns true if it is possible to invite the specified participant to the group chat right
     * now, else returns false.
     * 
     * @param participant
     * @return boolean
     * @throws RcsServiceException
     */
    public boolean isAllowedToInviteParticipant(ContactId participant) throws RcsServiceException {
        try {
            return mGroupChatInf.isAllowedToInviteParticipant(participant);

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Invite additional participants to this group chat.
     * 
     * @param participants List of participants
     * @throws RcsServiceException
     */
    public void inviteParticipants(Set<ContactId> participants) throws RcsServiceException {
        try {
            mGroupChatInf.inviteParticipants(new ArrayList<ContactId>(participants));
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsUnsupportedOperationException.assertException(e);
            RcsPermissionDeniedException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the max number of participants in the group chat. This limit is read during the
     * conference event subscription and overrides the provisioning parameter.
     * 
     * @return Number
     * @throws RcsServiceException
     */
    public int getMaxParticipants() throws RcsServiceException {
        try {
            return mGroupChatInf.getMaxParticipants();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns true if it is possible to leave this group chat.
     * 
     * @return boolean
     * @throws RcsServiceException
     */
    public boolean isAllowedToLeave() throws RcsServiceException {
        try {
            return mGroupChatInf.isAllowedToLeave();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Leaves a group chat willingly and permanently. The group chat will continue between other
     * participants if there are enough participants.
     * 
     * @throws RcsServiceException
     */
    public void leave() throws RcsServiceException {
        try {
            mGroupChatInf.leave();
        } catch (Exception e) {
            RcsUnsupportedOperationException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * open the chat conversation.<br>
     * Note: if it is an incoming pending chat session and the parameter IM SESSION START is 0 then
     * the session is accepted now.
     * 
     * @throws RcsServiceException
     */
    public void openChat() throws RcsServiceException {
        try {
            mGroupChatInf.openChat();
        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }
}
