/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsIllegalArgumentException;
import com.gsma.services.rcs.RcsMaxAllowedSessionLimitReachedException;
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

    private final Map<FileUploadListener, WeakReference<IFileUploadListener>> mFileUploadListeners = new WeakHashMap<FileUploadListener, WeakReference<IFileUploadListener>>();

    private static boolean sApiCompatible = false;

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
    public final void connect() throws RcsPermissionDeniedException {
        if (!sApiCompatible) {
            try {
                sApiCompatible = mRcsServiceControl.isCompatible(this);
                if (!sApiCompatible) {
                    throw new RcsPermissionDeniedException(
                            "The TAPI client version of the file upload service is not compatible with the TAPI service implementation version on this device!");
                }
            } catch (RcsServiceException e) {
                throw new RcsPermissionDeniedException(
                        "The compatibility of TAPI client version with the TAPI service implementation version of this device cannot be checked for the file upload service!",
                        e);
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
     * @return boolean true if a file can be uploaded, else returns false
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public boolean canUploadFile() throws RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            return mApi.canUploadFile();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the configuration of the file upload service
     * 
     * @return FileUploadServiceConfiguration
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public FileUploadServiceConfiguration getConfiguration()
            throws RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            return new FileUploadServiceConfiguration(mApi.getConfiguration());

        } catch (Exception e) {
            throw new RcsGenericException(e);
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
     * @throws RcsMaxAllowedSessionLimitReachedException
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public FileUpload uploadFile(Uri file, boolean attachFileIcon)
            throws RcsMaxAllowedSessionLimitReachedException, RcsServiceNotAvailableException,
            RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            /* Only grant permission for content Uris */
            tryToGrantUriPermissionToStackServices(file);
            IFileUpload uploadIntf = mApi.uploadFile(file, attachFileIcon);
            if (uploadIntf != null) {
                return new FileUpload(uploadIntf);

            }
            return null;

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsMaxAllowedSessionLimitReachedException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the list of file uploads in progress
     * 
     * @return Set&lt;FileUpload&gt; List of file uploads
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public Set<FileUpload> getFileUploads() throws RcsServiceNotAvailableException,
            RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            Set<FileUpload> result = new HashSet<FileUpload>();
            List<IBinder> ishList = mApi.getFileUploads();
            for (IBinder binder : ishList) {
                FileUpload upload = new FileUpload(IFileUpload.Stub.asInterface(binder));
                result.add(upload);
            }
            return result;

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns a current file upload from its unique ID
     * 
     * @param uploadId Upload ID
     * @return FileUpload File upload or null if not found
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public FileUpload getFileUpload(String uploadId) throws RcsServiceNotAvailableException,
            RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            IFileUpload uploadIntf = mApi.getFileUpload(uploadId);
            if (uploadIntf != null) {
                return new FileUpload(uploadIntf);

            }
            return null;

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Adds a listener on file upload events
     * 
     * @param listener Listener
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void addEventListener(FileUploadListener listener)
            throws RcsServiceNotAvailableException, RcsGenericException {
        if (listener == null) {
            throw new RcsIllegalArgumentException("listener must not be null!");
        }
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            IFileUploadListener fileUploadListener = new FileUploadListenerImpl(listener);
            mFileUploadListeners.put(listener, new WeakReference<IFileUploadListener>(
                    fileUploadListener));
            mApi.addEventListener(fileUploadListener);
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Removes a listener on file upload events
     * 
     * @param listener Listener
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void removeEventListener(FileUploadListener listener)
            throws RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
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
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }
}
