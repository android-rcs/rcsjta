
package com.orangelabs.rcs.ri.history.messaging;

import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransferLog;
import com.gsma.services.rcs.history.HistoryLog;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.history.HistoryListView;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Messaging conversation log
 */
public class MessagingListView extends HistoryListView {

    private final static String[] PROJECTION_GROUP_CHAT = new String[] {
            ChatLog.GroupChat.CHAT_ID, ChatLog.GroupChat.SUBJECT, ChatLog.GroupChat.DIRECTION,
            ChatLog.GroupChat.STATE, ChatLog.GroupChat.TIMESTAMP
    };
    private final static String SORT_ORDER_GROUP_CHAT = new StringBuilder(
            ChatLog.GroupChat.TIMESTAMP).append(" DESC").toString();

    /**
     * WHERE mime_type!='rcs/groupchat-event' group by chat_id
     */
    private final static String WHERE_CLAUSE = new StringBuilder(HistoryLog.MIME_TYPE)
            .append("!='").append(ChatLog.Message.MimeType.GROUPCHAT_EVENT).append("' group by ")
            .append(HistoryLog.PROVIDER_ID).append(",").append(HistoryLog.CHAT_ID).toString();

    /**
     * Associate the providers name menu with providerIds defined in HistoryLog
     */
    private final static TreeMap<Integer, String> sProviders = new TreeMap<Integer, String>();

    /* mapping chat_id / group chat info */
    private final Map<String, MessagingLogInfos> mGroupChatMap = new HashMap<String, MessagingLogInfos>();

    private ArrayAdapter<MessagingLogInfos> mArrayAdapter;

    private final List<MessagingLogInfos> mMessagingLogInfos = new ArrayList<MessagingLogInfos>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_log_messaging);
        mArrayAdapter = new MessagingLogAdapter(this);
        TextView emptyView = (TextView) findViewById(android.R.id.empty);
        ListView view = (ListView) findViewById(android.R.id.list);
        view.setEmptyView(emptyView);
        view.setAdapter(mArrayAdapter);
        getGroupChatInfos();

        sProviders.put(ChatLog.Message.HISTORYLOG_MEMBER_ID,
                getString(R.string.label_history_log_menu_chat));
        sProviders.put(FileTransferLog.HISTORYLOG_MEMBER_ID,
                getString(R.string.label_history_log_menu_file_transfer));
        setProviders(sProviders);

        startQuery();
    }

    /**
     * Messaging log adapter
     */
    private class MessagingLogAdapter extends ArrayAdapter<MessagingLogInfos> {
        private Context mContext;

        private Drawable mDrawableIncomingFailed;
        private Drawable mDrawableOutgoingFailed;
        private Drawable mDrawableIncoming;
        private Drawable mDrawableOutgoing;
        private Drawable mDrawableChat;
        private Drawable mDrawableFileTransfer;

        public MessagingLogAdapter(Context context) {
            super(context, R.layout.history_log_list, mMessagingLogInfos);

            // Load the drawables
            mDrawableIncomingFailed = context.getResources().getDrawable(
                    R.drawable.ri_historylog_list_incoming_call_failed);
            mDrawableOutgoingFailed = context.getResources().getDrawable(
                    R.drawable.ri_historylog_list_outgoing_call_failed);
            mDrawableIncoming = context.getResources().getDrawable(
                    R.drawable.ri_historylog_list_incoming_call);
            mDrawableOutgoing = context.getResources().getDrawable(
                    R.drawable.ri_historylog_list_outgoing_call);
            mDrawableChat = context.getResources().getDrawable(R.drawable.ri_historylog_chat);
            mDrawableFileTransfer = context.getResources().getDrawable(
                    R.drawable.ri_historylog_filetransfer);

            mContext = context;
        }

        private class ViewHolder {

            TextView mConversationType;
            TextView mConversationLabel;
            TextView mDescription;
            TextView mDate;
            ImageView mEventDirection;
            ImageView mEvent;

            ViewHolder(View view) {
                mConversationType = (TextView) view.findViewById(R.id.conversation_type);
                mConversationLabel = (TextView) view.findViewById(R.id.conversation_label);
                mDescription = (TextView) view.findViewById(R.id.description);
                mDate = (TextView) view.findViewById(R.id.date);
                mEventDirection = (ImageView) view.findViewById(R.id.call_type_icon);
                mEvent = (ImageView) view.findViewById(R.id.call_icon);
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.history_log_list, parent, false);
                viewHolder = new ViewHolder(convertView);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            MessagingLogInfos item = mMessagingLogInfos.get(position);
            int providerId = item.getProviderId();

            // Set the date/time field by mixing relative and absolute times
            long date = item.getTimestamp();
            viewHolder.mDate.setText(DateUtils.getRelativeTimeSpanString(date,
                    System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE));

            // Set the status text and destination icon
            int status = item.getStatus();
            switch (item.getDirection()) {
                case INCOMING:
                    if (status == ChatLog.Message.Content.Status.FAILED.toInt()
                            || status == FileTransfer.State.FAILED.toInt()) {
                        viewHolder.mEventDirection.setImageDrawable(mDrawableIncomingFailed);
                    } else {
                        viewHolder.mEventDirection.setImageDrawable(mDrawableIncoming);
                    }
                    break;
                case OUTGOING:
                    if (status == ChatLog.Message.Content.Status.FAILED.toInt()
                            || status == FileTransfer.State.FAILED.toInt()) {
                        viewHolder.mEventDirection.setImageDrawable(mDrawableOutgoingFailed);
                    } else {
                        viewHolder.mEventDirection.setImageDrawable(mDrawableOutgoing);
                    }
                case IRRELEVANT:
                    break;
            }

            String contact = item.getContact();
            String chat_id = item.getChatId();
            boolean isOnetoOneConversation = chat_id.equals(contact);
            if (isOnetoOneConversation) {
                viewHolder.mConversationType
                        .setText(R.string.label_history_log_single_conversation);
                viewHolder.mConversationLabel.setText(contact);
            } else {
                viewHolder.mConversationType.setText(R.string.label_history_log_group_conversation);
                String subject = item.getSubject();
                viewHolder.mConversationLabel.setText((TextUtils.isEmpty(subject)) ? "" : truncateString(
                        subject, MAX_LENGTH_SUBJECT));
            }

            if (ChatLog.Message.HISTORYLOG_MEMBER_ID == providerId) {
                viewHolder.mEvent.setImageDrawable(mDrawableChat);
                String content = item.getContent();
                viewHolder.mDescription.setText(TextUtils.isEmpty(content) ? "" : truncateString(content,
                        MAX_LENGTH_DESCRIPTION));
            } else if (FileTransferLog.HISTORYLOG_MEMBER_ID == providerId) {
                viewHolder.mEvent.setImageDrawable(mDrawableFileTransfer);
                String filename = item.getFilename();
                viewHolder.mDescription.setText(TextUtils.isEmpty(filename) ? "" : truncateString(filename,
                        MAX_LENGTH_DESCRIPTION));
            }
            return convertView;
        }
    }

    @Override
    protected void startQuery() {
        List<Integer> providers = getSelectedProviderIds();
        Map<String, MessagingLogInfos> dataMap = new HashMap<String, MessagingLogInfos>();
        if (!providers.isEmpty()) {
            Uri uri = createHistoryUri(providers);
            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(uri, null, WHERE_CLAUSE, null, SORT_BY);

                int columnChatId = cursor.getColumnIndexOrThrow(HistoryLog.CHAT_ID);
                int columnTimestamp = cursor.getColumnIndexOrThrow(HistoryLog.TIMESTAMP);
                int columnProviderId = cursor.getColumnIndexOrThrow(HistoryLog.PROVIDER_ID);
                int columnDirection = cursor.getColumnIndexOrThrow(HistoryLog.DIRECTION);
                int columnContact = cursor.getColumnIndexOrThrow(HistoryLog.CONTACT);
                int columnContent = cursor.getColumnIndexOrThrow(HistoryLog.CONTENT);
                int columnFilename = cursor.getColumnIndexOrThrow(HistoryLog.FILENAME);
                int columnStatus = cursor.getColumnIndexOrThrow(HistoryLog.STATUS);
                while (cursor.moveToNext()) {
                    String chatId = cursor.getString(columnChatId);
                    long timestamp = cursor.getLong(columnTimestamp);
                    /*
                     * We may have both GC and FT messages for the same chat ID. Only keep the most
                     * recent one.
                     */
                    MessagingLogInfos infos = dataMap.get(chatId);
                    if (infos != null && timestamp < infos.getTimestamp()) {
                        continue;
                    }
                    dataMap.put(
                            chatId,
                            new MessagingLogInfos(cursor.getInt(columnProviderId), timestamp,
                                    cursor.getInt(columnStatus), Direction.valueOf(cursor
                                            .getInt(columnDirection)), cursor
                                            .getString(columnContact), chatId, cursor
                                            .getString(columnContent), cursor
                                            .getString(columnFilename)));
                }

            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            if (getSelectedProviderIds().contains(ChatLog.Message.HISTORYLOG_MEMBER_ID)) {
                for (Entry<String, MessagingLogInfos> groupChat : mGroupChatMap.entrySet()) {
                    String chatId = groupChat.getKey();
                    MessagingLogInfos groupChatInfos = groupChat.getValue();
                    MessagingLogInfos messageInfos = dataMap.get(chatId);
                    if (messageInfos == null) {
                        /* Add group chat entries if there is no associated message */
                        dataMap.put(chatId, groupChatInfos);
                    } else {
                        /* update subject if message exists */
                        messageInfos.setSubject(groupChatInfos.getSubject());
                    }
                }
            }
        }

        mMessagingLogInfos.clear();
        mMessagingLogInfos.addAll(dataMap.values());
        Collections.sort(mMessagingLogInfos);
        mArrayAdapter.notifyDataSetChanged();
    }

    private void getGroupChatInfos() {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(ChatLog.GroupChat.CONTENT_URI,
                    PROJECTION_GROUP_CHAT, null, null, SORT_ORDER_GROUP_CHAT);
            if (cursor == null) {
                return;
            }
            int columnChatId = cursor.getColumnIndexOrThrow(ChatLog.GroupChat.CHAT_ID);
            int columnSubject = cursor.getColumnIndexOrThrow(ChatLog.GroupChat.SUBJECT);
            int columnTimestamp = cursor.getColumnIndexOrThrow(ChatLog.GroupChat.TIMESTAMP);
            int columnDirection = cursor.getColumnIndexOrThrow(ChatLog.GroupChat.DIRECTION);
            int columnStatus = cursor.getColumnIndexOrThrow(ChatLog.GroupChat.STATE);
            while (cursor.moveToNext()) {
                String chatId = cursor.getString(columnChatId);
                mGroupChatMap.put(
                        chatId,
                        new MessagingLogInfos(cursor.getLong(columnTimestamp), cursor
                                .getInt(columnStatus), Direction.valueOf(cursor
                                .getInt(columnDirection)), cursor.getString(columnChatId), cursor
                                .getString(columnSubject)));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private class MessagingLogInfos implements Comparable<MessagingLogInfos> {

        private int mProviderId;
        private long mTimestamp;
        private int mStatus;
        private Direction mDirection;
        private String mContact;
        private String mChatId;
        private String mContent;
        private String mFilename;
        private String mSubject;

        public MessagingLogInfos(long timestamp, int status, Direction direction, String chatId,
                String subject) {
            super();
            mTimestamp = timestamp;
            mStatus = status;
            mDirection = direction;
            mChatId = chatId;
            mSubject = subject;
            mProviderId = ChatLog.Message.HISTORYLOG_MEMBER_ID;
        }

        public MessagingLogInfos(int providerId, long timestamp, int status, Direction direction,
                String contact, String chatId, String content, String filename) {
            super();
            mProviderId = providerId;
            mTimestamp = timestamp;
            mStatus = status;
            mDirection = direction;
            mContact = contact;
            mChatId = chatId;
            mContent = content;
            mFilename = filename;
        }

        public int getProviderId() {
            return mProviderId;
        }

        public long getTimestamp() {
            return mTimestamp;
        }

        public int getStatus() {
            return mStatus;
        }

        public Direction getDirection() {
            return mDirection;
        }

        public String getContact() {
            return mContact;
        }

        public String getChatId() {
            return mChatId;
        }

        public String getContent() {
            return mContent;
        }

        public String getFilename() {
            return mFilename;
        }

        /**
         * @return the mSubject
         */
        public String getSubject() {
            return mSubject;
        }

        public void setSubject(String subject) {
            mSubject = subject;
        }

        @Override
        public int compareTo(MessagingLogInfos another) {
            return (int) (another.getTimestamp() - mTimestamp);
        }

    }
}
