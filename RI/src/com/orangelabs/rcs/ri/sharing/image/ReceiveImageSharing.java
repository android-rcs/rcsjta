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

import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.image.ImageSharing;
import com.gsma.services.rcs.sharing.image.ImageSharingListener;
import com.gsma.services.rcs.sharing.image.ImageSharingService;

import com.orangelabs.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.api.connection.utils.ExceptionUtil;
import com.orangelabs.rcs.api.connection.utils.RcsActivity;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsContactUtil;
import com.orangelabs.rcs.ri.utils.RcsSessionUtil;
import com.orangelabs.rcs.ri.utils.Utils;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
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
 * @author Philippe LEMORDANT
 */
public class ReceiveImageSharing extends RcsActivity {
    /**
     * UI handler
     */
    private final Handler handler = new Handler();

    private ImageSharing mImageSharing;

    /**
     * The Image Sharing Data Object
     */
    private ImageSharingDAO mIshDao;

    private static final String LOGTAG = LogUtils.getTag(ReceiveImageSharing.class.getSimpleName());

    private ImageSharingListener mListener;

    private OnClickListener mAcceptBtnListener;

    private OnClickListener mDeclineBtnListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        intitialize();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.image_sharing_receive);
        /* Register to API connection manager */
        if (!isServiceConnected(RcsServiceName.IMAGE_SHARING, RcsServiceName.CONTACT)) {
            showMessageThenExit(R.string.label_service_not_available);
            return;
        }
        startMonitorServices(RcsServiceName.IMAGE_SHARING, RcsServiceName.CONTACT);
        processIntent(getIntent());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!isServiceConnected(RcsServiceName.IMAGE_SHARING)) {
            return;
        }
        try {
            getImageSharingApi().removeEventListener(mListener);
        } catch (RcsServiceException e) {
            Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        processIntent(intent);
    }

    private void processIntent(Intent invitation) {
        mIshDao = invitation.getParcelableExtra(ImageSharingIntentService.BUNDLE_ISHDAO_ID);
        initiateImageSharing();
    }

    private void initiateImageSharing() {
        ImageSharingService ishApi = getImageSharingApi();
        try {
            /* Add service listener */
            ishApi.addEventListener(mListener);

            /* Get the image sharing */
            mImageSharing = ishApi.getImageSharing(mIshDao.getSharingId());
            if (mImageSharing == null) {
                // Session not found or expired
                showMessageThenExit(R.string.label_session_not_found);
                return;
            }

            String from = RcsContactUtil.getInstance(this).getDisplayName(mIshDao.getContact());
            /* Display sharing infos */
            TextView fromTextView = (TextView) findViewById(R.id.from);
            fromTextView.setText(getString(R.string.label_from_args, from));

            long fileSize = mIshDao.getSize();
            String size = getString(R.string.label_file_size, fileSize / 1024);
            TextView sizeTxt = (TextView) findViewById(R.id.image_size);
            sizeTxt.setText(size);

            updateProgressBar(0, fileSize);

            /* Display accept/reject dialog */
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.title_image_sharing);
            builder.setMessage(getString(R.string.label_ft_from_size, from, fileSize / 1024));
            builder.setCancelable(false);
            builder.setIcon(R.drawable.ri_notif_csh_icon);
            builder.setPositiveButton(R.string.label_accept, mAcceptBtnListener);
            builder.setNegativeButton(R.string.label_decline, mDeclineBtnListener);
            registerDialog(builder.show());

        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    private void acceptInvitation() {
        try {
            mImageSharing.acceptInvitation();
        } catch (RcsGenericException e) {
            showExceptionThenExit(e);
        }
    }

    private void rejectInvitation() {
        try {
            mImageSharing.rejectInvitation();
        } catch (RcsGenericException e) {
            showExceptionThenExit(e);
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
            if (mImageSharing != null && ImageSharing.State.STARTED == mImageSharing.getState()) {
                mImageSharing.abortSharing();
            }
        } catch (RcsServiceException e) {
            showException(e);

        } finally {
            mImageSharing = null;
            /* Exit activity */
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

    private void intitialize() {
        mListener = new ImageSharingListener() {

            @Override
            public void onProgressUpdate(ContactId contact, String sharingId,
                    final long currentSize, final long totalSize) {
                /* Discard event if not for current sharingId */
                if (mIshDao == null || !mIshDao.getSharingId().equals(sharingId)) {
                    return;
                }
                handler.post(new Runnable() {
                    public void run() {
                        /* Display sharing progress */
                        updateProgressBar(currentSize, totalSize);
                    }
                });
            }

            @Override
            public void onStateChanged(ContactId contact, String sharingId,
                    final ImageSharing.State state, ImageSharing.ReasonCode reasonCode) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onStateChanged contact=" + contact.toString() + " sharingId="
                            + sharingId + " state=" + state + " reason=" + reasonCode);
                }
                /* Discard event if not for current sharingId */
                if (mIshDao == null || !mIshDao.getSharingId().equals(sharingId)) {
                    return;
                }
                final String _reasonCode = RiApplication.sImageSharingReasonCodes[reasonCode
                        .toInt()];
                final String _state = RiApplication.sImageSharingStates[state.toInt()];
                handler.post(new Runnable() {
                    public void run() {

                        TextView statusView = (TextView) findViewById(R.id.progress_status);
                        switch (state) {
                            case ABORTED:
                                showMessageThenExit(getString(R.string.label_sharing_aborted,
                                        _reasonCode));
                                break;

                            case FAILED:
                                showMessageThenExit(getString(R.string.label_sharing_failed,
                                        _reasonCode));
                                break;

                            case REJECTED:
                                showMessageThenExit(getString(R.string.label_sharing_rejected,
                                        _reasonCode));
                                break;

                            case TRANSFERRED:
                                // Display transfer progress
                                statusView.setText(_state);
                                // Make sure progress bar is at the end
                                ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
                                progressBar.setProgress(progressBar.getMax());

                                // Show the shared image
                                Utils.showPictureAndExit(ReceiveImageSharing.this,
                                        mIshDao.getFile());
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
                    Log.w(LOGTAG, "onDeleted contact=" + contact + " sharingIds=" + sharingIds);
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
    }
}
