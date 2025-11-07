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

package com.gsma.rcs.ri.extension.streaming;

import com.gsma.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.gsma.rcs.api.connection.utils.ExceptionUtil;
import com.gsma.rcs.api.connection.utils.RcsActivity;
import com.gsma.rcs.ri.R;
import com.gsma.rcs.ri.RiApplication;
import com.gsma.rcs.ri.utils.LogUtils;
import com.gsma.rcs.ri.utils.RcsContactUtil;
import com.gsma.rcs.ri.utils.RcsSessionUtil;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.extension.MultimediaSession;
import com.gsma.services.rcs.extension.MultimediaSessionService;
import com.gsma.services.rcs.extension.MultimediaStreamingSession;
import com.gsma.services.rcs.extension.MultimediaStreamingSessionIntent;
import com.gsma.services.rcs.extension.MultimediaStreamingSessionListener;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
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
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Streaming session view
 *
 * @author Jean-Marc AUFFRET
 * @author Philippe LEMORDANT
 */
public class StreamingSessionView extends RcsActivity {

    /**
     * View mode: incoming session
     */
    public final static int MODE_INCOMING = 0;

    /**
     * View mode: outgoing session
     */
    public final static int MODE_OUTGOING = 1;

    /**
     * View mode: open session history
     */
    public final static int MODE_OPEN = 2;

    /**
     * Intent parameter: view mode
     */
    public final static String EXTRA_MODE = "mode";

    /**
     * Intent parameter: session ID
     */
    public final static String EXTRA_SESSION_ID = "session_id";

    /**
     * Intent parameter: contact
     */
    public final static String EXTRA_CONTACT = "contact";

    private final Handler handler = new Handler();

    private String mSessionId;

    private ContactId mContact;

    private String mServiceId = StreamingSessionUtils.SERVICE_ID;

    private MultimediaStreamingSession mSession;

    private Dialog mProgressDialog;

    private android.view.View.OnClickListener mBtnStartStopListener;

    private OnClickListener mAcceptBtnListener;

    private OnClickListener mDeclineBtnListener;

    private MultimediaStreamingSessionListener mServiceListener;

    private boolean mStarted = false;

    private Button mStartStopBtn;

    private TextView mTxDataView;

    private TextView mRxDataView;

    private Integer mCounter = 0;

    private ScheduledExecutorService mPeriodicWorker;

    private Runnable mPeriodicRtpSendTask;

    private MultimediaSessionService mSessionService;

    private static final String LOGTAG = LogUtils
            .getTag(StreamingSessionView.class.getSimpleName());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.extension_streaming_session_view);

        /* Set buttons callback */
        mStartStopBtn = (Button) findViewById(R.id.start_stop_btn);
        mStartStopBtn.setOnClickListener(mBtnStartStopListener);
        mStartStopBtn.setEnabled(false);

        mTxDataView = (TextView) findViewById(R.id.tx_data);
        mRxDataView = (TextView) findViewById(R.id.rx_data);

        mSessionService = getMultimediaSessionApi();
        /* Register to API connection manager */
        if (mSessionService == null
                || !isServiceConnected(RcsServiceName.MULTIMEDIA, RcsServiceName.CONTACT)) {
            showMessageThenExit(R.string.label_service_not_available);
            return;
        }
        startMonitorServices(RcsServiceName.MULTIMEDIA, RcsServiceName.CONTACT);
        try {
            mSessionService.addEventListener(mServiceListener);
            initialiseStreamingSession(getIntent());
        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSessionService != null && isServiceConnected(RcsServiceName.MULTIMEDIA)) {
            try {
                mSessionService.removeEventListener(mServiceListener);
            } catch (RcsServiceException e) {
                Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
            }
        }
        if (mPeriodicWorker != null) {
            mPeriodicWorker.shutdown();
        }
    }

    private void acceptInvitation() {
        try {
            mSession.acceptInvitation();
            /*
             * Wait for the SIP-ACK to allow the user to send message once session is established.
             */
            showProgressDialog();
        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    private void rejectInvitation() {
        try {
            mSession.rejectInvitation();
        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    private void initialiseStreamingSession(Intent intent) {
        try {
            int mode = intent.getIntExtra(StreamingSessionView.EXTRA_MODE, -1);
            if (mode == StreamingSessionView.MODE_OUTGOING) {
                /* Outgoing session: Check if the service is available. */
                if (!mSessionService.isServiceRegistered()) {
                    showMessageThenExit(R.string.error_not_registered);
                    return;
                }
                mContact = intent.getParcelableExtra(StreamingSessionView.EXTRA_CONTACT);
                startSession();

            } else if (mode == StreamingSessionView.MODE_OPEN) {
                /* Open an existing session. */
                mSessionId = intent.getStringExtra(StreamingSessionView.EXTRA_SESSION_ID);
                mSession = mSessionService.getStreamingSession(mSessionId);
                if (mSession == null) {
                    showMessageThenExit(R.string.label_session_has_expired);
                    return;
                }
                mContact = mSession.getRemoteContact();

            } else {
                /* Incoming session from its Intent. */
                mSessionId = intent
                        .getStringExtra(MultimediaStreamingSessionIntent.EXTRA_SESSION_ID);

                mSession = mSessionService.getStreamingSession(mSessionId);
                if (mSession == null) {
                    showMessageThenExit(R.string.label_session_has_expired);
                    return;
                }
                mContact = mSession.getRemoteContact();
                String from = RcsContactUtil.getInstance(this).getDisplayName(mContact);

                /* Manual accept */
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.title_streaming_session);
                builder.setMessage(getString(R.string.label_mm_from_id, from, mServiceId));
                builder.setCancelable(false);
                builder.setIcon(R.drawable.ri_notif_mm_session_icon);
                builder.setPositiveButton(R.string.label_accept, mAcceptBtnListener);
                builder.setNegativeButton(R.string.label_decline, mDeclineBtnListener);
                registerDialog(builder.show());
            }

            /* Display session info */
            TextView featureTagEdit = (TextView) findViewById(R.id.feature_tag);
            featureTagEdit.setText(mServiceId);
            TextView contactEdit = (TextView) findViewById(R.id.contact);
            String from = RcsContactUtil.getInstance(this).getDisplayName(mContact);
            contactEdit.setText(from);
            Button sendBtn = (Button) findViewById(R.id.send_btn);
            if (mSession != null && MultimediaSession.State.STARTED == mSession.getState()) {
                sendBtn.setEnabled(true);
            }
        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    private void startSession() {
        try {
            mSession = mSessionService.initiateStreamingSession(mServiceId, mContact,
                    StreamingSessionUtils.ENCODING);
            mSessionId = mSession.getSessionId();
            showProgressDialog();

        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    private void showProgressDialog() {
        mProgressDialog = showProgressDialog(getString(R.string.label_command_in_progress));
        mProgressDialog.setOnCancelListener(new OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                Toast.makeText(StreamingSessionView.this,
                        getString(R.string.label_session_canceled), Toast.LENGTH_SHORT).show();
                quitSession();
            }
        });
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    private void quitSession() {
        try {
            if (mSession != null && RcsSessionUtil.isAllowedToAbortMultimediaSession(mSession)) {
                mSession.abortSession();
            }
        } catch (RcsServiceException e) {
            showException(e);

        } finally {
            mSession = null;
            /* Exit activity */
            finish();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_BACK == keyCode) {
            if (mSession != null) {
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
                                // Exit activity
                                finish();
                            }
                        });
                builder.setCancelable(true);
                registerDialog(builder.show());

            } else {
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(getApplicationContext());
        inflater.inflate(R.menu.menu_mm_session, menu);
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
        mPeriodicRtpSendTask = new Runnable() {
            public void run() {
                if (mSession == null) {
                    return;
                }
                mCounter++;
                final String data = "data".concat(mCounter.toString());
                try {
                    mSession.sendPayload(data.getBytes());
                    handler.post(new Runnable() {
                        public void run() {
                            /* Display Transmitted data */
                            mTxDataView.setText(data);
                        }
                    });
                } catch (RcsServiceException e) {
                    showExceptionThenExit(e);
                }

            }
        };
        mBtnStartStopListener = new android.view.View.OnClickListener() {

            public void onClick(View v) {
                mStarted = !mStarted;
                handler.post(new Runnable() {
                    public void run() {
                        mStartStopBtn
                                .setText(mStarted ? R.string.label_stop : R.string.label_start);
                    }
                });
                if (mStarted) {
                    mPeriodicWorker = Executors.newSingleThreadScheduledExecutor();
                    mPeriodicWorker.scheduleAtFixedRate(mPeriodicRtpSendTask, 0, 1,
                            TimeUnit.SECONDS);
                } else {
                    mPeriodicWorker.shutdown();
                    mPeriodicWorker = null;
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

        mServiceListener = new MultimediaStreamingSessionListener() {

            @Override
            public void onStateChanged(ContactId contact, String sessionId,
                    final MultimediaSession.State state, MultimediaSession.ReasonCode reasonCode) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onMultimediaStreamingStateChanged contact=" + contact
                            + " sessionId=" + sessionId + " state=" + state + " reason="
                            + reasonCode);
                }
                /* Discard event if not for current sessionId */
                if (mSessionId == null || !mSessionId.equals(sessionId)) {
                    return;
                }
                final String _reasonCode = RiApplication.sMultimediaReasonCodes[reasonCode.toInt()];
                handler.post(new Runnable() {
                    public void run() {

                        switch (state) {
                            case STARTED:
                                /* Session is established: hide progress dialog. */
                                hideProgressDialog();
                                /* Activate sen button. */
                                mStartStopBtn.setEnabled(true);
                                break;

                            case ABORTED:
                                showMessageThenExit(getString(R.string.label_session_aborted,
                                        _reasonCode));
                                break;

                            case REJECTED:
                                showMessageThenExit(getString(R.string.label_session_rejected,
                                        _reasonCode));
                                break;

                            case FAILED:
                                showMessageThenExit(getString(R.string.label_session_failed,
                                        _reasonCode));
                                break;

                            default:
                                if (LogUtils.isActive) {
                                    Log.d(LOGTAG,
                                            "onMultimediaStreamingStateChanged "
                                                    + getString(R.string.label_mms_state_changed,
                                                            RiApplication.sMultimediaStates[state
                                                                    .toInt()], _reasonCode));
                                }
                        }
                    }
                });
            }

            @Override
            public void onPayloadReceived(ContactId contact, String sessionId, final byte[] content) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onNewMessage contact=" + contact + " sessionId=" + sessionId);
                }
                if (mSessionId == null || !mSessionId.equals(sessionId)) {
                    return;
                }
                handler.post(new Runnable() {
                    public void run() {
                        /* Display received data */
                        mRxDataView.setText(new String(content));
                    }
                });
            }
        };
    }

}
