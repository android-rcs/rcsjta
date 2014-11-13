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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.gsma.services.rcs.RcsCommon;
import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatMessage;
import com.gsma.services.rcs.chat.ChatService;
import com.gsma.services.rcs.chat.ChatServiceConfiguration;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.chat.GeolocMessage;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.GroupChatIntent;
import com.gsma.services.rcs.chat.GroupChatListener;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.chat.ParticipantInfo.Status;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.ContactUtils;
import com.gsma.services.rcs.contacts.RcsContact;
import com.orangelabs.rcs.ri.ApiConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
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
	/* package private */ final static String EXTRA_CHAT_ID = "chat_id";
	/* package private */ final static String EXTRA_PARTICIPANTS = "participants";
	/* package private */ final static String EXTRA_SUBJECT = "subject";

	/**
	 * Subject
	 */
	private String subject;
	
    /**
     * CHAT ID 
     */
	private String chatId;

	/**
	 * Group chat
	 */
	private GroupChat groupChat;

    /**
     * List of participants
     */
    private Set<ContactId> participants = new HashSet<ContactId>();
    
    /**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(GroupChatView.class.getSimpleName());
	
    /**
     * Group chat listener
     */
	private GroupChatListener chatListener = new GroupChatListener() {

		// Callback called when an Is-composing event has been received
		public void onComposingEvent(final String chatId, final ContactId contact, final boolean status) {
			// Discard event if not for current chatId
			if (GroupChatView.this.chatId == null || !GroupChatView.this.chatId.equals(chatId)) {
				return;
			}
			handler.post(new Runnable() {
				public void run() {
					TextView view = (TextView) findViewById(R.id.isComposingText);
					if (status) {
						view.setText(getString(R.string.label_contact_is_composing, contact.toString()));
						view.setVisibility(View.VISIBLE);
					} else {
						view.setVisibility(View.GONE);
					}
				}
			});
		}

		@Override
		public void onParticipantInfoChanged(String chatId, final ParticipantInfo participant) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onParticipantStatusChanged chatId=" + chatId + " contact=" + participant.getContact() + " status="
						+ participant.getStatus());
			}
			if (participant.getStatus() > RiApplication.DELIVERY_STATUSES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onParticipantInfoStatusChanged unhandled status=" + participant.getStatus());
				}
				return;
			}
			// Discard event if not for current chatId
			if (GroupChatView.this.chatId == null || !GroupChatView.this.chatId.equals(chatId)) {
				return;
			}
			handler.post(new Runnable() {
				public void run() {
					String newStatus = RiApplication.PARTICIPANT_STATUSES[participant.getStatus()];
					addNotifHistory(getString(R.string.label_contact_status_changed, participant.getContact(), newStatus), null);
				}
			});
		}

		@Override
		public void onMessageGroupDeliveryInfoChanged(String chatId, ContactId contact, final String msgId, int status, int reasonCode) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onGroupDeliveryInfoChanged chatId=" + chatId + " contact=" + contact + " msgId=" + msgId
						+ " status=" + status + " reason=" + reasonCode);
			}
			if (status > RiApplication.DELIVERY_STATUSES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onGroupDeliveryInfoChanged unhandled status=" + status);
				}
				return;
			}
			if (reasonCode > RiApplication.DELIVERY_REASON_CODES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onGroupDeliveryInfoChanged unhandled reason=" + reasonCode);
				}
				return;
			}
			// Discard event if not for current chatId
			if (GroupChatView.this.chatId == null || !GroupChatView.this.chatId.equals(chatId)) {
				return;
			}
			final String notif = getString(R.string.label_delivery_status_changed, contact.toString(), RiApplication.DELIVERY_STATUSES[status], RiApplication.DELIVERY_REASON_CODES[0]);
			handler.post(new Runnable() {
				public void run() {
					addNotifHistory(notif, msgId);
				}
			});
		}

		@Override
		public void onStateChanged(String chatId, final int state, final int reasonCode) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onGroupChatStateChanged chatId=" + chatId + " state=" + state+" reason="+reasonCode);
			}
			// TODO CR031 enumerated types
			if (state > RiApplication.GC_STATES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onGroupChatStateChanged unhandled status=" + state);
				}
				return;
			}
			if (reasonCode > RiApplication.GC_REASON_CODES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onGroupChatStateChanged unhandled reason=" + reasonCode);
				}
				return;
			}
			// Discard event if not for current chatId
			if (GroupChatView.this.chatId == null || !GroupChatView.this.chatId.equals(chatId)) {
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
						addNotifHistory(getString(R.string.label_gc_state_changed, RiApplication.GC_STATES[state], _reasonCode),
								null);
					}
				}
			});
		};

		@Override
		public void onMessageStatusChanged(String chatId, final String msgId, int status, int reasonCode) {
			if (LogUtils.isActive) {
				Log.w(LOGTAG, "onMessageStatusChanged chatId="+chatId+" msgId=" + msgId + " status=" + status+ " reason="+reasonCode);
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
			// Discard event if not for current chatId
			if (GroupChatView.this.chatId == null || !GroupChatView.this.chatId.equals(chatId)) {
				return;
			}
			final String notif = getString(R.string.label_message_status_changed, RiApplication.MESSAGE_STATUSES[status], RiApplication.MESSAGE_REASON_CODES[reasonCode]);
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
		if (connectionManager != null && !connectionManager.isServiceConnected(RcsServiceName.CHAT, RcsServiceName.CONTACTS)) {
			return;
		}
		try {
			ChatService chatService = connectionManager.getChatApi();
			// Add group chat event listener
			chatService.addEventListener(chatListener);
			
			processIntent();
			
			ChatServiceConfiguration configuration = chatService.getConfiguration();
			// Set max label length
			int maxMsgLength = configuration.getGroupChatMessageMaxLength();
			if (maxMsgLength > 0) {
				InputFilter[] filterArray = new InputFilter[1];
				filterArray[0] = new InputFilter.LengthFilter(maxMsgLength);
				composeText.setFilters(filterArray);
			}

			// Instantiate the composing manager
			composingManager = new IsComposingManager(configuration.getIsComposingTimeout() * 1000);
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
			Log.d(LOGTAG, "onNewIntent chatId=" + chatId);
		}
		super.onNewIntent(intent);
		// Replace the value of intent
		setIntent(intent);
		
		if (connectionManager.isServiceConnected(RcsServiceName.CHAT, RcsServiceName.CONTACTS)) {
			processIntent();
		}
	}

	private void processIntent() {
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "processIntent");
		}
		try {
			switch (getIntent().getIntExtra(GroupChatView.EXTRA_MODE, -1)) {
			case ChatView.MODE_OUTGOING:
				// Check if the service is available
				boolean registered = connectionManager.getChatApi().isServiceRegistered();
				if (!registered) {
					Utils.showMessageAndExit(this, getString(R.string.label_service_not_available), exitOnce);
					return;
				}

				// Get subject
				subject = getIntent().getStringExtra(GroupChatView.EXTRA_SUBJECT);

				// Get participants
				ContactUtils contactUtils = ContactUtils.getInstance(this);
				List<String> contacts = getIntent().getStringArrayListExtra(GroupChatView.EXTRA_PARTICIPANTS);
				if (contacts != null && contacts.size() != 0) {
					for (String contact : contacts) {
						try {
							participants.add(contactUtils.formatContact(contact));
						} catch (RcsContactFormatException e) {
							if (LogUtils.isActive) {
								Log.e(LOGTAG, "processIntent invalid participant " + contact);
							}
						}
					}
					if (participants.isEmpty()) {
						Utils.showMessageAndExit(this, getString(R.string.label_invalid_contacts), exitOnce);
						return;
					}
				} else {
					Utils.showMessageAndExit(this, getString(R.string.label_invalid_contacts), exitOnce);
					return;
				}

				// Initiate group chat
				startGroupChat();
				break;
				
			case ChatView.MODE_OPEN:
				// Open an existing session
				chatId = getIntent().getStringExtra(GroupChatView.EXTRA_CHAT_ID);

				// Get chat session
				groupChat = connectionManager.getChatApi().getGroupChat(chatId);
				if (groupChat == null) {
					if (LogUtils.isActive) {
						Log.e(LOGTAG, "processIntent session not found for chatId=" + chatId);
					}
					Utils.showMessageAndExit(this, getString(R.string.label_session_not_found), exitOnce);
					return;
				}
				keyChat = chatId;
				// Get subject
				subject = groupChat.getSubject();

				// Set list of participants
				participants = getListOfParticipants(groupChat.getParticipants());
				if (LogUtils.isActive) {
					if (participants == null) {
						Log.e(LOGTAG, "processIntent chatId=" + chatId + " subject='" + subject + "'");
					}
				}
				break;
				
			case ChatView.MODE_INCOMING:
				ChatMessageDAO messageDao = (ChatMessageDAO) (getIntent().getExtras()
						.getParcelable(ChatIntentService.BUNDLE_CHATMESSAGE_DAO_ID));
				if (messageDao != null) {
					// New message
					if (messageDao.getChatId().equals(chatId)) {
						if (messageDao.getMimeType() == null || messageDao.getBody() == null) {
							if (LogUtils.isActive) {
								Log.e(LOGTAG, "processIntent invalid chat message");
							}
							return;
						}
						ContactId contact = messageDao.getContact();
						String displayName = RcsDisplayName.get(this, contact);
						// Add chat message to history
						addMessageHistory(messageDao.getDirection(), contact, messageDao.getBody(), messageDao.getMimeType(),
								messageDao.getMsgId(), displayName);
						connectionManager.getChatApi().markMessageAsRead(messageDao.getMsgId());
						return;
					} else {
						// Ignore message if it does not belong to current GC
						if (LogUtils.isActive) {
							Log.d(LOGTAG, "processIntent discard chat message "+messageDao.getMsgId()+" for chatId "+messageDao.getChatId());
						}
						return;
					}
				} else {
					// New GC invitation
					chatId = getIntent().getStringExtra(GroupChatIntent.EXTRA_CHAT_ID);
					groupChat = connectionManager.getChatApi().getGroupChat(chatId);
					if (groupChat == null) {
						Utils.showMessageAndExit(this, getString(R.string.label_session_not_found), exitOnce);
						return;
					}
					keyChat = chatId;
					// Get remote contact
					ContactId contact = groupChat.getRemoteContact();
					// Get subject
					subject = groupChat.getSubject();
					// Set list of participants
					participants = getListOfParticipants(groupChat.getParticipants());
					// Display accept/reject dialog
					// TODO manage new state ACCEPTING and REJECTED
					if (groupChat.getState() == GroupChat.State.INVITED) {
						displayAcceptRejectDialog(contact);
					}
				}
				break;
				
			default:
				return;
			}
			// Set title
			if ((subject != null) || (subject.length() > 0)) {
				setTitle(getString(R.string.title_group_chat) + " " + subject);
			} else {
				setTitle(getString(R.string.title_group_chat));
			}
			// Load history
			Set<String> unreadMessageIDs = loadHistory(chatId);
			for (String msgId : unreadMessageIDs) {
				connectionManager.getChatApi().markMessageAsRead(msgId);
			}

		} catch (RcsServiceNotAvailableException e) {
			e.printStackTrace();
			Utils.showMessageAndExit(this, getString(R.string.label_api_disabled), exitOnce);
		} catch (RcsServiceException e) {
			e.printStackTrace();
			Utils.showMessageAndExit(this, getString(R.string.label_api_failed), exitOnce);
		}
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
		String displayName = RcsDisplayName.get(this, remote);
		String from = RcsDisplayName.convert(this, RcsCommon.Direction.INCOMING, remote, displayName);
		String topic = (TextUtils.isEmpty(subject)) ? getString(R.string.label_no_subject) : subject;
		String msg = getString(R.string.label_gc_from_subject, from, topic);
		builder.setMessage(msg);
		builder.setCancelable(false);
		builder.setIcon(R.drawable.ri_notif_chat_icon);
		builder.setPositiveButton(getString(R.string.label_accept), new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				try {
					// Accept the invitation
					groupChat.acceptInvitation();
				} catch (Exception e) {
					e.printStackTrace();
					Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_invitation_failed), exitOnce);
				}
			}
		});
		builder.setNegativeButton(getString(R.string.label_decline), new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				try {
					// Reject the invitation
					groupChat.rejectInvitation();
				} catch (Exception e) {
					e.printStackTrace();
				}
				// Exit activity
				finish();
			}
		});
		builder.show();
	}
    
	/**
	 * get a list of contact from a set of participant info
	 * @param setOfParticipant a set of participant info
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
	 * @param setOfParticipant a set of participant info
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

    private void startGroupChat() {
		// Initiate the chat session in background
    	try {
    		groupChat = connectionManager.getChatApi().initiateGroupChat(new HashSet<ContactId>(participants), subject);
    		chatId = groupChat.getChatId();
    		keyChat = chatId;
    	} catch(Exception e) {
    		e.printStackTrace();
			Utils.showMessageAndExit(this, getString(R.string.label_invitation_failed), exitOnce);
			return;
    	}

        // Display a progress dialog
        progressDialog = Utils.showProgressDialog(GroupChatView.this, getString(R.string.label_command_in_progress));
        progressDialog.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				Toast.makeText(GroupChatView.this, getString(R.string.label_chat_initiation_canceled), Toast.LENGTH_SHORT).show();
				quitSession();
			}
		});
    }
    
    @Override
    protected ChatMessage sendTextMessage(String msg) {
        try {
			// Send the text to remote
        	ChatMessage message = groupChat.sendMessage(msg);
	    	
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
        	GeolocMessage message = groupChat.sendMessage(geoloc);
	    	
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
		// Stop session
    	try {
            if (groupChat != null) {
            	groupChat.leave();
            }
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	groupChat = null;
        
        // Exit activity
		finish();        
    }
    
    @Override
    protected void setTypingStatus(boolean isTyping) {
		try {
			if (groupChat != null) {
				groupChat.sendIsComposingEvent(isTyping);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	} 
    
    /**
	 * Add participants to be invited in the session
	 */
    private void addParticipants() {
		// Build list of available contacts not already in the conference
		Set<ContactId> availableParticipants = new HashSet<ContactId>();
		try {
			Set<ParticipantInfo> currentContacts = groupChat.getParticipants();
			Set<RcsContact> contacts = connectionManager.getContactsApi().getRcsContacts();
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
            	String c = (String)items[which];
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
            		int max = groupChat.getMaxParticipants()-1;
            		int connected = groupChat.getParticipants().size(); 
            		int limit = max-connected;
        			if (selectedParticipants.size() > limit) {
        				Utils.showMessage(GroupChatView.this, getString(R.string.label_max_participants));
        				return;
        			}

        			// Display a progress dialog
					progressDialog = Utils.showProgressDialog(GroupChatView.this, getString(R.string.label_command_in_progress));            

					Set<ContactId> contacts = new HashSet<ContactId>();
					ContactUtils contactUtils = ContactUtils.getInstance(GroupChatView.this);
					for (String participant : selectedParticipants) {
						contacts.add(contactUtils.formatContact(participant));
					}
					// Add participants
					groupChat.addParticipants(contacts);

					// Hide progress dialog 
					if (progressDialog != null && progressDialog.isShowing()) {
						progressDialog.dismiss();
					}
            	} catch(Exception e) {
            		e.printStackTrace();
					if (progressDialog != null && progressDialog.isShowing()) {
						progressDialog.dismiss();
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
		MenuInflater inflater=new MenuInflater(getApplicationContext());
		inflater.inflate(R.menu.menu_group_chat, menu);

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
				
			case R.id.menu_participants:
				try {
					Utils.showList(this, getString(R.string.menu_participants), getSetOfParticipants(groupChat.getParticipants()));			
			    } catch(RcsServiceNotAvailableException e) {
			    	e.printStackTrace();
					Utils.showMessageAndExit(this, getString(R.string.label_api_disabled), exitOnce);
			    } catch(RcsServiceException e) {
			    	e.printStackTrace();
					Utils.showMessageAndExit(this, getString(R.string.label_api_failed), exitOnce);
				}
				break;
	
			case R.id.menu_add_participant:
				addParticipants();
				break;
	
			case R.id.menu_send_geoloc:
				getGeoLoc();
				break;	
							
			case R.id.menu_send_file:
				try {
					Intent intent = new Intent(this, SendGroupFile.class);
					intent.putExtra(SendGroupFile.EXTRA_CHAT_ID, groupChat.getChatId());
					startActivity(intent);
			    } catch(RcsServiceException e) {
			    	e.printStackTrace();
					Utils.showMessageAndExit(this, getString(R.string.label_api_failed), exitOnce);
				}
				break;	

			case R.id.menu_showus_map:
				try {
					showUsInMap(getSetOfParticipants(groupChat.getParticipants()));
			    } catch(RcsServiceException e) {
			    	e.printStackTrace();
					Utils.showMessageAndExit(this, getString(R.string.label_api_failed), exitOnce);
				}
				break;	
				
			case R.id.menu_quicktext:
				addQuickText();
				break;
				
			case R.id.menu_clear_log:
				// Delete conversation
				String where = ChatLog.Message.CHAT_ID + " = '" + chatId + "'"; 
				getContentResolver().delete(ChatLog.Message.CONTENT_URI, where, null);
				
				// Refresh view
		        msgListAdapter = new MessageListAdapter(this);
		        setListAdapter(msgListAdapter);
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
		return ((status == Status.CONNECTED) || (status == Status.PENDING) || (status == Status.BOOTED));
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
