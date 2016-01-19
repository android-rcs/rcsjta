/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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

package com.gsma.services.rcs.capability;

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

import java.util.Iterator;
import java.util.Set;

/**
 * Capability service offers the main entry point to read capabilities of remote contacts, to
 * initiate capability discovery and to receive capabilities updates. Several applications may
 * connect/disconnect to the API. The parameter contact in the API supports the following formats:
 * MSISDN in national or international format, SIP address, SIP-URI or Tel-URI.
 * 
 * @author Jean-Marc AUFFRET
 */
public final class CapabilityService extends RcsService {
    /**
     * Intent broadcasted to discover extensions
     */
    public final static String INTENT_EXTENSIONS = "com.gsma.services.rcs.capability.EXTENSION";

    /**
     * Extension MIME type
     */
    public final static String EXTENSION_MIME_TYPE = "com.gsma.services.rcs";

    private static boolean sApiCompatible = false;

    /**
     * API
     */
    private ICapabilityService mApi;

    /**
     * Constructor
     * 
     * @param ctx Application context
     * @param listener Service listener
     */
    public CapabilityService(Context ctx, RcsServiceListener listener) {
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
                            "The TAPI client version of the capability service is not compatible with the TAPI service implementation version on this device!");
                }
            } catch (RcsServiceException e) {
                throw new RcsPermissionDeniedException(
                        "The compatibility of TAPI client version with the TAPI service implementation version of this device cannot be checked for the capability service!",
                        e);
            }
        }
        Intent serviceIntent = new Intent(ICapabilityService.class.getName());
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
        mApi = (ICapabilityService) api;
    }

    /**
     * Service connection
     */
    private ServiceConnection apiConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            setApi(ICapabilityService.Stub.asInterface(service));
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
     * Returns the capabilities supported by the local end user. The supported capabilities are
     * fixed by the MNO and read during the provisioning.
     * 
     * @return Capabilities
     * @throws RcsPersistentStorageException
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public Capabilities getMyCapabilities() throws RcsPersistentStorageException,
            RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            return mApi.getMyCapabilities();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the capabilities of a given contact from the local database. This method doesn't
     * request any network update to the remote contact. The parameter contact supports the
     * following formats: MSISDN in national or international format, SIP address, SIP-URI or
     * Tel-URI. If the format of the contact is not supported an exception is thrown.
     * 
     * @param contact Contact Identifier
     * @return Capabilities
     * @throws RcsPersistentStorageException
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public Capabilities getContactCapabilities(ContactId contact)
            throws RcsPersistentStorageException, RcsServiceNotAvailableException,
            RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            return mApi.getContactCapabilities(contact);

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Requests capabilities to a remote contact. This method initiates in background a new
     * capability request to the remote contact by sending a SIP OPTIONS. The result of the
     * capability request is sent asynchronously via callback method of the capabilities listener.
     * The parameter contact supports the following formats: MSISDN in national or international
     * format, SIP address, SIP-URI or Tel-URI. If the format of the contact is not supported an
     * exception is thrown. The result of the capability refresh request is provided to all the
     * clients that have registered the listener for this event.
     * 
     * @param contact Contact Identifier
     * @throws RcsServiceNotRegisteredException
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void requestContactCapabilities(ContactId contact)
            throws RcsServiceNotRegisteredException, RcsServiceNotAvailableException,
            RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mApi.requestContactCapabilities(contact);
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsServiceNotRegisteredException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Requests capabilities for a group of remote contacts. This method initiates in background new
     * capability requests to the remote contact by sending a SIP OPTIONS. The result of the
     * capability request is sent asynchronously via callback method of the capabilities listener.
     * The parameter contacts supports the following formats: MSISDN in national or international
     * format, SIP address, SIP-URI or Tel-URI. If the format of the contact is not supported an
     * exception is thrown. The result of the capability refresh request is provided to all the
     * clients that have registered the listener for this event.
     * 
     * @param contacts Set of contact identifiers
     * @throws RcsServiceNotRegisteredException
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void requestContactCapabilities(Set<ContactId> contacts)
            throws RcsServiceNotRegisteredException, RcsServiceNotAvailableException,
            RcsGenericException {
        Iterator<ContactId> values = contacts.iterator();
        while (values.hasNext()) {
            requestContactCapabilities(values.next());
        }
    }

    /**
     * Requests capabilities for all contacts existing in the local address book. This method
     * initiates in background new capability requests for each contact of the address book by
     * sending SIP OPTIONS. The result of a capability request is sent asynchronously via callback
     * method of the capabilities listener. The result of the capability refresh request is provided
     * to all the clients that have registered the listener for this event.
     * 
     * @throws RcsServiceNotRegisteredException
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void requestAllContactsCapabilities() throws RcsServiceNotRegisteredException,
            RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mApi.requestAllContactsCapabilities();
        } catch (Exception e) {
            RcsServiceNotRegisteredException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Registers a capabilities listener on any contact
     * 
     * @param listener Capabilities listener
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void addCapabilitiesListener(CapabilitiesListener listener)
            throws RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mApi.addCapabilitiesListener(listener);
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Unregisters a capabilities listener
     * 
     * @param listener Capabilities listener
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void removeCapabilitiesListener(CapabilitiesListener listener)
            throws RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mApi.removeCapabilitiesListener(listener);
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Registers a capabilities listener on a list of contacts
     * 
     * @param contacts Set of contact Identifiers
     * @param listener Capabilities listener
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void addCapabilitiesListener(Set<ContactId> contacts, CapabilitiesListener listener)
            throws RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        if (contacts == null || contacts.isEmpty()) {
            throw new RcsIllegalArgumentException("contacts must not be null or empty!");
        }
        try {
            for (ContactId contact : contacts) {
                mApi.addCapabilitiesListener2(contact, listener);
            }
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Unregisters a capabilities listener on a list of contacts
     * 
     * @param contacts Set of contact identifiers
     * @param listener Capabilities listener
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void removeCapabilitiesListener(Set<ContactId> contacts, CapabilitiesListener listener)
            throws RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        if (contacts == null || contacts.isEmpty()) {
            throw new RcsIllegalArgumentException("contacts must not be null or empty!");
        }
        try {
            for (ContactId contact : contacts) {
                mApi.removeCapabilitiesListener2(contact, listener);
            }
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }
}
