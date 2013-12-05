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
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
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

import com.gsma.services.rcs.JoynServiceListener;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatService;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * List chats from the content provider 
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
		
        // Disconnect API
        chatApi.disconnect();
	}
		
	/**
	 * Create chat list adapter with unique contact entries
	 */
	private ChatListAdapter createListAdapter() {
		Uri uri = ChatLog.Message.CONTENT_URI;
	    String[] PROJECTION = new String[] {
	    		ChatLog.Message.ID,
	    		ChatLog.Message.CHAT_ID,
	    		ChatLog.Message.CONTACT_NUMBER,
	    		ChatLog.Message.BODY,
	    		ChatLog.Message.TIMESTAMP
	    };
        String sortOrder = ChatLog.Message.TIMESTAMP + " DESC";
        String selection = ChatLog.Message.CHAT_ID + " = " + ChatLog.Message.CONTACT_NUMBER + ") GROUP BY (" + ChatLog.Message.CONTACT_NUMBER;        
		Cursor cursor = getContentResolver().query(uri, PROJECTION, selection, null, sortOrder);
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
    		cache.contact = cursor.getString(2);
    		cache.msg = cursor.getString(3);
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
			line1View.setText(getString(R.string.label_chat) + " " + cache.contact);
    		TextView msgView = (TextView)view.findViewById(R.id.line2);
    		msgView.setText(cache.msg);
    		msgView.setVisibility(View.VISIBLE);
    	}
    }

    /**
     * Chat list item in cache
     */
	private class ChatListItemCache {
		private String contact;
		private String msg;
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
     * Onclick list listener
     */
    private OnClickListener clickItemListener = new OnClickListener() {
		public void onClick(View v) {
			// TODO: if not connected offers possibility to show history
			if (!apiEnabled) {
				Utils.showMessage(ChatList.this, getString(R.string.label_continue_chat_failed));
				return;
			}

			// Get selected item
			ChatListItemCache cache = (ChatListItemCache)v.getTag();

			// Open chat
    		Intent intent = new Intent(ChatList.this, SingleChatView.class);
        	intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        	intent.putExtra(SingleChatView.EXTRA_MODE, SingleChatView.MODE_OPEN);
        	intent.putExtra(SingleChatView.EXTRA_CONTACT, cache.contact);
    		startActivity(intent);
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
		        String where = ChatLog.Message.CHAT_ID + " = " + ChatLog.Message.CONTACT_NUMBER;        
				getContentResolver().delete(ChatLog.Message.CONTENT_URI, where, null);
				
				// Refresh view
		        listView.setAdapter(createListAdapter());
				break;
		}
		return true;
	}
}