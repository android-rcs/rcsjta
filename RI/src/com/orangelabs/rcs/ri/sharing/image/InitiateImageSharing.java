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

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.image.ImageSharing;
import com.gsma.services.rcs.sharing.image.ImageSharingListener;

import com.orangelabs.rcs.ri.ConnectionManager;
import com.orangelabs.rcs.ri.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.utils.ContactListAdapter;
import com.orangelabs.rcs.ri.utils.ContactUtil;
import com.orangelabs.rcs.ri.utils.FileUtils;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

import android.app.Activity;
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
 * @author YPLO6403
 */
public class InitiateImageSharing extends Activity {
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
     * A locker to exit only once
     */
    private LockAccess mExitOnce = new LockAccess();

    /**
     * API connection manager
     */
    private ConnectionManager mCnxManager;

    /**
     * Spinner for contact selection
     */
    private Spinner mSpinner;

    private static final String LOGTAG = LogUtils
            .getTag(InitiateImageSharing.class.getSimpleName());

    private ImageSharingListener ishListener = new ImageSharingListener() {

        @Override
        public void onProgressUpdate(ContactId contact, String sharingId, final long currentSize,
                final long totalSize) {
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
            final String _reasonCode = RiApplication.sImageSharingReasonCodes[reasonCode.toInt()];
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
                            // Session is aborted: hide progress dialog then
                            // exit
                            hideProgressDialog();
                            Utils.showMessageAndExit(InitiateImageSharing.this,
                                    getString(R.string.label_sharing_aborted, _reasonCode),
                                    mExitOnce);
                            break;

                        case REJECTED:
                            // Session is rejected: hide progress dialog then
                            // exit
                            hideProgressDialog();
                            Utils.showMessageAndExit(InitiateImageSharing.this,
                                    getString(R.string.label_sharing_rejected, _reasonCode),
                                    mExitOnce);
                            break;

                        case FAILED:
                            // Session failed: hide progress dialog then exit
                            hideProgressDialog();
                            Utils.showMessageAndExit(InitiateImageSharing.this,
                                    getString(R.string.label_sharing_failed, _reasonCode),
                                    mExitOnce);
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
                Log.w(LOGTAG,
                        new StringBuilder("onDeleted contact=").append(contact)
                                .append(" sharingIds=").append(sharingIds).toString());
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.image_sharing_initiate);

        // Set contact selector
        mSpinner = (Spinner) findViewById(R.id.contact);
        mSpinner.setAdapter(ContactListAdapter.createRcsContactListAdapter(this));

        // Set buttons callback
        Button inviteBtn = (Button) findViewById(R.id.invite_btn);
        inviteBtn.setOnClickListener(btnInviteListener);
        inviteBtn.setEnabled(false);
        Button selectBtn = (Button) findViewById(R.id.select_btn);
        selectBtn.setOnClickListener(btnSelectListener);
        selectBtn.setEnabled(false);
        Button dialBtn = (Button) findViewById(R.id.dial_btn);
        dialBtn.setOnClickListener(btnDialListener);
        dialBtn.setEnabled(false);
        // Disable button if no contact available
        if (mSpinner.getAdapter().getCount() != 0) {
            dialBtn.setEnabled(true);
            selectBtn.setEnabled(true);
        }

        // Register to API connection manager
        mCnxManager = ConnectionManager.getInstance(this);
        if (mCnxManager == null || !mCnxManager.isServiceConnected(RcsServiceName.IMAGE_SHARING)) {
            Utils.showMessageAndExit(this, getString(R.string.label_service_not_available),
                    mExitOnce);
            return;
        }
        mCnxManager.startMonitorServices(this, mExitOnce, RcsServiceName.IMAGE_SHARING);
        try {
            // Add service listener
            mCnxManager.getImageSharingApi().addEventListener(ishListener);
        } catch (RcsServiceException e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Failed to add listener", e);
            }
            Utils.showMessageAndExit(this, getString(R.string.label_api_failed), mExitOnce);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCnxManager == null) {
            return;

        }
        mCnxManager.stopMonitorServices(this);
        if (!mCnxManager.isServiceConnected(RcsServiceName.IMAGE_SHARING)) {
            return;

        }
        // Remove image sharing listener
        try {
            mCnxManager.getImageSharingApi().removeEventListener(ishListener);
        } catch (Exception e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Failed to remove listener", e);
            }
        }
    }

    /**
     * Dial button listener
     */
    private OnClickListener btnDialListener = new OnClickListener() {
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

    /**
     * Invite button listener
     */
    private OnClickListener btnInviteListener = new OnClickListener() {
        public void onClick(View v) {
            // Check if the service is available
            boolean registered = false;
            try {
                registered = mCnxManager.getImageSharingApi().isServiceRegistered();
            } catch (Exception e) {
                Utils.showMessageAndExit(InitiateImageSharing.this,
                        getString(R.string.label_api_failed), mExitOnce, e);
                return;
            }
            if (!registered) {
                Utils.showMessage(InitiateImageSharing.this,
                        getString(R.string.label_service_not_available));
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
                FileUtils.tryToTakePersistableContentUriPermission(getApplicationContext(), mFile);

                // Initiate sharing
                mImageSharing = mCnxManager.getImageSharingApi().shareImage(remote, mFile);
                mSharingId = mImageSharing.getSharingId();

                // Display a progress dialog
                mProgressDialog = Utils.showProgressDialog(InitiateImageSharing.this,
                        getString(R.string.label_command_in_progress));
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
            } catch (Exception e) {
                hideProgressDialog();
                Utils.showMessageAndExit(InitiateImageSharing.this,
                        getString(R.string.label_invitation_failed), mExitOnce, e);
            }
        }
    };

    /**
     * Select image button listener
     */
    private OnClickListener btnSelectListener = new OnClickListener() {
        public void onClick(View v) {
            FileUtils.openFile(InitiateImageSharing.this, "image/*", SELECT_IMAGE);
        }
    };

    /**
     * On activity result
     * 
     * @param requestCode Request code
     * @param resultCode Result code
     * @param data Data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;

        }

        switch (requestCode) {
            case SELECT_IMAGE:
                if ((data != null) && (data.getData() != null)) {
                    // Get selected photo URI
                    mFile = data.getData();
                    // Display the selected filename attribute
                    TextView uriEdit = (TextView) findViewById(R.id.uri);
                    try {
                        mFilename = FileUtils.getFileName(this, mFile);
                        mFilesize = FileUtils.getFileSize(this, mFile) / 1024;
                        uriEdit.setText(mFilesize + " KB");
                    } catch (Exception e) {
                        mFilesize = -1;
                        uriEdit.setText("Unknown");
                    }
                    // Enable invite button
                    Button inviteBtn = (Button) findViewById(R.id.invite_btn);
                    inviteBtn.setEnabled(true);
                }
                break;
        }
    }

    /**
     * Hide progress dialog
     */
    public void hideProgressDialog() {
        if (mProgressDialog == null) {
            return;
        }
        if (mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        mProgressDialog = null;
    }

    /**
     * Show the sharing progress
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
            if (mImageSharing != null && ImageSharing.State.STARTED == mImageSharing.getState()) {
                mImageSharing.abortSharing();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mImageSharing = null;

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
        inflater.inflate(R.menu.menu_image_sharing, menu);
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
}
