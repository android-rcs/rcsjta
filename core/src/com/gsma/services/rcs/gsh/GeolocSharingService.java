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
package com.gsma.services.rcs.gsh;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.IInterface;

import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceListener;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * This class offers the main entry point to share geolocation info
 * during a CS call. Several applications may connect/disconnect to
 * the API.
 * 
 * The parameter contact in the API supports the following formats:
 * MSISDN in national or international format, SIP address, SIP-URI
 * or Tel-URI.
 * 
 * @author Jean-Marc AUFFRET
 */
public class GeolocSharingService extends RcsService {
	/**
	 * API
	 */
	private IGeolocSharingService api;
	
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
     */
    public void connect() {
    	ctx.bindService(new Intent(IGeolocSharingService.class.getName()), apiConnection, 0);
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
    	
        this.api = (IGeolocSharingService)api;
    }

    /**
	 * Service connection
	 */
	private ServiceConnection apiConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
        	setApi(IGeolocSharingService.Stub.asInterface(service));
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

    /**
     * Shares a geolocation with a contact. An exception if thrown if there is no ongoing
     * CS call. The parameter contact supports the following formats: MSISDN in national
     * or international format, SIP address, SIP-URI or Tel-URI. If the format of the
     * contact is not supported an exception is thrown.
     * 
     * @param contact Contact identifier
     * @param geoloc Geolocation info
     * @return Geoloc sharing
     * @throws RcsServiceException
	 * @see Geoloc
     */
    public GeolocSharing shareGeoloc(ContactId contact, Geoloc geoloc) throws RcsServiceException {
		if (api != null) {
			try {
				IGeolocSharing sharingIntf = api.shareGeoloc(contact, geoloc);
				if (sharingIntf != null) {
					return new GeolocSharing(sharingIntf);
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
     * Returns the list of geoloc sharings in progress
     * 
     * @return List of geoloc sharings
     * @throws RcsServiceException
     */
    public Set<GeolocSharing> getGeolocSharings() throws RcsServiceException {
		if (api != null) {
			try {
	    		Set<GeolocSharing> result = new HashSet<GeolocSharing>();
				List<IBinder> ishList = api.getGeolocSharings();
				for (IBinder binder : ishList) {
					GeolocSharing sharing = new GeolocSharing(IGeolocSharing.Stub.asInterface(binder));
					result.add(sharing);
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
     * Returns a current geoloc sharing from its unique ID
     * 
     * @param sharingId Sharing ID
     * @return Geoloc sharing or null if not found
     * @throws RcsServiceException
     */
    public GeolocSharing getGeolocSharing(String sharingId) throws RcsServiceException {
		if (api != null) {
			try {
				IGeolocSharing sharingIntf = api.getGeolocSharing(sharingId);
				if (sharingIntf != null) {
					return new GeolocSharing(sharingIntf);
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
	 * Adds a listener on geoloc sharing events
	 *
	 * @param listener Listener
	 * @throws RcsServiceException
	 */
	public void addEventListener(GeolocSharingListener listener) throws RcsServiceException {
		if (api != null) {
			try {
				api.addEventListener2(listener);
			} catch (Exception e) {
				throw new RcsServiceException(e.getMessage());
			}
		} else {
			throw new RcsServiceNotAvailableException();
		}
	}

	/**
	 * Removes a listener on geoloc sharing events
	 *
	 * @param listener Listener
	 * @throws RcsServiceException
	 */
	public void removeEventListener(GeolocSharingListener listener) throws RcsServiceException {
		if (api != null) {
			try {
				api.removeEventListener2(listener);
			} catch (Exception e) {
				throw new RcsServiceException(e.getMessage());
			}
		} else {
			throw new RcsServiceNotAvailableException();
		}
	}
}
