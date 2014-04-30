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
package com.orangelabs.rcs.whiteboard;

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
import android.widget.Toast;

import com.gsma.services.rcs.JoynService;
import com.gsma.services.rcs.JoynServiceException;
import com.gsma.services.rcs.JoynServiceListener;
import com.gsma.services.rcs.session.MultimediaSession;
import com.gsma.services.rcs.session.MultimediaSessionIntent;
import com.gsma.services.rcs.session.MultimediaSessionListener;
import com.gsma.services.rcs.session.MultimediaSessionService;
import com.orangelabs.rcs.whiteboard.utils.Utils;


/**
 * Multimedia session view
 *  
 * @author Jean-Marc AUFFRET
 * @author ming
 */
public class MultimediaSessionView extends Activity implements JoynServiceListener {
	private static final String TAG = "whiteboard";

	/**
	 * View modes
	 */
	public final static int MODE_INCOMING = 0;
	public final static int MODE_OUTGOING = 1;

	/**
	 * Intent parameters
	 */
	public final static String EXTRA_MODE = "mode";
	public final static String EXTRA_SESSION_ID = "session_id";
	public final static String EXTRA_CONTACT = "contact";

	/**
     * UI handler
     */
    private final Handler handler = new Handler();

	/**
	 * MM session API
	 */
	private MultimediaSessionService sessionApi;

	/**
	 * Mode
	 */
	private int mode;
	
	/**
	 * Session ID
	 */
    private String sessionId;
    
	/**
	 * Remote contact
	 */
    private String contact;

    /**
	 * MM session
	 */
	private MultimediaSession session = null;

    /**
     * MM session listener
     */
    private MySessionListener sessionListener = new MySessionListener();    
	
    /**
	 * Progress dialog
	 */
	private Dialog progressDialog = null;
	
	//dynamic menu
	public int currentTask;
	public final int CURRENTTASK_DRAWPHOTO=1;
	
	//spawn task will exit if stop is true
	boolean stop=false;
	
	public final static int BIG_MSG_SZ=1048576; //1M	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.session_view);

        // Set title
        setTitle(R.string.app_name);
    	    	
    	// Instanciate API
        sessionApi = new MultimediaSessionService(getApplicationContext(), this);
        
        // Connect API
        sessionApi.connect();
    }
    
	@Override
	protected void onDestroy() {
		super.onDestroy();

		// Remove session listener
		if (session != null) {
			try {
				session.removeEventListener(sessionListener);
			} catch (Exception e) {
			}
		}

        // Disconnect API
        sessionApi.disconnect();
        
        //DrawPhoto onDestroy
        if (drawPhoto!=null) {
        	drawPhoto.onDestroy();
        }
        
        //signal to transmission service
        stop=true;
	}
		    
    /**
     * Callback called when service is connected. This method is called when the
     * service is well connected to the RCS service (binding procedure successfull):
     * this means the methods of the API may be used.
     * 
     * Here the TCP server is setup for the call terminator
     */
    public void onServiceConnected() {
		try {
	        mode = getIntent().getIntExtra(MultimediaSessionView.EXTRA_MODE, -1);
			if (mode == MultimediaSessionView.MODE_OUTGOING) {
				// Outgoing session

	            // Check if the service is available
	        	boolean registered = false;
	        	try {
	        		if ((sessionApi != null) && sessionApi.isServiceRegistered()) {
	        			registered = true;
	        		}
	        	} catch(Exception e) {}
	            if (!registered) {
	    	    	Utils.showMessageAndExit(MultimediaSessionView.this, getString(R.string.label_service_not_available));
	    	    	return;
	            } 
	            
		    	// Get remote contact
				contact = getIntent().getStringExtra(MultimediaSessionView.EXTRA_CONTACT);
		        
		        // Initiate session
    			startSession();
			} else {
				// Incoming session from its Intent
		        sessionId = getIntent().getStringExtra(MultimediaSessionIntent.EXTRA_SESSION_ID);

		        // Get the session
	    		session = sessionApi.getSession(sessionId);
				if (session == null) {
					// Session not found or expired
					Utils.showMessageAndExit(MultimediaSessionView.this, getString(R.string.label_session_has_expired));
					return;
				}
				
    			// Add session event listener
				session.addEventListener(sessionListener);
				
		    	// Get remote contact
				contact = session.getRemoteContact();
		
		        // Manual accept
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.app_name);
				builder.setMessage(getString(R.string.label_invitation, contact));
				builder.setCancelable(false);
				builder.setIcon(R.drawable.notif_invitation_icon);
				builder.setPositiveButton(getString(R.string.label_accept), acceptBtnListener);
				builder.setNegativeButton(getString(R.string.label_decline), declineBtnListener);
				builder.show();
			}
		} catch(JoynServiceException e) {
			Utils.showMessageAndExit(MultimediaSessionView.this, getString(R.string.label_service_not_available));
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
		Utils.showMessageAndExit(MultimediaSessionView.this, getString(R.string.label_service_not_available));
    }    
	
	/**
	 * Accept invitation
	 */
	private void acceptInvitation() {
    	try {
    		// Accept the invitation
			session.acceptInvitation();
    	} catch(Exception e) {
			Utils.showMessageAndExit(MultimediaSessionView.this, getString(R.string.label_invitation_failed));
    	}
	}
	
	/**
	 * Reject invitation
	 */
	private void rejectInvitation() {
    	try {
    		// Reject the invitation
    		session.removeEventListener(sessionListener);
			session.rejectInvitation();
    	} catch(Exception e) {
    	}
	}	

	/**
     * Start session
     */
    private void startSession() {
    	try {
			// Initiate session
			session = sessionApi.initiateSession(ServiceUtils.SERVICE_ID, contact, sessionListener);
    	} catch(Exception e) {
    		e.printStackTrace();
			Utils.showMessageAndExit(MultimediaSessionView.this, getString(R.string.label_invitation_failed));		
    	}

        // Display a progress dialog
        progressDialog = Utils.showProgressDialog(MultimediaSessionView.this, getString(R.string.label_command_in_progress));
        progressDialog.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				Toast.makeText(MultimediaSessionView.this, getString(R.string.label_session_canceled), Toast.LENGTH_SHORT).show();
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
     * Session event listener: mainly invoked when call terminator accept session
     */
    private class MySessionListener extends MultimediaSessionListener {
    	// Session ringing
    	public void onSessionRinging() {
    	}

    	// Session started. Here the TCP server is setup for the call initiator
    	public void onSessionStarted() {
			handler.post(new Runnable() { 
				public void run() {
					// Hide progress dialog
					hideProgressDialog();
					
					// Init whiteboard
			    	drawPhoto = new DrawPhoto(MultimediaSessionView.this);
				}
			});
    	}
    	
    	// Session aborted
    	public void onSessionAborted() {
			handler.post(new Runnable(){
				public void run(){
					// Hide progress dialog
					hideProgressDialog();

					// Show info
					Utils.showMessageAndExit(MultimediaSessionView.this, getString(R.string.label_session_aborted));
				}
			});
    	}

    	// Session error
    	public void onSessionError(final int error) {
			handler.post(new Runnable() {
				public void run() {
					// Hide progress dialog
					hideProgressDialog();
					
					// Display error
					if (error == MultimediaSession.Error.INVITATION_DECLINED) {
						Utils.showMessageAndExit(MultimediaSessionView.this, getString(R.string.label_session_declined));
					} else {
						Utils.showMessageAndExit(MultimediaSessionView.this, getString(R.string.label_session_failed, error));
					}					
				}
			});
    	}
    	
    	// Receive new message
    	public void onNewMessage(byte[] content) {
    		// TODO
    	}    	
    };
        
	/**
     * Quit the session
     */
    private void quitSession() {
    	try {
            if (session != null) {
            	try {
            		session.removeEventListener(sessionListener);
            		session.abortSession();
            	} catch(Exception e) {
            	}
            	session = null;
            }
    	} catch(Exception e) {
    	}
    	
        // Exit activity
		finish();
    }    

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
    			if (session != null) {
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

    // start whiteboard after joyn session establishment
    //---------------
    private DrawPhoto drawPhoto=null;
    
    public void send(String txt) {
    	try {
    		if (session != null) {
        		Log.d(TAG, "Send draw event " + txt);
    			session.sendMessage(txt.getBytes());
    		}
    	} catch (Throwable e) {
    		Log.d(TAG, "Send draw event failed", e);
    	}
    }
}