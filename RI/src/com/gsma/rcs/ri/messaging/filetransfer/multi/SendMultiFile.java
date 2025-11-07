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

package com.gsma.rcs.ri.messaging.filetransfer.multi;

import com.gsma.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.gsma.rcs.api.connection.utils.ExceptionUtil;
import com.gsma.rcs.api.connection.utils.RcsActivity;
import com.gsma.rcs.ri.R;
import com.gsma.rcs.ri.utils.FileUtils;
import com.gsma.rcs.ri.utils.LogUtils;
import com.gsma.rcs.ri.utils.RcsSessionUtil;
import com.gsma.rcs.ri.utils.Utils;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransferService;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * SendMultiFile
 * 
 * @author Philippe LEMORDANT
 */
public abstract class SendMultiFile extends RcsActivity implements ISendMultiFile {

    private static final String LOGTAG = LogUtils.getTag(SendMultiFile.class.getSimpleName());

    private final static String EXTRA_CHAT_ID = "chat_id";

    protected String mChatId;
    /**
     * UI Handler
     */
    protected final Handler mHandler = new Handler();
    /**
     * The list of file transfer IDs
     */
    protected List<String> mTransferIds;
    /**
     * The list of files to transfer
     */
    protected List<FileTransferProperties> mFiles;
    /**
     * A flag to only add listener once and to remove only if previously added
     */
    protected boolean mFileTransferListenerAdded = false;
    protected Set<FileTransfer> mFileTransfers;
    private ProgressDialog mProgressDialog;
    protected FileTransferAdapter mFileTransferAdapter;
    protected FileTransferService mFileTransferService;
    private boolean mInvited;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize();
        mTransferIds = new ArrayList<>();
        if (!parseIntent(getIntent())) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "onCreate invalid intent");
            }
            finish();
            return;
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.filetransfer_send_multi_file);
        ListView listView = (ListView) findViewById(android.R.id.list);
        mFileTransferAdapter = new FileTransferAdapter(this,
                R.layout.filetransfer_send_multi_file_item,
                mFiles.toArray(new FileTransferProperties[mFiles.size()]));
        listView.setAdapter(mFileTransferAdapter);
        mFileTransfers = new HashSet<>();
        /* Set start button */
        Button startButton = (Button) findViewById(R.id.ft_start_btn);
        startButton.setVisibility(View.GONE);
        startButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // TODO check warn size
                initiateTransfer();
            }
        });
        /* Register to API connection manager */
        if (!isServiceConnected(RcsServiceName.CHAT, RcsServiceName.FILE_TRANSFER,
                RcsServiceName.CONTACT)) {
            showMessageThenExit(R.string.label_service_not_available);

        } else {
            startMonitorServices(RcsServiceName.CHAT, RcsServiceName.FILE_TRANSFER,
                    RcsServiceName.CONTACT);
            mFileTransferService = getFileTransferApi();
            try {
                Boolean authorized = checkPermissionToSendFile(mChatId);
                if (authorized) {
                    startButton.setVisibility(View.VISIBLE);
                    addFileTransferEventListener(mFileTransferService);

                } else {
                    if (LogUtils.isActive) {
                        Log.d(LOGTAG, "Not allowed to transfer file to ".concat(mChatId));
                    }
                    startButton.setVisibility(View.INVISIBLE);
                    showMessage(R.string.label_ft_not_allowed);
                }
            } catch (RcsServiceException e) {
                showExceptionThenExit(e);
            }
        }
    }

    private boolean parseIntent(Intent intent) {
        mChatId = intent.getStringExtra(EXTRA_CHAT_ID);
        String action = intent.getAction();
        // Here we get data from the event.
        if (Intent.ACTION_SEND.equals(action)) {
            mFiles = new ArrayList<>();
            // Handle normal one file or text sharing.
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            String fileName = FileUtils.getFileName(this, uri);
            long fileSize = FileUtils.getFileSize(this, uri);
            String mimeType = FileUtils.getMimeType(this, uri);
            mFiles.add(new FileTransferProperties(uri, fileName, fileSize, mimeType));
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "Transfer single file " + fileName + " (size=" + fileSize + ")");
            }
            return true;
        }
        if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            // Handle multiple file sharing.
            ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (uris != null) {
                mFiles = new ArrayList<>();
                StringBuilder files = new StringBuilder("Transfer multiple files [");
                String loopDelim = "";
                for (Uri uri : uris) {
                    String fileName = FileUtils.getFileName(this, uri);
                    long fileSize = FileUtils.getFileSize(this, uri);
                    String mimeType = FileUtils.getMimeType(this, uri);
                    mFiles.add(new FileTransferProperties(uri, fileName, fileSize, mimeType));
                    files.append(loopDelim);
                    files.append(fileName);
                    files.append("(");
                    files.append(fileSize);
                    files.append(")");
                    loopDelim = ",";
                }
                files.append("]");
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, files.toString());
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isServiceConnected(RcsServiceName.FILE_TRANSFER)) {
            try {
                removeFileTransferEventListener(mFileTransferService);
            } catch (RcsServiceException e) {
                Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
            }
        }
    }

    private void initiateTransfer() {
        if (transferFiles(mFiles)) {
            mProgressDialog = showProgressDialog(getString(R.string.label_command_in_progress));
            mProgressDialog.setOnCancelListener(new OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    hideProgressDialog();
                    if (!isThereAnyOnGoingSession()) {
                        finish();
                    }
                    if (LogUtils.isActive) {
                        Log.d(LOGTAG, "onKeyDown abort on going sessions");
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(SendMultiFile.this);
                    builder.setTitle(R.string.label_confirm_close);
                    builder.setPositiveButton(R.string.label_ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    Toast.makeText(SendMultiFile.this,
                                            getString(R.string.label_transfer_cancelled),
                                            Toast.LENGTH_SHORT).show();
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
                }
            });
            /* Hide start button */
            Button inviteBtn = (Button) findViewById(R.id.ft_start_btn);
            inviteBtn.setVisibility(View.GONE);
            mInvited = true;
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
     * @param position of the item whose data we want within the adapter's data set.
     * @param currentSize Current size transferred
     * @param totalSize Total size to be transferred
     */
    protected void updateProgressBar(Integer position, long currentSize, long totalSize) {
        FileTransferProperties prop = mFileTransferAdapter.getItem(position);
        prop.setStatus(Utils.getProgressLabel(currentSize, totalSize));
        double progress = ((double) currentSize / (double) totalSize) * 100.0;
        prop.setProgress((int) progress);
        mFileTransferAdapter.notifyDataSetChanged();
    }

    private void quitSession() {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "quitSession");
        }
        try {
            if (mFileTransfers != null) {
                for (FileTransfer fileTransfer : mFileTransfers) {
                    if (RcsSessionUtil.isAllowedToAbortFileTransferSession(fileTransfer)) {
                        fileTransfer.abortTransfer();
                    }
                }
                mFileTransfers.clear();
            }
        } catch (RcsServiceException e) {
            showException(e);

        } finally {
            mFileTransfers = null;
            /* Exit activity */
            finish();
        }
    }

    private boolean isThereAnyOnGoingSession() {
        if (mFileTransfers == null) {
            return false;
        }
        try {
            for (FileTransfer fileTransfer : mFileTransfers) {
                if (RcsSessionUtil.isAllowedToAbortFileTransferSession(fileTransfer)) {
                    return true;
                }
            }
        } catch (RcsServiceException e) {
            showException(e);
        }
        return false;
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
     * Start activity
     * 
     * @param context The context
     * @param intent The original intent (with action SEND or SEND_MULTIPLE).
     * @param isSingleChat True if file(s) is(are) sent over a single chat.
     * @param chatId The chat ID.
     */
    public static void startActivity(Context context, Intent intent, boolean isSingleChat,
            String chatId) {
        if (isSingleChat) {
            intent.setClass(context, SendMultiFileSingleChat.class);
        } else {
            intent.setClass(context, SendMultiFileGroupChat.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_CHAT_ID, chatId);
        context.startActivity(intent);
    }

    /**
     * FileTransfer adapter
     */
    protected class FileTransferAdapter extends ArrayAdapter<FileTransferProperties> {

        private Context mContext;
        private int mLayoutResourceId;
        private FileTransferProperties[] mFileTransferViewItems;

        /**
         * Constructor
         * 
         * @param context The context.
         * @param layoutResourceId the layout resource ID.
         * @param filetransferViewItems the list of file transfer view items.
         */
        public FileTransferAdapter(Context context, int layoutResourceId,
                FileTransferProperties[] filetransferViewItems) {
            super(context, layoutResourceId, filetransferViewItems);
            mContext = context;
            mLayoutResourceId = layoutResourceId;
            mFileTransferViewItems = filetransferViewItems;
        }

        private class ViewHolder {
            TextView mUri;
            TextView mSize;
            TextView mProgressStatus;
            TableRow mReasonCodeTableRow;
            TextView mReasonCodeText;
            CheckBox mFileIcon;
            CheckBox mAudiomsg;
            ProgressBar mProgressBar;

            ViewHolder(View view) {
                mUri = (TextView) view.findViewById(R.id.uri);
                mSize = (TextView) view.findViewById(R.id.filesize);
                mProgressStatus = (TextView) view.findViewById(R.id.progress_status);
                mFileIcon = (CheckBox) view.findViewById(R.id.ft_thumb);
                mProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
                mReasonCodeTableRow = (TableRow) view.findViewById(R.id.row_reason_code);
                mReasonCodeText = (TextView) view.findViewById(R.id.text_reason_code);
                mAudiomsg = (CheckBox) view.findViewById(R.id.send_audio_msg);
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = inflater.inflate(mLayoutResourceId, parent, false);
                viewHolder = new ViewHolder(convertView);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            final FileTransferProperties item = mFileTransferViewItems[position];
            viewHolder.mUri.setText(item.getFilename());
            viewHolder.mSize.setText(FileUtils.humanReadableByteCount(item.getSize(), true));
            viewHolder.mProgressStatus.setText(item.getStatus());
            String mimeType = item.getMimeType();
            viewHolder.mAudiomsg.setEnabled(false);
            viewHolder.mAudiomsg.setChecked(item.isAudioMessage());
            viewHolder.mFileIcon.setEnabled(false);
            viewHolder.mFileIcon.setChecked(item.isFileicon());
            if (!mInvited) {
                if (Utils.isImageType(mimeType)) {
                    viewHolder.mFileIcon.setEnabled(true);
                    viewHolder.mFileIcon.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            CheckBox cb = (CheckBox) v;
                            item.setFileicon(cb.isChecked());
                        }
                    });
                } else if (Utils.isAudioType(mimeType)) {
                    viewHolder.mAudiomsg.setEnabled(true);
                    viewHolder.mAudiomsg.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            CheckBox cb = (CheckBox) v;
                            item.setAudioMessage(cb.isChecked());
                        }
                    });
                }
            }
            viewHolder.mProgressBar.setProgress(item.getProgress());
            if (item.getReasonCode() == null) {
                viewHolder.mReasonCodeTableRow.setVisibility(View.GONE);
            } else {
                viewHolder.mReasonCodeTableRow.setVisibility(View.VISIBLE);
                viewHolder.mReasonCodeText.setText(item.getReasonCode());
            }
            return convertView;
        }
    }

    private boolean isFileTransferFinished(FileTransfer fileTransfer) throws RcsServiceException {
        switch (fileTransfer.getState()) {
            case STARTED:
            case QUEUED:
            case PAUSED:
            case INITIATING:
                return false;
            default:
                return true;
        }
    }

    protected void closeDialogIfMultipleFileTransferIsFinished() {
        if (mFileTransfers == null) {
            return;
        }
        try {
            for (FileTransfer fileTransfer : mFileTransfers) {
                if (!isFileTransferFinished(fileTransfer)) {
                    /* There is one ongoing file transfer -> exit */
                    return;
                }
            }
            /* There is no ongoing file transfer -> hide progress dialog */
            hideProgressDialog();
        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

}
