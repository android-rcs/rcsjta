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

import android.database.Cursor;
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
	 * View modes
	 */
	public final static int MODE_INCOMING = 0;
	public final static int MODE_OUTGOING = 1;
	public final static int MODE_OPEN = 2;

	/**
	 * Intent parameters
	 */
	public final static String EXTRA_MODE = "mode";
	public static String EXTRA_CONTACT = "contact";

	/**
	 * Remote contact
	 */
	private String contact = null;
	
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
	        int mode = getIntent().getIntExtra(SingleChatView.EXTRA_MODE, -1);
			if ((mode == SingleChatView.MODE_OPEN) || (mode == SingleChatView.MODE_OUTGOING)) {
				// Open chat
				contact = getIntent().getStringExtra(SingleChatView.EXTRA_CONTACT);				
			} else {
				// Incoming chat from its Intent
				contact = getIntent().getStringExtra(ChatIntent.EXTRA_CONTACT);
			}
    		
			// Set title
			setTitle(getString(R.string.title_chat) + " " +	contact);	

			// Set chat settings
	        isDeliveryDisplayed = chatApi.getConfiguration().isDisplayedDeliveryReport();

	        // Open chat
    		chat = chatApi.openSingleChat(contact, chatListener);
				
            // Load history
			loadHistory();

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
     */
    private void loadHistory() {
		try {
	    	Cursor cursor = getContentResolver().query(ChatLog.Message.CONTENT_URI, 
	    			new String[] {
	    				ChatLog.Message.DIRECTION,
	    				ChatLog.Message.CONTACT_NUMBER,
	    				ChatLog.Message.BODY,
	    				ChatLog.Message.TIMESTAMP,
	    				ChatLog.Message.MESSAGE_STATUS,
	    				ChatLog.Message.MESSAGE_TYPE
	    				},
	    			ChatLog.Message.CHAT_ID + "='" + contact + "'", 
	    			null, 
	    			ChatLog.Message.TIMESTAMP + " ASC");
	    	while(cursor.moveToNext()) {
	    		int direction = cursor.getInt(0);
	    		String contact = cursor.getString(1);
	    		String text = cursor.getString(2);
	    		int type = cursor.getInt(5);

	    		// Add only message to the history
	    		if (type == ChatLog.Message.Type.CONTENT) {
					addMessageHistory(direction, contact, text);
	    		}
	    	}
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
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
    }
}
