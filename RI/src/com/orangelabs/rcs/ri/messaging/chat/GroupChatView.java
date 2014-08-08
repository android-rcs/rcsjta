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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.gsma.services.rcs.JoynContactFormatException;
import com.gsma.services.rcs.JoynService;
import com.gsma.services.rcs.JoynServiceException;
import com.gsma.services.rcs.JoynServiceNotAvailableException;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.GroupChatIntent;
import com.gsma.services.rcs.chat.GroupChatListener;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.chat.ParticipantInfo.Status;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.ContactUtils;
import com.gsma.services.rcs.contacts.JoynContact;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Smileys;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Group chat view
 */
public class GroupChatView extends ChatView {
	/**
	 * View modes
	 */
	public final static int MODE_INCOMING = 0;
	public final static int MODE_OUTGOING = 1;
	public final static int MODE_OPEN = 2;

	/**
	 * Intent parameters
	 */
	public final static String EXTRA_MODE = "mode";
	public final static String EXTRA_CHAT_ID = "chat_id";
	public final static String EXTRA_PARTICIPANTS = "participants";
	public final static String EXTRA_SUBJECT = "subject";
	public final static String EXTRA_CONTACT = "contact";

	/**
	 * Array of participant status
	 */
	private static final String[] PARTICIPANT_STATUSES = RiApplication.getContext().getResources()
			.getStringArray(R.array.participant_statuses);

	/**
	 * Array of delivery states
	 */
	private static final String[] DELIVERY_STATUSES = RiApplication.getContext().getResources()
			.getStringArray(R.array.delivery_statuses);

	/**
	 * Array of delivery reason codes
	 */
	private static final String[] DELIVERY_REASON_CODES = RiApplication.getContext().getResources()
			.getStringArray(R.array.delivery_reason_codes);

	/**
	 * Array of Group Chat states
	 */
	private static final String[] GC_STATES = RiApplication.getContext().getResources().getStringArray(R.array.group_chat_states);

	/**
	 * Array of Group Chat reason codes
	 */
	private static final String[] GC_REASON_CODES = RiApplication.getContext().getResources()
			.getStringArray(R.array.group_chat_reason_codes);

	/**
	 * Subject
	 */
	private String subject;
	
    /**
     * Chat ID 
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
		public void onParticipantInfoStatusChanged(String chatId, final ParticipantInfo participant) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onParticipantStatusChanged chatId=" + chatId + " contact=" + participant.getContact() + " status="
						+ participant.getStatus());
			}
			if (participant.getStatus() > DELIVERY_STATUSES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onParticipantInfoStatusChanged unhandled status=" + participant.getStatus());
				}
				return;
			}
			handler.post(new Runnable() {
				public void run() {
					String newStatus = PARTICIPANT_STATUSES[participant.getStatus()];
					addNotifHistory(getString(R.string.label_contact_status_changed, participant.getContact(), newStatus), null);
				}
			});
		}

		@Override
		public void onDeliveryInfoStatusChanged(String chatId, ContactId contact, final String msgId, int status, int reasonCode) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onDeliveryInfoStatusChanged chatId=" + chatId + " contact=" + contact + " msgId=" + msgId
						+ " status=" + status + " reasonCode=" + reasonCode);
			}
			if (status > DELIVERY_STATUSES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onDeliveryInfoStatusChanged unhandled status=" + status);
				}
				return;
			}
			if (reasonCode > DELIVERY_REASON_CODES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onDeliveryInfoStatusChanged unhandled reason=" + reasonCode);
				}
				return;
			}
			final String notif = getString(R.string.label_delivery_status_changed, contact.toString(), DELIVERY_STATUSES[status], DELIVERY_REASON_CODES[reasonCode]);
			handler.post(new Runnable() {
				public void run() {
					addNotifHistory(notif, msgId);
				}
			});
		}

		@Override
		public void onGroupChatStateChanged(String chatId, final int state) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onGroupChatStateChanged chatId=" + chatId + " state=" + state);
			}
			if (state > GC_STATES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onGroupChatStateChanged unhandled status=" + state);
				}
				return;
			}
			// TODO : handle reason code (CR025)
			final String reason = GC_REASON_CODES[0];
			final String notif = getString(R.string.label_gc_state_changed, GC_STATES[state], reason);
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
						Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_chat_aborted, reason), exitOnce);
						break;
						
					// Add states
					// case GroupChat.State.REJECTED:
					// Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_chat_declined));
					// break;
						
					case GroupChat.State.FAILED:
						// Session is failed: exit
						Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_chat_failed, reason), exitOnce);
						break;
						
					default:
						addNotifHistory(notif, null);
					}
				}
			});
		};

		@Override
		public void onMessageStatusChanged(String chatId, final String msgId, int status) {
			if (LogUtils.isActive) {
				Log.w(LOGTAG, "onMessageStatusChanged chatId="+chatId+" msgId=" + msgId + " status=" + status);
			}
			if (status > MESSAGE_STATUSES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onMessageStatusChanged unhandled status=" + status);
				}
				return;
			}
			// TODO : handle reason code (CR025)
			int reasonCode = 0;
			String reason = (reasonCode == 0) ? "" : MESSAGE_REASON_CODES[0];
			final String notif = getString(R.string.label_message_status_changed, MESSAGE_STATUSES[status], reason);
			handler.post(new Runnable() {
				public void run() {
					addNotifHistory(notif, msgId);
				}
			});
		}
	};

    /**
     * Callback called when service is connected. This method is called when the
     * service is well connected to the RCS service (binding procedure successful):
     * this means the methods of the API may be used.
     */
    public void onServiceConnected() {
		try {
  		 	// Add group chat event listener
			chatApi.addGroupChatEventListener(chatListener);
			serviceConnected = true;

	        int mode = getIntent().getIntExtra(GroupChatView.EXTRA_MODE, -1);
			if (mode == GroupChatView.MODE_OUTGOING) {
				// Outgoing session
				
	            // Check if the service is available
	        	boolean registered = chatApi.isServiceRegistered();
	            if (!registered) {
	    	    	Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_service_not_available), exitOnce);
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
		        			participants.add(contactUtils.formatContactId(contact));
		        		} catch (JoynContactFormatException e) {
		        			if (LogUtils.isActive) {
								Log.e(LOGTAG, "onServiceConnected invalid participant "+contact);
							}
		        		}
					}
		        	if (participants.isEmpty()) {
		    			Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_invalid_contacts), exitOnce);
		    			return;
		        	}
		        } else {
	    			Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_invalid_contacts), exitOnce);
	    			return;
		        }
		        
		        // Initiate group chat
    			startGroupChat();				
			} else {
				if (mode == GroupChatView.MODE_OPEN) {
					// Open an existing session
					chatId = getIntent().getStringExtra(GroupChatView.EXTRA_CHAT_ID);

					// Get chat session
					groupChat = chatApi.getGroupChat(chatId);
					if (groupChat == null) {
						if (LogUtils.isActive) {
							Log.e(LOGTAG, "onServiceConnected session not found for chatId=" + chatId);
						}
						Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_session_not_found), exitOnce);
						return;
					}
					
					// Get subject
					subject = groupChat.getSubject();

					// Set list of participants
					participants = getListOfParticipants(groupChat.getParticipants());
					if (LogUtils.isActive) {
						if (participants == null) {
							Log.d(LOGTAG, "onServiceConnected chatId=" + chatId + " subject='" + subject + "'");
						} else {
							Log.d(LOGTAG, "onServiceConnected chatId=" + chatId + " subject='" + subject + "' participants="
									+ Arrays.toString(participants.toArray()));
						}

					}
				} else {
					// Incoming chat from its Intent
					chatId = getIntent().getStringExtra(GroupChatIntent.EXTRA_CHAT_ID);

					// Get chat session
					groupChat = chatApi.getGroupChat(chatId);
					if (groupChat == null) {
						Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_session_not_found), exitOnce);
						return;
					}
					
					// Get remote contact
					ContactId contact = groupChat.getRemoteContact();

					// Get subject
					subject = groupChat.getSubject();

					// Set list of participants
					participants = getListOfParticipants(groupChat.getParticipants());

					// Display accept/reject dialog
					// TODO manage new state ACCEPTING and REJECTED
					if (groupChat.getState() == GroupChat.State.INVITED) {
						// Manual accept
						AlertDialog.Builder builder = new AlertDialog.Builder(this);
						builder.setTitle(R.string.title_group_chat);
						String msg = getString(R.string.label_from) + " " + contact;
						if (TextUtils.isEmpty(subject)) {
							subject = "<" + getString(R.string.label_no_subject) + ">";
						}
						msg = msg + "\n" + getString(R.string.label_subject) + " " + subject;
						builder.setMessage(msg);
						builder.setCancelable(false);
						builder.setIcon(R.drawable.ri_notif_chat_icon);
						builder.setPositiveButton(getString(R.string.label_accept), acceptBtnListener);
						builder.setNegativeButton(getString(R.string.label_decline), declineBtnListener);
						builder.show();
					}
				}
			}
			
	        // Set title
	        if ((subject != null) || (subject.length() > 0)) {
    	    	setTitle(getString(R.string.title_group_chat) + " " + subject);
	        } else {
    	    	setTitle(getString(R.string.title_group_chat));
	        }

			// Load history
			unreadMessageIDs = loadHistory(chatId);

			for (String msgId : unreadMessageIDs) {
				chatApi.markMessageAsRead(msgId);
			}
			
            // Set max label length
			int maxMsgLength = chatApi.getConfiguration().getGroupChatMessageMaxLength();
			if (maxMsgLength > 0) {
				InputFilter[] filterArray = new InputFilter[1];
				filterArray[0] = new InputFilter.LengthFilter(maxMsgLength);
				composeText.setFilters(filterArray);
			}
			
			// Instantiate the composing manager
			composingManager = new IsComposingManager(chatApi.getConfiguration().getIsComposingTimeout() * 1000);
	    } catch(JoynServiceNotAvailableException e) {
	    	e.printStackTrace();
			Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_api_disabled), exitOnce);
	    } catch(JoynServiceException e) {
	    	e.printStackTrace();
			Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_api_failed), exitOnce);
		}
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
	
    /**
     * Callback called when service has been disconnected. This method is called when
     * the service is disconnected from the RCS service (e.g. service deactivated).
     * 
     * @param error Error
     * @see JoynService.Error
     */
    public void onServiceDisconnected(int error) {
		serviceConnected = false;
		Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_api_disabled), exitOnce);
    }    
    
    /**
     * Accept button listener
     */
    private OnClickListener acceptBtnListener = new OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
        	try {
        		// Accept the invitation
    			groupChat.acceptInvitation();
        	} catch(Exception e) {
        		e.printStackTrace();
				Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_invitation_failed), exitOnce);
        	}
        }
    };

    /**
     * Reject button listener
     */
    private OnClickListener declineBtnListener = new OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
        	try {
        		// Reject the invitation
    			groupChat.rejectInvitation();
        	} catch(Exception e) {
        		e.printStackTrace();
        	}

            // Exit activity
			finish();
        }
    };

    /**
     * Start group chat
     */
    private void startGroupChat() {
		// Initiate the chat session in background
    	try {
    		groupChat = chatApi.initiateGroupChat(new HashSet<ContactId>(participants), subject);
    	} catch(Exception e) {
    		e.printStackTrace();
			Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_invitation_failed), exitOnce);
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
    
    /**
     * Send text message
     * 
     * @param msg Message
     * @return Message ID
     */
    protected String sendTextMessage(String msg) {
        try {
			// Send the text to remote
        	String msgId = groupChat.sendMessage(msg);
	    	
	        // Warn the composing manager that the message was sent
	    	composingManager.messageWasSent();

	    	return msgId;
	    } catch(Exception e) {
	    	e.printStackTrace();
	    	return null;
	    }
    }
    
    /**
     * Send geoloc message
     * 
     * @param geoloc Geoloc
     * @return Message ID
     */
    protected String sendGeolocMessage(Geoloc geoloc) {
        try {
			// Send the text to remote
        	String msgId = groupChat.sendGeoloc(geoloc);
	    	
	        // Warn the composing manager that the message was sent
	    	composingManager.messageWasSent();

	    	return msgId;
	    } catch(Exception e) {
	    	e.printStackTrace();
	    	return null;
	    }
    }

    /**
     * Quit the session
     */
    protected void quitSession() {
		// Stop session
    	try {
            if (groupChat != null) {
            	groupChat.quitConversation();
            }
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	groupChat = null;
        
        // Exit activity
		finish();        
    }
    
    /**
     * Update the is composing status
     * 
     * @param isTyping Is composing status
     */
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
			Set<JoynContact> contacts = contactsApi.getJoynContacts();
			for (JoynContact c1 : contacts) {
				ContactId contact = c1.getContactId();
				boolean found = false;
				for(ParticipantInfo c2 : currentContacts) {
					if (c2.getContact().equals(contact) && isConnected(c2.getStatus())) {
						found = true;
						break;
					}
				}
				if (!found) {
					availableParticipants.add(contact);
				}
			}
		} catch(Exception e) {
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
						contacts.add(contactUtils.formatContactId(participant));
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
			    } catch(JoynServiceNotAvailableException e) {
			    	e.printStackTrace();
					Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_api_disabled), exitOnce);
			    } catch(JoynServiceException e) {
			    	e.printStackTrace();
					Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_api_failed), exitOnce);
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
			    } catch(JoynServiceException e) {
			    	e.printStackTrace();
					Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_api_failed), exitOnce);
				}
				break;	

			case R.id.menu_showus_map:
				try {
					showUsInMap(getSetOfParticipants(groupChat.getParticipants()));
			    } catch(JoynServiceException e) {
			    	e.printStackTrace();
					Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_api_failed), exitOnce);
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
	 * Test is status is connected
	 * 
	 * @param status
	 *            the status
	 * @return true if connected
	 * @hide
	 */
	private static boolean isConnected(int status) {
		return ((status == Status.CONNECTED) || (status == Status.PENDING) || (status == Status.BOOTED));
	}

	@Override
	protected void removeServiceListener() {
		if (serviceConnected) {
			try {
				chatApi.removeGroupChatEventListener(chatListener);
			} catch (JoynServiceException e) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "removeServiceListener failed",e);
				}
			}
		}
	}
}
