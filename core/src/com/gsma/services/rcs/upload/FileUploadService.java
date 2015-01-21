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
	private IFileUploadService mApi;
	
	private static final String ERROR_CNX = "FileUpload service not connected";
	
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
    	mCtx.bindService(new Intent(IFileUploadService.class.getName()), apiConnection, 0);
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
        mApi = (IFileUploadService)api;
    }
    
    /**
	 * Service connection
	 */
	private ServiceConnection apiConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
        	setApi(IFileUploadService.Stub.asInterface(service));
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
    
	private void grantUriPermissionToStackServices(Uri file) {
		Intent fileTransferServiceIntent = new Intent(IFileUploadService.class.getName());
		List<ResolveInfo> stackServices = mCtx.getPackageManager().queryIntentServices(
				fileTransferServiceIntent, 0);
		for (ResolveInfo stackService : stackServices) {
			mCtx.grantUriPermission(stackService.serviceInfo.packageName, file,
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
			ContentResolver contentResolver = mCtx.getContentResolver();
			Method takePersistableUriPermissionMethod = contentResolver.getClass()
					.getMethod(TAKE_PERSISTABLE_URI_PERMISSION_METHOD_NAME,
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
		if (mApi != null) {
			try {
				return mApi.canUploadFile();
			} catch(Exception e) {
				throw new RcsServiceException(e);
			}
		} else {
			throw new RcsServiceNotAvailableException(ERROR_CNX);
		}
    }
    
    /**
     * Returns the configuration of the file upload service
     * 
     * @return Configuration
     * @throws RcsServiceException
     */
    public FileUploadServiceConfiguration getConfiguration() throws RcsServiceException {
		if (mApi != null) {
			try {
				return new FileUploadServiceConfiguration(mApi.getConfiguration());
			} catch(Exception e) {
				throw new RcsServiceException(e);
			}
		} else {
			throw new RcsServiceNotAvailableException(ERROR_CNX);
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
		if (mApi != null) {
			try {
				tryToGrantAndPersistUriPermission(file);
				
				IFileUpload uploadIntf = mApi.uploadFile(file, attachFileIcon);
				if (uploadIntf != null) {
					return new FileUpload(uploadIntf);
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
     * Returns the list of file uploads in progress
     * 
     * @return List of file uploads
     * @throws RcsServiceException
     */
    public Set<FileUpload> getFileUploads() throws RcsServiceException {
		if (mApi != null) {
			try {
	    		Set<FileUpload> result = new HashSet<FileUpload>();
				List<IBinder> ishList = mApi.getFileUploads();
				for (IBinder binder : ishList) {
					FileUpload upload = new FileUpload(IFileUpload.Stub.asInterface(binder));
					result.add(upload);
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
     * Returns a current file upload from its unique ID
     * 
     * @param uploadId Upload ID
     * @return File upload or null if not found
     * @throws RcsServiceException
     */
    public FileUpload getFileUpload(String uploadId) throws RcsServiceException {
		if (mApi != null) {
			try {
				IFileUpload uploadIntf = mApi.getFileUpload(uploadId);
				if (uploadIntf != null) {
					return new FileUpload(uploadIntf);
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
	 * Adds a listener on file upload events
	 * 
	 * @param listener Listener
	 * @throws RcsServiceException
	 */
	public void addEventListener(FileUploadListener listener) throws RcsServiceException {
		if (mApi != null) {
			try {
				mApi.addEventListener(listener);
			} catch (Exception e) {
				throw new RcsServiceException(e);
			}
		} else {
			throw new RcsServiceNotAvailableException(ERROR_CNX);
		}
	}

	/**
	 * Removes a listener on file upload events
	 * 
	 * @param listener Listener
	 * @throws RcsServiceException
	 */
	public void removeEventListener(FileUploadListener listener) throws RcsServiceException {
		if (mApi != null) {
			try {
				mApi.removeEventListener(listener);
			} catch (Exception e) {
				throw new RcsServiceException(e);
			}
		} else {
			throw new RcsServiceNotAvailableException(ERROR_CNX);
		}
	}
}
