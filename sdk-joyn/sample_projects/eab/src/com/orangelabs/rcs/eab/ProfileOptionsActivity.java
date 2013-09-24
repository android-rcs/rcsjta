/*******************************************************************************
 * Software Name : RCS
 *
 * Copyright © 2010 France Telecom S.A.
 ******************************************************************************/

package com.orangelabs.rcs.eab;

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
import android.widget.TextView;

import com.orangelabs.rcs.eab.R;
import com.orangelabs.rcs.common.UrlUtils;
import com.orangelabs.rcs.common.dialog.DialogUtils;
import com.orangelabs.rcs.service.api.client.contacts.ContactInfo;
import com.orangelabs.rcs.service.api.client.contacts.ContactsApi;
import com.orangelabs.rcs.service.api.client.presence.PresenceApiIntents;
import com.orangelabs.rcs.service.api.client.presence.PresenceInfo;
import com.orangelabs.rcs.utils.PhoneUtils;

/**
 * Profile options activity
 * <br>
 * <br>Launched when a RCS profile is clicked
 * <br>Permits to:
 * <li>Open the associated web link in browser (if any was set)
 * <li>Save the image (to be defined)
 * <li>Manage the profile
 * <li>Call the contact
 * <li>Text the contact
 */
public class ProfileOptionsActivity extends Activity {

	/**
	 * Return codes
	 */
	public final static int VISIT_WEB_LINK = 1034;
//	public final static int SAVE_IMAGE = 1035;
	public final static int MANAGE_PROFILE = 1036;
	public final static int CALL = 1037;
	public final static int TEXT = 1038;

	String return_number;
	String weblink_url;
	String weblink_name;
	boolean is_available;
	String profile_label;
	
	private Vector<Item> items = new Vector<Item>();
	private Handler handler = new Handler();
	ImageListManageAdapter manageAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Register intent receiver
		IntentFilter filter = new IntentFilter(PresenceApiIntents.CONTACT_INFO_CHANGED);
		registerReceiver(mContactChangedIntentReceiver, filter, null, handler);
		filter = new IntentFilter(PresenceApiIntents.CONTACT_PHOTO_CHANGED);
		registerReceiver(mContactPhotoChangedIntentReceiver, filter, null, handler);
		filter = new IntentFilter(PresenceApiIntents.PRESENCE_SHARING_CHANGED);
		registerReceiver(mContactStatusChangedIntentReceiver, filter, null, handler);

		Intent intent = getIntent();
		return_number = intent.getStringExtra("number");
		weblink_url = intent.getStringExtra("linkUrl");
		weblink_name = intent.getStringExtra("linkName");
		is_available = intent.getBooleanExtra("availability", false);
		profile_label = intent.getStringExtra("label");
		
		updateItems();

		manageAdapter = new ImageListManageAdapter(this);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setAdapter(manageAdapter, alertDialogListener);
		builder.setOnCancelListener(cancelDialogListener);
		
		builder.setTitle(getString(R.string.rcs_eab_mobile_profile, profile_label));
		AlertDialog alert = builder.create();
		alert.show();
	}

    protected void onDestroy() {
    	// Unregister the intent receivers
		unregisterReceiver(mContactChangedIntentReceiver);
		unregisterReceiver(mContactPhotoChangedIntentReceiver);
		unregisterReceiver(mContactStatusChangedIntentReceiver);
		super.onDestroy();
    }

	
    /**
     * Action when an item is clicked
     */
	private DialogInterface.OnClickListener alertDialogListener = new DialogInterface.OnClickListener(){
		 
		public void onClick(DialogInterface dialog, int position) {
			Intent resultIntent = new Intent();
			resultIntent.putExtra("called_number", return_number);
			if (weblink_url==null){
				// No weblink, we have to act as if position 0 was position 1, etc, so add 1 to position
				position++;
			}
			
			switch (position){
			case 0: 
				// Visit web link
				resultIntent.putExtra("weblink_url", weblink_url);
				resultIntent.putExtra("result_choice", VISIT_WEB_LINK);
				break;
//			case 1:				
//				// Save image
//				resultIntent.putExtra("result_choice", SAVE_IMAGE);
//				break;
			case 1:
				// Manage Profile sharing
				resultIntent.putExtra("result_choice", MANAGE_PROFILE);
				break;
			case 2:
				//Call now!
				resultIntent.putExtra("result_choice", CALL);
				break;
			case 3:
				// Text now!
				resultIntent.putExtra("result_choice", TEXT);
				break;
			}
			
			setResult(RESULT_OK, resultIntent);
			finish();
		}
	};

	/**
	 * Action when cancelling dialog
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
			TextView primaryText;
			TextView secondaryText;

			View view;
			if (convertView == null) {
				view = mInflater.inflate(R.layout.rcs_eab_edit_weblink_options_row_layout, parent, false);
			} else {
				view = convertView;
			}

			Item item = (Item)items.elementAt(position);

			// Set text field
			primaryText = (TextView)view.findViewById(R.id.edit_weblink_options_primary_text);
			secondaryText = (TextView)view.findViewById(R.id.edit_weblink_options_secondary_text);

			primaryText.setText(item.getPrimaryText());
			if (item.getSecondaryText()!=null){
				secondaryText.setText(item.getSecondaryText());
				secondaryText.setVisibility(View.VISIBLE);
			}else{
				secondaryText.setVisibility(View.GONE);
			}
			return view;
		}
	}

	/**
	 * 
	 * List item
	 */
	private class Item {

		private String primaryTxt;
		private String secondaryTxt;

		public Item(String primaryTxt) {
			this.primaryTxt = primaryTxt;
			this.secondaryTxt = null;
		}
		
		public Item(String primaryTxt, String secondaryTxt) {
			this.primaryTxt = primaryTxt;
			this.secondaryTxt = secondaryTxt;
		}

		public String getPrimaryText() {
			return primaryTxt;
		}

		public String getSecondaryText() {
			return secondaryTxt;
		}

	}

	
	/**
	 * Updates the items that must be shown to the user
	 * <br>It may change dynamically if contact info changes when we are already on this options menu
	 */
	private void updateItems(){
		items.clear();
		if (weblink_url!=null){
			if (weblink_name == null || (weblink_name.length()<1)){
				weblink_name = UrlUtils.formatUrlToDomainName(weblink_url);
			}

			// Visit web link item
			items.add(new Item(getString(R.string.rcs_eab_visit_web_link), getString(R.string.rcs_eab_visit_web_link_at, weblink_name)));
		}
		
		// Save image item
//		items.add(new Item(getString(R.string.rcs_eab_save_image)));
		
		// Manage profile sharing item
		items.add(new Item(getString(R.string.rcs_eab_manage_profile_sharing)));
		
		if (is_available){
			// Call now item
			items.add(new Item(getString(R.string.rcs_eab_call_now)));
			
			// Text now item
			items.add(new Item(getString(R.string.rcs_eab_text_now)));
		}
	}
	
	/**
	 * Broadcast receiver receiving contact info changes
	 */
	private final BroadcastReceiver mContactChangedIntentReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, final Intent intent) {
			handler.post(new Runnable(){
				public void run(){
					String contact = intent.getStringExtra("contact");
					if (PhoneUtils.extractNumberFromUri(contact).equalsIgnoreCase(PhoneUtils.extractNumberFromUri(return_number))){
						// This event is for this contact
						ContactsApi contactsApi = new ContactsApi(ProfileOptionsActivity.this);
						ContactInfo info = contactsApi.getContactInfo(contact);

						if (info!=null && info.getPresenceInfo()!=null){
							// Get weblink
							weblink_url = info.getPresenceInfo().getFavoriteLinkUrl();
							if (info.getPresenceInfo().getFavoriteLink()!=null){
								weblink_name = info.getPresenceInfo().getFavoriteLink().getName();
							}
							// Get availability
							is_available = info.getPresenceInfo().isOnline();
						}
						updateItems();
						manageAdapter.notifyDataSetChanged();
					}
				}
			});
		}
	};    
    
	/**
	 * Broadcast receiver receiving contact photo changes
	 */
	private final BroadcastReceiver mContactPhotoChangedIntentReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, final Intent intent) {
			handler.post(new Runnable(){
				public void run(){
					String contact = intent.getStringExtra("contact");
					if (PhoneUtils.extractNumberFromUri(contact).equalsIgnoreCase(PhoneUtils.extractNumberFromUri(return_number))){
						// The contact photo changed
						
						//TODO change the photo
						manageAdapter.notifyDataSetChanged();
					}
				}
			});
		}
	};    

	/**
	 * Broadcast receiver receiving contact presence status changes 
	 */
	private final BroadcastReceiver mContactStatusChangedIntentReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, final Intent intent) {
			handler.post(new Runnable(){
				public void run(){
					String contact = intent.getStringExtra("contact");
					String status = intent.getStringExtra("status");
					if (PhoneUtils.extractNumberFromUri(contact).equalsIgnoreCase(PhoneUtils.extractNumberFromUri(return_number))){
						// This event is for this contact
						if (!status.equalsIgnoreCase(PresenceInfo.RCS_ACTIVE)){
							// We are not active anymore
							// Tell the user and leave
							DialogUtils.displayToast(ProfileOptionsActivity.this, handler, getString(R.string.rcs_eab_user_not_rcs_anymore));
							ProfileOptionsActivity.this.setResult(RESULT_CANCELED);
							finish();
						}
					}
				}
			});
		}
	};    

}
