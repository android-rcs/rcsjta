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

package com.orangelabs.rcs.ri.sharing.video;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.video.VideoDescriptor;
import com.gsma.services.rcs.sharing.video.VideoSharing;
import com.gsma.services.rcs.sharing.video.VideoSharingListener;
import com.gsma.services.rcs.sharing.video.VideoSharingService;

import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.H264Config;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.CameraOptions;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.Orientation;
import com.orangelabs.rcs.ri.ConnectionManager;
import com.orangelabs.rcs.ri.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.sharing.video.media.OriginatingVideoPlayer;
import com.orangelabs.rcs.ri.sharing.video.media.VideoPlayerListener;
import com.orangelabs.rcs.ri.sharing.video.media.VideoSurfaceView;
import com.orangelabs.rcs.ri.utils.ContactListAdapter;
import com.orangelabs.rcs.ri.utils.ContactUtil;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsDisplayName;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Initiate video sharing.
 * 
 * @author Jean-Marc AUFFRET
 * @author YPLO6403
 */
public class OutgoingVideoSharing extends Activity implements VideoPlayerListener,
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
     * Progress dialog
     */
    private Dialog mProgressDialog;

    /**
     * A locker to exit only once
     */
    private LockAccess mExitOnce = new LockAccess();

    /**
     * API connection manager
     */
    private ConnectionManager mCnxManager;

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

    /**
     * The log tag for this class
     */
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
        mSpinner.setAdapter(ContactListAdapter.createRcsContactListAdapter(this));

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
            Log.d(LOGTAG,
                    new StringBuilder("Sharing ID ").append(mSharingId).append(" Nb of cameras=")
                            .append(mNbfCameras).append(" active camera=").append(mOpenedCameraId)
                            .toString());
        }
        if (LogUtils.isActive) {
            Log.d(LOGTAG,
                    new StringBuilder("Resolution: ").append(mVideoWidth).append("x")
                            .append(mVideoHeight).toString());
        }

        // Set button callback
        mInviteBtn = (Button) findViewById(R.id.invite_btn);
        mInviteBtn.setOnClickListener(btnInviteListener);
        mDialBtn = (Button) findViewById(R.id.dial_btn);
        mDialBtn.setOnClickListener(btnDialListener);

        mSwitchCamBtn = (Button) findViewById(R.id.switch_cam_btn);

        // Disable button if no contact available
        if (mSpinner.getAdapter().getCount() == 0) {
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

            boolean canInitiate = (mSpinner.getAdapter().getCount() != 0);
            mDialBtn.setEnabled(canInitiate);
            mInviteBtn.setEnabled(canInitiate);
        }

        // Register to API connection manager
        mCnxManager = ConnectionManager.getInstance(this);
        if (mCnxManager == null || !mCnxManager.isServiceConnected(RcsServiceName.VIDEO_SHARING)) {
            Utils.showMessageAndExit(this, getString(R.string.label_service_not_available),
                    mExitOnce);
            return;

        }
        mCnxManager.startMonitorServices(this, mExitOnce, RcsServiceName.VIDEO_SHARING);

        // Add service listener
        try {
            VideoSharingService vshService = mCnxManager.getVideoSharingApi();
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
            Utils.showMessageAndExit(this, getString(R.string.label_api_failed), mExitOnce, e);
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
    };

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mCnxManager == null) {
            return;

        }
        mCnxManager.stopMonitorServices(this);
        if (!mCnxManager.isServiceConnected(RcsServiceName.VIDEO_SHARING)) {
            return;

        }
        // Remove video sharing listener
        try {
            mCnxManager.getVideoSharingApi().removeEventListener(vshListener);
        } catch (Exception e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Failed to remove listener", e);
            }
        }

        // Close the camera
        closeCamera();
    }

    /**
     * Dial button listener
     */
    private OnClickListener btnDialListener = new OnClickListener() {
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

    /**
     * Invite button listener
     */
    private OnClickListener btnInviteListener = new OnClickListener() {
        public void onClick(View v) {
            // Check if the service is available
            boolean registered = false;
            try {
                registered = mCnxManager.getVideoSharingApi().isServiceRegistered();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!registered) {
                Utils.showMessage(OutgoingVideoSharing.this,
                        getString(R.string.label_service_not_available));
                return;

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
                        mVideoSharing = mCnxManager.getVideoSharingApi().shareVideo(mContact,
                                mVideoPlayer);
                        mSharingId = mVideoSharing.getSharingId();
                    } catch (Exception e) {
                        e.printStackTrace();

                        // Free the camera
                        closeCamera();

                        handler.post(new Runnable() {
                            public void run() {
                                hideProgressDialog();
                                Utils.showMessageAndExit(OutgoingVideoSharing.this,
                                        getString(R.string.label_invitation_failed), mExitOnce);
                            }
                        });
                    }
                }
            }.start();

            mSwitchCamBtn.setEnabled(true);

            // Display a progress dialog
            mProgressDialog = Utils.showProgressDialog(OutgoingVideoSharing.this,
                    getString(R.string.label_command_in_progress));
            mProgressDialog.setOnCancelListener(new OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    Toast.makeText(OutgoingVideoSharing.this,
                            getString(R.string.label_sharing_cancelled), Toast.LENGTH_SHORT).show();
                    quitSession();
                }
            });

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

    /**
     * Hide progress dialog
     */
    public void hideProgressDialog() {
        if (mProgressDialog == null) {
            return;

        }
        if (mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        mProgressDialog = null;
    }

    /**
     * Quit the session
     */
    private void quitSession() {
        // Stop the sharing
        try {
            if (mVideoSharing != null && VideoSharing.State.STARTED == mVideoSharing.getState()) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "Abort sharing");
                }
                mVideoSharing.abortSharing();
            }
        } catch (Exception e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Exception occurred", e);
            }
        }
        mVideoSharing = null;

        // Exit activity
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "Back key pressed");
                }
                // Quit the session
                quitSession();
                return true;
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
        switch (item.getItemId()) {
            case R.id.menu_close_session:
                // Quit the session
                quitSession();
                break;
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
     * @param cameraId
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
                Log.d(LOGTAG,
                        new StringBuilder("Camera preview initialized with size ")
                                .append(mVideoWidth).append("x").append(mVideoHeight).toString());
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
                    Log.d(LOGTAG,
                            new StringBuilder("Camera preview initialized with size ").append(w)
                                    .append("x").append(h).append(" with a resizing to ")
                                    .append(mVideoWidth).append("x").append(mVideoHeight)
                                    .toString());
                }
            } else {
                // The camera don't have known size, we can't use it
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, new StringBuilder(
                            "Camera preview can't be initialized with size ").append(mVideoWidth)
                            .append("x").append(mVideoHeight).toString());
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
            } catch (NoSuchMethodException e) {
            }
        } catch (ClassNotFoundException e) {
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
            } catch (NoSuchMethodException e) {
            }
        } catch (ClassNotFoundException e) {
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
                Integer ret = (Integer) method.invoke(null, (Object[]) null);
                return ret.intValue();
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
                Log.d(LOGTAG,
                        new StringBuilder("onStateChanged contact=").append(contact)
                                .append(" sharingId=").append(sharingId).append(" state=")
                                .append(state).append(" reason=").append(reasonCode).toString());
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
                            hideProgressDialog();
                            break;

                        case ABORTED:
                            // Stop the player
                            mVideoPlayer.stop();
                            mVideoPlayer.close();

                            // Release the camera
                            closeCamera();

                            // Hide progress dialog
                            hideProgressDialog();

                            // Display message info and exit
                            Utils.showMessageAndExit(OutgoingVideoSharing.this,
                                    getString(R.string.label_sharing_aborted, _reasonCode),
                                    mExitOnce);
                            break;

                        case REJECTED:
                            // Release the camera
                            closeCamera();

                            // Hide progress dialog
                            hideProgressDialog();
                            Utils.showMessageAndExit(OutgoingVideoSharing.this,
                                    getString(R.string.label_sharing_rejected, _reasonCode),
                                    mExitOnce);
                            break;

                        case FAILED:
                            // Stop the player
                            mVideoPlayer.stop();
                            mVideoPlayer.close();

                            // Release the camera
                            closeCamera();

                            // Hide progress dialog
                            hideProgressDialog();

                            // Display error info and exit
                            Utils.showMessageAndExit(OutgoingVideoSharing.this,
                                    getString(R.string.label_sharing_failed, _reasonCode),
                                    mExitOnce);
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
                Log.w(LOGTAG,
                        new StringBuilder("onDeleted contact=").append(contact)
                                .append(" sharingIds=").append(sharingIds).toString());
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
     * @param width
     * @param height
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
            String format = new StringBuilder(mVideoSharing.getVideoEncoding()).append(" ")
                    .append(videoDescriptor.getWidth()).append("x")
                    .append(videoDescriptor.getHeight()).toString();
            TextView fmtView = (TextView) findViewById(R.id.video_format);
            fmtView.setVisibility(View.VISIBLE);
            fmtView.setText(getString(R.string.label_video_format, format));
        } catch (Exception e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Exception occurred", e);
            }
        }
    }

    /**
     * Display remote contact
     */
    private void displayRemoteContact() {
        TextView fromTextView = (TextView) findViewById(R.id.with);
        fromTextView.setVisibility(View.VISIBLE);
        String displayName = RcsDisplayName.getInstance(this).getDisplayName(mContact);
        fromTextView.setText(getString(R.string.label_with_args, displayName));
    }
}
