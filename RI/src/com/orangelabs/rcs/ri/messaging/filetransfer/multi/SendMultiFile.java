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

package com.orangelabs.rcs.ri.messaging.filetransfer.multi;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransferService;

import com.orangelabs.rcs.ri.ConnectionManager;
import com.orangelabs.rcs.ri.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.FileUtils;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
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
public abstract class SendMultiFile extends Activity implements ISendMultiFile {

    private static final String LOGTAG = LogUtils.getTag(SendMultiFile.class.getSimpleName());

    /**
     * Intent parameters
     */
    private final static String EXTRA_CHAT_ID = "chat_id";

    /**
     * The chatId
     */
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

    private ConnectionManager mCnxManager;

    /**
     * A flag to only add listener once and to remove only if previously added
     */
    protected boolean mFileTransferListenerAdded = false;

    /**
     * Set of file transfers
     */
    protected Set<FileTransfer> mFileTransfers;

    private Dialog progressDialog;

    /**
     * List view file transfer adapter
     */
    protected FileTransferAdapter mFileTransferAdapter;

    /**
     * A locker to exit only once
     */
    protected LockAccess mExitOnce = new LockAccess();

    /**
     * Instance of file transfer service
     */
    protected FileTransferService mFileTransferService;

    private Button mStartButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCnxManager = ConnectionManager.getInstance();

        mTransferIds = new ArrayList<String>();
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
        mFileTransferAdapter = new FileTransferAdapter(
                this,
                R.layout.filetransfer_send_multi_file_item,
                (FileTransferProperties[]) mFiles.toArray(new FileTransferProperties[mFiles.size()]));
        listView.setAdapter(mFileTransferAdapter);

        mFileTransfers = new HashSet<FileTransfer>();

        /* Set start button */
        mStartButton = (Button) findViewById(R.id.ft_start_btn);
        mStartButton.setVisibility(View.GONE);
        mStartButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // TODO check warn size
                initiateTransfer();
            }
        });

        /* Register to API connection manager */
        if (!mCnxManager.isServiceConnected(RcsServiceName.CHAT, RcsServiceName.FILE_TRANSFER,
                RcsServiceName.CONTACT)) {
            Utils.showMessageAndExit(this, getString(R.string.label_service_not_available),
                    mExitOnce);
        } else {
            mCnxManager.startMonitorServices(this, mExitOnce, RcsServiceName.CHAT,
                    RcsServiceName.FILE_TRANSFER, RcsServiceName.CONTACT);
            mFileTransferService = mCnxManager.getFileTransferApi();
            try {
                Boolean authorized = checkPermissionToSendFile(mChatId);
                if (authorized) {
                    mStartButton.setVisibility(View.VISIBLE);
                    addFileTransferEventListener(mFileTransferService);
                } else {
                    if (LogUtils.isActive) {
                        Log.d(LOGTAG, "Not allowed to transfer file to ".concat(mChatId));
                    }
                    mStartButton.setVisibility(View.INVISIBLE);
                    Utils.showMessage(this, getString(R.string.label_ft_not_allowed));
                }
            } catch (RcsServiceException e) {
                Utils.showMessageAndExit(this, getString(R.string.label_api_failed), mExitOnce, e);
            }
        }
    }

    private boolean parseIntent(Intent intent) {
        mChatId = intent.getStringExtra(EXTRA_CHAT_ID);
        String action = intent.getAction();
        // Here we get data from the event.
        if (Intent.ACTION_SEND.equals(action)) {
            mFiles = new ArrayList<FileTransferProperties>();
            // Handle normal one file or text sharing.
            Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
            String fileName = FileUtils.getFileName(this, uri);
            long fileSize = FileUtils.getFileSize(this, uri) / 1024;
            mFiles.add(new FileTransferProperties(uri, fileName, fileSize));
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "Transfer single file " + fileName + " (size=" + fileSize + ")");
            }
            return true;
        }
        if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            // Handle multiple file sharing.
            ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (uris != null) {
                mFiles = new ArrayList<FileTransferProperties>();
                StringBuilder files = new StringBuilder("Transfer multiple files [");
                String loopDelim = "";
                for (Uri uri : uris) {
                    String fileName = FileUtils.getFileName(this, uri);
                    long fileSize = FileUtils.getFileSize(this, uri) / 1024;
                    mFiles.add(new FileTransferProperties(uri, fileName, fileSize));
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
        mCnxManager.stopMonitorServices(this);
        if (mCnxManager.isServiceConnected(RcsServiceName.FILE_TRANSFER)) {
            // Remove file listener
            try {
                removeFileTransferEventListener(mFileTransferService);
            } catch (RcsServiceException e) {
            }
        }
    }

    private void initiateTransfer() {
        if (transferFiles(mFiles)) {
            // Display a progress dialog
            progressDialog = Utils.showProgressDialog(SendMultiFile.this,
                    getString(R.string.label_command_in_progress));
            progressDialog.setOnCancelListener(new OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    Toast.makeText(SendMultiFile.this,
                            getString(R.string.label_transfer_cancelled), Toast.LENGTH_SHORT)
                            .show();
                    quitSession();
                }
            });

            /* Hide start button */
            Button inviteBtn = (Button) findViewById(R.id.ft_start_btn);
            inviteBtn.setVisibility(View.GONE);
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

    /**
     * Quit the session
     */
    private void quitSession() {
        /* Stop sessions */
        try {
            for (FileTransfer fileTransfer : mFileTransfers) {
                if (FileTransfer.State.STARTED == fileTransfer.getState()) {
                    fileTransfer.abortTransfer();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mFileTransfers.clear();

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
            ProgressBar mProgressBar;

            ViewHolder(View view) {
                mUri = (TextView) view.findViewById(R.id.uri);
                mSize = (TextView) view.findViewById(R.id.filesize);
                mProgressStatus = (TextView) view.findViewById(R.id.progress_status);
                mFileIcon = (CheckBox) view.findViewById(R.id.ft_thumb);
                mProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
                mReasonCodeTableRow = (TableRow) view.findViewById(R.id.row_reason_code);
                mReasonCodeText = (TextView) view.findViewById(R.id.text_reason_code);
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
            viewHolder.mSize.setText(String.valueOf(item.getSize()).concat(" KB"));
            viewHolder.mProgressStatus.setText(item.getStatus());
            viewHolder.mFileIcon.setChecked(item.isFileicon());
            viewHolder.mFileIcon.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    CheckBox cb = (CheckBox) v;
                    item.setFileicon(cb.isChecked());
                }
            });
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

    /**
     * 
     */
    protected void closeDialogIfMultipleFileTransferIsFinished() {
        try {
            for (FileTransfer fileTransfer : mFileTransfers) {
                if (!isFileTransferFinished(fileTransfer)) {
                    /* There is one ongoing file transfer -> exit */
                    return;
                }
            }
            /* There is no ongoing file transfer -> hide progress dialog */
            hideProgressDialog();
        } catch (Exception e) {
            hideProgressDialog();
            Utils.showMessageAndExit(SendMultiFile.this,
                    getString(R.string.label_invitation_failed), mExitOnce, e);
        }
    }
}
