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
import com.gsma.rcs.core.content.GeolocContent;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.content.VideoContent;
import com.gsma.rcs.core.ims.ImsError;
import com.gsma.rcs.core.ims.service.capability.Capabilities;
import com.gsma.rcs.core.ims.service.im.chat.OneToOneChatSession;
import com.gsma.rcs.core.ims.service.im.chat.TerminatingAdhocGroupChatSession;
import com.gsma.rcs.core.ims.service.im.chat.TerminatingOneToOneChatSession;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.core.ims.service.im.chat.standfw.TerminatingStoreAndForwardOneToOneMessageSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.gsma.rcs.core.ims.service.ipcall.IPCallSession;
import com.gsma.rcs.core.ims.service.presence.pidf.PidfDocument;
import com.gsma.rcs.core.ims.service.richcall.geoloc.GeolocTransferSession;
import com.gsma.rcs.core.ims.service.richcall.image.ImageTransferSession;
import com.gsma.rcs.core.ims.service.richcall.video.VideoStreamingSession;
import com.gsma.rcs.core.ims.service.sip.messaging.GenericSipMsrpSession;
import com.gsma.rcs.core.ims.service.sip.streaming.GenericSipRtpSession;
import com.gsma.rcs.service.api.ServerApiException;
import com.gsma.rcs.service.ipcalldraft.IPCall;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.RcsServiceRegistration;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharing;
import com.gsma.services.rcs.sharing.image.ImageSharing;
import com.gsma.services.rcs.sharing.video.VideoSharing;

import android.content.Intent;

import java.util.Set;

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
     */
    public void handleFileTransferInvitation(FileSharingSession fileSharingSession,
            boolean isGroup, ContactId contact, String displayName);

    /**
     * A new file transfer invitation has been received
     * 
     * @param fileSharingSession File transfer session
     * @param oneToOneChatSession Chat session
     */
    public void handleOneToOneFileTransferInvitation(FileSharingSession fileSharingSession,
            OneToOneChatSession oneToOneChatSession);

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
            TerminatingStoreAndForwardOneToOneMessageSession session);

    /**
     * New message delivery status
     * 
     * @param contact Contact identifier
     * @param imdn Imdn document
     */
    public void handleMessageDeliveryStatus(ContactId contact, ImdnDocument imdn);

    /**
     * New file delivery status
     * 
     * @param contact who notified status
     * @param imdn Imdn document
     */
    public void handleFileDeliveryStatus(ContactId contact, ImdnDocument imdn);

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
     * @param timeout Timeout request
     */
    public void handleUserConfirmationRequest(ContactId contact, String id, String type,
            boolean pin, String subject, String text, String btnLabelAccept, String btnLabelReject,
            int timeout);

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
     * Try to send delayed displayed notification after service reconnection
     */
    public void tryToDispatchAllPendingDisplayNotifications();

    /**
     * Handle the case of rejected file transfer
     * 
     * @param contact Remote contact
     * @param content File content
     * @param fileIcon Fileicon content
     * @param reasonCode Rejected reason code
     */

    public void handleFileTransferInvitationRejected(ContactId contact, MmContent content,
            MmContent fileIcon, FileTransfer.ReasonCode reasonCode);

    /**
     * Handle the case of rejected group chat
     * 
     * @param chatId Chat ID
     * @param contact Contact ID
     * @param subject Subject
     * @param participants Participants
     * @param reasonCode Rejected reason code
     */
    public void handleGroupChatInvitationRejected(String chatId, ContactId contact, String subject,
            Set<ParticipantInfo> participants, GroupChat.ReasonCode reasonCode);

    /**
     * Handles image sharing rejection
     * 
     * @param contact Remote contact
     * @param content Multimedia content
     * @param reasonCode Rejected reason code
     */
    public void handleImageSharingInvitationRejected(ContactId contact, MmContent content,
            ImageSharing.ReasonCode reasonCode);

    /**
     * Handle the case of rejected video sharing
     * 
     * @param contact Remote contact
     * @param content Video content
     * @param reasonCode Rejected reason code
     */
    public void handleVideoSharingInvitationRejected(ContactId contact, VideoContent content,
            VideoSharing.ReasonCode reasonCode);

    /**
     * Handle the case of rejected geoloc sharing
     * 
     * @param contact Remote contact
     * @param content Geoloc content
     * @param reasonCode Rejected reason code
     */
    public void handleGeolocSharingInvitationRejected(ContactId contact, GeolocContent content,
            GeolocSharing.ReasonCode reasonCode);

    /**
     * Handle the case of rejected ip call
     * 
     * @param contact Remote contact
     * @param audioContent Audio content
     * @param videoContent Video content
     * @param reasonCode Rejected reason code
     */
    public void handleIPCallInvitationRejected(ContactId contact, AudioContent audioContent,
            VideoContent videoContent, IPCall.ReasonCode reasonCode);

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
     * @throws ServerApiException
     */
    public void handleRejoinGroupChatAsPartOfSendOperation(String chatId) throws ServerApiException;

    /**
     * Handle auto rejoin group chat
     * 
     * @param chatId
     * @throws ServerApiException
     */
    public void handleAutoRejoinGroupChat(String chatId) throws ServerApiException;
}
