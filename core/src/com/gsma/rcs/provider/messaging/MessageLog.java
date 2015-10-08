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

package com.gsma.rcs.provider.messaging;

import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.ReasonCode;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.Status;
import com.gsma.services.rcs.chat.ChatLog.Message.GroupChatEvent;
import com.gsma.services.rcs.chat.ChatLog.Message.MimeType;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class to interface the message table
 */
public class MessageLog implements IMessageLog {

    private LocalContentResolver mLocalContentResolver;

    private GroupDeliveryInfoLog mGroupChatDeliveryInfoLog;

    private final RcsSettings mRcsSettings;

    private static final Logger sLogger = Logger.getLogger(MessageLog.class.getSimpleName());

    private static final String[] PROJECTION_MESSAGE_ID = new String[] {
        MessageData.KEY_MESSAGE_ID
    };

    private static final String[] PROJECTION_GROUP_CHAT_EVENTS = new String[] {
            MessageData.KEY_STATUS, MessageData.KEY_CONTACT
    };

    private static final String SELECTION_GROUP_CHAT_EVENTS = new StringBuilder(
            MessageData.KEY_CHAT_ID).append("=? AND ").append(MessageData.KEY_MIME_TYPE)
            .append("='").append(MimeType.GROUPCHAT_EVENT).append("' GROUP BY ")
            .append(MessageData.KEY_CONTACT).toString();

    private static final int FIRST_COLUMN_IDX = 0;

    private static final String SELECTION_QUEUED_ONETOONE_CHAT_MESSAGES = new StringBuilder(
            MessageData.KEY_CHAT_ID).append("=? AND ").append(MessageData.KEY_STATUS).append("=")
            .append(Status.QUEUED.toInt()).toString();

    private static final String SELECTION_ALL_QUEUED_ONETOONE_CHAT_MESSAGES = new StringBuilder(
            MessageData.KEY_CHAT_ID).append("=").append(MessageData.KEY_CONTACT).append(" AND ")
            .append(MessageData.KEY_STATUS).append("=").append(Status.QUEUED.toInt()).toString();

    private static final int CHAT_MESSAGE_DELIVERY_EXPIRED = 1;

    private static final int CHAT_MESSAGE_DELIVERY_EXPIRATION_NOT_APPLICABLE = 0;

    private static final String SELECTION_BY_UNDELIVERED_ONETOONE_CHAT_MESSAGES = new StringBuilder(
            MessageData.KEY_EXPIRED_DELIVERY).append("<>").append(CHAT_MESSAGE_DELIVERY_EXPIRED)
            .append(" AND ").append(MessageData.KEY_DELIVERY_EXPIRATION).append("<>")
            .append(CHAT_MESSAGE_DELIVERY_EXPIRATION_NOT_APPLICABLE).append(" AND ")
            .append(MessageData.KEY_STATUS).append(" NOT IN(").append(Status.DELIVERED.toInt())
            .append(",").append(Status.DISPLAYED.toInt()).append(")").toString();

    private static final String ORDER_BY_TIMESTAMP_ASC = MessageData.KEY_TIMESTAMP.concat(" ASC");

    private static final String SELECTION_BY_NOT_DISPLAYED = new StringBuilder(
            MessageData.KEY_STATUS).append("<>").append(Status.DISPLAYED.toInt()).toString();

    /**
     * Constructor
     * 
     * @param localContentResolver Local content resolver
     * @param groupChatDeliveryInfoLog
     * @param rcsSettings
     */
    /* package private */MessageLog(LocalContentResolver localContentResolver,
            GroupDeliveryInfoLog groupChatDeliveryInfoLog, RcsSettings rcsSettings) {
        mLocalContentResolver = localContentResolver;
        mGroupChatDeliveryInfoLog = groupChatDeliveryInfoLog;
        mRcsSettings = rcsSettings;
    }

    private void addIncomingOneToOneMessage(ChatMessage msg, Status status, ReasonCode reasonCode)
            throws PayloadException {
        ContactId contact = msg.getRemoteContact();
        String msgId = msg.getMessageId();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Add incoming chat message: contact=").append(contact)
                    .append(", msg=").append(msgId).append(", status=").append(status)
                    .append(", reasonCode=").append(reasonCode).append(".").toString());
        }

        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_CHAT_ID, contact.toString());
        values.put(MessageData.KEY_MESSAGE_ID, msgId);
        values.put(MessageData.KEY_CONTACT, contact.toString());
        values.put(MessageData.KEY_DIRECTION, Direction.INCOMING.toInt());
        values.put(MessageData.KEY_READ_STATUS, ReadStatus.UNREAD.toInt());
        values.put(MessageData.KEY_MIME_TYPE, msg.getMimeType());
        values.put(MessageData.KEY_CONTENT, msg.getContent());

        values.put(MessageData.KEY_TIMESTAMP, msg.getTimestamp());
        values.put(MessageData.KEY_TIMESTAMP_SENT, msg.getTimestampSent());
        values.put(MessageData.KEY_TIMESTAMP_DELIVERED, 0);
        values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, 0);
        values.put(MessageData.KEY_DELIVERY_EXPIRATION, 0);
        values.put(MessageData.KEY_EXPIRED_DELIVERY, 0);

        values.put(MessageData.KEY_STATUS, status.toInt());
        values.put(MessageData.KEY_REASON_CODE, reasonCode.toInt());
        mLocalContentResolver.insert(MessageData.CONTENT_URI, values);
    }

    /**
     * Add outgoing one-to-one chat message
     * 
     * @param msg Chat message
     * @param status Status
     * @param reasonCode Reason code
     * @param deliveryExpiration
     * @throws PayloadException
     */
    @Override
    public void addOutgoingOneToOneChatMessage(ChatMessage msg, Status status,
            ReasonCode reasonCode, long deliveryExpiration) throws PayloadException {
        ContactId contact = msg.getRemoteContact();
        String msgId = msg.getMessageId();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Add outgoing chat message: contact=").append(contact)
                    .append(", msg=").append(msgId).append(", status=").append(status)
                    .append(", reasonCode=").append(reasonCode).append(".").toString());
        }
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_CHAT_ID, contact.toString());
        values.put(MessageData.KEY_MESSAGE_ID, msgId);
        values.put(MessageData.KEY_CONTACT, contact.toString());
        values.put(MessageData.KEY_DIRECTION, Direction.OUTGOING.toInt());
        values.put(MessageData.KEY_READ_STATUS, ReadStatus.UNREAD.toInt());
        values.put(MessageData.KEY_MIME_TYPE, msg.getMimeType());
        values.put(MessageData.KEY_CONTENT, msg.getContent());

        values.put(MessageData.KEY_TIMESTAMP, msg.getTimestamp());
        values.put(MessageData.KEY_TIMESTAMP_SENT, msg.getTimestampSent());
        values.put(MessageData.KEY_TIMESTAMP_DELIVERED, 0);
        values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, 0);
        values.put(MessageData.KEY_DELIVERY_EXPIRATION, deliveryExpiration);
        values.put(MessageData.KEY_EXPIRED_DELIVERY, 0);
        values.put(MessageData.KEY_STATUS, status.toInt());
        values.put(MessageData.KEY_REASON_CODE, reasonCode.toInt());
        mLocalContentResolver.insert(MessageData.CONTENT_URI, values);
    }

    @Override
    public void addOneToOneSpamMessage(ChatMessage msg) throws PayloadException {
        addIncomingOneToOneMessage(msg, Status.REJECTED, ReasonCode.REJECTED_SPAM);
    }

    /**
     * Add incoming one-to-one chat message
     * 
     * @param msg Chat message
     * @param imdnDisplayedRequested Indicates whether IMDN display was requested
     * @throws PayloadException
     */
    @Override
    public void addIncomingOneToOneChatMessage(ChatMessage msg, boolean imdnDisplayedRequested)
            throws PayloadException {
        if (imdnDisplayedRequested) {
            addIncomingOneToOneMessage(msg, Status.DISPLAY_REPORT_REQUESTED, ReasonCode.UNSPECIFIED);

        } else {
            addIncomingOneToOneMessage(msg, Status.RECEIVED, ReasonCode.UNSPECIFIED);
        }
    }

    /**
     * Add incoming group chat message
     * 
     * @param chatId Chat ID
     * @param msg Chat message
     * @param imdnDisplayedRequested Indicates whether IMDN display was requested
     * @throws PayloadException
     */
    @Override
    public void addIncomingGroupChatMessage(String chatId, ChatMessage msg,
            boolean imdnDisplayedRequested) throws PayloadException {
        Status chatMessageStatus = imdnDisplayedRequested ? Status.DISPLAY_REPORT_REQUESTED
                : Status.RECEIVED;
        addGroupChatMessage(chatId, msg, Direction.INCOMING, null, chatMessageStatus,
                ReasonCode.UNSPECIFIED);
    }

    /**
     * Add outgoing group chat message
     * 
     * @param chatId Chat ID
     * @param msg Chat message
     * @param status Status
     * @param reasonCode Reason code
     * @throws NetworkException
     * @throws PayloadException
     */
    @Override
    public void addOutgoingGroupChatMessage(String chatId, ChatMessage msg,
            Set<ContactId> recipients, Status status, ReasonCode reasonCode)
            throws PayloadException {
        addGroupChatMessage(chatId, msg, Direction.OUTGOING, recipients, status, reasonCode);
    }

    /**
     * Add group chat message
     * 
     * @param chatId Chat ID
     * @param msg Chat message
     * @param direction Direction
     * @param status Status
     * @param reasonCode Reason code
     * @throws PayloadException
     */
    private void addGroupChatMessage(String chatId, ChatMessage msg, Direction direction,
            Set<ContactId> recipients, Status status, ReasonCode reasonCode)
            throws PayloadException {
        String msgId = msg.getMessageId();
        ContactId contact = msg.getRemoteContact();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Add group chat message; chatId=").append(chatId)
                    .append(", msg=").append(msgId).append(", dir=").append(direction)
                    .append(", contact=").append(contact).append(".").toString());
        }

        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_CHAT_ID, chatId);
        values.put(MessageData.KEY_MESSAGE_ID, msgId);
        if (contact != null) {
            values.put(MessageData.KEY_CONTACT, contact.toString());
        }
        values.put(MessageData.KEY_DIRECTION, direction.toInt());
        values.put(MessageData.KEY_READ_STATUS, ReadStatus.UNREAD.toInt());
        values.put(MessageData.KEY_STATUS, status.toInt());
        values.put(MessageData.KEY_REASON_CODE, reasonCode.toInt());
        values.put(MessageData.KEY_MIME_TYPE, msg.getMimeType());
        values.put(MessageData.KEY_CONTENT, msg.getContent());
        values.put(MessageData.KEY_TIMESTAMP, msg.getTimestamp());
        values.put(MessageData.KEY_TIMESTAMP_SENT, msg.getTimestampSent());
        values.put(MessageData.KEY_TIMESTAMP_DELIVERED, 0);
        values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, 0);
        values.put(MessageData.KEY_DELIVERY_EXPIRATION, 0);
        values.put(MessageData.KEY_EXPIRED_DELIVERY, 0);
        mLocalContentResolver.insert(MessageData.CONTENT_URI, values);

        if (direction == Direction.OUTGOING) {
            try {
                GroupDeliveryInfo.Status deliveryStatus = GroupDeliveryInfo.Status.NOT_DELIVERED;
                if (mRcsSettings.isAlbatrosRelease()) {
                    deliveryStatus = GroupDeliveryInfo.Status.UNSUPPORTED;
                }
                for (ContactId recipient : recipients) {
                    /* Add entry with delivered and displayed timestamps set to 0. */
                    mGroupChatDeliveryInfoLog.addGroupChatDeliveryInfoEntry(chatId, recipient,
                            msgId, deliveryStatus, GroupDeliveryInfo.ReasonCode.UNSPECIFIED, 0, 0);
                }
            } catch (Exception e) {
                mLocalContentResolver.delete(Uri.withAppendedPath(MessageData.CONTENT_URI, msgId),
                        null, null);
                mLocalContentResolver.delete(
                        Uri.withAppendedPath(GroupDeliveryInfoData.CONTENT_URI, msgId), null, null);
                if (sLogger.isActivated()) {
                    sLogger.warn("Group chat message with msgId '" + msgId
                            + "' could not be added to database!");
                }
            }
        }
    }

    @Override
    public String addGroupChatEvent(String chatId, ContactId contact, GroupChatEvent.Status status,
            long timestamp) {
        if (sLogger.isActivated()) {
            sLogger.debug("Add group chat system message: chatID=" + chatId + ", contact="
                    + contact + ", status=" + status);
        }
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_CHAT_ID, chatId);
        if (contact != null) {
            values.put(MessageData.KEY_CONTACT, contact.toString());
        }
        String msgId = IdGenerator.generateMessageID();
        values.put(MessageData.KEY_MESSAGE_ID, msgId);
        values.put(MessageData.KEY_MIME_TYPE, MimeType.GROUPCHAT_EVENT);
        values.put(MessageData.KEY_STATUS, status.toInt());
        values.put(MessageData.KEY_REASON_CODE, ReasonCode.UNSPECIFIED.toInt());
        values.put(MessageData.KEY_DIRECTION, Direction.IRRELEVANT.toInt());
        values.put(MessageData.KEY_READ_STATUS, ReadStatus.UNREAD.toInt());
        values.put(MessageData.KEY_TIMESTAMP, timestamp);
        values.put(MessageData.KEY_TIMESTAMP_SENT, timestamp);
        values.put(MessageData.KEY_TIMESTAMP_DELIVERED, 0);
        values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, 0);
        values.put(MessageData.KEY_DELIVERY_EXPIRATION, 0);
        values.put(MessageData.KEY_EXPIRED_DELIVERY, 0);
        mLocalContentResolver.insert(MessageData.CONTENT_URI, values);
        return msgId;
    }

    @Override
    public void markMessageAsRead(String msgId) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Marking chat message as read: msgId=").append(msgId)
                    .toString());
        }
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_READ_STATUS, ReadStatus.READ.toInt());

        if (mLocalContentResolver.update(Uri.withAppendedPath(MessageData.CONTENT_URI, msgId),
                values, null, null) < 1) {
            if (sLogger.isActivated()) {
                sLogger.warn("There was no message with msgId '" + msgId + "' to mark as read.");
            }
        }
    }

    /**
     * Set chat message status and reason code. Note that this method should not be used for
     * Status.DELIVERED and Status.DISPLAYED. These states require timestamps and should be set
     * through setChatMessageStatusDelivered and setChatMessageStatusDisplayed respectively.
     * 
     * @param msgId Message ID
     * @param status Message status (See restriction above)
     * @param reasonCode Message status reason code
     * @return the number of updated rows
     */
    @Override
    public boolean setChatMessageStatusAndReasonCode(String msgId, Status status,
            ReasonCode reasonCode) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Update chat message: msgId=").append(msgId)
                    .append(", status=").append(status).append(", reasonCode=").append(reasonCode)
                    .toString());
        }
        switch (status) {
            case DELIVERED:
            case DISPLAYED:
                throw new IllegalArgumentException(new StringBuilder("Status that requires ")
                        .append("timestamp passed, use specific method taking timestamp")
                        .append(" to set status ").append(status.toString()).toString());
            default:
        }
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_STATUS, status.toInt());
        values.put(MessageData.KEY_REASON_CODE, reasonCode.toInt());
        return mLocalContentResolver.update(Uri.withAppendedPath(MessageData.CONTENT_URI, msgId),
                values, null, null) > 0;
    }

    @Override
    public boolean isMessagePersisted(String msgId) {
        Cursor cursor = null;
        Uri contentUri = Uri.withAppendedPath(MessageData.CONTENT_URI, msgId);
        try {
            cursor = mLocalContentResolver.query(contentUri, PROJECTION_MESSAGE_ID, null, null,
                    null);
            CursorUtil.assertCursorIsNotNull(cursor, contentUri);
            return cursor.moveToNext();
        } finally {
            CursorUtil.close(cursor);
        }
    }

    private Cursor getMessageData(String columnName, String msgId) {
        String[] projection = new String[] {
            columnName
        };
        Uri contentUri = Uri.withAppendedPath(MessageData.CONTENT_URI, msgId);
        Cursor cursor = mLocalContentResolver.query(contentUri, projection, null, null, null);
        CursorUtil.assertCursorIsNotNull(cursor, contentUri);
        if (!cursor.moveToNext()) {
            CursorUtil.close(cursor);
            return null;
        }
        return cursor;
    }

    private Integer getDataAsInteger(Cursor cursor) {
        try {
            if (cursor.isNull(FIRST_COLUMN_IDX)) {
                return null;
            }
            return cursor.getInt(FIRST_COLUMN_IDX);

        } finally {
            CursorUtil.close(cursor);
        }
    }

    private Long getDataAsLong(Cursor cursor) {
        try {
            if (cursor.isNull(FIRST_COLUMN_IDX)) {
                return null;
            }
            return cursor.getLong(FIRST_COLUMN_IDX);

        } finally {
            CursorUtil.close(cursor);
        }
    }

    private String getDataAsString(Cursor cursor) {
        try {
            return cursor.getString(FIRST_COLUMN_IDX);

        } finally {
            CursorUtil.close(cursor);
        }
    }

    private Boolean getDataAsBoolean(Cursor cursor) {
        try {
            if (cursor.isNull(FIRST_COLUMN_IDX)) {
                return null;
            }
            return cursor.getInt(FIRST_COLUMN_IDX) == 1;

        } finally {
            CursorUtil.close(cursor);
        }
    }

    @Override
    public Boolean isMessageRead(String msgId) {
        Cursor cursor = getMessageData(MessageData.KEY_READ_STATUS, msgId);
        if (cursor == null) {
            return null;
        }
        return (getDataAsInteger(cursor) == ReadStatus.READ.toInt());
    }

    @Override
    public Long getMessageSentTimestamp(String msgId) {
        Cursor cursor = getMessageData(MessageData.KEY_TIMESTAMP_SENT, msgId);
        if (cursor == null) {
            return null;
        }
        return getDataAsLong(cursor);
    }

    @Override
    public Long getMessageTimestamp(String msgId) {
        Cursor cursor = getMessageData(MessageData.KEY_TIMESTAMP, msgId);
        if (cursor == null) {
            return null;
        }
        return getDataAsLong(cursor);
    }

    @Override
    public Status getMessageStatus(String msgId) {
        Cursor cursor = getMessageData(MessageData.KEY_STATUS, msgId);
        if (cursor == null) {
            return null;
        }
        return Status.valueOf(getDataAsInteger(cursor));
    }

    @Override
    public ReasonCode getMessageReasonCode(String msgId) {
        Cursor cursor = getMessageData(MessageData.KEY_REASON_CODE, msgId);
        if (cursor == null) {
            return null;
        }
        return ReasonCode.valueOf(getDataAsInteger(cursor));
    }

    @Override
    public String getMessageMimeType(String msgId) {
        Cursor cursor = getMessageData(MessageData.KEY_MIME_TYPE, msgId);
        if (cursor == null) {
            return null;
        }
        return getDataAsString(cursor);
    }

    @Override
    public String getMessageChatId(String msgId) {
        Cursor cursor = getMessageData(MessageData.KEY_CHAT_ID, msgId);
        if (cursor == null) {
            return null;
        }
        return getDataAsString(cursor);
    }

    @Override
    public String getChatMessageContent(String msgId) {
        Cursor cursor = getMessageData(MessageData.KEY_CONTENT, msgId);
        if (cursor == null) {
            return null;
        }
        return getDataAsString(cursor);
    }

    @Override
    public Boolean isChatMessageExpiredDelivery(String msgId) {
        Cursor cursor = getMessageData(MessageData.KEY_EXPIRED_DELIVERY, msgId);
        if (cursor == null) {
            return null;
        }
        return getDataAsBoolean(cursor);
    }

    @Override
    public Cursor getChatMessageData(String msgId) {
        Uri contentUri = Uri.withAppendedPath(MessageData.CONTENT_URI, msgId);
        Cursor cursor = mLocalContentResolver.query(contentUri, null, null, null, null);
        CursorUtil.assertCursorIsNotNull(cursor, contentUri);
        return cursor;
    }

    @Override
    public Cursor getQueuedOneToOneChatMessages(ContactId contact) {
        String[] selectionArgs = new String[] {
            contact.toString()
        };
        Cursor cursor = mLocalContentResolver.query(MessageData.CONTENT_URI, null,
                SELECTION_QUEUED_ONETOONE_CHAT_MESSAGES, selectionArgs, ORDER_BY_TIMESTAMP_ASC);
        CursorUtil.assertCursorIsNotNull(cursor, MessageData.CONTENT_URI);
        return cursor;
    }

    @Override
    public Cursor getAllQueuedOneToOneChatMessages() {
        Cursor cursor = mLocalContentResolver.query(MessageData.CONTENT_URI, null,
                SELECTION_ALL_QUEUED_ONETOONE_CHAT_MESSAGES, null, ORDER_BY_TIMESTAMP_ASC);
        CursorUtil.assertCursorIsNotNull(cursor, MessageData.CONTENT_URI);
        return cursor;
    }

    @Override
    public boolean setChatMessageTimestamp(String msgId, long timestamp, long timestampSent) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Set chat message timestamp msgId=").append(msgId)
                    .append(", timestamp=").append(timestamp).append(", timestampSent=")
                    .append(timestampSent).toString());
        }
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_TIMESTAMP, timestamp);
        values.put(MessageData.KEY_TIMESTAMP_SENT, timestampSent);
        return mLocalContentResolver.update(Uri.withAppendedPath(MessageData.CONTENT_URI, msgId),
                values, null, null) > 0;
    }

    @Override
    public Map<ContactId, GroupChatEvent.Status> getGroupChatEvents(String chatId) {
        String[] selectionArgs = new String[] {
            chatId
        };
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(MessageData.CONTENT_URI,
                    PROJECTION_GROUP_CHAT_EVENTS, SELECTION_GROUP_CHAT_EVENTS, selectionArgs,
                    ORDER_BY_TIMESTAMP_ASC);
            CursorUtil.assertCursorIsNotNull(cursor, MessageData.CONTENT_URI);
            if (!cursor.moveToNext()) {
                return Collections.emptyMap();
            }
            Map<ContactId, GroupChatEvent.Status> groupChatEvents = new HashMap<ContactId, GroupChatEvent.Status>();
            int columnIdxStatus = cursor.getColumnIndexOrThrow(MessageData.KEY_STATUS);
            int columnIdxContact = cursor.getColumnIndexOrThrow(MessageData.KEY_CONTACT);
            do {
                GroupChatEvent.Status status = GroupChatEvent.Status.valueOf(cursor
                        .getInt(columnIdxStatus));
                ContactId contact = ContactUtil.createContactIdFromTrustedData(cursor
                        .getString(columnIdxContact));
                groupChatEvents.put(contact, status);
            } while (cursor.moveToNext());
            return groupChatEvents;

        } finally {
            CursorUtil.close(cursor);
        }
    }

    @Override
    public boolean isOneToOneChatMessage(String msgId) {
        Cursor cursor = null;
        try {
            Uri contentUri = Uri.withAppendedPath(MessageData.CONTENT_URI, msgId);
            cursor = mLocalContentResolver.query(contentUri, new String[] {
                    MessageData.KEY_CONTACT, MessageData.KEY_CHAT_ID
            }, null, null, null);
            CursorUtil.assertCursorIsNotNull(cursor, contentUri);
            if (!cursor.moveToNext()) {
                return false;
            }
            String contactId = cursor.getString(cursor
                    .getColumnIndexOrThrow(MessageData.KEY_CONTACT));
            String chatId = cursor.getString(cursor.getColumnIndexOrThrow(MessageData.KEY_CHAT_ID));
            return chatId.equals(contactId);

        } finally {
            CursorUtil.close(cursor);
        }
    }

    @Override
    public boolean setChatMessageStatusDelivered(String msgId, long timestampDelivered) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("setChatMessageStatusDelivered msgId=").append(msgId)
                    .append(", timestampDelivered=").append(timestampDelivered).toString());
        }

        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_STATUS, Status.DELIVERED.toInt());
        values.put(MessageData.KEY_REASON_CODE, ReasonCode.UNSPECIFIED.toInt());
        values.put(MessageData.KEY_TIMESTAMP_DELIVERED, timestampDelivered);
        values.put(MessageData.KEY_EXPIRED_DELIVERY, 0);

        return mLocalContentResolver.update(Uri.withAppendedPath(MessageData.CONTENT_URI, msgId),
                values, SELECTION_BY_NOT_DISPLAYED, null) > 0;
    }

    @Override
    public boolean setChatMessageStatusDisplayed(String msgId, long timestampDisplayed) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("setChatMessageStatusDisplayed msgId=").append(msgId)
                    .append(", timestampDisplayed=").append(timestampDisplayed).toString());
        }

        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_STATUS, Status.DISPLAYED.toInt());
        values.put(MessageData.KEY_REASON_CODE, ReasonCode.UNSPECIFIED.toInt());
        values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, timestampDisplayed);
        values.put(MessageData.KEY_EXPIRED_DELIVERY, 0);
        return mLocalContentResolver.update(Uri.withAppendedPath(MessageData.CONTENT_URI, msgId),
                values, null, null) > 0;
    }

    @Override
    public void clearMessageDeliveryExpiration(List<String> msgIds) {
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_DELIVERY_EXPIRATION, 0);
        values.put(MessageData.KEY_EXPIRED_DELIVERY, 0);
        List<String> parameters = new ArrayList<String>();
        for (int i = 0; i < msgIds.size(); i++) {
            parameters.add("?");
        }
        String selection = new StringBuilder(MessageData.KEY_MESSAGE_ID).append(" IN (")
                .append(TextUtils.join(",", parameters)).append(")").toString();
        mLocalContentResolver.update(MessageData.CONTENT_URI, values, selection,
                msgIds.toArray(new String[msgIds.size()]));
    }

    @Override
    public boolean setChatMessageDeliveryExpired(String msgId) {
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_EXPIRED_DELIVERY, 1);
        return mLocalContentResolver.update(Uri.withAppendedPath(MessageData.CONTENT_URI, msgId),
                values, null, null) > 0;
    }

    @Override
    public Cursor getUndeliveredOneToOneChatMessages() {
        Cursor cursor = mLocalContentResolver.query(MessageData.CONTENT_URI, null,
                SELECTION_BY_UNDELIVERED_ONETOONE_CHAT_MESSAGES, null, ORDER_BY_TIMESTAMP_ASC);
        CursorUtil.assertCursorIsNotNull(cursor, MessageData.CONTENT_URI);
        return cursor;
    }

    @Override
    public boolean setChatMessageStatusAndTimestamp(String msgId, Status status,
            ReasonCode reasonCode, long timestamp, long timestampSent) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Update chat message: msgId=").append(msgId)
                    .append(", status=").append(status).append(", reasonCode=").append(reasonCode)
                    .append(", timestamp=").append(timestamp).append(", timestampSent=")
                    .append(timestampSent).toString());
        }
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_STATUS, status.toInt());
        values.put(MessageData.KEY_REASON_CODE, reasonCode.toInt());
        values.put(MessageData.KEY_TIMESTAMP, timestamp);
        values.put(MessageData.KEY_TIMESTAMP_SENT, timestampSent);
        return mLocalContentResolver.update(Uri.withAppendedPath(MessageData.CONTENT_URI, msgId),
                values, null, null) > 0;
    }
}
