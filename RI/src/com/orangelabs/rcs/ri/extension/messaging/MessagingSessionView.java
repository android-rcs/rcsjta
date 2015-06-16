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

package com.orangelabs.rcs.ri.extension.messaging;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.extension.MultimediaMessagingSession;
import com.gsma.services.rcs.extension.MultimediaMessagingSessionIntent;
import com.gsma.services.rcs.extension.MultimediaMessagingSessionListener;
import com.gsma.services.rcs.extension.MultimediaSession;
import com.gsma.services.rcs.extension.MultimediaSessionService;
import com.gsma.services.rcs.extension.MultimediaSessionServiceConfiguration;

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

/**
 * Messaging session view
 * 
 * @author Jean-Marc AUFFRET
 */
public class MessagingSessionView extends Activity {
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

    private final Handler mHandler = new Handler();

    private String mSessionId;

    private ContactId mContact;

    private String mServiceId = MessagingSessionUtils.SERVICE_ID;

    private MultimediaMessagingSession mSession;

    private Dialog mProgressDialog;

    private LockAccess mExitOnce = new LockAccess();

    private ConnectionManager mCnxManager;

    private static final String LOGTAG = LogUtils
            .getTag(MessagingSessionView.class.getSimpleName());

    private MultimediaMessagingSessionListener mServiceListener = new MultimediaMessagingSessionListener() {

        @Override
        public void onStateChanged(ContactId contact, String sessionId,
                final MultimediaSession.State state, MultimediaSession.ReasonCode reasonCode) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "onStateChanged contact=" + contact + " sessionId=" + sessionId
                        + " state=" + state + " reason=" + reasonCode);
            }
            // Discard event if not for current sessionId
            if (mSessionId == null || !mSessionId.equals(sessionId)) {
                return;
            }
            final String _reasonCode = RiApplication.sMultimediaReasonCodes[reasonCode.toInt()];
            mHandler.post(new Runnable() {
                public void run() {
                    switch (state) {
                        case STARTED:
                            // Session is established: hide progress dialog
                            hideProgressDialog();
                            // Activate button
                            Button sendBtn = (Button) findViewById(R.id.send_btn);
                            sendBtn.setEnabled(true);
                            break;

                        case ABORTED:
                            // Session is aborted: hide progress dialog then
                            // exit
                            hideProgressDialog();
                            Utils.showMessageAndExit(MessagingSessionView.this,
                                    getString(R.string.label_session_aborted, _reasonCode),
                                    mExitOnce);
                            break;

                        case REJECTED:
                            // Session is rejected: hide progress dialog then
                            // exit
                            hideProgressDialog();
                            Utils.showMessageAndExit(MessagingSessionView.this,
                                    getString(R.string.label_session_rejected, _reasonCode),
                                    mExitOnce);
                            break;

                        case FAILED:
                            // Session is failed: hide progress dialog then exit
                            hideProgressDialog();
                            Utils.showMessageAndExit(MessagingSessionView.this,
                                    getString(R.string.label_session_failed, _reasonCode),
                                    mExitOnce);
                            break;

                        default:
                            if (LogUtils.isActive) {
                                Log.d(LOGTAG,
                                        "onStateChanged "
                                                + getString(R.string.label_mms_state_changed,
                                                        RiApplication.sMultimediaStates[state
                                                                .toInt()], _reasonCode));
                            }
                    }
                }
            });
        }

        @Override
        public void onMessageReceived(ContactId contact, String sessionId, byte[] content) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "onMessageReceived contact=" + contact + " sessionId=" + sessionId);
            }
            // Discard event if not for current sessionId
            if (mSessionId == null || !mSessionId.equals(sessionId)) {
                return;
            }
            final String data = new String(content);

            mHandler.post(new Runnable() {
                public void run() {
                    // Display received data
                    TextView txt = (TextView) MessagingSessionView.this
                            .findViewById(R.id.recv_data);
                    txt.setText(data);
                }
            });
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCnxManager = ConnectionManager.getInstance();

        /* Set layout */
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.extension_session_view);

        /* Set buttons callback */
        Button sendBtn = (Button) findViewById(R.id.send_btn);
        sendBtn.setOnClickListener(btnSendListener);
        sendBtn.setEnabled(false);

        /* Register to API connection manager */
        if (!mCnxManager.isServiceConnected(RcsServiceName.MULTIMEDIA, RcsServiceName.CONTACT)) {
            Utils.showMessageAndExit(this, getString(R.string.label_service_not_available),
                    mExitOnce);
            return;
        }
        mCnxManager.startMonitorServices(this, mExitOnce, RcsServiceName.MULTIMEDIA,
                RcsServiceName.CONTACT);
        try {
            /* Add service listener */
            mCnxManager.getMultimediaSessionApi().addEventListener(mServiceListener);

            initialiseMessagingSession(getIntent());
        } catch (RcsServiceException e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Failed to add listener", e);
            }
            Utils.showMessageAndExit(this, getString(R.string.label_api_failed), mExitOnce);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCnxManager.stopMonitorServices(this);
        if (mCnxManager.isServiceConnected(RcsServiceName.MULTIMEDIA)) {
            // Remove listener
            try {
                mCnxManager.getMultimediaSessionApi().removeEventListener(mServiceListener);
            } catch (Exception e) {
                if (LogUtils.isActive) {
                    Log.e(LOGTAG, "Failed to remove listener", e);
                }
            }
        }
    }

    /**
     * Accept invitation
     */
    private void acceptInvitation() {
        try {
            // Accept the invitation
            mSession.acceptInvitation();
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
            mSession.rejectInvitation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initialiseMessagingSession(Intent intent) {
        MultimediaSessionService sessionApi = mCnxManager.getMultimediaSessionApi();
        try {
            MultimediaSessionServiceConfiguration config = sessionApi.getConfiguration();
            if (LogUtils.isActive) {
                Log.d(LOGTAG,
                        "MessageMaxLength: ".concat(Integer.toString(config.getMessageMaxLength())));
            }
            int mode = intent.getIntExtra(MessagingSessionView.EXTRA_MODE, -1);
            if (mode == MessagingSessionView.MODE_OUTGOING) {
                // Outgoing session

                // Check if the service is available
                if (!sessionApi.isServiceRegistered()) {
                    Utils.showMessageAndExit(this, getString(R.string.label_service_not_available),
                            mExitOnce);
                    return;
                }

                // Get remote contact
                mContact = intent.getParcelableExtra(MessagingSessionView.EXTRA_CONTACT);

                // Initiate session
                startSession();
            } else if (mode == MessagingSessionView.MODE_OPEN) {
                // Open an existing session

                // Incoming session
                mSessionId = intent.getStringExtra(MessagingSessionView.EXTRA_SESSION_ID);

                // Get the session
                mSession = sessionApi.getMessagingSession(mSessionId);
                if (mSession == null) {
                    // Session not found or expired
                    Utils.showMessageAndExit(this, getString(R.string.label_session_has_expired),
                            mExitOnce);
                    return;
                }

                // Get remote contact
                mContact = mSession.getRemoteContact();
            } else {
                // Incoming session from its Intent
                mSessionId = intent
                        .getStringExtra(MultimediaMessagingSessionIntent.EXTRA_SESSION_ID);

                // Get the session
                mSession = sessionApi.getMessagingSession(mSessionId);
                if (mSession == null) {
                    // Session not found or expired
                    Utils.showMessageAndExit(this, getString(R.string.label_session_has_expired),
                            mExitOnce);
                    return;
                }

                // Get remote contact
                mContact = mSession.getRemoteContact();
                String from = RcsDisplayName.getInstance(this).getDisplayName(mContact);

                // Manual accept
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.title_messaging_session);
                builder.setMessage(getString(R.string.label_mm_from_id, from, mServiceId));
                builder.setCancelable(false);
                builder.setIcon(R.drawable.ri_notif_mm_session_icon);
                builder.setPositiveButton(getString(R.string.label_accept), acceptBtnListener);
                builder.setNegativeButton(getString(R.string.label_decline), declineBtnListener);
                builder.show();
            }
            // Display session info
            TextView featureTagEdit = (TextView) findViewById(R.id.feature_tag);
            featureTagEdit.setText(mServiceId);
            String from = RcsDisplayName.getInstance(this).getDisplayName(mContact);
            TextView contactEdit = (TextView) findViewById(R.id.contact);
            contactEdit.setText(from);
            Button sendBtn = (Button) findViewById(R.id.send_btn);
            if (mSession != null) {
                sendBtn.setEnabled(true);
            } else {
                sendBtn.setEnabled(false);
            }

        } catch (RcsServiceException e) {
            Utils.showMessageAndExit(this, getString(R.string.label_api_failed), mExitOnce, e);
        }
    }

    /**
     * Start session
     */
    private void startSession() {
        // Initiate the chat session in background
        try {
            // Initiate session
            mSession = mCnxManager.getMultimediaSessionApi().initiateMessagingSession(mServiceId,
                    mContact);
            mSessionId = mSession.getSessionId();
        } catch (Exception e) {
            Utils.showMessageAndExit(this, getString(R.string.label_invitation_failed), mExitOnce,
                    e);
            return;
        }

        // Display a progress dialog
        mProgressDialog = Utils.showProgressDialog(MessagingSessionView.this,
                getString(R.string.label_command_in_progress));
        mProgressDialog.setOnCancelListener(new OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                Toast.makeText(MessagingSessionView.this,
                        getString(R.string.label_session_canceled), Toast.LENGTH_SHORT).show();
                quitSession();
            }
        });
    }

    /**
     * Hide progress dialog
     */
    public void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
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
     * Quit the session
     */
    private void quitSession() {
        // Stop session
        if (mSession != null) {
            try {
                mSession.abortSession();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mSession = null;
        }

        // Exit activity
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (mSession != null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(getString(R.string.label_confirm_close));
                    builder.setPositiveButton(getString(R.string.label_ok),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // Quit the session
                                    quitSession();
                                }
                            });
                    builder.setNegativeButton(getString(R.string.label_cancel),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // Exit activity
                                    finish();
                                }
                            });
                    builder.setCancelable(true);
                    builder.show();
                } else {
                    // Exit activity
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
        switch (item.getItemId()) {
            case R.id.menu_close_session:
                // Quit the session
                quitSession();
                break;
        }
        return true;
    }

    /**
     * Send button callback
     */
    private android.view.View.OnClickListener btnSendListener = new android.view.View.OnClickListener() {
        private int i = 0;

        public void onClick(View v) {
            try {
                String data = "data" + i++;
                mSession.sendMessage(data.getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
}
