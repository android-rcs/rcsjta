/*
 * Copyright 2013, France Telecom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsma.rcs.eventslog;

/**
 * Class EventsLogApi.
 */
public class EventsLogApi extends org.gsma.rcs.ClientApi {
    /**
     * Constant ID_COLUMN.
     */
    public static final int ID_COLUMN = 0;

    /**
     * Constant TYPE_COLUMN.
     */
    public static final int TYPE_COLUMN = 1;

    /**
     * Constant CHAT_SESSION_ID_COLUMN.
     */
    public static final int CHAT_SESSION_ID_COLUMN = 2;

    /**
     * Constant DATE_COLUMN.
     */
    public static final int DATE_COLUMN = 3;

    /**
     * Constant CONTACT_COLUMN.
     */
    public static final int CONTACT_COLUMN = 4;

    /**
     * Constant STATUS_COLUMN.
     */
    public static final int STATUS_COLUMN = 5;

    /**
     * Constant DATA_COLUMN.
     */
    public static final int DATA_COLUMN = 6;

    /**
     * Constant MESSAGE_ID_COLUMN.
     */
    public static final int MESSAGE_ID_COLUMN = 7;

    /**
     * Constant MIMETYPE_COLUMN.
     */
    public static final int MIMETYPE_COLUMN = 8;

    /**
     * Constant NAME_COLUMN.
     */
    public static final int NAME_COLUMN = 9;

    /**
     * Constant SIZE_COLUMN.
     */
    public static final int SIZE_COLUMN = 10;

    /**
     * Constant TOTAL_SIZE_COLUMN.
     */
    public static final int TOTAL_SIZE_COLUMN = 11;

    /**
     * Constant IS_SPAM_COLUMN.
     */
    public static final int IS_SPAM_COLUMN = 12;

    /**
     * Constant CHAT_ID_COLUMN.
     */
    public static final int CHAT_ID_COLUMN = 13;

    /**
     * Constant CHAT_REJOIN_ID_COLUMN.
     */
    public static final int CHAT_REJOIN_ID_COLUMN = 14;

    /**
     * Constant TYPE_INCOMING_CHAT_MESSAGE.
     */
    public static final int TYPE_INCOMING_CHAT_MESSAGE = 0;

    /**
     * Constant TYPE_OUTGOING_CHAT_MESSAGE.
     */
    public static final int TYPE_OUTGOING_CHAT_MESSAGE = 1;

    /**
     * Constant TYPE_CHAT_SYSTEM_MESSAGE.
     */
    public static final int TYPE_CHAT_SYSTEM_MESSAGE = 2;

    /**
     * Constant TYPE_INCOMING_GROUP_CHAT_MESSAGE.
     */
    public static final int TYPE_INCOMING_GROUP_CHAT_MESSAGE = 3;

    /**
     * Constant TYPE_OUTGOING_GROUP_CHAT_MESSAGE.
     */
    public static final int TYPE_OUTGOING_GROUP_CHAT_MESSAGE = 4;

    /**
     * Constant TYPE_GROUP_CHAT_SYSTEM_MESSAGE.
     */
    public static final int TYPE_GROUP_CHAT_SYSTEM_MESSAGE = 5;

    /**
     * Constant TYPE_INCOMING_FILE_TRANSFER.
     */
    public static final int TYPE_INCOMING_FILE_TRANSFER = 6;

    /**
     * Constant TYPE_OUTGOING_FILE_TRANSFER.
     */
    public static final int TYPE_OUTGOING_FILE_TRANSFER = 7;

    /**
     * Constant TYPE_INCOMING_RICH_CALL.
     */
    public static final int TYPE_INCOMING_RICH_CALL = 8;

    /**
     * Constant TYPE_OUTGOING_RICH_CALL.
     */
    public static final int TYPE_OUTGOING_RICH_CALL = 9;

    /**
     * Constant TYPE_INCOMING_SMS.
     */
    public static final int TYPE_INCOMING_SMS = 10;

    /**
     * Constant TYPE_OUTGOING_SMS.
     */
    public static final int TYPE_OUTGOING_SMS = 11;

    /**
     * Constant STATUS_STARTED.
     */
    public static final int STATUS_STARTED = 0;

    /**
     * Constant STATUS_TERMINATED.
     */
    public static final int STATUS_TERMINATED = 1;

    /**
     * Constant STATUS_FAILED.
     */
    public static final int STATUS_FAILED = 2;

    /**
     * Constant STATUS_IN_PROGRESS.
     */
    public static final int STATUS_IN_PROGRESS = 3;

    /**
     * Constant STATUS_CANCELED.
     */
    public static final int STATUS_CANCELED = 20;

    /**
     * Constant STATUS_TERMINATED_BY_USER.
     */
    public static final int STATUS_TERMINATED_BY_USER = 21;

    /**
     * Constant STATUS_TERMINATED_BY_REMOTE.
     */
    public static final int STATUS_TERMINATED_BY_REMOTE = 22;

    /**
     * Constant STATUS_SENT.
     */
    public static final int STATUS_SENT = 4;

    /**
     * Constant STATUS_RECEIVED.
     */
    public static final int STATUS_RECEIVED = 5;

    /**
     * Constant STATUS_MISSED.
     */
    public static final int STATUS_MISSED = 6;

    /**
     * Constant STATUS_DELIVERED.
     */
    public static final int STATUS_DELIVERED = 7;

    /**
     * Constant STATUS_DISPLAYED.
     */
    public static final int STATUS_DISPLAYED = 8;

    /**
     * Constant STATUS_ALL_DISPLAYED.
     */
    public static final int STATUS_ALL_DISPLAYED = 9;

    /**
     * Constant STATUS_REPORT_REQUESTED.
     */
    public static final int STATUS_REPORT_REQUESTED = 10;

    /**
     * Constant EVENT_JOINED_CHAT.
     */
    public static final int EVENT_JOINED_CHAT = 12;

    /**
     * Constant EVENT_LEFT_CHAT.
     */
    public static final int EVENT_LEFT_CHAT = 13;

    /**
     * Constant EVENT_INVITED.
     */
    public static final int EVENT_INVITED = 14;

    /**
     * Constant EVENT_INITIATED.
     */
    public static final int EVENT_INITIATED = 15;

    /**
     * Constant EVENT_DISCONNECT_CHAT.
     */
    public static final int EVENT_DISCONNECT_CHAT = 16;

    /**
     * Constant EVENT_FAILED.
     */
    public static final int EVENT_FAILED = 17;

    /**
     * Constant EVENT_BUSY.
     */
    public static final int EVENT_BUSY = 18;

    /**
     * Constant EVENT_DECLINED.
     */
    public static final int EVENT_DECLINED = 19;

    /**
     * Constant EVENT_PENDING.
     */
    public static final int EVENT_PENDING = 20;

    /**
     * Constant MESSAGE_IS_NOT_SPAM.
     */
    public static final int MESSAGE_IS_NOT_SPAM = 0;

    /**
     * Constant MESSAGE_IS_SPAM.
     */
    public static final int MESSAGE_IS_SPAM = 1;

    /**
     * Constant MODE_ONE_TO_ONE_CHAT.
     */
    public static final int MODE_ONE_TO_ONE_CHAT = 32;

    /**
     * Constant MODE_GROUP_CHAT.
     */
    public static final int MODE_GROUP_CHAT = 33;

    /**
     * Constant MODE_SPAM_BOX.
     */
    public static final int MODE_SPAM_BOX = 34;

    /**
     * Constant MODE_RC_CHAT_FT_SMS.
     */
    public static final int MODE_RC_CHAT_FT_SMS = 15;

    /**
     * Constant MODE_RC_CHAT_FT.
     */
    public static final int MODE_RC_CHAT_FT = 14;

    /**
     * Constant MODE_RC_CHAT_SMS.
     */
    public static final int MODE_RC_CHAT_SMS = 13;

    /**
     * Constant MODE_RC_CHAT.
     */
    public static final int MODE_RC_CHAT = 12;

    /**
     * Constant MODE_RC_FT_SMS.
     */
    public static final int MODE_RC_FT_SMS = 11;

    /**
     * Constant MODE_RC_FT.
     */
    public static final int MODE_RC_FT = 10;

    /**
     * Constant MODE_RC_SMS.
     */
    public static final int MODE_RC_SMS = 9;

    /**
     * Constant MODE_RC.
     */
    public static final int MODE_RC = 8;

    /**
     * Constant MODE_CHAT_FT_SMS.
     */
    public static final int MODE_CHAT_FT_SMS = 7;

    /**
     * Constant MODE_CHAT_FT.
     */
    public static final int MODE_CHAT_FT = 6;

    /**
     * Constant MODE_CHAT_SMS.
     */
    public static final int MODE_CHAT_SMS = 5;

    /**
     * Constant MODE_CHAT.
     */
    public static final int MODE_CHAT = 4;

    /**
     * Constant MODE_FT_SMS.
     */
    public static final int MODE_FT_SMS = 3;

    /**
     * Constant MODE_FT.
     */
    public static final int MODE_FT = 2;

    /**
     * Constant MODE_SMS.
     */
    public static final int MODE_SMS = 1;

    /**
     * Constant MODE_NONE.
     */
    public static final int MODE_NONE = 0;

    /**
     * Creates a new instance of EventsLogApi.
     *  
     * @param arg1 The arg1.
     */
    public EventsLogApi(android.content.Context arg1) {
        super((android.content.Context) null);
    }

    /**
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     */
    public void markChatMessageAsRead(String arg1, boolean arg2) {

    }

    /**
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     */
    public void markChatMessageAsSpam(String arg1, boolean arg2) {

    }

    public void deleteAllSpams() {

    }

    /**
     *  
     * @param arg1 The arg1.
     */
    public void deleteSpamMessage(String arg1) {

    }

    /**
     *  
     * @param arg1 The arg1.
     */
    public void deleteGroupChatConversation(String arg1) {

    }

    /**
     * Returns the event log content provider uri.
     *  
     * @param arg1 The arg1.
     * @return  The event log content provider uri.
     */
    public android.net.Uri getEventLogContentProviderUri(int arg1) {
        return (android.net.Uri) null;
    }

    /**
     * Returns the one to one chat log content provider uri.
     *  
     * @return  The one to one chat log content provider uri.
     */
    public android.net.Uri getOneToOneChatLogContentProviderUri() {
        return (android.net.Uri) null;
    }

    /**
     * Returns the group chat log content provider uri.
     *  
     * @return  The group chat log content provider uri.
     */
    public android.net.Uri getGroupChatLogContentProviderUri() {
        return (android.net.Uri) null;
    }

    /**
     * Returns the spam box log content provider uri.
     *  
     * @return  The spam box log content provider uri.
     */
    public android.net.Uri getSpamBoxLogContentProviderUri() {
        return (android.net.Uri) null;
    }

    /**
     *  
     * @param arg1 The arg1.
     */
    public void clearHistoryForContact(String arg1) {

    }

    /**
     *  
     * @param arg1 The arg1.
     */
    public void deleteLogEntry(long arg1) {

    }

    /**
     *  
     * @param arg1 The arg1.
     */
    public void deleteSmsEntry(long arg1) {

    }

    /**
     *  
     * @param arg1 The arg1.
     */
    public void deleteMmsEntry(long arg1) {

    }

    /**
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     */
    public void deleteRichCallEntry(String arg1, long arg2) {

    }

    /**
     *  
     * @param arg1 The arg1.
     */
    public void deleteImEntry(long arg1) {

    }

    /**
     *  
     * @param arg1 The arg1.
     */
    public void deleteMessagingLogForContact(String arg1) {

    }

    /**
     *  
     * @param arg1 The arg1.
     */
    public void deleteImSessionEntry(String arg1) {

    }

    /**
     * Returns the chat session cursor.
     *  
     * @param arg1 The arg1.
     * @return  The chat session cursor.
     */
    public android.database.Cursor getChatSessionCursor(String arg1) {
        return (android.database.Cursor) null;
    }

    /**
     * Returns the chat contact cursor.
     *  
     * @param arg1 The arg1.
     * @return  The chat contact cursor.
     */
    public android.database.Cursor getChatContactCursor(String arg1) {
        return (android.database.Cursor) null;
    }

    /**
     * Returns the last chat message.
     *  
     * @param arg1 The arg1.
     * @return  The last chat message.
     */
    public org.gsma.rcs.messaging.InstantMessage getLastChatMessage(String arg1) {
        return (org.gsma.rcs.messaging.InstantMessage) null;
    }

    /**
     * Returns the spam messages provider uri.
     *  
     * @return  The spam messages provider uri.
     */
    public android.net.Uri getSpamMessagesProviderUri() {
        return (android.net.Uri) null;
    }

    /**
     * Returns the number of unread chat messages.
     *  
     * @param arg1 The arg1.
     * @return  The number of unread chat messages.
     */
    public int getNumberOfUnreadChatMessages(String arg1) {
        return 0;
    }

    /**
     *  
     * @param arg1 The arg1.
     */
    public void clearSpamMessagesForContact(String arg1) {

    }

} // end EventsLogApi
