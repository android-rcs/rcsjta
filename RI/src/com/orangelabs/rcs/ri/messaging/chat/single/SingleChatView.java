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

package com.orangelabs.rcs.ri.messaging.chat.single;

import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.RcsServiceNotRegisteredException;
import com.gsma.services.rcs.capability.CapabilityService;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatLog.Message.Content;
import com.gsma.services.rcs.chat.ChatMessage;
import com.gsma.services.rcs.chat.ChatService;
import com.gsma.services.rcs.chat.ChatServiceConfiguration;
import com.gsma.services.rcs.chat.OneToOneChat;
import com.gsma.services.rcs.chat.OneToOneChatIntent;
import com.gsma.services.rcs.chat.OneToOneChatListener;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransferIntent;
import com.gsma.services.rcs.filetransfer.FileTransferLog;
import com.gsma.services.rcs.history.HistoryLog;

import com.orangelabs.rcs.api.connection.utils.ExceptionUtil;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.messaging.chat.ChatView;
import com.orangelabs.rcs.ri.messaging.chat.IsComposingManager;
import com.orangelabs.rcs.ri.messaging.chat.IsComposingManager.INotifyComposing;
import com.orangelabs.rcs.ri.messaging.geoloc.DisplayGeoloc;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsContactUtil;
import com.orangelabs.rcs.ri.utils.Smileys;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.InputFilter;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Single chat view
 */
public class SingleChatView extends ChatView {

    private final static String EXTRA_CONTACT = "contact";

    private final static String[] PROJ_UNDELIVERED_MSG = new String[] {
        ChatLog.Message.MESSAGE_ID
    };

    private final static String[] PROJ_UNDELIVERED_FT = new String[] {
        FileTransferLog.FT_ID
    };

    private ContactId mContact;

    private OneToOneChat mChat;

    /**
     * List of items for contextual menu
     */
    private final static int MENU_ITEM_DELETE = 0;

    private final static int MENU_ITEM_RESEND = 1;

    private static final String LOGTAG = LogUtils.getTag(SingleChatView.class.getSimpleName());

    /**
     * Chat_id is set to contact id for one to one chat and file transfer messages.
     */
    private static final String WHERE_CLAUSE = HistoryLog.CHAT_ID + "=?";

    private final static String UNREADS_WHERE_CLAUSE = HistoryLog.CHAT_ID + "=? AND "
            + HistoryLog.READ_STATUS + "=" + ReadStatus.UNREAD.toInt();

    private final static String[] PROJECTION_UNREAD_MESSAGE = new String[] {
            HistoryLog.PROVIDER_ID, HistoryLog.ID
    };

    private static final String OPEN_ONE_TO_ONE_CHAT_CONVERSATION = "open_one_to_one_conversation";

    private static final String SEL_UNDELIVERED_MESSAGES = ChatLog.Message.CHAT_ID + "=? AND "
            + ChatLog.Message.EXPIRED_DELIVERY + "='1'";

    private static final String SEL_UNDELIVERED_FTS = FileTransferLog.CHAT_ID + "=? AND "
            + FileTransferLog.EXPIRED_DELIVERY + "='1'";

    private OneToOneChatListener mChatListener;

    private CapabilityService mCapabilityService;

    private AlertDialog mClearUndeliveredAlertDialog;

    private OnCancelListener mClearUndeliveredCancelListener;

    private OnClickListener mClearUndeliveredMessageClickListener;

    private OnClickListener mClearUndeliveredFileTransferClickListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isExiting()) {
            return;
        }
        try {
            addChatEventListener(mChatService);
            ChatServiceConfiguration configuration = mChatService.getConfiguration();
            // Set max label length
            int maxMsgLength = configuration.getOneToOneChatMessageMaxLength();
            if (maxMsgLength > 0) {
                // Set the message composer max length
                InputFilter[] filterArray = new InputFilter[1];
                filterArray[0] = new InputFilter.LengthFilter(maxMsgLength);
                mComposeText.setFilters(filterArray);
            }
            // Instantiate the composing manager
            mComposingManager = new IsComposingManager(configuration.getIsComposingTimeout(),
                    getNotifyComposing());

        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onCreate");
        }
    }

    @Override
    public void onDestroy() {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onDestroy");
        }
        if (mChat != null) {
            try {
                mChat.setComposingStatus(false);
            } catch (RcsGenericException e) {
                Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
            }
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mContact != null) {
            sChatIdOnForeground = mContact.toString();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sChatIdOnForeground = null;
    }

    @Override
    public boolean processIntent(Intent intent) {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "processIntent ".concat(intent.getAction()));
        }
        ContactId newContact = intent.getParcelableExtra(EXTRA_CONTACT);
        if (newContact == null) {
            if (LogUtils.isActive) {
                Log.w(LOGTAG, "Cannot process intent: contact is null");
            }
            return false;
        }
        try {
            if (!newContact.equals(mContact) || mChat == null) {
                /* Either it is the first conversation loading or switch to another conversation */
                loadConversation(newContact);
            }
            /*
             * Open chat to accept session if the parameter IM SESSION START is 0. Client
             * application is not aware of the one to one chat session state nor of the IM session
             * start mode so we call the method systematically.
             */
            mChat.openChat();

            /* Set activity title with display name */
            String displayName = RcsContactUtil.getInstance(this).getDisplayName(mContact);
            setTitle(getString(R.string.title_chat, displayName));
            /* Mark as read messages if required */
            Map<String, Integer> msgIdUnreads = getUnreadMessageIds(mContact);
            for (Entry<String, Integer> entryMsgIdUnread : msgIdUnreads.entrySet()) {
                if (ChatLog.Message.HISTORYLOG_MEMBER_ID == entryMsgIdUnread.getValue()) {
                    mChatService.markMessageAsRead(entryMsgIdUnread.getKey());
                } else {
                    mFileTransferService.markFileTransferAsRead(entryMsgIdUnread.getKey());
                }
            }
            if (OneToOneChatIntent.ACTION_MESSAGE_DELIVERY_EXPIRED.equals(intent.getAction())) {
                processUndeliveredMessages(displayName);
            }
            if (FileTransferIntent.ACTION_FILE_TRANSFER_DELIVERY_EXPIRED.equals(intent.getAction())) {
                processUndeliveredFileTransfers(displayName);
            }
            return true;

        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
            return false;
        }
    }

    private void processUndeliveredFileTransfers(String displayName) {
        /* Do not propose to clear undelivered if a dialog is already opened */
        if (mClearUndeliveredAlertDialog == null) {
            mClearUndeliveredAlertDialog = popUpToClearMessageDeliveryExpiration(this,
                    getString(R.string.title_undelivered_filetransfer),
                    getString(R.string.label_undelivered_filetransfer, displayName),
                    mClearUndeliveredFileTransferClickListener, mClearUndeliveredCancelListener);
            registerDialog(mClearUndeliveredAlertDialog);
        }
    }

    private void processUndeliveredMessages(String displayName) {
        /* Do not propose to clear undelivered if a dialog is already opened */
        if (mClearUndeliveredAlertDialog == null) {
            mClearUndeliveredAlertDialog = popUpToClearMessageDeliveryExpiration(this,
                    getString(R.string.title_undelivered_message),
                    getString(R.string.label_undelivered_message, displayName),
                    mClearUndeliveredMessageClickListener, mClearUndeliveredCancelListener);
            registerDialog(mClearUndeliveredAlertDialog);
        }
    }

    private void loadConversation(ContactId newContact) throws RcsServiceNotAvailableException,
            RcsGenericException {
        boolean firstLoad = (mChat == null);
        /* Save contact ID */
        mContact = newContact;
        /*
         * Open chat so that if the parameter IM SESSION START is 0 then the session is accepted
         * now.
         */
        mChat = mChatService.getOneToOneChat(mContact);
        setCursorLoader(firstLoad);

        sChatIdOnForeground = mContact.toString();
        if (mCapabilityService == null) {
            mCapabilityService = getCapabilityApi();
        }
        try {
            /* Request options for this new contact */
            Set<ContactId> setOfContact = new HashSet<>();
            setOfContact.add(mContact);
            mCapabilityService.requestContactCapabilities(setOfContact);

        } catch (RcsServiceNotRegisteredException e) {
            showMessage(R.string.error_not_registered);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        /* Create a new CursorLoader with the following query parameters. */
        return new CursorLoader(this, mUriHistoryProvider, PROJ_CHAT_MSG, WHERE_CLAUSE,
                new String[] {
                    mContact.toString()
                }, ORDER_CHAT_MSG);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        // Get the list item position
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        Cursor cursor = (Cursor) mAdapter.getItem(info.position);
        // Adapt the contextual menu according to the selected item
        menu.add(0, MENU_ITEM_DELETE, MENU_ITEM_DELETE, R.string.menu_delete_message);
        Direction direction = Direction.valueOf(cursor.getInt(cursor
                .getColumnIndexOrThrow(HistoryLog.DIRECTION)));
        if (Direction.OUTGOING != direction) {
            return;
        }
        int providerId = cursor.getInt(cursor.getColumnIndexOrThrow(HistoryLog.PROVIDER_ID));
        if (ChatLog.Message.HISTORYLOG_MEMBER_ID == providerId) {
            Content.Status status = Content.Status.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(HistoryLog.STATUS)));
            if (Content.Status.FAILED == status) {
                menu.add(0, MENU_ITEM_RESEND, MENU_ITEM_RESEND, R.string.menu_resend_message);
            }
            // TODO depending on mime-type allow user to view file image

        } else if (FileTransferLog.HISTORYLOG_MEMBER_ID == providerId) {
            FileTransfer.State state = FileTransfer.State.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(HistoryLog.STATUS)));
            if (FileTransfer.State.FAILED == state) {
                menu.add(0, MENU_ITEM_RESEND, MENU_ITEM_RESEND, R.string.menu_resend_message);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        Cursor cursor = (Cursor) (mAdapter.getItem(info.position));
        int providerId = cursor.getInt(cursor.getColumnIndexOrThrow(HistoryLog.PROVIDER_ID));
        String messageId = cursor.getString(cursor.getColumnIndexOrThrow(HistoryLog.ID));
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onContextItemSelected Id=".concat(messageId));
        }
        switch (item.getItemId()) {
            case MENU_ITEM_RESEND:
                try {
                    if (ChatLog.Message.HISTORYLOG_MEMBER_ID == providerId) {
                        mChat.resendMessage(messageId);
                    } else {
                        FileTransfer fileTransfer = mFileTransferService.getFileTransfer(messageId);
                        if (fileTransfer != null) {
                            fileTransfer.resendTransfer();
                        }
                    }
                } catch (RcsServiceException e) {
                    showException(e);
                }
                return true;

            case MENU_ITEM_DELETE:
                try {
                    if (ChatLog.Message.HISTORYLOG_MEMBER_ID == providerId) {
                        mChatService.deleteMessage(messageId);
                    } else {
                        mFileTransferService.deleteFileTransfer(messageId);
                    }
                } catch (RcsServiceException e) {
                    showException(e);
                }
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * Forge intent to start SingleChatView activity
     * 
     * @param context The context
     * @param contact The contact ID
     * @return intent
     */
    public static Intent forgeIntentToOpenConversation(Context context, ContactId contact) {
        Intent intent = new Intent(context, SingleChatView.class);
        intent.setAction(OPEN_ONE_TO_ONE_CHAT_CONVERSATION);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_CONTACT, (Parcelable) contact);
        return intent;
    }

    /**
     * Forge intent to start SingleChatView activity
     * 
     * @param ctx The context
     * @param contact The contact ID
     * @param intent intent
     * @return intent
     */
    public static Intent forgeIntentOnStackEvent(Context ctx, ContactId contact, Intent intent) {
        intent.setClass(ctx, SingleChatView.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_CONTACT, (Parcelable) contact);
        return intent;
    }

    /**
     * Get unread messages for contact
     * 
     * @param contact contact ID
     * @return Map of unread message IDs associated with the provider ID
     */
    private Map<String, Integer> getUnreadMessageIds(ContactId contact) {
        Map<String, Integer> unReadMessageIDs = new HashMap<>();
        String[] where_args = new String[] {
            contact.toString()
        };
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(mUriHistoryProvider, PROJECTION_UNREAD_MESSAGE,
                    UNREADS_WHERE_CLAUSE, where_args, ORDER_CHAT_MSG);
            if (!cursor.moveToFirst()) {
                return unReadMessageIDs;
            }
            int msgIdcolumIdx = cursor.getColumnIndexOrThrow(HistoryLog.ID);
            int providerIdColumIdx = cursor.getColumnIndexOrThrow(HistoryLog.PROVIDER_ID);
            do {
                unReadMessageIDs.put(cursor.getString(msgIdcolumIdx),
                        cursor.getInt(providerIdColumIdx));
            } while (cursor.moveToNext());
            return unReadMessageIDs;

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public ChatMessage sendMessage(String message) throws RcsServiceException {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "sendTextMessage: ".concat(message));
        }
        return mChat.sendMessage(message);
    }

    @Override
    public ChatMessage sendMessage(Geoloc geoloc) throws RcsServiceException {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "sendGeolocMessage: ".concat(geoloc.toString()));
        }
        return mChat.sendMessage(geoloc);
    }

    @Override
    public void addChatEventListener(ChatService chatService) throws RcsServiceException {
        mChatService.addEventListener(mChatListener);
    }

    @Override
    public void removeChatEventListener(ChatService chatService) throws RcsServiceException {
        mChatService.removeEventListener(mChatListener);
    }

    @Override
    public INotifyComposing getNotifyComposing() {
        return new INotifyComposing() {
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(getApplicationContext());
        inflater.inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_insert_smiley:
                Smileys.showSmileyDialog(this, mComposeText, getResources(),
                        getString(R.string.menu_insert_smiley));
                break;

            case R.id.menu_quicktext:
                addQuickText();
                break;

            case R.id.menu_send_geoloc:
                getGeoLoc();
                break;

            case R.id.menu_showus_map:
                DisplayGeoloc.showContactOnMap(this, mContact);
                break;

            case R.id.menu_send_file:
                SendSingleFile.startActivity(this, mContact);
                break;
        }
        return true;
    }

    @Override
    public boolean isSingleChat() {
        return true;
    }

    private Set<String> getUndeliveredMessages(ContactId contact) {
        Set<String> messageIds = new HashSet<>();
        Cursor cursor = null;
        try {
            cursor = this.getContentResolver().query(ChatLog.Message.CONTENT_URI,
                    PROJ_UNDELIVERED_MSG, SEL_UNDELIVERED_MESSAGES, new String[] {
                        contact.toString()
                    }, null);
            if (!cursor.moveToFirst()) {
                return messageIds;
            }
            int messageIdColumnIdx = cursor.getColumnIndexOrThrow(ChatLog.Message.MESSAGE_ID);
            do {
                messageIds.add(cursor.getString(messageIdColumnIdx));
            } while (cursor.moveToNext());
            return messageIds;

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private Set<String> getUndeliveredFileTransfers(ContactId contact) {
        Set<String> ids = new HashSet<>();
        Cursor cursor = null;
        try {
            cursor = this.getContentResolver().query(FileTransferLog.CONTENT_URI,
                    PROJ_UNDELIVERED_FT, SEL_UNDELIVERED_FTS, new String[] {
                        contact.toString()
                    }, null);
            if (!cursor.moveToFirst()) {
                return ids;
            }
            int idColumnIdx = cursor.getColumnIndexOrThrow(FileTransferLog.FT_ID);
            do {
                ids.add(cursor.getString(idColumnIdx));
            } while (cursor.moveToNext());
            return ids;

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private AlertDialog popUpToClearMessageDeliveryExpiration(Context ctx, String title,
            String msg, OnClickListener onClickiLstener, OnCancelListener onCancelListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setMessage(msg);
        builder.setTitle(title);
        builder.setOnCancelListener(onCancelListener);
        builder.setPositiveButton(R.string.label_ok, onClickiLstener);
        return builder.show();
    }

    @Override
    public void initialize() {
        mClearUndeliveredMessageClickListener = new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    Set<String> msgIds = getUndeliveredMessages(mContact);
                    mChatService.clearMessageDeliveryExpiration(msgIds);
                    if (LogUtils.isActive) {
                        Log.d(LOGTAG, "clearMessageDeliveryExpiration ".concat(msgIds.toString()));
                    }
                } catch (RcsServiceException e) {
                    showException(e);
                } finally {
                    mClearUndeliveredAlertDialog = null;
                }

            }
        };

        mClearUndeliveredFileTransferClickListener = new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    Set<String> fileTransfersIds = getUndeliveredFileTransfers(mContact);
                    mFileTransferService.clearFileTransferDeliveryExpiration(fileTransfersIds);
                    if (LogUtils.isActive) {
                        Log.d(LOGTAG, "clearFileTransferDeliveryExpiration "
                                .concat(fileTransfersIds.toString()));
                    }
                } catch (RcsServiceException e) {
                    showException(e);
                } finally {
                    mClearUndeliveredAlertDialog = null;
                }

            }
        };
        mClearUndeliveredCancelListener = new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                mClearUndeliveredAlertDialog = null;
            }
        };

        mChatListener = new OneToOneChatListener() {

            /* Callback called when an Is-composing event has been received */
            @Override
            public void onComposingEvent(ContactId contact, boolean status) {
                /* Discard event if not for current contact */
                if (!mContact.equals(contact)) {
                    return;

                }
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onComposingEvent contact=" + contact + " status=" + status);
                }
                displayComposingEvent(contact, status);
            }

            @Override
            public void onMessageStatusChanged(ContactId contact, String mimeType, String msgId,
                    Content.Status status, Content.ReasonCode reasonCode) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onMessageStatusChanged contact=" + contact + " mime-type="
                            + mimeType + " msgId=" + msgId + " status=" + status);
                }
            }

            @Override
            public void onMessagesDeleted(ContactId contact, Set<String> msgIds) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onMessagesDeleted contact=" + contact + " for message IDs="
                            + Arrays.toString(msgIds.toArray()));
                }
            }

        };
    }
}
