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

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.util.Log;
import android.widget.Toast;

import com.gsma.services.rcs.JoynServiceException;
import com.gsma.services.rcs.chat.ChatService;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.GroupChatListener;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Rejoin a group chat session
 * 
 * TODO to remove since rejoin will be handled internally by the stack
 */
public class RejoinChat {
    /**
     * UI handler
     */
    //private Handler handler = new Handler();

    /**
     * Progress dialog
     */
    private Dialog progressDialog;

	/**
     * Activity
     */
    private Activity activity;
    
    /**
	 * Chat API
	 */
    private ChatService chatApi;
    
    /**
	 * Chat ID to rejoin
	 */
	private String chatId;

	/**
	 * Restarted chat
	 */
	private GroupChat groupChat; 

    /**
     * Group chat listener
     */
    private MyGroupChatListener chatListener = new MyGroupChatListener();	    

	/**
	 * Restart chat manager
	 */
	private RestartChat restartChat;
	
	/**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(RejoinChat.class.getSimpleName());

	/**
     * Constructor
     * 
     * @param context Context
     * @param chatApi Chat API
     * @param chatId Chat ID
     */
	public RejoinChat(Activity activity, ChatService chatApi, String chatId) {
		this.activity = activity;
		this.chatApi = chatApi;
		this.chatId = chatId;
		try {
			this.chatApi.addGroupChatEventListener(chatListener);
		} catch (JoynServiceException e) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Failed to add GC listener", e);
			}
			// Show error
			Utils.showMessage(activity, activity.getString(R.string.label_rejoin_chat_exception));	
		}
	}
    
    /**
     * Start rejoin session
     */
    public synchronized void start() {
    	if (groupChat != null) {
    		return;
    	}
    	
    	// Initiate the session in background
    	try {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "RejoinChat chatId=" + chatId);
			}
    		groupChat = chatApi.rejoinGroupChat(chatId);
    	} catch(Exception e) {
    		e.printStackTrace();
    		
			// Hide progress dialog
			hideProgressDialog();

			// Show error
			Utils.showMessage(activity, activity.getString(R.string.label_rejoin_chat_exception));		
    	}
        
        // Display a progress dialog
        progressDialog = Utils.showProgressDialog(activity, activity.getString(R.string.label_command_in_progress));
        progressDialog.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				// Stop session
				stop();

				// Hide progress dialog
				hideProgressDialog();

				// Display feedback info
				Toast.makeText(activity,activity.getString(R.string.label_chat_rejoin_canceled), Toast.LENGTH_SHORT).show();
			}
		});
    }    

    /**
     * Stop rejoin session
     */
    public synchronized void stop() {
    	try {
			chatApi.removeGroupChatEventListener(chatListener);
		} catch (JoynServiceException e1) {
			e1.printStackTrace();
		}
    	if (restartChat != null) {
    		restartChat.stop();
    	}
    	
    	if (groupChat == null) {
    		return;
    	}

    	// Stop session
    	try {
            if (groupChat != null) {
            	groupChat.quitConversation();
            }
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	groupChat = null;
    }
    
    /**
     * Group chat event listener
     */
    private class MyGroupChatListener extends GroupChatListener {
//    	// Session started
//    	public void onSessionStarted() {
//			handler.post(new Runnable() { 
//				public void run() {
//					if (LogUtils.isActive) {
//						Log.d(LOGTAG, "onSessionStarted chatId=" + chatId);
//					}
//					try {
//	                    // Hide progress dialog
//						hideProgressDialog();
//	
//						// Remove listener now
//                		groupChat.removeEventListener(chatListener);
//
//						// Display chat view
//                		Intent intent = new Intent(activity, GroupChatView.class);
//			        	intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//		            	intent.putExtra(GroupChatView.EXTRA_MODE, GroupChatView.MODE_OPEN);
//			    		intent.putExtra(GroupChatView.EXTRA_CHAT_ID, groupChat.getChatId());
//			    		activity.startActivity(intent);
//				    } catch(JoynServiceNotAvailableException e) {
//				    	e.printStackTrace();
//						Utils.showMessageAndExit(activity, activity.getString(R.string.label_api_disabled));
//				    } catch(JoynServiceException e) {
//				    	e.printStackTrace();
//						Utils.showMessageAndExit(activity, activity.getString(R.string.label_api_failed));
//					}
//				}
//			});
//    	}
//    	
//    	// Session aborted
//    	public void onSessionAborted() {
//    		if (LogUtils.isActive) {
//				Log.d(LOGTAG, "onSessionAborted chatId=" + chatId);
//			}
//			handler.post(new Runnable() { 
//				public void run() {
//					// Hide progress dialog
//					hideProgressDialog();
//					
//					// Session aborted
//					Utils.showMessageAndExit(activity, activity.getString(R.string.label_chat_aborted));
//				}
//			});
//    	}    	
//    	
//    	// Session error
//    	public void onSessionError(final int error) {
//    		if (LogUtils.isActive) {
//				Log.w(LOGTAG, "onSessionError chatId=" + chatId+" error="+error);
//			}
//			handler.post(new Runnable() { 
//				public void run() {
//					// Hide progress dialog
//					hideProgressDialog();
//
//					if (error == GroupChat.Error.CHAT_NOT_FOUND) {
//						// Propose to restart the session
//						restartChat = new RestartChat(activity, chatApi, chatId);
//						restartChat.start();
//					} else {
//						// Display error
//						Utils.showMessage(activity, activity.getString(R.string.label_rejoin_chat_failed, error));
//					}
//				}
//			});
//    	}
//    	
//    	// New message has been received
//    	public void onNewMessage(ChatMessage message) {
//    		// Not used here
//    	}
//    	
//    	// New geoloc has been received
//    	public void onNewGeoloc(GeolocMessage message) {
//    		// Not used here
//    	}    	
//    	
//    	// Message has been delivered to the remote
//    	public void onReportMessageDelivered(String msgId) {
//    		// Not used here
//    	}
//
//    	// Message has been displayed by the remote
//    	public void onReportMessageDisplayed(String msgId) {
//    		// Not used here
//    	}
//
//    	// Message has failed to be delivered to the remote
//    	public void onReportMessageFailed(String msgId) {
//    		// Not used here
//    	}
//    	
//    	// Is-composing event has been received
//    	public void onComposingEvent(ContactId contact, boolean status) {
//    		// Not used here
//    	}
//
//    	// A new participant has joined the group chat
//    	public void onParticipantJoined(ContactId contact, String contactDisplayname) {
//    		// Not used here
//    	}
//    	
//    	// A participant has left voluntary the group chat
//    	public void onParticipantLeft(ContactId contact) {
//    		// Not used here
//    	}
//
//    	// A participant is disconnected from the group chat
//    	public void onParticipantDisconnected(ContactId contact) {
//    		// Not used here
//    	}
//
//    	// The status of a GC participant has changed
//		@Override
//		public void onParticipantStatusChanged(ParticipantInfo arg0) {
//			// Not used here
//		}

		@Override
		public void onComposingEvent(String arg0, ContactId arg1, boolean arg2) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onDeliveryInfoStatusChanged(String arg0, ContactId arg1, String arg2, int arg3, int arg4) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onGroupChatStateChanged(String arg0, int arg1) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onMessageStatusChanged(String arg0, String arg1, int arg2) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onParticipantInfoStatusChanged(String arg0, ParticipantInfo arg1) {
			// TODO Auto-generated method stub
			
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
