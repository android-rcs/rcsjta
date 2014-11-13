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

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.RcsCommon;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.ish.ImageSharing;
import com.gsma.services.rcs.ish.ImageSharingListener;
import com.gsma.services.rcs.ish.ImageSharingService;
import com.orangelabs.rcs.ri.ApiConnectionManager;
import com.orangelabs.rcs.ri.ApiConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsDisplayName;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Receive image sharing
 * 
 * @author Jean-Marc AUFFRET
 * @author YPLO6403
 */
public class ReceiveImageSharing extends Activity {
    /**
     * UI handler
     */
    private final Handler handler = new Handler();
    
	/**
     * Image sharing
     */
    private ImageSharing imageSharing;
    
    /**
     * The Image Sharing Data Object 
     */
    ImageSharingDAO ishDao;
    
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
   	private static final String LOGTAG = LogUtils.getTag(ReceiveImageSharing.class.getSimpleName());
   	
    /**
     * Image sharing listener
     */
	private ImageSharingListener ishListener = new ImageSharingListener() {

		@Override
		public void onProgressUpdate(ContactId contact, String sharingId, final long currentSize, final long totalSize) {
			// Discard event if not for current sharingId
			if (ReceiveImageSharing.this.ishDao == null || !ReceiveImageSharing.this.ishDao.getSharingId().equals(sharingId)) {
				return;
			}
			handler.post(new Runnable() {
				public void run() {
					// Display sharing progress
					updateProgressBar(currentSize, totalSize);
				}
			});
		}

		@Override
		public void onStateChanged(ContactId contact, String sharingId, final int state, int reasonCode) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onImageSharingStateChanged contact=" + contact + " sharingId=" + sharingId + " state=" + state
						+ " reason=" + reasonCode);
			}
			if (state > RiApplication.ISH_STATES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onImageSharingStateChanged unhandled state=" + state);
				}
				return;
			}
			if (reasonCode > RiApplication.ISH_REASON_CODES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onImageSharingStateChanged unhandled reason=" + reasonCode);
				}
				return;
			}
			// Discard event if not for current sharingId
			if (ReceiveImageSharing.this.ishDao == null || !ReceiveImageSharing.this.ishDao.getSharingId().equals(sharingId)) {
				return;
			}
			final String _reasonCode = RiApplication.ISH_REASON_CODES[reasonCode];
			final String _state = RiApplication.ISH_STATES[state];
			handler.post(new Runnable() {
				public void run() {
					
					TextView statusView = (TextView) findViewById(R.id.progress_status);
					switch (state) {
					case ImageSharing.State.STARTED:
						// Display session status
						statusView.setText(_state);
						break;

					case ImageSharing.State.ABORTED:
						// Session is aborted: exit
						Utils.showMessageAndExit(ReceiveImageSharing.this, getString(R.string.label_sharing_aborted, _reasonCode), exitOnce);
						break;

					case ImageSharing.State.FAILED:
						// Session is failed: exit
						Utils.showMessageAndExit(ReceiveImageSharing.this, getString(R.string.label_sharing_failed, _reasonCode), exitOnce);
						break;
						
					case ImageSharing.State.REJECTED:
						// Session is failed: exit
						Utils.showMessageAndExit(ReceiveImageSharing.this, getString(R.string.label_sharing_rejected, _reasonCode), exitOnce);
						break;

					case ImageSharing.State.TRANSFERRED:
						// Display transfer progress
						statusView.setText(_state);
						// Make sure progress bar is at the end
						ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
						progressBar.setProgress(progressBar.getMax());

						// Show the shared image
						Utils.showPictureAndExit(ReceiveImageSharing.this, ishDao.getFile());
						break;

					default:
						// Display session status
						statusView.setText(_state);
						if (LogUtils.isActive) {
							Log.d(LOGTAG, "onImageSharingStateChanged " + getString(R.string.label_ish_state_changed, _state, _reasonCode));
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
				
		// Register to API connection manager
		connectionManager = ApiConnectionManager.getInstance(this);
		if (connectionManager == null || !connectionManager.isServiceConnected(RcsServiceName.IMAGE_SHARING, RcsServiceName.CONTACTS)) {
			Utils.showMessageAndExit(this, getString(R.string.label_service_not_available), exitOnce);
		} else {
			connectionManager.startMonitorServices(this, exitOnce, RcsServiceName.IMAGE_SHARING, RcsServiceName.CONTACTS);
			initiateImageSharing();
		}
    }

    @Override
    public void onDestroy() {
		super.onDestroy();
		if (connectionManager == null) {
			return;
		}
		connectionManager.stopMonitorServices(this);
		if (connectionManager.isServiceConnected(RcsServiceName.IMAGE_SHARING)) {
			// Remove file transfer listener
			try {
				connectionManager.getImageSharingApi().removeEventListener(ishListener);
			} catch (Exception e) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "Failed to remove listener", e);
				}
			}
		}
    }
    
    private void initiateImageSharing() {
    	ImageSharingService ishApi = connectionManager.getImageSharingApi();
		try {
			// Add service listener
			ishApi.addEventListener(ishListener);
			
			// Get the image sharing
			imageSharing = ishApi.getImageSharing(ishDao.getSharingId());
			if (imageSharing == null) {
				// Session not found or expired
				Utils.showMessageAndExit(this, getString(R.string.label_session_not_found), exitOnce);
				return;
			}
			
			ContactId remote = ishDao.getContact();
			String displayName = RcsDisplayName.get(this, remote);
			String from = RcsDisplayName.convert(this, RcsCommon.Direction.INCOMING, remote, displayName);

			// Display sharing infos
			TextView fromTextView = (TextView) findViewById(R.id.from);
			fromTextView.setText(getString(R.string.label_from_args, from));

			String size = getString(R.string.label_file_size, ishDao.getSize() / 1024);
			TextView sizeTxt = (TextView) findViewById(R.id.image_size);
			sizeTxt.setText(size);
	    	
			// Display accept/reject dialog
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.title_image_sharing);
			builder.setMessage(getString(R.string.label_ft_from_size, displayName, ishDao.getSize() / 1024));
			builder.setCancelable(false);
			builder.setIcon(R.drawable.ri_notif_csh_icon);
			builder.setPositiveButton(getString(R.string.label_accept), acceptBtnListener);
			builder.setNegativeButton(getString(R.string.label_decline), declineBtnListener);
			builder.show();
	    } catch(RcsServiceNotAvailableException e) {
	    	if (LogUtils.isActive) {
				Log.e(LOGTAG, e.getMessage(), e);
			}
	    	Utils.showMessageAndExit(this, getString(R.string.label_api_disabled), exitOnce);
	    } catch(RcsServiceException e) {
	    	if (LogUtils.isActive) {
				Log.e(LOGTAG, e.getMessage(), e);
			}
	    	Utils.showMessageAndExit(this, getString(R.string.label_api_failed), exitOnce);
		}
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
    		Utils.showMessageAndExit(this, getString(R.string.label_invitation_failed), exitOnce);
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
