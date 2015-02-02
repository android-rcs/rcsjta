/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
package com.orangelabs.rcs.ri.sharing.video;

import android.app.Activity;
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

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.vsh.VideoDescriptor;
import com.gsma.services.rcs.vsh.VideoSharing;
import com.gsma.services.rcs.vsh.VideoSharingListener;
import com.gsma.services.rcs.vsh.VideoSharingService;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.H264Config;
import com.orangelabs.rcs.ri.ApiConnectionManager;
import com.orangelabs.rcs.ri.ApiConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.sharing.video.media.TerminatingVideoPlayer;
import com.orangelabs.rcs.ri.sharing.video.media.VideoPlayerListener;
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
public class IncomingVideoSharing extends Activity implements VideoPlayerListener {

	/**
	 * UI handler
	 */
	private final Handler handler = new Handler();
	
	/**
	 * Video sharing
	 */
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
    
	/**
	 * A locker to exit only once
	 */
	private LockAccess exitOnce = new LockAccess();
	
   	/**
	 * API connection manager
	 */
	private ApiConnectionManager mCnxManager;
	
	private static final String SAVE_VIDEO_SHARING_DAO = "videoSharingDao";
	private static final String SAVE_WAIT_USER_ACCEPT = "waitUserAccept";
	
    private boolean mWaitForUseAcceptance = true;
    
    private AlertDialog mAcceptDeclineDialog;

    /**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(IncomingVideoSharing.class.getSimpleName());
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Always on window
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        // Set layout
        setContentView(R.layout.video_sharing_incoming);

        // Saved datas
        if (savedInstanceState == null) {
        	// Get invitation info
        	mVshDao = (VideoSharingDAO) (getIntent().getExtras().getParcelable(VideoSharingIntentService.BUNDLE_VSHDAO_ID));
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
        mVideoView = (VideoSurfaceView)findViewById(R.id.video_view);
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
		mCnxManager = ApiConnectionManager.getInstance(this);
		if (mCnxManager == null || !mCnxManager.isServiceConnected(RcsServiceName.VIDEO_SHARING, RcsServiceName.CONTACTS)) {
			Utils.showMessageAndExit(this, getString(R.string.label_service_not_available), exitOnce);
		} else {
			mCnxManager.startMonitorServices(this, exitOnce, RcsServiceName.VIDEO_SHARING, RcsServiceName.CONTACTS);
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
		
		if (mCnxManager == null) {
			return;
			
		}
		mCnxManager.stopMonitorServices(this);
		if (mCnxManager.isServiceConnected(RcsServiceName.VIDEO_SHARING)) {
			// Remove video sharing listener
			try {
				if (LogUtils.isActive) {
					Log.d(LOGTAG, "onDestroy Remove listener");
				}
				mCnxManager.getVideoSharingApi().removeEventListener(vshListener);
			} catch (Exception e) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "Failed to remove listener", e);
				}
			}
		}
	}

    @Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(SAVE_VIDEO_SHARING_DAO, mVshDao);
		outState.putBoolean(SAVE_WAIT_USER_ACCEPT, mWaitForUseAcceptance);
	};
	
    private void startOrRestartVideoSharing() {
    	VideoSharingService vshApi = mCnxManager.getVideoSharingApi();
		try {
			// Add service listener
			vshApi.addEventListener(vshListener);
			
			// Get the video sharing
			mVideoSharing = vshApi.getVideoSharing(mVshDao.getSharingId());
			if (mVideoSharing == null) {
				// Session not found or expired
				Utils.showMessageAndExit(this, getString(R.string.label_session_not_found), exitOnce);
				return;
				
			}
			
			ContactId remote = mVshDao.getContact();
			String from = RcsDisplayName.getInstance(this).getDisplayName(remote);
			
	    	// Display sharing information
    		TextView fromTextView = (TextView)findViewById(R.id.from);
    		fromTextView.setText(getString(R.string.label_from_args, from));
	    	
    		if (mWaitForUseAcceptance) {
    			showReceiveNotification(from);
    		} else {
    			displayVideoFormat();
    		}
	    } catch(RcsServiceNotAvailableException e) {
	    	if (LogUtils.isActive) {
				Log.e(LOGTAG, e.getMessage(), e);
			}
			Utils.showMessageAndExit(this, getString(R.string.label_api_disabled), exitOnce);
	    } catch(RcsServiceException e) {
	    	if (LogUtils.isActive) {
				Log.e(LOGTAG, e.getMessage(), e);
			}
			Utils.showMessageAndExit(this, getString(R.string.label_api_failed), exitOnce);
		}
    }
    
    /**
     * Show Incoming alert dialog 
	 * @param from
	 */
	private void showReceiveNotification(String from) {
		// User alert
		// Display accept/reject dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.title_video_sharing);
		builder.setMessage(getString(R.string.label_from_args, from));
		builder.setCancelable(false);
		builder.setIcon(R.drawable.ri_notif_csh_icon);
		builder.setPositiveButton(getString(R.string.label_accept), acceptBtnListener);
		builder.setNegativeButton(getString(R.string.label_decline), declineBtnListener);
		mAcceptDeclineDialog = builder.show();
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
    		mVideoSharing.acceptInvitation(mVideoRenderer);
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
    		if (LogUtils.isActive) {
				Log.d(LOGTAG, "rejectInvitation");
			}
    		// Reject the invitation
    		mVideoSharing.rejectInvitation();
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
	}	
	
	/**
     * Accept button listener
     */
    private OnClickListener acceptBtnListener = new OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
        	mAcceptDeclineDialog = null;
        	mWaitForUseAcceptance = false;
        	// Accept invitation
        	acceptInvitation();
        }
    };

    /**
     * Reject button listener
     */    
    private OnClickListener declineBtnListener = new OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
        	mAcceptDeclineDialog = null;
        	mWaitForUseAcceptance = false;
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
            if (mVideoSharing != null) {
            	mVideoSharing.abortSharing();
            }
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	mVideoSharing = null;
		
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
    
    /*-------------------------- Session callbacks ------------------*/
    
    /**
     * Video sharing listener
     */
    private VideoSharingListener vshListener = new VideoSharingListener() {

		@Override
		public void onStateChanged(ContactId contact, String sharingId, final int state, int reasonCode) {
			String _state = Integer.valueOf(state).toString();
			String _reason = Integer.valueOf(reasonCode).toString();
			if (LogUtils.isActive) {
				Log.d(LOGTAG,
						new StringBuilder("onStateChanged contact=").append(contact)
								.append(" sharingId=").append(sharingId).append(" state=")
								.append(_state).append(" reason=").append(_reason).toString());
			}
			if (state > RiApplication.VSH_STATES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onStateChanged unhandled state=".concat(_state));
				}
				return;
				
			}
			if (reasonCode > RiApplication.VSH_REASON_CODES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onStateChanged unhandled reason=".concat(_reason));
				}
				return;
				
			}
			// Discard event if not for current sharingId
			if (mVshDao == null || !mVshDao.getSharingId().equals(sharingId)) {
				return;
				
			}
			final String _reasonCode = RiApplication.VSH_REASON_CODES[reasonCode];
			handler.post(new Runnable() {
				public void run() {
					switch (state) {
					case VideoSharing.State.STARTED:
						displayVideoFormat();

						// Start the renderer
						mVideoRenderer.open();
						mVideoRenderer.start();
						break;

					case VideoSharing.State.ABORTED:
						// Stop the renderer
						mVideoRenderer.stop();
						mVideoRenderer.close();
						
						// Display session status
						Utils.showMessageAndExit(IncomingVideoSharing.this, getString(R.string.label_sharing_aborted, _reasonCode),
								exitOnce);
						break;

					case VideoSharing.State.FAILED:
						// Stop the renderer
						mVideoRenderer.stop();
						mVideoRenderer.close();						

						// Session is failed: exit
						Utils.showMessageAndExit(IncomingVideoSharing.this, getString(R.string.label_sharing_failed, _reasonCode),
								exitOnce);
						break;

					case VideoSharing.State.REJECTED:
						// Stop the renderer
						mVideoRenderer.stop();
						mVideoRenderer.close();									

						// Session is rejected: exit
						Utils.showMessageAndExit(IncomingVideoSharing.this, getString(R.string.label_sharing_rejected, _reasonCode),
								exitOnce);
						break;
					
					default:
						if (LogUtils.isActive) {
							Log.d(LOGTAG, "onStateChanged ".concat(getString(
									R.string.label_vsh_state_changed,
									RiApplication.VSH_STATES[state], _reasonCode)));
						}
					}
				}
			});
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
		if (LogUtils.isActive) {
			Log.d(LOGTAG,
					new StringBuilder("onPlayerResized ").append(width).append("x").append(height)
							.toString());
		}
		mVideoView.setAspectRatio(width, height);

		LinearLayout l = (LinearLayout) mVideoView.getParent();
		l.setLayoutParams(new FrameLayout.LayoutParams(width, height));
	}	
}

