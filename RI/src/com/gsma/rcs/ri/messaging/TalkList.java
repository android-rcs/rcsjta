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

import com.gsma.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.gsma.rcs.api.connection.utils.ExceptionUtil;
import com.gsma.rcs.api.connection.utils.RcsActivity;
import com.gsma.rcs.ri.R;
import com.gsma.rcs.ri.messaging.adapter.TalkListArrayAdapter;
import com.gsma.rcs.ri.messaging.adapter.TalkListArrayItem;
import com.gsma.rcs.ri.messaging.chat.ChatPendingIntentManager;
import com.gsma.rcs.ri.messaging.filetransfer.multi.SendMultiFile;
import com.gsma.rcs.ri.utils.LogUtils;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatService;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.GroupChatIntent;
import com.gsma.services.rcs.chat.GroupChatListener;
import com.gsma.services.rcs.chat.OneToOneChatIntent;
import com.gsma.services.rcs.chat.OneToOneChatListener;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransferIntent;
import com.gsma.services.rcs.filetransfer.FileTransferService;
import com.gsma.services.rcs.filetransfer.GroupFileTransferListener;
import com.gsma.services.rcs.filetransfer.OneToOneFileTransferListener;
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * List of conversations from the content provider: RCS chat + RCS file transfer
 *
 * @author Philippe LEMORDANT
 */
public class TalkList extends RcsActivity {

    private TalkListArrayAdapter mAdapter;
    private List<TalkListArrayItem> mMessageLogs;
    private static final String LOGTAG = LogUtils.getTag(TalkList.class.getSimpleName());

    private ChatService mChatService;
    private FileTransferService mFileTransferService;
    private boolean mOneToOneChatListenerSet;
    private OneToOneChatListener mOneToOneChatListener;
    private Context mCtx;
    private boolean mFileTransferListenerSet;
    private OneToOneFileTransferListener mOneToOneFileTransferListener;
    private TalkListUpdate.TaskCompleted mUpdateTalkListListener;
    private static boolean sActivityVisible;
    private boolean mGroupChatListenerSet;
    private GroupChatListener mGroupChatListener;
    private GroupFileTransferListener mGroupFileTransferListener;
    private boolean mGroupFileTransferListenerSet;
    private boolean mTalkListOpenedToSendFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.chat_list);
        initialize();
        /*
         * If action to launch activity is not null then activity is opened to transfer a file
         */
        mTalkListOpenedToSendFile = getIntent().getAction() != null;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String action = intent.getAction();
        if (action == null) {
            return;
        }
        switch (action) {
            case GroupChatIntent.ACTION_NEW_INVITATION:
            case GroupChatIntent.ACTION_NEW_GROUP_CHAT_MESSAGE:
            case OneToOneChatIntent.ACTION_NEW_ONE_TO_ONE_CHAT_MESSAGE:
            case FileTransferIntent.ACTION_NEW_INVITATION:
                /* Replace the value of intent */
                setIntent(intent);
                TalkListUpdate updateTalkList = new TalkListUpdate(this, mUpdateTalkListListener);
                updateTalkList.execute();
                break;

            default:
                throw new IllegalArgumentException("Invalid action=" + action);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sActivityVisible = true;
        addServiceListeners();
        updateView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sActivityVisible = false;
        removeServiceListeners();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(getApplicationContext());
        inflater.inflate(R.menu.menu_log, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            if (item.getItemId() == R.id.menu_clear_log) {
                /* Delete all messages */
                if (!isServiceConnected(RcsServiceName.CHAT, RcsServiceName.FILE_TRANSFER)) {
                    showMessage(R.string.label_service_not_available);
                    return true;
                }
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "delete conversations");
                }
                mChatService.deleteOneToOneChats();
                mChatService.deleteGroupChats();
                mFileTransferService.deleteOneToOneFileTransfers();
                mFileTransferService.deleteGroupFileTransfers();
            }
        } catch (RcsServiceNotAvailableException e) {
            showMessage(R.string.label_service_not_available);

        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_log_item, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        /* Get selected item */
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        TalkListArrayItem message = mAdapter.getItem(info.position);
        String chatId = message.getChatId();
        ContactId contact = message.getContact();
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onContextItemSelected chatId=".concat(chatId));
        }
        try {
            if (item.getItemId() == R.id.menu_delete_message) {
                if (!isServiceConnected(RcsServiceName.CHAT, RcsServiceName.FILE_TRANSFER)) {
                    showMessage(R.string.error_conversation_delete);
                    return true;
                }
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "Delete conversation for chatId=".concat(chatId));
                }
                if (message.isGroupChat()) {
                    mChatService.deleteGroupChat(chatId);
                    mFileTransferService.deleteGroupFileTransfers(chatId);
                } else {
                    mChatService.deleteOneToOneChat(contact);
                    mFileTransferService.deleteOneToOneFileTransfers(contact);
                }
                return true;
            }
            return super.onContextItemSelected(item);
        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
            return true;
        }
    }

    private void updateView() {
        if (sActivityVisible) {
            TalkListUpdate updateTalkList = new TalkListUpdate(mCtx, mUpdateTalkListListener);
            updateTalkList.execute();
        }
    }

    private void initialize() {
        mCtx = this;
        mMessageLogs = new ArrayList<>();
        mChatService = getChatApi();
        mFileTransferService = getFileTransferApi();
        ListView listView = (ListView) findViewById(android.R.id.list);
        TextView emptyView = (TextView) findViewById(android.R.id.empty);
        listView.setEmptyView(emptyView);
        registerForContextMenu(listView);
        mAdapter = new TalkListArrayAdapter(this, mMessageLogs);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TalkListArrayItem message = mAdapter.getItem(position);
                boolean gchat = message.isGroupChat();
                if (mTalkListOpenedToSendFile) {
                    // Open multiple file transfer
                    SendMultiFile.startActivity(TalkList.this, getIntent(), !gchat,
                            message.getChatId());
                    return;
                }
                if (gchat) {
                    GroupTalkView.openGroupChat(mCtx, message.getChatId());
                } else {
                    startActivity(OneToOneTalkView.forgeIntentToOpenConversation(mCtx,
                            message.getContact()));
                }
            }
        });
        mOneToOneFileTransferListener = new OneToOneFileTransferListener() {
            @Override
            public void onStateChanged(ContactId contact, String transferId,
                                       FileTransfer.State state, FileTransfer.ReasonCode reasonCode) {
            }

            @Override
            public void onProgressUpdate(ContactId contact, String transferId, long currentSize,
                                         long totalSize) {
            }

            @Override
            public void onDeleted(final ContactId contact, Set<String> transferIds) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onDeleted contact=" + contact + " FT IDs=" + transferIds);
                }
                updateView();
            }
        };
        mOneToOneChatListener = new OneToOneChatListener() {
            @Override
            public void onMessageStatusChanged(ContactId contact, String mimeType, String msgId,
                                               ChatLog.Message.Content.Status status,
                                               ChatLog.Message.Content.ReasonCode reasonCode) {
            }

            @Override
            public void onComposingEvent(ContactId contact, boolean status) {
            }

            @Override
            public void onMessagesDeleted(final ContactId contact, Set<String> msgIds) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onMessagesDeleted contact=" + contact + " msg IDs=" + msgIds);
                }
                updateView();
            }
        };
        mGroupFileTransferListener = new GroupFileTransferListener() {
            @Override
            public void onStateChanged(String chatId, String transferId, FileTransfer.State state,
                                       FileTransfer.ReasonCode reasonCode) {
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
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onDeleted ftIds=" + transferIds);
                }
                updateView();
            }
        };
        mGroupChatListener = new GroupChatListener() {
            @Override
            public void onStateChanged(String chatId, GroupChat.State state,
                                       GroupChat.ReasonCode reasonCode) {
            }

            @Override
            public void onComposingEvent(String chatId, ContactId contact, boolean status) {
            }

            @Override
            public void onMessageStatusChanged(String chatId, String mimeType, String msgId,
                                               ChatLog.Message.Content.Status status,
                                               ChatLog.Message.Content.ReasonCode reasonCode) {
            }

            @Override
            public void onMessageGroupDeliveryInfoChanged(String chatId, ContactId contact,
                                                          String mimeType, String msgId, GroupDeliveryInfo.Status status,
                                                          GroupDeliveryInfo.ReasonCode reasonCode) {
            }

            @Override
            public void onParticipantStatusChanged(String chatId, ContactId contact,
                                                   GroupChat.ParticipantStatus status) {
            }

            @Override
            public void onDeleted(Set<String> chatIds) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onDeleted chatIds=" + chatIds);
                }
                updateView();
            }

            @Override
            public void onMessagesDeleted(final String chatId, Set<String> msgIds) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onMessagesDeleted msgIds=" + msgIds);
                }
                updateView();
            }
        };
        mUpdateTalkListListener = new TalkListUpdate.TaskCompleted() {
            @Override
            public void onTaskComplete(Collection<TalkListArrayItem> talks) {
                if (talks == null) {
                    TalkList.this.showMessageThenExit(R.string.cannot_read_log);
                    return;
                }
                mMessageLogs.clear();
                mMessageLogs.addAll(talks);
                /* Sort by descending timestamp */
                Collections.sort(mMessageLogs);
                mAdapter.notifyDataSetChanged();
                ChatPendingIntentManager talkPendingIntentManager = ChatPendingIntentManager
                        .getChatPendingIntentManager(mCtx);
                for (TalkListArrayItem talk : talks) {
                    if (talk.getUnreadCount() == 0) {
                        /*
                         * TODO check that if invitation is pending, there is always at least one
                         * unread message (assertion may be false)
                         */
                        talkPendingIntentManager.clearNotification(talk.getChatId());
                    }
                }
            }
        };
    }

    /**
     * Notify new conversation event
     *
     * @param ctx    the context
     * @param action the action intent
     */
    public static void notifyNewConversationEvent(Context ctx, String action) {
        if (sActivityVisible) {
            Intent intent = new Intent(ctx, TalkList.class);
            intent.setAction(action);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(intent);
        }
    }

    private void addServiceListeners() {
        if (!isServiceConnected(RcsServiceName.FILE_TRANSFER, RcsServiceName.CHAT)) {
            return;
        }
        try {
            if (!mGroupChatListenerSet) {
                mChatService.addEventListener(mGroupChatListener);
                mGroupChatListenerSet = true;
            }
            if (!mOneToOneChatListenerSet) {
                mChatService.addEventListener(mOneToOneChatListener);
                mOneToOneChatListenerSet = true;
            }
            if (!mFileTransferListenerSet) {
                mFileTransferService.addEventListener(mOneToOneFileTransferListener);
                mFileTransferListenerSet = true;
            }
            if (!mGroupFileTransferListenerSet) {
                mFileTransferService.addEventListener(mGroupFileTransferListener);
                mGroupFileTransferListenerSet = true;
            }
        } catch (RcsServiceNotAvailableException ignore) {
        } catch (RcsServiceException e) {
            Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
        }
    }

    private void removeServiceListeners() {
        if (!isServiceConnected(RcsServiceName.FILE_TRANSFER, RcsServiceName.CHAT)) {
            return;
        }
        try {
            if (mGroupChatListenerSet) {
                mChatService.removeEventListener(mGroupChatListener);
                mGroupChatListenerSet = false;
            }
            if (mOneToOneChatListenerSet) {
                mChatService.removeEventListener(mOneToOneChatListener);
                mOneToOneChatListenerSet = false;
            }
            if (mFileTransferListenerSet) {
                mFileTransferService.removeEventListener(mOneToOneFileTransferListener);
                mFileTransferListenerSet = false;
            }
            if (mGroupFileTransferListenerSet) {
                mFileTransferService.removeEventListener(mGroupFileTransferListener);
                mGroupFileTransferListenerSet = false;
            }
        } catch (RcsServiceNotAvailableException ignore) {
        } catch (RcsServiceException e) {
            Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
        }
    }
}
