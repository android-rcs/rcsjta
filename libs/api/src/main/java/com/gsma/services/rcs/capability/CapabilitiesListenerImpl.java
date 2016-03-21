/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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
 ******************************************************************************/

package com.gsma.services.rcs.capability;

import com.gsma.services.rcs.contact.ContactId;

import android.os.RemoteException;

/**
 * Capabilities event listener implementation
 * 
 * @author Philippe LEMORDANT
 * @hide
 */
public class CapabilitiesListenerImpl extends ICapabilitiesListener.Stub {

    private final CapabilitiesListener mListener;

    /**
     * Constructor
     * 
     * @param listener Capabilities listener
     */
    public CapabilitiesListenerImpl(CapabilitiesListener listener) {
        mListener = listener;
    }

    @Override
    public void onCapabilitiesReceived(ContactId contact, Capabilities capabilities)
            throws RemoteException {
        mListener.onCapabilitiesReceived(contact, capabilities);
    }
}
