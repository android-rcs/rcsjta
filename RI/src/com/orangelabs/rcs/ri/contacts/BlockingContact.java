package com.orangelabs.rcs.ri.contacts;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import android.widget.ToggleButton;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.ContactUtils;
import com.gsma.services.rcs.contacts.RcsContact;
import com.orangelabs.rcs.ri.ApiConnectionManager;
import com.orangelabs.rcs.ri.ApiConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.ContactListAdapter;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Block/unblock a contact
 * 
 * @author Jean-Marc AUFFRET
 */
public class BlockingContact extends Activity {
	/**
	 * API connection manager
	 */
	private ApiConnectionManager connectionManager;
	
	/**
   	 * A locker to exit only once
   	 */
   	private LockAccess exitOnce = new LockAccess();	
	
	/**
	 * Spinner for contact selection
	 */
	private Spinner mSpinner;

	/**
	 * Toggle button
	 */
	private ToggleButton toggleBtn;
	
	/**
	 * Contact utils
	 */
	private ContactUtils mContactUtils;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set layout
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.contacts_blocking);

		// Set contact utils instance
		mContactUtils = ContactUtils.getInstance(this);		

		// Set the contact selector
		mSpinner = (Spinner) findViewById(R.id.contact);
		ContactListAdapter adapter = ContactListAdapter.createRcsContactListAdapter(this);
		mSpinner.setAdapter(adapter);
		mSpinner.setOnItemSelectedListener(listenerContact);

		// Set button callback
		toggleBtn = (ToggleButton) findViewById(R.id.block_btn);
		toggleBtn.setOnClickListener(toggleListener);

		// Update refresh button
		if (mSpinner.getAdapter().getCount() == 0) {
			// Disable button if no contact available
			toggleBtn.setEnabled(false);
		} else {
			toggleBtn.setEnabled(true);
		}
		
		// Register to API connection manager
		connectionManager = ApiConnectionManager.getInstance(this);
		if (connectionManager == null || !connectionManager.isServiceConnected(RcsServiceName.CONTACTS)) {
			Utils.showMessageAndExit(this, getString(R.string.label_service_not_available), exitOnce);
			return;
		}
		connectionManager.startMonitorServices(this, null, RcsServiceName.CONTACTS);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (connectionManager != null) {
			connectionManager.stopMonitorServices(this);
    	}
	}
	
	private void updateBlockingState(ContactId contactId) {
	   	try {
			RcsContact contact = connectionManager.getContactsApi().getRcsContact(contactId);
			toggleBtn.setChecked(contact.isBlocked());
		} catch (RcsServiceNotAvailableException e) {
			e.printStackTrace();
			Utils.showMessageAndExit(BlockingContact.this, getString(R.string.label_api_disabled), exitOnce);
		} catch (RcsServiceException e) {
			e.printStackTrace();
			Utils.showMessageAndExit(BlockingContact.this, getString(R.string.label_api_failed), exitOnce);
		}
	}
	
   	/**
	 * Spinner contact listener
	 */
	private OnItemSelectedListener listenerContact = new OnItemSelectedListener() {
		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
			ContactId contactId = getSelectedContact();
			updateBlockingState(contactId);
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
		}
	};
	
	/**
	 * Returns the selected contact
	 * 
	 * @return Contact ID
	 */
	private ContactId getSelectedContact() {
		// get selected phone number
		ContactListAdapter adapter = (ContactListAdapter) mSpinner.getAdapter();
		return mContactUtils.formatContact(adapter.getSelectedNumber(mSpinner.getSelectedView()));
	}
	
	/**
	 * Toggle button listener
     */
    private OnClickListener toggleListener = new OnClickListener() {
    	public void onClick(View view) {
        	try {
				ContactId contact = getSelectedContact();
	        	if (toggleBtn.isChecked()) {
					// Block the contact
	        		connectionManager.getContactsApi().blockContact(contact);
	        		Utils.displayToast(BlockingContact.this, getString(R.string.label_contact_blocked, contact.toString()));
				} else {
					// Unblock the contact
	        		connectionManager.getContactsApi().unblockContact(contact);
	        		Utils.displayToast(BlockingContact.this, getString(R.string.label_contact_unblocked, contact.toString()));
				}
			} catch (RcsServiceNotAvailableException e) {
				e.printStackTrace();
				Utils.showMessageAndExit(BlockingContact.this, getString(R.string.label_api_disabled), exitOnce);
			} catch (RcsServiceException e) {
				Utils.showMessageAndExit(BlockingContact.this, getString(R.string.label_api_failed), exitOnce);
			}
		}
	};	
}
