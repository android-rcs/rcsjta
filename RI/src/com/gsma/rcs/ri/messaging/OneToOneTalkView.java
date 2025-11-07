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
import com.gsma.rcs.ri.messaging.adapter.TalkCursorAdapter;
import com.gsma.rcs.ri.messaging.chat.ChatCursorObserver;
import com.gsma.rcs.ri.messaging.chat.ChatMessageLogView;
import com.gsma.rcs.ri.messaging.chat.ChatPendingIntentManager;
import com.gsma.rcs.ri.messaging.chat.IsComposingManager;
import com.gsma.rcs.ri.messaging.chat.single.SendSingleFile;
import com.gsma.rcs.ri.messaging.chat.single.SingleChatIntentService;
import com.gsma.rcs.ri.messaging.filetransfer.FileTransferIntentService;
import com.gsma.rcs.ri.messaging.filetransfer.FileTransferLogView;
import com.gsma.rcs.ri.messaging.geoloc.EditGeoloc;
import com.gsma.rcs.ri.utils.ContactUtil;
import com.gsma.rcs.ri.utils.LogUtils;
import com.gsma.rcs.ri.utils.RcsContactUtil;
import com.gsma.rcs.ri.utils.Utils;
import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.RcsPersistentStorageException;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.RcsServiceNotRegisteredException;
import com.gsma.services.rcs.capability.CapabilityService;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatService;
import com.gsma.services.rcs.chat.ChatServiceConfiguration;
import com.gsma.services.rcs.chat.OneToOneChat;
import com.gsma.services.rcs.chat.OneToOneChatIntent;
import com.gsma.services.rcs.chat.OneToOneChatListener;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransferIntent;
import com.gsma.services.rcs.filetransfer.FileTransferLog;
import com.gsma.services.rcs.filetransfer.FileTransferService;
import com.gsma.services.rcs.filetransfer.OneToOneFileTransferListener;
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
import android.os.Parcelable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * One to one talk view : aggregates the RCS IM messages.
 *
 * @author Philippe LEMORDANT
 */
public class OneToOneTalkView extends RcsFragmentActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * The loader's unique ID. Loader IDs are specific to the Activity in which they reside.
     */
    private static final int LOADER_ID = 1;

    // @formatter:off
    private static final String[] PROJECTION = new String[]{
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

    private final static String EXTRA_CONTACT = "contact";

    private static final String LOGTAG = LogUtils.getTag(OneToOneTalkView.class.getSimpleName());
    /**
     * Chat_id is set to contact id for one to one chat and file transfer messages.
     */
    private static final String WHERE_CLAUSE = HistoryLog.CHAT_ID + "=?";
    private final static String ORDER_ASC = HistoryLog.TIMESTAMP + " ASC";

    private static final String OPEN_TALK = "open_talk";

    private final static int SELECT_GEOLOCATION = 0;

    /**
     * The adapter that binds data to the ListView
     */
    private TalkCursorAdapter mAdapter;
    private Uri mUriHistoryProvider;
    private ContactId mContact;
    private ChatCursorObserver mObserver;
    private EditText mComposeText;
    private ChatService mChatService;
    private OneToOneChat mChat;
    private FileTransferService mFileTransferService;
    private OneToOneChatListener mChatListener;
    private OneToOneFileTransferListener mFileTransferListener;
    private Handler mHandler;
    private AlertDialog mClearUndeliveredAlertDialog;
    private DialogInterface.OnCancelListener mUndeliveredCancelListener;
    /**
     * Utility class to manage the is-composing status
     */
    private IsComposingManager mComposingManager;
    private CapabilityService mCapabilityService;
    private Context mCtx;

    // @formatter:off
    private static final Set<String> sAllowedIntentActions = new HashSet<>(Arrays.asList(
            OneToOneChatIntent.ACTION_MESSAGE_DELIVERY_EXPIRED,
            OneToOneChatIntent.ACTION_NEW_ONE_TO_ONE_CHAT_MESSAGE,
            FileTransferIntent.ACTION_FILE_TRANSFER_DELIVERY_EXPIRED,
            OPEN_TALK));
    // @formatter:on

    private DialogInterface.OnClickListener mClearUndeliveredChat;
    private DialogInterface.OnClickListener mClearUndeliveredFt;
    private boolean mChatListenerSet;
    private boolean mFileTransferListenerSet;

    /**
     * Forge intent to start XmsView activity
     *
     * @param context The context
     * @param contact The contact ID
     * @return intent
     */
    public static Intent forgeIntentToOpenConversation(Context context, ContactId contact) {
        Intent intent = new Intent(context, OneToOneTalkView.class);
        intent.setAction(OPEN_TALK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_CONTACT, (Parcelable) contact);
        return intent;
    }

    /**
     * Forge intent to start OneToOneTalkView activity upon reception of a stack event
     *
     * @param ctx The context
     * @param contact The contact ID
     * @param intent intent
     * @return intent
     */
    public static Intent forgeIntentOnStackEvent(Context ctx, ContactId contact, Intent intent) {
        intent.setClass(ctx, OneToOneTalkView.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_CONTACT, (Parcelable) contact);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_view);
        if (!isServiceConnected(ConnectionManager.RcsServiceName.CONTACT,
                ConnectionManager.RcsServiceName.CHAT,
                ConnectionManager.RcsServiceName.FILE_TRANSFER,
                ConnectionManager.RcsServiceName.CAPABILITY)) {
            showMessageThenExit(R.string.label_service_not_available);
            return;
        }
        startMonitorServices(ConnectionManager.RcsServiceName.CONTACT,
                ConnectionManager.RcsServiceName.CHAT,
                ConnectionManager.RcsServiceName.FILE_TRANSFER,
                ConnectionManager.RcsServiceName.CAPABILITY);
        try {
            initialize();
            processIntent(getIntent());

        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    private void sendText() {
        final String text = mComposeText.getText().toString();
        if (TextUtils.isEmpty(text)) {
            return;
        }
        try {
            if (mChat != null) {
                mChat.sendMessage(text);
            }
            mComposeText.setText(null);

        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    private void initialize() throws RcsGenericException, RcsServiceNotAvailableException {
        mCtx = this;
        /* Set send button listener */
        Button sendBtn = (Button) findViewById(R.id.send_button);
        sendBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                sendText();
            }
        });
        mHandler = new Handler();
        mClearUndeliveredChat = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Set<String> msgIds = SingleChatIntentService.getUndelivered(mCtx, mContact);
                if (!msgIds.isEmpty()) {
                    try {
                        if (LogUtils.isActive) {
                            Log.d(LOGTAG, "Clear delivery expiration for IDs=" + msgIds);
                        }
                        mChatService.clearMessageDeliveryExpiration(msgIds);

                    } catch (RcsServiceException e) {
                        showException(e);
                    } finally {
                        mClearUndeliveredAlertDialog = null;
                    }
                }
            }
        };
        mUndeliveredCancelListener = new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mClearUndeliveredAlertDialog = null;
            }
        };
        mClearUndeliveredFt = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    Set<String> transferIds = FileTransferIntentService.getUndelivered(mCtx,
                            mContact);
                    if (!transferIds.isEmpty()) {
                        if (LogUtils.isActive) {
                            Log.d(LOGTAG, "Clear delivery expiration for IDs=" + transferIds);
                        }
                        mFileTransferService.clearFileTransferDeliveryExpiration(transferIds);
                        mClearUndeliveredAlertDialog = null;
                    }
                } catch (RcsServiceException e) {
                    showException(e);
                } finally {
                    mClearUndeliveredAlertDialog = null;
                }
            }
        };

        mChatListener = new OneToOneChatListener() {

            /* Callback called when an Is-composing event has been received */
            @Override
            public void onComposingEvent(ContactId contact, boolean status) {
                /* Discard event if not for current contact */
                if (!contact.equals(mContact)) {
                    return;
                }
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onComposingEvent contact=" + contact + " status=" + status);
                }
                displayComposingEvent(contact, status);
            }

            @Override
            public void onMessageStatusChanged(ContactId contact, String mimeType, String msgId,
                    ChatLog.Message.Content.Status status,
                    ChatLog.Message.Content.ReasonCode reasonCode) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onMessageStatusChanged contact=" + contact + " mime-type="
                            + mimeType + " msgId=" + msgId + " status=" + status);
                }
            }

            @Override
            public void onMessagesDeleted(ContactId contact, Set<String> msgIds) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onMessagesDeleted contact=" + contact + " for IDs=" + msgIds);
                }
            }

        };
        mFileTransferListener = new OneToOneFileTransferListener() {
            @Override
            public void onStateChanged(ContactId contact, String transferId,
                    FileTransfer.State state, FileTransfer.ReasonCode reasonCode) {
                if (!contact.equals(mContact)) {
                    return;
                }
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onStateChanged contact=" + contact + " transferId=" + transferId
                            + " state=" + state + " reason=" + reasonCode);
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
                            Utils.playAudio(OneToOneTalkView.this, fileTransfer.getFile());
                            mFileTransferService.markFileTransferAsRead(transferId);
                        }
                    } catch (RcsPersistentStorageException | RcsServiceNotAvailableException
                            | RcsGenericException e) {
                        showException(e);
                    }
                }
            }

            @Override
            public void onProgressUpdate(ContactId contact, String transferId, long currentSize,
                    long totalSize) {
            }

            @Override
            public void onDeleted(ContactId contact, Set<String> transferIds) {
            }
        };
        mChatService = getChatApi();
        mCapabilityService = getCapabilityApi();
        mFileTransferService = getFileTransferApi();

        HistoryUriBuilder uriBuilder = new HistoryUriBuilder(HistoryLog.CONTENT_URI);
        uriBuilder.appendProvider(ChatLog.Message.HISTORYLOG_MEMBER_ID);
        uriBuilder.appendProvider(FileTransferLog.HISTORYLOG_MEMBER_ID);
        mUriHistoryProvider = uriBuilder.build();

        mComposeText = (EditText) findViewById(R.id.userText);
        ChatServiceConfiguration configuration = mChatService.getConfiguration();
        // Set max label length
        int maxMsgLength = configuration.getOneToOneChatMessageMaxLength();
        if (maxMsgLength > 0) {
            /* Set the message composer max length */
            InputFilter[] filterArray = new InputFilter[1];
            filterArray[0] = new InputFilter.LengthFilter(maxMsgLength);
            mComposeText.setFilters(filterArray);
        }
        IsComposingManager.INotifyComposing iNotifyComposing = new IsComposingManager.INotifyComposing() {
            public void setTypingStatus(boolean isTyping) {
                try {
                    if (mChat == null) {
                        return;
                    }
                    mChat.setComposingStatus(isTyping);
                    if (LogUtils.isActive) {
                        Boolean _isTyping = isTyping;
                        Log.d(LOGTAG, "sendIsComposingEvent ".concat(_isTyping.toString()));
                    }
                } catch (RcsGenericException e) {
                    showException(e);
                }
            }
        };
        /* Instantiate the composing manager */
        mComposingManager = new IsComposingManager(configuration.getIsComposingTimeout(),
                iNotifyComposing);
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

        /* Initialize the adapter. */
        mAdapter = new TalkCursorAdapter(this, true, mChatService, mFileTransferService);

        /* Associate the list adapter with the ListView. */
        ListView listView = (ListView) findViewById(android.R.id.list);
        listView.setDivider(null);
        listView.setAdapter(mAdapter);
        registerForContextMenu(listView);
    }

    private boolean processIntent(Intent intent) {
        String action = intent.getAction();
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "processIntent " + action);
        }
        ContactId newContact = intent.getParcelableExtra(EXTRA_CONTACT);
        if (newContact == null) {
            if (LogUtils.isActive) {
                Log.w(LOGTAG, "Cannot process intent: contact is null");
            }
            return false;
        }
        if (action == null) {
            if (LogUtils.isActive) {
                Log.w(LOGTAG, "Cannot process intent: action is null");
            }
            return false;
        }
        if (!sAllowedIntentActions.contains(action)) {
            if (LogUtils.isActive) {
                Log.w(LOGTAG, "Cannot process intent: unauthorized action " + action);
            }
            return false;
        }
        try {
            if (!newContact.equals(mContact)) {
                /* Either it is the first conversation loading or switch to another conversation */
                loadConversation(newContact);
            }
            /* Set activity title with display name */
            String displayName = RcsContactUtil.getInstance(this).getDisplayName(mContact);
            setTitle(getString(R.string.title_chat, displayName));
            switch (action) {
                case OneToOneChatIntent.ACTION_NEW_ONE_TO_ONE_CHAT_MESSAGE:
                    /*
                     * Open chat to accept session if the parameter IM SESSION START is 0. Client
                     * application is not aware of the one to one chat session state nor of the IM
                     * session start mode so we call the method systematically.
                     */
                    mChat.openChat();
                    break;

                case OneToOneChatIntent.ACTION_MESSAGE_DELIVERY_EXPIRED:
                    processUndeliveredMessages(displayName);
                    break;

                case FileTransferIntent.ACTION_FILE_TRANSFER_DELIVERY_EXPIRED:
                    processUndeliveredFileTransfers(displayName);
                    break;
            }
            return true;

        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
            return false;
        }
    }

    private void clearNotification() {
        ChatPendingIntentManager pendingIntentManager = ChatPendingIntentManager
                .getChatPendingIntentManager(this);
        pendingIntentManager.clearNotification(mContact.toString());
    }

    private void loadConversation(ContactId newContact) throws RcsServiceNotAvailableException,
            RcsGenericException, RcsPersistentStorageException {
        boolean firstLoad = (mContact == null);
        /* Save contact ID */
        mContact = newContact;
        clearNotification();
        /*
         * Open chat so that if the parameter IM SESSION START is 0 then the session is accepted
         * now.
         */
        mChat = mChatService.getOneToOneChat(mContact);
        setCursorLoader(firstLoad);
        RI.sChatIdOnForeground = mContact.toString();
        /* Request for capabilities ony if they are not available or expired */
        requestCapabilities(mContact);
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
    protected void onPause() {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "--> onPause");
        }
        super.onPause();
        RI.sChatIdOnForeground = null;
        try {
            if (mChatListener != null && mChatService != null && mChatListenerSet) {
                mChatService.removeEventListener(mChatListener);
                mChatListenerSet = false;
            }
            if (mFileTransferListener != null && mFileTransferService != null
                    && mFileTransferListenerSet) {
                mFileTransferService.removeEventListener(mFileTransferListener);
                mFileTransferListenerSet = false;
            }
        } catch (RcsServiceNotAvailableException ignore) {
        } catch (RcsGenericException e) {
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
    protected void onResume() {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "--> onResume");
        }
        super.onResume();
        if (mContact != null) {
            RI.sChatIdOnForeground = mContact.toString();
            clearNotification();
        }
        try {
            if (mChatListener != null && mChatService != null && !mChatListenerSet) {
                mChatService.addEventListener(mChatListener);
                mChatListenerSet = true;
            }
            if (mFileTransferListener != null && mFileTransferService != null
                    && !mFileTransferListenerSet) {
                mFileTransferService.addEventListener(mFileTransferListener);
                mFileTransferListenerSet = true;
            }
        } catch (RcsServiceNotAvailableException ignore) {
        } catch (RcsGenericException e) {
            Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(getApplicationContext());
        inflater.inflate(R.menu.menu_1to1_talk, menu);
        return true;
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
                    if (mChat != null) {
                        mChat.sendMessage(geoloc);
                    }
                } catch (RcsServiceException e) {
                    showExceptionThenExit(e);
                }
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_send_geoloc) {/* Start a new activity to select a geolocation */
                startActivityForResult(new Intent(this, EditGeoloc.class), SELECT_GEOLOCATION);
            } else if (itemId == R.id.menu_send_rcs_file) {
                SendSingleFile.startActivity(this, mContact);
            } else if (itemId == R.id.menu_delete_talk) {
                mFileTransferService.deleteOneToOneFileTransfers(mContact);
                mChatService.deleteOneToOneChat(mContact);
            }
        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_1to1_talk_item, menu);
        menu.findItem(R.id.menu_resend_message).setVisible(false);
        menu.findItem(R.id.menu_display_content).setVisible(false);
        menu.findItem(R.id.menu_listen_content).setVisible(false);
        /* Get the list item position */
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Cursor cursor = (Cursor) mAdapter.getItem(info.position);
        /* Adapt the contextual menu according to the selected item */
        int providerId = cursor.getInt(cursor.getColumnIndexOrThrow(HistoryLog.PROVIDER_ID));
        String id = cursor.getString(cursor.getColumnIndexOrThrow(HistoryLog.ID));
        Direction direction = Direction.valueOf(cursor.getInt(cursor
                .getColumnIndexOrThrow(HistoryLog.DIRECTION)));
        try {
            switch (providerId) {
                case ChatLog.Message.HISTORYLOG_MEMBER_ID:
                    if (Direction.OUTGOING == direction) {
                        ChatLog.Message.Content.Status status = ChatLog.Message.Content.Status
                                .valueOf(cursor.getInt(cursor
                                        .getColumnIndexOrThrow(HistoryLog.STATUS)));
                        if (ChatLog.Message.Content.Status.FAILED == status) {
                            String number = cursor.getString(cursor
                                    .getColumnIndexOrThrow(HistoryLog.CONTACT));
                            if (number != null) {
                                ContactId contact = ContactUtil.formatContact(number);
                                OneToOneChat chat = mChatService.getOneToOneChat(contact);
                                if (chat != null && chat.isAllowedToSendMessage()) {
                                    menu.findItem(R.id.menu_resend_message).setVisible(true);
                                }
                            }
                        }
                    }
                    break;

                case FileTransferLog.HISTORYLOG_MEMBER_ID:
                    String mimeType = cursor.getString(cursor
                            .getColumnIndexOrThrow(HistoryLog.MIME_TYPE));
                    FileTransfer.State state = FileTransfer.State.valueOf(cursor.getInt(cursor
                            .getColumnIndexOrThrow(HistoryLog.STATUS)));
                    if (FileTransfer.State.FAILED == state) {
                        FileTransfer transfer = mFileTransferService.getFileTransfer(id);
                        if (transfer != null && transfer.isAllowedToResendTransfer()) {
                            menu.findItem(R.id.menu_resend_message).setVisible(true);
                        }
                    } else if (Utils.isImageType(mimeType)) {
                        if (Direction.OUTGOING == direction) {
                            menu.findItem(R.id.menu_display_content).setVisible(true);

                        } else if (Direction.INCOMING == direction) {
                            Long transferred = cursor.getLong(cursor
                                    .getColumnIndexOrThrow(HistoryLog.TRANSFERRED));
                            Long size = cursor.getLong(cursor
                                    .getColumnIndexOrThrow(HistoryLog.FILESIZE));
                            if (size.equals(transferred)) {
                                menu.findItem(R.id.menu_display_content).setVisible(true);
                            }
                        }
                    } else if (Utils.isAudioType(mimeType)) {
                        if (Direction.OUTGOING == direction) {
                            menu.findItem(R.id.menu_listen_content).setVisible(true);

                        } else if (Direction.INCOMING == direction) {
                            Long transferred = cursor.getLong(cursor
                                    .getColumnIndexOrThrow(HistoryLog.TRANSFERRED));
                            Long size = cursor.getLong(cursor
                                    .getColumnIndexOrThrow(HistoryLog.FILESIZE));
                            if (size.equals(transferred)) {
                                menu.findItem(R.id.menu_listen_content).setVisible(true);
                            }
                        }
                    }
                    break;

                default:
                    throw new IllegalArgumentException("Invalid provider ID=" + providerId);
            }
        } catch (RcsServiceNotAvailableException e) {
            menu.findItem(R.id.menu_resend_message).setVisible(false);

        } catch (RcsGenericException | RcsPersistentStorageException e) {
            menu.findItem(R.id.menu_resend_message).setVisible(false);
            showException(e);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();
        Cursor cursor = (Cursor) (mAdapter.getItem(info.position));
        int providerId = cursor.getInt(cursor.getColumnIndexOrThrow(HistoryLog.PROVIDER_ID));
        String id = cursor.getString(cursor.getColumnIndexOrThrow(HistoryLog.ID));
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onContextItemSelected Id=".concat(id));
        }
        try {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_delete_message) {
                switch (providerId) {
                    case ChatLog.Message.HISTORYLOG_MEMBER_ID:
                        mChatService.deleteMessage(id);
                        return true;
                    case FileTransferLog.HISTORYLOG_MEMBER_ID:
                        mFileTransferService.deleteFileTransfer(id);
                        return true;
                }
            } else if (itemId == R.id.menu_resend_message) {
                switch (providerId) {
                    case ChatLog.Message.HISTORYLOG_MEMBER_ID:
                        OneToOneChat chat = mChatService.getOneToOneChat(mContact);
                        if (chat != null) {
                            chat.resendMessage(id);
                        }
                        return true;

                    case FileTransferLog.HISTORYLOG_MEMBER_ID:
                        FileTransfer fileTransfer = mFileTransferService.getFileTransfer(id);
                        if (fileTransfer != null) {
                            fileTransfer.resendTransfer();
                        }
                        return true;
                }
            } else if (itemId == R.id.menu_display_content) {
                switch (providerId) {
                    case FileTransferLog.HISTORYLOG_MEMBER_ID:
                        String file = cursor.getString(cursor
                                .getColumnIndexOrThrow(HistoryLog.CONTENT));
                        Utils.showPicture(this, Uri.parse(file));
                        markFileTransferAsRead(cursor, id);
                        return true;
                }
            } else if (itemId == R.id.menu_view_detail) {
                switch (providerId) {
                    case ChatLog.Message.HISTORYLOG_MEMBER_ID:
                        ChatMessageLogView.startActivity(this, id);
                        return true;
                    case FileTransferLog.HISTORYLOG_MEMBER_ID:
                        FileTransferLogView.startActivity(this, id);
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

        } catch (RcsGenericException | RcsPermissionDeniedException | RcsPersistentStorageException e) {
            showException(e);
            return true;

        } catch (RcsServiceNotAvailableException e) {
            Utils.displayLongToast(this, getString(R.string.label_service_not_available));
            return true;
        }
    }

    private void markFileTransferAsRead(Cursor cursor, String ftId) {
        try {
            Direction dir = Direction.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(HistoryLog.DIRECTION)));
            if (Direction.INCOMING == dir) {
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

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        /* Create a new CursorLoader with the following query parameters. */
        return new CursorLoader(this, mUriHistoryProvider, PROJECTION, WHERE_CLAUSE, new String[] {
            mContact.toString()
        }, ORDER_ASC);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (LOADER_ID == loader.getId()) {
            /*
             * The asynchronous load is complete and the data is now available for use. Only now can
             * we associate the queried Cursor with the CursorAdapter.
             */
            mAdapter.swapCursor(data);
            /**
             * Registering content observer for XMS message content URI. When this content URI will
             * change, this will notify the loader to reload its data.
             */
            if (mObserver != null && !mObserver.getLoader().equals(loader)) {
                ContentResolver resolver = getContentResolver();
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
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        /*
         * For whatever reason, the Loader's data is now unavailable. Remove any references to the
         * old data by replacing it with a null Cursor.
         */
        mAdapter.swapCursor(null);
    }

    private void displayComposingEvent(final ContactId contact, final boolean status) {
        final String from = RcsContactUtil.getInstance(this).getDisplayName(contact);
        // Execute on UI handler since callback is executed from service
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

    private void processUndeliveredFileTransfers(String displayName) throws RcsGenericException,
            RcsServiceNotAvailableException, RcsPersistentStorageException,
            RcsPermissionDeniedException {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "processUndeliveredFileTransfers: ask");
        }
        /* Do not propose to clear undelivered if a dialog is already opened */
        if (mClearUndeliveredAlertDialog == null) {
            mClearUndeliveredAlertDialog = popUpDeliveryExpiration(this,
                    getString(R.string.title_undelivered_filetransfer),
                    getString(R.string.label_undelivered_filetransfer, displayName),
                    mClearUndeliveredFt, null, mUndeliveredCancelListener);
            registerDialog(mClearUndeliveredAlertDialog);
        }
    }

    private void processUndeliveredMessages(String displayName) throws RcsGenericException,
            RcsPersistentStorageException, RcsServiceNotAvailableException {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "processUndeliveredMessages: ask");
        }
        /* Do not propose to clear undelivered if a dialog is already opened */
        if (mClearUndeliveredAlertDialog == null) {
            mClearUndeliveredAlertDialog = popUpDeliveryExpiration(this,
                    getString(R.string.title_undelivered_message),
                    getString(R.string.label_undelivered_message, displayName),
                    mClearUndeliveredChat, null, mUndeliveredCancelListener);
            registerDialog(mClearUndeliveredAlertDialog);
        }
    }

    private AlertDialog popUpDeliveryExpiration(Context ctx, String title, String msg,
            DialogInterface.OnClickListener onPositiveClickListener,
            DialogInterface.OnClickListener onNegativeClickListener,
            DialogInterface.OnCancelListener onCancelListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setMessage(msg);
        builder.setTitle(title);
        if (onNegativeClickListener != null) {
            builder.setNegativeButton(R.string.label_cancel, onNegativeClickListener);
        }
        builder.setPositiveButton(R.string.label_ok, onPositiveClickListener);
        builder.setOnCancelListener(onCancelListener);
        return builder.show();
    }

    private void requestCapabilities(ContactId contact) throws RcsServiceNotAvailableException,
            RcsGenericException {
        try {
            mCapabilityService.requestContactCapabilities(new HashSet<>(Collections
                    .singletonList(contact)));

        } catch (RcsServiceNotRegisteredException e) {
            Log.w(LOGTAG, "Cannot request capabilities: RCS not registered!");
        }
    }

}
