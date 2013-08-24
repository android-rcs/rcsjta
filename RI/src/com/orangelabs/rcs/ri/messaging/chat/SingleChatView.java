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
import org.gsma.joyn.chat.ChatLog;
import org.gsma.joyn.chat.ChatMessage;

import android.os.Bundle;
import android.text.InputFilter;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

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
    		String firstMsg = getIntent().getStringExtra(ChatIntent.EXTRA_FIRST_MESSAGE);
			if (firstMsg == null) {
				// Incoming session
				contact = getIntent().getStringExtra(ChatIntent.EXTRA_CONTACT);
			} else {
				// Outgoing session
				contact = getIntent().getStringExtra(SingleChatView.EXTRA_CONTACT);				
			}
			
			// Set title
			setTitle(getString(R.string.title_chat) + " " +	contact);	

			// Set chat settings
	        isDeliveryDisplayed = chatApi.getConfiguration().isDisplayedDeliveryReport();

	        // Open chat
    		chat = chatApi.openSingleChat(contact, chatListener);
				
            // Load history
			loadHistory(chatId);

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
     * Send a message
     * 
     * @param msg Message
     * @return Message ID
     */
    protected String sendMessage(String msg) {
    	try {
			// Send the text to remote
			String msgId = chat.sendMessage(msg);
			
	        // Warn the composing manager that the message was sent
			composingManager.messageWasSent();

			return msgId;
	    } catch(Exception e) {
	    	e.printStackTrace();
	    	return null;
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
     * Quit the session
     */
    protected void quitSession() {
		// Remove listener
    	try {
            if (chat != null) {
        		chat.removeEventListener(chatListener);
            }
    	} catch(Exception e) {
    	}
    	chat = null;
        
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
     * Send a displayed report
     * 
     * @param msg Message
     */
    private void sendDisplayedReport(ChatMessage msg) {
        if ((isDeliveryDisplayed) && msg.isDisplayedReportRequested()) {
            try {
                chat.sendDisplayedDeliveryReport(msg.getId());
            } catch(Exception e) {
                // Nothing to do
            }
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
	
			case R.id.menu_add_participant:
				addParticipants();
				break;
	
			case R.id.menu_quicktext:
				addQuickText();
				break;

			case R.id.menu_clear_log:
				// Delete conversation
				String where = ChatLog.Message.CHAT_ID + " = '" + contact + "'"; 
				getContentResolver().delete(ChatLog.Message.CONTENT_URI, where, null);
				
				// Refresh view
		        msgListAdapter = new MessageListAdapter(this);
		        setListAdapter(msgListAdapter);
				break;
				
			case R.id.menu_close_session:
            	// Exit activity
				finish();
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
			handler.post(new Runnable() { 
				public void run() {
					// Send a delivery report
					sendDisplayedReport(message);
					
					// Display the received message
					displayReceivedMessage(message);
				}
			});
    	}

    	// Callback called when a message has been delivered to the remote
    	public void onReportMessageDelivered(String msgId) {
			handler.post(new Runnable(){
				public void run(){
					// Display a notification
					addNotifHistory(getString(R.string.label_receive_delivery_status_delivered));
				}
			});
    	}

    	// Callback called when a message has been displayed by the remote
    	public void onReportMessageDisplayed(String msgId) {
			handler.post(new Runnable(){
				public void run(){
					// Display a notification
					addNotifHistory(getString(R.string.label_receive_delivery_status_displayed));
				}
			});
    	}

    	// Callback called when a message has failed to be delivered to the remote
    	public void onReportMessageFailed(String msgId) {
			handler.post(new Runnable(){
				public void run(){
					// Display a notification
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
						// Display is-composing notification
						view.setText(contact + " " + getString(R.string.label_contact_is_composing));
						view.setVisibility(View.VISIBLE);
					} else {
						// Hide is-composing notification
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
