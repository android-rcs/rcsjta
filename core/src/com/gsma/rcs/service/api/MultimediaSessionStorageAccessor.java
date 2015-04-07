/*
 * Copyright (C) 2010 France Telecom S.A.
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

package com.gsma.rcs.service.api;

import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.extension.MultimediaSession.ReasonCode;
import com.gsma.services.rcs.extension.MultimediaSession.State;

/**
 * MultimediaSessionStorageAccessor helps in retrieving data related to a multimedia session.
 */
public class MultimediaSessionStorageAccessor {

    private State mState;

    private ReasonCode mReason;

    private final ContactId mRemoteContact;

    private final Direction mDirection;

    private final String mServiceId;

    /**
     * Constructor
     * 
     * @param direction
     * @param remoteContact
     * @param serviceId
     * @param state
     */
    public MultimediaSessionStorageAccessor(Direction direction, ContactId remoteContact,
            String serviceId, State state) {
        mState = state;
        mDirection = direction;
        mRemoteContact = remoteContact;
        mServiceId = serviceId;
    }

    /**
     * Gets the state of the multimedia session
     * 
     * @return the state of the multimedia session
     */
    public State getState() {
        return mState;
    }

    /**
     * Gets the remote contact
     * 
     * @return the remote contact
     */
    public ContactId getRemoteContact() {
        return mRemoteContact;
    }

    /**
     * Gets the direction
     * 
     * @return the direction
     */
    public Direction getDirection() {
        return mDirection;
    }

    /**
     * Gets the service identifier
     * 
     * @return the service identifier
     */
    public String getServiceId() {
        return mServiceId;
    }

    /**
     * Sets the state and reason code of the multimedia session
     * 
     * @param state the state of the multimedia session
     * @param reason the reason code for the multimedia session
     */
    public void setStateAndReasonCode(State state, ReasonCode reason) {
        mState = state;
    }

    /**
     * Gets reason code for the multimedia session
     * 
     * @return the reason code for the multimedia session
     */
    public ReasonCode getReasonCode() {
        return mReason;
    }

}
