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
import com.gsma.rcs.ri.messaging.geoloc.EditGeoloc;
import com.gsma.rcs.ri.messaging.geoloc.ShowGeoloc;
import com.gsma.rcs.ri.utils.ContactListAdapter;
import com.gsma.rcs.ri.utils.ContactUtil;
import com.gsma.rcs.ri.utils.LogUtils;
import com.gsma.rcs.ri.utils.Utils;
import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharing;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharingListener;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharingService;

import android.Manifest;
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
 * Initiate geoloc sharing
 *
 * @author vfml3370
 * @author Philippe LEMORDANT
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
    private Geoloc mGeoloc;
    private GeolocSharing mGeolocSharing;
    private String mSharingId;

    private static final String LOGTAG = LogUtils.getTag(InitiateGeolocSharing.class.getName());

    /**
     * Spinner for contact selection
     */
    private Spinner mSpinner;
    private GeolocSharingListener mListener;
    private GeolocSharingService mGeolocSharingService;
    private Button mInviteBtn;
    private ProgressBar mProgressBar;
    private Button mSelectBtn;
    private TextView mStatusView;
    private Button mDialBtn;
    private TextView mPositionView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.geoloc_sharing_initiate);
        initialize();
        if (!isServiceConnected(RcsServiceName.GEOLOC_SHARING)) {
            showMessageThenExit(R.string.label_service_not_available);
            return;
        }
        startMonitorServices(RcsServiceName.GEOLOC_SHARING);
        try {
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
                mPositionView.setText(mGeoloc.toString());
                /* Enable invite button */
                mInviteBtn.setEnabled(true);
                break;
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
        if (item.getItemId() == R.id.menu_close_session) {
            quitSession();
        }
        return true;
    }

    private void initialize() {
        mGeolocSharingService = getGeolocSharingApi();

        mSpinner = (Spinner) findViewById(R.id.contact);
        ContactListAdapter adapter = ContactListAdapter.createRcsContactListAdapter(this);
        mSpinner.setAdapter(adapter);

        OnClickListener btnInviteListener = new OnClickListener() {
            public void onClick(View v) {
                // Check if the service is available
                try {
                    if (!mGeolocSharingService.isServiceRegistered()) {
                        showMessage(R.string.error_not_registered);
                        return;
                    }
                    // get selected phone number
                    ContactListAdapter adapter = (ContactListAdapter) mSpinner.getAdapter();
                    String phoneNumber = adapter.getSelectedNumber(mSpinner.getSelectedView());
                    ContactId contact = ContactUtil.formatContact(phoneNumber);
                    if (LogUtils.isActive) {
                        Log.d(LOGTAG, "share geoloc=" + mGeoloc + " contact=" + contact);
                    }
                    mGeolocSharing = mGeolocSharingService.shareGeoloc(contact, mGeoloc);
                    mSharingId = mGeolocSharing.getSharingId();
                    mSpinner.setEnabled(false);
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
                // Start a new activity to send a geolocation
                startActivityForResult(new Intent(InitiateGeolocSharing.this, EditGeoloc.class),
                        SELECT_GEOLOCATION);
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
                if (ActivityCompat.checkSelfPermission(InitiateGeolocSharing.this,
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
        mPositionView = (TextView) findViewById(R.id.position);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        updateProgressBar(0, 0);
        mPositionView.setText("");

        if (adapter == null || adapter.getCount() != 0) {
            mDialBtn.setEnabled(true);
            mSelectBtn.setEnabled(true);
        }
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
    }
}
