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

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransferService;

import com.orangelabs.rcs.ri.ConnectionManager;
import com.orangelabs.rcs.ri.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.FileUtils;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.Utils;

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

/**
 * Send file
 * 
 * @author Philippe LEMORDANT
 */
public abstract class SendFile extends Activity implements ISendFile {

    /**
     * Activity result constants
     */
    private final static int SELECT_IMAGE = 0;

    /**
     * UI handler
     */
    protected final Handler handler = new Handler();

    /**
     * Transfer Id
     */
    protected String mTransferId;

    /**
     * Selected filename
     */
    protected String filename;

    /**
     * Selected fileUri
     */
    private Uri file;

    /**
     * Selected filesize (kB)
     */
    protected long filesize = -1;

    /**
     * API connection manager
     */
    protected ConnectionManager mCnxManager;

    /**
     * File transfer
     */
    protected FileTransfer mFileTransfer;

    /**
     * Progress dialog
     */
    private Dialog progressDialog;

    /**
     * A locker to exit only once
     */
    protected LockAccess mExitOnce = new LockAccess();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.chat_send_file);

        // Set buttons callback
        Button inviteBtn = (Button) findViewById(R.id.invite_btn);
        inviteBtn.setOnClickListener(btnInviteListener);
        inviteBtn.setEnabled(false);
        Button selectBtn = (Button) findViewById(R.id.select_btn);
        selectBtn.setOnClickListener(btnSelectListener);

        // Register to API connection manager
        mCnxManager = ConnectionManager.getInstance();
        if (!mCnxManager.isServiceConnected(RcsServiceName.CHAT, RcsServiceName.FILE_TRANSFER,
                RcsServiceName.CONTACT)) {
            Utils.showMessageAndExit(this, getString(R.string.label_service_not_available),
                    mExitOnce);
        } else {
            mCnxManager.startMonitorServices(this, mExitOnce, RcsServiceName.CHAT,
                    RcsServiceName.FILE_TRANSFER, RcsServiceName.CONTACT);
            FileTransferService ftApi = mCnxManager.getFileTransferApi();
            try {
                addFileTransferEventListener(ftApi);
            } catch (Exception e) {
                Utils.showMessageAndExit(this, getString(R.string.label_api_failed), mExitOnce, e);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCnxManager.stopMonitorServices(this);
        if (mCnxManager.isServiceConnected(RcsServiceName.FILE_TRANSFER)) {
            // Remove file listener
            try {
                removeFileTransferEventListener(mCnxManager.getFileTransferApi());
            } catch (RcsServiceException e) {
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
                warnSize = mCnxManager.getFileTransferApi().getConfiguration().getWarnSize();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (warnSize > 0 && filesize >= warnSize) {
                // Display a warning message
                AlertDialog.Builder builder = new AlertDialog.Builder(SendFile.this);
                builder.setMessage(getString(R.string.label_sharing_warn_size, filesize));
                builder.setCancelable(false);
                builder.setPositiveButton(getString(R.string.label_yes),
                        new DialogInterface.OnClickListener() {
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
        // Get thumbnail option
        CheckBox ftThumb = (CheckBox) findViewById(R.id.ft_thumb);
        if (transferFile(file, ftThumb.isChecked())) {

            // Display a progress dialog
            progressDialog = Utils.showProgressDialog(SendFile.this,
                    getString(R.string.label_command_in_progress));
            progressDialog.setOnCancelListener(new OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    Toast.makeText(SendFile.this, getString(R.string.label_transfer_cancelled),
                            Toast.LENGTH_SHORT).show();
                    quitSession();
                }
            });

            // Hide buttons
            Button inviteBtn = (Button) findViewById(R.id.invite_btn);
            inviteBtn.setVisibility(View.INVISIBLE);
            Button selectBtn = (Button) findViewById(R.id.select_btn);
            selectBtn.setVisibility(View.INVISIBLE);
            ftThumb.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Select file button listener
     */
    private OnClickListener btnSelectListener = new OnClickListener() {
        public void onClick(View v) {
            FileUtils.openFile(SendFile.this, "image/*", SELECT_IMAGE);
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
    protected void updateProgressBar(long currentSize, long totalSize) {
        TextView statusView = (TextView) findViewById(R.id.progress_status);
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        statusView.setText(Utils.getProgressLabel(currentSize, totalSize));
        double position = ((double) currentSize / (double) totalSize) * 100.0;
        progressBar.setProgress((int) position);
    }

    /**
     * Quit the session
     */
    private void quitSession() {
        // Stop session
        try {
            if (mFileTransfer != null) {
                mFileTransfer.abortTransfer();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mFileTransfer = null;

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
        MenuInflater inflater = new MenuInflater(getApplicationContext());
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
