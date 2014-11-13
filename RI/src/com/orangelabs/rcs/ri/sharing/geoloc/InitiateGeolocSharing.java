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

package com.orangelabs.rcs.ri.sharing.geoloc;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.ContactUtils;
import com.gsma.services.rcs.gsh.GeolocSharing;
import com.gsma.services.rcs.gsh.GeolocSharingListener;
import com.orangelabs.rcs.ri.ApiConnectionManager;
import com.orangelabs.rcs.ri.ApiConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.messaging.geoloc.DisplayGeoloc;
import com.orangelabs.rcs.ri.messaging.geoloc.EditGeoloc;
import com.orangelabs.rcs.ri.utils.ContactListAdapter;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Initiate geoloc sharing
 * 
 * @author vfml3370
 */
public class InitiateGeolocSharing extends Activity {
	/**
	 * Activity result constants
	 */
	private final static int SELECT_GEOLOCATION = 0;

	/**
	 * UI handler
	 */
	private final Handler handler = new Handler();
	
    /**
     * Progress dialog
     */
    private Dialog progressDialog;    
    
    /**
     * Geoloc info
     */
    private Geoloc geoloc;
    
	/**
     * Geoloc sharing session
     */
    private GeolocSharing geolocSharing;
    
    /**
     * Geoloc sharing identifier
     */
    private String sharingId;
    
    /**
     * Remote contact
     */
    private ContactId contact;
    
    /**
   	 * The log tag for this class
   	 */
   	private static final String LOGTAG = LogUtils.getTag(InitiateGeolocSharing.class.getSimpleName());
    
    /**
	 * A locker to exit only once
	 */
	private LockAccess exitOnce = new LockAccess();
	
  	/**
	 * API connection manager
	 */
	private ApiConnectionManager connectionManager;
   	
	
	/**
	 * Spinner for contact selection
	 */
	private Spinner mSpinner;
	
    /**
     * Geolocation sharing listener
     */
	private GeolocSharingListener gshListener = new GeolocSharingListener() {

		@Override
		public void onProgressUpdate(ContactId contact, String sharingId, final long currentSize, final long totalSize) {
			// Discard event if not for current sharingId
			if (InitiateGeolocSharing.this.sharingId == null || !InitiateGeolocSharing.this.sharingId.equals(sharingId)) {
				return;
			}
			handler.post(new Runnable() {
				public void run() {
					// Display sharing progress
					updateProgressBar(currentSize, totalSize);
				}
			});
		}

		@Override
		public void onStateChanged(final ContactId contact, String sharingId, final int state, int reasonCode) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onGeolocSharingStateChanged contact=" + contact + " sharingId=" + sharingId + " state=" + state
						+ " reason=" + reasonCode);
			}
			// Discard event if not for current sharingId
			if (InitiateGeolocSharing.this.sharingId == null || !InitiateGeolocSharing.this.sharingId.equals(sharingId)) {
				return;
			}
			if (state > RiApplication.GSH_STATES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onGeolocSharingStateChanged unhandled state=" + state);
				}
				return;
			}
			if (reasonCode > RiApplication.GSH_REASON_CODES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onGeolocSharingStateChanged unhandled reason=" + reasonCode);
				}
				return;
			}
			final String _state = RiApplication.GSH_STATES[state];
			final String _reasonCode = RiApplication.GSH_REASON_CODES[reasonCode];
			
			handler.post(new Runnable() {
				public void run() {
					TextView statusView = (TextView) findViewById(R.id.progress_status);
					switch (state) {
					case GeolocSharing.State.STARTED:
						// Session is established: hide progress dialog
						hideProgressDialog();
						// Display session status
						statusView.setText(_state);
						break;

					case GeolocSharing.State.ABORTED:
						// sharing aborted: hide progress dialog then exit
						hideProgressDialog();
						// Display session status
						Utils.showMessageAndExit(InitiateGeolocSharing.this, getString(R.string.label_sharing_aborted, _reasonCode), exitOnce);
						break;

					case GeolocSharing.State.REJECTED:
						// sharing rejected: hide progress dialog then exit
						hideProgressDialog();
						Utils.showMessageAndExit(InitiateGeolocSharing.this, getString(R.string.label_sharing_rejected, _reasonCode), exitOnce);
						break;

					case GeolocSharing.State.FAILED:
						// sharing failed: hide progress dialog then exit
						hideProgressDialog();
						Utils.showMessageAndExit(InitiateGeolocSharing.this, getString(R.string.label_sharing_failed, _reasonCode), exitOnce);
						break;

					case GeolocSharing.State.TRANSFERRED:
						// Hide progress dialog
						hideProgressDialog();
						// Display transfer progress
						statusView.setText(_state);
						// Make sure progress bar is at the end
						ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
						progressBar.setProgress(progressBar.getMax());

						// Show the shared geoloc
						Intent intent = new Intent(InitiateGeolocSharing.this, DisplayGeoloc.class);
						intent.putExtra(DisplayGeoloc.EXTRA_CONTACT, (Parcelable) contact);
						intent.putExtra(DisplayGeoloc.EXTRA_GEOLOC, (Parcelable) geoloc);
						startActivity(intent);
						break;

					default:
						statusView.setText(getString(R.string.label_gsh_state_changed, _state, _reasonCode));
					}
				}
			});
		}
	};
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.geoloc_sharing_initiate);
        
        // Set contact selector
        mSpinner = (Spinner)findViewById(R.id.contact);
        mSpinner.setAdapter(ContactListAdapter.createRcsContactListAdapter(this));

        // Set buttons callback
        Button inviteBtn = (Button)findViewById(R.id.invite_btn);
        inviteBtn.setOnClickListener(btnInviteListener);
        inviteBtn.setEnabled(false);
        Button selectBtn = (Button)findViewById(R.id.select_btn);
        selectBtn.setOnClickListener(btnSelectListener);
        selectBtn.setEnabled(false);
        Button dialBtn = (Button)findViewById(R.id.dial_btn);
        dialBtn.setOnClickListener(btnDialListener);
        dialBtn.setEnabled(false);        
        
        // Disable button if no contact available
        if (mSpinner.getAdapter().getCount() != 0) {
        	dialBtn.setEnabled(true);
        	selectBtn.setEnabled(true);
        }
        
        // Register to API connection manager
		connectionManager = ApiConnectionManager.getInstance(this);
		if (connectionManager == null || !connectionManager.isServiceConnected(RcsServiceName.GEOLOC_SHARING)) {
			Utils.showMessageAndExit(this, getString(R.string.label_service_not_available), exitOnce);
			return;
		}
		connectionManager.startMonitorServices(this, exitOnce, RcsServiceName.GEOLOC_SHARING);
		try {
			// Add service listener
			connectionManager.getGeolocSharingApi().addEventListener(gshListener);
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
		if (connectionManager.isServiceConnected(RcsServiceName.GEOLOC_SHARING)) {
			// Remove geoloc sharing listener
			try {
				connectionManager.getGeolocSharingApi().removeEventListener(gshListener);
			} catch (Exception e) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "Failed to remove listener", e);
				}
			}
		}
	}
    
    /**
     * Dial button listener
     */
    private OnClickListener btnDialListener = new OnClickListener() {
        public void onClick(View v) {
        	// get selected phone number
    		ContactListAdapter adapter = (ContactListAdapter) mSpinner.getAdapter();
    		String phoneNumber = adapter.getSelectedNumber(mSpinner.getSelectedView());

            // Initiate a GSM call before to be able to share content
            Intent intent = new Intent(Intent.ACTION_CALL);
        	intent.setData(Uri.parse("tel:"+phoneNumber));
            startActivity(intent);
        }
    };

    /**
     * Invite button listener
     */
    private OnClickListener btnInviteListener = new OnClickListener() {
        public void onClick(View v) {
            // Check if the service is available
        	boolean registered = false;
        	try {
        		registered = connectionManager.getGeolocSharingApi().isServiceRegistered();
        	} catch(Exception e) {
        		e.printStackTrace();
        	}
            if (!registered) {
    	    	Utils.showMessage(InitiateGeolocSharing.this, getString(R.string.label_service_not_available));
    	    	return;
            }    
            
            // get selected phone number
    		ContactListAdapter adapter = (ContactListAdapter) mSpinner.getAdapter();
    		String phoneNumber = adapter.getSelectedNumber(mSpinner.getSelectedView());
            
			ContactUtils contactUtils = ContactUtils.getInstance(InitiateGeolocSharing.this);
			try {
				contact = contactUtils.formatContact(phoneNumber);
			} catch (RcsContactFormatException e1) {
				Utils.showMessage(InitiateGeolocSharing.this, getString(R.string.label_invalid_contact, phoneNumber));
				return;
			}

        	try {
                // Initiate location share
        		geolocSharing = connectionManager.getGeolocSharingApi().shareGeoloc(contact, geoloc);
        		sharingId = geolocSharing.getSharingId();
        	} catch(Exception e) {
        		e.printStackTrace();
				hideProgressDialog();
				Utils.showMessageAndExit(InitiateGeolocSharing.this, getString(R.string.label_invitation_failed), exitOnce);
        	}

            // Display a progress dialog
            progressDialog = Utils.showProgressDialog(InitiateGeolocSharing.this, getString(R.string.label_command_in_progress));            
            progressDialog.setOnCancelListener(new OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					Toast.makeText(InitiateGeolocSharing.this, getString(R.string.label_sharing_cancelled), Toast.LENGTH_SHORT).show();
					quitSession();
				}
			});
            
            // Disable UI
            mSpinner.setEnabled(false);

            // Hide buttons
            Button inviteBtn = (Button)findViewById(R.id.invite_btn);
        	inviteBtn.setVisibility(View.INVISIBLE);
            Button selectBtn = (Button)findViewById(R.id.select_btn);
            selectBtn.setVisibility(View.INVISIBLE);
            Button dialBtn = (Button)findViewById(R.id.dial_btn);
            dialBtn.setVisibility(View.INVISIBLE);        	
        }
    };
       
    /**
     * Select location button listener
     */
    private OnClickListener btnSelectListener = new OnClickListener() {
        public void onClick(View v) {
    		// Start a new activity to send a geolocation
        	startActivityForResult(new Intent(InitiateGeolocSharing.this, EditGeoloc.class), SELECT_GEOLOCATION);
        }
    };

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
				geoloc = data.getParcelableExtra(EditGeoloc.EXTRA_GEOLOC); 
				
                // Enable invite button
                Button inviteBtn = (Button)findViewById(R.id.invite_btn);
            	inviteBtn.setEnabled(true);  
			}             
            break;
        }
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
     * Show the sharing progress
     * 
     * @param currentSize Current size transferred
     * @param totalSize Total size to be transferred
     */
    private void updateProgressBar(long currentSize, long totalSize) {
    	TextView statusView = (TextView)findViewById(R.id.progress_status);
        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progress_bar);
    	
		String value = "" + (currentSize/1024);
		if (totalSize != 0) {
			value += "/" + (totalSize/1024);
		}
		value += " Kb";
		statusView.setText(value);
	    
	    if (currentSize != 0) {
	    	double position = ((double)currentSize / (double)totalSize)*100.0;
	    	progressBar.setProgress((int)position);
	    } else {
	    	progressBar.setProgress(0);
	    }
    }    
    
    /**
     * Quit the session
     */
    private void quitSession() {
		// Stop session
		try {
	        if (geolocSharing != null) {
	        	geolocSharing.abortSharing();
	        }
		} catch(Exception e) {
			e.printStackTrace();
		}
		geolocSharing = null;
		
	    // Exit activity
		finish();
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
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater=new MenuInflater(getApplicationContext());
		inflater.inflate(R.menu.menu_geoloc_sharing, menu);
		return true;
	}
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_close_session:
				// Quit the session
				quitSession();
				break;
		}
		return true;
	}    
}    
