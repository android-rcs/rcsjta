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
import com.gsma.services.rcs.RcsPersistentStorageException;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.RcsServiceNotRegisteredException;
import com.gsma.services.rcs.capability.CapabilityService;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatLog.Message;
import com.gsma.services.rcs.chat.ChatLog.Message.Content;
import com.gsma.services.rcs.chat.ChatMessage;
import com.gsma.services.rcs.chat.ChatService;
import com.gsma.services.rcs.chat.ChatServiceConfiguration;
import com.gsma.services.rcs.chat.OneToOneChat;
import com.gsma.services.rcs.chat.OneToOneChatIntent;
import com.gsma.services.rcs.chat.OneToOneChatListener;
import com.gsma.services.rcs.contact.ContactId;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.messaging.chat.ChatView;
import com.orangelabs.rcs.ri.messaging.chat.IsComposingManager;
import com.orangelabs.rcs.ri.messaging.chat.IsComposingManager.INotifyComposing;
import com.orangelabs.rcs.ri.messaging.geoloc.DisplayGeoloc;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsContactUtil;
import com.orangelabs.rcs.ri.utils.Smileys;
import com.orangelabs.rcs.ri.utils.Utils;

import android.app.Activity;
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
import java.util.HashSet;
import java.util.Set;

/**
 * Single chat view
 */
public class SingleChatView extends ChatView {

    private final static String EXTRA_CONTACT = "contact";

    private final static String[] PROJ_UNDELIVERED_MSG = new String[] {
        ChatLog.Message.MESSAGE_ID
    };

    private ContactId mContact;

    private OneToOneChat mChat;

    /**
     * ContactId of the displayed single chat
     */
    /* private package */static ContactId contactOnForeground;

    /**
     * List of items for contextual menu
     */
    private final static int CHAT_MENU_ITEM_DELETE = 0;

    private final static int CHAT_MENU_ITEM_RESEND = 1;

    private final static int CHAT_MENU_ITEM_REVOKE = 2;

    private static final String LOGTAG = LogUtils.getTag(SingleChatView.class.getSimpleName());

    /**
     * Chat_id is set to contact id for one to one chat messages.
     */
    private static final String WHERE_CLAUSE = new StringBuilder(Message.CHAT_ID)
            .append("=? AND (").append(Message.MIME_TYPE).append("='")
            .append(Message.MimeType.GEOLOC_MESSAGE).append("' OR ").append(Message.MIME_TYPE)
            .append("='").append(Message.MimeType.TEXT_MESSAGE).append("')").toString();

    private final static String UNREADS_WHERE_CLAUSE = new StringBuilder(Message.CHAT_ID)
            .append("=? AND ").append(Message.READ_STATUS).append("=")
            .append(ReadStatus.UNREAD.toInt()).append(" AND (").append(Message.MIME_TYPE)
            .append("='").append(Message.MimeType.GEOLOC_MESSAGE).append("' OR ")
            .append(Message.MIME_TYPE).append("='").append(Message.MimeType.TEXT_MESSAGE)
            .append("')").toString();

    private final static String[] PROJECTION_MSG_ID = new String[] {
        Message.MESSAGE_ID
    };

    private static final String OPEN_ONE_TO_ONE_CHAT_CONVERSATION = "open_one_to_one_conversation";

    private static final String SEL_UNDELIVERED_MESSAGES = new StringBuilder(
            ChatLog.Message.CHAT_ID).append("=? AND ").append(ChatLog.Message.EXPIRED_DELIVERY)
            .append("='1'").toString();

    private OneToOneChatListener mChatListener;

    private CapabilityService mCapabilityService;

    private AlertDialog mClearUndeliveredAlertDialog;

    private OnCancelListener mClearUndeliveredMessageCancelListener;

    private OnClickListener mClearUndeliveredMessageClickListener;

    private String mDisplayName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mExitOnce.isLocked()) {
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

        } catch (RcsServiceNotAvailableException e) {
            Utils.showMessageAndExit(this, getString(R.string.label_api_unavailable), mExitOnce, e);
        } catch (RcsServiceException e) {
            Utils.showMessageAndExit(this, getString(R.string.label_api_failed), mExitOnce, e);
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
                if (LogUtils.isActive) {
                    Log.e(LOGTAG, "onComposing failed", e);
                }
            }
        }
        super.onDestroy();
        contactOnForeground = null;
    }

    @Override
    public boolean processIntent(Intent intent) {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "processIntent ".concat(intent.getAction()));
        }
        ContactId newContact = (ContactId) intent.getParcelableExtra(EXTRA_CONTACT);
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
            mDisplayName = RcsContactUtil.getInstance(this).getDisplayName(mContact);
            setTitle(getString(R.string.title_chat, mDisplayName));
            /* Mark as read messages if required */
            Set<String> msgIdUnreads = getUnreadMessageIds(mContact);
            for (String msgId : msgIdUnreads) {
                mChatService.markMessageAsRead(msgId);
            }
            if (OneToOneChatIntent.ACTION_MESSAGE_DELIVERY_EXPIRED.equals(intent.getAction())) {
                processUndeliveredMessages(mDisplayName);
            }
            return true;

        } catch (RcsServiceNotAvailableException e) {
            Utils.showMessageAndExit(this, getString(R.string.label_api_unavailable), mExitOnce, e);
            return false;

        } catch (RcsServiceException e) {
            Utils.showMessageAndExit(this, getString(R.string.label_api_failed), mExitOnce, e);
            return false;
        }
    }

    private void processUndeliveredMessages(String displayName) {
        /* Do not propose to clear undelivered if a dialog is already opened */
        if (mClearUndeliveredAlertDialog == null) {
            mClearUndeliveredAlertDialog = popUpToClearMessageDeliveryExpiration(this,
                    getString(R.string.title_undelivered_message),
                    getString(R.string.label_undelivered_message, displayName),
                    mClearUndeliveredMessageClickListener, mClearUndeliveredMessageCancelListener);
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
        if (firstLoad) {
            /*
             * Initialize the Loader with id '1' and callbacks 'mCallbacks'.
             */
            getSupportLoaderManager().initLoader(LOADER_ID, null, this);
        } else {
            /* We switched from one contact to another: reload history since */
            getSupportLoaderManager().restartLoader(LOADER_ID, null, this);
        }
        contactOnForeground = mContact;
        if (mCapabilityService == null) {
            mCapabilityService = mCnxManager.getCapabilityApi();
        }
        try {
            /* Request options for this new contact */
            mCapabilityService.requestContactCapabilities(mContact);
        } catch (RcsServiceNotRegisteredException e) {
            if (LogUtils.isActive) {
                Log.w(LOGTAG, "RcsServiceNotRegisteredException: " + e.getMessage());
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        /* Create a new CursorLoader with the following query parameters. */
        return new CursorLoader(this, Message.CONTENT_URI, PROJ_CHAT_MSG, WHERE_CLAUSE,
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
        menu.add(0, CHAT_MENU_ITEM_DELETE, CHAT_MENU_ITEM_DELETE, R.string.menu_delete_message);
        Direction direction = Direction.valueOf(cursor.getInt(cursor
                .getColumnIndexOrThrow(Message.DIRECTION)));
        if (Direction.OUTGOING != direction) {
            return;

        }
        Content.Status status = Content.Status.valueOf(cursor.getInt(cursor
                .getColumnIndexOrThrow(Message.STATUS)));
        switch (status) {
            case FAILED:
                menu.add(0, CHAT_MENU_ITEM_RESEND, CHAT_MENU_ITEM_RESEND,
                        R.string.menu_resend_message);
                break;
            case DISPLAY_REPORT_REQUESTED:
            case DELIVERED:
            case SENT:
            case SENDING:
            case QUEUED:
                menu.add(0, CHAT_MENU_ITEM_REVOKE, CHAT_MENU_ITEM_REVOKE,
                        R.string.menu_revoke_message);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        Cursor cursor = (Cursor) (mAdapter.getItem(info.position));
        String messageId = cursor.getString(cursor.getColumnIndexOrThrow(Message.MESSAGE_ID));
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onContextItemSelected msgId=".concat(messageId));
        }
        switch (item.getItemId()) {
            case CHAT_MENU_ITEM_RESEND:
                try {
                    mChat.resendMessage(messageId);
                } catch (Exception e) {
                    if (LogUtils.isActive) {
                        Log.e(LOGTAG, "resend message failed", e);
                    }
                }
                return true;

            case CHAT_MENU_ITEM_REVOKE:
                return true;

            case CHAT_MENU_ITEM_DELETE:
                try {
                    mChatService.deleteMessage(messageId);
                } catch (Exception e) {
                    if (LogUtils.isActive) {
                        Log.e(LOGTAG, "delete message failed", e);
                    }
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
     * @param intent
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
     * @param contact
     * @return set of unread message IDs
     */
    private Set<String> getUnreadMessageIds(ContactId contact) {
        Set<String> unReadMessageIDs = new HashSet<String>();
        String[] where_args = new String[] {
            contact.toString()
        };
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(Message.CONTENT_URI, PROJECTION_MSG_ID,
                    UNREADS_WHERE_CLAUSE, where_args, ORDER_CHAT_MSG);
            int columIndex = cursor.getColumnIndexOrThrow(Message.MESSAGE_ID);
            while (cursor.moveToNext()) {
                unReadMessageIDs.add(cursor.getString(columIndex));
            }
        } catch (Exception e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Exception occurred", e);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return unReadMessageIDs;
    }

    @Override
    public ChatMessage sendMessage(String message) {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "sendTextMessage: ".concat(message));
        }
        try {
            return mChat.sendMessage(message);

        } catch (Exception e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "sendTextMessage failed", e);
            }
            return null;
        }
    }

    @Override
    public ChatMessage sendMessage(Geoloc geoloc) {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "sendGeolocMessage: ".concat(geoloc.toString()));
        }
        try {
            return mChat.sendMessage(geoloc);

        } catch (Exception e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "sendMessage failed", e);
            }
            return null;
        }
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
        INotifyComposing notifyComposing = new IsComposingManager.INotifyComposing() {
            public void setTypingStatus(boolean isTyping) {
                try {
                    if (mChat == null) {
                        return;

                    }
                    mChat.setComposingStatus(isTyping);
                    if (LogUtils.isActive) {
                        Boolean _isTyping = Boolean.valueOf(isTyping);
                        Log.d(LOGTAG, "sendIsComposingEvent ".concat(_isTyping.toString()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        return notifyComposing;
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
        Set<String> messageIds = new HashSet<String>();
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

    private AlertDialog popUpToClearMessageDeliveryExpiration(Activity activity, String title,
            String msg, OnClickListener onClickiLstener, OnCancelListener onCancelListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(msg);
        builder.setTitle(title);
        builder.setOnCancelListener(onCancelListener);
        builder.setPositiveButton(activity.getString(R.string.label_ok), onClickiLstener);
        AlertDialog alert = builder.create();
        alert.show();
        return alert;
    }

    @Override
    public void initialize() {
        mClearUndeliveredMessageClickListener = new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    Set<String> msgIds = SingleChatView.this.getUndeliveredMessages(mContact);
                    mChatService.clearMessageDeliveryExpiration(msgIds);
                    if (LogUtils.isActive) {
                        Log.d(LOGTAG, "clearMessageDeliveryExpiration ".concat(msgIds.toString()));
                    }
                } catch (RcsServiceNotAvailableException e) {
                    Utils.displayToast(SingleChatView.this, e);
                } catch (RcsPersistentStorageException e) {
                    Utils.displayToast(SingleChatView.this, e);
                } catch (RcsGenericException e) {
                    Utils.displayToast(SingleChatView.this, e);
                } finally {
                    mClearUndeliveredAlertDialog = null;
                }

            }
        };
        mClearUndeliveredMessageCancelListener = new OnCancelListener() {

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
