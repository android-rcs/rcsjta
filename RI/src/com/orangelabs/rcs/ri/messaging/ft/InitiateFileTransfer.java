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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.ContactUtils;
import com.gsma.services.rcs.ft.FileTransfer;
import com.gsma.services.rcs.ft.OneToOneFileTransferListener;
import com.orangelabs.rcs.ri.ApiConnectionManager;
import com.orangelabs.rcs.ri.ApiConnectionManager.RcsServiceName;
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
public class InitiateFileTransfer extends Activity {
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
     * File transfer
     */
    private FileTransfer fileTransfer;
    
    /**
	 * A locker to exit only once
	 */
	private LockAccess exitOnce = new LockAccess();
    
    /**
     * Progress dialog
     */
    private Dialog progressDialog;
    
   	/**
	 * API connection manager
	 */
	private ApiConnectionManager connectionManager;
	
    /**
   	 * The log tag for this class
   	 */
   	private static final String LOGTAG = LogUtils.getTag(InitiateFileTransfer.class.getSimpleName());

    /**
     * File transfer is resuming
     */
    private boolean resuming = false;
    
    /**
     * File transfer identifier
     */
    private String ftId;
   
	/**
	 * File transfer listener
	 */
	private OneToOneFileTransferListener ftListener = new OneToOneFileTransferListener() {

		@Override
		public void onProgressUpdate(ContactId contact, String transferId, final long currentSize, final long totalSize) {
			// Discard event if not for current transferId
			if (InitiateFileTransfer.this.ftId == null || !InitiateFileTransfer.this.ftId.equals(transferId)) {
				return;
			}
			handler.post(new Runnable() {
				public void run() {
					// Display transfer progress
					updateProgressBar(currentSize, totalSize);
				}
			});
		}

		@Override
		public void onStateChanged(ContactId contact, String transferId, final int state, final int reasonCode) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onTransferStateChanged contact=" + contact + " transferId=" + transferId + " state=" + state+ " reason="+reasonCode);
			}
			if (state > RiApplication.FT_STATES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onTransferStateChanged unhandled state=" + state);
				}
				return;
			}
			if (reasonCode > RiApplication.FT_REASON_CODES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onTransferStateChanged unhandled reason=" + reasonCode);
				}
				return;
			}
			// Discard event if not for current transferId
			if (InitiateFileTransfer.this.ftId == null || !InitiateFileTransfer.this.ftId.equals(transferId)) {
				return;
			}
			final String _reasonCode = RiApplication.FT_REASON_CODES[reasonCode];
			final String _state = RiApplication.FT_STATES[state];
			handler.post(new Runnable() {
				public void run() {
					TextView statusView = (TextView) findViewById(R.id.progress_status);
					switch (state) {
					case FileTransfer.State.STARTED:
						// Session is well established : hide progress dialog
						hideProgressDialog();
						// Display session status
						statusView.setText(_state);
						break;

					case FileTransfer.State.ABORTED:
						// Session is aborted: hide progress dialog then exit
						hideProgressDialog();
						Utils.showMessageAndExit(InitiateFileTransfer.this, getString(R.string.label_transfer_aborted, _reasonCode), exitOnce);
						break;

					case FileTransfer.State.REJECTED:
						// Session is rejected: hide progress dialog then exit
						hideProgressDialog();
						Utils.showMessageAndExit(InitiateFileTransfer.this, getString(R.string.label_transfer_rejected, _reasonCode), exitOnce);
						break;

					case FileTransfer.State.FAILED:
						// Session failed: hide progress dialog then exit
						hideProgressDialog();
						Utils.showMessageAndExit(InitiateFileTransfer.this, getString(R.string.label_transfer_failed, _reasonCode), exitOnce);
						break;

					case FileTransfer.State.TRANSFERRED:
						// Hide progress dialog
						hideProgressDialog();
						// Display transfer progress
						statusView.setText(_state);
						// Hide buttons Pause and Resume
						Button pauseBtn = (Button) findViewById(R.id.pause_btn);
						pauseBtn.setVisibility(View.INVISIBLE);
						Button resumeBtn = (Button) findViewById(R.id.resume_btn);
						resumeBtn.setVisibility(View.INVISIBLE);
						break;

					default:
						statusView.setText(_state);
						if (LogUtils.isActive) {
							Log.d(LOGTAG,
									"onTransferStateChanged " + getString(R.string.label_ft_state_changed, _state, _reasonCode));
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
		}

		// Set layout
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.filetransfer_initiate);

		// Set title
		setTitle(R.string.menu_transfer_file);

		// Set contact selector
		Spinner spinner = (Spinner) findViewById(R.id.contact);
		
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
		
		// Register to API connection manager
		connectionManager = ApiConnectionManager.getInstance(this);
		if (connectionManager == null || !connectionManager.isServiceConnected(RcsServiceName.FILE_TRANSFER)) {
			Utils.showMessageAndExit(this, getString(R.string.label_service_not_available), exitOnce);
			return;
		}
		connectionManager.startMonitorServices(this, exitOnce, RcsServiceName.FILE_TRANSFER);
		try {
			// Add service listener
			connectionManager.getFileTransferApi().addEventListener(ftListener);
			if (resuming) {
				// Get resuming info
				FileTransferDAO ftdao = (FileTransferDAO) (getIntent().getExtras()
						.getSerializable(FileTransferIntentService.BUNDLE_FTDAO_ID));
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
				ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
						new String[] { remoteContact.toString() });
				spinner.setAdapter(adapter);
				TextView uriEdit = (TextView) findViewById(R.id.uri);
				TextView sizeEdit = (TextView) findViewById(R.id.size);
				sizeEdit.setText((filesize / 1024) + " KB");
				uriEdit.setText(filename);
				// Check if session still exists
				if (connectionManager.getFileTransferApi().getFileTransfer(ftId) == null) {
					// Session not found or expired
					Utils.showMessageAndExit(this, getString(R.string.label_transfer_session_has_expired),
							exitOnce);
					return;
				}
				pauseBtn.setEnabled(true);
				if (LogUtils.isActive) {
					Log.d(LOGTAG, "onCreate (file=" + filename + ") (size=" + filesize + ") (contact=" + remoteContact + ")");
				}
			} else {
				spinner.setAdapter(Utils.createRcsContactListAdapter(this));
				// Enable button if contact available
				if (spinner.getAdapter().getCount() != 0) {
					selectBtn.setEnabled(true);
				}
				if (LogUtils.isActive) {
					Log.d(LOGTAG, "onCreate");
				}
			}
		} catch (RcsServiceNotAvailableException e) {
			e.printStackTrace();
			Utils.showMessageAndExit(this, getString(R.string.label_api_disabled), exitOnce);
		} catch (RcsServiceException e) {
			e.printStackTrace();
			Utils.showMessageAndExit(this, getString(R.string.label_api_failed), exitOnce);
		}
	}
    
	@Override
	public void onDestroy() {
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "onDestroy");
		}
		super.onDestroy();
		if (connectionManager == null) {
			return;
		}
		connectionManager.stopMonitorServices(this);
		if (connectionManager.isServiceConnected(RcsServiceName.FILE_TRANSFER)) {
			// Remove file transfer listener
			try {
				connectionManager.getFileTransferApi().removeEventListener(ftListener);
			} catch (Exception e) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "Failed to remove listener", e);
				}
			}
		}
	}
    
    /**
     * Invite button listener
     */
    private OnClickListener btnInviteListener = new OnClickListener() {
        public void onClick(View v) {
        	long warnSize = 0;
        	try {
        		warnSize = connectionManager.getFileTransferApi().getConfiguration().getWarnSize();
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
			registered = connectionManager.getFileTransferApi().isServiceRegistered();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (!registered) {
			Utils.showMessage(this, getString(R.string.label_service_not_available));
			return;
		}    	    	
    	
    	// Get remote contact
        Spinner spinner = (Spinner)findViewById(R.id.contact);
        MatrixCursor cursor = (MatrixCursor)spinner.getSelectedItem();
        ContactUtils contactUtils = ContactUtils.getInstance(this);
        ContactId remote;
		try {
			remote = contactUtils.formatContact(cursor.getString(1));
		} catch (RcsContactFormatException e1) {
			Utils.showMessage(this, getString(R.string.label_invalid_contact,cursor.getString(1)));
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
    		fileTransfer = connectionManager.getFileTransferApi().transferFile(remote, file, tryToSendFileicon);
    		ftId = fileTransfer.getTransferId();
    		
            Button pauseBtn = (Button)findViewById(R.id.pause_btn);
            pauseBtn.setEnabled(true);
            
            // Display a progress dialog
            progressDialog = Utils.showProgressDialog(this, getString(R.string.label_command_in_progress));
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
    		if (LogUtils.isActive) {
				Log.e(LOGTAG, "Failed to transfer file", e);
			}
			hideProgressDialog();
			Utils.showMessageAndExit(this, getString(R.string.label_invitation_failed), exitOnce);
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
				if (which == SELECT_IMAGE) {
					FileUtils.openFile(InitiateFileTransfer.this, "image/*", SELECT_IMAGE);
				} else {
					FileUtils.openFile(InitiateFileTransfer.this, "text/plain", SELECT_TEXT_FILE);
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
			} catch (RcsServiceException e) {
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
			} catch (RcsServiceException e) {
				e.printStackTrace();
				hideProgressDialog();
				Utils.showMessageAndExit(InitiateFileTransfer.this, getString(R.string.label_resume_failed), exitOnce);
			}
		}
	};
}
