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

import com.gsma.services.rcs.JoynContactFormatException;
import com.gsma.services.rcs.JoynService;
import com.gsma.services.rcs.JoynServiceException;
import com.gsma.services.rcs.JoynServiceListener;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.ContactUtils;
import com.gsma.services.rcs.ish.ImageSharing;
import com.gsma.services.rcs.ish.ImageSharingListener;
import com.gsma.services.rcs.ish.ImageSharingService;
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
public class InitiateImageSharing extends Activity implements JoynServiceListener {
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
	 * Image sharing API
	 */
    private ImageSharingService ishApi;
    
	/**
     * Image sharing
     */
    private ImageSharing imageSharing;
    
	/**
	 * Array of Image sharing states
	 */
	private static final String[] ISH_STATES = RiApplication.getContext().getResources().getStringArray(R.array.ish_states);

	/**
	 * Array of Image sharing reason codes
	 */
	private static final String[] ISH_REASON_CODES = RiApplication.getContext().getResources()
			.getStringArray(R.array.ish_reason_codes);
	
	/**
     * Progress dialog
     */
    private Dialog progressDialog;
    
    private boolean serviceConnected = false;
    
    /**
   	 * A locker to exit only once
   	 */
   	private LockAccess exitOnce = new LockAccess();
    
    /**
   	 * The log tag for this class
   	 */
   	private static final String LOGTAG = LogUtils.getTag(InitiateImageSharing.class.getSimpleName());
   	
    /**
     * Image sharing listener
     */
	private ImageSharingListener ishListener = new ImageSharingListener() {

		@Override
		public void onImageSharingProgress(ContactId contact, String sharingId, final long currentSize, final long totalSize) {
			handler.post(new Runnable() {
				public void run() {
					// Display sharing progress
					updateProgressBar(currentSize, totalSize);
				}
			});
		}

		@Override
		public void onImageSharingStateChanged(ContactId contact, String sharingId, final int state) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onImageSharingStateChanged contact=" + contact + " sharingId=" + sharingId + " state=" + state);
			}
			if (state > ISH_STATES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onImageSharingStateChanged unhandled state=" + state);
				}
				return;
			}
			// TODO : handle reason code (CR025)
			final String reason = ISH_REASON_CODES[0];
			final String notif = getString(R.string.label_ish_state_changed, ISH_STATES[state], reason);
			handler.post(new Runnable() {
				public void run() {
					TextView statusView = (TextView) findViewById(R.id.progress_status);
					switch (state) {
					case ImageSharing.State.STARTED:
						// Session is established: hide progress dialog
						hideProgressDialog();
						// Display session status
						statusView.setText("started");
						break;

					case ImageSharing.State.ABORTED:
						// Session is aborted: hide progress dialog then exit
						// Hide progress dialog
						hideProgressDialog();
						// Display session status
						Utils.showMessageAndExit(InitiateImageSharing.this, getString(R.string.label_sharing_aborted, reason), exitOnce);
						break;

					// Add states
					// case ImageSharing.State.REJECTED:
					// Hide progress dialog
					// hideProgressDialog();
					// Utils.showMessageAndExit(InitiateImageSharing.this, getString(R.string.label_sharing_declined));
					// break;

					case ImageSharing.State.FAILED:
						// Session is failed: exit
						// Hide progress dialog
						hideProgressDialog();
						Utils.showMessageAndExit(InitiateImageSharing.this, getString(R.string.label_sharing_failed, reason), exitOnce);
						break;

					case ImageSharing.State.TRANSFERRED:
						// Hide progress dialog
						hideProgressDialog();
						// Display transfer progress
						statusView.setText("transferred");
						break;

					default:
						if (LogUtils.isActive) {
							Log.d(LOGTAG, "onImageSharingStateChanged " + notif);
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

        // Instantiate API
		ishApi = new ImageSharingService(getApplicationContext(), this);
		
		// Connect API
		ishApi.connect();
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
		if (serviceConnected) {
			// Remove image sharing listener
			try {
				ishApi.removeEventListener(ishListener);
			} catch (Exception e) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "Failed to remove listener", e);
				}
			}
			// Disconnect API
			ishApi.disconnect();
		}
    }
    
    /**
     * Callback called when service is connected. This method is called when the
     * service is well connected to the RCS service (binding procedure successful):
     * this means the methods of the API may be used.
     */
    public void onServiceConnected() {
        // Disable button if no contact available
        Spinner spinner = (Spinner)findViewById(R.id.contact);
        Button dialBtn = (Button)findViewById(R.id.dial_btn);
        Button selectBtn = (Button)findViewById(R.id.select_btn);
        if (spinner.getAdapter().getCount() != 0) {
        	dialBtn.setEnabled(true);
        	selectBtn.setEnabled(true);
        }
		// Add service listener
		try {
			ishApi.addEventListener(ishListener);
			serviceConnected = true;
		} catch (JoynServiceException e) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Failed to add listener", e);
			}
			Utils.showMessageAndExit(InitiateImageSharing.this, getString(R.string.label_api_failed), exitOnce);
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
    	serviceConnected = false;
		Utils.showMessageAndExit(InitiateImageSharing.this, getString(R.string.label_api_disabled), exitOnce);
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
        		registered = ishApi.isServiceRegistered();
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
    			remote = contactUtils.formatContactId(cursor.getString(1));
    		} catch (JoynContactFormatException e1) {
    			Utils.showMessage(InitiateImageSharing.this, getString(R.string.label_invalid_contact,cursor.getString(1)));
    	    	return;
    		}
    		
        	try {
                if (LogUtils.isActive) {
    				Log.d(LOGTAG, "shareImage image="+filename+" size="+filesize);
    			}
                // Initiate sharing
        		imageSharing = ishApi.shareImage(remote, file);
        	} catch(Exception e) {
        		e.printStackTrace();
				hideProgressDialog();
				Utils.showMessageAndExit(InitiateImageSharing.this, getString(R.string.label_invitation_failed), exitOnce);
        	}

            // Display a progress dialog
            progressDialog = Utils.showProgressDialog(InitiateImageSharing.this, getString(R.string.label_command_in_progress));            
            progressDialog.setOnCancelListener(new OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					Toast.makeText(InitiateImageSharing.this, getString(R.string.label_sharing_cancelled), Toast.LENGTH_SHORT).show();
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
     * Select image button listener
     */
    private OnClickListener btnSelectListener = new OnClickListener() {
        public void onClick(View v) {
        	// Select a picture from the gallery
        	Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            Intent wrapperIntent = Intent.createChooser(intent, null);
            startActivityForResult(wrapperIntent, SELECT_IMAGE);
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
