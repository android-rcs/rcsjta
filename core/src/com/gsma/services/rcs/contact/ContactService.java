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

package com.gsma.services.rcs.contact;

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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.IInterface;

import java.util.HashSet;
import java.util.Set;

/**
 * Contact service offers additional methods to manage RCS info in the local address book. The
 * parameter contact in the API supports the following formats: MSISDN in national or international
 * format, SIP address, SIP-URI or Tel-URI.
 * 
 * @author Jean-Marc AUFFRET
 */
public final class ContactService extends RcsService {
    /**
     * API
     */
    private IContactService mApi;

    /**
     * Constructor
     * 
     * @param ctx Application context
     * @param listener Service listener
     */
    public ContactService(Context ctx, RcsServiceListener listener) {
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
        Intent serviceIntent = new Intent(IContactService.class.getName());
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
        mApi = (IContactService) api;
    }

    /**
     * Service connection
     */
    private ServiceConnection apiConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            setApi(IContactService.Stub.asInterface(service));
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
     * Returns the rcs contact infos from its contact ID (i.e. MSISDN)
     * 
     * @param contact Contact ID
     * @return RcsContact
     * @throws RcsServiceException
     * @see RcsContact
     */
    public RcsContact getRcsContact(ContactId contact) throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            return mApi.getRcsContact(contact);

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the list of rcs contacts
     * 
     * @return List of contacts
     * @throws RcsServiceException
     * @see RcsContact
     */
    public Set<RcsContact> getRcsContacts() throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            return new HashSet<RcsContact>(mApi.getRcsContacts());

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the list of online contacts (i.e. registered)
     * 
     * @return List of contacts
     * @throws RcsServiceException
     * @see RcsContact
     */
    public Set<RcsContact> getRcsContactsOnline() throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            return new HashSet<RcsContact>(mApi.getRcsContactsOnline());

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the list of contacts supporting a given extension or service ID
     * 
     * @param serviceId Service ID
     * @return List of contacts
     * @throws RcsServiceException
     * @see RcsContact
     */
    public Set<RcsContact> getRcsContactsSupporting(String serviceId) throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            return new HashSet<RcsContact>(mApi.getRcsContactsSupporting(serviceId));

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Block a contact. Any communication from the given contact will be blocked and redirected to
     * the corresponding spambox.
     * 
     * @param contact Contact ID
     * @throws RcsServiceException
     */
    public void blockContact(ContactId contact) throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mApi.blockContact(contact);
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Unblock a contact
     * 
     * @param contact Contact ID
     * @throws RcsServiceException
     */
    public void unblockContact(ContactId contact) throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mApi.unblockContact(contact);
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }
}
