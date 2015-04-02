/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gsma.services.rcs.history;

import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsIllegalArgumentException;
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
import android.net.Uri;
import android.os.IBinder;
import android.os.IInterface;

import java.util.Map;

/**
 * History service offers the entry point to register and unregister extra history log provider
 * members to the API.
 */
public class HistoryService extends RcsService {

    private IHistoryService mService;

    /**
     * Constructor
     * 
     * @param ctx Application context
     * @param listener Service listener
     */
    public HistoryService(Context ctx, RcsServiceListener listener) {
        super(ctx, listener);
    }

    /**
     * Connects to the API
     */
    public void connect() {
        Intent serviceIntent = new Intent(IHistoryService.class.getName());
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
     * Set service interface
     * 
     * @param service Service interface
     */
    protected void setApi(IInterface service) {
        super.setApi(service);
        mService = (IHistoryService) service;
    }

    /**
     * Service connection
     */
    private ServiceConnection apiConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            setApi(IHistoryService.Stub.asInterface(service));
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
     * Registers an external history log member.
     * 
     * @param providerId Provider ID of history log member
     * @param providerUri Provider Uri
     * @param database URI of database to register
     * @param table Name of table to register
     * @param columnMapping Translator of internal field names to history log provider field names
     * @throws RcsServiceException
     */
    public void registerExtraHistoryLogMember(int providerId, Uri providerUri, Uri database,
            String table, Map<String, String> columnMapping) throws RcsServiceException {
        if (mService == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mService.registerExtraHistoryLogMember(providerId, providerUri, database, table,
                    columnMapping);
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Unregisters an external history log member.
     * 
     * @param int Provider ID of history log member
     * @throws RcsServiceException
     */
    public void unregisterExtraHistoryLogMember(int providerId) throws RcsServiceException {
        if (mService == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mService.unregisterExtraHistoryLogMember(providerId);
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Creates an id that will be unique across all tables in the base column "_id".
     * 
     * @param id of the provider that requires the generated id for its table
     * @return the generated id as long
     * @throws RcsServiceException
     */
    public long createUniqueId(int providerId) throws RcsServiceException {
        if (mService == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            return mService.createUniqueId(providerId);

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

}
