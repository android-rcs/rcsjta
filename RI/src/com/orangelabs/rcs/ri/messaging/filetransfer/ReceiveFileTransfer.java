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

import com.gsma.services.rcs.GroupDeliveryInfo;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransferLog;
import com.gsma.services.rcs.filetransfer.FileTransferService;
import com.gsma.services.rcs.filetransfer.FileTransferServiceConfiguration;
import com.gsma.services.rcs.filetransfer.GroupFileTransferListener;
import com.gsma.services.rcs.filetransfer.OneToOneFileTransferListener;

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
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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

    /**
     * File transfer
     */
    private FileTransfer mFileTransfer;

    /**
     * File transfer is mResuming
     */
    private boolean mResuming = false;

    /**
     * The File Transfer Data Object
     */
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

    /**
     * The log tag for this class
     */
    private static final String LOGTAG = LogUtils.getTag(ReceiveFileTransfer.class.getSimpleName());

    private static final String VCARD_MIME_TYPE = "text/x-vcard";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.filetransfer_receive);

        // Set pause and resume button
        Button pauseBtn = (Button) findViewById(R.id.pause_btn);
        pauseBtn.setOnClickListener(btnPauseListener);
        pauseBtn.setEnabled(true);
        Button resumeBtn = (Button) findViewById(R.id.resume_btn);
        resumeBtn.setOnClickListener(btnResumeListener);
        resumeBtn.setEnabled(false);

        // Get invitation info
        mFtDao = (FileTransferDAO) (getIntent().getExtras()
                .getParcelable(FileTransferIntentService.BUNDLE_FTDAO_ID));
        if (mFtDao == null) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "onCreate cannot read File Transfer invitation");
            }
            finish();
            return;
        }

        if (getIntent().getAction() != null) {
            mResuming = getIntent().getAction().equals(FileTransferResumeReceiver.ACTION_FT_RESUME);
        }

        mGgroupFileTransfer = (getIntent().getBooleanExtra(
                FileTransferIntentService.EXTRA_GROUP_FILE, false));

        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onCreate " + mFtDao);
        }

        // Register to API connection manager
        mCnxManager = ConnectionManager.getInstance(this);
        if (mCnxManager == null
                || !mCnxManager.isServiceConnected(RcsServiceName.FILE_TRANSFER,
                        RcsServiceName.CONTACT)) {
            Utils.showMessageAndExit(this, getString(R.string.label_service_not_available),
                    mExitOnce);
        } else {
            mCnxManager.startMonitorServices(this, mExitOnce, RcsServiceName.FILE_TRANSFER,
                    RcsServiceName.CONTACT);
            initiateFileTransfer();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCnxManager == null) {
            return;
        }
        mCnxManager.stopMonitorServices(this);
        if (mCnxManager.isServiceConnected(RcsServiceName.FILE_TRANSFER)) {
            // Remove service listener
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

    private void initiateFileTransfer() {
        try {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "initiateFileTransfer ".concat(mFtDao.toString()));
            }

            String from = RcsDisplayName.getInstance(this).getDisplayName(mFtDao.getContact());
            TextView fromTextView = (TextView) findViewById(R.id.from);
            fromTextView.setText(getString(R.string.label_from_args, from));

            String size = getString(R.string.label_file_size, mFtDao.getSize() / 1024);
            TextView sizeTxt = (TextView) findViewById(R.id.image_size);
            sizeTxt.setText(size);

            TextView expirationView = (TextView) findViewById(R.id.expiration);
            long fileExpiration = mFtDao.getFileExpiration();
            if (fileExpiration != FileTransferLog.NOT_APPLICABLE_EXPIRATION) {
                CharSequence expiration = DateUtils.getRelativeTimeSpanString(
                        mFtDao.getFileExpiration(), System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE);
                expirationView.setText(getString(R.string.label_expiration_args, expiration));
            } else {
                expirationView.setVisibility(View.GONE);
            }

            FileTransferService ftApi = mCnxManager.getFileTransferApi();
            // Get the file transfer session
            mFileTransfer = ftApi.getFileTransfer(mFtDao.getTransferId());
            if (mFileTransfer == null) {
                try {
                    // Fetch state from the provider
                    mFtDao = new FileTransferDAO(this, mFtDao.getTransferId());
                    if (FileTransfer.State.TRANSFERRED == mFtDao.getState()) {
                        displayTransferredFile();
                        return;

                    } else {
                        String reasonCode = RiApplication.FT_REASON_CODES[mFtDao.getReasonCode()
                                .toInt()];
                        if (LogUtils.isActive) {
                            Log.e(LOGTAG,
                                    new StringBuilder("Transfer failed state: ")
                                            .append(mFtDao.getState()).append(" reason: ")
                                            .append(reasonCode).toString());
                        }
                        // Transfer failed
                        Utils.showMessageAndExit(this,
                                getString(R.string.label_transfer_failed, reasonCode), mExitOnce);
                        return;

                    }
                } catch (Exception e) {
                    if (LogUtils.isActive) {
                        Log.e(LOGTAG, "Failed to retrieve transferred file", e);
                    }

                    // Session not found or expired
                    Utils.showMessageAndExit(this, getString(R.string.label_session_not_found),
                            mExitOnce);
                    return;

                }
            }
            // Add service event listener
            if (mGgroupFileTransfer) {
                ftApi.addEventListener(groupFtListener);
            } else {
                ftApi.addEventListener(ftListener);
            }

            // Do not consider acceptance if mResuming
            if (mResuming) {
                return;
            }
            // TODO To be changed with CR018 which will introduce a new state :
            // ACCEPTING.
            // The test is kept in the meantime because it is the only way
            // to know if FT is auto accepted by the stack (at least in normal
            // conditions)

            // Check if not already accepted by the stack
            if (isFileTransferInvitationAutoAccepted(ftApi.getConfiguration())) {
                // File Transfer is auto accepted by the stack. Check capacity
                isCapacityOk(mFtDao.getSize());

                // Reevaluate the File Transfer state from provider
                try {
                    mFtDao = new FileTransferDAO(this, mFtDao.getTransferId());
                    if (FileTransfer.State.TRANSFERRED == mFtDao.getState()) {
                        displayTransferredFile();
                    }
                } catch (Exception e) {
                    if (LogUtils.isActive) {
                        Log.e(LOGTAG, "Failed to read file from DB", e);
                    }
                }
            } else {
                // File Transfer must be accepted/rejected by user
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "Wait for user acceptance");
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
                builder.setTitle(R.string.title_file_transfer);

                builder.setMessage(getString(R.string.label_ft_from_size, from,
                        mFtDao.getSize() / 1024));
                builder.setCancelable(false);
                if (mFtDao.getThumbnail() != null) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),
                                mFtDao.getThumbnail());
                        builder.setIcon(new BitmapDrawable(getResources(), bitmap));
                    } catch (Exception e) {
                        if (LogUtils.isActive) {
                            Log.e(LOGTAG, "Failed to load thumbnail", e);
                        }
                    }
                } else {
                    if (VCARD_MIME_TYPE.equals(mFtDao.getMimeType())) {
                        builder.setIcon(R.drawable.ri_contact_card_icon);
                    } else {
                        builder.setIcon(R.drawable.ri_notif_file_transfer_icon);
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
                Log.d(LOGTAG, "Accept invitation");
            }
            // Accept the invitation
            mFileTransfer.acceptInvitation();
        } catch (Exception e) {
            Utils.showMessageAndExit(this, getString(R.string.label_invitation_failed), mExitOnce,
                    e);
        }
    }

    /**
     * Check if file transfer invitation is auto-accepted
     * 
     * @param config the file transfer service configuration
     * @return True if already auto accepted by the stack
     * @throws RcsServiceException
     */
    private boolean isFileTransferInvitationAutoAccepted(FileTransferServiceConfiguration config)
            throws RcsServiceException {
        TelephonyManager telephony = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (telephony.isNetworkRoaming()) {
            return config.isAutoAcceptInRoamingEnabled();
        } else {
            return config.isAutoAcceptEnabled();
        }
    }

    /**
     * Reject invitation
     */
    private void rejectInvitation() {
        try {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "Reject invitation");
            }
            // Reject the invitation
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
     * Show the transfer progress
     * 
     * @param currentSize Current size transferred
     * @param totalSize Total size to be transferred
     */
    private void updateProgressBar(long currentSize, long totalSize) {
        TextView statusView = (TextView) findViewById(R.id.progress_status);
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);

        String value = "" + (currentSize / 1024);
        if (totalSize != 0) {
            value += "/" + (totalSize / 1024);
        }
        value += " Kb";
        statusView.setText(value);

        if (currentSize != 0) {
            double position = ((double) currentSize / (double) totalSize) * 100.0;
            progressBar.setProgress((int) position);
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
            if (mFileTransfer != null && !mTransferred) {
                mFileTransfer.abortTransfer();
            }
        } catch (Exception e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, e.getMessage(), e);
            }
        }
        mFileTransfer = null;

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
    private android.view.View.OnClickListener btnPauseListener = new android.view.View.OnClickListener() {
        @Override
        public void onClick(View arg0) {
            Button resumeBtn = (Button) findViewById(R.id.resume_btn);
            resumeBtn.setEnabled(true);
            Button pauseBtn = (Button) findViewById(R.id.pause_btn);
            pauseBtn.setEnabled(false);

            try {
                mFileTransfer.pauseTransfer();
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
            Button resumeBtn = (Button) findViewById(R.id.resume_btn);
            resumeBtn.setEnabled(false);
            Button pauseBtn = (Button) findViewById(R.id.pause_btn);
            pauseBtn.setEnabled(true);

            try {
                mFileTransfer.resumeTransfer();
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
     * @param state new FT state
     */
    private void onTransferStateChangedUpdateUI(final FileTransfer.State state,
            FileTransfer.ReasonCode reasonCode) {
        final String _reasonCode = RiApplication.FT_REASON_CODES[reasonCode.toInt()];
        final String _state = RiApplication.FT_STATES[state.toInt()];

        if (LogUtils.isActive) {
            Log.d(LOGTAG,
                    new StringBuilder("TransferStateChanged state=").append(_state)
                            .append(" reason=").append(_reasonCode).toString());
        }

        mHandler.post(new Runnable() {

            public void run() {
                TextView statusView = (TextView) findViewById(R.id.progress_status);
                switch (state) {
                    case STARTED:
                        // Session is well established display session status
                        statusView.setText(_state);
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
        try {
            long fileExpiration = mFileTransfer.getFileExpiration();
            long iconExpiration = mFileTransfer.getFileIconExpiration();
            Uri iconUri = mFileTransfer.getFileIcon();
            String iconMimeType = mFileTransfer.getFileIconMimeType();
            if (LogUtils.isActive) {
                Log.d(LOGTAG,
                        new StringBuilder("FileTransfer iconUri=")
                                .append((iconUri == null) ? null : iconUri.toString())
                                .append(" iconMimeType=").append(iconMimeType)
                                .append(" iconExpiration=").append(iconExpiration)
                                .append(" fileExpiration=").append(fileExpiration).toString());
            }
        } catch (RcsServiceException e) {
            e.printStackTrace();
        }

        mTransferred = true;
        TextView statusView = (TextView) findViewById(R.id.progress_status);
        statusView.setText(RiApplication.FT_STATES[FileTransfer.State.TRANSFERRED.toInt()]);
        // Make sure progress bar is at the end
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setProgress(progressBar.getMax());

        // Disable pause button
        Button pauseBtn = (Button) findViewById(R.id.pause_btn);
        pauseBtn.setEnabled(false);
        // Disable resume button
        Button resumeBtn = (Button) findViewById(R.id.resume_btn);
        resumeBtn.setEnabled(false);

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
                // Display transfer progress
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
                Log.d(LOGTAG, new StringBuilder("onDeliveryInfoChanged contact=").append(contact)
                        .append(" transferId=").append(transferId).append(" state=").append(status)
                        .append(" reason=").append(reasonCode).toString());
            }
        }

        @Override
        public void onProgressUpdate(String chatId, String transferId, long currentSize,
                long totalSize) {
            // Discard event if not for current transferId
            if (!mFtDao.getTransferId().equals(transferId)) {
                return;
            }
            ReceiveFileTransfer.this.onTransferProgressUpdateUI(currentSize, totalSize);
        }

        @Override
        public void onStateChanged(String chatId, String transferId, FileTransfer.State state,
                FileTransfer.ReasonCode reasonCode) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG,
                        new StringBuilder("onStateChanged chatId=").append(chatId)
                                .append(" transferId=").append(transferId).toString());
            }
            // Discard event if not for current transferId
            if (!mFtDao.getTransferId().equals(transferId)) {
                return;
            }
            ReceiveFileTransfer.this.onTransferStateChangedUpdateUI(state, reasonCode);
        }

        @Override
        public void onDeleted(String chatId, Set<String> transferIds) {
            if (LogUtils.isActive) {
                Log.w(LOGTAG,
                        new StringBuilder("onDeleted chatId=").append(chatId)
                                .append(" transferIds=").append(transferIds).toString());
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
            ReceiveFileTransfer.this.onTransferProgressUpdateUI(currentSize, totalSize);
        }

        @Override
        public void onStateChanged(ContactId contact, String transferId,
                final FileTransfer.State state, FileTransfer.ReasonCode reasonCode) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG,
                        new StringBuilder("onStateChanged contact=").append(contact)
                                .append(" transferId=").append(transferId).toString());
            }
            // Discard event if not for current transferId
            if (!mFtDao.getTransferId().equals(transferId)) {
                return;
            }
            ReceiveFileTransfer.this.onTransferStateChangedUpdateUI(state, reasonCode);
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
}
