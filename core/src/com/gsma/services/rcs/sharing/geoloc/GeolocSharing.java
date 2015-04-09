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

import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contact.ContactId;

import android.util.SparseArray;

/**
 * Geoloc sharing
 * 
 * @author Jean-Marc AUFFRET
 */
/**
 * @author yplo6403
 */
public class GeolocSharing {

    /**
     * Geoloc sharing state
     */
    public enum State {

        /**
         * Sharing invitation received
         */
        INVITED(0),

        /**
         * Sharing invitation sent
         */
        INITIATING(1),

        /**
         * Sharing is started
         */
        STARTED(2),

        /**
         * Sharing has been aborted
         */
        ABORTED(3),

        /**
         * Sharing has failed
         */
        FAILED(4),

        /**
         * Sharing has been transferred
         */
        TRANSFERRED(5),

        /**
         * Sharing invitation was rejected
         */
        REJECTED(6),

        /**
         * Call ringing
         */
        RINGING(7),

        /**
         * Sharing has been accepted and is in the process of becoming started
         */
        ACCEPTING(8);

        private final int mValue;

        private static SparseArray<State> mValueToEnum = new SparseArray<State>();
        static {
            for (State entry : State.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        private State(int value) {
            mValue = value;
        }

        public final int toInt() {
            return mValue;
        }

        public final static State valueOf(int value) {
            State entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException(new StringBuilder("No enum const class ")
                    .append(State.class.getName()).append(".").append(value).append("!").toString());
        }
    }

    /**
     * Reason code
     */
    public enum ReasonCode {

        /**
         * No specific reason code specified.
         */
        UNSPECIFIED(0),

        /**
         * Geolocation share is aborted by local user.
         */
        ABORTED_BY_USER(1),

        /**
         * Geolocation share is aborted by remote user.
         */
        ABORTED_BY_REMOTE(2),

        /**
         * Geolocation share is aborted by system.
         */
        ABORTED_BY_SYSTEM(3),

        /**
         * Geolocation share is rejected because already taken by the secondary device.
         */
        REJECTED_BY_SECONDARY_DEVICE(4),

        /**
         * Geolocation share invitation was rejected because it is spam.
         */
        REJECTED_SPAM(5),

        /**
         * Geolocation share invitation was rejected due to to many open sharing sessions.
         */
        REJECTED_MAX_SHARING_SESSIONS(6),

        /**
         * Geolocation share invitation was rejected by local user.
         */
        REJECTED_BY_USER(7),

        /**
         * Geolocation share invitation was rejected by remote.
         */
        REJECTED_BY_REMOTE(8),

        /**
         * Geolocation share invitation was rejected by timeout.
         */
        REJECTED_BY_TIMEOUT(9),

        /**
         * Geolocation share initiation failed.
         */
        FAILED_INITIATION(10),

        /**
         * Sharing of the geolocation has failed.
         */
        FAILED_SHARING(11);

        private final int mValue;

        private static SparseArray<ReasonCode> mValueToEnum = new SparseArray<ReasonCode>();
        static {
            for (ReasonCode entry : ReasonCode.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        private ReasonCode(int value) {
            mValue = value;
        }

        public final int toInt() {
            return mValue;
        }

        public final static ReasonCode valueOf(int value) {
            ReasonCode entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException(new StringBuilder("No enum const class ")
                    .append(ReasonCode.class.getName()).append(".").append(value).append("!")
                    .toString());
        }
    }

    /**
     * Geoloc sharing interface
     */
    private IGeolocSharing mSharingInf;

    /**
     * Constructor
     * 
     * @param sharingInf Geoloc sharing interface
     */
    /* package private */GeolocSharing(IGeolocSharing sharingInf) {
        mSharingInf = sharingInf;
    }

    /**
     * Returns the sharing ID of the geoloc sharing
     * 
     * @return Sharing ID
     * @throws RcsServiceException
     */
    public String getSharingId() throws RcsServiceException {
        try {
            return mSharingInf.getSharingId();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the remote contact identifier
     * 
     * @return ContactId
     * @throws RcsServiceException
     */
    public ContactId getRemoteContact() throws RcsServiceException {
        try {
            return mSharingInf.getRemoteContact();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the geolocation info
     * 
     * @return Geoloc object
     * @throws RcsServiceException
     * @see Geoloc
     */
    public Geoloc getGeoloc() throws RcsServiceException {
        try {
            return mSharingInf.getGeoloc();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the state of the sharing
     * 
     * @return State
     * @see State
     * @throws RcsServiceException
     */
    public State getState() throws RcsServiceException {
        try {
            return State.valueOf(mSharingInf.getState());
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the reason code of the state of the sharing
     * 
     * @return ReasonCode
     * @see ReasonCode
     * @throws RcsServiceException
     */
    public ReasonCode getReasonCode() throws RcsServiceException {
        try {
            return ReasonCode.valueOf(mSharingInf.getReasonCode());
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the direction of the sharing
     * 
     * @return Direction
     * @see Direction
     * @throws RcsServiceException
     */
    public Direction getDirection() throws RcsServiceException {
        try {
            return Direction.valueOf(mSharingInf.getDirection());
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the local timestamp of when the geoloc sharing was initiated for outgoing geoloc
     * sharing or the local timestamp of when the geoloc sharing invitation was received for
     * incoming geoloc sharings.
     * 
     * @return long
     * @throws RcsServiceException
     */
    public long getTimestamp() throws RcsServiceException {
        try {
            return mSharingInf.getTimestamp();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Accepts geoloc sharing invitation
     * 
     * @throws RcsServiceException
     */
    public void acceptInvitation() throws RcsServiceException {
        try {
            mSharingInf.acceptInvitation();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Rejects geoloc sharing invitation
     * 
     * @throws RcsServiceException
     */
    public void rejectInvitation() throws RcsServiceException {
        try {
            mSharingInf.rejectInvitation();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Aborts the sharing
     * 
     * @throws RcsServiceException
     */
    public void abortSharing() throws RcsServiceException {
        try {
            mSharingInf.abortSharing();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }
}
