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

package com.orangelabs.rcs.ri.messaging;

import java.io.File;

import org.gsma.joyn.JoynService;
import org.gsma.joyn.JoynServiceListener;
import org.gsma.joyn.ft.FileTransfer;
import org.gsma.joyn.ft.FileTransferIntent;
import org.gsma.joyn.ft.FileTransferListener;
import org.gsma.joyn.ft.FileTransferService;

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
import android.net.Uri;
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
 * Received file transfer
 * 
 * @author Jean-Marc AUFFRET
 */
public class ReceiveFileTransfer extends Activity implements JoynServiceListener {
    /**
     * UI handler
     */
    private final Handler handler = new Handler();
    
	/**
	 * File transfer API
	 */
    private FileTransferService ftApi;
    
	/**
	 * Transfer ID
	 */
    private String transferId;
    
    /**
     * Remote Contact
     */
    private String remoteContact;
   
    /**
     * File size
     */
    private long fileSize;
    
    /**
     * File type
     */
    private String fileType;
    
    /**
     * File transfer
     */
    private FileTransfer fileTransfer = null;
    
    /**
     * File transfer listener
     */
    private FileTransferListener ftListener = new MyFileTransferListener();
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.filetransfer_receive);

        // Get invitation info
        transferId = getIntent().getStringExtra(FileTransferIntent.EXTRA_TRANSFER_ID);
		remoteContact = getIntent().getStringExtra(FileTransferIntent.EXTRA_CONTACT);
		fileSize = getIntent().getLongExtra(FileTransferIntent.EXTRA_FILESIZE, -1);
		fileType = getIntent().getStringExtra(FileTransferIntent.EXTRA_FILETYPE);
		
		// Remove the notification
        ReceiveFileTransfer.removeFileTransferNotification(this, transferId);
        
        // Instanciate API
        ftApi = new FileTransferService(getApplicationContext(), this);
        
        // Connect API
        ftApi.connect();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
        // Remove session listener
        if (fileTransfer != null) {
        	try {
        		fileTransfer.removeEventListener(ftListener);
        	} catch(Exception e) {
        	}
        }

        // Disconnect API
        ftApi.disconnect();
	}
	
	/**
	 * Accept invitation
	 */
	private void acceptInvitation() {
    	Thread thread = new Thread() {
        	public void run() {
            	try {
            		// Accept the invitation
        			fileTransfer.acceptInvitation();
            	} catch(Exception e) {
        			handler.post(new Runnable() { 
        				public void run() {
        					Utils.showMessageAndExit(ReceiveFileTransfer.this, getString(R.string.label_invitation_failed));
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
            		fileTransfer.removeEventListener(ftListener);
        			fileTransfer.rejectInvitation();
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
			// Get the file transfer session
    		fileTransfer = ftApi.getFileTransfer(transferId);
			if (fileTransfer == null) {
				// Session not found or expired
				Utils.showMessageAndExit(ReceiveFileTransfer.this, getString(R.string.label_session_has_expired));
				return;
			}
			fileTransfer.addEventListener(ftListener);
			
			String size;
	    	if (fileSize != -1) {
	    		size = getString(R.string.label_file_size, " "+ (fileSize/1024), " Kb");
	    	} else {
	    		size = getString(R.string.label_file_size_unknown);
	    	}

	    	// Display transfer infos
            setTitle(R.string.title_recv_file_transfer);
    		TextView from = (TextView)findViewById(R.id.from);
	        from.setText(getString(R.string.label_from) + " " + remoteContact);
	    	TextView sizeTxt = (TextView)findViewById(R.id.image_size);
	    	sizeTxt.setText(size);

	    	if (FileTransferService.getConfiguration().getAutoAcceptMode()) {
	    		// Auto accept
	    	} else {
				// Manual accept
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.title_recv_file_transfer);
				builder.setMessage(getString(R.string.label_from) +	remoteContact +	"\n" + size);
				builder.setCancelable(false);
				builder.setIcon(R.drawable.ri_file_transfer_icon);
				builder.setPositiveButton(getString(R.string.label_accept), acceptBtnListener);
				builder.setNegativeButton(getString(R.string.label_decline), declineBtnListener);
				builder.show();
			}
		} catch(Exception e) {
			handler.post(new Runnable(){
				public void run(){					
					Utils.showMessageAndExit(ReceiveFileTransfer.this, getString(R.string.label_api_failed));
				}
			});
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
		Utils.showMessageAndExit(ReceiveFileTransfer.this, getString(R.string.label_api_disabled));
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
     * File transfer event listener
     */
    private class MyFileTransferListener extends FileTransferListener {
    	/**
    	 * Callback called when the file has been transfered
    	 * 
    	 * @param filename Filename including the path of the transfered file
    	 */
    	public void onFileTransfered(final String filename) {
			handler.post(new Runnable() { 
				public void run() {
					TextView statusView = (TextView)findViewById(R.id.progress_status);
					statusView.setText("transfered");
					
					// Make sure progress bar is at the end
			        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progress_bar);
			        progressBar.setProgress(progressBar.getMax());

			        if (fileType.equals("text/vcard")) {
			        	// Show the transfered vCard
			        	File file = new File(filename);
			    		Uri uri = Uri.fromFile(file);
			    		Intent intent = new Intent(Intent.ACTION_VIEW);
			    		intent.setDataAndType(uri, "text/x-vcard");   		
			    		startActivity(intent);
			        } else {
				        // Show the transfered image
				        Utils.showPictureAndExit(ReceiveFileTransfer.this, filename);
			        }
				}
			});
		}

		/**
		 * Callback called when the file transfer has been aborted
		 */
		public void onTransferAborted() {
			handler.post(new Runnable() { 
				public void run() {
					Utils.showMessageAndExit(ReceiveFileTransfer.this, getString(R.string.label_sharing_aborted));
				}
			});
		}

		/**
		 * Callback called when the transfer has failed
		 * 
		 * @param error Error
		 * @see FileTransfer.Error
		 */
		public void onTransferError(final int error) {
			handler.post(new Runnable() { 
				public void run() {
                    if (error == FileTransfer.Error.SIZE_TOO_BIG) {
                        Utils.showMessageAndExit(ReceiveFileTransfer.this,
                                getString(R.string.label_transfer_failed_too_big));
                    } else {
                        Utils.showMessageAndExit(ReceiveFileTransfer.this,
                                getString(R.string.label_transfer_failed, error));
                    }
				}
			});
		}

		/**
		 * Callback called during the transfer progress
		 * 
		 * @param currentSize Current transfered size in bytes
		 * @param totalSize Total size to transfer in bytes
		 */
		public void onTransferProgress(final long currentSize, final long totalSize) {
			handler.post(new Runnable() { 
    			public void run() {
    				updateProgressBar(currentSize, totalSize);
    			}
    		});
		}

		/**
		 * Callback called when the file transfer is started
		 */
		public void onTransferStarted() {
			handler.post(new Runnable() { 
				public void run() {
					TextView statusView = (TextView)findViewById(R.id.progress_status);
					statusView.setText("started");
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
     * Add file transfer notification
     * 
     * @param context Context
     * @param invitation Intent invitation
     */
    public static void addFileTransferInvitationNotification(Context context, Intent invitation) {
    	// Create notification
		Intent intent = new Intent(invitation);
		intent.setClass(context, ReceiveFileTransfer.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notif = new Notification(R.drawable.ri_notif_file_transfer_icon,
        		context.getString(R.string.title_recv_file_transfer),
        		System.currentTimeMillis());
        notif.flags = Notification.FLAG_AUTO_CANCEL;
        notif.setLatestEventInfo(context,
        		context.getString(R.string.title_recv_file_transfer),
        		context.getString(R.string.label_from) + " " + Utils.formatCallerId(invitation),
        		contentIntent);
		notif.sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    	notif.defaults |= Notification.DEFAULT_VIBRATE;
        
        // Send notification
		String transferId = invitation.getStringExtra(FileTransferIntent.EXTRA_TRANSFER_ID);
		NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(transferId, Utils.NOTIF_ID_FT, notif);
    }
    
	/**
     * Remove file transfer notification
     * 
     * @param context Context
     * @param transferId Transfer ID
     */
    public static void removeFileTransferNotification(Context context, String transferId) {
		NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(transferId, Utils.NOTIF_ID_FT);
    }

    /**
     * Quit the session
     */
    private void quitSession() {
		// Stop session
        Thread thread = new Thread() {
        	public void run() {
            	try {
                    if (fileTransfer != null) {
                		fileTransfer.removeEventListener(ftListener);
                		fileTransfer.abortTransfer();
                    }
            	} catch(Exception e) {
            	}
            	fileTransfer = null;
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
		inflater.inflate(R.menu.menu_ft, menu);
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
