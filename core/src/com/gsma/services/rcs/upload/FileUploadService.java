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
package com.gsma.services.rcs.upload;

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

/**
 * This class offers the main entry point to upload a file to the RCS content
 * server. Several applications may connect/disconnect to the API. There is no
 * pause and resume supported here.
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileUploadService extends RcsService {

	private static final int KITKAT_VERSION_CODE = 19;

	private static final String TAKE_PERSISTABLE_URI_PERMISSION_METHOD_NAME = "takePersistableUriPermission";

	private static final Class<?>[] TAKE_PERSISTABLE_URI_PERMISSION_PARAM_TYPES = new Class[] {
			Uri.class, int.class
	};

	/**
	 * API
	 */
	private IFileUploadService api;
	
    /**
     * Constructor
     * 
     * @param ctx Application context
     * @param listener Service listener
     */
    public FileUploadService(Context ctx, RcsServiceListener listener) {
    	super(ctx, listener);
    }

    /**
     * Connects to the API
     */
    public void connect() {
    	ctx.bindService(new Intent(IFileUploadService.class.getName()), apiConnection, 0);
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
    	
        this.api = (IFileUploadService)api;
    }
    
    /**
	 * Service connection
	 */
	private ServiceConnection apiConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
        	setApi(IFileUploadService.Stub.asInterface(service));
        	if (serviceListener != null) {
        		serviceListener.onServiceConnected();
        	}
        }

        public void onServiceDisconnected(ComponentName className) {
        	setApi(null);
        	if (serviceListener != null) {
        		serviceListener.onServiceDisconnected(RcsService.Error.CONNECTION_LOST);
        	}
        }
    };
    
	private void grantUriPermissionToStackServices(Uri file) {
		Intent fileTransferServiceIntent = new Intent(IFileUploadService.class.getName());
		List<ResolveInfo> stackServices = ctx.getPackageManager().queryIntentServices(
				fileTransferServiceIntent, 0);
		for (ResolveInfo stackService : stackServices) {
			ctx.grantUriPermission(stackService.serviceInfo.packageName, file,
					Intent.FLAG_GRANT_READ_URI_PERMISSION);
		}
	}

	/**
	 * Using reflection to persist Uri permission in order to support backward
	 * compatibility since this API is available only from Kitkat onwards.
	 *
	 * @param file Uri of file to transfer
	 * @throws RcsServiceException
	 */
	private void takePersistableUriPermission(Uri file) throws RcsServiceException {
		try {
			ContentResolver contentResolver = ctx.getContentResolver();
			Method takePersistableUriPermissionMethod = contentResolver.getClass()
					.getMethod(TAKE_PERSISTABLE_URI_PERMISSION_METHOD_NAME,
							TAKE_PERSISTABLE_URI_PERMISSION_PARAM_TYPES);
			Object[] methodArgs = new Object[] {
					file,
					Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
			};
			takePersistableUriPermissionMethod.invoke(contentResolver, methodArgs);
		} catch (Exception e) {
			throw new RcsServiceException(e.getMessage());
		}
	}    
    
	/**
	 * Grant permission to the stack and persist access permission
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
     * Can a file be uploaded now
     * 
     * @return Returns true if a file can be uploaded, else returns false
     * @throws RcsServiceException
     */
    public boolean canUploadFile() throws RcsServiceException {
		if (api != null) {
			try {
				return api.canUploadFile();
			} catch(Exception e) {
				throw new RcsServiceException(e.getMessage());
			}
		} else {
			throw new RcsServiceNotAvailableException();
		}
    }
    
    /**
     * Returns the configuration of the file upload service
     * 
     * @return Configuration
     * @throws RcsServiceException
     */
    public FileUploadServiceConfiguration getConfiguration() throws RcsServiceException {
		if (api != null) {
			try {
				return api.getConfiguration();
			} catch(Exception e) {
				throw new RcsServiceException(e.getMessage());
			}
		} else {
			throw new RcsServiceNotAvailableException();
		}
	}
    
    /**
	 * Uploads a file to the RCS content server. The parameter file contains the
	 * URI of the file to be uploaded (for a local or a remote file).
	 * 
	 * @param file Uri of file to upload
	 * @param attachFileIcon Attach file icon option. If true and if it's an
	 *            image, a file icon is attached.
	 * @return FileUpload
	 * @throws RcsServiceException
	 */
    public FileUpload uploadFile(Uri file, boolean attachFileIcon) throws RcsServiceException {
		if (api != null) {
			try {
				tryToGrantAndPersistUriPermission(file);
				
				IFileUpload uploadIntf = api.uploadFile(file, attachFileIcon);
				if (uploadIntf != null) {
					return new FileUpload(uploadIntf);
				} else {
					return null;
				}
			} catch(Exception e) {
				throw new RcsServiceException(e.getMessage());
			}
		} else {
			throw new RcsServiceNotAvailableException();
		}
    }    
    
    /**
     * Returns the list of file uploads in progress
     * 
     * @return List of file uploads
     * @throws RcsServiceException
     */
    public Set<FileUpload> getFileUploads() throws RcsServiceException {
		if (api != null) {
			try {
	    		Set<FileUpload> result = new HashSet<FileUpload>();
				List<IBinder> ishList = api.getFileUploads();
				for (IBinder binder : ishList) {
					FileUpload upload = new FileUpload(IFileUpload.Stub.asInterface(binder));
					result.add(upload);
				}
				return result;
			} catch(Exception e) {
				throw new RcsServiceException(e.getMessage());
			}
		} else {
			throw new RcsServiceNotAvailableException();
		}
    }    

    /**
     * Returns a current file upload from its unique ID
     * 
     * @param uploadId Upload ID
     * @return File upload or null if not found
     * @throws RcsServiceException
     */
    public FileUpload getFileUpload(String uploadId) throws RcsServiceException {
		if (api != null) {
			try {
				IFileUpload uploadIntf = api.getFileUpload(uploadId);
				if (uploadIntf != null) {
					return new FileUpload(uploadIntf);
				} else {
					return null;
				}
			} catch(Exception e) {
				throw new RcsServiceException(e.getMessage());
			}
		} else {
			throw new RcsServiceNotAvailableException();
		}
    }    
 
	/**
	 * Adds a listener on file upload events
	 * 
	 * @param listener Listener
	 * @throws RcsServiceException
	 */
	public void addEventListener(FileUploadListener listener) throws RcsServiceException {
		if (api != null) {
			try {
				api.addEventListener(listener);
			} catch (Exception e) {
				throw new RcsServiceException(e.getMessage());
			}
		} else {
			throw new RcsServiceNotAvailableException();
		}
	}

	/**
	 * Removes a listener on file upload events
	 * 
	 * @param listener Listener
	 * @throws RcsServiceException
	 */
	public void removeEventListener(FileUploadListener listener) throws RcsServiceException {
		if (api != null) {
			try {
				api.removeEventListener(listener);
			} catch (Exception e) {
				throw new RcsServiceException(e.getMessage());
			}
		} else {
			throw new RcsServiceNotAvailableException();
		}
	}
}
