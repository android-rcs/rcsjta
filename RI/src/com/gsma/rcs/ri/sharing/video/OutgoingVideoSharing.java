/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010-2016 Orange.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
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
import com.gsma.rcs.ri.sharing.video.media.OriginatingVideoPlayer;
import com.gsma.rcs.ri.sharing.video.media.VideoPlayerListener;
import com.gsma.rcs.ri.sharing.video.media.VideoSurfaceView;
import com.gsma.rcs.ri.utils.ContactListAdapter;
import com.gsma.rcs.ri.utils.ContactUtil;
import com.gsma.rcs.ri.utils.LogUtils;
import com.gsma.rcs.ri.utils.RcsContactUtil;
import com.gsma.rcs.ri.utils.RcsSessionUtil;
import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsPersistentStorageException;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.video.VideoDescriptor;
import com.gsma.services.rcs.sharing.video.VideoSharing;
import com.gsma.services.rcs.sharing.video.VideoSharingListener;
import com.gsma.services.rcs.sharing.video.VideoSharingService;

import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.H264Config;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.CameraOptions;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.Orientation;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

/**
 * Initiate video sharing.
 *
 * @author Jean-Marc AUFFRET
 * @author Philippe LEMORDANT
 */
public class OutgoingVideoSharing extends RcsActivity implements VideoPlayerListener,
        SurfaceHolder.Callback {

    /**
     * UI handler
     */
    private final Handler handler = new Handler();

    /**
     * Video sharing
     */
    private VideoSharing mVideoSharing;

    /**
     * Video sharing Id
     */
    private String mSharingId;

    /**
     * Video player<br>
     * Note: this field is intentionally static
     */
    private static OriginatingVideoPlayer mVideoPlayer;

    /**
     * Camera of the device
     */
    private Camera mCamera;

    /**
     * Opened camera id
     */
    private CameraOptions mOpenedCameraId = CameraOptions.FRONT;

    /**
     * Camera preview started flag
     */
    private boolean mCameraPreviewRunning = false;

    /**
     * Video width
     */
    private int mVideoWidth = H264Config.QCIF_WIDTH;

    /**
     * Video height
     */
    private int mVideoHeight = H264Config.QCIF_HEIGHT;

    /**
     * Number of cameras
     */
    private int mNbfCameras = 1;

    /**
     * Live video preview
     */
    private VideoSurfaceView mVideoView;

    /**
     * Video surface holder
     */
    private SurfaceHolder mSurface;

    /**
     * Spinner for contact selection
     */
    private Spinner mSpinner;

    private Button mInviteBtn;

    private Button mDialBtn;

    private Button mSwitchCamBtn;

    private ContactId mContact;

    /**
     * Session is started and video format is then negotiated.
     */
    private boolean mStarted = false;

    /**
     * Preview surface view is created
     */
    private boolean mIsSurfaceCreated;

    private static final String LOGTAG = LogUtils
            .getTag(OutgoingVideoSharing.class.getSimpleName());

    private static final String SAVE_SHARING_ID = "sharingId";

    private static final String SAVE_VIDEO_HEIGHT = "videoHeight";

    private static final String SAVE_VIDEO_WIDTH = "videoWidth";

    private static final String SAVE_NB_OF_CAMERAS = "numberOfCameras";

    private static final String SAVE_OPENED_CAMERA_ID = "openedCameraId";

    /**
     * We save the remote contact into the activity bundle.<br>
     * This information could also be retrieved from session instance.
     */
    private static final String SAVE_REMOTE_CONTACT = "remoteContact";

    /**
     * We save this information into the activity bundle.<br>
     * This information could also be retrieved from session instance state.
     */
    private static final String SAVE_STARTED = "started";

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Always on window
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        // Set layout
        setContentView(R.layout.video_sharing_outgoing);

        // Set the contact selector
        mSpinner = (Spinner) findViewById(R.id.contact);
        ContactListAdapter adapter = ContactListAdapter.createRcsContactListAdapter(this);
        mSpinner.setAdapter(adapter);

        // Saved datas
        if (savedInstanceState == null) {
            mNbfCameras = getNumberOfCameras();
        } else {
            mSharingId = savedInstanceState.getString(SAVE_SHARING_ID);
            mNbfCameras = savedInstanceState.getInt(SAVE_NB_OF_CAMERAS);
            mVideoHeight = savedInstanceState.getInt(SAVE_VIDEO_HEIGHT);
            mVideoWidth = savedInstanceState.getInt(SAVE_VIDEO_WIDTH);
            mOpenedCameraId = CameraOptions.convert(savedInstanceState
                    .getInt(SAVE_OPENED_CAMERA_ID));
            mContact = savedInstanceState.getParcelable(SAVE_REMOTE_CONTACT);
            mStarted = savedInstanceState.getBoolean(SAVE_STARTED);
        }
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "Sharing ID " + mSharingId + " Nb of cameras=" + mNbfCameras
                    + " active camera=" + mOpenedCameraId);
        }
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "Resolution: " + mVideoWidth + "x" + mVideoHeight);
        }

        // Set button callback
        mInviteBtn = (Button) findViewById(R.id.invite_btn);
        mInviteBtn.setOnClickListener(btnInviteListener);
        mDialBtn = (Button) findViewById(R.id.dial_btn);
        mDialBtn.setOnClickListener(btnDialListener);
        mSwitchCamBtn = (Button) findViewById(R.id.switch_cam_btn);

        // Disable button if no contact available
        if (adapter == null || adapter.getCount() == 0) {
            mDialBtn.setEnabled(false);
            mInviteBtn.setEnabled(false);
        }

        // Get camera info
        if (mNbfCameras > 1) {
            boolean backAvailable = checkCameraSize(CameraOptions.BACK);
            boolean frontAvailable = checkCameraSize(CameraOptions.FRONT);
            if (frontAvailable && backAvailable) {
                mSwitchCamBtn.setOnClickListener(btnSwitchCamListener);
            } else if (frontAvailable) {
                mOpenedCameraId = CameraOptions.FRONT;
                mSwitchCamBtn.setVisibility(View.INVISIBLE);
            } else if (backAvailable) {
                mOpenedCameraId = CameraOptions.BACK;
                mSwitchCamBtn.setVisibility(View.INVISIBLE);
            } else {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "No camera available for encoding");
                }
            }
        } else {
            if (checkCameraSize(CameraOptions.FRONT)) {
                mSwitchCamBtn.setVisibility(View.INVISIBLE);
            } else {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "No camera available for encoding");
                }
            }
        }

        // Create the live video view
        mVideoView = (VideoSurfaceView) findViewById(R.id.video_preview);
        mVideoView.setAspectRatio(mVideoWidth, mVideoHeight);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mVideoView.setAspectRatio(mVideoWidth, mVideoHeight);

        } else {
            mVideoView.setAspectRatio(mVideoHeight, mVideoWidth);
        }

        mSurface = mVideoView.getHolder();
        mSurface.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurface.setKeepScreenOn(true);
        mSurface.addCallback(this);

        // Check if session in progress
        if (mSharingId != null) {
            // Sharing in progress
            mDialBtn.setVisibility(View.GONE);
            mInviteBtn.setVisibility(View.GONE);
            mSpinner.setVisibility(View.GONE);
            mSwitchCamBtn.setEnabled((mNbfCameras > 1));
            handler.post(continueOutgoingSessionRunnable);
            displayRemoteContact();
        } else {
            // Sharing not yet initiated
            mDialBtn.setVisibility(View.VISIBLE);
            mInviteBtn.setVisibility(View.VISIBLE);
            mSwitchCamBtn.setEnabled(false);

            boolean canInitiate = true;
            if (adapter == null || adapter.getCount() == 0) {
                canInitiate = false;
            }
            mDialBtn.setEnabled(canInitiate);
            mInviteBtn.setEnabled(canInitiate);
        }

        // Register to API connection manager
        if (!isServiceConnected(RcsServiceName.VIDEO_SHARING)) {
            showMessageThenExit(R.string.label_service_not_available);
            return;
        }
        startMonitorServices(RcsServiceName.VIDEO_SHARING);

        // Add service listener
        try {
            VideoSharingService vshService = getVideoSharingApi();
            if (mSharingId != null) {
                // Sharing is in progress: get sharing session
                mVideoSharing = vshService.getVideoSharing(mSharingId);
                if (mStarted) {
                    displayVideoFormat();
                }
            }
            vshService.addEventListener(vshListener);
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "onCreate video sharing");
            }
        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SAVE_SHARING_ID, mSharingId);
        outState.putInt(SAVE_VIDEO_HEIGHT, mVideoHeight);
        outState.putInt(SAVE_VIDEO_WIDTH, mVideoWidth);
        outState.putInt(SAVE_NB_OF_CAMERAS, mNbfCameras);
        outState.putInt(SAVE_OPENED_CAMERA_ID, mOpenedCameraId.getValue());
        outState.putParcelable(SAVE_REMOTE_CONTACT, mContact);
        outState.putBoolean(SAVE_STARTED, mStarted);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!isServiceConnected(RcsServiceName.VIDEO_SHARING)) {
            return;
        }
        try {
            getVideoSharingApi().removeEventListener(vshListener);

        } catch (RcsServiceException e) {
            Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
        }
        closeCamera();
    }

    /**
     * Dial button listener
     */
    // TODO initialize
    private OnClickListener btnDialListener = new OnClickListener() {
        public void onClick(View v) {

            // get selected phone number
            ContactListAdapter adapter = (ContactListAdapter) mSpinner.getAdapter();
            String phoneNumber = adapter.getSelectedNumber(mSpinner.getSelectedView());

            // Initiate a GSM call before to be able to share content
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:".concat(phoneNumber)));
            if (ActivityCompat.checkSelfPermission(OutgoingVideoSharing.this,
                    Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            startActivity(intent);
        }
    };

    /**
     * Invite button listener
     */
    private OnClickListener btnInviteListener = new OnClickListener() {
        public void onClick(View v) {
            // Check if the service is available
            try {
                if (!getVideoSharingApi().isServiceRegistered()) {
                    showMessage(R.string.label_service_not_available);
                    return;
                }
            } catch (RcsServiceException e) {
                showExceptionThenExit(e);
            }

            // Get the remote contact
            ContactListAdapter adapter = (ContactListAdapter) mSpinner.getAdapter();
            String phoneNumber = adapter.getSelectedNumber(mSpinner.getSelectedView());

            mContact = ContactUtil.formatContact(phoneNumber);

            new Thread() {
                public void run() {
                    try {
                        // Create the video player
                        mVideoPlayer = new OriginatingVideoPlayer(OutgoingVideoSharing.this);

                        // Open the camera
                        openCamera();

                        // Initiate sharing
                        mVideoSharing = getVideoSharingApi().shareVideo(mContact, mVideoPlayer);
                        mSharingId = mVideoSharing.getSharingId();

                    } catch (final RcsServiceException e) {
                        // Free the camera
                        closeCamera();

                        handler.post(new Runnable() {
                            public void run() {
                                showExceptionThenExit(e);
                            }
                        });
                    }
                }
            }.start();

            mSwitchCamBtn.setEnabled(true);

            // Hide buttons
            mInviteBtn.setVisibility(View.GONE);
            mDialBtn.setVisibility(View.GONE);
            mSpinner.setVisibility(View.GONE);
            displayRemoteContact();
        }
    };

    /**
     * Switch camera button listener
     */
    private View.OnClickListener btnSwitchCamListener = new View.OnClickListener() {
        public void onClick(View v) {
            // Switch camera
            switchCamera();
        }
    };

    private void quitSession() {
        try {
            if (mVideoSharing != null && VideoSharing.State.STARTED == mVideoSharing.getState()) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "Abort sharing");
                }
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
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "Back key pressed");
            }
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

    /*-------------------------- Camera methods ------------------*/

    /**
     * Open the camera
     */
    private synchronized void openCamera() {
        if (mCamera != null) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "Already opened camera");
            }
            return;

        }
        openCamera(mOpenedCameraId);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mVideoView.setAspectRatio(mVideoWidth, mVideoHeight);
        } else {
            mVideoView.setAspectRatio(mVideoHeight, mVideoWidth);
        }
        // Start camera
        mCamera.setPreviewCallback(mVideoPlayer);
        startCameraPreview();
    }

    /**
     * Close the camera
     */
    private synchronized void closeCamera() {
        if (mCamera == null) {
            return;

        }
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "Close camera");
        }
        mCamera.setPreviewCallback(null);
        if (mCameraPreviewRunning) {
            mCameraPreviewRunning = false;
            mCamera.stopPreview();
        }
        mCamera.release();
        mCamera = null;
    }

    /**
     * Switch the camera
     */
    private synchronized void switchCamera() {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "Switch camera");
        }
        closeCamera();
        // Open the other camera
        if (mOpenedCameraId.getValue() == CameraOptions.BACK.getValue()) {
            mOpenedCameraId = CameraOptions.FRONT;
        } else {
            mOpenedCameraId = CameraOptions.BACK;
        }

        openCamera();
    }

    /**
     * Check if good camera sizes are available for encoder. Must be used only before open camera.
     *
     * @param cameraId the camera ID
     * @return false if the camera don't have the good preview size for the encoder
     */
    boolean checkCameraSize(CameraOptions cameraId) {
        boolean sizeAvailable = false;
        Camera camera = null;
        Method method = getCameraOpenMethod();
        if (method != null) {
            try {
                camera = (Camera) method.invoke(camera, new Object[] {
                    cameraId.getValue()
                });
            } catch (Exception e) {
                camera = Camera.open();
            }
        } else {
            camera = Camera.open();
        }
        if (camera == null) {
            return false;

        }
        // Check common sizes
        Parameters param = camera.getParameters();
        List<Camera.Size> sizes = param.getSupportedPreviewSizes();
        for (Camera.Size size : sizes) {
            if ((size.width == H264Config.QVGA_WIDTH && size.height == H264Config.QVGA_HEIGHT)
                    || (size.width == H264Config.CIF_WIDTH && size.height == H264Config.CIF_HEIGHT)
                    || (size.width == H264Config.VGA_WIDTH && size.height == H264Config.VGA_HEIGHT)) {
                sizeAvailable = true;
                break;
            }
        }

        // Release camera
        camera.release();

        return sizeAvailable;
    }

    /**
     * Start the camera preview
     */
    private void startCameraPreview() {
        if (mCamera == null) {
            return;

        }
        // Camera settings
        Camera.Parameters p = mCamera.getParameters();
        p.setPreviewFormat(PixelFormat.YCbCr_420_SP);

        // Orientation
        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        switch (display.getRotation()) {
            case Surface.ROTATION_0:
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "ROTATION_0");
                }
                if (mOpenedCameraId == CameraOptions.FRONT) {
                    mVideoPlayer.setOrientation(Orientation.ROTATE_90_CCW);
                } else {
                    mVideoPlayer.setOrientation(Orientation.ROTATE_90_CW);
                }
                mCamera.setDisplayOrientation(90);
                break;

            case Surface.ROTATION_90:
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "ROTATION_90");
                }
                mVideoPlayer.setOrientation(Orientation.NONE);
                break;

            case Surface.ROTATION_180:
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "ROTATION_180");
                }
                if (mOpenedCameraId == CameraOptions.FRONT) {
                    mVideoPlayer.setOrientation(Orientation.ROTATE_90_CW);
                } else {
                    mVideoPlayer.setOrientation(Orientation.ROTATE_90_CCW);
                }
                mCamera.setDisplayOrientation(270);
                break;

            case Surface.ROTATION_270:
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "ROTATION_270");
                }
                if (mOpenedCameraId == CameraOptions.FRONT) {
                    mVideoPlayer.setOrientation(Orientation.ROTATE_180);
                } else {
                    mVideoPlayer.setOrientation(Orientation.ROTATE_180);
                }
                mCamera.setDisplayOrientation(180);
                break;
        }

        // Check if preview size is supported
        if (isPreviewSizeSupported(p, mVideoWidth, mVideoHeight)) {
            // Use the existing size without resizing
            p.setPreviewSize(mVideoWidth, mVideoHeight);
            // TODO videoPlayer.activateResizing(videoWidth, videoHeight); //
            // same size = no
            // resizing
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "Camera preview initialized with size " + mVideoWidth + "x"
                        + mVideoHeight);
            }
        } else {
            // Check if can use a other known size (QVGA, CIF or VGA)
            int w = 0;
            int h = 0;
            for (Camera.Size size : p.getSupportedPreviewSizes()) {
                w = size.width;
                h = size.height;
                if ((w == H264Config.QVGA_WIDTH && h == H264Config.QVGA_HEIGHT)
                        || (w == H264Config.CIF_WIDTH && h == H264Config.CIF_HEIGHT)
                        || (w == H264Config.VGA_WIDTH && h == H264Config.VGA_HEIGHT)) {
                    break;
                }
            }

            if (w != 0) {
                p.setPreviewSize(w, h);
                // TODO does not work if default sizes are not supported like
                // for Samsung S5 mini
                // mVideoPlayer.activateResizing(w, h);
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "Camera preview initialized with size " + w + "x" + h
                            + " with a resizing to " + mVideoWidth + "x" + mVideoHeight);
                }
            } else {
                // The camera don't have known size, we can't use it
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "Camera preview can't be initialized with size " + mVideoWidth
                            + "x" + mVideoHeight);
                }
                Toast.makeText(this,
                        getString(R.string.label_session_failed, "Camera is not compatible"),
                        Toast.LENGTH_SHORT).show();
                quitSession();
                return;

            }
        }

        // Set camera parameters
        mCamera.setParameters(p);
        try {
            mCamera.setPreviewDisplay(mVideoView.getHolder());
            mCamera.startPreview();
            mCameraPreviewRunning = true;
        } catch (Exception e) {
            mCamera = null;
        }
    }

    /**
     * Get Camera "open" Method
     *
     * @return Method
     */
    private Method getCameraOpenMethod() {
        ClassLoader classLoader = OutgoingVideoSharing.class.getClassLoader();
        try {
            Class<?> cameraClass = classLoader.loadClass("android.hardware.Camera");
            try {
                return cameraClass.getMethod("open", new Class[] {
                    int.class
                });
            } catch (NoSuchMethodException ignored) {
            }
        } catch (ClassNotFoundException ignored) {
        }
        return null;
    }

    /**
     * Open the camera
     *
     * @param cameraId Camera ID
     */
    private void openCamera(CameraOptions cameraId) {
        Method method = getCameraOpenMethod();
        if (mNbfCameras > 1 && method != null) {
            try {
                int hCamId = 0;
                if (cameraId == CameraOptions.FRONT) {
                    Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                    for (int id = 0; id < mNbfCameras; id++) {
                        Camera.getCameraInfo(id, cameraInfo);
                        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                            hCamId = id;
                            break;
                        }
                    }
                }
                mCamera = (Camera) method.invoke(mCamera, new Object[] {
                    hCamId
                });
                mOpenedCameraId = cameraId;
            } catch (Exception e) {
                mCamera = Camera.open();
                mOpenedCameraId = CameraOptions.BACK;
            }
        } else {
            mCamera = Camera.open();
            mOpenedCameraId = CameraOptions.BACK;
        }
        if (mVideoPlayer != null) {
            mVideoPlayer.setCameraId(mOpenedCameraId.getValue());
        }
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "Open camera ".concat(mOpenedCameraId.toString()));
        }
    }

    /**
     * Get Camera "numberOfCameras" Method
     *
     * @return Method
     */
    private Method getCameraNumberOfCamerasMethod() {
        ClassLoader classLoader = OutgoingVideoSharing.class.getClassLoader();
        try {
            Class<?> cameraClass = classLoader.loadClass("android.hardware.Camera");
            try {
                return cameraClass.getMethod("getNumberOfCameras", (Class[]) null);
            } catch (NoSuchMethodException ignored) {
            }
        } catch (ClassNotFoundException ignored) {
        }
        return null;
    }

    /**
     * Get number of cameras
     *
     * @return number of cameras
     */
    private int getNumberOfCameras() {
        Method method = getCameraNumberOfCamerasMethod();
        if (method != null) {
            try {
                return (Integer) method.invoke(null, (Object[]) null);
            } catch (Exception e) {
                return 1;
            }
        } else {
            return 1;
        }
    }

    /*-------------------------- Session callbacks ------------------*/

    /**
     * Video sharing listener
     */
    private VideoSharingListener vshListener = new VideoSharingListener() {
        @Override
        public void onStateChanged(ContactId contact, String sharingId,
                final VideoSharing.State state, VideoSharing.ReasonCode reasonCode) {
            // Discard event if not for current sharingId
            if (mSharingId == null || !mSharingId.equals(sharingId)) {
                return;
            }
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "onStateChanged contact=" + contact + " sharingId=" + sharingId
                        + " state=" + state + " reason=" + reasonCode);
            }
            final String _reasonCode = RiApplication.sVideoReasonCodes[reasonCode.toInt()];
            handler.post(new Runnable() {
                public void run() {
                    switch (state) {
                        case STARTED:
                            mStarted = true;
                            displayVideoFormat();

                            // Start the player
                            mVideoPlayer.open();
                            mVideoPlayer.start();

                            // Update camera button
                            Button switchCamBtn = (Button) findViewById(R.id.switch_cam_btn);
                            switchCamBtn.setEnabled(true);

                            // Session is established : hide progress dialog
                            break;

                        case ABORTED:
                            // Stop the player
                            mVideoPlayer.stop();
                            mVideoPlayer.close();

                            // Release the camera
                            closeCamera();

                            // Display message info and exit
                            showMessageThenExit(getString(R.string.label_sharing_aborted,
                                    _reasonCode));
                            break;

                        case REJECTED:
                            // Release the camera
                            closeCamera();

                            showMessageThenExit(getString(R.string.label_sharing_rejected,
                                    _reasonCode));
                            break;

                        case FAILED:
                            // Stop the player
                            mVideoPlayer.stop();
                            mVideoPlayer.close();

                            // Release the camera
                            closeCamera();

                            // Display error info and exit
                            showMessageThenExit(getString(R.string.label_sharing_failed,
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

    /*-------------------------- Video player callbacks ------------------*/

    /**
     * Callback called when the player is opened
     */
    public void onPlayerOpened() {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onPlayerOpened");
        }
    }

    /**
     * Callback called when the player is started
     */
    public void onPlayerStarted() {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onPlayerStarted");
        }
    }

    /**
     * Callback called when the player is stopped
     */
    public void onPlayerStopped() {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onPlayerStopped");
        }
    }

    /**
     * Callback called when the player is closed
     */
    public void onPlayerClosed() {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onPlayerClosed");
        }
    }

    /**
     * Callback called when the player has failed
     */
    public void onPlayerError() {
        // TODO
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onPlayerError");
        }
    }

    /**
     * Callback called when the player has been resized
     */
    public void onPlayerResized(int width, int height) {
        // TODO
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onPlayerResized");
        }
    }

    /**
     * Check if preview size is supported
     *
     * @param parameters Camera parameters
     * @param width width
     * @param height height
     * @return True if supported
     */
    private boolean isPreviewSizeSupported(Parameters parameters, int width, int height) {
        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        for (Size size : sizes) {
            if (size.width == width && size.height == height) {
                return true;

            }
        }
        return false;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mIsSurfaceCreated = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mIsSurfaceCreated = true;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mIsSurfaceCreated = false;
    }

    /**
     * Runnable to continue outgoing session.<br>
     * Note: the surface view be created to display preview.
     */
    private Runnable continueOutgoingSessionRunnable = new Runnable() {
        private int delay = 0;

        @Override
        public void run() {
            if (mIsSurfaceCreated) {
                // Open camera only once surface is created
                openCamera();
            } else {
                delay += 200;
                handler.removeCallbacks(this);
                if (delay < 2000) {
                    if (LogUtils.isActive) {
                        Log.e(LOGTAG, "Delaying continue Outgoing");
                    }
                    handler.postDelayed(this, delay);
                }
            }
        }
    };

    /**
     * Display video format
     */
    private void displayVideoFormat() {
        try {
            VideoDescriptor videoDescriptor = mVideoSharing.getVideoDescriptor();
            String format = mVideoSharing.getVideoEncoding() + " " + videoDescriptor.getWidth()
                    + "x" + videoDescriptor.getHeight();
            TextView fmtView = (TextView) findViewById(R.id.video_format);
            fmtView.setText(format);

        } catch (RcsPersistentStorageException | RcsGenericException e) {
            Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
        }
    }

    /**
     * Display remote contact
     */
    private void displayRemoteContact() {
        TextView fromTextView = (TextView) findViewById(R.id.remote);
        String displayName = RcsContactUtil.getInstance(this).getDisplayName(mContact);
        fromTextView.setText(displayName);
    }
}
