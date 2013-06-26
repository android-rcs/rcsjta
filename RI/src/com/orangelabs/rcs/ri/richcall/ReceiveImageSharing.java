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

import org.gsma.joyn.JoynService;
import org.gsma.joyn.JoynServiceListener;
import org.gsma.joyn.ish.ImageSharing;
import org.gsma.joyn.ish.ImageSharingIntent;
import org.gsma.joyn.ish.ImageSharingListener;
import org.gsma.joyn.ish.ImageSharingService;

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
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Receive image sharing
 * 
 * @author jexa7410
 */
public class ReceiveImageSharing extends Activity implements JoynServiceListener {
    /**
     * UI handler
     */
    private final Handler handler = new Handler();
    
	/**
	 * Sharing ID
	 */
    private String sharingId;
    
    /**
	 * Image sharing API
	 */
    private ImageSharingService ishApi;
    
	/**
     * Image sharing
     */
    private ImageSharing imageSharing = null;
    
    /**
     * Image sharing listener
     */
    private MyImageSharingListener ishListener = new MyImageSharingListener();    
    
    /**
     * Remote Contact
     */
    private String remoteContact;
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    	// Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.image_sharing_receive);

        // Get invitation info
        sharingId = getIntent().getStringExtra(ImageSharingIntent.EXTRA_SHARING_ID);
		remoteContact = getIntent().getStringExtra(ImageSharingIntent.EXTRA_CONTACT);

		// Remove the notification
		ReceiveImageSharing.removeImageSharingNotification(this, sharingId);

        // Instanciate API
		ishApi = new ImageSharingService(getApplicationContext(), this);
		
		// Connect API
		ishApi.connect();
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();

        // Remove file transfer listener
        if (imageSharing != null) {
        	try {
        		imageSharing.removeEventListener(ishListener);
        	} catch(Exception e) {
        	}
        }

        // Disconnect API
        ishApi.disconnect();
    }
    
    /**
     * Callback called when service is connected. This method is called when the
     * service is well connected to the RCS service (binding procedure successfull):
     * this means the methods of the API may be used.
     */
    public void onServiceConnected() {
    	if (imageSharing != null) {
    		try{
    			// Get the image sharing
    			imageSharing = ishApi.getImageSharing(sharingId);
    			if (imageSharing == null) {
    				// Session not found or expired
    				Utils.showMessageAndExit(ReceiveImageSharing.this, getString(R.string.label_session_not_found));
    				return;
    			}
    			imageSharing.addEventListener(ishListener);
    			
    			String sizeDesc;
    			long fileSize = imageSharing.getFileSize();
    	    	if (fileSize != -1) {
    	    		sizeDesc = getString(R.string.label_file_size, " "+ (fileSize/1024), " Kb");
    	    	} else {
    	    		sizeDesc = getString(R.string.label_file_size_unknown);
    	    	}
    			
    			AlertDialog.Builder builder = new AlertDialog.Builder(this);
    			builder.setTitle(R.string.title_recv_image_sharing);
    			builder.setMessage(getString(R.string.label_from) + " " + remoteContact + "\n" + sizeDesc);
    			builder.setCancelable(false);
    			builder.setPositiveButton(getString(R.string.label_accept), acceptBtnListener);
    			builder.setNegativeButton(getString(R.string.label_decline), declineBtnListener);
    			builder.show();    			
    		} catch(Exception e) {
    			handler.post(new Runnable(){
    				public void run(){
    					Utils.showMessageAndExit(ReceiveImageSharing.this, getString(R.string.label_api_failed));
    				}
    			});
			}
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
		Utils.showMessageAndExit(ReceiveImageSharing.this, getString(R.string.label_api_disabled));
    }    

    /**
     * Callback called when service is registered to the RCS/IMS platform
     */
    public void onServiceRegistered() {
    	// Not used here
    }
    
    /**
     * Callback called when service is unregistered from the RCS/IMS platform
     */
    public void onServiceUnregistered() {
    	// Not used here
    }
    
    /**
     * Accept button listener
     */
    private OnClickListener acceptBtnListener = new OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
        	            
            // Set title
            setTitle(R.string.title_recv_image_sharing);
            
    		// Display the remote contact
            TextView from = (TextView)findViewById(R.id.from);
            from.setText(getString(R.string.label_from) + " " + remoteContact);

        	// Display the filename attributes to be shared
            try {
		    	TextView size = (TextView)findViewById(R.id.image_size);
		    	size.setText(getString(R.string.label_file_size, " " + (imageSharing.getFileSize()/1024), " Kb"));
            } catch(Exception e){
            	Utils.showMessageAndExit(ReceiveImageSharing.this, getString(R.string.label_api_failed));
            }
            
            Thread thread = new Thread() {
            	public void run() {
                	try {
                		// Accept the invitation
            			imageSharing.acceptInvitation();
	            	} catch(Exception e) {
	    				handler.post(new Runnable() { 
	    					public void run() {
	    						Utils.showMessageAndExit(ReceiveImageSharing.this, getString(R.string.label_invitation_failed));
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
                		imageSharing.removeEventListener(ishListener);
                		imageSharing.rejectInvitation();
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
     * Image sharing event listener
     */
    private class MyImageSharingListener extends ImageSharingListener {
    	// Sharing started
    	public void onSharingStarted() {
			handler.post(new Runnable() { 
				public void run() {
					// Display session status
					TextView statusView = (TextView)findViewById(R.id.progress_status);
					statusView.setText("started");
				}
			});
    	}
    	
    	// Sharing aborted
    	public void onSharingAborted() {
			handler.post(new Runnable() { 
				public void run() {
					// Display session status
					Utils.showMessageAndExit(ReceiveImageSharing.this, getString(R.string.label_sharing_aborted));
				}
			});
    	}

    	// Sharing error
    	public void onSharingError(final int error) {
			handler.post(new Runnable() { 
				public void run() {
					// Display error
					Utils.showMessageAndExit(ReceiveImageSharing.this, getString(R.string.label_transfer_failed, error));
				}
			});
    	}
    	
    	// Sharing progress
    	public void onSharingProgress(final long currentSize, final long totalSize) {
			handler.post(new Runnable() { 
    			public void run() {
					// Display transfer progress
    				updateProgressBar(currentSize, totalSize);
    			}
    		});
    	}

    	// Image shared
    	public void onImageShared(final String filename) {
			handler.post(new Runnable() { 
				public void run() {
					// Display transfer progress
					TextView statusView = (TextView)findViewById(R.id.progress_status);
					statusView.setText("transfered");
					
					// Make sure progress bar is at the end
			        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progress_bar);
			        progressBar.setProgress(progressBar.getMax());
					
			        // Show the shared image
			        Utils.showPictureAndExit(ReceiveImageSharing.this, filename);			        
				}
			});
    	}
    };

    /**
     * Show the transfer progress
     * 
     * @param currentSize Current size transfered
     * @param totalSize Total size to be transfered
     */
    private void updateProgressBar(long currentSize, long totalSize) {
    	TextView statusView = (TextView)findViewById(R.id.progress_status);
        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progress_bar);
    	
		String value = "" + (currentSize/1024);
		if (totalSize != 0) {
			value += "/" + (totalSize/1024);
		}
		value += " Kb";
		statusView.setText(value);
	    
	    if (currentSize != 0) {
	    	double position = ((double)currentSize / (double)totalSize)*100.0;
	    	progressBar.setProgress((int)position);
	    } else {
	    	progressBar.setProgress(0);
	    }
    }

    /**
     * Add image share notification
     * 
     * @param context Context
     * @param intent Intent invitation
     */
	public static void addImageSharingInvitationNotification(Context context, Intent invitation) {
    	// Get remote contact
		String contact = invitation.getStringExtra(ImageSharingIntent.EXTRA_CONTACT);

		// Create notification
		Intent intent = new Intent(invitation);
		intent.setClass(context, ReceiveImageSharing.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);		
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        String notifTitle = context.getString(R.string.title_recv_image_sharing);
        Notification notif = new Notification(R.drawable.ri_notif_csh_icon,
        		notifTitle,
        		System.currentTimeMillis());
        notif.flags = Notification.FLAG_AUTO_CANCEL;
        notif.setLatestEventInfo(context,
        		notifTitle,
        		context.getString(R.string.label_from) + " " + contact,
        		contentIntent);
		notif.sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    	notif.defaults |= Notification.DEFAULT_VIBRATE;
        
        // Send notification
		String sharingId = invitation.getStringExtra(ImageSharingIntent.EXTRA_SHARING_ID);
		NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(sharingId, Utils.NOTIF_ID_IMAGE_SHARE, notif);
	}
	
    /**
     * Remove image share notification
     * 
     * @param context Context
     * @param sharingId SharingId ID
     */
	public static void removeImageSharingNotification(Context context, String sharingId) {
		NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(sharingId, Utils.NOTIF_ID_IMAGE_SHARE);
	}

    /**
     * Quit the session
     */
    private void quitSession() {
		// Stop session
	    Thread thread = new Thread() {
	    	public void run() {
	        	try {
	                if (imageSharing != null) {
	                	imageSharing.removeEventListener(ishListener);
                		imageSharing.abortSharing();
	                }
	        	} catch(Exception e) {
	        	}
	        	imageSharing = null;
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
		inflater.inflate(R.menu.menu_image_sharing, menu);
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
