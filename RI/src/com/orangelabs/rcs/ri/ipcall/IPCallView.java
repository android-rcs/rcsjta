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

import org.gsma.joyn.JoynService;
import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.JoynServiceListener;
import org.gsma.joyn.ipcall.IPCall;
import org.gsma.joyn.ipcall.IPCallIntent;
import org.gsma.joyn.ipcall.IPCallListener;
import org.gsma.joyn.ipcall.IPCallService;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.widget.Toast;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.ipcall.media.MyIPCallPlayer;
import com.orangelabs.rcs.ri.ipcall.media.MyIPCallRenderer;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * IP call view
 * 
 * @author Jean-Marc AUFFRET
 */
public class IPCallView extends Activity implements JoynServiceListener {
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
	 * IP call API
	 */
	private IPCallService ipcallApi;

	/**
	 * Call ID
	 */
    private String callId;
    
	/**
	 * Remote contact
	 */
    private String contact;

    /**
	 * IP call
	 */
	private IPCall call = null;

    /**
     * IP call listener
     */
    private MyCallListener callListener = new MyCallListener();    
	
    /**
	 * IP call player
	 */
	private MyIPCallPlayer player = null;

    /**
	 * IP call renderer
	 */
	private MyIPCallRenderer renderer = null;

	/**
	 * Progress dialog
	 */
	private Dialog progressDialog = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.ipcall_view);

        // Set title
        setTitle(R.string.title_ipcall);
    	
    	// Instanciate API
        ipcallApi = new IPCallService(getApplicationContext(), this);
        
        // Connect API
        ipcallApi.connect();
    }
    
	@Override
	protected void onDestroy() {
		super.onDestroy();

		// Remove session listener
		if (call != null) {
			try {
				call.removeEventListener(callListener);
			} catch (Exception e) {
			}
		}

        // Disconnect API
        ipcallApi.disconnect();
	}
	
	/**
	 * Accept invitation
	 */
	private void acceptInvitation() {
    	Thread thread = new Thread() {
        	public void run() {
            	try {
            		// Accept the invitation
        			call.acceptInvitation(player, renderer);
            	} catch(Exception e) {
        			handler.post(new Runnable() { 
        				public void run() {
        					Utils.showMessageAndExit(IPCallView.this, getString(R.string.label_invitation_failed));
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
            		call.removeEventListener(callListener);
        			call.rejectInvitation();
            	} catch(Exception e) {
            	}
        	}
        };
        thread.start();
	}	
    
    /**
     * Callback called when service is connected. This method is called when the
     * service is well connected to the RCS service (binding procedure successfull):
     * this means the methods of the API may be used.
     */
    public void onServiceConnected() {
		try {
	        int mode = getIntent().getIntExtra(IPCallView.EXTRA_MODE, -1);
			if (mode == IPCallView.MODE_OUTGOING) {
				// Outgoing call

	            // Check if the service is available
	        	boolean registered = false;
	        	try {
	        		if ((ipcallApi != null) && ipcallApi.isServiceRegistered()) {
	        			registered = true;
	        		}
	        	} catch(Exception e) {}
	            if (!registered) {
	    	    	Utils.showMessageAndExit(IPCallView.this, getString(R.string.label_service_not_available));
	    	    	return;
	            } 
	            
		    	// Get remote contact
				contact = getIntent().getStringExtra(IPCallView.EXTRA_CONTACT);
		        
		        // Initiate call
    			startCall();
			} else
			if (mode == IPCallView.MODE_OPEN) {
				// Open an existing session
				
				// Incoming call
		        callId = getIntent().getStringExtra(IPCallView.EXTRA_CALL_ID);

		    	// Get the call
	    		call = ipcallApi.getIPCall(callId);
				if (call == null) {
					// Session not found or expired
					Utils.showMessageAndExit(IPCallView.this, getString(R.string.label_ipcall_has_expired));
					return;
				}
				
    			// Add call event listener
				call.addEventListener(callListener);
				
		    	// Get remote contact
				contact = call.getRemoteContact();
		
				// Display call info
				// TODO
			} else {
				// Incoming session from its Intent
		        callId = getIntent().getStringExtra(IPCallIntent.EXTRA_CALL_ID);

		    	// Get the call
	    		call = ipcallApi.getIPCall(callId);
				if (call == null) {
					// Session not found or expired
					Utils.showMessageAndExit(IPCallView.this, getString(R.string.label_ipcall_has_expired));
					return;
				}
				
    			// Add call event listener
				call.addEventListener(callListener);
				
		    	// Get remote contact
				contact = call.getRemoteContact();
		
				// Display call info
				// TODO

				// Manual accept
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.title_ipcall);
				builder.setMessage(getString(R.string.label_from) +	" " + contact);
				builder.setCancelable(false);
				builder.setIcon(R.drawable.ri_notif_ipcall_icon);
				builder.setPositiveButton(getString(R.string.label_accept), acceptBtnListener);
				builder.setNegativeButton(getString(R.string.label_decline), declineBtnListener);
				builder.show();
			}
			
			// Display call info
			// TODO
		} catch(JoynServiceException e) {
			Utils.showMessageAndExit(IPCallView.this, getString(R.string.label_api_failed));
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
		Utils.showMessageAndExit(IPCallView.this, getString(R.string.label_api_disabled));
    }    
	
    /**
     * Start session
     */
    private void startCall() {
		// Initiate the chat session in background
        Thread thread = new Thread() {
        	public void run() {
            	try {
            		// Instanciate player and renderer
            		player = new MyIPCallPlayer();
            		renderer = new MyIPCallRenderer();
            		
					// Initiate session
					call = ipcallApi.initiateCall(contact, player, renderer, callListener);
            	} catch(Exception e) {
            		e.printStackTrace();
            		handler.post(new Runnable(){
            			public void run(){
            				Utils.showMessageAndExit(IPCallView.this, getString(R.string.label_invitation_failed));		
            			}
            		});
            	}
        	}
        };
        thread.start();

        // Display a progress dialog
        progressDialog = Utils.showProgressDialog(IPCallView.this, getString(R.string.label_command_in_progress));
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
     * Call event listener
     */
    private class MyCallListener extends IPCallListener {
    	// Call ringing
    	public void onCallRinging() {
    		// TODO: play ringtone
    	}

    	// Call started
    	public void onCallStarted() {
			handler.post(new Runnable() { 
				public void run() {
					// Hide progress dialog
					hideProgressDialog();
					
					// Update UI
					// TODO
				}
			});
    	}
    	
    	// Call held
    	public void onCallHeld() {
			handler.post(new Runnable() { 
				public void run() {
					// TODO
				}
			});
    	}
    	
    	// Call continue
    	public void onCallContinue() {
			handler.post(new Runnable() { 
				public void run() {
					// TODO
				}
			});
    	}

    	// Call aborted
    	public void onCallAborted() {
			handler.post(new Runnable(){
				public void run(){
					// Hide progress dialog
					hideProgressDialog();

					// Show info
					Utils.showMessageAndExit(IPCallView.this, getString(R.string.label_ipcall_aborted));
				}
			});
    	}

    	// Call error
    	public void onCallError(final int error) {
			handler.post(new Runnable() {
				public void run() {
					// Hide progress dialog
					hideProgressDialog();
					
					// Display error
					if (error == IPCall.Error.INVITATION_DECLINED) {
						Utils.showMessageAndExit(IPCallView.this, getString(R.string.label_ipcall_declined));
					} else {
						Utils.showMessageAndExit(IPCallView.this, getString(R.string.label_ipcall_failed, error));
					}					
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
            	try {
                    if (call != null) {
                    	try {
                    		call.removeEventListener(callListener);
                    		call.abortCall();
                    	} catch(Exception e) {
                    	}
                    	call = null;
                    }
            	} catch(Exception e) {
            	}
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
}