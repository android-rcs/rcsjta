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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.gsh.GeolocSharing;
import com.gsma.services.rcs.gsh.GeolocSharingIntent;
import com.gsma.services.rcs.gsh.GeolocSharingListener;
import com.gsma.services.rcs.gsh.GeolocSharingService;
import com.orangelabs.rcs.ri.ApiConnectionManager;
import com.orangelabs.rcs.ri.ApiConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.messaging.geoloc.DisplayGeoloc;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsDisplayName;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Receive geoloc sharing
 * 
 * @author vfml3370
 */
public class ReceiveGeolocSharing extends Activity {
    /**
     * UI handler
     */
    private final Handler handler = new Handler();
    
    /**
     * Sharing ID
     */
    private String sharingId;
    
    /**
     * Remote Contact
     */
    private ContactId remoteContact;
   
	/**
     * Geoloc sharing session
     */
    private GeolocSharing geolocSharing;
    
    /**
	 * A locker to exit only once
	 */
	private LockAccess exitOnce = new LockAccess();
    
   	/**
	 * API connection manager
	 */
	private ApiConnectionManager connectionManager;
	
    /**
   	 * The log tag for this class
   	 */
   	private static final String LOGTAG = LogUtils.getTag(ReceiveGeolocSharing.class.getSimpleName());
    
    /**
     * Geoloc sharing listener
     */
	private GeolocSharingListener gshListener = new GeolocSharingListener() {

		@Override
		public void onProgressUpdate(ContactId contact, String sharingId, final long currentSize, final long totalSize) {
			// Discard event if not for current sharingId
			if (ReceiveGeolocSharing.this.sharingId == null || !ReceiveGeolocSharing.this.sharingId.equals(sharingId)) {
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
		public void onStateChanged(final ContactId contact, String sharingId, final int state, final int reasonCode) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onGeolocSharingStateChanged contact=" + contact + " sharingId=" + sharingId + " state=" + state
						+ " reason=" + reasonCode);
			}
			if (state > RiApplication.GSH_STATES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onGeolocSharingStateChanged unhandled state=" + state);
				}
				return;
			}
			// Discard event if not for current sharingId
			if (ReceiveGeolocSharing.this.sharingId == null || !ReceiveGeolocSharing.this.sharingId.equals(sharingId)) {
				return;
			}
			// TODO : handle reason code (CR025)
			final String notif = getString(R.string.label_gsh_state_changed, RiApplication.GSH_STATES[state], reasonCode);
			handler.post(new Runnable() {
				public void run() {
					TextView statusView = (TextView) findViewById(R.id.progress_status);
					switch (state) {
					case GeolocSharing.State.STARTED:
						// Session is established: display session status
						statusView.setText("started");
						break;

					case GeolocSharing.State.ABORTED:
						// Session is aborted: display session status
						Utils.showMessageAndExit(ReceiveGeolocSharing.this, getString(R.string.label_sharing_aborted, reasonCode), exitOnce);
						break;

					case GeolocSharing.State.FAILED:
						// Session is failed: exit
						Utils.showMessageAndExit(ReceiveGeolocSharing.this, getString(R.string.label_sharing_failed, reasonCode), exitOnce);
						break;

					case GeolocSharing.State.TRANSFERRED:
						// Display transfer progress
						statusView.setText("transferred");
						// Make sure progress bar is at the end
						ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
						progressBar.setProgress(progressBar.getMax());

						// Show the shared geoloc
						Intent intent = new Intent(ReceiveGeolocSharing.this, DisplayGeoloc.class);
						intent.putExtra(DisplayGeoloc.EXTRA_CONTACT, (Parcelable) contact);
						// TODO CR025: dedicated provider for GSH
						// intent.putExtra(DisplayGeoloc.EXTRA_GEOLOC, (Parcelable) geoloc);
						// startActivity(intent);
						break;

					default:
						if (LogUtils.isActive) {
							Log.d(LOGTAG, "onGeolocSharingStateChanged " + notif);
						}
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
        setContentView(R.layout.geoloc_sharing_receive);
        
        // Get invitation info
        sharingId = getIntent().getStringExtra(GeolocSharingIntent.EXTRA_SHARING_ID);
        // TODO CR025 implement provider for geolocation sharing
		remoteContact = getIntent().getParcelableExtra("TODO");

		// Register to API connection manager
		connectionManager = ApiConnectionManager.getInstance(this);
		if (connectionManager == null || !connectionManager.isServiceConnected(RcsServiceName.GEOLOC_SHARING, RcsServiceName.CONTACTS)) {
			Utils.showMessageAndExit(this, getString(R.string.label_service_not_available), exitOnce);
		} else {
			connectionManager.startMonitorServices(this, exitOnce, RcsServiceName.GEOLOC_SHARING, RcsServiceName.CONTACTS);
			// TODO CR025 add provider
			initiateGeolocSharing();
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
			// Remove service listener
			try {
				connectionManager.getGeolocSharingApi().removeEventListener(gshListener);
			} catch (Exception e) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "Failed to remove listener", e);
				}
			}
		}
	}
    
    public void initiateGeolocSharing() {
    	GeolocSharingService gshApi = connectionManager.getGeolocSharingApi();
		try {
			// Add service listener
			gshApi.addEventListener(gshListener);
			
			// Get the geoloc sharing
			geolocSharing = gshApi.getGeolocSharing(sharingId);
			if (geolocSharing == null) {
				// Session not found or expired
				Utils.showMessageAndExit(this, getString(R.string.label_session_not_found), exitOnce);
				return;
			}
			
	    	// Display sharing infos
    		TextView fromTextView = (TextView)findViewById(R.id.from);
			String from = RcsDisplayName.getInstance(this).getDisplayName(remoteContact);
			fromTextView.setText(getString(R.string.label_from_args, from));
	    	
			// Display accept/reject dialog
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.title_geoloc_sharing);
			builder.setMessage(getString(R.string.label_from_args, from));
			builder.setCancelable(false);
			builder.setIcon(R.drawable.ri_notif_gsh_icon);
			builder.setPositiveButton(getString(R.string.label_accept), acceptBtnListener);
			builder.setNegativeButton(getString(R.string.label_decline), declineBtnListener);
			builder.show();
	    } catch(RcsServiceNotAvailableException e) {
	    	e.printStackTrace();
			Utils.showMessageAndExit(this, getString(R.string.label_api_disabled), exitOnce);
	    } catch(RcsServiceException e) {
	    	e.printStackTrace();
			Utils.showMessageAndExit(this, getString(R.string.label_api_failed), exitOnce);
		}
    }

	/**
	 * Accept invitation
	 */
	private void acceptInvitation() {
    	try {
    		// Accept the invitation
    		geolocSharing.acceptInvitation();
    	} catch(Exception e) {
    		e.printStackTrace();
			Utils.showMessageAndExit(ReceiveGeolocSharing.this, getString(R.string.label_invitation_failed), exitOnce);
    	}
	}    
	/**
	 * Reject invitation
	 */
	private void rejectInvitation() {
    	try {
    		// Reject the invitation
    		geolocSharing.rejectInvitation();
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
	}	
	
	/**
     * Accept button listener
     */
    private OnClickListener acceptBtnListener = new OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {     
        	// Accept invitation
        	acceptInvitation();
        }
    };

    /**
     * Reject button listener
     */    
    private OnClickListener declineBtnListener = new OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
        	// Reject invitation
        	rejectInvitation();
        	
            // Exit activity
			finish();
        }
    };  
    
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
		inflater.inflate(R.menu.menu_image_sharing, menu);
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
