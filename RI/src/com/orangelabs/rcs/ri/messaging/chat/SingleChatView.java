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

package com.orangelabs.rcs.ri.messaging.chat;

import java.util.Set;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.RcsCommon;
import com.gsma.services.rcs.chat.ChatMessage;
import com.gsma.services.rcs.chat.GeolocMessage;
import com.gsma.services.rcs.chat.OneToOneChat;
import com.gsma.services.rcs.chat.OneToOneChatListener;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatService;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.ri.ApiConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
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
	/* package private */ final static String EXTRA_CONTACT = "contact";

	/**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(SingleChatView.class.getSimpleName());
	
	/**
	 * Remote contact
	 */
	private ContactId contact;
    
    /**
     * CHAT 
     */
	private OneToOneChat chat;
	
    /**
     * CHAT listener
     */
	private OneToOneChatListener chatListener = new OneToOneChatListener() {
		// Callback called when an Is-composing event has been received
		@Override
		public void onComposingEvent(final ContactId contact, final boolean status) {
			// Discard event if not for current contact
			if (SingleChatView.this.contact == null || !SingleChatView.this.contact.equals(contact)) {
				return;
			}
			handler.post(new Runnable() {
				public void run() {
					TextView view = (TextView) findViewById(R.id.isComposingText);
					if (status) {
						// Display is-composing notification
						view.setText(getString(R.string.label_contact_is_composing, contact.toString()));
						view.setVisibility(View.VISIBLE);
					} else {
						// Hide is-composing notification
						view.setVisibility(View.GONE);
					}
				}
			});
		}

		@Override
		public void onMessageStatusChanged(ContactId contact, final String msgId, int status, int reasonCode) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onMessageStatusChanged contact=" + contact + " msgId=" + msgId + " status=" + status+ " reason="+reasonCode);
			}
			// Discard event if not for current contact
			if (SingleChatView.this.contact == null || !SingleChatView.this.contact.equals(contact)) {
				return;
			}
			if (status > RiApplication.MESSAGE_STATUSES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onMessageStatusChanged unhandled status=" + status);
				}
				return;
			}
			if (reasonCode > RiApplication.MESSAGE_REASON_CODES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onMessageStatusChanged unhandled reason=" + reasonCode);
				}
				return;
			}
			final String notif = getString(R.string.label_message_status_changed, RiApplication.MESSAGE_STATUSES[status],
					RiApplication.MESSAGE_REASON_CODES[reasonCode]);
			handler.post(new Runnable() {
				public void run() {
					addNotifHistory(notif, msgId);
				}
			});
		}

	};
	
    @Override
	protected void onCreate(Bundle savedInstanceState) {
    	if (LogUtils.isActive) {
			Log.d(LOGTAG, "onCreate");
		}
        super.onCreate(savedInstanceState);
        
		if (connectionManager != null && !connectionManager.isServiceConnected(RcsServiceName.CHAT,RcsServiceName.CONTACTS)) {
			return;
		}
		try {
			ChatService chatService = connectionManager.getChatApi();

			// Add single chat event listener
			chatService.addEventListener(chatListener);

			processIntent(true);

			// Set max label length
			int maxMsgLength = chatService.getConfiguration().getOneToOneChatMessageMaxLength();
			if (maxMsgLength > 0) {
				// Set the message composer max length
				InputFilter[] filterArray = new InputFilter[1];
				filterArray[0] = new InputFilter.LengthFilter(maxMsgLength);
				composeText.setFilters(filterArray);
			}
			// Instantiate the composing manager
			composingManager = new IsComposingManager(chatService.getConfiguration().getIsComposingTimeout() * 1000);
		} catch (RcsServiceNotAvailableException e) {
			e.printStackTrace();
			Utils.showMessageAndExit(this, getString(R.string.label_api_disabled), exitOnce);
		} catch (RcsServiceException e) {
			e.printStackTrace();
			Utils.showMessageAndExit(this, getString(R.string.label_api_failed), exitOnce);
		}
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();
    	removeServiceListener();
    }
    
	@Override
	protected void onNewIntent(Intent intent) {
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "onNewIntent contact=" + contact);
		}
		super.onNewIntent(intent);
		// Replace the value of intent
		setIntent(intent);
		
		if (connectionManager.isServiceConnected(RcsServiceName.CHAT, RcsServiceName.CONTACTS)) {
			processIntent(false);
		}
	}
	
	/**
	 * Process intent
	 * @param loadHistory the history must be (re)loaded
	 */
	private void processIntent(boolean loadHistory) {
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "processIntent loadHistory="+loadHistory);
		}
		ChatService chatService = connectionManager.getChatApi();
    	ChatMessageDAO messageDao = null;
        int mode = getIntent().getIntExtra(SingleChatView.EXTRA_MODE, -1);
		switch (mode) {
		case ChatView.MODE_OPEN:
		case ChatView.MODE_OUTGOING:
			// Open chat
			contact = (ContactId) getIntent().getParcelableExtra(SingleChatView.EXTRA_CONTACT);
			try {
				// Open chat
				chat = chatService.openSingleChat(contact);
			} catch (RcsServiceNotAvailableException e) {
				e.printStackTrace();
				Utils.showMessageAndExit(this, getString(R.string.label_api_disabled), exitOnce);
			} catch (RcsServiceException e) {
				e.printStackTrace();
				Utils.showMessageAndExit(this, getString(R.string.label_api_failed), exitOnce);
			}
			loadHistory = true;
			break;

		case ChatView.MODE_INCOMING:
			// Get Incoming chat from its Intent
			messageDao = (ChatMessageDAO) (getIntent().getExtras()
					.getParcelable(ChatIntentService.BUNDLE_CHATMESSAGE_DAO_ID));
			if (messageDao == null || messageDao.getContact() == null) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "processIntent invalid chat message");
				}
				return;
			}
			// Are we switching to a new conversation ?
			if (!messageDao.getContact().equals(contact)) {
				// Force reload of history
				loadHistory = true;
				// Save new contactId
				contact = messageDao.getContact();
				try {
					// Open chat
					chat = chatService.openSingleChat(contact);
				} catch (RcsServiceNotAvailableException e) {
					e.printStackTrace();
					Utils.showMessageAndExit(this, getString(R.string.label_api_disabled), exitOnce);
				} catch (RcsServiceException e) {
					e.printStackTrace();
					Utils.showMessageAndExit(this, getString(R.string.label_api_failed), exitOnce);
				}
			}
			break;

		default:
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "processIntent invalid mode "+mode);
			}
			return;
		}
		keyChat = contact;
		// Set title
		setTitle(getString(R.string.title_chat) + " " +	contact);
		if (loadHistory) {
			// Activity is new or switch to new contact: load history
			Set<String> unreadMessageIDs = loadHistory(contact.toString());
			try {
				for (String msgId : unreadMessageIDs) {
					chatService.markMessageAsRead(msgId);
				}
			} catch (RcsServiceException e) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "processIntent failed to mark chat message as read");
				}
			}
		} else {
			// Activity is created: display new message
			if (messageDao.getMimeType() == null|| messageDao.getBody() == null) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "processIntent invalid chat message");
				}
				return;
			}
			
			try {
				String displayName = null;
				int direction = messageDao.getDirection();
				String msgId = messageDao.getMsgId();
				if (direction == RcsCommon.Direction.INCOMING) {
					// Get display name for incoming messages
					displayName = RcsDisplayName.get(this, contact);
				}
				// Add chat message to history
				addMessageHistory(direction, contact, messageDao.getBody(), messageDao.getMimeType(), msgId, displayName);

				try {
					chatService.markMessageAsRead(messageDao.getMsgId());
				} catch (RcsServiceException e) {
					if (LogUtils.isActive) {
						Log.e(LOGTAG, "onNewIntent failed to mark chat message as read");
					}
				}
			} catch (Exception e) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "processIntent failed to decode chat message", e);
				}
				return;
			}			
		}
	}
	
	@Override
    protected ChatMessage sendTextMessage(String msg) {
    	try {
    		if (LogUtils.isActive) {
        		Log.d(LOGTAG, "sendTextMessage msg=" + msg+" chat="+chat);
        	}
			// Send the text to remote
			ChatMessage message = chat.sendMessage(msg);
	        // Warn the composing manager that the message was sent
			composingManager.messageWasSent();
			return message;
	    } catch(Exception e) {
	    	e.printStackTrace();
	    	return null;
	    }
    }
    
    @Override
    protected GeolocMessage sendGeolocMessage(Geoloc geoloc) {
        try {
			// Send the text to remote
        	GeolocMessage message = chat.sendMessage(geoloc);
	        // Warn the composing manager that the message was sent
	    	composingManager.messageWasSent();
	    	return message;
	    } catch(Exception e) {
	    	e.printStackTrace();
	    	return null;
	    }
    }
    
    @Override
    protected void quitSession() {
    	chat = null;
        // Exit activity
		finish();        
    }        	
        
    @Override
    protected void setTypingStatus(boolean isTyping) {
		try {
			if (chat != null) {
				chat.sendIsComposingEvent(isTyping);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
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
				Smileys.showSmileyDialog(
						this, 
						composeText, 
						getResources(), 
						getString(R.string.menu_insert_smiley));
				break;
	
			case R.id.menu_quicktext:
				addQuickText();
				break;

			case R.id.menu_send_geoloc:
				getGeoLoc();
				break;	
							
			case R.id.menu_showus_map:
					showUsInMap(contact);
				break;	

			case R.id.menu_clear_log:
				// Delete conversation
				String where = ChatLog.Message.CHAT_ID + " = '" + contact + "'"; 
				getContentResolver().delete(ChatLog.Message.CONTENT_URI, where, null);
				
				// Refresh view
		        msgListAdapter = new MessageListAdapter(this);
		        setListAdapter(msgListAdapter);
				break;
		}
		return true;
	}
    
	private void removeServiceListener() {
		if (connectionManager != null && connectionManager.isServiceConnected(RcsServiceName.CHAT)) {
			try {
				connectionManager.getChatApi().removeEventListener(chatListener);
			} catch (RcsServiceException e) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "removeServiceListener failed", e);
				}
			}
		}
	}

}
