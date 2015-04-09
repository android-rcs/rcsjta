/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
 ******************************************************************************/

package com.gsma.rcs.im.filetransfer;

import com.gsma.rcs.core.content.FileContent;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.messaging.FileTransferData;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtilMockContext;
import com.gsma.rcs.utils.MimeManager;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransfer.ReasonCode;
import com.gsma.services.rcs.filetransfer.FileTransfer.State;
import com.gsma.services.rcs.filetransfer.FileTransferLog;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class FileTransferLogTest extends AndroidTestCase {

    private ContactId mContact;
    private ContentResolver mContentResolver;
    private LocalContentResolver mLocalContentResolver;
    private MessagingLog mMessagingLog;
    private String mChatId;
    private String mFileTransferId;
    private long mTimestamp;
    private long mTimestampSent;
    private long mFileExpiration;
    private Random mRandom = new Random();

    private static final String IMAGE_FILENAME = "image1.jpg";
    private static final String IMAGE_URI = "content://file/image1.jpg";
    private static final long IMAGE_FILESIZE = 1234567;
    private static final MmContent CONTENT = new FileContent(Uri.parse(IMAGE_URI), IMAGE_FILESIZE,
            IMAGE_FILENAME);

    private static final String IMAGE_ICON_FILENAME = "icon1.jpg";
    private static final String IMAGE_ICON_URI = "content://file/icon1.jpg";
    private static final long IMAGE_ICON_FILESIZE = 123;
    private static final MmContent ICON_CONTENT = new FileContent(Uri.parse(IMAGE_ICON_URI),
            IMAGE_ICON_FILESIZE, IMAGE_ICON_FILENAME);

    protected void setUp() throws Exception {
        super.setUp();
        Context context = getContext();
        mContentResolver = context.getContentResolver();
        mLocalContentResolver = new LocalContentResolver(mContentResolver);
        RcsSettings rcsSettings = RcsSettings.createInstance(mLocalContentResolver);
        mMessagingLog = MessagingLog.createInstance(mContext, mLocalContentResolver, rcsSettings);
        ContactUtil contactUtils = ContactUtil.getInstance(new ContactUtilMockContext(mContext));
        mContact = contactUtils.formatContact("+339000000");
        mFileTransferId = Long.toString(mRandom.nextLong());
        mChatId = String.valueOf(mRandom.nextLong());
        mTimestamp = mRandom.nextLong();
        mTimestampSent = mRandom.nextLong();
        mFileExpiration = mRandom.nextLong();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        mMessagingLog.deleteAllEntries();
    }

    public void testAddFileTransfer() {
        // Add entry
        mMessagingLog.addFileTransfer(mFileTransferId, mContact, Direction.OUTGOING, CONTENT, null,
                State.INITIATING, ReasonCode.UNSPECIFIED, mTimestamp, mTimestampSent,
                mFileExpiration, FileTransferLog.UNKNOWN_EXPIRATION);
        // Read entry
        Uri uri = Uri.withAppendedPath(FileTransferLog.CONTENT_URI, mFileTransferId);
        Cursor cursor = mContentResolver.query(uri, null, null, null, null);
        // Check entry
        assertEquals(1, cursor.getCount());
        assertTrue(cursor.moveToFirst());
        String id = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferLog.FT_ID));
        String chatId = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferLog.CHAT_ID));
        String contact = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferLog.CONTACT));
        String fileUri = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferLog.FILE));
        String filename = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferLog.FILENAME));
        String fileMimeType = cursor.getString(cursor
                .getColumnIndexOrThrow(FileTransferLog.MIME_TYPE));
        int direction = cursor.getInt(cursor.getColumnIndexOrThrow(FileTransferLog.DIRECTION));
        long transferred = cursor
                .getLong(cursor.getColumnIndexOrThrow(FileTransferLog.TRANSFERRED));
        long size = cursor.getLong(cursor.getColumnIndexOrThrow(FileTransferLog.FILESIZE));
        String icon = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferLog.FILEICON));
        String iconMimeType = cursor.getString(cursor
                .getColumnIndexOrThrow(FileTransferLog.FILEICON_MIME_TYPE));
        long iconExpiration = cursor.getLong(cursor
                .getColumnIndexOrThrow(FileTransferLog.FILEICON_EXPIRATION));
        int readStatus = cursor.getInt(cursor.getColumnIndexOrThrow(FileTransferLog.READ_STATUS));
        int state = cursor.getInt(cursor.getColumnIndexOrThrow(FileTransferLog.STATE));
        int reason = cursor.getInt(cursor.getColumnIndexOrThrow(FileTransferLog.REASON_CODE));
        long fileExpiration = cursor.getLong(cursor
                .getColumnIndexOrThrow(FileTransferLog.FILE_EXPIRATION));
        long timestampDelivered = cursor.getLong(cursor
                .getColumnIndexOrThrow(FileTransferLog.TIMESTAMP_DELIVERED));
        long timestampDisplayed = cursor.getLong(cursor
                .getColumnIndexOrThrow(FileTransferLog.TIMESTAMP_DISPLAYED));

        assertEquals(mFileTransferId, id);
        assertEquals(mContact.toString(), chatId);
        assertEquals(mContact.toString(), contact);
        assertEquals(IMAGE_URI, fileUri);
        assertEquals(IMAGE_FILENAME, filename);
        assertEquals(MimeManager.getInstance().getMimeType(IMAGE_FILENAME), fileMimeType);
        assertEquals(Direction.OUTGOING.toInt(), direction);
        assertEquals(0, transferred);
        assertEquals(IMAGE_FILESIZE, size);
        assertEquals(null, icon);
        assertEquals(null, iconMimeType);
        assertEquals(FileTransferLog.UNKNOWN_EXPIRATION, iconExpiration);
        assertEquals(ReadStatus.UNREAD.toInt(), readStatus);
        assertEquals(FileTransfer.State.INITIATING.toInt(), state);
        assertEquals(FileTransfer.ReasonCode.UNSPECIFIED.toInt(), reason);
        assertEquals(mFileExpiration, fileExpiration);
        assertEquals(0L, timestampDelivered);
        assertEquals(0L, timestampDisplayed);

        assertEquals(true, mMessagingLog.isFileTransfer(mFileTransferId));
        assertEquals(false, mMessagingLog.isGroupFileTransfer(mFileTransferId));
        assertEquals(FileTransfer.State.INITIATING,
                mMessagingLog.getFileTransferState(mFileTransferId));
        assertEquals(FileTransfer.ReasonCode.UNSPECIFIED,
                mMessagingLog.getFileTransferStateReasonCode(mFileTransferId));

        mLocalContentResolver.delete(
                Uri.withAppendedPath(FileTransferData.CONTENT_URI, mFileTransferId), null, null);
        assertEquals(false, mMessagingLog.isFileTransfer(mFileTransferId));
    }

    public void testAddOutgoingGroupFileTransfer() {
        // Add entry
        Map<ContactId, GroupChat.ParticipantStatus> participants = new HashMap<ContactId, GroupChat.ParticipantStatus>();
        participants.put(mContact, GroupChat.ParticipantStatus.INVITING);
        mMessagingLog.addGroupChat(mChatId, null, null, participants, GroupChat.State.INITIATING,
                GroupChat.ReasonCode.UNSPECIFIED, Direction.OUTGOING, mTimestamp);
        mMessagingLog.addOutgoingGroupFileTransfer(mFileTransferId, mChatId, CONTENT, ICON_CONTENT,
                State.INITIATING, ReasonCode.UNSPECIFIED, mTimestamp, mTimestampSent);
        // Read entry
        Uri uri = Uri.withAppendedPath(FileTransferLog.CONTENT_URI, mFileTransferId);
        Cursor cursor = mContentResolver.query(uri, null, null, null, null);
        // Check entry
        assertEquals(1, cursor.getCount());
        assertTrue(cursor.moveToFirst());
        String id = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferLog.FT_ID));
        String chatId = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferLog.CHAT_ID));
        String contact = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferLog.CONTACT));
        String fileUri = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferLog.FILE));
        String filename = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferLog.FILENAME));
        String fileMimeType = cursor.getString(cursor
                .getColumnIndexOrThrow(FileTransferLog.MIME_TYPE));
        int direction = cursor.getInt(cursor.getColumnIndexOrThrow(FileTransferLog.DIRECTION));
        long transferred = cursor
                .getLong(cursor.getColumnIndexOrThrow(FileTransferLog.TRANSFERRED));
        long size = cursor.getLong(cursor.getColumnIndexOrThrow(FileTransferLog.FILESIZE));
        String icon = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferLog.FILEICON));
        String iconMimeType = cursor.getString(cursor
                .getColumnIndexOrThrow(FileTransferLog.FILEICON_MIME_TYPE));
        long iconExpiration = cursor.getLong(cursor
                .getColumnIndexOrThrow(FileTransferLog.FILEICON_EXPIRATION));
        int readStatus = cursor.getInt(cursor.getColumnIndexOrThrow(FileTransferLog.READ_STATUS));
        int state = cursor.getInt(cursor.getColumnIndexOrThrow(FileTransferLog.STATE));
        int reason = cursor.getInt(cursor.getColumnIndexOrThrow(FileTransferLog.REASON_CODE));
        long fileExpiration = cursor.getLong(cursor
                .getColumnIndexOrThrow(FileTransferLog.FILE_EXPIRATION));
        long timestampDelivered = cursor.getLong(cursor
                .getColumnIndexOrThrow(FileTransferLog.TIMESTAMP_DELIVERED));
        long timestampDisplayed = cursor.getLong(cursor
                .getColumnIndexOrThrow(FileTransferLog.TIMESTAMP_DISPLAYED));

        assertEquals(mFileTransferId, id);
        assertEquals(mChatId, chatId);
        assertEquals(null, contact);
        assertEquals(IMAGE_URI, fileUri);
        assertEquals(IMAGE_FILENAME, filename);
        assertEquals(MimeManager.getInstance().getMimeType(IMAGE_FILENAME), fileMimeType);
        assertEquals(Direction.OUTGOING.toInt(), direction);
        assertEquals(0, transferred);
        assertEquals(IMAGE_FILESIZE, size);
        assertEquals(IMAGE_ICON_URI, icon);
        assertEquals(MimeManager.getInstance().getMimeType(IMAGE_ICON_FILENAME), iconMimeType);
        assertEquals(FileTransferLog.UNKNOWN_EXPIRATION, iconExpiration);
        assertEquals(ReadStatus.UNREAD.toInt(), readStatus);
        assertEquals(FileTransfer.State.INITIATING.toInt(), state);
        assertEquals(FileTransfer.ReasonCode.UNSPECIFIED.toInt(), reason);
        assertEquals(FileTransferLog.UNKNOWN_EXPIRATION, fileExpiration);
        assertEquals(0L, timestampDelivered);
        assertEquals(0L, timestampDisplayed);

        assertEquals(true, mMessagingLog.isFileTransfer(mFileTransferId));
        assertEquals(true, mMessagingLog.isGroupFileTransfer(mFileTransferId));
        assertEquals(FileTransfer.State.INITIATING,
                mMessagingLog.getFileTransferState(mFileTransferId));
        assertEquals(FileTransfer.ReasonCode.UNSPECIFIED,
                mMessagingLog.getFileTransferStateReasonCode(mFileTransferId));
        mLocalContentResolver.delete(
                Uri.withAppendedPath(FileTransferData.CONTENT_URI, mFileTransferId), null, null);
        assertEquals(false, mMessagingLog.isFileTransfer(mFileTransferId));
    }

    public void testAddIncomingGroupFileTransfer() {
        long fileIconExpiration = mRandom.nextLong();
        // Add entry
        mMessagingLog.addIncomingGroupFileTransfer(mFileTransferId, mChatId, mContact, CONTENT,
                ICON_CONTENT, State.ACCEPTING, ReasonCode.UNSPECIFIED, mTimestamp, mTimestampSent,
                mFileExpiration, fileIconExpiration);
        // Read entry
        Uri uri = Uri.withAppendedPath(FileTransferLog.CONTENT_URI, mFileTransferId);
        Cursor cursor = mContentResolver.query(uri, null, null, null, null);
        // Check entry
        assertEquals(1, cursor.getCount());
        assertTrue(cursor.moveToFirst());
        String id = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferLog.FT_ID));
        String chatId = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferLog.CHAT_ID));
        String contact = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferLog.CONTACT));
        String fileUri = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferLog.FILE));
        String filename = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferLog.FILENAME));
        String fileMimeType = cursor.getString(cursor
                .getColumnIndexOrThrow(FileTransferLog.MIME_TYPE));
        int direction = cursor.getInt(cursor.getColumnIndexOrThrow(FileTransferLog.DIRECTION));
        long transferred = cursor
                .getLong(cursor.getColumnIndexOrThrow(FileTransferLog.TRANSFERRED));
        long size = cursor.getLong(cursor.getColumnIndexOrThrow(FileTransferLog.FILESIZE));
        String icon = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferLog.FILEICON));
        String iconMimeType = cursor.getString(cursor
                .getColumnIndexOrThrow(FileTransferLog.FILEICON_MIME_TYPE));
        long _fileIconExpiration = cursor.getLong(cursor
                .getColumnIndexOrThrow(FileTransferLog.FILEICON_EXPIRATION));
        int readStatus = cursor.getInt(cursor.getColumnIndexOrThrow(FileTransferLog.READ_STATUS));
        int state = cursor.getInt(cursor.getColumnIndexOrThrow(FileTransferLog.STATE));
        int reason = cursor.getInt(cursor.getColumnIndexOrThrow(FileTransferLog.REASON_CODE));
        long _fileExpiration = cursor.getLong(cursor
                .getColumnIndexOrThrow(FileTransferLog.FILE_EXPIRATION));
        long timestampDelivered = cursor.getLong(cursor
                .getColumnIndexOrThrow(FileTransferLog.TIMESTAMP_DELIVERED));
        long timestampDisplayed = cursor.getLong(cursor
                .getColumnIndexOrThrow(FileTransferLog.TIMESTAMP_DISPLAYED));

        assertEquals(mFileTransferId, id);
        assertEquals(mChatId, chatId);
        assertEquals(mContact.toString(), contact);
        assertEquals(IMAGE_URI, fileUri);
        assertEquals(IMAGE_FILENAME, filename);
        assertEquals(MimeManager.getInstance().getMimeType(IMAGE_FILENAME), fileMimeType);
        assertEquals(Direction.INCOMING.toInt(), direction);
        assertEquals(0, transferred);
        assertEquals(IMAGE_FILESIZE, size);
        assertEquals(IMAGE_ICON_URI, icon);
        assertEquals(MimeManager.getInstance().getMimeType(IMAGE_ICON_FILENAME), iconMimeType);
        assertEquals(fileIconExpiration, _fileIconExpiration);
        assertEquals(ReadStatus.UNREAD.toInt(), readStatus);
        assertEquals(FileTransfer.State.ACCEPTING.toInt(), state);
        assertEquals(FileTransfer.ReasonCode.UNSPECIFIED.toInt(), reason);
        assertEquals(mFileExpiration, _fileExpiration);
        assertEquals(0L, timestampDelivered);
        assertEquals(0L, timestampDisplayed);

        assertEquals(true, mMessagingLog.isFileTransfer(mFileTransferId));
        assertEquals(true, mMessagingLog.isGroupFileTransfer(mFileTransferId));
        assertEquals(FileTransfer.State.ACCEPTING,
                mMessagingLog.getFileTransferState(mFileTransferId));
        assertEquals(FileTransfer.ReasonCode.UNSPECIFIED,
                mMessagingLog.getFileTransferStateReasonCode(mFileTransferId));
        mLocalContentResolver.delete(
                Uri.withAppendedPath(FileTransferData.CONTENT_URI, mFileTransferId), null, null);
        assertEquals(false, mMessagingLog.isFileTransfer(mFileTransferId));
    }
}
