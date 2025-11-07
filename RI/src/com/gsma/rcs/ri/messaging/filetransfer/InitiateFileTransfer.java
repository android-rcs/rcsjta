/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.ri.messaging.filetransfer;

import static com.gsma.rcs.ri.utils.FileUtils.takePersistableContentUriPermission;

import com.gsma.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.gsma.rcs.api.connection.utils.ExceptionUtil;
import com.gsma.rcs.api.connection.utils.RcsActivity;
import com.gsma.rcs.ri.R;
import com.gsma.rcs.ri.RiApplication;
import com.gsma.rcs.ri.utils.ContactListAdapter;
import com.gsma.rcs.ri.utils.ContactUtil;
import com.gsma.rcs.ri.utils.FileUtils;
import com.gsma.rcs.ri.utils.LogUtils;
import com.gsma.rcs.ri.utils.RcsSessionUtil;
import com.gsma.rcs.ri.utils.Utils;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransferIntent;
import com.gsma.services.rcs.filetransfer.FileTransferLog;
import com.gsma.services.rcs.filetransfer.FileTransferService;
import com.gsma.services.rcs.filetransfer.OneToOneFileTransferListener;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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

import java.util.Set;

/**
 * Initiate file transfer
 * 
 * @author Jean-Marc AUFFRET
 * @author Philippe LEMORDANT
 */
public class InitiateFileTransfer extends RcsActivity {

    private final static int RC_SELECT_IMAGE = 0;
    private final static int RC_SELECT_TEXT_FILE = 1;
    private final static int RC_SELECT_AUDIO = 2;
    private final static int RC_RECORD_AUDIO = 3;

    private static final String BUNDLE_FTDAO_ID = "ftdao";

    private static final String LOGTAG = LogUtils.getTag(InitiateFileTransfer.class.getName());

    /**
     * UI handler
     */
    private final Handler mHandler = new Handler();
    private String mFilename;
    private Uri mFile;
    private long mFilesize = -1;
    private FileTransfer mFileTransfer;
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
    private FileTransferService mFileTransferService;
    private TextView mUriTextView;
    private TextView mSizeTextView;
    private CheckBox mIconCheckBox;
    private CheckBox mAudioMessageCheckBox;
    private TextView mStatusView;
    private ProgressBar mProgressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        boolean resuming = FileTransferIntent.ACTION_RESUME.equals(intent.getAction());
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.filetransfer_initiate);
        intitialize();
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
                ContactId remoteContact = ftdao.getContact();
                mFileTransferId = ftdao.getTransferId();
                mFilename = ftdao.getFilename();
                mFilesize = ftdao.getSize();
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_item, new String[] {
                            remoteContact.toString()
                        });
                mSpinner.setAdapter(adapter);
                mSizeTextView.setText(FileUtils.humanReadableByteCount(mFilesize, true));
                mUriTextView.setText(mFilename);
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
        boolean tryToSendFileicon = mIconCheckBox.isChecked();
        String mimeType = getContentResolver().getType(mFile);
        if (tryToSendFileicon && mimeType != null && !mimeType.startsWith("image")) {
            tryToSendFileicon = false;
        }
        try {
            FileTransfer.Disposition dispo = (mAudioMessageCheckBox.isChecked()) ? FileTransfer.Disposition.RENDER
                    : FileTransfer.Disposition.ATTACH;
            /* Only take persistable permission for content Uris */
            takePersistableContentUriPermission(this, mFile);
            /* Initiate transfer */
            mFileTransfer = mFileTransferService.transferFile(remote, mFile, dispo,
                    tryToSendFileicon);
            if (mFileTransfer != null) {
                mFileTransferId = mFileTransfer.getTransferId();
            } else {
                Log.e(LOGTAG, "Cannot initiate transfer: ID not found");
                return;
            }
            /* Disable UI */
            mSpinner.setEnabled(false);
            /* Hide buttons */
            mInviteBtn.setVisibility(View.INVISIBLE);
            mSelectBtn.setVisibility(View.INVISIBLE);
            mIconCheckBox.setEnabled(false);
            mAudioMessageCheckBox.setEnabled(false);

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
                if (RC_SELECT_IMAGE == which) {
                    FileUtils.openFile(InitiateFileTransfer.this, "image/*", RC_SELECT_IMAGE);
                    return;
                }
                if (RC_SELECT_TEXT_FILE == which) {
                    FileUtils
                            .openFile(InitiateFileTransfer.this, "text/plain", RC_SELECT_TEXT_FILE);
                    return;
                }
                FileUtils.openFile(InitiateFileTransfer.this, "audio/*", RC_SELECT_AUDIO);
            }
        });

        registerDialog(builder.show());
    }

    private void displaySelectedFileInfo() {
        /*
         * Display file info and the selected filename attribute.
         */
        mFilename = FileUtils.getFileName(this, mFile);
        mFilesize = FileUtils.getFileSize(this, mFile);
        mSizeTextView.setText(FileUtils.humanReadableByteCount(mFilesize, true));
        mUriTextView.setText(mFilename);
        if (LogUtils.isActive) {
            Log.i(LOGTAG, "Select file " + mFilename + " of size " + mFilesize);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        switch (requestCode) {
            case RC_SELECT_IMAGE:
            case RC_SELECT_TEXT_FILE:
            case RC_SELECT_AUDIO:
                if (data.getData() == null) {
                    return;
                }
                mFile = data.getData();
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "Selected file uri:".concat(mFile.toString()));
                }
                displaySelectedFileInfo();
                mInviteBtn.setEnabled(true);
                if (RC_SELECT_AUDIO == requestCode) {
                    mAudioMessageCheckBox.setEnabled(true);
                    mIconCheckBox.setEnabled(false);
                    mIconCheckBox.setChecked(false);
                } else {
                    mIconCheckBox.setEnabled(true);
                    mAudioMessageCheckBox.setChecked(false);
                    mAudioMessageCheckBox.setEnabled(false);
                }
                break;

            case RC_RECORD_AUDIO:
                mFile = data.getData();
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "Created audio file:" + mFile);
                }
                displaySelectedFileInfo();
                mInviteBtn.setEnabled(true);
                mAudioMessageCheckBox.setEnabled(true);
                mIconCheckBox.setEnabled(false);
                mIconCheckBox.setChecked(false);
                break;
        }
    }

    private void updateProgressBar(long currentSize, long totalSize) {
        mStatusView.setText(Utils.getProgressLabel(currentSize, totalSize));
        double position = ((double) currentSize / (double) totalSize) * 100.0;
        mProgressBar.setProgress((int) position);
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
        inflater.inflate(R.menu.menu_initiate_ft, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.menu_record_audio);
        item.setVisible(mFileTransfer == null);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_record_audio) {
            startActivityForResult(new Intent(this, AudioMessageRecordActivity.class),
                    RC_RECORD_AUDIO);
        } else if (itemId == R.id.menu_close_session) {
            quitSession();
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
        OnClickListener btnInviteListener = new OnClickListener() {
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
        OnClickListener btnSelectListener = new OnClickListener() {
            public void onClick(View v) {
                selectDocument();
            }
        };
        OnClickListener btnPauseListener = new OnClickListener() {
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
        OnClickListener btnResumeListener = new OnClickListener() {
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
        /* Set contact selector */
        mSpinner = (Spinner) findViewById(R.id.contact);

        /* Set buttons callback */
        mInviteBtn = (Button) findViewById(R.id.invite_btn);
        mInviteBtn.setOnClickListener(btnInviteListener);
        mInviteBtn.setEnabled(false);
        mSelectBtn = (Button) findViewById(R.id.select_btn);
        mSelectBtn.setOnClickListener(btnSelectListener);
        mSelectBtn.setEnabled(false);

        mPauseBtn = (Button) findViewById(R.id.pause_btn);
        mPauseBtn.setOnClickListener(btnPauseListener);
        mPauseBtn.setEnabled(false);

        mResumeBtn = (Button) findViewById(R.id.resume_btn);
        mResumeBtn.setOnClickListener(btnResumeListener);
        mResumeBtn.setEnabled(false);

        mUriTextView = (TextView) findViewById(R.id.uri);
        mUriTextView.setText("");
        mSizeTextView = (TextView) findViewById(R.id.size);
        mSizeTextView.setText("");

        mStatusView = (TextView) findViewById(R.id.progress_status);
        mStatusView.setText("");
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);

        mIconCheckBox = (CheckBox) findViewById(R.id.ft_thumb);

        mAudioMessageCheckBox = (CheckBox) findViewById(R.id.send_audio_msg);

        TableRow expiration = (TableRow) findViewById(R.id.expiration);
        expiration.setVisibility(View.GONE);

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
                        switch (state) {
                            case STARTED:
                                /* Display session status */
                                mStatusView.setText(_state);
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
                                /* Display transfer progress */
                                mStatusView.setText(_state);

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
                                mStatusView.setText(_state);
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
