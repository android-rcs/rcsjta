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
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.image.ImageSharing;
import com.gsma.services.rcs.sharing.image.ImageSharingListener;
import com.gsma.services.rcs.sharing.image.ImageSharingService;

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
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Set;

/**
 * Receive image sharing
 * 
 * @author Jean-Marc AUFFRET
 * @author YPLO6403
 */
public class ReceiveImageSharing extends Activity {
    /**
     * UI handler
     */
    private final Handler handler = new Handler();

    /**
     * Image sharing
     */
    private ImageSharing mImageSharing;

    /**
     * The Image Sharing Data Object
     */
    private ImageSharingDAO mIshDao;

    /**
     * A locker to exit only once
     */
    private LockAccess mExitOnce = new LockAccess();

    /**
     * API connection manager
     */
    private ConnectionManager mCnxManager;

    /**
     * The log tag for this class
     */
    private static final String LOGTAG = LogUtils.getTag(ReceiveImageSharing.class.getSimpleName());

    /**
     * Image sharing listener
     */
    private ImageSharingListener mListener = new ImageSharingListener() {

        @Override
        public void onProgressUpdate(ContactId contact, String sharingId, final long currentSize,
                final long totalSize) {
            // Discard event if not for current sharingId
            if (mIshDao == null || !mIshDao.getSharingId().equals(sharingId)) {
                return;
            }
            handler.post(new Runnable() {
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
                Log.d(LOGTAG,
                        new StringBuilder("onStateChanged contact=").append(contact.toString())
                                .append(" sharingId=").append(sharingId).append(" state=")
                                .append(state).append(" reason=").append(reasonCode).toString());
            }
            // Discard event if not for current sharingId
            if (mIshDao == null || !mIshDao.getSharingId().equals(sharingId)) {
                return;
            }
            final String _reasonCode = RiApplication.sImageSharingReasonCodes[reasonCode.toInt()];
            final String _state = RiApplication.sImageSharingStates[state.toInt()];
            handler.post(new Runnable() {
                public void run() {

                    TextView statusView = (TextView) findViewById(R.id.progress_status);
                    switch (state) {
                        case STARTED:
                            // Display session status
                            statusView.setText(_state);
                            break;

                        case ABORTED:
                            // Session is aborted: exit
                            Utils.showMessageAndExit(ReceiveImageSharing.this,
                                    getString(R.string.label_sharing_aborted, _reasonCode),
                                    mExitOnce);
                            break;

                        case FAILED:
                            // Session is failed: exit
                            Utils.showMessageAndExit(ReceiveImageSharing.this,
                                    getString(R.string.label_sharing_failed, _reasonCode),
                                    mExitOnce);
                            break;

                        case REJECTED:
                            // Session is failed: exit
                            Utils.showMessageAndExit(ReceiveImageSharing.this,
                                    getString(R.string.label_sharing_rejected, _reasonCode),
                                    mExitOnce);
                            break;

                        case TRANSFERRED:
                            // Display transfer progress
                            statusView.setText(_state);
                            // Make sure progress bar is at the end
                            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
                            progressBar.setProgress(progressBar.getMax());

                            // Show the shared image
                            Utils.showPictureAndExit(ReceiveImageSharing.this, mIshDao.getFile());
                            break;

                        default:
                            // Display session status
                            statusView.setText(_state);
                            if (LogUtils.isActive) {
                                Log.d(LOGTAG, "onStateChanged ".concat(getString(
                                        R.string.label_ish_state_changed, _state, _reasonCode)));
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

        mCnxManager = ConnectionManager.getInstance();

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.image_sharing_receive);

        // Get invitation info
        mIshDao = (ImageSharingDAO) (getIntent().getExtras()
                .getParcelable(ImageSharingIntentService.BUNDLE_ISHDAO_ID));
        if (mIshDao == null) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "onCreate cannot read Image Sharing invitation");
            }
            finish();
            return;
        }

        // Register to API connection manager
        if (!mCnxManager.isServiceConnected(RcsServiceName.IMAGE_SHARING, RcsServiceName.CONTACT)) {
            Utils.showMessageAndExit(this, getString(R.string.label_service_not_available),
                    mExitOnce);
        } else {
            mCnxManager.startMonitorServices(this, mExitOnce, RcsServiceName.IMAGE_SHARING,
                    RcsServiceName.CONTACT);
            initiateImageSharing();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCnxManager.stopMonitorServices(this);
        if (!mCnxManager.isServiceConnected(RcsServiceName.IMAGE_SHARING)) {
            return;
        }
        // Remove file transfer listener
        try {
            mCnxManager.getImageSharingApi().removeEventListener(mListener);
        } catch (Exception e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Failed to remove listener", e);
            }
        }
    }

    private void initiateImageSharing() {
        ImageSharingService ishApi = mCnxManager.getImageSharingApi();
        try {
            // Add service listener
            ishApi.addEventListener(mListener);

            // Get the image sharing
            mImageSharing = ishApi.getImageSharing(mIshDao.getSharingId());
            if (mImageSharing == null) {
                // Session not found or expired
                Utils.showMessageAndExit(this, getString(R.string.label_session_not_found),
                        mExitOnce);
                return;
            }

            String from = RcsDisplayName.getInstance(this).getDisplayName(mIshDao.getContact());
            // Display sharing infos
            TextView fromTextView = (TextView) findViewById(R.id.from);
            fromTextView.setText(getString(R.string.label_from_args, from));

            String size = getString(R.string.label_file_size, mIshDao.getSize() / 1024);
            TextView sizeTxt = (TextView) findViewById(R.id.image_size);
            sizeTxt.setText(size);

            // Display accept/reject dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.title_image_sharing);
            builder.setMessage(getString(R.string.label_ft_from_size, from,
                    mIshDao.getSize() / 1024));
            builder.setCancelable(false);
            builder.setIcon(R.drawable.ri_notif_csh_icon);
            builder.setPositiveButton(getString(R.string.label_accept), acceptBtnListener);
            builder.setNegativeButton(getString(R.string.label_decline), declineBtnListener);
            builder.show();
        } catch (RcsServiceNotAvailableException e) {
            Utils.showMessageAndExit(this, getString(R.string.label_api_unavailable), mExitOnce, e);
        } catch (RcsServiceException e) {
            Utils.showMessageAndExit(this, getString(R.string.label_api_failed), mExitOnce, e);
        }
    }

    /**
     * Accept invitation
     */
    private void acceptInvitation() {
        try {
            // Accept the invitation
            mImageSharing.acceptInvitation();
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
            // Reject the invitation
            mImageSharing.rejectInvitation();
        } catch (Exception e) {
            e.printStackTrace();
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
        statusView.setText(Utils.getProgressLabel(currentSize, totalSize));
        double position = ((double) currentSize / (double) totalSize) * 100.0;
        progressBar.setProgress((int) position);
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
