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

import java.util.Vector;

import org.gsma.joyn.JoynService;
import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.JoynServiceListener;
import org.gsma.joyn.JoynServiceNotAvailableException;
import org.gsma.joyn.chat.Chat;
import org.gsma.joyn.chat.ChatLog;
import org.gsma.joyn.chat.ChatService;
import org.gsma.joyn.chat.GroupChat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * List of current chat sessions and blocked contacts
 */
public class ChatList extends Activity implements JoynServiceListener {
	/**
	 * Chat API
	 */
    private ChatService chatApi;
    
	/**
	 * Rejoin chat manager
	 */
	private RejoinChat rejoinChat = null;

	/**
	 * Restart chat manager
	 */
	private RestartChat restartChat = null;
	
	/**
	 * API connection state
	 */
	private boolean apiEnabled = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.chat_list);

		// Set UI title
        setTitle(getString(R.string.menu_chat_log));

        // Set list adapter
        ListView view = (ListView)findViewById(android.R.id.list);
        TextView emptyView = (TextView)findViewById(android.R.id.empty);
        view.setEmptyView(emptyView);
        view.setAdapter(createChatListAdapter());
        
        // Instanciate API
        chatApi = new ChatService(getApplicationContext(), this);
        
        // Connect API
        chatApi.connect();        
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if (rejoinChat != null) {
			rejoinChat.stop();
		}
		
		if (restartChat != null) {
			restartChat.stop();
		}

        // Disconnect API
        chatApi.disconnect();
	}
		
	/**
	 * Create chat list adapter with unique chat ID entries
	 */
	private ChatListAdapter createChatListAdapter() {
		Uri uri = ChatLog.CONTENT_URI;
	    String[] PROJECTION = new String[] {
	    		ChatLog.Chat.CHAT_ID,
	    		ChatLog.Chat.IS_GROUP_CHAT,
	    		ChatLog.Chat.REMOTE,
	    		ChatLog.Chat.SUBJECT,
	    		ChatLog.Chat.STATE,
	    		ChatLog.Chat.TIMESTAMP
	    };
        String sortOrder = ChatLog.Chat.TIMESTAMP + " DESC ";
		Cursor cursor = getContentResolver().query(uri, PROJECTION, null, null, sortOrder);

		Vector<String> items = new Vector<String>();
		MatrixCursor matrix = new MatrixCursor(PROJECTION);
		while (cursor.moveToNext()){
    		String chatId = cursor.getString(1);
			if (!items.contains(chatId)) {
				matrix.addRow(new Object[]{
						cursor.getString(0), 
						cursor.getInt(1), 
						cursor.getString(2),
						cursor.getString(3),
						cursor.getInt(4)});
				items.add(chatId);
			}
		}
		cursor.close();

		return new ChatListAdapter(this, matrix);
	}
	
    /**
     * Chat list adapter
     */
    private class ChatListAdapter extends CursorAdapter {
    	/**
    	 * Constructor
    	 * 
    	 * @param context Context
    	 * @param c Cursor
    	 */
		public ChatListAdapter(Context context, Cursor c) {
            super(context, c);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.chat_list_item, parent, false);
            view.setOnClickListener(clickItemListener);
            
    		ChatListItemCache cache = new ChatListItemCache();
    		cache.chatId = cursor.getString(0);
    		cache.isGroup = (cursor.getInt(1) == ChatLog.Chat.Type.GROUP_CHAT);
    		cache.contact = cursor.getString(2);
    		cache.subject = cursor.getString(3);
    		cache.state = cursor.getInt(4);
    		cache.date = cursor.getLong(5);
            view.setTag(cache);
            return view;
        }
        
    	@Override
    	public void bindView(View view, Context context, Cursor cursor) {
    		ChatListItemCache cache = (ChatListItemCache)view.getTag();

			// Set the date/time field by mixing relative and absolute times
    		TextView dateView = (TextView)view.findViewById(R.id.date);
    		dateView.setText(DateUtils.getRelativeTimeSpanString(cache.date,
    				System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
    				DateUtils.FORMAT_ABBREV_RELATIVE));
    		
			// Set the label
    		TextView line1View = (TextView)view.findViewById(R.id.line1); 
    		TextView numberView = (TextView)view.findViewById(R.id.number);
    		if (cache.isGroup) {
    			line1View.setText(R.string.label_group_chat);
        		numberView.setText(cache.subject);
        		numberView.setVisibility(View.VISIBLE);
    		} else {
    			line1View.setText(R.string.label_chat);
        		numberView.setText(cache.contact);
        		numberView.setVisibility(View.VISIBLE);
    		}
    	}
    }

    /**
     * Chat list item in cache
     */
	private class ChatListItemCache {
		private String chatId;
		private boolean isGroup;
		private String contact;
		private String subject;
		private int state;
		private long date;
	}    
    
    /**
     * Callback called when service is connected. This method is called when the
     * service is well connected to the RCS service (binding procedure successfull):
     * this means the methods of the API may be used.
     */
    public void onServiceConnected() {
        Button inviteBtn = (Button)findViewById(R.id.invite_btn);
        inviteBtn.setEnabled(true);

        // Disable button if no contact available
        Spinner spinner = (Spinner)findViewById(R.id.contact);
        Button selectBtn = (Button)findViewById(R.id.select_btn);
        if (spinner.getAdapter().getCount() != 0) {
        	selectBtn.setEnabled(true);
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
		apiEnabled = false;
    }    
    
    /**
     * Callback called when service is registered to the RCS/IMS platform
     */
    public void onServiceRegistered() {
		apiEnabled = true;
    }
    
    /**
     * Callback called when service is unregistered from the RCS/IMS platform
     */
    public void onServiceUnregistered() {
		apiEnabled = false;
    }      
    
    /**
     * Onclick list listener
     */
    private OnClickListener clickItemListener = new OnClickListener() {
		public void onClick(View v) {
			if (!apiEnabled) {
				Utils.showMessage(ChatList.this, getString(R.string.label_continue_chat_failed));
				return;
			}

			try {
				// Get selected item
				ChatListItemCache cache = (ChatListItemCache)v.getTag();
				if (cache.isGroup) {
					// Group chat
					GroupChat groupChat = null;
					try {
						groupChat = chatApi.getGroupChat(cache.chatId);
					} catch(JoynServiceException e) {
						// TODO
					}
					
					if (groupChat != null) {
						// Session already active on the device: just reload it in the UI
						Intent intent = new Intent(ChatList.this, GroupChatView.class);
			        	intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		            	intent.putExtra("subject", groupChat.getSubject());
			    		intent.putExtra("chatId", groupChat.getChatId());
			    		startActivity(intent);				
					} else {
						// Test if the session may be rejoined or not
						int state = cache.state;
						if (state == GroupChat.State.TERMINATED_BY_USER) {
							// The session was terminated by user itself: rejoin or restart are not authorized
							Utils.showMessage(ChatList.this, getString(R.string.label_rejoin_unauthorized));
						} else
						if (state == GroupChat.State.TERMINATED) {
							// The session was terminated: only a restart may be done
							restartChat = new RestartChat(ChatList.this, chatApi, cache.chatId);
							restartChat.start();
						} else {					
							// Session terminated on the device: try to rejoin the session
							rejoinChat = new RejoinChat(ChatList.this, chatApi, cache.chatId);
							rejoinChat.start();
						}
					}
				} else {
					// 1-1 chat
					Chat chat = null;
					try {
						chat = chatApi.getChat(cache.chatId);
					} catch(JoynServiceException e) {
						// TODO
					}
					
					if (chat != null) {
						// Session already active on the device: just reload it in the UI
			    		Intent intent = new Intent(ChatList.this, SingleChatView.class);
			        	intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		            	intent.putExtra("contact", chat.getRemoteContact());
			    		intent.putExtra("chatId", chat.getChatId());
			    		startActivity(intent);
					} else {
						// Session terminated on the device: create a new one on the first message
			    		Intent intent = new Intent(ChatList.this, SingleChatView.class);
			        	intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		            	intent.putExtra("contact", cache.contact);
			    		startActivity(intent);
					}
				}
		    } catch(JoynServiceNotAvailableException e) {
		    	e.printStackTrace();
				Utils.showMessageAndExit(ChatList.this, getString(R.string.label_api_disabled));
		    } catch(JoynServiceException e) {
		    	e.printStackTrace();
				Utils.showMessageAndExit(ChatList.this, getString(R.string.label_api_failed));
			}
		}
    };
}