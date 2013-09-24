/*******************************************************************************
 * Software Name : RCS
 *
 * Copyright © 2010 France Telecom S.A.
 ******************************************************************************/

package com.orangelabs.rcs.eab;

import java.util.ArrayList;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.orangelabs.rcs.eab.R;
import com.orangelabs.rcs.common.ContactUtils;
import com.orangelabs.rcs.common.PhoneNumber;
import com.orangelabs.rcs.service.api.client.contacts.ContactInfo;
import com.orangelabs.rcs.service.api.client.contacts.ContactsApi;
import com.orangelabs.rcs.service.api.client.presence.PresenceApiIntents;
import com.orangelabs.rcs.utils.PhoneUtils;

/**
 * Phone number selection activity
 * 
 * <br>Used to resolve TEXT or CALL action on a contact that has many numbers 
 */
public class PhoneNumberSelectorActivity extends Activity {

	/**
	 * Return codes
	 */
	public final static int CALL = 1039;
	public final static int TEXT = 1040;

	String return_number;
	int action;
	
	private Vector<Item> items = new Vector<Item>();
	private Handler handler = new Handler();
	ImageListManageAdapter manageAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Register intent receiver
		IntentFilter filter = new IntentFilter(PresenceApiIntents.CONTACT_INFO_CHANGED);
		registerReceiver(mContactChangedIntentReceiver, filter, null, handler);

		Intent intent = getIntent();
		action = intent.getIntExtra("action", 0);
		int contactId  = intent.getIntExtra("contactId",0);
		String defaultNumber = ContactUtils.getPrimaryNumber(this, contactId);
		ArrayList<PhoneNumber> phones = ContactUtils.getContactNumbers(this, contactId);
		// If only one phone number, no need to continue we have the result
		if (phones.size()==1){
			Intent resultIntent = new Intent();
			resultIntent.putExtra("result_choice", action);
			resultIntent.putExtra("called_number", phones.get(0).number);

			setResult(RESULT_OK, resultIntent);
			finish();
		}
		
		// Check each phone number for the contact
		for (int i=0;i<phones.size();i++){
			String number = phones.get(i).number;
			boolean isDefaultNumber = false;
			if (PhoneUtils.formatNumberToInternational(number).equalsIgnoreCase(PhoneUtils.formatNumberToInternational(defaultNumber))){
				// Check if this number is the default number
				isDefaultNumber = true;
			}
			
			ContactsApi contactsApi = new ContactsApi(this);			
			// Check if this number is available
			ContactInfo info = contactsApi.getContactInfo(number);
			boolean isAvailable = false;
			if (info!=null && info.getPresenceInfo()!=null){
				isAvailable = info.getPresenceInfo().isOnline();
			}
			
			// Get the label associated to this number
			String label = phones.get(i).label;
			
			// Add the phone number in the list
			items.add(new Item(number, label, isDefaultNumber, isAvailable));
		}

		// Build the adapter
		manageAdapter = new ImageListManageAdapter(this);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setAdapter(manageAdapter, alertDialogListener);
		builder.setOnCancelListener(cancelDialogListener);

		// Set the text according to the action that was asked (Call or Text)
		if (action==CALL){
			builder.setTitle(getString(R.string.rcs_eab_call_using));
		}else if (action == TEXT){
			builder.setTitle(getString(R.string.rcs_eab_text_using));
		}
		AlertDialog alert = builder.create();
		alert.show();
	}

    protected void onDestroy() {
    	// Unregister intent receiver
		unregisterReceiver(mContactChangedIntentReceiver);
		super.onDestroy();
    }

	
    /**
     * Action when a number is selected
     */
	private DialogInterface.OnClickListener alertDialogListener = new DialogInterface.OnClickListener(){
		 
		public void onClick(DialogInterface dialog, int position) {
			Intent resultIntent = new Intent();
			resultIntent.putExtra("result_choice", action);
			resultIntent.putExtra("called_number", items.get(position).number);
			
			setResult(RESULT_OK, resultIntent);
			finish();
		}
	};

	/**
	 * Action if no number is selected
	 */
	private DialogInterface.OnCancelListener cancelDialogListener = new DialogInterface.OnCancelListener() {
		public void onCancel(DialogInterface dialog) {
			setResult(RESULT_CANCELED);
			finish();
		}
	};
		 
	
	/**
	 * List adapter
	 */
	private class ImageListManageAdapter extends BaseAdapter {
		private LayoutInflater mInflater;

		public ImageListManageAdapter(Context context) {
			// Cache the LayoutInflate to avoid asking for a new one each time.
			mInflater = LayoutInflater.from(context);
		}

		/**
		 * The number of items in the list is determined by the number of speeches
		 * in our array.
		 *
		 * @see android.widget.ListAdapter#getCount()
		 */
		public int getCount() {
			return items.size();
		}

		/**
		 * Since the data comes from an array, just returning the index is
		 * sufficent to get at the data. If we were using a more complex data
		 * structure, we would return whatever object represents one row in the
		 * list.
		 *
		 * @see android.widget.ListAdapter#getItem(int)
		 */
		public Object getItem(int position) {
			return position;
		}

		/**
		 * Use the array index as a unique id.
		 *
		 * @see android.widget.ListAdapter#getItemId(int)
		 */
		public long getItemId(int position) {
			return position;
		}

		/**
		 * Make a view to hold each row.
		 *
		 * @see android.widget.ListAdapter#getView(int, android.view.View,
		 *      android.view.ViewGroup)
		 */
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView number;
			TextView label;
			ImageView image;

			View view;
			if (convertView == null) {
				view = mInflater.inflate(R.layout.rcs_eab_phone_selector_options_row_layout, parent, false);
			} else {
				view = convertView;
			}

			Item item = (Item)items.elementAt(position);

			// Set text field
			number = (TextView)view.findViewById(R.id.phone_selector_options_number);
			label = (TextView)view.findViewById(R.id.phone_selector_options_label);
			image = (ImageView)view.findViewById(R.id.phone_selector_options_image);
			
			number.setText(item.getNumber());
			image.setVisibility(View.GONE);
			if (item.isDefault()){
				image.setVisibility(View.VISIBLE);
				image.setImageResource(R.drawable.checkbox_on_background);
			}
			if (item.isAvailable()){
				image.setVisibility(View.VISIBLE);
				image.setImageResource(R.drawable.rcs_widget_icon_available);
			}
			label.setText(item.getLabel().toLowerCase());
			return view;
		}
	}

	/**
	 * List item
	 */
	private class Item {

		private String number;
		private String label;
		private boolean availability;
		private boolean isDefault;

		public Item(String number, String label, boolean isDefault, boolean isAvailable) {
			this.number = number;
			this.label = label;
			this.availability = isAvailable;
			this.isDefault = isDefault;
		}
		
		public void setAvailability(boolean available) {
			this.availability = available;
		}
		
		public String getNumber() {
			return number;
		}

		public String getLabel() {
			return label;
		}
		
		public boolean isAvailable() {
			return availability;
		}
		
		public boolean isDefault() {
			return isDefault;
		}
	}

	
	/**
	 * Broadcast receiver if contact info changes
	 */
	private final BroadcastReceiver mContactChangedIntentReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, final Intent intent) {
			handler.post(new Runnable(){
				public void run(){
					String contact = intent.getStringExtra("contact");
					// Check if event is for one of our numbers
					for (int i=0;i<items.size();i++){
						Item item = items.get(i);
						if (PhoneUtils.extractNumberFromUri(contact).equalsIgnoreCase(PhoneUtils.extractNumberFromUri(item.number))){
							// This event is for this number
							// Change availability
							ContactsApi contactsApi = new ContactsApi(PhoneNumberSelectorActivity.this);
							ContactInfo info = contactsApi.getContactInfo(contact);
							if (info!=null && info.getPresenceInfo()!=null){
								item.setAvailability(info.getPresenceInfo().isOnline());
							}else{
								item.setAvailability(false);
							}
							items.setElementAt(item, i);
							manageAdapter.notifyDataSetChanged();
						}
					}
				}
			});
		}
	};    
    
}
