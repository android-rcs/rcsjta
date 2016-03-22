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

package com.gsma.services.rcs.sharing.image;

import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsIllegalArgumentException;
import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.RcsPersistentStorageException;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsServiceControl;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceListener;
import com.gsma.services.rcs.RcsServiceListener.ReasonCode;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.RcsServiceNotRegisteredException;
import com.gsma.services.rcs.contact.ContactId;

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
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * This class offers the main entry point to transfer image during a CS call. Several applications
 * may connect/disconnect to the API. The parameter contact in the API supports the following
 * formats: MSISDN in national or international format, SIP address, SIP-URI or Tel-URI.
 * 
 * @author Jean-Marc AUFFRET
 */
public final class ImageSharingService extends RcsService {

    /**
     * API
     */
    private IImageSharingService mApi;

    private final Map<ImageSharingListener, WeakReference<IImageSharingListener>> mImageSharingListeners = new WeakHashMap<>();

    private static boolean sApiCompatible = false;

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
     * 
     * @throws RcsPermissionDeniedException
     */
    public final void connect() throws RcsPermissionDeniedException {
        if (!sApiCompatible) {
            try {
                sApiCompatible = mRcsServiceControl.isCompatible(this);
                if (!sApiCompatible) {
                    throw new RcsPermissionDeniedException(
                            "The TAPI client version of the image sharing service is not compatible with the TAPI service implementation version on this device!");
                }
            } catch (RcsServiceException e) {
                throw new RcsPermissionDeniedException(
                        "The compatibility of TAPI client version with the TAPI service implementation version of this device cannot be checked for the image sharing service!",
                        e);
            }
        }
        Intent serviceIntent = new Intent(IImageSharingService.class.getName());
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
     * @hide
     */
    protected void setApi(IInterface api) {
        super.setApi(api);
        mApi = (IImageSharingService) api;
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
        Intent fileTransferServiceIntent = new Intent(IImageSharingService.class.getName());
        List<ResolveInfo> stackServices = mCtx.getPackageManager().queryIntentServices(
                fileTransferServiceIntent, 0);
        for (ResolveInfo stackService : stackServices) {
            mCtx.grantUriPermission(stackService.serviceInfo.packageName, file,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
    }

    /**
     * Returns the configuration of image sharing service
     * 
     * @return ImageSharingServiceConfiguration
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public ImageSharingServiceConfiguration getConfiguration()
            throws RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            return new ImageSharingServiceConfiguration(mApi.getConfiguration());

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Shares an image with a contact. The parameter file contains the URI of the image to be shared
     * (for a local or a remote image). An exception if thrown if there is no ongoing CS call. The
     * parameter contact supports the following formats: MSISDN in national or international format,
     * SIP address, SIP-URI or Tel-URI. If the format of the contact is not supported an exception
     * is thrown.
     * 
     * @param contact Contact identifier
     * @param file Uri of file to share
     * @return ImageSharing
     * @throws RcsServiceNotRegisteredException
     * @throws RcsPersistentStorageException
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public ImageSharing shareImage(ContactId contact, Uri file)
            throws RcsServiceNotRegisteredException, RcsPersistentStorageException,
            RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            /* Only grant permission for content Uris */
            tryToGrantUriPermissionToStackServices(file);
            IImageSharing sharingIntf = mApi.shareImage(contact, file);
            if (sharingIntf != null) {
                return new ImageSharing(sharingIntf);

            }
            return null;

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsServiceNotRegisteredException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns a current image sharing from its unique ID
     * 
     * @param sharingId Sharing ID
     * @return ImageSharing Image sharing or null if not found
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public ImageSharing getImageSharing(String sharingId) throws RcsServiceNotAvailableException,
            RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            return new ImageSharing(mApi.getImageSharing(sharingId));

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Adds a listener on image sharing events
     * 
     * @param listener Listener
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void addEventListener(ImageSharingListener listener)
            throws RcsServiceNotAvailableException, RcsGenericException {
        if (listener == null) {
            throw new RcsIllegalArgumentException("listener must not be null!");
        }
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            IImageSharingListener rcsListener = new ImageSharingListenerImpl(listener);
            mImageSharingListeners.put(listener, new WeakReference<>(rcsListener));
            mApi.addEventListener2(rcsListener);
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Removes a listener on image sharing events
     * 
     * @param listener Listener
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void removeEventListener(ImageSharingListener listener)
            throws RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            WeakReference<IImageSharingListener> weakRef = mImageSharingListeners.remove(listener);
            if (weakRef == null) {
                return;
            }
            IImageSharingListener rcsListener = weakRef.get();
            if (rcsListener != null) {
                mApi.removeEventListener2(rcsListener);
            }
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Deletes all image sharing from history and abort/reject any associated ongoing session if
     * such exists.
     * 
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void deleteImageSharings() throws RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mApi.deleteImageSharings();
        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Deletes image sharing with a given contact from history and abort/reject any associated
     * ongoing session if such exists
     * 
     * @param contact The remote contact
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void deleteImageSharings(ContactId contact) throws RcsServiceNotAvailableException,
            RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mApi.deleteImageSharings2(contact);
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * deletes an image sharing by its sharing id from history and abort/reject any associated
     * ongoing session if such exists.
     * 
     * @param sharingId the sharing ID
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void deleteImageSharing(String sharingId) throws RcsServiceNotAvailableException,
            RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mApi.deleteImageSharing(sharingId);
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }
}
