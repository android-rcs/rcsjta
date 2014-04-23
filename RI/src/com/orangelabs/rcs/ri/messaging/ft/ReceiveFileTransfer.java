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

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gsma.services.rcs.JoynServiceException;
import com.gsma.services.rcs.JoynServiceListener;
import com.gsma.services.rcs.JoynServiceNotAvailableException;
import com.gsma.services.rcs.ft.FileTransfer;
import com.gsma.services.rcs.ft.FileTransferIntent;
import com.gsma.services.rcs.ft.FileTransferListener;
import com.gsma.services.rcs.ft.FileTransferService;
import com.orangelabs.rcs.ri.R;
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
	 * Transfer ID
	 */
    private String transferId;
    
    /**
     * Remote Contact
     */
    private String remoteContact;
   
    /**
     * File size
     */
    private long fileSize;
    
    /**
     * File type
     */
    private String fileType;
    
    /**
     * File transfer
     */
    private FileTransfer fileTransfer = null;
    
    /**
     * File transfer listener
     */
    private FileTransferListener ftListener = new MyFileTransferListener();
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.filetransfer_receive);

        // Set title
		setTitle(R.string.title_file_transfer);
        
        // Get invitation info
        transferId = getIntent().getStringExtra(FileTransferIntent.EXTRA_TRANSFER_ID);
		remoteContact = getIntent().getStringExtra(FileTransferIntent.EXTRA_CONTACT);
		fileSize = getIntent().getLongExtra(FileTransferIntent.EXTRA_FILESIZE, -1);
		fileType = getIntent().getStringExtra(FileTransferIntent.EXTRA_FILETYPE);
		
        // Instanciate API
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
    		fileTransfer = ftApi.getFileTransfer(transferId);
			if (fileTransfer == null) {
				// Session not found or expired
				Utils.showMessageAndExit(ReceiveFileTransfer.this, getString(R.string.label_session_not_found));
				return;
			}
			fileTransfer.addEventListener(ftListener);
			
			String size;
	    	if (fileSize != -1) {
	    		size = getString(R.string.label_file_size, " " + (fileSize/1024), " Kb");
	    	} else {
	    		size = getString(R.string.label_file_size_unknown);
	    	}

	    	// Display transfer infos
    		TextView from = (TextView)findViewById(R.id.from);
	        from.setText(getString(R.string.label_from) + " " + remoteContact);
	    	TextView sizeTxt = (TextView)findViewById(R.id.image_size);
	    	sizeTxt.setText(size);

			// Display accept/reject dialog
	    	if (!ftApi.getConfiguration().isAutoAcceptMode()) {
				// Manual accept
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.title_file_transfer);
				builder.setMessage(getString(R.string.label_from) +	" " + remoteContact + "\n" + size);
				builder.setCancelable(false);
				builder.setIcon(R.drawable.ri_notif_file_transfer_icon);
				builder.setPositiveButton(getString(R.string.label_accept), acceptBtnListener);
				builder.setNegativeButton(getString(R.string.label_decline), declineBtnListener);
				builder.show();
			}
	    } catch(JoynServiceNotAvailableException e) {
	    	e.printStackTrace();
			Utils.showMessageAndExit(ReceiveFileTransfer.this, getString(R.string.label_api_disabled));
	    } catch(JoynServiceException e) {
	    	e.printStackTrace();
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
    		// Accept the invitation
			fileTransfer.acceptInvitation();
    	} catch(Exception e) {
    		e.printStackTrace();
			Utils.showMessageAndExit(ReceiveFileTransfer.this, getString(R.string.label_invitation_failed));
    	}
	}
	
	/**
	 * Reject invitation
	 */
	private void rejectInvitation() {
    	try {
    		// Reject the invitation
    		fileTransfer.removeEventListener(ftListener);
			fileTransfer.rejectInvitation();
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
     * File transfer event listener
     */
    private class MyFileTransferListener extends FileTransferListener {
    	/**
    	 * Callback called when the file has been transferred
    	 * 
    	 * @param filename Filename including the path of the transferred file
    	 */
    	public void onFileTransferred(final String filename) {
			handler.post(new Runnable() { 
				public void run() {
					TextView statusView = (TextView)findViewById(R.id.progress_status);
					statusView.setText("transferred");
					
					// Make sure progress bar is at the end
			        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progress_bar);
			        progressBar.setProgress(progressBar.getMax());

			        if (fileType.equals("text/vcard")) {
			        	// Show the transferred vCard
			        	File file = new File(filename);
			    		Uri uri = Uri.fromFile(file);
			    		Intent intent = new Intent(Intent.ACTION_VIEW);
			    		intent.setDataAndType(uri, "text/x-vcard");   		
			    		startActivity(intent);
			        } else {
				        // Show the transferred image
				        Utils.showPictureAndExit(ReceiveFileTransfer.this, filename);
			        }
				}
			});
		}

		/**
		 * Callback called when the file transfer has been aborted
		 */
		public void onTransferAborted() {
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
			handler.post(new Runnable() { 
				public void run() {
					TextView statusView = (TextView)findViewById(R.id.progress_status);
					statusView.setText("started");
				}
			});
		}

		@Override
		public void onFileTransferPaused() throws RemoteException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onFileTransferResumed() throws RemoteException {
			// TODO Auto-generated method stub
			
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
}
