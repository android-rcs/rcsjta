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

import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsServiceControl;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceListener;
import com.gsma.services.rcs.RcsServiceListener.ReasonCode;
import com.gsma.services.rcs.RcsServiceNotAvailableException;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.IBinder;
import android.os.IInterface;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * This class offers the main entry point to upload a file to the RCS content server. Several
 * applications may connect/disconnect to the API. There is no pause and resume supported here.
 * 
 * @author Jean-Marc AUFFRET
 */
public final class FileUploadService extends RcsService {

    /**
     * API
     */
    private IFileUploadService mApi;

    private static final String ERROR_CNX = "FileUpload service not connected";

    private final Map<FileUploadListener, WeakReference<IFileUploadListener>> mFileUploadListeners = new WeakHashMap<FileUploadListener, WeakReference<IFileUploadListener>>();

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
     * 
     * @throws RcsPermissionDeniedException
     */
    public void connect() throws RcsPermissionDeniedException {
        if (!sApiCompatible) {
            try {
                sApiCompatible = mRcsServiceControl.isCompatible();
                if (!sApiCompatible) {
                    throw new RcsPermissionDeniedException("API is not compatible");
                }
            } catch (RcsServiceException e) {
                throw new RcsPermissionDeniedException("Cannot check API compatibility");
            }
        }
        Intent serviceIntent = new Intent(IFileUploadService.class.getName());
        serviceIntent.setPackage(RcsServiceControl.RCS_STACK_PACKAGENAME);
        mCtx.bindService(serviceIntent, apiConnection, 0);
    }

    /**
     * Disconnects from the API
     */
    public void disconnect() {
        try {
            mCtx.unbindService(apiConnection);
        } catch (IllegalArgumentException e) {
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
        mApi = (IFileUploadService) api;
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
            if (mListener == null) {
                return;
            }
            ReasonCode reasonCode = ReasonCode.CONNECTION_LOST;
            try {
                if (!mRcsServiceControl.isActivated()) {
                    reasonCode = ReasonCode.SERVICE_DISABLED;
                }
            } catch (RcsServiceException e) {
                // Do nothing
            }
            mListener.onServiceDisconnected(reasonCode);
        }
    };

    /**
     * Granting temporary read Uri permission from client to stack service if it is a content URI
     * 
     * @param file Uri of file to grant permission
     */
    private void tryToGrantUriPermissionToStackServices(Uri file) {
        if (!ContentResolver.SCHEME_CONTENT.equals(file.getScheme())) {
            return;
        }
        Intent fileTransferServiceIntent = new Intent(IFileUploadService.class.getName());
        List<ResolveInfo> stackServices = mCtx.getPackageManager().queryIntentServices(
                fileTransferServiceIntent, 0);
        for (ResolveInfo stackService : stackServices) {
            mCtx.grantUriPermission(stackService.serviceInfo.packageName, file,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
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
            } catch (Exception e) {
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
            } catch (Exception e) {
                throw new RcsServiceException(e);
            }
        } else {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
    }

    /**
     * Uploads a file to the RCS content server. The parameter file contains the URI of the file to
     * be uploaded (for a local or a remote file).
     * 
     * @param file Uri of file to upload
     * @param attachFileIcon Attach file icon option. If true and if it's an image, a file icon is
     *            attached.
     * @return FileUpload
     * @throws RcsServiceException
     */
    public FileUpload uploadFile(Uri file, boolean attachFileIcon) throws RcsServiceException {
        if (mApi != null) {
            try {
                /* Only grant permission for content Uris */
                tryToGrantUriPermissionToStackServices(file);
                IFileUpload uploadIntf = mApi.uploadFile(file, attachFileIcon);
                if (uploadIntf != null) {
                    return new FileUpload(uploadIntf);
                } else {
                    return null;
                }
            } catch (Exception e) {
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
            } catch (Exception e) {
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
            } catch (Exception e) {
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
                IFileUploadListener fileUploadListener = new FileUploadListenerImpl(listener);
                mFileUploadListeners.put(listener, new WeakReference<IFileUploadListener>(
                        fileUploadListener));
                mApi.addEventListener(fileUploadListener);
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
                WeakReference<IFileUploadListener> weakRef = mFileUploadListeners.remove(listener);
                if (weakRef == null) {
                    return;
                }
                IFileUploadListener fileUploadListener = weakRef.get();
                if (fileUploadListener != null) {
                    mApi.removeEventListener(fileUploadListener);
                }
            } catch (Exception e) {
                throw new RcsServiceException(e);
            }
        } else {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
    }
}
