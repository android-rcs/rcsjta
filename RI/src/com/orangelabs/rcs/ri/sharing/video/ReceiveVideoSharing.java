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

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.vsh.VideoSharing;
import com.gsma.services.rcs.vsh.VideoSharingListener;
import com.gsma.services.rcs.vsh.VideoSharingService;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.H264Config;
import com.orangelabs.rcs.ri.ApiConnectionManager;
import com.orangelabs.rcs.ri.ApiConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.sharing.video.media.MyVideoRenderer;
import com.orangelabs.rcs.ri.sharing.video.media.VideoSurfaceView;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsDisplayName;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Receive video sharing
 *  
 * @author Jean-Marc AUFFRET
 * @author YPLO6403
 */
public class ReceiveVideoSharing extends Activity {

	/**
	 * UI handler
	 */
	private final Handler handler = new Handler();
	
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
    
	/**
	 * A locker to exit only once
	 */
	private LockAccess exitOnce = new LockAccess();
	
   	/**
	 * API connection manager
	 */
	private ApiConnectionManager connectionManager;
    
    /**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(ReceiveVideoSharing.class.getSimpleName());
	
   /**
     * Video sharing listener
     */
    private VideoSharingListener vshListener = new VideoSharingListener() {

		@Override
		public void onStateChanged(ContactId contact, String sharingId, final int state, int reasonCode) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onVideoSharingStateChanged contact=" + contact + " sharingId=" + sharingId + " state=" + state
						+ " reason=" + reasonCode);
			}
			if (state > RiApplication.VSH_STATES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onVideoSharingStateChanged unhandled state=" + state);
				}
				return;
			}
			if (reasonCode > RiApplication.VSH_REASON_CODES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onVideoSharingStateChanged unhandled reason=" + reasonCode);
				}
				return;
			}
			// Discard event if not for current sharingId
			if (ReceiveVideoSharing.this.vshDao == null || !ReceiveVideoSharing.this.vshDao.getSharingId().equals(sharingId)) {
				return;
			}
			final String _reasonCode = RiApplication.VSH_REASON_CODES[reasonCode];
			handler.post(new Runnable() {
				public void run() {
					switch (state) {
					case VideoSharing.State.STARTED:
						// Session is established
						break;

					case VideoSharing.State.ABORTED:
						// Display session status
						Utils.showMessageAndExit(ReceiveVideoSharing.this, getString(R.string.label_sharing_aborted, _reasonCode),
								exitOnce);
						break;

					case VideoSharing.State.FAILED:
						// Session is failed: exit
						Utils.showMessageAndExit(ReceiveVideoSharing.this, getString(R.string.label_sharing_failed, _reasonCode),
								exitOnce);
						break;

					case VideoSharing.State.REJECTED:
						// Session is rejected: exit
						Utils.showMessageAndExit(ReceiveVideoSharing.this, getString(R.string.label_sharing_rejected, _reasonCode),
								exitOnce);
						break;
					
					default:
						if (LogUtils.isActive) {
							Log.d(LOGTAG,
									"onVideoSharingStateChanged "
											+ getString(R.string.label_vsh_state_changed, RiApplication.VSH_STATES[state],
													_reasonCode));
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

		// Register to API connection manager
		connectionManager = ApiConnectionManager.getInstance(this);
		if (connectionManager == null || !connectionManager.isServiceConnected(RcsServiceName.VIDEO_SHARING, RcsServiceName.CONTACTS)) {
			Utils.showMessageAndExit(this, getString(R.string.label_service_not_available), exitOnce);
		} else {
			connectionManager.startMonitorServices(this, exitOnce, RcsServiceName.VIDEO_SHARING, RcsServiceName.CONTACTS);
			initiateVideoSharing();
		}
    }

    @Override
	public void onDestroy() {
		super.onDestroy();
		if (connectionManager == null) {
			return;
		}
		connectionManager.stopMonitorServices(this);
		if (connectionManager.isServiceConnected(RcsServiceName.VIDEO_SHARING)) {
			// Remove video sharing listener
			try {
				if (LogUtils.isActive) {
					Log.d(LOGTAG, "onDestroy Remove listener");
				}
				connectionManager.getVideoSharingApi().removeEventListener(vshListener);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

    public void initiateVideoSharing() {
    	VideoSharingService vshApi = connectionManager.getVideoSharingApi();
		try {
			// Add service listener
			vshApi.addEventListener(vshListener);
			
			// Get the video sharing
			videoSharing = vshApi.getVideoSharing(vshDao.getSharingId());
			if (videoSharing == null) {
				// Session not found or expired
				Utils.showMessageAndExit(this, getString(R.string.label_session_not_found), exitOnce);
				return;
			}
			
			ContactId remote = vshDao.getContact();
			String from = RcsDisplayName.getInstance(this).getDisplayName(remote);
			
	    	// Display sharing infos
    		TextView fromTextView = (TextView)findViewById(R.id.from);
    		fromTextView.setText(getString(R.string.label_from_args, from));
	    	
			// Display accept/reject dialog
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.title_video_sharing);
			builder.setMessage(getString(R.string.label_from_args, from));
			builder.setCancelable(false);
			builder.setIcon(R.drawable.ri_notif_csh_icon);
			builder.setPositiveButton(getString(R.string.label_accept), acceptBtnListener);
			builder.setNegativeButton(getString(R.string.label_decline), declineBtnListener);
			builder.show();
	    } catch(RcsServiceNotAvailableException e) {
	    	e.printStackTrace();
			Utils.showMessageAndExit(this, getString(R.string.label_api_disabled), exitOnce);
	    } catch(RcsServiceException e) {
	    	e.printStackTrace();
			Utils.showMessageAndExit(this, getString(R.string.label_api_failed), exitOnce);
		}
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
			Utils.showMessageAndExit(this, getString(R.string.label_invitation_failed), exitOnce);
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

