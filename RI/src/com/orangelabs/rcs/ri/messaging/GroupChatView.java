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

package com.orangelabs.rcs.ri.messaging;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gsma.joyn.JoynService;
import org.gsma.joyn.chat.ChatMessage;
import org.gsma.joyn.chat.GroupChat;
import org.gsma.joyn.chat.GroupChatIntent;
import org.gsma.joyn.chat.GroupChatListener;
import org.gsma.joyn.contacts.JoynContact;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.text.InputFilter;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Smileys;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Group chat view
 */
public class GroupChatView extends ChatView {
	/**
	 * Intent parameters
	 */
	public static String EXTRA_PARTICIPANTS = "participants";
	public static String EXTRA_SUBJECT = "subject";
	public static String EXTRA_CONTACT = "contact";

	/**
	 * Remote contact
	 */
	private String contact = null;

	/**
	 * Subject
	 */
	private String subject;
	
    /**
	 * Group chat
	 */
	private GroupChat groupChat = null;

    /**
     * List of participants
     */
    private ArrayList<String> participants = new ArrayList<String>();

    /**
     * Group chat listener
     */
    private MyGroupChatListener chatListener = new MyGroupChatListener();	
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set title
        setTitle(getString(R.string.title_chat_view_group));
    }
    
    /**
     * Callback called when service is connected. This method is called when the
     * service is well connected to the RCS service (binding procedure successfull):
     * this means the methods of the API may be used.
     */
    public void onServiceConnected() {
		try {
	        String chatId = getIntent().getStringExtra(GroupChatIntent.EXTRA_CHAT_ID);
			if (chatId != null) {
				// Incoming session

		        // Get chat session
				groupChat = chatApi.getGroupChat(chatId);
				if (groupChat == null) {
	    			Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_session_not_found));
	    			return;
				}

		    	// Get remote contact
				contact = groupChat.getRemoteContact();
				
				// Get subject
		        subject = groupChat.getSubject();

	            if (!chatApi.getConfiguration().isGroupChatAutoAcceptMode()) {
	                // Manual accept
	    			AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    			builder.setTitle(R.string.title_recv_group_chat);
	    			String msg = getString(R.string.label_from) + " " + contact;
	    			if (subject != null) {
	    				msg = msg + "\n" + getString(R.string.label_subject) + " " + subject;
	    			}
	    			builder.setMessage(msg);
	    			builder.setCancelable(false);
	    			builder.setIcon(R.drawable.ri_notif_chat_icon);
	    			builder.setPositiveButton(getString(R.string.label_accept), acceptBtnListener);
	    			builder.setNegativeButton(getString(R.string.label_decline), declineBtnListener);
	    			builder.show();
	            }

		        // Set list of participants
				participants = new ArrayList<String>(groupChat.getParticipants());

				// Load history
    			loadHistory(chatId);
    			
    			// Add chat event listener
				groupChat.addEventListener(chatListener);						
			} else {
				// Outgoing session
						    	        
		    	// Get remote contact
				contact = getIntent().getStringExtra(GroupChatView.EXTRA_CONTACT);

				// Get subject
		        subject = getIntent().getStringExtra(GroupChatView.EXTRA_SUBJECT);

		        // Get participants
		        participants = getIntent().getStringArrayListExtra(GroupChatView.EXTRA_PARTICIPANTS);
		        
		        // Initiate group chat
    			startGroupChat();
			}
			
	        // Set title
	        if ((subject != null) || (subject.length() > 0)) {
    	    	setTitle(getString(R.string.title_chat_view_group) + " " + subject);
	        }

			// Set the message composer max length
			InputFilter[] filterArray = new InputFilter[1];
			filterArray[0] = new InputFilter.LengthFilter(chatApi.getConfiguration().getGroupChatMessageMaxLength());
			composeText.setFilters(filterArray);

			// Instanciate the composing manager
			composingManager = new IsComposingManager(chatApi.getConfiguration().getIsComposingTimeout() * 1000);
		} catch(Exception e) {
			e.printStackTrace();
			Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_api_failed));
		}
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
     * Callback called when service is registered to the RCS/IMS platform
     */
    public void onServiceRegistered() {
    	// Nothing to do 
    }
    
    /**
     * Callback called when service is unregistered from the RCS/IMS platform
     */
    public void onServiceUnregistered() {
		handler.post(new Runnable(){
			public void run(){
				Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_ims_disconnected));
			}
		});
    }      
	
    /**
     * Accept button listener
     */
    private OnClickListener acceptBtnListener = new OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            Thread thread = new Thread() {
            	public void run() {
                	try {
                		// Accept the invitation
            			groupChat.acceptInvitation();
	            	} catch(Exception e) {
	        			handler.post(new Runnable() { 
	        				public void run() {
	        					Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_invitation_failed));
	        				}
	        			});
	            	}
            	}
            };
            thread.start();
        }
    };

    /**
     * Reject button listener
     */
    private OnClickListener declineBtnListener = new OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            Thread thread = new Thread() {
            	public void run() {
                	try {
                		// Reject the invitation
            			groupChat.rejectInvitation();
	            	} catch(Exception e) {
	            	}
            	}
            };
            thread.start();

            // Exit activity
			finish();
        }
    };

    /**
     * Start group chat
     */
    private void startGroupChat() {
		// Initiate the chat session in background
        Thread thread = new Thread() {
        	public void run() {
            	try {
            		groupChat = chatApi.initiateGroupChat(new HashSet<String>(participants), subject, chatListener);
            	} catch(Exception e) {
            		e.printStackTrace();
            		handler.post(new Runnable(){
            			public void run(){
            				Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_invitation_failed));		
            			}
            		});
            	}
        	}
        };
        thread.start();

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
     * 
     * @param chatId Chat ID
     */
    protected void loadHistory(String chatId) {
/*    	try {
	    	EventsLogApi log = new EventsLogApi(this);
	    	Uri uri = log.getGroupChatLogContentProviderUri();
	    	Cursor cursor = getContentResolver().query(uri, 
	    			new String[] {
	    				RichMessagingData.KEY_CONTACT,
	    				RichMessagingData.KEY_DATA,
	    				RichMessagingData.KEY_TIMESTAMP,
	    				RichMessagingData.KEY_STATUS,
	    				RichMessagingData.KEY_TYPE
	    				},
	    			RichMessagingData.KEY_CHAT_ID + "='" + session.getChatID() + "'", 
	    			null, 
	    			RichMessagingData.KEY_TIMESTAMP + " DESC");
	    	
	    	// The system message are not loaded
	    	while(cursor.moveToNext()) {
				int messageMessageType = cursor.getInt(EventsLogApi.TYPE_COLUMN);
				switch (messageMessageType) {
					case EventsLogApi.TYPE_OUTGOING_GROUP_CHAT_MESSAGE:
					case EventsLogApi.TYPE_INCOMING_GROUP_CHAT_MESSAGE:
					case EventsLogApi.TYPE_OUTGOING_GROUP_GEOLOC:
					case EventsLogApi.TYPE_INCOMING_GROUP_GEOLOC:
						updateView(cursor);
						break;
				}
	    	}
    	} catch(Exception e) {
    		e.printStackTrace();
    	}*/
    }
    
    /**
     * Send message
     * 
     * @param msg Message
     */
    protected void sendMessage(String msg) {
        try {
			// Send the text to remote
	    	groupChat.sendMessage(msg);
	    	
	        // Warn the composing manager that the message was sent
	    	composingManager.messageWasSent();
	    } catch(Exception e) {
	    	Utils.showMessage(GroupChatView.this, getString(R.string.label_send_im_failed));
	    }
    }
    
    /**
     * Mark a message as "displayed"
     * 
     * @param msg Message
     */
    protected void markMessageAsDisplayed(ChatMessage msg) {
        // Nothing to do
    }

    /**
     * Mark a message as "read"
     */
    protected void markMessageAsRead(ChatMessage msg) {
        // Nothing to do here
    }

    /**
     * Quit the session
     */
    protected void quitSession() {
		// Stop session
        Thread thread = new Thread() {
        	public void run() {
            	try {
                    if (groupChat != null) {
                    	groupChat.removeEventListener(chatListener);
                    	groupChat.quitConversation();
                    }
            	} catch(Exception e) {
            	}
            	groupChat = null;
        	}
        };
        thread.start();
        
        // Exit activity
		finish();        
    }
    
    /**
     * Update the is composing status
     * 
     * @param isTyping Is compoing status
     */
    protected void setTypingStatus(boolean isTyping) {
		try {
			groupChat.sendIsComposingEvent(isTyping);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
    
    /**
	 * Add participants to be invited in the session
	 */
    private void addParticipants() {
    	// Build list of available RCS contacts not already in the
    	// conference
    	List<String> availableParticipants = new ArrayList<String>(); 
		try {
			Set<String> currentContacts = groupChat.getParticipants();
			Set<JoynContact> contacts = contactsApi.getJoynContacts();
			for (JoynContact c1 : contacts) {
				for(String c2 : currentContacts) {
					if (!PhoneNumberUtils.compare(c1.getContact(), c2)) {
						availableParticipants.add(c1.getContact());
					}
				}
			}
		} catch(Exception e) {}
		
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
                Thread thread = new Thread() {
            		private Dialog progressDialog = null;
            		public void run() {
                        try {
                    		int max = groupChat.getMaxParticipants()-1;
                    		int connected = groupChat.getParticipants().size(); 
                    		int limit = max-connected;
	            			if (selectedParticipants.size() > limit) {
	            				Utils.showMessage(GroupChatView.this, getString(R.string.label_max_participants));
	            				return;
	            			}
	
	            			// Display a progress dialog
	    					handler.post(new Runnable(){
	    						public void run(){
	    							progressDialog = Utils.showProgressDialog(GroupChatView.this, getString(R.string.label_command_in_progress));            
	    						}
	    					});

	    					// Add participants
							groupChat.addParticipants(new HashSet<String>(selectedParticipants));

							// Hide progress dialog 
							handler.post(new Runnable(){
        						public void run(){
        							if (progressDialog != null && progressDialog.isShowing()) {
										progressDialog.dismiss();
									}
        						}
        					});
                    	} catch(Exception e) {
        					handler.post(new Runnable(){
        						public void run(){
        							if (progressDialog != null && progressDialog.isShowing()) {
        								progressDialog.dismiss();
        							}
        							Utils.showMessage(GroupChatView.this, getString(R.string.label_add_participant_failed));
        						}
        					});
                    	}
                	}
                };
                thread.start();
		    }
		});
        AlertDialog alert = builder.create();
    	alert.show();
    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater=new MenuInflater(getApplicationContext());
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
			
		case R.id.menu_wizz:
	        sendWizz();
			break;

		case R.id.menu_participants:
			try {
				Utils.showList(this, getString(R.string.menu_participants), groupChat.getParticipants());			
			} catch(Exception e) {
				Utils.showMessage(GroupChatView.this, getString(R.string.label_api_failed));
			}
			break;

		case R.id.menu_add_participant:
			if (groupChat != null) {
				addParticipants();
			} else {
				Utils.showMessage(GroupChatView.this, getString(R.string.label_session_not_yet_started));
			}
			break;

		case R.id.menu_close_session:
			if (groupChat != null) {
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
			} else {
            	// Exit activity
				finish();
			}
			break;
			
		case R.id.menu_quicktext:
			addQuickText();
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
    	
    	// Callback called when the session is terminated
    	public void onSessionTerminated(int reason) {
			handler.post(new Runnable(){
				public void run(){
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
						Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_invitation_declined));
					} else {
						Utils.showMessageAndExit(GroupChatView.this, getString(R.string.label_chat_failed, error));
					}					
				}
			});
    	}
    	
    	// Callback called when a new message has been received
    	public void onNewMessage(final ChatMessage message) {
			if (message.isDisplayedReportRequested()) {
				if (!isInBackground) {
					// We received the message, mark it as displayed if the view is not in background
					markMessageAsDisplayed(message);
				} else {
					// We save this message and will mark it as displayed when the activity resumes
					imReceivedInBackgroundToBeDisplayed.add(message);
				}
			} else {
				if (!isInBackground) {
					// We received the message, mark it as read if the view is not in background
					markMessageAsRead(message);
				} else {
					// We save this message and will mark it as read when the activity resumes
					imReceivedInBackgroundToBeRead.add(message);
				}
			}
			
			handler.post(new Runnable() { 
				public void run() {
					displayReceivedMessage(message);
				}
			});
    	}

    	// Callback called when a message has been delivered to the remote
    	public void onReportMessageDelivered(String msgId) {
    		// Not used here
    	}

    	// Callback called when a message has been displayed by the remote
    	public void onReportMessageDisplayed(String msgId) {
    		// Not used here
    	}
    	
    	// Callback called when a message has failed to be delivered to the remote
    	public void onReportMessageFailed(String msgId) {
    		// TODO
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
    };	
}
