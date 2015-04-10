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

import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.service.api.ServerApiException;
import com.gsma.rcs.utils.IntentUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.GroupDeliveryInfo;
import com.gsma.services.rcs.chat.ChatLog.Message.Content;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.Status;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.chat.GroupChat.ReasonCode;
import com.gsma.services.rcs.chat.GroupChat.State;
import com.gsma.services.rcs.chat.GroupChatIntent;
import com.gsma.services.rcs.chat.IGroupChatListener;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Intent;
import android.os.RemoteCallbackList;

import java.util.List;

/**
 * GroupChatEventBroadcaster maintains the registering and unregistering of IGroupChatListener and
 * also performs broadcast events on these listeners upon the trigger of corresponding callbacks.
 */
public class GroupChatEventBroadcaster implements IGroupChatEventBroadcaster {

    private final RemoteCallbackList<IGroupChatListener> mGroupChatListeners = new RemoteCallbackList<IGroupChatListener>();

    private final Logger logger = Logger.getLogger(getClass().getName());

    public GroupChatEventBroadcaster() {
    }

    public void addGroupChatEventListener(IGroupChatListener listener) throws ServerApiException {
        mGroupChatListeners.register(listener);
    }

    public void removeGroupChatEventListener(IGroupChatListener listener) throws ServerApiException {
        mGroupChatListeners.unregister(listener);
    }

    public void broadcastMessageStatusChanged(String chatId, String mimeType, String msgId,
            Status status, Content.ReasonCode reasonCode) {
        int rcsStatus = status.toInt();
        int rcsReasonCode = reasonCode.toInt();
        final int N = mGroupChatListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mGroupChatListeners.getBroadcastItem(i).onMessageStatusChanged(chatId, mimeType,
                        msgId, rcsStatus, rcsReasonCode);
            } catch (Exception e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener.", e);
                }
            }
        }
        mGroupChatListeners.finishBroadcast();
    }

    public void broadcastMessageGroupDeliveryInfoChanged(String chatId, ContactId contact,
            String apiMimeType, String msgId, GroupDeliveryInfo.Status status,
            GroupDeliveryInfo.ReasonCode reasonCode) {
        final int N = mGroupChatListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mGroupChatListeners.getBroadcastItem(i).onMessageGroupDeliveryInfoChanged(chatId,
                        contact, apiMimeType, msgId, status.toInt(), reasonCode.toInt());
            } catch (Exception e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener.", e);
                }
            }
        }
        mGroupChatListeners.finishBroadcast();
    }

    public void broadcastParticipantStatusChanged(String chatId, ContactId contact,
            ParticipantStatus status) {
        final int N = mGroupChatListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mGroupChatListeners.getBroadcastItem(i).onParticipantStatusChanged(chatId, contact,
                        status.toInt());
            } catch (Exception e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
        mGroupChatListeners.finishBroadcast();
    }

    public void broadcastStateChanged(String chatId, State state, ReasonCode reasonCode) {
        final int N = mGroupChatListeners.beginBroadcast();
        int rcsState = state.toInt();
        int rcsReasonCode = reasonCode.toInt();
        for (int i = 0; i < N; i++) {
            try {
                mGroupChatListeners.getBroadcastItem(i).onStateChanged(chatId, rcsState,
                        rcsReasonCode);
            } catch (Exception e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
        mGroupChatListeners.finishBroadcast();
    }

    public void broadcastComposingEvent(String chatId, ContactId contact, boolean status) {
        final int N = mGroupChatListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mGroupChatListeners.getBroadcastItem(i).onComposingEvent(chatId, contact, status);
            } catch (Exception e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
        mGroupChatListeners.finishBroadcast();
    }

    public void broadcastInvitation(String chatId) {
        Intent invitation = new Intent(GroupChatIntent.ACTION_NEW_INVITATION);
        IntentUtils.tryToSetExcludeStoppedPackagesFlag(invitation);
        IntentUtils.tryToSetReceiverForegroundFlag(invitation);
        invitation.putExtra(GroupChatIntent.EXTRA_CHAT_ID, chatId);
        AndroidFactory.getApplicationContext().sendBroadcast(invitation);
    }

    public void broadcastMessageReceived(String apiMimeType, String msgId) {
        Intent newGroupChatMessage = new Intent(GroupChatIntent.ACTION_NEW_GROUP_CHAT_MESSAGE);
        IntentUtils.tryToSetExcludeStoppedPackagesFlag(newGroupChatMessage);
        IntentUtils.tryToSetReceiverForegroundFlag(newGroupChatMessage);
        newGroupChatMessage.putExtra(GroupChatIntent.EXTRA_MIME_TYPE, apiMimeType);
        newGroupChatMessage.putExtra(GroupChatIntent.EXTRA_MESSAGE_ID, msgId);
        AndroidFactory.getApplicationContext().sendBroadcast(newGroupChatMessage);
    }

    public void broadcastMessagesDeleted(String chatId, List<String> msgIds) {
        final int N = mGroupChatListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mGroupChatListeners.getBroadcastItem(i).onMessagesDeleted(chatId, msgIds);
            } catch (Exception e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener.", e);
                }
            }
        }
        mGroupChatListeners.finishBroadcast();
    }

    public void broadcastGroupChatsDeleted(List<String> chatIds) {
        final int N = mGroupChatListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mGroupChatListeners.getBroadcastItem(i).onDeleted(chatIds);
            } catch (Exception e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener.", e);
                }
            }
        }
        mGroupChatListeners.finishBroadcast();
    }
}
