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
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransferIntent;
import com.gsma.services.rcs.filetransfer.FileTransferLog;
import com.gsma.services.rcs.filetransfer.FileTransferService;
import com.gsma.services.rcs.filetransfer.OneToOneFileTransferListener;

import com.orangelabs.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.api.connection.utils.ExceptionUtil;
import com.orangelabs.rcs.api.connection.utils.RcsActivity;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.utils.ContactListAdapter;
import com.orangelabs.rcs.ri.utils.ContactUtil;
import com.orangelabs.rcs.ri.utils.FileUtils;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsSessionUtil;
import com.orangelabs.rcs.ri.utils.Utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
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
public class InitiateFileTransfer extends RcsActivity {

    private final static int SELECT_IMAGE = 0;

    private final static int SELECT_TEXT_FILE = 1;

    private static final String BUNDLE_FTDAO_ID = "ftdao";

    /**
     * UI handler
     */
    private final Handler mHandler = new Handler();

    private String mFilename;

    private Uri mFile;

    private long mFilesize = -1;

    private FileTransfer mFileTransfer;

    private Dialog mProgressDialog;

    private static final String LOGTAG = LogUtils
            .getTag(InitiateFileTransfer.class.getSimpleName());

    private String mFileTransferId;

    /**
     * Spinner for contact selection
     */
    private Spinner mSpinner;

    private Button mResumeBtn;

    private Button mPauseBtn;

    private Button mInviteBtn;

    private Button mSelectBtn;

    private OneToOneFileTransferListener mFileTransferListener;

    private OnClickListener mBtnInviteListener;

    private OnClickListener mBtnSelectListener;

    private OnClickListener mBtnPauseListener;

    private OnClickListener mBtnResumeListener;

    private FileTransferService mFileTransferService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        intitialize();
        ContactId remoteContact;
        Intent intent = getIntent();
        boolean resuming = FileTransferIntent.ACTION_RESUME.equals(intent.getAction());

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.filetransfer_initiate);

        /* Set contact selector */
        mSpinner = (Spinner) findViewById(R.id.contact);

        /* Set buttons callback */
        mInviteBtn = (Button) findViewById(R.id.invite_btn);
        mInviteBtn.setOnClickListener(mBtnInviteListener);
        mInviteBtn.setEnabled(false);
        mSelectBtn = (Button) findViewById(R.id.select_btn);
        mSelectBtn.setOnClickListener(mBtnSelectListener);
        mSelectBtn.setEnabled(false);

        mPauseBtn = (Button) findViewById(R.id.pause_btn);
        mPauseBtn.setOnClickListener(mBtnPauseListener);
        mPauseBtn.setEnabled(false);

        mResumeBtn = (Button) findViewById(R.id.resume_btn);
        mResumeBtn.setOnClickListener(mBtnResumeListener);
        mResumeBtn.setEnabled(false);

        TableRow expiration = (TableRow) findViewById(R.id.expiration);
        expiration.setVisibility(View.GONE);

        if (!isServiceConnected(RcsServiceName.FILE_TRANSFER)) {
            showMessageThenExit(R.string.label_service_not_available);
            return;
        }
        startMonitorServices(RcsServiceName.FILE_TRANSFER);
        mFileTransferService = getFileTransferApi();
        try {
            mFileTransferService.addEventListener(mFileTransferListener);
            if (resuming) {
                /* Get resuming info */
                FileTransferDAO ftdao = (FileTransferDAO) (intent.getExtras()
                        .getSerializable(BUNDLE_FTDAO_ID));
                if (ftdao == null) {
                    if (LogUtils.isActive) {
                        Log.e(LOGTAG, "onCreate cannot read File Transfer resuming info");
                    }
                    finish();
                    return;
                }
                remoteContact = ftdao.getContact();
                mFileTransferId = ftdao.getTransferId();
                mFilename = ftdao.getFilename();
                mFilesize = ftdao.getSize();
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_item, new String[] {
                            remoteContact.toString()
                        });
                mSpinner.setAdapter(adapter);
                TextView uriEdit = (TextView) findViewById(R.id.uri);
                TextView sizeEdit = (TextView) findViewById(R.id.size);
                sizeEdit.setText((mFilesize / 1024) + " KB");
                uriEdit.setText(mFilename);
                /* Check if session still exists */
                if (mFileTransferService.getFileTransfer(mFileTransferId) == null) {
                    /* Session not found or expired */
                    showMessageThenExit(R.string.label_transfer_session_has_expired);
                    return;
                }
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onCreate (file=" + mFilename + ") (size=" + mFilesize
                            + ") (contact=" + remoteContact + ")");
                }
            } else {
                mSpinner.setAdapter(ContactListAdapter.createRcsContactListAdapter(this));
                /* Enable button if contact available */
                if (mSpinner.getAdapter().getCount() != 0) {
                    mSelectBtn.setEnabled(true);
                }
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onCreate");
                }
            }

        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    @Override
    public void onDestroy() {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onDestroy");
        }
        super.onDestroy();
        if (mFileTransferService != null && isServiceConnected(RcsServiceName.FILE_TRANSFER)) {
            // Remove file transfer listener
            try {
                mFileTransferService.removeEventListener(mFileTransferListener);
            } catch (RcsServiceException e) {
                Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
            }
        }
    }

    private void initiateTransfer(ContactId remote) {
        /* Get thumbnail option */
        CheckBox ftThumb = (CheckBox) findViewById(R.id.ft_thumb);
        boolean tryToSendFileicon = ftThumb.isChecked();
        String mimeType = getContentResolver().getType(mFile);
        if (tryToSendFileicon && mimeType != null && !mimeType.startsWith("image")) {
            tryToSendFileicon = false;
        }
        try {
            /* Only take persistable permission for content Uris */
            FileUtils.tryToTakePersistableContentUriPermission(getApplicationContext(), mFile);
            /* Initiate transfer */
            mFileTransfer = mFileTransferService.transferFile(remote, mFile, tryToSendFileicon);
            mFileTransferId = mFileTransfer.getTransferId();
            mProgressDialog = showProgressDialog(getString(R.string.label_command_in_progress));
            mProgressDialog.setOnCancelListener(new OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    Toast.makeText(InitiateFileTransfer.this,
                            getString(R.string.label_transfer_cancelled), Toast.LENGTH_SHORT)
                            .show();
                    quitSession();
                }
            });
            /* Disable UI */
            mSpinner.setEnabled(false);
            /* Hide buttons */
            mInviteBtn.setVisibility(View.INVISIBLE);
            mSelectBtn.setVisibility(View.INVISIBLE);
            ftThumb.setVisibility(View.INVISIBLE);

        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

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
        registerDialog(builder.show());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK || (data == null) || (data.getData() == null)) {
            return;
        }
        mFile = data.getData();
        TextView uriEdit = (TextView) findViewById(R.id.uri);
        TextView sizeEdit = (TextView) findViewById(R.id.size);
        switch (requestCode) {
            case SELECT_IMAGE:
            case SELECT_TEXT_FILE:
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "Selected file uri:" + mFile);
                }
                /*
                 * Display file info and the selected filename attribute.
                 */
                mFilename = FileUtils.getFileName(this, mFile);
                mFilesize = FileUtils.getFileSize(this, mFile) / 1024;
                sizeEdit.setText(mFilesize + " KB");
                uriEdit.setText(mFilename);
                if (LogUtils.isActive) {
                    Log.i(LOGTAG, "Select file " + mFilename + " of size " + mFilesize + " file="
                            + mFile);
                }
                mInviteBtn.setEnabled(true);
                break;
        }
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    private void updateProgressBar(long currentSize, long totalSize) {
        TextView statusView = (TextView) findViewById(R.id.progress_status);
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        statusView.setText(Utils.getProgressLabel(currentSize, totalSize));
        double position = ((double) currentSize / (double) totalSize) * 100.0;
        progressBar.setProgress((int) position);
    }

    private void quitSession() {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "quitSession");
        }
        try {
            if (mFileTransfer != null
                    && RcsSessionUtil.isAllowedToAbortFileTransferSession(mFileTransfer)) {
                mFileTransfer.abortTransfer();
            }
        } catch (RcsServiceException e) {
            showException(e);

        } finally {
            mFileTransfer = null;
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

    private void intitialize() {
        mFileTransferListener = new OneToOneFileTransferListener() {

            @Override
            public void onProgressUpdate(ContactId contact, String transferId,
                    final long currentSize, final long totalSize) {
                /* Discard event if not for current transferId */
                if (InitiateFileTransfer.this.mFileTransferId == null
                        || !InitiateFileTransfer.this.mFileTransferId.equals(transferId)) {
                    return;
                }
                mHandler.post(new Runnable() {
                    public void run() {
                        updateProgressBar(currentSize, totalSize);
                    }
                });
            }

            @Override
            public void onStateChanged(ContactId contact, String transferId,
                    final FileTransfer.State state, final FileTransfer.ReasonCode reasonCode) {
                /* Discard event if not for current transferId */
                if (InitiateFileTransfer.this.mFileTransferId == null
                        || !InitiateFileTransfer.this.mFileTransferId.equals(transferId)) {
                    return;
                }
                final String _reasonCode = RiApplication.sFileTransferReasonCodes[reasonCode
                        .toInt()];
                final String _state = RiApplication.sFileTransferStates[state.toInt()];
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onStateChanged contact=" + contact + " transferId=" + transferId
                            + " state=" + _state + " reason=" + _reasonCode);
                }
                mHandler.post(new Runnable() {

                    public void run() {
                        if (mFileTransfer != null) {
                            try {
                                mResumeBtn.setEnabled(mFileTransfer.isAllowedToResumeTransfer());

                            } catch (RcsServiceException e) {
                                mResumeBtn.setEnabled(false);
                                showException(e);
                            }
                            try {
                                mPauseBtn.setEnabled(mFileTransfer.isAllowedToPauseTransfer());

                            } catch (RcsServiceException e) {
                                mPauseBtn.setEnabled(false);
                                showException(e);
                            }
                        }
                        TextView statusView = (TextView) findViewById(R.id.progress_status);
                        switch (state) {
                            case STARTED:
                                /* Session is well established : hide progress dialog */
                                hideProgressDialog();
                                /* Display session status */
                                statusView.setText(_state);
                                break;

                            case ABORTED:
                                showMessageThenExit(getString(R.string.label_transfer_aborted,
                                        _reasonCode));
                                break;

                            case REJECTED:
                                showMessageThenExit(getString(R.string.label_transfer_rejected,
                                        _reasonCode));
                                break;

                            case FAILED:
                                showMessageThenExit(getString(R.string.label_transfer_failed,
                                        _reasonCode));
                                break;

                            case TRANSFERRED:
                                hideProgressDialog(); // TODO check if required
                                /* Display transfer progress */
                                statusView.setText(_state);

                                try {
                                    displayFileExpiration(mFileTransfer.getFileExpiration());
                                    Uri fileIconUri = mFileTransfer.getFileIcon();
                                    long fileIconExpiration = mFileTransfer.getFileIconExpiration();
                                    if (LogUtils.isActive) {
                                        Log.d(LOGTAG, "File transferred icon uri= " + fileIconUri
                                                + " expiration=" + fileIconExpiration);
                                    }
                                } catch (RcsServiceException e) {
                                    showException(e);
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
                    Log.w(LOGTAG, "onDeleted contact=" + contact + " transferIds=" + transferIds);
                }
            }
        };

        mBtnInviteListener = new OnClickListener() {
            public void onClick(View v) {
                ContactListAdapter adapter = (ContactListAdapter) mSpinner.getAdapter();
                String phoneNumber = adapter.getSelectedNumber(mSpinner.getSelectedView());
                final ContactId remote = ContactUtil.formatContact(phoneNumber);

                long warnSize;
                try {
                    warnSize = mFileTransferService.getConfiguration().getWarnSize();

                } catch (RcsServiceException e) {
                    showExceptionThenExit(e);
                    return;
                }
                if ((warnSize > 0) && (mFilesize >= warnSize)) {
                    // Display a warning message
                    AlertDialog.Builder builder = new AlertDialog.Builder(InitiateFileTransfer.this);
                    builder.setMessage(getString(R.string.label_sharing_warn_size, mFilesize));
                    builder.setCancelable(false);
                    builder.setPositiveButton(R.string.label_yes,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int position) {
                                    initiateTransfer(remote);
                                }
                            });
                    builder.setNegativeButton(R.string.label_no, null);
                    registerDialog(builder.show());

                } else {
                    initiateTransfer(remote);
                }
            }
        };

        mBtnSelectListener = new OnClickListener() {
            public void onClick(View v) {
                selectDocument();
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

    /**
     * Forge intent to resume Originating File Transfer
     * 
     * @param ctx The context
     * @param ftDao The FileTransfer DAO
     * @param resume intent
     * @return intent
     */
    public static Intent forgeResumeIntent(Context ctx, FileTransferDAO ftDao, Intent resume) {
        resume.setClass(ctx, InitiateFileTransfer.class);
        resume.addFlags(Intent.FLAG_FROM_BACKGROUND | Intent.FLAG_ACTIVITY_NEW_TASK);
        /* Save FileTransferDAO into intent */
        Bundle bundle = new Bundle();
        bundle.putParcelable(BUNDLE_FTDAO_ID, ftDao);
        resume.putExtras(bundle);
        return resume;
    }
}
