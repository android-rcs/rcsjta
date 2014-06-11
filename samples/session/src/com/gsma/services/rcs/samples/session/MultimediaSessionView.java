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
package com.gsma.services.rcs.samples.session;

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
import com.gsma.services.rcs.samples.session.utils.Utils;


/**
 * Multimedia session view
 *  
 * @author Jean-Marc AUFFRET
 */
public class MultimediaSessionView extends Activity implements JoynServiceListener {
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
	 * Exchanged data
	 */
	private static byte[] data = new byte[2048];

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
	private MultimediaMessagingSession session = null;

    /**
     * MM session listener
     */
    private MySessionListener sessionListener = new MySessionListener();
    
    /**
     * Data transfer status
     */
    private boolean dataTransferTerminated = false;
    
    /**
     * Data sent
     */
    private long dataSent = 0;
	
    /**
     * Data received
     */
    private long dataRecv = 0;

    /**
	 * Progress dialog
	 */
	private Dialog progressDialog = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.session_view);

        // Set title
        setTitle(R.string.title_session);

        // Init data
    	for(int i=0; i < data.length; i++) {
        	data[i] = 0x00;
    	}
        
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
     * Callback called when service is connected. This method is called when the
     * service is well connected to the RCS service (binding procedure successfull):
     * this means the methods of the API may be used.
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
		        sessionId = getIntent().getStringExtra(MultimediaMessagingSessionIntent.EXTRA_SESSION_ID);

		        // Get the session
	    		session = sessionApi.getMessagingSession(sessionId);
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
				builder.setTitle(R.string.title_session);
				builder.setMessage(getString(R.string.label_from) +	" " + contact);
				builder.setCancelable(false);
				builder.setIcon(R.drawable.notif_invitation_icon);
				builder.setPositiveButton(getString(R.string.label_accept), acceptBtnListener);
				builder.setNegativeButton(getString(R.string.label_decline), declineBtnListener);
				builder.show();
			}
			
			// Display session info
	    	TextView contactEdit = (TextView)findViewById(R.id.contact);
	    	contactEdit.setText(contact);
		} catch(JoynServiceException e) {
			Utils.showMessageAndExit(MultimediaSessionView.this, getString(R.string.label_api_failed));
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
		Utils.showMessageAndExit(MultimediaSessionView.this, getString(R.string.label_api_disabled));
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
		// Initiate the chat session in background
    	try {
			// Initiate session
			session = sessionApi.initiateMessagingSession(ServiceUtils.SERVICE_ID, contact, sessionListener);
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
     * Session event listener
     */
    private class MySessionListener extends MultimediaMessagingSessionListener {
    	// Session ringing
    	public void onSessionRinging() {
    	}

    	// Session started
    	public void onSessionStarted() {
			handler.post(new Runnable() { 
				public void run() {
					// Hide progress dialog
					hideProgressDialog();
					
            		// Send data
					if (mode == MultimediaSessionView.MODE_OUTGOING) {
						// Originating
			    		Thread t = new Thread() {
			    			public void run() {
			    				try  {
				            		while(!dataTransferTerminated) {
					            		// Send data
				            			dataSent += data.length;
				            			session.sendMessage(data);
				            			
				            			// Update UI
				            			handler.post(new Runnable() {
				            				public void run() {
						            	    	TextView txtView = (TextView)findViewById(R.id.data);
						            	    	txtView.setText(getString(R.string.label_data_sent, dataSent/1024));
				            				}
				            			});
				            		}
			    				} catch(Exception e) {
									e.printStackTrace();
									// Can't connect media: abort the session
									try {
										session.abortSession();
									} catch(Exception ex) {}
			    				}
			    			}
			    		};
			    		t.start();
					}
				}
			});
    	}
    	
    	// Session aborted
    	public void onSessionAborted() {
			handler.post(new Runnable(){
				public void run(){
			    	// Stop data transfer
					dataTransferTerminated = true;

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
			    	// Stop data transfer
					dataTransferTerminated = true;

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
    		// Receive data
    		dataRecv += content.length;
    		
			// Update UI
			handler.post(new Runnable() {
				public void run() {
			    	TextView txtView = (TextView)findViewById(R.id.data);
			    	txtView.setText(getString(R.string.label_data_recv, dataRecv/1024));
				}
			});
    	}
    };
        
	/**
     * Quit the session
     */
    private void quitSession() {
    	// Stop data transfer
		dataTransferTerminated = true;

		// Stop session
        if (session != null) {
        	try {
        		session.removeEventListener(sessionListener);
        		session.abortSession();
        	} catch(Exception e) {
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
		inflater.inflate(R.menu.menu_session, menu);
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