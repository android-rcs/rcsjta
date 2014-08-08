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

package com.orangelabs.rcs.ri.messaging.ft;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract.Data;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
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
import com.gsma.services.rcs.ft.FileTransfer;
import com.gsma.services.rcs.ft.FileTransferListener;
import com.gsma.services.rcs.ft.FileTransferService;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.utils.FileUtils;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Initiate file transfer
 * 
 * @author Jean-Marc AUFFRET
 * @author Philippe LEMORDANT
 */
public class InitiateFileTransfer extends Activity implements JoynServiceListener {
	/**
	 * Activity result constants
	 */
	private final static int SELECT_IMAGE = 0;
	private final static int SELECT_TEXT_FILE = 1;
	
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
	 * File transfer API
	 */
    private FileTransferService ftApi;
    
    /**
     * File transfer
     */
    private FileTransfer fileTransfer;
    
    private boolean serviceConnected = false;
    
    /**
	 * A locker to exit only once
	 */
	private LockAccess exitOnce = new LockAccess();
    
    /**
     * Progress dialog
     */
    private Dialog progressDialog;
    
	/**
	 * Array of file transfer states
	 */
	private static final String[] FT_STATES = RiApplication.getContext().getResources()
			.getStringArray(R.array.file_transfer_states);

	/**
	 * Array of file transfer reason codes
	 */
	private static final String[] FT_REASON_CODES = RiApplication.getContext().getResources()
			.getStringArray(R.array.file_transfer_reason_codes);
	
    /**
   	 * The log tag for this class
   	 */
   	private static final String LOGTAG = LogUtils.getTag(InitiateFileTransfer.class.getSimpleName());

    /**
     * File transfer is resuming
     */
    private boolean resuming = false;
    
    private String ftId;
   
	/**
	 * File transfer listener
	 */
	private FileTransferListener ftListener = new FileTransferListener() {

		@Override
		public void onTransferProgress(ContactId contact, String transferId, final long currentSize, final long totalSize) {
			handler.post(new Runnable() {
				public void run() {
					// Display transfer progress
					updateProgressBar(currentSize, totalSize);
				}
			});
		}

		@Override
		public void onTransferStateChanged(ContactId contact, String transferId, final int state) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onTransferStateChanged contact=" + contact + " transferId=" + transferId + " state=" + state);
			}
			if (state > FT_STATES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onTransferStateChanged unhandled state=" + state);
				}
				return;
			}
			// TODO : handle reason code (CR025)
			final String reason = FT_REASON_CODES[0];
			final String notif = getString(R.string.label_ft_state_changed, FT_STATES[state], reason);
			handler.post(new Runnable() {
				public void run() {
					TextView statusView = (TextView) findViewById(R.id.progress_status);
					switch (state) {
					case FileTransfer.State.STARTED:
						// Session is well established : hide progress dialog
						hideProgressDialog();
						// Display session status
						statusView.setText("started");
						break;

					case FileTransfer.State.ABORTED:
						// Session is aborted: hide progress dialog then exit
						// Hide progress dialog
						hideProgressDialog();
						// Display message
						Utils.showMessageAndExit(InitiateFileTransfer.this, getString(R.string.label_transfer_aborted, reason), exitOnce);
						break;

					// Add states
					// case FileTransfer.State.REJECTED:
					// Utils.showMessageAndExit(InitiateFileTransfer.this, getString(R.string.label_transfer_declined));
					// break;

					case FileTransfer.State.FAILED:
						// Session is failed: exit
						Utils.showMessageAndExit(InitiateFileTransfer.this, getString(R.string.label_transfer_failed, reason), exitOnce);
						break;

					case FileTransfer.State.TRANSFERRED:
						// Hide progress dialog
						hideProgressDialog();
						// Display transfer progress
						statusView.setText("transferred");
						// Hide buttons Pause and Resume
						Button pauseBtn = (Button) findViewById(R.id.pause_btn);
						pauseBtn.setVisibility(View.INVISIBLE);
						Button resumeBtn = (Button) findViewById(R.id.resume_btn);
						resumeBtn.setVisibility(View.INVISIBLE);
						break;

					default:
						if (LogUtils.isActive) {
							Log.d(LOGTAG, "onTransferStateChanged " + notif);
						}
					}
				}
			});
		}
	};
	
    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ContactId remoteContact = null;
		if (getIntent().getAction() != null) {
			resuming = getIntent().getAction().equals(FileTransferResumeReceiver.ACTION_FT_RESUME);
			if (resuming) {
				// Get resuming info
				FileTransferDAO ftdao =  (FileTransferDAO)(getIntent().getExtras().getSerializable(FileTransferIntentService.BUNDLE_FTDAO_ID));
				if (ftdao == null) {
					if (LogUtils.isActive) {
						Log.e(LOGTAG, "onCreate cannot read File Transfer resuming info");
					}
					finish();
					return;
				}
				remoteContact = ftdao.getContact();
				ftId = ftdao.getTransferId();
				filename = ftdao.getFilename();
				filesize = ftdao.getSize();
			}
		}

		// Set layout
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.filetransfer_initiate);

		// Set title
		setTitle(R.string.menu_transfer_file);

		// Set contact selector
		Spinner spinner = (Spinner) findViewById(R.id.contact);
		spinner.setAdapter(Utils.createRcsContactListAdapter(this));

		// Set buttons callback
		Button inviteBtn = (Button) findViewById(R.id.invite_btn);
		inviteBtn.setOnClickListener(btnInviteListener);
		inviteBtn.setEnabled(false);
		Button selectBtn = (Button) findViewById(R.id.select_btn);
		selectBtn.setOnClickListener(btnSelectListener);
		selectBtn.setEnabled(false);

		Button pauseBtn = (Button) findViewById(R.id.pause_btn);
		pauseBtn.setOnClickListener(btnPauseListener);
		pauseBtn.setEnabled(false);

		Button resumeBtn = (Button) findViewById(R.id.resume_btn);
		resumeBtn.setOnClickListener(btnResumeListener);
		resumeBtn.setEnabled(false);

		// Instantiate API
		ftApi = new FileTransferService(getApplicationContext(), this);

		// Connect API
		ftApi.connect();

		if (resuming) {
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
					new String[] { remoteContact.toString() });
			spinner.setAdapter(adapter);
			TextView uriEdit = (TextView) findViewById(R.id.uri);
			TextView sizeEdit = (TextView) findViewById(R.id.size);
			sizeEdit.setText((filesize / 1024) + " KB");
			uriEdit.setText(filename);
		} else {
			// Select the corresponding contact from the intent
			Intent intent = getIntent();
			Uri contactUri = intent.getData();
			if (contactUri != null) {
				Cursor cursor = managedQuery(contactUri, null, null, null, null);
				if (cursor.moveToNext()) {
					String selectedContact = cursor.getString(cursor.getColumnIndex(Data.DATA1));
					if (selectedContact != null) {
						for (int i = 0; i < spinner.getAdapter().getCount(); i++) {
							MatrixCursor cursor2 = (MatrixCursor) spinner.getAdapter().getItem(i);
							if (PhoneNumberUtils.compare(selectedContact, cursor2.getString(1))) {
								// Select contact
								spinner.setSelection(i);
								spinner.setEnabled(false);
								break;
							}
						}
					}
				}
				cursor.close();
			}
		}
		if (LogUtils.isActive) {
			if (resuming) {
				Log.d(LOGTAG, "onCreate (filename=" + filename + ") (filesize=" + filesize + ") (remoteContact=" + remoteContact
						+ ")");
			} else {
				Log.d(LOGTAG, "onCreate");
			}
		}
	}
    
	@Override
	public void onDestroy() {
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "onDestroy");
		}
		super.onDestroy();
		if (serviceConnected) {
			// Remove file transfer listener
			try {
				ftApi.removeOneToOneFileTransferListener(ftListener);
			} catch (Exception e) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "Failed to remove listener", e);
				}
			}
			// Disconnect API
			ftApi.disconnect();
		}
	}
    
    /**
     * Callback called when service is connected. This method is called when the
     * service is well connected to the RCS service (binding procedure successfull):
     * this means the methods of the API may be used.
     */
    public void onServiceConnected() {
		try {
			// Add service listener
			ftApi.addOneToOneFileTransferListener(ftListener);
			serviceConnected = true;
			
			if (resuming) {

				// Get the file transfer session
				fileTransfer = ftApi.getFileTransfer(ftId);
				if (fileTransfer == null) {
					// Session not found or expired
					Utils.showMessageAndExit(InitiateFileTransfer.this, getString(R.string.label_transfer_session_has_expired), exitOnce);
					return;
				}

				Button pauseBtn = (Button) findViewById(R.id.pause_btn);
				pauseBtn.setEnabled(true);

			} else {
				// Enable button if contact available
				Spinner spinner = (Spinner) findViewById(R.id.contact);
				if (spinner.getAdapter().getCount() != 0) {
					Button selectBtn = (Button) findViewById(R.id.select_btn);
					selectBtn.setEnabled(true);
				}
			}
		} catch (Exception e) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Exception occurred", e);
			}
			Utils.showMessageAndExit(InitiateFileTransfer.this, getString(R.string.label_api_failed), exitOnce);
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
		Utils.showMessageAndExit(InitiateFileTransfer.this, getString(R.string.label_api_disabled), exitOnce);
    }   
    
    /**
     * Invite button listener
     */
    private OnClickListener btnInviteListener = new OnClickListener() {
        public void onClick(View v) {
        	long warnSize = 0;
        	try {
        		warnSize = ftApi.getConfiguration().getWarnSize();
        	} catch(Exception e) {
        		e.printStackTrace();
        	}
        	
            if ((warnSize > 0) && (filesize >= warnSize)) {
				// Display a warning message
            	AlertDialog.Builder builder = new AlertDialog.Builder(InitiateFileTransfer.this);
            	builder.setMessage(getString(R.string.label_sharing_warn_size, filesize));
            	builder.setCancelable(false);
            	builder.setPositiveButton(getString(R.string.label_yes), new DialogInterface.OnClickListener() {
                	public void onClick(DialogInterface dialog, int position) {
                		initiateTransfer();
                	}
        		});	                    			
            	builder.setNegativeButton(getString(R.string.label_no), null);
                AlertDialog alert = builder.create();
            	alert.show();
            } else {
            	initiateTransfer();
            }
    	}
	};
      
	/**
	 * Initiate transfer
	 */
    private void initiateTransfer() {
		// Check if the service is available
		boolean registered = false;
		try {
			registered = ftApi.isServiceRegistered();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (!registered) {
			Utils.showMessage(InitiateFileTransfer.this, getString(R.string.label_service_not_available));
			return;
		}    	    	
    	
    	// Get remote contact
        Spinner spinner = (Spinner)findViewById(R.id.contact);
        MatrixCursor cursor = (MatrixCursor)spinner.getSelectedItem();
        ContactUtils contactUtils = ContactUtils.getInstance(this);
        ContactId remote;
		try {
			remote = contactUtils.formatContactId(cursor.getString(1));
		} catch (JoynContactFormatException e1) {
			Utils.showMessage(InitiateFileTransfer.this, getString(R.string.label_invalid_contact,cursor.getString(1)));
	    	return;
		}

        // Get thumbnail option
        CheckBox ftThumb = (CheckBox)findViewById(R.id.ft_thumb);
        
        try {
        	boolean tryToSendFileicon = ftThumb.isChecked();
        	String mimeType = getContentResolver().getType(file);
        	if (tryToSendFileicon && mimeType != null && !mimeType.toLowerCase().startsWith("image")) {
        		tryToSendFileicon = false;
        	}
    		// Initiate transfer
    		fileTransfer = ftApi.transferFile(remote, file, tryToSendFileicon);
    		
            Button pauseBtn = (Button)findViewById(R.id.pause_btn);
            pauseBtn.setEnabled(true);
            
            // Display a progress dialog
            progressDialog = Utils.showProgressDialog(InitiateFileTransfer.this, getString(R.string.label_command_in_progress));
            progressDialog.setOnCancelListener(new OnCancelListener() {
    			public void onCancel(DialogInterface dialog) {
    				Toast.makeText(InitiateFileTransfer.this, getString(R.string.label_transfer_cancelled), Toast.LENGTH_SHORT).show();
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
            ftThumb.setVisibility(View.INVISIBLE);
    	} catch(Exception e) {
    		e.printStackTrace();
			hideProgressDialog();
			Utils.showMessageAndExit(InitiateFileTransfer.this, getString(R.string.label_invitation_failed), exitOnce);
    	}
    }
       
    /**
     * Select file button listener
     */
    private OnClickListener btnSelectListener = new OnClickListener() {
        public void onClick(View v) {
        	selectDocument();
        }
    };
        
    /**
     * Display a alert dialog to select the kind of file to transfer
     */
	private void selectDocument() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.label_select_file);
		builder.setCancelable(true);
		builder.setItems(R.array.select_filetotransfer, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent intent;
				if (Build.VERSION.SDK_INT < 19) { // Build.VERSION_CODES.KITKAT
					intent = new Intent(Intent.ACTION_GET_CONTENT, null);
				} else {
					intent = new Intent("android.intent.action.OPEN_DOCUMENT");
				}
				intent.addCategory(Intent.CATEGORY_OPENABLE);
				if (which == SELECT_IMAGE) {
					intent.setType("image/*");
					startActivityForResult(intent, SELECT_IMAGE);
				} else {
					intent.setType("text/plain");
					startActivityForResult(intent, SELECT_TEXT_FILE);
				}
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}
	
    /**
     * On activity result
     * 
     * @param requestCode Request code
     * @param resultCode Result code
     * @param data Data
     */
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK || (data == null) || (data.getData() == null)) {
			return;
		}
		file = data.getData();
		TextView uriEdit = (TextView) findViewById(R.id.uri);
		TextView sizeEdit = (TextView) findViewById(R.id.size);
		Button inviteBtn = (Button) findViewById(R.id.invite_btn);
		switch (requestCode) {
		case SELECT_IMAGE:
		case SELECT_TEXT_FILE:
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "Selected file uri:" + file);
			}
			// Display file info
			// Display the selected filename attribute
			try {
				// Get image filename and size
				filename = FileUtils.getFileName(this, file);
				filesize = FileUtils.getFileSize(this, file) / 1024;
				sizeEdit.setText(filesize + " KB");
				uriEdit.setText(filename);
				if (LogUtils.isActive) {
					Log.i(LOGTAG, "Select file " + filename + " of size " + filesize + " file=" + file);
				}
			} catch (Exception e) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, e.getMessage(), e);
				}
				filesize = -1;
				sizeEdit.setText("Unknown");
				uriEdit.setText("Unknown");
			}
			// Enable invite button
			inviteBtn.setEnabled(true);
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
     * Show the transfer progress
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
    	if (LogUtils.isActive) {
			Log.d(LOGTAG, "quitSession");
		}
		// Stop session
    	try {
            if (fileTransfer != null) {
            	fileTransfer.abortTransfer();
            }
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	fileTransfer = null;
    	
        // Exit activity
		finish();
    }    

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
				// Quit session
            	quitSession();
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }    

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater=new MenuInflater(getApplicationContext());
		inflater.inflate(R.menu.menu_ft, menu);
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
    
    /**
     * Pause button listener
     */
	private OnClickListener btnPauseListener = new OnClickListener() {
		public void onClick(View v) {
			Button resumeBtn = (Button) findViewById(R.id.resume_btn);
			resumeBtn.setEnabled(true);
			Button pauseBtn = (Button) findViewById(R.id.pause_btn);
			pauseBtn.setEnabled(false);
			try {
				fileTransfer.pauseTransfer();
			} catch (JoynServiceException e) {
				e.printStackTrace();
				hideProgressDialog();
				Utils.showMessageAndExit(InitiateFileTransfer.this, getString(R.string.label_pause_failed), exitOnce);
			}
		}
	};
	
	 /**
     * Resume button listener
     */
	private OnClickListener btnResumeListener = new OnClickListener() {
		public void onClick(View v) {
			Button resumeBtn = (Button) findViewById(R.id.resume_btn);
			resumeBtn.setEnabled(false);
			Button pauseBtn = (Button) findViewById(R.id.pause_btn);
			pauseBtn.setEnabled(true);
			try {
				fileTransfer.resumeTransfer();
			} catch (JoynServiceException e) {
				e.printStackTrace();
				hideProgressDialog();
				Utils.showMessageAndExit(InitiateFileTransfer.this, getString(R.string.label_resume_failed), exitOnce);
			}
		}
	};
}
