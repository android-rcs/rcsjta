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
package com.orangelabs.rcs.ri.extension;

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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.gsma.services.rcs.JoynService;
import com.gsma.services.rcs.JoynServiceException;
import com.gsma.services.rcs.JoynServiceListener;
import com.gsma.services.rcs.extension.MultimediaMessagingSession;
import com.gsma.services.rcs.extension.MultimediaMessagingSessionIntent;
import com.gsma.services.rcs.extension.MultimediaMessagingSessionListener;
import com.gsma.services.rcs.extension.MultimediaSession;
import com.gsma.services.rcs.extension.MultimediaSessionService;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Messaging session view
 *  
 * @author Jean-Marc AUFFRET
 */
public class MessagingSessionView extends Activity implements JoynServiceListener {
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
	 * Session ID
	 */
    private String sessionId;
    
	/**
	 * Remote contact
	 */
    private String contact;

	/**
	 * Service ID
	 */
    private String serviceId = TestMultimediaSessionApi.SERVICE_ID;

    /**
	 * Session
	 */
	private MultimediaMessagingSession session = null;

    /**
     * Session listener
     */
    private MySessionListener sessionListener = new MySessionListener();    
	
    /**
	 * Progress dialog
	 */
	private Dialog progressDialog = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.extension_messaging_session_view);

        // Set title
        setTitle(R.string.title_messaging_session);
    	
        // Set buttons callback
		Button sendBtn = (Button)findViewById(R.id.send_btn);
		sendBtn.setOnClickListener(btnSendListener);
		sendBtn.setEnabled(false);

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
	}
	
	/**
	 * Accept invitation
	 */
	private void acceptInvitation() {
    	try {
    		// Accept the invitation
			session.acceptInvitation();
    	} catch(Exception e) {
    		e.printStackTrace();
			Utils.showMessageAndExit(MessagingSessionView.this, getString(R.string.label_invitation_failed));
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
    		e.printStackTrace();
    	}
	}	
    
    /**
     * Callback called when service is connected. This method is called when the
     * service is well connected to the RCS service (binding procedure successfull):
     * this means the methods of the API may be used.
     */
    public void onServiceConnected() {
		try {
	        int mode = getIntent().getIntExtra(MessagingSessionView.EXTRA_MODE, -1);
			if (mode == MessagingSessionView.MODE_OUTGOING) {
				// Outgoing session

	            // Check if the service is available
	        	boolean registered = false;
	        	try {
	        		if ((sessionApi != null) && sessionApi.isServiceRegistered()) {
	        			registered = true;
	        		}
	        	} catch(Exception e) {
	        		e.printStackTrace();
	        	}
	            if (!registered) {
	    	    	Utils.showMessageAndExit(MessagingSessionView.this, getString(R.string.label_service_not_available));
	    	    	return;
	            } 
	            
		    	// Get remote contact
				contact = getIntent().getStringExtra(MessagingSessionView.EXTRA_CONTACT);
		        
		        // Initiate session
    			startSession();
			} else
			if (mode == MessagingSessionView.MODE_OPEN) {
				// Open an existing session
				
				// Incoming session
		        sessionId = getIntent().getStringExtra(MessagingSessionView.EXTRA_SESSION_ID);

		    	// Get the session
	    		session = sessionApi.getMessagingSession(sessionId);
				if (session == null) {
					// Session not found or expired
					Utils.showMessageAndExit(MessagingSessionView.this, getString(R.string.label_session_has_expired));
					return;
				}
				
    			// Add session event listener
				session.addEventListener(sessionListener);
				
		    	// Get remote contact
				contact = session.getRemoteContact();
			} else {
				// Incoming session from its Intent
		        sessionId = getIntent().getStringExtra(MultimediaMessagingSessionIntent.EXTRA_SESSION_ID);

		    	// Get the session
	    		session = sessionApi.getMessagingSession(sessionId);
				if (session == null) {
					// Session not found or expired
					Utils.showMessageAndExit(MessagingSessionView.this, getString(R.string.label_session_has_expired));
					return;
				}
				
    			// Add session event listener
				session.addEventListener(sessionListener);
				
		    	// Get remote contact
				contact = session.getRemoteContact();
		
				// Manual accept
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.title_messaging_session);
				builder.setMessage(getString(R.string.label_from) +	" " + contact + "\n" +
						getString(R.string.label_service_id) + " " + serviceId);
				builder.setCancelable(false);
				builder.setIcon(R.drawable.ri_notif_mm_session_icon);
				builder.setPositiveButton(getString(R.string.label_accept), acceptBtnListener);
				builder.setNegativeButton(getString(R.string.label_decline), declineBtnListener);
				builder.show();
			}
			
			// Display session info
	    	TextView featureTagEdit = (TextView)findViewById(R.id.feature_tag);
	    	featureTagEdit.setText(serviceId);
	    	TextView contactEdit = (TextView)findViewById(R.id.contact);
	    	contactEdit.setText(contact);
		} catch(JoynServiceException e) {
			e.printStackTrace();
			Utils.showMessageAndExit(MessagingSessionView.this, getString(R.string.label_api_failed));
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
		Utils.showMessageAndExit(MessagingSessionView.this, getString(R.string.label_api_disabled));
    }    
	
    /**
     * Start session
     */
    private void startSession() {
		// Initiate the chat session in background
    	try {
			// Initiate session
			session = sessionApi.initiateMessagingSession(serviceId, contact, sessionListener);
    	} catch(Exception e) {
    		e.printStackTrace();
			Utils.showMessageAndExit(MessagingSessionView.this, getString(R.string.label_invitation_failed));		
    	}

        // Display a progress dialog
        progressDialog = Utils.showProgressDialog(MessagingSessionView.this, getString(R.string.label_command_in_progress));
        progressDialog.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				Toast.makeText(MessagingSessionView.this, getString(R.string.label_session_canceled), Toast.LENGTH_SHORT).show();
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
     * Session event listener
     */
    private class MySessionListener extends MultimediaMessagingSessionListener {
    	// Session ringing
    	public void onSessionRinging() {
    		// TODO: play ringtone
    	}

    	// Session started
    	public void onSessionStarted() {
			handler.post(new Runnable() { 
				public void run() {
					// Hide progress dialog
					hideProgressDialog();
					
					// Activate button
					Button sendBtn = (Button)findViewById(R.id.send_btn);
					sendBtn.setEnabled(true);
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
					Utils.showMessageAndExit(MessagingSessionView.this, getString(R.string.label_session_aborted));
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
						Utils.showMessageAndExit(MessagingSessionView.this, getString(R.string.label_session_declined));
					} else {
						Utils.showMessageAndExit(MessagingSessionView.this, getString(R.string.label_session_failed, error));
					}					
				}
			});
    	}
    	
    	// Receive new message
    	public void onNewMessage(byte[] content) {
    		final String data = new String(content);
    		
			handler.post(new Runnable() {
				public void run() {
					// Display received data
					TextView txt = (TextView)MessagingSessionView.this.findViewById(R.id.recv_data);
			        txt.setText(data);					
				}
			});
    	}
    };
        
	/**
     * Quit the session
     */
    private void quitSession() {
		// Stop session
        if (session != null) {
        	try {
        		session.removeEventListener(sessionListener);
        		session.abortSession();
        	} catch(Exception e) {
        		e.printStackTrace();
        	}
        	session = null;
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

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater=new MenuInflater(getApplicationContext());
		inflater.inflate(R.menu.menu_mm_session, menu);
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
    
	/**
	 * Send button callback
	 */
	private android.view.View.OnClickListener btnSendListener = new android.view.View.OnClickListener() {
		private int i = 0;
		
		public void onClick(View v) {
			try {
				String data = "data" + i++;
				session.sendMessage(data.getBytes());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};
}