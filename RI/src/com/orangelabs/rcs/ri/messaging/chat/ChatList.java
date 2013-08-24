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
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * List of current chat sessions and blocked contacts
 */
public class ChatList extends Activity implements JoynServiceListener {
	/**
	 * List view
	 */
    private ListView listView;

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
        listView = (ListView)findViewById(android.R.id.list);
        TextView emptyView = (TextView)findViewById(android.R.id.empty);
        listView.setEmptyView(emptyView);
        listView.setAdapter(createListAdapter());
        
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
	private ChatListAdapter createListAdapter() {
		// TODO: add single chat also
		Uri uri = ChatLog.GroupChat.CONTENT_URI;
	    String[] PROJECTION = new String[] {
	    		ChatLog.GroupChat.ID,
	    		ChatLog.GroupChat.CHAT_ID,
	    		ChatLog.GroupChat.SUBJECT,
	    		ChatLog.GroupChat.STATE,
	    		ChatLog.GroupChat.TIMESTAMP
	    };
        String sortOrder = ChatLog.GroupChat.TIMESTAMP + " DESC";
		Cursor cursor = getContentResolver().query(uri, PROJECTION, null, null, sortOrder);
		if (cursor == null) {
			Utils.showMessageAndExit(this, getString(R.string.label_load_log_failed));
			return null;
		}
		return new ChatListAdapter(this, cursor);
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
    		cache.chatId = cursor.getString(1);
    		cache.isGroup = true; //TODO: single chat
    		cache.contact = null; //TODO: single chat
    		cache.subject = cursor.getString(2);
    		cache.state = cursor.getInt(3);
    		cache.date = cursor.getLong(4);
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
    			if (TextUtils.isEmpty(cache.subject)) {
    				numberView.setText("<" + context.getString(R.string.label_no_subject) + ">");
    			} else {
        			numberView.setText(cache.subject);
    			}
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
		apiEnabled = true;
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
					GroupChat groupChat = chatApi.getGroupChat(cache.chatId);
					if (groupChat != null) {
						// Session already active on the device: just reload it in the UI
						Intent intent = new Intent(ChatList.this, GroupChatView.class);
			        	intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		            	intent.putExtra(GroupChatView.EXTRA_MODE, GroupChatView.MODE_OPEN);
			    		intent.putExtra(GroupChatView.EXTRA_CHAT_ID, groupChat.getChatId());
			    		startActivity(intent);				
					} else {
						// Rejoin or restart the session
						int state = cache.state;
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
					Chat chat = chatApi.getChat(cache.chatId);					
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

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater=new MenuInflater(getApplicationContext());
		inflater.inflate(R.menu.menu_log, menu);

		return true;
	}
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_clear_log:
				// Delete all
				getContentResolver().delete(ChatLog.GroupChat.CONTENT_URI, null, null);
				
				// Refresh view
		        listView.setAdapter(createListAdapter());
				break;
		}
		return true;
	}
}