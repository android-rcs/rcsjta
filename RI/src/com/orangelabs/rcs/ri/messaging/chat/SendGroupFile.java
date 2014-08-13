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

import com.gsma.services.rcs.JoynService;
import com.gsma.services.rcs.JoynServiceException;
import com.gsma.services.rcs.JoynServiceListener;
import com.gsma.services.rcs.chat.ChatService;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.ft.FileTransfer;
import com.gsma.services.rcs.ft.FileTransferService;
import com.gsma.services.rcs.ft.GroupFileTransferListener;
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
public class SendGroupFile extends Activity implements JoynServiceListener {
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
     * Chat ID 
     */
	private String chatId;

    /**
	 * Chat API
	 */
	private ChatService chatApi;

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
    
    /**
   	 * The log tag for this class
   	 */
   	private static final String LOGTAG = LogUtils.getTag(SendGroupFile.class.getSimpleName());
    
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
     * Progress dialog
     */
    private Dialog progressDialog;
    
    private boolean serviceConnected = false;
    
    /**
	 * A locker to exit only once
	 */
	private LockAccess exitOnce = new LockAccess();
    
    /**
     * File transfer listener
     */
	private GroupFileTransferListener ftListener = new GroupFileTransferListener() {

		@Override
		public void onSingleRecipientDeliveryStateChanged(String chatId, ContactId contact, String transferId, int state) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onSingleRecipientDeliveryStateChanged chatId=" + chatId + " contact=" + contact + " trasnferId="
						+ transferId + " state=" + state);
			}
		}

		@Override
		public void onTransferProgress(String chatId, String transferId, final long currentSize, final long totalSize) {
			handler.post(new Runnable() {
				public void run() {
					// Display transfer progress
					updateProgressBar(currentSize, totalSize);
				}
			});
		}

		@Override
		public void onTransferStateChanged(String chatId, String transferId, final int state) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onTransferStateChanged chatId=" + chatId + " transferId=" + transferId + " state=" + state);
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
						// Display session status started
						statusView.setText("started");
						break;

					case FileTransfer.State.TRANSFERRED:
						// Hide progress dialog
						hideProgressDialog();
						// Display session status transferred
						statusView.setText("transferred");
						break;

					case FileTransfer.State.ABORTED:
						// Session is aborted: hide progress dialog then exit
						// Hide progress dialog
						hideProgressDialog();
						// Display message
						Utils.showMessageAndExit(SendGroupFile.this, getString(R.string.label_transfer_aborted), exitOnce);
						break;

					// TODO: Add states
					// case FileTransfer.State.REJECTED:
					// // Hide progress dialog
					// hideProgressDialog();
					// Utils.showMessageAndExit(SendGroupFile.this, getString(R.string.label_transfer_declined), exitOnce);
					// break;

					case FileTransfer.State.FAILED:
						// Session is failed: exit
						// Hide progress dialog
						hideProgressDialog();
						Utils.showMessageAndExit(SendGroupFile.this, getString(R.string.label_transfer_failed, reason), exitOnce);
						break;

					default:
						statusView.setText(notif);
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
        selectBtn.setEnabled(false);
               
        // Instantiate API
        chatApi = new ChatService(getApplicationContext(), null);
        ftApi = new FileTransferService(getApplicationContext(), this);
        
        // Connect API
        chatApi.connect();
        ftApi.connect();
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();

		if (serviceConnected) {
			// Disconnect chat API
			chatApi.disconnect();
			// Remove Group file listener
			try {
				ftApi.removeGroupFileTransferListener(ftListener);
			} catch (JoynServiceException e) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "Failed to remove listener", e);
				}
			}
			// Disconnect File Transfer API
			ftApi.disconnect();
		}
    }
    
    /**
     * Callback called when service is connected. This method is called when the
     * service is well connected to the RCS service (binding procedure successful):
     * this means the methods of the API may be used.
     */
    public void onServiceConnected() {
        try {
            // Enable thumbnail option if supported
            CheckBox ftThumb = (CheckBox)findViewById(R.id.ft_thumb);
	        if (ftApi.getConfiguration().isFileIconSupported()) {
	        	ftThumb.setEnabled(true);
	        }

	        Button selectBtn = (Button)findViewById(R.id.select_btn);
	        selectBtn.setEnabled(true);
	        
	        // Add group file listener
			ftApi.addGroupFileTransferListener(ftListener);
			
			serviceConnected = true;
			
        } catch(Exception e) {
        	if (LogUtils.isActive) {
				Log.e(LOGTAG, "Failed to connect", e);
			}
			Utils.showMessageAndExit(SendGroupFile.this, getString(R.string.label_api_failed), exitOnce);
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
		Utils.showMessageAndExit(SendGroupFile.this, getString(R.string.label_api_disabled), exitOnce);
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
			registered = ftApi.isServiceRegistered();
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
			GroupChat groupChat = chatApi.getGroupChat(chatId);
			if (groupChat == null) {
				Utils.showMessageAndExit(SendGroupFile.this, getString(R.string.label_chat_aborted), exitOnce);
				return;
			}
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "initiateTransfer filename=" + filename + " size=" + filesize);
			}
			// Initiate transfer
			fileTransfer = ftApi.transferFileToGroupChat(chatId, file, ftThumb.isChecked());
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
			Intent pictureShareIntent = new Intent(Intent.ACTION_GET_CONTENT, null);
			pictureShareIntent.setType("image/*");
			startActivityForResult(pictureShareIntent, SELECT_IMAGE);
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
