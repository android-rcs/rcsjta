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
import com.gsma.services.rcs.filetransfer.FileTransferIntent;
import com.gsma.services.rcs.filetransfer.FileTransferService;

import com.orangelabs.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.api.connection.utils.ExceptionUtil;
import com.orangelabs.rcs.api.connection.utils.RcsActivity;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.FileUtils;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsSessionUtil;
import com.orangelabs.rcs.ri.utils.Utils;

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

/**
 * Send file
 * 
 * @author Philippe LEMORDANT
 */
public abstract class SendFile extends RcsActivity implements ISendFile {

    private final static int SELECT_IMAGE = 0;

    /**
     * UI handler
     */
    protected final Handler mHandler = new Handler();

    protected String mTransferId;

    protected String mFilename;

    private Uri mFile;

    protected long mFilesize = -1;

    protected FileTransfer mFileTransfer;

    private Dialog mProgressDialog;

    protected Button mResumeBtn;

    protected Button mPauseBtn;

    private boolean mResuming = false;

    private Button mInviteBtn;

    private Button mSelectBtn;

    protected FileTransferService mFileTransferService;

    private OnClickListener mBtnInviteListener;

    private OnClickListener mBtnSelectListener;

    private OnClickListener mBtnPauseListener;

    private OnClickListener mBtnResumeListener;

    private static final String LOGTAG = LogUtils.getTag(SendFile.class.getSimpleName());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize();
        mResuming = FileTransferIntent.ACTION_RESUME.equals(getIntent().getAction());

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.chat_send_file);

        mInviteBtn = (Button) findViewById(R.id.invite_btn);
        mInviteBtn.setOnClickListener(mBtnInviteListener);
        mInviteBtn.setEnabled(false);

        mSelectBtn = (Button) findViewById(R.id.select_btn);
        mSelectBtn.setOnClickListener(mBtnSelectListener);

        mPauseBtn = (Button) findViewById(R.id.pause_btn);
        mPauseBtn.setOnClickListener(mBtnPauseListener);
        mPauseBtn.setEnabled(false);

        mResumeBtn = (Button) findViewById(R.id.resume_btn);
        mResumeBtn.setOnClickListener(mBtnResumeListener);
        mResumeBtn.setEnabled(false);

        /* Register to API connection manager */
        if (!isServiceConnected(RcsServiceName.CHAT, RcsServiceName.FILE_TRANSFER,
                RcsServiceName.CONTACT)) {
            showMessageThenExit(R.string.label_service_not_available);

        } else {
            startMonitorServices(RcsServiceName.CHAT, RcsServiceName.FILE_TRANSFER,
                    RcsServiceName.CONTACT);
            mFileTransferService = getFileTransferApi();
            try {
                addFileTransferEventListener(mFileTransferService);

            } catch (RcsServiceException e) {
                showExceptionThenExit(e);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFileTransferService != null && isServiceConnected(RcsServiceName.FILE_TRANSFER)) {
            try {
                removeFileTransferEventListener(mFileTransferService);
            } catch (RcsServiceException e) {
                Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
            }
        }
    }

    private void initiateTransfer() {
        /* Get thumbnail option */
        CheckBox ftThumb = (CheckBox) findViewById(R.id.ft_thumb);
        if (transferFile(mFile, ftThumb.isChecked())) {
            /* Display a progress dialog */
            mProgressDialog = showProgressDialog(getString(R.string.label_command_in_progress));
            mProgressDialog.setOnCancelListener(new OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    Toast.makeText(SendFile.this, getString(R.string.label_transfer_cancelled),
                            Toast.LENGTH_SHORT).show();
                    quitSession();
                }
            });
            /* Hide buttons */
            mInviteBtn.setVisibility(View.INVISIBLE);
            mSelectBtn.setVisibility(View.INVISIBLE);
            ftThumb.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case SELECT_IMAGE:
                if ((data != null) && (data.getData() != null)) {
                    /* Get selected photo URI */
                    mFile = data.getData();
                    /* Display the selected filename attribute */
                    TextView uriEdit = (TextView) findViewById(R.id.uri);
                    mFilename = FileUtils.getFileName(this, mFile);
                    mFilesize = FileUtils.getFileSize(this, mFile) / 1024;
                    uriEdit.setText(mFilesize + " KB");

                    mInviteBtn.setEnabled(true);
                }
                break;
        }
    }

    /**
     * Hide progress dialog
     */
    protected void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
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

    private void quitSession() {
        try {
            if (mFileTransfer != null
                    && RcsSessionUtil.isAllowedToAbortFileTransferSession(mFileTransfer)) {
                mFileTransfer.abortTransfer();
            }
        } catch (RcsServiceException e) {
            showException(e);

        } finally {
            mFileTransfer = null;
            /* Exit activity */
            finish();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        try {
            if (KeyEvent.KEYCODE_BACK == keyCode) {
                if (mFileTransfer == null
                        || !RcsSessionUtil.isAllowedToAbortFileTransferSession(mFileTransfer)) {
                    finish();
                    return true;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.label_confirm_close);
                builder.setPositiveButton(R.string.label_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        quitSession();
                    }
                });
                builder.setNegativeButton(R.string.label_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                /* Exit activity */
                                finish();
                            }
                        });
                builder.setCancelable(true);
                registerDialog(builder.show());
                return true;
            }
        } catch (RcsServiceException e) {
            showException(e);
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
                quitSession();
                break;
        }
        return true;
    }

    @Override
    public void initialize() {
        mBtnInviteListener = new OnClickListener() {
            public void onClick(View v) {
                long warnSize = 0;
                try {
                    warnSize = mFileTransferService.getConfiguration().getWarnSize();
                } catch (RcsServiceException e) {
                    showException(e);
                }

                if (warnSize > 0 && mFilesize >= warnSize) {
                    // Display a warning message
                    AlertDialog.Builder builder = new AlertDialog.Builder(SendFile.this);
                    builder.setMessage(getString(R.string.label_sharing_warn_size, mFilesize));
                    builder.setCancelable(false);
                    builder.setPositiveButton(R.string.label_yes,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int position) {
                                    initiateTransfer();
                                }
                            });
                    builder.setNegativeButton(R.string.label_no, null);
                    registerDialog(builder.show());

                } else {
                    initiateTransfer();
                }
            }
        };

        mBtnSelectListener = new OnClickListener() {
            public void onClick(View v) {
                FileUtils.openFile(SendFile.this, "image/*", SELECT_IMAGE);
            }
        };

        mBtnPauseListener = new OnClickListener() {
            public void onClick(View v) {
                try {
                    if (mFileTransfer.isAllowedToPauseTransfer()) {
                        mFileTransfer.pauseTransfer();
                    } else {
                        mPauseBtn.setEnabled(false);
                        showMessage(R.string.label_pause_ft_not_allowed);
                    }
                } catch (RcsServiceException e) {
                    showExceptionThenExit(e);
                }
            }
        };

        mBtnResumeListener = new OnClickListener() {
            public void onClick(View v) {
                try {
                    if (mFileTransfer.isAllowedToResumeTransfer()) {
                        mFileTransfer.resumeTransfer();
                    } else {
                        mResumeBtn.setEnabled(false);
                        showMessage(R.string.label_resume_ft_not_allowed);
                    }
                } catch (RcsServiceException e) {
                    showExceptionThenExit(e);
                }
            }
        };
    }

}
