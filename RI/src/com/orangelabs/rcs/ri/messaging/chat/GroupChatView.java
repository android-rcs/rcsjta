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
import android.database.Cursor;
import android.net.Uri;
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

import com.gsma.services.rcs.JoynService;
import com.gsma.services.rcs.JoynServiceException;
import com.gsma.services.rcs.JoynServiceNotAvailableException;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatMessage;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.chat.GeolocMessage;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.GroupChatIntent;
import com.gsma.services.rcs.chat.GroupChatListener;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.chat.ParticipantInfo.Status;
import com.gsma.services.rcs.contacts.JoynContact;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.session.MultimediaSessionView;
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

	// @formatter:off
	private final static String[] TAB_PARTICIPANT_STATUS = { "UNKNOWN", 
															"CONNECTED", 
															"DISCONNECTED", 
															"DEPARTED", 
															"BOOTED", 
															"FAILED", 
															"BUSY",
															"DECLINED", 
															"PENDING" };
	// @formatter:on
	/**
	 * Remote contact
	 */
	private String contact;

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
    private List<String> participants = new ArrayList<String>();

    /**
     * Group chat listener
     */
    private MyGroupChatListener chatListener = new MyGroupChatListener();
    
    /**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(GroupChatView.class.getSimpleName());
    
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    /**
     * Callback called when service is connected. This method is called when the
     * service is well connected to the RCS service (binding procedure successful):
     * this means the methods of the API may be used.
     */
    public void onServiceConnected() {
		try {
	        int mode = getIntent().getIntExtra(GroupChatView.EXTRA_MODE, -1);
			if (mode == GroupChatView.MODE_OUTGOING) {
				// Outgoing session
				
	            // Check if the service is available
	        	boolean registered = false;
	        	try {
	        		if ((chatApi != null) && chatApi.isServiceRegistered()) {
	        			registered = true;
	        		}
	        	} catch(Exception e) {
	        		e.printStackTrace();
	        	}
	            if (!registered) {
	    	    	Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_service_not_available));
	    	    	return;
	            }     				

		    	// Get remote contact
				contact = getIntent().getStringExtra(GroupChatView.EXTRA_CONTACT);

				// Get subject
		        subject = getIntent().getStringExtra(GroupChatView.EXTRA_SUBJECT);

		        // Get participants
		        participants = getIntent().getStringArrayListExtra(GroupChatView.EXTRA_PARTICIPANTS);
		        
		        // Initiate group chat
    			startGroupChat();				
			} else
			if (mode == MultimediaSessionView.MODE_OPEN) {
				// Open an existing session
		        chatId = getIntent().getStringExtra(GroupChatView.EXTRA_CHAT_ID);
		        
		    	// Get chat session
				groupChat = chatApi.getGroupChat(chatId);
				if (groupChat == null) {
					if (LogUtils.isActive) {
						Log.e(LOGTAG, "onServiceConnected session not found for chatId=" + chatId);
					}
	    			Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_session_not_found));
	    			return;
				}

    			// Add chat event listener
				groupChat.addEventListener(chatListener);

				// Get remote contact
				contact = groupChat.getRemoteContact();
				
				// Get subject
		        subject = groupChat.getSubject();

		        // Set list of participants
		        participants = getListOfParticipants(groupChat.getParticipants());
		        if (LogUtils.isActive) {
		        	if (participants == null) {
		        		Log.d(LOGTAG, "onServiceConnected chatId=" + chatId+" subject='"+subject+"'");
		        	} else {
		        		Log.d(LOGTAG, "onServiceConnected chatId=" + chatId+" subject='"+subject+"' participants="+Arrays.toString(participants.toArray()));
		        	}
					
					
				}
			} else {
				// Incoming chat from its Intent
		        chatId = getIntent().getStringExtra(GroupChatIntent.EXTRA_CHAT_ID);

		    	// Get chat session
				groupChat = chatApi.getGroupChat(chatId);
				if (groupChat == null) {
	    			Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_session_not_found));
	    			return;
				}

    			// Add chat event listener
				groupChat.addEventListener(chatListener);
				
		    	// Get remote contact
				contact = groupChat.getRemoteContact();
				
				// Get subject
		        subject = groupChat.getSubject();

		        // Set list of participants
				participants = getListOfParticipants(groupChat.getParticipants());
				
				// Display accept/reject dialog
				if (!chatApi.getConfiguration().isGroupChatAutoAcceptMode()) {
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
			
	        // Set title
	        if ((subject != null) || (subject.length() > 0)) {
    	    	setTitle(getString(R.string.title_group_chat) + " " + subject);
	        } else {
    	    	setTitle(getString(R.string.title_group_chat));
	        }

			// Load history
			loadHistory();

    		// Set chat settings
            isDeliveryDisplayed = chatApi.getConfiguration().isDisplayedDeliveryReport();

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
			Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_api_disabled));
	    } catch(JoynServiceException e) {
	    	e.printStackTrace();
			Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_api_failed));
		}
    }
    
	/**
	 * get a list of contact from a set of participant info
	 * @param setOfParticipant a set of participant info
	 * @return a list of contact
	 */
	private List<String> getListOfParticipants(Set<ParticipantInfo> setOfParticipant) {
		List<String> result = new ArrayList<String>();
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
				result.add(participantInfo.getContact());
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
		Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_api_disabled));
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
				Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_invitation_failed));
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
    		groupChat = chatApi.initiateGroupChat(new HashSet<String>(participants), subject, chatListener);
    	} catch(Exception e) {
    		e.printStackTrace();
			Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_invitation_failed));		
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
     * Load history
     */
    private void loadHistory() {
		if (chatId == null) {
			return;
		}		
		// TODO bug Uri uri = Uri.withAppendedPath(ChatLog.Message.CONTENT_CHAT_URI, chatId);
		Uri uri = ChatLog.Message.CONTENT_URI;
		Cursor cursor = null;
		try {
			// @formatter:off
			String[] projection = new String[] {
	    				ChatLog.Message.DIRECTION,
	    				ChatLog.Message.CONTACT_NUMBER,
	    				ChatLog.Message.BODY,
	    				ChatLog.Message.MIME_TYPE,
	    				ChatLog.Message.MESSAGE_TYPE };
			// @formatter:on
			String where = ChatLog.Message.CHAT_ID + "= ?";
			String[] whereArgs = new String[] { chatId };
			cursor = getContentResolver().query(uri, projection, where, whereArgs, ChatLog.Message.TIMESTAMP + " ASC");
			while (cursor.moveToNext()) {
				int direction = cursor.getInt(0);
				String contact = cursor.getString(1);
				String content = cursor.getString(2);
				String contentType = cursor.getString(3);
				int type = cursor.getInt(4);

				// Add only messages to the history
				if (type != ChatLog.Message.Type.SYSTEM) {
					addMessageHistory(direction, contact, content, contentType);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
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
            	groupChat.removeEventListener(chatListener);
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
     * Send a displayed report
     * 
     * @param msgId Message ID
     */
    private void sendDisplayedReport(String msgId) {
        try {
			if (chatApi != null) {
				chatApi.markMessageAsRead(msgId);
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
    	List<String> availableParticipants = new ArrayList<String>(); 
		try {
			Set<ParticipantInfo> currentContacts = groupChat.getParticipants();
			Set<JoynContact> contacts = contactsApi.getJoynContacts();
			for (JoynContact c1 : contacts) {
				String contact = c1.getContactId();
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
		for(int i=0; i < availableParticipants.size(); i++) {
			items[i] = availableParticipants.get(i);
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

					// Add participants
					groupChat.addParticipants(new HashSet<String>(selectedParticipants));

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
					Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_api_disabled));
			    } catch(JoynServiceException e) {
			    	e.printStackTrace();
					Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_api_failed));
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
					Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_api_failed));
				}
				break;	

			case R.id.menu_showus_map:
				try {
					showUsInMap(getSetOfParticipants(groupChat.getParticipants()));
			    } catch(JoynServiceException e) {
			    	e.printStackTrace();
					Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_api_failed));
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
     * Group chat event listener
     */
    private class MyGroupChatListener extends GroupChatListener {
    	// Callback called when the session is well established
    	public void onSessionStarted() {
			handler.post(new Runnable() { 
				public void run() {
					// Hide progress dialog
					hideProgressDialog();
				}
			});
    	}
    	
    	// Callback called when the session has been aborted
    	public void onSessionAborted() {
			handler.post(new Runnable() { 
				public void run() {
					// Hide progress dialog
					hideProgressDialog();
					
					// Session aborted
					Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_chat_aborted));
				}
			});
    	}

    	// Callback called when the session has failed
    	public void onSessionError(final int error) {
			handler.post(new Runnable() {
				public void run() {
					// Display error
					if (error == GroupChat.Error.INVITATION_DECLINED) {
						Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_chat_declined));
					} else {
						Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_chat_failed, error));
					}					
				}
			});
    	}
    	
    	// Callback called when a new message has been received
    	public void onNewMessage(final ChatMessage message) {
			handler.post(new Runnable() { 
				public void run() {
					// Send a displayed delivery report
			        if (isDeliveryDisplayed) {
			        	sendDisplayedReport(message.getId());
			        }

			        // Display the received message
					displayReceivedMessage(message);
				}
			});
    	}
    	
    	// Callback called when a new geoloc has been received
    	public void onNewGeoloc(final GeolocMessage message) {
			handler.post(new Runnable() { 
				public void run() {
					// Send a displayed delivery report
			        if (isDeliveryDisplayed) {
			        	sendDisplayedReport(message.getId());
			        }

			        // Display the received geoloc
			        displayReceivedGeoloc(message);
				}
			});
    	}    	

    	// Callback called when a message has been delivered to the remote
    	public void onReportMessageDelivered(String msgId) {
			handler.post(new Runnable() { 
				public void run() {
					addNotifHistory(getString(R.string.label_receive_delivery_status_delivered));
				}
			});
    	}

    	// Callback called when a message has been displayed by the remote
    	public void onReportMessageDisplayed(String msgId) {
			handler.post(new Runnable() { 
				public void run() {
					addNotifHistory(getString(R.string.label_receive_delivery_status_displayed));
				}
			});
    	}
    	
    	// Callback called when a message has failed to be delivered to the remote
    	public void onReportMessageFailed(String msgId) {
			handler.post(new Runnable() { 
				public void run() {
					addNotifHistory(getString(R.string.label_receive_delivery_status_failed));
				}
			});
    	}
    	
    	// Callback called when an Is-composing event has been received
    	public void onComposingEvent(final String contact, final boolean status) {
			handler.post(new Runnable() {
				public void run(){
					TextView view = (TextView)findViewById(R.id.isComposingText);
					if (status) {
						view.setText(contact + " " + getString(R.string.label_contact_is_composing));
						view.setVisibility(View.VISIBLE);
					} else {
						view.setVisibility(View.GONE);
					}
				}
			});
    	}

    	// Callback called when a new participant has joined the group chat
    	public void onParticipantJoined(final String contact, String contactDisplayname) {
			handler.post(new Runnable() {
				public void run(){
					addNotifHistory(getString(R.string.label_contact_joined, contact));
				}
			});
    	}
    	
    	// Callback called when a participant has left voluntary the group chat
    	public void onParticipantLeft(final String contact) {
			handler.post(new Runnable() {
				public void run(){
					addNotifHistory(getString(R.string.label_contact_left, contact));
				}
			});
    	}

    	// Callback called when a participant is disconnected from the group chat
    	public void onParticipantDisconnected(final String contact) {
			handler.post(new Runnable() {
				public void run(){
					addNotifHistory(getString(R.string.label_contact_disconnected, contact));
				}
			});
    	}

		/* (non-Javadoc)
		 * @see com.gsma.services.rcs.chat.GroupChatListener#onParticipantStatusChanged(com.gsma.services.rcs.chat.ParticipantInfo)
		 */
		@Override
		public void onParticipantStatusChanged(final ParticipantInfo participant) {
			handler.post(new Runnable() {
				public void run() {
					String newStatus = TAB_PARTICIPANT_STATUS[participant.getStatus() % (TAB_PARTICIPANT_STATUS.length + 1)];
					if (LogUtils.isActive) {
						Log.d(LOGTAG, "onParticipantStatusChanged contact=" + participant.getContact() + " status=" + newStatus);
					}
					addNotifHistory(getString(R.string.label_contact_status_changed, participant.getContact(), newStatus));
				}
			});
		}
    };
    
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
}
