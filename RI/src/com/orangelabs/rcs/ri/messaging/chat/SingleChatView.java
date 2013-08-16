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

import org.gsma.joyn.JoynService;
import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.JoynServiceNotAvailableException;
import org.gsma.joyn.chat.Chat;
import org.gsma.joyn.chat.ChatIntent;
import org.gsma.joyn.chat.ChatListener;
import org.gsma.joyn.chat.ChatMessage;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
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
 * Single chat view
 */
public class SingleChatView extends ChatView {
	/**
	 * Intent parameters
	 */
	public static String EXTRA_CONTACT = "contact";

	/**
	 * Remote contact
	 */
	private String contact = null;
	
    /**
     * Chat ID 
     */
	private String chatId = null;

    /**
     * Chat 
     */
	private Chat chat = null;

	/**
	 * Delivery display report
	 */
	private boolean isDeliveryDisplayed = true;
	
    /**
     * Chat listener
     */
    private MyChatListener chatListener = new MyChatListener();	
    
    @Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    /**
     * Callback called when service is connected. This method is called when the
     * service is well connected to the RCS service (binding procedure successfull):
     * this means the methods of the API may be used.
     */
    public void onServiceConnected() {
    	try {
	        chatId = getIntent().getStringExtra(ChatIntent.EXTRA_CHAT_ID);
	        if (chatId != null) {
				// Incoming session
				
				// Remove the notification
		    	SingleChatInvitationReceiver.removeSingleChatNotification(this, chatId);
		    			
		    	// Get chat session
				Chat chat = chatApi.getChat(chatId);
				if (chat == null) {
	    			Utils.showMessageAndExit(SingleChatView.this, getString(R.string.label_session_not_found));
	    			return;
				}

				// Get contact
				contact = chat.getRemoteContact();

		    	// Get first message
				ChatMessage firstMessage = getIntent().getParcelableExtra(ChatIntent.EXTRA_FIRST_MESSAGE);

				// Display accept/reject dialog
	            if (!chatApi.getConfiguration().isChatAutoAcceptMode()) {
	                // Manual accept
	    			AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    			builder.setTitle(R.string.title_recv_chat);
	    			String msg = getString(R.string.label_from) + " " + contact;
	    			if ((firstMessage != null) && (firstMessage.getMessage().length() > 0)) {
	    				msg = msg + "\n" + getString(R.string.label_msg) + " " + firstMessage.getMessage();
	    			}
	    			builder.setMessage(msg);
	    			builder.setCancelable(false);
	    			builder.setIcon(R.drawable.ri_notif_chat_icon);
	    			builder.setPositiveButton(getString(R.string.label_accept), acceptBtnListener);
	    			builder.setNegativeButton(getString(R.string.label_decline), declineBtnListener);
	    			builder.show();
	            }				

	            // Load history
    			loadHistory(chatId);
    			
    			// Add chat event listener
				chat.addEventListener(chatListener);						

				// Send delivery report on the first message
		        if ((firstMessage != null) && (chatApi.getConfiguration().isDisplayedDeliveryReport())) {
					chat.sendDisplayedDeliveryReport(firstMessage.getId());
				}
			} else {
				// Outgoing session
    	        
		    	// Get remote contact
				contact = getIntent().getStringExtra(SingleChatView.EXTRA_CONTACT);
			}
			
			// Set title
			setTitle(getString(R.string.title_chat_view_oneone) + " " +	contact);	

			// Set chat settings
	        isDeliveryDisplayed = chatApi.getConfiguration().isDisplayedDeliveryReport();

	        // Set the message composer max length
			InputFilter[] filterArray = new InputFilter[1];
			filterArray[0] = new InputFilter.LengthFilter(chatApi.getConfiguration().getSingleChatMessageMaxLength());
			composeText.setFilters(filterArray);
			
			// Instanciate the composing manager
			composingManager = new IsComposingManager(chatApi.getConfiguration().getIsComposingTimeout() * 1000);
	    } catch(JoynServiceNotAvailableException e) {
	    	e.printStackTrace();
			Utils.showMessageAndExit(SingleChatView.this, getString(R.string.label_api_disabled));
	    } catch(JoynServiceException e) {
	    	e.printStackTrace();
			Utils.showMessageAndExit(SingleChatView.this, getString(R.string.label_api_failed));
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
		Utils.showMessageAndExit(SingleChatView.this, getString(R.string.label_api_disabled));
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
				Utils.showMessageAndExit(SingleChatView.this, getString(R.string.label_ims_disconnected));
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
// TODO            			chat.acceptInvitation();
	            	} catch(Exception e) {
	        			handler.post(new Runnable() { 
	        				public void run() {
	        					Utils.showMessageAndExit(SingleChatView.this, getString(R.string.label_invitation_failed));
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
// TODO            			chat.rejectInvitation();
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
     * Start chat
     * 
     * @param msg First message
     */
    private void startChat(final String msg) {
		// Initiate the chat session in background
        Thread thread = new Thread() {
        	public void run() {
            	try {
            		chat = chatApi.initiateSingleChat(contact, msg, chatListener);
            	} catch(Exception e) {
            		e.printStackTrace();
            		handler.post(new Runnable(){
            			public void run(){
            				Utils.showMessageAndExit(SingleChatView.this, getString(R.string.label_invitation_failed));		
            			}
            		});
            	}
        	}
        };
        thread.start();

        // Display a progress dialog
        progressDialog = Utils.showProgressDialog(SingleChatView.this, getString(R.string.label_command_in_progress));
        progressDialog.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				Toast.makeText(SingleChatView.this, getString(R.string.label_chat_initiation_canceled), Toast.LENGTH_SHORT).show();
				quitSession();
			}
		});
    }
    
    /**
     * Send message
     * 
     * @param msg Message
     */
    protected void sendMessage(String msg) {
        try {
        	if (chat == null) {
				// Initiate chat
    			startChat(msg);				
        	} else {
				// Send the text to remote
		    	chat.sendMessage(msg);
		    	
		        // Warn the composing manager that the message was sent
				composingManager.messageWasSent();
        	}
	    } catch(Exception e) {
	    	e.printStackTrace();
	    	Utils.showMessage(this, getString(R.string.label_send_im_failed));
	    }
    }
    
    /**
     * Load history
     * 
     * @param chatId Chat ID
     */
    protected void loadHistory(String chatId) {
    	// TODO
    	/*    	try {
    	EventsLogApi log = new EventsLogApi(this);
    	Uri uri = log.getOneToOneChatLogContentProviderUri();
    	Cursor cursor = getContentResolver().query(uri, 
    			new String[] {
    				RichMessagingData.KEY_CONTACT,
    				RichMessagingData.KEY_DATA,
    				RichMessagingData.KEY_TIMESTAMP,
    				RichMessagingData.KEY_STATUS,
    				RichMessagingData.KEY_TYPE
    				},
    			RichMessagingData.KEY_CHAT_SESSION_ID + "='" + session.getSessionID() + "'", 
    			null, 
    			RichMessagingData.KEY_TIMESTAMP + " DESC");
    	
    	// The system message are not loaded
    	while(cursor.moveToNext()) {
			int messageMessageType = cursor.getInt(EventsLogApi.TYPE_COLUMN);
			switch (messageMessageType) {
				case EventsLogApi.TYPE_OUTGOING_CHAT_MESSAGE:
				case EventsLogApi.TYPE_INCOMING_CHAT_MESSAGE:
				case EventsLogApi.TYPE_OUTGOING_GEOLOC:
				case EventsLogApi.TYPE_INCOMING_GEOLOC:
					updateView(cursor);
					break;
			}
    	}*/
	}

    /**
     * Mark a message as "displayed"
     * 
     * @param msg Message
     */
    protected void markMessageAsDisplayed(ChatMessage msg) {
        if (isDeliveryDisplayed) {
            try {
                chat.sendDisplayedDeliveryReport(msg.getId());
            } catch(Exception e) {
                // Nothing to do
            }
        }
    }

    /**
     * Mark a message as "read"
     */
    protected void markMessageAsRead(ChatMessage msg){
    	// TODO
    	/*
    	EventsLogApi events = new EventsLogApi(getApplicationContext());
    	events.markChatMessageAsRead(msg.getId(), true);*/
    }
        
    /**
     * Quit the session
     */
    protected void quitSession() {
		// Stop session
        Thread thread = new Thread() {
        	public void run() {
            	try {
                    if (chat != null) {
                		chat.removeEventListener(chatListener);
                    }
            	} catch(Exception e) {
            	}
            	chat = null;
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
			if (chat != null) {
				chat.sendIsComposingEvent(isTyping);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}    
    
    /**
	 * Add participants to be invited in the session
	 */
    private void addParticipants() {
    	// TODO
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
	
			case R.id.menu_add_participant:
				if (chat != null) {
					addParticipants();
				} else {
					Utils.showMessage(SingleChatView.this, getString(R.string.label_session_not_yet_started));
				}
				break;
	
			case R.id.menu_close_session:
				if (chat != null) {
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
     * Chat event listener
     */
    private class MyChatListener extends ChatListener {
    	// Callback called when a new message has been received
    	public void onNewMessage(final ChatMessage message) {
			if (message.isDisplayedReportRequested()) {
				// We received the message, mark it as displayed if the view is not in background
				markMessageAsDisplayed(message);
			}
			
			handler.post(new Runnable() { 
				public void run() {
					displayReceivedMessage(message);
				}
			});
    	}

    	// Callback called when a message has been delivered to the remote
    	public void onReportMessageDelivered(String msgId) {
			handler.post(new Runnable(){
				public void run(){
					addNotifHistory(getString(R.string.label_receive_delivery_status_delivered));
				}
			});
    	}

    	// Callback called when a message has been displayed by the remote
    	public void onReportMessageDisplayed(String msgId) {
			handler.post(new Runnable(){
				public void run(){
					addNotifHistory(getString(R.string.label_receive_delivery_status_displayed));
				}
			});
    	}

    	// Callback called when a message has failed to be delivered to the remote
    	public void onReportMessageFailed(String msgId) {
			handler.post(new Runnable(){
				public void run(){
					addNotifHistory(getString(R.string.label_receive_delivery_status_failed));
				}
			});
    	}

    	// Callback called when an Is-composing event has been received
    	public void onComposingEvent(final boolean status) {
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

    	// Callback called when a 1-1 conversation with a given contact has been
    	public void onChatExtendedToGroup(String contact, String groupChatId) {
    		// TODO
    	}
    }
}
