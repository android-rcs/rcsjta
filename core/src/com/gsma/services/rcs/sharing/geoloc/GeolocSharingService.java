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

package com.gsma.services.rcs.sharing.geoloc;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.IInterface;

import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsServiceControl;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceListener;
import com.gsma.services.rcs.RcsServiceListener.ReasonCode;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.geoloc.IGeolocSharing;
import com.gsma.services.rcs.sharing.geoloc.IGeolocSharingService;

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

    private static final String ERROR_CNX = "GeolocSharing service not connected";

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
     * @return Geoloc sharing
     * @throws RcsServiceException
     * @see Geoloc
     */
    public GeolocSharing shareGeoloc(ContactId contact, Geoloc geoloc) throws RcsServiceException {
        if (mApi != null) {
            try {
                IGeolocSharing sharingIntf = mApi.shareGeoloc(contact, geoloc);
                if (sharingIntf != null) {
                    return new GeolocSharing(sharingIntf);
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
     * Returns the list of geoloc sharings in progress
     * 
     * @return List of geoloc sharings
     * @throws RcsServiceException
     */
    public Set<GeolocSharing> getGeolocSharings() throws RcsServiceException {
        if (mApi != null) {
            try {
                Set<GeolocSharing> result = new HashSet<GeolocSharing>();
                List<IBinder> ishList = mApi.getGeolocSharings();
                for (IBinder binder : ishList) {
                    GeolocSharing sharing = new GeolocSharing(
                            IGeolocSharing.Stub.asInterface(binder));
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
     * Returns a current geoloc sharing from its unique ID
     * 
     * @param sharingId Sharing ID
     * @return Geoloc sharing or null if not found
     * @throws RcsServiceException
     */
    public GeolocSharing getGeolocSharing(String sharingId) throws RcsServiceException {
        if (mApi != null) {
            try {
                return new GeolocSharing(mApi.getGeolocSharing(sharingId));
            } catch (Exception e) {
                throw new RcsServiceException(e);
            }
        } else {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
    }

    /**
     * Deletes all geoloc sharing from history and abort/reject any associated ongoing session if
     * such exists.
     * 
     * @throws RcsServiceException
     */
    public void deleteGeolocSharings() throws RcsServiceException {
        if (mApi != null) {
            try {
                mApi.deleteGeolocSharings();
            } catch (Exception e) {
                throw new RcsServiceException(e);
            }
        } else {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
    }

    /**
     * Deletes geoloc sharing with a given contact from history and abort/reject any associated
     * ongoing session if such exists.
     * 
     * @param contact
     * @throws RcsServiceException
     */
    public void deleteGeolocSharings(ContactId contact) throws RcsServiceException {
        if (mApi != null) {
            try {
                mApi.deleteGeolocSharings2(contact);
            } catch (Exception e) {
                throw new RcsServiceException(e);
            }
        } else {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
    }

    /**
     * Deletes a geoloc sharing by its sharing id from history and abort/reject any associated
     * ongoing session if such exists.
     * 
     * @param sharingId
     * @throws RcsServiceException
     */
    public void deleteGeolocSharing(String sharingId) throws RcsServiceException {
        if (mApi != null) {
            try {
                mApi.deleteGeolocSharing(sharingId);
            } catch (Exception e) {
                throw new RcsServiceException(e.getMessage());
            }
        } else {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
    }

    /**
     * Adds a listener on geoloc sharing events
     * 
     * @param listener Listener
     * @throws RcsServiceException
     */
    public void addEventListener(GeolocSharingListener listener) throws RcsServiceException {
        if (mApi != null) {
            try {
                IGeolocSharingListener rcsListener = new GeolocSharingListenerImpl(listener);
                mGeolocSharingListeners.put(listener, new WeakReference<IGeolocSharingListener>(
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
     * Removes a listener on geoloc sharing events
     * 
     * @param listener Listener
     * @throws RcsServiceException
     */
    public void removeEventListener(GeolocSharingListener listener) throws RcsServiceException {
        if (mApi != null) {
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
                throw new RcsServiceException(e);
            }
        } else {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
    }
}
