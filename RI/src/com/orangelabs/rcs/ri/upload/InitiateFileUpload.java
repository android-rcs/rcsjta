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

package com.orangelabs.rcs.ri.upload;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.upload.FileUpload;
import com.gsma.services.rcs.upload.FileUploadListener;
import com.orangelabs.rcs.ri.ApiConnectionManager;
import com.orangelabs.rcs.ri.ApiConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.messaging.ft.InitiateFileTransfer;
import com.orangelabs.rcs.ri.utils.FileUtils;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Initiate file upload
 * 
 * @author Jean-Marc AUFFRET
 */
public class InitiateFileUpload extends Activity {
	/**
	 * Activity result constants
	 */
	private final static int SELECT_IMAGE = 0;
	
	/**
	 * UI handler
	 */
	private final Handler handler = new Handler();
	
	/**
	 * Selected file URI
	 */
	private Uri file;
	
	/**
	 * Selected filesize (kB)
	 */
	private long filesize = -1;	
	
	/**
     * File upload session
     */
    private FileUpload upload;
    
	
	/**
     * File upload Id
     */
    private String uploadId;
    
    /**
	 * A locker to exit only once
	 */
	private LockAccess exitOnce = new LockAccess();
	
   	/**
	 * API connection manager
	 */
	private ApiConnectionManager connectionManager;
	
    
    /**
     * File upload listener
     */
    private MyFileUploadListener uploadListener = new MyFileUploadListener();  
    
    /**
   	 * The log tag for this class
   	 */
   	private static final String LOGTAG = LogUtils.getTag(InitiateFileTransfer.class.getSimpleName());
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.fileupload_initiate);
        
        // Set buttons callback
        Button uploadBtn = (Button)findViewById(R.id.upload_btn);
        uploadBtn.setOnClickListener(btnUploadListener);
        uploadBtn.setEnabled(false);
        Button showBtn = (Button)findViewById(R.id.show_btn);
        showBtn.setOnClickListener(btnShowListener);
        showBtn.setEnabled(false);
        Button selectBtn = (Button)findViewById(R.id.select_btn);
        selectBtn.setOnClickListener(btnSelectListener);

		// Register to API connection manager
		connectionManager = ApiConnectionManager.getInstance(this);
		if (connectionManager == null || !connectionManager.isServiceConnected(RcsServiceName.FILE_TRANSFER)) {
			Utils.showMessageAndExit(this, getString(R.string.label_service_not_available), exitOnce);
			return;
		}
		connectionManager.startMonitorServices(this, exitOnce, RcsServiceName.FILE_UPLOAD);
		try {
			// Add upload listener
			connectionManager.getFileUploadApi().addEventListener(uploadListener);
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
    	super.onDestroy();
    	if (connectionManager == null) {
			return;
		}
		connectionManager.stopMonitorServices(this);
        // Remove upload listener
        if (connectionManager.isServiceConnected(RcsServiceName.FILE_TRANSFER)) {
        	try {
        		connectionManager.getFileUploadApi().removeEventListener(uploadListener);
        	} catch(Exception e) {
        		if (LogUtils.isActive) {
					Log.e(LOGTAG, "Failed to remove listener", e);
				}
        	}
        }
    }
    
    /**
     * Upload button listener
     */
    private OnClickListener btnUploadListener = new OnClickListener() {
        public void onClick(View v) {
        	try {
        		// Check max size
            	long maxSize = 0;
            	try {
            		maxSize = connectionManager.getFileUploadApi().getConfiguration().getMaxSize();
            		if (LogUtils.isActive) {
    					Log.d(LOGTAG, "FileUpload max size=".concat(Long.valueOf(maxSize).toString()));
    				}
            	} catch(Exception e) {
            		e.printStackTrace();
            	}
                if ((maxSize > 0) && (filesize >= maxSize)) {
    				// Display an error
                	Utils.showMessage(InitiateFileUpload.this, getString(R.string.label_upload_max_size, maxSize));
                	return;
                	
                }

                // Get thumbnail option
        		CheckBox ftThumb = (CheckBox) findViewById(R.id.file_thumb);
        		boolean thumbnail = ftThumb.isChecked();
        		
            	// Initiate upload
        		upload = connectionManager.getFileUploadApi().uploadFile(file, thumbnail);
        		uploadId = upload.getUploadId();
        		
                // Hide buttons
                Button uploadBtn = (Button)findViewById(R.id.upload_btn);
                uploadBtn.setVisibility(View.GONE);
                Button selectBtn = (Button)findViewById(R.id.select_btn);
                selectBtn.setVisibility(View.GONE);
        	} catch(Exception e) {
        		if (LogUtils.isActive) {
    				Log.e(LOGTAG, "Failed to upload file", e);
    			}
				Utils.showMessageAndExit(InitiateFileUpload.this, getString(R.string.label_upload_failed), exitOnce);
        	}
        }
    };
    
    /**
     * Select file button listener
     */
    private OnClickListener btnSelectListener = new OnClickListener() {
		public void onClick(View v) {
			FileUtils.openFile(InitiateFileUpload.this, "image/*", SELECT_IMAGE);
		}
    };
    
    /**
     * Show uploaded file button listener
     */
    private OnClickListener btnShowListener = new OnClickListener() {
        public void onClick(View v) {
        	// Show upload info
        	try {
        		Intent intent = new Intent(Intent.ACTION_VIEW);
        		String filename = upload.getUploadInfo().getFile().toString() + "/" + upload.getUploadInfo().getFileName();
        		intent.setData(Uri.parse(filename));
        		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        		startActivity(intent);
        	} catch(Exception e) {
        		e.printStackTrace();
        		Utils.showMessage(InitiateFileUpload.this, getString(R.string.label_upload_failed));
        	}
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
		if (resultCode != RESULT_OK || (data == null) || (data.getData() == null)) {
			return;
		}
		file = data.getData();
		TextView uriEdit = (TextView) findViewById(R.id.uri);
		Button uploadBtn = (Button) findViewById(R.id.upload_btn);
		switch (requestCode) {
		case SELECT_IMAGE:
			// Display file info
			try {
				// Get image filename and size
				filesize = FileUtils.getFileSize(this, file) / 1024;
				uriEdit.setText(filesize + " KB");
			} catch (Exception e) {
				filesize = -1;
				uriEdit.setText("Unknown");
			}
			
			// Enable upload button
			uploadBtn.setEnabled(true);
			break;
		}
	}
    
    /**
     * File upload event listener
     */
    private class MyFileUploadListener extends FileUploadListener {
    	/**
    	 * Callback called when the upload state changes
    	 *
    	 * @param uploadId ID of upload
    	 * @param state State of upload 
    	 */
    	@Override
    	public void onStateChanged(String uploadId, final int state) {
			// Discard event if not for current uploadId
			if (InitiateFileUpload.this.uploadId == null || !InitiateFileUpload.this.uploadId.equals(uploadId)) {
				return;
			}
			handler.post(new Runnable() { 
				public void run() {
					if (state == FileUpload.State.STARTED) {
						// Display session status
						TextView statusView = (TextView)findViewById(R.id.progress_status);
						statusView.setText("started");
					} else
					if (state == FileUpload.State.FAILED) {
						// Display sharing status
	                    Utils.showMessage(InitiateFileUpload.this,
							getString(R.string.label_upload_failed));
					} else
					if (state == FileUpload.State.ABORTED) {
						// Display sharing status
	                    Utils.showMessage(InitiateFileUpload.this,
							getString(R.string.label_upload_aborted));
					} else
					if (state == FileUpload.State.TRANSFERRED) {
						// Display sharing status
						TextView statusView = (TextView)findViewById(R.id.progress_status);
						statusView.setText("transferred");
						
						// Activate show button
				        Button showBtn = (Button)findViewById(R.id.show_btn);
				        showBtn.setEnabled(true);
					}						
				}
			});
    		
    	}

    	/**
    	 * Callback called during the upload progress
    	 *
    	 * @param sharingId ID of upload
    	 * @param currentSize Current transferred size in bytes
    	 * @param totalSize Total size to transfer in bytes
    	 */
    	@Override
    	public void onProgressUpdate(String uploadId, final long currentSize, final long totalSize) {
			handler.post(new Runnable() { 
    			public void run() {
					// Display sharing progress
    				updateProgressBar(currentSize, totalSize);
    			}
    		});
    	}
    };
    
    /**
     * Show the sharing progress
     * 
     * @param currentSize Current size transferred
     * @param totalSize Total size to be transferred
     */
    private void updateProgressBar(long currentSize, long totalSize) {
    	TextView statusView = (TextView)findViewById(R.id.progress_status);
        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progress_bar);
    	
		String value = "" + Math.min(currentSize/1024, totalSize);
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
     * Quit the upload
     */
    private void quitUpload() {
		// Stop upload
    	try {
            if (upload != null) {
            	upload.abortUpload();
            }
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	upload = null;
		
	    // Exit activity
		finish();
    }    
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
            	// Quit the upload
            	quitUpload();
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }
}    
