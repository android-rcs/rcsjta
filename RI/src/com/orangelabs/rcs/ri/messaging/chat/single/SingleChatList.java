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

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.text.format.DateUtils;
import android.util.Log;
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

import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.chat.ChatLog.Message;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.ContactUtils;
import com.orangelabs.rcs.ri.ApiConnectionManager;
import com.orangelabs.rcs.ri.ApiConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsDisplayName;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * List chats from the content provider
 * 
 * @author YPLO6403
 *
 */
public class SingleChatList extends Activity {

	/**
	 * Contact is the ID since there is a single contact occurrence in the query result
	 */
	private static final String CONTACT_AS_ID = new StringBuilder(Message.CONTACT).append(" AS ").append(BaseColumns._ID)
			.toString();

	// @formatter:off
	private static final String[] PROJECTION = new String[] {
				CONTACT_AS_ID,
				Message.CHAT_ID,
	    		Message.CONTENT,
	    		Message.MIME_TYPE,
	    		Message.TIMESTAMP
	    		};
	// @formatter:on

	private static final String WHERE_CLAUSE_GROUPED = new StringBuilder(Message.CHAT_ID).append("=")
			.append(Message.CONTACT).append(" GROUP BY ").append(Message.CONTACT).toString();

	private static final String SORT_ORDER = new StringBuilder(Message.TIMESTAMP).append(" DESC").toString();

	private static final String WHERE_CLAUSE = new StringBuilder(Message.CHAT_ID).append("=")
			.append(Message.CONTACT).toString();

	/**
	 * List view
	 */
	private ListView mListView;
	
	private ContactUtils mContactUtils;

	/**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(SingleChatList.class.getSimpleName());

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set layout
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.chat_list);

		mContactUtils = ContactUtils.getInstance(this);
		
		// Set list adapter
		mListView = (ListView) findViewById(android.R.id.list);
		TextView emptyView = (TextView) findViewById(android.R.id.empty);
		mListView.setEmptyView(emptyView);
		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
				// TODO: if not connected offers possibility to show history
				ApiConnectionManager apiConnectionManager = ApiConnectionManager.getInstance(SingleChatList.this);
				if (apiConnectionManager == null || !apiConnectionManager.isServiceConnected(RcsServiceName.CHAT)) {
					Utils.showMessage(SingleChatList.this, getString(R.string.label_continue_chat_failed));
					return;
					
				}
				// Get selected item
				Cursor cursor = (Cursor) (parent.getAdapter()).getItem(pos);
				String number = cursor.getString(cursor.getColumnIndex(BaseColumns._ID));

				ContactId contact;
				try {
					contact = mContactUtils.formatContact(number);
					// Open chat
					startActivity(SingleChatView.forgeIntentToStart(SingleChatList.this, contact));
				} catch (RcsContactFormatException e) {
					if (LogUtils.isActive) {
						Log.e(LOGTAG, "Cannot parse contact ".concat(number));
					}
				}
				
			}
			
		});
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Refresh view
		mListView.setAdapter(createListAdapter());
	}

	/**
	 * Create chat list adapter with unique contact entries
	 */
	private ChatListAdapter createListAdapter() {
		Cursor cursor = getContentResolver().query(Message.CONTENT_URI, PROJECTION, WHERE_CLAUSE_GROUPED, null, SORT_ORDER);
		if (cursor == null) {
			Utils.showMessageAndExit(this, getString(R.string.label_load_log_failed));
			return null;
			
		}
		return new ChatListAdapter(this, cursor);
	}

	/**
	 * Single chat list adapter
	 */
	private class ChatListAdapter extends CursorAdapter {
		/**
		 * Constructor
		 * 
		 * @param context
		 *            Context
		 * @param c
		 *            Cursor
		 */
		public ChatListAdapter(Context context, Cursor c) {
			super(context, c);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			LayoutInflater inflater = LayoutInflater.from(context);
			View view = inflater.inflate(R.layout.chat_list_item, parent, false);
			SingleChatListItemViewHolder holder = new SingleChatListItemViewHolder(view, cursor);
			view.setTag(holder);
			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			SingleChatListItemViewHolder holder = (SingleChatListItemViewHolder) view.getTag();

			// Set the date/time field by mixing relative and absolute times
			long date = cursor.getLong(holder.columnTimestamp);
			holder.dateText.setText(DateUtils.getRelativeTimeSpanString(date, System.currentTimeMillis(),
					DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE));

			// Set the contact name
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
							.append(geoloc.getLatitude()).append(",")
							.append(geoloc.getLongitude()).toString();
				} catch (Exception e) {
					if (LogUtils.isActive) {
						Log.e(LOGTAG,"Invalid geoloc message:".concat(content));
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
	 * A ViewHolder class keeps references to children views to avoid unnecessary calls to findViewById() or getColumnIndex() on
	 * each row.
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
			columnContact = cursor.getColumnIndexOrThrow(BaseColumns._ID);
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
			// Delete all
			getContentResolver().delete(Message.CONTENT_URI, WHERE_CLAUSE, null);

			// Refresh view
			mListView.setAdapter(createListAdapter());
			break;
		}
		return true;
	}
}