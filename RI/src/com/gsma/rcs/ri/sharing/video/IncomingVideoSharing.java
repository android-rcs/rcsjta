/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License ats
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.ri.sharing.video;

import com.gsma.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.gsma.rcs.api.connection.utils.ExceptionUtil;
import com.gsma.rcs.api.connection.utils.RcsActivity;
import com.gsma.rcs.ri.R;
import com.gsma.rcs.ri.RiApplication;
import com.gsma.rcs.ri.sharing.video.media.TerminatingVideoPlayer;
import com.gsma.rcs.ri.sharing.video.media.VideoPlayerListener;
import com.gsma.rcs.ri.sharing.video.media.VideoSurfaceView;
import com.gsma.rcs.ri.utils.LogUtils;
import com.gsma.rcs.ri.utils.RcsContactUtil;
import com.gsma.rcs.ri.utils.RcsSessionUtil;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.video.VideoDescriptor;
import com.gsma.services.rcs.sharing.video.VideoSharing;
import com.gsma.services.rcs.sharing.video.VideoSharingListener;
import com.gsma.services.rcs.sharing.video.VideoSharingService;

import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.H264Config;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Set;

/**
 * Receive video sharing
 * 
 * @author Jean-Marc AUFFRET
 * @author Philippe LEMORDANT
 */
public class IncomingVideoSharing extends RcsActivity implements VideoPlayerListener {

    /**
     * UI handler
     */
    private final Handler handler = new Handler();

    private VideoSharing mVideoSharing;

    /**
     * The Video Sharing Data Object
     */
    private VideoSharingDAO mVshDao;

    /**
     * Video renderer<br>
     * Note: this field is intentionally static
     */
    private static TerminatingVideoPlayer mVideoRenderer;

    /**
     * Video width
     */
    private int mVideoWidth = H264Config.QCIF_WIDTH;

    /**
     * Video height
     */
    private int mVideoHeight = H264Config.QCIF_HEIGHT;

    /**
     * Live video preview
     */
    private VideoSurfaceView mVideoView;

    private static final String SAVE_VIDEO_SHARING_DAO = "videoSharingDao";

    private static final String SAVE_WAIT_USER_ACCEPT = "waitUserAccept";

    private boolean mWaitForUseAcceptance = true;

    private AlertDialog mAcceptDeclineDialog;

    private OnClickListener mAcceptBtnListener;

    private OnClickListener mDeclineBtnListener;

    private VideoSharingListener mVshListener;

    private static final String LOGTAG = LogUtils
            .getTag(IncomingVideoSharing.class.getSimpleName());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set layout
        setContentView(R.layout.video_sharing_incoming);

        initialize();
        // Always on window
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        // Saved datas
        if (savedInstanceState == null) {
            // Get invitation info
            mVshDao = getIntent().getExtras().getParcelable(
                    VideoSharingIntentService.BUNDLE_VSHDAO_ID);
        } else {
            mVshDao = savedInstanceState.getParcelable(SAVE_VIDEO_SHARING_DAO);
            mWaitForUseAcceptance = savedInstanceState.getBoolean(SAVE_WAIT_USER_ACCEPT);
        }
        if (mVshDao == null) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "onCreate cannot read Video Sharing invitation");
            }
            finish();
            return;
        }
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onCreate ".concat(mVshDao.toString()));
        }
        // Create the live video view
        mVideoView = (VideoSurfaceView) findViewById(R.id.video_view);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mVideoView.setAspectRatio(mVideoWidth, mVideoHeight);
        } else {
            mVideoView.setAspectRatio(mVideoHeight, mVideoWidth);
        }
        SurfaceHolder surface = mVideoView.getHolder();
        surface.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surface.setKeepScreenOn(true);

        if (mVideoRenderer == null) {
            // Instantiate the renderer
            mVideoRenderer = new TerminatingVideoPlayer(mVideoView, this);
        } else {
            mVideoRenderer.setSurface(mVideoView);
        }

        // Register to API connection manager
        if (!isServiceConnected(RcsServiceName.VIDEO_SHARING, RcsServiceName.CONTACT)) {
            showMessageThenExit(R.string.label_service_not_available);
        } else {
            startMonitorServices(RcsServiceName.VIDEO_SHARING, RcsServiceName.CONTACT);
            startOrRestartVideoSharing();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAcceptDeclineDialog != null) {
            mAcceptDeclineDialog.cancel();
            mAcceptDeclineDialog = null;
        }
        if (isFinishing()) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "onDestroy reset video renderer");
            }
            mVideoRenderer = null;
        }
        if (isServiceConnected(RcsServiceName.VIDEO_SHARING)) {
            // Remove video sharing listener
            try {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onDestroy Remove listener");
                }
                getVideoSharingApi().removeEventListener(mVshListener);

            } catch (RcsServiceException e) {
                Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(SAVE_VIDEO_SHARING_DAO, mVshDao);
        outState.putBoolean(SAVE_WAIT_USER_ACCEPT, mWaitForUseAcceptance);
    }

    private void startOrRestartVideoSharing() {
        VideoSharingService vshApi = getVideoSharingApi();
        try {
            mVideoSharing = vshApi.getVideoSharing(mVshDao.getSharingId());
            if (mVideoSharing == null) {
                // Session not found or expired
                showMessageThenExit(R.string.label_session_not_found);
                return;

            }
            vshApi.addEventListener(mVshListener);

            ContactId remote = mVshDao.getContact();
            // Display sharing information
            String from = RcsContactUtil.getInstance(this).getDisplayName(remote);
            TextView fromTextView = (TextView) findViewById(R.id.contact);
            fromTextView.setText(from);

            if (mWaitForUseAcceptance) {
                showReceiveNotification(from);
            } else {
                displayVideoFormat();
            }

        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    private void showReceiveNotification(String from) {
        // User alert
        // Display accept/reject dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_video_sharing);
        builder.setMessage(getString(R.string.label_from_args, from));
        builder.setCancelable(false);
        builder.setIcon(R.drawable.ri_notif_csh_icon);
        builder.setPositiveButton(R.string.label_accept, mAcceptBtnListener);
        builder.setNegativeButton(R.string.label_decline, mDeclineBtnListener);
        mAcceptDeclineDialog = builder.show();
        registerDialog(mAcceptDeclineDialog);
    }

    private void acceptInvitation() {
        try {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "acceptInvitation");
            }
            mVideoSharing.acceptInvitation(mVideoRenderer);

        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    private void rejectInvitation() {
        try {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "rejectInvitation");
            }
            mVideoSharing.rejectInvitation();

        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    private void quitSession() {
        try {
            if (mVideoSharing != null && VideoSharing.State.STARTED == mVideoSharing.getState()) {
                mVideoSharing.abortSharing();
            }
        } catch (RcsServiceException e) {
            showException(e);

        } finally {
            mVideoSharing = null;
            // Exit activity
            finish();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_BACK == keyCode) {
            try {
                if (mVideoSharing == null
                        || !RcsSessionUtil.isAllowedToAbortVideoSharingSession(mVideoSharing)) {
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
        inflater.inflate(R.menu.menu_video_sharing, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_close_session) {
            quitSession();
        }
        return true;
    }

    private void displayVideoFormat() {
        try {
            VideoDescriptor videoDescriptor = mVideoSharing.getVideoDescriptor();
            String format = mVideoSharing.getVideoEncoding() + " " + videoDescriptor.getWidth()
                    + "x" + videoDescriptor.getHeight();
            TextView fmtView = (TextView) findViewById(R.id.video_format);
            fmtView.setVisibility(View.VISIBLE);
            fmtView.setText(format);

        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    /*-------------------------- Video player callbacks ------------------*/

    @Override
    public void onPlayerOpened() {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onPlayerOpened");
        }
    }

    @Override
    public void onPlayerStarted() {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onPlayerStarted");
        }
    }

    @Override
    public void onPlayerStopped() {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onPlayerStopped");
        }
    }

    @Override
    public void onPlayerClosed() {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onPlayerClosed");
        }
    }

    @Override
    public void onPlayerError() {
        // TODO
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onPlayerError");
        }
    }

    @Override
    public void onPlayerResized(int width, int height) {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onPlayerResized " + width + "x" + height);
        }
        mVideoView.setAspectRatio(width, height);

        LinearLayout l = (LinearLayout) mVideoView.getParent();
        l.setLayoutParams(new FrameLayout.LayoutParams(width, height));
    }

    private void initialize() {
        mAcceptBtnListener = new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                mAcceptDeclineDialog = null;
                mWaitForUseAcceptance = false;
                acceptInvitation();
            }
        };

        mDeclineBtnListener = new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                mAcceptDeclineDialog = null;
                mWaitForUseAcceptance = false;
                rejectInvitation();
                // Exit activity
                finish();
            }
        };

        mVshListener = new VideoSharingListener() {

            @Override
            public void onStateChanged(ContactId contact, String sharingId,
                    final VideoSharing.State state, VideoSharing.ReasonCode reasonCode) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onStateChanged contact=" + contact + " sharingId=" + sharingId
                            + " state=" + state + " reason=" + reasonCode);
                }
                // Discard event if not for current sharingId
                if (mVshDao == null || !mVshDao.getSharingId().equals(sharingId)) {
                    return;

                }
                final String _reasonCode = RiApplication.sVideoReasonCodes[reasonCode.toInt()];
                handler.post(new Runnable() {
                    public void run() {
                        switch (state) {
                            case STARTED:
                                displayVideoFormat();

                                // Start the renderer
                                mVideoRenderer.open();
                                mVideoRenderer.start();
                                break;

                            case ABORTED:
                                // Stop the renderer
                                mVideoRenderer.stop();
                                mVideoRenderer.close();

                                // Display session status
                                showMessageThenExit(getString(R.string.label_sharing_aborted,
                                        _reasonCode));
                                break;

                            case FAILED:
                                // Stop the renderer
                                mVideoRenderer.stop();
                                mVideoRenderer.close();

                                // Session is failed: exit
                                showMessageThenExit(getString(R.string.label_sharing_failed,
                                        _reasonCode));
                                break;

                            case REJECTED:
                                // Stop the renderer
                                mVideoRenderer.stop();
                                mVideoRenderer.close();

                                // Session is rejected: exit
                                showMessageThenExit(getString(R.string.label_sharing_rejected,
                                        _reasonCode));
                                break;

                            default:
                                if (LogUtils.isActive) {
                                    Log.d(LOGTAG, "onStateChanged ".concat(getString(
                                            R.string.label_vsh_state_changed,
                                            RiApplication.sVideoSharingStates[state.toInt()],
                                            _reasonCode)));
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
