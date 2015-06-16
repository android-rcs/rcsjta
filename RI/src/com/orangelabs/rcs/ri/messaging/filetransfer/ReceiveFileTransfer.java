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

import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransferLog;
import com.gsma.services.rcs.filetransfer.FileTransferService;
import com.gsma.services.rcs.filetransfer.GroupFileTransferListener;
import com.gsma.services.rcs.filetransfer.OneToOneFileTransferListener;
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo;

import com.orangelabs.rcs.ri.ConnectionManager;
import com.orangelabs.rcs.ri.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsDisplayName;
import com.orangelabs.rcs.ri.utils.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.provider.MediaStore;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Set;

/**
 * Received file transfer
 * 
 * @author Jean-Marc AUFFRET
 * @author YPLO6403
 */
public class ReceiveFileTransfer extends Activity {
    /**
     * UI mHandler
     */
    private final Handler mHandler = new Handler();

    private FileTransfer mFileTransfer;

    private boolean mResuming = false;

    private FileTransferDAO mFtDao;

    private boolean mGgroupFileTransfer = false;

    /**
     * A locker to exit only once
     */
    private LockAccess mExitOnce = new LockAccess();

    /**
     * API connection manager
     */
    private ConnectionManager mCnxManager;

    private boolean mTransferred = false;

    private String mTransferId;

    private Button mPauseBtn;

    private Button mResumeBtn;

    private static final String LOGTAG = LogUtils.getTag(ReceiveFileTransfer.class.getSimpleName());

    private static final String VCARD_MIME_TYPE = "text/x-vcard";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.filetransfer_receive);

        /* Set pause and resume button */
        mPauseBtn = (Button) findViewById(R.id.pause_btn);
        mPauseBtn.setOnClickListener(btnPauseListener);
        mPauseBtn.setEnabled(false);
        mResumeBtn = (Button) findViewById(R.id.resume_btn);
        mResumeBtn.setOnClickListener(btnResumeListener);
        mResumeBtn.setEnabled(false);

        /* Register to API connection manager */
        mCnxManager = ConnectionManager.getInstance();
        if (!mCnxManager.isServiceConnected(RcsServiceName.FILE_TRANSFER, RcsServiceName.CONTACT)) {
            Utils.showMessageAndExit(this, getString(R.string.label_service_not_available),
                    mExitOnce);
        } else {
            mCnxManager.startMonitorServices(this, mExitOnce, RcsServiceName.FILE_TRANSFER,
                    RcsServiceName.CONTACT);
            processIntent(getIntent(), false);
            if (mFileTransfer == null) {
                return;
            }
            FileTransferService fileTransferService = mCnxManager.getFileTransferApi();
            try {
                if (mGgroupFileTransfer) {
                    fileTransferService.addEventListener(groupFtListener);
                } else {
                    fileTransferService.addEventListener(ftListener);
                }
            } catch (RcsServiceException e) {
                Utils.showMessageAndExit(this, getString(R.string.label_api_failed, mExitOnce),
                        mExitOnce);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        processIntent(intent, true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCnxManager.stopMonitorServices(this);
        if (mCnxManager.isServiceConnected(RcsServiceName.FILE_TRANSFER)) {
            /* Remove service listener */
            try {
                if (mGgroupFileTransfer) {
                    mCnxManager.getFileTransferApi().removeEventListener(groupFtListener);
                } else {
                    mCnxManager.getFileTransferApi().removeEventListener(ftListener);
                }
            } catch (Exception e) {
                if (LogUtils.isActive) {
                    Log.e(LOGTAG, "Failed to remove listener", e);
                }
            }
        }
    }

    void processIntent(Intent intent, boolean newIntent) {
        mFtDao = (FileTransferDAO) (intent.getExtras()
                .getParcelable(FileTransferIntentService.BUNDLE_FTDAO_ID));
        if (mFtDao == null) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "processIntent cannot read File Transfer invitation (newIntent="
                        + newIntent + ")");
            }
            finish();
            return;
        }
        /* Get the file transfer session */
        mTransferId = mFtDao.getTransferId();
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "processIntent (newIntent=" + newIntent + ") " + mFtDao);
        }

        // Get invitation info
        mResuming = intent.getAction().equals(FileTransferResumeReceiver.ACTION_FT_RESUME);
        mGgroupFileTransfer = (intent.getBooleanExtra(FileTransferIntentService.EXTRA_GROUP_FILE,
                false));

        String from = RcsDisplayName.getInstance(this).getDisplayName(mFtDao.getContact());
        TextView fromTextView = (TextView) findViewById(R.id.from);
        fromTextView.setText(getString(R.string.label_from_args, from));

        String size = getString(R.string.label_file_size, mFtDao.getSize() / 1024);
        TextView sizeTxt = (TextView) findViewById(R.id.image_size);
        sizeTxt.setText(size);
        TextView filenameTxt = (TextView) findViewById(R.id.image_filename);
        filenameTxt.setText(getString(R.string.label_filename, mFtDao.getFilename()));

        TextView expirationView = (TextView) findViewById(R.id.expiration);
        long fileExpiration = mFtDao.getFileExpiration();
        if (fileExpiration != FileTransferLog.UNKNOWN_EXPIRATION) {
            CharSequence expiration = DateUtils.getRelativeTimeSpanString(
                    mFtDao.getFileExpiration(), System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE);
            expirationView.setText(getString(R.string.label_expiration_args, expiration));
        } else {
            expirationView.setVisibility(View.GONE);
        }
        FileTransferService ftApi = mCnxManager.getFileTransferApi();
        try {
            mFileTransfer = ftApi.getFileTransfer(mTransferId);
            if (mFileTransfer == null) {
                if (FileTransfer.State.TRANSFERRED == mFtDao.getState()) {
                    displayTransferredFile();
                    return;

                } else {
                    String reasonCode = RiApplication.sFileTransferReasonCodes[mFtDao
                            .getReasonCode().toInt()];
                    if (LogUtils.isActive) {
                        Log.e(LOGTAG,
                                "Transfer ID=" + mTransferId + " failed state=" + mFtDao.getState()
                                        + " reason=" + reasonCode);
                    }
                    // Transfer failed
                    Utils.showMessageAndExit(this,
                            getString(R.string.label_transfer_failed, reasonCode), mExitOnce);
                    return;

                }

            }

            // Do not consider acceptance if mResuming
            if (mResuming) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "processIntent resuming ".concat(mTransferId));
                }
                return;
            }

            FileTransfer.State state = mFileTransfer.getState();

            // Check if not already accepted by the stack
            if (FileTransfer.State.INVITED != state) {
                // File Transfer is auto accepted by the stack. Check capacity
                isCapacityOk(mFtDao.getSize());

                if (FileTransfer.State.TRANSFERRED == state) {
                    if (LogUtils.isActive) {
                        Log.d(LOGTAG, "processIntent file is transferred ".concat(mTransferId));
                    }
                    displayTransferredFile();
                }

            } else {
                // File Transfer must be accepted/rejected by user
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "Wait for user acceptance (transferId=" + mTransferId + ")");
                }
                // @formatter:off

                // The following code is intentionally commented to test the
                // CORE.
                // UI should check the file size to cancel if it is too big.
                // if (isCapacityOk(fileSize) == false) {
                // rejectInvitation();
                // return;
                // }

                // @formatter:on

                // Manual accept
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                View titleView = getLayoutInflater().inflate(R.layout.filetransfer_custom_title, null);
                builder.setCustomTitle(titleView);
                builder.setMessage(getString(R.string.label_ft_from_size, from,
                        mFtDao.getSize() / 1024));
                builder.setCancelable(false);
                // Make sure progress bar is at the beginning
                ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
                progressBar.setProgress(0);
                ImageView iconView = (ImageView)titleView.findViewById(R.id.filetransfer_alert_title_icon);
                if (mFtDao.getThumbnail() != null) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),
                                mFtDao.getThumbnail());
                        iconView.setImageBitmap(bitmap);
                    } catch (Exception e) {
                        if (LogUtils.isActive) {
                            Log.e(LOGTAG, "Failed to load thumbnail", e);
                        }
                    }
                } else {
                    if (VCARD_MIME_TYPE.equals(mFtDao.getMimeType())) {
                        iconView.setImageResource(R.drawable.ri_contact_card_icon);
                    } else {
                        iconView.setImageResource(R.drawable.ri_notif_file_transfer_icon);
                    }
                }
                builder.setPositiveButton(getString(R.string.label_accept), acceptBtnListener);
                builder.setNegativeButton(getString(R.string.label_decline), declineBtnListener);
                builder.show();
            }
        } catch (RcsServiceNotAvailableException e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, e.getMessage(), e);
            }
            Utils.showMessageAndExit(this, getString(R.string.label_api_unavailable), mExitOnce);
        } catch (RcsServiceException e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, e.getMessage(), e);
            }
            Utils.showMessageAndExit(this, getString(R.string.label_api_failed), mExitOnce);
        }
    }

    /**
     * Accept invitation
     */
    private void acceptInvitation() {
        try {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "Accept invitation (transferId=" + mTransferId + ")");
            }
            mFileTransfer.acceptInvitation();
        } catch (Exception e) {
            Utils.showMessageAndExit(this, getString(R.string.label_invitation_failed), mExitOnce,
                    e);
        }
    }

    /**
     * Reject invitation
     */
    private void rejectInvitation() {
        try {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "Reject invitation (transferId=" + mTransferId + ")");
            }
            mFileTransfer.rejectInvitation();
        } catch (Exception e) {
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
            acceptInvitation();
        }
    };

    /**
     * Reject button listener
     */
    private OnClickListener declineBtnListener = new OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            rejectInvitation();
            /* Exit activity */
            finish();
        }
    };

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
        /* Stop session */
        try {
            if (mFileTransfer != null && !mTransferred) {
                mFileTransfer.abortTransfer();
            }
        } catch (Exception e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, e.getMessage(), e);
            }
        }
        mFileTransfer = null;

        /* Exit activity */
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
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
            mPauseBtn.setEnabled(false);
            try {
                mFileTransfer.pauseTransfer();
                if (mFileTransfer.isAllowedToResumeTransfer()) {
                    mResumeBtn.setEnabled(true);
                }
            } catch (RcsServiceException e) {
                Utils.showMessageAndExit(ReceiveFileTransfer.this,
                        getString(R.string.label_pause_failed), mExitOnce, e);
            }
        }
    };

    /**
     * Resume button listener
     */
    private android.view.View.OnClickListener btnResumeListener = new android.view.View.OnClickListener() {
        @Override
        public void onClick(View arg0) {
            mResumeBtn.setEnabled(false);
            try {
                mFileTransfer.resumeTransfer();
                if (mFileTransfer.isAllowedToPauseTransfer()) {
                    mPauseBtn.setEnabled(true);
                }
            } catch (RcsServiceException e) {
                Utils.showMessageAndExit(ReceiveFileTransfer.this,
                        getString(R.string.label_resume_failed), mExitOnce, e);
            }
        }
    };

    /**
     * Check whether file size exceeds the limit
     * 
     * @param size Size of file
     * @return {@code true} if file size limit is exceeded, otherwise {@code false}
     */
    private boolean isFileSizeExceeded(long size) {
        try {
            long maxSize = mCnxManager.getFileTransferApi().getConfiguration().getMaxSize() * 1024;
            return (maxSize > 0 && size > maxSize);
        } catch (Exception e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, e.getMessage(), e);
            }
            return false;
        }
    }

    /**
     * Get available space in external storage, only if external storage is ready to write
     * 
     * @return Available space in bytes, otherwise <code>-1</code>
     */
    @SuppressWarnings("deprecation")
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
                Utils.showMessageAndExit(this, getString(R.string.label_transfer_failed_too_big),
                        mExitOnce);
                return false;
            case STORAGE_TOO_SMALL:
                if (LogUtils.isActive) {
                    Log.w(LOGTAG, "Not enough storage capacity, reject the File Transfer");
                }
                Utils.showMessageAndExit(this,
                        getString(R.string.label_transfer_failed_capacity_too_small), mExitOnce);
                return false;
            default:
                return true;
        }
    }

    /**
     * Update UI on file transfer state change
     * 
     * @param transferId
     * @param state new FT state
     * @param reasonCode
     */
    private void onTransferStateChangedUpdateUI(String transferId, final FileTransfer.State state,
            FileTransfer.ReasonCode reasonCode) {
        final String _reasonCode = RiApplication.sFileTransferReasonCodes[reasonCode.toInt()];
        final String _state = RiApplication.sFileTransferStates[state.toInt()];

        if (LogUtils.isActive) {
            Log.d(LOGTAG, "TransferStateChanged transferId=" + transferId + " state=" + state
                    + " reason=" + reasonCode);
        }

        mHandler.post(new Runnable() {

            public void run() {
                TextView statusView = (TextView) findViewById(R.id.progress_status);
                switch (state) {
                    case STARTED:
                        // Session is well established display session status
                        statusView.setText(_state);
                        try {
                            if (mFileTransfer.isAllowedToPauseTransfer()) {
                                mPauseBtn.setEnabled(true);
                            }
                        } catch (RcsGenericException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }                        
                        break;

                    case ABORTED:
                        // Session is aborted: display message then exit
                        Utils.showMessageAndExit(ReceiveFileTransfer.this,
                                getString(R.string.label_transfer_aborted, _reasonCode), mExitOnce);
                        break;

                    case FAILED:
                        // Session is failed: ReceiveFileTransfer
                        Utils.showMessageAndExit(ReceiveFileTransfer.this,
                                getString(R.string.label_transfer_failed, _reasonCode), mExitOnce);
                        break;

                    case REJECTED:
                        // Session is rejected: display message then exit
                        Utils.showMessageAndExit(ReceiveFileTransfer.this,
                                getString(R.string.label_transfer_rejected, _reasonCode), mExitOnce);
                        break;

                    case TRANSFERRED:
                        displayTransferredFile();
                        break;

                    default:
                        statusView.setText(_state);
                }
            }
        });
    }

    private void displayTransferredFile() {
        mTransferred = true;
        TextView statusView = (TextView) findViewById(R.id.progress_status);
        statusView
                .setText(RiApplication.sFileTransferStates[FileTransfer.State.TRANSFERRED.toInt()]);
        // Make sure progress bar is at the end
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setProgress(progressBar.getMax());

        mPauseBtn.setEnabled(false);
        mResumeBtn.setEnabled(false);

        if (VCARD_MIME_TYPE.equals(mFtDao.getMimeType())) {
            // Show the transferred vCard
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(mFtDao.getFile(), VCARD_MIME_TYPE);
            startActivity(intent);
        } else {
            if (mFtDao.getMimeType().startsWith("image/")) {
                // Show the transferred image
                Utils.showPictureAndExit(this, mFtDao.getFile());
            }
        }
    }

    /**
     * Update UI on FT progress
     * 
     * @param currentSize current size
     * @param totalSize total size
     */
    private void onTransferProgressUpdateUI(final long currentSize, final long totalSize) {
        mHandler.post(new Runnable() {
            public void run() {
                updateProgressBar(currentSize, totalSize);
            }
        });
    }

    /**
     * Group File transfer listener
     */
    private GroupFileTransferListener groupFtListener = new GroupFileTransferListener() {

        @Override
        public void onDeliveryInfoChanged(String chatId, ContactId contact, String transferId,
                GroupDeliveryInfo.Status status, GroupDeliveryInfo.ReasonCode reasonCode) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "onDeliveryInfoChanged contact=" + contact + " transferId="
                        + transferId + " state=" + status + " reason=" + reasonCode);
            }
        }

        @Override
        public void onProgressUpdate(String chatId, String transferId, long currentSize,
                long totalSize) {
            // Discard event if not for current transferId
            if (!mFtDao.getTransferId().equals(transferId)) {
                return;
            }
            onTransferProgressUpdateUI(currentSize, totalSize);
        }

        @Override
        public void onStateChanged(String chatId, String transferId, FileTransfer.State state,
                FileTransfer.ReasonCode reasonCode) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "onStateChanged chatId=" + chatId + " transferId=" + transferId);
            }
            // Discard event if not for current transferId
            if (!mFtDao.getTransferId().equals(transferId)) {
                return;
            }
            onTransferStateChangedUpdateUI(transferId, state, reasonCode);
        }

        @Override
        public void onDeleted(String chatId, Set<String> transferIds) {
            if (LogUtils.isActive) {
                Log.w(LOGTAG, "onDeleted chatId=" + chatId + " transferIds=" + transferIds);
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
            if (!mFtDao.getTransferId().equals(transferId)) {
                return;
            }
            onTransferProgressUpdateUI(currentSize, totalSize);
        }

        @Override
        public void onStateChanged(ContactId contact, String transferId,
                final FileTransfer.State state, FileTransfer.ReasonCode reasonCode) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "onStateChanged contact=" + contact + " transferId=" + transferId);
            }
            // Discard event if not for current transferId
            if (!mFtDao.getTransferId().equals(transferId)) {
                return;
            }
            onTransferStateChangedUpdateUI(transferId, state, reasonCode);
        }

        @Override
        public void onDeleted(ContactId contact, Set<String> transferIds) {
            if (LogUtils.isActive) {
                Log.w(LOGTAG, "onDeleted contact=" + contact + " transferIds=" + transferIds);
            }
        }
    };
}
