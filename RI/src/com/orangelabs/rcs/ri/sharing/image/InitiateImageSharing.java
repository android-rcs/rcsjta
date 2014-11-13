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

package com.orangelabs.rcs.ri.sharing.image;

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
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.ContactUtils;
import com.gsma.services.rcs.ish.ImageSharing;
import com.gsma.services.rcs.ish.ImageSharingListener;
import com.orangelabs.rcs.ri.ApiConnectionManager;
import com.orangelabs.rcs.ri.ApiConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.utils.FileUtils;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Initiate image sharing
 * 
 * @author Jean-Marc AUFFRET
 * @author YPLO6403
 */
public class InitiateImageSharing extends Activity {
	/**
	 * Activity result constants
	 */
	private final static int SELECT_IMAGE = 0;
	
	/**
	 * UI handler
	 */
	private final Handler handler = new Handler();
	
	/**
	 * Selected filename
	 */
	private String filename;
	
	/**
	 * Selected fileUri
	 */
	private Uri file;
	
	/**
	 * Selected filesize (kB)
	 */
	private long filesize = -1;	
	   
	/**
     * Image sharing
     */
    private ImageSharing imageSharing;
    
    /**
     * Image sharing Id
     */
    private String sharingId;
    
	/**
     * Progress dialog
     */
    private Dialog progressDialog;
    
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
   	private static final String LOGTAG = LogUtils.getTag(InitiateImageSharing.class.getSimpleName());
   	
    /**
     * Image sharing listener
     */
	private ImageSharingListener ishListener = new ImageSharingListener() {

		@Override
		public void onProgressUpdate(ContactId contact, String sharingId, final long currentSize, final long totalSize) {
			// Discard event if not for current sharingId
			if (InitiateImageSharing.this.sharingId == null || !InitiateImageSharing.this.sharingId.equals(sharingId)) {
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
		public void onStateChanged(ContactId contact, String sharingId, final int state, int reasonCode) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onImageSharingStateChanged contact=" + contact + " sharingId=" + sharingId + " state=" + state
						+ " reason=" + reasonCode);
			}
			if (state > RiApplication.ISH_STATES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onImageSharingStateChanged unhandled state=" + state);
				}
				return;
			}
			if (reasonCode > RiApplication.ISH_REASON_CODES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onImageSharingStateChanged unhandled reason=" + reasonCode);
				}
				return;
			}
			// Discard event if not for current sharingId
			if (InitiateImageSharing.this.sharingId == null || !InitiateImageSharing.this.sharingId.equals(sharingId)) {
				return;
			}
			final String _reasonCode = RiApplication.ISH_REASON_CODES[reasonCode];
			final String _state = RiApplication.ISH_STATES[state];
			handler.post(new Runnable() {
				public void run() {
					TextView statusView = (TextView) findViewById(R.id.progress_status);
					switch (state) {
					case ImageSharing.State.STARTED:
						// Session is established: hide progress dialog
						hideProgressDialog();
						// Display session status
						statusView.setText(_state);
						break;

					case ImageSharing.State.ABORTED:
						// Session is aborted: hide progress dialog then exit
						hideProgressDialog();
						Utils.showMessageAndExit(InitiateImageSharing.this, getString(R.string.label_sharing_aborted, _reasonCode), exitOnce);
						break;

					case ImageSharing.State.REJECTED:
						//  Session is rejected: hide progress dialog then exit
						hideProgressDialog();
						Utils.showMessageAndExit(InitiateImageSharing.this,
								getString(R.string.label_sharing_rejected, _reasonCode), exitOnce);
						break;

					case ImageSharing.State.FAILED:
						//  Session failed: hide progress dialog then exit
						hideProgressDialog();
						Utils.showMessageAndExit(InitiateImageSharing.this, getString(R.string.label_sharing_failed, _reasonCode), exitOnce);
						break;

					case ImageSharing.State.TRANSFERRED:
						// Hide progress dialog
						hideProgressDialog();
						// Display transfer progress
						statusView.setText(_state);
						break;

					default:
						// Display session status
						statusView.setText(_state);
						if (LogUtils.isActive) {
							Log.d(LOGTAG,
									"onImageSharingStateChanged "
											+ getString(R.string.label_ish_state_changed, _state, _reasonCode));
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
        setContentView(R.layout.image_sharing_initiate);
        
        // Set title
        setTitle(R.string.menu_initiate_image_sharing);
        
        // Set contact selector
        Spinner spinner = (Spinner)findViewById(R.id.contact);
        spinner.setAdapter(Utils.createRcsContactListAdapter(this));

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
        if (spinner.getAdapter().getCount() != 0) {
        	dialBtn.setEnabled(true);
        	selectBtn.setEnabled(true);
        }
        
        // Register to API connection manager
		connectionManager = ApiConnectionManager.getInstance(this);
		if (connectionManager == null || !connectionManager.isServiceConnected(RcsServiceName.IMAGE_SHARING)) {
			Utils.showMessageAndExit(this, getString(R.string.label_service_not_available), exitOnce);
			return;
		}
		connectionManager.startMonitorServices(this, exitOnce, RcsServiceName.IMAGE_SHARING);
		try {
			// Add service listener
			connectionManager.getImageSharingApi().addEventListener(ishListener);
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
		if (connectionManager.isServiceConnected(RcsServiceName.IMAGE_SHARING)) {
			// Remove image sharing listener
			try {
				connectionManager.getImageSharingApi().removeEventListener(ishListener);
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
            // Check if the service is available
        	boolean registered = false;
        	try {
        		registered = connectionManager.getImageSharingApi().isServiceRegistered();
        	} catch(Exception e) {
        		e.printStackTrace();
        	}
            if (!registered) {
    	    	Utils.showMessage(InitiateImageSharing.this, getString(R.string.label_service_not_available));
    	    	return;
            }    
            
            // Get the remote contact
            Spinner spinner = (Spinner)findViewById(R.id.contact);
            MatrixCursor cursor = (MatrixCursor)spinner.getSelectedItem();

            ContactUtils contactUtils = ContactUtils.getInstance(InitiateImageSharing.this);
            final ContactId remote;
    		try {
    			remote = contactUtils.formatContact(cursor.getString(1));
    		} catch (RcsContactFormatException e1) {
    			Utils.showMessage(InitiateImageSharing.this, getString(R.string.label_invalid_contact,cursor.getString(1)));
    	    	return;
    		}
            if (LogUtils.isActive) {
				Log.d(LOGTAG, "shareImage image="+filename+" size="+filesize);
			}    		
			try {
				// Initiate sharing
				imageSharing = connectionManager.getImageSharingApi().shareImage(remote, file);
				sharingId = imageSharing.getSharingId();
				
				// Display a progress dialog
				progressDialog = Utils.showProgressDialog(InitiateImageSharing.this, getString(R.string.label_command_in_progress));
				progressDialog.setOnCancelListener(new OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						Toast.makeText(InitiateImageSharing.this, getString(R.string.label_sharing_cancelled), Toast.LENGTH_SHORT)
								.show();
						quitSession();
					}
				});

				// Disable UI
				spinner.setEnabled(false);

				// Hide buttons
				Button inviteBtn = (Button) findViewById(R.id.invite_btn);
				inviteBtn.setVisibility(View.INVISIBLE);
				Button selectBtn = (Button) findViewById(R.id.select_btn);
				selectBtn.setVisibility(View.INVISIBLE);
				Button dialBtn = (Button) findViewById(R.id.dial_btn);
				dialBtn.setVisibility(View.INVISIBLE);
			} catch (Exception e) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "Failed to share image", e);
				}
				hideProgressDialog();
				Utils.showMessageAndExit(InitiateImageSharing.this, getString(R.string.label_invitation_failed), exitOnce);
			}
        }
    };
       
    /**
     * Select image button listener
     */
    private OnClickListener btnSelectListener = new OnClickListener() {
		public void onClick(View v) {
			FileUtils.openFile(InitiateImageSharing.this, "image/*", SELECT_IMAGE);
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

		switch (requestCode) {
		case SELECT_IMAGE:
			if ((data != null) && (data.getData() != null)) {
				// Get selected photo URI
				file = data.getData();
				// Display the selected filename attribute
				TextView uriEdit = (TextView) findViewById(R.id.uri);
				try {
					filename = FileUtils.getFileName(this, file);
					filesize = FileUtils.getFileSize(this, file) / 1024;
					uriEdit.setText(filesize + " KB");
				} catch (Exception e) {
					filesize = -1;
					uriEdit.setText("Unknown");
				}
				// Enable invite button
				Button inviteBtn = (Button) findViewById(R.id.invite_btn);
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
            if (imageSharing != null) {
            	imageSharing.abortSharing();
            }
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	imageSharing = null;
		
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
