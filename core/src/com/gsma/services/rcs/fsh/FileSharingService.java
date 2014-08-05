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
package com.gsma.services.rcs.fsh;

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

import com.gsma.services.rcs.JoynService;
import com.gsma.services.rcs.JoynServiceException;
import com.gsma.services.rcs.JoynServiceListener;
import com.gsma.services.rcs.JoynServiceNotAvailableException;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * This class offers the main entry point to share file link during
 * a CS call. Several applications may connect/disconnect to the API.
 * 
 * The parameter contact in the API supports the following formats:
 * MSISDN in national or international format, SIP address, SIP-URI
 * or Tel-URI.
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileSharingService extends JoynService {

	private static final String TAKE_PERSISTABLE_URI_PERMISSION_METHOD_NAME = "takePersistableUriPermission";

	private static final Class[] TAKE_PERSISTABLE_URI_PERMISSION_PARAM_TYPES = new Class[] {
			Uri.class, int.class
	};

	/**
	 * API
	 */
	private IFileSharingService api = null;
	
    /**
     * Constructor
     * 
     * @param ctx Application context
     * @param listener Service listener
     */
    public FileSharingService(Context ctx, JoynServiceListener listener) {
    	super(ctx, listener);
    }

    /**
     * Connects to the API
     */
    public void connect() {
    	ctx.bindService(new Intent(IFileSharingService.class.getName()), apiConnection, 0);
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
    	
        this.api = (IFileSharingService)api;
    }
    
    /**
	 * Service connection
	 */
	private ServiceConnection apiConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
        	setApi(IFileSharingService.Stub.asInterface(service));
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

	private void grantUriPermissionToStackServices(Uri file) {
		Intent fileSharingServiceIntent = new Intent(IFileSharingService.class.getName());
		List<ResolveInfo> stackServices = ctx.getPackageManager().queryIntentServices(
				fileSharingServiceIntent, 0);
		for (ResolveInfo stackService : stackServices) {
			ctx.grantUriPermission(stackService.serviceInfo.packageName, file,
					Intent.FLAG_GRANT_READ_URI_PERMISSION);
		}
	}

	/**
	 * Using reflection to persist Uri permission in order to support backward
	 * compatibility since this API is available only from Kitkat onwards.
	 *
	 * @param file Uri of file to share
	 * @throws JoynServiceException
	 */
	private void persistUriPermissionForClient(Uri file) throws JoynServiceException {
		try {
			ContentResolver contentResolver = ctx.getContentResolver();
			Method takePersistableUriPermissionMethod = contentResolver.getClass()
					.getDeclaredMethod(TAKE_PERSISTABLE_URI_PERMISSION_METHOD_NAME,
							TAKE_PERSISTABLE_URI_PERMISSION_PARAM_TYPES);
			if (takePersistableUriPermissionMethod == null) {
				return;
			}
			Object[] methodArgs = new Object[] {
					file,
					Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
			};
			takePersistableUriPermissionMethod.invoke(contentResolver, methodArgs);
		} catch (Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}

    /**
     * Shares a file or its link with a contact. The parameter file contains the URI
     * of the file to be shared (for a local or a remote file). An exception if thrown if there is
     * no ongoing CS call. The parameter contact supports the following formats: MSISDN
     * in national or international format, SIP address, SIP-URI or Tel-URI. If the format
     * of the contact is not supported an exception is thrown.
     * 
     * @param contact Contact identifier
     * @param file Uri of file to share
     * @return File sharing
     * @throws JoynServiceException
     */
    public FileSharing shareFile(ContactId contact, Uri file) throws JoynServiceException {
		if (api != null) {
			try {
				if (ContentResolver.SCHEME_CONTENT.equals(file.getScheme())) {
					// Granting temporary read Uri permission from client to
					// stack service if it is a content URI
					grantUriPermissionToStackServices(file);
					// Persist Uri access permission for the client
					// to be able to read the contents from this Uri even
					// after the client is restarted after device reboot.
					persistUriPermissionForClient(file);
				}
				IFileSharing sharingIntf = api.shareFile(contact, file);
				if (sharingIntf != null) {
					return new FileSharing(sharingIntf);
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
     * Returns the list of file sharings in progress
     * 
     * @return List of file sharings
     * @throws JoynServiceException
     */
    public Set<FileSharing> getFileSharings() throws JoynServiceException {
		if (api != null) {
			try {
	    		Set<FileSharing> result = new HashSet<FileSharing>();
				List<IBinder> ishList = api.getFileSharings();
				for (IBinder binder : ishList) {
					FileSharing sharing = new FileSharing(IFileSharing.Stub.asInterface(binder));
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
     * Returns a current file sharing from its unique ID
     * 
     * @param sharingId Sharing ID
     * @return File sharing or null if not found
     * @throws JoynServiceException
     */
    public FileSharing getFileSharing(String sharingId) throws JoynServiceException {
		if (api != null) {
			try {
				IFileSharing sharingIntf = api.getFileSharing(sharingId);
				if (sharingIntf != null) {
					return new FileSharing(sharingIntf);
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
     * Returns a current file sharing from its invitation Intent
     * 
     * @param intent Invitation intent
     * @return File sharing or null if not found
     * @throws JoynServiceException
     */
    public FileSharing getFileSharingFor(Intent intent) throws JoynServiceException {
		if (api != null) {
			try {
				String sharingId = intent.getStringExtra(FileSharingIntent.EXTRA_SHARING_ID);
				if (sharingId != null) {
					return getFileSharing(sharingId);
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
	 * Adds an event listener on file sharing events
	 * 
	 * @param listener Listener
	 * @throws JoynServiceException
	 */
	public void addEventListener(IFileSharingListener listener) throws JoynServiceException {
		if (api != null) {
			try {
				api.addEventListener(listener);
			} catch (Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
	}

	/**
	 * Removes an event listener from file sharing
	 * 
	 * @param listener Listener
	 * @throws JoynServiceException
	 */
	public void removeEventListener(FileSharingListener listener) throws JoynServiceException {
		if (api != null) {
			try {
				api.removeEventListener(listener);
			} catch (Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
	}
}
