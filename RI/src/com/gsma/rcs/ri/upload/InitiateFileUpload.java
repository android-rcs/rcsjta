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

package com.gsma.rcs.ri.upload;

import static com.gsma.rcs.ri.utils.FileUtils.takePersistableContentUriPermission;

import com.gsma.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.gsma.rcs.api.connection.utils.ExceptionUtil;
import com.gsma.rcs.api.connection.utils.RcsActivity;
import com.gsma.rcs.ri.R;
import com.gsma.rcs.ri.utils.FileUtils;
import com.gsma.rcs.ri.utils.LogUtils;
import com.gsma.rcs.ri.utils.RcsSessionUtil;
import com.gsma.rcs.ri.utils.Utils;
import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.upload.FileUpload;
import com.gsma.services.rcs.upload.FileUploadInfo;
import com.gsma.services.rcs.upload.FileUploadListener;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Initiate file upload
 * 
 * @author Jean-Marc AUFFRET
 * @author Philippe LEMORDANT
 */
public class InitiateFileUpload extends RcsActivity {
    /**
     * Activity result constants
     */
    private final static int SELECT_IMAGE = 0;

    private final Handler mHandler = new Handler();

    private Uri mFile;

    /**
     * Selected filesize (kB)
     */
    private long mFilesize = -1;

    private FileUpload mUpload;

    private String mUploadId;

    private MyFileUploadListener mUploadListener = new MyFileUploadListener();

    private boolean mUploadThumbnail = false;

    private Button mShowThumbnailBtn;

    private Button mShowBtn;

    private Button mUploadBtn;

    private Button mSelectBtn;

    private OnClickListener mBtnUploadListener;

    private OnClickListener mBtnSelectListener;

    private OnClickListener mBtnShowListener;

    private OnClickListener mBtnShowThumbnailListener;

    private static final String LOGTAG = LogUtils.getTag(InitiateFileUpload.class.getSimpleName());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initialize();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.fileupload_initiate);

        // Set buttons callback
        mUploadBtn = (Button) findViewById(R.id.upload_btn);
        mUploadBtn.setOnClickListener(mBtnUploadListener);
        mUploadBtn.setEnabled(false);

        mShowBtn = (Button) findViewById(R.id.show_btn);
        mShowBtn.setOnClickListener(mBtnShowListener);
        mShowBtn.setEnabled(false);

        mShowThumbnailBtn = (Button) findViewById(R.id.show_icon_btn);
        mShowThumbnailBtn.setOnClickListener(mBtnShowThumbnailListener);
        mShowThumbnailBtn.setEnabled(false);

        mSelectBtn = (Button) findViewById(R.id.select_btn);
        mSelectBtn.setOnClickListener(mBtnSelectListener);

        // Register to API connection manager
        if (!isServiceConnected(RcsServiceName.FILE_TRANSFER)) {
            showMessageThenExit(R.string.label_service_not_available);
            return;
        }
        startMonitorServices(RcsServiceName.FILE_UPLOAD);
        try {
            getFileUploadApi().addEventListener(mUploadListener);
        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!isServiceConnected(RcsServiceName.FILE_TRANSFER)) {
            return;
        }
        try {
            getFileUploadApi().removeEventListener(mUploadListener);
        } catch (RcsServiceException e) {
            Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        mFile = data.getData();
        TextView uriEdit = (TextView) findViewById(R.id.uri);
        switch (requestCode) {
            case SELECT_IMAGE:
                /* Display file info */
                mFilesize = FileUtils.getFileSize(this, mFile);
                uriEdit.setText(FileUtils.humanReadableByteCount(mFilesize, true));
                /* Enable upload button */
                mUploadBtn.setEnabled(true);
                break;
        }
    }

    /**
     * File upload event listener
     */
    private class MyFileUploadListener extends FileUploadListener {
        /**
         * Callback called when the upload state changes
         * 
         * @param uploadId ID of upload
         * @param state State of upload
         */
        @Override
        public void onStateChanged(String uploadId, final FileUpload.State state) {
            // Discard event if not for current uploadId
            if (mUploadId == null || !mUploadId.equals(uploadId)) {
                return;
            }
            mHandler.post(new Runnable() {
                public void run() {
                    TextView statusView = (TextView) findViewById(R.id.progress_status);
                    if (state == FileUpload.State.STARTED) {
                        // Display session status
                        statusView.setText(getString(R.string.label_upload_started));
                    } else if (state == FileUpload.State.FAILED) {
                        // Display sharing status
                        showMessageThenExit(R.string.label_upload_failed);

                    } else if (state == FileUpload.State.ABORTED) {
                        // Display sharing status
                        showMessageThenExit(R.string.label_upload_aborted);

                    } else if (state == FileUpload.State.TRANSFERRED) {
                        // Display sharing status
                        statusView.setText(getString(R.string.label_upload_transferred));
                        try {
                            Uri file = mUpload.getFile();
                            FileUpload.State state = mUpload.getState();
                            String id = mUpload.getUploadId();
                            FileUploadInfo fileInfo = mUpload.getUploadInfo();
                            if (LogUtils.isActive) {
                                Log.i(LOGTAG, "FileUpload transferred (id=" + id + ") uri=" + file
                                        + ") (state=" + state + ") (info=" + fileInfo + ")");
                            }
                        } catch (RcsServiceException e) {
                            showExceptionThenExit(e);
                        }

                    }
                }
            });

        }

        @Override
        public void onProgressUpdate(String uploadId, final long currentSize, final long totalSize) {
            mHandler.post(new Runnable() {
                public void run() {
                    updateProgressBar(currentSize, totalSize);
                }
            });
        }

        @Override
        public void onUploaded(String uploadId, final FileUploadInfo info) {
            mHandler.post(new Runnable() {
                public void run() {
                    mShowBtn.setEnabled(true);
                    mShowThumbnailBtn.setEnabled(mUploadThumbnail);
                }
            });
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
        try {
            if (mUpload != null && RcsSessionUtil.isAllowedToAbortFileUploadSession(mUpload)) {
                mUpload.abortUpload();
            }
        } catch (RcsServiceException e) {
            showException(e);

        } finally {
            mUpload = null;
            finish();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_BACK == keyCode) {
            quitSession();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void initialize() {
        mBtnUploadListener = new OnClickListener() {
            public void onClick(View v) {
                try {
                    // Check max size
                    long maxSize = 0;
                    try {
                        maxSize = getFileUploadApi().getConfiguration().getMaxSize();
                        if (LogUtils.isActive) {
                            Log.d(LOGTAG,
                                    "FileUpload max size=".concat(Long.valueOf(maxSize).toString()));
                        }
                    } catch (RcsServiceException e) {
                        showException(e);
                    }
                    if ((maxSize > 0) && (mFilesize >= maxSize)) {
                        // Display an error
                        showMessage(getString(R.string.label_upload_max_size, maxSize));
                        return;
                    }

                    // Get thumbnail option
                    CheckBox ftThumb = (CheckBox) findViewById(R.id.file_thumb);
                    mUploadThumbnail = ftThumb.isChecked();

                    /* Only take persistable permission for content Uris */
                    takePersistableContentUriPermission(InitiateFileUpload.this, mFile);

                    // Initiate upload
                    mUpload = getFileUploadApi().uploadFile(mFile, mUploadThumbnail);
                    mUploadId = mUpload.getUploadId();

                    // Hide buttons
                    mUploadBtn.setVisibility(View.GONE);
                    mSelectBtn.setVisibility(View.GONE);

                } catch (RcsServiceException e) {
                    showExceptionThenExit(e);
                }
            }
        };

        mBtnSelectListener = new OnClickListener() {
            public void onClick(View v) {
                FileUtils.openFile(InitiateFileUpload.this, "image/*", SELECT_IMAGE);
            }
        };

        mBtnShowListener = new OnClickListener() {
            public void onClick(View v) {
                /* Show uploaded file */
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(mUpload.getUploadInfo().getFile());
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } catch (RcsGenericException e) {
                    showExceptionThenExit(e);
                }
            }
        };

        mBtnShowThumbnailListener = new OnClickListener() {
            public void onClick(View v) {
                /* Show uploaded thumbnail */
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(mUpload.getUploadInfo().getFileIcon());
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);

                } catch (RcsGenericException e) {
                    showExceptionThenExit(e);
                }
            }
        };

    }

}
