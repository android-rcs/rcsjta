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

package com.gsma.rcs.core;

import com.gsma.rcs.core.content.AudioContent;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.content.VideoContent;
import com.gsma.rcs.core.ims.ImsError;
import com.gsma.rcs.core.ims.service.capability.Capabilities;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.OneToOneChatSession;
import com.gsma.rcs.core.ims.service.im.chat.TerminatingAdhocGroupChatSession;
import com.gsma.rcs.core.ims.service.im.chat.TerminatingOneToOneChatSession;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.core.ims.service.im.chat.standfw.TerminatingStoreAndForwardOneToOneChatMessageSession;
import com.gsma.rcs.core.ims.service.im.chat.standfw.TerminatingStoreAndForwardOneToOneChatNotificationSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.gsma.rcs.core.ims.service.ipcall.IPCallSession;
import com.gsma.rcs.core.ims.service.presence.pidf.PidfDocument;
import com.gsma.rcs.core.ims.service.richcall.geoloc.GeolocTransferSession;
import com.gsma.rcs.core.ims.service.richcall.image.ImageTransferSession;
import com.gsma.rcs.core.ims.service.richcall.video.VideoStreamingSession;
import com.gsma.rcs.core.ims.service.sip.messaging.GenericSipMsrpSession;
import com.gsma.rcs.core.ims.service.sip.streaming.GenericSipRtpSession;
import com.gsma.rcs.service.ipcalldraft.IPCall;
import com.gsma.services.rcs.RcsServiceRegistration;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharing;
import com.gsma.services.rcs.sharing.image.ImageSharing;
import com.gsma.services.rcs.sharing.video.VideoSharing;

import android.content.Intent;

import java.util.Map;

/**
 * Observer of core events
 * 
 * @author Jean-Marc AUFFRET
 */
public interface CoreListener {
    /**
     * Core layer has been started
     */
    public void handleCoreLayerStarted();

    /**
     * Core layer has been stopped
     */
    public void handleCoreLayerStopped();

    /**
     * Registered to IMS
     */
    public void handleRegistrationSuccessful();

    /**
     * IMS registration has failed
     * 
     * @param error Error
     */
    public void handleRegistrationFailed(ImsError error);

    /**
     * Unregistered from IMS
     * 
     * @param reason reason code for registration termmination
     */
    public void handleRegistrationTerminated(RcsServiceRegistration.ReasonCode reason);

    /**
     * A new presence sharing notification has been received
     * 
     * @param contact Contact identifier
     * @param status Status
     * @param reason Reason
     */
    public void handlePresenceSharingNotification(ContactId contact, String status, String reason);

    /**
     * A new presence info notification has been received
     * 
     * @param contact Contact identifier
     * @param presence Presence info document
     */
    public void handlePresenceInfoNotification(ContactId contact, PidfDocument presence);

    /**
     * Capabilities update notification has been received
     * 
     * @param contact Contact identifier
     * @param capabilities Capabilities
     */
    public void handleCapabilitiesNotification(ContactId contact, Capabilities capabilities);

    /**
     * A new presence sharing invitation has been received
     * 
     * @param contact Contact identifier
     */
    public void handlePresenceSharingInvitation(ContactId contact);

    /**
     * A new IP call invitation has been received
     * 
     * @param session IP call session
     */
    public void handleIPCallInvitation(IPCallSession session);

    /**
     * A new content sharing transfer invitation has been received
     * 
     * @param session CSh session
     */
    public void handleContentSharingTransferInvitation(ImageTransferSession session);

    /**
     * A new content sharing transfer invitation has been received
     * 
     * @param session CSh session
     */
    public void handleContentSharingTransferInvitation(GeolocTransferSession session);

    /**
     * A new content sharing streaming invitation has been received
     * 
     * @param session CSh session
     */
    public void handleContentSharingStreamingInvitation(VideoStreamingSession session);

    /**
     * A new file transfer invitation has been received when already in a chat session
     * 
     * @param fileSharingSession File transfer session
     * @param isGroup is Group file transfer
     * @param contact Contact ID
     * @param displayName the display name of the remote contact
     * @param fileExpiration file transfer validity in milliseconds (or 0 if not applicable)
     */
    public void handleFileTransferInvitation(FileSharingSession fileSharingSession,
            boolean isGroup, ContactId contact, String displayName, long fileExpiration);

    /**
     * A new file transfer invitation has been received
     * 
     * @param fileSharingSession File transfer session
     * @param oneToOneChatSession Chat session
     * @param fileTransferValidity file transfer validity in milliseconds (or 0 if applicable)
     */
    public void handleOneToOneFileTransferInvitation(FileSharingSession fileSharingSession,
            OneToOneChatSession oneToOneChatSession, long fileTransferValidity);

    /**
     * Handle resend file transfer invitation
     * 
     * @param session
     * @param contact
     * @param displayName
     */
    public void handleOneToOneResendFileTransferInvitation(FileSharingSession session,
            ContactId contact, String displayName);

    /**
     * An incoming file transfer has been resumed
     * 
     * @param session File transfer session
     * @param isGroup is group file transfer
     * @param chatSessionId corresponding chatSessionId
     * @param chatId corresponding chatId
     */
    public void handleIncomingFileTransferResuming(FileSharingSession session, boolean isGroup,
            String chatSessionId, String chatId);

    /**
     * An outgoing file transfer has been resumed
     * 
     * @param session File transfer session
     * @param isGroup is group file transfer
     */
    public void handleOutgoingFileTransferResuming(FileSharingSession session, boolean isGroup);

    /**
     * New one-to-one chat session invitation
     * 
     * @param session Chat session
     */
    public void handleOneOneChatSessionInvitation(TerminatingOneToOneChatSession session);

    /**
     * New ad-hoc group chat session invitation
     * 
     * @param session Chat session
     */
    public void handleAdhocGroupChatSessionInvitation(TerminatingAdhocGroupChatSession session);

    /**
     * Store and Forward messages session invitation
     * 
     * @param session Chat session
     */
    public void handleStoreAndForwardMsgSessionInvitation(
            TerminatingStoreAndForwardOneToOneChatMessageSession session);

    /**
     * Handle store and forward notification session invitation
     * 
     * @param session
     */
    public void handleStoreAndForwardNotificationSessionInvitation(
            TerminatingStoreAndForwardOneToOneChatNotificationSession session);

    /**
     * New one to one message delivery status
     * 
     * @param contact Contact identifier
     * @param imdn Imdn document
     */
    public void handleOneToOneMessageDeliveryStatus(ContactId contact, ImdnDocument imdn);

    /**
     * New group message delivery status
     * 
     * @param chatId Chat ID
     * @param contact Contact identifier
     * @param imdn Imdn document
     */
    public void handleGroupMessageDeliveryStatus(String chatId, ContactId contact, ImdnDocument imdn);

    /**
     * New file delivery status
     * 
     * @param contact who notified status
     * @param imdn Imdn document
     */
    public void handleOneToOneFileDeliveryStatus(ContactId contact, ImdnDocument imdn);

    /**
     * New group file delivery status
     * 
     * @param chatId Chat Id
     * @param contact who notified status
     * @param imdn Imdn document
     */
    public void handleGroupFileDeliveryStatus(String chatId, ContactId contact, ImdnDocument imdn);

    /**
     * New SIP MSRP session invitation
     * 
     * @param intent Resolved intent
     * @param session SIP session
     */
    public void handleSipMsrpSessionInvitation(Intent intent, GenericSipMsrpSession session);

    /**
     * New SIP RTP session invitation
     * 
     * @param intent Resolved intent
     * @param session SIP session
     */
    public void handleSipRtpSessionInvitation(Intent intent, GenericSipRtpSession session);

    /**
     * User terms confirmation request
     * 
     * @param contact Remote server
     * @param id Request ID
     * @param type Type of request
     * @param pin PIN number requested
     * @param subject Subject
     * @param text Text
     * @param btnLabelAccept Label of Accept button
     * @param btnLabelReject Label of Reject button
     * @param timeout Timeout request in milliseconds
     */
    public void handleUserConfirmationRequest(ContactId contact, String id, String type,
            boolean pin, String subject, String text, String btnLabelAccept, String btnLabelReject,
            long timeout);

    /**
     * User terms confirmation acknowledge
     * 
     * @param contact Remote server
     * @param id Request ID
     * @param status Status
     * @param subject Subject
     * @param text Text
     */
    public void handleUserConfirmationAck(ContactId contact, String id, String status,
            String subject, String text);

    /**
     * User terms notification
     * 
     * @param contact Remote server
     * @param id Request ID
     * @param subject Subject
     * @param text Text
     * @param btnLabel Label of OK button
     */
    public void handleUserNotification(ContactId contact, String id, String subject, String text,
            String btnLabel);

    /**
     * SIM has changed
     */
    public void handleSimHasChanged();

    /**
     * Handle the case of rejected file transfer
     * 
     * @param remoteContact Remote contact
     * @param content File content
     * @param fileIcon Fileicon content
     * @param reasonCode Rejected reason code
     * @param timestamp Local timestamp when got file transfer invitation
     * @param timestampSent Remote timestamp sent in payload for the file transfer
     */

    public void handleFileTransferInvitationRejected(ContactId remoteContact, MmContent content,
            MmContent fileIcon, FileTransfer.ReasonCode reasonCode, long timestamp,
            long timestampSent);

    /**
     * Handle the case of rejected resend file transfer
     * 
     * @param fileTransferId
     * @param remoteContact Remote contact
     * @param reasonCode Rejected reason code
     * @param timestamp Local timestamp when got file transfer invitation
     * @param timestampSent Remote timestamp sent in payload for the file transfer
     */
    public void handleResendFileTransferInvitationRejected(String fileTransferId,
            ContactId remoteContact, FileTransfer.ReasonCode reasonCode, long timestamp,
            long timestampSent);

    /**
     * Handle the case of rejected group chat
     * 
     * @param chatId Chat ID
     * @param remoteContact Contact ID
     * @param subject Subject
     * @param participants Participants
     * @param reasonCode Rejected reason code
     * @param timestamp Local timestamp when got group chat invitation
     */
    public void handleGroupChatInvitationRejected(String chatId, ContactId remoteContact,
            String subject, Map<ContactId, ParticipantStatus> participants,
            GroupChat.ReasonCode reasonCode, long timestamp);

    /**
     * Handles image sharing rejection
     * 
     * @param remoteContact Remote contact
     * @param content Multimedia content
     * @param reasonCode Rejected reason code
     * @param timestamp Local timestamp when got image sharing invitation
     */
    public void handleImageSharingInvitationRejected(ContactId remoteContact, MmContent content,
            ImageSharing.ReasonCode reasonCode, long timestamp);

    /**
     * Handle the case of rejected video sharing
     * 
     * @param remoteContact Remote contact
     * @param content Video content
     * @param reasonCode Rejected reason code
     * @param timestamp Local timestamp when got video sharing invitation
     */
    public void handleVideoSharingInvitationRejected(ContactId remoteContact, VideoContent content,
            VideoSharing.ReasonCode reasonCode, long timestamp);

    /**
     * Handle the case of rejected geoloc sharing
     * 
     * @param remoteContact Remote contact
     * @param reasonCode Rejected reason code
     * @param timestamp Local timestamp when got geoloc sharing invitation
     */
    public void handleGeolocSharingInvitationRejected(ContactId remoteContact,
            GeolocSharing.ReasonCode reasonCode, long timestamp);

    /**
     * Handle the case of rejected ip call
     * 
     * @param remoteContact Remote contact
     * @param audioContent Audio content
     * @param videoContent Video content
     * @param reasonCode Rejected reason code
     * @param timestamp Local timestamp when got IP call invitation
     */
    public void handleIPCallInvitationRejected(ContactId remoteContact, AudioContent audioContent,
            VideoContent videoContent, IPCall.ReasonCode reasonCode, long timestamp);

    /**
     * Handle one-to-one chat session initiation
     * 
     * @param session Chat session
     */
    public void handleOneOneChatSessionInitiation(OneToOneChatSession session);

    /**
     * Handle rejoin group chat as part of send operation
     * 
     * @param chatId
     */
    public void handleRejoinGroupChatAsPartOfSendOperation(String chatId);

    /**
     * Handle auto rejoin group chat
     * 
     * @param chatId
     */
    public void handleRejoinGroupChat(String chatId);

    /**
     * Try to start ImService tasks once the IMS connection is re-established and the ImsServices
     * are restarted
     * 
     * @param core
     */
    public void tryToStartImServiceTasks(Core core);

    /**
     * Try to invite queued group chat participants
     * 
     * @param chatId
     * @param imService
     */
    public void tryToInviteQueuedGroupChatParticipantInvitations(String chatId,
            InstantMessagingService imService);

    /**
     * Try to send delayed displayed notification after service reconnection
     */
    public void tryToDispatchAllPendingDisplayNotifications();

    /**
     * Try to dequeue group chat messages and group file transfers
     * 
     * @param chatId
     * @param core
     */
    public void tryToDequeueGroupChatMessagesAndGroupFileTransfers(String chatId, Core core);

    /**
     * Try to dequeue of one-to-one chat messages for specific contact
     * 
     * @param contact
     * @param core
     */
    public void tryToDequeueOneToOneChatMessages(ContactId contact, Core core);

    /**
     * Try to dequeue all one-to-one chat messages and one-one file transfers
     * 
     * @param core
     */
    public void tryToDequeueAllOneToOneChatMessagesAndOneToOneFileTransfers(Core core);

    /**
     * Try to dequeue one-to-one and group file transfers
     * 
     * @param core
     */
    public void tryToDequeueFileTransfers(Core core);

    /**
     * Try to mark all queued group chat messages and group file transfers corresponding to contact
     * as failed
     * 
     * @param chatId
     */
    public void tryToMarkQueuedGroupChatMessagesAndGroupFileTransfersAsFailed(String chatId);

    /**
     * Handle one-one chat message delivery expiration
     * 
     * @param intent
     */
    public void handleOneToOneChatMessageDeliveryExpiration(Intent intent);

    /**
     * Handle one-one file transfer delivery expiration
     * 
     * @param intent
     */
    public void handleOneToOneFileTransferDeliveryExpiration(Intent intent);

    /**
     * Handle imdn DISPLAY report sent for message
     * 
     * @param chatId ChatId
     * @param contactId Remote contact
     * @param msgId
     */
    public void handleChatMessageDisplayReportSent(String chatId, ContactId remote, String msgId);

    /**
     * Handle one-one file transferfailure
     * 
     * @param fileTransferId
     * @param contact
     * @param reasonCode
     */
    public void handleOneToOneFileTransferFailure(String fileTransferId, ContactId contact,
            FileTransfer.ReasonCode reasonCode);

    /**
     * Deletes all one to one chat from history and abort/reject any associated ongoing session if
     * such exists.
     * 
     * @param imService
     */
    public void handleDeleteOneToOneChats(InstantMessagingService imService);

    /**
     * Deletes all group chat from history and abort/reject any associated ongoing session if such
     * exists.
     * 
     * @param imService
     */
    public void handleDeleteGroupChats(InstantMessagingService imService);

    /**
     * Deletes a one to one chat with a given contact from history and abort/reject any associated
     * ongoing session if such exists.
     * 
     * @param imService
     * @param contact
     */
    public void handleDeleteOneToOneChat(InstantMessagingService imService, ContactId contact);

    /**
     * Delete a group chat by its chat id from history and abort/reject any associated ongoing
     * session if such exists.
     * 
     * @param imService
     * @param chatId
     */
    public void handleDeleteGroupChat(InstantMessagingService imService, String chatId);

    /**
     * Delete a message from its message id from history. Will resolve if the message is one to one
     * or from a group chat.
     * 
     * @param imService
     * @param msgId
     */
    public void handleDeleteMessage(InstantMessagingService imService, String msgId);
}
