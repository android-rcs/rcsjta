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

import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.gsma.services.rcs.RcsCommon;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.RcsCommon.ReadStatus;
import com.gsma.services.rcs.chat.ChatMessage;
import com.gsma.services.rcs.chat.GeolocMessage;
import com.gsma.services.rcs.chat.OneToOneChat;
import com.gsma.services.rcs.chat.OneToOneChatListener;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatService;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.messaging.chat.ChatView;
import com.orangelabs.rcs.ri.messaging.chat.IsComposingManager;
import com.orangelabs.rcs.ri.messaging.chat.IsComposingManager.INotifyComposing;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsDisplayName;
import com.orangelabs.rcs.ri.utils.Smileys;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Single chat view
 */
public class SingleChatView extends ChatView {
	/**
	 * Intent parameters
	 */
	private final static String EXTRA_CONTACT = "contact";

	/**
	 * The remote contact
	 */
	private ContactId mContact;

	/**
	 * The chat session instance
	 */
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

	/**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(SingleChatView.class.getSimpleName());

	private static final String WHERE_CLAUSE = new StringBuilder(ChatLog.Message.CONTACT).append("=? AND (")
			.append(ChatLog.Message.MIME_TYPE).append("='").append(ChatLog.Message.MimeType.GEOLOC_MESSAGE).append("' OR ")
			.append(ChatLog.Message.MIME_TYPE).append("='").append(ChatLog.Message.MimeType.TEXT_MESSAGE).append("')").toString();

	private final static String UNREADS_WHERE_CLAUSE = new StringBuilder(ChatLog.Message.CONTACT).append("=? AND ").append(ChatLog.Message.READ_STATUS)
			.append("=").append(ReadStatus.UNREAD).append(" AND (").append(ChatLog.Message.MIME_TYPE).append("='")
			.append(ChatLog.Message.MimeType.GEOLOC_MESSAGE).append("' OR ").append(ChatLog.Message.MIME_TYPE).append("='")
			.append(ChatLog.Message.MimeType.TEXT_MESSAGE).append("')").toString();

	/**
	 * Single Chat listener
	 */
	private OneToOneChatListener mListener = new OneToOneChatListener() {
		// Callback called when an Is-composing event has been received
		@Override
		public void onComposingEvent(ContactId contact, boolean status) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onComposingEvent contact=" + contact + " status=" + status);
			}
			// Discard event if not for current contact
			if (mContact == null || !mContact.equals(contact)) {
				return;
			}
			displayComposingEvent(contact, status);
		}

		@Override
		public void onMessageStatusChanged(ContactId contact, String msgId, int status, int reasonCode) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onMessageStatusChanged contact=" + contact + " msgId=" + msgId + " status=" + status);
			}
		}

	};

	@Override
	public void onDestroy() {
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "onDestroy");
		}
		super.onDestroy();
		contactOnForeground = null;
	}

	@Override
	public boolean processIntent() {
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "processIntent");
		}
		ChatService chatService = connectionManager.getChatApi();
		// Open chat
		ContactId newContact = (ContactId) getIntent().getParcelableExtra(EXTRA_CONTACT);
		if (newContact == null) {
			if (LogUtils.isActive) {
				Log.w(LOGTAG, "Cannot process intent: contact is null");
			}
			return false;
		}
		try {
			if (!newContact.equals(mContact) || mChat == null) {
				boolean firstLoad = (mChat == null);
				boolean switchConversation = (mContact != null && !newContact.equals(mContact));
				// Save contact
				mContact = newContact;
				// Open chat
				mChat = chatService.openSingleChat(mContact);
				if (firstLoad) {
					// Initialize the Loader with id '1' and callbacks 'mCallbacks'.
					getSupportLoaderManager().initLoader(LOADER_ID, null, this);
				} else {
					if (switchConversation) {
						// Reload history since
						getSupportLoaderManager().restartLoader(LOADER_ID, null, this);
					}
				}
			}
			contactOnForeground = mContact;
			// Set activity title with display name
			String from = RcsDisplayName.getInstance(this).getDisplayName(mContact);
			setTitle(getString(R.string.title_chat, from));
			// Mark as read messages if required
			Set<String> msgIdUnreads = getUnreadMessageIds(mContact);
			for (String msgId : msgIdUnreads) {
				chatService.markMessageAsRead(msgId);
			}
			return true;
		} catch (RcsServiceNotAvailableException e) {
			Utils.showMessageAndExit(this, getString(R.string.label_api_disabled), exitOnce);
		} catch (RcsServiceException e) {
			Utils.showMessageAndExit(this, getString(R.string.label_api_failed), exitOnce);
		}
		return false;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "onCreateLoader " + id);
		}
		// Create a new CursorLoader with the following query parameters.
		Uri uri = ChatLog.Message.CONTENT_URI;
		CursorLoader loader = new CursorLoader(this, uri, PROJECTION, WHERE_CLAUSE, new String[] { mContact.toString() },
				QUERY_SORT_ORDER);
		return loader;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		// Get the list item position
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		Cursor cursor = (Cursor) mAdapter.getItem(info.position);
		// Adapt the contextual menu according to the selected item
		menu.add(0, CHAT_MENU_ITEM_DELETE, CHAT_MENU_ITEM_DELETE, R.string.menu_delete_message);
		int direction = cursor.getInt(cursor.getColumnIndex(ChatLog.Message.DIRECTION));
		if (direction == RcsCommon.Direction.OUTGOING) {
			int status = cursor.getInt(cursor.getColumnIndex(ChatLog.Message.STATUS));
			switch (status) {
			case ChatLog.Message.Status.Content.FAILED:
				menu.add(0, CHAT_MENU_ITEM_RESEND, CHAT_MENU_ITEM_RESEND, R.string.menu_resend_message);
				break;
			case ChatLog.Message.Status.Content.DISPLAY_REPORT_REQUESTED:
			case ChatLog.Message.Status.Content.DELIVERED:
			case ChatLog.Message.Status.Content.SENT:
			case ChatLog.Message.Status.Content.SENDING:
			case ChatLog.Message.Status.Content.QUEUED:
				menu.add(0, CHAT_MENU_ITEM_REVOKE, CHAT_MENU_ITEM_REVOKE, R.string.menu_revoke_message);
				break;
			default:
				break;
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		Cursor cursor = (Cursor) (mAdapter.getItem(info.position));
		String messageId = cursor.getString(cursor.getColumnIndex(BaseColumns._ID));
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "onContextItemSelected msgId=" + messageId);
		}
		switch (item.getItemId()) {
		case CHAT_MENU_ITEM_RESEND:
			// TODO
			return true;
		case CHAT_MENU_ITEM_REVOKE:
			// TODO
			return true;
		case CHAT_MENU_ITEM_DELETE:
			// TODO CR005 delete methods
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	/**
	 * Forge intent to start SingleChatView activity
	 * 
	 * @param context
	 * @param contact
	 * @return intent
	 */
	public static Intent forgeIntentToStart(Context context, ContactId contact) {
		Intent intent = new Intent(context, SingleChatView.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(EXTRA_CONTACT, (Parcelable) contact);
		return intent;
	}

	/**
	 * Get unread messages for contact
	 * @param contact
	 * @return set of unread message IDs
	 */
	private Set<String> getUnreadMessageIds(ContactId contact) {
		Set<String> unReadMessageIDs = new HashSet<String>();
		String[] where_args = new String[] { contact.toString() };
		String[] projection = new String[] { ChatLog.Message.MESSAGE_ID };
		Cursor cursor = null;
		try {
			cursor = getContentResolver().query(ChatLog.Message.CONTENT_URI, projection, UNREADS_WHERE_CLAUSE, where_args, QUERY_SORT_ORDER);
			while (cursor.moveToNext()) {
				unReadMessageIDs.add(cursor.getString(cursor.getColumnIndex(ChatLog.Message.MESSAGE_ID)));
			}
		} catch (Exception e) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Exception getUnreads", e);
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return unReadMessageIDs;
	}

	@Override
	public ChatMessage sendTextMessage(String message) {
		// Send text message
		try {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "sendTextMessage msg=" + message);
			}
			// Send the text to remote
			return mChat.sendMessage(message);
		} catch (Exception e) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "sendTextMessage failed", e);
			}
			return null;
		}
	}

	@Override
	public GeolocMessage sendGeolocMessage(Geoloc geoloc) {
		// Send geoloc message
		try {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "sendGeolocMessage geoloc=" + geoloc);
			}
			// Send the text to remote
			return mChat.sendMessage(geoloc);
		} catch (Exception e) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "sendGeolocMessage failed", e);
			}
			return null;
		}
	}

	@Override
	public void addChatEventListener(ChatService chatService) throws RcsServiceException {
		connectionManager.getChatApi().addEventListener(mListener);
	}

	@Override
	public void removeChatEventListener(ChatService chatService) throws RcsServiceException {
		connectionManager.getChatApi().removeEventListener(mListener);
	}

	@Override
	public INotifyComposing getNotifyComposing() {
		INotifyComposing notifyComposing = new IsComposingManager.INotifyComposing() {
			public void setTypingStatus(boolean isTyping) {
				try {
					if (mChat != null) {
						mChat.sendIsComposingEvent(isTyping);
						if (LogUtils.isActive) {
							Log.d(LOGTAG, "sendIsComposingEvent " + isTyping);
						}
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
			Smileys.showSmileyDialog(this, composeText, getResources(), getString(R.string.menu_insert_smiley));
			break;

		case R.id.menu_quicktext:
			addQuickText();
			break;

		case R.id.menu_send_geoloc:
			getGeoLoc();
			break;

		case R.id.menu_showus_map:
			Set<String> contacts = new HashSet<String>();
			contacts.add(mContact.toString());
			showUsInMap(contacts);
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
}
