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
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gsma.services.rcs.JoynService;
import com.gsma.services.rcs.JoynServiceException;
import com.gsma.services.rcs.JoynServiceListener;
import com.gsma.services.rcs.JoynServiceNotAvailableException;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.gsh.GeolocSharing;
import com.gsma.services.rcs.gsh.GeolocSharingIntent;
import com.gsma.services.rcs.gsh.GeolocSharingListener;
import com.gsma.services.rcs.gsh.GeolocSharingService;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.messaging.geoloc.DisplayGeoloc;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Receive geoloc sharing
 * 
 * @author vfml3370
 */
public class ReceiveGeolocSharing extends Activity implements JoynServiceListener {
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
	 * Geoloc sharing API
	 */
    private GeolocSharingService gshApi;
    
	/**
     * Geoloc sharing session
     */
    private GeolocSharing geolocSharing = null;
    
    /**
     * Geoloc sharing listener
     */
    private MyGeolocSharingListener gshListener = new MyGeolocSharingListener();    
   
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.geoloc_sharing_receive);
        
        // Set title
		setTitle(R.string.title_geoloc_sharing);

        // Get invitation info
        sharingId = getIntent().getStringExtra(GeolocSharingIntent.EXTRA_SHARING_ID);
		remoteContact = getIntent().getParcelableExtra(GeolocSharingIntent.EXTRA_CONTACT);

        // Instanciate API
		gshApi = new GeolocSharingService(getApplicationContext(), this);
		
		// Connect API
		gshApi.connect();        
        
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();

        // Remove file transfer listener
        if (geolocSharing != null) {
        	try {
        		geolocSharing.removeEventListener(gshListener);
        	} catch(Exception e) {
        		e.printStackTrace();
        	}
        }

        // Disconnect API
        gshApi.disconnect();
    }
    
    /**
     * Callback called when service is connected. This method is called when the
     * service is well connected to the RCS service (binding procedure successfull):
     * this means the methods of the API may be used.
     */
    public void onServiceConnected() {
		try{
			// Get the geoloc sharing
			geolocSharing = gshApi.getGeolocSharing(sharingId);
			if (geolocSharing == null) {
				// Session not found or expired
				Utils.showMessageAndExit(ReceiveGeolocSharing.this, getString(R.string.label_session_not_found));
				return;
			}
			geolocSharing.addEventListener(gshListener);
			
	    	// Display sharing infos
    		TextView from = (TextView)findViewById(R.id.from);
	        from.setText(getString(R.string.label_from) + " " + remoteContact);
	    	
			// Display accept/reject dialog
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.title_geoloc_sharing);
			builder.setMessage(getString(R.string.label_from) +	" " + remoteContact);
			builder.setCancelable(false);
			builder.setIcon(R.drawable.ri_notif_gsh_icon);
			builder.setPositiveButton(getString(R.string.label_accept), acceptBtnListener);
			builder.setNegativeButton(getString(R.string.label_decline), declineBtnListener);
			builder.show();
	    } catch(JoynServiceNotAvailableException e) {
	    	e.printStackTrace();
			Utils.showMessageAndExit(ReceiveGeolocSharing.this, getString(R.string.label_api_disabled));
	    } catch(JoynServiceException e) {
	    	e.printStackTrace();
			Utils.showMessageAndExit(ReceiveGeolocSharing.this, getString(R.string.label_api_failed));
		}
    }

    /**
     * Callback called when service has been disconnected. This method is called when
     * the service is disconnected from the RCS service (e.g. service deactivated).
     * 
     * @param error Error
     * @see JoynService.Error
     */
    public void onServiceDisconnected(int error) {
		Utils.showMessageAndExit(ReceiveGeolocSharing.this, getString(R.string.label_api_disabled));
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
			Utils.showMessageAndExit(ReceiveGeolocSharing.this, getString(R.string.label_invitation_failed));
    	}
	}    
	/**
	 * Reject invitation
	 */
	private void rejectInvitation() {
    	try {
    		// Reject the invitation
    		geolocSharing.removeEventListener(gshListener);
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
     * Geoloc sharing event listener
     */
    private class MyGeolocSharingListener extends GeolocSharingListener {
    	// Sharing started
    	public void onSharingStarted() {
			handler.post(new Runnable() { 
				public void run() {
					TextView statusView = (TextView)findViewById(R.id.progress_status);
					statusView.setText("started");
				}
			});
    	}
    	
    	// Sharing aborted
    	public void onSharingAborted() {
			handler.post(new Runnable() { 
				public void run() {
					Utils.showMessageAndExit(ReceiveGeolocSharing.this, getString(R.string.label_sharing_aborted));
				}
			});
    	}

    	// Sharing error
    	public void onSharingError(final int error) {
			handler.post(new Runnable() { 
				public void run() {
					Utils.showMessageAndExit(ReceiveGeolocSharing.this, getString(R.string.label_transfer_failed, error));
				}
			});
    	}
    	
    	// Sharing progress
    	public void onSharingProgress(final long currentSize, final long totalSize) {
			handler.post(new Runnable() { 
    			public void run() {
					// Display sharing progress
    				updateProgressBar(currentSize, totalSize);
    			}
    		});
    	}

    	// Geoloc shared
    	public void onGeolocShared(final Geoloc geoloc) {
			handler.post(new Runnable() { 
				public void run() {
					TextView statusView = (TextView)findViewById(R.id.progress_status);
					statusView.setText("transferred");
					
					// Make sure progress bar is at the end
			        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progress_bar);
			        progressBar.setProgress(progressBar.getMax());
					
			        // Show the shared geoloc
					Intent intent = new Intent(ReceiveGeolocSharing.this, DisplayGeoloc.class);
			    	intent.putExtra(DisplayGeoloc.EXTRA_CONTACT, (Parcelable)remoteContact);
			    	intent.putExtra(DisplayGeoloc.EXTRA_GEOLOC, (Parcelable)geoloc);
					startActivity(intent);
				}
			});
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
            	geolocSharing.removeEventListener(gshListener);
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
