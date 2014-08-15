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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import android.widget.TextView;

import com.gsma.services.rcs.JoynService;
import com.gsma.services.rcs.JoynServiceException;
import com.gsma.services.rcs.JoynServiceListener;
import com.gsma.services.rcs.JoynServiceNotAvailableException;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.vsh.VideoSharing;
import com.gsma.services.rcs.vsh.VideoSharingListener;
import com.gsma.services.rcs.vsh.VideoSharingService;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.H264Config;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.sharing.video.media.MyVideoRenderer;
import com.orangelabs.rcs.ri.sharing.video.media.VideoSurfaceView;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Receive video sharing
 *  
 * @author Jean-Marc AUFFRET
 * @author YPLO6403
 */
public class ReceiveVideoSharing extends Activity implements JoynServiceListener {

	/**
	 * UI handler
	 */
	private final Handler handler = new Handler();

    /**
	 * Video sharing API
	 */
	private VideoSharingService vshApi;
	
	/**
	 * Video sharing
	 */
	private VideoSharing videoSharing;
	
    /**
     * The Video Sharing Data Object 
     */
    VideoSharingDAO vshDao;

    /**
     * Video renderer
     */
    private MyVideoRenderer videoRenderer;

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
    private SurfaceHolder surface;
    
    private boolean serviceConnected = false;
    
	/**
	 * A locker to exit only once
	 */
	private LockAccess exitOnce = new LockAccess();
    
    /**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(ReceiveVideoSharing.class.getSimpleName());
	
	/**
	 * Array of Video sharing states
	 */
	private static final String[] VSH_STATES = RiApplication.getContext().getResources().getStringArray(R.array.vsh_states);
	
	/**
	 * Array of Video sharing reason codes
	 */
	private static final String[] VSH_REASON_CODES = RiApplication.getContext().getResources()
			.getStringArray(R.array.vsh_reason_codes);
	
	   /**
     * Video sharing listener
     */
    private VideoSharingListener vshListener = new VideoSharingListener() {

		@Override
		public void onVideoSharingStateChanged(ContactId contact, String sharingId, final int state) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onVideoSharingStateChanged contact=" + contact + " sharingId=" + sharingId + " state=" + state);
			}
			if (state > VSH_STATES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onVideoSharingStateChanged unhandled state=" + state);
				}
				return;
			}
			// TODO : handle reason code (CR025)
			final String reason = VSH_REASON_CODES[0];
			final String notif = getString(R.string.label_vsh_state_changed, VSH_STATES[state], reason);
			handler.post(new Runnable() {
				public void run() {
					switch (state) {
					case VideoSharing.State.STARTED:
						// Session is established
						break;

					case VideoSharing.State.ABORTED:
						// Display session status
						Utils.showMessageAndExit(ReceiveVideoSharing.this, getString(R.string.label_sharing_aborted, reason),
								exitOnce);
						break;

					case VideoSharing.State.FAILED:
						// Session is failed: exit
						Utils.showMessageAndExit(ReceiveVideoSharing.this, getString(R.string.label_sharing_failed, reason),
								exitOnce);
						break;

					case VideoSharing.State.TERMINATED:
						// Session is failed: exit
						Utils.showMessageAndExit(ReceiveVideoSharing.this, getString(R.string.label_vsh_terminated), exitOnce);
						break;

					default:
						if (LogUtils.isActive) {
							Log.d(LOGTAG, "onVideoSharingStateChanged " + notif);
						}
					}
				}
			});
		}
	};
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Always on window
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        // Set layout
        setContentView(R.layout.video_sharing_receive);

        // Set title
        setTitle(R.string.menu_video_sharing);

        // Get invitation info
        vshDao = (VideoSharingDAO) (getIntent().getExtras().getParcelable(VideoSharingIntentService.BUNDLE_VSHDAO_ID));
		if (vshDao == null) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "onCreate cannot read Video Sharing invitation");
			}
			finish();
			return;
		}

		if (LogUtils.isActive) {
			Log.d(LOGTAG, "onCreate "+vshDao);
		}
		
        // Create the live video view
        videoView = (VideoSurfaceView)findViewById(R.id.video_view);
        videoView.setAspectRatio(videoWidth, videoHeight);
        surface = videoView.getHolder();
        surface.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surface.setKeepScreenOn(true);

        // Instantiate the renderer
        videoRenderer = new MyVideoRenderer(videoView);

		// Instantiate API
        vshApi = new VideoSharingService(getApplicationContext(), this);
		
		// Connect API
        vshApi.connect();
    }

    @Override
	public void onDestroy() {
		super.onDestroy();
		if (serviceConnected) {
			// Remove video sharing listener
			try {
				if (LogUtils.isActive) {
					Log.d(LOGTAG, "onDestroy Remove listener");
				}
				vshApi.removeEventListener(vshListener);
			} catch (Exception e) {
				e.printStackTrace();
			}
			// Disconnect API
			vshApi.disconnect();
		}
	}

    /**
     * Callback called when service is connected. This method is called when the
     * service is well connected to the RCS service (binding procedure successful):
     * this means the methods of the API may be used.
     */
    public void onServiceConnected() {
		try {
			// Add service listener
			vshApi.addEventListener(vshListener);
			serviceConnected = true;
			
			// Get the video sharing
			videoSharing = vshApi.getVideoSharing(vshDao.getSharingId());
			if (videoSharing == null) {
				// Session not found or expired
				Utils.showMessageAndExit(ReceiveVideoSharing.this, getString(R.string.label_session_not_found), exitOnce);
				return;
			}
			
	    	// Display sharing infos
    		TextView from = (TextView)findViewById(R.id.from);
	        from.setText(getString(R.string.label_from) + " " + vshDao.getContact());
	    	
			// Display accept/reject dialog
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.title_video_sharing);
			builder.setMessage(getString(R.string.label_from) +	" " + vshDao.getContact());
			builder.setCancelable(false);
			builder.setIcon(R.drawable.ri_notif_csh_icon);
			builder.setPositiveButton(getString(R.string.label_accept), acceptBtnListener);
			builder.setNegativeButton(getString(R.string.label_decline), declineBtnListener);
			builder.show();
	    } catch(JoynServiceNotAvailableException e) {
	    	e.printStackTrace();
			Utils.showMessageAndExit(ReceiveVideoSharing.this, getString(R.string.label_api_disabled), exitOnce);
	    } catch(JoynServiceException e) {
	    	e.printStackTrace();
			Utils.showMessageAndExit(ReceiveVideoSharing.this, getString(R.string.label_api_failed), exitOnce);
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
    	serviceConnected = false;
		Utils.showMessageAndExit(ReceiveVideoSharing.this, getString(R.string.label_api_disabled), exitOnce);
    }    
    
    /**
	 * Accept invitation
	 */
	private void acceptInvitation() {
    	try {
    		if (LogUtils.isActive) {
				Log.d(LOGTAG, "acceptInvitation");
			}
    		// Accept the invitation
    		videoSharing.acceptInvitation(videoRenderer);
    	} catch(Exception e) {
    		e.printStackTrace();
			Utils.showMessageAndExit(ReceiveVideoSharing.this, getString(R.string.label_invitation_failed), exitOnce);
    	}
	}
	
	/**
	 * Reject invitation
	 */
	private void rejectInvitation() {
    	try {
    		// Reject the invitation
    		videoSharing.rejectInvitation();
    	} catch(Exception e) {
    		e.printStackTrace();
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
    	// Stop the sharing
    	try {
            if (videoSharing != null) {
            	videoSharing.abortSharing();
            }
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	videoSharing = null;
		
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

