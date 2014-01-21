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
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract.Data;
import android.provider.MediaStore;
import android.telephony.PhoneNumberUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.gsma.services.rcs.JoynService;
import com.gsma.services.rcs.JoynServiceListener;
import com.gsma.services.rcs.ft.FileTransfer;
import com.gsma.services.rcs.ft.FileTransferListener;
import com.gsma.services.rcs.ft.FileTransferService;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Initiate file transfer
 * 
 * @author Jean-Marc AUFFRET
 */
public class InitiateFileTransfer extends Activity implements JoynServiceListener {
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
    private FileTransfer fileTransfer = null;
    
    /**
     * File transfer listener
     */
    private MyFileTransferListener ftListener = new MyFileTransferListener();
    
    /**
     * Progress dialog
     */
    private Dialog progressDialog = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.filetransfer_initiate);
        
        // Set title
        setTitle(R.string.menu_transfer_file);
        
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
               
        // Instanciate API
        ftApi = new FileTransferService(getApplicationContext(), this);
        
        // Connect API
        ftApi.connect();
        
        // Select the corresponding contact from the intent
        Intent intent = getIntent();
        Uri contactUri = intent.getData();
    	if (contactUri != null) {
			Cursor cursor = managedQuery(contactUri, null, null, null, null);
	        if (cursor.moveToNext()) {
	        	String selectedContact = cursor.getString(cursor.getColumnIndex(Data.DATA1));
	            if (selectedContact != null) {
	    	        for (int i=0;i<spinner.getAdapter().getCount();i++) {
	    	        	MatrixCursor cursor2 = (MatrixCursor)spinner.getAdapter().getItem(i);
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
    
    @Override
    public void onDestroy() {
    	super.onDestroy();

        // Remove file transfer listener
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
        // Disable button if no contact available
        Spinner spinner = (Spinner)findViewById(R.id.contact);
        Button selectBtn = (Button)findViewById(R.id.select_btn);
        if (spinner.getAdapter().getCount() != 0) {
        	selectBtn.setEnabled(true);
        }
        
        // Enable thumbnail option if supported
        try {
            CheckBox ftThumb = (CheckBox)findViewById(R.id.ft_thumb);
	        if (ftApi.getConfiguration().isFileIconSupported()) {
	        	ftThumb.setEnabled(true);
	        }
        } catch(Exception e) {
        	e.printStackTrace();
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
		Utils.showMessageAndExit(InitiateFileTransfer.this, getString(R.string.label_api_disabled));
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
    		if ((ftApi != null) && ftApi.isServiceRegistered()) {
    			registered = true;
    		}
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
        if (!registered) {
	    	Utils.showMessage(InitiateFileTransfer.this, getString(R.string.label_service_not_available));
	    	return;
        }     	    	
    	
    	// Get remote contact
        Spinner spinner = (Spinner)findViewById(R.id.contact);
        MatrixCursor cursor = (MatrixCursor)spinner.getSelectedItem();
        final String remote = cursor.getString(1);

        // Get thumbnail option
    	String tumbnail = null; 
        CheckBox ftThumb = (CheckBox)findViewById(R.id.ft_thumb);
        if (ftThumb.isChecked()) {
        	// Create a tumbnail
        	tumbnail = Utils.createPictureThumbnail(getApplicationContext(), filename, 50 * 1024);
        }
    	final String fileicon = tumbnail; 
        
        // Initiate session in background
    	try {
    		// Initiate transfer
    		fileTransfer = ftApi.transferFile(remote, filename, fileicon, ftListener);
    	} catch(Exception e) {
    		e.printStackTrace();
			hideProgressDialog();
			Utils.showMessageAndExit(InitiateFileTransfer.this, getString(R.string.label_invitation_failed));
    	}
        
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

    	switch(requestCode) {
	    	case SELECT_IMAGE: {
	    		if ((data != null) && (data.getData() != null)) {
	    			// Get selected photo URI
	    			Uri uri = data.getData();
	
	    			// Get image filename
	    			Cursor cursor = getContentResolver().query(uri, new String[] {MediaStore.Images.ImageColumns.DATA}, null, null, null); 
	    			cursor.moveToFirst();
	    			filename = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA));
	    			cursor.close();     	    		
	    			
	    			// Display the selected filename attribute
	    			TextView uriEdit = (TextView)findViewById(R.id.uri);
	    			try {
	    				File file = new File(filename);
	    				filesize = file.length()/1024;
	    				uriEdit.setText(filesize + " KB");
	    			} catch(Exception e) {
	    				filesize = -1;
	    				uriEdit.setText(filename);
	    			}
	    			
	    			// Show invite button
	    			Button inviteBtn = (Button)findViewById(R.id.invite_btn);
	    			inviteBtn.setEnabled(true);
	    		}
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
     * File transfer event listener
     */
    private class MyFileTransferListener extends FileTransferListener {
    	// File transfer started
    	public void onTransferStarted() {
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
    	
    	// File transfer aborted
    	public void onTransferAborted() {
			handler.post(new Runnable() { 
				public void run() {
					// Hide progress dialog
					hideProgressDialog();

					// Display message
					Utils.showMessageAndExit(InitiateFileTransfer.this, getString(R.string.label_sharing_aborted));
				}
			});
    	}

    	// File transfer error
    	public void onTransferError(final int error) {
			handler.post(new Runnable() { 
				public void run() {
					// Hide progress dialog
					hideProgressDialog();
					
                    // Display error
                    if (error == FileTransfer.Error.INVITATION_DECLINED) {
                        Utils.showMessageAndExit(InitiateFileTransfer.this,
                                getString(R.string.label_transfer_declined));
                    } else {
                        Utils.showMessageAndExit(InitiateFileTransfer.this,
                                getString(R.string.label_transfer_failed, error));
                    }
				}
			});
    	}
    	
    	// File transfer progress
    	public void onTransferProgress(final long currentSize, final long totalSize) {
			handler.post(new Runnable() { 
    			public void run() {
					// Display transfer progress
    				updateProgressBar(currentSize, totalSize);
    			}
    		});
    	}

    	// File transferred
    	public void onFileTransferred(String filename) {
			handler.post(new Runnable() { 
				public void run() {
					// Hide progress dialog
					hideProgressDialog();

					// Display transfer progress
					TextView statusView = (TextView)findViewById(R.id.progress_status);
					statusView.setText("transferred");
				}
			});
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
