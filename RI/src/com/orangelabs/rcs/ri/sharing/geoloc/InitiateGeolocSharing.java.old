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

package com.orangelabs.rcs.ri.richcall;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.orangelabs.rcs.core.ims.service.richcall.ContentSharingError;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.messaging.EditGeoloc;
import com.orangelabs.rcs.ri.utils.Utils;
import com.orangelabs.rcs.service.api.client.messaging.GeolocPush;
import com.orangelabs.rcs.service.api.client.richcall.IGeolocSharingEventListener;
import com.orangelabs.rcs.service.api.client.richcall.IGeolocSharingSession;
import com.orangelabs.rcs.service.api.client.richcall.RichCallApi;

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
	 * Rich call API
	 */
    private RichCallApi callApi;
    
    /**
     * Progress dialog
     */
    private Dialog progressDialog = null;    
    
    /**
     * GeolocPush
     */
    private GeolocPush geoloc;
    
	/**
     * Geoloc sharing session
     */
    private IGeolocSharingSession sharingSession = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.richcall_initiate_geoloc_sharing);
        
        // Set title
        setTitle(R.string.menu_initiate_geoloc_sharing);
        
        // Set contact selector
        Spinner spinner = (Spinner)findViewById(R.id.contact);
        spinner.setAdapter(Utils.createRcsContactListAdapter(this));

        // Set buttons callback
        Button inviteBtn = (Button)findViewById(R.id.invite_btn);
        inviteBtn.setOnClickListener(btnInviteListener);
        inviteBtn.setEnabled(false);
        Button selectBtn = (Button)findViewById(R.id.select_btn);
        selectBtn.setOnClickListener(btnSelectListener);
        Button dialBtn = (Button)findViewById(R.id.dial_btn);
        dialBtn.setOnClickListener(btnDialListener);

        // Disable button if no contact available
        if (spinner.getAdapter().getCount() == 0) {
        	dialBtn.setEnabled(false);
        	selectBtn.setEnabled(false);
        }       
        
        // Rich Call API
        callApi = new RichCallApi(getApplicationContext());
        callApi.connectApi();
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();

        // Remove session listener
        if (sharingSession != null) {
        	try {
        		sharingSession.removeSessionListener(geolocSessionListener);
        	} catch(Exception e) {
        	}
        }

        // Disconnect rich call API
        callApi.disconnectApi();
    }
    
    /**
     * Dial button listener
     */
    private OnClickListener btnDialListener = new OnClickListener() {
        public void onClick(View v) {
        	// Get the remote contact
            Spinner spinner = (Spinner)findViewById(R.id.contact);
            MatrixCursor cursor = (MatrixCursor)spinner.getSelectedItem();
            String remote = cursor.getString(1);

            // Initiate a GSM call before to be able to share content
            Intent intent = new Intent(Intent.ACTION_CALL);
        	intent.setData(Uri.parse("tel:"+remote));
            startActivity(intent);
        }
    };

    /**
     * Invite button listener
     */
    private OnClickListener btnInviteListener = new OnClickListener() {
        public void onClick(View v) {
        	// Get the remote contact
            Spinner spinner = (Spinner)findViewById(R.id.contact);
            MatrixCursor cursor = (MatrixCursor)spinner.getSelectedItem();
            final String remote = cursor.getString(1);

            Thread thread = new Thread() {
            	public void run() {
                	try {
                        // Initiate location share
                		sharingSession = callApi.initiateGeolocSharing(remote, geoloc);
                		sharingSession.addSessionListener(geolocSessionListener);
	            	} catch(Exception e) {
	    				handler.post(new Runnable() { 
	    					public void run() {
	    						Utils.showMessageAndExit(InitiateGeolocSharing.this, getString(R.string.label_invitation_failed));
		    				}
		    			});
	            	}
            	}
            };
            thread.start();

            // Display a progress dialog
            progressDialog = Utils.showProgressDialog(InitiateGeolocSharing.this, getString(R.string.label_command_in_progress));            
            progressDialog.setOnCancelListener(new OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					Toast.makeText(InitiateGeolocSharing.this, getString(R.string.label_geoloc_sharing_canceled), Toast.LENGTH_SHORT).show();
					quitSession();
				}
			});
            
            // Disable UI
            spinner.setEnabled(false);

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
				geoloc = data.getParcelableExtra("geoloc"); 
				
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
     * File transfer session event listener
     */
    private IGeolocSharingEventListener geolocSessionListener = new IGeolocSharingEventListener.Stub() {

    	// Geoloc has been transfered
		public void handleGeolocTransfered(GeolocPush geoloc) {
			handler.post(new Runnable() { 
				public void run() {
					// Hide progress dialog
					hideProgressDialog();

					// Display session status
					TextView statusView = (TextView)findViewById(R.id.progress_status);
					statusView.setText("transfered");
				}
			});
			
		}

		// Session has been aborted
		public void handleSessionAborted(int reason) {
			handler.post(new Runnable() { 
				public void run() {
					// Hide progress dialog
					hideProgressDialog();
					
					// Display session status
					Utils.showMessageAndExit(InitiateGeolocSharing.this, getString(R.string.label_sharing_aborted));
				}
			});
		}

		// Session is started
		public void handleSessionStarted() throws RemoteException {
			handler.post(new Runnable() { 
				public void run() {
					// Hide progress dialog
					hideProgressDialog();
					
					// Display session status
					TextView statusView = (TextView)findViewById(R.id.progress_status);
					statusView.setText("started");
				}
			});
			
		}

		// Session has been terminated by remote
		public void handleSessionTerminatedByRemote() {
			handler.post(new Runnable() { 
				public void run() {
					// Hide progress dialog
					hideProgressDialog();
					
					// Display session status
					Utils.showMessageAndExit(InitiateGeolocSharing.this, getString(R.string.label_sharing_terminated_by_remote));
				}
			});
		}
		
		// Content sharing error
		public void handleSharingError(final int error) {
			handler.post(new Runnable() { 
				public void run() {
					// Hide progress dialog
					hideProgressDialog();
					
					// Display session status
					TextView statusView = (TextView)findViewById(R.id.progress_status);
					statusView.setText("error");
					if (error == ContentSharingError.SESSION_INITIATION_DECLINED) {
    					Utils.showMessage(InitiateGeolocSharing.this, getString(R.string.label_invitation_declined));
					} else {
    					Utils.showMessageAndExit(InitiateGeolocSharing.this, getString(R.string.label_csh_failed, error));
					}
				}
			});
		}

    };
    
    /**
     * Quit the session
     */
    private void quitSession() {
		// Stop session
	    Thread thread = new Thread() {
	    	public void run() {
	        	try {
	                if (sharingSession != null) {
	                	sharingSession.cancelSession();
	                	sharingSession.removeSessionListener(geolocSessionListener);
	                }
	        	} catch(Exception e) {
	        	}
	        	sharingSession = null;
	    	}
	    };
	    thread.start();
		
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
