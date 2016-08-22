/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.ri.messaging.chat;

import com.gsma.rcs.api.connection.utils.RcsActivity;
import com.gsma.rcs.ri.R;
import com.gsma.rcs.ri.RiApplication;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A class to view the persisted information for chat message<br>
 * Created by Philippe LEMORDANT.
 */
public class ChatMessageLogView extends RcsActivity {
    private static final String EXTRA_MESSAGE_ID = "id";
    private String mMessageId;
    private TextView mTxtViewChatId;
    private TextView mTxtViewContact;
    private TextView mTxtViewContent;
    private TextView mTxtViewDate;
    private TextView mTxtViewDir;
    private TextView mTxtViewMime;
    private TextView mTxtViewReason;
    private TextView mTxtViewState;
    private TextView mTxtViewDateSent;
    private TextView mTxtViewDateDelivered;
    private TextView mTxtViewDateDisplayed;
    private TextView mTxtViewRead;
    private TextView mTxtViewExpiredDelivery;

    private static DateFormat sDateFormat;
    private TextView mTxtViewId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_message_log_item);
        mMessageId = getIntent().getStringExtra(EXTRA_MESSAGE_ID);
        initialize();
    }

    private void initialize() {
        mTxtViewId = (TextView) findViewById(R.id.history_log_item_id);
        mTxtViewChatId = (TextView) findViewById(R.id.history_log_item_chat_id);
        mTxtViewContact = (TextView) findViewById(R.id.history_log_item_contact);
        mTxtViewState = (TextView) findViewById(R.id.history_log_item_state);
        mTxtViewReason = (TextView) findViewById(R.id.history_log_item_reason);
        mTxtViewDir = (TextView) findViewById(R.id.history_log_item_direction);
        mTxtViewDate = (TextView) findViewById(R.id.history_log_item_date);
        mTxtViewMime = (TextView) findViewById(R.id.history_log_item_mime);
        mTxtViewContent = (TextView) findViewById(R.id.history_log_item_content);
        mTxtViewDateSent = (TextView) findViewById(R.id.history_log_item_date_sent);
        mTxtViewDateDelivered = (TextView) findViewById(R.id.history_log_item_date_delivered);
        mTxtViewDateDisplayed = (TextView) findViewById(R.id.history_log_item_date_displayed);
        mTxtViewRead = (TextView) findViewById(R.id.history_log_item_read_status);
        mTxtViewExpiredDelivery = (TextView) findViewById(R.id.history_log_item_expired_delivery);
    }

    private String getDateFromDb(long timestamp) {
        if (0 == timestamp) {
            return "";
        }
        if (sDateFormat == null) {
            sDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());
        }
        return sDateFormat.format(new Date(timestamp));
    }

    @Override
    protected void onResume() {
        super.onResume();
        ChatMessageDAO dao = ChatMessageDAO.getChatMessageDAO(this, mMessageId);
        if (dao == null) {
            showMessageThenExit(R.string.error_item_not_found);
            return;
        }
        mTxtViewId.setText(mMessageId);
        mTxtViewChatId.setText(dao.getChatId());
        ContactId contact = dao.getContact();
        if (contact != null) {
            mTxtViewContact.setText(contact.toString());
        } else {
            mTxtViewContact.setText("");
        }
        String mime = dao.getMimeType();
        if (ChatLog.Message.MimeType.GROUPCHAT_EVENT.equals(mime)) {
            mTxtViewState.setText(RiApplication.sGroupChatEvents[dao.getChatEvent().toInt()]);
            mTxtViewContent.setText("");
            mTxtViewReason.setText("");
        } else {
            mTxtViewState.setText(RiApplication.sMessagesStatuses[dao.getStatus().toInt()]);
            mTxtViewReason.setText(RiApplication.sMessageReasonCodes[dao.getReasonCode().toInt()]);
            mTxtViewContent.setText(dao.getContent());
        }
        mTxtViewDir.setText(RiApplication.getDirection(dao.getDirection()));
        mTxtViewDate.setText(getDateFromDb(dao.getTimestamp()));
        mTxtViewDateSent.setText(getDateFromDb(dao.getTimestampSent()));
        mTxtViewDateDelivered.setText(getDateFromDb(dao.getTimestampDelivered()));
        mTxtViewDateDisplayed.setText(getDateFromDb(dao.getTimestampDisplayed()));
        mTxtViewRead.setText(dao.getReadStatus().toString());
        mTxtViewExpiredDelivery.setText(Boolean.toString(dao.isExpiredDelivery()));
        mTxtViewMime.setText(mime);
    }

    /**
     * Start activity to view details of chat message record
     *
     * @param context the context
     * @param messageId the message ID
     */
    public static void startActivity(Context context, String messageId) {
        Intent intent = new Intent(context, ChatMessageLogView.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(EXTRA_MESSAGE_ID, messageId);
        context.startActivity(intent);
    }
}
