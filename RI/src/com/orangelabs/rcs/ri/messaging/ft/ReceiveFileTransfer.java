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
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.ft.FileTransfer;
import com.gsma.services.rcs.ft.FileTransferService;
import com.gsma.services.rcs.ft.FileTransferServiceConfiguration;
import com.gsma.services.rcs.ft.GroupFileTransferListener;
import com.gsma.services.rcs.ft.OneToOneFileTransferListener;
import com.orangelabs.rcs.ri.ApiConnectionManager;
import com.orangelabs.rcs.ri.ApiConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsDisplayName;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Received file transfer
 * 
 * @author Jean-Marc AUFFRET
 * @author YPLO6403
 *
 */
public class ReceiveFileTransfer extends Activity {
    /**
     * UI handler
     */
    private final Handler handler = new Handler();
    
    /**
     * File transfer
     */
    private FileTransfer fileTransfer;
    
    /**
     * File transfer is resuming
     */
    private boolean resuming = false;
    
    /**
     * The File Transfer Data Object 
     */
    private FileTransferDAO ftDao;
    
    private boolean groupFileTransfer = false;
    
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
	private static final String LOGTAG = LogUtils.getTag(ReceiveFileTransfer.class.getSimpleName());
	
	private static final String VCARD_MIME_TYPE = "text/x-vcard";
	
	/**
	 * Group File transfer listener
	 */
	private GroupFileTransferListener groupFtListener = new GroupFileTransferListener() {

		@Override
		public void onDeliveryInfoChanged(String chatId, ContactId contact, String transferId, int state, int reasonCode) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onSingleRecipientDeliveryStateChanged contact=" + contact + " transferId=" + transferId + " state=" + state
						+ " reason=" + reasonCode);
			}
		}

		@Override
		public void onProgressUpdate(String chatId, String transferId, long currentSize, long totalSize) {
			// Discard event if not for current transferId
			if (!ftDao.getTransferId().equals(transferId)) {
				return;
			}
			ReceiveFileTransfer.this.onTransferProgressUpdateUI(currentSize, totalSize);
		}

		@Override
		public void onStateChanged(String chatId, String transferId, int state, int reasonCode) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onTransferStateChanged chatId=" + chatId + " transferId=" + transferId + " state=" + state+ " reason="+reasonCode);
			}
			// Discard event if not for current transferId
			if (!ftDao.getTransferId().equals(transferId)) {
				return;
			}
			ReceiveFileTransfer.this.onTransferStateChangedUpdateUI(state, reasonCode);
		}

	};

	/**
	 * File transfer listener
	 */
	private OneToOneFileTransferListener ftListener = new OneToOneFileTransferListener() {

		@Override
		public void onProgressUpdate(ContactId contact, String transferId, final long currentSize, final long totalSize) {
			// Discard event if not for current transferId
			if (!ftDao.getTransferId().equals(transferId)) {
				return;
			}
			ReceiveFileTransfer.this.onTransferProgressUpdateUI(currentSize, totalSize);
		}

		@Override
		public void onStateChanged(ContactId contact, String transferId, final int state, int reasonCode) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onTransferStateChanged contact=" + contact + " transferId=" + transferId + " state=" + state+ " reason="+reasonCode);
			}
			// Discard event if not for current transferId
			if (!ftDao.getTransferId().equals(transferId)) {
				return;
			}
			ReceiveFileTransfer.this.onTransferStateChangedUpdateUI(state, reasonCode);
		}
	};
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.filetransfer_receive);

		// Set pause and resume button
		Button pauseBtn = (Button) findViewById(R.id.pause_btn);
		pauseBtn.setOnClickListener(btnPauseListener);
		pauseBtn.setEnabled(true);
		Button resumeBtn = (Button) findViewById(R.id.resume_btn);
		resumeBtn.setOnClickListener(btnResumeListener);
		resumeBtn.setEnabled(false);
		
		// Get invitation info
		ftDao = (FileTransferDAO) (getIntent().getExtras().getParcelable(FileTransferIntentService.BUNDLE_FTDAO_ID));
		if (ftDao == null) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "onCreate cannot read File Transfer invitation");
			}
			finish();
			return;
		}

		if (getIntent().getAction() != null) {
			resuming = getIntent().getAction().equals(FileTransferResumeReceiver.ACTION_FT_RESUME);
		}

		groupFileTransfer = (getIntent().getBooleanExtra(FileTransferIntentService.EXTRA_GROUP_FILE, false));
		
        if (LogUtils.isActive) {
			Log.d(LOGTAG, "onCreate "+ftDao);
        }
        
		// Register to API connection manager
		connectionManager = ApiConnectionManager.getInstance(this);
		if (connectionManager == null || !connectionManager.isServiceConnected(RcsServiceName.FILE_TRANSFER, RcsServiceName.CONTACTS)) {
			Utils.showMessageAndExit(this, getString(R.string.label_service_not_available), exitOnce);
		} else {
			connectionManager.startMonitorServices(this, exitOnce, RcsServiceName.FILE_TRANSFER, RcsServiceName.CONTACTS);
			initiateFileTransfer();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (connectionManager == null) {
			return;
		}
		connectionManager.stopMonitorServices(this);
		if (connectionManager.isServiceConnected(RcsServiceName.FILE_TRANSFER)) {
			// Remove service listener
			try {
				if (groupFileTransfer) {
					connectionManager.getFileTransferApi().removeEventListener(groupFtListener);
				} else {
					connectionManager.getFileTransferApi().removeEventListener(ftListener);
				}
			} catch (Exception e) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "Failed to remove listener", e);
				}
			}
		}
	}
	
    public void initiateFileTransfer() {
		try {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "initiateFileTransfer "+ftDao);
			}
			FileTransferService ftApi = connectionManager.getFileTransferApi();
			// Get the file transfer session
    		fileTransfer = ftApi.getFileTransfer(ftDao.getTransferId());
			if (fileTransfer == null) {
				try {
					// Fetch state from the provider
					ftDao = new FileTransferDAO(this, ftDao.getTransferId());
					if (ftDao.getState() == FileTransfer.State.TRANSFERRED) {
						displayTransferredFile();
						return;
					} else {
						String reasonCode = RiApplication.FT_REASON_CODES[ftDao.getReasonCode()];
						if (LogUtils.isActive) {
							Log.e(LOGTAG, "Transfer failed state: "+ftDao.getState()+" reason: "+reasonCode);
						}
						// Transfer failed
						Utils.showMessageAndExit(this, getString(R.string.label_transfer_failed, reasonCode), exitOnce);
						return;
					}
				} catch (Exception e) {
					if (LogUtils.isActive) {
						Log.e(LOGTAG, "Failed to retrieve transferred file", e);
					}
					
					// Session not found or expired
					Utils.showMessageAndExit(this, getString(R.string.label_session_not_found), exitOnce);
					return;
				}
			}
			// Add service event listener
			if (groupFileTransfer) {
				ftApi.addEventListener(groupFtListener);
			} else {
				ftApi.addEventListener(ftListener);
			}

			String from = RcsDisplayName.getInstance(this).getDisplayName(ftDao.getContact());
			
			// Display transfer infos
			TextView fromTextView = (TextView) findViewById(R.id.from);
			fromTextView.setText(getString(R.string.label_from_args, from));
	        
			String size = getString(R.string.label_file_size, ftDao.getSize() / 1024);
			TextView sizeTxt = (TextView) findViewById(R.id.image_size);
			sizeTxt.setText(size);

	    	// Do not consider acceptance if resuming
	    	if (resuming) {
	    		return;
	    	}
	    	// TODO To be changed with CR018 which will introduce a new state : ACCEPTING.
	    	// The test is kept in the meantime because it is the only way
	    	// to know if FT is auto accepted by the stack (at least in normal conditions)
	    	
	    	// Check if not already accepted by the stack
	    	if (isFileTransferInvitationAutoAccepted(ftApi.getConfiguration())) {	    		
	    		// File Transfer is auto accepted by the stack. Check capacity
				isCapacityOk(ftDao.getSize());
				
				// Reevaluate the File Transfer state from provider
				try {
					ftDao = new FileTransferDAO(this, ftDao.getTransferId());
					if (ftDao.getState() == FileTransfer.State.TRANSFERRED) {
						displayTransferredFile();
					}
				} catch (Exception e) {
					if (LogUtils.isActive) {
						Log.e(LOGTAG, "Failed to read file from DB", e);
					}
				}
	    	} else {
	    		// File Transfer must be accepted/rejected by user 
				if (LogUtils.isActive) {
					Log.d(LOGTAG, "Wait for user acceptance");
				}
	    		// @formatter:off

	    		// The following code is intentionally commented to test the CORE.
	    		// UI should check the file size to cancel if it is too big.
//	    						if (isCapacityOk(fileSize) == false) {
//	    							rejectInvitation();
//	    							return;
//	    						}

	    		// @formatter:on

				// Manual accept
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.title_file_transfer);
				
				builder.setMessage(getString(R.string.label_ft_from_size, from, ftDao.getSize()/1024));
				builder.setCancelable(false);
				if (ftDao.getThumbnail() != null) {
					try {
						Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), ftDao.getThumbnail());
						builder.setIcon(new BitmapDrawable(getResources(), bitmap));
					} catch (Exception e) {
						if (LogUtils.isActive) {
							Log.e(LOGTAG, "Failed to load thumbnail", e);
						}
					}
				} else {
					if (VCARD_MIME_TYPE.equals(ftDao.getMimeType())) {
						builder.setIcon(R.drawable.ri_contact_card_icon);
					} else {
						builder.setIcon(R.drawable.ri_notif_file_transfer_icon);
					}
				}
				builder.setPositiveButton(getString(R.string.label_accept), acceptBtnListener);
				builder.setNegativeButton(getString(R.string.label_decline), declineBtnListener);
				builder.show();
			}
	    } catch(RcsServiceNotAvailableException e) {
	    	if (LogUtils.isActive) {
				Log.e(LOGTAG, e.getMessage(), e);
			}
			Utils.showMessageAndExit(this, getString(R.string.label_api_disabled), exitOnce);
	    } catch(RcsServiceException e) {
	    	if (LogUtils.isActive) {
				Log.e(LOGTAG, e.getMessage(), e);
			}
			Utils.showMessageAndExit(this, getString(R.string.label_api_failed), exitOnce);
		}
    }
    
	/**
	 * Accept invitation
	 */
	private void acceptInvitation() {
    	try {
    		if (LogUtils.isActive) {
				Log.d(LOGTAG, "Accept invitation");
			}
    		// Accept the invitation
			fileTransfer.acceptInvitation();
    	} catch(Exception e) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, e.getMessage(), e);
			}
			Utils.showMessageAndExit(this, getString(R.string.label_invitation_failed), exitOnce);
    	}
	}
	
	/**
	 * Check if file transfer invitation is auto-accepted
	 * @param config the file transfer service configuration
	 * @return True if already auto accepted by the stack
	 */
	private boolean isFileTransferInvitationAutoAccepted(FileTransferServiceConfiguration config) {
		TelephonyManager telephony = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		if (telephony.isNetworkRoaming()) {
			return config.isAutoAcceptInRoamingEnabled();
		} else {
			return config.isAutoAcceptEnabled();
		}
	}
	
	/**
	 * Reject invitation
	 */
	private void rejectInvitation() {
    	try {
    		if (LogUtils.isActive) {
				Log.d(LOGTAG, "Reject invitation");
			}
    		// Reject the invitation
			fileTransfer.rejectInvitation();
    	} catch(Exception e) {
    		if (LogUtils.isActive) {
				Log.e(LOGTAG, e.getMessage(), e);
			}
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
    		if (LogUtils.isActive) {
				Log.e(LOGTAG, e.getMessage(), e);
			}
    	}
    	fileTransfer = null;
    	
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
	private android.view.View.OnClickListener btnPauseListener = new android.view.View.OnClickListener() {
		@Override
		public void onClick(View arg0) {
			Button resumeBtn = (Button) findViewById(R.id.resume_btn);
			resumeBtn.setEnabled(true);
			Button pauseBtn = (Button) findViewById(R.id.pause_btn);
			pauseBtn.setEnabled(false);

			try {
				fileTransfer.pauseTransfer();
			} catch (RcsServiceException e) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, e.getMessage(), e);
				}
				Utils.showMessageAndExit(ReceiveFileTransfer.this, getString(R.string.label_pause_failed), exitOnce);
			}
		}
	};

	/**
	 * Resume button listener
	 */
	private android.view.View.OnClickListener btnResumeListener = new android.view.View.OnClickListener() {
		@Override
		public void onClick(View arg0) {
			Button resumeBtn = (Button) findViewById(R.id.resume_btn);
			resumeBtn.setEnabled(false);
			Button pauseBtn = (Button) findViewById(R.id.pause_btn);
			pauseBtn.setEnabled(true);

			try {
				fileTransfer.resumeTransfer();
			} catch (RcsServiceException e) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, e.getMessage(), e);
				}
				Utils.showMessageAndExit(ReceiveFileTransfer.this, getString(R.string.label_resume_failed), exitOnce);
			}
		}
	};
	
	/**
	 * Check whether file size exceeds the limit
	 * 
	 * @param size
	 *           Size of file
	 * @return {@code true} if file size limit is exceeded, otherwise {@code false}
	 */
	private boolean isFileSizeExceeded(long size) {
		try {
			long maxSize = connectionManager.getFileTransferApi().getConfiguration().getMaxSize() * 1024;
			return (maxSize > 0 && size > maxSize);
		} catch (Exception e) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, e.getMessage(), e);
			}
			return false;
		}
	}
	
    /**
     * Get available space in external storage, only if external storage is
     * ready to write
     *
     * @return Available space in bytes, otherwise <code>-1</code>
     */
    @SuppressWarnings("deprecation")
	private static long getExternalStorageFreeSpace() {
        long freeSpace = -1;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
			long blockSize = stat.getBlockSize();
            long availableBlocks = stat.getAvailableBlocks();
            freeSpace = blockSize * availableBlocks;
        }
        return freeSpace;
    }
    
	private static enum FileCapacity {
		OK, FILE_TOO_BIG, STORAGE_TOO_SMALL;
	}

	/**
	 * Check if file capacity is acceptable
	 * 
	 * @param fileSize
	 * @return FileSharingError or null if file capacity is acceptable
	 */
	private FileCapacity isFileCapacityAcceptable(long fileSize) {
		if (isFileSizeExceeded(fileSize)) {
			return FileCapacity.FILE_TOO_BIG;
		}
		long freeSpage = getExternalStorageFreeSpace();
		boolean storageIsTooSmall = (freeSpage > 0) ? fileSize > freeSpage : false;
		if (storageIsTooSmall) {
			return FileCapacity.STORAGE_TOO_SMALL;
		}
		return FileCapacity.OK;
	}
	
	/**
	 * Check if file size is less than maximum or then free space on disk
	 * 
	 * @param fileSize
	 * @return boolean
	 */
	private boolean isCapacityOk(long fileSize) {
		FileCapacity capacity = isFileCapacityAcceptable(fileSize);
		switch (capacity) {
		case FILE_TOO_BIG:
			if (LogUtils.isActive) {
				Log.w(LOGTAG, "File is too big, reject the File Transfer");
			}
			Utils.showMessageAndExit(this, getString(R.string.label_transfer_failed_too_big), exitOnce);
			return false;
		case STORAGE_TOO_SMALL:
			if (LogUtils.isActive) {
				Log.w(LOGTAG, "Not enough storage capacity, reject the File Transfer");
			}
			Utils.showMessageAndExit(this, getString(R.string.label_transfer_failed_capacity_too_small), exitOnce);
			return false;
		default:
			return true;
		}
	}

	/**
	 * Update UI on file transfer state change
	 * 
	 * @param state
	 *            new FT state
	 */
	private void onTransferStateChangedUpdateUI(final int state, final int reasonCode) {
		if (state > RiApplication.FT_STATES.length) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "onTransferStateChanged unhandled state=" + state);
			}
			return;
		}
		if (reasonCode > RiApplication.FT_REASON_CODES.length) {
			Log.e(LOGTAG, "onTransferStateChanged unhandled reason=" + reasonCode);
			return;
		}
		final String _reasonCode = RiApplication.FT_REASON_CODES[reasonCode];
		final String _state = RiApplication.FT_STATES[state];
		handler.post(new Runnable() {

			public void run() {
				TextView statusView = (TextView) findViewById(R.id.progress_status);
				switch (state) {
				case FileTransfer.State.STARTED:
					// Session is well established display session status
					statusView.setText(_state);
					break;

				case FileTransfer.State.ABORTED:
					// Session is aborted: display message then exit
					Utils.showMessageAndExit(ReceiveFileTransfer.this, getString(R.string.label_transfer_aborted, _reasonCode), exitOnce);
					break;

				case FileTransfer.State.FAILED:
					// Session is failed: ReceiveFileTransfer
					Utils.showMessageAndExit(ReceiveFileTransfer.this, getString(R.string.label_transfer_failed, _reasonCode), exitOnce);
					break;
					
				case FileTransfer.State.REJECTED:
					// Session is rejected: display message then exit
					Utils.showMessageAndExit(ReceiveFileTransfer.this, getString(R.string.label_transfer_rejected, _reasonCode), exitOnce);
					break;

				case FileTransfer.State.TRANSFERRED:
					displayTransferredFile();
					break;

				default:
					statusView.setText(getString(R.string.label_ft_state_changed, _state, _reasonCode));
				}
			}
		});
	}
	
	private void displayTransferredFile() {
		TextView statusView = (TextView) findViewById(R.id.progress_status);
		statusView.setText(RiApplication.FT_STATES[FileTransfer.State.TRANSFERRED]);
		// Make sure progress bar is at the end
		ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
		progressBar.setProgress(progressBar.getMax());

		// Disable pause button
		Button pauseBtn = (Button) findViewById(R.id.pause_btn);
		pauseBtn.setEnabled(false);
		// Disable resume button
		Button resumeBtn = (Button) findViewById(R.id.resume_btn);
		resumeBtn.setEnabled(false);

		if (VCARD_MIME_TYPE.equals(ftDao.getMimeType())) {
			// Show the transferred vCard
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setDataAndType(ftDao.getFile(), VCARD_MIME_TYPE);
			startActivity(intent);
		} else {
			if (ftDao.getMimeType().startsWith("image/")) {
				// Show the transferred image
				Utils.showPictureAndExit(this, ftDao.getFile());
			}
		}
	}
	
	/**
	 * Update UI on FT progress
	 * 
	 * @param currentSize
	 *            current size
	 * @param totalSize
	 *            total size
	 */
	private void onTransferProgressUpdateUI(final long currentSize, final long totalSize) {
		handler.post(new Runnable() {
			public void run() {
				// Display transfer progress
				updateProgressBar(currentSize, totalSize);
			}
		});
	}
}
