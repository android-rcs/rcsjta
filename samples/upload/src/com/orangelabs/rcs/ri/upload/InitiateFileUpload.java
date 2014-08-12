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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gsma.services.rcs.JoynService;
import com.gsma.services.rcs.JoynServiceListener;
import com.gsma.services.rcs.upload.FileUpload;
import com.gsma.services.rcs.upload.FileUploadListener;
import com.gsma.services.rcs.upload.FileUploadService;
import com.orangelabs.rcs.ri.utils.FileUtils;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Initiate file upload
 * 
 * @author Jean-Marc AUFFRET
 */
public class InitiateFileUpload extends Activity implements JoynServiceListener {
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
	 * File upload API
	 */
    private FileUploadService uploadApi;
    
	/**
     * File upload
     */
    private FileUpload upload = null;
    
    /**
     * File upload listener
     */
    private MyFileUploadListener uploadListener = new MyFileUploadListener();  
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.fileupload_initiate);
        
        // Set title
        setTitle(R.string.title_initiate_upload);
        
        // Set buttons callback
        Button uploadBtn = (Button)findViewById(R.id.upload_btn);
        uploadBtn.setOnClickListener(btnUploadListener);
        uploadBtn.setEnabled(false);
        Button selectBtn = (Button)findViewById(R.id.select_btn);
        selectBtn.setOnClickListener(btnSelectListener);

        // Instantiate API
        uploadApi = new FileUploadService(getApplicationContext(), this);
		
		// Connect API
        uploadApi.connect();
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();

        // Remove upload listener
        if (uploadApi != null) {
        	try {
        		uploadApi.removeEventListener(uploadListener);
        	} catch(Exception e) {
        		e.printStackTrace();
        	}
        }

        // Disconnect API
        uploadApi.disconnect();
    }
    
    /**
     * Callback called when service is connected. This method is called when the
     * service is well connected to the RCS service (binding procedure successfull):
     * this means the methods of the API may be used.
     */
    public void onServiceConnected() {
    	// Activate the select button when connected to the API
        Button selectBtn = (Button)findViewById(R.id.select_btn);
        selectBtn.setEnabled(true);
    }
    
    /**
     * Callback called when service has been disconnected. This method is called when
     * the service is disconnected from the RCS service (e.g. service deactivated).
     * 
     * @param error Error
     * @see JoynService.Error
     */
    public void onServiceDisconnected(int error) {
		Utils.showMessageAndExit(InitiateFileUpload.this, getString(R.string.label_api_disabled));
    }    
    
    /**
     * Upload button listener
     */
    private OnClickListener btnUploadListener = new OnClickListener() {
        public void onClick(View v) {
        	try {
            	// Add upload listener
        		uploadApi.addEventListener(uploadListener);

                // Get thumbnail option
        		CheckBox ftThumb = (CheckBox) findViewById(R.id.file_thumb);
        		boolean thumbnail = ftThumb.isChecked();
        		
            	// Initiate upload
        		upload = uploadApi.uploadFile(file, thumbnail);
        	} catch(Exception e) {
        		e.printStackTrace();
				Utils.showMessageAndExit(InitiateFileUpload.this, getString(R.string.label_upload_failed));
        	}

            // Hide buttons
            Button uploadBtn = (Button)findViewById(R.id.upload_btn);
            uploadBtn.setVisibility(View.INVISIBLE);
        }
    };
    
    /**
     * Select file button listener
     */
    private OnClickListener btnSelectListener = new OnClickListener() {
        public void onClick(View v) {
			Intent intent;
			if (Build.VERSION.SDK_INT < 19) { // Build.VERSION_CODES.KITKAT
				intent = new Intent(Intent.ACTION_GET_CONTENT, null);
			} else {
				intent = new Intent("android.intent.action.OPEN_DOCUMENT");
			}
			intent.addCategory(Intent.CATEGORY_OPENABLE);
			intent.setType("image/*");
			startActivityForResult(intent, SELECT_IMAGE);
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
    	public void onUploadStateChanged(String uploadId, final int state) {
			handler.post(new Runnable() { 
				public void run() {
					if (state == FileUpload.State.STARTED) {
						// Display session status
						TextView statusView = (TextView)findViewById(R.id.progress_status);
						statusView.setText("started");
					} else
					if (state == FileUpload.State.FAILED) {
						// Display error
	                    Utils.showMessageAndExit(InitiateFileUpload.this,
							getString(R.string.label_upload_failed));
					} else
					if (state == FileUpload.State.ABORTED) {
						// Display session status
						Utils.showMessageAndExit(InitiateFileUpload.this,
							getString(R.string.label_upload_aborted));
					} else
					if (state == FileUpload.State.TRANSFERRED) {
						// Display sharing progress
						TextView statusView = (TextView)findViewById(R.id.progress_status);
						statusView.setText("transferred");
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
    	public void onUploadProgress(String uploadId, final long currentSize, final long totalSize) {
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
