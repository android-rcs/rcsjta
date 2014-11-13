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

package com.orangelabs.rcs.ri.messaging.chat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
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
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.ft.FileTransfer;
import com.gsma.services.rcs.ft.FileTransferService;
import com.gsma.services.rcs.ft.GroupFileTransferListener;
import com.orangelabs.rcs.ri.ApiConnectionManager;
import com.orangelabs.rcs.ri.ApiConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.utils.FileUtils;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Send file to group
 * 
 * @author jexa7410
 * @author Philippe LEMORDANT
 */
public class SendGroupFile extends Activity {
	/**
	 * Intent parameters
	 */
	/* package private */ final static String EXTRA_CHAT_ID = "chat_id";

	/**
	 * Activity result constants
	 */
	private final static int SELECT_IMAGE = 0;
	
	/**
	 * UI handler
	 */
	private final Handler handler = new Handler();

    /**
     * CHAT ID 
     */
	private String chatId;

	/**
	 * Transfer Id
	 */
	private String transferId;
	
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
	 * API connection manager
	 */
	private ApiConnectionManager connectionManager;
    
    /**
     * File transfer
     */
    private FileTransfer fileTransfer;
    
    /**
   	 * The log tag for this class
   	 */
   	private static final String LOGTAG = LogUtils.getTag(SendGroupFile.class.getSimpleName());
    
    /**
     * Progress dialog
     */
    private Dialog progressDialog;
      
    /**
	 * A locker to exit only once
	 */
	private LockAccess exitOnce = new LockAccess();
    
    /**
     * File transfer listener
     */
	private GroupFileTransferListener ftListener = new GroupFileTransferListener() {

		@Override
		public void onDeliveryInfoChanged(String chatId, ContactId contact, String transferId, int state, int reasonCode) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onSingleRecipientDeliveryStateChanged chatId=" + chatId + " contact=" + contact + " trasnferId="
						+ transferId + " state=" + state+ " reason="+reasonCode);
			}
		}

		@Override
		public void onProgressUpdate(String chatId, String transferId, final long currentSize, final long totalSize) {
			// Discard event if not for current transferId
			if (SendGroupFile.this.transferId == null || !SendGroupFile.this.transferId.equals(transferId)) {
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
		public void onStateChanged(String chatId, String transferId, final int state, final int reasonCode) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onTransferStateChanged chatId=" + chatId + " transferId=" + transferId + " state=" + state
						+ " reason=" + reasonCode);
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
			if (SendGroupFile.this.transferId == null || !SendGroupFile.this.transferId.equals(transferId)) {
				return;
			}
			final String _reasonCode = RiApplication.GC_REASON_CODES[reasonCode];
			handler.post(new Runnable() {
				public void run() {
					TextView statusView = (TextView) findViewById(R.id.progress_status);
					switch (state) {
					case FileTransfer.State.STARTED:
					case FileTransfer.State.TRANSFERRED:
						// hide progress dialog
						hideProgressDialog();
						// Display transfer state started
						statusView.setText(RiApplication.FT_STATES[state]);
						break;

					case FileTransfer.State.ABORTED:
						// Transfer is aborted: hide progress dialog then exit
						hideProgressDialog();
						Utils.showMessageAndExit(SendGroupFile.this, getString(R.string.label_transfer_aborted, _reasonCode),
								exitOnce);
						break;

					case FileTransfer.State.REJECTED:
						// Transfer is rejected: hide progress dialog then exit
						hideProgressDialog();
						Utils.showMessageAndExit(SendGroupFile.this, getString(R.string.label_transfer_rejected, _reasonCode),
								exitOnce);
						break;

					case FileTransfer.State.FAILED:
						// Transfer failed: hide progress dialog then exit
						hideProgressDialog();
						Utils.showMessageAndExit(SendGroupFile.this, getString(R.string.label_transfer_failed, _reasonCode),
								exitOnce);
						break;

					default:
						statusView.setText(getString(R.string.label_ft_state_changed, RiApplication.FT_STATES[state], _reasonCode));
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
        setContentView(R.layout.chat_send_file);
        
        // Set title
        setTitle(R.string.menu_transfer_file);
        
        // Get chat ID
        chatId = getIntent().getStringExtra(GroupChatView.EXTRA_CHAT_ID);

        // Set buttons callback
        Button inviteBtn = (Button)findViewById(R.id.invite_btn);
        inviteBtn.setOnClickListener(btnInviteListener);
    	inviteBtn.setEnabled(false);
        Button selectBtn = (Button)findViewById(R.id.select_btn);
        selectBtn.setOnClickListener(btnSelectListener);
               
		// Register to API connection manager
		connectionManager = ApiConnectionManager.getInstance(this);
		if (connectionManager == null || !connectionManager.isServiceConnected(RcsServiceName.CHAT, RcsServiceName.FILE_TRANSFER, RcsServiceName.CONTACTS)) {
			Utils.showMessageAndExit(SendGroupFile.this, getString(R.string.label_service_not_available), exitOnce);
		} else {
			connectionManager
					.startMonitorServices(this, exitOnce, RcsServiceName.CHAT, RcsServiceName.FILE_TRANSFER, RcsServiceName.CONTACTS);
			FileTransferService ftApi = connectionManager.getFileTransferApi();
			try {
//				// Enable thumbnail option if supported
//				CheckBox ftThumb = (CheckBox) findViewById(R.id.ft_thumb);
//				if (ftApi.getConfiguration().isFileIconSupported()) {
//					ftThumb.setEnabled(true);
//				}

				// Add group file listener
				ftApi.addEventListener(ftListener);
			} catch (Exception e) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "API failure", e);
				}
				Utils.showMessageAndExit(SendGroupFile.this, getString(R.string.label_api_failed), exitOnce);
			}
		}
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
		if (connectionManager == null) {
			return;
		}
		connectionManager.stopMonitorServices(this);
		if (connectionManager.isServiceConnected(RcsServiceName.FILE_TRANSFER)) {
			// Remove Group file listener
			try {
				connectionManager.getFileTransferApi().removeEventListener(ftListener);
			} catch (RcsServiceException e) {
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
            	AlertDialog.Builder builder = new AlertDialog.Builder(SendGroupFile.this);
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
			Utils.showMessage(SendGroupFile.this, getString(R.string.label_service_not_available));
			return;
		}

		// Get thumbnail option
		CheckBox ftThumb = (CheckBox) findViewById(R.id.ft_thumb);
		// Initiate session in background
		try {
			// Get chat session
			GroupChat groupChat = connectionManager.getChatApi().getGroupChat(chatId);
			if (groupChat == null) {
				Utils.showMessageAndExit(SendGroupFile.this, getString(R.string.label_chat_aborted), exitOnce);
				return;
			}
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "initiateTransfer filename=" + filename + " size=" + filesize);
			}
			// Initiate transfer
			fileTransfer = connectionManager.getFileTransferApi().transferFileToGroupChat(chatId, file, ftThumb.isChecked());
			transferId = fileTransfer.getTransferId();
		} catch (Exception e) {
			hideProgressDialog();
			Utils.showMessageAndExit(SendGroupFile.this, getString(R.string.label_invitation_failed), exitOnce);
		}
        
        // Display a progress dialog
        progressDialog = Utils.showProgressDialog(SendGroupFile.this, getString(R.string.label_command_in_progress));
        progressDialog.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				Toast.makeText(SendGroupFile.this, getString(R.string.label_transfer_cancelled), Toast.LENGTH_SHORT).show();
				quitSession();
			}
		});            

        // Hide buttons
        Button inviteBtn = (Button)findViewById(R.id.invite_btn);
    	inviteBtn.setVisibility(View.INVISIBLE);
        Button selectBtn = (Button)findViewById(R.id.select_btn);
        selectBtn.setVisibility(View.INVISIBLE);
        ftThumb.setVisibility(View.INVISIBLE);
    }
       
    /**
     * Select file button listener
     */
	private OnClickListener btnSelectListener = new OnClickListener() {
		public void onClick(View v) {
			FileUtils.openFile(SendGroupFile.this, "image/*", SELECT_IMAGE);
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
				// Show invite button
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
}
