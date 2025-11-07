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

package com.gsma.rcs.ri.sharing.image;

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
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.image.ImageSharing;
import com.gsma.services.rcs.sharing.image.ImageSharingListener;
import com.gsma.services.rcs.sharing.image.ImageSharingService;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Set;

/**
 * Initiate image sharing
 *
 * @author Jean-Marc AUFFRET
 * @author Philippe LEMORDANT
 */
public class InitiateImageSharing extends RcsActivity {

    private static final String LOGTAG = LogUtils.getTag(InitiateImageSharing.class.getName());

    /**
     * Activity result constants
     */
    private final static int SELECT_IMAGE = 0;

    /**
     * UI handler
     */
    private final Handler mHandler = new Handler();

    private String mFilename;
    private Uri mFile;
    private long mFilesize = -1;
    private ImageSharing mImageSharing;
    private String mSharingId;

    /**
     * Spinner for contact selection
     */
    private Spinner mSpinner;
    private ImageSharingListener mIshListener;
    private Button mDialBtn;
    private Button mInviteBtn;
    private ProgressBar mProgressBar;
    private Button mSelectBtn;
    private TextView mStatusView;
    private TextView mFilenameView;
    private ImageSharingService mImageSharingService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.image_sharing_initiate);
        initialize();
        // Register to API connection manager
        if (!isServiceConnected(RcsServiceName.IMAGE_SHARING)) {
            showMessageThenExit(R.string.label_service_not_available);
            return;
        }
        startMonitorServices(RcsServiceName.IMAGE_SHARING);
        try {
            mImageSharingService.addEventListener(mIshListener);

        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!isServiceConnected(RcsServiceName.IMAGE_SHARING)) {
            return;
        }
        try {
            getImageSharingApi().removeEventListener(mIshListener);
        } catch (RcsServiceException e) {
            Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (RESULT_OK != resultCode || SELECT_IMAGE != requestCode) {
            return;
        }
        if (data != null && data.getData() != null) {
            // Get selected photo URI
            mFile = data.getData();
            // Display the selected filename attribute
            mFilename = FileUtils.getFileName(this, mFile);
            mFilesize = FileUtils.getFileSize(this, mFile);
            mFilenameView.setText(mFilename);
            TextView sizeView = (TextView) findViewById(R.id.size);
            sizeView.setText(FileUtils.humanReadableByteCount(mFilesize, true));
            mInviteBtn.setEnabled(true);
        }
    }

    private void updateProgressBar(long currentSize, long totalSize) {
        double position = 0.0d;
        if (totalSize != 0) {
            mStatusView.setText(Utils.getProgressLabel(currentSize, totalSize));
            position = (((double) currentSize) / ((double) totalSize)) * 100.0d;
        } else {
            mStatusView.setText("");
        }
        mProgressBar.setProgress((int) position);
    }

    private void quitSession() {
        try {
            if (mImageSharing != null
                    && RcsSessionUtil.isAllowedToAbortImageSharingSession(mImageSharing)) {
                mImageSharing.abortSharing();
            }
        } catch (RcsServiceException e) {
            showException(e);

        } finally {
            mImageSharing = null;
            // Exit activity
            finish();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_BACK == keyCode) {
            try {
                if (mImageSharing == null
                        || !RcsSessionUtil.isAllowedToAbortImageSharingSession(mImageSharing)) {
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
                            }
                        });
                builder.setCancelable(true);
                registerDialog(builder.show());
                return true;

            } catch (RcsServiceException e) {
                showException(e);
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(getApplicationContext());
        inflater.inflate(R.menu.menu_image_sharing, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_close_session) {
            quitSession();
        }
        return true;
    }

    private void initialize() {
        mImageSharingService = getImageSharingApi();
        mSpinner = (Spinner) findViewById(R.id.contact);
        ContactListAdapter adapter = ContactListAdapter.createRcsContactListAdapter(this);
        mSpinner.setAdapter(adapter);

        OnClickListener btnInviteListener = new OnClickListener() {
            public void onClick(View v) {
                // Check if the service is available
                try {
                    if (!mImageSharingService.isServiceRegistered()) {
                        showMessage(R.string.error_not_registered);
                        return;
                    }
                    // Get the remote contact
                    ContactListAdapter adapter = (ContactListAdapter) mSpinner.getAdapter();
                    String phoneNumber = adapter.getSelectedNumber(mSpinner.getSelectedView());
                    final ContactId remote = ContactUtil.formatContact(phoneNumber);
                    if (LogUtils.isActive) {
                        Log.d(LOGTAG, "shareImage image=" + mFilename + " size=" + mFilesize);
                    }
                    /* Only take persistable permission for content Uris */
                    takePersistableContentUriPermission(InitiateImageSharing.this, mFile);
                    // Initiate sharing
                    mImageSharing = mImageSharingService.shareImage(remote, mFile);
                    mSharingId = mImageSharing.getSharingId();
                    // Disable UI
                    mSpinner.setEnabled(false);
                    // Hide buttons
                    mInviteBtn.setVisibility(View.INVISIBLE);
                    mSelectBtn.setVisibility(View.INVISIBLE);
                    mDialBtn.setVisibility(View.INVISIBLE);

                } catch (RcsServiceNotAvailableException e) {
                    showMessage(R.string.label_service_not_available);

                } catch (RcsServiceException e) {
                    showExceptionThenExit(e);
                }
            }
        };
        mInviteBtn = (Button) findViewById(R.id.invite_btn);
        mInviteBtn.setOnClickListener(btnInviteListener);
        mInviteBtn.setEnabled(false);

        OnClickListener btnSelectListener = new OnClickListener() {
            public void onClick(View v) {
                FileUtils.openFile(InitiateImageSharing.this, "image/*", SELECT_IMAGE);
            }
        };
        mSelectBtn = (Button) findViewById(R.id.select_btn);
        mSelectBtn.setOnClickListener(btnSelectListener);
        mSelectBtn.setEnabled(false);

        OnClickListener btnDialListener = new OnClickListener() {
            public void onClick(View v) {
                // get selected phone number
                ContactListAdapter adapter = (ContactListAdapter) mSpinner.getAdapter();
                String phoneNumber = adapter.getSelectedNumber(mSpinner.getSelectedView());

                // Initiate a GSM call before to be able to share content
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:".concat(phoneNumber)));
                if (ActivityCompat.checkSelfPermission(InitiateImageSharing.this,
                        Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                startActivity(intent);
            }
        };
        mDialBtn = (Button) findViewById(R.id.dial_btn);
        mDialBtn.setOnClickListener(btnDialListener);
        mDialBtn.setEnabled(false);

        mStatusView = (TextView) findViewById(R.id.progress_status);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mFilenameView = (TextView) findViewById(R.id.uri);
        updateProgressBar(0, 0);
        mFilenameView.setText("");
        if (adapter == null || adapter.getCount() != 0) {
            mDialBtn.setEnabled(true);
            mSelectBtn.setEnabled(true);
        }

        mIshListener = new ImageSharingListener() {

            @Override
            public void onProgressUpdate(ContactId contact, String sharingId,
                    final long currentSize, final long totalSize) {
                // Discard event if not for current sharingId
                if (mSharingId == null || !mSharingId.equals(sharingId)) {
                    return;
                }
                mHandler.post(new Runnable() {
                    public void run() {
                        // Display sharing progress
                        updateProgressBar(currentSize, totalSize);
                    }
                });
            }

            @Override
            public void onStateChanged(ContactId contact, String sharingId,
                    final ImageSharing.State state, ImageSharing.ReasonCode reasonCode) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onStateChanged contact=" + contact + " sharingId=" + sharingId
                            + " state=" + state + " reason=" + reasonCode);
                }
                // Discard event if not for current sharingId
                if (mSharingId == null || !mSharingId.equals(sharingId)) {
                    return;
                }
                final String _reasonCode = RiApplication.sImageSharingReasonCodes[reasonCode
                        .toInt()];
                final String _state = RiApplication.sImageSharingStates[state.toInt()];
                mHandler.post(new Runnable() {
                    public void run() {
                        TextView statusView = (TextView) findViewById(R.id.progress_status);
                        switch (state) {
                            case STARTED:
                                statusView.setText(_state);
                                break;

                            case ABORTED:
                                String msg = getString(R.string.label_sharing_aborted, _reasonCode);
                                mStatusView.setText(msg);
                                showMessageThenExit(msg);
                                break;

                            case REJECTED:
                                msg = getString(R.string.label_sharing_rejected, _reasonCode);
                                mStatusView.setText(msg);
                                showMessageThenExit(msg);
                                break;

                            case FAILED:
                                msg = getString(R.string.label_sharing_failed, _reasonCode);
                                mStatusView.setText(msg);
                                showMessageThenExit(msg);
                                break;

                            case TRANSFERRED:
                                statusView.setText(_state);
                                break;

                            default:
                                statusView.setText(_state);
                                if (LogUtils.isActive) {
                                    Log.d(LOGTAG,
                                            "onStateChanged "
                                                    + getString(R.string.label_ish_state_changed,
                                                            _state, _reasonCode));
                                }
                        }
                    }
                });
            }

            @Override
            public void onDeleted(ContactId contact, Set<String> sharingIds) {
                if (LogUtils.isActive) {
                    Log.w(LOGTAG, "onDeleted contact=" + contact + " sharingIds=" + sharingIds);
                }
            }
        };
    }
}
