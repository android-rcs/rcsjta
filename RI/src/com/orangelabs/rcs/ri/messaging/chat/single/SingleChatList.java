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
import com.gsma.services.rcs.chat.ChatLog.Message;
import com.gsma.services.rcs.chat.ChatLog.Message.Content;
import com.gsma.services.rcs.chat.ChatService;
import com.gsma.services.rcs.chat.OneToOneChatListener;
import com.gsma.services.rcs.contact.ContactId;

import com.orangelabs.rcs.ri.ConnectionManager;
import com.orangelabs.rcs.ri.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.ContactUtil;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsDisplayName;
import com.orangelabs.rcs.ri.utils.Utils;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Set;

/**
 * List chats from the content provider
 * 
 * @author YPLO6403
 */
public class SingleChatList extends FragmentActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    // @formatter:off
    private static final String[] PROJECTION = new String[] {
        Message.BASECOLUMN_ID,
        Message.CONTACT,
        Message.CHAT_ID,
        Message.CONTENT,
        Message.MIME_TYPE,
        Message.TIMESTAMP
    };
    // @formatter:on

    /**
     * One to one chat are raws where chat_id equals contact.
     */
    private static final String WHERE_CLAUSE_GROUPED = new StringBuilder(Message.CHAT_ID)
            .append("=").append(Message.CONTACT).append(" GROUP BY ").append(Message.CONTACT)
            .toString();

    private static final String SORT_ORDER = new StringBuilder(Message.TIMESTAMP).append(" DESC")
            .toString();

    private ListView mListView;

    private ChatService mChatService;

    private boolean mOneToOneChatListenerSet = false;

    private ConnectionManager mCnxManager;

    private ChatListAdapter mAdapter;

    private Handler mHandler = new Handler();

    private LockAccess mExitOnce = new LockAccess();

    private static final String LOGTAG = LogUtils.getTag(SingleChatList.class.getSimpleName());

    /**
     * The loader's unique ID. Loader IDs are specific to the Activity in which they reside.
     */
    private static final int LOADER_ID = 1;

    /**
     * List of items for contextual menu
     */
    private static final int CHAT_MENU_ITEM_DELETE = 1;
    private static final int CHAT_MENU_ITEM_OPEN = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Set layout */
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.chat_list);

        mCnxManager = ConnectionManager.getInstance();
        mChatService = mCnxManager.getChatApi();

        mListView = (ListView) findViewById(android.R.id.list);
        TextView emptyView = (TextView) findViewById(android.R.id.empty);
        mListView.setEmptyView(emptyView);
        registerForContextMenu(mListView);

        mAdapter = new ChatListAdapter(this);
        mListView.setAdapter(mAdapter);
        /*
         * Initialize the Loader with id '1' and callbacks 'mCallbacks'.
         */
        getSupportLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mChatService == null || !mOneToOneChatListenerSet) {
            return;
        }
        try {
            mChatService.removeEventListener(mOneChatListener);
        } catch (Exception e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "removeEventListener failed", e);
            }
        }
    }

    /**
     * Single chat list adapter
     */
    private class ChatListAdapter extends CursorAdapter {

        private LayoutInflater mInflater;

        /**
         * Constructor
         * 
         * @param context Context
         */
        public ChatListAdapter(Context context) {
            super(context, null, 0);
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            final View view = mInflater.inflate(R.layout.chat_one2one_list_item, parent, false);
            view.setTag(new SingleChatListItemViewHolder(view, cursor));
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final SingleChatListItemViewHolder holder = (SingleChatListItemViewHolder) view
                    .getTag();
            long date = cursor.getLong(holder.columnTimestamp);
            holder.dateText.setText(DateUtils.getRelativeTimeSpanString(date,
                    System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE));

            String number = cursor.getString(holder.columnContact);
            String displayName = RcsDisplayName.getInstance(context).getDisplayName(number);
            holder.contactText.setText(getString(R.string.title_chat, displayName));

            String content = cursor.getString(holder.columnContent);
            String mimetype = cursor.getString(holder.columnMimetype);
            String text = "";
            if (Message.MimeType.GEOLOC_MESSAGE.equals(mimetype)) {
                try {
                    Geoloc geoloc = new Geoloc(content);
                    text = new StringBuilder(geoloc.getLabel()).append(",")
                            .append(geoloc.getLatitude()).append(",").append(geoloc.getLongitude())
                            .toString();
                } catch (Exception e) {
                    if (LogUtils.isActive) {
                        Log.e(LOGTAG, "Invalid geoloc message:".concat(content));
                    }
                    text = content;
                }
            } else {
                if (Message.MimeType.TEXT_MESSAGE.equals(mimetype)) {
                    text = content;
                }
            }
            holder.contentText.setText(text);
            holder.contentText.setVisibility(View.VISIBLE);
        }
    }

    /**
     * A ViewHolder class keeps references to children views to avoid unnecessary calls to
     * findViewById() or getColumnIndex() on each row.
     */
    private class SingleChatListItemViewHolder {
        int columnContact;

        int columnContent;

        int columnMimetype;

        int columnTimestamp;

        TextView contactText;

        TextView contentText;

        TextView dateText;

        SingleChatListItemViewHolder(View base, Cursor cursor) {
            columnContact = cursor.getColumnIndexOrThrow(Message.CONTACT);
            columnContent = cursor.getColumnIndexOrThrow(Message.CONTENT);
            columnMimetype = cursor.getColumnIndexOrThrow(Message.MIME_TYPE);
            columnTimestamp = cursor.getColumnIndexOrThrow(Message.TIMESTAMP);

            contactText = (TextView) base.findViewById(R.id.line1);
            contentText = (TextView) base.findViewById(R.id.line2);
            dateText = (TextView) base.findViewById(R.id.date);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(getApplicationContext());
        inflater.inflate(R.menu.menu_log, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_clear_log:
                /* Delete all one-to-one chat messages */
                if (!mCnxManager.isServiceConnected(RcsServiceName.CHAT)) {
                    Utils.showMessage(this, getString(R.string.label_api_unavailable));
                    break;
                }
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "delete all one-to-one chat sessions");
                }
                try {
                    if (!mOneToOneChatListenerSet) {
                        mChatService.addEventListener(mOneChatListener);
                        mOneToOneChatListenerSet = true;
                    }
                    mChatService.deleteOneToOneChats();
                } catch (Exception e) {
                    Utils.showMessageAndExit(this, getString(R.string.label_delete_chat_failed),
                            mExitOnce, e);
                }
                break;
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        /* Check file transfer API is connected */
        if (!mCnxManager.isServiceConnected(RcsServiceName.CHAT)) {
            Utils.showMessage(this, getString(R.string.label_api_unavailable));
            return;
        }
        menu.add(0, CHAT_MENU_ITEM_OPEN, CHAT_MENU_ITEM_OPEN, R.string.menu_open_chat_session);
        menu.add(0, CHAT_MENU_ITEM_DELETE, CHAT_MENU_ITEM_DELETE, R.string.menu_delete_chat_session);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        /* Get selected item */
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        Cursor cursor = (Cursor) (mListView.getAdapter()).getItem(info.position);
        String number = cursor.getString(cursor.getColumnIndexOrThrow(Message.CONTACT));
        ContactId contact = ContactUtil.formatContact(number);
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onContextItemSelected contact=".concat(contact.toString()));
        }
        switch (item.getItemId()) {
            case CHAT_MENU_ITEM_OPEN:
                if (mCnxManager.isServiceConnected(RcsServiceName.CHAT)) {
                    /* Open one-to-one chat view */
                    startActivity(SingleChatView.forgeIntentToStart(this, contact));
                } else {
                    Utils.showMessage(this, getString(R.string.label_continue_chat_failed));
                }
                return true;

            case CHAT_MENU_ITEM_DELETE:
                if (!mCnxManager.isServiceConnected(RcsServiceName.CHAT)) {
                    Utils.showMessage(this, getString(R.string.label_delete_chat_failed));
                    return true;
                }
                /* Delete messages for contact */
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "Delete messages for contact=".concat(contact.toString()));
                }
                try {
                    if (!mOneToOneChatListenerSet) {
                        mChatService.addEventListener(mOneChatListener);
                        mOneToOneChatListenerSet = true;
                    }
                    mChatService.deleteOneToOneChat(contact);
                } catch (Exception e) {
                    Utils.showMessageAndExit(this, getString(R.string.label_delete_chat_failed),
                            mExitOnce, e);
                }
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private OneToOneChatListener mOneChatListener = new OneToOneChatListener() {

        @Override
        public void onMessagesDeleted(final ContactId contact, Set<String> msgIds) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG,
                        "onMessagesDeleted contact=" + contact + " for message IDs="
                                + Arrays.toString(msgIds.toArray()));
            }
            mHandler.post(new Runnable() {
                public void run() {
                    Utils.displayLongToast(SingleChatList.this,
                            getString(R.string.label_delete_chat_success, contact.toString()));
                }
            });
        }

        @Override
        public void onMessageStatusChanged(ContactId contact, String mimeType, String msgId,
                Content.Status status, Content.ReasonCode reasonCode) {
        }

        @Override
        public void onComposingEvent(ContactId contact, boolean status) {
        }
    };

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        /* Create a new CursorLoader with the following query parameters. */
        return new CursorLoader(this, Message.CONTENT_URI, PROJECTION, WHERE_CLAUSE_GROUPED, null,
                SORT_ORDER);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        /* A switch-case is useful when dealing with multiple Loaders/IDs */
        switch (loader.getId()) {
            case LOADER_ID:
                /*
                 * The asynchronous load is complete and the data is now available for use. Only now
                 * can we associate the queried Cursor with the CursorAdapter.
                 */
                mAdapter.swapCursor(cursor);
                break;
        }
        /* The listview now displays the queried data. */
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        /*
         * For whatever reason, the Loader's data is now unavailable. Remove any references to the
         * old data by replacing it with a null Cursor.
         */
        mAdapter.swapCursor(null);
    }
}
