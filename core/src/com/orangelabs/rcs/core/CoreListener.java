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

package com.orangelabs.rcs.core;

import android.content.Intent;

import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.ims.ImsError;
import com.orangelabs.rcs.core.ims.service.capability.Capabilities;
import com.orangelabs.rcs.core.ims.service.im.chat.OneOneChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.TerminatingAdhocGroupChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.TerminatingOne2OneChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.standfw.TerminatingStoreAndForwardMsgSession;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.orangelabs.rcs.core.ims.service.ipcall.IPCallSession;
import com.orangelabs.rcs.core.ims.service.presence.pidf.PidfDocument;
import com.orangelabs.rcs.core.ims.service.richcall.geoloc.GeolocTransferSession;
import com.orangelabs.rcs.core.ims.service.richcall.image.ImageTransferSession;
import com.orangelabs.rcs.core.ims.service.richcall.video.VideoStreamingSession;
import com.orangelabs.rcs.core.ims.service.sip.messaging.GenericSipMsrpSession;
import com.orangelabs.rcs.core.ims.service.sip.streaming.GenericSipRtpSession;

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
     */
    public void handleRegistrationTerminated();
    
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
     * @param presense Presence info document
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
	 */
	public void handleFileTransferInvitation(FileSharingSession fileSharingSession, boolean isGroup, ContactId contact);

	/**
	 * A new file transfer invitation has been received
	 * 
	 * @param fileSharingSession File transfer session
	 * @param one2oneChatSession Chat session
	 */
	public void handle1to1FileTransferInvitation(FileSharingSession fileSharingSession, OneOneChatSession one2oneChatSession);

	/**
	 * A new file transfer invitation has been received and creating a chat session
	 * 
	 * @param session File transfer session
	 * @param chatSession Group chat session
	 * @param contact Contact ID
	 */
	public void handleGroupFileTransferInvitation(FileSharingSession session, TerminatingAdhocGroupChatSession chatSession, ContactId contact);

    /**
     * An incoming file transfer has been resumed
     * 
     * @param session File transfer session
     * @param isGroup is group file transfer
     * @param chatSessionId corresponding chatSessionId
     * @param chatId corresponding chatId
     */
    public void handleIncomingFileTransferResuming(FileSharingSession session, boolean isGroup, String chatSessionId, String chatId);

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
    public void handleOneOneChatSessionInvitation(TerminatingOne2OneChatSession session);
    
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
    public void handleStoreAndForwardMsgSessionInvitation(TerminatingStoreAndForwardMsgSession session);
    
    /**
     * New message delivery status
     * 
     * @param contact Contact identifier
	 * @param msgId Message ID
     * @param status Delivery status
     */
    public void handleMessageDeliveryStatus(ContactId contact, String msgId, String status);

    /**
     * New file delivery status
     *
     * @param fileTransferId File transfer Id
     * @param status Delivery status
     * @param contact who notified status
     */
    public void handleFileDeliveryStatus(String fileTransferId, String status, ContactId contact);

    /**
     * New group file delivery status
     *
     * @param chatId Chat Id
     * @param fileTransferId File transfer Id
     * @param status Delivery status
     * @param contact who notified status
     */
    public void handleGroupFileDeliveryStatus(String chatId, String fileTransferId, String status, ContactId contact);

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
    public void handleUserConfirmationRequest(ContactId contact, String id,
            String type, boolean pin, String subject, String text,
            String btnLabelAccept, String btnLabelReject, int timeout);

    /**
     * User terms confirmation acknowledge
     * 
     * @param contact Remote server
     * @param id Request ID
     * @param status Status
     * @param subject Subject
     * @param text Text
     */
    public void handleUserConfirmationAck(ContactId contact, String id, String status, String subject, String text);

    /**
     * User terms notification
     *
     * @param contact Remote server
     * @param id Request ID
     * @param subject Subject
     * @param text Text
     * @param btnLabel Label of OK button
     */
    public void handleUserNotification(ContactId contact, String id, String subject, String text, String btnLabel);

    /**
     * SIM has changed
     */
    public void handleSimHasChanged();

    /**
     * Try to send delayed displayed notification after service reconnection
     */
    public void tryToDispatchAllPendingDisplayNotifications();
}
