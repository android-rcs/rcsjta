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

package com.gsma.services.rcs.sharing.image;

import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsServiceControl;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceListener;
import com.gsma.services.rcs.RcsServiceListener.ReasonCode;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private final Map<ImageSharingListener, WeakReference<IImageSharingListener>> mImageSharingListeners = new WeakHashMap<ImageSharingListener, WeakReference<IImageSharingListener>>();

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
     * @return Configuration
     * @throws RcsServiceException
     */
    public ImageSharingServiceConfiguration getConfiguration() throws RcsServiceException {
        if (mApi != null) {
            try {
                return new ImageSharingServiceConfiguration(mApi.getConfiguration());
            } catch (Exception e) {
                throw new RcsServiceException(e);
            }
        } else {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
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
     * @return Image sharing
     * @throws RcsServiceException
     */
    public ImageSharing shareImage(ContactId contact, Uri file) throws RcsServiceException {
        if (mApi != null) {
            try {
                /* Only grant permission for content Uris */
                tryToGrantUriPermissionToStackServices(file);
                IImageSharing sharingIntf = mApi.shareImage(contact, file);
                if (sharingIntf != null) {
                    return new ImageSharing(sharingIntf);
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
            } catch (Exception e) {
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
            } catch (Exception e) {
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
                IImageSharingListener rcsListener = new ImageSharingListenerImpl(listener);
                mImageSharingListeners.put(listener, new WeakReference<IImageSharingListener>(
                        rcsListener));
                mApi.addEventListener2(rcsListener);
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
                WeakReference<IImageSharingListener> weakRef = mImageSharingListeners
                        .remove(listener);
                if (weakRef == null) {
                    return;
                }
                IImageSharingListener rcsListener = weakRef.get();
                if (rcsListener != null) {
                    mApi.removeEventListener2(rcsListener);
                }
            } catch (Exception e) {
                throw new RcsServiceException(e);
            }
        } else {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
    }

    /**
     * Deletes all image sharing from history and abort/reject any associated ongoing session if
     * such exists.
     * 
     * @throws RcsServiceException
     */
    public void deleteImageSharings() throws RcsServiceException {
        if (mApi != null) {
            try {
                mApi.deleteImageSharings();
            } catch (Exception e) {
                throw new RcsServiceException(e);
            }
        } else {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
    }

    /**
     * Deletes image sharing with a given contact from history and abort/reject any associated
     * ongoing session if such exists
     * 
     * @param contact
     * @throws RcsServiceException
     */
    public void deleteImageSharings(ContactId contact) throws RcsServiceException {
        if (mApi != null) {
            try {
                mApi.deleteImageSharings2(contact);
            } catch (Exception e) {
                throw new RcsServiceException(e);
            }
        } else {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
    }

    /**
     * deletes an image sharing by its sharing id from history and abort/reject any associated
     * ongoing session if such exists.
     * 
     * @param sharingId
     * @throws RcsServiceException
     */
    public void deleteImageSharing(String sharingId) throws RcsServiceException {
        if (mApi != null) {
            try {
                mApi.deleteImageSharing(sharingId);
            } catch (Exception e) {
                throw new RcsServiceException(e);
            }
        } else {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
    }
}
