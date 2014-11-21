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

package com.orangelabs.rcs.ri.capabilities;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.capability.Capabilities;
import com.gsma.services.rcs.capability.CapabilitiesListener;
import com.gsma.services.rcs.capability.CapabilityService;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.ContactUtils;
import com.orangelabs.rcs.ri.ApiConnectionManager;
import com.orangelabs.rcs.ri.ApiConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.ContactListAdapter;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Refresh capabilities of a given contact
 * 
 * @author Jean-Marc AUFFRET
 */
public class RequestCapabilities extends Activity {
	/**
	 * UI handler
	 */
	private final Handler handler = new Handler();

	/**
	 * A locker to exit only once
	 */
	private LockAccess exitOnce = new LockAccess();

	/**
	 * API connection manager
	 */
	private ApiConnectionManager connectionManager;

	/**
	 * Capabilities listener
	 */
	private MyCapabilitiesListener capabilitiesListener = new MyCapabilitiesListener();

	private static final String EXTENSION_SEPARATOR = "\n";

	/**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(RequestCapabilities.class.getSimpleName());

	private ContactUtils mContactUtils;

	/**
	 * Spinner for contact selection
	 */
	private Spinner mSpinner;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set layout
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.capabilities_request);

		// Set the contact selector
		mSpinner = (Spinner) findViewById(R.id.contact);
		mSpinner.setAdapter(ContactListAdapter.createContactListAdapter(this));
		mSpinner.setOnItemSelectedListener(listenerContact);

		// Set button callback
		Button refreshBtn = (Button) findViewById(R.id.refresh_btn);
		refreshBtn.setOnClickListener(btnRefreshListener);

		// Update refresh button
		if (mSpinner.getAdapter().getCount() == 0) {
			// Disable button if no contact available
			refreshBtn.setEnabled(false);
		} else {
			refreshBtn.setEnabled(true);
		}

		mContactUtils = ContactUtils.getInstance(this);

		// Register to API connection manager
		connectionManager = ApiConnectionManager.getInstance(this);
		if (connectionManager == null || !connectionManager.isServiceConnected(RcsServiceName.CAPABILITY)) {
			Utils.showMessageAndExit(this, getString(R.string.label_service_not_available), exitOnce);
			return;
		}
		connectionManager.startMonitorServices(this, null, RcsServiceName.CAPABILITY);
		try {
			// Add service listener
			connectionManager.getCapabilityApi().addCapabilitiesListener(capabilitiesListener);
		} catch (RcsServiceException e) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Failed to add listener", e);
			}
			Utils.showMessageAndExit(this, getString(R.string.label_api_failed), exitOnce);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (connectionManager == null) {
			return;
		}
		connectionManager.stopMonitorServices(this);
		if (connectionManager.isServiceConnected(RcsServiceName.CAPABILITY)) {
			// Remove image sharing listener
			try {
				connectionManager.getCapabilityApi().removeCapabilitiesListener(capabilitiesListener);
			} catch (Exception e) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "Failed to remove listener", e);
				}
			}
		}
	}

	/**
	 * Capabilities event listener
	 */
	private class MyCapabilitiesListener extends CapabilitiesListener {
		/**
		 * Callback called when new capabilities are received for a given contact
		 * 
		 * @param contact
		 *            Contact
		 * @param capabilities
		 *            Capabilities
		 */
		public void onCapabilitiesReceived(final ContactId contact, final Capabilities capabilities) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onCapabilitiesReceived " + contact);
			}
			final ContactId selectedContact = getSelectedContact();
			if (!contact.equals(selectedContact)) {
				// Discard capabilities if not for selected contact
				return;
			}
			handler.post(new Runnable() {
				public void run() {
					// Check if this intent concerns the current selected
					// contact
					if (contact != null && contact.equals(selectedContact)) {
						// Update UI
						displayCapabilities(capabilities);
					}
				}
			});
		};
	}

	/**
	 * Spinner contact listener
	 */
	private OnItemSelectedListener listenerContact = new OnItemSelectedListener() {
		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
			CapabilityService capabilityApi = connectionManager.getCapabilityApi();
			try {
				// Get selected contact
				ContactId contactId = getSelectedContact();

				// Get current capabilities
				Capabilities currentCapabilities = capabilityApi.getContactCapabilities(contactId);

				// Display default capabilities
				displayCapabilities(currentCapabilities);
			} catch (RcsServiceNotAvailableException e) {
				e.printStackTrace();
				Utils.showMessageAndExit(RequestCapabilities.this, getString(R.string.label_api_disabled), exitOnce);
			} catch (RcsServiceException e) {
				Utils.showMessageAndExit(RequestCapabilities.this, getString(R.string.label_api_failed), exitOnce);
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
		}
	};

	/**
	 * Returns the selected contact
	 * 
	 * @return Contact
	 */
	private ContactId getSelectedContact() {
		// get selected phone number
		ContactListAdapter adapter = (ContactListAdapter) mSpinner.getAdapter();
		return mContactUtils.formatContact(adapter.getSelectedNumber(mSpinner.getSelectedView()));
	}

	/**
	 * Request button callback
	 */
	private OnClickListener btnRefreshListener = new OnClickListener() {
		public void onClick(View v) {
			// Check if the service is available
			boolean registered = false;
			try {
				registered = connectionManager.getCapabilityApi().isServiceRegistered();
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (!registered) {
				Utils.showMessage(RequestCapabilities.this, getString(R.string.label_service_not_available));
				return;
			}

			ContactId contact = getSelectedContact();
			if (contact != null) {
				updateCapabilities(contact);
			}
		}
	};

	/**
	 * Update capabilities of a given contact
	 * 
	 * @param contact
	 *            Contact
	 */
	private void updateCapabilities(ContactId contact) {
		// Display info
		Utils.displayLongToast(RequestCapabilities.this, getString(R.string.label_request_in_background, contact));
		// Request capabilities
		try {
			// Request new capabilities
			connectionManager.getCapabilityApi().requestContactCapabilities(contact);
		} catch (RcsServiceNotAvailableException e) {
			e.printStackTrace();
			Utils.showMessageAndExit(this, getString(R.string.label_api_disabled), exitOnce);
		} catch (RcsServiceException e) {
			e.printStackTrace();
			Utils.showMessageAndExit(this, getString(R.string.label_api_failed), exitOnce);
		}
	}

	/**
	 * Display the capabilities
	 * 
	 * @param capabilities
	 *            Capabilities
	 */
	private void displayCapabilities(Capabilities capabilities) {
		CheckBox imageCSh = (CheckBox) findViewById(R.id.image_sharing);
		CheckBox videoCSh = (CheckBox) findViewById(R.id.video_sharing);
		CheckBox ft = (CheckBox) findViewById(R.id.file_transfer);
		CheckBox im = (CheckBox) findViewById(R.id.im);
		CheckBox geoloc = (CheckBox) findViewById(R.id.geoloc_push);
		CheckBox ipVoiceCall = (CheckBox) findViewById(R.id.ip_voice_call);
		CheckBox ipVideoCall = (CheckBox) findViewById(R.id.ip_video_call);
		TextView extensions = (TextView) findViewById(R.id.extensions);
		TextView timestamp = (TextView) findViewById(R.id.last_refresh);
		CheckBox automata = (CheckBox) findViewById(R.id.automata);
		CheckBox valid = (CheckBox) findViewById(R.id.is_valid);
		// Set capabilities
		imageCSh.setChecked((capabilities != null) ? capabilities.isImageSharingSupported() : false);
		videoCSh.setChecked((capabilities != null) ? capabilities.isVideoSharingSupported() : false);
		ft.setChecked((capabilities != null) ? capabilities.isFileTransferSupported() : false);
		im.setChecked((capabilities != null) ? capabilities.isImSessionSupported() : false);
		geoloc.setChecked((capabilities != null) ? capabilities.isGeolocPushSupported() : false);
		ipVoiceCall.setChecked((capabilities != null) ? capabilities.isIPVoiceCallSupported() : false);
		ipVideoCall.setChecked((capabilities != null) ? capabilities.isIPVideoCallSupported() : false);

		// Set extensions
		extensions.setVisibility(View.VISIBLE);
		extensions.setText(getExtensions(capabilities));
		automata.setChecked((capabilities != null) ? capabilities.isAutomata() : false);
		timestamp.setText((capabilities != null) ? DateUtils.getRelativeTimeSpanString(capabilities.getTimestamp(), System.currentTimeMillis(),
				DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE) : "");
		valid.setChecked((capabilities != null) ? capabilities.isValid() : false);
	}

	/* package private */static String getExtensions(Capabilities capabilities) {
		if (capabilities == null || capabilities.getSupportedExtensions().isEmpty()) {
			return "";
		}
		StringBuilder extensions = new StringBuilder();
		for (String capability : capabilities.getSupportedExtensions()) {
			extensions.append(EXTENSION_SEPARATOR).append(capability);
		}
		return extensions.substring(EXTENSION_SEPARATOR.length());
	}
}
