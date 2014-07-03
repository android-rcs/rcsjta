/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications AB.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications AB.
 * Modifications are licensed under the License.
 ******************************************************************************/
package com.gsma.services.rcs.ish;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.IBinder;
import android.os.IInterface;

import com.gsma.services.rcs.JoynContactFormatException;
import com.gsma.services.rcs.JoynService;
import com.gsma.services.rcs.JoynServiceException;
import com.gsma.services.rcs.JoynServiceListener;
import com.gsma.services.rcs.JoynServiceNotAvailableException;

/**
 * This class offers the main entry point to transfer image during
 * a CS call. Several applications may connect/disconnect to the API.
 * 
 * The parameter contact in the API supports the following formats:
 * MSISDN in national or international format, SIP address, SIP-URI
 * or Tel-URI.
 * 
 * @author Jean-Marc AUFFRET
 */
public class ImageSharingService extends JoynService {
	/**
	 * API
	 */
	private IImageSharingService api = null;
	
    /**
     * Constructor
     * 
     * @param ctx Application context
     * @param listener Service listener
     */
    public ImageSharingService(Context ctx, JoynServiceListener listener) {
    	super(ctx, listener);
    }

    /**
     * Connects to the API
     */
    public void connect() {
    	ctx.bindService(new Intent(IImageSharingService.class.getName()), apiConnection, 0);
    }
    
    /**
     * Disconnects from the API
     */
    public void disconnect() {
    	try {
    		ctx.unbindService(apiConnection);
        } catch(IllegalArgumentException e) {
        	// Nothing to do
        }
    }

	/**
	 * Set API interface
	 * 
	 * @param api API interface
	 */
    protected void setApi(IInterface api) {
    	super.setApi(api);
    	
        this.api = (IImageSharingService)api;
    }
    
    /**
	 * Service connection
	 */
	private ServiceConnection apiConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
        	setApi(IImageSharingService.Stub.asInterface(service));
        	if (serviceListener != null) {
        		serviceListener.onServiceConnected();
        	}
        }

        public void onServiceDisconnected(ComponentName className) {
        	setApi(null);
        	if (serviceListener != null) {
        		serviceListener.onServiceDisconnected(JoynService.Error.CONNECTION_LOST);
        	}
        }
    };
    
    /**
     * Returns the configuration of image sharing service
     * 
     * @return Configuration
     * @throws JoynServiceException
     */
    public ImageSharingServiceConfiguration getConfiguration() throws JoynServiceException {
		if (api != null) {
			try {
				return api.getConfiguration();
			} catch(Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
	}

	private void grantUriPermissionToStackServices(Uri file) {
		Intent imageSharingServiceIntent = new Intent(IImageSharingService.class.getName());
		List<ResolveInfo> stackServices = ctx.getPackageManager().queryIntentServices(
				imageSharingServiceIntent, 0);
		for (ResolveInfo stackService : stackServices) {
			ctx.grantUriPermission(stackService.serviceInfo.packageName, file,
					Intent.FLAG_GRANT_READ_URI_PERMISSION);
		}
	}

	private void persistUriPermissionForClient(Uri file) {
		ctx.getContentResolver().takePersistableUriPermission(file,
				Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
	}

    /**
     * Shares an image with a contact. The parameter file contains the URI
     * of the image to be shared(for a local or a remote image). An exception if thrown if there is
     * no ongoing CS call. The parameter contact supports the following formats: MSISDN
     * in national or international format, SIP address, SIP-URI or Tel-URI. If the format
     * of the contact is not supported an exception is thrown.
     * 
     * @param contact Contact
     * @param file Uri of file to share
     * @param listener Image sharing event listener
     * @return Image sharing
     * @throws JoynServiceException
	 * @throws JoynContactFormatException
     */
    public ImageSharing shareImage(String contact, Uri file, ImageSharingListener listener) throws JoynServiceException, JoynContactFormatException {
		if (api != null) {
			try {
				// Allow permission to the stack server for content URI if release is KitKat or greater
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT
						&& ContentResolver.SCHEME_CONTENT.equals(file.getScheme())) {
					// Granting temporary read Uri permission from client to
					// stack service if it is a content URI
					grantUriPermissionToStackServices(file);
					// Persist Uri access permission for the client
					// to be able to read the contents from this Uri even
					// after the client is restarted after device reboot.
					persistUriPermissionForClient(file);
				}
				IImageSharing sharingIntf = api.shareImage(contact, file, listener);
				if (sharingIntf != null) {
					return new ImageSharing(sharingIntf);
				} else {
					return null;
				}
			} catch(Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
    }    
    
    /**
     * Returns the list of image sharings in progress
     * 
     * @return List of image sharings
     * @throws JoynServiceException
     */
    public Set<ImageSharing> getImageSharings() throws JoynServiceException {
		if (api != null) {
			try {
	    		Set<ImageSharing> result = new HashSet<ImageSharing>();
				List<IBinder> ishList = api.getImageSharings();
				for (IBinder binder : ishList) {
					ImageSharing sharing = new ImageSharing(IImageSharing.Stub.asInterface(binder));
					result.add(sharing);
				}
				return result;
			} catch(Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
    }    

    /**
     * Returns a current image sharing from its unique ID
     * 
     * @param sharingId Sharing ID
     * @return Image sharing or null if not found
     * @throws JoynServiceException
     */
    public ImageSharing getImageSharing(String sharingId) throws JoynServiceException {
		if (api != null) {
			try {
				IImageSharing sharingIntf = api.getImageSharing(sharingId);
				if (sharingIntf != null) {
					return new ImageSharing(sharingIntf);
				} else {
					return null;
				}
			} catch(Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
    }    
    
    /**
     * Returns a current image sharing from its invitation Intent
     * 
     * @param intent Invitation intent
     * @return Image sharing or null if not found
     * @throws JoynServiceException
     */
    public ImageSharing getImageSharingFor(Intent intent) throws JoynServiceException {
		if (api != null) {
			try {
				String sharingId = intent.getStringExtra(ImageSharingIntent.EXTRA_SHARING_ID);
				if (sharingId != null) {
					return getImageSharing(sharingId);
				} else {
					return null;
				}
			} catch(Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
    }     
    
    /**
	 * Registers a new image sharing invitation listener
	 * 
	 * @param listener New image sharing listener
	 * @throws JoynServiceException
	 */
	public void addNewImageSharingListener(NewImageSharingListener listener) throws JoynServiceException {
		if (api != null) {
			try {
				api.addNewImageSharingListener(listener);
			} catch(Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
	}

	/**
	 * Unregisters a new image sharing invitation listener
	 * 
	 * @param listener New image sharing listener
	 * @throws JoynServiceException
	 */
	public void removeNewImageSharingListener(NewImageSharingListener listener) throws JoynServiceException {
		if (api != null) {
			try {
				api.removeNewImageSharingListener(listener);
			} catch(Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
	}
}
