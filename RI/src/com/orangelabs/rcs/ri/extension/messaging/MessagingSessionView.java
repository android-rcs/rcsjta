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
package com.orangelabs.rcs.ri.extension.messaging;

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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsCommon;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.extension.MultimediaMessagingSession;
import com.gsma.services.rcs.extension.MultimediaMessagingSessionIntent;
import com.gsma.services.rcs.extension.MultimediaMessagingSessionListener;
import com.gsma.services.rcs.extension.MultimediaSession;
import com.gsma.services.rcs.extension.MultimediaSessionService;
import com.orangelabs.rcs.ri.ApiConnectionManager;
import com.orangelabs.rcs.ri.ApiConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsDisplayName;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Messaging session view
 *  
 * @author Jean-Marc AUFFRET
 */
public class MessagingSessionView extends Activity {
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
	 * Session ID
	 */
    private String sessionId;
    
	/**
	 * Remote contact
	 */
    private ContactId contact;

	/**
	 * Service ID
	 */
    private String serviceId = MessagingSessionUtils.SERVICE_ID;

    /**
	 * Session
	 */
	private MultimediaMessagingSession session;
	
    /**
	 * Progress dialog
	 */
	private Dialog progressDialog;
	
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
   	private static final String LOGTAG = LogUtils.getTag(MessagingSessionView.class.getSimpleName());
   	
    /**
     * Session listener
     */
	private MultimediaMessagingSessionListener serviceListener = new MultimediaMessagingSessionListener() {

		@Override
		public void onStateChanged(ContactId contact, String sessionId, final int state, int reasonCode) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onMultimediaMessagingStateChanged contact=" + contact + " sessionId=" + sessionId + " state="
						+ state + " reason=" + reasonCode);
			}
			if (state > RiApplication.MMS_STATES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onMultimediaMessagingStateChanged unhandled state=" + state);
				}
				return;
			}
			if (reasonCode > RiApplication.MMS_REASON_CODES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onMultimediaMessagingStateChanged unhandled reason=" + reasonCode);
				}
				return;
			}
			// Discard event if not for current sessionId
			if (MessagingSessionView.this.sessionId == null || !MessagingSessionView.this.sessionId.equals(sessionId)) {
				return;
			}
			final String _reasonCode = RiApplication.MMS_REASON_CODES[reasonCode];
			handler.post(new Runnable() {
				public void run() {
					switch (state) {
					case MultimediaSession.State.STARTED:
						// Session is established: hide progress dialog
						hideProgressDialog();
						// Activate button
						Button sendBtn = (Button) findViewById(R.id.send_btn);
						sendBtn.setEnabled(true);
						break;

					case MultimediaSession.State.ABORTED:
						// Session is aborted: hide progress dialog then exit
						hideProgressDialog();
						Utils.showMessageAndExit(MessagingSessionView.this, getString(R.string.label_session_aborted, _reasonCode), exitOnce);
						break;

					case MultimediaSession.State.REJECTED:
						// Session is rejected: hide progress dialog then exit
						hideProgressDialog();
						Utils.showMessageAndExit(MessagingSessionView.this, getString(R.string.label_session_rejected, _reasonCode), exitOnce);
						break;

					case MultimediaSession.State.FAILED:
						// Session is failed: hide progress dialog then exit
						hideProgressDialog();
						Utils.showMessageAndExit(MessagingSessionView.this, getString(R.string.label_session_failed, _reasonCode), exitOnce);
						break;

					default:
						if (LogUtils.isActive) {
							Log.d(LOGTAG,
									"onMultimediaMessagingStateChanged "
											+ getString(R.string.label_mms_state_changed, RiApplication.MMS_STATES[state],
													_reasonCode));
						}
					}
				}
			});
		}

		@Override
		public void onMessageReceived(ContactId contact, String sessionId, byte[] content) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onNewMessage contact=" + contact + " sessionId=" + sessionId);
			}
			// Discard event if not for current sessionId
			if (MessagingSessionView.this.sessionId == null || !MessagingSessionView.this.sessionId.equals(sessionId)) {
				return;
			}
			final String data = new String(content);

			handler.post(new Runnable() {
				public void run() {
					// Display received data
					TextView txt = (TextView) MessagingSessionView.this.findViewById(R.id.recv_data);
					txt.setText(data);
				}
			});
		}
	};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.extension_session_view);

        // Set title
        setTitle(R.string.title_messaging_session);
    	
        // Set buttons callback
		Button sendBtn = (Button)findViewById(R.id.send_btn);
		sendBtn.setOnClickListener(btnSendListener);
		sendBtn.setEnabled(false);

        // Register to API connection manager
		connectionManager = ApiConnectionManager.getInstance(this);
		if (connectionManager == null || !connectionManager.isServiceConnected(RcsServiceName.MULTIMEDIA,RcsServiceName.CONTACTS)) {
			Utils.showMessageAndExit(this, getString(R.string.label_service_not_available), exitOnce);
			return;
		}
		connectionManager.startMonitorServices(this, exitOnce, RcsServiceName.MULTIMEDIA, RcsServiceName.CONTACTS);
		try {
			// Add service listener
			connectionManager.getMultimediaSessionApi().addEventListener(serviceListener);
			initialiseMessagingSession();
		} catch (RcsServiceException e) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Failed to add listener", e);
			}
			Utils.showMessageAndExit(this, getString(R.string.label_api_failed), exitOnce);
		}
    }
    
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (connectionManager == null) {
    		return;
    	}
		connectionManager.stopMonitorServices(this);
		if (connectionManager.isServiceConnected(RcsServiceName.MULTIMEDIA)) {
			// Remove listener
			try {
				connectionManager.getMultimediaSessionApi().removeEventListener(serviceListener);
			} catch (Exception e) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "Failed to remove listener", e);
				}
			}
		}
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
			Utils.showMessageAndExit(this, getString(R.string.label_invitation_failed), exitOnce);
    	}
	}
	
	/**
	 * Reject invitation
	 */
	private void rejectInvitation() {
    	try {
    		// Reject the invitation
			session.rejectInvitation();
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
	}	
    
    public void initialiseMessagingSession() {
    	MultimediaSessionService sessionApi = connectionManager.getMultimediaSessionApi();
		try {
	        int mode = getIntent().getIntExtra(MessagingSessionView.EXTRA_MODE, -1);
			if (mode == MessagingSessionView.MODE_OUTGOING) {
				// Outgoing session

	            // Check if the service is available
	            if (!sessionApi.isServiceRegistered()) {
	    	    	Utils.showMessageAndExit(this, getString(R.string.label_service_not_available), exitOnce);
	    	    	return;
	            } 
	            
		    	// Get remote contact
				contact = getIntent().getParcelableExtra(MessagingSessionView.EXTRA_CONTACT);
		        
		        // Initiate session
    			startSession();
			} else {
				if (mode == MessagingSessionView.MODE_OPEN) {
					// Open an existing session

					// Incoming session
					sessionId = getIntent().getStringExtra(MessagingSessionView.EXTRA_SESSION_ID);

					// Get the session
					session = sessionApi.getMessagingSession(sessionId);
					if (session == null) {
						// Session not found or expired
						Utils.showMessageAndExit(this, getString(R.string.label_session_has_expired), exitOnce);
						return;
					}

					// Get remote contact
					contact = session.getRemoteContact();
				} else {
					// Incoming session from its Intent
					sessionId = getIntent().getStringExtra(MultimediaMessagingSessionIntent.EXTRA_SESSION_ID);

					// Get the session
					session = sessionApi.getMessagingSession(sessionId);
					if (session == null) {
						// Session not found or expired
						Utils.showMessageAndExit(this, getString(R.string.label_session_has_expired), exitOnce);
						return;
					}

					// Get remote contact
					contact = session.getRemoteContact();
					
					String displayName = RcsDisplayName.get(this, contact);
					String from = RcsDisplayName.convert(this, RcsCommon.Direction.INCOMING, contact, displayName);
					
					// Manual accept
					AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setTitle(R.string.title_messaging_session);
					builder.setMessage(getString(R.string.label_mm_from_id, from, serviceId));
					builder.setCancelable(false);
					builder.setIcon(R.drawable.ri_notif_mm_session_icon);
					builder.setPositiveButton(getString(R.string.label_accept), acceptBtnListener);
					builder.setNegativeButton(getString(R.string.label_decline), declineBtnListener);
					builder.show();
				}
			}
			// Display session info
	    	TextView featureTagEdit = (TextView)findViewById(R.id.feature_tag);
	    	featureTagEdit.setText(serviceId);
	    	String displayName = RcsDisplayName.get(this, contact);
			String from = RcsDisplayName.convert(this, RcsCommon.Direction.INCOMING, contact, displayName);
	    	TextView contactEdit = (TextView)findViewById(R.id.contact);
	    	contactEdit.setText(from);
			Button sendBtn = (Button)findViewById(R.id.send_btn);
			if (session != null) {
				sendBtn.setEnabled(true);
			} else {
				sendBtn.setEnabled(false);
			}

		} catch(RcsServiceException e) {
			e.printStackTrace();
			Utils.showMessageAndExit(this, getString(R.string.label_api_failed), exitOnce);
		}
    }
    
    /**
     * Start session
     */
    private void startSession() {
		// Initiate the chat session in background
    	try {
			// Initiate session
			session = connectionManager.getMultimediaSessionApi().initiateMessagingSession(serviceId, contact);
			sessionId = session.getSessionId();
    	} catch(Exception e) {
    		e.printStackTrace();
			Utils.showMessageAndExit(this, getString(R.string.label_invitation_failed), exitOnce);
			return;
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
     * Quit the session
     */
    private void quitSession() {
		// Stop session
        if (session != null) {
        	try {
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