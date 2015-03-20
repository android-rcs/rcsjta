/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gsma.rcs.service.broadcaster;

import com.gsma.services.rcs.GroupDeliveryInfo;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.ReasonCode;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.Status;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.chat.GroupChat.State;
import com.gsma.services.rcs.contact.ContactId;

/**
 * Interface to perform broadcast events on GroupChatListener
 */
public interface IGroupChatEventBroadcaster {

    public void broadcastMessageStatusChanged(String chatId, String mimeType, String msgId,
            Status status, ReasonCode reasonCode);

    public void broadcastMessageGroupDeliveryInfoChanged(String chatId, ContactId contact,
            String mimeType, String msgId, GroupDeliveryInfo.Status status,
            GroupDeliveryInfo.ReasonCode reasonCode);

    public void broadcastParticipantStatusChanged(String chatId, ContactId contact,
            ParticipantStatus status);

    public void broadcastStateChanged(String chatId, State state, GroupChat.ReasonCode reasonCode);

    public void broadcastComposingEvent(String chatId, ContactId contact, boolean status);

    public void broadcastInvitation(String chatId);

    public void broadcastMessageReceived(String mimeType, String msgId);
}
