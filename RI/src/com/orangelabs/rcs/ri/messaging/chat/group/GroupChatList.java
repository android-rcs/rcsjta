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

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.GroupChat;
import com.orangelabs.rcs.ri.ApiConnectionManager;
import com.orangelabs.rcs.ri.ApiConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * List group chats from the content provider 
 * 
 * @author YPLO6403
 *
 */
public class GroupChatList extends Activity {
	/**
	 * ChatId is the ID since there is a single occurrence in group chat log
	 */
	private static final String CHATID_AS_ID = new StringBuilder(ChatLog.GroupChat.CHAT_ID).append(" AS ").append(BaseColumns._ID)
			.toString();

	// @formatter:off
	String[] PROJECTION = new String[] {
			CHATID_AS_ID,
			ChatLog.GroupChat.SUBJECT,
			ChatLog.GroupChat.STATE,
			ChatLog.GroupChat.TIMESTAMP
	    };
	 // @formatter:on

	private static final String SORT_ORDER = new StringBuilder(ChatLog.GroupChat.TIMESTAMP).append(" DESC").toString();

	/**
	 * List view
	 */
	private ListView listView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set layout
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.chat_list);

		// Set list adapter
		listView = (ListView) findViewById(android.R.id.list);
		TextView emptyView = (TextView) findViewById(android.R.id.empty);
		listView.setEmptyView(emptyView);
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
				ApiConnectionManager cnxManager = ApiConnectionManager.getInstance(GroupChatList.this);
				if (cnxManager == null || !cnxManager.isServiceConnected(RcsServiceName.CHAT)) {
					Utils.showMessage(GroupChatList.this, getString(R.string.label_continue_chat_failed));
					return;
					
				}
				Cursor cursor = (Cursor) (parent.getAdapter()).getItem(pos);
				String chatId = cursor.getString(cursor.getColumnIndex(BaseColumns._ID));
				try {
					// Get group chat
					GroupChat groupChat = cnxManager.getChatApi().getGroupChat(chatId);
					if (groupChat != null) {
						// Session already active on the device: just reload it in the UI
						GroupChatView.openGroupChat(GroupChatList.this, groupChat.getChatId());
					} else {
						// Rejoin or restart the session
						// TODO CR018
					}
				} catch (RcsServiceNotAvailableException e) {
					e.printStackTrace();
					Utils.showMessageAndExit(GroupChatList.this, getString(R.string.label_api_disabled));
				} catch (RcsServiceException e) {
					e.printStackTrace();
					Utils.showMessageAndExit(GroupChatList.this, getString(R.string.label_api_failed));
				}
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		// Refresh view
		listView.setAdapter(createListAdapter());
	}

	/**
	 * Create chat list adapter with unique chat ID entries
	 */
	private GroupChatListAdapter createListAdapter() {
		Cursor cursor = getContentResolver().query(ChatLog.GroupChat.CONTENT_URI, PROJECTION, null, null, SORT_ORDER);
		if (cursor == null) {
			Utils.showMessageAndExit(this, getString(R.string.label_load_log_failed));
			return null;
			
		}
		return new GroupChatListAdapter(this, cursor);
	}

	/**
	 * Group chat list adapter
	 */
	private class GroupChatListAdapter extends CursorAdapter {
		/**
		 * Constructor
		 * 
		 * @param context
		 *            Context
		 * @param c
		 *            Cursor
		 */
		public GroupChatListAdapter(Context context, Cursor c) {
			super(context, c);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			LayoutInflater inflater = LayoutInflater.from(context);
			View view = inflater.inflate(R.layout.chat_list_item, parent, false);
			GroupChatListItemViewHolder holder = new GroupChatListItemViewHolder(view, cursor);
			view.setTag(holder);
			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			GroupChatListItemViewHolder holder = (GroupChatListItemViewHolder) view.getTag();

			// Set the date/time field by mixing relative and absolute times
			long date = cursor.getLong(holder.columnDate);
			holder.dateText.setText(DateUtils.getRelativeTimeSpanString(date, System.currentTimeMillis(),
					DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE));

			// Set the label
			holder.titleText.setText(R.string.label_group_chat);

			String subject = cursor.getString(holder.columnSubject);
			if (TextUtils.isEmpty(subject)) {
				holder.subjectText.setText(context.getString(R.string.label_subject_notif,
						"<" + context.getString(R.string.label_no_subject) + ">"));
			} else {
				holder.subjectText.setText(context.getString(R.string.label_subject_notif, subject));
			}
		}
	}

	/**
	 * A ViewHolder class keeps references to children views to avoid unnecessary calls to findViewById() or getColumnIndex() on
	 * each row.
	 */
	private class GroupChatListItemViewHolder {
		TextView titleText;
		TextView subjectText;
		TextView dateText;
		int columnSubject;
		int columnDate;

		GroupChatListItemViewHolder(View base, Cursor cursor) {
			columnSubject = cursor.getColumnIndexOrThrow(ChatLog.GroupChat.SUBJECT);
			columnDate = cursor.getColumnIndexOrThrow(ChatLog.GroupChat.TIMESTAMP);
			
			titleText = (TextView) base.findViewById(R.id.line1);
			subjectText = (TextView) base.findViewById(R.id.line2);
			dateText = (TextView) base.findViewById(R.id.date);
			titleText.setVisibility(View.VISIBLE);
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
			// Delete all: TODO CR005 delete methods
			getContentResolver().delete(ChatLog.GroupChat.CONTENT_URI, null, null);
			// Refresh view
			listView.setAdapter(createListAdapter());
			break;
		}
		return true;
	}
}