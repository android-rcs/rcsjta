
package com.orangelabs.rcs.ri.history.messaging;

import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatLog.Message.Content;
import com.gsma.services.rcs.chat.ChatService;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.chat.GroupChatListener;
import com.gsma.services.rcs.chat.OneToOneChatListener;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransferLog;
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo;
import com.gsma.services.rcs.history.HistoryLog;

import com.orangelabs.rcs.ri.ConnectionManager;
import com.orangelabs.rcs.ri.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.history.HistoryListView;
import com.orangelabs.rcs.ri.messaging.filetransfer.multi.SendMultiFile;
import com.orangelabs.rcs.ri.utils.ContactUtil;
import com.orangelabs.rcs.ri.utils.LogUtils;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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

    /**
     * List of items for contextual menu
     */
    private static final int CHAT_MENU_ITEM_DELETE = 0;

    /* mapping chat_id / group chat info */
    private final Map<String, MessagingLogInfo> mGroupChatMap = new HashMap<String, MessagingLogInfo>();

    private ArrayAdapter<MessagingLogInfo> mArrayAdapter;

    private final List<MessagingLogInfo> mMessagingLogInfos = new ArrayList<MessagingLogInfo>();

    private boolean mSendFile = false;

    private boolean mOneToOneChatListenerSet = false;

    private boolean mGroupChatListenerSet = false;

    private ChatService mChatService;

    private ConnectionManager mCnxManager;

    private static final String LOGTAG = LogUtils.getTag(MessagingListView.class.getSimpleName());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_log_messaging);

        mCnxManager = ConnectionManager.getInstance();

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

        mSendFile = getIntent().getAction() != null;
        if (mSendFile) {
            view.setOnItemClickListener(getOnItemClickListener());
        } else {
            registerForContextMenu(view);
        }
        queryHistoryLogAndRefreshView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mChatService == null) {
            return;
        }
        try {
            if (mOneToOneChatListenerSet) {
                mChatService.removeEventListener(mOneChatListener);
            }
            if (mGroupChatListenerSet) {
                mChatService.removeEventListener(mGroupChatListener);
            }
        } catch (Exception e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "removeEventListener failed", e);
            }
        }
    }

    /**
     * Messaging log adapter
     */
    private class MessagingLogAdapter extends ArrayAdapter<MessagingLogInfo> {
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

            MessagingLogInfo item = mMessagingLogInfos.get(position);
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

            if (item.isSingleChat()) {
                viewHolder.mConversationType
                        .setText(R.string.label_history_log_single_conversation);
                viewHolder.mConversationLabel.setText(item.getContact().toString());
            } else {
                viewHolder.mConversationType.setText(R.string.label_history_log_group_conversation);
                String subject = item.getSubject();
                viewHolder.mConversationLabel.setText((TextUtils.isEmpty(subject)) ? ""
                        : truncateString(subject, MAX_LENGTH_SUBJECT));
            }

            if (ChatLog.Message.HISTORYLOG_MEMBER_ID == providerId) {
                viewHolder.mEvent.setImageDrawable(mDrawableChat);
                String content = item.getContent();
                viewHolder.mDescription.setText(TextUtils.isEmpty(content) ? "" : truncateString(
                        content, MAX_LENGTH_DESCRIPTION));
            } else if (FileTransferLog.HISTORYLOG_MEMBER_ID == providerId) {
                viewHolder.mEvent.setImageDrawable(mDrawableFileTransfer);
                String filename = item.getFilename();
                viewHolder.mDescription.setText(TextUtils.isEmpty(filename) ? "" : truncateString(
                        filename, MAX_LENGTH_DESCRIPTION));
            }
            return convertView;
        }
    }

    @Override
    protected void queryHistoryLogAndRefreshView() {
        List<Integer> providers = getSelectedProviderIds();
        Map<String, MessagingLogInfo> dataMap = new HashMap<String, MessagingLogInfo>();
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
                    MessagingLogInfo infos = dataMap.get(chatId);
                    if (infos != null && timestamp < infos.getTimestamp()) {
                        continue;
                    }
                    String phoneNumber = cursor.getString(columnContact);
                    ContactId contact = null;
                    if (phoneNumber != null) {
                        contact = ContactUtil.formatContact(phoneNumber);
                    }
                    dataMap.put(
                            chatId,
                            new MessagingLogInfo(cursor.getInt(columnProviderId), timestamp, cursor
                                    .getInt(columnStatus), Direction.valueOf(cursor
                                    .getInt(columnDirection)), contact, chatId, cursor
                                    .getString(columnContent), cursor.getString(columnFilename)));
                }

            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            for (Entry<String, MessagingLogInfo> groupChat : mGroupChatMap.entrySet()) {
                String chatId = groupChat.getKey();
                MessagingLogInfo groupChatInfos = groupChat.getValue();
                MessagingLogInfo messageInfos = dataMap.get(chatId);
                if (messageInfos == null) {
                    if (mSendFile) {
                        /* Add group chat entries if there is no associated message */
                        dataMap.put(chatId, groupChatInfos);
                    }
                } else {
                    /* update subject if message exists */
                    messageInfos.setSubject(groupChatInfos.getSubject());
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
                        new MessagingLogInfo(cursor.getLong(columnTimestamp), cursor
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

    private OnItemClickListener getOnItemClickListener() {
        return new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
                // Get selected item
                MessagingLogInfo chatLogInfo = (MessagingLogInfo) (parent.getAdapter())
                        .getItem(pos);
                // Open multiple file transfer
                SendMultiFile.startActivity(MessagingListView.this, getIntent(),
                        chatLogInfo.isSingleChat(), chatLogInfo.getChatId());
            }
        };
    }

    /**
     * A POJO class to hold the messaging log information.
     */
    public class MessagingLogInfo implements Comparable<MessagingLogInfo> {

        private final int mProviderId;
        private final long mTimestamp;
        private final int mStatus;
        private final Direction mDirection;
        private final ContactId mContact;
        private final String mChatId;
        private final String mContent;
        private final String mFilename;
        private String mSubject;

        /**
         * Constructor for group chat information
         * 
         * @param timestamp the timestamp
         * @param state the group chat state
         * @param direction the direction
         * @param chatId the chat ID
         * @param subject the subject (or null if not defined)
         */
        public MessagingLogInfo(long timestamp, int state, Direction direction, String chatId,
                String subject) {
            super();
            mTimestamp = timestamp;
            mStatus = state;
            mDirection = direction;
            mChatId = chatId;
            mSubject = subject;
            mProviderId = ChatLog.Message.HISTORYLOG_MEMBER_ID;
            mContact = null;
            mContent = null;
            mFilename = null;
        }

        /**
         * Constructor for chat or file transfer message
         * 
         * @param providerId the provider ID
         * @param timestamp the timestamp
         * @param status the message status
         * @param direction the direction
         * @param contact the contact ID (may be null)
         * @param chatId the chat ID
         * @param content the message content
         * @param filename the filename
         */
        public MessagingLogInfo(int providerId, long timestamp, int status, Direction direction,
                ContactId contact, String chatId, String content, String filename) {
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

        /**
         * Gets the provide ID
         * 
         * @return the provide ID
         */
        public int getProviderId() {
            return mProviderId;
        }

        /**
         * Gets the timestamp
         * 
         * @return the timestamp
         */
        public long getTimestamp() {
            return mTimestamp;
        }

        /**
         * Gets the message / gc status
         * 
         * @return the message / gc status
         */
        public int getStatus() {
            return mStatus;
        }

        /**
         * Gets the direction
         * 
         * @return the direction
         */
        public Direction getDirection() {
            return mDirection;
        }

        /**
         * Gets the contact ID
         * 
         * @return the contact ID
         */
        public ContactId getContact() {
            return mContact;
        }

        /**
         * Gets the chat ID
         * 
         * @return the chat ID
         */
        public String getChatId() {
            return mChatId;
        }

        /**
         * Gets the message content
         * 
         * @return the message content
         */
        public String getContent() {
            return mContent;
        }

        /**
         * Gets the file name
         * 
         * @return the file name
         */
        public String getFilename() {
            return mFilename;
        }

        /**
         * Gets the group chat subject
         * 
         * @return the group chat subject
         */
        public String getSubject() {
            return mSubject;
        }

        /**
         * Checks if single chat
         * 
         * @return true if single chat
         */
        public boolean isSingleChat() {
            if (mContact == null) {
                return false;
            }
            return mChatId.equals(mContact.toString());
        }

        /**
         * Sets the group chat subject
         * 
         * @param subject the group chat subject
         */
        public void setSubject(String subject) {
            mSubject = subject;
        }

        @Override
        public int compareTo(MessagingLogInfo another) {
            return (int) (another.getTimestamp() - mTimestamp);
        }

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, CHAT_MENU_ITEM_DELETE, CHAT_MENU_ITEM_DELETE, R.string.menu_delete_chat_session);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        MessagingLogInfo chatInfo = (MessagingLogInfo) (mArrayAdapter.getItem(info.position));
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onContextItemSelected chatId=".concat(chatInfo.mChatId));
        }
        switch (item.getItemId()) {
            case CHAT_MENU_ITEM_DELETE:
                if (mCnxManager.isServiceConnected(RcsServiceName.CHAT)) {
                    if (mChatService == null) {
                        mChatService = mCnxManager.getChatApi();
                    }
                    boolean singleChat = chatInfo.isSingleChat();
                    try {
                        if (singleChat) {
                            if (!mOneToOneChatListenerSet) {
                                mChatService.addEventListener(mOneChatListener);
                                mOneToOneChatListenerSet = true;
                            }
                            ContactId contact = chatInfo.getContact();
                            if (LogUtils.isActive) {
                                Log.d(LOGTAG,
                                        "Delete messages for contact=".concat(contact.toString()));
                            }
                            mChatService.deleteOneToOneChat(contact);
                        } else {
                            if (!mGroupChatListenerSet) {
                                mChatService.addEventListener(mGroupChatListener);
                                mGroupChatListenerSet = true;
                            }
                            String chatId = chatInfo.getChatId();
                            if (LogUtils.isActive) {
                                Log.d(LOGTAG, "Delete Group chat chatId=".concat(chatId));
                            }
                            mChatService.deleteGroupChat(chatId);
                        }
                    } catch (Exception e) {
                        if (LogUtils.isActive) {
                            Log.e(LOGTAG, "delete chat session failed", e);
                        }
                    }
                }
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    MessagingLogInfo getmMessagingLogInfos(String chatId) {
        for (MessagingLogInfo messagingLogInfo : mMessagingLogInfos) {
            if (messagingLogInfo.getChatId().equals(chatId)) {
                return messagingLogInfo;
            }
        }
        return null;
    }

    OneToOneChatListener mOneChatListener = new OneToOneChatListener() {

        @Override
        public void onMessagesDeleted(ContactId contact, Set<String> msgIds) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG,
                        "onMessagesDeleted contact=" + contact + " for message IDs="
                                + Arrays.toString(msgIds.toArray()));
            }
            boolean refreshRequired = false;
            MessagingLogInfo messagingLogInfo = getmMessagingLogInfos(contact.toString());
            if (messagingLogInfo != null) {
                refreshRequired = true;
                mMessagingLogInfos.remove(messagingLogInfo);
            }
            if (refreshRequired) {
                mHandler.post(new Runnable() {
                    public void run() {
                        mArrayAdapter.notifyDataSetChanged();
                    }
                });
            }
        }

        @Override
        public void onMessageStatusChanged(ContactId contact, String mimeType, String msgId,
                Content.Status status, Content.ReasonCode reasonCode) {
        }

        @Override
        public void onComposingEvent(ContactId contact, boolean status) {
        }
    };

    GroupChatListener mGroupChatListener = new GroupChatListener() {

        @Override
        public void onMessagesDeleted(String chatId, Set<String> msgIds) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG,
                        "onMessagesDeleted chatId=" + chatId + " for message IDs="
                                + Arrays.toString(msgIds.toArray()));
            }
        }

        @Override
        public void onDeleted(Set<String> chatIds) {
            if (LogUtils.isActive) {
                Log.i(LOGTAG, "onDeleted chatIds=".concat(Arrays.toString(chatIds.toArray())));
            }
            boolean refresh = false;
            for (String chatId : chatIds) {
                MessagingLogInfo messagingLogInfo = getmMessagingLogInfos(chatId);
                if (messagingLogInfo != null) {
                    refresh = true;
                    mMessagingLogInfos.remove(messagingLogInfo);
                }
            }
            if (refresh) {
                mHandler.post(new Runnable() {
                    public void run() {
                        mArrayAdapter.notifyDataSetChanged();
                    }
                });
            }
        }

        @Override
        public void onComposingEvent(String chatId, ContactId contact, boolean status) {
        }

        @Override
        public void onMessageGroupDeliveryInfoChanged(String chatId, ContactId contact,
                String mimeType, String msgId, GroupDeliveryInfo.Status status,
                GroupDeliveryInfo.ReasonCode reasonCode) {
        }

        @Override
        public void onMessageStatusChanged(String chatId, String mimeType, String msgId,
                Content.Status status, Content.ReasonCode reasonCode) {
        }

        @Override
        public void onParticipantStatusChanged(String chatId, ContactId contact,
                ParticipantStatus status) {
        }

        @Override
        public void onStateChanged(String chatId, final GroupChat.State state,
                GroupChat.ReasonCode reasonCode) {
        }

    };
}
