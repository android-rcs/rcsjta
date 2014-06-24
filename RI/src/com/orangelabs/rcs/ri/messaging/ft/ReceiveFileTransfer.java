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
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.RemoteException;
import android.os.StatFs;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gsma.services.rcs.JoynService;
import com.gsma.services.rcs.JoynServiceException;
import com.gsma.services.rcs.JoynServiceListener;
import com.gsma.services.rcs.JoynServiceNotAvailableException;
import com.gsma.services.rcs.ft.FileTransfer;
import com.gsma.services.rcs.ft.FileTransferListener;
import com.gsma.services.rcs.ft.FileTransferService;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Received file transfer
 * 
 * @author Jean-Marc AUFFRET
 */
public class ReceiveFileTransfer extends Activity implements JoynServiceListener {
    /**
     * UI handler
     */
    private final Handler handler = new Handler();
    
	/**
	 * File transfer API
	 */
    private FileTransferService ftApi;
    
    /**
     * File transfer
     */
    private FileTransfer fileTransfer;
    
    /**
     * File transfer listener
     */
    private FileTransferListener ftListener = new MyFileTransferListener();
    
    /**
     * File transfer is resuming
     */
    private boolean resuming = false;
    
    /**
     * The File Transfer Data Object 
     */
    private FileTransferDAO ftdao;
    
    /**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(ReceiveFileTransfer.class.getSimpleName());
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.filetransfer_receive);

        // Set title
		setTitle(R.string.title_file_transfer);
        
		// Set pause and resume button
		Button pauseBtn = (Button) findViewById(R.id.pause_btn);
		pauseBtn.setOnClickListener(btnPauseListener);
		pauseBtn.setEnabled(true);
		Button resumeBtn = (Button) findViewById(R.id.resume_btn);
		resumeBtn.setOnClickListener(btnResumeListener);
		resumeBtn.setEnabled(false);
		
		// Get invitation info
		ftdao = (FileTransferDAO) (getIntent().getExtras().getSerializable(FileTransferIntentService.BUNDLE_FTDAO_ID));
		if (ftdao == null) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "onCreate cannot read File Transfer invitation");
			}
			finish();
			return;
		}

		if (getIntent().getAction() != null) {
			resuming = getIntent().getAction().equals(FileTransferResumeReceiver.ACTION_FT_RESUME);
		}

        // Instantiate API
        ftApi = new FileTransferService(getApplicationContext(), this);
        
        // Connect API
        ftApi.connect();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
        // Remove session listener
        if (fileTransfer != null) {
        	try {
        		fileTransfer.removeEventListener(ftListener);
        	} catch(Exception e) {
        		e.printStackTrace();
        	}
        }

        // Disconnect API
        ftApi.disconnect();
	}
	
    /**
     * Callback called when service is connected. This method is called when the
     * service is well connected to the RCS service (binding procedure successfull):
     * this means the methods of the API may be used.
     */
    public void onServiceConnected() {
		try {
			// Get the file transfer session
    		fileTransfer = ftApi.getFileTransfer(ftdao.getFtId());
			if (fileTransfer == null) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onServiceConnected failed to find session "+ftdao.getFtId());
				}
				// Session not found or expired
				Utils.showMessageAndExit(ReceiveFileTransfer.this, getString(R.string.label_session_not_found));
				return;
			}
			fileTransfer.addEventListener(ftListener);
			
			String size;
	    	if (ftdao.getSize() != -1) {
	    		size = getString(R.string.label_file_size, " " + (ftdao.getSize()/1024), " Kb");
	    	} else {
	    		size = getString(R.string.label_file_size_unknown);
	    	}

	    	// Display transfer infos
    		TextView from = (TextView)findViewById(R.id.from);
	        from.setText(getString(R.string.label_from) + " " + ftdao.getContact());
	    	TextView sizeTxt = (TextView)findViewById(R.id.image_size);
	    	sizeTxt.setText(size);

	    	// Do not consider acceptance if resuming
	    	if (resuming) {
	    		return;
	    	}
	    	// TODO Below test if wrong. It does not consider roaming conditions.
	    	// To be changed with CR018 which will introduce a new state : ACCEPTING.
	    	// The test is kept in the meantime because it is the only way
	    	// to know if FT is auto accepted by the stack (at least in normal conditions)
	    	
	    	// Check if not already accepted by the stack
	    	if (ftApi.getConfiguration().isAutoAcceptEnabled()) {	    		
	    		// File Transfer is auto accepted by the stack. Check capacity
				isCapacityOk(ftdao.getSize());
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
				builder.setMessage(getString(R.string.label_from) +	" " + ftdao.getContact() + "\n" + size);
				builder.setCancelable(false);
				builder.setIcon(R.drawable.ri_notif_file_transfer_icon);
				builder.setPositiveButton(getString(R.string.label_accept), acceptBtnListener);
				builder.setNegativeButton(getString(R.string.label_decline), declineBtnListener);
				builder.show();
			}
	    } catch(JoynServiceNotAvailableException e) {
	    	if (LogUtils.isActive) {
				Log.e(LOGTAG, e.getMessage(), e);
			}
			Utils.showMessageAndExit(ReceiveFileTransfer.this, getString(R.string.label_api_disabled));
	    } catch(JoynServiceException e) {
	    	if (LogUtils.isActive) {
				Log.e(LOGTAG, e.getMessage(), e);
			}
			Utils.showMessageAndExit(ReceiveFileTransfer.this, getString(R.string.label_api_failed));
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
		Utils.showMessageAndExit(ReceiveFileTransfer.this, getString(R.string.label_api_disabled));
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
			Utils.showMessageAndExit(ReceiveFileTransfer.this, getString(R.string.label_invitation_failed));
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
    		fileTransfer.removeEventListener(ftListener);
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
     * File transfer event listener
     */
    private class MyFileTransferListener extends FileTransferListener {
    	/**
    	 * Callback called when the file has been transferred
    	 * 
    	 * @param filename Filename including the path of the transferred file
    	 */
    	public void onFileTransferred(final Uri file) {
    		if (LogUtils.isActive) {
				Log.d(LOGTAG, "onFileTransferred file="+file.toString());
			}
			handler.post(new Runnable() { 
				public void run() {
					TextView statusView = (TextView)findViewById(R.id.progress_status);
					statusView.setText("transferred");
					
					// Make sure progress bar is at the end
			        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progress_bar);
			        progressBar.setProgress(progressBar.getMax());
			        
			        // Disable pause button
			        Button pauseBtn = (Button) findViewById(R.id.pause_btn);
					pauseBtn.setEnabled(false);
					// Disable resume button
					Button resumeBtn = (Button) findViewById(R.id.resume_btn);
					resumeBtn.setEnabled(false);

			        if (ftdao.getMimeType().equals("text/vcard")) {
			        	// Show the transferred vCard
			    		Intent intent = new Intent(Intent.ACTION_VIEW);
			    		intent.setDataAndType(file, "text/x-vcard");   		
			    		startActivity(intent);
			        } else {
				        // Show the transferred image
				        Utils.showPictureAndExit(ReceiveFileTransfer.this, file);
			        }
				}
			});
		}

		/**
		 * Callback called when the file transfer has been aborted
		 */
		public void onTransferAborted() {
			if (LogUtils.isActive) {
				Log.w(LOGTAG, "onTransferAborted");
			}
			handler.post(new Runnable() { 
				public void run() {
					Utils.showMessageAndExit(ReceiveFileTransfer.this, getString(R.string.label_sharing_aborted));
				}
			});
		}

		/**
		 * Callback called when the transfer has failed
		 * 
		 * @param error Error
		 * @see FileTransfer.Error
		 */
		public void onTransferError(final int error) {
			if (LogUtils.isActive) {
				Log.w(LOGTAG, "onTransferError error="+error);
			}
			handler.post(new Runnable() { 
				public void run() {
                    Utils.showMessageAndExit(ReceiveFileTransfer.this,
                            getString(R.string.label_transfer_failed, error));
				}
			});
		}

		/**
		 * Callback called during the transfer progress
		 * 
		 * @param currentSize Current transferred size in bytes
		 * @param totalSize Total size to transfer in bytes
		 */
		public void onTransferProgress(final long currentSize, final long totalSize) {
			handler.post(new Runnable() { 
    			public void run() {
    				updateProgressBar(currentSize, totalSize);
    			}
    		});
		}

		/**
		 * Callback called when the file transfer is started
		 */
		public void onTransferStarted() {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onTransferStarted");
			}
			handler.post(new Runnable() { 
				public void run() {
					TextView statusView = (TextView)findViewById(R.id.progress_status);
					statusView.setText("started");
				}
			});
		}

		@Override
		public void onFileTransferPaused() throws RemoteException {
			if (LogUtils.isActive) {
				Log.i(LOGTAG, "onFileTransferPaused");
			}
		}

		@Override
		public void onFileTransferResumed() throws RemoteException {
			if (LogUtils.isActive) {
				Log.i(LOGTAG, "onFileTransferResumed");
			}
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
        		fileTransfer.removeEventListener(ftListener);
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
			} catch (JoynServiceException e) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, e.getMessage(), e);
				}
				Utils.showMessageAndExit(ReceiveFileTransfer.this, getString(R.string.label_pause_failed));
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
			} catch (JoynServiceException e) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, e.getMessage(), e);
				}
				Utils.showMessageAndExit(ReceiveFileTransfer.this, getString(R.string.label_resume_failed));
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
			long maxSize = ftApi.getConfiguration().getMaxSize() * 1024;
			return (maxSize > 0 && size > maxSize);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}	
	}
	
    /**
     * Get available space in external storage, only if external storage is
     * ready to write
     *
     * @return Available space in bytes, otherwise <code>-1</code>
     */
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
			Utils.showMessageAndExit(ReceiveFileTransfer.this, getString(R.string.label_transfer_failed_too_big));
			return false;
		case STORAGE_TOO_SMALL:
			if (LogUtils.isActive) {
				Log.w(LOGTAG, "Not enough storage capacity, reject the File Transfer");
			}
			Utils.showMessageAndExit(ReceiveFileTransfer.this, getString(R.string.label_transfer_failed_capacity_too_small));
			return false;
		default:
			return true;
		}
	}

}
