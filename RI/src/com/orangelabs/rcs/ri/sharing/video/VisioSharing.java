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

import org.gsma.joyn.JoynService;
import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.JoynServiceListener;
import org.gsma.joyn.JoynServiceNotAvailableException;
import org.gsma.joyn.vsh.VideoRenderer;
import org.gsma.joyn.vsh.VideoSharing;
import org.gsma.joyn.vsh.VideoSharingIntent;
import org.gsma.joyn.vsh.VideoSharingListener;
import org.gsma.joyn.vsh.VideoSharingService;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.H264Config;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.CameraOptions;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.Orientation;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.sharing.video.player.MyVideoPlayer;
import com.orangelabs.rcs.ri.sharing.video.player.MyVideoRenderer;
import com.orangelabs.rcs.ri.sharing.video.player.VideoSurfaceView;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Visio sharing activity - two half duplex live video sharing
 *
 * @author hlxn7157
 */
public class VisioSharing extends Activity implements SurfaceHolder.Callback, JoynServiceListener {
	/**
	 * Intent parameters
	 */
	// TODO

	/**
	 * UI handler
	 */
	private final Handler handler = new Handler();

/**
 * Sharing API connected
 */
private boolean isApiConnected = false;
    
    /**
	 * Switch camera button
	 */
	private Button switchCamBtn = null;

	/**
	 * Outgoing button
	 */
	private Button outgoingBtn = null;

	/**
	 * Incoming button
	 */
	private Button incomingBtn = null;

	/**
	 * Camera
	 */
	private Camera camera = null;

	/**
	 * Opened camera id
	 */
	private CameraOptions openedCameraId = CameraOptions.FRONT;

	/**
	 * Camera preview started flag
	 */
	private boolean cameraPreviewRunning = false;

	/**
	 * Number of cameras
	 */
	private int numberOfCameras = 1;

	/**
	 * Remote contact
	 */
	private String remoteContact;

/**
 * Pending incoming notification
 */
private boolean pendingIncomingNotification = false;

/**
 * Pending incoming notification
 */
private AlertDialog incomingAlert = null;

/**
 * Pending incoming session
 */
private boolean pendingIncomingSession = false;

	/**
	 * Video sharing API
	 */
	private VideoSharingService vshApi = null;

	/*--------- incoming info ----------*/

	/**
	 * Incoming video sharing session
	 */
	private VideoSharing incomingCshSession = null;

	/**
	 * Video renderer
	 */
	private VideoRenderer incomingRenderer = null;

	/**
	 * Incoming Video preview
	 */
	private VideoSurfaceView incomingVideoView = null;

	/**
	 * Incoming session ID
	 */
	private String incomingSessionId = null;

	/**
	 * Incoming Video width
	 */
	private int incomingWidth = 0;

	/**
	 * Incoming Video height
	 */
	private int incomingHeight = 0;

    /**
     * Incoming video sharing session event listener
     */
    private IncomingVideoSharingListener incomingSessionEventListener = null;

    /*--------- outgoing info ----------*/
	
/**
 * Pending outgoing session
 */
private boolean pendingOutgoingSession = false;

	/**
	 * Outgoing video sharing session
	 */
	private VideoSharing outgoingCshSession = null;

	/**
	 * Outgoing Video player
	 */
	private MyVideoPlayer outgoingPlayer;

	/**
	 * Outgoing Video preview
	 */
	private VideoSurfaceView outgoingVideoView = null;

	/**
	 * Outgoing session ID
	 */
	private String outgoingSessionId = null;

	/**
	 * Outgoing Video width
	 */
	private int outgoingWidth = H264Config.QCIF_WIDTH;

	/**
	 * Outgoing Video height
	 */
	private int outgoingHeight = H264Config.QCIF_HEIGHT;

	/**
	 * Outgoing video surface holder
	 */
	private SurfaceHolder surface;

	/**
	 * Outgoing Progress dialog
	 */
	private Dialog outgoingProgressDialog = null;

	/**
	 * Preview surface view is created
	 */
	private boolean isSurfaceCreated = false;

    /**
     * Outgoing video sharing session event listener
     */
    private OutgoingVideoSharingListener outgoingSessionEventListener = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Always on window
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        // Set layout
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.richcall_visio_sharing2);

        // Instantiate buttons
        incomingBtn = (Button)findViewById(R.id.incoming_btn);
        incomingBtn.setOnClickListener(btnIncomingListener);
        outgoingBtn = (Button)findViewById(R.id.outgoing_btn);
        outgoingBtn.setOnClickListener(btnOutgoingListener);
        switchCamBtn = (Button)findViewById(R.id.switch_cam_btn);

        // Get camera info
        numberOfCameras = getNumberOfCameras();

// Get invitation info
remoteContact = getIntent().getStringExtra("contact");
if (getIntent().getBooleanExtra("incoming", false)) {
    extractIncomingInfo(getIntent());
} else {
    handler.post(startOutgoingSessionRunnable);
}

        // Pending Incoming notification
        if (pendingIncomingNotification) {
            handler.post(receiveIncomingSessionRunnable);
        }

        // Update view
        TextView fromTxt = (TextView)findViewById(R.id.visio_with_txt);
        fromTxt.setText(getString(R.string.label_video_sharing_with, remoteContact));
        if (pendingIncomingSession) {
            incomingBtn.setEnabled(true);
        } else {
            incomingBtn.setEnabled(false);
        }
        if (pendingOutgoingSession) {
            outgoingBtn.setText(R.string.label_stop_outgoing_btn);
            switchCamBtn.setEnabled(true);
        } else {
            outgoingBtn.setText(R.string.label_start_outgoing_btn);
            switchCamBtn.setEnabled(false);
        }
        if (numberOfCameras > 1) {
            boolean backAvailable = checkCameraSize(CameraOptions.BACK);
            boolean frontAvailable = checkCameraSize(CameraOptions.FRONT);
            if (frontAvailable && backAvailable) {
                switchCamBtn.setOnClickListener(btnSwitchCamListener);
            } else if (frontAvailable) {
                openedCameraId = CameraOptions.FRONT;
                switchCamBtn.setVisibility(View.INVISIBLE);
            } else if (backAvailable) {
                openedCameraId = CameraOptions.BACK;
                switchCamBtn.setVisibility(View.INVISIBLE);
            } else {
                // TODO: Error - no camera available for encoding
            }
        } else {
            if (checkCameraSize(CameraOptions.FRONT)) {
                switchCamBtn.setVisibility(View.INVISIBLE);
            } else {
                // TODO: Error - no camera available for encoding
            }
        }

        // Set incoming video preview
        incomingVideoView = (VideoSurfaceView)findViewById(R.id.incoming_video_view);
        if (incomingWidth != 0 && incomingHeight != 0) {
            incomingVideoView.setAspectRatio(incomingWidth, incomingHeight);
        }

        // Create the live video player
        outgoingVideoView = (VideoSurfaceView)findViewById(R.id.outgoing_video_preview);
        if (outgoingWidth == 0 || outgoingHeight == 0) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                outgoingVideoView.setAspectRatio(H264Config.QCIF_WIDTH, H264Config.QCIF_HEIGHT);
            } else {
                outgoingVideoView.setAspectRatio(H264Config.QCIF_HEIGHT, H264Config.QCIF_WIDTH);
            }
        } else {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                outgoingVideoView.setAspectRatio(outgoingWidth, outgoingHeight);
            } else {
                outgoingVideoView.setAspectRatio(outgoingHeight, outgoingWidth);
            }
        }
        surface = outgoingVideoView.getHolder();
        surface.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surface.setKeepScreenOn(true);
        surface.addCallback(this);

        if (pendingOutgoingSession) {
            handler.post(continueOutgoingSessionRunnable);
        }

        if (pendingIncomingSession) {
            handler.post(continueIncomingSessionRunnable);
        }
        
        // Instanciate API
        vshApi = new VideoSharingService(getApplicationContext(), this);
		
		// Connect API
        vshApi.connect();        
    }

    @Override
    public void onResume() {
        super.onResume();

        // Set broadcast receiver
        IntentFilter filter = new IntentFilter(VideoSharingIntent.ACTION_NEW_INVITATION);
        registerReceiver(intentReceiver, filter, null, handler);
    }

    @Override
    public void onPause() {
        super.onPause();

        // Unregister intent receiver
        try {
            unregisterReceiver(intentReceiver);
        } catch (IllegalArgumentException e) {
            // Nothing to do
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (pendingIncomingNotification && incomingAlert != null) {
            incomingAlert.cancel();
        }

        if (isFinishing()) {
            // Close sessions
            new Thread() {
                public void run() {
                    stopIncomingSession(true);
                    stopOutgoingSession(true);
                }
            }.start();
        } else {
            releaseCamera();
        }

        // Disconnect API
        vshApi.disconnect();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                // Quit session
                new Thread() {
                    public void run() {
                        stopIncomingSession(true);
                        stopOutgoingSession(true);
                        exitIfNoSession(null);
                    }
                }.start();
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=new MenuInflater(getApplicationContext());
        inflater.inflate(R.menu.menu_video_sharing, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_close_session:
                stopOutgoingSession(true);
                stopIncomingSession(true);
                exitIfNoSession(null);
                break;
        }
        return true;
    }

    /* *****************************************
     *              API listener
     ***************************************** */

    /**
     * Callback called when service is connected. This method is called when the
     * service is well connected to the RCS service (binding procedure successfull):
     * this means the methods of the API may be used.
     */
    public void onServiceConnected() {
        isApiConnected = true;
    }
    
    /**
     * Callback called when service has been disconnected. This method is called when
     * the service is disconnected from the RCS service (e.g. service deactivated).
     * 
     * @param error Error
     * @see JoynService.Error
     */
    public void onServiceDisconnected(int error) {
        isApiConnected = false;
        handler.post(new Runnable() {
            public void run() {
                Utils.showMessageAndExit(VisioSharing.this,
                        getString(R.string.label_api_disabled));
            }
        });        
    }
    
    /**
     * Callback called when service is registered to the RCS/IMS platform
     */
    public void onServiceRegistered() {
    	// Not used here
    }
    
    /**
     * Callback called when service is unregistered from the RCS/IMS platform
     */
    public void onServiceUnregistered() {
        handler.post(new Runnable() {
            public void run() {
                Utils.showMessageAndExit(VisioSharing.this,
                        getString(R.string.label_ims_disconnected));
            }
        });
    }    
    
    /* *****************************************
     *              Button listener
     ***************************************** */

    /**
     * Accept button listener
     */
    private OnClickListener acceptBtnListener = new OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            pendingIncomingNotification = false;
            VideoSharingInvitationReceiver.removeVideoSharingNotification(getApplicationContext(), incomingSessionId);
            acceptIncomingSession();
        }
    };

    /**
     * Reject button listener
     */
    private OnClickListener declineBtnListener = new OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            pendingIncomingNotification = false;
            VideoSharingInvitationReceiver.removeVideoSharingNotification(getApplicationContext(), incomingSessionId);
            declineIncomingSession();
        }
    };

    /**
     * Switch camera button listener
     */
    private View.OnClickListener btnSwitchCamListener = new View.OnClickListener() {
        public void onClick(View v) {
            // Release camera
            releaseCamera();

            // Open the other camera
            if (openedCameraId.getValue() == CameraOptions.BACK.getValue()) {
                OpenCamera(CameraOptions.FRONT);
            } else {
                OpenCamera(CameraOptions.BACK);
            }

            // Restart the preview
            camera.setPreviewCallback(outgoingPlayer);
            startCameraPreview();
        }
    };

    /**
     * Outgoing button listener
     */
    private View.OnClickListener btnOutgoingListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (pendingOutgoingSession) {
                // Stop the outgoing session
                new Thread() {
                    public void run() {
                        stopOutgoingSession(true);
                        exitIfNoSession(null);

                        recreateVideoPlayer();
                    }
                }.start();
            } else {
                // Start a new outgoing session
                handler.post(startOutgoingSessionRunnable);
            }
        }
    };

    /**
     * Incoming button listener
     */
    private View.OnClickListener btnIncomingListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (pendingIncomingSession) {
                new Thread() {
                    public void run() {
                        stopIncomingSession(true);
                        exitIfNoSession(null);
                    }
                }.start();
            }
        }
    };

    /* *****************************************
     *                Camera
     ***************************************** */

    /**
     * Get Camera "open" Method
     *
     * @return Method
     */
    private Method getCameraOpenMethod() {
        ClassLoader classLoader = VisioSharing.class.getClassLoader();
        Class cameraClass = null;
        try {
            cameraClass = classLoader.loadClass("android.hardware.Camera");
            try {
                return cameraClass.getMethod("open", new Class[] {
                    int.class
                });
            } catch (NoSuchMethodException e) {
                return null;
            }
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Get Camera "numberOfCameras" Method
     *
     * @return Method
     */
    private Method getCameraNumberOfCamerasMethod() {
        ClassLoader classLoader = VisioSharing.class.getClassLoader();
        Class cameraClass = null;
        try {
            cameraClass = classLoader.loadClass("android.hardware.Camera");
            try {
                return cameraClass.getMethod("getNumberOfCameras", (Class[])null);
            } catch (NoSuchMethodException e) {
                return null;
            }
        } catch (ClassNotFoundException e) {
            return null;
        }
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
                Integer ret = (Integer)method.invoke(null, (Object[])null);
                return ret.intValue();
            } catch (Exception e) {
                return 1;
            }
        } else {
            return 1;
        }
    }

    /**
     * Open a camera
     *
     * @param cameraId
     */
    private void OpenCamera(CameraOptions cameraId) {
        Method method = getCameraOpenMethod();
        if (numberOfCameras > 1 && method != null) {
            try {
                camera = (Camera)method.invoke(camera, new Object[] {
                    cameraId.getValue()
                });
                openedCameraId = cameraId;
            } catch (Exception e) {
                camera = Camera.open();
                openedCameraId = CameraOptions.BACK;
            }
        } else {
            camera = Camera.open();
            openedCameraId = CameraOptions.BACK;
        }
        if (outgoingPlayer != null) {
            outgoingPlayer.setCameraId(openedCameraId.getValue());
        }
    }

    /**
     * Check if good camera sizes are available for encoder.
     * Must be used only before open camera.
     * 
     * @param cameraId
     * @return false if the camera don't have the good preview size for the encoder
     */
    private boolean checkCameraSize(CameraOptions cameraId) {
        boolean sizeAvailable = false;

        // Open the camera
        OpenCamera(cameraId);

        // Check common sizes
        Parameters param = camera.getParameters();
        List<Camera.Size> sizes = param.getSupportedPreviewSizes();
        for (Camera.Size size:sizes) {
            if (    (size.width == H264Config.QVGA_WIDTH && size.height == H264Config.QVGA_HEIGHT) ||
                    (size.width == H264Config.CIF_WIDTH && size.height == H264Config.CIF_HEIGHT) ||
                    (size.width == H264Config.VGA_WIDTH && size.height == H264Config.VGA_HEIGHT)) {
                sizeAvailable = true;
                break;
            }
        }

        // Release camera
        releaseCamera();

        return sizeAvailable;
    }

    /**
     * Start the camera preview
     */
    private void startCameraPreview() {
        if (camera != null) {
            // Camera settings
            Camera.Parameters p = camera.getParameters();
            p.setPreviewFormat(PixelFormat.YCbCr_420_SP);

            // Orientation
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
                Display display = ((WindowManager)getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
                switch (display.getRotation()) {
                    case Surface.ROTATION_0:
                        if (openedCameraId == CameraOptions.FRONT) {
                            outgoingPlayer.setOrientation(Orientation.ROTATE_90_CCW);
                        } else {
                            outgoingPlayer.setOrientation(Orientation.ROTATE_90_CW);
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
                            camera.setDisplayOrientation(90);
                        } else {
                            p.setRotation(90);
                        }
                        break;
                    case Surface.ROTATION_90:
                        outgoingPlayer.setOrientation(Orientation.NONE);
                        break;
                    case Surface.ROTATION_180:
                        if (openedCameraId == CameraOptions.FRONT) {
                            outgoingPlayer.setOrientation(Orientation.ROTATE_90_CW);
                        } else {
                            outgoingPlayer.setOrientation(Orientation.ROTATE_90_CCW);
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
                            camera.setDisplayOrientation(270);
                        } else {
                            p.setRotation(270);
                        }
                        break;
                    case Surface.ROTATION_270:
                        if (openedCameraId == CameraOptions.FRONT) {
                            outgoingPlayer.setOrientation(Orientation.ROTATE_180);
                        } else {
                            outgoingPlayer.setOrientation(Orientation.ROTATE_180);
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
                            camera.setDisplayOrientation(180);
                        } else {
                            p.setRotation(180);
                        }
                        break;
                }
            } else {
                // getRotation not managed under Froyo
                outgoingPlayer.setOrientation(Orientation.NONE);
            }

            // Camera size
            List<Camera.Size> sizes = p.getSupportedPreviewSizes();
            if (sizeContains(sizes, outgoingWidth, outgoingHeight)) {
                // Use the existing size without resizing
                p.setPreviewSize(outgoingWidth, outgoingHeight);
                outgoingPlayer.activateResizing(outgoingWidth, outgoingHeight); // same size = no resizing
            } else {
                // Check if can use a other known size (QVGA, CIF or VGA)
                int w = 0;
                int h = 0;
                for (Camera.Size size:sizes) {
                    w = size.width;
                    h = size.height;
                    if (    (w == H264Config.QVGA_WIDTH && h == H264Config.QVGA_HEIGHT) ||
                            (w == H264Config.CIF_WIDTH && h == H264Config.CIF_HEIGHT) ||
                            (w == H264Config.VGA_WIDTH && h == H264Config.VGA_HEIGHT)) {
                        break;
                    }
                }

                if (w != 0) {
                    p.setPreviewSize(w, h);
                    outgoingPlayer.activateResizing(w, h);
                } else {
                    // The camera don't have known size, we can't use it
                    camera = null;
                    return;
                }
            }

            camera.setParameters(p);
            try {
                camera.setPreviewDisplay(outgoingVideoView.getHolder());
                camera.startPreview();
                cameraPreviewRunning = true;
            } catch (Exception e) {
                camera = null;
            }
        }
    }

    /**
     * Release the camera
     */
    private void releaseCamera() {
        if (camera != null) {
            camera.setPreviewCallback(null);
            if (cameraPreviewRunning) {
                cameraPreviewRunning = false;
                camera.stopPreview();
            }
            camera.release();
            camera = null;
        }
    }

    /**
     * Test if size is in list.
     * Can't use List.contains because it doesn't work with some devices.
     *
     * @param list
     * @param width
     * @param height
     * @return boolean
     */
    private boolean sizeContains(List<Camera.Size> list, int width, int height) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).width == width && list.get(i).height == height) {
                return true;
            }
        }
        return false;
    }

    /**
     * Start the camera
     */
    private void startCamera() {
        if (camera == null) {
            // Open camera
            OpenCamera(openedCameraId);
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                outgoingVideoView.setAspectRatio(outgoingWidth, outgoingHeight);
            } else {
                outgoingVideoView.setAspectRatio(outgoingHeight, outgoingWidth);
            }
            // Start camera
            camera.setPreviewCallback(outgoingPlayer);
            startCameraPreview();
        }
    }

    /**
     * ReStart the camera
     */
    private void reStartCamera() {
        if (camera != null) {
            releaseCamera();
        }
        startCamera();
    }

    /* *****************************************
     *          SurfaceHolder.Callback
     ***************************************** */

    @Override
    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
        isSurfaceCreated = true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder arg0) {
        isSurfaceCreated = true;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder arg0) {
        isSurfaceCreated = false;
    }

    /* *****************************************
     *            Intent BroadcastReceiver
     ***************************************** */

    /**
     * Intent BroadcastReceiver
     */
    private BroadcastReceiver intentReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, final Intent intent) {
            handler.post(new Runnable() {
                public void run() {
                    extractIncomingInfo(intent);
                }
            });
        }
    };

    /* *****************************************
     *              Incoming session
     ***************************************** */

    /**
     * Extract incoming session information from intent
     *
     * @param intent
     */
    private void extractIncomingInfo(Intent intent) {
        String lastIncomingSessionId = incomingSessionId;
        incomingSessionId = intent.getStringExtra("sessionId");

        // Get incoming session
        if (lastIncomingSessionId != incomingSessionId) {
            handler.post(receiveIncomingSessionRunnable);
        }
        // Remove notification
        VideoSharingInvitationReceiver.removeVideoSharingNotification(getApplicationContext(), incomingSessionId);
    }

    /**
     * Runnable to Receive incoming session
     */
    private Runnable receiveIncomingSessionRunnable = new Runnable() {
        private int delay = 0;
        
        @Override
        public void run() {
            if (isApiConnected) {
                try {
                	// Create the video renderer
                    incomingRenderer = new MyVideoRenderer(incomingVideoView);

                    // Get the video sharing session
                    incomingCshSession = vshApi.getVideoSharing(incomingSessionId);
                    incomingCshSession.addEventListener(incomingSessionEventListener);
                    showReceiveNotification();
    		    } catch(JoynServiceNotAvailableException e) {
    		    	e.printStackTrace();
    				Utils.showMessageAndExit(VisioSharing.this, getString(R.string.label_api_disabled));
    		    } catch(JoynServiceException e) {
    		    	e.printStackTrace();
    				Utils.showMessageAndExit(VisioSharing.this, getString(R.string.label_api_failed));
    			}
            } else {
                delay += 200;
                handler.removeCallbacks(this);
                if (delay < 2000) {
                    handler.postDelayed(this, delay);
                }
            }
        }
    };


    /**
     * Show Incoming alert dialog 
     */
    private void showReceiveNotification() {
        // User alert
        pendingIncomingNotification = true;
        incomingAlert = new AlertDialog.Builder(this)
                .setTitle(R.string.title_recv_video_sharing)
                .setMessage(getString(R.string.label_from) + " " + remoteContact)
                .setCancelable(false)
                .setIcon(R.drawable.ri_notif_csh_icon)
                .setPositiveButton(getString(R.string.label_accept), acceptBtnListener)
                .setNegativeButton(getString(R.string.label_decline), declineBtnListener)
                .show();
    }

    /**
     * Runnable to Continue incoming session
     */
    private Runnable continueIncomingSessionRunnable = new Runnable() {
        private int delay = 0;

        @Override
        public void run() {
            if (isApiConnected) {
                try {
                	// Create the video renderer
                    incomingRenderer = new MyVideoRenderer(incomingVideoView);

                    // Get the video sharing session
                    incomingCshSession = vshApi.getVideoSharing(incomingSessionId);
                    incomingCshSession.addEventListener(incomingSessionEventListener);
    		    } catch(JoynServiceNotAvailableException e) {
    		    	e.printStackTrace();
    				Utils.showMessageAndExit(VisioSharing.this, getString(R.string.label_api_disabled));
    		    } catch(JoynServiceException e) {
    		    	e.printStackTrace();
    				Utils.showMessageAndExit(VisioSharing.this, getString(R.string.label_api_failed));
    			}
            } else {
                delay += 200;
                handler.removeCallbacks(this);
                if (delay < 2000) {
                    handler.postDelayed(this, delay);
                }
            }
        }
    };

    /**
     * Stop the incoming session
     *
     * @param boolean cancel or not the session
     */
    private void stopIncomingSession(boolean cancel) {
        if (pendingIncomingSession && incomingCshSession != null) {
            try {
                incomingCshSession.removeEventListener(incomingSessionEventListener);
                if (cancel) {
                    incomingCshSession.abortSharing();
                }
            } catch (Exception e) {
            	e.printStackTrace();
            }
            handler.post(new Runnable() {
                public void run() {
                    incomingBtn.setEnabled(false);
                }
            });
            pendingIncomingSession = false;
            incomingCshSession = null;
        }
    }

    /**
     * Accept incoming session.
     */
    private void acceptIncomingSession() {
        // Accept the session in background
        new Thread() {
            public void run() {
                try {
                    // Accept the invitation
                    incomingCshSession.acceptInvitation(incomingRenderer);
                } catch (Exception e) {
                    handler.post(new Runnable() {
                        public void run() {
                            Utils.showMessageAndExit(VisioSharing.this, getString(R.string.label_invitation_failed));
                        }
                    });
                }
            }
        }.start();
    }

    /**
     * Decline incoming session.
     */
    private void declineIncomingSession() {
        new Thread() {
            public void run() {
                try {
                    // Reject the invitation
                    incomingCshSession.rejectInvitation();
                    incomingCshSession = null;

                    // Exit activity if no session
                    exitIfNoSession(null);
                } catch (Exception e) {
                }
            }
        }.start();
    }

    /**
     * Incoming video sharing session event listener
     */
    private class IncomingVideoSharingListener extends VideoSharingListener {
        // Sharing is started
        public void onSharingStarted() {
            handler.post(new Runnable() {
                public void run() {
                    pendingIncomingSession = true;
                    incomingBtn.setEnabled(true);
                }
            });
        }

        // Sharing has been aborted
        public void onSharingAborted() {
            handler.post(new Runnable() {
                public void run() {
                    stopIncomingSession(false);
                    exitIfNoSession(getString(R.string.label_incoming_sharing_aborted));
                }
            });
        }

        // Sharing error
        public void onSharingError(final int error) {
            handler.post(new Runnable() {
                public void run() {
                    stopIncomingSession(false);
                    exitIfNoSession(getString(R.string.label_csh_failed, error));
                }
            });

        }
    };

    /* *****************************************
     *              Outgoing session
     ***************************************** */

    /**
     * Runnable to start outgoing session
     */
    private Runnable startOutgoingSessionRunnable = new Runnable() {
        private int delay = 0;
        @Override
        public void run() {
            if (isSurfaceCreated && isApiConnected) {
            	// Create the video player
                outgoingPlayer = new MyVideoPlayer();

                // Start camera
                startCamera();

                // Start outgoing session
                startOutgoingSession();
            } else {
                delay += 200;
                handler.removeCallbacks(this);
                if (delay < 2000) {
                    handler.postDelayed(this, delay);
                }
            }
        }
    };

    /**
     * Start the outgoing session
     */
    private void startOutgoingSession() {
        // Initiate session
        new Thread() {
            public void run() {
                try {
                    // Initiate sharing
                    outgoingCshSession = vshApi.shareVideo(remoteContact, outgoingPlayer, outgoingSessionEventListener);
                    outgoingSessionId = outgoingCshSession.getSharingId();
                } catch (Exception e) {
                    handler.post(new Runnable() {
                        public void run() {
                            Utils.showMessageAndExit(VisioSharing.this, getString(R.string.label_invitation_failed));
                        }
                    });
                }
            }
        }.start();

        pendingOutgoingSession = true;
        outgoingBtn.setText(R.string.label_stop_outgoing_btn);
        switchCamBtn.setEnabled(true);

        // Display a progress dialog
        outgoingProgressDialog = Utils.showProgressDialog(VisioSharing.this, getString(R.string.label_command_in_progress));
        outgoingProgressDialog.setOnCancelListener(new OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                Toast.makeText(VisioSharing.this, getString(R.string.label_video_sharing_canceled), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    /**
     * Runnable to continue outgoing session
     */
    private Runnable continueOutgoingSessionRunnable = new Runnable() {
        private int delay = 0;
        @Override
        public void run() {
            if (isSurfaceCreated && isApiConnected) {
                // Start outgoing session
                continueOutgoingSession();
            } else {
                delay += 200;
                handler.removeCallbacks(this);
                if (delay < 2000) {
                    handler.postDelayed(this, delay);
                }
            }
        }
    };

    /**
     * Continue a pending outgoing session
     */
    private void continueOutgoingSession() {
        try {
            // continue sharing
            outgoingCshSession = vshApi.getVideoSharing(outgoingSessionId);
            outgoingCshSession.addEventListener(outgoingSessionEventListener);
            // TODO outgoingPlayer = (LiveVideoPlayer)outgoingCshSession.getVideoPlayer();

            // Start camera
            startCamera();
        } catch (Exception e) {
            handler.post(new Runnable() {
                public void run() {
                    Utils.showMessageAndExit(VisioSharing.this, getString(R.string.label_outgoing_sharing_aborted));
                }
            });
        }
    }

    /**
     * Stop the outgoing session
     *
     * @param boolean cancel the session or not
     */
    private void stopOutgoingSession(final boolean cancel) {
        if (outgoingCshSession != null && pendingOutgoingSession) {
            hideProgressDialog();
            new Thread() {
                public void run() {
                    try {
                        outgoingCshSession.removeEventListener(outgoingSessionEventListener);
                        if (cancel) {
                            outgoingCshSession.abortSharing();
                        }
                    } catch(Exception e) {
                    	e.printStackTrace();
                    }
                }
            }.start();
            releaseCamera();
            outgoingCshSession = null;
            pendingOutgoingSession = false;
            handler.post(new Runnable() {
                public void run() {
                    outgoingBtn.setText(R.string.label_start_outgoing_btn);
                    switchCamBtn.setEnabled(false);
                }
            });
        }
    }

    /**
     * Hide progress dialog
     */
    private void hideProgressDialog() {
        if (outgoingProgressDialog != null && outgoingProgressDialog.isShowing()) {
            outgoingProgressDialog.dismiss();
            outgoingProgressDialog = null;
        }
    }

    /**
     * Recreate the video player
     */
    private void recreateVideoPlayer(){
        // Create the video player
        outgoingPlayer = new MyVideoPlayer();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            outgoingVideoView.setAspectRatio(outgoingWidth, outgoingHeight);
        } else {
            outgoingVideoView.setAspectRatio(outgoingHeight, outgoingWidth);
        }
        surface = outgoingVideoView.getHolder();
        surface.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surface.addCallback(this);
        if (camera != null) {
            camera.setPreviewCallback(outgoingPlayer);
        }
    }

    /**
     * Outgoing video sharing session event listener
     */
    private class OutgoingVideoSharingListener extends VideoSharingListener {
        // Sharing is started
        public void onSharingStarted() {
            handler.post(new Runnable() {
                public void run() {
                    // Update Camera
                    outgoingHeight = outgoingPlayer.getMediaCodecHeight();
                    outgoingWidth = outgoingPlayer.getMediaCodecWidth();
                    reStartCamera();

                    // Hide progress bar
                    hideProgressDialog();
                }
            });
        }

        // Sharing has been aborted
        public void onSharingAborted() {
            handler.post(new Runnable() {
                public void run() {
                    hideProgressDialog();
                    stopOutgoingSession(false);
                    exitIfNoSession(getString(R.string.label_outgoing_sharing_aborted));

                    recreateVideoPlayer();
                }
            });
        }

        // Sharing error
        public void onSharingError(final int error) {
            handler.post(new Runnable() {
                public void run() {
                    hideProgressDialog();
                    stopOutgoingSession(false);
                    if (error == VideoSharing.Error.INVITATION_DECLINED) {
                        exitIfNoSession(getString(R.string.label_sharing_declined));
                    } else {
                        exitIfNoSession(getString(R.string.label_csh_failed, error));
                    }

                    recreateVideoPlayer();
                }
            });
        }
    };

    /**
     * Exit activity if all sessions are stopped.
     *
     * @param message the message to display. Can be null for no message.
     */
    private void exitIfNoSession(String message) {
        if ((outgoingCshSession == null) && (incomingCshSession == null)) {
            if (message == null) {
                finish();
            } else {
                Utils.showMessageAndExit(VisioSharing.this, message);
            }
        } else {
            if (message != null) {
                Utils.showMessage(VisioSharing.this, message);
            }
        }
    }
}
