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

package com.gsma.rcs.ri.sharing.geoloc;

import com.gsma.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.gsma.rcs.api.connection.utils.ExceptionUtil;
import com.gsma.rcs.api.connection.utils.RcsActivity;
import com.gsma.rcs.ri.R;
import com.gsma.rcs.ri.RiApplication;
import com.gsma.rcs.ri.messaging.geoloc.ShowGeoloc;
import com.gsma.rcs.ri.utils.LogUtils;
import com.gsma.rcs.ri.utils.RcsContactUtil;
import com.gsma.rcs.ri.utils.Utils;
import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharing;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharingIntent;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharingListener;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharingService;

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
 * Receive geoloc sharing
 * 
 * @author vfml3370
 * @author yplo6403
 */
public class ReceiveGeolocSharing extends RcsActivity {
    /**
     * UI handler
     */
    private final Handler mHandler = new Handler();
    private String mSharingId;
    private ContactId mRemoteContact;

    /**
     * Geoloc sharing session
     */
    private GeolocSharing mGeolocSharing;
    private Geoloc mGeoloc;

    private static final String LOGTAG = LogUtils.getTag(ReceiveGeolocSharing.class.getName());

    private GeolocSharingListener mListener;
    private OnClickListener mAcceptBtnListener;
    private OnClickListener mDeclineBtnListener;
    private ProgressBar mProgressBar;
    private boolean mSessionListenerSet;
    private TextView mStatusView;
    private TextView mPositionView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.geoloc_sharing_receive);

        initialize();

        /* Register to API connection manager */
        if (!isServiceConnected(RcsServiceName.GEOLOC_SHARING, RcsServiceName.CONTACT)) {
            showMessageThenExit(R.string.label_service_not_available);
            return;
        }
        startMonitorServices(RcsServiceName.GEOLOC_SHARING, RcsServiceName.CONTACT);
        processIntent(getIntent());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!mSessionListenerSet || !isServiceConnected(RcsServiceName.GEOLOC_SHARING)) {
            return;
        }
        try {
            getGeolocSharingApi().removeEventListener(mListener);
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
        mSharingId = invitation.getStringExtra(GeolocSharingIntent.EXTRA_SHARING_ID);
        mRemoteContact = invitation.getParcelableExtra(GeolocSharingIntentService.BUNDLE_GSH_ID);
        initiateGeolocSharing();
    }

    private void initiateGeolocSharing() {
        GeolocSharingService gshApi = getGeolocSharingApi();
        try {
            // Get the geoloc sharing
            mGeolocSharing = gshApi.getGeolocSharing(mSharingId);
            if (mGeolocSharing == null) {
                // Session not found or expired
                showMessageThenExit(R.string.label_session_not_found);
                return;
            }
            mSessionListenerSet = true;
            gshApi.addEventListener(mListener);

            /* Display sharing infos */
            String from = RcsContactUtil.getInstance(this).getDisplayName(mRemoteContact);
            TextView fromTextView = (TextView) findViewById(R.id.contact);
            fromTextView.setText(from);
            mPositionView = (TextView) findViewById(R.id.position);
            mProgressBar.setProgress(0);
            mPositionView.setText("");

            /* Display accept/reject dialog */
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.title_geoloc_sharing);
            builder.setMessage(getString(R.string.label_from_args, from));
            builder.setCancelable(false);
            builder.setIcon(R.drawable.ri_notif_gsh_icon);
            builder.setPositiveButton(R.string.label_accept, mAcceptBtnListener);
            builder.setNegativeButton(R.string.label_decline, mDeclineBtnListener);
            registerDialog(builder.show());

        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    private void acceptInvitation() {
        try {
            mGeolocSharing.acceptInvitation();
        } catch (RcsGenericException e) {
            showExceptionThenExit(e);
        }
    }

    private void rejectInvitation() {
        try {
            mGeolocSharing.rejectInvitation();
        } catch (RcsGenericException e) {
            showExceptionThenExit(e);
        }
    }

    private void updateProgressBar(long currentSize, long totalSize) {
        mStatusView.setText(Utils.getProgressLabel(currentSize, totalSize));
        double position = ((double) currentSize / (double) totalSize) * 100.0d;
        mProgressBar.setProgress((int) position);
    }

    private void quitSession() {
        try {
            if (mGeolocSharing != null && GeolocSharing.State.STARTED == mGeolocSharing.getState()) {
                mGeolocSharing.abortSharing();
            }
        } catch (RcsServiceException e) {
            showException(e);
        } finally {
            mGeolocSharing = null;
            /* Exit activity */
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
        mStatusView = (TextView) findViewById(R.id.progress_status);
        mListener = new GeolocSharingListener() {

            @Override
            public void onProgressUpdate(ContactId contact, String sharingId,
                    final long currentSize, final long totalSize) {
                /* Discard event if not for current sharingId */
                if (mSharingId == null || !mSharingId.equals(sharingId)) {
                    return;
                }
                mHandler.post(new Runnable() {
                    public void run() {
                        updateProgressBar(currentSize, totalSize);
                    }
                });
            }

            @Override
            public void onStateChanged(final ContactId contact, final String sharingId,
                    final GeolocSharing.State state, GeolocSharing.ReasonCode reasonCode) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onStateChanged contact=" + contact.toString() + " sharingId="
                            + sharingId + " state=" + state + " reason=" + reasonCode);
                }
                /* Discard event if not for current sharingId */
                if (mSharingId == null || !mSharingId.equals(sharingId)) {
                    return;
                }
                final String _reasonCode = RiApplication.sGeolocReasonCodes[reasonCode.toInt()];
                final String _state = RiApplication.sGeolocSharingStates[state.toInt()];
                mHandler.post(new Runnable() {
                    public void run() {
                        switch (state) {
                            case ABORTED:
                                String msg = getString(R.string.label_sharing_aborted, _reasonCode);
                                mStatusView.setText(msg);
                                showMessageThenExit(msg);
                                break;

                            case FAILED:
                                msg = getString(R.string.label_sharing_failed, _reasonCode);
                                mStatusView.setText(msg);
                                showMessageThenExit(msg);
                                break;

                            case TRANSFERRED:
                                /* Display transfer progress */
                                mStatusView.setText(_state);
                                /* Make sure progress bar is at the end */
                                mProgressBar.setProgress(mProgressBar.getMax());

                                /* Show the shared geoloc */
                                try {
                                    mGeoloc = mGeolocSharing.getGeoloc();
                                    mPositionView.setText(mGeoloc.toString());
                                    ShowGeoloc.ShowGeolocForContact(ReceiveGeolocSharing.this,
                                            contact, mGeoloc);
                                } catch (RcsServiceException e) {
                                    if (LogUtils.isActive) {
                                        Log.d(LOGTAG, "onStateChanged failed to get geoloc for "
                                                .concat(sharingId));
                                    }
                                }
                                break;

                            default:
                                mStatusView.setText(_state);
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
                finish();
            }
        };
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
    }
}
