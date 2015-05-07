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

package com.orangelabs.rcs.ri.messaging.chat.group;

import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
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
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.messaging.GroupDeliveryInfoList;
import com.orangelabs.rcs.ri.messaging.chat.ChatMessageDAO;
import com.orangelabs.rcs.ri.messaging.chat.ChatView;
import com.orangelabs.rcs.ri.messaging.chat.IsComposingManager;
import com.orangelabs.rcs.ri.messaging.chat.IsComposingManager.INotifyComposing;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsDisplayName;
import com.orangelabs.rcs.ri.utils.Smileys;
import com.orangelabs.rcs.ri.utils.Utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Group chat view
 */
public class GroupChatView extends ChatView {
    /**
     * Intent parameters
     */
    private static final String BUNDLE_CHATMESSAGE_DAO_ID = "ChatMessageDao";

    private static final String BUNDLE_GROUPCHAT_DAO_ID = "GroupChatDao";

    private final static String EXTRA_CHAT_ID = "chat_id";

    private final static String EXTRA_PARTICIPANTS = "participants";

    private final static String EXTRA_SUBJECT = "subject";

    private final static String EXTRA_MODE = "mode";

    private enum GroupChatMode {
        INCOMING, OUTGOING, OPEN
    };

    /**
     * List of items for contextual menu
     */
    private final static int GROUPCHAT_MENU_ITEM_DELETE = 0;

    private final static int GROUPCHAT_MENU_ITEM_VIEW_GC_INFO = 1;

    private static final String WHERE_CLAUSE = new StringBuilder(Message.CHAT_ID)
            .append("=? AND (").append(Message.MIME_TYPE).append("='")
            .append(Message.MimeType.GEOLOC_MESSAGE).append("' OR ").append(Message.MIME_TYPE)
            .append("='").append(Message.MimeType.TEXT_MESSAGE).append("' OR ")
            .append(Message.MIME_TYPE).append("='").append(Message.MimeType.GROUPCHAT_EVENT)
            .append("')").toString();

    /**
     * Subject
     */
    private String mSubject;

    /**
     * Chat ID
     */
    private String mChatId;

    /**
     * The Group chat session instance
     */
    private GroupChat mGroupChat;

    /**
     * Progress dialog
     */
    private Dialog mProgressDialog;

    /**
     * List of participants
     */
    private Set<ContactId> mParticipants = new HashSet<ContactId>();

    /**
     * Chat ID of the displayed conversation
     */
    /* package private */static String chatIdOnForeground;

    private static final String LOGTAG = LogUtils.getTag(GroupChatView.class.getSimpleName());

    /**
     * Group chat listener
     */
    private GroupChatListener mListener = new GroupChatListener() {

        @Override
        public void onMessageStatusChanged(String chatId, String mimeType, String msgId,
                Content.Status status, Content.ReasonCode reasonCode) {
            if (LogUtils.isActive) {
                Log.i(LOGTAG, new StringBuilder("onMessageStatusChanged chatId=").append(chatId)
                        .append(" mime-type=").append(mimeType).append(" msgId=").append(msgId)
                        .append(" status=").append(status).append(" reason=").append(reasonCode)
                        .toString());
            }
        }

        // Callback called when an Is-composing event has been received
        public void onComposingEvent(String chatId, ContactId contact, boolean status) {
            // Discard event if not for current chatId
            if (!mChatId.equals(chatId)) {
                return;

            }
            displayComposingEvent(contact, status);
        }

        @Override
        public void onParticipantStatusChanged(String chatId, ContactId contact,
                ParticipantStatus status) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "onParticipantStatusChanged chatId=" + chatId + " contact=" + contact
                        + " status=" + status);
            }
        }

        @Override
        public void onMessageGroupDeliveryInfoChanged(String chatId, ContactId contact,
                String mimeType, String msgId, GroupDeliveryInfo.Status status,
                GroupDeliveryInfo.ReasonCode reasonCode) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG,
                        new StringBuilder("onMessageGroupDeliveryInfoChanged chatId=")
                                .append(chatId).append(" contact=").append(contact)
                                .append(" msgId=").append(msgId).append(" status=").append(status)
                                .append(" reason=").append(reasonCode).toString());
            }
        }

        @Override
        public void onStateChanged(String chatId, final GroupChat.State state,
                GroupChat.ReasonCode reasonCode) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG,
                        new StringBuilder("onStateChanged chatId=").append(chatId)
                                .append(" state=").append(state).append(" reason=")
                                .append(reasonCode).toString());
            }
            /* Discard event if not for current chatId */
            if (mChatId == null || !mChatId.equals(chatId)) {
                return;

            }
            final String _reasonCode = RiApplication.sGroupChatReasonCodes[reasonCode.toInt()];
            handler.post(new Runnable() {
                public void run() {
                    switch (state) {
                        case STARTED:
                            /* Session is well established : hide progress dialog. */
                            hideProgressDialog();
                            break;

                        case ABORTED:
                            /* Session is aborted: hide progress dialog then exit. */
                            hideProgressDialog();
                            Utils.showMessageAndExit(GroupChatView.this,
                                    getString(R.string.label_chat_aborted, _reasonCode), mExitOnce);
                            break;

                        case REJECTED:
                            /* Session is rejected: hide progress dialog then exit. */
                            hideProgressDialog();
                            Utils.showMessageAndExit(GroupChatView.this,
                                    getString(R.string.label_chat_rejected, _reasonCode), mExitOnce);
                            break;

                        case FAILED:
                            /* Session is failed: hide progress dialog then exit. */
                            hideProgressDialog();
                            Utils.showMessageAndExit(GroupChatView.this,
                                    getString(R.string.label_chat_failed, _reasonCode), mExitOnce);
                            break;

                        default:
                    }
                }
            });
        }

        @Override
        public void onDeleted(Set<String> chatIds) {
            if (LogUtils.isActive) {
                Log.i(LOGTAG, new StringBuilder("onDeleted chatIds=").append(chatIds).toString());
            }
        }

        @Override
        public void onMessagesDeleted(String chatId, Set<String> msgIds) {
            if (LogUtils.isActive) {
                Log.i(LOGTAG,
                        new StringBuilder("onMessagesDeleted chatId=").append(chatId)
                                .append(" msgIds=").append(msgIds).toString());
            }
        }

    };

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        /* Get the list item position. */
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        Cursor cursor = (Cursor) mAdapter.getItem(info.position);
        menu.add(0, GROUPCHAT_MENU_ITEM_DELETE, 0, R.string.menu_delete_message);
        Direction direction = Direction.valueOf(cursor.getInt(cursor
                .getColumnIndexOrThrow(Message.DIRECTION)));
        if (Direction.OUTGOING == direction) {
            menu.add(0, GROUPCHAT_MENU_ITEM_VIEW_GC_INFO, 1, R.string.menu_view_groupdelivery);
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
            case GROUPCHAT_MENU_ITEM_VIEW_GC_INFO:
                GroupDeliveryInfoList.startActivity(this, messageId);
                return true;

            case GROUPCHAT_MENU_ITEM_DELETE:
                // TODO CR005 delete methods
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ChatService chatService = mCnxManager.getChatApi();
        try {
            addChatEventListener(chatService);
            ChatServiceConfiguration configuration = chatService.getConfiguration();
            // Set max label length
            int maxMsgLength = configuration.getGroupChatMessageMaxLength();
            if (maxMsgLength > 0) {
                // Set the message composer max length
                InputFilter[] filterArray = new InputFilter[1];
                filterArray[0] = new InputFilter.LengthFilter(maxMsgLength);
                composeText.setFilters(filterArray);
            }
            // Instantiate the composing manager
            composingManager = new IsComposingManager(configuration.getIsComposingTimeout(),
                    getNotifyComposing());
        } catch (RcsServiceNotAvailableException e) {
            Utils.showMessageAndExit(this, getString(R.string.label_api_unavailable), mExitOnce);
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
        if (mGroupChat != null) {
            try {
                mGroupChat.setComposingStatus(false);
            } catch (Exception e) {
                if (LogUtils.isActive) {
                    Log.e(LOGTAG, "onComposing failed", e);
                }
            }
        }
        super.onDestroy();
        chatIdOnForeground = null;
    }

    @Override
    public boolean processIntent() {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "processIntent");
        }
        try {
            switch ((GroupChatMode) getIntent().getSerializableExtra(EXTRA_MODE)) {
                case OUTGOING:
                    /* Initiate a Group Chat: Get subject */
                    mSubject = getIntent().getStringExtra(GroupChatView.EXTRA_SUBJECT);
                    updateGroupChatViewTitle(mSubject);

                    // Get participants
                    ContactUtil contactUtil = ContactUtil.getInstance(this);
                    List<String> contacts = getIntent().getStringArrayListExtra(
                            GroupChatView.EXTRA_PARTICIPANTS);
                    if (contacts == null || contacts.isEmpty()) {
                        Utils.showMessageAndExit(this, getString(R.string.label_invalid_contacts),
                                mExitOnce);
                        return false;

                    }

                    for (String contact : contacts) {
                        mParticipants.add(contactUtil.formatContact(contact));
                    }
                    if (mParticipants.isEmpty()) {
                        Utils.showMessageAndExit(this, getString(R.string.label_invalid_contacts),
                                mExitOnce);
                        return false;

                    }

                    // Initiate group chat
                    return startGroupChat();

                case OPEN:
                    // Open an existing session from the history log
                    mChatId = getIntent().getStringExtra(GroupChatView.EXTRA_CHAT_ID);

                    // Get chat session
                    mGroupChat = mCnxManager.getChatApi().getGroupChat(mChatId);
                    if (mGroupChat == null) {
                        if (LogUtils.isActive) {
                            Log.e(LOGTAG,
                                    "processIntent session not found for chatId=".concat(mChatId));
                        }
                        Utils.showMessageAndExit(this, getString(R.string.label_session_not_found),
                                mExitOnce);
                        return false;

                    }
                    getSupportLoaderManager().initLoader(LOADER_ID, null, this);

                    chatIdOnForeground = mChatId;

                    // Get subject
                    mSubject = mGroupChat.getSubject();
                    updateGroupChatViewTitle(mSubject);

                    // Set list of participants
                    mParticipants = mGroupChat.getParticipants().keySet();
                    if (LogUtils.isActive) {
                        if (mParticipants == null) {
                            Log.e(LOGTAG,
                                    new StringBuilder("processIntent chatId=").append(mChatId)
                                            .append(" subject='").append(mSubject).append("'")
                                            .toString());
                        }
                    }
                    return true;

                case INCOMING:
                    ChatMessageDAO message = (ChatMessageDAO) (getIntent().getExtras()
                            .getParcelable(BUNDLE_CHATMESSAGE_DAO_ID));
                    if (message != null) {
                        // It is a new message: check if for the displayed
                        // conversation
                        if (message.getChatId().equals(mChatId) || mChatId == null) {
                            // Mark the message as read
                            mCnxManager.getChatApi().markMessageAsRead(message.getMsgId());
                            if (mChatId != null) {
                                return true;
                            }
                            mChatId = message.getChatId();
                        } else {
                            // Ignore message if it does not belong to current
                            // GC
                            if (LogUtils.isActive) {
                                Log.d(LOGTAG,
                                        new StringBuilder("processIntent discard chat message ")
                                                .append(message.getMsgId()).append(" for chatId ")
                                                .append(message.getChatId()).toString());
                            }
                            return true;

                        }
                    } else {
                        // New GC invitation
                        mChatId = getIntent().getStringExtra(GroupChatIntent.EXTRA_CHAT_ID);
                    }
                    mGroupChat = mCnxManager.getChatApi().getGroupChat(mChatId);
                    if (mGroupChat == null) {
                        Utils.showMessageAndExit(this, getString(R.string.label_session_not_found),
                                mExitOnce);
                        return false;

                    }
                    getSupportLoaderManager().initLoader(LOADER_ID, null, this);
                    chatIdOnForeground = mChatId;
                    // Get remote contact
                    ContactId contact = null; // mGroupChat.getRemoteContact();
                    // Get subject
                    mSubject = mGroupChat.getSubject();
                    updateGroupChatViewTitle(mSubject);
                    // Set list of participants
                    mParticipants = mGroupChat.getParticipants().keySet();
                    // Display accept/reject dialog
                    // TODO manage new state ACCEPTING and REJECTED
                    if (GroupChat.State.INVITED == mGroupChat.getState()) {
                        displayAcceptRejectDialog(contact);
                    }
                    if (LogUtils.isActive) {
                        Log.d(LOGTAG, "New group chat for chatId ".concat(mChatId));
                    }
                    return true;

            }
        } catch (RcsServiceNotAvailableException e) {
            Utils.showMessageAndExit(this, getString(R.string.label_api_unavailable), mExitOnce, e);
        } catch (RcsServiceException e) {
            Utils.showMessageAndExit(this, getString(R.string.label_api_failed), mExitOnce, e);
        }
        return false;
    }

    /**
     * Update the view title
     *
     * @param subject the group chat subject or null
     */
    private void updateGroupChatViewTitle(String subject) {
        // Set title
        if (!TextUtils.isEmpty(subject)) {
            setTitle(new StringBuilder(getString(R.string.title_group_chat)).append(" '")
                    .append(mSubject).append("'").toString());
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle arg) {
        // Create a new CursorLoader with the following query parameters.
        CursorLoader loader = new CursorLoader(this, Message.CONTENT_URI, PROJ_CHAT_MSG,
                WHERE_CLAUSE, new String[] {
                    mChatId
                }, ORDER_CHAT_MSG);
        return loader;
    }

    /**
     * Display notification to accept or reject invitation
     *
     * @param remote remote contact
     */
    private void displayAcceptRejectDialog(ContactId remote) {
        // Manual accept
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_group_chat);
        String from = RcsDisplayName.getInstance(this).getDisplayName(remote);
        String topic = (TextUtils.isEmpty(mSubject)) ? getString(R.string.label_no_subject)
                : mSubject;
        String msg = getString(R.string.label_gc_from_subject, from, topic);
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setIcon(R.drawable.ri_notif_chat_icon);
        builder.setPositiveButton(getString(R.string.label_accept),
                new android.content.DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            // Accept the invitation
                            mGroupChat.openChat();
                        } catch (Exception e) {
                            Utils.showMessageAndExit(GroupChatView.this,
                                    getString(R.string.label_invitation_failed), mExitOnce, e);
                        }
                    }
                });
        builder.setNegativeButton(getString(R.string.label_decline),
                new android.content.DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Let session die by timeout
                        // Exit activity
                        finish();
                    }
                });
        builder.show();
    }

    /**
     * get a set of contact from a set of participant info
     *
     * @param setOfParticipant a set of participant info
     * @return a set of contact
     */
    private Set<String> getSetOfParticipants(Map<ContactId, ParticipantStatus> setOfParticipant) {
        Set<String> result = new HashSet<String>();
        if (setOfParticipant.size() != 0) {
            for (ContactId contact : setOfParticipant.keySet()) {
                // TODO consider status ?
                result.add(contact.toString());
            }
        }
        return result;
    }

    /**
     * Start the group chat
     *
     * @return True if successful
     */
    private boolean startGroupChat() {
        // Initiate the chat session in background
        try {
            mGroupChat = mCnxManager.getChatApi().initiateGroupChat(
                    new HashSet<ContactId>(mParticipants), mSubject);
            mChatId = mGroupChat.getChatId();
            getSupportLoaderManager().initLoader(LOADER_ID, null, this);
            chatIdOnForeground = mChatId;
        } catch (Exception e) {
            Utils.showMessageAndExit(this, getString(R.string.label_invitation_failed), mExitOnce,
                    e);
            return false;
        }

        // Display a progress dialog
        mProgressDialog = Utils.showProgressDialog(GroupChatView.this,
                getString(R.string.label_command_in_progress));
        mProgressDialog.setOnCancelListener(new OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                Toast.makeText(GroupChatView.this,
                        getString(R.string.label_chat_initiation_canceled), Toast.LENGTH_SHORT)
                        .show();
                quitSession();
            }
        });
        return true;
    }

    /**
     * Quit the group chat session
     */
    private void quitSession() {
        // Stop session
        try {
            if (mGroupChat != null) {
                mGroupChat.leave();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mGroupChat = null;

        // Exit activity
        finish();
    }

    /**
     * Add participants to be invited in the session
     */
    private void addParticipants() {
        // Build list of available contacts not already in the conference
        Set<ContactId> availableParticipants = new HashSet<ContactId>();
        try {
            Set<RcsContact> contacts = mCnxManager.getContactApi().getRcsContacts();
            for (RcsContact rcsContact : contacts) {
                ContactId contact = rcsContact.getContactId();
                if (mGroupChat.isAllowedToInviteParticipant(contact)) {
                    availableParticipants.add(contact);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Utils.showMessage(GroupChatView.this, getString(R.string.label_api_failed));
            return;

        }

        // Check if some participants are available
        if (availableParticipants.size() == 0) {
            Utils.showMessage(GroupChatView.this, getString(R.string.label_no_participant_found));
            return;

        }

        // Display contacts
        final List<String> selectedParticipants = new ArrayList<String>();
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
        builder.setNegativeButton(getString(R.string.label_cancel), null);
        builder.setPositiveButton(getString(R.string.label_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int position) {
                        // Add new participants in the session in background
                        try {
                            int max = mGroupChat.getMaxParticipants() - 1;
                            int connected = mGroupChat.getParticipants().size();
                            int limit = max - connected;
                            if (selectedParticipants.size() > limit) {
                                Utils.showMessage(GroupChatView.this,
                                        getString(R.string.label_max_participants));
                                return;
                            }

                            // Display a progress dialog
                            mProgressDialog = Utils.showProgressDialog(GroupChatView.this,
                                    getString(R.string.label_command_in_progress));

                            Set<ContactId> contacts = new HashSet<ContactId>();
                            ContactUtil contactUtils = ContactUtil.getInstance(GroupChatView.this);
                            for (String participant : selectedParticipants) {
                                contacts.add(contactUtils.formatContact(participant));
                            }
                            // Add participants
                            mGroupChat.inviteParticipants(contacts);

                            // Hide progress dialog
                            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                                mProgressDialog.dismiss();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                                mProgressDialog.dismiss();
                            }
                            Utils.showMessage(GroupChatView.this,
                                    getString(R.string.label_add_participant_failed));
                        }
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
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
        MenuItem menuItemLeave = menu.findItem(R.id.menu_close_session);
        try {
            if (mGroupChat != null) {
                menuItemParticipants.setEnabled(mGroupChat.isAllowedToInviteParticipants());
                menuItemLeave.setEnabled(mGroupChat.isAllowedToLeave());
            } else {
                menuItemParticipants.setEnabled(false);
                menuItemLeave.setEnabled(false);
            }
        } catch (RcsServiceException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_insert_smiley:
                Smileys.showSmileyDialog(this, composeText, getResources(),
                        getString(R.string.menu_insert_smiley));
                break;

            case R.id.menu_participants:
                try {
                    Utils.showList(this, getString(R.string.menu_participants),
                            getSetOfParticipants(mGroupChat.getParticipants()));
                } catch (RcsServiceException e) {
                    Utils.showMessageAndExit(this, getString(R.string.label_api_failed), mExitOnce,
                            e);
                }
                break;

            case R.id.menu_add_participant:
                addParticipants();
                break;

            case R.id.menu_quicktext:
                addQuickText();
                break;

            case R.id.menu_send_file:
                if (mChatId != null) {
                    SendGroupFile.startActivity(this, mChatId);
                }
                break;

            case R.id.menu_send_geoloc:
                getGeoLoc();
                break;

            case R.id.menu_showus_map:
                try {
                    showUsInMap(getSetOfParticipants(mGroupChat.getParticipants()));
                } catch (RcsServiceException e) {
                    Utils.showMessageAndExit(this, getString(R.string.label_api_failed), mExitOnce,
                            e);
                }
                break;

            case R.id.menu_close_session:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.title_chat_exit));
                builder.setPositiveButton(getString(R.string.label_ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (mGroupChat != null) {
                                    try {
                                        mGroupChat.leave();
                                    } catch (RcsServiceException e) {
                                        Utils.showMessageAndExit(GroupChatView.this,
                                                getString(R.string.label_chat_leave_failed),
                                                mExitOnce, e);
                                    }
                                }
                                // Quit the session
                                quitSession();
                            }
                        });
                builder.setNegativeButton(getString(R.string.label_cancel), null);
                builder.setCancelable(true);
                builder.show();
                break;
        }
        return true;
    }

    /**
     * Hide progress dialog
     */
    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    /**
     * Initiate a new Group Chat
     *
     * @param context context
     * @param subject subject
     * @param participants list of participants
     */
    public static void initiateGroupChat(Context context, String subject,
            ArrayList<String> participants) {
        Intent intent = new Intent(context, GroupChatView.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putStringArrayListExtra(GroupChatView.EXTRA_PARTICIPANTS, participants);
        intent.putExtra(GroupChatView.EXTRA_MODE, GroupChatMode.OUTGOING);
        intent.putExtra(GroupChatView.EXTRA_SUBJECT, subject);
        context.startActivity(intent);
    }

    /**
     * Open a Group Chat
     *
     * @param context The context.
     * @param chatId The chat ID.
     */
    public static void openGroupChat(Context context, String chatId) {
        Intent intent = new Intent(context, GroupChatView.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(GroupChatView.EXTRA_MODE, GroupChatMode.OPEN);
        intent.putExtra(GroupChatView.EXTRA_CHAT_ID, chatId);
        context.startActivity(intent);
    }

    /**
     * Forge intent to notify Group Chat message
     *
     * @param context The context.
     * @param chatMessageDAO The chat message from provider.
     * @return intent
     */
    public static Intent forgeIntentNewMessage(Context context, ChatMessageDAO chatMessageDAO) {
        Intent intent = new Intent(context, GroupChatView.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(GroupChatView.EXTRA_MODE, GroupChatMode.INCOMING);
        Bundle bundle = new Bundle();
        bundle.putParcelable(BUNDLE_CHATMESSAGE_DAO_ID, chatMessageDAO);
        intent.putExtras(bundle);
        return intent;
    }

    /**
     * Forge intent to notify new Group Chat
     *
     * @param context The context.
     * @param chatId The chat ID.
     * @param groupChatDAO The Group Chat session from provider.
     * @return intent
     */
    public static Intent forgeIntentInvitation(Context context, String chatId,
            GroupChatDAO groupChatDAO) {
        Intent intent = new Intent(context, GroupChatView.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(GroupChatView.EXTRA_MODE, GroupChatMode.INCOMING);
        intent.putExtra(GroupChatIntent.EXTRA_CHAT_ID, chatId);
        Bundle bundle = new Bundle();
        bundle.putParcelable(BUNDLE_GROUPCHAT_DAO_ID, groupChatDAO);
        intent.putExtras(bundle);
        return intent;
    }

    @Override
    public ChatMessage sendMessage(String message) {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "sendTextMessage: ".concat(message));
        }
        try {
            // Send the text to Group Chat
            return mGroupChat.sendMessage(message);
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
            // Send the geoloc to Group Chat
            return mGroupChat.sendMessage(geoloc);
        } catch (Exception e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "sendMessage failed", e);
            }
            return null;
        }
    }

    @Override
    public void addChatEventListener(ChatService chatService) throws RcsServiceException {
        mCnxManager.getChatApi().addEventListener(mListener);
    }

    @Override
    public void removeChatEventListener(ChatService chatService) throws RcsServiceException {
        mCnxManager.getChatApi().removeEventListener(mListener);
    }

    @Override
    public boolean isSingleChat() {
        return false;
    }

    @Override
    public INotifyComposing getNotifyComposing() {
        INotifyComposing notifyComposing = new IsComposingManager.INotifyComposing() {
            public void setTypingStatus(boolean isTyping) {
                try {
                    if (mGroupChat != null) {
                        mGroupChat.setComposingStatus(isTyping);
                        if (LogUtils.isActive) {
                            Log.d(LOGTAG, "sendIsComposingEvent ".concat(String.valueOf(isTyping)));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        return notifyComposing;
    }

}
