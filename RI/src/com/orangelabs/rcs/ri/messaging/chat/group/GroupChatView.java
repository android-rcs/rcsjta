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
import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsServiceException;
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
import com.gsma.services.rcs.filetransfer.FileTransferService;
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo;
import com.gsma.services.rcs.history.HistoryLog;

import com.orangelabs.rcs.api.connection.utils.ExceptionUtil;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.messaging.GroupDeliveryInfoList;
import com.orangelabs.rcs.ri.messaging.chat.ChatView;
import com.orangelabs.rcs.ri.messaging.chat.IsComposingManager;
import com.orangelabs.rcs.ri.messaging.chat.IsComposingManager.INotifyComposing;
import com.orangelabs.rcs.ri.messaging.geoloc.DisplayGeoloc;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsContactUtil;
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

import java.util.ArrayList;
import java.util.Arrays;
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
    private final static String EXTRA_PARTICIPANTS = "participants";

    private final static String EXTRA_SUBJECT = "subject";

    private final static String EXTRA_MODE = "mode";

    private enum GroupChatMode {
        INCOMING, OUTGOING, OPEN
    }

    /**
     * List of items for contextual menu
     */
    private final static int GROUPCHAT_MENU_ITEM_DELETE = 0;

    private final static int GROUPCHAT_MENU_ITEM_VIEW_GC_INFO = 1;

    private static final String WHERE_CLAUSE = HistoryLog.CHAT_ID + "=?";

    private String mSubject;

    private String mChatId;

    private GroupChat mGroupChat;

    private Dialog mProgressDialog;

    private Set<ContactId> mParticipants = new HashSet<>();

    private static final String LOGTAG = LogUtils.getTag(GroupChatView.class.getSimpleName());

    private static final String OPEN_GROUPCHAT = "OPEN_GROUPCHAT";

    private static final String INTITIATE_GROUPCHAT = "INTITIATE_GROUPCHAT";

    private GroupChatListener mChatListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isExiting()) {
            return;
        }
        try {
            addChatEventListener(mChatService);
            ChatServiceConfiguration configuration = mChatService.getConfiguration();
            /* Set max label length */
            int maxMsgLength = configuration.getGroupChatMessageMaxLength();
            if (maxMsgLength > 0) {
                // Set the message composer max length
                InputFilter[] filterArray = new InputFilter[1];
                filterArray[0] = new InputFilter.LengthFilter(maxMsgLength);
                mComposeText.setFilters(filterArray);
            }
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
        sChatIdOnForeground = mChatId;
    }

    @Override
    protected void onPause() {
        super.onPause();
        sChatIdOnForeground = null;
    }

    @Override
    public boolean processIntent(Intent intent) {
        String action = intent.getAction();
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "processIntent: " + action);
        }
        String oldChatId = mChatId;
        try {
            switch ((GroupChatMode) intent.getSerializableExtra(EXTRA_MODE)) {
                case OUTGOING:
                    /* Initiate a Group Chat: Get subject */
                    mSubject = intent.getStringExtra(GroupChatView.EXTRA_SUBJECT);
                    updateGroupChatViewTitle(mSubject);
                    /* Get the list of participants */
                    ContactUtil contactUtil = ContactUtil.getInstance(this);
                    List<String> contacts = intent
                            .getStringArrayListExtra(GroupChatView.EXTRA_PARTICIPANTS);
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
                    setCursorLoader(oldChatId == null);
                    sChatIdOnForeground = mChatId;
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
                    sChatIdOnForeground = mChatId;
                    ContactId contact = mGroupChat.getRemoteContact();
                    mSubject = mGroupChat.getSubject();
                    updateGroupChatViewTitle(mSubject);
                    mParticipants = mGroupChat.getParticipants().keySet();
                    /* Display accept/reject dialog */
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
            // TODO depending on mime-type and provider ID, allow user to view file image
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        Cursor cursor = (Cursor) (mAdapter.getItem(info.position));
        String messageId = cursor.getString(cursor.getColumnIndexOrThrow(HistoryLog.ID));
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onContextItemSelected Id=".concat(messageId));
        }
        int providerId = cursor.getInt(cursor.getColumnIndexOrThrow(HistoryLog.PROVIDER_ID));
        switch (item.getItemId()) {
            case GROUPCHAT_MENU_ITEM_VIEW_GC_INFO:
                GroupDeliveryInfoList.startActivity(this, messageId);
                return true;

            case GROUPCHAT_MENU_ITEM_DELETE:
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
            sChatIdOnForeground = mChatId;
        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
            return false;
        }
        /* Display a progress dialog waiting for the session to start */
        mProgressDialog = showProgressDialog(getString(R.string.label_command_in_progress));
        mProgressDialog.setOnCancelListener(new OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                Utils.displayToast(GroupChatView.this,
                        getString(R.string.label_chat_initiation_canceled));
                quitSession();
            }
        });
        return true;
    }

    private void quitSession() {
        try {
            /* check if the session is not already stopped */
            if (mGroupChat != null) {
                mGroupChat.leave();
            }
        } catch (RcsServiceException e) {
            showException(e);

        } finally {
            mGroupChat = null;
            finish();
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
                    ContactUtil contactUtils = ContactUtil.getInstance(GroupChatView.this);
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
            switch (item.getItemId()) {
                case R.id.menu_insert_smiley:
                    AlertDialog alert = Smileys.showSmileyDialog(this, mComposeText,
                            getResources(), getString(R.string.menu_insert_smiley));
                    registerDialog(alert);
                    break;

                case R.id.menu_participants:
                    alert = Utils.showList(this, getString(R.string.menu_participants),
                            getSetOfParticipants(mGroupChat.getParticipants()));
                    registerDialog(alert);
                    break;

                case R.id.menu_add_participant:
                    addParticipants();
                    break;

                case R.id.menu_quicktext:
                    addQuickText();
                    break;

                case R.id.menu_send_file:
                    SendGroupFile.startActivity(this, mChatId);
                    break;

                case R.id.menu_send_geoloc:
                    getGeoLoc();
                    break;

                case R.id.menu_showus_map:
                    DisplayGeoloc.showContactsOnMap(this, mGroupChat.getParticipants().keySet());
                    break;

                case R.id.menu_close_session:
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
                                    GroupChatView.this.finish();
                                }
                            });
                    builder.setNegativeButton(R.string.label_cancel, null);
                    builder.setCancelable(true);
                    registerDialog(builder.show());
                    break;
            }

        } catch (RcsServiceException e) {
            showException(e);
        }
        return true;
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    /**
     * Initiate a new Group Chat
     *
     * @param ctx context
     * @param subject subject
     * @param participants list of participants
     */
    public static void initiateGroupChat(Context ctx, String subject, ArrayList<String> participants) {
        Intent intent = new Intent(ctx, GroupChatView.class);
        intent.setAction(INTITIATE_GROUPCHAT);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putStringArrayListExtra(GroupChatView.EXTRA_PARTICIPANTS, participants);
        intent.putExtra(GroupChatView.EXTRA_MODE, GroupChatMode.OUTGOING);
        intent.putExtra(GroupChatView.EXTRA_SUBJECT, subject);
        ctx.startActivity(intent);
    }

    /**
     * Open a Group Chat
     *
     * @param ctx The context.
     * @param chatId The chat ID.
     */
    public static void openGroupChat(Context ctx, String chatId) {
        Intent intent = new Intent(ctx, GroupChatView.class);
        intent.setAction(OPEN_GROUPCHAT);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(GroupChatView.EXTRA_MODE, GroupChatMode.OPEN);
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
        newgroupChatMessage.setClass(ctx, GroupChatView.class);
        newgroupChatMessage
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        newgroupChatMessage.putExtra(GroupChatView.EXTRA_MODE, GroupChatMode.INCOMING);
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
        invitation.setClass(ctx, GroupChatView.class);
        invitation.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        invitation.putExtra(GroupChatView.EXTRA_MODE, GroupChatMode.INCOMING);
        return invitation;
    }

    @Override
    public ChatMessage sendMessage(String message) throws RcsServiceException {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "sendTextMessage: ".concat(message));
        }
        return mGroupChat.sendMessage(message);
    }

    @Override
    public ChatMessage sendMessage(Geoloc geoloc) throws RcsServiceException {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "sendGeolocMessage: ".concat(geoloc.toString()));
        }
        return mGroupChat.sendMessage(geoloc);
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
    public boolean isSingleChat() {
        return false;
    }

    @Override
    public INotifyComposing getNotifyComposing() {
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

    @Override
    public void initialize() {
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
                if (!mChatId.equals(chatId)) {
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
                                /* Session is well established : hide progress dialog. */
                                hideProgressDialog();
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
    }

}
