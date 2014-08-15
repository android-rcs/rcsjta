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

package com.orangelabs.rcs.ri.sharing.image;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gsma.services.rcs.JoynService;
import com.gsma.services.rcs.JoynServiceException;
import com.gsma.services.rcs.JoynServiceListener;
import com.gsma.services.rcs.JoynServiceNotAvailableException;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.ish.ImageSharing;
import com.gsma.services.rcs.ish.ImageSharingListener;
import com.gsma.services.rcs.ish.ImageSharingService;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Receive image sharing
 * 
 * @author Jean-Marc AUFFRET
 * @author YPLO6403
 */
public class ReceiveImageSharing extends Activity implements JoynServiceListener {
    /**
     * UI handler
     */
    private final Handler handler = new Handler();
    
    /**
	 * Image sharing API
	 */
    private ImageSharingService ishApi;
    
	/**
     * Image sharing
     */
    private ImageSharing imageSharing;
    
    /**
     * The Image Sharing Data Object 
     */
    ImageSharingDAO ishDao;
    
    /**
     * Array of Image sharing states
     */
    private static final String[] ISH_STATES = RiApplication.getContext().getResources().getStringArray(R.array.ish_states);
    
    /**
     * Array of Image sharing reason codes
     */
	private static final String[] ISH_REASON_CODES = RiApplication.getContext().getResources().getStringArray(R.array.ish_reason_codes);
    
	private boolean serviceConnected = false;
	
	/**
	 * A locker to exit only once
	 */
	private LockAccess exitOnce = new LockAccess();
		
	/**
   	 * The log tag for this class
   	 */
   	private static final String LOGTAG = LogUtils.getTag(ReceiveImageSharing.class.getSimpleName());
   	
    /**
     * Image sharing listener
     */
	private ImageSharingListener ishListener = new ImageSharingListener() {

		@Override
		public void onImageSharingProgress(ContactId contact, String sharingId, final long currentSize, final long totalSize) {
			handler.post(new Runnable() {
				public void run() {
					// Display sharing progress
					updateProgressBar(currentSize, totalSize);
				}
			});
		}

		@Override
		public void onImageSharingStateChanged(ContactId contact, String sharingId, final int state) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onImageSharingStateChanged contact=" + contact + " sharingId=" + sharingId + " state=" + state);
			}
			if (state > ISH_STATES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onImageSharingStateChanged unhandled state=" + state);
				}
				return;
			}
			// TODO : handle reason code (CR025)
			final String reason = ISH_REASON_CODES[0];
			final String notif = getString(R.string.label_ish_state_changed, ISH_STATES[state], reason);
			handler.post(new Runnable() {
				public void run() {
					
					TextView statusView = (TextView) findViewById(R.id.progress_status);
					switch (state) {
					case ImageSharing.State.STARTED:
						// Display session status
						statusView.setText("started");
						break;

					case ImageSharing.State.ABORTED:
						// Session is aborted: display session status then exit
						Utils.showMessageAndExit(ReceiveImageSharing.this, getString(R.string.label_sharing_aborted, reason), exitOnce);
						break;

					case ImageSharing.State.FAILED:
						// Session is failed: exit
						Utils.showMessageAndExit(ReceiveImageSharing.this, getString(R.string.label_sharing_failed, reason), exitOnce);
						break;

					case ImageSharing.State.TRANSFERRED:
						// Display transfer progress
						statusView.setText("transferred");
						// Make sure progress bar is at the end
						ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
						progressBar.setProgress(progressBar.getMax());

						// Show the shared image
						Utils.showPictureAndExit(ReceiveImageSharing.this, ishDao.getFile());
						break;

					default:
						if (LogUtils.isActive) {
							Log.d(LOGTAG, "onImageSharingStateChanged " + notif);
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
        setContentView(R.layout.image_sharing_receive);
        
        // Set title
		setTitle(R.string.title_image_sharing);

		// Get invitation info
		ishDao = (ImageSharingDAO) (getIntent().getExtras().getParcelable(ImageSharingIntentService.BUNDLE_ISHDAO_ID));
		if (ishDao == null) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "onCreate cannot read Image Sharing invitation");
			}
			finish();
			return;
		}
				
        // Instantiate API
		ishApi = new ImageSharingService(getApplicationContext(), this);
		
		// Connect API
		ishApi.connect();
    }

    @Override
    public void onDestroy() {
		super.onDestroy();
		if (serviceConnected) {
			// Remove file transfer listener
			try {
				ishApi.removeEventListener(ishListener);
			} catch (Exception e) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "Failed to remove listener", e);
				}
			}
			// Disconnect API
			ishApi.disconnect();
		}
    }
    
    /**
     * Callback called when service is connected. This method is called when the
     * service is well connected to the RCS service (binding procedure successful):
     * this means the methods of the API may be used.
     */
    public void onServiceConnected() {
		try {
			// Add service listener
			ishApi.addEventListener(ishListener);
			serviceConnected = true;
			
			// Get the image sharing
			imageSharing = ishApi.getImageSharing(ishDao.getSharingId());
			if (imageSharing == null) {
				// Session not found or expired
				Utils.showMessageAndExit(ReceiveImageSharing.this, getString(R.string.label_session_not_found), exitOnce);
				return;
			}
			
			String size;
	    	if (ishDao.getSize() != -1) {
	    		size = getString(R.string.label_file_size, " " + (ishDao.getSize()/1024), " Kb");
	    	} else {
	    		size = getString(R.string.label_file_size_unknown);
	    	}
			
	    	// Display sharing infos
    		TextView from = (TextView)findViewById(R.id.from);
	        from.setText(getString(R.string.label_from) + " " + ishDao.getContact());
	    	TextView sizeTxt = (TextView)findViewById(R.id.image_size);
	    	sizeTxt.setText(size);
	    	
			// Display accept/reject dialog
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.title_image_sharing);
			builder.setMessage(getString(R.string.label_from) +	" " + ishDao.getContact() + "\n" + size);
			builder.setCancelable(false);
			builder.setIcon(R.drawable.ri_notif_csh_icon);
			builder.setPositiveButton(getString(R.string.label_accept), acceptBtnListener);
			builder.setNegativeButton(getString(R.string.label_decline), declineBtnListener);
			builder.show();
	    } catch(JoynServiceNotAvailableException e) {
	    	e.printStackTrace();
			Utils.showMessageAndExit(ReceiveImageSharing.this, getString(R.string.label_api_disabled), exitOnce);
	    } catch(JoynServiceException e) {
	    	e.printStackTrace();
			Utils.showMessageAndExit(ReceiveImageSharing.this, getString(R.string.label_api_failed), exitOnce);
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
    	serviceConnected = false;
		Utils.showMessageAndExit(ReceiveImageSharing.this, getString(R.string.label_api_disabled), exitOnce);
    }    
    
	/**
	 * Accept invitation
	 */
	private void acceptInvitation() {
    	try {
    		// Accept the invitation
    		imageSharing.acceptInvitation();
    	} catch(Exception e) {
    		e.printStackTrace();
    		Utils.showMessageAndExit(ReceiveImageSharing.this, getString(R.string.label_invitation_failed), exitOnce);
    	}
	}
	
	/**
	 * Reject invitation
	 */
	private void rejectInvitation() {
    	try {
    		// Reject the invitation
    		imageSharing.rejectInvitation();
    	} catch(Exception e) {
    		e.printStackTrace();
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
     * Show the transfer progress
     * 
     * @param currentSize Current size transferred
     * @param totalSize Total size to be transferred
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
     * Quit the session
     */
    private void quitSession() {
		// Stop session
    	try {
            if (imageSharing != null) {
        		imageSharing.abortSharing();
            }
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	imageSharing = null;
		
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
