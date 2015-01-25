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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Toast;

import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsCommon;
import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.chat.ChatLog.Message;
import com.gsma.services.rcs.chat.ChatMessage;
import com.gsma.services.rcs.chat.ChatService;
import com.gsma.services.rcs.chat.ChatServiceConfiguration;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.GroupChatIntent;
import com.gsma.services.rcs.chat.GroupChatListener;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.chat.ParticipantInfo.Status;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.ContactUtils;
import com.gsma.services.rcs.contacts.RcsContact;
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

	private static final String WHERE_CLAUSE = new StringBuilder(
			Message.CHAT_ID).append("=? AND (").append(Message.MIME_TYPE)
			.append("='").append(Message.MimeType.GEOLOC_MESSAGE)
			.append("' OR ").append(Message.MIME_TYPE).append("='")
			.append(Message.MimeType.TEXT_MESSAGE).append("' OR ")
			.append(Message.MIME_TYPE).append("='")
			.append(Message.MimeType.GROUPCHAT_EVENT).append("')").toString();

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

	/**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(GroupChatView.class.getSimpleName());

	/**
	 * Group chat listener
	 */
	private GroupChatListener mListener = new GroupChatListener() {

		@Override
		public void onMessageStatusChanged(String chatId, String mimeType, String msgId, int status, int reasonCode) {
			if (LogUtils.isActive) {
				Log.w(LOGTAG, new StringBuilder("onMessageStatusChanged chatId=").append(chatId)
						.append(" mime-type=").append(mimeType).append(" msgId=").append(msgId)
						.append(" status=").append(status).append(" reason=").append(reasonCode)
						.toString());
			}
		}

		// Callback called when an Is-composing event has been received
		public void onComposingEvent( String chatId, ContactId contact, boolean status) {
			// Discard event if not for current chatId
			if (!mChatId.equals(chatId)) {
				return;
				
			}
			displayComposingEvent(contact, status);
		}

		@Override
		public void onParticipantInfoChanged(String chatId, ParticipantInfo participant) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG,
						new StringBuilder("onParticipantInfoChanged chatId=")
								.append(chatId).append(" contact=")
								.append(participant.getContact().toString())
								.append(" status=")
								.append(participant.getStatus()).toString());
			}
		}

		@Override
		public void onMessageGroupDeliveryInfoChanged(String chatId, ContactId contact,
				String mimeType, String msgId, int status, int reasonCode) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG,
						new StringBuilder(
								"onMessageGroupDeliveryInfoChanged chatId=")
								.append(chatId).append(" contact=")
								.append(contact).append(" msgId=")
								.append(msgId).append(" status=")
								.append(status).append(" reason=")
								.append(reasonCode).toString());
			}
		}

		/* (non-Javadoc)
		 * @see com.gsma.services.rcs.chat.GroupChatListener#onStateChanged(java.lang.String, int, int)
		 */
		@Override
		public void onStateChanged(String chatId, final int state, final int reasonCode) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG,
						new StringBuilder("onStateChanged chatId=")
								.append(chatId).append(" state=").append(state)
								.append(" reason=").append(reasonCode)
								.toString());
			}
			// TODO CR031 enumerated types
			if (state > RiApplication.GC_STATES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onStateChanged unhandled status=".concat(String.valueOf(state)));
				}
				return;
				
			}
			if (reasonCode > RiApplication.GC_REASON_CODES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onStateChanged unhandled reason=".concat(String.valueOf(reasonCode)));
				}
				return;
				
			}
			// Discard event if not for current chatId
			if (mChatId == null || !mChatId.equals(chatId)) {
				return;
				
			}
			final String _reasonCode = RiApplication.GC_REASON_CODES[reasonCode];
			handler.post(new Runnable() {
				public void run() {
					switch (state) {
					case GroupChat.State.STARTED:
						// Session is well established : hide progress dialog
						hideProgressDialog();
						break;

					case GroupChat.State.ABORTED:
						// Session is aborted: hide progress dialog then exit
						hideProgressDialog();
						Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_chat_aborted, _reasonCode), exitOnce);
						break;

					case GroupChat.State.REJECTED:
						// Session is rejected: hide progress dialog then exit
						hideProgressDialog();
						Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_chat_rejected, _reasonCode), exitOnce);
						break;

					case GroupChat.State.FAILED:
						// Session is failed: hide progress dialog then exit
						hideProgressDialog();
						Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_chat_failed, _reasonCode), exitOnce);
						break;

					default:
					}
				}
			});
		};

	};

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		// Get the list item position
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		Cursor cursor = (Cursor) mAdapter.getItem(info.position);
		menu.add(0, GROUPCHAT_MENU_ITEM_DELETE, 0, R.string.menu_delete_message);
		int direction = cursor.getInt(cursor.getColumnIndex(Message.DIRECTION));
		if (RcsCommon.Direction.OUTGOING == direction) {
			menu.add(0, GROUPCHAT_MENU_ITEM_VIEW_GC_INFO, 1, R.string.menu_view_groupdelivery);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		Cursor cursor = (Cursor) (mAdapter.getItem(info.position));
		String messageId = cursor.getString(cursor.getColumnIndexOrThrow(BaseColumns._ID));
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
			composingManager = new IsComposingManager(configuration.getIsComposingTimeout() * 1000, getNotifyComposing());
		} catch (RcsServiceNotAvailableException e) {
			Utils.showMessageAndExit(this, getString(R.string.label_api_disabled), exitOnce);
		} catch (RcsServiceException e) {
			Utils.showMessageAndExit(this, getString(R.string.label_api_failed), exitOnce);
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
				// Initiate a Group Chat: check if the service is available
				boolean registered = mCnxManager.getChatApi().isServiceRegistered();
				if (!registered) {
					Utils.showMessageAndExit(this, getString(R.string.label_service_not_available), exitOnce);
					return false;
					
				}

				// Get subject
				mSubject = getIntent().getStringExtra(GroupChatView.EXTRA_SUBJECT);
				updateGroupChatViewTitle(mSubject);

				// Get participants
				ContactUtils contactUtils = ContactUtils.getInstance(this);
				List<String> contacts = getIntent().getStringArrayListExtra(GroupChatView.EXTRA_PARTICIPANTS);
				if (contacts == null || contacts.isEmpty()) {
					Utils.showMessageAndExit(this, getString(R.string.label_invalid_contacts), exitOnce);
					return false;
					
				}
				
				for (String contact : contacts) {
					try {
						mParticipants.add(contactUtils.formatContact(contact));
					} catch (RcsContactFormatException e) {
						if (LogUtils.isActive) {
							Log.e(LOGTAG, "processIntent invalid participant ".concat(contact));
						}
					}
				}
				if (mParticipants.isEmpty()) {
					Utils.showMessageAndExit(this,
							getString(R.string.label_invalid_contacts),
							exitOnce);
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
						Log.e(LOGTAG, "processIntent session not found for chatId=".concat(mChatId));
					}
					Utils.showMessageAndExit(this, getString(R.string.label_session_not_found), exitOnce);
					return false;
					
				}
				getSupportLoaderManager().initLoader(LOADER_ID, null, this);

				chatIdOnForeground = mChatId;

				// Get subject
				mSubject = mGroupChat.getSubject();
				updateGroupChatViewTitle(mSubject);

				// Set list of participants
				mParticipants = getListOfParticipants(mGroupChat.getParticipants());
				if (LogUtils.isActive) {
					if (mParticipants == null) {
						Log.e(LOGTAG,
								new StringBuilder("processIntent chatId=")
										.append(mChatId).append(" subject='")
										.append(mSubject).append("'")
										.toString());
					}
				}
				return true;

			case INCOMING:
				ChatMessageDAO message = (ChatMessageDAO) (getIntent().getExtras().getParcelable(BUNDLE_CHATMESSAGE_DAO_ID));
				if (message != null) {
					// It is a new message: check if for the displayed conversation
					if (message.getChatId().equals(mChatId)) {
						// Mark the message as read
						mCnxManager.getChatApi().markMessageAsRead(message.getMsgId());
						return true;
						
					} else {
						// Ignore message if it does not belong to current GC
						if (LogUtils.isActive) {
							Log.d(LOGTAG,
									new StringBuilder(
											"processIntent discard chat message ")
											.append(message.getMsgId())
											.append(" for chatId ")
											.append(message.getChatId())
											.toString());
						}
						return true;
						
					}
				} else {
					// New GC invitation
					mChatId = getIntent().getStringExtra(GroupChatIntent.EXTRA_CHAT_ID);
					mGroupChat = mCnxManager.getChatApi().getGroupChat(mChatId);
					if (mGroupChat == null) {
						Utils.showMessageAndExit(this, getString(R.string.label_session_not_found), exitOnce);
						return false;
						
					}
					getSupportLoaderManager().initLoader(LOADER_ID, null, this);
					chatIdOnForeground = mChatId;
					// Get remote contact
					ContactId contact = null; //mGroupChat.getRemoteContact();
					// Get subject
					mSubject = mGroupChat.getSubject();
					updateGroupChatViewTitle(mSubject);
					// Set list of participants
					mParticipants = getListOfParticipants(mGroupChat.getParticipants());
					// Display accept/reject dialog
					// TODO manage new state ACCEPTING and REJECTED
					if (GroupChat.State.INVITED == mGroupChat.getState()) {
						displayAcceptRejectDialog(contact);
					}
				}
				return true;
				
			}
		} catch (RcsServiceNotAvailableException e) {
			e.printStackTrace();
			Utils.showMessageAndExit(this, getString(R.string.label_api_disabled), exitOnce);
		} catch (RcsServiceException e) {
			e.printStackTrace();
			Utils.showMessageAndExit(this, getString(R.string.label_api_failed), exitOnce);
		}
		return false;
	}

	/**
	 * Update the view title
	 * 
	 * @param subject
	 *            the group chat subject or null
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
		CursorLoader loader = new CursorLoader(this, Message.CONTENT_URI, PROJECTION, WHERE_CLAUSE,
				new String[] { mChatId }, QUERY_SORT_ORDER);
		return loader;
	}

	/**
	 * Display notification to accept or reject invitation
	 * 
	 * @param remote
	 *            remote contact
	 */
	private void displayAcceptRejectDialog(ContactId remote) {
		// Manual accept
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.title_group_chat);
		String from = RcsDisplayName.getInstance(this).getDisplayName(remote);
		String topic = (TextUtils.isEmpty(mSubject)) ? getString(R.string.label_no_subject) : mSubject;
		String msg = getString(R.string.label_gc_from_subject, from, topic);
		builder.setMessage(msg);
		builder.setCancelable(false);
		builder.setIcon(R.drawable.ri_notif_chat_icon);
		builder.setPositiveButton(getString(R.string.label_accept), new android.content.DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				try {
					// Accept the invitation
					mGroupChat.openChat();
				} catch (Exception e) {
					e.printStackTrace();
					Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_invitation_failed), exitOnce);
				}
			}
		});
		builder.setNegativeButton(getString(R.string.label_decline), new android.content.DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// Let session die by timeout
				// Exit activity
				finish();
			}
		});
		builder.show();
	}

	/**
	 * get a list of contact from a set of participant info
	 * 
	 * @param setOfParticipant
	 *            a set of participant info
	 * @return a list of contact
	 */
	private Set<ContactId> getListOfParticipants(Set<ParticipantInfo> setOfParticipant) {
		Set<ContactId> result = new HashSet<ContactId>();
		if (setOfParticipant.size() != 0) {
			for (ParticipantInfo participantInfo : setOfParticipant) {
				// TODO consider status ?
				result.add(participantInfo.getContact());
			}
		}
		return result;
	}

	/**
	 * get a set of contact from a set of participant info
	 * 
	 * @param setOfParticipant
	 *            a set of participant info
	 * @return a set of contact
	 */
	private Set<String> getSetOfParticipants(Set<ParticipantInfo> setOfParticipant) {
		Set<String> result = new HashSet<String>();
		if (setOfParticipant.size() != 0) {
			for (ParticipantInfo participantInfo : setOfParticipant) {
				// TODO consider status ?
				result.add(participantInfo.getContact().toString());
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
			mGroupChat = mCnxManager.getChatApi().initiateGroupChat(new HashSet<ContactId>(mParticipants), mSubject);
			mChatId = mGroupChat.getChatId();
			getSupportLoaderManager().initLoader(LOADER_ID, null, this);
			chatIdOnForeground = mChatId;
		} catch (Exception e) {
			e.printStackTrace();
			Utils.showMessageAndExit(this, getString(R.string.label_invitation_failed), exitOnce);
			return false;
		}

		// Display a progress dialog
		mProgressDialog = Utils.showProgressDialog(GroupChatView.this, getString(R.string.label_command_in_progress));
		mProgressDialog.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				Toast.makeText(GroupChatView.this, getString(R.string.label_chat_initiation_canceled), Toast.LENGTH_SHORT).show();
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
			Set<ParticipantInfo> currentContacts = mGroupChat.getParticipants();
			Set<RcsContact> contacts = mCnxManager.getContactsApi().getRcsContacts();
			for (RcsContact c1 : contacts) {
				ContactId contact = c1.getContactId();
				boolean found = false;
				for (ParticipantInfo c2 : currentContacts) {
					if (c2.getContact().equals(contact) && isConnected(c2.getStatus())) {
						found = true;
						break;
					}
				}
				if (!found) {
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
		builder.setPositiveButton(getString(R.string.label_ok), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int position) {
				// Add new participants in the session in background
				try {
					int max = mGroupChat.getMaxParticipants() - 1;
					int connected = mGroupChat.getParticipants().size();
					int limit = max - connected;
					if (selectedParticipants.size() > limit) {
						Utils.showMessage(GroupChatView.this, getString(R.string.label_max_participants));
						return;
					}

					// Display a progress dialog
					mProgressDialog = Utils.showProgressDialog(GroupChatView.this, getString(R.string.label_command_in_progress));

					Set<ContactId> contacts = new HashSet<ContactId>();
					ContactUtils contactUtils = ContactUtils.getInstance(GroupChatView.this);
					for (String participant : selectedParticipants) {
						contacts.add(contactUtils.formatContact(participant));
					}
					// Add participants
					mGroupChat.addParticipants(contacts);

					// Hide progress dialog
					if (mProgressDialog != null && mProgressDialog.isShowing()) {
						mProgressDialog.dismiss();
					}
				} catch (Exception e) {
					e.printStackTrace();
					if (mProgressDialog != null && mProgressDialog.isShowing()) {
						mProgressDialog.dismiss();
					}
					Utils.showMessage(GroupChatView.this, getString(R.string.label_add_participant_failed));
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
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_insert_smiley:
			Smileys.showSmileyDialog(this, composeText, getResources(), getString(R.string.menu_insert_smiley));
			break;

		case R.id.menu_participants:
			try {
				Utils.showList(this, getString(R.string.menu_participants), getSetOfParticipants(mGroupChat.getParticipants()));
			} catch (RcsServiceNotAvailableException e) {
				e.printStackTrace();
				Utils.showMessageAndExit(this, getString(R.string.label_api_disabled), exitOnce);
			} catch (RcsServiceException e) {
				e.printStackTrace();
				Utils.showMessageAndExit(this, getString(R.string.label_api_failed), exitOnce);
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
				e.printStackTrace();
				Utils.showMessageAndExit(this, getString(R.string.label_api_failed), exitOnce);
			}
			break;

		case R.id.menu_close_session:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(getString(R.string.title_chat_exit));
			builder.setPositiveButton(getString(R.string.label_ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
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
	 * Test if status is connected
	 * 
	 * @param status
	 *            the status
	 * @return true if connected
	 */
	private static boolean isConnected(int status) {
		return ((Status.CONNECTED == status) || (Status.PENDING == status) || (Status.BOOTED == status));
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

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			if (mChatId == null) {
				// Exit activity
				finish();
				return true;
				
			}
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(getString(R.string.title_chat_exit));
			builder.setPositiveButton(getString(R.string.label_ok),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							// Quit the session
							quitSession();
							if (LogUtils.isActive) {
								Log.d(LOGTAG, "Quit the session");
							}
						}
					});
			builder.setNegativeButton(getString(R.string.label_cancel),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							// Exit activity without aborting the session
							finish();
						}
					});
			builder.setCancelable(true);
			builder.show();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Initiate a new Group Chat
	 * 
	 * @param context
	 *            context
	 * @param subject
	 *            subject
	 * @param participants
	 *            list of participants
	 */
	public static void initiateGroupChat(Context context, String subject, ArrayList<String> participants) {
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
	 * @param context
	 * @param chatId
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
	 * @param context
	 * @param chatMessageDAO
	 *            the chat message from provider
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
	 * @param context
	 * @param chatId
	 *            the chat ID
	 * @param groupChatDAO
	 *            the Group Chat session from provider
	 * @return intent
	 */
	public static Intent forgeIntentInvitation(Context context, String chatId, GroupChatDAO groupChatDAO) {
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
	public INotifyComposing getNotifyComposing() {
		INotifyComposing notifyComposing = new IsComposingManager.INotifyComposing() {
			public void setTypingStatus(boolean isTyping) {
				if (mGroupChat != null) {
					return;
					
				}
				try {
					if (mGroupChat != null) {
						mGroupChat.sendIsComposingEvent(isTyping);
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

	@Override
	public boolean isSingleChat() {
		return false;
	}
}
