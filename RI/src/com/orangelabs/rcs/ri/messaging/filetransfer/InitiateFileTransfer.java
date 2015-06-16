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

package com.orangelabs.rcs.ri.messaging.filetransfer;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransferLog;
import com.gsma.services.rcs.filetransfer.FileTransferService;
import com.gsma.services.rcs.filetransfer.OneToOneFileTransferListener;

import com.orangelabs.rcs.ri.ConnectionManager;
import com.orangelabs.rcs.ri.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.utils.ContactListAdapter;
import com.orangelabs.rcs.ri.utils.ContactUtil;
import com.orangelabs.rcs.ri.utils.FileUtils;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
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
import android.text.format.DateUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

/**
 * Initiate file transfer
 * 
 * @author Jean-Marc AUFFRET
 * @author Philippe LEMORDANT
 */
public class InitiateFileTransfer extends Activity {
    /**
     * Activity result constants
     */
    private final static int SELECT_IMAGE = 0;

    private final static int SELECT_TEXT_FILE = 1;

    /**
     * UI handler
     */
    private final Handler mHandler = new Handler();

    /**
     * Selected filename
     */
    private String mFilename;

    /**
     * Selected fileUri
     */
    private Uri mFile;

    /**
     * Selected filesize (kB)
     */
    private long mFilesize = -1;

    /**
     * File transfer
     */
    private FileTransfer mFileTransfer;

    /**
     * A locker to exit only once
     */
    private LockAccess mExitOnce = new LockAccess();

    /**
     * Progress dialog
     */
    private Dialog mProgressDialog;

    /**
     * API connection manager
     */
    private ConnectionManager mCnxManager;

    /**
     * The log tag for this class
     */
    private static final String LOGTAG = LogUtils
            .getTag(InitiateFileTransfer.class.getSimpleName());

    /**
     * File transfer is resuming
     */
    private boolean mResuming = false;

    /**
     * File transfer identifier
     */
    private String mFtId;

    /**
     * Spinner for contact selection
     */
    private Spinner mSpinner;

    private boolean mTransferred = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ContactId remoteContact = null;
        if (getIntent().getAction() != null) {
            mResuming = getIntent().getAction().equals(FileTransferResumeReceiver.ACTION_FT_RESUME);
        }

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.filetransfer_initiate);

        // Set contact selector
        mSpinner = (Spinner) findViewById(R.id.contact);

        // Set buttons callback
        Button inviteBtn = (Button) findViewById(R.id.invite_btn);
        inviteBtn.setOnClickListener(btnInviteListener);
        inviteBtn.setEnabled(false);
        Button selectBtn = (Button) findViewById(R.id.select_btn);
        selectBtn.setOnClickListener(btnSelectListener);
        selectBtn.setEnabled(false);

        Button pauseBtn = (Button) findViewById(R.id.pause_btn);
        pauseBtn.setOnClickListener(btnPauseListener);
        pauseBtn.setEnabled(false);

        Button resumeBtn = (Button) findViewById(R.id.resume_btn);
        resumeBtn.setOnClickListener(btnResumeListener);
        resumeBtn.setEnabled(false);

        TableRow expiration = (TableRow) findViewById(R.id.expiration);
        expiration.setVisibility(View.GONE);

        // Register to API connection manager
        mCnxManager = ConnectionManager.getInstance();
        if (!mCnxManager.isServiceConnected(RcsServiceName.FILE_TRANSFER)) {
            Utils.showMessageAndExit(this, getString(R.string.label_service_not_available),
                    mExitOnce);
            return;
        }
        mCnxManager.startMonitorServices(this, mExitOnce, RcsServiceName.FILE_TRANSFER);
        FileTransferService fileTransferService = mCnxManager.getFileTransferApi();
        try {
            // Add service listener
            fileTransferService.addEventListener(ftListener);
            if (mResuming) {
                // Get resuming info
                FileTransferDAO ftdao = (FileTransferDAO) (getIntent().getExtras()
                        .getSerializable(FileTransferIntentService.BUNDLE_FTDAO_ID));
                if (ftdao == null) {
                    if (LogUtils.isActive) {
                        Log.e(LOGTAG, "onCreate cannot read File Transfer resuming info");
                    }
                    finish();
                    return;
                }
                remoteContact = ftdao.getContact();
                mFtId = ftdao.getTransferId();
                mFilename = ftdao.getFilename();
                mFilesize = ftdao.getSize();
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                        android.R.layout.simple_spinner_item, new String[] {
                            remoteContact.toString()
                        });
                mSpinner.setAdapter(adapter);
                TextView uriEdit = (TextView) findViewById(R.id.uri);
                TextView sizeEdit = (TextView) findViewById(R.id.size);
                sizeEdit.setText((mFilesize / 1024) + " KB");
                uriEdit.setText(mFilename);
                // Check if session still exists
                if (fileTransferService.getFileTransfer(mFtId) == null) {
                    // Session not found or expired
                    Utils.showMessageAndExit(this,
                            getString(R.string.label_transfer_session_has_expired), mExitOnce);
                    return;
                }
                pauseBtn.setEnabled(true);
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onCreate (file=" + mFilename + ") (size=" + mFilesize
                            + ") (contact=" + remoteContact + ")");
                }
            } else {
                mSpinner.setAdapter(ContactListAdapter.createRcsContactListAdapter(this));
                // Enable button if contact available
                if (mSpinner.getAdapter().getCount() != 0) {
                    selectBtn.setEnabled(true);
                }
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onCreate");
                }
            }
        } catch (RcsServiceNotAvailableException e) {
            Utils.showMessageAndExit(this, getString(R.string.label_api_unavailable), mExitOnce, e);
        } catch (RcsServiceException e) {
            Utils.showMessageAndExit(this, getString(R.string.label_api_failed), mExitOnce, e);
        }
    }

    @Override
    public void onDestroy() {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onDestroy");
        }
        super.onDestroy();
        mCnxManager.stopMonitorServices(this);
        if (mCnxManager.isServiceConnected(RcsServiceName.FILE_TRANSFER)) {
            // Remove file transfer listener
            try {
                mCnxManager.getFileTransferApi().removeEventListener(ftListener);
            } catch (Exception e) {
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
            ContactListAdapter adapter = (ContactListAdapter) mSpinner.getAdapter();
            String phoneNumber = adapter.getSelectedNumber(mSpinner.getSelectedView());
            final ContactId remote = ContactUtil.formatContact(phoneNumber);

            long warnSize = 0;
            FileTransferService fileTransferService = mCnxManager.getFileTransferApi();
            try {
                warnSize = fileTransferService.getConfiguration().getWarnSize();
            } catch (Exception e) {
                Utils.showMessageAndExit(InitiateFileTransfer.this,
                        getString(R.string.label_api_failed), mExitOnce, e);
                return;
            }
            if ((warnSize > 0) && (mFilesize >= warnSize)) {
                // Display a warning message
                AlertDialog.Builder builder = new AlertDialog.Builder(InitiateFileTransfer.this);
                builder.setMessage(getString(R.string.label_sharing_warn_size, mFilesize));
                builder.setCancelable(false);
                builder.setPositiveButton(getString(R.string.label_yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int position) {
                                initiateTransfer(remote);
                            }
                        });
                builder.setNegativeButton(getString(R.string.label_no), null);
                AlertDialog alert = builder.create();
                alert.show();
            } else {
                initiateTransfer(remote);
            }
        }
    };

    private void initiateTransfer(ContactId remote) {
        // Get thumbnail option
        CheckBox ftThumb = (CheckBox) findViewById(R.id.ft_thumb);
        boolean tryToSendFileicon = ftThumb.isChecked();
        String mimeType = getContentResolver().getType(mFile);
        if (tryToSendFileicon && mimeType != null
                && !mimeType.startsWith("image")) {
            tryToSendFileicon = false;
        }
        try {
            /* Only take persistable permission for content Uris */
            FileUtils.tryToTakePersistableContentUriPermission(getApplicationContext(), mFile);

            // Initiate transfer
            mFileTransfer = mCnxManager.getFileTransferApi().transferFile(remote, mFile,
                    tryToSendFileicon);
            mFtId = mFileTransfer.getTransferId();

            Button pauseBtn = (Button) findViewById(R.id.pause_btn);
            pauseBtn.setEnabled(true);

            // Display a progress dialog
            mProgressDialog = Utils.showProgressDialog(this,
                    getString(R.string.label_command_in_progress));
            mProgressDialog.setOnCancelListener(new OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    Toast.makeText(InitiateFileTransfer.this,
                            getString(R.string.label_transfer_cancelled), Toast.LENGTH_SHORT)
                            .show();
                    quitSession();
                }
            });

            // Disable UI
            mSpinner.setEnabled(false);

            // Hide buttons
            Button inviteBtn = (Button) findViewById(R.id.invite_btn);
            inviteBtn.setVisibility(View.INVISIBLE);
            Button selectBtn = (Button) findViewById(R.id.select_btn);
            selectBtn.setVisibility(View.INVISIBLE);
            ftThumb.setVisibility(View.INVISIBLE);
        } catch (Exception e) {
            hideProgressDialog();
            Utils.showMessageAndExit(this, getString(R.string.label_invitation_failed), mExitOnce,
                    e);
        }
    }

    /**
     * Select file button listener
     */
    private OnClickListener btnSelectListener = new OnClickListener() {
        public void onClick(View v) {
            selectDocument();
        }
    };

    /**
     * Display a alert dialog to select the kind of file to transfer
     */
    private void selectDocument() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.label_select_file);
        builder.setCancelable(true);
        builder.setItems(R.array.select_filetotransfer, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == SELECT_IMAGE) {
                    FileUtils.openFile(InitiateFileTransfer.this, "image/*", SELECT_IMAGE);
                } else {
                    FileUtils.openFile(InitiateFileTransfer.this, "text/plain", SELECT_TEXT_FILE);
                }
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

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
        mFile = data.getData();
        TextView uriEdit = (TextView) findViewById(R.id.uri);
        TextView sizeEdit = (TextView) findViewById(R.id.size);
        Button inviteBtn = (Button) findViewById(R.id.invite_btn);
        switch (requestCode) {
            case SELECT_IMAGE:
            case SELECT_TEXT_FILE:
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "Selected file uri:" + mFile);
                }
                // Display file info
                // Display the selected filename attribute
                try {
                    // Get image filename and size
                    mFilename = FileUtils.getFileName(this, mFile);
                    mFilesize = FileUtils.getFileSize(this, mFile) / 1024;
                    sizeEdit.setText(mFilesize + " KB");
                    uriEdit.setText(mFilename);
                    if (LogUtils.isActive) {
                        Log.i(LOGTAG, "Select file " + mFilename + " of size " + mFilesize
                                + " file=" + mFile);
                    }
                } catch (Exception e) {
                    if (LogUtils.isActive) {
                        Log.e(LOGTAG, e.getMessage(), e);
                    }
                    mFilesize = -1;
                    sizeEdit.setText("Unknown");
                    uriEdit.setText("Unknown");
                }
                // Enable invite button
                inviteBtn.setEnabled(true);
                break;
        }
    }

    /**
     * Hide progress dialog
     */
    public void hideProgressDialog() {
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
    private void updateProgressBar(long currentSize, long totalSize) {
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
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "quitSession");
        }
        // Stop session
        try {
            if (mFileTransfer != null && !mTransferred) {
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

    /**
     * Pause button listener
     */
    private OnClickListener btnPauseListener = new OnClickListener() {
        public void onClick(View v) {
            Button resumeBtn = (Button) findViewById(R.id.resume_btn);
            resumeBtn.setEnabled(true);
            Button pauseBtn = (Button) findViewById(R.id.pause_btn);
            pauseBtn.setEnabled(false);
            try {
                mFileTransfer.pauseTransfer();
            } catch (RcsServiceException e) {
                hideProgressDialog();
                Utils.showMessageAndExit(InitiateFileTransfer.this,
                        getString(R.string.label_pause_failed), mExitOnce, e);
            }
        }
    };

    /**
     * Resume button listener
     */
    private OnClickListener btnResumeListener = new OnClickListener() {
        public void onClick(View v) {
            Button resumeBtn = (Button) findViewById(R.id.resume_btn);
            resumeBtn.setEnabled(false);
            Button pauseBtn = (Button) findViewById(R.id.pause_btn);
            pauseBtn.setEnabled(true);
            try {
                mFileTransfer.resumeTransfer();
            } catch (RcsServiceException e) {
                hideProgressDialog();
                Utils.showMessageAndExit(InitiateFileTransfer.this,
                        getString(R.string.label_resume_failed), mExitOnce, e);
            }
        }
    };

    /**
     * File transfer listener
     */
    private OneToOneFileTransferListener ftListener = new OneToOneFileTransferListener() {

        @Override
        public void onProgressUpdate(ContactId contact, String transferId, final long currentSize,
                final long totalSize) {
            // Discard event if not for current transferId
            if (InitiateFileTransfer.this.mFtId == null
                    || !InitiateFileTransfer.this.mFtId.equals(transferId)) {
                return;
            }
            mHandler.post(new Runnable() {
                public void run() {
                    // Display transfer progress
                    updateProgressBar(currentSize, totalSize);
                }
            });
        }

        @Override
        public void onStateChanged(ContactId contact, String transferId,
                final FileTransfer.State state, final FileTransfer.ReasonCode reasonCode) {
            // Discard event if not for current transferId
            if (InitiateFileTransfer.this.mFtId == null
                    || !InitiateFileTransfer.this.mFtId.equals(transferId)) {
                return;
            }
            final String _reasonCode = RiApplication.sFileTransferReasonCodes[reasonCode.toInt()];
            final String _state = RiApplication.sFileTransferStates[state.toInt()];
            if (LogUtils.isActive) {
                Log.d(LOGTAG,
                        new StringBuilder("onStateChanged contact=").append(contact)
                                .append(" transferId=").append(transferId).append(" state=")
                                .append(_state).append(" reason=").append(_reasonCode).toString());
            }
            mHandler.post(new Runnable() {
                public void run() {
                    TextView statusView = (TextView) findViewById(R.id.progress_status);
                    switch (state) {
                        case STARTED:
                            // Session is well established : hide progress
                            // dialog
                            hideProgressDialog();
                            // Display session status
                            statusView.setText(_state);
                            break;

                        case ABORTED:
                            // Session is aborted: hide progress dialog then
                            // exit
                            hideProgressDialog();
                            Utils.showMessageAndExit(InitiateFileTransfer.this,
                                    getString(R.string.label_transfer_aborted, _reasonCode),
                                    mExitOnce);
                            break;

                        case REJECTED:
                            // Session is rejected: hide progress dialog then
                            // exit
                            hideProgressDialog();
                            Utils.showMessageAndExit(InitiateFileTransfer.this,
                                    getString(R.string.label_transfer_rejected, _reasonCode),
                                    mExitOnce);
                            break;

                        case FAILED:
                            // Session failed: hide progress dialog then exit
                            hideProgressDialog();
                            Utils.showMessageAndExit(InitiateFileTransfer.this,
                                    getString(R.string.label_transfer_failed, _reasonCode),
                                    mExitOnce);
                            break;

                        case TRANSFERRED:
                            // Hide progress dialog
                            hideProgressDialog();
                            // Display transfer progress
                            statusView.setText(_state);
                            // Hide buttons Pause and Resume
                            Button pauseBtn = (Button) findViewById(R.id.pause_btn);
                            pauseBtn.setVisibility(View.INVISIBLE);
                            Button resumeBtn = (Button) findViewById(R.id.resume_btn);
                            resumeBtn.setVisibility(View.INVISIBLE);
                            mTransferred = true;

                            try {
                                displayFileExpiration(mFileTransfer.getFileExpiration());
                                Uri fileIconUri = mFileTransfer.getFileIcon();
                                long fileIconExpiration = mFileTransfer.getFileIconExpiration();
                                if (LogUtils.isActive) {
                                    Log.d(LOGTAG,
                                            new StringBuilder("File transferred icon uri= ")
                                                    .append(fileIconUri).append(" expiration=")
                                                    .append(fileIconExpiration).toString());
                                }
                            } catch (RcsServiceException e) {
                                e.printStackTrace();
                            }
                            break;

                        default:
                            statusView.setText(_state);
                            if (LogUtils.isActive) {
                                Log.d(LOGTAG, "onStateChanged ".concat(getString(
                                        R.string.label_ft_state_changed, _state, _reasonCode)));
                            }
                    }
                }

            });
        }

        @Override
        public void onDeleted(ContactId contact, Set<String> transferIds) {
            if (LogUtils.isActive) {
                Log.w(LOGTAG,
                        new StringBuilder("onDeleted contact=").append(contact)
                                .append(" transferIds=").append(transferIds).toString());
            }
        }
    };

    private void displayFileExpiration(long fileExpiration) {
        if (FileTransferLog.UNKNOWN_EXPIRATION == fileExpiration) {
            return;
        }
        TableRow expirationTableRow = (TableRow) findViewById(R.id.expiration);
        expirationTableRow.setVisibility(View.VISIBLE);
        TextView expirationView = (TextView) findViewById(R.id.value_expiration);
        CharSequence expirationDate = DateUtils.getRelativeTimeSpanString(fileExpiration,
                System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE);
        expirationView.setText(expirationDate);
    }
}
