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

package com.orangelabs.rcs.ri.sharing.geoloc;

import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharing;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharingIntent;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharingListener;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharingService;

import com.orangelabs.rcs.ri.ConnectionManager;
import com.orangelabs.rcs.ri.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.messaging.geoloc.DisplayGeoloc;
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
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Set;

/**
 * Receive geoloc sharing
 * 
 * @author vfml3370
 */
public class ReceiveGeolocSharing extends Activity {
    /**
     * UI handler
     */
    private final Handler handler = new Handler();

    /**
     * Sharing ID
     */
    private String mSharingId;

    /**
     * Remote Contact
     */
    private ContactId mRemoteContact;

    /**
     * Geoloc sharing session
     */
    private GeolocSharing mGeolocSharing;

    /**
     * A locker to exit only once
     */
    private LockAccess mExitOnce = new LockAccess();

    /**
     * API connection manager
     */
    private ConnectionManager mCnxManager;

    private Geoloc mGeoloc;

    /**
     * The log tag for this class
     */
    private static final String LOGTAG = LogUtils
            .getTag(ReceiveGeolocSharing.class.getSimpleName());

    /**
     * Geoloc sharing listener
     */
    private GeolocSharingListener gshListener = new GeolocSharingListener() {

        @Override
        public void onProgressUpdate(ContactId contact, String sharingId, final long currentSize,
                final long totalSize) {
            // Discard event if not for current sharingId
            if (mSharingId == null || !mSharingId.equals(sharingId)) {
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
        public void onStateChanged(final ContactId contact, final String sharingId,
                final GeolocSharing.State state, GeolocSharing.ReasonCode reasonCode) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG,
                        new StringBuilder("onStateChanged contact=").append(contact.toString())
                                .append(" sharingId=").append(sharingId).append(" state=")
                                .append(state).append(" reason=").append(reasonCode).toString());
            }
            // Discard event if not for current sharingId
            if (mSharingId == null || !mSharingId.equals(sharingId)) {
                return;
            }
            final String _reasonCode = RiApplication.sGeolocReasonCodes[reasonCode.toInt()];
            final String _state = RiApplication.sGeolocSharingStates[state.toInt()];
            handler.post(new Runnable() {
                public void run() {
                    TextView statusView = (TextView) findViewById(R.id.progress_status);
                    switch (state) {
                        case STARTED:
                            // Session is established: display session status
                            statusView.setText("started");
                            break;

                        case ABORTED:
                            // Session is aborted: display session status
                            Utils.showMessageAndExit(ReceiveGeolocSharing.this,
                                    getString(R.string.label_sharing_aborted, _reasonCode),
                                    mExitOnce);
                            break;

                        case FAILED:
                            // Session is failed: exit
                            Utils.showMessageAndExit(ReceiveGeolocSharing.this,
                                    getString(R.string.label_sharing_failed, _reasonCode),
                                    mExitOnce);
                            break;

                        case TRANSFERRED:
                            // Display transfer progress
                            statusView.setText(_state);
                            // Make sure progress bar is at the end
                            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
                            progressBar.setProgress(progressBar.getMax());

                            // Show the shared geoloc
                            Intent intent = new Intent(ReceiveGeolocSharing.this,
                                    DisplayGeoloc.class);
                            intent.putExtra(DisplayGeoloc.EXTRA_CONTACT, (Parcelable) contact);

                            try {
                                mGeoloc = mGeolocSharing.getGeoloc();
                                intent.putExtra(DisplayGeoloc.EXTRA_GEOLOC, (Parcelable) mGeoloc);
                                startActivity(intent);
                            } catch (RcsServiceException e) {
                                if (LogUtils.isActive) {
                                    Log.d(LOGTAG, "onStateChanged failed to get geoloc for "
                                            .concat(sharingId));
                                }
                            }
                            break;

                        default:
                            statusView.setText(_state);
                            if (LogUtils.isActive) {
                                Log.d(LOGTAG, "onStateChanged ".concat(getString(
                                        R.string.label_gsh_state_changed, _state, _reasonCode)));
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
        setContentView(R.layout.geoloc_sharing_receive);

        // Get invitation info
        mSharingId = getIntent().getStringExtra(GeolocSharingIntent.EXTRA_SHARING_ID);
        mRemoteContact = getIntent().getParcelableExtra(GeolocSharingIntentService.BUNDLE_GSH_ID);

        // Register to API connection manager
        if (!mCnxManager.isServiceConnected(RcsServiceName.GEOLOC_SHARING, RcsServiceName.CONTACT)) {
            Utils.showMessageAndExit(this, getString(R.string.label_service_not_available),
                    mExitOnce);
        } else {
            mCnxManager.startMonitorServices(this, mExitOnce, RcsServiceName.GEOLOC_SHARING,
                    RcsServiceName.CONTACT);
            initiateGeolocSharing();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCnxManager.stopMonitorServices(this);
        if (mCnxManager.isServiceConnected(RcsServiceName.GEOLOC_SHARING)) {
            // Remove service listener
            try {
                mCnxManager.getGeolocSharingApi().removeEventListener(gshListener);
            } catch (Exception e) {
                if (LogUtils.isActive) {
                    Log.e(LOGTAG, "Failed to remove listener", e);
                }
            }
        }
    }

    private void initiateGeolocSharing() {
        GeolocSharingService gshApi = mCnxManager.getGeolocSharingApi();
        try {
            // Add service listener
            gshApi.addEventListener(gshListener);

            // Get the geoloc sharing
            mGeolocSharing = gshApi.getGeolocSharing(mSharingId);
            if (mGeolocSharing == null) {
                // Session not found or expired
                Utils.showMessageAndExit(this, getString(R.string.label_session_not_found),
                        mExitOnce);
                return;
            }

            // Display sharing infos
            TextView fromTextView = (TextView) findViewById(R.id.from);
            String from = RcsDisplayName.getInstance(this).getDisplayName(mRemoteContact);
            fromTextView.setText(getString(R.string.label_from_args, from));

            // Display accept/reject dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.title_geoloc_sharing);
            builder.setMessage(getString(R.string.label_from_args, from));
            builder.setCancelable(false);
            builder.setIcon(R.drawable.ri_notif_gsh_icon);
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
            mGeolocSharing.acceptInvitation();
        } catch (Exception e) {
            Utils.showMessageAndExit(ReceiveGeolocSharing.this,
                    getString(R.string.label_invitation_failed), mExitOnce, e);
        }
    }

    /**
     * Reject invitation
     */
    private void rejectInvitation() {
        try {
            // Reject the invitation
            mGeolocSharing.rejectInvitation();
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
     * Show the sharing progress
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
            if (mGeolocSharing != null && GeolocSharing.State.STARTED == mGeolocSharing.getState()) {
                mGeolocSharing.abortSharing();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mGeolocSharing = null;

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
