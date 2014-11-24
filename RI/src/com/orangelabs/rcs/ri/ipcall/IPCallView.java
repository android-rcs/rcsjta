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
package com.orangelabs.rcs.ri.ipcall;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.ipcall.IPCall;
import com.gsma.services.rcs.ipcall.IPCallIntent;
import com.gsma.services.rcs.ipcall.IPCallListener;
import com.gsma.services.rcs.ipcall.IPCallService;
import com.orangelabs.rcs.ri.ApiConnectionManager;
import com.orangelabs.rcs.ri.ApiConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.ipcall.media.MyIPCallPlayer;
import com.orangelabs.rcs.ri.ipcall.media.MyIPCallRenderer;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsDisplayName;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * IP call view
 * 
 * @author Jean-Marc AUFFRET
 */
public class IPCallView extends Activity {
	/**
	 * View modes
	 */
	public final static int MODE_INCOMING = 0;
	public final static int MODE_OUTGOING = 1;
	public final static int MODE_OPEN = 2;

	/**
	 * Intent parameters
	 */
	public final static String EXTRA_MODE = "mode";
	public final static String EXTRA_CALL_ID = "callId";
	public final static String EXTRA_CONTACT = "contact";
	public final static String EXTRA_VIDEO_OPTION = "video";

	/**
     * UI handler
     */
    private final Handler handler = new Handler();

	/**
	 * Call ID
	 */
    private String callId;
    
	/**
	 * Remote contact
	 */
    private ContactId contact;

	/**
	 * Video
	 */
    private boolean video = false;

    /**
	 * IP call
	 */
	private IPCall call;

    /**
	 * IP call player
	 */
	private MyIPCallPlayer player;

    /**
	 * IP call renderer
	 */
	private MyIPCallRenderer renderer;

	
    /**
	 * A locker to exit only once
	 */
	private LockAccess exitOnce = new LockAccess();
	
	/**
	 * API connection manager
	 */
	private ApiConnectionManager connectionManager;
	
	/**
	 * Progress dialog
	 */
	private Dialog progressDialog;
	
	 /**
   	 * The log tag for this class
   	 */
   	private static final String LOGTAG = LogUtils.getTag(IPCallView.class.getSimpleName());

	 /**
     * IP call listener
     */
    private IPCallListener callListener = new IPCallListener() {

		@Override
		public void onIPCallStateChanged(ContactId contact, String callId, final int state, int reasonCode) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onIPCallStateChanged contact=" + contact + " callId=" + callId + " state=" + state + " reason="
						+ reasonCode);
			}
			// TODO : remove controls (CR031 enum)
			if (state > RiApplication.IPCALL_STATES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onIPCallStateChanged unhandled state=" + state);
				}
				return;
			}
			if (reasonCode > RiApplication.IPCALL_REASON_CODES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onIPCallStateChanged unhandled reason=" + reasonCode);
				}
				return;
			}
			// Discard event if not for current callId
			if (IPCallView.this.callId == null || !IPCallView.this.callId.equals(callId)) {
				return;
			}
			final String _reasonCode = RiApplication.IPCALL_REASON_CODES[reasonCode];
			handler.post(new Runnable() {
				public void run() {
					
					ToggleButton holdBtn = (ToggleButton)findViewById(R.id.hold);
					switch (state) {
					case IPCall.State.STARTED:
						// Session is established: hide progress dialog
						hideProgressDialog();
				        holdBtn.setChecked(false);
						break;
						
					case IPCall.State.HOLD:
						// Update UI
				        holdBtn.setChecked(true);
						break;
						
					case IPCall.State.ABORTED:
						// Session is aborted: hide progress dialog then exit
						hideProgressDialog();
						Utils.showMessageAndExit(IPCallView.this, getString(R.string.label_ipcall_aborted, _reasonCode), exitOnce);
						break;

					case IPCall.State.REJECTED:
						// Session is rejected: hide progress dialog then exit
						hideProgressDialog();
						Utils.showMessageAndExit(IPCallView.this, getString(R.string.label_ipcall_rejected, _reasonCode), exitOnce);
						break;

					case IPCall.State.FAILED:
						// Session is failed: hide progress dialog then exit
						hideProgressDialog();
						Utils.showMessageAndExit(IPCallView.this, getString(R.string.label_ipcall_failed, _reasonCode), exitOnce);
						break;

					default:
						if (LogUtils.isActive) {
							Log.d(LOGTAG,
									"onIPCallStateChanged "
											+ getString(R.string.label_ipcall_state_changed, RiApplication.IPCALL_STATES[state],
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
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.ipcall_view);

        // Set title
        setTitle(R.string.title_ipcall);
    	
		// Set buttons callback
        Button hangupBtn = (Button)findViewById(R.id.hangup_btn);
        hangupBtn.setOnClickListener(btnHangupListener);

        // Register to API connection manager
     	connectionManager = ApiConnectionManager.getInstance(this);
     	if (connectionManager == null || !connectionManager.isServiceConnected(RcsServiceName.IP_CALL, RcsServiceName.CONTACTS)) {
			Utils.showMessageAndExit(this, getString(R.string.label_service_not_available), exitOnce);
		} else {
			connectionManager.startMonitorServices(this, exitOnce, RcsServiceName.IMAGE_SHARING, RcsServiceName.CONTACTS);
			initiateIpCall();
		}
    }
    
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (connectionManager == null) {
    		return;
    	}
		connectionManager.stopMonitorServices(this);
		if (connectionManager.isServiceConnected(RcsServiceName.IP_CALL)) {
			// Remove service listener
			try {
				connectionManager.getIPCallApi().removeEventListener(callListener);
			} catch (Exception e) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "removeEventListener failed", e);
				}
			}
		}
	}
	
	/**
	 * Accept invitation
	 */
	private void acceptInvitation() {
    	try {
    		// Instantiate player and renderer
    		player = new MyIPCallPlayer();
    		renderer = new MyIPCallRenderer();

    		// Accept the invitation
			call.acceptInvitation(player, renderer);
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
			call.rejectInvitation();
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
	}	
    
    public void initiateIpCall() {
    	IPCallService ipcallApi = connectionManager.getIPCallApi();
		try {
			// Add service listener
			ipcallApi.addEventListener(callListener);
	        int mode = getIntent().getIntExtra(IPCallView.EXTRA_MODE, -1);
			if (mode == IPCallView.MODE_OUTGOING) {
				// Outgoing call

	            // Check if the service is available
	        	boolean registered = ipcallApi.isServiceRegistered();
	            if (!registered) {
	    	    	Utils.showMessageAndExit(this, getString(R.string.label_service_not_available), exitOnce);
	    	    	return;
	            } 
	            
		    	// Get remote contact
				contact = getIntent().getParcelableExtra(IPCallView.EXTRA_CONTACT);
		        
		    	// Get video option
				video = getIntent().getBooleanExtra(IPCallView.EXTRA_VIDEO_OPTION, false);

				// Initiate call
    			startCall();
			} else {
				if (mode == IPCallView.MODE_OPEN) {
					// Open an existing session

					// Incoming call
					callId = getIntent().getStringExtra(IPCallView.EXTRA_CALL_ID);

					// Get the call
					call = ipcallApi.getIPCall(callId);
					if (call == null) {
						// Session not found or expired
						Utils.showMessageAndExit(this, getString(R.string.label_ipcall_has_expired), exitOnce);
						return;
					}

					// Get video option
					video = call.isVideo();

					// Get remote contact
					contact = call.getRemoteContact();
				} else {
					// Incoming session from its Intent
					callId = getIntent().getStringExtra(IPCallIntent.EXTRA_CALL_ID);

					// Get the call
					call = ipcallApi.getIPCall(callId);
					if (call == null) {
						// Session not found or expired
						Utils.showMessageAndExit(this, getString(R.string.label_ipcall_has_expired), exitOnce);
						return;
					}

					// Get remote contact
					contact = call.getRemoteContact();
					String from = RcsDisplayName.getInstance(this).getDisplayName(contact);
					// Get video option
					video = call.isVideo();

					// Manual accept
					AlertDialog.Builder builder = new AlertDialog.Builder(this);
					if (video) {
						builder.setTitle(R.string.title_ipcall_video);
					} else {
						builder.setTitle(R.string.title_ipcall);
					}
					builder.setMessage(getString(R.string.label_from_args, from));
					builder.setCancelable(false);
					builder.setIcon(R.drawable.ri_notif_ipcall_icon);
					builder.setPositiveButton(getString(R.string.label_accept), acceptBtnListener);
					builder.setNegativeButton(getString(R.string.label_decline), declineBtnListener);
					builder.show();
				}
			}
			
			String from = RcsDisplayName.getInstance(this).getDisplayName(contact);
			// Display call info
	    	TextView contactEdit = (TextView)findViewById(R.id.contact);
	    	contactEdit.setText(from);
	        ToggleButton videoBtn = (ToggleButton)findViewById(R.id.video);
	        videoBtn.setChecked(video);        
	        videoBtn.setOnCheckedChangeListener(btnVideoListener);        
	        ToggleButton holdBtn = (ToggleButton)findViewById(R.id.hold);
	        holdBtn.setChecked(false);
	        holdBtn.setOnCheckedChangeListener(btnHoldListener);        
		} catch(RcsServiceException e) {
			e.printStackTrace();
			Utils.showMessageAndExit(this, getString(R.string.label_api_failed), exitOnce);
		}
    }

    /**
     * Start session
     */
    private void startCall() {
		// Initiate the IP call session in background
    	try {
    		// Instantiate player and renderer
    		player = new MyIPCallPlayer();
    		renderer = new MyIPCallRenderer();
    		
			// Initiate session
    		if (video) {
    			// Visio call
    			call = connectionManager.getIPCallApi().initiateVisioCall(contact, player, renderer);
    		} else {
    			// Audio call
    			call = connectionManager.getIPCallApi().initiateCall(contact, player, renderer);
    		}
    		callId = call.getCallId();
    	} catch(Exception e) {
    		e.printStackTrace();
			Utils.showMessageAndExit(this, getString(R.string.label_invitation_failed), exitOnce);		
    	}

        // Display a progress dialog
        progressDialog = Utils.showProgressDialog(this, getString(R.string.label_command_in_progress));
        progressDialog.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				Toast.makeText(IPCallView.this, getString(R.string.label_ipcall_canceled), Toast.LENGTH_SHORT).show();
				quitSession();
			}
		});
    }
    
	/**
	 * Hide progress dialog
	 */
    public void hideProgressDialog() {
    	if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
			progressDialog = null;
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
        if (call != null) {
        	try {
        		call.abortCall();
        	} catch(Exception e) {
        		e.printStackTrace();
        	}
        	call = null;
        }
    	
        // Exit activity
		finish();
    }    

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
    			if (call != null) {
    				AlertDialog.Builder builder = new AlertDialog.Builder(this);
    				builder.setTitle(getString(R.string.label_confirm_close));
    				builder.setPositiveButton(getString(R.string.label_ok), new DialogInterface.OnClickListener() {
    					public void onClick(DialogInterface dialog, int which) {
    		            	// Quit the session
    		            	quitSession();
    					}
    				});
    				builder.setNegativeButton(getString(R.string.label_cancel), new DialogInterface.OnClickListener() {
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
    
    /**
     * Hangup button callback
     */
    private android.view.View.OnClickListener btnHangupListener = new android.view.View.OnClickListener() {
        public void onClick(View v) {
        	// Quit the session
        	quitSession();
        }
    };

    /**
	 * Video toggle button listener
     */
    private OnCheckedChangeListener btnVideoListener = new OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        	if (isChecked) {
				// Add the video stream
				addVideo();
			} else {
				// Remove the video stream
				removeVideo();
			}
		}
	};
    
	/**
	 * Add video
	 */
	private void addVideo() {
		// Initiate the operation in background
		try {
			call.addVideo();
		} catch(Exception e) {
			e.printStackTrace();
			// TODO: update UI
		}
		
        // Display a progress dialog
        progressDialog = Utils.showProgressDialog(IPCallView.this, getString(R.string.label_command_in_progress));
        progressDialog.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				// TODO
			}
		});
	}

	/**
	 * Remove the video
	 */
	private void removeVideo() {
		// Initiate the operation in background
		try {
			call.removeVideo();
		} catch(Exception e) {
			e.printStackTrace();
			// TODO: update UI
		}
		
        // Display a progress dialog
        progressDialog = Utils.showProgressDialog(IPCallView.this, getString(R.string.label_command_in_progress));
        progressDialog.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				// TODO
			}
		});
	}

	/**
	 * Hold toggle button listener
     */
    private OnCheckedChangeListener btnHoldListener = new OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        	if (isChecked) {
				// Hold the call
				holdCall();
			} else {
				// Continue the call
				continueCall();
			}
		}
	};
	
	/**
	 * Hold the call
	 */
	private void holdCall() {
		try {
			call.holdCall();
		} catch(Exception e) {
			e.printStackTrace();
			// TODO: update UI
		}
	}

	/**
	 * Continue the call
	 */
	private void continueCall() {
		try {
			call.continueCall();
		} catch(Exception e) {
			e.printStackTrace();
			// TODO: update UI
		}
	}
}