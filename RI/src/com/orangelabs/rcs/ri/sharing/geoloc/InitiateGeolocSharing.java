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
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharing;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharingListener;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharingService;

import com.orangelabs.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.api.connection.utils.ExceptionUtil;
import com.orangelabs.rcs.api.connection.utils.RcsActivity;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.messaging.geoloc.EditGeoloc;
import com.orangelabs.rcs.ri.messaging.geoloc.ShowGeoloc;
import com.orangelabs.rcs.ri.utils.ContactListAdapter;
import com.orangelabs.rcs.ri.utils.ContactUtil;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

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
 * Initiate geoloc sharing
 * 
 * @author vfml3370
 * @author yplo6403
 */
public class InitiateGeolocSharing extends RcsActivity {
    /**
     * Activity result constants
     */
    private final static int SELECT_GEOLOCATION = 0;

    /**
     * UI handler
     */
    private final Handler mHandler = new Handler();

    private Dialog mProgressDialog;

    private Geoloc mGeoloc;

    private GeolocSharing mGeolocSharing;

    private String mSharingId;

    private static final String LOGTAG = LogUtils.getTag(InitiateGeolocSharing.class
            .getSimpleName());

    /**
     * Spinner for contact selection
     */
    private Spinner mSpinner;

    private GeolocSharingListener mListener;

    private OnClickListener mBtnInviteListener;

    private OnClickListener mBtnSelectListener;

    private OnClickListener mBtnDialListener;

    private GeolocSharingService mGeolocSharingService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.geoloc_sharing_initiate);

        /* Set contact selector */
        mSpinner = (Spinner) findViewById(R.id.contact);
        mSpinner.setAdapter(ContactListAdapter.createRcsContactListAdapter(this));

        /* Set buttons callback */
        Button inviteBtn = (Button) findViewById(R.id.invite_btn);
        inviteBtn.setOnClickListener(mBtnInviteListener);
        inviteBtn.setEnabled(false);
        Button selectBtn = (Button) findViewById(R.id.select_btn);
        selectBtn.setOnClickListener(mBtnSelectListener);
        selectBtn.setEnabled(false);
        Button dialBtn = (Button) findViewById(R.id.dial_btn);
        dialBtn.setOnClickListener(mBtnDialListener);
        dialBtn.setEnabled(false);

        /* Disable button if no contact available */
        if (mSpinner.getAdapter().getCount() != 0) {
            dialBtn.setEnabled(true);
            selectBtn.setEnabled(true);
        }
        if (!isServiceConnected(RcsServiceName.GEOLOC_SHARING)) {
            showMessageThenExit(R.string.label_service_not_available);
            return;
        }
        startMonitorServices(RcsServiceName.GEOLOC_SHARING);
        try {
            mGeolocSharingService = getGeolocSharingApi();
            mGeolocSharingService.addEventListener(mListener);

        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mGeolocSharingService != null && isServiceConnected(RcsServiceName.GEOLOC_SHARING)) {
            try {
                mGeolocSharingService.removeEventListener(mListener);
            } catch (RcsServiceException e) {
                Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case SELECT_GEOLOCATION:
                /* Get selected geoloc */
                mGeoloc = data.getParcelableExtra(EditGeoloc.EXTRA_GEOLOC);
                /* Enable invite button */
                Button inviteBtn = (Button) findViewById(R.id.invite_btn);
                inviteBtn.setEnabled(true);
                break;
        }
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
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
        inflater.inflate(R.menu.menu_geoloc_sharing, menu);
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
        mListener = new GeolocSharingListener() {

            @Override
            public void onProgressUpdate(ContactId contact, String sharingId,
                    final long currentSize, final long totalSize) {
                /* Discard event if not for current sharingId */
                if (InitiateGeolocSharing.this.mSharingId == null
                        || !InitiateGeolocSharing.this.mSharingId.equals(sharingId)) {
                    return;
                }
                mHandler.post(new Runnable() {
                    public void run() {
                        updateProgressBar(currentSize, totalSize);
                    }
                });
            }

            @Override
            public void onStateChanged(final ContactId contact, String sharingId,
                    final GeolocSharing.State state, GeolocSharing.ReasonCode reasonCode) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onStateChanged contact=" + contact + " sharingId=" + sharingId
                            + " state=" + state + " reason=" + reasonCode);
                }
                /* Discard event if not for current sharingId */
                if (InitiateGeolocSharing.this.mSharingId == null
                        || !InitiateGeolocSharing.this.mSharingId.equals(sharingId)) {
                    return;
                }
                final String _state = RiApplication.sGeolocSharingStates[state.toInt()];
                final String _reasonCode = RiApplication.sGeolocReasonCodes[reasonCode.toInt()];
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
                                hideProgressDialog();
                                /* Display transfer progress */
                                statusView.setText(_state);
                                /* Make sure progress bar is at the end */
                                ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
                                progressBar.setProgress(progressBar.getMax());

                                ShowGeoloc.ShowGeolocForContact(InitiateGeolocSharing.this,
                                        contact, mGeoloc);
                                break;

                            default:
                                statusView.setText(getString(R.string.label_gsh_state_changed,
                                        _state, _reasonCode));
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

        mBtnInviteListener = new OnClickListener() {
            public void onClick(View v) {
                // Check if the service is available
                try {
                    if (!mGeolocSharingService.isServiceRegistered()) {
                        showMessage(R.string.error_not_registered);
                        return;
                    }
                } catch (RcsServiceException e) {
                    showExceptionThenExit(e);
                    return;
                }

                // get selected phone number
                ContactListAdapter adapter = (ContactListAdapter) mSpinner.getAdapter();
                String phoneNumber = adapter.getSelectedNumber(mSpinner.getSelectedView());

                ContactId contact = ContactUtil.formatContact(phoneNumber);

                try {
                    // Initiate location share
                    mGeolocSharing = mGeolocSharingService.shareGeoloc(contact, mGeoloc);
                    mSharingId = mGeolocSharing.getSharingId();

                } catch (RcsServiceException e) {
                    showExceptionThenExit(e);
                }

                // Display a progress dialog
                mProgressDialog = showProgressDialog(getString(R.string.label_command_in_progress));
                mProgressDialog.setOnCancelListener(new OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        Toast.makeText(InitiateGeolocSharing.this,
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
            }
        };

        mBtnSelectListener = new OnClickListener() {
            public void onClick(View v) {
                // Start a new activity to send a geolocation
                startActivityForResult(new Intent(InitiateGeolocSharing.this, EditGeoloc.class),
                        SELECT_GEOLOCATION);
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
    }
}
