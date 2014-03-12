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
package com.orangelabs.rcs.popup;

import java.io.InputStream;
import java.util.Set;
import java.util.Vector;

import android.app.ListActivity;
import android.content.ContentUris;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.Spinner;
import android.widget.TextView;

import com.gsma.services.rcs.JoynServiceListener;
import com.gsma.services.rcs.capability.Capabilities;
import com.gsma.services.rcs.capability.CapabilitiesListener;
import com.gsma.services.rcs.capability.CapabilityService;
import com.gsma.services.rcs.contacts.ContactsService;
import com.gsma.services.rcs.contacts.JoynContact;
import com.gsma.services.rcs.session.MultimediaSessionService;
import com.orangelabs.rcs.popup.utils.Utils;

/**
 * Send popup
 * 
 * @author Jean-Marc AUFFRET
 */
public class SendPopup extends ListActivity implements OnItemClickListener {
	private static final String TAG = "Popup";

	/**
	 * Contacts API
	 */
	private ContactsService contactsApi;

	/**
	 * Capability API
	 */
	private CapabilityService capabilityApi = null;

    /**
     * Capabilities listener
     */
    private MyCapabilitiesListener capabilitiesListener = new MyCapabilitiesListener(); 
    
    /**
	 * List of items
	 */
	private Vector<ListItem> items = new Vector<ListItem>();
	
	/**
	 * List adapter
	 */
	private MultipleListAdapter adapter;
	
	/**
	 * Session API
	 */
	private MultimediaSessionService sessionApi;
	
    /**
     * Send button
     */
	private Button sendBtn;
    
    /**
     * Selected contacts
     */
    private Vector<ContactItem> selectedContacts = new Vector<ContactItem>();

    /**
	 * UI handler
	 */
	private final Handler handler = new Handler();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        // Set title
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Set layout
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.send_popup);

		// Hide keybord by default
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		
		// Set list adapter
		adapter = new MultipleListAdapter();
		setListAdapter(adapter);
		getListView().setOnItemClickListener(this);
		getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		
		// Set animation selector
		Spinner spinnerAnim = (Spinner)findViewById(R.id.animation);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.animations, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerAnim.setAdapter(adapter);
		
		// Set buttons callback
		sendBtn = (Button)findViewById(R.id.send);
		sendBtn.setOnClickListener(btnSendListener);
		sendBtn.setEnabled(false);
		
		// Init APIs
		contactsApi = new ContactsService(getApplicationContext(), new MyContactsApiListener());
		contactsApi.connect();
		capabilityApi = new CapabilityService(getApplicationContext(), new MyCapabilityApiListener());
		capabilityApi.connect();
		sessionApi = new MultimediaSessionService(getApplicationContext(), new MySessionApiListener());
		sessionApi.connect();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();

		// Disconnect APIs
		contactsApi.disconnect();
		sessionApi.disconnect();
        capabilityApi.disconnect();    	
	}
	
    /**
     * Capability API listener
     */
    private class MyCapabilityApiListener implements JoynServiceListener {
        // Service connected
        public void onServiceConnected() {
        	try {
	        	// Register a capability listener
	    		capabilityApi.addCapabilitiesListener(capabilitiesListener);

	        	// Refresh capabilities in background
	        	if (capabilityApi.isServiceRegistered()) { 
	    			RefreshCapabilitiesAsyncTask task = new RefreshCapabilitiesAsyncTask();
	    			task.execute((Void[])null);
	    		}
        	} catch(Exception e) {
    			Log.e(TAG, "Can't resfresh capabilities", e);
        	}
    	}
    
        // Service disconnected
        public void onServiceDisconnected(int error) {
        }  
    }
    
    /**
     * Contacts API listener
     */
    private class MyContactsApiListener implements JoynServiceListener {
        // Service connected
        public void onServiceConnected() {
        	// Load contact info in the list 
        	loadDataSet();
        }
        
        // Service disconnected
        public void onServiceDisconnected(int error) {
        }  
    }    
    
    /**
     * MM session API listener
     */
    private class MySessionApiListener implements JoynServiceListener {
        // Service connected
        public void onServiceConnected() {
			sendBtn.setEnabled((selectedContacts.size() > 0) && (sessionApi.isServiceConnected()));
        }
        
        // Service disconnected
        public void onServiceDisconnected(int error) {
			sendBtn.setEnabled(false);
        }  
    }        
	
    /**
     * Callback called when service is registered to the RCS/IMS platform
     */
    public void onServiceRegistered() {
    	// Nothing done here
    }
    
    /**
     * Callback called when service is unregistered from the RCS/IMS platform
     */
    public void onServiceUnregistered() {
		handler.post(new Runnable(){
			public void run(){
				Utils.showMessageAndExit(SendPopup.this, getString(R.string.label_service_disabled));
			}
		});
    }   	

	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
		synchronized(selectedContacts) {
			ContactItem contact = ((ContactItem)adapter.getItem(position));
			contact.selected = !contact.selected;
			
			if (contact.selected) {
				selectedContacts.add(contact);
			} else {
				selectedContacts.remove(contact);
			}
			
			adapter.notifyDataSetChanged();
			sendBtn.setEnabled((selectedContacts.size() > 0) && (sessionApi.isServiceConnected()));
		}
	}
	
	/**
	 * Load contact info in the list 
	 */
	private synchronized void loadDataSet() {
		Log.d(TAG, "Load list of contacts");

		// Init
        items.clear();        

		// Load RCS contacts
        try {
			Set<JoynContact> supportedContacts = contactsApi.getJoynContactsSupporting(PopupManager.SERVICE_ID);
			for (JoynContact contact : supportedContacts) {
				if (contact.isRegistered()) {
					Log.d(TAG, "Add contact " + contact.getContactId());
					items.add(new ContactItem(contact.getContactId()));
				}
			}
        } catch(Exception e) {
        }
		adapter.notifyDataSetChanged();
	}
	
	/**
	 * Update contact info in the list
	 * 
	 * @param contact Contact
	 * @param capabilities Capabilities
	 */
	private synchronized void updateDataSet(String contact, Capabilities capabilities) {
		Log.d(TAG, "Update list of contacts");
		
		// Update list item
		int position = -1;
		for(int i=0; i < items.size(); i++) {
			ListItem item = items.get(i);
			ContactItem contactItem = (ContactItem)item;
			if (contactItem.number.equals(contact)) {
				position = i;				
			}
		}
		
		// Get contact info
		JoynContact joynContact = null;
		try {
			joynContact = contactsApi.getJoynContact(contact);
		} catch(Exception e) {}
		
		// Check if Popup is supported by the contact
		if ((joynContact == null) && (position != -1)) {
			// Contact is no more joyn compliant, remove it from the list
			Log.d(TAG, "Remove contact " + contact);
			items.remove(position);
		} else {
			// Check if the joyn contact supports Popup and is online 
			boolean supported = false;
			boolean online = joynContact.isRegistered();
			Set<String> exts = joynContact.getCapabilities().getSupportedExtensions();
			for(String ext : exts) {
				if (ext.equals(PopupManager.SERVICE_ID)) {
					supported = true;
					break;
				}
			}

			// Add a new contact if the contact not yet in the list and supports the feature
			if ((position == -1) && supported && online) {
				Log.d(TAG, "Add contact " + contact);
				items.add(new ContactItem(contact));
			}
			
			// Remove the contact if the contact in the list but offline or Popup no more supported
			if ((position != -1) && (!supported || !online)) {
				// Remove contact
				Log.d(TAG, "Remove contact " + contact);
				items.remove(position);
			}
		}
		
		adapter.notifyDataSetChanged();
	}
	
	/**
	 * Refresh capabilities task
	 */
	private class RefreshCapabilitiesAsyncTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				Set<JoynContact> rcsContacts = contactsApi.getJoynContacts();
				for (JoynContact contact : rcsContacts) {
					Log.d(TAG, "Refresh capabilities for contact " + contact.getContactId());
					capabilityApi.requestContactCapabilities(contact.getContactId());
				}
			} catch(Exception e) {
				Log.e(TAG, "Can't refresh capabilities", e);
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
		}
	}
	
	/**
	 * List item
	 */
	private class ListItem {
		public String name;

		public ListItem(String name) {
			this.name = name;
		}
	}
	
	/**
	 * Contact item
	 */
	private class ContactItem extends ListItem {
		public String number;
		public boolean selected = false;
		
		public ContactItem(String number) {
			super(number);

			this.number = number;
		}
	}
	
	/**
	 * List adapter
	 */
	private class MultipleListAdapter extends BaseAdapter {
		public int getCount() {
			return items.size();
		}
		
		public Object getItem(int position) {
			return items.get(position);
		}
	
		public long getItemId(int position) {
			return position;
		}
		
		public View getView(int position, View convertView, ViewGroup parent) {
			ListItem item = items.get(position);
			ContactViewHolder dataHolder;			
			if (convertView == null) {
				LayoutInflater inflater = getLayoutInflater();
				convertView = inflater.inflate(R.layout.contact_list_item, null);
				dataHolder = new ContactViewHolder();
				dataHolder.init(convertView);
				convertView.setTag(dataHolder);
			} else {
				dataHolder = (ContactViewHolder)convertView.getTag();
			}
			dataHolder.update((ContactItem)item);

			return convertView;
		}
	}

	/**
	 * Contact list view
	 */
	private class ContactViewHolder {
		public QuickContactBadge quickContactBadge;
		public TextView contactNameTextView;
		public CheckBox selectedView;
		public ImageView rcsLogoView;
		public ImageView statusView;
	
		public void init(View root) {
			quickContactBadge = (QuickContactBadge)root.findViewById(R.id.contact_badge);
			quickContactBadge.setMode(ContactsContract.QuickContact.MODE_LARGE);				
			contactNameTextView = (TextView)root.findViewById(R.id.contact);
			selectedView = (CheckBox)root.findViewById(R.id.contact_selected);
			rcsLogoView = (ImageView)root.findViewById(R.id.rcs_logo);
			statusView = (ImageView)root.findViewById(R.id.status);
		}
		
		public void update(final ContactItem data) {
			contactNameTextView.setText(data.name);
			selectedView.setChecked(data.selected);
			quickContactBadge.assignContactFromPhone(data.number, true);
			
			rcsLogoView.setVisibility(View.VISIBLE);

			statusView.setVisibility(View.VISIBLE);
			statusView.setImageResource(R.drawable.status_online);
			
			int contactId = Utils.getContactId(getApplicationContext(), data.number);
			if (contactId != -1) {
				Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
			    InputStream input = Contacts.openContactPhotoInputStream(getContentResolver(), contactUri);     
			    if (input != null) {         
			    	Uri photoUri = Uri.withAppendedPath(contactUri, Contacts.Photo.CONTENT_DIRECTORY);
			    	quickContactBadge.setImageURI(photoUri);
			    } else {
			    	quickContactBadge.setImageResource(R.drawable.default_portrait_icon);
			    }
			}
		}
	}	

	/**
	 * Initiate button callback
	 */
	private OnClickListener btnSendListener = new OnClickListener() {
		public void onClick(View v) {
			EditText msgEdit = (EditText)findViewById(R.id.message);
			final String message = msgEdit.getText().toString();

			Spinner spinnerAnim = (Spinner)findViewById(R.id.animation);
			final String animation = (String)spinnerAnim.getSelectedItem();
			
			CheckBox ttsCheck = (CheckBox)findViewById(R.id.tts);
			final boolean tts = ttsCheck.isChecked(); 
			
			// Send in background
			Thread thread = new Thread() {
				public void run() {
					for(int i=0; i < selectedContacts.size(); i++) {
						try {
		    				final ContactItem contact = selectedContacts.get(i);
							Log.d(TAG, "Send popup to " + contact.number);
							final boolean result = sessionApi.sendMessage(
									PopupManager.SERVICE_ID,
									contact.number,
									PopupManager.generatePopup(message, animation, tts));
							handler.post(new Runnable() {
								public void run() {
									if (result) {
										Utils.displayToast(SendPopup.this, getString(R.string.label_send_success, contact.name));
									} else {
										Utils.displayToast(SendPopup.this, getString(R.string.label_send_failed, contact.name));										
									}
								} 
							});
						} catch(Exception e) {
							Log.e(TAG, "Unexpected exception", e);
							handler.post(new Runnable() {
								public void run() {
									Utils.showMessageAndExit(SendPopup.this, getString(R.string.label_popup_failed));
								}
							});
						}
					}	
				}
			};
			thread.start();
		}
	};

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			// Exit activity
			finish();
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

    /**
     * Capabilities event listener
     */
    private class MyCapabilitiesListener extends CapabilitiesListener {
	    /**
	     * Callback called when new capabilities are received for a given contact
	     * 
	     * @param contact Contact
	     * @param capabilities Capabilities
	     */
	    public void onCapabilitiesReceived(final String contact, final Capabilities capabilities) {
			handler.post(new Runnable(){
				public void run(){
					// Update list of displayed contacts
					updateDataSet(contact, capabilities);
				}
			});
	    };    
    }  
}