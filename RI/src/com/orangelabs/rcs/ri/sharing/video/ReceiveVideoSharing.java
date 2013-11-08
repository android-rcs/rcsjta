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

import org.gsma.joyn.JoynService;
import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.JoynServiceListener;
import org.gsma.joyn.JoynServiceNotAvailableException;
import org.gsma.joyn.ish.ImageSharingIntent;
import org.gsma.joyn.vsh.VideoSharing;
import org.gsma.joyn.vsh.VideoSharingListener;
import org.gsma.joyn.vsh.VideoSharingService;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import android.widget.TextView;

import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.H264Config;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.sharing.video.media.MyVideoRenderer;
import com.orangelabs.rcs.ri.sharing.video.media.VideoSurfaceView;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Receive video sharing
 *  
 * @author Jean-Marc AUFFRET
 */
public class ReceiveVideoSharing extends Activity implements JoynServiceListener {

	/**
	 * UI handler
	 */
	private final Handler handler = new Handler();

	/**
	 * Sharing ID
	 */
    private String sharingId;
    
    /**
     * Remote Contact
     */
    private String remoteContact;
    
    /**
	 * Video sharing API
	 */
	private VideoSharingService vshApi;
	
	/**
	 * Video sharing
	 */
	private VideoSharing videoSharing = null;

    /**
     * Video sharing listener
     */
    private MyVideoSharingListener vshListener = new MyVideoSharingListener();    
    
    /**
     * Video renderer
     */
    private MyVideoRenderer videoRenderer = null;

    /**
     * Video width
     */
    private int videoWidth = H264Config.QCIF_WIDTH;
    
    /**
     * Video height
     */
    private int videoHeight = H264Config.QCIF_HEIGHT;
    
    /**
     * Live video preview
     */
    private VideoSurfaceView videoView;
    
    /**
     * Video surface holder
     */
    private SurfaceHolder surface = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Always on window
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.video_sharing_receive);

        // Set title
        setTitle(R.string.menu_video_sharing);

        // Get invitation info
        sharingId = getIntent().getStringExtra(ImageSharingIntent.EXTRA_SHARING_ID);
		remoteContact = getIntent().getStringExtra(ImageSharingIntent.EXTRA_CONTACT);

        // Create the live video view
        videoView = (VideoSurfaceView)findViewById(R.id.video_view);
        videoView.setAspectRatio(videoWidth, videoHeight);
        surface = videoView.getHolder();
        surface.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surface.setKeepScreenOn(true);

        // Instanciate the renderer
        videoRenderer = new MyVideoRenderer(videoView);

		// Instanciate API
        vshApi = new VideoSharingService(getApplicationContext(), this);
		
		// Connect API
        vshApi.connect();                
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();
    	
        // Remove file transfer listener
        if (videoSharing != null) {
        	try {
        		videoSharing.removeEventListener(vshListener);
        	} catch(Exception e) {
        	}
        }    	
    	
        // Disconnect API
        vshApi.disconnect();
    }

    /**
     * Callback called when service is connected. This method is called when the
     * service is well connected to the RCS service (binding procedure successfull):
     * this means the methods of the API may be used.
     */
    public void onServiceConnected() {
		try{
			// Get the video sharing
			videoSharing = vshApi.getVideoSharing(sharingId);
			if (videoSharing == null) {
				// Session not found or expired
				Utils.showMessageAndExit(ReceiveVideoSharing.this, getString(R.string.label_session_not_found));
				return;
			}
			videoSharing.addEventListener(vshListener);
			
	    	// Display sharing infos
    		TextView from = (TextView)findViewById(R.id.from);
	        from.setText(getString(R.string.label_from) + " " + remoteContact);
	    	
			// Display accept/reject dialog
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.title_video_sharing);
			builder.setMessage(getString(R.string.label_from) +	" " + remoteContact);
			builder.setCancelable(false);
			builder.setIcon(R.drawable.ri_notif_csh_icon);
			builder.setPositiveButton(getString(R.string.label_accept), acceptBtnListener);
			builder.setNegativeButton(getString(R.string.label_decline), declineBtnListener);
			builder.show();
	    } catch(JoynServiceNotAvailableException e) {
	    	e.printStackTrace();
			Utils.showMessageAndExit(ReceiveVideoSharing.this, getString(R.string.label_api_disabled));
	    } catch(JoynServiceException e) {
	    	e.printStackTrace();
			Utils.showMessageAndExit(ReceiveVideoSharing.this, getString(R.string.label_api_failed));
		}
    }
    
    /**
     * Callback called when service has been disconnected. This method is called when
     * the service is disconnected from the RCS service (e.g. service deactivated).
     * 
     * @param error Error
     * @see JoynService.Error
     */
    public void onServiceDisconnected(int error) {
		Utils.showMessageAndExit(ReceiveVideoSharing.this, getString(R.string.label_api_disabled));
    }    
    
    /**
	 * Accept invitation
	 */
	private void acceptInvitation() {
    	Thread thread = new Thread() {
        	public void run() {
            	try {
            		// Accept the invitation
            		videoSharing.acceptInvitation(videoRenderer);
            	} catch(Exception e) {
        			handler.post(new Runnable() { 
        				public void run() {
        					Utils.showMessageAndExit(ReceiveVideoSharing.this, getString(R.string.label_invitation_failed));
						}
	    			});
            	}
        	}
        };
        thread.start();
	}
	
	/**
	 * Reject invitation
	 */
	private void rejectInvitation() {
        Thread thread = new Thread() {
        	public void run() {
            	try {
            		// Reject the invitation
            		videoSharing.removeEventListener(vshListener);
            		videoSharing.rejectInvitation();
            	} catch(Exception e) {
            	}
        	}
        };
        thread.start();
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
     * Video sharing event listener
     */
    private class MyVideoSharingListener extends VideoSharingListener {
    	// Sharing started
    	public void onSharingStarted() {
			handler.post(new Runnable() { 
				public void run() {
					// TODO
				}
			});
    	}
    	
    	// Sharing aborted
    	public void onSharingAborted() {
			handler.post(new Runnable() { 
				public void run() {
					// Display session status
					Utils.showMessageAndExit(ReceiveVideoSharing.this, getString(R.string.label_sharing_aborted));
				}
			});
    	}

    	// Sharing error
    	public void onSharingError(final int error) {
			handler.post(new Runnable() { 
				public void run() {
					// Display error
                    Utils.showMessageAndExit(ReceiveVideoSharing.this,
                            getString(R.string.label_sharing_failed, error));
				}
			});
    	}
    };

    /**
     * Quit the session
     */
    private void quitSession() {
		// Stop session
	    Thread thread = new Thread() {
	    	public void run() {
            	// Stop the sharing
            	try {
	                if (videoSharing != null) {
	                	videoSharing.removeEventListener(vshListener);
	                	videoSharing.abortSharing();
	                }
	        	} catch(Exception e) {
	        	}
	        	videoSharing = null;
	    	}
	    };
	    thread.start();
		
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
		MenuInflater inflater=new MenuInflater(getApplicationContext());
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
}

