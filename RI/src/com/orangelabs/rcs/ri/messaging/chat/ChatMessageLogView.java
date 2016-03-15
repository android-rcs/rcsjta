
package com.orangelabs.rcs.ri.messaging.chat;

import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.contact.ContactId;

import com.orangelabs.rcs.api.connection.utils.RcsActivity;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.one2one_chat_log_item);
        mMessageId = getIntent().getStringExtra(EXTRA_MESSAGE_ID);
        initialize();
    }

    private void initialize() {
        mTxtViewChatId = (TextView) findViewById(R.id.history_log_item_chat_id);
        mTxtViewContact = (TextView) findViewById(R.id.history_log_item_contact);
        mTxtViewState = (TextView) findViewById(R.id.history_log_item_state);
        mTxtViewReason = (TextView) findViewById(R.id.history_log_item_reason);
        mTxtViewDir = (TextView) findViewById(R.id.history_log_item_direction);
        mTxtViewDate = (TextView) findViewById(R.id.history_log_item_date);
        mTxtViewMime = (TextView) findViewById(R.id.history_log_item_mime);
        mTxtViewContent = (TextView) findViewById(R.id.history_log_item_content);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ChatMessageDAO dao = ChatMessageDAO.getChatMessageDAO(this, mMessageId);
        if (dao == null) {
            showMessageThenExit(R.string.error_item_not_found);
            return;
        }
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
        mTxtViewDate.setText(DateFormat.getInstance().format(new Date(dao.getTimestamp())));
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
