/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/
package com.gsma.services.rcs.ish;

import java.lang.reflect.Method;
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

import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceListener;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.contacts.ContactId;

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
public class ImageSharingService extends RcsService {

	private static final int KITKAT_VERSION_CODE = 19;

	private static final String TAKE_PERSISTABLE_URI_PERMISSION_METHOD_NAME = "takePersistableUriPermission";

	private static final Class<?>[] TAKE_PERSISTABLE_URI_PERMISSION_PARAM_TYPES = new Class[] {
			Uri.class, int.class
	};

	/**
	 * API
	 */
	private IImageSharingService mApi;
	
	private static final String ERROR_CNX = "ImageSharing service not connected";
	
    /**
     * Constructor
     * 
     * @param ctx Application context
     * @param listener Service listener
     */
    public ImageSharingService(Context ctx, RcsServiceListener listener) {
    	super(ctx, listener);
    }

    /**
     * Connects to the API
     */
    public void connect() {
    	mCtx.bindService(new Intent(IImageSharingService.class.getName()), apiConnection, 0);
    }
    
    /**
     * Disconnects from the API
     */
    public void disconnect() {
    	try {
    		mCtx.unbindService(apiConnection);
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
        mApi = (IImageSharingService)api;
    }
    
    /**
	 * Service connection
	 */
	private ServiceConnection apiConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
        	setApi(IImageSharingService.Stub.asInterface(service));
        	if (mListener != null) {
        		mListener.onServiceConnected();
        	}
        }

        public void onServiceDisconnected(ComponentName className) {
        	setApi(null);
        	if (mListener != null) {
        		mListener.onServiceDisconnected(RcsService.Error.CONNECTION_LOST);
        	}
        }
    };
    
    /**
     * Returns the configuration of image sharing service
     * 
     * @return Configuration
     * @throws RcsServiceException
     */
    public ImageSharingServiceConfiguration getConfiguration() throws RcsServiceException {
		if (mApi != null) {
			try {
				return new ImageSharingServiceConfiguration(mApi.getConfiguration());
			} catch(Exception e) {
				throw new RcsServiceException(e);
			}
		} else {
			throw new RcsServiceNotAvailableException(ERROR_CNX);
		}
	}

	private void grantUriPermissionToStackServices(Uri file) {
		Intent imageSharingServiceIntent = new Intent(IImageSharingService.class.getName());
		List<ResolveInfo> stackServices = mCtx.getPackageManager().queryIntentServices(
				imageSharingServiceIntent, 0);
		for (ResolveInfo stackService : stackServices) {
			mCtx.grantUriPermission(stackService.serviceInfo.packageName, file,
					Intent.FLAG_GRANT_READ_URI_PERMISSION);
		}
	}

	/**
	 * Using reflection to persist Uri permission in order to support backward
	 * compatibility since this API is available only from Kitkat onwards.
	 *
	 * @param file Uri of file to share
	 * @throws RcsServiceException
	 */
	private void takePersistableUriPermission(Uri file) throws RcsServiceException {
		try {
			ContentResolver contentResolver = mCtx.getContentResolver();
			Method takePersistableUriPermissionMethod = contentResolver.getClass().getMethod(
					TAKE_PERSISTABLE_URI_PERMISSION_METHOD_NAME,
					TAKE_PERSISTABLE_URI_PERMISSION_PARAM_TYPES);
			Object[] methodArgs = new Object[] {
					file,
					Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
			};
			takePersistableUriPermissionMethod.invoke(contentResolver, methodArgs);
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}

    /**
     * Grant permission to the stack and persist access permission
     * 
     * @param file the file URI
     * @throws RcsServiceException
     */
    private void tryToGrantAndPersistUriPermission(Uri file) throws RcsServiceException {
        if (ContentResolver.SCHEME_CONTENT.equals(file.getScheme())) {
            // Granting temporary read Uri permission from client to
            // stack service if it is a content URI
            grantUriPermissionToStackServices(file);
            // Persist Uri access permission for the client
            // to be able to read the contents from this Uri even
            // after the client is restarted after device reboot.
            if (android.os.Build.VERSION.SDK_INT >= KITKAT_VERSION_CODE) {
                takePersistableUriPermission(file);
            }
        }
    }

    /**
     * Shares an image with a contact. The parameter file contains the URI
     * of the image to be shared (for a local or a remote image). An exception if thrown if there is
     * no ongoing CS call. The parameter contact supports the following formats: MSISDN
     * in national or international format, SIP address, SIP-URI or Tel-URI. If the format
     * of the contact is not supported an exception is thrown.
     * 
     * @param contact Contact identifier
     * @param file Uri of file to share
     * @return Image sharing
     * @throws RcsServiceException
     */
    public ImageSharing shareImage(ContactId contact, Uri file) throws RcsServiceException {
		if (mApi != null) {
			try {
				tryToGrantAndPersistUriPermission(file);
				IImageSharing sharingIntf = mApi.shareImage(contact, file);
				if (sharingIntf != null) {
					return new ImageSharing(sharingIntf);
				} else {
					return null;
				}
			} catch(Exception e) {
				throw new RcsServiceException(e);
			}
		} else {
			throw new RcsServiceNotAvailableException(ERROR_CNX);
		}
    }    
    
    /**
     * Returns the list of image sharings in progress
     * 
     * @return List of image sharings
     * @throws RcsServiceException
     */
    public Set<ImageSharing> getImageSharings() throws RcsServiceException {
		if (mApi != null) {
			try {
	    		Set<ImageSharing> result = new HashSet<ImageSharing>();
				List<IBinder> ishList = mApi.getImageSharings();
				for (IBinder binder : ishList) {
					ImageSharing sharing = new ImageSharing(IImageSharing.Stub.asInterface(binder));
					result.add(sharing);
				}
				return result;
			} catch(Exception e) {
				throw new RcsServiceException(e);
			}
		} else {
			throw new RcsServiceNotAvailableException(ERROR_CNX);
		}
    }    

    /**
     * Returns a current image sharing from its unique ID
     * 
     * @param sharingId Sharing ID
     * @return Image sharing or null if not found
     * @throws RcsServiceException
     */
    public ImageSharing getImageSharing(String sharingId) throws RcsServiceException {
		if (mApi != null) {
			try {
				return new ImageSharing(mApi.getImageSharing(sharingId));
			} catch(Exception e) {
				throw new RcsServiceException(e);
			}
		} else {
			throw new RcsServiceNotAvailableException(ERROR_CNX);
		}
    }    

	/**
	 * Adds a listener on image sharing events
	 * 
	 * @param listener Listener
	 * @throws RcsServiceException
	 */
	public void addEventListener(ImageSharingListener listener) throws RcsServiceException {
		if (mApi != null) {
			try {
				mApi.addEventListener2(listener);
			} catch (Exception e) {
				throw new RcsServiceException(e);
			}
		} else {
			throw new RcsServiceNotAvailableException(ERROR_CNX);
		}
	}

	/**
	 * Removes a listener on image sharing events
	 * 
	 * @param listener Listener
	 * @throws RcsServiceException
	 */
	public void removeEventListener(ImageSharingListener listener) throws RcsServiceException {
		if (mApi != null) {
			try {
				mApi.removeEventListener2(listener);
			} catch (Exception e) {
				throw new RcsServiceException(e);
			}
		} else {
			throw new RcsServiceNotAvailableException(ERROR_CNX);
		}
	}
}
