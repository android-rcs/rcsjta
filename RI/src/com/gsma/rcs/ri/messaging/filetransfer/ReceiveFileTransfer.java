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

import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransferIntent;
import com.gsma.services.rcs.filetransfer.FileTransferLog;
import com.gsma.services.rcs.filetransfer.FileTransferService;
import com.gsma.services.rcs.filetransfer.GroupFileTransferListener;
import com.gsma.services.rcs.filetransfer.OneToOneFileTransferListener;
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo;

import com.gsma.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.gsma.rcs.api.connection.utils.ExceptionUtil;
import com.gsma.rcs.api.connection.utils.RcsActivity;
import com.gsma.rcs.ri.R;
import com.gsma.rcs.ri.RiApplication;
import com.gsma.rcs.ri.messaging.chat.group.GroupChatDAO;
import com.gsma.rcs.ri.utils.FileUtils;
import com.gsma.rcs.ri.utils.LogUtils;
import com.gsma.rcs.ri.utils.RcsContactUtil;
import com.gsma.rcs.ri.utils.RcsSessionUtil;
import com.gsma.rcs.ri.utils.Utils;
import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransferIntent;
import com.gsma.services.rcs.filetransfer.FileTransferLog;
import com.gsma.services.rcs.filetransfer.FileTransferService;
import com.gsma.services.rcs.filetransfer.GroupFileTransferListener;
import com.gsma.services.rcs.filetransfer.OneToOneFileTransferListener;
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo;

import android.app.AlertDialog;
import android.content.Context;
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

import java.io.IOException;
import java.util.Set;

/**
 * Received file transfer
 * 
 * @author Jean-Marc AUFFRET
 * @author Philippe LEMORDANT
 */
public class ReceiveFileTransfer extends RcsActivity {

    private static final String LOGTAG = LogUtils.getTag(ReceiveFileTransfer.class.getSimpleName());
    private static final String VCARD_MIME_TYPE = "text/x-vcard";
    private static final String BUNDLE_FTDAO_ID = "ftdao";

    /**
     * UI mHandler
     */
    private final Handler mHandler = new Handler();
    private FileTransfer mFileTransfer;
    private FileTransferDAO mFtDao;
    private boolean mGroupFileTransfer = false;
    private String mTransferId;
    private Button mPauseBtn;
    private Button mResumeBtn;
    private OnClickListener mDeclineBtnListener;
    private OnClickListener mAcceptBtnListener;
    private OneToOneFileTransferListener mFileTransferListener;
    private GroupFileTransferListener mGroupFtListener;
    private FileTransferService mFileTransferService;
    private ProgressBar mProgressBar;
    private TextView mStatusView;
    private TextView mSizeTextView;
    private TextView mFilenameTextView;
    private TextView mExpirationTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.filetransfer_receive);
        initialize();

        /* Register to API connection manager */
        if (!isServiceConnected(RcsServiceName.FILE_TRANSFER, RcsServiceName.CONTACT)) {
            showMessageThenExit(R.string.label_service_not_available);
        } else {
            startMonitorServices(RcsServiceName.FILE_TRANSFER, RcsServiceName.CONTACT);
            mFileTransferService = getFileTransferApi();
            if (!processIntent(getIntent(), false)) {
                return;
            }
            try {
                if (mGroupFileTransfer) {
                    mFileTransferService.addEventListener(mGroupFtListener);
                } else {
                    mFileTransferService.addEventListener(mFileTransferListener);
                }
            } catch (RcsServiceException e) {
                showExceptionThenExit(e);
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
        if (mFileTransferService != null && isServiceConnected(RcsServiceName.FILE_TRANSFER)) {
            /* Remove service listener */
            try {
                if (mGroupFileTransfer) {
                    mFileTransferService.removeEventListener(mGroupFtListener);
                } else {
                    mFileTransferService.removeEventListener(mFileTransferListener);
                }
            } catch (RcsServiceException e) {
                Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
            }
        }
    }

    boolean processIntent(Intent intent, boolean newIntent) {
        mFtDao = intent.getExtras().getParcelable(BUNDLE_FTDAO_ID);
        if (mFtDao == null) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "processIntent cannot read File Transfer invitation (newIntent="
                        + newIntent + ")");
            }
            finish();
            return false;
        }
        /* Get the file transfer session */
        mTransferId = mFtDao.getTransferId();
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "processIntent (newIntent=" + newIntent + ") " + mFtDao);
        }
        ContactId contact = mFtDao.getContact();
        /* Get invitation info */
        boolean resuming = FileTransferIntent.ACTION_RESUME.equals(intent.getAction());
        mGroupFileTransfer = GroupChatDAO.isGroupChat(mFtDao.getChatId(), contact);

        String from = RcsContactUtil.getInstance(this).getDisplayName(contact);
        TextView fromTextView = (TextView) findViewById(R.id.from);
        fromTextView.setText(getString(R.string.label_from_args, from));

        String size = getString(R.string.label_file_size,
                FileUtils.humanReadableByteCount(mFtDao.getSize(), true));
        mSizeTextView.setText(size);
        mFilenameTextView.setText(getString(R.string.label_filename, mFtDao.getFilename()));

        long fileExpiration = mFtDao.getFileExpiration();
        if (fileExpiration != FileTransferLog.UNKNOWN_EXPIRATION) {
            CharSequence expiration = DateUtils.getRelativeTimeSpanString(
                    mFtDao.getFileExpiration(), System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE);
            mExpirationTextView.setText(getString(R.string.label_expiration_args, expiration));
        } else {
            mExpirationTextView.setVisibility(View.GONE);
        }
        try {
            mFileTransfer = mFileTransferService.getFileTransfer(mTransferId);
            if (mFileTransfer == null) {
                if (FileTransfer.State.TRANSFERRED == mFtDao.getState()) {
                    displayTransferredFile();
                    return false;

                } else {
                    String reasonCode = RiApplication.sFileTransferReasonCodes[mFtDao
                            .getReasonCode().toInt()];
                    if (LogUtils.isActive) {
                        Log.e(LOGTAG,
                                "Transfer ID=" + mTransferId + " failed state=" + mFtDao.getState()
                                        + " reason=" + reasonCode);
                    }
                    showMessageThenExit(getString(R.string.label_transfer_failed, reasonCode));
                    return false;

                }

            }
            /* Do not consider acceptance if resuming */
            if (resuming) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "processIntent resuming ".concat(mTransferId));
                }
                return true;
            }
            FileTransfer.State state = mFileTransfer.getState();
            /* Check if not already accepted by the stack */
            if (FileTransfer.State.INVITED != state) {
                /* File Transfer is auto accepted by the stack. Check capacity */
                isCapacityOk(mFtDao.getSize());

                if (FileTransfer.State.TRANSFERRED == state) {
                    if (LogUtils.isActive) {
                        Log.d(LOGTAG, "processIntent file is transferred ".concat(mTransferId));
                    }
                    displayTransferredFile();
                    return false;
                }

            } else {
                /* File Transfer must be accepted/rejected by user */
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

                /* Manual accept */
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                View titleView = getLayoutInflater().inflate(R.layout.filetransfer_custom_title,
                        null);
                builder.setCustomTitle(titleView);
                builder.setMessage(getString(R.string.label_ft_from_size, from,
                        FileUtils.humanReadableByteCount(mFtDao.getSize(), true)));
                builder.setCancelable(false);
                /* Make sure progress bar is at the beginning */
                mProgressBar.setProgress(0);
                ImageView iconView = (ImageView) titleView
                        .findViewById(R.id.filetransfer_alert_title_icon);
                if (mFtDao.getThumbnail() != null) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),
                                mFtDao.getThumbnail());
                        iconView.setImageBitmap(bitmap);

                    } catch (IOException e) {
                        showException(e);
                    }
                } else {
                    if (VCARD_MIME_TYPE.equals(mFtDao.getMimeType())) {
                        iconView.setImageResource(R.drawable.ri_contact_card_icon);
                    } else {
                        iconView.setImageResource(R.drawable.ri_notif_file_transfer_icon);
                    }
                }
                builder.setPositiveButton(R.string.label_accept, mAcceptBtnListener);
                builder.setNegativeButton(R.string.label_decline, mDeclineBtnListener);
                registerDialog(builder.show());
            }
            return true;

        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
        return false;
    }

    private void acceptInvitation() {
        try {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "Accept invitation (transferId=" + mTransferId + ")");
            }
            mFileTransfer.acceptInvitation();
        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    private void rejectInvitation() {
        try {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "Reject invitation (transferId=" + mTransferId + ")");
            }
            mFileTransfer.rejectInvitation();
        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    private void updateProgressBar(long currentSize, long totalSize) {
        mStatusView.setText(Utils.getProgressLabel(currentSize, totalSize));
        double position = ((double) currentSize / (double) totalSize) * 100.0;
        mProgressBar.setProgress((int) position);
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
        if (item.getItemId() == R.id.menu_close_session) {
            quitSession();
        }
        return true;
    }

    /**
     * Check whether file size exceeds the limit
     * 
     * @param size Size of file
     * @return {@code true} if file size limit is exceeded, otherwise {@code false}
     */
    private boolean isFileSizeExceeded(long size) {
        try {
            long maxSize = mFileTransferService.getConfiguration().getMaxSize();
            return (maxSize > 0 && size > maxSize);

        } catch (RcsServiceException e) {
            showException(e);
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

    private enum FileCapacity {
        OK, FILE_TOO_BIG, STORAGE_TOO_SMALL
    }

    /**
     * Check if file capacity is acceptable
     * 
     * @param fileSize file size in bytes
     * @return FileSharingError or null if file capacity is acceptable
     */
    private FileCapacity isFileCapacityAcceptable(long fileSize) {
        if (isFileSizeExceeded(fileSize)) {
            return FileCapacity.FILE_TOO_BIG;
        }
        long freeSpage = getExternalStorageFreeSpace();
        boolean storageIsTooSmall = (freeSpage > 0) && fileSize > freeSpage;
        if (storageIsTooSmall) {
            return FileCapacity.STORAGE_TOO_SMALL;
        }
        return FileCapacity.OK;
    }

    /**
     * Check if file size is less than maximum or then free space on disk
     * 
     * @param fileSize file size in bytes
     * @return boolean
     */
    private boolean isCapacityOk(long fileSize) {
        FileCapacity capacity = isFileCapacityAcceptable(fileSize);
        switch (capacity) {
            case FILE_TOO_BIG:
                showMessageThenExit(R.string.label_transfer_failed_too_big);
                return false;

            case STORAGE_TOO_SMALL:
                showMessageThenExit(R.string.label_transfer_failed_capacity_too_small);
                return false;

            default:
                return true;
        }
    }

    /**
     * Update UI on file transfer state change
     * 
     * @param transferId transfer ID
     * @param state new FT state
     * @param reasonCode reason code
     */
    private void onTransferStateChangedUpdateUI(String transferId, final FileTransfer.State state,
            final FileTransfer.ReasonCode reasonCode) {
        final String _reasonCode = RiApplication.sFileTransferReasonCodes[reasonCode.toInt()];
        final String _state = RiApplication.sFileTransferStates[state.toInt()];
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "TransferStateChanged transferId=" + transferId + " state=" + state
                    + " reason=" + reasonCode);
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
                    case ABORTED:
                        showMessageThenExit(getString(R.string.label_transfer_aborted, _reasonCode));
                        break;

                    case FAILED:
                        showMessageThenExit(getString(R.string.label_transfer_failed, _reasonCode));
                        break;

                    case REJECTED:
                        showMessageThenExit(getString(R.string.label_transfer_rejected, _reasonCode));
                        break;

                    case TRANSFERRED:
                        displayTransferredFile();
                        break;

                    default:
                        mStatusView.setText(_state);
                }
            }
        });
    }

    private void displayTransferredFile() {
        mStatusView.setText(RiApplication.sFileTransferStates[FileTransfer.State.TRANSFERRED
                .toInt()]);
        /* Make sure progress bar is at the end */
        mProgressBar.setProgress(mProgressBar.getMax());

        try {
            mFileTransferService.markFileTransferAsRead(mTransferId);
        } catch (RcsServiceNotAvailableException ignore) {
        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
        String mimeType = mFtDao.getMimeType();
        if (VCARD_MIME_TYPE.equals(mimeType)) {
            // Show the transferred vCard
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(mFtDao.getFile(), VCARD_MIME_TYPE);
            startActivity(intent);

        } else if (Utils.isImageType(mimeType)) {
            /* Show the transferred image */
            Utils.showPicture(this, mFtDao.getFile());

        } else if (Utils.isAudioType(mimeType)
                && FileTransfer.Disposition.RENDER == mFtDao.getDisposition()) {
            Utils.playAudio(this, mFtDao.getFile());
        }
    }

    private void onTransferProgressUpdateUI(final long currentSize, final long totalSize) {
        mHandler.post(new Runnable() {
            public void run() {
                updateProgressBar(currentSize, totalSize);
            }
        });
    }

    private void initialize() {
        mGroupFtListener = new GroupFileTransferListener() {

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
                /* Discard event if not for current transferId */
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
                /* Discard event if not for current transferId */
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
        mAcceptBtnListener = new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                acceptInvitation();
            }
        };

        mDeclineBtnListener = new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                rejectInvitation();
                /* Exit activity */
                finish();
            }
        };

        View.OnClickListener btnPauseListener = new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    if (mFileTransfer.isAllowedToPauseTransfer()) {
                        mFileTransfer.pauseTransfer();
                    } else {
                        mPauseBtn.setEnabled(false);
                        showMessage(R.string.label_pause_ft_not_allowed);
                    }
                } catch (RcsPermissionDeniedException e) {
                    Utils.displayToast(ReceiveFileTransfer.this,
                            getString(R.string.label_pause_failed));

                } catch (RcsServiceException e) {
                    showExceptionThenExit(e);
                }
            }
        };

        View.OnClickListener btnResumeListener = new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    if (mFileTransfer.isAllowedToResumeTransfer()) {
                        mFileTransfer.resumeTransfer();
                    } else {
                        mResumeBtn.setEnabled(false);
                        showMessage(R.string.label_resume_ft_not_allowed);
                    }
                } catch (RcsPermissionDeniedException e) {
                    Utils.displayToast(ReceiveFileTransfer.this,
                            getString(R.string.label_resume_failed));

                } catch (RcsServiceException e) {
                    showExceptionThenExit(e);
                }
            }
        };
        mFileTransferListener = new OneToOneFileTransferListener() {

            @Override
            public void onProgressUpdate(ContactId contact, String transferId,
                    final long currentSize, final long totalSize) {
                /* Discard event if not for current transferId */
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
                /* Discard event if not for current transferId */
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
        /* Set pause and resume button */
        mPauseBtn = (Button) findViewById(R.id.pause_btn);
        mPauseBtn.setOnClickListener(btnPauseListener);
        mPauseBtn.setEnabled(false);
        mResumeBtn = (Button) findViewById(R.id.resume_btn);
        mResumeBtn.setOnClickListener(btnResumeListener);
        mResumeBtn.setEnabled(false);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);

        mStatusView = (TextView) findViewById(R.id.progress_status);
        mStatusView.setText("");
        mSizeTextView = (TextView) findViewById(R.id.image_size);
        mSizeTextView.setText("");
        mFilenameTextView = (TextView) findViewById(R.id.image_filename);
        mFilenameTextView.setText("");
        mExpirationTextView = (TextView) findViewById(R.id.expiration);
        mExpirationTextView.setText("");
    }

    /**
     * Forge invitation intent to start ReceiveFileTransfer activity
     * 
     * @param ctx The context
     * @param ftDao The FileTransfer DAO
     * @param invitation intent
     * @return intent
     */
    public static Intent forgeInvitationIntent(Context ctx, FileTransferDAO ftDao, Intent invitation) {
        invitation.setClass(ctx, ReceiveFileTransfer.class);
        invitation.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        /* Save FileTransferDAO into intent */
        Bundle bundle = new Bundle();
        bundle.putParcelable(BUNDLE_FTDAO_ID, ftDao);
        invitation.putExtras(bundle);
        return invitation;
    }

    /**
     * Forge resume intent to start ReceiveFileTransfer activity
     * 
     * @param ctx The context
     * @param ftDao The FileTransfer DAO
     * @param resume intent
     * @return intent
     */
    public static Intent forgeResumeIntent(Context ctx, FileTransferDAO ftDao, Intent resume) {
        resume.setClass(ctx, ReceiveFileTransfer.class);
        resume.addFlags(Intent.FLAG_FROM_BACKGROUND | Intent.FLAG_ACTIVITY_NEW_TASK);
        /* Save FileTransferDAO into intent */
        Bundle bundle = new Bundle();
        bundle.putParcelable(BUNDLE_FTDAO_ID, ftDao);
        resume.putExtras(bundle);
        return resume;
    }

}
