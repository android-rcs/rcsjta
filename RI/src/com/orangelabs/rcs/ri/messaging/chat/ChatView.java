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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.gsma.services.rcs.RcsCommon;
import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatMessage;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.chat.GeolocMessage;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.ContactUtils;
import com.orangelabs.rcs.ri.ApiConnectionManager;
import com.orangelabs.rcs.ri.ApiConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.messaging.geoloc.EditGeoloc;
import com.orangelabs.rcs.ri.messaging.geoloc.ShowUsInMap;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsDisplayName;
import com.orangelabs.rcs.ri.utils.SmileyParser;
import com.orangelabs.rcs.ri.utils.Smileys;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * CHAT view
 */
public abstract class ChatView extends ListActivity implements OnClickListener, OnKeyListener {	
	/**
	 * View modes
	 */
	/* package private */ final static int MODE_INCOMING = 0;
	/* package private */ final static int MODE_OUTGOING = 1;
	/* package private */ final static int MODE_OPEN = 2;
	
	/**
	 * Intent parameters
	 */
	/* package private */ final static String EXTRA_MODE = "mode";
	
	/**
	 * Activity result constant
	 */
	private final static int SELECT_GEOLOCATION = 0;

	/**
     * UI handler
     */
	protected Handler handler = new Handler();
    
    /**
     * Progress dialog
     */
	protected Dialog progressDialog;
    
	/**
	 * Message composer
	 */
    protected EditText composeText;
    
    /**
     * Message list adapter
     */
    protected MessageListAdapter msgListAdapter;
    
    /**
     * Utility class to manage the is-composing status
     */
    protected IsComposingManager composingManager;
    
    /**
	 * A locker to exit only once
	 */
	protected LockAccess exitOnce = new LockAccess();
	
	/**
	 * Activity displayed status
	 */
	private static boolean activityDisplayed = false;
	
	/**
	 * Key for chat : ContactId for one to one chat or ChatId for Group chat
	 */
	protected static Object keyChat;
	
	/**
	 * Smileys
	 */
    protected Smileys smileyResources;
    
	/**
	 * API connection manager
	 */
	protected ApiConnectionManager connectionManager;
    
	/**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(ChatView.class.getSimpleName());
	
	private final static String LOAD_HISTORY_WHERE_CLAUSE = new StringBuilder(ChatLog.Message.MIME_TYPE).append("='")
			.append(ChatLog.Message.MimeType.TEXT_MESSAGE).append("' OR ").append(ChatLog.Message.MIME_TYPE).append("='")
			.append(ChatLog.Message.MimeType.GEOLOC_MESSAGE).append("'").toString();

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.chat_view);
        
        // Set the message list adapter
        msgListAdapter = new MessageListAdapter(this);
        setListAdapter(msgListAdapter);
        
        // Smiley resources
		smileyResources = new Smileys(this);

        // Set message composer callbacks
        composeText = (EditText)findViewById(R.id.userText);
        composeText.setOnClickListener(this);
        composeText.setOnKeyListener(this);
        composeText.addTextChangedListener(mUserTextWatcher);
                
		// Set send button listener
        Button sendBtn = (Button)findViewById(R.id.send_button);
        sendBtn.setOnClickListener(this);
        
        // Register to API connection manager
		connectionManager = ApiConnectionManager.getInstance(this);
		
		if (connectionManager == null || !connectionManager.isServiceConnected(RcsServiceName.CHAT, RcsServiceName.CONTACTS)) {
			Utils.showMessageAndExit(this, getString(R.string.label_service_not_available), exitOnce);
		} else {
			connectionManager.startMonitorServices(this, exitOnce, RcsServiceName.CHAT, RcsServiceName.CONTACTS);
		}
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();
    	if (connectionManager != null) {
    		connectionManager.stopMonitorServices(this);
    	}
    }
    
    @Override
	protected void onResume() {
        super.onResume();
        activityDisplayed = true;
    }

    @Override
	protected void onPause() {
        super.onStart();
        activityDisplayed = false;
    }
	
    /**
     * Return true if the activity is currently displayed or not
     *   
     * @return Boolean
     */
    public static boolean isDisplayed() {
    	return activityDisplayed;
    }
    
    /**
     * Message composer listener
     * 
     * @param v View
     */
    public void onClick(View v) {
        sendText();
    }
    
    /**
     * Message composer listener
     * 
     * @param v View
     * @param keyCode Key code
     * @event Key event
     */
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    sendText();
                    return true;
            }
        }
        return false;
    }
    
	/**
	 * Hide progress dialog
	 */
    public void hideProgressDialog() {
    	if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
			progressDialog = null;
		}
    }        
    
    /**
     * Add a message from database in the message history
     * 
     */
	protected void addMessageHistory(int direction, ContactId contact, String content, String contentType, String msgId,
			String displayName) {
		if (ChatLog.Message.MimeType.GEOLOC_MESSAGE.equals(contentType)) {
			Geoloc geoloc = ChatLog.getGeoloc(content);
			if (geoloc != null) {
				addGeolocHistory(direction, contact, geoloc, msgId, displayName);
			} else {
				if (LogUtils.isActive) {
					Log.w(LOGTAG, "Invalid geoloc " + content);
				}
			}
		} else {
			addMessageHistory(direction, contact, content, msgId, displayName);
		}
	}
    
    /**
     * Add a text message in the message history
     * 
     */
    private void addMessageHistory(int direction, ContactId contact, String text, String msgId, String displayName) {
    	displayName = RcsDisplayName.convert(this, direction, contact, displayName);
		TextMessageItem item = new TextMessageItem(direction, displayName, text, msgId);
		this.addMessageHistory(item);
    }
    
    /**
     * Add a text message in the message history
     * 
     */
    private void addMessageHistory(TextMessageItem item) {
		msgListAdapter.add(item);
    }
    
    /**
     * Add a geoloc message in the message history
     * 
     */
	private void addGeolocHistory(int direction, ContactId contact, Geoloc geoloc, String msgId, String displayName) {
		String text = geoloc.getLabel() + "," + geoloc.getLatitude() + "," + geoloc.getLongitude();
		displayName = RcsDisplayName.convert(this, direction, contact, displayName);
		TextMessageItem item = new TextMessageItem(direction, displayName, text, msgId);
		this.addMessageHistory(item);
	}

    /**
     * Add a notif in the message history
     * 
     */
    protected void addNotifHistory(String notif, String messageId) {
		NotifMessageItem item = new NotifMessageItem(messageId, notif);
		msgListAdapter.add(item);
    }    
        
    /**
     * Send a text and display it
     */
    private void sendText() {
        String text = composeText.getText().toString();
        if ((text == null) || (text.length() == 0)) {
        	return;
        }
        
        // Check if the service is available
    	boolean registered = false;
    	try {
    		registered = ApiConnectionManager.getInstance(ChatView.this).getChatApi().isServiceRegistered();
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
        if (!registered) {
	    	Utils.showMessage(ChatView.this, getString(R.string.label_service_not_available));
	    	return;
        }

		// Send text message
		ChatMessage message = sendTextMessage(text);
		if (message != null) {
			// Add text to the message history
			TextMessageItem item = new TextMessageItem(RcsCommon.Direction.OUTGOING, getString(R.string.label_me), text, message.getId());
			addMessageHistory(item);
			composeText.setText(null);
		} else {
			Utils.showMessage(ChatView.this, getString(R.string.label_send_im_failed));
		}
    }

    /**
     * Send a geoloc and display it
     * 
     * @param geoloc Geoloc
     */
    private void sendGeoloc(Geoloc geoloc) {
        // Check if the service is available
    	boolean registered = false;
    	try {
    		registered = connectionManager.getChatApi().isServiceRegistered();
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
        if (!registered) {
	    	Utils.showMessage(ChatView.this, getString(R.string.label_service_not_available));
	    	return;
        }

        // Send text message
        GeolocMessage message = sendGeolocMessage(geoloc);
    	if (message != null) {
	    	// Add geoloc to the message history
    		// Add text to the message history
    		String text = geoloc.getLabel() + "," + geoloc.getLatitude() + "," + geoloc.getLongitude();
    		TextMessageItem item = new TextMessageItem(RcsCommon.Direction.OUTGOING, getString(R.string.label_me), text, message.getId());
    		addMessageHistory(item);
    	} else {
	    	Utils.showMessage(ChatView.this, getString(R.string.label_send_im_failed));
    	}
    }

    /**
	 * Add quick text
	 */
    protected void addQuickText() {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle(R.string.label_select_quicktext);
    	builder.setCancelable(true);
        builder.setItems(R.array.select_quicktext, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String[] items = getResources().getStringArray(R.array.select_quicktext);
        		composeText.append(items[which]);
            }
        });
        
        AlertDialog alert = builder.create();
    	alert.show();
    }
    
    /**********************************************************************
     ******************	Deals with isComposing feature ********************
     **********************************************************************/
    
    private final TextWatcher mUserTextWatcher = new TextWatcher(){
		@Override
		public void afterTextChanged(Editable s) {
			// Check if the text is not null.
			// we do not wish to consider putting the edit text back to null (like when sending message), is having activity 
			if (s.length()>0) {
				// Warn the composing manager that we have some activity
				if (composingManager != null) {
					composingManager.hasActivity();
				}
			}
		}
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
		
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {}
    };
    
    /**
	 * Utility class to handle is_typing timers (see RFC3994)  
	 */
	protected class IsComposingManager {
        // Idle time out (in ms)
        private int idleTimeOut = 0;

 		// Active state refresh interval (in ms)
		private final static int ACTIVE_STATE_REFRESH = 60*1000; 

		// Clock handler
		private ClockHandler handler = new ClockHandler();

		// Is composing state
		public boolean isComposing = false;

		// Event IDs
		private final static int IS_STARTING_COMPOSING = 1;
		private final static int IS_STILL_COMPOSING = 2;
		private final static int MESSAGE_WAS_SENT = 3;
		private final static int ACTIVE_MESSAGE_NEEDS_REFRESH = 4;
		private final static int IS_IDLE = 5;

        public IsComposingManager(int timeout) {
            idleTimeOut = timeout;
        }

		// Clock handler class
		private class ClockHandler extends Handler {
			public void handleMessage(Message msg){
				switch(msg.what){
					case IS_STARTING_COMPOSING :{
						// Send a typing status "active"
						ChatView.this.setTypingStatus(true);
	
						// In IDLE_TIME_OUT we will need to send a is-idle status message 
						handler.sendEmptyMessageDelayed(IS_IDLE, idleTimeOut);
	
						// In ACTIVE_STATE_REFRESH we will need to send an active status message refresh
						handler.sendEmptyMessageDelayed(ACTIVE_MESSAGE_NEEDS_REFRESH, ACTIVE_STATE_REFRESH);
						break;
					}    			
					case IS_STILL_COMPOSING :{
						// Cancel the IS_IDLE messages in queue, if there was one
						handler.removeMessages(IS_IDLE);
	
						// In IDLE_TIME_OUT we will need to send a is-idle status message
						handler.sendEmptyMessageDelayed(IS_IDLE, idleTimeOut);
						break;
					}
					case MESSAGE_WAS_SENT :{
						// We are now going to idle state
						composingManager.hasNoActivity();
	
						// Cancel the IS_IDLE messages in queue, if there was one
						handler.removeMessages(IS_IDLE);
	
						// Cancel the ACTIVE_MESSAGE_NEEDS_REFRESH messages in queue, if there was one
						handler.removeMessages(ACTIVE_MESSAGE_NEEDS_REFRESH);
						break;
					}	    			
					case ACTIVE_MESSAGE_NEEDS_REFRESH :{
						// We have to refresh the "active" state
						ChatView.this.setTypingStatus(true);
	
						// In ACTIVE_STATE_REFRESH we will need to send an active status message refresh
						handler.sendEmptyMessageDelayed(ACTIVE_MESSAGE_NEEDS_REFRESH, ACTIVE_STATE_REFRESH);
						break;
					}
					case IS_IDLE :{
						// End of typing
						composingManager.hasNoActivity();
	
						// Send a typing status "idle"
						ChatView.this.setTypingStatus(false);
	
						// Cancel the ACTIVE_MESSAGE_NEEDS_REFRESH messages in queue, if there was one
						handler.removeMessages(ACTIVE_MESSAGE_NEEDS_REFRESH);
						break;
					}
				}
			}
		}

		/**
		 * Edit text has activity
		 */
		public void hasActivity() {
			// We have activity on the edit text
			if (!isComposing){
				// If we were not already in isComposing state
				handler.sendEmptyMessage(IS_STARTING_COMPOSING);
				isComposing = true;
			} else {
				// We already were composing
				handler.sendEmptyMessage(IS_STILL_COMPOSING);
			}
		}

		/**
		 * Edit text has no activity anymore
		 */
		public void hasNoActivity(){
			isComposing = false;
		}

		/**
		 * The message was sent
		 */
		public void messageWasSent(){
			handler.sendEmptyMessage(MESSAGE_WAS_SENT);
		}
	}
	
	/**
	 * Format text with smiley
	 * 
	 * @param txt Text
	 * @return String
	 */
	private CharSequence formatMessageWithSmiley(String txt) {
		SpannableStringBuilder buf = new SpannableStringBuilder();
		if (!TextUtils.isEmpty(txt)) {
			SmileyParser smileyParser = new SmileyParser(txt, smileyResources);
			smileyParser.parse();
			buf.append(smileyParser.getSpannableString(this));
		}
		return buf;
	}	    

	/**
	 * Message item
	 */
	protected abstract class MessageItem {
		private int direction;
	    private String contact;
	    private String messageId;

	    public MessageItem(int direction, String contact, String messageId) {
	    	this.direction = direction;
    		this.contact = contact;
    		this.messageId = messageId;
	    }
	    
	    public int getDirection() {
	    	return direction;
	    }
	    
	    public String getContact() {
	    	return contact;
	    }

		public String getMessageId() {
			return messageId;
		}

	}	
	
	/**
	 * Text message item
	 */
	protected class TextMessageItem extends MessageItem {
	    private String text;
	    
	    public TextMessageItem(int direction, String contact, String text, String messageId) {
	    	super(direction, contact, messageId);
	    	this.text = text;
	    }
	    
		/**
		 * Constructor
		 * 
		 * @param dao
		 *            the message Data Object (either text or geolocation message)
		 * @param displayName
		 *            the RCS display name or null
		 */
		public TextMessageItem(ChatMessageDAO dao, String displayName) {
			super(dao.getDirection(), TextUtils.isEmpty(displayName) ? dao.getContact().toString() : displayName, dao.getMsgId());
			if (ChatLog.Message.MimeType.GEOLOC_MESSAGE.equals(dao.getMimeType())) {
				Geoloc geoloc = ChatLog.getGeoloc(dao.getBody());
				if (geoloc == null) {
					throw new IllegalArgumentException("Cannot decode geolocation");
				} else {
					this.text = geoloc.getLabel() + "," + geoloc.getLatitude() + "," + geoloc.getLongitude();
				}
			} else {
				if (ChatLog.Message.MimeType.TEXT_MESSAGE.equals(dao.getMimeType())) {
					this.text = dao.getBody();
				} else {
					throw new IllegalArgumentException("Invalid mime-type "+dao.getMimeType());
				}
			}
		}
	    
	    public String getText() {
	    	return text;
	    }
	}	

	/**
	 * Notif message item
	 */
	private class NotifMessageItem extends MessageItem {
	    private String text;
	    
	    public NotifMessageItem(String msgId, String text) {
	    	super(RcsCommon.Direction.IRRELEVANT, null, msgId);
	    	this.text = text;
	    }
	    
	    public String getText() {
	    	return text;
	    }
	}	

	/**
	 * Message list adapter
	 */
	public class MessageListAdapter extends ArrayAdapter<MessageItem> {
	    private Context context; 

	    public MessageListAdapter(Context context) {
	        super(context, R.layout.chat_view_item);
	        
	        this.context = context;
	    }
	    
	    @Override
	    public View getView(int position, View convertView, ViewGroup parent) {
	        View row = convertView;
	        MessageItemHolder holder = null;
	        if (row == null) {
	            LayoutInflater inflater = LayoutInflater.from(context);
	            row = inflater.inflate(R.layout.chat_view_item, parent, false);
	            holder = new MessageItemHolder();
	            holder.text = (TextView)row.findViewById(R.id.item_text);
	            row.setTag(holder);
	        } else {
	            holder = (MessageItemHolder)row.getTag();
	        }
	        
        	MessageItem item = (MessageItem)getItem(position);
        	String line;
        	if (item.getDirection() == RcsCommon.Direction.OUTGOING) {
        		line = "[" + getString(R.string.label_me) + "] ";
        	} else {
        		line = "[" + item.getContact() + "] ";
        	}
        	if (item instanceof NotifMessageItem) {
        		NotifMessageItem notifItem = (NotifMessageItem)item;
				holder.text.setText(notifItem.getText());
        	} else {
        		TextMessageItem txtItem = (TextMessageItem)item;
				String txt = txtItem.getText();
				line += formatMessageWithSmiley(txt);
				
				holder.text.setText(line);
        	}

	        return row;
	    }
	    
	    private class MessageItemHolder {
	        TextView text;
	    }
	}
	
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
            	// Quit the session
            	quitSession();
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }
    
    /**
     * Send text message
     * 
     * @param msg Message
     * @return Chat message
     */
    protected abstract ChatMessage sendTextMessage(String msg);
    
    /**
     * Send geoloc message
     * 
     * @param geoloc Geoloc
     * @return geoloc message
     */
    protected abstract GeolocMessage sendGeolocMessage(Geoloc geoloc);

    /**
     * Quit the session
     */
    protected abstract void quitSession();
    
    /**
     * Update the is composing status
     * 
     * @param isTyping Is composing status
     */
    protected abstract void setTypingStatus(boolean isTyping);
    
    /**
     * Get a geoloc
     */
    protected void getGeoLoc() {
		// Start a new activity to send a geolocation
    	startActivityForResult(new Intent(this, EditGeoloc.class), SELECT_GEOLOCATION);
    }
        
    /**
     * On activity result
     * 
     * @param requestCode Request code
     * @param resultCode Result code
     * @param data Data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (resultCode != RESULT_OK) {
    		return;
    	}

    	switch(requestCode) {
	    	case SELECT_GEOLOCATION: {
				// Get selected geoloc
				Geoloc geoloc = data.getParcelableExtra(EditGeoloc.EXTRA_GEOLOC);

				// Send geoloc
				sendGeoloc(geoloc);
	    	}             
	    	break;
    	}
    }

    /**
     * Show us in a map
     * 
     * @param participant A participant
     */
    protected void showUsInMap(ContactId participant) {
    	Intent intent = new Intent(this, ShowUsInMap.class);
    	ArrayList<String> list = new ArrayList<String>();
    	list.add(participant.toString());
    	intent.putStringArrayListExtra(ShowUsInMap.EXTRA_CONTACTS, list);
    	startActivity(intent);
    }    

    /**
     * Show us in a map
     * 
     * @param participants List of participants
     */
    protected void showUsInMap(Set<String> participants) {
    	Intent intent = new Intent(this, ShowUsInMap.class);
    	ArrayList<String> list = new ArrayList<String>(participants);
    	intent.putStringArrayListExtra(ShowUsInMap.EXTRA_CONTACTS, list);
    	startActivity(intent);
    }
    
	/**
	 * Load history
	 * 
	 * @param key
	 *            the contactId for OneToOneChat or the chatId for GroupChat
	 * @return the set of unread chat message identifiers
	 */
	protected Set<String> loadHistory(String key) {
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "loadHistory key" + key);
		}
		msgListAdapter.clear();
		Set<String> unReadMessageIDs = new HashSet<String>();
		Map<ContactId,String> participants = new HashMap<ContactId,String>();
		Uri uri = Uri.withAppendedPath(ChatLog.Message.CONTENT_CHAT_URI, key);
		Cursor cursor = null;
		ContactUtils contactUtils = ContactUtils.getInstance(this);
		try {
			// @formatter:off
			String[] projection = new String[] {
	    				ChatLog.Message.DIRECTION,
	    				ChatLog.Message.CONTACT,
	    				ChatLog.Message.CONTENT,
	    				ChatLog.Message.MIME_TYPE,
	    				ChatLog.Message.MESSAGE_ID,
	    				ChatLog.Message.READ_STATUS };
			// @formatter:on
			cursor = getContentResolver().query(uri, projection, LOAD_HISTORY_WHERE_CLAUSE, null,
					ChatLog.Message.TIMESTAMP + " ASC");
			while (cursor.moveToNext()) {
				int direction = cursor.getInt(0);
				String _contact = cursor.getString(1);
				String content = cursor.getString(2);
				String contentType = cursor.getString(3);
				String msgId = cursor.getString(4);
				ContactId contact = null;
				if (_contact != null) {
					try {
						contact = contactUtils.formatContact(_contact);
						// Do not fill map if record already exists
						if (!participants.containsKey(contact)) {
							participants.put(contact, RcsDisplayName.get(this, contact));
						}
						
						String displayName = participants.get(contact);
						displayName = RcsDisplayName.convert(ChatView.this, direction, contact, displayName);
						if (ChatLog.Message.MimeType.GEOLOC_MESSAGE.equals(contentType)) {
							Geoloc geoloc = ChatLog.getGeoloc(content);
							if (geoloc != null) {
								addGeolocHistory(direction, contact, geoloc, msgId, displayName);
							} else  {
								if (LogUtils.isActive) {
									Log.w(LOGTAG, "Invalid geoloc " + content);
								}	
							}
						} else {
							addMessageHistory(direction, contact, content, msgId, displayName);
						}
						boolean unread = cursor.getString(5).equals(Integer.toString(RcsCommon.ReadStatus.UNREAD));
						if (unread) {
							unReadMessageIDs.add(msgId);
						}
					} catch (RcsContactFormatException e) {
						if (LogUtils.isActive) {
							Log.e(LOGTAG, "Bad contact in history " + _contact, e);
						}
					}
				}
			}
		} catch (Exception e) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Exception loadHistory", e);
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return unReadMessageIDs;
	}
	
}