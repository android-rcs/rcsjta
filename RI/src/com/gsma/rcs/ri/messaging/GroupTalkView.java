/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010-2016 Orange.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.ri.messaging;

import com.gsma.rcs.api.connection.ConnectionManager;
import com.gsma.rcs.api.connection.utils.ExceptionUtil;
import com.gsma.rcs.api.connection.utils.RcsFragmentActivity;
import com.gsma.rcs.ri.R;
import com.gsma.rcs.ri.RI;
import com.gsma.rcs.ri.RiApplication;
import com.gsma.rcs.ri.messaging.adapter.TalkCursorAdapter;
import com.gsma.rcs.ri.messaging.chat.ChatCursorObserver;
import com.gsma.rcs.ri.messaging.chat.ChatMessageLogView;
import com.gsma.rcs.ri.messaging.chat.ChatPendingIntentManager;
import com.gsma.rcs.ri.messaging.chat.IsComposingManager;
import com.gsma.rcs.ri.messaging.chat.IsComposingManager.INotifyComposing;
import com.gsma.rcs.ri.messaging.chat.group.SendGroupFile;
import com.gsma.rcs.ri.messaging.filetransfer.FileTransferLogView;
import com.gsma.rcs.ri.messaging.geoloc.DisplayGeoloc;
import com.gsma.rcs.ri.messaging.geoloc.EditGeoloc;
import com.gsma.rcs.ri.utils.LogUtils;
import com.gsma.rcs.ri.utils.RcsContactUtil;
import com.gsma.rcs.ri.utils.Smileys;
import com.gsma.rcs.ri.utils.Utils;
import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsPersistentStorageException;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatLog.Message;
import com.gsma.services.rcs.chat.ChatLog.Message.Content;
import com.gsma.services.rcs.chat.ChatMessage;
import com.gsma.services.rcs.chat.ChatService;
import com.gsma.services.rcs.chat.ChatServiceConfiguration;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.chat.GroupChatIntent;
import com.gsma.services.rcs.chat.GroupChatListener;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;
import com.gsma.services.rcs.contact.RcsContact;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransferLog;
import com.gsma.services.rcs.filetransfer.FileTransferService;
import com.gsma.services.rcs.filetransfer.GroupFileTransferListener;
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo;
import com.gsma.services.rcs.history.HistoryLog;
import com.gsma.services.rcs.history.HistoryUriBuilder;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Group chat view
 */
public class GroupTalkView extends RcsFragmentActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * The loader's unique ID. Loader IDs are specific to the Activity in which they reside.
     */
    private static final int LOADER_ID = 1;

    private final static int SELECT_GEOLOCATION = 0;

    // @formatter:off
    private static final String[] PROJ_CHAT_MSG = new String[]{
            HistoryLog.BASECOLUMN_ID,
            HistoryLog.ID,
            HistoryLog.PROVIDER_ID,
            HistoryLog.MIME_TYPE,
            HistoryLog.CONTENT,
            HistoryLog.TIMESTAMP,
            HistoryLog.STATUS,
            HistoryLog.DIRECTION,
            HistoryLog.CONTACT,
            HistoryLog.EXPIRED_DELIVERY,
            HistoryLog.FILENAME,
            HistoryLog.FILESIZE,
            HistoryLog.TRANSFERRED,
            HistoryLog.REASON_CODE,
            HistoryLog.READ_STATUS};
    // @formatter:on

    /**
     * Query sort order
     */
    private final static String ORDER_CHAT_MSG = HistoryLog.TIMESTAMP + " ASC";

    /**
     * Intent parameters
     */
    private final static String EXTRA_PARTICIPANTS = "participants";
    private final static String EXTRA_SUBJECT = "subject";
    private final static String EXTRA_MODE = "mode";

    private enum GroupChatMode {
        INCOMING, OUTGOING, OPEN
    }

    private static final String WHERE_CLAUSE = HistoryLog.CHAT_ID + "=?";
    private static final String LOGTAG = LogUtils.getTag(GroupTalkView.class.getSimpleName());
    private static final String OPEN_GROUPCHAT = "OPEN_GROUPCHAT";
    private static final String INTITIATE_GROUPCHAT = "INTITIATE_GROUPCHAT";

    private Handler mHandler;
    private EditText mComposeText;
    private ChatService mChatService;
    private FileTransferService mFileTransferService;
    private Uri mUriHistoryProvider;
    private IsComposingManager mComposingManager;
    private TalkCursorAdapter mAdapter;
    private ChatCursorObserver mObserver;
    private boolean mChatListnerSet;
    private String mSubject;
    private String mChatId;
    private GroupChat mGroupChat;
    private Set<ContactId> mParticipants = new HashSet<>();
    private GroupChatListener mChatListener;
    private GroupFileTransferListener mFileTransferListener;
    private boolean mFileTransferListenerSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_view);
        if (!isServiceConnected(ConnectionManager.RcsServiceName.CHAT,
                ConnectionManager.RcsServiceName.CONTACT,
                ConnectionManager.RcsServiceName.CAPABILITY,
                ConnectionManager.RcsServiceName.FILE_TRANSFER)) {
            showMessageThenExit(R.string.label_service_not_available);
            return;
        }
        startMonitorServices(ConnectionManager.RcsServiceName.CHAT,
                ConnectionManager.RcsServiceName.CONTACT,
                ConnectionManager.RcsServiceName.CAPABILITY,
                ConnectionManager.RcsServiceName.FILE_TRANSFER);
        try {
            initialize();
            processIntent(getIntent());
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "onCreate");
            }
        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    private void initialize() throws RcsServiceNotAvailableException, RcsGenericException {
        Button sendButton = (Button) findViewById(R.id.send_button);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendText();
            }
        });
        mHandler = new Handler();
        mChatListener = new GroupChatListener() {

            @Override
            public void onMessageStatusChanged(String chatId, String mimeType, String msgId,
                    Content.Status status, Content.ReasonCode reasonCode) {
                if (LogUtils.isActive) {
                    Log.i(LOGTAG, "onMessageStatusChanged chatId=" + chatId + " mime-type="
                            + mimeType + " msgId=" + msgId + " status=" + status + " reason="
                            + reasonCode);
                }
            }

            // Callback called when an Is-composing event has been received
            public void onComposingEvent(String chatId, ContactId contact, boolean status) {
                // Discard event if not for current chatId
                if (!chatId.equals(mChatId)) {
                    return;
                }
                displayComposingEvent(contact, status);
            }

            @Override
            public void onParticipantStatusChanged(String chatId, ContactId contact,
                    ParticipantStatus status) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onParticipantStatusChanged chatId=" + chatId + " contact="
                            + contact + " status=" + status);
                }
            }

            @Override
            public void onMessageGroupDeliveryInfoChanged(String chatId, ContactId contact,
                    String mimeType, String msgId, GroupDeliveryInfo.Status status,
                    GroupDeliveryInfo.ReasonCode reasonCode) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onMessageGroupDeliveryInfoChanged chatId=" + chatId
                            + " contact=" + contact + " msgId=" + msgId + " status=" + status
                            + " reason=" + reasonCode);
                }
            }

            @Override
            public void onStateChanged(String chatId, final GroupChat.State state,
                    GroupChat.ReasonCode reasonCode) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onStateChanged chatId=" + chatId + " state=" + state
                            + " reason=" + reasonCode);
                }
                /* Discard event if not for current chatId */
                if (mChatId == null || !mChatId.equals(chatId)) {
                    return;
                }
                final String _reasonCode = RiApplication.sGroupChatReasonCodes[reasonCode.toInt()];
                mHandler.post(new Runnable() {
                    public void run() {
                        switch (state) {
                            case STARTED:
                                break;

                            case ABORTED:
                                showMessageThenExit(getString(R.string.label_chat_aborted,
                                        _reasonCode));
                                break;

                            case REJECTED:
                                showMessageThenExit(getString(R.string.label_chat_rejected,
                                        _reasonCode));
                                break;

                            case FAILED:
                                showMessageThenExit(getString(R.string.label_chat_failed,
                                        _reasonCode));
                                break;

                            default:
                        }
                    }
                });
            }

            @Override
            public void onDeleted(Set<String> chatIds) {
                if (LogUtils.isActive) {
                    Log.i(LOGTAG, "onDeleted chatIds=".concat(Arrays.toString(chatIds.toArray())));
                }
            }

            @Override
            public void onMessagesDeleted(String chatId, Set<String> msgIds) {
                if (LogUtils.isActive) {
                    Log.i(LOGTAG,
                            "onMessagesDeleted chatId=" + chatId + " msgIds="
                                    + Arrays.toString(msgIds.toArray()));
                }
            }

        };
        mFileTransferListener = new GroupFileTransferListener() {
            @Override
            public void onStateChanged(String chatId, String transferId, FileTransfer.State state,
                    FileTransfer.ReasonCode reasonCode) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onStateChanged chatId=" + chatId + " transferId=" + transferId
                            + " state=" + state + " reason=" + reasonCode);
                }
                /* Discard event if not for current chatId */
                if (mChatId == null || !mChatId.equals(chatId)) {
                    return;
                }
                if (FileTransfer.State.TRANSFERRED == state) {
                    try {
                        FileTransfer fileTransfer = mFileTransferService
                                .getFileTransfer(transferId);
                        if (fileTransfer == null) {
                            return;
                        }
                        if (Utils.isAudioType(fileTransfer.getMimeType())
                                && FileTransfer.Disposition.RENDER == fileTransfer.getDisposition()) {
                            Utils.playAudio(GroupTalkView.this, fileTransfer.getFile());
                            mFileTransferService.markFileTransferAsRead(transferId);
                        }
                    } catch (RcsPersistentStorageException | RcsServiceNotAvailableException
                            | RcsGenericException e) {
                        showException(e);
                    }
                }
            }

            @Override
            public void onDeliveryInfoChanged(String chatId, ContactId contact, String transferId,
                    GroupDeliveryInfo.Status status, GroupDeliveryInfo.ReasonCode reasonCode) {
            }

            @Override
            public void onProgressUpdate(String chatId, String transferId, long currentSize,
                    long totalSize) {
            }

            @Override
            public void onDeleted(String chatId, Set<String> transferIds) {
            }

        };
        mChatService = getChatApi();
        mFileTransferService = getFileTransferApi();

        HistoryUriBuilder uriBuilder = new HistoryUriBuilder(HistoryLog.CONTENT_URI);
        uriBuilder.appendProvider(ChatLog.Message.HISTORYLOG_MEMBER_ID);
        uriBuilder.appendProvider(FileTransferLog.HISTORYLOG_MEMBER_ID);
        mUriHistoryProvider = uriBuilder.build();

        mComposeText = (EditText) findViewById(R.id.userText);
        ChatServiceConfiguration configuration = mChatService.getConfiguration();
        // Set max label length
        int maxMsgLength = configuration.getGroupChatMessageMaxLength();
        if (maxMsgLength > 0) {
            InputFilter[] filterArray = new InputFilter[1];
            filterArray[0] = new InputFilter.LengthFilter(maxMsgLength);
            mComposeText.setFilters(filterArray);
        }
        mComposingManager = new IsComposingManager(configuration.getIsComposingTimeout(),
                getNotifyComposing());
        mComposeText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Check if the text is not null.
                // we do not wish to consider putting the edit text back to null
                // (like when sending message), is having activity
                if (!TextUtils.isEmpty(s)) {
                    // Warn the composing manager that we have some activity
                    if (mComposingManager != null) {
                        mComposingManager.hasActivity();
                    }
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        mAdapter = new TalkCursorAdapter(this, false, mChatService, mFileTransferService);
        ListView listView = (ListView) findViewById(android.R.id.list);
        listView.setAdapter(mAdapter);
        registerForContextMenu(listView);
    }

    private boolean processIntent(Intent intent) {
        String action = intent.getAction();
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "processIntent: " + action);
        }
        String oldChatId = mChatId;
        try {
            switch ((GroupChatMode) intent.getSerializableExtra(EXTRA_MODE)) {
                case OUTGOING:
                    /* Initiate a Group Chat: Get subject */
                    mSubject = intent.getStringExtra(GroupTalkView.EXTRA_SUBJECT);
                    updateGroupChatViewTitle(mSubject);
                    /* Get the list of participants */
                    ContactUtil contactUtil = ContactUtil.getInstance(this);
                    List<String> contacts = intent
                            .getStringArrayListExtra(GroupTalkView.EXTRA_PARTICIPANTS);
                    if (contacts == null || contacts.isEmpty()) {
                        showMessageThenExit(R.string.label_invalid_contacts);
                        return false;
                    }
                    for (String contact : contacts) {
                        mParticipants.add(contactUtil.formatContact(contact));
                    }
                    if (mParticipants.isEmpty()) {
                        showMessageThenExit(R.string.label_invalid_contacts);
                        return false;
                    }
                    return initiateGroupChat(oldChatId == null);

                case OPEN:
                    /* Open an existing session from the history log */
                    mChatId = intent.getStringExtra(GroupChatIntent.EXTRA_CHAT_ID);
                    mGroupChat = mChatService.getGroupChat(mChatId);
                    if (mGroupChat == null) {
                        if (LogUtils.isActive) {
                            Log.e(LOGTAG, "Groupchat not found for Id=".concat(mChatId));
                        }
                        showMessageThenExit(R.string.label_session_not_found);
                        return false;
                    }
                    ChatPendingIntentManager.getChatPendingIntentManager(this).clearNotification(
                            mChatId);
                    setCursorLoader(oldChatId == null);
                    RI.sChatIdOnForeground = mChatId;
                    mSubject = mGroupChat.getSubject();
                    updateGroupChatViewTitle(mSubject);
                    /* Set list of participants */
                    mParticipants = mGroupChat.getParticipants().keySet();
                    if (LogUtils.isActive) {
                        Log.i(LOGTAG, "processIntent chatId=" + mChatId + " subject='" + mSubject
                                + "'");
                    }
                    return true;

                case INCOMING:
                    String rxChatId = intent.getStringExtra(GroupChatIntent.EXTRA_CHAT_ID);
                    if (GroupChatIntent.ACTION_NEW_GROUP_CHAT_MESSAGE.equals(action)) {
                        String rxMsgId = intent.getStringExtra(GroupChatIntent.EXTRA_MESSAGE_ID);
                        mChatService.markMessageAsRead(rxMsgId);
                    }
                    mChatId = rxChatId;
                    mGroupChat = mChatService.getGroupChat(mChatId);
                    if (mGroupChat == null) {
                        showMessageThenExit(R.string.label_session_not_found);
                        return false;
                    }
                    setCursorLoader(oldChatId == null);
                    RI.sChatIdOnForeground = mChatId;
                    ContactId contact = mGroupChat.getRemoteContact();
                    mSubject = mGroupChat.getSubject();
                    updateGroupChatViewTitle(mSubject);
                    mParticipants = mGroupChat.getParticipants().keySet();
                    /* Display accept/reject dialog */
                    if (LogUtils.isActive) {
                        Log.d(LOGTAG, "New group chat for chatId " + mChatId + " state="
                                + mGroupChat.getState());
                    }
                    if (GroupChat.State.INVITED == mGroupChat.getState()) {
                        displayAcceptRejectDialog(contact);
                    }
                    if (LogUtils.isActive) {
                        Log.d(LOGTAG, "New group chat for chatId ".concat(mChatId));
                    }
                    return true;
            }

        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
        return false;
    }

    private void setCursorLoader(boolean firstLoad) {
        if (firstLoad) {
            /*
             * Initialize the Loader with id '1' and callbacks 'mCallbacks'.
             */
            getSupportLoaderManager().initLoader(LOADER_ID, null, this);
        } else {
            /* We switched from one contact to another: reload history since */
            getSupportLoaderManager().restartLoader(LOADER_ID, null, this);
        }
    }

    @Override
    public void onDestroy() {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onDestroy");
        }
        if (mGroupChat != null) {
            try {
                mGroupChat.setComposingStatus(false);
            } catch (RcsServiceException e) {
                Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
            }
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        RI.sChatIdOnForeground = mChatId;
        if (mChatId != null) {
            ChatPendingIntentManager.getChatPendingIntentManager(this).clearNotification(mChatId);
        }
        try {
            if (mChatListener != null && mChatService != null && !mChatListnerSet) {
                mChatService.addEventListener(mChatListener);
                mChatListnerSet = true;
            }
            if (mFileTransferListener != null && mFileTransferService != null
                    && !mFileTransferListenerSet) {
                mFileTransferService.addEventListener(mFileTransferListener);
                mFileTransferListenerSet = true;
            }
        } catch (RcsServiceNotAvailableException ignore) {
        } catch (RcsServiceException e) {
            Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        /* Replace the value of intent */
        setIntent(intent);
        processIntent(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        RI.sChatIdOnForeground = null;
        try {
            if (mChatListener != null && mChatService != null && mChatListnerSet) {
                mChatService.removeEventListener(mChatListener);
                mChatListnerSet = false;
            }
            if (mFileTransferListener != null && mFileTransferService != null
                    && mFileTransferListenerSet) {
                mFileTransferService.removeEventListener(mFileTransferListener);
                mFileTransferListenerSet = false;
            }
        } catch (RcsServiceNotAvailableException ignore) {
        } catch (RcsServiceException e) {
            Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case SELECT_GEOLOCATION:
                Geoloc geoloc = data.getParcelableExtra(EditGeoloc.EXTRA_GEOLOC);
                try {
                    if (mGroupChat != null && geoloc != null) {
                        mGroupChat.sendMessage(geoloc);
                    }
                } catch (RcsServiceException e) {
                    showExceptionThenExit(e);
                }
                break;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_gchat_item, menu);
        menu.findItem(R.id.menu_display_content).setVisible(false);
        menu.findItem(R.id.menu_listen_content).setVisible(false);
        /* Get the list item position. */
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        Cursor cursor = (Cursor) mAdapter.getItem(info.position);
        int providerId = cursor.getInt(cursor.getColumnIndexOrThrow(HistoryLog.PROVIDER_ID));
        Direction direction = Direction.valueOf(cursor.getInt(cursor
                .getColumnIndexOrThrow(Message.DIRECTION)));
        if (FileTransferLog.HISTORYLOG_MEMBER_ID == providerId) {
            String mimeType = cursor.getString(cursor.getColumnIndexOrThrow(HistoryLog.MIME_TYPE));
            if (Utils.isImageType(mimeType)) {
                if (Direction.OUTGOING == direction) {
                    menu.findItem(R.id.menu_display_content).setVisible(true);

                } else {
                    Long transferred = cursor.getLong(cursor
                            .getColumnIndexOrThrow(HistoryLog.TRANSFERRED));
                    Long size = cursor.getLong(cursor.getColumnIndexOrThrow(HistoryLog.FILESIZE));
                    if (size.equals(transferred)) {
                        menu.findItem(R.id.menu_display_content).setVisible(true);
                    }
                }
            } else if (Utils.isAudioType(mimeType)) {
                if (Direction.OUTGOING == direction) {
                    menu.findItem(R.id.menu_listen_content).setVisible(true);

                } else {
                    Long transferred = cursor.getLong(cursor
                            .getColumnIndexOrThrow(HistoryLog.TRANSFERRED));
                    Long size = cursor.getLong(cursor.getColumnIndexOrThrow(HistoryLog.FILESIZE));
                    if (size.equals(transferred)) {
                        menu.findItem(R.id.menu_listen_content).setVisible(true);
                    }
                }
            }
        }
        if (Direction.OUTGOING != direction) {
            menu.findItem(R.id.menu_view_group_delivery).setVisible(false);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        Cursor cursor = (Cursor) (mAdapter.getItem(info.position));
        int providerId = cursor.getInt(cursor.getColumnIndexOrThrow(HistoryLog.PROVIDER_ID));
        String id = cursor.getString(cursor.getColumnIndexOrThrow(HistoryLog.ID));
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onContextItemSelected Id=".concat(id));
        }
        try {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_view_group_delivery) {
                GroupDeliveryInfoList.startActivity(this, id);
                return true;
            } else if (itemId == R.id.menu_delete_message) {
                if (Message.HISTORYLOG_MEMBER_ID == providerId) {
                    mChatService.deleteMessage(id);
                } else {
                    mFileTransferService.deleteFileTransfer(id);
                }
                return true;
            } else if (itemId == R.id.menu_view_detail) {
                if (Message.HISTORYLOG_MEMBER_ID == providerId) {
                    ChatMessageLogView.startActivity(this, id);
                } else {
                    FileTransferLogView.startActivity(this, id);
                }
                return true;
            } else if (itemId == R.id.menu_display_content) {
                if (FileTransferLog.HISTORYLOG_MEMBER_ID == providerId) {
                    String file = cursor.getString(cursor
                            .getColumnIndexOrThrow(HistoryLog.CONTENT));
                    Utils.showPicture(this, Uri.parse(file));
                    markFileTransferAsRead(cursor, id);
                    return true;
                }
            } else if (itemId == R.id.menu_listen_content) {
                if (FileTransferLog.HISTORYLOG_MEMBER_ID == providerId) {
                    String file = cursor.getString(cursor
                            .getColumnIndexOrThrow(HistoryLog.CONTENT));
                    Utils.playAudio(this, Uri.parse(file));
                    markFileTransferAsRead(cursor, id);
                    return true;
                }
            }
            return super.onContextItemSelected(item);
        } catch (RcsServiceException e) {
            showException(e);
        }
        return true;
    }

    private void markFileTransferAsRead(Cursor cursor, String ftId) {
        try {
            RcsService.Direction dir = RcsService.Direction.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(HistoryLog.DIRECTION)));
            if (RcsService.Direction.INCOMING == dir) {
                RcsService.ReadStatus status = RcsService.ReadStatus.valueOf(cursor.getInt(cursor
                        .getColumnIndexOrThrow(HistoryLog.READ_STATUS)));
                if (RcsService.ReadStatus.UNREAD == status) {
                    mFileTransferService.markFileTransferAsRead(ftId);
                    if (LogUtils.isActive) {
                        Log.d(LOGTAG, "Mark file transfer " + ftId + " as read");
                    }
                }
            }
        } catch (RcsServiceNotAvailableException e) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "Cannot mark message as read: service not available");
            }
        } catch (RcsGenericException | RcsPersistentStorageException e) {
            Log.e(LOGTAG, ExceptionUtil.getFullStackTrace(e));
        }
    }

    /**
     * Update the view title
     *
     * @param subject the group chat subject or null
     */
    private void updateGroupChatViewTitle(String subject) {
        if (!TextUtils.isEmpty(subject)) {
            setTitle(getString(R.string.title_group_chat) + " '" + mSubject + "'");
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle arg) {
        /* Create a new CursorLoader with the following query parameters. */
        return new CursorLoader(this, mUriHistoryProvider, PROJ_CHAT_MSG, WHERE_CLAUSE,
                new String[] {
                    mChatId
                }, ORDER_CHAT_MSG);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (LOADER_ID != loader.getId()) {
            return;
        }
        /*
         * The asynchronous load is complete and the data is now available for use. Only now can we
         * associate the queried Cursor with the CursorAdapter.
         */
        mAdapter.swapCursor(cursor);
        /**
         * Registering content observer for chat message and file transfer content URIs. When these
         * content URIs will change, this will notify the loader to reload its data.
         */
        if (mObserver != null && !mObserver.getLoader().equals(loader)) {
            ContentResolver resolver = getContentResolver();
            resolver.unregisterContentObserver(mObserver);
            resolver.unregisterContentObserver(mObserver);
            mObserver = null;
        }
        if (mObserver == null) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "onLoadFinished: register content observer");
            }
            mObserver = new ChatCursorObserver(new Handler(), loader);
            ContentResolver resolver = getContentResolver();
            resolver.registerContentObserver(ChatLog.Message.CONTENT_URI, true, mObserver);
            resolver.registerContentObserver(FileTransferLog.CONTENT_URI, true, mObserver);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        /*
         * For whatever reason, the Loader's data is now unavailable. Remove any references to the
         * old data by replacing it with a null Cursor.
         */
        mAdapter.swapCursor(null);
    }

    /**
     * Display notification to accept or reject invitation
     *
     * @param remote remote contact
     */
    private void displayAcceptRejectDialog(ContactId remote) {
        /* Manual accept */
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_group_chat);
        String from = RcsContactUtil.getInstance(this).getDisplayName(remote);
        String topic = (TextUtils.isEmpty(mSubject)) ? getString(R.string.label_no_subject)
                : mSubject;
        String msg = getString(R.string.label_gc_from_subject, from, topic);
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setIcon(R.drawable.ri_notif_chat_icon);
        builder.setPositiveButton(R.string.label_accept,
                new android.content.DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            /* Accept the invitation */
                            mGroupChat.openChat();
                        } catch (RcsServiceException e) {
                            showExceptionThenExit(e);
                        }
                    }
                });
        builder.setNegativeButton(R.string.label_decline,
                new android.content.DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        /*
                         * Let session die by timeout. Exit activity
                         */
                        finish();
                    }
                });
        registerDialog(builder.show());
    }

    /**
     * get a set of contact from a set of participant info
     *
     * @param setOfParticipant a set of participant info
     * @return a set of contact
     */
    private Set<String> getSetOfParticipants(Map<ContactId, ParticipantStatus> setOfParticipant) {
        Set<String> result = new HashSet<>();
        if (setOfParticipant.size() != 0) {
            for (ContactId contact : setOfParticipant.keySet()) {
                // TODO consider status ?
                result.add(contact.toString());
            }
        }
        return result;
    }

    /**
     * Initiate the group chat and open a progress dialog waiting for the session to start
     *
     * @return True if successful
     */
    private boolean initiateGroupChat(boolean firstLoad) {
        /* Initiate the group chat session in background */
        try {
            mGroupChat = mChatService.initiateGroupChat(new HashSet<>(mParticipants), mSubject);
            mChatId = mGroupChat.getChatId();
            setCursorLoader(firstLoad);
            RI.sChatIdOnForeground = mChatId;
            return true;

        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
            return false;
        }
    }

    /**
     * Add participants to be invited in the session
     */
    private void addParticipants() {
        /* Build list of available contacts not already in the conference */
        Set<ContactId> availableParticipants = new HashSet<>();
        try {
            Set<RcsContact> contacts = getContactApi().getRcsContacts();
            for (RcsContact rcsContact : contacts) {
                ContactId contact = rcsContact.getContactId();
                if (mGroupChat.isAllowedToInviteParticipant(contact)) {
                    availableParticipants.add(contact);
                }
            }
        } catch (RcsServiceException e) {
            showException(e);
            return;
        }
        /* Check if some participants are available */
        if (availableParticipants.size() == 0) {
            showMessage(R.string.label_no_participant_found);
            return;
        }
        /* Display contacts */
        final List<String> selectedParticipants = new ArrayList<>();
        final CharSequence[] items = new CharSequence[availableParticipants.size()];
        int i = 0;
        for (ContactId contact : availableParticipants) {
            items[i++] = contact.toString();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.label_select_contacts);
        builder.setCancelable(true);
        builder.setMultiChoiceItems(items, null, new DialogInterface.OnMultiChoiceClickListener() {
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                String c = (String) items[which];
                if (isChecked) {
                    selectedParticipants.add(c);
                } else {
                    selectedParticipants.remove(c);
                }
            }
        });
        builder.setNegativeButton(R.string.label_cancel, null);
        builder.setPositiveButton(R.string.label_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int position) {
                /* Add new participants in the session in background */
                try {
                    int max = mGroupChat.getMaxParticipants() - 1;
                    int connected = mGroupChat.getParticipants().size();
                    int limit = max - connected;
                    if (selectedParticipants.size() > limit) {
                        showMessage(R.string.label_max_participants);
                        return;
                    }
                    Set<ContactId> contacts = new HashSet<>();
                    ContactUtil contactUtils = ContactUtil.getInstance(GroupTalkView.this);
                    for (String participant : selectedParticipants) {
                        contacts.add(contactUtils.formatContact(participant));
                    }
                    /* Add participants */
                    mGroupChat.inviteParticipants(contacts);

                } catch (RcsServiceException e) {
                    showException(e);
                }
            }
        });
        registerDialog(builder.show());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(getApplicationContext());
        inflater.inflate(R.menu.menu_group_chat, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem menuItemParticipants = menu.findItem(R.id.menu_participants);
        MenuItem menuItemSendFile = menu.findItem(R.id.menu_send_file);
        MenuItem menuItemLeave = menu.findItem(R.id.menu_close_session);
        try {
            if (mGroupChat != null) {
                menuItemParticipants.setEnabled(mGroupChat.isAllowedToInviteParticipants());
                menuItemLeave.setEnabled(mGroupChat.isAllowedToLeave());
                FileTransferService fileTransferService = getFileTransferApi();
                menuItemSendFile.setEnabled(fileTransferService
                        .isAllowedToTransferFileToGroupChat(mChatId));
            } else {
                menuItemParticipants.setEnabled(false);
                menuItemSendFile.setEnabled(false);
                menuItemLeave.setEnabled(false);
            }
        } catch (RcsServiceException e) {
            showException(e);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_insert_smiley) {
                AlertDialog alert = Smileys.showSmileyDialog(this, mComposeText,
                        getResources(), getString(R.string.menu_insert_smiley));
                registerDialog(alert);
            } else if (itemId == R.id.menu_participants) {
                AlertDialog alert;
                alert = Utils.showList(this, getString(R.string.menu_participants),
                        getSetOfParticipants(mGroupChat.getParticipants()));
                registerDialog(alert);
            } else if (itemId == R.id.menu_add_participant) {
                addParticipants();
            } else if (itemId == R.id.menu_quicktext) {
                addQuickText();
            } else if (itemId == R.id.menu_send_file) {
                SendGroupFile.startActivity(this, mChatId);
            } else if (itemId == R.id.menu_send_geoloc) {
                getGeoLoc();
            } else if (itemId == R.id.menu_showus_map) {
                DisplayGeoloc.showContactsOnMap(this, mGroupChat.getParticipants().keySet());
            } else if (itemId == R.id.menu_close_session) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.title_chat_exit);
                builder.setPositiveButton(R.string.label_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (mGroupChat != null) {
                                    try {
                                        mGroupChat.leave();
                                    } catch (RcsServiceException e) {
                                        showExceptionThenExit(e);
                                    }
                                }
                                GroupTalkView.this.finish();
                            }
                        });
                builder.setNegativeButton(R.string.label_cancel, null);
                builder.setCancelable(true);
                registerDialog(builder.show());
            }

        } catch (RcsServiceException e) {
            showException(e);
        }
        return true;
    }

    /**
     * Initiate a new Group Chat
     *
     * @param ctx context
     * @param subject subject
     * @param participants list of participants
     */
    public static void initiateGroupChat(Context ctx, String subject, ArrayList<String> participants) {
        Intent intent = new Intent(ctx, GroupTalkView.class);
        intent.setAction(INTITIATE_GROUPCHAT);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putStringArrayListExtra(GroupTalkView.EXTRA_PARTICIPANTS, participants);
        intent.putExtra(GroupTalkView.EXTRA_MODE, GroupChatMode.OUTGOING);
        intent.putExtra(GroupTalkView.EXTRA_SUBJECT, subject);
        ctx.startActivity(intent);
    }

    /**
     * Open a Group Chat
     *
     * @param ctx The context.
     * @param chatId The chat ID.
     */
    public static void openGroupChat(Context ctx, String chatId) {
        Intent intent = new Intent(ctx, GroupTalkView.class);
        intent.setAction(OPEN_GROUPCHAT);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(GroupTalkView.EXTRA_MODE, GroupChatMode.OPEN);
        intent.putExtra(GroupChatIntent.EXTRA_CHAT_ID, chatId);
        ctx.startActivity(intent);
    }

    /**
     * Forge intent to notify Group Chat message
     *
     * @param ctx The context.
     * @param newgroupChatMessage The original intent.
     * @param chatId the chat ID
     * @return intent
     */
    public static Intent forgeIntentNewMessage(Context ctx, Intent newgroupChatMessage,
            String chatId) {
        newgroupChatMessage.setClass(ctx, GroupTalkView.class);
        newgroupChatMessage
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        newgroupChatMessage.putExtra(GroupTalkView.EXTRA_MODE, GroupChatMode.INCOMING);
        newgroupChatMessage.putExtra(GroupChatIntent.EXTRA_CHAT_ID, chatId);
        return newgroupChatMessage;
    }

    /**
     * Forge intent to notify new Group Chat
     *
     * @param ctx The context.
     * @param invitation The original intent.
     * @return intent
     */
    public static Intent forgeIntentInvitation(Context ctx, Intent invitation) {
        invitation.setClass(ctx, GroupTalkView.class);
        invitation.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        invitation.putExtra(GroupTalkView.EXTRA_MODE, GroupChatMode.INCOMING);
        return invitation;
    }

    private ChatMessage sendMessage(String message) throws RcsServiceException {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "sendTextMessage: ".concat(message));
        }
        return mGroupChat.sendMessage(message);
    }

    private INotifyComposing getNotifyComposing() {
        return new INotifyComposing() {
            public void setTypingStatus(boolean isTyping) {
                try {
                    if (mGroupChat != null) {
                        mGroupChat.setComposingStatus(isTyping);
                        if (LogUtils.isActive) {
                            Log.d(LOGTAG, "sendIsComposingEvent ".concat(String.valueOf(isTyping)));
                        }
                    }
                } catch (RcsGenericException e) {
                    showException(e);
                }
            }
        };
    }

    private void displayComposingEvent(ContactId contact, final boolean status) {
        final String from = RcsContactUtil.getInstance(this).getDisplayName(contact);
        mHandler.post(new Runnable() {
            public void run() {
                TextView view = (TextView) findViewById(R.id.isComposingText);
                if (status) {
                    // Display is-composing notification
                    view.setText(getString(R.string.label_contact_is_composing, from));
                    view.setVisibility(View.VISIBLE);
                } else {
                    // Hide is-composing notification
                    view.setVisibility(View.GONE);
                }
            }
        });
    }

    private void sendText() {
        String text = mComposeText.getText().toString();
        if (!TextUtils.isEmpty(text)) {
            try {
                sendMessage(text);
                mComposingManager.messageWasSent();
                mComposeText.setText(null);
            } catch (RcsServiceException e) {
                showExceptionThenExit(e);
            }
        }
    }

    private void addQuickText() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.label_select_quicktext);
        builder.setCancelable(true);
        builder.setItems(R.array.select_quicktext, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String[] items = getResources().getStringArray(R.array.select_quicktext);
                mComposeText.append(items[which]);
            }
        });
        registerDialog(builder.show());
    }

    private void getGeoLoc() {
        // Start a new activity to send a geolocation
        startActivityForResult(new Intent(this, EditGeoloc.class), SELECT_GEOLOCATION);
    }

}
