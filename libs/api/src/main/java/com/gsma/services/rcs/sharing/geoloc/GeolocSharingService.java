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

package com.gsma.services.rcs.sharing.geoloc;

import com.gsma.services.rcs.Geoloc;
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
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.IInterface;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * This class offers the main entry point to share geolocation info during a CS call. Several
 * applications may connect/disconnect to the API. The parameter contact in the API supports the
 * following formats: MSISDN in national or international format, SIP address, SIP-URI or Tel-URI.
 * 
 * @author Jean-Marc AUFFRET
 */
public final class GeolocSharingService extends RcsService {
    /**
     * API
     */
    private IGeolocSharingService mApi;

    private final Map<GeolocSharingListener, WeakReference<IGeolocSharingListener>> mGeolocSharingListeners = new WeakHashMap<GeolocSharingListener, WeakReference<IGeolocSharingListener>>();

    private static boolean sApiCompatible = false;

    /**
     * Constructor
     * 
     * @param ctx Application context
     * @param listener Service listener
     */
    public GeolocSharingService(Context ctx, RcsServiceListener listener) {
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
                            "The TAPI client version of the geoloc sharing service is not compatible with the TAPI service implementation version on this device!");
                }
            } catch (RcsServiceException e) {
                throw new RcsPermissionDeniedException(
                        "The compatibility of TAPI client version with the TAPI service implementation version of this device cannot be checked for the geoloc sharing service!",
                        e);
            }
        }
        Intent serviceIntent = new Intent(IGeolocSharingService.class.getName());
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
        mApi = (IGeolocSharingService) api;
    }

    /**
     * Service connection
     */
    private ServiceConnection apiConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            setApi(IGeolocSharingService.Stub.asInterface(service));
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
     * Shares a geolocation with a contact. An exception if thrown if there is no ongoing CS call.
     * The parameter contact supports the following formats: MSISDN in national or international
     * format, SIP address, SIP-URI or Tel-URI. If the format of the contact is not supported an
     * exception is thrown.
     * 
     * @param contact Contact identifier
     * @param geoloc Geolocation info
     * @return GeolocSharing
     * @throws RcsServiceNotRegisteredException
     * @throws RcsPersistentStorageException
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     * @see Geoloc
     */
    public GeolocSharing shareGeoloc(ContactId contact, Geoloc geoloc)
            throws RcsServiceNotRegisteredException, RcsPersistentStorageException,
            RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            IGeolocSharing sharingIntf = mApi.shareGeoloc(contact, geoloc);
            if (sharingIntf != null) {
                return new GeolocSharing(sharingIntf);

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
     * Returns a current geoloc sharing from its unique ID
     * 
     * @param sharingId Sharing ID
     * @return GeolocSharing Geoloc sharing or null if not found
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public GeolocSharing getGeolocSharing(String sharingId) throws RcsServiceNotAvailableException,
            RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            return new GeolocSharing(mApi.getGeolocSharing(sharingId));

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Deletes all geoloc sharing from history and abort/reject any associated ongoing session if
     * such exists.
     * 
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void deleteGeolocSharings() throws RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mApi.deleteGeolocSharings();
        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Deletes geoloc sharing with a given contact from history and abort/reject any associated
     * ongoing session if such exists.
     * 
     * @param contact the remote contact
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void deleteGeolocSharings(ContactId contact) throws RcsServiceNotAvailableException,
            RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mApi.deleteGeolocSharings2(contact);
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Deletes a geoloc sharing by its sharing id from history and abort/reject any associated
     * ongoing session if such exists.
     * 
     * @param sharingId the sharing ID
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void deleteGeolocSharing(String sharingId) throws RcsServiceNotAvailableException,
            RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mApi.deleteGeolocSharing(sharingId);
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Adds a listener on geoloc sharing events
     * 
     * @param listener Listener
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void addEventListener(GeolocSharingListener listener)
            throws RcsServiceNotAvailableException, RcsGenericException {
        if (listener == null) {
            throw new RcsIllegalArgumentException("listener must not be null!");
        }
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            IGeolocSharingListener rcsListener = new GeolocSharingListenerImpl(listener);
            mGeolocSharingListeners.put(listener, new WeakReference<>(rcsListener));
            mApi.addEventListener2(rcsListener);
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Removes a listener on geoloc sharing events
     * 
     * @param listener Listener
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void removeEventListener(GeolocSharingListener listener)
            throws RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            WeakReference<IGeolocSharingListener> weakRef = mGeolocSharingListeners
                    .remove(listener);
            if (weakRef == null) {
                return;
            }
            IGeolocSharingListener rcsListener = weakRef.get();
            if (rcsListener != null) {
                mApi.removeEventListener2(rcsListener);
            }
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }
}
