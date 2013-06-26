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

import org.gsma.joyn.chat.ChatMessage;
import org.gsma.joyn.chat.ChatService;
import org.gsma.joyn.chat.GroupChat;
import org.gsma.joyn.chat.GroupChatListener;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Handler;
import android.widget.Toast;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Restart a group chat session
 */
public class RestartChat {
    /**
     * UI handler
     */
    private Handler handler = new Handler();

    /**
     * Progress dialog
     */
    private Dialog progressDialog = null;

	/**
     * Activity
     */
    private Activity activity;
    
    /**
	 * Chat API
	 */
    private ChatService chatApi;
    
    /**
	 * Chat ID to restart
	 */
	private String chatId;

	/**
	 * Restarted chat session
	 */
	private GroupChat groupChat = null; 

    /**
     * Group chat listener
     */
    private MyGroupChatListener chatListener = new MyGroupChatListener();	
	
	/**
     * Constructor
     * 
     * @param context Context
     * @param chatApi Chat API
     * @param chatId Chat ID
     */
	public RestartChat(Activity activity, ChatService chatApi, String chatId) {
		this.activity = activity;
		this.chatApi = chatApi;
		this.chatId = chatId;
	}
    
    /**
     * Start restart session
     */
    public synchronized void start() {
    	if (groupChat != null) {
    		return;
    	}
    	
    	// Initiate the session in background
        Thread thread = new Thread() {
        	public void run() {
            	try {
            		groupChat = chatApi.restartGroupChat(chatId);
            		groupChat.addEventListener(chatListener);
            	} catch(Exception e) {
            		handler.post(new Runnable(){
            			public void run(){
        					// Hide progress dialog
        					hideProgressDialog();

        					// Show error
        					Utils.showMessage(activity, activity.getString(R.string.label_restart_chat_exception));		
            			}
            		});
            	}
        	}
        };
        thread.start();
        
        // Display a progress dialog
        progressDialog = Utils.showProgressDialog(activity, activity.getString(R.string.label_command_in_progress));
        progressDialog.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				// Stop session
				stop();

				// Hide progress dialog
				hideProgressDialog();

				// Display feedback info
				Toast.makeText(activity,activity.getString(R.string.label_chat_restart_canceled), Toast.LENGTH_SHORT).show();
			}
		});
    }    

    /**
     * Stop restart session
     */
    public synchronized void stop() {
    	if (groupChat == null) {
    		return;
    	}

    	// Stop session
        Thread thread = new Thread() {
        	public void run() {
            	try {
            		groupChat.removeEventListener(chatListener);
            		groupChat.quitConversation();
            	} catch(Exception e) {
            	}
            	groupChat = null;
        	}
        };
        thread.start();
    }    
    
    /**
     * Group chat event listener
     */
    private class MyGroupChatListener extends GroupChatListener {
    	// Session started
    	public void onSessionStarted() {
			handler.post(new Runnable() { 
				public void run() {
					try {
	                    // Hide progress dialog
						hideProgressDialog();
	
						// Remove listener now
                		groupChat.removeEventListener(chatListener);

						// Display chat view
                		Intent intent = new Intent(activity, GroupChatView.class);
			        	intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		            	intent.putExtra("subject", groupChat.getSubject());
			    		intent.putExtra("sessionId", groupChat.getChatId());
			    		activity.startActivity(intent);
					} catch(Exception e) {
						Utils.showMessage(activity, activity.getString(R.string.label_api_failed));
					}
				}
			});
    	}
    	
    	// Session terminated
    	public void onSessionTerminated(int reason) {
			handler.post(new Runnable() { 
				public void run() {
					// Hide progress dialog
					hideProgressDialog();

					// Display error
					Utils.showMessage(activity, activity.getString(R.string.label_restart_chat_terminated));
				}
			});
    	}

    	// Session error
    	public void onSessionError(final int error) {
			handler.post(new Runnable() { 
				public void run() {
					// Hide progress dialog
					hideProgressDialog();

					// Display error
					Utils.showMessage(activity, activity.getString(R.string.label_restart_chat_failed, error));
				}
			});
    	}
    	
    	// New message has been received
    	public void onNewMessage(ChatMessage message) {
    		// Not used here
    	}
    	
    	// A message has been delivered to the remote
    	public void onReportMessageDelivered(String msgId) {
    		// Not used here
    	}

    	// A message has been displayed by the remote
    	public void onReportMessageDisplayed(String msgId) {
    		// Not used here
    	}

    	// A message has failed to be delivered to the remote
    	public void onReportMessageFailed(String msgId) {
    		// Not used here
    	}
    	
    	// Is-composing event has been received
    	public void onComposingEvent(String contact, boolean status) {
    		// Not used here
    	}

    	// A new participant has joined the group chat
    	public void onParticipantJoined(String contact, String contactDisplayname) {
    		// Not used here
    	}
    	
    	// A participant has left voluntary the group chat
    	public void onParticipantLeft(String contact) {
    		// Not used here
    	}

    	// A participant is disconnected from the group chat
    	public void onParticipantDisconnected(String contact) {
    		// Not used here
    	}    	
    };

	/**
	 * Hide progress dialog
	 */
    public void hideProgressDialog() {
    	if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
			progressDialog = null;
		}
    }
}
