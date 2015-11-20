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

package com.orangelabs.rcs.ri.sharing.image;

import static com.orangelabs.rcs.ri.utils.FileUtils.takePersistableContentUriPermission;

import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.image.ImageSharing;
import com.gsma.services.rcs.sharing.image.ImageSharingListener;

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
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.Toast;

import java.util.Set;

/**
 * Initiate image sharing
 * 
 * @author Jean-Marc AUFFRET
 * @author Philippe LEMORDANT
 */
public class InitiateImageSharing extends RcsActivity {
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

    private Dialog mProgressDialog;

    /**
     * Spinner for contact selection
     */
    private Spinner mSpinner;

    private OnClickListener mBtnSelectListener;

    private OnClickListener mBtnInviteListener;

    private OnClickListener mBtnDialListener;

    private ImageSharingListener mIshListener;

    private static final String LOGTAG = LogUtils
            .getTag(InitiateImageSharing.class.getSimpleName());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.image_sharing_initiate);

        // Set contact selector
        mSpinner = (Spinner) findViewById(R.id.contact);
        mSpinner.setAdapter(ContactListAdapter.createRcsContactListAdapter(this));

        // Set buttons callback
        Button inviteBtn = (Button) findViewById(R.id.invite_btn);
        inviteBtn.setOnClickListener(mBtnInviteListener);
        inviteBtn.setEnabled(false);
        Button selectBtn = (Button) findViewById(R.id.select_btn);
        selectBtn.setOnClickListener(mBtnSelectListener);
        selectBtn.setEnabled(false);
        Button dialBtn = (Button) findViewById(R.id.dial_btn);
        dialBtn.setOnClickListener(mBtnDialListener);
        dialBtn.setEnabled(false);
        // Disable button if no contact available
        if (mSpinner.getAdapter().getCount() != 0) {
            dialBtn.setEnabled(true);
            selectBtn.setEnabled(true);
        }

        // Register to API connection manager
        if (!isServiceConnected(RcsServiceName.IMAGE_SHARING)) {
            showMessageThenExit(R.string.label_service_not_available);
            return;
        }
        startMonitorServices(RcsServiceName.IMAGE_SHARING);
        try {
            getImageSharingApi().addEventListener(mIshListener);

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
            TextView uriEdit = (TextView) findViewById(R.id.uri);
            mFilename = FileUtils.getFileName(this, mFile);
            mFilesize = FileUtils.getFileSize(this, mFile) / 1024;
            uriEdit.setText(mFilesize + " KB");
            // Enable invite button
            Button inviteBtn = (Button) findViewById(R.id.invite_btn);
            inviteBtn.setEnabled(true);
        }
    }

    private void hideProgressDialog() {
        if (mProgressDialog == null) {
            return;
        }
        if (mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        mProgressDialog = null;
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
        switch (item.getItemId()) {
            case R.id.menu_close_session:
                quitSession();
                break;
        }
        return true;
    }

    private void initialize() {
        mBtnSelectListener = new OnClickListener() {
            public void onClick(View v) {
                FileUtils.openFile(InitiateImageSharing.this, "image/*", SELECT_IMAGE);
            }
        };

        mBtnInviteListener = new OnClickListener() {
            public void onClick(View v) {
                // Check if the service is available
                try {
                    boolean registered = getImageSharingApi().isServiceRegistered();
                    if (!registered) {
                        showMessage(R.string.error_not_registered);
                        return;
                    }

                } catch (RcsGenericException e) {
                    showExceptionThenExit(e);
                    return;

                } catch (RcsServiceNotAvailableException e) {
                    showMessage(R.string.label_service_not_available);
                    return;
                }

                // Get the remote contact
                ContactListAdapter adapter = (ContactListAdapter) mSpinner.getAdapter();
                String phoneNumber = adapter.getSelectedNumber(mSpinner.getSelectedView());
                final ContactId remote = ContactUtil.formatContact(phoneNumber);

                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "shareImage image=" + mFilename + " size=" + mFilesize);
                }
                try {
                    /* Only take persistable permission for content Uris */
                    takePersistableContentUriPermission(InitiateImageSharing.this, mFile);

                    // Initiate sharing
                    mImageSharing = getImageSharingApi().shareImage(remote, mFile);
                    mSharingId = mImageSharing.getSharingId();

                    // Display a progress dialog
                    mProgressDialog = showProgressDialog(getString(R.string.label_command_in_progress));
                    mProgressDialog.setOnCancelListener(new OnCancelListener() {
                        public void onCancel(DialogInterface dialog) {
                            Toast.makeText(InitiateImageSharing.this,
                                    getString(R.string.label_sharing_cancelled), Toast.LENGTH_SHORT)
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
                    Button dialBtn = (Button) findViewById(R.id.dial_btn);
                    dialBtn.setVisibility(View.INVISIBLE);
                } catch (RcsServiceException e) {
                    showExceptionThenExit(e);
                }
            }
        };

        mBtnDialListener = new OnClickListener() {
            public void onClick(View v) {
                // get selected phone number
                ContactListAdapter adapter = (ContactListAdapter) mSpinner.getAdapter();
                String phoneNumber = adapter.getSelectedNumber(mSpinner.getSelectedView());

                // Initiate a GSM call before to be able to share content
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:".concat(phoneNumber)));
                startActivity(intent);
            }
        };

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
                                // Session is established: hide progress dialog
                                hideProgressDialog();
                                // Display session status
                                statusView.setText(_state);
                                break;

                            case ABORTED:
                                showMessageThenExit(getString(R.string.label_sharing_aborted,
                                        _reasonCode));
                                break;

                            case REJECTED:
                                showMessageThenExit(getString(R.string.label_sharing_rejected,
                                        _reasonCode));
                                break;

                            case FAILED:
                                showMessageThenExit(getString(R.string.label_sharing_failed,
                                        _reasonCode));
                                break;

                            case TRANSFERRED:
                                // Hide progress dialog
                                hideProgressDialog();
                                // Display transfer progress
                                statusView.setText(_state);
                                break;

                            default:
                                // Display session status
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
