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
 * List group chats from the content provider 
 */
public class GroupChatList extends Activity implements JoynServiceListener {
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
        setTitle(getString(R.string.menu_group_chat_log));

        // Set list adapter
        listView = (ListView)findViewById(android.R.id.list);
        TextView emptyView = (TextView)findViewById(android.R.id.empty);
        listView.setEmptyView(emptyView);
        
        // Instanciate API
        chatApi = new ChatService(getApplicationContext(), this);
        
        // Connect API
        chatApi.connect();        
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Refresh view
		listView.setAdapter(createListAdapter());
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
	private GroupChatListAdapter createListAdapter() {
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
		return new GroupChatListAdapter(this, cursor);
	}
	
    /**
     * Group chat list adapter
     */
    private class GroupChatListAdapter extends CursorAdapter {
    	/**
    	 * Constructor
    	 * 
    	 * @param context Context
    	 * @param c Cursor
    	 */
		public GroupChatListAdapter(Context context, Cursor c) {
            super(context, c);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.chat_list_item, parent, false);
            view.setOnClickListener(clickItemListener);
            
    		GroupChatListItemCache cache = new GroupChatListItemCache();
    		cache.chatId = cursor.getString(1);
    		cache.subject = cursor.getString(2);
    		cache.state = cursor.getInt(3);
    		cache.date = cursor.getLong(4);
            view.setTag(cache);
            return view;
        }
        
    	@Override
    	public void bindView(View view, Context context, Cursor cursor) {
    		GroupChatListItemCache cache = (GroupChatListItemCache)view.getTag();

			// Set the date/time field by mixing relative and absolute times
    		TextView dateView = (TextView)view.findViewById(R.id.date);
    		dateView.setText(DateUtils.getRelativeTimeSpanString(cache.date,
    				System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
    				DateUtils.FORMAT_ABBREV_RELATIVE));
    		
			// Set the label
    		TextView line1View = (TextView)view.findViewById(R.id.line1); 
			line1View.setText(R.string.label_group_chat);
    		TextView subjectView = (TextView)view.findViewById(R.id.line2);
			if (TextUtils.isEmpty(cache.subject)) {
				subjectView.setText("<" + context.getString(R.string.label_no_subject) + ">");
			} else {
				subjectView.setText(cache.subject);
			}
			subjectView.setVisibility(View.VISIBLE);
    	}
    }

    /**
     * Group chat list item in cache
     */
	private class GroupChatListItemCache {
		private String chatId;
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
				Utils.showMessage(GroupChatList.this, getString(R.string.label_continue_chat_failed));
				return;
			}

			try {
				// Get selected item
				GroupChatListItemCache cache = (GroupChatListItemCache)v.getTag();

				// Get group chat
				GroupChat groupChat = chatApi.getGroupChat(cache.chatId);
				if (groupChat != null) {
					// Session already active on the device: just reload it in the UI
					Intent intent = new Intent(GroupChatList.this, GroupChatView.class);
		        	intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	            	intent.putExtra(GroupChatView.EXTRA_MODE, GroupChatView.MODE_OPEN);
		    		intent.putExtra(GroupChatView.EXTRA_CHAT_ID, groupChat.getChatId());
		    		startActivity(intent);				
				} else {
					// Rejoin or restart the session
					int state = cache.state;
					if (state == GroupChat.State.TERMINATED) {
						// The session was terminated: only a restart may be done
						restartChat = new RestartChat(GroupChatList.this, chatApi, cache.chatId);
						restartChat.start();
					} else {					
						// Session terminated on the device: try to rejoin the session
						rejoinChat = new RejoinChat(GroupChatList.this, chatApi, cache.chatId);
						rejoinChat.start();
					}
				}
		    } catch(JoynServiceNotAvailableException e) {
		    	e.printStackTrace();
				Utils.showMessageAndExit(GroupChatList.this, getString(R.string.label_api_disabled));
		    } catch(JoynServiceException e) {
		    	e.printStackTrace();
				Utils.showMessageAndExit(GroupChatList.this, getString(R.string.label_api_failed));
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