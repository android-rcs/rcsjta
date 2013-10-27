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

package com.orangelabs.rcs.ri.richcall;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.messaging.DisplayGeoloc;
import com.orangelabs.rcs.ri.utils.Utils;
import com.orangelabs.rcs.service.api.client.ClientApiListener;
import com.orangelabs.rcs.service.api.client.ImsEventListener;
import com.orangelabs.rcs.service.api.client.messaging.GeolocPush;
import com.orangelabs.rcs.service.api.client.richcall.IGeolocSharingEventListener;
import com.orangelabs.rcs.service.api.client.richcall.IGeolocSharingSession;
import com.orangelabs.rcs.service.api.client.richcall.RichCallApi;

/**
 * Receive geoloc sharing
 * 
 * @author vfml3370
 */
public class ReceiveGeolocSharing extends Activity implements ClientApiListener, ImsEventListener {
    /**
     * UI handler
     */
    private final Handler handler = new Handler();
    
    /**
	 * Rich call API
	 */
    private RichCallApi callApi;
    
    /**
     * Geoloc sharing session
     */
    private IGeolocSharingSession sharingSession = null;

    /**
     * Session ID
     */
    private String sessionId = null;
    
    /**
     * Remote Contact
     */
    private String remoteContact;
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    	// Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.richcall_receive_geoloc_sharing);

        // Get invitation info
        sessionId = getIntent().getStringExtra("sessionId");
		remoteContact = getIntent().getStringExtra("contact");

		// Remove the notification
		ReceiveGeolocSharing.removeLocationSharingNotification(this, sessionId);

        // Instanciate messaging API
        callApi = new RichCallApi(getApplicationContext());
        callApi.addApiEventListener(this);
        callApi.addImsEventListener(this);
        callApi.connectApi();
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();

        // Remove session listener
        if (sharingSession != null) {
        	try {
        		sharingSession.removeSessionListener(geolocSharingEventListener);
        	} catch(Exception e) {
        	}
        }

        // Disconnect rich call API
        callApi.disconnectApi();
    }
    
    /**
     * API disabled
     */
    public void handleApiDisabled() {
		handler.post(new Runnable() { 
			public void run() {
				Utils.showMessageAndExit(ReceiveGeolocSharing.this, getString(R.string.label_api_disabled));
			}
		});
    }
    
    /**
     * API connected
     */
    public void handleApiConnected() {
    	if (sessionId != null) {
    		try{
    			// Get the location sharing session
    			sharingSession = callApi.getGeolocSharingSession(sessionId);
    			if (sharingSession == null) {
    				// Session not found or expired
    				Utils.showMessageAndExit(ReceiveGeolocSharing.this, getString(R.string.label_session_has_expired));
    				return;
    			}
    			sharingSession.addSessionListener(geolocSharingEventListener);
    			
    			AlertDialog.Builder builder = new AlertDialog.Builder(this);
    			builder.setTitle(R.string.title_recv_geoloc_sharing);
    			builder.setMessage(getString(R.string.label_from) + " " + remoteContact + "\n");
    			builder.setCancelable(false);
				builder.setIcon(R.drawable.ri_notif_gsh_icon);
    			builder.setPositiveButton(getString(R.string.label_accept), acceptBtnListener);
    			builder.setNegativeButton(getString(R.string.label_decline), declineBtnListener);
    			builder.show();    			
    		} catch(Exception e) {
    			handler.post(new Runnable(){
    				public void run(){
    					Utils.showMessageAndExit(ReceiveGeolocSharing.this, getString(R.string.label_api_failed));
    				}
    			});
			}
    	}
    }

    /**
     * API disconnected
     */
    public void handleApiDisconnected() {
		handler.post(new Runnable(){
			public void run(){
				Utils.showMessageAndExit(ReceiveGeolocSharing.this, getString(R.string.label_api_disconnected));
			}
		});
	}
    
    /**
     * Client is connected to the IMS
     */
	public void handleImsConnected() {
	}

    /**
     * Client is disconnected from the IMS
     * 
     * @param reason Disconnection reason
     */
	public void handleImsDisconnected(int reason) {
    	// IMS has been disconnected
		handler.post(new Runnable(){
			public void run(){
				Utils.showMessageAndExit(ReceiveGeolocSharing.this, getString(R.string.label_ims_disconnected));
			}
		});
	}
    
    /**
     * Accept button listener
     */
    private OnClickListener acceptBtnListener = new OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
        	            
            // Set title
            setTitle(R.string.title_recv_geoloc_sharing);
            
    		// Display the remote contact
            TextView from = (TextView)findViewById(R.id.from);
            from.setText(getString(R.string.label_from) + " " + remoteContact);
            
            Thread thread = new Thread() {
            	public void run() {
                	try {
                		// Accept the invitation
            			sharingSession.acceptSession();
	            	} catch(Exception e) {
	    				handler.post(new Runnable() { 
	    					public void run() {
	    						Utils.showMessageAndExit(ReceiveGeolocSharing.this, getString(R.string.label_invitation_failed));
	    					}
	    				});
	            	}
            	}
            };
            thread.start();
        }
    };
    
    /**
     * Reject button listener
     */
    private OnClickListener declineBtnListener = new OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            Thread thread = new Thread() {
            	public void run() {
                	try {
                		// Reject the invitation
                		sharingSession.removeSessionListener(geolocSharingEventListener);
                		sharingSession.rejectSession();
	            	} catch(Exception e) {
	            	}
            	}
            };
            thread.start();
            
            // Exit activity
			finish();
		}
    };
    
    /**
     * Geoloc sharing session event listener
     */
    private IGeolocSharingEventListener geolocSharingEventListener = new IGeolocSharingEventListener.Stub() {
		// Session is started
		public void handleSessionStarted() {
			handler.post(new Runnable() { 
				public void run() {
					TextView statusView = (TextView)findViewById(R.id.progress_status);
					statusView.setText("started");
				}
			});
		}
	
		// Session has been aborted
		public void handleSessionAborted(int reason) {
			handler.post(new Runnable() { 
				public void run() {
					// Display session status
					Utils.showMessageAndExit(ReceiveGeolocSharing.this, getString(R.string.label_sharing_aborted));
				}
			});
		}
	    
		// Session has been terminated by remote
		public void handleSessionTerminatedByRemote() {
			handler.post(new Runnable() { 
				public void run() {
					// Display session status
					Utils.showMessageAndExit(ReceiveGeolocSharing.this, getString(R.string.label_sharing_terminated_by_remote));
				}
			});
		}

		// Sharing error
		public void handleSharingError(final int error) {
			handler.post(new Runnable() { 
				public void run() {
					Utils.showMessageAndExit(ReceiveGeolocSharing.this, getString(R.string.label_csh_failed, error));
				}
			});
		}

		// Content transfered
		public void handleGeolocTransfered(final GeolocPush geoloc) {
			handler.post(new Runnable() { 
				public void run() {
					TextView statusView = (TextView)findViewById(R.id.progress_status);
					statusView.setText("transfered");
					
			        // Show the shared geoloc
					Intent intent = new Intent(ReceiveGeolocSharing.this, DisplayGeoloc.class);
			    	intent.putExtra("contact", remoteContact);
			    	intent.putExtra("geoloc", geoloc);
					startActivity(intent);
			        			        
				}
			});
			
		}
    };    

    /**
     * Add location share notification
     * 
     * @param context Context
     * @param intent Intent invitation
     */
	public static void addLocationSharingInvitationNotification(Context context, Intent invitation) {
        // Initialize settings
		RcsSettings.createInstance(context);        

		// Create notification
		Intent intent = new Intent(invitation);
		intent.setClass(context, ReceiveGeolocSharing.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);		
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        String notifTitle = context.getString(R.string.title_recv_geoloc_sharing);
        Notification notif = new Notification(R.drawable.ri_notif_gsh_icon,
        		notifTitle,
        		System.currentTimeMillis());
        notif.flags = Notification.FLAG_AUTO_CANCEL;
        notif.setLatestEventInfo(context,
        		notifTitle,
        		context.getString(R.string.label_from) + " " + Utils.formatCallerId(invitation),
        		contentIntent);
        
        // Set ringtone
        String ringtone = RcsSettings.getInstance().getCShInvitationRingtone();
        if (!TextUtils.isEmpty(ringtone)) {
			notif.sound = Uri.parse(ringtone);
        }
        
        // Set vibration
        if (RcsSettings.getInstance().isPhoneVibrateForCShInvitation()) {
        	notif.defaults |= Notification.DEFAULT_VIBRATE;
        }
        
        // Send notification
		String sessionId = invitation.getStringExtra("sessionId");
		NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(sessionId, Utils.NOTIF_ID_LOCATION_SHARE, notif);
	}
	
    /**
     * Remove location share notification
     * 
     * @param context Context
     * @param sessionId Session ID
     */
	public static void removeLocationSharingNotification(Context context, String sessionId) {
		NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(sessionId, Utils.NOTIF_ID_LOCATION_SHARE);
	}
    
    /**
     * Quit the session
     */
    private void quitSession() {
		// Stop session
	    Thread thread = new Thread() {
	    	public void run() {
	        	try {
	                if (sharingSession != null) {
                		sharingSession.removeSessionListener(geolocSharingEventListener);
                		sharingSession.cancelSession();
	                }
	        	} catch(Exception e) {
	        	}
            	sharingSession = null;
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
		inflater.inflate(R.menu.menu_geoloc_sharing, menu);
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
